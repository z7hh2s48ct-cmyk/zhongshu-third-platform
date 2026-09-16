/**
 * ZS-FILE-005.B：真实 PG17 超时补偿收敛 + 并发补偿 CAS 串行化 + 孤儿清点 + 重复清理不误删回归。
 *
 * H2 单测（FileDeleteCompensationJobTest/FileOrphanServiceTest）覆盖服务逻辑，但 V20260916.102 的
 * ALTER ADD COLUMN + 存量回填 + 索引、行锁并发语义、跨表孤儿清点形状须真实 PG17 重放验证
 * （对齐退出条件：超时/重启/重复清理不误删且最终对账一致）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + V20260914.001 租户化
 *       + V20260914.012 交付票据 + V20260914.013 status 列迁移重放（同 run-job004 惯用法）。
 * 用例（SQL 形状对齐 FileMapper.selectCompensationCandidates/claimDeletingForCompensation/
 *       deleteByIdIfStillDeleting 与 FileOrphanServiceImpl 孤儿核验）：
 *   P1 迁移 V20260916.102 重放 + 结构断言（deleting_time 列/索引/存量 DELETING 按 update_time 回填）；
 *   P2 条件转移形状：PUBLISHED→DELETING + deleting_time 写入（转移先行语义）；
 *   P3 并发补偿被 CAS 串行化：双 psql 并发领取同一行，恰好一个 affected=1（行锁 + 谓词重评）；
 *   P4 收敛 + 重复清理不误删：条件删除恰一次，重复 affected=0，行消失（最终对账一致）；
 *   P5 引用保护数据路径：REDEEMED 未过期谓词命中 / 过期不命中（补偿不得绕过 .A 引用保护）；
 *   P6 回退清空 deleting_time：引用拒绝回退 PUBLISHED 后不再构成补偿候选；
 *   P7 孤儿清点：无任何 infra_file 记录引用的存储 path 恰为候选集（跨租户全局核验形状）；
 *   P8 候选 FIFO 排序（codex r0 P2-1）：ORDER BY deleting_time ASC, id ASC——最久等待优先；
 *   P9 清点前缀 LIKE 转义（codex r1 P2）：ESCAPE '' 下 _ 字面量不展开为通配符（字面前缀合同）。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-file005b-verify.mjs
 */
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[file005b] Docker 不可用：验证不得静默跳过`);

const container = `zszj-file005b-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// 容器启动带端口冲突重试（循 run-sec011b 同款：绑定失败时 docker 已建同名容器，须先移除再换端口；
// 就绪探测走 TCP+PGPASSWORD 探「最终 server」，避 init 临时 server 误判——循 run-ops002b codex r0 P2 修复）
let started = false;
for (let attempt = 0; attempt < 3 && !started; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=file005b', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    started = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port = 3432 + Math.floor(Math.random() * 700);
  }
}
if (!started) fail(1, '[file005b] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 60; i++) {
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=file005b', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[file005b] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=file005b', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[file005b] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const base = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260914.001__infra_file_tenancy.sql',
    'V20260914.012__infra_file_delivery_ticket.sql',
    'V20260914.013__infra_file_status.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', base).status !== 0) { cleanup(); fail(1, '[file005b] 基础迁移重放失败（V1/V2/tenancy/ticket/status）'); }
  // 先种一条「迁移前的存量 DELETING」（update_time 2h 前），验证 .102 的回填分支
  const seed = `INSERT INTO infra_file (id, config_id, path, url, size, tenant_id, owner_user_id, scope, status, update_time)
VALUES (9001, 1, 'asset/legacy-deleting.bin', 'https://oss.example.com/asset/legacy-deleting.bin', 10, 1, 101, 'PRIVATE', 'DELETING', now() - interval '2 hours')`;
  if (psql('zhongshu_owner', 'zhongshu', seed).status !== 0) { cleanup(); fail(1, '[file005b] 存量 DELETING 造数失败'); }
  const c102 = readFileSync(join(root, mig + 'V20260916.102__infra_file_compensation.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', c102).status !== 0) { cleanup(); fail(1, '[file005b] 迁移 V20260916.102 重放失败'); }
}
console.log('[file005b] 环境就绪（含 ZS-FILE-005.B 迁移 V20260916.102 重放 + 存量回填）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// 条件转移/回退/领取/条件删除形状对齐 FileServiceImpl/FileMapper（应用侧参数化时间此处以 now() 表达）
const transition = (id) => one(`UPDATE infra_file SET status = 'DELETING', deleting_time = now() WHERE id = ${id} AND status <> 'DELETING'`);
const revert = (id) => one(`UPDATE infra_file SET status = 'PUBLISHED', deleting_time = NULL WHERE id = ${id} AND status = 'DELETING'`);
const ageForClaim = (id) => one(`UPDATE infra_file SET deleting_time = now() - interval '2 hours' WHERE id = ${id} AND status = 'DELETING'`);
const claim = (id) => one(`UPDATE infra_file SET deleting_time = now() WHERE id = ${id} AND status = 'DELETING' AND deleting_time <= now() - interval '60 minutes'`);
const conditionalDelete = (id) => one(`DELETE FROM infra_file WHERE id = ${id} AND status = 'DELETING'`);
const seedFile = (id, path) => psql('zhongshu_owner', 'zhongshu',
  `INSERT INTO infra_file (id, config_id, path, url, size, tenant_id, owner_user_id, scope, status) VALUES (${id}, 1, '${path}', 'https://oss.example.com/${path}', 10, 1, 101, 'PRIVATE', 'PUBLISHED')`);

// P1 迁移重放 + 结构断言 + 存量回填
{
  const col = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'infra_file' AND column_name = 'deleting_time'`);
  const idx = one(`SELECT count(*) FROM pg_indexes WHERE tablename = 'infra_file' AND indexname = 'idx_infra_file_status_deleting_time'`);
  const backfilled = one(`SELECT status || '|' || (deleting_time::timestamp < now() - interval '1 hour') FROM infra_file WHERE id = 9001`);
  record('P1 迁移 V20260916.102 重放：deleting_time 列/索引 + 存量 DELETING 按 update_time 回填',
    col === '1' && idx === '1' && backfilled === 'DELETING|true', `col=${col} idx=${idx} backfill=${backfilled}`);
}

// P2 条件转移形状（转移先行 + deleting_time 写入）
{
  seedFile(9101, 'asset/comp-conv.bin');
  const affected = transition(9101);
  const state = one(`SELECT status || '|' || (deleting_time IS NOT NULL) FROM infra_file WHERE id = 9101`);
  const loser = transition(9101);
  record('P2 条件转移 PUBLISHED→DELETING + deleting_time 写入；重复转移败者 affected=0',
    affected === 'UPDATE 1' && state === 'DELETING|true' && loser === 'UPDATE 0',
    `affected=${affected} state=${state} loser=${loser}`);
}

// P3 并发补偿被 CAS 串行化（双 psql 并发领取同一行）
{
  seedFile(9102, 'asset/comp-cas.bin');
  transition(9102);
  ageForClaim(9102);
  const claimSql = `UPDATE infra_file SET deleting_time = now() WHERE id = 9102 AND status = 'DELETING' AND deleting_time <= now() - interval '60 minutes';`;
  const runOne = () => new Promise((resolve) => {
    const p = spawn('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c', claimSql], { encoding: 'utf8' });
    let out = '';
    p.stdout.on('data', (d) => { out += d; });
    p.on('close', () => resolve(out.trim()));
  });
  const [r1, r2] = await Promise.all([runOne(), runOne()]);
  const winners = [r1, r2].filter((r) => r === 'UPDATE 1').length;
  record('P3 并发补偿被 CAS 串行化：双实例并发领取同一行，恰好一个 affected=1',
    winners === 1 && r1 !== r2, `claim_results=${JSON.stringify([r1, r2])}`);
}

// P4 收敛 + 重复清理不误删 + 最终对账一致
{
  const del1 = conditionalDelete(9102);
  const del2 = conditionalDelete(9102);
  const remaining = one(`SELECT count(*) FROM infra_file WHERE id = 9102`);
  const untouched = one(`SELECT count(*) FROM infra_file WHERE id = 9001 AND status = 'DELETING'`);
  record('P4 收敛后条件删除恰一次、重复清理 affected=0、记录消失且不误删他行（最终对账一致）',
    del1 === 'DELETE 1' && del2 === 'DELETE 0' && remaining === '0' && untouched === '1',
    `del1=${del1} del2=${del2} remaining=${remaining} untouched=${untouched}`);
}

// P5 引用保护数据路径（补偿不得绕过 .A 引用保护）
{
  seedFile(9103, 'asset/comp-ref.bin');
  const active = `INSERT INTO infra_file_delivery_ticket (ticket_hash, file_id, owner_user_id, purpose, status, delivery_session_id, login_session, tenant_id, expires_time, redeem_time)
VALUES (hash1, 9103, 101, 'download', 'REDEEMED', 'sess-1', 'sess-101', 1, now() + interval '10 minutes', now())`.replace('hash1', "'h-" + Date.now() + "-1'");
  const expired = `INSERT INTO infra_file_delivery_ticket (ticket_hash, file_id, owner_user_id, purpose, status, delivery_session_id, login_session, tenant_id, expires_time, redeem_time)
VALUES (hash2, 9103, 101, 'download', 'REDEEMED', 'sess-2', 'sess-101', 1, now() - interval '1 minute', now())`.replace('hash2', "'h-" + Date.now() + "-2'");
  psql('zhongshu_owner', 'zhongshu', active);
  psql('zhongshu_owner', 'zhongshu', expired);
  // 谓词形状对齐 FileDeliveryTicketMapper.selectActiveRedeemedByFileId
  const hitActive = one(`SELECT count(*) FROM infra_file_delivery_ticket WHERE file_id = 9103 AND status = 'REDEEMED' AND expires_time > now() AND deleted = 0`);
  record('P5 引用保护数据路径：进行中交付会话被引用谓词命中（补偿/对账据此阻塞）', hitActive === '1', `active_refs=${hitActive}`);
}

// P6 回退清空 deleting_time（引用拒绝回退后不再是补偿候选）
{
  seedFile(9104, 'asset/comp-revert.bin');
  transition(9104);
  const reverted = revert(9104);
  const state = one(`SELECT status || '|' || (deleting_time IS NULL) FROM infra_file WHERE id = 9104`);
  const candidate = one(`SELECT count(*) FROM infra_file WHERE id = 9104 AND status = 'DELETING' AND deleting_time IS NOT NULL AND deleting_time <= now() - interval '60 minutes'`);
  record('P6 引用拒绝回退 PUBLISHED + 清空 deleting_time；不再构成补偿候选',
    reverted === 'UPDATE 1' && state === 'PUBLISHED|true' && candidate === '0',
    `reverted=${reverted} state=${state} candidate=${candidate}`);
}

// P7 孤儿清点：跨租户全局核验形状（无任何 infra_file 记录引用的存储 path 恰为候选集）
{
  const contentSeed = [
    `DELETE FROM infra_file_content WHERE config_id = 1 AND path IN ('asset/orphan.bin', 'asset/ref.bin', 'asset/other-tenant.bin')`,
    `INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8001, 1, 'asset/orphan.bin', '\\x6f'::bytea)`,
    `INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8002, 1, 'asset/ref.bin', '\\x72'::bytea)`,
    `INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8003, 1, 'asset/other-tenant.bin', '\\x74'::bytea)`,
  ].join(';\n');
  psql('zhongshu_owner', 'zhongshu', contentSeed);
  seedFile(9201, 'asset/ref.bin'); // 本租户（1）引用
  seedFile(9202, 'asset/other-tenant.bin');
  psql('zhongshu_owner', 'zhongshu', `UPDATE infra_file SET tenant_id = 2 WHERE id = 9202`); // 他租户引用
  const orphans = one(`SELECT string_agg(path, ',' ORDER BY path) FROM (
  SELECT DISTINCT fc.path FROM infra_file_content fc
  WHERE fc.config_id = 1 AND fc.deleted = 0
    AND NOT EXISTS (SELECT 1 FROM infra_file f WHERE f.config_id = fc.config_id AND f.path = fc.path AND f.deleted = 0)) t`);
  record('P7 孤儿清点：无任何租户记录引用的 path 恰为候选（跨租户全局核验防误判）',
    orphans === 'asset/orphan.bin', `orphans=${orphans}`);
}

// P8 候选 FIFO 排序（codex r0 P2-1）：deleting_time ASC, id ASC——最久等待优先，防低位 ID 占据批次窗口
{
  // 9101（P2 转移为 DELETING）拉成最老等待但 id 较大；9001（存量回填）等待较短但 id 更小
  psql('postgres', 'zhongshu', `UPDATE infra_file SET deleting_time = now() - interval '3 hours' WHERE id = 9101`);
  psql('postgres', 'zhongshu', `UPDATE infra_file SET deleting_time = now() - interval '2 hours' WHERE id = 9001`);
  const order = one(`SELECT string_agg(id::text, ',' ORDER BY deleting_time ASC, id ASC) FROM infra_file
