/**
 * ZS-FILE-004.B：真实 PG17 导出件用途保留期清理验证——迁移重放 + 候选查询形状 + 保留期边界
 * （含 NULL 与 <= 语义）+ 用途/状态/逻辑删除门 + 清理数据路径与引用保护交叉 + 租户隔离。
 *
 * H2 单测（FileExportRetentionTest R1~R8）覆盖服务逻辑，但 V20260925.002 的
 * ALTER ADD COLUMN + 索引 + COMMENT、候选查询的 WHERE 链/排序/LIMIT 截断语义、
 * `purpose='export' ∧ status='PUBLISHED' ∧ retention_expire_time IS NOT NULL ∧ <= now()`
 * 的边界行为须真实 PG17 重放验证（对齐计划 §6 步 4：P1 迁移重放/P2 候选查询形状/P3 保留期边界）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + V20260914.001 租户化
 *       + V20260914.012 交付票据 + V20260914.013 status 列迁移重放（同 run-file005b 惯用法）。
 * 用例（SQL 形状对齐 FileMapper.selectExportRetentionCandidates 与 FileExportRetentionServiceImpl）：
 *   P1 迁移 V20260925.002 重放 + 结构断言（purpose/retention_expire_time 列/索引/注释）
 *      + 存量行两列 NULL 零变化（既有上传/交付/补偿路径不受影响）；
 *   P2 候选查询形状：过滤链 + FIFO 排序（retention_expire_time ASC, id ASC）
 *      + LIMIT 截断如实（count >= limit ⇒ truncated=true，循 orphan/清点截断先例）；
 *   P3 保留期边界（含 NULL 与 <= 语义）：NULL 不命中 / 恰到期（<= now）命中 / 未来不命中；
 *   P4 用途/状态/逻辑删除门（防误删普通上传与中间态）：purpose NULL 或非 export 不命中、
 *      DELETING 不命中、deleted=1 不命中；
 *   P5 清理数据路径 + 引用保护交叉：条件转移 + 条件删除恰一次（重复 affected=0），
 *      活跃 REDEEMED 未过期票据存在时引用谓词命中（deleteFile 阻塞路径），票据过期后不命中；
 *   P6 租户隔离（D10 不跨技术租户）：他租户过期导出件不进入本租户候选，不污染截断判定。
 * 任一失败退出非零；缺 Docker 退出码 3。用法：node scripts/db/run-file004b-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[file004b] Docker 不可用：验证不得静默跳过`);

const container = `zszj-file004b-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 4132 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// 容器启动带端口冲突重试（循 run-file005b 同款：绑定失败时 docker 已建同名容器，须先移除再换端口；
// 就绪探测走 TCP+PGPASSWORD 探「最终 server」，避 init 临时 server 误判）
let started = false;
for (let attempt = 0; attempt < 3 && !started; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=file004b', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    started = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port = 4132 + Math.floor(Math.random() * 700);
  }
}
if (!started) fail(1, '[file004b] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 60; i++) {
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=file004b', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[file004b] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=file004b', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[file004b] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const base = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260914.001__infra_file_tenancy.sql',
    'V20260914.012__infra_file_delivery_ticket.sql',
    'V20260914.013__infra_file_status.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', base).status !== 0) { cleanup(); fail(1, '[file004b] 基础迁移重放失败（V1/V2/tenancy/ticket/status）'); }
  // 先种一条「迁移前的存量普通上传」（无 purpose/retention 列），验证 V20260925.002 两列对存量零变化
  const seed = `INSERT INTO infra_file (id, config_id, path, url, size, tenant_id, owner_user_id, scope, status)
VALUES (9001, 1, 'asset/legacy-upload.bin', 'https://oss.example.com/asset/legacy-upload.bin', 10, 1, 101, 'PRIVATE', 'PUBLISHED')`;
  if (psql('zhongshu_owner', 'zhongshu', seed).status !== 0) { cleanup(); fail(1, '[file004b] 存量行造数失败'); }
  const v002 = readFileSync(join(root, mig + 'V20260925.002__infra_file_export_retention.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v002).status !== 0) { cleanup(); fail(1, '[file004b] 迁移 V20260925.002 重放失败'); }
}
console.log('[file004b] 环境就绪（含 ZS-FILE-004.B 迁移 V20260925.002 重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
// 种子（对齐 infra_file 全列形状；deleted 默认 0）
const seedFile = (id, path, purpose, retention, status = 'PUBLISHED', tenantId = 1, deleted = 0) => psql('zhongshu_owner', 'zhongshu',
  `INSERT INTO infra_file (id, config_id, path, url, size, tenant_id, owner_user_id, scope, status, purpose, retention_expire_time, deleted)
VALUES (${id}, 1, '${path}', 'https://oss.example.com/${path}', 10, ${tenantId}, 101, 'PRIVATE', '${status}', ${purpose === null ? 'NULL' : `'${purpose}'`}, ${retention}, ${deleted})`);
// 候选查询形状：对齐 FileMapper.selectExportRetentionCandidates（LambdaQueryWrapperX 自动 deleted=0 + 租户拦截 tenant_id）
const candidates = (tenantId, limit) => one(`SELECT string_agg(id::text, ',' ORDER BY retention_expire_time ASC, id ASC) FROM (
  SELECT id, retention_expire_time FROM infra_file
  WHERE tenant_id = ${tenantId} AND deleted = 0 AND purpose = 'export' AND status = 'PUBLISHED'
    AND retention_expire_time IS NOT NULL AND retention_expire_time <= now()
  ORDER BY retention_expire_time ASC, id ASC LIMIT ${limit}) t`);
const candidateCount = (tenantId) => one(`SELECT count(*) FROM infra_file
  WHERE tenant_id = ${tenantId} AND deleted = 0 AND purpose = 'export' AND status = 'PUBLISHED'
    AND retention_expire_time IS NOT NULL AND retention_expire_time <= now()`);

// P1 迁移重放 + 结构断言 + 存量零变化
{
  const cols = one(`SELECT count(*) FROM information_schema.columns WHERE table_name = 'infra_file' AND column_name IN ('purpose', 'retention_expire_time')`);
  const idx = one(`SELECT count(*) FROM pg_indexes WHERE tablename = 'infra_file' AND indexname = 'idx_infra_file_05'`);
  const comments = one(`SELECT count(*) FROM pg_description d JOIN pg_class c ON c.oid = d.objoid
JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = d.objsubid
WHERE c.relname = 'infra_file' AND a.attname IN ('purpose', 'retention_expire_time')`);
  const legacy = one(`SELECT (purpose IS NULL AND retention_expire_time IS NULL) FROM infra_file WHERE id = 9001`);
  record('P1 迁移 V20260925.002 重放：两列/idx_infra_file_05/COMMENT + 存量行两列 NULL（零变化）',
    cols === '2' && idx === '1' && comments === '2' && legacy === 't',
    `cols=${cols} idx=${idx} comments=${comments} legacyNull=${legacy}`);
}

// P2 候选查询形状：过滤链 + FIFO 排序 + LIMIT 截断如实
{
  seedFile(9301, 'export/older.bin', 'export', `now() - interval '3 days'`);
  seedFile(9302, 'export/newer.bin', 'export', `now() - interval '1 day'`);
  seedFile(9303, 'export/future.bin', 'export', `now() + interval '1 day'`);
  seedFile(9304, 'export/no-expiry.bin', 'export', `NULL`);
  const order = candidates(1, 10);
  // 恰 2 个过期导出件（9301/9302），排序 retention ASC ⇒ 9301 在前（更早到期）
  const limited = candidates(1, 1);
  const full = candidates(1, 10);
  const total = candidateCount(1);
  // 截断判定对齐服务端 `size >= max` 双向对照（codex r1 P3-4，防同义反复）：
  //   上限 1（2 候选 ≥ 1 ⇒ truncated=true）；上限 10（2 候选 < 10 ⇒ truncated=false）
  const truncatedAtLimit1 = Number(total) >= 1;
  const truncatedAtLimit10 = Number(total) >= 10;
  record('P2 候选查询形状：过滤链（purpose/status/非空/<=now）+ FIFO 排序 + LIMIT 截断双向对照',
    order === '9301,9302' && limited === '9301' && full === '9301,9302' && total === '2'
    && truncatedAtLimit1 && !truncatedAtLimit10,
    `order=${order} limit1=${limited} limit10=${full} total=${total} trunc1=${truncatedAtLimit1} trunc10=${truncatedAtLimit10}`);
}

// P3 保留期边界（含 NULL 与 <= 语义）：NULL 不命中 / 恰到期命中 / 未来不命中
{
  seedFile(9321, 'export/edge-now.bin', 'export', `now()`);
  seedFile(9322, 'export/edge-future.bin', 'export', `now() + interval '1 minute'`);
  seedFile(9323, 'export/edge-null.bin', 'export', `NULL`);
  const after = candidates(1, 10);
  // 9321 命中（写入 now 后查询必 <= now）；9322/9323 不命中；存量 9001 非 export 不命中
  record('P3 保留期边界：恰到期（<= now）命中；未来与 NULL 不命中',
    after === '9301,9302,9321', `after=${after}`);
}

// P4 用途/状态/逻辑删除门（防误删普通上传与中间态）
{
  seedFile(9331, 'export/deleting.bin', 'export', `now() - interval '1 day'`, 'DELETING');
  seedFile(9332, 'asset/plain.bin', null, `now() - interval '1 day'`);
  seedFile(9333, 'export/soft-deleted.bin', 'export', `now() - interval '1 day'`, 'PUBLISHED', 1, 1);
  const after = candidates(1, 10);
  const gates = one(`SELECT count(*) FROM infra_file WHERE id IN (9331, 9332, 9333) AND status = 'PUBLISHED' AND purpose = 'export' AND deleted = 0`);
  record('P4 用途/状态/逻辑删除门：DELETING 中间态、非导出用途、软删除行均不构成候选',
    after === '9301,9302,9321' && gates === '0',
    `after=${after} gates=${gates}`);
}

// P5 清理数据路径 + 引用保护交叉
{
  // 清理形状（对齐 deleteByIdIfStillDeleting：WHERE id = ? AND status = 'DELETING'）
  seedFile(9401, 'export/cleanup.bin', 'export', `now() - interval '1 day'`);
  const transitioned = one(`UPDATE infra_file SET status = 'DELETING' WHERE id = 9401 AND status <> 'DELETING'`);
  const del1 = one(`DELETE FROM infra_file WHERE id = 9401 AND status = 'DELETING'`);
  const del2 = one(`DELETE FROM infra_file WHERE id = 9401 AND status = 'DELETING'`);
  const gone = one(`SELECT count(*) FROM infra_file WHERE id = 9401`);
  // 引用保护：活跃 REDEEMED 未过期票据 ⇒ 谓词命中（deleteFile 应抛 FILE_DELETE_REFERENCED 阻塞清理）
  seedFile(9410, 'export/referenced.bin', 'export', `now() - interval '1 day'`);
  const hash1 = `'h-${Date.now()}-1'`;
  psql('zhongshu_owner', 'zhongshu', `INSERT INTO infra_file_delivery_ticket (ticket_hash, file_id, owner_user_id, purpose, status, delivery_session_id, login_session, tenant_id, expires_time, redeem_time)
VALUES (${hash1}, 9410, 101, 'download', 'REDEEMED', 'sess-1', 'sess-101', 1, now() + interval '10 minutes', now())`);
  const activeRef = one(`SELECT count(*) FROM infra_file_delivery_ticket WHERE file_id = 9410 AND status = 'REDEEMED' AND expires_time > now() AND deleted = 0`);
  // 票据过期后谓词不命中 ⇒ 不再阻塞
  psql('zhongshu_owner', 'zhongshu', `UPDATE infra_file_delivery_ticket SET expires_time = now() - interval '1 minute' WHERE file_id = 9410`);
  const expiredRef = one(`SELECT count(*) FROM infra_file_delivery_ticket WHERE file_id = 9410 AND status = 'REDEEMED' AND expires_time > now() AND deleted = 0`);
  record('P5 清理数据路径：条件转移+条件删除恰一次（重复 affected=0 行消失）；活跃票据引用谓词命中（阻塞），过期后不命中（放行）',
    transitioned === 'UPDATE 1' && del1 === 'DELETE 1' && del2 === 'DELETE 0' && gone === '0'
    && activeRef === '1' && expiredRef === '0',
    `transition=${transitioned} del1=${del1} del2=${del2} gone=${gone} activeRef=${activeRef} expiredRef=${expiredRef}`);
}

// P6 租户隔离（D10：清理不跨技术租户）
{
  seedFile(9501, 'export/tenant1.bin', 'export', `now() - interval '2 days'`, 'PUBLISHED', 1);
  seedFile(9502, 'export/tenant2.bin', 'export', `now() - interval '2 days'`, 'PUBLISHED', 2);
  const t1 = candidates(1, 10);
  const t2 = candidates(2, 10);
  // 本租户候选按 retention ASC：9301(-3d) → 9501(-2d) → 9302(-1d, P2) → 9410(-1d, P5 余留) → 9321(恰到期)
  record('P6 租户隔离：他租户过期导出件不进入本租户候选（各自收敛）',
    t1 === '9301,9501,9302,9410,9321' && t2 === '9502',
    `t1=${t1} t2=${t2}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
