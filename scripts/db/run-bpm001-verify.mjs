/**
 * ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收编排器（B09 技术准备，本地与 CI 同一规则）。
 *
 * 两阶段语义（同一一次性 PG 容器内的同一物理库）：
 *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
 *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
 *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
 *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器开启）：
 *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
 *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被消化且无死信。
 * 结构检查（编排器 psql 直证，不经 Java）：
 *   S0 关闭 BPM 无副作用：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表；
 *   S1 低权限运行账号无 DDL：zhongshu_app 建表必须失败；
 *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）。
 *
 * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
 *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理；
 *   - 失败非零：任一阶段/检查失败 → 非零退出；
 *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束；
 *   - Docker 负载 flaky：容器启动失败重试一次。
 * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
 *
 * 用法：node scripts/db/run-bpm001-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
const outDir = join(root, 'outputs', 'bpm-001');

function fail(code, message) { console.error(message); process.exit(code); }

// ---- 工具链与 Docker 预检（缺依赖不静默跳过）----
const toolsDir = join(root, 'tools');
const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
if (!existsSync(jdkDir) || !existsSync(mavenBin)) {
  fail(3, `[bpm001] 工具链缺失（${jdkDir} / ${mavenBin}）：worktree 须供给 tools/`);
}
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) {
  fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
}

const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 4332 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(130); });

const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const psqlOut = (user, db, sql, host) => spawnSync('docker', ['exec', container, 'psql', ...(host ? ['-h', host] : []), '-U', user, '-d', db, '-At', '-c', sql],
  { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
const psqlOk = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' }).status === 0;

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

// ---- 拉起一次性 PG（负载 flaky 重试一次）----
function dockerRunOnce() {
  const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
  if (process.platform === 'win32') {
    return spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' }).status === 0;
  }
  return spawnSync('docker', args, { encoding: 'utf8' }).status === 0;
}
console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
if (!dockerRunOnce() && !dockerRunOnce()) {
  fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
}

let ready = false;
// 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
// socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
for (let i = 0; i < 40; i++) {
  if (psqlOut('postgres', 'postgres', 'SELECT 1', '127.0.0.1').status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }

// ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
{
  let created = false, last = '';
  for (let i = 0; i < 10 && !created; i++) {
    const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { encoding: 'utf8' });
    created = r.status === 0;
    if (!created) { last = (r.stderr ?? '') + (r.stdout ?? ''); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
  }
  if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
}
{
  const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
  const r = psql('postgres', 'zhongshu', setup);
  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
}
{
  const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
  const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
  const r = psql('zhongshu_owner', 'zhongshu', bundled);
  if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
  console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
}
if (spawnSync('docker', ['exec', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-q', '-c',
  'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());'], { encoding: 'utf8' }).status !== 0) {
  cleanup(); fail(1, '[bpm001] 探针表创建失败');
}

// ---- Maven 子进程（Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe）----
const childEnv = {
  ...process.env,
  JAVA_HOME: jdkDir,
  PATH: process.platform === 'win32'
    ? `${jdkDir}\\bin;${mavenBin};${process.env.PATH}`
    : `${jdkDir}/bin:${mavenBin}:${process.env.PATH}`,
};
function runHarnessTest(testClass, username, password, schemaUpdate, asyncExecutor, timeoutMs) {
  const args = ['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`];
  const cmd = `mvn ${args.join(' ')}`;
  const r = spawnSync(process.platform === 'win32' ? 'cmd.exe' : 'mvn',
    process.platform === 'win32' ? ['/d', '/s', '/c', cmd] : args,
    {
      cwd: core, env: {
        ...childEnv,
        ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
        ZSZJ_BPM_HARNESS_USERNAME: username,
        ZSZJ_BPM_HARNESS_PASSWORD: password,
        ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
        ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
      },
      encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, timeout: timeoutMs,
    });
  return { ...r, output: (r.stdout ?? '') + (r.stderr ?? '') };
}
function surefireSummary(testClass) {
  const xml = join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
    `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
  if (!existsSync(xml)) return null;
  const text = readFileSync(xml, 'utf8');
  const attr = (name) => Number((text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1] ?? -1);
  return { tests: attr('tests'), failures: attr('failures'), errors: attr('errors'), skipped: attr('skipped') };
}
const countTables = (pattern) => Number((psqlOut('postgres', 'zhongshu',
  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`).stdout ?? '0').trim()) || 0;

// ---- S0：关闭 BPM 的结构性证明——System/Infra 全基线零流程表 ----
{
  const act = countTables('act\\_%'), flw = countTables('flw\\_%');
  record('S0 关闭BPM无副作用：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
}
// ---- S1：低权限运行账号无 DDL ----
{
  const r = psqlOk('zhongshu_app', 'zhongshu', 'CREATE TABLE zszj_app_should_fail(id int);');
  record('S1 运行账号无DDL：zhongshu_app 建表被拒', r === false, r ? 'app 竟然建表成功' : '按预期失败');
}

// ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
mkdirSync(outDir, { recursive: true });
const bootLog = runHarnessTest('BpmPgHarnessBootstrapTest', 'zhongshu_owner', 'owner_local_1', 'true', 'false', 20 * 60 * 1000);
writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
const bootSummary = surefireSummary('BpmPgHarnessBootstrapTest');
const bootOk = bootLog.status === 0 && bootSummary && bootSummary.tests > 0 && bootSummary.failures === 0 && bootSummary.errors === 0;
record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk,
  bootSummary ? `tests=${bootSummary.tests} failures=${bootSummary.failures} errors=${bootSummary.errors} skipped=${bootSummary.skipped}` : `exit=${bootLog.status}（无 surefire 报告）`);
if (!bootOk) {
  console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
  finish();
}

const actAfterBootstrap = countTables('act\\_%');
const schemaVersionAfterBootstrap = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();

// ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
const runLog = runHarnessTest('BpmPgHarnessRuntimeTest', 'zhongshu_app', 'app_local_1', 'false', 'false', 15 * 60 * 1000);
writeFileSync(join(outDir, 'runtime.log'), runLog.output);
const runSummary = surefireSummary('BpmPgHarnessRuntimeTest');
const runOk = runLog.status === 0 && runSummary && runSummary.tests > 0 && runSummary.failures === 0 && runSummary.errors === 0;
record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk,
  runSummary ? `tests=${runSummary.tests} failures=${runSummary.failures} errors=${runSummary.errors} skipped=${runSummary.skipped}` : `exit=${runLog.status}（无 surefire 报告）`);
if (!runOk) {
  console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
}

// ---- S2：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
{
  const actAfterRuntime = countTables('act\\_%');
  const schemaVersionAfterRuntime = (psqlOut('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'").stdout ?? '').trim();
  const probeRows = Number((psqlOut('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe').stdout ?? '0').trim()) || 0;
  record('S2 运行期零DDL：引擎表数量与 schema.version 不变',
    actAfterBootstrap === actAfterRuntime && schemaVersionAfterBootstrap === schemaVersionAfterRuntime,
    `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${schemaVersionAfterBootstrap}→${schemaVersionAfterRuntime}`);
  record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', probeRows >= 3, `探针行=${probeRows}`);
}

function finish() {
  cleanup();
  console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
  console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
  try {
    writeFileSync(join(outDir, 'runtime-report.json'),
      JSON.stringify({ finishedAt: new Date().toISOString(), container: cleaned ? container : `${container}(运行中)`, results, pass, failCount }, null, 2));
  } catch { /* 报告落盘失败不影响退出码语义 */ }
  process.exit(failCount === 0 ? 0 : 1);
}
finish();
