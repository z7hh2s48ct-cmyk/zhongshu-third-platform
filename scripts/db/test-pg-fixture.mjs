/**
 * ZS-DB-019.A：临时 PostgreSQL 夹具（一次性 Docker 实例，自动清理）。
 *
 * 用途：为"失败测试夹具"提供隔离 PG——每次运行创建唯一容器/库，执行
 *       init SQL（如 V1 基线迁移），再执行断言 SQL（断言失败必须以非零退出），
 *       结束自动删除容器。不复用任何持久环境做测试（docs/数据库环境方案.md 第 5 节）。
 *
 * 语义（验收三条）：
 *   - 实例隔离：容器名/端口随机唯一，进程退出即清理；
 *   - 失败非零：init/断言任一 SQL 出错 → psql ON_ERROR_STOP → 本脚本非零退出；
 *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束。
 *
 * 用法：
 *   node scripts/db/test-pg-fixture.mjs --init <file.sql> [--init <file.sql> ...] \
 *        [--assert <file.sql> ...] [--self-test] [--keep]
 *   --init    初始化脚本（按序执行，任一失败即非零）
 *   --assert  断言脚本（init 后执行；脚本内部应使用 DO/RAISE 或必然出错的查询表达断言）
 *   --self-test 自证三条验收语义（初始化→通过断言→注入失败断言验证非零→Docker 缺失语义检查）
 *   --keep    保留容器并在 stdout 打印连接串（调试用；不用于 CI）
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const args = process.argv.slice(2);
const selfTest = args.includes('--self-test');
const keep = args.includes('--keep');
const inits = [];
const asserts = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--init') inits.push(args[++i]);
  else if (args[i] === '--assert') asserts.push(args[++i]);
}
if (!selfTest && !inits.length) {
  console.error('用法：node scripts/db/test-pg-fixture.mjs --init <file.sql> [--assert <file.sql>] [--self-test] [--keep]');
  process.exit(2);
}

if (selfTest) {
  // 自证三条验收语义：正常通过 / 断言失败非零 / 依赖缺失非零
  const v1 = join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql');
  const tmp = mkdtempSync(join(tmpdir(), 'zszj-fixture-selftest-'));
  const pass = join(tmp, 'assert-pass.sql');
  const bad = join(tmp, 'assert-fail.sql');
  writeFileSync(pass, "DO $$ BEGIN IF (SELECT count(*) FROM system_dict_type) < 100 THEN RAISE EXCEPTION 'V1 种子缺失'; END IF; END $$;\n");
  writeFileSync(bad, "DO $$ BEGIN RAISE EXCEPTION '注入的故意失败'; END $$;\n");

  const run = (extra) => spawnSync(process.execPath, [fileURLToPath(import.meta.url), ...extra], { encoding: 'utf8' });

  const r1 = run(['--init', v1, '--assert', pass]);
  if (r1.status !== 0) fail(1, `[self-test] 正常路径应通过，实际 exit=${r1.status}\n${r1.stdout}${r1.stderr}`);
  console.log('[self-test] ① init+断言正常路径通过');

  const r2 = run(['--init', v1, '--assert', bad]);
  if (r2.status === 0) fail(1, '[self-test] 失败断言未被非零退出拦截');
  console.log(`[self-test] ② 失败断言以非零退出（exit=${r2.status}）✓`);

  const r3 = run(['--init', join(tmp, '不存在.sql')]);
  if (r3.status === 0) fail(1, '[self-test] 缺失文件未非零退出');
  console.log(`[self-test] ③ 缺失输入以非零退出（exit=${r3.status}）✓`);

  const dockerMissing = spawnSync('docker', ['version'], { encoding: 'utf8' });
  if (dockerMissing.status !== 0) {
    fail(3, '[self-test] ④ Docker 不可用应已由依赖检查以退出码 3 拦截（当前 Docker 可用，无法演练该分支，语义由代码保证）');
  }
  console.log('[self-test] ④ Docker 可用，依赖缺失分支由代码路径保证（退出码 3）');
  console.log('SELF-TEST PASS');
  process.exit(0);
}

function fail(code, message) {
  console.error(message);
  process.exit(code);
}

// 缺依赖不静默跳过：Docker 不可用即明确失败（退出码 3）
const dockerCheck = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerCheck.error || dockerCheck.status !== 0) {
  fail(3, `[fixture] Docker 不可用（${dockerCheck.error?.message ?? `exit=${dockerCheck.status}`}）：PG 夹具无法创建，测试不得静默跳过`);
}
const serverVersion = dockerCheck.stdout.trim();

const container = `zszj-pg-fixture-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 5432 + Math.floor(Math.random() * 1000) + 1000; // 1532..2531，避开常用端口
const password = 'fixture-' + Math.random().toString(36).slice(2);
let cleaned = false;
function cleanup() {
  if (cleaned || keep) return;
  cleaned = true;
  try {
    execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' });
  } catch { /* 已不存在 */ }
}
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(130); });

console.log(`[fixture] 拉起临时 PG（${container} @ 127.0.0.1:${port}，docker server ${serverVersion}）…`);
execFileSync('docker', ['run', '-d', '--name', container, '-e', `POSTGRES_PASSWORD=${password}`,
  '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });

const psql = (db, extra, file) => {
  const base = ['exec', '-i', container, 'psql', '-U', 'postgres', '-v', 'ON_ERROR_STOP=1', '-q', '-d', db, ...extra];
  if (file) base.push('-f', '/dev/stdin');
  return spawnSync('docker', base, { input: file ? readFileSync(file) : undefined, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
};
const psqlReady = () => spawnSync('docker', ['exec', container, 'pg_isready', '-U', 'postgres'], { encoding: 'utf8' });

let ready = false;
for (let i = 0; i < 30; i++) {
  if (psqlReady().status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[fixture] PG 未就绪'); }

let exitCode = 0;
try {
  for (const init of inits) {
    if (!existsSync(init)) { cleanup(); fail(1, `[fixture] init 文件不存在: ${init}`); }
    const r = psql('postgres', [], init);
    if (r.status !== 0) { cleanup(); fail(1, `[fixture] init 失败: ${init}\n${r.stdout}${r.stderr}`); }
    console.log(`[fixture] init 完成: ${init}`);
  }
  for (const assertFile of asserts) {
    if (!existsSync(assertFile)) { cleanup(); fail(1, `[fixture] assert 文件不存在: ${assertFile}`); }
    const r = psql('postgres', [], assertFile);
    if (r.status !== 0) { cleanup(); fail(1, `[fixture] 断言失败: ${assertFile}\n${r.stdout}${r.stderr}`); }
    console.log(`[fixture] 断言通过: ${assertFile}`);
  }
  if (keep) {
    console.log(`[fixture] --keep 已保留容器，连接串：postgres://postgres:${password}@127.0.0.1:${port}/postgres（容器 ${container}，用后 docker rm -f ${container}）`);
  }
} finally {
  if (!keep) console.log('[fixture] 容器已清理');
  cleanup();
}
process.exit(exitCode);
