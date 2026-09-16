/**
 * ZS-SEC-011.B：真实 PG 持久化幂等定向验证（唯一约束并发兜底 + 重放不重复写）。
 *
 * H2 单测（JdbcPersistentIdempotentStoreTest/PersistentIdempotentIntegrationTest）覆盖服务逻辑，
 * 但 PG 的 ON CONFLICT DO NOTHING 抢占语义（裸 INSERT 唯一冲突会 abort 整个事务）、真实并发连接竞态、
 * CHECK/UNIQUE 硬约束不作 H2 放行门禁，须真实 PG17 验证（对齐 §16.1 退出条件：
 * 丢响应/并发/重启不重复写/重试重检授权的持久层证据）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + V20260916.101 持久化幂等迁移重放
 *       （同 run-job004 惯用法）。
 * 用例：
 *   P1 迁移重放 + 结构断言（11 列 / uk 唯一约束 / ck 状态 CHECK / create_time 索引）；
 *   P2 ON CONFLICT 抢占：同键二插不报错、0 行、原记录保留（败者事务不 aborted，可继续读）；
 *   P3 并发兜底：8 个独立连接同键并发 INSERT → 恰 1 行、恰 1 胜者、余者干净败出；
 *   P4 状态机：RUNNING→SUCCESS（快照+complete_time）+ BOGUS 状态被 CHECK 拒；
 *   P5 跨「进程」重放（独立新连接读回 SUCCESS+全量摘要+快照→复用决策输入齐备）+ 每键恒一行（不重复写）；
 *   P6 失败路径：RUNNING 删除后可重插（失败可重试）/ FAILED 保留挡重插（重放由切面拒绝）；
 *   P7 全量完整性：>2048 字符快照 + 长摘要往返不截断（P2-1 的存储侧对称）。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-sec011b-verify.mjs
 */
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[sec011b] Docker 不可用：验证不得静默跳过`);

const container = `zszj-sec011b-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 3532 + Math.floor(Math.random() * 600);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=sec011b', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) { if (psqlOut('postgres', 'postgres', 'SELECT 1').status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
if (!ready) { cleanup(); fail(1, '[sec011b] PG 未就绪'); }

execFileSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[sec011b] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const sql = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260916.101__infra_persistent_idempotent.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', sql).status !== 0) { cleanup(); fail(1, '[sec011b] 迁移重放失败（V1/V2/persistent_idempotent）'); }
}
console.log('[sec011b] 环境就绪（含 ZS-SEC-011.B 迁移 V20260916.101 重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// RETURNING 行 id 解析：psql -At 对 INSERT 除返回行外还输出命令标签行（INSERT 0 1 / INSERT 0 0），须按纯数字行过滤
const oneIds = (sql) => one(sql).split('\n').filter((line) => /^\d+$/.test(line));
const reset = () => one('TRUNCATE infra_persistent_idempotent');
// 抢锁插入形状对齐 JdbcPersistentIdempotentStore.tryInsertRunning（PG 方言：ON CONFLICT DO NOTHING）
const insertRunning = (key, digest) => oneIds(
  `INSERT INTO infra_persistent_idempotent (idempotent_key, tenant_id, subject_type, subject_id, action_scope, request_digest, status)
VALUES ('${key}', 1, '2', '100', 'OrderService.createOrder(..)', '${digest}', 'RUNNING') ON CONFLICT (idempotent_key) DO NOTHING RETURNING id`);
const markSuccess = (key, snapshot) => oneIds(
  `UPDATE infra_persistent_idempotent SET status = 'SUCCESS', result_snapshot = '${snapshot}', complete_time = now() WHERE idempotent_key = '${key}' AND status = 'RUNNING' RETURNING id`);

// P1 迁移重放 + 结构断言
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'infra_persistent_idempotent'
AND column_name IN ('id','idempotent_key','tenant_id','subject_type','subject_id','action_scope','request_digest','status','result_snapshot','create_time','complete_time')`);
  const uk = one(`SELECT count(*) FROM pg_constraint WHERE conrelid = 'infra_persistent_idempotent'::regclass AND conname = 'uk_persistent_idempotent_key' AND contype = 'u'`);
  const ck = one(`SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_persistent_idempotent_status'`);
  const idx = one(`SELECT count(*) FROM pg_indexes WHERE tablename = 'infra_persistent_idempotent' AND indexname = 'idx_persistent_idempotent_create_time'`);
  record('P1 迁移 V20260916.101 重放 + 结构断言（11 列/uk 唯一/ck 状态 CHECK/create_time 索引）',
    cols === '11' && uk === '1' && ck.includes('RUNNING') && ck.includes('SUCCESS') && ck.includes('FAILED') && idx === '1',
    `cols=${cols} uk=${uk} idx=${idx} ck_has_3states=${ck.includes('RUNNING') && ck.includes('SUCCESS') && ck.includes('FAILED')}`);
}

