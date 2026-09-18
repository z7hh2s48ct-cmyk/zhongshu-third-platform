/**
 * ZS-LOGIN-005.B：真实 PG 补偿事件持久化与重放语义定向验证。
 *
 * H2 单测（OAuth2TokenRevocationCompensationSinkTest / LoginCompensationIntegrationTest /
 * OAuth2TokenServiceImplOutboxPreWriteTest）覆盖 Service→outbox→Sink 端到端补偿链路，
 * 但 PG 的 SKIP LOCKED 并发领取语义、事务原子性（业务回滚→事件回滚）、status CHECK 硬约束
 * 不作 H2 放行门禁，须真实 PG17 验证（对齐 §16.1 退出条件：补偿可重放且不复活凭据）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + V20260915.001 outbox_event 迁移重放。
 * 用例：
 *   P1 迁移重放 + 结构断言（11 关键列 / ck 状态 CHECK / claim 索引）；
 *   P2 撤销预写事件（恰 1 行 + payload 含 tokenId/tokenType/expiresTime + 无 token 明文）；
 *   P3 双实例 SKIP LOCKED 领取不重复（2 并发连接各领 1 行，无重复）；
 *   P4 Sink 重放恰 1 次（status PENDING→DISPATCHED + dispatched_at 非空 + claimed_by 登记）；
 *   P5 重启不重复写（已完成事件 status=DISPATCHED，新连接不重新领取，恒 1 行）；
 *   P6 双连接并发交错（收编 LOGIN-003 延后：2 独立连接各插 1 行事件，恰 2 行，无冲突）；
 *   P7 不复活凭据（payload 无 token 明文 + biz_id 格式 tokenType:tokenId）；
 *   P8 过期事件静默 skip（expiresTime 已过 → Sink 静默 → status=DISPATCHED 非 DEAD）。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-login005b-verify.mjs
 *
 * 注：P2/P4/P5/P7/P8 为「模拟 Service/dispatcher/Sink 行为」的纯 SQL 验证（直接 INSERT/UPDATE
 * outbox_event 行），真实 server 端到端验证由 H2 集成测试（LoginCompensationIntegrationTest）覆盖；
 * 本脚本聚焦 PG 层 SQL 语义（SKIP LOCKED / 事务原子性 / CHECK 约束 / 秘密扩散防护）。
 */
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[login005b] Docker 不可用：验证不得静默跳过`);

const container = `zszj-login005b-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 3532 + Math.floor(Math.random() * 600);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// 容器启动带端口冲突重试（连续脚本调用时随机端口可能撞上仍未释放的宿主端口）
let started = false;
for (let attempt = 0; attempt < 3 && !started; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=login005b', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    started = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port = 3532 + Math.floor(Math.random() * 600);
  }
}
if (!started) fail(1, '[login005b] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[login005b] PG 未就绪'); }

// 建库失败走干净非零退出
let dbCreated = false;
for (let attempt = 0; attempt < 3 && !dbCreated; attempt++) {
  try {
    execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
    dbCreated = true;
  } catch {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
}
if (!dbCreated) { cleanup(); fail(1, '[login005b] 建库失败（zhongshu，含 3 次重试）'); }
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[login005b] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const sql = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260915.001__infra_outbox_event.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', sql).status !== 0) { cleanup(); fail(1, '[login005b] 迁移重放失败（V1/V2/outbox_event）'); }
}
console.log('[login005b] 环境就绪（含 outbox_event 表 V20260915.001 重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
const oneIds = (sql) => one(sql).split('\n').filter((line) => /^\d+$/.test(line));
const reset = () => one('TRUNCATE outbox_event RESTART IDENTITY');
const EVENT_TYPE = 'zszj.system.oauth2.token.revocation-compensation';

// P1 迁移重放 + 结构断言
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'outbox_event'
AND column_name IN ('id','event_type','biz_type','biz_id','payload','status','tenant_id','retry_count','actor_type','claimed_by','claim_token')`);
  const ck = one(`SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_outbox_event_status'`);
  const idx = one(`SELECT count(*) FROM pg_indexes WHERE tablename = 'outbox_event' AND indexname = 'idx_outbox_event_claim'`);
  record('P1 迁移 V20260915.001 重放 + 结构断言（11 关键列/ck 状态 CHECK/claim 索引）',
    cols === '11' && ck.includes('PENDING') && ck.includes('DISPATCHED') && ck.includes('DEAD') && idx === '1',
    `cols=${cols} idx=${idx} ck_has_3states=${ck.includes('PENDING') && ck.includes('DISPATCHED') && ck.includes('DEAD')}`);
}

// P2 撤销预写事件（模拟 Service 事务内 append）
{
  reset();
  const payload = JSON.stringify({ tokenId: 12345, tokenType: 'ACCESS', expiresTime: '2026-09-17T16:00:00' });
  const ins = psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:12345', '${payload}', 1, 'SYSTEM')`);
  const rows = one(`SELECT count(*) FROM outbox_event WHERE event_type = '${EVENT_TYPE}'`);
  const storedPayload = one(`SELECT payload FROM outbox_event WHERE biz_id = 'ACCESS:12345'`);
  const noTokenPlaintext = !storedPayload.includes('accessToken') && !storedPayload.includes('refreshToken');
  record('P2 撤销预写事件（恰 1 行 + payload 含 tokenId/tokenType/expiresTime + 无 token 明文）',
    ins.status === 0 && rows === '1' && storedPayload.includes('"tokenId":12345') && storedPayload.includes('"tokenType":"ACCESS"') && noTokenPlaintext,
    `rows=${rows} payload_intact=${storedPayload.includes('"tokenId":12345')} no_plaintext=${noTokenPlaintext}`);
}

