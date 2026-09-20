/**
 * ZS-IAM-002：真实 PG 组织/任职建模与历史账号迁移回填定向验证。
 *
 * H2 单测（OrganizationServiceImplTest / MembershipServiceImplTest / MembershipContextResolverTest）
 * 覆盖应用层组织树守卫、任职生命周期与默认任职上下文解析；但 PG 的部分唯一索引语义
 * （默认任职唯一 / 同账号同组织在职去重 / 组织编码租户内唯一）、历史 dept→组织、users→任职
 * 的回填映射（不误合并、null 部门/无匹配组织/已删除账号不回填）不作 H2 放行门禁，须真实 PG17
 * 验证（对齐 D-09 FND-IAM-001/002/003 与 docs/02 §5.3 退出条件）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + 播种历史 dept/users/user_role
 *       + V20260921.001（组织回填）+ V20260921.002（任职/历史回填）重放。
 * 用例：
 *   P1 迁移重放 + 三表结构断言（关键列 / 3 部分唯一索引 / 3 序列）；
 *   P2 部门回填映射（org type=6 计数 == dept 计数 + ref_dept_id 桥接 + parent_id 沿用 + id 复用）；
 *   P3 历史账号回填不误合并（按 user_id 逐一驱动；null 部门 / 无匹配组织 / 已删除账号均不回填；
 *      每回填任职 is_primary=1、status=1）；
 *   P4 历史流水回填（每回填任职一条 action=1、reason='历史账号迁移回填' 起点流水）；
 *   P5 默认任职唯一约束（第 2 条 is_primary=1 违反 uk_system_membership_primary）；
 *   P6 同账号同组织去重 + 离职后可再入职（在职重复违反 uk_system_membership_user_org；status=4 允许）；
 *   P7 组织编码租户内唯一 + 逻辑删除后可重用 + 空编码多行并存（uk_system_organization_code 部分唯一）；
 *   P8 跨组织隔离 + role_ids JSON 聚合（1001='[3,5]' 有序、1002 无角色=NULL；同账号跨双组织两条任职仅一条默认）。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-iam002-verify.mjs
 *
 * 注：本脚本聚焦 PG 层结构约束与迁移回填契约（部分唯一索引 / 历史映射不误合并）；服务端上下文
 * 「客户端不能伪造组织」的<b>行为级</b>验证由 MembershipContextResolverTest（方法签名仅 userId、
 * fail-closed 降级）承担，非本 SQL 套件职责。
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, '[iam002] Docker 不可用：验证不得静默跳过');

const container = `zszj-iam002-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 3632 + Math.floor(Math.random() * 600);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// 容器启动带端口冲突重试
let started = false;
for (let attempt = 0; attempt < 3 && !started; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=iam002', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    started = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port = 3632 + Math.floor(Math.random() * 600);
  }
}
if (!started) fail(1, '[iam002] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[iam002] PG 未就绪'); }

let dbCreated = false;
for (let attempt = 0; attempt < 3 && !dbCreated; attempt++) {
  try {
    execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
    dbCreated = true;
  } catch {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
}
if (!dbCreated) { cleanup(); fail(1, '[iam002] 建库失败（zhongshu，含 3 次重试）'); }

const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[iam002] 角色授权失败'); }
  const baseline = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', baseline).status !== 0) { cleanup(); fail(1, '[iam002] 基线迁移重放失败（V1/V2）'); }
}

// 播种历史 dept/users/user_role（清空基线可能存在的演示数据，保证计数确定性）
// 语义覆盖：正常账号（有部门有角色 / 有部门无角色）、null 部门、部门无匹配组织、已逻辑删除账号。
{
  const seed = `
DELETE FROM system_user_role;
DELETE FROM system_users;
DELETE FROM system_dept;
INSERT INTO system_dept (id, name, parent_id, sort, leader_user_id, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (100, '平台总部', 0, 1, NULL, 0, '1', now(), '1', now(), 0, 1),
 (101, '门店A', 100, 2, NULL, 0, '1', now(), '1', now(), 0, 1);
INSERT INTO system_users (id, username, password, nickname, dept_id, post_ids, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (1001, 'u1001', '', '张三', 100, '[1,2]', 0, '1', now(), '1', now(), 0, 1),
 (1002, 'u1002', '', '李四', 101, NULL,   0, '1', now(), '1', now(), 0, 1),
 (1003, 'u1003', '', '王五', NULL, NULL,  0, '1', now(), '1', now(), 0, 1),
 (1004, 'u1004', '', '赵六', 999, NULL,   0, '1', now(), '1', now(), 0, 1),
 (1005, 'u1005', '', '钱七', 100, NULL,   0, '1', now(), '1', now(), 1, 1);
INSERT INTO system_user_role (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
 (1, 1001, 5, '1', now(), '1', now(), 0, 1),
 (2, 1001, 3, '1', now(), '1', now(), 0, 1);`;
  if (psql('zhongshu_owner', 'zhongshu', seed).status !== 0) { cleanup(); fail(1, '[iam002] 历史数据播种失败'); }
  const iam = [
    'V20260921.001__system_organization.sql',
    'V20260921.002__system_membership.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', iam).status !== 0) { cleanup(); fail(1, '[iam002] IAM-002 迁移重放失败（V20260921.001/.002）'); }
}
console.log('[iam002] 环境就绪（基线 + 历史播种 + 组织/任职迁移 V20260921.001/.002 重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
const ok = (sql) => psql('zhongshu_owner', 'zhongshu', sql).status === 0;   // 期望成功
const denied = (sql) => psql('zhongshu_owner', 'zhongshu', sql).status !== 0; // 期望被约束拒绝

// P1 迁移重放 + 三表结构断言
{
  const tables = one(`SELECT count(*) FROM information_schema.tables WHERE table_name IN ('system_organization','system_membership','system_membership_history')`);
  const orgCols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name='system_organization'
AND column_name IN ('id','name','code','type','parent_id','ref_dept_id','status','deleted','tenant_id')`);
  const memCols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name='system_membership'
AND column_name IN ('id','user_id','organization_id','post_ids','role_ids','status','valid_from','valid_to','is_primary','deleted','tenant_id')`);
  const hisCols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name='system_membership_history'
AND column_name IN ('id','membership_id','user_id','action','from_organization_id','to_organization_id','from_status','to_status','operator_id','reason')`);
  const uidx = one(`SELECT count(*) FROM pg_indexes WHERE indexname IN ('uk_system_organization_code','uk_system_membership_primary','uk_system_membership_user_org') AND indexdef LIKE '%WHERE%'`);
  const seqs = one(`SELECT count(*) FROM pg_class WHERE relkind='S' AND relname IN ('system_organization_seq','system_membership_seq','system_membership_history_seq')`);
  record('P1 迁移 V20260921.001/.002 重放 + 三表结构（关键列 / 3 部分唯一索引 / 3 序列）',
    tables === '3' && orgCols === '9' && memCols === '11' && hisCols === '10' && uidx === '3' && seqs === '3',
    `tables=${tables} orgCols=${orgCols} memCols=${memCols} hisCols=${hisCols} partialUk=${uidx} seqs=${seqs}`);
}

// P2 部门回填映射（DEPARTMENT 类型组织桥接 system_dept）
{
  const deptCount = one(`SELECT count(*) FROM system_dept WHERE deleted = 0`);
  const orgType6 = one(`SELECT count(*) FROM system_organization WHERE type = 6`);
  const bridge = one(`SELECT count(*) FROM system_organization WHERE type = 6 AND ref_dept_id = id`);
  const parentKept = one(`SELECT parent_id FROM system_organization WHERE id = 101`);
  const nameKept = one(`SELECT name FROM system_organization WHERE id = 100`);
  record('P2 部门回填映射（org type=6 计数==dept + ref_dept_id 桥接 + parent_id 沿用 + id/name 复用）',
    deptCount === orgType6 && orgType6 === '2' && bridge === '2' && parentKept === '100' && nameKept === '平台总部',
    `dept=${deptCount} orgType6=${orgType6} bridge=${bridge} parent(101)=${parentKept} name(100)=${nameKept}`);
}

// P3 历史账号回填不误合并（按 user_id 逐一驱动）
{
  const total = one(`SELECT count(*) FROM system_membership WHERE deleted = 0`);
  const allPrimaryActive = one(`SELECT count(*) FROM system_membership WHERE deleted = 0 AND is_primary = 1 AND status = 1`);
  const u1003 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1003`); // null 部门
  const u1004 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1004`); // 部门无匹配组织
  const u1005 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1005`); // 已逻辑删除
  const distinctUsers = one(`SELECT count(DISTINCT user_id) FROM system_membership WHERE deleted = 0`);
  record('P3 历史账号回填不误合并（1001/1002 各 1 条 primary+active；null 部门/无匹配组织/已删除不回填）',
    total === '2' && allPrimaryActive === '2' && u1003 === '0' && u1004 === '0' && u1005 === '0' && distinctUsers === '2',
    `total=${total} primaryActive=${allPrimaryActive} u1003=${u1003} u1004=${u1004} u1005=${u1005} distinctUsers=${distinctUsers}`);
}

// P4 历史流水回填（每回填任职一条入职起点流水）
{
  const his = one(`SELECT count(*) FROM system_membership_history WHERE action = 1 AND reason = '历史账号迁移回填'`);
  const memCount = one(`SELECT count(*) FROM system_membership WHERE deleted = 0`);
  const orgMapped = one(`SELECT count(*) FROM system_membership_history h JOIN system_membership m ON m.id = h.membership_id WHERE h.to_organization_id = m.organization_id`);
  record('P4 历史流水回填（每回填任职一条 action=1、reason=历史账号迁移回填，且 to_organization 对齐任职组织）',
    his === memCount && his === '2' && orgMapped === '2',
    `history=${his} membership=${memCount} orgAligned=${orgMapped}`);
}

// P5 默认任职唯一约束（第 2 条 is_primary=1 违反 uk_system_membership_primary）
{
  const dup = denied(`INSERT INTO system_membership (id, user_id, organization_id, status, is_primary, tenant_id)
VALUES (nextval('system_membership_seq'), 1002, 101, 1, 1, 1)`);
  const stillOne = one(`SELECT count(*) FROM system_membership WHERE user_id = 1002 AND is_primary = 1 AND deleted = 0`);
  record('P5 默认任职唯一约束（同账号第 2 条 is_primary=1 被 uk_system_membership_primary 拒绝，仍恒 1 条默认）',
    dup && stillOne === '1',
    `secondPrimaryDenied=${dup} primaryCount(1002)=${stillOne}`);
}

// P6 同账号同组织去重 + 离职后可再入职
{
  const activeDup = denied(`INSERT INTO system_membership (id, user_id, organization_id, status, is_primary, tenant_id)
VALUES (nextval('system_membership_seq'), 1001, 100, 1, 0, 1)`); // 已有在职回填任职
  const suspendDup = denied(`INSERT INTO system_membership (id, user_id, organization_id, status, is_primary, tenant_id)
VALUES (nextval('system_membership_seq'), 1001, 100, 2, 0, 1)`); // status IN (1,2) 均占位
  const terminatedOk = ok(`INSERT INTO system_membership (id, user_id, organization_id, status, is_primary, tenant_id)
VALUES (nextval('system_membership_seq'), 1001, 100, 4, 0, 1)`); // 离职不占位，允许再入职留痕
  record('P6 同账号同组织去重（在职/停用重复被 uk_system_membership_user_org 拒绝；离职 status=4 允许）',
    activeDup && suspendDup && terminatedOk,
    `activeDupDenied=${activeDup} suspendDupDenied=${suspendDup} terminatedAllowed=${terminatedOk}`);
}

// P7 组织编码租户内唯一 + 逻辑删除后可重用 + 空编码多行并存
{
  const first = ok(`INSERT INTO system_organization (id, name, code, type, parent_id, status, tenant_id)
VALUES (nextval('system_organization_seq'), '品牌甲', 'BRAND_X', 2, 0, 0, 1)`);
  const codeId = one(`SELECT id FROM system_organization WHERE code = 'BRAND_X' AND deleted = 0`);
  const dup = denied(`INSERT INTO system_organization (id, name, code, type, parent_id, status, tenant_id)
VALUES (nextval('system_organization_seq'), '品牌乙', 'BRAND_X', 2, 0, 0, 1)`);
  const logicalDelete = ok(`UPDATE system_organization SET deleted = 1 WHERE id = ${codeId}`);
  const reuse = ok(`INSERT INTO system_organization (id, name, code, type, parent_id, status, tenant_id)
VALUES (nextval('system_organization_seq'), '品牌甲重建', 'BRAND_X', 2, 0, 0, 1)`);
  const empty1 = ok(`INSERT INTO system_organization (id, name, code, type, parent_id, status, tenant_id)
VALUES (nextval('system_organization_seq'), '空码1', '', 4, 0, 0, 1)`);
  const empty2 = ok(`INSERT INTO system_organization (id, name, code, type, parent_id, status, tenant_id)
VALUES (nextval('system_organization_seq'), '空码2', '', 4, 0, 0, 1)`);
  record('P7 组织编码租户内唯一（重复拒绝 / 逻辑删除后可重用 / 空编码多行并存）',
    first && dup && logicalDelete && reuse && empty1 && empty2,
    `first=${first} dupDenied=${dup} logicalDelete=${logicalDelete} reuse=${reuse} emptyCoexist=${empty1 && empty2}`);
}

// P8 跨组织隔离 + role_ids JSON 聚合
{
  const role1001 = one(`SELECT role_ids FROM system_membership WHERE user_id = 1001 AND is_primary = 1 AND deleted = 0`);
  const role1002 = one(`SELECT coalesce(role_ids,'NULL') FROM system_membership WHERE user_id = 1002 AND is_primary = 1 AND deleted = 0`);
  // 同账号跨第二组织（非默认、在职）→ 允许，验证一账号多任职跨组织隔离
  const secondOrg = ok(`INSERT INTO system_membership (id, user_id, organization_id, status, is_primary, tenant_id)
VALUES (nextval('system_membership_seq'), 1001, 101, 1, 0, 1)`);
  const orgs = one(`SELECT count(DISTINCT organization_id) FROM system_membership WHERE user_id = 1001 AND deleted = 0 AND status IN (1,2)`);
  const primaries = one(`SELECT count(*) FROM system_membership WHERE user_id = 1001 AND is_primary = 1 AND deleted = 0`);
  record('P8 跨组织隔离 + role_ids JSON 聚合（1001=[3,5] 有序、1002 无角色=NULL；跨双组织两任职仅一条默认）',
    role1001 === '[3,5]' && role1002 === 'NULL' && secondOrg && orgs === '2' && primaries === '1',
    `role1001=${role1001} role1002=${role1002} secondOrg=${secondOrg} distinctOrgs=${orgs} primaries=${primaries}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
