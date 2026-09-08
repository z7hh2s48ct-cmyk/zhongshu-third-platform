/**
 * ZS-DB-004：把 sql/postgresql 种子整理为 Flyway V1 基线迁移。
 *
 * 输入：sql/postgresql/ruoyi-vue-pro.sql + sql/postgresql/quartz.sql（改名后的新装基线）
 * 输出：
 *   1. zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql
 *      —— 全量 DDL（含 quartz 调度表）+ 必要种子（字典/菜单[仅启用模块]/角色/租户/岗位/部门[租户1]/
 *         OAuth2 default 客户端/infra 参数/必要清理 Job）；不含任何用户账号与演示数据。
 *   2. sql/postgresql/demo-data-optional.sql —— 被分离的演示数据（不随应用执行）。
 * 规则（与 docs/数据库迁移规范.md、ZS-ENG-001 白名单一致）：
 *   - 移除全部 DROP 语句（Flyway 历史表防重跑；V1 不提供破坏性重放）；
 *   - 菜单仅保留启用模块（system/infra）子树：permission 前缀、component/path 前缀 + 祖先闭包；
 *   - 角色仅保留超级管理员(1)/普通角色(2)；租户仅保留 id=1；OAuth2 客户端仅保留 default；
 *   - Job 仅保留启用模块的日志清理类（accessLog/errorLog/jobLog CleanJob）；
 *   - 用户一律不迁移：初始管理员经安全初始化流程建立（init-admin.sql.example），不复用演示凭据。
 * 用法：node scripts/db/build-baseline-migration.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const sqlDir = join(root, 'services/zhongshu-core/sql/postgresql');
const outMigration = join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql');
const outDemo = join(root, 'services/zhongshu-core/sql/postgresql/demo-data-optional.sql');

const ENABLED = ['system', 'infra'];
const KEEP_ROLES = new Set([1, 2]);
const KEEP_TENANTS = new Set([1]);
const KEEP_JOBS = new Set(['accessLogCleanJob', 'errorLogCleanJob', 'jobLogCleanJob']);
const KEEP_OAUTH_CLIENTS = new Set(['default']);

/** 按行累积解析语句（语句以行尾分号结束），返回 {kind, table, text, lines} */
function parseStatements(text) {
  const statements = [];
  let buf = [];
  for (const raw of text.split(/\r?\n/)) {
    buf.push(raw);
    const trimmed = raw.trimEnd();
    if (trimmed.endsWith(';')) {
      // 去除前导空行/注释行后分类（"-- ----"分隔符注释不参与语句判定）
      const text1 = buf.join('\n');
      buf = [];
      const head = text1.replace(/^(\s*--[^\n]*\n|\s*\n)*/, '').trimStart();
      if (!head) continue;
      const kind = /^(CREATE TABLE|CREATE SEQUENCE|CREATE INDEX|ALTER TABLE|COMMENT ON|DROP TABLE|DROP SEQUENCE|INSERT INTO (\w+)|SELECT setval)/.exec(head);
      if (!kind) continue;
      statements.push({
        kind: kind[1],
        table: kind[2] ?? null,
        text: text1,
      });
    }
  }
  return statements;
}

/** 解析 INSERT VALUES 行的字段（单引号转义 ''），返回字符串数组 */
function parseValues(stmt, columns) {
  const m = /VALUES \(([\s\S]*)\);?\s*$/.exec(stmt.text.trimEnd());
  if (!m) return null;
  const fields = [];
  let cur = '', inStr = false;
  const body = m[1];
  for (let i = 0; i < body.length; i++) {
    const ch = body[i];
    if (inStr) {
      if (ch === "'" && body[i + 1] === "'") { cur += "''"; i++; continue; }
      if (ch === "'") { inStr = false; continue; }
      cur += ch; continue;
    }
    if (ch === "'") { inStr = true; continue; }
    if (ch === ',') { fields.push(cur.trim()); cur = ''; continue; }
    cur += ch;
  }
  fields.push(cur.trim());
  return Object.fromEntries(columns.map((c, i) => [c, fields[i]]));
}

