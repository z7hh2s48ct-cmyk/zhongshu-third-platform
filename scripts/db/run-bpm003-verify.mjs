/**
 * ZS-BPM-003：首链领域状态与幂等写回 PG 验收编排器（真实 Flowable 引擎 + 真实 PostgreSQL，B09）。
 *
 * 单阶段语义（一次性 PG 容器，owner 直连，引擎 schema-update=true 建表，执行器挂起）：
 *   - 基线：建库 + 角色授权 + 全部 Flyway V* 迁移（含 V20260928.001 首链两表）；
 *   - 依赖：BPM 独立构建不在 reactor，前置 `mvn install`（zszj-dependencies BOM + system/infra 及
 *     依赖链，-Dmaven.test.skip=true），保证消费当前提交产物；
 *   - 验证：BpmFirstChainPgRuntimeTest —— 提交/通过/拒绝/撤回/转派四操作与业务状态一致、
 *     重复回调幂等吸收、旧流程晚到弃单留痕、并发审批/撤回恰一方生效（真实双线程门竞争）、
 *     审计与弃单落真实 PG 可回查。
 * 工具链：优先仓库 tools/（worktree 供给）；否则 PATH（CI provision）；皆缺退出码 3。
 * 语义对齐 run-bpm001-verify.mjs：容器随机隔离、验后强制清理、失败非零、负载 flaky 重试一次、
 * TCP+PGPASSWORD 就绪探测（FLAKY-2）、surefire 报告身份/用例数严格校验。
 *
 * 用法：node scripts/db/run-bpm003-verify.mjs
 */
import { spawn, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const migrationDir = join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration');
const outDir = join(root, 'outputs', 'bpm-003');
const isWin = process.platform === 'win32';
const TEST_CLASS = 'BpmFirstChainPgRuntimeTest';
const TEST_FQN = `cn.zszj.module.bpm.firstchain.${TEST_CLASS}`;
const EXPECTED_TESTS = 7;

function fail(code, message) { console.error(message); process.exit(code); }

const toolsDir = join(root, 'tools');
const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
let javaHomeEnv, pathPrefix;
if (existsSync(jdkDir) && existsSync(mavenBin)) {
  javaHomeEnv = jdkDir;
  pathPrefix = isWin ? `${jdkDir}\\bin;${mavenBin};` : `${jdkDir}/bin:${mavenBin}:`;
} else {
  const probe = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'mvn -v'], { encoding: 'utf8' })
    : spawnSync('mvn', ['-v'], { encoding: 'utf8' });
  const hasJava = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'java -version'], { encoding: 'utf8' })
    : spawnSync('java', ['-version'], { encoding: 'utf8' });
  if (probe.error || probe.status !== 0 || hasJava.error || hasJava.status !== 0) {
    fail(3, '[bpm003] 工具链缺失：无 tools/ 供给且 PATH 上无可用 mvn/java');
  }
  javaHomeEnv = process.env.JAVA_HOME;
  pathPrefix = '';
}
const childEnv = {
  ...process.env,
  ...(javaHomeEnv ? { JAVA_HOME: javaHomeEnv } : {}),
  PATH: `${pathPrefix}${process.env.PATH}`,
};

const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) {
  fail(3, `[bpm003] Docker 不可用（${dockerUp.error?.message ?? `exit=${dockerUp.status}`}）：验证不得静默跳过`);
}

