/**
 * ZS-JOB-003：真实 PG 消费者幂等 Inbox 回归（唯一键抢占并发语义/租户隔离/状态机硬约束——H2 不作数据库放行门禁）。
 *
 * 前置：一次性 PG17 容器 + V1/V2 基线 + V20260915.002 迁移重放（同 run-job002）。
 * 用例（SQL 语句形状对齐 JdbcConsumerInboxPort 的 INSERT/推进/重领；PARAM_CONFLICT 与版本乱序护栏为
 * Java 侧查后判定逻辑，由 H2 用例承载，此处验证 DB 硬兜底层）：
 *   P1 迁移在真实 PG 重放 + 关键列/约束结构断言（uk_inbox_event_key/CHECK status,retry_count/tenant NOT NULL）；
 *   P2 业务事务回滚无记录、提交留存（幂等记录与业务副作用同事务的 SQL 级等价复证）；
 *   P3 并发唯一键抢占：会话 A 事务内插入处理键未提交，会话 B 同键插入阻塞至超时（speculative insertion
 *      wait），不同键立即成功；A 回滚后 B 同键重试成功——同事件并发只产生一次登记的 DB 兜底；
 *   P4 租户隔离：同消费者同事件键跨租户各自行成功（唯一键含租户）；
 *   P5 状态推进形状：PROCESSING→COMPLETED/FAILED 条件推进与 FAILED→PROCESSING 重领（WHERE 状态前置，
 *      双重推进归零）；retry_count 由 fail 递增、重领不重复递增；
 *   P6 硬约束：status CHECK 拒非法值、retry_count 负值拒绝、tenant_id NOT NULL 拒无归属写入。
 * 任一失败退出非零。用法：node scripts/db/run-job003-verify.mjs
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[job003] Docker 不可用：验证不得静默跳过`);

const container = `zszj-job003-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// FLAKY-2 收敛（循 run-db007 同款）：容器启动带端口冲突重试 + 就绪探测/建库走 TCP+PGPASSWORD 探「最终 server」
// socket 探测可命中 init 临时 server（entrypoint 随后关闭它），导致建库/迁移落到临时库失败
let pgStarted = false;
for (let attempt = 0; attempt < 3 && !pgStarted; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=job003', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    pgStarted = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port += 37 + Math.floor(Math.random() * 100);
  }
}
if (!pgStarted) fail(1, '[job003] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) {
  // TCP+PGPASSWORD 探「最终 server」（FLAKY-2：socket 会命中 init 临时 server）
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=job003', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[job003] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=job003', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[job003] 角色授权失败'); }
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  const vJob2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.001__infra_outbox_event.sql'), 'utf8');
  const vJob3 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.002__infra_inbox_event.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2 + '\n' + vJob2 + '\n' + vJob3).status !== 0) { cleanup(); fail(1, '[job003] V1/V2/V20260915.001~002 执行失败（迁移重放不通过）'); }
}
console.log('[job003] 环境就绪（含 ZS-JOB-002/003 迁移重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
const oneIds = (sql) => one(sql).split('\n').filter(line => /^\d+$/.test(line));
// 等待后台持锁会话进入 pg_sleep（其 INSERT 已完成、唯一键 speculative wait 已建立）/退出
const waitAState = async (expectActive) => {
  for (let i = 0; i < 60; i++) {
    const n = one(`SELECT count(*) FROM pg_stat_activity WHERE query LIKE '%pg_sleep(8)%' AND state = 'active' AND pid <> pg_backend_pid()`);
    if (Number(n) === (expectActive ? 1 : 0)) return true;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 250);
  }
  return false;
};
const insertInbox = (tenant, consumer, eventKey, hash) =>
  `INSERT INTO inbox_event (consumer, event_key, payload_hash, biz_type, biz_id, biz_version, tenant_id, trace_id) `
  + `VALUES ('${consumer}', '${eventKey}', '${hash}', 'demo_order', '2048', '1', ${tenant}, 'trace-pg');`;
const insertReturning = (tenant, consumer, eventKey, hash) =>
  insertInbox(tenant, consumer, eventKey, hash).replace(/;$/, '') + ' RETURNING id';

// P1 迁移结构断言
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'inbox_event'
AND column_name IN ('id','consumer','event_key','payload_hash','status','result','retry_count','biz_type','biz_id','biz_version','last_error','tenant_id','trace_id','create_time','complete_time')`);
  const uk = one(`SELECT count(*) FROM information_schema.table_constraints WHERE table_name = 'inbox_event' AND constraint_type = 'UNIQUE' AND constraint_name = 'uk_inbox_event_key'`);
  const checks = one(`SELECT count(*) FROM information_schema.table_constraints WHERE table_name = 'inbox_event' AND constraint_type = 'CHECK'`);
  const tenantNullable = one(`SELECT is_nullable FROM information_schema.columns WHERE table_name = 'inbox_event' AND column_name = 'tenant_id'`);
  record('P1 迁移重放 + 结构断言（15 列/唯一键/CHECK×2/tenant 非空）',
    cols === '15' && uk === '1' && Number(checks) >= 2 && tenantNullable === 'NO',
    `columns=${cols} uk=${uk} checks=${checks} tenant_nullable=${tenantNullable}`);
}

// P2 业务回滚无记录 / 提交留存
{
  one('TRUNCATE inbox_event');
  const r1 = psql('postgres', 'zhongshu', `BEGIN;\n${insertInbox(1, 'ut-consumer', 'p2-rollback', 'h1')}\nROLLBACK;`);
  const rolled = one(`SELECT count(*) FROM inbox_event WHERE event_key = 'p2-rollback'`);
  const r2 = psql('postgres', 'zhongshu', `BEGIN;\n${insertInbox(1, 'ut-consumer', 'p2-commit', 'h1')}\nCOMMIT;`);
  const committed = one(`SELECT count(*) FROM inbox_event WHERE event_key = 'p2-commit'`);
  record('P2 业务回滚无记录/提交留存', r1.status === 0 && r2.status === 0 && rolled === '0' && committed === '1',
    `rollback=${rolled} commit=${committed}`);
}

// P3 并发唯一键抢占：A 事务内插键未提交，B 同键插入阻塞至超时，不同键成功；A 回滚后同键可插入
{
  one('TRUNCATE inbox_event');
  psql('postgres', 'zhongshu', insertInbox(1, 'ut-consumer', 'p3-other', 'h1'));
  const holdSql = `BEGIN; ${insertInbox(1, 'ut-consumer', 'p3-contested', 'h1')} SELECT pg_sleep(8); ROLLBACK;`;
  const a = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-c', holdSql], { stdio: 'ignore' });
  const aLocked = await waitAState(true);

  // 会话 B：同键插入（speculative insertion wait）→ lock_timeout 2s 超时；不同键立即成功
  const bSame = aLocked
    ? psql('postgres', 'zhongshu', `SET lock_timeout = '2s'; ${insertInbox(1, 'ut-consumer', 'p3-contested', 'h1')}`)
    : { status: -1 };
  const bOther = oneIds(`INSERT INTO inbox_event (consumer, event_key, payload_hash, tenant_id) VALUES ('ut-consumer', 'p3-free', 'h1', 1) RETURNING id`);
  const sameBlocked = bSame.status !== 0 && /timeout|waiting|canceling/.test((bSame.stderr ?? '') + (bSame.stdout ?? ''));
  record('P3a 同键插入被 A 阻塞至超时、异键不受影响', sameBlocked && bOther.length === 1,
    `A持锁=${aLocked} 同键exit=${bSame.status} 异键登记=${bOther.length}`);

  // A 回滚释放后：同键可插入（崩溃/回滚不留半态，重放可重新处理）
  const aGone = await waitAState(false);
  const retry = aGone ? oneIds(insertReturning(1, 'ut-consumer', 'p3-contested', 'h1')) : [];
  record('P3b A 回滚后同键可登记（重放可重新处理）', retry.length === 1, `A释放=${aGone} 重放登记=${retry.length}`);
  void a;
}

// P4 租户隔离：同消费者同事件键跨租户各自成功
{
  one('TRUNCATE inbox_event');
  const t1 = oneIds(insertReturning(1, 'ut-consumer', 'p4-same-key', 'h1'));
  const t2 = oneIds(insertReturning(2, 'ut-consumer', 'p4-same-key', 'h1'));
  record('P4 跨租户同业务号互不冲突（唯一键含租户）', t1.length === 1 && t2.length === 1,
    `tenant1=${t1.length} tenant2=${t2.length}`);
}

// P5 状态推进形状：条件推进 + FAILED 重领（retry_count 由 fail 递增、重领不重复递增）
{
  one('TRUNCATE inbox_event');
  one(insertInbox(1, 'ut-consumer', 'p5-state', 'h1'));
  const id = one(`SELECT id FROM inbox_event WHERE event_key = 'p5-state'`);
  const f1 = oneIds(`UPDATE inbox_event SET status = 'FAILED', last_error = '{"errorClass":"IllegalStateException","messageLength":9}', retry_count = retry_count + 1 WHERE id = ${id} AND status = 'PROCESSING' RETURNING id`);
  const f2 = oneIds(`UPDATE inbox_event SET status = 'FAILED', retry_count = retry_count + 1 WHERE id = ${id} AND status = 'PROCESSING' RETURNING id`);
  const retry = oneIds(`UPDATE inbox_event SET status = 'PROCESSING', last_error = NULL WHERE id = ${id} AND status = 'FAILED' RETURNING id`);
  const retryCount = one(`SELECT retry_count FROM inbox_event WHERE id = ${id}`);
  const done = oneIds(`UPDATE inbox_event SET status = 'COMPLETED', complete_time = now() WHERE id = ${id} AND status = 'PROCESSING' RETURNING id`);
  const doneAgain = oneIds(`UPDATE inbox_event SET status = 'COMPLETED' WHERE id = ${id} AND status = 'PROCESSING' RETURNING id`);
  record('P5 状态条件推进/失败重领/retry_count 单调', f1.length === 1 && f2.length === 0 && retry.length === 1
    && retryCount === '1' && done.length === 1 && doneAgain.length === 0,
    `首次失败=${f1.length} 二次失败=${f2.length} 重领=${retry.length} 计数=${retryCount} 完成=${done.length} 重复完成=${doneAgain.length}`);
}

// P6 硬约束
{
  const checkRejected = psql('postgres', 'zhongshu',
    `INSERT INTO inbox_event (consumer, event_key, payload_hash, status, tenant_id) VALUES ('ut', 'p6-check', 'h', 'BOGUS', 1);`);
  const retryRejected = psql('postgres', 'zhongshu',
    `INSERT INTO inbox_event (consumer, event_key, payload_hash, retry_count, tenant_id) VALUES ('ut', 'p6-retry', 'h', -1, 1);`);
  const tenantRejected = psql('postgres', 'zhongshu',
    `INSERT INTO inbox_event (consumer, event_key, payload_hash) VALUES ('ut', 'p6-tenant', 'h');`);
  record('P6 CHECK 拒非法状态/负计数 / tenant_id 非空硬约束',
    checkRejected.status !== 0 && retryRejected.status !== 0 && tenantRejected.status !== 0,
    `check_exit=${checkRejected.status} retry_exit=${retryRejected.status} tenant_exit=${tenantRejected.status}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
