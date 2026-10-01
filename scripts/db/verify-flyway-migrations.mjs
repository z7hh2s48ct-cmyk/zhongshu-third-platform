/**
 * ZS-DB-003 迁移机制静态检查（规范见 services/zhongshu-core/docs/数据库迁移规范.md）。
 *
 * 检查项：
 *  1. Flyway 依赖已在 zszj-server 声明（flyway-core + flyway-database-postgresql，无手写版本号）；
 *  2. 基础配置合同：enabled=false、单一 location、out-of-order=false、validate-on-migrate=true、
 *     clean-disabled=true、baseline-on-migrate=true；
 *  3. 部署模板经 ZSZJ_FLYWAY_ENABLED 激活；
 *  4. 迁移目录内文件命名符合 V<8位日期>.<3位序号>__<描述>.sql 且版本号唯一；
 *  5. 不存在第二套会随应用执行的迁移目录（zszj-server 资源内不得有其他 flyway location 声明）；
 *  6. MyBatis-Plus 逻辑删除表（@TableName 绑定）的 deleted 列最终类型为 boolean 时，其 DO 必须字段级覆写
 *     `@TableLogic(value = "FALSE", delval = "TRUE")`——全局 @TableLogic 为 0/1 数值字面量，MP 注入的 selectById 等
 *     拼 `deleted = 0`，PG 上 `boolean = integer` 不成立；H2 单测与手写 `deleted = FALSE` 路径均不暴露
 *     （首链申请域 get/page 真实 PG 500、MSG 域 send_log/todo 同款错配的根因）。平台基线约定 `deleted int2 DEFAULT 0`
 *     （无需覆写）；仅 JDBC 手写访问、无 MP DO 的表不受此规则约束。
 * 用法：node scripts/db/verify-flyway-migrations.mjs
 */
import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const issues = [];

// 1. 依赖
const pom = readFileSync(join(core, 'zszj-server/pom.xml'), 'utf8');
if (!pom.includes('<artifactId>flyway-core</artifactId>')) issues.push('zszj-server 缺少 flyway-core 依赖');
if (!pom.includes('<artifactId>flyway-database-postgresql</artifactId>')) issues.push('缺少 flyway-database-postgresql（Flyway 10+ PG 支持模块）');
for (const m of pom.matchAll(/<artifactId>flyway[^<]*<\/artifactId>([\s\S]{0,40}?<version>)/g)) {
  issues.push(`flyway 依赖不应手写版本号（应随 Spring Boot BOM）: ${m[1] && '发现 version 标签'}`);
}

// 2. 基础配置合同
const base = readFileSync(join(core, 'zszj-server/src/main/resources/application.yaml'), 'utf8');
const contract = [
  ['enabled: false', 'flyway 默认必须关闭（基线由 ZS-DB-004 交付）'],
  ['locations: classpath:db/migration', '必须声明唯一迁移目录'],
  ['out-of-order: false', '必须禁止乱序迁移'],
  ['validate-on-migrate: true', '必须开启篡改校验'],
  ['clean-disabled: true', '必须全环境禁用 clean'],
  ['baseline-on-migrate: true', '必须允许既有旧库吸收为基线'],
];
for (const [needle, why] of contract) {
  if (!base.includes(needle)) issues.push(`基础配置缺少 ${needle}（${why}）`);
}

// 3. 部署模板
const prod = readFileSync(join(core, 'script/config/application-prod.yaml'), 'utf8');
if (!prod.includes('ZSZJ_FLYWAY_ENABLED')) issues.push('部署模板缺少 ZSZJ_FLYWAY_ENABLED 激活开关');

// 4. 迁移文件命名与唯一性
const migrationDir = join(core, 'zszj-server/src/main/resources/db/migration');
if (!existsSync(migrationDir)) issues.push('迁移目录 db/migration 不存在');
else {
  const versions = new Set();
  const nameRe = /^V(\d{8})\.(\d{3})__([a-z0-9_]+)\.sql$/;
  for (const name of readdirSync(migrationDir)) {
    if (!name.endsWith('.sql')) continue;
    const m = nameRe.exec(name);
    if (!m) { issues.push(`迁移文件命名不符合规范: ${name}（应为 V<8位日期>.<3位序号>__<module>_<描述>.sql）`); continue; }
    const version = `V${m[1]}.${m[2]}`;
    if (versions.has(version)) issues.push(`迁移版本号重复: ${version}`);
    versions.add(version);
  }
}

