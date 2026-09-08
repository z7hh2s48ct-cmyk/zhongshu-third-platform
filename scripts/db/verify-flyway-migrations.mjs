/**
 * ZS-DB-003 迁移机制静态检查（规范见 services/zhongshu-core/docs/数据库迁移规范.md）。
 *
 * 检查项：
 *  1. Flyway 依赖已在 zszj-server 声明（flyway-core + flyway-database-postgresql，无手写版本号）；
 *  2. 基础配置合同：enabled=false、单一 location、out-of-order=false、validate-on-migrate=true、
 *     clean-disabled=true、baseline-on-migrate=true；
 *  3. 部署模板经 ZSZJ_FLYWAY_ENABLED 激活；
 *  4. 迁移目录内文件命名符合 V<8位日期>.<3位序号>__<描述>.sql 且版本号唯一；
 *  5. 不存在第二套会随应用执行的迁移目录（zszj-server 资源内不得有其他 flyway location 声明）。
 * 用法：node scripts/db/verify-flyway-migrations.mjs
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
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

console.log(JSON.stringify({ issueCount: issues.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