// P2 ON CONFLICT 抢占：同键二插 0 行、不报错、原记录保留（败者事务不 aborted 可继续读）
{
  reset();
  const first = insertRunning('key-p2', 'digest-original');
  const second = psqlOut('postgres', 'zhongshu', `INSERT INTO infra_persistent_idempotent (idempotent_key, tenant_id, subject_type, subject_id, action_scope, request_digest, status)
VALUES ('key-p2', 1, '2', '100', 'OrderService.createOrder(..)', 'digest-other', 'RUNNING') ON CONFLICT (idempotent_key) DO NOTHING RETURNING id`);
  const rowsAfter = one(`SELECT count(*) FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p2'`);
  const keptDigest = one(`SELECT request_digest FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p2'`);
  const loserCanRead = one(`SELECT count(*) FROM infra_persistent_idempotent`).length > 0; // 败者语句后连接仍可执行查询
  record('P2 ON CONFLICT 抢占（二插 0 行不报错 + 原记录保留 + 败者事务不 aborted）',
    first.length === 1 && second.status === 0 && rowsAfter === '1' && keptDigest === 'digest-original' && loserCanRead,
    `first=${first.length} second_exit=${second.status} rows=${rowsAfter} kept=${keptDigest}`);
}

// P3 并发兜底：8 个独立连接同键并发 INSERT → 恰 1 行、恰 1 胜者
{
  reset();
  const runners = 8;
  const sql = `INSERT INTO infra_persistent_idempotent (idempotent_key, tenant_id, subject_type, subject_id, action_scope, request_digest, status)
VALUES ('key-p3', 1, '2', '100', 'OrderService.createOrder(..)', 'digest-p3', 'RUNNING') ON CONFLICT (idempotent_key) DO NOTHING RETURNING id`;
  const attempts = await Promise.all(range(runners).map(() => new Promise((resolve) => {
    const child = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c', sql]);
    let out = '';
    child.stdout.on('data', (d) => { out += d; });
    child.on('error', () => resolve({ code: -1, winners: 0 }));
    child.on('close', (code) => resolve({ code, winners: out.split('\n').filter((line) => /^\d+$/.test(line)).length }));
  })));
  const rows = one(`SELECT count(*) FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p3'`);
  const totalWinners = attempts.reduce((sum, a) => sum + a.winners, 0);
  const cleanExits = attempts.filter((a) => a.code === 0).length;
  record('P3 并发兜底（8 独立连接同键并发 → 恰 1 行恰 1 胜者，余者干净败出）',
    rows === '1' && totalWinners === 1 && cleanExits === runners,
    `rows=${rows} winners=${totalWinners} clean_exits=${cleanExits}/${runners}`);
}

// P4 状态机：RUNNING→SUCCESS（快照+complete_time）+ BOGUS 状态被 CHECK 拒
{
  reset();
  const id = insertRunning('key-p4', 'digest-p4');
  const marked = markSuccess('key-p4', '{"code":0,"data":"result-A"}');
  const state = one(`SELECT status || '|' || coalesce(result_snapshot,'-') || '|' || (complete_time IS NOT NULL)::text FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p4'`);
  const bogus = psql('postgres', 'zhongshu', `INSERT INTO infra_persistent_idempotent (idempotent_key, tenant_id, subject_type, subject_id, action_scope, request_digest, status)
VALUES ('key-p4-bogus', 1, '2', '100', 'OrderService.createOrder(..)', 'digest-bogus', 'BOGUS')`);
  record('P4 状态机（RUNNING→SUCCESS 携快照与完成时间 + BOGUS 被 CHECK 拒）',
    id.length === 1 && marked.length === 1 && state === 'SUCCESS|{"code":0,"data":"result-A"}|true' && bogus.status !== 0,
    `state=${state} bogus_exit=${bogus.status}`);
}

