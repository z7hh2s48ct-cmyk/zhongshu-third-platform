/**
 * ZS-BPM-001：BPM 独立装配与 PostgreSQL 验收编排器（B09 技术准备，本地与 CI 同一规则）。
 *
 * 两阶段语义（同一一次性 PG 容器内的同一物理库）：
 *   阶段1 bootstrap（zhongshu_owner，schema-update=true，异步执行器挂起）：
 *     - 空库上由 Flowable 引擎自建 ACT_ 表（建表责任=迁移期 owner 账号）；
 *     - 部署中性技术夹具（重复部署幂等、变更部署出新版本）、发起运行实例、留下异步积压。
 *   阶段2 runtime（zhongshu_app 低权限，schema-update=false，异步执行器挂起待 R70 显式启停）：
 *     - 新 JVM 重启接入同库（重启恢复：定义/实例/历史全部可见、schema 版本不变=运行期零 DDL）；
 *     - 通过/拒绝/撤回/转办/分页历史、Spring 事务回滚不留引擎痕迹、积压任务被显式启停的执行器消化且无死信。
 * 结构检查（编排器 psql 直证，不经 Java；全部查询强制成功，失败即 FAIL 不吞错）：
 *   S0 关闭 BPM 结构零残留：System/Infra 基线（全部 Flyway V* 应用后）零 ACT_/FLW_ 表
 *      （运行期装配面证明=既有 V1.6 真实启动联验 + ModuleWhitelistTest 静态门禁，见决策文档边界）；
 *   S1 低权限运行账号无 DDL：app 连接可用（SELECT 1 成功）且 CREATE TABLE 以 SQLSTATE 42501 被拒；
 *   S2 引擎表数量与 schema.version 前后一致（阶段2 未改结构）；
 *   S3 异步回声探针落库。
 * 工具链：优先仓库 tools/（worktree 供给）；否则使用 PATH 上的 java/mvn（CI 已由工作流 provision）；
 *   两者皆缺以退出码 3 明确失败，不静默跳过。套件前置 `mvn install` 依赖模块
 *   （zszj-dependencies BOM + system/infra 及其依赖链，-Dmaven.test.skip=true），保证 BPM 独立
 *   构建消费的是当前提交的产物（BPM 不入 reactor，BOM 不会经 -am 传递，须显式列出）。
 *
 * 语义（对齐 scripts/db/run-db018-verify.mjs / test-pg-fixture.mjs 夹具约定）：
 *   - 实例隔离：容器名随机唯一，验后强制清理（docker rm -f -v，重试后验证不存在）；
 *   - 失败非零：任一阶段/检查失败 → 非零退出；
 *   - 缺依赖不静默跳过：Docker/工具链不可用 → 退出码 3；
 *   - Docker 负载 flaky：容器启动失败先清理残件、换端口重试一次。
 *   - 长 Maven 阶段走异步 spawn：SIGINT/SIGTERM 在事件循环内可达，触发清理后退出
 *     （sync 快操作除外；kill -9 级强杀不可拦截，容器遗留交由环境清理）；
 *     超时经 detached 进程组整树终止（taskkill /T、kill(-pid)）+ 15s 有界兜底返回。
 * 证据报告：JSON 摘要落 outputs/bpm-001/runtime-report.json（outputs/ 不入库）。
 *
 * 用法：node scripts/db/run-bpm001-verify.mjs
 */
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { readFileSync, existsSync, mkdirSync, writeFileSync, readdirSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
const outDir = join(root, 'outputs', 'bpm-001');
const isWin = process.platform === 'win32';

function fail(code, message) { console.error(message); process.exit(code); }

// ---- 工具链解析：tools/ 优先（本地/worktree），否则 PATH（CI 由工作流 provision），皆缺退出码 3 ----
const toolsDir = join(root, 'tools');
const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
let javaHomeEnv, pathPrefix;
if (existsSync(jdkDir) && existsSync(mavenBin)) {
  javaHomeEnv = jdkDir;
  pathPrefix = isWin ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
} else {
  // Windows 下 mvn 是 .cmd，须经 cmd.exe 调起（与后续执行方式一致），否则探测恒 ENOENT 误判缺失
  const probe = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'mvn -v'], { encoding: 'utf8' })
    : spawnSync('mvn', ['-v'], { encoding: 'utf8' });
  const hasJava = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'java -version'], { encoding: 'utf8' })
    : spawnSync('java', ['-version'], { encoding: 'utf8' });
  if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
    fail(3, `[bpm001] 工具链缺失：无 tools/ 供给且 PATH 上无可用 mvn/java（本地请供给 tools/，CI 由 pg-regression.yml provision）`);
  }
  javaHomeEnv = process.env.JAVA_HOME;
  pathPrefix = '';
}
const childEnv = {
  ...process.env,
  ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
  PATH: `${pathPrefix}${process.env.PATH}`,
};