function columnsOf(stmt) {
  const m = /INSERT INTO \w+ \(([^)]+)\) VALUES/.exec(stmt.text);
  return m ? m[1].split(',').map((c) => c.trim()) : [];
}

// ============ 1. 解析主种子 ============
const main = parseStatements(readFileSync(join(sqlDir, 'ruoyi-vue-pro.sql'), 'utf8'));
const quartz = parseStatements(readFileSync(join(sqlDir, 'quartz.sql'), 'utf8'));

const ddl = [], demo = [], keep = [];
const buckets = { menu: [], dictType: [], dictData: [], roleMenu: [], role: [], dept: [], post: [], tenant: [], oauthClient: [], config: [], job: [], other: [] };

let dropped = 0;
for (const stmt of main) {
  if (stmt.kind === 'DROP TABLE' || stmt.kind === 'DROP SEQUENCE') { dropped++; continue; }
  if (stmt.table) {
    const cols = columnsOf(stmt);
    const v = parseValues(stmt, cols);
    if (!v) { demo.push(stmt); continue; }
    if (stmt.table === 'system_menu') { buckets.menu.push({ stmt, v }); continue; }
    if (stmt.table === 'system_role_menu') { buckets.roleMenu.push({ stmt, v }); continue; }
    if (stmt.table === 'system_role') { buckets.role.push({ stmt, v }); continue; }
    if (stmt.table === 'system_dept') { buckets.dept.push({ stmt, v }); continue; }
    if (stmt.table === 'system_tenant') { buckets.tenant.push({ stmt, v }); continue; }
    if (stmt.table === 'system_oauth2_client') { buckets.oauthClient.push({ stmt, v }); continue; }
    if (stmt.table === 'infra_job') { buckets.job.push({ stmt, v }); continue; }
    if (stmt.table === 'system_dict_type' || stmt.table === 'system_dict_data' || stmt.table === 'system_post' || stmt.table === 'infra_config') {
      keep.push(stmt); continue;
    }
    demo.push(stmt); continue; // users/notice/sms/mail/notify/user_role/user_post/oauth token/demo 表数据等 → 演示分离
  }
  ddl.push(stmt);
}

