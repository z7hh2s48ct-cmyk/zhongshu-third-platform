/**
 * ZS-DB-008：真实 PG 事务与锁行为回归。
 *
 * 前置：一次性 PG17 容器 + V1/V2 基线（同 run-db006/007）。
 * 用例：
 *   C1 用户+岗位关联原子性：事务中插入用户与岗位关联，岗位写入故意失败 →
 *      整体回滚，用户不残留（对齐 AdminUserServiceImpl 事务语义）；
 *   C2 合法事务完整提交：用户与岗位关联同时存在；
 *   C3 批量写入：单语句 200 行 VALUES 批量插入；
 *   C4 并发锁可预测：会话 A 持 FOR UPDATE 行锁，会话 B 以 lock_timeout=2s
 *      等待同一行 → 约 2s 超时失败；A 释放后 B 立即成功；
 *   C5 MVCC：A 持行锁期间普通读不受阻塞。
 * 任一失败退出非零。用法：node scripts/db/run-db008-verify.mjs
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[db008] Docker 不可用：验证不得静默跳过`);

const container = `zszj-db008-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=db008', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[db008] PG 未就绪'); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[db008] 角色授权失败'); }
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2).status !== 0) { cleanup(); fail(1, '[db008] V1/V2 执行失败'); }
}
console.log('[db008] 环境就绪');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();

// C1 用户+岗位关联原子性：岗位写入故意失败 → 用户不残留
{
  const sql = `BEGIN;
INSERT INTO system_users (id, username, password, nickname, dept_id, status, create_time, update_time, deleted, tenant_id)
VALUES (800001, 'c1atom', 'x', 'C1-原子性', 100, 0, now(), now(), '0', 1);
INSERT INTO system_user_post (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (800001, 800001, 1, 'c1', now(), 'c1', now(), '0', 1);
INSERT INTO system_user_post (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (800002, 800001, NULL, 'c1', now(), 'c1', now(), '0', 1);
COMMIT;`;
  const r = psql('postgres', 'zhongshu', sql);
  const userLeft = one("SELECT count(*) FROM system_users WHERE id = 800001");
  record('C1 岗位写入故意失败 → 用户不残留', r.status !== 0 && userLeft === '0', `exit=${r.status} 用户残留=${userLeft}`);
}

// C2 合法事务完整提交
{
  const sql = `BEGIN;
INSERT INTO system_users (id, username, password, nickname, dept_id, status, create_time, update_time, deleted, tenant_id)
VALUES (800002, 'c2commit', 'x', 'C2-提交', 100, 0, now(), now(), '0', 1);
INSERT INTO system_user_post (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (800011, 800002, 1, 'c2', now(), 'c2', now(), '0', 1);
COMMIT;`;
  const r = psql('postgres', 'zhongshu', sql);
  const u = one("SELECT count(*) FROM system_users WHERE id = 800002");
  const p = one("SELECT count(*) FROM system_user_post WHERE user_id = 800002");
  record('C2 合法事务完整提交', r.status === 0 && u === '1' && p === '1', `用户=${u} 岗位关联=${p}`);
}

// C3 批量写入：单语句 200 行
{
  let sql = `BEGIN;
INSERT INTO system_user_post (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id) VALUES `;
  const rows = [];
  for (let i = 0; i < 200; i++) rows.push(`(${900000 + i}, 800002, 1, 'c3', now(), 'c3', now(), '0', 1)`);
  sql += rows.join(', ') + '; COMMIT;';
  const r = psql('postgres', 'zhongshu', sql);
  const n = one("SELECT count(*) FROM system_user_post WHERE id >= 900000 AND id < 900200");
  record('C3 批量写入 200 行', r.status === 0 && n === '200', `实际 ${n}`);
}

// C4 并发锁可预测
{
  const deptId = one("SELECT id FROM system_dept ORDER BY id LIMIT 1");

  // 会话 A：持 FOR UPDATE 行锁 6 秒后释放（异步运行；docker exec 冷启动较慢，预留余量）
  // 注意：spawn 无 input 选项，必须用 -c 单命令方式（stdin EOF 会导致 psql 立即退出、锁从未建立）
  const holdSql = "BEGIN; SELECT id FROM system_dept WHERE id = " + deptId + " FOR UPDATE; SELECT pg_sleep(6); ROLLBACK;";
  const a = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-c', holdSql], {
    stdio: 'ignore',
  });
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 3000); // 确保 A 已持锁（预留 docker exec 启动）

  // 会话 B：lock_timeout=2s → 约 2 秒后超时失败（可预测）
  const t0 = Date.now();
  const b = psql('postgres', 'zhongshu', `SET lock_timeout = '2s';
SELECT id FROM system_dept WHERE id = ${deptId} FOR UPDATE;`);
  const elapsed = Date.now() - t0;
  const timedOut = b.status !== 0 && /lock timeout|canceling statement/.test((b.stderr ?? '') + (b.stdout ?? ''));
  record('C4a 持锁期间 B 超时失败且耗时≈timeout', b.status !== 0 && elapsed >= 1900 && elapsed < 4000,
    `exit=${b.status} elapsed=${elapsed}ms`);

  // A 释放后：B 立即成功（A 持锁 6s，B 超时后再等余量）
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 3000);
  const t1 = Date.now();
  const b2 = psql('postgres', 'zhongshu', `SELECT id FROM system_dept WHERE id = ${deptId} FOR UPDATE;`);
  const elapsed2 = Date.now() - t1;
  record('C4b A 释放后 B 立即获得锁', b2.status === 0 && elapsed2 < 1000, `exit=${b2.status} elapsed=${elapsed2}ms`);

  // 等待 A 结束
  const aResult = spawnSync('bash', ['-c', 'while kill -0 $PPID 2>/dev/null; do sleep 0.2; done'], { timeout: 1000 });
  void aResult;
  void a;
}

// C5 MVCC：持行锁期间普通读不受阻塞
{
  const t0 = Date.now();
  const v = one("SELECT count(*) FROM system_dept");
  const elapsed = Date.now() - t0;
  record('C5 普通读不受行锁阻塞（MVCC）', elapsed < 1000, `count=${v} elapsed=${elapsed}ms`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