// 5. 无第二套 location
const configFiles = [
  ['application-local.yaml', join(core, 'zszj-server/src/main/resources/application-local.yaml')],
  ['application-dev.yaml', join(core, 'zszj-server/src/main/resources/application-dev.yaml')],
  ['application-prod.yaml（部署模板）', join(core, 'script/config/application-prod.yaml')],
];
for (const [label, path] of configFiles) {
  const text = readFileSync(path, 'utf8');
  const matches = [...text.matchAll(/locations:\s*(.+)$/gm)];
  for (const m of matches) {
    if (!m[1].includes('classpath:db/migration')) issues.push(`${label} 声明了规范外的迁移 location: ${m[1].trim()}`);
  }
}

// 6. MP 逻辑删除表的 deleted 列最终类型（按版本顺序回放 CREATE TABLE / ALTER COLUMN TYPE，取最终态）
if (existsSync(migrationDir)) {
  const finalDeletedType = new Map(); // table -> 最终 deleted 列类型（小写）
  const files = readdirSync(migrationDir).filter((n) => /^V\d{8}\.\d{3}__.+\.sql$/.test(n)).sort();
  for (const name of files) {
    const text = readFileSync(join(migrationDir, name), 'utf8');
    // CREATE TABLE [IF NOT EXISTS] <t> ( ... ); —— 体取到首个行首 `);`
    for (const m of text.matchAll(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?"?([a-z0-9_]+)"?\s*\(([\s\S]*?)\n\)\s*;/gi)) {
      const col = /^\s*"?deleted"?\s+([a-z0-9_]+)/im.exec(m[2]);
      if (col) finalDeletedType.set(m[1].toLowerCase(), col[1].toLowerCase());
    }
    // ALTER TABLE <t> ALTER COLUMN deleted [SET DATA] TYPE <type>
    for (const m of text.matchAll(/ALTER\s+TABLE\s+(?:ONLY\s+)?"?([a-z0-9_]+)"?\s+ALTER\s+COLUMN\s+"?deleted"?\s+(?:SET\s+DATA\s+)?TYPE\s+([a-z0-9_]+)/gi)) {
      finalDeletedType.set(m[1].toLowerCase(), m[2].toLowerCase());
    }
  }
  // MP 绑定表：services/zhongshu-core 下主源码的 @TableName("<table>")
  const boundTables = new Map(); // table -> 首个出现的 DO 文件
  const walk = (dir) => {
    for (const name of readdirSync(dir)) {
      if (name === 'target' || name === 'node_modules' || name === '.git') continue;
      const full = join(dir, name);
      const st = statSync(full);
      if (st.isDirectory()) { if (name === 'test') continue; walk(full); continue; }
      if (!name.endsWith('.java')) continue;
      const src = readFileSync(full, 'utf8');
      // 字段级覆写：@TableLogic(value = "FALSE", ...) —— boolean 列的合法形态
      const booleanOverride = /@(?:com\.baomidou\.mybatisplus\.annotation\.)?TableLogic\s*\(\s*value\s*=\s*"(?:FALSE|false)"/.test(src);
      for (const m of src.matchAll(/@TableName\(\s*(?:value\s*=\s*)?"([a-zA-Z0-9_]+)"/g)) {
        if (!boundTables.has(m[1].toLowerCase())) boundTables.set(m[1].toLowerCase(), { file: full.slice(core.length + 1), booleanOverride });
      }
    }
  };
  walk(core);
  for (const [table, { file, booleanOverride }] of boundTables) {
    const type = finalDeletedType.get(table);
    if ((type === 'boolean' || type === 'bool') && !booleanOverride) {
      issues.push(`MP 逻辑删除表 ${table}（${file}）的 deleted 列最终类型为 ${type}，但 DO 未字段级覆写 @TableLogic(value = "FALSE", delval = "TRUE")：`
        + '全局逻辑删除字面量为 0/1，MP 注入的 selectById 等拼 deleted = 0，PG 上 boolean = integer 不成立（H2 与手写 deleted = FALSE 路径均不暴露）；'
        + '修复二选一：DO 的 deleted 字段加覆写（MSG-004 NotifyChannelSendDO 先例），或新迁移 ALTER COLUMN deleted TYPE smallint（须先重建引用 deleted 的部分索引）');
    }
  }
}

console.log(JSON.stringify({ issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
