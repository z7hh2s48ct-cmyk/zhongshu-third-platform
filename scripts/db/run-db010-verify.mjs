/**
 * ZS-DB-010：账号唯一约束与并发兜底——真实 PG 验证用例集。
 *
 * 背景：ZS-DB-009 实证 system_users 的 username/mobile/email 仅普通索引，
 *       应用层「先查重后写入」在并发与跨租户下无法拦截重复账号。
 * 前置：一次性 PG17 容器 + V1 基线 + V20260920.001 账号唯一约束迁移 + 双账号授权。
 * 用例：
 *   P0 复现回归：真并发重叠事务插相同 username → 在册行数 = 1（DB-009 场景不再产生重复）；
 *   P1 结构：三个部分唯一索引存在、idx_01/02/03 等值查询普通索引保留、idx_04(dept_id) 保留；
 *   P2 历史冲突 fail-loudly：存量有重复时重跑迁移 RAISE EXCEPTION（禁止擅自删重）；
 *   P3 并发兜底：两会话真并发重叠事务插相同 username → 恰一个成功、另一个撞 23505；
 *   P4 大小写规范化：'Admin' 与 'admin'/' ADMIN ' 撞 lower() 表达式唯一索引；
 *   P5 删除重建：逻辑删除(deleted=1)后同 username 可重建，在册仍唯一；
 *   P6 空值多行：mobile/email 为空串或 NULL 时多行共存（M3「无值允许多个」）；
 *   P7 跨租户全局唯一：不同 tenant_id 相同 username 被拒（M1 全平台唯一）。
 * 任一失败退出非零；缺 Docker 退出码 3（不得静默跳过）。
 * 用法：node scripts/db/run-db010-verify.mjs
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db010] Docker 不可用：验证不得静默跳过`);

const container = `zszj-db010-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 4620 + Math.floor(Math.random() * 500);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// FLAKY-2 收敛（循 run-cfg002 同款）：容器启动带端口冲突重试 + 就绪探测走 TCP+PGPASSWORD 探「最终 server」
let pgStarted = false;
for (let attempt = 0; attempt < 3 && !pgStarted; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db010', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    pgStarted = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port += 37 + Math.floor(Math.random() * 100);
  }
}
if (!pgStarted) fail(1, '[db010] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) {
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=db010', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[db010] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=db010', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
const migDir = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration';
const migFile = 'V20260920.001__system_users_account_unique.sql';
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[db010] 角色授权失败'); }
  const all = ['V20260909.001__system_infra_baseline.sql', migFile]
    .map((f) => readFileSync(join(root, migDir, f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', all).status !== 0) { cleanup(); fail(1, '[db010] V1 基线 + V20260920.001 迁移执行失败'); }
}
console.log('[db010] 环境就绪：V1 基线 + V20260920.001 账号唯一约束迁移已执行');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const q = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// 插入一名用户；cols 覆盖 mobile/email/tenant_id/deleted 等可选列
const ins = (username, extra = {}) => {
  const cols = ['id', 'username', 'password', 'nickname', 'dept_id', 'status', 'create_time', 'update_time', 'deleted', 'tenant_id'];
  const vals = [`nextval('system_users_seq')`, `'${username}'`, `'x'`, `'u-${username}'`, '100', '0', 'now()', 'now()', `'${extra.deleted ?? 0}'`, `${extra.tenant_id ?? 1}`];
  if (extra.mobile !== undefined) { cols.push('mobile'); vals.push(extra.mobile === null ? 'NULL' : `'${extra.mobile}'`); }
  if (extra.email !== undefined) { cols.push('email'); vals.push(extra.email === null ? 'NULL' : `'${extra.email}'`); }
  return `INSERT INTO system_users (${cols.join(', ')}) VALUES (${vals.join(', ')});`;
};

// codex r1 P2：真并发原语——psqlAsync 以子进程异步跑 SQL（不阻塞事件循环），配合 pg_sleep 制造重叠事务窗口。
// codex r2 P2：① elapsed 在各自进程 close/error 时打点（每进程独立完成计时，B 不再被算上等待 A 的时间）；
//              ② raceInsert 先轮询 pg_stat_activity 确认 A 已进入 pg_sleep（INSERT 已执行、事务未提交、持唯一索引锁）
//                 再起 B——以「A 确在持锁」这一确定性同步点取代固定延时猜测，杜绝伪重叠误判。
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function psqlAsync(sql) {
  const startedAt = Date.now();
  return new Promise((resolve) => {
    const p = spawn('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q']);
    let out = '', err = '';
    p.stdout.on('data', (d) => { out += d; });
    p.stderr.on('data', (d) => { err += d; });
    p.on('close', (code) => resolve({ code, out, err, elapsed: Date.now() - startedAt }));
    p.on('error', (e) => resolve({ code: -1, out, err: err + String(e), elapsed: Date.now() - startedAt }));
    p.stdin.write(sql);
    p.stdin.end();
  });
}
// 轮询直到有后端（排除轮询自身 pid）在 zhongshu 库上执行含 substr 的 active 查询——证明目标会话已进入该语句。
// 用于确认 A 已在 pg_sleep：pg_sleep 紧跟 INSERT 之后、COMMIT 之前，故其 active 即代表 A 已插入且事务未提交（持唯一索引锁）。
async function waitForActiveQuery(substr, timeoutMs = 8000) {
  const deadline = Date.now() + timeoutMs;
  const probe = `SELECT count(*) FROM pg_stat_activity WHERE datname='zhongshu' AND state='active' AND pid <> pg_backend_pid() AND query ILIKE '%${substr}%'`;
  while (Date.now() < deadline) {
    if (Number(q(probe)) > 0) return true;
    await sleep(100);
  }
  return false;
}
// 重叠事务：A 先 BEGIN+INSERT+pg_sleep(holdSec) 持未提交锁；待确认 A 已在 pg_sleep（持锁）后 B 才发同键 INSERT——
// B 的 INSERT 被唯一索引阻塞，直到 A COMMIT 才收 23505（确定性并发重叠，而非串行或固定延时猜测）。
async function raceInsert(username, holdSec = 3) {
  const a = psqlAsync(`BEGIN;\n${ins(username)}\nSELECT pg_sleep(${holdSec});\nCOMMIT;\n`);
  const aHolding = await waitForActiveQuery('pg_sleep');
  const b = psqlAsync(`BEGIN;\n${ins(username)}\nCOMMIT;\n`);
  const [ra, rb] = await Promise.all([a, b]);
  return { a: ra, b: rb, aHolding, holdSec };
}

// P0 复现回归：真并发重叠事务插相同 username → 在册行数 = 1（DB-009 场景不再产生重复）
{
  const { a, b, aHolding } = await raceInsert('p0_dup');
  const live = Number(q("SELECT count(*) FROM system_users WHERE username = 'p0_dup' AND deleted = 0"));
  const oneDup = [a, b].filter((r) => /duplicate key|unique constraint|23505/i.test((r.err ?? '') + (r.out ?? ''))).length;
  record('P0 复现回归：真并发重叠事务同名仅存一行', live === 1 && oneDup === 1 && aHolding, `在册行数=${live} 撞唯一约束方=${oneDup} A持锁同步=${aHolding}（A=${a.code}/${a.elapsed}ms B=${b.code}/${b.elapsed}ms）`);
}

// P1 结构：三唯一索引存在 + idx_01/02/03 等值查询普通索引保留 + idx_04 保留
// codex r1 P2：唯一索引为表达式/部分索引，服务不了 `WHERE col = ?` 等值查询；迁移保留 idx_01/02/03 作查询路径。
{
  const uk = q("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND tablename='system_users' AND indexname IN ('uk_system_users_username','uk_system_users_mobile','uk_system_users_email')");
  const plainKept = q("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN ('idx_system_users_01','idx_system_users_02','idx_system_users_03')");
  const kept = q("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname='idx_system_users_04'");
  record('P1 唯一索引就位/等值查询普通索引保留/dept 索引保留', uk === '3' && plainKept === '3' && kept === '1', `uk=${uk} plain(idx01~03)=${plainKept} idx04=${kept}`);
}

// P2 历史冲突 fail-loudly：存量有重复时重跑迁移 RAISE EXCEPTION（事务隔离，不污染环境）
{
  const migration = readFileSync(join(root, migDir, migFile), 'utf8');
  // 事务内先移除唯一索引模拟「历史库已有重复」，插入大小写归一后冲突的两行，再重跑迁移 DO 块
  const staged = `BEGIN;
DROP INDEX IF EXISTS uk_system_users_username;
DROP INDEX IF EXISTS uk_system_users_mobile;
DROP INDEX IF EXISTS uk_system_users_email;
${ins('P2_Conflict')}
${ins('p2_conflict')}
${migration}
ROLLBACK;`;
  const r = psql('postgres', 'zhongshu', staged);
  const out = (r.stderr ?? '') + (r.stdout ?? '');
  const raised = r.status !== 0 && /ZS-DB-010 历史账号冲突|RAISE|EXCEPTION/i.test(out);
  // 事务已 abort，环境未污染：确认在册无 p2_conflict 残留
  const leaked = Number(q("SELECT count(*) FROM system_users WHERE lower(username) = 'p2_conflict'"));
  record('P2 历史冲突 fail-loudly（禁止擅自删重）', raised && leaked === 0, `raised=${raised} 残留=${leaked}`);
}

// P3 并发兜底：两会话真并发重叠事务插相同 username → 恰一个成功、另一个撞 23505
// codex r2 P2：先确认 A 已持锁（aHolding）再起 B，B 的 elapsed 为其进程独立完成耗时；两者共同证明确有重叠事务。
{
  const { a, b, aHolding } = await raceInsert('p3_race');
  const succ = [a, b].filter((r) => r.code === 0).length;
  const dupErr = [a, b].some((r) => /duplicate key|unique constraint|23505/i.test((r.err ?? '') + (r.out ?? '')));
  const bBlocked = b.elapsed >= 1000; // B 在 A 持锁期间被唯一索引阻塞；>=1s 佐证真重叠（串行插入约 300ms）
  record('P3 并发兜底：重叠事务恰一成功/失败方撞 23505/B 确被阻塞', aHolding && succ === 1 && dupErr && bBlocked, `A持锁同步=${aHolding} 成功数=${succ} 唯一约束报错=${dupErr} B阻塞=${bBlocked}(B耗时=${b.elapsed}ms A耗时=${a.elapsed}ms)`);
}

// P4 大小写规范化：'Admin' 与 'admin'/' ADMIN ' 撞 lower() 表达式索引
{
  const first = psql('postgres', 'zhongshu', ins('P4_Admin'));
  const lower = psql('postgres', 'zhongshu', ins('p4_admin'));
  const padded = psql('postgres', 'zhongshu', ins(' P4_ADMIN '));
  const blocked = lower.status !== 0 && padded.status !== 0;
  record('P4 大小写/空格归一撞表达式唯一索引', first.status === 0 && blocked, `首插=${first.status} 小写撞=${lower.status !== 0} 带空格撞=${padded.status !== 0}`);
}

// P5 删除重建：逻辑删除后同 username 可重建，在册仍唯一
{
  const create = psql('postgres', 'zhongshu', ins('p5_zombie'));
  const del = psql('postgres', 'zhongshu', "UPDATE system_users SET deleted = 1 WHERE username = 'p5_zombie';");
  const rebuild = psql('postgres', 'zhongshu', ins('p5_zombie'));
  const dupLive = psql('postgres', 'zhongshu', ins('p5_zombie'));
  const live = Number(q("SELECT count(*) FROM system_users WHERE username = 'p5_zombie' AND deleted = 0"));
  record('P5 逻辑删除后可重建、在册仍唯一', create.status === 0 && del.status === 0 && rebuild.status === 0 && dupLive.status !== 0 && live === 1, `重建=${rebuild.status} 再插拒=${dupLive.status !== 0} 在册=${live}`);
}

// P6 空值多行：mobile/email 空串或 NULL 多行共存（M3「无值允许多个」）
{
  const e1 = psql('postgres', 'zhongshu', ins('p6_u1', { mobile: '', email: '' }));
  const e2 = psql('postgres', 'zhongshu', ins('p6_u2', { mobile: '', email: '' }));
  const n1 = psql('postgres', 'zhongshu', ins('p6_u3', { mobile: null, email: null }));
  const n2 = psql('postgres', 'zhongshu', ins('p6_u4', { mobile: null, email: null }));
  const allOk = [e1, e2, n1, n2].every((r) => r.status === 0);
  // 反证：真实有值的 mobile 重复仍被拒
  const m1 = psql('postgres', 'zhongshu', ins('p6_m1', { mobile: '13800000001' }));
  const m2 = psql('postgres', 'zhongshu', ins('p6_m2', { mobile: '13800000001' }));
  record('P6 空值多行共存、有值仍唯一', allOk && m1.status === 0 && m2.status !== 0, `空值全通过=${allOk} 有值重复拒=${m2.status !== 0}`);
}

// P7 跨租户全局唯一：不同 tenant_id 相同 username 被拒（M1 全平台唯一）
{
  const t1 = psql('postgres', 'zhongshu', ins('p7_cross', { tenant_id: 1 }));
  const t2 = psql('postgres', 'zhongshu', ins('p7_cross', { tenant_id: 2 }));
  record('P7 跨租户 username 全局唯一', t1.status === 0 && t2.status !== 0, `租户1=${t1.status} 租户2拒=${t2.status !== 0}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
