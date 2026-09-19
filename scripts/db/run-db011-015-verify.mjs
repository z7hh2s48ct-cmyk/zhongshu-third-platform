/**
 * ZS-DB-011~015：有界删除 PG 适配——真实 PG 验证用例集。
 *
 * 对五个 Mapper 的等效删除语句（与注解逐字一致，仅参数内联）在真实 PG 上验证：
 *   a) 边界截止不误删（截止 2026-01-10 之前的过期行清空、之后的未过期行保留）；
 *   b) 每批不超过 limit=3（5 行过期 → 首轮删 3、次轮删 2）；
 *   c) 空集可执行（清空后再跑不报错）。
 * 表与驱动语句（与各 Mapper 注解逐字一致，仅参数内联）：
 *   D1 infra_api_access_log（create_time）— ZS-DB-011
 *   D2 infra_api_error_log（create_time）— ZS-DB-012
 *   D3 infra_job_log（create_time）— ZS-DB-013
 *   D4 system_oauth2_access_token（expires_time）— ZS-DB-014
 *   D5 system_oauth2_refresh_token（expires_time）— ZS-DB-015
 * 任一失败退出非零。用法：node scripts/db/run-db011-015-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
if (spawnSync('docker', ['version'], { encoding: 'utf8' }).status !== 0) fail(3, '[db011-015] Docker 不可用：验证不得静默跳过');

const container = `zszj-db011-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 4432 + Math.floor(Math.random() * 500);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

// FLAKY-2 收敛（循 run-db007 同款）：容器启动带端口冲突重试 + 就绪探测/建库走 TCP+PGPASSWORD 探「最终 server」
// socket 探测可命中 init 临时 server（entrypoint 随后关闭它），导致建库/迁移落到临时库失败
let pgStarted = false;
for (let attempt = 0; attempt < 3 && !pgStarted; attempt++) {
  try {
    execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=x', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
    pgStarted = true;
  } catch {
    try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
    port += 37 + Math.floor(Math.random() * 100);
  }
}
if (!pgStarted) fail(1, '[db011-015] PG 容器启动失败（含 3 次端口冲突重试）');
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const one = (sql) => spawnSync('docker', ['exec', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-At', '-c', sql], { encoding: 'utf8' }).stdout.trim();

let ready = false;
for (let i = 0; i < 30; i++) {
  // TCP+PGPASSWORD 探「最终 server」（FLAKY-2：socket 会命中 init 临时 server）
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=x', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[db011-015] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=x', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[db011-015] 角色授权失败'); }
  const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
  const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
  if (psql('zhongshu_owner', 'zhongshu', v1 + '\n' + v2).status !== 0) { cleanup(); fail(1, '[db011-015] V1/V2 执行失败'); }
}
console.log('[db011-015] 环境就绪');

const LIMIT = 3;
const DEADLINE = '2026-01-10 00:00:00';

const tables = [
  {
    tag: 'D1', table: 'infra_api_access_log', timeCol: 'create_time',
    cols: '(id, trace_id, user_id, user_type, application_name, request_method, request_url, user_ip, user_agent, begin_time, end_time, duration, result_code, create_time, update_time, deleted, tenant_id)',
    row: (id, t) => `(${id}, 'trace-${id}', 1, 2, 'zhongshu', 'GET', '/c/${id}', '127.0.0.1', 'ua', '${t}', '${t}', 10, 0, '${t}', '${t}', 0, 1)`,
  },
  {
    tag: 'D2', table: 'infra_api_error_log', timeCol: 'create_time',
    cols: '(id, trace_id, user_id, user_type, application_name, request_method, request_url, request_params, user_ip, user_agent, exception_time, exception_name, exception_message, exception_root_cause_message, exception_stack_trace, exception_class_name, exception_file_name, exception_method_name, exception_line_number, process_status, create_time, update_time, deleted, tenant_id)',
    row: (id, t) => `(${id}, 'trace-${id}', 1, 2, 'zhongshu', 'GET', '/c/${id}', '{}', '127.0.0.1', 'ua', '${t}', 'Ex${id}', 'm', 'm', 's', 'C', 'F', 'M', ${id}, 0, '${t}', '${t}', 0, 1)`,
  },
  {
    tag: 'D3', table: 'infra_job_log', timeCol: 'create_time',
    cols: '(id, job_id, handler_name, handler_param, execute_index, begin_time, end_time, duration, status, result, creator, create_time, updater, update_time, deleted)',
    row: (id, t) => `(${id}, 1, 'job-${id}', '', 1, '${t}', '${t}', 10, 0, '', 'c', '${t}', 'c', '${t}', 0)`,
  },
  {
    tag: 'D4', table: 'system_oauth2_access_token', timeCol: 'expires_time',
    cols: '(id, user_id, user_type, user_info, access_token, refresh_token, client_id, expires_time, create_time, update_time, deleted, tenant_id)',
    row: (id, t) => `(${id}, 1, 2, '{}', 'at-${id}', 'rt-${id}', 'default', '${t}', '${t}', '${t}', 0, 1)`,
  },
  {
    tag: 'D5', table: 'system_oauth2_refresh_token', timeCol: 'expires_time',
    cols: '(id, user_id, refresh_token, user_type, client_id, expires_time, create_time, update_time, deleted, tenant_id)',
    row: (id, t) => `(${id}, 1, 'rt-${id}', 2, 'default', '${t}', '${t}', '${t}', 0, 1)`,
  },
];

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };

for (const c of tables) {
  // 造数：5 行过期（2026-01-01~05，均早于截止）+ 2 行未过期（2026-12-31）
  let seed = `BEGIN;\nDELETE FROM ${c.table};\n`;
  for (let i = 1; i <= 5; i++) seed += `INSERT INTO ${c.table} ${c.cols} VALUES ${c.row(id0(i), `2026-01-0${i} 12:00:00`)};\n`;
  for (let i = 6; i <= 7; i++) seed += `INSERT INTO ${c.table} ${c.cols} VALUES ${c.row(id0(i), '2026-12-31 12:00:00')};\n`;
  seed += `COMMIT;`;
  if (psql('postgres', 'zhongshu', seed).status !== 0) { record(`${c.tag} ${c.table}`, false, '造数失败'); continue; }

  const delSql = `DELETE FROM ${c.table} WHERE id IN (SELECT id FROM ${c.table} WHERE ${c.timeCol} < '${DEADLINE}' LIMIT ${LIMIT})`;

  // 有界删除循环：每轮删除 ≤ limit，直至清空（模拟 Job 循环）
  let bounded = true, rounds = 0, lastDeleted = 0;
  for (let round = 0; round < 10; round++) {
    const before = Number(one(`SELECT count(*) FROM ${c.table} WHERE ${c.timeCol} < '${DEADLINE}'`));
    if (before === 0) break;
    const r = psql('postgres', 'zhongshu', delSql);
    if (r.status !== 0) { bounded = false; break; }
    const after = Number(one(`SELECT count(*) FROM ${c.table} WHERE ${c.timeCol} < '${DEADLINE}'`));
    lastDeleted = before - after;
    if (lastDeleted > LIMIT) bounded = false;
    rounds++;
  }
  const expiredLeft = Number(one(`SELECT count(*) FROM ${c.table} WHERE ${c.timeCol} < '${DEADLINE}'`));
  const liveLeft = Number(one(`SELECT count(*) FROM ${c.table} WHERE ${c.timeCol} >= '2026-12-31 00:00:00'`));
  const emptyRun = psql('postgres', 'zhongshu', delSql);

  const ok = bounded && expiredLeft === 0 && liveLeft === 2 && emptyRun.status === 0;
  record(`${c.tag} ${c.table} 有界删除`, ok, `轮次=${rounds} 末轮删除=${lastDeleted} 过期残留=${expiredLeft} 未过期=${liveLeft} 空跑=${emptyRun.status === 0 ? 'OK' : 'ERR'}`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
function id0(i) { return i; }
