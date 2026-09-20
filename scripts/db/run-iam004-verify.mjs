/**
 * ZS-IAM-004：历史归属迁移「前后核对 + 恢复方案」定向验证。
 *
 * 背景：存量技术账号 → 任职的历史归属回填已由 ZS-IAM-002 的 V20260921.002 交付（按 user_id 逐一驱动，
 * 绝不按 username/mobile 聚合）。run-iam002-verify 的 P3/P4 已断言「不误合并 / 起点流水」。本脚本承担
 * IAM-004 卡片验收「迁移有前后核对和恢复方案」的增量职责：
 *   R1 前后核对（parity）：迁移前「合格账号」快照 == 迁移后回填任职，逐一对应、无遗漏、无越界、无合并；
 *   R2 一账号一 primary：回填后每合格账号恰一条 is_primary=1 且 status=1 的任职；
 *   R3 不合格不回填：null 部门 / 部门无匹配组织 / 已逻辑删除账号，迁移前后计数恒 0；
 *   R4 恢复方案演练（rollback）：按迁移标记回滚回填任职 + 起点流水后，三表回到迁移前基线（计数归零）；
 *   R5 幂等重放：恢复后重新执行回填段，parity 与 R1 一致（恢复方案可安全重试，不产生重复/悬挂）。
 *
 * 「合格账号」定义（与 V20260921.002 回填 WHERE 完全同口径）：
 *   system_users.deleted = 0 AND dept_id IS NOT NULL AND EXISTS(匹配的同 id 组织，deleted = 0)。
 *
 * 恢复方案（rollback SQL，演练于 R4，生产回滚须先备份 + 停写窗口）：
 *   DELETE FROM system_membership_history WHERE reason = '历史账号迁移回填';
 *   DELETE FROM system_membership m WHERE NOT EXISTS (
 *     SELECT 1 FROM system_membership_history h WHERE h.membership_id = m.id AND h.reason <> '历史账号迁移回填');
 *   —— 仅回收「纯回填、无后续业务流水」的任职，绝不触碰迁移后已产生生命周期流水的行。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + 播种历史 dept/users/user_role
 *       + V20260921.001（组织回填）重放；V20260921.002 的回填段由本脚本按前后核对需要分步执行。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-iam004-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, '[iam004] Docker 不可用：验证不得静默跳过');

const container = `zszj-iam004-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 4232 + Math.floor(Math.random() * 600);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

let started = false;
for (let attempt = 0; attempt < 3 && !started; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=iam004', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    started = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port = 4232 + Math.floor(Math.random() * 600);
  }
}
if (!started) fail(1, '[iam004] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[iam004] PG 未就绪'); }

let dbCreated = false;
for (let attempt = 0; attempt < 3 && !dbCreated; attempt++) {
  try {
    execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
    dbCreated = true;
  } catch { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
}
if (!dbCreated) { cleanup(); fail(1, '[iam004] 建库失败（zhongshu，含 3 次重试）'); }

const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[iam004] 角色授权失败'); }
  const baseline = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', baseline).status !== 0) { cleanup(); fail(1, '[iam004] 基线迁移重放失败（V1/V2）'); }
}

// 播种历史 dept/users/user_role：覆盖 合格（有部门有角色 / 有部门无角色）、null 部门、部门无匹配组织、已逻辑删除。
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
  if (psql('zhongshu_owner', 'zhongshu', seed).status !== 0) { cleanup(); fail(1, '[iam004] 历史数据播种失败'); }
  // 仅重放组织回填（V20260921.001）；任职回填段由本脚本分步执行以做前后核对。
  const org = readFileSync(join(root, mig + 'V20260921.001__system_organization.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', org).status !== 0) { cleanup(); fail(1, '[iam004] 组织迁移重放失败（V20260921.001）'); }
  // 建任职/历史表结构（V20260921.002 的 DDL 段），但暂不执行回填 INSERT——回填由 R1 前后核对驱动。
  const membershipDdl = readFileSync(join(root, mig + 'V20260921.002__system_membership.sql'), 'utf8')
    .split(/\n(?=-- 历史技术账号回填)/)[0];
  if (psql('zhongshu_owner', 'zhongshu', membershipDdl).status !== 0) { cleanup(); fail(1, '[iam004] 任职表 DDL 重放失败'); }
}

// 迁移前「合格账号」快照（与 V20260921.002 回填 WHERE 同口径）
const ELIGIBLE = `SELECT count(*) FROM system_users u
 WHERE u.deleted = 0 AND u.dept_id IS NOT NULL
   AND EXISTS (SELECT 1 FROM system_organization o WHERE o.id = u.dept_id AND o.deleted = 0)`;
const BACKFILL = readFileSync(join(root, mig + 'V20260921.002__system_membership.sql'), 'utf8')
  .split(/\n(?=-- 历史技术账号回填)/)[1] || '';
if (!BACKFILL.includes('INSERT INTO system_membership')) { cleanup(); fail(1, '[iam004] 未能从 V20260921.002 切分出回填段'); }

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
const run = (sql) => psql('zhongshu_owner', 'zhongshu', sql).status === 0;

const eligibleBefore = one(ELIGIBLE);
console.log(`[iam004] 迁移前合格账号快照 = ${eligibleBefore}`);

// 执行回填（迁移）
if (!run(BACKFILL)) { cleanup(); fail(1, '[iam004] 回填段执行失败'); }

// R1 前后核对：合格账号数 == 回填任职数 == 回填 distinct 账号数（无遗漏、无越界、无合并）
{
  const memCount = one(`SELECT count(*) FROM system_membership WHERE deleted = 0`);
  const distinctUsers = one(`SELECT count(DISTINCT user_id) FROM system_membership WHERE deleted = 0`);
  // 每个回填任职都能反查到合格账号（无孤儿）；每个合格账号都被回填（无遗漏）
  const orphan = one(`SELECT count(*) FROM system_membership m WHERE m.deleted = 0 AND NOT EXISTS (SELECT 1 FROM system_users u WHERE u.id = m.user_id AND u.deleted = 0 AND u.dept_id IS NOT NULL)`);
  const missed = one(`SELECT count(*) FROM system_users u WHERE u.deleted = 0 AND u.dept_id IS NOT NULL AND EXISTS (SELECT 1 FROM system_organization o WHERE o.id = u.dept_id AND o.deleted = 0) AND NOT EXISTS (SELECT 1 FROM system_membership m WHERE m.user_id = u.id AND m.deleted = 0)`);
  record('R1 前后核对 parity（合格账号数 == 回填任职数 == distinct 账号数；无孤儿、无遗漏）',
    eligibleBefore === memCount && memCount === distinctUsers && orphan === '0' && missed === '0',
    `eligibleBefore=${eligibleBefore} memCount=${memCount} distinctUsers=${distinctUsers} orphan=${orphan} missed=${missed}`);
}

// R2 一账号一 primary（回填后每合格账号恰一条 is_primary=1 且 status=1）
{
  const primaryActive = one(`SELECT count(*) FROM system_membership WHERE deleted = 0 AND is_primary = 1 AND status = 1`);
  const multiPrimary = one(`SELECT count(*) FROM (SELECT user_id FROM system_membership WHERE deleted = 0 AND is_primary = 1 GROUP BY user_id HAVING count(*) > 1) t`);
  record('R2 一账号一 primary（is_primary=1&status=1 计数 == 合格账号数；无账号多 primary）',
    primaryActive === eligibleBefore && multiPrimary === '0',
    `primaryActive=${primaryActive} eligible=${eligibleBefore} multiPrimaryUsers=${multiPrimary}`);
}

// R3 不合格不回填（null 部门 1003 / 部门无匹配组织 1004 / 已逻辑删除 1005 迁移前后恒 0）
{
  const u1003 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1003`);
  const u1004 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1004`);
  const u1005 = one(`SELECT count(*) FROM system_membership WHERE user_id = 1005`);
  record('R3 不合格不回填（null 部门 / 无匹配组织 / 已删除账号均 0 任职，不执行未批准账号合并）',
    u1003 === '0' && u1004 === '0' && u1005 === '0',
    `u1003(null部门)=${u1003} u1004(无匹配组织)=${u1004} u1005(已删除)=${u1005}`);
}

// R4 恢复方案演练：按迁移标记回滚回填任职 + 起点流水，三表回到迁移前基线
{
  const rollback = `
DELETE FROM system_membership_history WHERE reason = '历史账号迁移回填';
DELETE FROM system_membership m WHERE NOT EXISTS (
  SELECT 1 FROM system_membership_history h WHERE h.membership_id = m.id AND h.reason <> '历史账号迁移回填');`;
  const rolled = run(rollback);
  const memAfter = one(`SELECT count(*) FROM system_membership WHERE deleted = 0`);
  const hisAfter = one(`SELECT count(*) FROM system_membership_history WHERE deleted = 0`);
  record('R4 恢复方案演练（回滚纯回填任职 + 起点流水后，任职/历史计数归零，回到迁移前基线）',
    rolled && memAfter === '0' && hisAfter === '0',
    `rollback=${rolled} membershipAfter=${memAfter} historyAfter=${hisAfter}`);
}

// R5 幂等重放：恢复后重新回填，parity 与 R1 一致（恢复方案可安全重试）
{
  const reapplied = run(BACKFILL);
  const memCount = one(`SELECT count(*) FROM system_membership WHERE deleted = 0`);
  const distinctUsers = one(`SELECT count(DISTINCT user_id) FROM system_membership WHERE deleted = 0`);
  const hisCount = one(`SELECT count(*) FROM system_membership_history WHERE action = 1 AND reason = '历史账号迁移回填'`);
  record('R5 幂等重放（恢复后重新回填，任职/distinct/起点流水计数与迁移前合格账号一致，无重复/悬挂）',
    reapplied && memCount === eligibleBefore && distinctUsers === eligibleBefore && hisCount === eligibleBefore,
    `reapplied=${reapplied} memCount=${memCount} distinctUsers=${distinctUsers} historyBackfill=${hisCount}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
