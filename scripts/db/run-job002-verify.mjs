/**
 * ZS-JOB-002：真实 PG 事务 Outbox 行为回归（双实例领取/租约/栅栏/DEAD——H2 不作锁语义放行门禁）。
 *
 * 前置：一次性 PG17 容器 + V1/V2 基线 + V20260915.001 迁移重放（同 run-db008）。
 * 用例（SQL 语句形状对齐 OutboxDispatcherService 的 CLAIM_SELECT/CLAIM_MARK/COMPLETE/FAIL，凭证以字面量模拟
 * Java 侧每次领取签发的 UUID；每用例 TRUNCATE 隔离候选池——无租约的遗留行 id 更小会抢占 ORDER BY id
 * LIMIT 名额，污染断言）：
 *   P1 迁移在真实 PG 重放 + 关键列/约束结构断言（claim_token/status CHECK/tenant_id NOT NULL）；
 *   P2 业务事务回滚无事件、提交必有事件（outbox 与业务同事务的 SQL 级等价复证）；
 *   P3 双实例领取不重复：会话 A 持 2 行行锁未提交，会话 B SKIP LOCKED 只领到其余 2 行；A 释放后其 2 行
 *      可再领且与 B 不相交（进程崩溃可恢复 + 不重复投递）；
 *   P4 租约过期回收：claim_expires_at 过期后事件可被重领；
 *   P5 栅栏：租约过期重领后旧凭证 complete 归零、新凭证确认成功（旧领取不能覆盖新状态）；
 *   P6 失败退避与 DEAD：fail 语句 5 次转 DEAD，DEAD 不再进入候选；
 *   P7/P8 硬约束：status CHECK 拒非法值、tenant_id NOT NULL 拒无归属写入。
 * 任一失败退出非零。用法：node scripts/db/run-job002-verify.mjs
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[job002] Docker 不可用：验证不得静默跳过`);

const container = `zszj-job002-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=job002', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[job002] PG 未就绪'); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[job002] 角色授权失败'); }
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  const vJob = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260915.001__infra_outbox_event.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2 + '\n' + vJob).status !== 0) { cleanup(); fail(1, '[job002] V1/V2/V20260915.001 执行失败（迁移重放不通过）'); }
}
console.log('[job002] 环境就绪（含 ZS-JOB-002 迁移重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// RETURNING 行 id 解析：psql -At 对 UPDATE 语句除返回行外还输出「UPDATE n」命令标签行，须按纯数字行过滤
const oneIds = (sql) => one(sql).split('\n').filter(line => /^\d+$/.test(line));
// 等待后台持锁会话进入 pg_sleep（其领取 UPDATE 已完成、行锁已持有）/退出（事务结束、锁已释放）；
// 排除自身 pid（本探针查询文本同样含 pg_sleep 字样，会自匹配）
const waitAState = async (expectActive) => {
  for (let i = 0; i < 60; i++) {
    const n = one(`SELECT count(*) FROM pg_stat_activity WHERE query LIKE '%pg_sleep(8)%' AND state = 'active' AND pid <> pg_backend_pid()`);
    if (Number(n) === (expectActive ? 1 : 0)) return true;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 250);
  }
  return false;
};

// 领取候选/标记语句形状（对齐 OutboxDispatcherService.claim 的 SELECT ... FOR UPDATE SKIP LOCKED + 同事务标记）
const claimSelect = (limit, token) => `UPDATE outbox_event SET claimed_by = 'stmt@job002', claim_token = '${token}', claim_expires_at = now() + interval '30 seconds'
WHERE id IN (SELECT id FROM outbox_event WHERE status = 'PENDING' AND next_retry_at <= now()
  AND (claim_expires_at IS NULL OR claim_expires_at < now()) ORDER BY id LIMIT ${limit} FOR UPDATE SKIP LOCKED) RETURNING id`;
const insertPending = (eventType, n) => Array.from({ length: n }, (_, i) =>
  `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type, next_retry_at)
VALUES ('${eventType}', 'infra_file', 'biz-${eventType}-${i}', '{}', 1, 'SYSTEM', now() - interval '1 second');`).join('\n');

// P1 迁移结构断言
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'outbox_event'
AND column_name IN ('id','event_type','biz_type','biz_id','biz_version','payload','headers','status','retry_count','next_retry_at','claimed_by','claim_token','claim_expires_at','last_error','dispatched_at','tenant_id','actor_type','actor_id','trace_id','create_time')`);
  const tenantNullable = one(`SELECT is_nullable FROM information_schema.columns WHERE table_name = 'outbox_event' AND column_name = 'tenant_id'`);
  const heartbeatNullable = one(`SELECT is_nullable FROM information_schema.columns WHERE table_name = 'dispatcher_lease' AND column_name = 'heartbeat_at'`);
  const leaseUk = one(`SELECT count(*) FROM information_schema.table_constraints WHERE table_name = 'dispatcher_lease' AND constraint_type = 'UNIQUE'`);
  record('P1 迁移重放 + 结构断言（20 列/tenant 非空/租约唯一约束）',
    cols === '20' && tenantNullable === 'NO' && heartbeatNullable === 'NO' && Number(leaseUk) >= 1,
    `columns=${cols} tenant_nullable=${tenantNullable} lease_uk=${leaseUk}`);
}

// P2 业务回滚无事件 / 提交必有事件
{
  one('TRUNCATE outbox_event');
  const r1 = psql('postgres', 'zhongshu', `BEGIN;\n${insertPending('P2_ROLLBACK', 1)}\nROLLBACK;`);
  const rolled = one(`SELECT count(*) FROM outbox_event WHERE event_type = 'P2_ROLLBACK'`);
  const r2 = psql('postgres', 'zhongshu', `BEGIN;\n${insertPending('P2_COMMIT', 1)}\nCOMMIT;`);
  const committed = one(`SELECT count(*) FROM outbox_event WHERE event_type = 'P2_COMMIT'`);
  record('P2 业务回滚无事件/提交必有事件', r1.status === 0 && r2.status === 0 && rolled === '0' && committed === '1',
    `rollback=${rolled} commit=${committed}`);
}

// P3 双实例领取不重复：A 持 2 行行锁未提交，B SKIP LOCKED 只得其余 2 行
{
  one('TRUNCATE outbox_event');
  psql('postgres', 'zhongshu', insertPending('P3_DUAL', 4));
  // 会话 A：领取 2 行并持锁 8 秒（未提交，行锁保持；以 pg_stat_activity 探针确定 A 已持锁/已释放）
  const holdSql = `BEGIN; ${claimSelect(2, 'a-token')} ; SELECT pg_sleep(8); ROLLBACK;`;
  const a = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-c', holdSql], { stdio: 'ignore' });
  const aLocked = await waitAState(true);

  // 会话 B：领取 4 行 → 只能领到未被 A 锁定的 2 行
  const bIds = aLocked ? oneIds(claimSelect(4, 'b-token')) : [];
  record('P3 双实例 SKIP LOCKED 领取不重复', bIds.length === 2, `A持锁=${aLocked} B 领取=${bIds.length} 行`);

  // A 释放后：A 持有的 2 行可再领，且与 B 已租约的 2 行不相交
  const aGone = await waitAState(false);
  const cIds = aGone ? oneIds(claimSelect(4, 'c-token')) : [];
  const disjoint = cIds.every(id => !bIds.includes(id));
  record('P3b A 释放后其 2 行可再领且与 B 不相交', cIds.length === 2 && disjoint, `A释放=${aGone} C 领取=${cIds.length} 不相交=${disjoint}`);
  void a;
}

// P4 租约过期回收（进程崩溃可恢复）
{
  one('TRUNCATE outbox_event');
  psql('postgres', 'zhongshu', insertPending('P4_LEASE', 1));
  one(claimSelect(1, 'p4-token'));
  const before = one(`SELECT count(*) FROM outbox_event WHERE event_type = 'P4_LEASE'
AND (claim_expires_at IS NULL OR claim_expires_at >= now())`);
  psql('postgres', 'zhongshu', `UPDATE outbox_event SET claim_expires_at = now() - interval '1 second' WHERE event_type = 'P4_LEASE';`);
  one(claimSelect(1, 'p4-reclaim'));
  const reclaimed = one(`SELECT claim_token FROM outbox_event WHERE event_type = 'P4_LEASE'`);
  record('P4 租约过期后可重领（崩溃恢复）', before === '1' && reclaimed === 'p4-reclaim', `重领凭证=${reclaimed}`);
}

// P5 栅栏：旧凭证不能确认新领取
{
  one('TRUNCATE outbox_event');
  psql('postgres', 'zhongshu', insertPending('P5_FENCE', 1));
  one(claimSelect(1, 'p5-stale'));
  psql('postgres', 'zhongshu', `UPDATE outbox_event SET claim_expires_at = now() - interval '1 second' WHERE event_type = 'P5_FENCE';`);
  one(claimSelect(1, 'p5-fresh'));
  const staleIds = oneIds(`UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = now()
WHERE status = 'PENDING' AND claim_token = 'p5-stale' RETURNING id`);
  const freshIds = oneIds(`UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = now()
WHERE status = 'PENDING' AND claim_token = 'p5-fresh' RETURNING id`);
  const finalStatus = one(`SELECT status FROM outbox_event WHERE event_type = 'P5_FENCE'`);
  record('P5 旧凭证不能确认新领取（栅栏）', staleIds.length === 0 && freshIds.length === 1 && finalStatus === 'DISPATCHED',
    `旧凭证确认=${staleIds.length} 行 新凭证确认=${freshIds.length} 行 终态=${finalStatus}`);
}

// P6 失败退避与 DEAD
{
  one('TRUNCATE outbox_event');
  psql('postgres', 'zhongshu', insertPending('P6_DEAD', 1));
  for (let round = 1; round <= 5; round++) {
    one(claimSelect(1, `p6-token-${round}`));
    one(`UPDATE outbox_event SET retry_count = retry_count + 1, last_error = 'P6-模拟失败', next_retry_at = now() - interval '1 second',
status = CASE WHEN retry_count + 1 >= 5 THEN 'DEAD' ELSE status END,
claimed_by = NULL, claim_token = NULL, claim_expires_at = NULL
WHERE status = 'PENDING' AND claim_token = 'p6-token-${round}'`);
  }
  const status = one(`SELECT status FROM outbox_event WHERE event_type = 'P6_DEAD'`);
  const deadClaimable = one(`SELECT count(*) FROM outbox_event WHERE status = 'PENDING' AND event_type = 'P6_DEAD'`);
  record('P6 失败 5 次转 DEAD 且不再进入候选', status === 'DEAD' && deadClaimable === '0', `终态=${status}`);
}

// P7/P8 硬约束
{
  const checkRejected = psql('postgres', 'zhongshu',
    `INSERT INTO outbox_event (event_type, payload, status, tenant_id, actor_type) VALUES ('P7_CHECK', '{}', 'BOGUS', 1, 'SYSTEM');`);
  const tenantRejected = psql('postgres', 'zhongshu',
    `INSERT INTO outbox_event (event_type, payload, actor_type) VALUES ('P8_TENANT', '{}', 'SYSTEM');`);
  record('P7/P8 CHECK 拒非法状态 / tenant_id 非空硬约束', checkRejected.status !== 0 && tenantRejected.status !== 0,
    `check_exit=${checkRejected.status} tenant_exit=${tenantRejected.status}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
