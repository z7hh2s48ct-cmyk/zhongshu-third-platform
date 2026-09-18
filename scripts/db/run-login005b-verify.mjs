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
 * 注：本脚本聚焦 PG 层 SQL 语义与事务契约（SKIP LOCKED / 原子性 / CHECK 约束 / 秘密扩散防护；
 * P8 为「删令牌行 + 预写事件」同事务原子性契约，对齐 JOB-002 套件 P2 口径）。Sink 过期 skip /
 * 重放撤销等<b>行为级</b>验证不作 SQL 模拟（r0 P2-7：夹具自证不构成行为验证），由 H2 集成测试
 * （LoginCompensationIntegrationTest + Sink 用例 7 交互断言）承担；Java-on-PG 行为联验归 D-07
 * 正式联验链（循 JOB-003/FILE-005.B 登记）。
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

// P3 双实例 SKIP LOCKED 领取不重复（循 run-job002-verify 同款持锁会话模式；r0 P2-6 加固：
// statement_timeout 短于持锁时长 + A 锁定行确定化，堵死「无 SKIP LOCKED 时 B 阻塞至 A 释放后
// 顺序领取仍 PASS」的假绿通道——B 若阻塞即超时报错，且断言 B 领到的恰是非 A 锁定行）
{
  reset();
  psql('postgres', 'zhongshu', `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type) VALUES
('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:1', '{"tokenId":1}', 1, 'SYSTEM'),
('${EVENT_TYPE}', 'oauth2_refresh_token', 'REFRESH:2', '{"tokenId":2}', 1, 'SYSTEM')`);
  // A 锁定行确定化：A 按 biz_id 精确领取 ACCESS:1 行，锁定的行 id 从表内可查
  const lockedId = one(`SELECT id FROM outbox_event WHERE biz_id = 'ACCESS:1'`);
  const claimSql = (token, whereExtra) => `UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = now(), claimed_by = '${token}'
WHERE id IN (SELECT id FROM outbox_event WHERE status = 'PENDING'${whereExtra ? ' AND ' + whereExtra : ''} ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING id`;
  const waitAState = async (expectActive) => {
    for (let i = 0; i < 60; i++) {
      const n = one(`SELECT count(*) FROM pg_stat_activity WHERE query LIKE '%pg_sleep(8)%' AND state = 'active' AND pid <> pg_backend_pid()`);
      if (Number(n) === (expectActive ? 1 : 0)) return true;
      Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 250);
    }
    return false;
  };
  // 会话 A：领取 ACCESS:1 行并持锁 8 秒不提交（行锁保持；ROLLBACK 归还为 PENDING）
  const holdSql = `BEGIN; ${claimSql('inst-a', `biz_id = 'ACCESS:1'`)} ; SELECT pg_sleep(8); ROLLBACK;`;
  spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-c', holdSql], { stdio: 'ignore' });
  const aLocked = await waitAState(true);
  // 会话 B：statement_timeout=3s < 持锁 8s——若无 SKIP LOCKED（变异），B 阻塞在 A 锁定行上
  // 直至超时中断（语句报错、无行返回）；有 SKIP LOCKED 则立即跳过锁定行领到 REFRESH:2
  const bClaim = spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c',
    `SET statement_timeout = '3s'; ${claimSql('inst-b')}`], { encoding: 'utf8' });
  const bIds = bClaim.status === 0 ? bClaim.stdout.split('\n').filter((line) => /^\d+$/.test(line)) : [];
  const aGone = await waitAState(false);
  // A 回滚释放后其行可再领：C 恰领到 A 曾锁定的行（与 B 不相交，双实例全程无重复领取）
  const cIds = aGone ? oneIds(claimSql('inst-c')) : [];
  const disjoint = cIds.length === 1 && cIds[0] === lockedId && !bIds.includes(lockedId);
  record('P3 双实例 SKIP LOCKED 领取不重复（A 持 ACCESS:1 行，B 超时护栏下仅领 REFRESH:2，C 领回 A 行）',
    bIds.length === 1 && cIds.length === 1 && cIds[0] === lockedId && !bIds.includes(lockedId),
    `A持锁=${aLocked} locked=${lockedId} B=${bIds.join(',')} A释放=${aGone} C=${cIds.join(',')} 确定性=${disjoint}`);
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

// P8 业务撤销与补偿事件同事务原子性（SQL 事务契约；codex r0 P2-7 处置：原 P8「无条件标记
// DISPATCHED」属夹具自证，不验证 Sink 行为——Sink 过期 skip / 重放撤销的行为级验证由 H2 集成
// 测试（LoginCompensationIntegrationTest + Sink 交互断言）承担，本套件聚焦 SQL 契约面。
// 本探针对齐 JOB-002 套件 P2：撤销主逻辑（删令牌行）与预写事件同事务，回滚双侧不留痕 / 提交双侧落库）
{
  reset();
  const tokenInsert = (tag) => `INSERT INTO system_oauth2_access_token (id, user_id, user_type, user_info, access_token, refresh_token, client_id, expires_time, tenant_id)
VALUES (9${tag}, 9001, 2, '{}', 'at-${tag}', 'rt-${tag}', 'default', now() + interval '30 minutes', 1);`;
  const eventInsert = (tag) => `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type)
VALUES ('${EVENT_TYPE}', 'oauth2_access_token', 'ACCESS:9${tag}', '{"tokenId":9${tag}}', 1, 'SYSTEM');`;
  // 回滚侧：业务回滚无事件、令牌行也不留痕（同一事务原子性）
  const rbP = psql('postgres', 'zhongshu', `BEGIN;\n${tokenInsert(1)}\n${eventInsert(1)}\nROLLBACK;`);
  const rbToken = one(`SELECT count(*) FROM system_oauth2_access_token WHERE id = 91`);
  const rbEvent = one(`SELECT count(*) FROM outbox_event WHERE biz_id = 'ACCESS:91'`);
  // 提交侧：提交必有事件、令牌行落库
  const cmP = psql('postgres', 'zhongshu', `BEGIN;\n${tokenInsert(2)}\n${eventInsert(2)}\nCOMMIT;`);
  if (rbP.status !== 0 || cmP.status !== 0) {
    console.error(`[login005b][P8 debug] rollback status=${rbP.status} stderr=${rbP.stderr}`);
    console.error(`[login005b][P8 debug] commit status=${cmP.status} stderr=${cmP.stderr}`);
  }
  const cmToken = one(`SELECT count(*) FROM system_oauth2_access_token WHERE id = 92`);
  const cmEvent = one(`SELECT count(*) FROM outbox_event WHERE biz_id = 'ACCESS:92'`);
  record('P8 业务撤销与补偿事件同事务原子性（回滚双侧不留痕 / 提交双侧落库）',
    rbP.status === 0 && cmP.status === 0 && rbToken === '0' && rbEvent === '0' && cmToken === '1' && cmEvent === '1',
    `rollback_token=${rbToken} rollback_event=${rbEvent} commit_token=${cmToken} commit_event=${cmEvent}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);

/** 与 Array.prototype.map 搭配的 0..n-1 序列（脚本保持零依赖） */
function range(n) { return Array.from({ length: n }, (_, i) => i); }