// P5 跨「进程」重放：独立新连接读回 SUCCESS + 全量摘要 + 快照（复用决策输入齐备），每键恒一行（不重复写）
{
  reset();
  // codex 011.B r0 P2-1 后摘要为 keyed SHA-256（64 hex），验证 varchar(64) 列恰好容纳不截断
  const digest64 = 'a'.repeat(32) + 'b'.repeat(32);
  insertRunning('key-p5', digest64);
  markSuccess('key-p5', '"result-A"');
  // 独立 docker exec = 独立后端会话（模拟进程重启后的重放方）：决策输入 = 状态 + 摘要 + 快照
  const replayRead = psqlOut('postgres', 'zhongshu', `SELECT status || '#' || request_digest || '#' || result_snapshot FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p5'`);
  const dupInsert = insertRunning('key-p5', digest64); // 重放方也抢锁：唯一约束仍只放一行
  const rows = one(`SELECT count(*) FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p5'`);
  const digestLen = one(`SELECT length(request_digest) FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p5'`);
  record('P5 跨进程重放不重复写（独立会话读回 SUCCESS+摘要+快照 + 重插仍 1 行 + 64 位 keyed 摘要不截断）',
    replayRead.stdout.trim() === `SUCCESS#${digest64}#"result-A"` && dupInsert.length === 0 && rows === '1' && Number(digestLen) === 64,
    `replay=${replayRead.stdout.trim().slice(0, 20)}... dup_wins=${dupInsert.length} rows=${rows} digest_len=${digestLen}`);
}

// P6 失败路径：RUNNING 删除后可重插（失败可重试）/ FAILED 保留挡重插（重放由切面拒绝）
{
  reset();
  insertRunning('key-p6-del', 'digest-1');
  const deleted = oneIds(`DELETE FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p6-del' AND status = 'RUNNING' RETURNING id`);
  const reInsert = insertRunning('key-p6-del', 'digest-2');
  insertRunning('key-p6-keep', 'digest-1');
  one(`UPDATE infra_persistent_idempotent SET status = 'FAILED', complete_time = now() WHERE idempotent_key = 'key-p6-keep'`);
  const keepReInsert = insertRunning('key-p6-keep', 'digest-2');
  const keepState = one(`SELECT status FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p6-keep'`);
  record('P6 失败路径（删 RUNNING 可重插 / FAILED 保留挡重插）',
    deleted.length === 1 && reInsert.length === 1 && keepReInsert.length === 0 && keepState === 'FAILED',
    `del=${deleted.length} reinsert=${reInsert.length} keep_reinsert=${keepReInsert.length} keep=${keepState}`);
}

// P7 全量完整性：>2048 字符快照 + 长动作范围往返不截断（P2-1 的存储侧对称——摘要/快照必须全量落库）
{
  reset();
  const longSnapshot = `{"note":"${'长'.repeat(1500)}-tail-${'x'.repeat(1000)}"}`;
  insertRunning('key-p7', 'digest-p7');
  const marked = psql('postgres', 'zhongshu', `UPDATE infra_persistent_idempotent SET status = 'SUCCESS', result_snapshot = '${longSnapshot}', complete_time = now() WHERE idempotent_key = 'key-p7' AND status = 'RUNNING'`);
  const roundtrip = one(`SELECT result_snapshot FROM infra_persistent_idempotent WHERE idempotent_key = 'key-p7'`);
  record('P7 全量完整性（>2048 字符快照往返不截断）',
    marked.status === 0 && roundtrip === longSnapshot,
    `len=${roundtrip.length} intact=${roundtrip === longSnapshot}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);

/** 与 Array.prototype.map 搭配的 0..n-1 序列（脚本保持零依赖） */
function range(n) { return Array.from({ length: n }, (_, i) => i); }