const container = `zszj-bpm003-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
let port = 5032 + Math.floor(Math.random() * 700);
let cleaned = false;
let cleanupError = '';
let currentChild = null;
const cleanup = () => {
  if (cleaned) return;
  for (let attempt = 0; attempt < 3 && !cleaned; attempt++) {
    const r = spawnSync('docker', ['rm', '-f', '-v', container], { encoding: 'utf8' });
    if (r.status === 0) { cleaned = true; break; }
    cleanupError = (r.stderr ?? '').trim();
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
};
process.on('exit', () => { if (!cleaned) cleanup(); });
process.on('SIGINT', () => { try { if (currentChild) killTree(currentChild); } catch { /* 已退出 */ } cleanup(); process.exit(130); });
process.on('SIGTERM', () => { try { if (currentChild) killTree(currentChild); } catch { /* 已退出 */ } cleanup(); process.exit(143); });

const psqlRun = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db,
  '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

function killTree(child) {
  try {
    if (isWin) {
      for (let attempt = 0; attempt < 2; attempt++) {
        const r = spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore', timeout: 5000, killSignal: 'SIGKILL' });
        if (!r.error && r.status === 0) return true;
      }
      try { child.kill('SIGKILL'); } catch { /* 已退出 */ }
      return false;
    }
    process.kill(-child.pid, 'SIGKILL');
    return true;
  } catch { try { child.kill('SIGKILL'); } catch { /* 已退出 */ } return false; }
}
function runAsync(argv, opts = {}) {
  return new Promise((resolve) => {
    let settled = false;
    const child = spawn(argv[0], argv.slice(1), { cwd: core, env: { ...childEnv, ...opts.env }, stdio: ['ignore', 'pipe', 'pipe'], detached: true });
    currentChild = child;
    let output = '';
    const done = (status) => {
      if (settled) return;
      settled = true;
      if (currentChild === child) currentChild = null;
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
        fallbackTimer = setTimeout(() => done(-2), 15 * 1000);
      }, opts.timeoutMs);
    }
    child.on('error', (e) => done(-1));
    child.on('close', (code) => done(code));
  });
}
const mvnArgv = (mvnArgs) => isWin ? ['cmd.exe', '/d', '/s', '/c', 'mvn ' + mvnArgs.join(' ')] : ['mvn', ...mvnArgs];

function dockerRunOnce() {
  const args = ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=bpm003', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'];
  const r = isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'docker ' + args.join(' ')], { encoding: 'utf8' })
    : spawnSync('docker', args, { encoding: 'utf8' });
  if (r.status === 0) return true;
  console.error(`[bpm003] docker run 失败（port=${port}）：${((r.stderr ?? '') || '').trim().slice(0, 200)}`);
  try { isWin ? spawnSync('cmd.exe', ['/d', '/s', '/c', `docker rm -f ${container}`], { stdio: 'ignore' }) : spawnSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
  return false;
}

const reportXml = () => join(core, 'zszj-module-bpm', 'target', 'surefire-reports', `TEST-${TEST_FQN}.xml`);
function assertSurefire() {
  const xml = reportXml();
  rmSync(xml, { force: true });
  return () => {
    if (!existsSync(xml)) throw new Error(`本轮 surefire 报告未生成：${xml}`);
    const text = readFileSync(xml, 'utf8');
    const attr = (name) => (text.match(new RegExp(`${name}="(\\d+)"`)) ?? [])[1];
    const suiteName = (text.match(/name="([^"]+)"/) ?? [])[1];
    if (suiteName !== TEST_FQN) throw new Error(`报告身份不符：${suiteName}`);
    const tests = Number(attr('tests')), failures = Number(attr('failures')),
      errors = Number(attr('errors')), skipped = Number(attr('skipped'));
    if (tests !== EXPECTED_TESTS) throw new Error(`用例数 ${tests} ≠ 预期 ${EXPECTED_TESTS}`);
    if (skipped !== 0) throw new Error(`存在跳过用例 ${skipped}（必测集不得跳过）`);
    if (failures !== 0 || errors !== 0) throw new Error(`failures=${failures} errors=${errors}`);
    return `tests=${tests} failures=0 errors=0 skipped=0`;
  };
}

function finish() {
  cleanup();
  if (!cleaned) record('S2 夹具容器清理', false, `docker rm 重试后仍失败：${cleanupError}`);
  console.log('\n===== ZS-BPM-003 首链领域状态与幂等写回 PG 验收汇总 =====');
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
  console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
  try {
    writeFileSync(join(outDir, 'runtime-report.json'),
      JSON.stringify({ finishedAt: new Date().toISOString(), container, results, pass, failCount }, null, 2));
  } catch { /* 报告落盘失败不影响退出码语义 */ }
  process.exit(failCount === 0 ? 0 : 1);
}

(async () => {
  console.log(`[bpm003] 拉起临时 PG（${container} @ 127.0.0.1:${port}）…`);
  if (!dockerRunOnce()) {
    spawnSync('docker', ['rm', '-f', '-v', container], { stdio: 'ignore' });
    port = 5032 + Math.floor(Math.random() * 700);
    console.log(`[bpm003] 重试：新端口 ${port}`);
    if (!dockerRunOnce()) fail(3, '[bpm003] PG 夹具容器两次启动失败');
  }

  let ready = false;
  for (let i = 0; i < 40; i++) {
    const r = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=bpm003', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
    if (r.status === 0) { ready = true; break; }
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
  if (!ready) { cleanup(); fail(1, '[bpm003] PG 未就绪'); }

  {
    let created = false, last = '';
    for (let i = 0; i < 10 && !created; i++) {
      const r = spawnSync('docker', ['exec', '-i', '-e', 'PGPASSWORD=bpm003', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres', '-v', 'ON_ERROR_STOP=1', '-q'], { input: 'CREATE DATABASE zhongshu;', encoding: 'utf8' });
      created = r.status === 0;
      if (!created) { last = ((r.stderr ?? '') + (r.stdout ?? '')).trim(); Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000); }
    }
    if (!created) { cleanup(); fail(1, `[bpm003] 建库失败（重试 10 次）：${last.slice(0, 400)}`); }
  }
  {
    const setup = readFileSync(join(core, 'sql', 'postgresql', 'env-setup-test.sql'), 'utf8');
    const r = psqlRun('postgres', 'zhongshu', setup);
    if (r.status !== 0) { cleanup(); fail(1, `[bpm003] 角色授权失败:\n${r.stdout}${r.stderr}`); }
  }
  {
    const migrations = readdirSync(migrationDir).filter((f) => /^V.*\.sql$/.test(f)).sort();
    const bundled = migrations.map((f) => readFileSync(join(migrationDir, f), 'utf8')).join('\n');
    const r = psqlRun('zhongshu_owner', 'zhongshu', bundled);
    if (r.status !== 0) { cleanup(); fail(1, `[bpm003] 基线迁移执行失败（${migrations.length} 个 V*）:\n${r.stdout}${r.stderr}`); }
    console.log(`[bpm003] 已应用基线迁移 ${migrations.length} 个（含 V20260928.001 首链两表）`);
  }

  console.log('[bpm003] mvn install 依赖模块（BOM+system/infra 及依赖链，跳过测试编译）…');
  const install = await runAsync(
    mvnArgv(['-B', '-pl', 'zszj-dependencies,zszj-module-system,zszj-module-infra', '-am', 'install', '-Dmaven.test.skip=true']),
    { timeoutMs: 20 * 60 * 1000 });
  if (install.status !== 0) {
    cleanup();
    fail(1, '[bpm003] 依赖模块安装失败：\n' + install.output.split('\n').slice(-25).join('\n'));
  }

  console.log(`[bpm003] 运行 ${TEST_CLASS}（真实 Flowable 引擎 + PG，单阶段 schema-update=true / 执行器挂起）…`);
  mkdirSync(outDir, { recursive: true });
  const assertReport = assertSurefire();
  const runLog = await runAsync(
    mvnArgv(['-B', '-f', 'zszj-module-bpm/pom.xml', 'test', `-Dtest=${TEST_CLASS}`]),
    {
      timeoutMs: 20 * 60 * 1000,
      env: {
        ZSZJ_BPM_HARNESS_JDBC_URL: `jdbc:postgresql://127.0.0.1:${port}/zhongshu`,
        ZSZJ_BPM_HARNESS_USERNAME: 'postgres',
        ZSZJ_BPM_HARNESS_PASSWORD: 'bpm003',
        ZSZJ_BPM_HARNESS_SCHEMA_UPDATE: 'true',
        ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR: 'false',
      },
    });
  writeFileSync(join(outDir, 'runtime.log'), runLog.output);
  let runOk = false, runNote = '';
  try { runNote = assertReport(); runOk = runLog.status === 0; if (!runOk) runNote += `，但 mvn exit=${runLog.status}`; }
  catch (e) { runNote = String(e.message ?? e); }
  record(`P1 首链四操作一致 + 幂等/晚到/并发门（真实引擎 ${EXPECTED_TESTS} 例）`, runOk, runNote);
  if (!runOk) {
    console.error('[bpm003] 运行阶段失败，日志见 outputs/bpm-003/runtime.log（尾部）：\n' + runLog.output.split('\n').slice(-40).join('\n'));
  }

  finish();
})().catch((e) => { console.error('[bpm003] 编排器异常：' + String(e?.stack ?? e)); cleanup(); process.exit(1); });