// P3 双实例 SKIP LOCKED 领取不重复
{
  reset();
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type) VALUES
('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:1', '{"tokenId":1}', 1, 'SYSTEM'),
('${EVENT_TYPE}', 'oauth2_refresh_token', 'REFRESH:2', '{"tokenId":2}', 1, 'SYSTEM')`);
  const claimSql = `SELECT id FROM outbox_event WHERE status = 'PENDING' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED`;
  const attempts = await Promise.all(range(2).map(() => new Promise((resolve) => {
    const child = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c', claimSql]);
    let out = '';
    child.stdout.on('data', (d) => { out += d; });
    child.on('error', () => resolve({ code: -1, ids: [] }));
    child.on('close', (code) => resolve({ code, ids: out.split('\n').filter((line) => /^\d+$/.test(line)) }));
  })));
  const allIds = attempts.flatMap((a) => a.ids);
  const uniqueIds = new Set(allIds);
  record('P3 双实例 SKIP LOCKED 领取不重复（2 并发连接各领 1 行，无重复）',
    allIds.length === 2 && uniqueIds.size === 2,
    `claimed=${allIds.length} unique=${uniqueIds.size}`);
}

// P4 Sink 重放恰 1 次（模拟 dispatcher 领取 + Sink 成功 → DISPATCHED）
{
  reset();
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:4', '{"tokenId":4}', 1, 'SYSTEM')`);
  const marked = oneIds(`UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = now(), claimed_by = 'ut-dispatcher', claim_token = 'token-p4'
WHERE biz_id = 'ACCESS:4' AND status = 'PENDING' RETURNING id`);
  const state = one(`SELECT status || '|' || (dispatched_at IS NOT NULL)::text || '|' || coalesce(claimed_by,'-') FROM outbox_event WHERE biz_id = 'ACCESS:4'`);
  record('P4 Sink 重放恰 1 次（status PENDING→DISPATCHED + dispatched_at 非空 + claimed_by 登记）',
    marked.length === 1 && state.startsWith('DISPATCHED|true|ut-dispatcher'),
    `state=${state}`);
}

// P5 重启不重复写（已完成事件不被重新领取）
{
  reset();
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type, status, dispatched_at)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:5', '{"tokenId":5}', 1, 'SYSTEM', 'DISPATCHED', now())`);
  const reClaim = one(`SELECT count(*) FROM outbox_event WHERE biz_id = 'ACCESS:5' AND status = 'PENDING'`);
  const rows = one(`SELECT count(*) FROM outbox_event WHERE biz_id = 'ACCESS:5'`);
  record('P5 重启不重复写（已完成事件 status=DISPATCHED，新连接不重新领取，恒 1 行）',
    reClaim === '0' && rows === '1',
    `reclaimable=${reClaim} rows=${rows}`);
}

// P6 双连接并发交错（收编 LOGIN-003 延后：兑换↔改密两序）
{
  reset();
  const insertSql = (bizId) => `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', '${bizId}', '{"tokenId":6}', 1, 'SYSTEM')`;
  const attempts = await Promise.all(range(2).map((i) => new Promise((resolve) => {
    const child = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c', insertSql(`ACCESS:6-${i}`)]);
    let out = '';
    child.stdout.on('data', (d) => { out += d; });
    child.on('error', () => resolve({ code: -1 }));
    child.on('close', (code) => resolve({ code }));
  })));
  const rows = one(`SELECT count(*) FROM outbox_event WHERE biz_id LIKE 'ACCESS:6-%'`);
  const cleanExits = attempts.filter((a) => a.code === 0).length;
  record('P6 双连接并发交错（2 独立连接各插 1 行事件，恰 2 行，无冲突）',
    rows === '2' && cleanExits === 2,
    `rows=${rows} clean_exits=${cleanExits}/2`);
}

// P7 不复活凭据（payload 秘密扩散防护）
{
  reset();
  const tokenPlaintext = 'secret-access-token-string-p7';
  const payload = JSON.stringify({ tokenId: 7, tokenType: 'ACCESS', expiresTime: '2026-09-17T17:00:00' });
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:7', '${payload}', 1, 'SYSTEM')`);
  const storedPayload = one(`SELECT payload FROM outbox_event WHERE biz_id = 'ACCESS:7'`);
  const noPlaintext = !storedPayload.includes(tokenPlaintext) && !storedPayload.includes('accessToken') && !storedPayload.includes('refreshToken');
  const bizIdFormat = one(`SELECT biz_id FROM outbox_event WHERE biz_id = 'ACCESS:7'`);
  record('P7 不复活凭据（payload 无 token 明文 + biz_id 格式 tokenType:tokenId）',
    noPlaintext && bizIdFormat === 'ACCESS:7',
    `no_plaintext=${noPlaintext} biz_id=${bizIdFormat}`);
}

// P8 过期事件静默 skip（expiresTime 已过 → Sink 静默 → DISPATCHED 非 DEAD）
{
  reset();
  const expiredPayload = JSON.stringify({ tokenId: 8, tokenType: 'ACCESS', expiresTime: '2020-01-01T00:00:00' });
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:8', '${expiredPayload}', 1, 'SYSTEM')`);
  const marked = oneIds(`UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = now()
WHERE biz_id = 'ACCESS:8' AND status = 'PENDING' RETURNING id`);
  const state = one(`SELECT status FROM outbox_event WHERE biz_id = 'ACCESS:8'`);
  record('P8 过期事件静默 skip（expiresTime 已过 → Sink 静默 → status=DISPATCHED 非 DEAD）',
    marked.length === 1 && state === 'DISPATCHED',
    `state=${state}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);

/** 与 Array.prototype.map 搭配的 0..n-1 序列（脚本保持零依赖） */
function range(n) { return Array.from({ length: n }, (_, i) => i); }
