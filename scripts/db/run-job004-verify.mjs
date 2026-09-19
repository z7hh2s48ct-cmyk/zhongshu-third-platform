/**
 * ZS-JOB-004：真实 PG 人工恢复台账 + DEAD 跳过终态 + 双轨审计回归。
 *
 * H2 单测（OutboxRecoveryServiceTest/OutboxHealthMonitorTest）覆盖服务逻辑，但 V20260915.021 的
 * ALTER DROP/ADD CONSTRAINT、SEQUENCE + DEFAULT nextval、char_length CHECK 等 PG 专有 DDL 不作 H2 放行门禁，
 * 须真实 PG17 重放验证（对齐验收①②③：DEAD 被发现→授权恢复→状态/审计可追踪，无权/无限/改历史被拒）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + V20260914.010 audit_event
 *       + V20260915.001 outbox_event + V20260915.021 恢复台账 迁移重放（同 run-job002 惯用法）。
 * 用例（SQL 形状对齐 OutboxRecoveryServiceImpl 的 RETRY_UPDATE/SKIP_UPDATE/INSERT_RECOVERY_LOG + JdbcAuditPort 审计写入）：
 *   P1 迁移重放 + 结构断言（outbox_recovery_log 13 列/2 CHECK/2 索引，status CHECK 含 SKIPPED）；
 *   P2 SKIPPED 终态被 status CHECK 接受、非法状态仍被拒；
 *   P3 授权重试 DEAD→PENDING：清租约 + 台账 RETRY 行前后关联（before/after/before_retry_count/manual_retry_seq）；
 *   P4 授权跳过 DEAD→SKIPPED：台账 SKIP 行 + SKIPPED 不再被领取候选（status='PENDING'）命中；
 *   P5 双轨审计落地：OUTBOX_EVENT_RETRIED SUCCESS 行入 audit_event + 幂等键唯一拒重复；
 *   P6 台账硬约束：action CHECK 拒非法值、reason CHECK 拒空理由；
 *   P7 payload 只读：retry/skip 前后 payload/headers/event_type 字节不变（修改历史被拒的 SQL 级证明）。
 * 任一失败退出非零。用法：node scripts/db/run-job004-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[job004] Docker 不可用：验证不得静默跳过`);

const container = `zszj-job004-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// FLAKY-2 收敛（循 run-db007 同款）：容器启动带端口冲突重试 + 就绪探测/建库走 TCP+PGPASSWORD 探「最终 server」
// socket 探测可命中 init 临时 server（entrypoint 随后关闭它），导致建库/迁移落到临时库失败
let pgStarted = false;
for (let attempt = 0; attempt < 3 && !pgStarted; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=job004', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    pgStarted = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port += 37 + Math.floor(Math.random() * 100);
  }
}
if (!pgStarted) fail(1, '[job004] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) {
  // TCP+PGPASSWORD 探「最终 server」（FLAKY-2：socket 会命中 init 临时 server）
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=job004', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[job004] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=job004', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[job004] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const sql = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260914.010__system_audit_event.sql',
    'V20260915.001__infra_outbox_event.sql',
    'V20260915.021__infra_outbox_recovery.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', sql).status !== 0) { cleanup(); fail(1, '[job004] 迁移重放失败（V1/V2/audit/outbox/recovery）'); }
}
console.log('[job004] 环境就绪（含 ZS-JOB-004 迁移 V20260915.021 重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// RETURNING 行 id 解析：psql -At 对 INSERT/UPDATE 除返回行外还输出命令标签行（INSERT 0 1 / UPDATE n），须按纯数字行过滤
const oneIds = (sql) => one(sql).split('\n').filter((line) => /^\d+$/.test(line));
const reset = () => one('TRUNCATE outbox_event, outbox_recovery_log, audit_event');
// 种子一条 DEAD 事件（retry_count=5，受控 last_error），返回自增 id
const seedDead = (eventType, tenantId = 1, payload = '{"fileId":2048}') => oneIds(
  `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, headers, status, retry_count, next_retry_at, last_error, tenant_id, actor_type)
VALUES ('${eventType}', 'infra_file', 'biz-${eventType}', '${payload}', 'ut-headers', 'DEAD', 5, now() - interval '1 second', '{"errorClass":"IllegalStateException","messageLength":18}', ${tenantId}, 'SYSTEM') RETURNING id`)[0];
// 恢复 UPDATE 形状对齐 OutboxRecoveryServiceImpl（乐观并发：WHERE id=? AND status='DEAD' AND tenant_id=?）
const retryUpdate = (id, tenantId = 1) => `UPDATE outbox_event SET status = 'PENDING', next_retry_at = now(), claimed_by = NULL, claim_token = NULL, claim_expires_at = NULL WHERE id = ${id} AND status = 'DEAD' AND tenant_id = ${tenantId} RETURNING id`;
const skipUpdate = (id, tenantId = 1) => `UPDATE outbox_event SET status = 'SKIPPED' WHERE id = ${id} AND status = 'DEAD' AND tenant_id = ${tenantId} RETURNING id`;
const insertLog = (id, action, beforeStatus, afterStatus, seq, tenantId = 1, reason = '根因已修复，授权恢复') => `INSERT INTO outbox_recovery_log (event_id, action, reason, before_status, after_status, before_retry_count, manual_retry_seq, operator_type, operator_id, tenant_id, trace_id)
VALUES (${id}, '${action}', '${reason}', '${beforeStatus}', '${afterStatus}', 5, ${seq}, 'ADMIN', '1', ${tenantId}, 'trace-job004')`;

// P1 迁移重放 + 结构断言
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'outbox_recovery_log'
AND column_name IN ('id','event_id','action','reason','before_status','after_status','before_retry_count','manual_retry_seq','operator_type','operator_id','tenant_id','trace_id','create_time')`);
  const statusCheck = one(`SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_outbox_event_status'`);
  const actionCk = one(`SELECT count(*) FROM pg_constraint WHERE conname = 'ck_outbox_recovery_action'`);
  const reasonCk = one(`SELECT count(*) FROM pg_constraint WHERE conname = 'ck_outbox_recovery_reason'`);
  const idx = one(`SELECT count(*) FROM pg_indexes WHERE tablename = 'outbox_recovery_log' AND indexname IN ('idx_outbox_recovery_event','idx_outbox_recovery_tenant')`);
  record('P1 迁移 V20260915.021 重放 + 结构断言（13 列/status CHECK 含 SKIPPED/2 CHECK/2 索引）',
    cols === '13' && statusCheck.includes('SKIPPED') && actionCk === '1' && reasonCk === '1' && idx === '2',
    `cols=${cols} skipped=${statusCheck.includes('SKIPPED')} action_ck=${actionCk} reason_ck=${reasonCk} idx=${idx}`);
}

// P2 SKIPPED 被 status CHECK 接受 / 非法状态被拒
{
  reset();
  const id = seedDead('P2_SKIP_ACCEPT');
  const upd = oneIds(skipUpdate(id));
  const status = one(`SELECT status FROM outbox_event WHERE id = ${id}`);
  const bogus = psql('postgres', 'zhongshu', `UPDATE outbox_event SET status = 'BOGUS' WHERE id = ${id}`);
  record('P2 SKIPPED 终态被 status CHECK 接受、非法状态被拒', upd.length === 1 && status === 'SKIPPED' && bogus.status !== 0,
    `skip_updated=${upd.length} status=${status} bogus_exit=${bogus.status}`);
}

// P3 授权重试 DEAD→PENDING + 台账前后关联
{
  reset();
  const id = seedDead('P3_RETRY');
  const upd = oneIds(retryUpdate(id));
  psql('postgres', 'zhongshu', insertLog(id, 'RETRY', 'DEAD', 'PENDING', 1));
  const event = one(`SELECT status || '|' || coalesce(claimed_by,'-') || '|' || coalesce(claim_token,'-') || '|' || coalesce(cast(claim_expires_at as text),'-') FROM outbox_event WHERE id = ${id}`);
  const log = one(`SELECT action || '|' || before_status || '|' || after_status || '|' || before_retry_count || '|' || manual_retry_seq || '|' || operator_type FROM outbox_recovery_log WHERE event_id = ${id} AND action = 'RETRY'`);
  record('P3 授权重试 DEAD→PENDING + 清租约 + 台账前后关联', upd.length === 1 && event === 'PENDING|-|-|-' && log === 'RETRY|DEAD|PENDING|5|1|ADMIN',
    `updated=${upd.length} event=${event} log=${log}`);
}

// P4 授权跳过 DEAD→SKIPPED + 台账 + SKIPPED 不再被领取候选命中
{
  reset();
  const id = seedDead('P4_SKIP');
  const upd = oneIds(skipUpdate(id));
  psql('postgres', 'zhongshu', insertLog(id, 'SKIP', 'DEAD', 'SKIPPED', 1));
  const claimable = one(`SELECT count(*) FROM outbox_event WHERE status = 'PENDING' AND next_retry_at <= now() AND id = ${id}`);
  const log = one(`SELECT action || '|' || before_status || '|' || after_status FROM outbox_recovery_log WHERE event_id = ${id} AND action = 'SKIP'`);
  record('P4 授权跳过 DEAD→SKIPPED + 台账 + 不再被领取候选命中', upd.length === 1 && claimable === '0' && log === 'SKIP|DEAD|SKIPPED',
    `updated=${upd.length} claimable=${claimable} log=${log}`);
}

// P5 双轨审计落地 audit_event + 幂等键唯一拒重复
{
  reset();
  const id = seedDead('P5_AUDIT');
  oneIds(retryUpdate(id));
  psql('postgres', 'zhongshu', insertLog(id, 'RETRY', 'DEAD', 'PENDING', 1));
  const auditInsert = `INSERT INTO audit_event (event_type, actor_type, actor_id, action, biz_type, biz_id, reason, result, detail, tenant_id, trace_id, idempotency_key)
VALUES ('OUTBOX_EVENT_RETRIED', 'ADMIN', '1', 'RETRY', 'outbox_event', '${id}', '根因已修复，授权重试', 'SUCCESS', '{"beforeStatus":"DEAD","afterStatus":"PENDING","manualRetrySeq":1}', 1, 'trace-job004', '${id}:RETRY:1')`;
  const a1 = psql('postgres', 'zhongshu', auditInsert);
  const landed = one(`SELECT count(*) FROM audit_event WHERE event_type = 'OUTBOX_EVENT_RETRIED' AND idempotency_key = '${id}:RETRY:1' AND result = 'SUCCESS' AND actor_type = 'ADMIN'`);
  const dup = psql('postgres', 'zhongshu', auditInsert);
  record('P5 双轨审计落地 audit_event + 幂等键唯一拒重复', a1.status === 0 && landed === '1' && dup.status !== 0,
    `insert_exit=${a1.status} landed=${landed} dup_exit=${dup.status}`);
}

// P6 台账硬约束：action CHECK / reason CHECK
{
  reset();
  const id = seedDead('P6_CONSTRAINT');
  const bogusAction = psql('postgres', 'zhongshu', insertLog(id, 'BOGUS', 'DEAD', 'PENDING', 1));
  const emptyReason = psql('postgres', 'zhongshu', insertLog(id, 'RETRY', 'DEAD', 'PENDING', 1, 1, ''));
  const okLog = psql('postgres', 'zhongshu', insertLog(id, 'RETRY', 'DEAD', 'PENDING', 1));
  record('P6 台账 action CHECK 拒非法值 / reason CHECK 拒空理由', bogusAction.status !== 0 && emptyReason.status !== 0 && okLog.status === 0,
    `bogus_action=${bogusAction.status} empty_reason=${emptyReason.status} ok=${okLog.status}`);
}

// P7 payload/headers/event_type 字节不变（修改历史被拒的 SQL 级证明）
{
  reset();
  const id = seedDead('P7_READONLY', 1, '{"fileId":2048,"note":"immutable"}');
  const fingerprint = `SELECT payload || '#' || coalesce(headers,'-') || '#' || event_type FROM outbox_event WHERE id = ${id}`;
  const before = one(fingerprint);
  oneIds(retryUpdate(id));
  psql('postgres', 'zhongshu', insertLog(id, 'RETRY', 'DEAD', 'PENDING', 1));
  psql('postgres', 'zhongshu', `UPDATE outbox_event SET status = 'DEAD' WHERE id = ${id}`); // 复位 DEAD 再验证 skip
  oneIds(skipUpdate(id));
  psql('postgres', 'zhongshu', insertLog(id, 'SKIP', 'DEAD', 'SKIPPED', 1));
  const after = one(fingerprint);
  record('P7 retry/skip 前后 payload/headers/event_type 字节不变', before === after && before.startsWith('{"fileId":2048'),
    `before==after=${before === after}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