// ============ 2. 菜单启用模块子树 ============
const menusById = new Map(buckets.menu.map(({ v }) => [Number(v.id), v]));
const keptMenus = new Set();
for (const { v } of buckets.menu) {
  const perm = String(v.permission ?? '');
  const component = String(v.component ?? '');
  const path = String(v.path ?? '');
  if (/^(system|infra):/.test(perm)) keptMenus.add(Number(v.id));
  else if (/^(system|infra)\//.test(component)) keptMenus.add(Number(v.id));
  else if (/^\/?(system|infra)(\/|$)/.test(path) && (v.type === '1' || v.type === '2')) keptMenus.add(Number(v.id));
}
// 祖先闭包：保留被保留节点的全部父链
let changed = true;
while (changed) {
  changed = false;
  for (const id of [...keptMenus]) {
    const v = menusById.get(id);
    const pid = Number(v?.parent_id ?? 0);
    if (pid !== 0 && !keptMenus.has(pid)) { keptMenus.add(pid); changed = true; }
  }
}

// 部门：仅租户 1，且 leader_user_id 指向被分离的演示账号时置 NULL
const keptDeptStmts = [];
for (const { stmt: deptStmt, v } of buckets.dept) {
  if (!KEEP_TENANTS.has(Number(v.tenant_id))) { demo.push(deptStmt); continue; }
  let text = deptStmt.text;
  if (v.leader_user_id && v.leader_user_id !== 'NULL') {
    text = deptStmt.text.replace(/(VALUES \([0-9]+, '[^']*', [0-9]+, [0-9]+, )(?:'[0-9]+'|NULL|[0-9]+)/, '$1NULL');
  }
  keptDeptStmts.push({ ...deptStmt, text });
}

// ============ 3. 组装 ============
const header = `-- =====================================================================
-- V20260909.001：底座基线（ZS-DB-004）
-- 来源：sql/postgresql/ruoyi-vue-pro.sql + quartz.sql（经 ZS-BRAND 改名后的新装基线）
-- 生成：scripts/db/build-baseline-migration.mjs（确定性规则转换，规则见文件头与
--       docs/数据库迁移规范.md）；本文件为 Flyway 唯一权威迁移入口的 V1。
-- 边界：不含任何用户账号（初始管理员经 sql/postgresql/init-admin.sql.example 安全初始化）；
--       演示数据分离至 sql/postgresql/demo-data-optional.sql（不随应用执行）；
--       菜单仅含启用模块（system/infra）子树；全部 DROP 语句已移除。
-- =====================================================================
`;

const ddlSection = [
  '\n-- ============================================================\n-- 1. 表结构 / 序列 / 索引 / 注释（含 Quartz 调度表）\n-- ============================================================\n',
  ...ddl.map((s) => s.text),
  ...quartz.filter((s) => s.kind !== 'DROP TABLE' && s.kind !== 'DROP SEQUENCE').map((s) => s.text),
].join('\n');

const seedSection = [
  '\n-- ============================================================\n-- 2. 必要种子（字典全部保留；菜单/角色/租户/客户端/任务按白名单裁剪）\n-- ============================================================\n',
  ...keep.map((s) => s.text),
  ...buckets.menu.filter(({ v }) => keptMenus.has(Number(v.id))).map(({ stmt }) => stmt.text),
  ...buckets.role.filter(({ v }) => KEEP_ROLES.has(Number(v.id))).map(({ stmt }) => stmt.text),
  ...buckets.roleMenu.filter(({ v }) => KEEP_ROLES.has(Number(v.role_id)) && keptMenus.has(Number(v.menu_id))).map(({ stmt }) => stmt.text),
  ...keptDeptStmts.map((s) => s.text),
  ...buckets.post.filter(({ stmt, v }) => { if (!KEEP_TENANTS.has(Number(v.tenant_id ?? 1))) { demo.push(stmt); return false; } return true; }).map(({ stmt }) => stmt.text),
  ...buckets.tenant.filter(({ stmt, v }) => { if (!KEEP_TENANTS.has(Number(v.id))) { demo.push(stmt); return false; } return true; }).map(({ stmt }) => stmt.text),
  ...buckets.oauthClient.filter(({ stmt, v }) => { if (!KEEP_OAUTH_CLIENTS.has(String(v.client_id))) { demo.push(stmt); return false; } return true; }).map(({ stmt }) => stmt.text),
  ...buckets.job.filter(({ stmt, v }) => { const h = String(v.handler_name ?? ''); if (!KEEP_JOBS.has(h)) { demo.push(stmt); return false; } return true; }).map(({ stmt }) => stmt.text),
].join('\n');

writeFileSync(outMigration, header + ddlSection + '\n' + seedSection + '\n', 'utf8');

const demoHeader = `-- =====================================================================
-- 演示数据（可选，ZS-DB-004 与正式基线分离）
-- 不随应用/Flyway 执行；仅开发/演示环境手工按需执行。
-- 来源：sql/postgresql/ruoyi-vue-pro.sql 整理过程中被分离的 INSERT 语句。
-- =====================================================================
`;
writeFileSync(outDemo, demoHeader + '\n' + demo.map((s) => s.text).join('\n') + '\n', 'utf8');

const stats = {
  ddlStatements: ddl.length + quartz.length,
  dropRemoved: dropped,
  menuTotal: buckets.menu.length, menuKept: keptMenus.size,
  roleKept: KEEP_ROLES.size,
  tenantKept: buckets.tenant.filter(({ v }) => KEEP_TENANTS.has(Number(v.id))).length,
  oauthKept: buckets.oauthClient.filter(({ v }) => KEEP_OAUTH_CLIENTS.has(String(v.client_id))).length,
  jobKept: buckets.job.filter(({ v }) => KEEP_JOBS.has(String(v.handler_name))).length,
  demoStatements: demo.length,
};
console.log(JSON.stringify(stats, null, 2));