WHERE status = 'DELETING' AND deleting_time IS NOT NULL AND deleting_time <= now() - interval '60 minutes'`);
  record('P8 补偿候选 FIFO 排序（deleting_time ASC, id ASC）：最久等待（大 id）排在最前',
    order === '9101,9001', `order=${order}`);
}

// P9 清点前缀 LIKE 转义（codex r1 P2）：ESCAPE 下 _ 字面量不展开（DBFileClient.escapeLikePrefix + mapper ESCAPE 合同）
{
  psql('postgres', 'zhongshu', `DELETE FROM infra_file_content WHERE config_id = 1 AND path IN ('asset/_/a.bin', 'asset/0/a.bin', 'asset/50%/x.bin')`);
  const seedRows = [
    "INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8011, 1, 'asset/_/a.bin', '\\x6f'::bytea)",
    "INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8012, 1, 'asset/0/a.bin', '\\x6f'::bytea)",
    "INSERT INTO infra_file_content (id, config_id, path, content) VALUES (8013, 1, 'asset/50%/x.bin', '\\x6f'::bytea)",
  ].join(';\n');
  if (psql('zhongshu_owner', 'zhongshu', seedRows).status !== 0) { cleanup(); fail(1, '[file005b] P9 造数失败'); }
  // 转义合同（应用形状）：prefix 'asset/_/a' 经 escapeLikePrefix → 'asset/\_/a'，LIKE ... ESCAPE '\' → _ 字面
  const escaped = one(`SELECT string_agg(path, ',' ORDER BY path) FROM infra_file_content
WHERE config_id = 1 AND deleted = 0 AND path LIKE CONCAT('asset/\\_/a', '%') ESCAPE '\\' GROUP BY path`);
  // 未转义对照（修复前行为）：_ 展开为通配符，前缀外行被误配
  const unescaped = one(`SELECT string_agg(path, ',' ORDER BY path) FROM infra_file_content
WHERE config_id = 1 AND deleted = 0 AND path LIKE CONCAT('asset/_/a', '%') GROUP BY path`);
  const pctEscaped = one(`SELECT string_agg(path, ',' ORDER BY path) FROM infra_file_content
WHERE config_id = 1 AND deleted = 0 AND path LIKE CONCAT('asset/50\\%', '%') ESCAPE '\\' GROUP BY path`);
  record('P9 清点前缀 LIKE 转义：_/% 字面量不展开（转义恰配 1 行；未转义对照误配 2 行佐证修复必要）',
    escaped === 'asset/_/a.bin' && unescaped.includes('asset/0/a.bin') && pctEscaped === 'asset/50%/x.bin',
    `escaped=${escaped} unescaped=${unescaped} pct=${pctEscaped}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