// ---- Docker 预检（缺依赖不静默跳过）----
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) {
  fail(3, `[bpm001] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
}

const container = `zszj-bpm001-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 4332 + Math.floor(Math.random() * 700);
let cleaned = false;
let cleanupError = '';
const cleanup = () => {
  if (cleaned) return;
  for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
    const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
    if (r.status === 0) { cleaned = true; break; }
    cleanupError = (r.stderr ?? '').trim();
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
};
const onSignal = (signal) => { cleanup(); process.exit(signal === 'SIGINT' ? 130 : 143); };
process.on('exit', () => { if (!cleaned) cleanup(); });
process.on('SIGINT', () => onSignal('SIGINT'));
process.on('SIGTERM', () => onSignal('SIGTERM'));

// ---- psql 严格封装：任何查询失败/空结果都必须显式暴露，不吞错 ----
const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
function psqlScalar(user, db, sql) {
  const r = spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql],
    { encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
  if (r.error || r.status !== 0) throw new Error(`psql 查询失败（exit=${r.status ?? 'ERR'}）：${((r.stderr ?? '') || (r.stdout ?? '')).slice(0, 300)}`);
  const value = (r.stdout ?? '').trim();
  if (value === '') throw new Error(`psql 查询返回空：${sql.slice(0, 120)}`);
  return value;
}
function psqlScalarInt(user, db, sql) {
  const value = psqlScalar(user, db, sql);
  const n = Number(value);
  if (!Number.isInteger(n) || n < 0) throw new Error(`期望非负整数，实际「${value}」：${sql.slice(0, 120)}`);
  return n;
}

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

// ---- 长 Maven 阶段：异步 spawn（事件循环保活，SIGINT/SIGTERM 可达并触发清理）----
// Windows 下 Node spawn .cmd 有 EINVAL 防护，须经 cmd.exe；detached 使子进程自成进程组，
// 超时须整树终止（taskkill /T 或 kill(-pid)）：只杀外层 cmd.exe 时孙辈 java 仍持有管道，
// 'close' 事件不会到达，流程将挂死（r2 评审实测复现）
function killTree(child) {
  try {
    if (isWin) spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
    else process.kill(-child.pid, 'SIGKILL');
  } catch { try { child.kill('SIGKILL'); } catch { /* 已退出 */ } }
}
function runAsync(argv, opts = {}) {
  return new Promise((resolve) => {
    let settled = false;
    const child = spawn(argv[0], argv.slice(1), { cwd: core, env: { ...childEnv, ...opts.env }, stdio: ['ignore', 'pipe', 'pipe'], detached: true });
    let output = '';
    const done = (status) => {
      if (settled) return;
      settled = true;
      if (fallbackTimer) clearTimeout(fallbackTimer);
      if (timer) clearTimeout(timer);
      resolve({ status, output });
    };
    child.stdout.on('data', (d) => { output += d; });
    child.stderr.on('data', (d) => { output += d; });
    let timer = null, fallbackTimer = null;
    if (opts.timeoutMs) {
      timer = setTimeout(() => {
        killTree(child);
        // 整树终止兜底：即使个别句柄延迟释放，也在有界时间内返回失败，保证清理可达
        fallbackTimer = setTimeout(() => done(-2), 15 * 1000);
      }, opts.timeoutMs);
    }
    child.on('error', (e) => done(-1));
    child.on('close', (code) => done(code));
  });
}
const mvnArgv = (mvnArgs) => isWin ? ['cmd.exe', '/d', '/s', '/c', 'mvn ' + mvnArgs.join(' ')] : ['mvn', ...mvnArgs];

// ---- 拉起一次性 PG（负载 flaky：清理残件+换端口重试一次）----
function dockerRunOnce() {
  const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm001', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
  const r = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
    : spawnSync('docker', args, { encoding: 'utf8' });
  if (r.status === 0) return true;
  console.error(`[bpm001] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
  return false;
}

const reportXml = (testClass) => join(core, 'zszj-module-bpm', 'target', 'surefire-reports',
  `TEST-cn.zszj.module.bpm.harness.${testClass}.xml`);
const countActFlw = (pattern) => psqlScalarInt('postgres', 'zhongshu',
  `SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND lower(table_name) LIKE '${pattern}'`);
const schemaVersion = () => psqlScalar('postgres', 'zhongshu', "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='schema.version'");

// surefire 严格校验：先删本轮报告（防陈旧文件复用），跑后要求存在、身份匹配、
// 用例数精确、零跳过零失败零错误
function assertSurefire(testClass, expectedTests) {
  const xml = reportXml(testClass);
  rmSync(xml, { force: true });
  return () => {
    if (!existsSync(xml)) throw new Error(`本轮 surefire 报告未生成：${xml}`);
    const text = readFileSync(xml, 'utf8');
    const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
    const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
    if (suiteName !== `cn.zszj.module.bpm.harness.${testClass}`) throw new Error(`报告身份不符：${suiteName}`);
    const tests = Number(attr('tests')), failures = Number(attr('failures')),
      errors = Number(attr('errors')), skipped = Number(attr('skipped'));
    if (tests !== expectedTests) throw new Error(`用例数 ${tests} ≠ 预期 ${expectedTests}`);
    if (skipped !== 0) throw new Error(`存在跳过用例 ${skipped}（必测集不得跳过）`);
    if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
    return `tests=${tests} failures=0 errors=0 skipped=0`;
  };
}

function runHarnessTestArgs(testClass, username, password, schemaUpdate, asyncExecutor) {
  return mvnArgv(['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${testClass}`]);
}
function harnessEnv(username, password, schemaUpdate, asyncExecutor) {
  return {
    ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
    ZSZJ_BPM_HARNESS_USERNAME: username,
    ZSZJ_BPM_HARNESS_PASSWORD: password,
    ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: schemaUpdate,
    ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: asyncExecutor,
  };
}

function finish() {
  cleanup();
  if (!cleaned) record('S4 夹具容器清理', false, `docker rm 重试后仍失败：${cleanupError}`);
  console.log('\n===== ZS-BPM-001 BPM 独立装配与 PG 验收汇总 =====');
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
  console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
  try {
    writeFileSync(join(outDir, 'runtime-report.json'),
      JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
  } catch { /* 报告落盘失败不影响退出码语义 */ }
  process.exit(failCount === 0 ? 0 : 1);
}

(async () => {
  console.log(`[bpm001] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
  if (!dockerRunOnce()) {
    spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
    port = 4332 + Math.floor(Math.random() * 700);
    console.log(`[bpm001] 重试：新端口 ${port}`);
    if (!dockerRunOnce()) fail(3, '[bpm001] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）');
  }

  let ready = false;
  // 就绪探测必须走 TCP（-h 127.0.0.1）：initdb 期间的临时服务器只监听 unix socket，
  // socket 探测可能误判就绪导致 CREATE DATABASE 落到临时库上失败
  for (let i = 0; i < 40; i++) {
    const r = spawnSync('docker', ['exec', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
    if (r.status === 0) { ready = true; break; }
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
  if (!ready) { cleanup(); fail(1, '[bpm001] PG 未就绪'); }

  // ---- 基线库：建库 + 角色授权 + 全部 Flyway 迁移 ----
  {
    let created = false, last = '';
    for (let i = 0; i < 10 && !created; i++) {
      const r = psqlRun('postgres', 'postgres', 'CREATE DATABASE zhongshu;');
      created = r.status === 0;
      if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
    }
    if (!created) { cleanup(); fail(1, `[bpm001] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
  }
  {
    const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
    const r = psqlRun('postgres', 'zhongshu', setup);
    if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 角色授权失败:\n${r.stdout}${r.stderr}`); }
  }
  {
    const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
    const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
    const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
    if (r.status !== 0) { cleanup(); fail(1, `[bpm001] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
    console.log(`[bpm001] 已应用基线迁移 ${migrations.length} 个：${migrations.join(', ')}`);
  }
  if (psqlRun('zhongshu_owner', 'zhongshu',
    'CREATE TABLE bpm_harness_probe(id bigserial PRIMARY KEY, note text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());').status !== 0) {
    cleanup(); fail(1, '[bpm001] 探针表创建失败');
  }

  // ---- 依赖模块产物安装（BPM 独立构建不在 reactor，消费本地仓库产物须来自当前提交；
  //      zszj-dependencies BOM 不会经 -am 传递，必须显式列入）----
  console.log('[bpm001] mvn install 依赖模块（BOM+system/infra 及依赖链，跳过测试编译）…');
  const install = await runAsync(
    mvnArgv(['-B', '-pl', 'zszj-dependencies,zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true']),
    { timeoutMs: 20 * 60 * 1000 });
  if (install.status !== 0) {
    cleanup();
    fail(1, '[bpm001] 依赖模块安装失败：\n' + install.output.split('\n').slice(-25).join('\n'));
  }

  // ---- S0：关闭 BPM 结构零残留——System/Infra 全基线零流程表（查询失败即 FAIL）----
  let actAfterBootstrap = -1, versionAfterBootstrap = '';
  try {
    const act = countActFlw('act\\_%'), flw = countActFlw('flw\\_%');
    record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', act === 0 && flw === 0, `ACT_=${act} FLW_=${flw}`);
  } catch (e) {
    record('S0 关闭BPM结构零残留：基线零 ACT_/FLW_ 表', false, String(e.message ?? e));
  }

  // ---- S1：低权限运行账号连接可用且无 DDL（区分权限拒绝 SQLSTATE 42501 与基础设施错误）----
  // psql 默认 verbosity 不回显 SQLSTATE，须 verbose 才能对 42501 做精确判定（PG15+ 建表被拒为 schema 级）
  {
    let ok = false, note = '';
    try {
      psqlScalar('zhongshu_app', 'zhongshu', 'SELECT 1');
      const r = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_app', '-d', 'zhongshu',
        '-v', 'ON_ERROR_STOP=1', '-q'], { input: '\\set VERBOSITY verbose\nCREATE TABLE zszj_app_should_fail(id int);', encoding: 'utf8' });
      const output = ((r.stderr ?? '') + (r.stdout ?? '')).trim();
      if (r.status === 0) { note = 'app 竟然建表成功'; }
      else if (output.includes('42501')) { ok = true; note = '建表按 SQLSTATE 42501（权限不足）被拒，app 连接本身可用'; }
      else { note = `建表失败但非权限拒绝：${output.slice(0, 160)}`; }
    } catch (e) { note = String(e.message ?? e); }
    record('S1 运行账号无DDL：app 连接可用且建表被 42501 拒绝', ok, note);
  }

  // ---- 阶段1 bootstrap（owner 账号：建表 + 部署 + 发起 + 留积压）----
  console.log('[bpm001] 阶段1 bootstrap：owner 账号建引擎表/部署中性夹具/发起实例/留异步积压…');
  mkdirSync(outDir, { recursive: true });
  const bootAssert = assertSurefire('BpmPgHarnessBootstrapTest', 5);
  const bootLog = await runAsync(
    runHarnessTestArgs('BpmPgHarnessBootstrapTest'),
    { env: harnessEnv('zhongshu_owner', 'owner_local_1', 'true', 'false'), timeoutMs: 30 * 60 * 1000 });
  writeFileSync(join(outDir, 'bootstrap.log'), bootLog.output);
  let bootOk = false, bootNote = '';
  try { bootNote = bootAssert(); bootOk = bootLog.status === 0; if (!bootOk) bootNote += `，但 mvn exit=${bootLog.status}`; }
  catch (e) { bootNote = String(e.message ?? e); }
  record('P1 bootstrap 引擎建表/部署幂等/发起/租户标签/版本钉住', bootOk, bootNote);
  if (!bootOk) {
    console.error('[bpm001] bootstrap 阶段失败，日志见 outputs/bpm-001/bootstrap.log（尾部）：\n' + bootLog.output.split('\n').slice(-40).join('\n'));
    finish();
    return;
  }

  try {
    actAfterBootstrap = countActFlw('act\\_%');
    versionAfterBootstrap = schemaVersion();
  } catch (e) {
    // 引导后结构读取失败必须记 FAIL 再收尾，不得伪绿
    record('P1b 引导后结构读取（表数量/schema.version）', false, String(e.message ?? e));
    finish();
    return;
  }

  // ---- 阶段2 runtime（app 低权限：重启恢复 + 全操作断言 + 零 DDL；执行器由 R70 显式启停）----
  console.log('[bpm001] 阶段2 runtime：app 低权限重启接入同库/审批操作/事务回滚/积压恢复…');
  const runAssert = assertSurefire('BpmPgHarnessRuntimeTest', 9);
  const runLog = await runAsync(
    runHarnessTestArgs('BpmPgHarnessRuntimeTest'),
    { env: harnessEnv('zhongshu_app', 'app_local_1', 'false', 'false'), timeoutMs: 20 * 60 * 1000 });
  writeFileSync(join(outDir, 'runtime.log'), runLog.output);
  let runOk = false, runNote = '';
  try { runNote = runAssert(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
  catch (e) { runNote = String(e.message ?? e); }
  record('P2 runtime 重启恢复/通过/拒绝/撤回/转办/分页历史/事务回滚/积压恢复', runOk, runNote);
  if (!runOk) {
    console.error('[bpm001] runtime 阶段失败，日志见 outputs/bpm-001/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
  }

  // ---- S2/S3：运行期零 DDL（表数量与 schema.version 不变）+ 探针落库 ----
  {
    let ok = false, note = '';
    try {
      const actAfterRuntime = countActFlw('act\\_%');
      const versionAfterRuntime = schemaVersion();
      ok = actAfterBootstrap === actAfterRuntime && versionAfterBootstrap === versionAfterRuntime;
      note = `ACT_ ${actAfterBootstrap}→${actAfterRuntime}, schema.version ${versionAfterBootstrap}→${versionAfterRuntime}`;
    } catch (e) { note = String(e.message ?? e); }
    record('S2 运行期零DDL：引擎表数量与 schema.version 不变', ok, note);
  }
  {
    let ok = false, note = '';
    try {
      const probeRows = psqlScalarInt('postgres', 'zhongshu', 'SELECT count(*) FROM bpm_harness_probe');
      ok = probeRows >= 3;
      note = `探针行=${probeRows}`;
    } catch (e) { note = String(e.message ?? e); }
    record('S3 异步回声探针落库（owner 引导版本 + app 运行期回声）', ok, note);
  }

  finish();
})().catch((e) => { console.error('[bpm001] 编排器异常：' + String(e?.stack ?? e)); cleanup(); process.exit(1); });
