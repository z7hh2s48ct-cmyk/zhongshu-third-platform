/**
 * ZS-BRAND-004.B 运行期隔离证明：一次性 Docker Redis + PG 夹具拉起真实 zszj-server，
 * 以账密登录（LOGIN-004 门控下账密默认可用）驱动会话/权限缓存/限流键生命周期，
 * 断言命名空间无旧名、租户后缀正确（TenantRedisCacheManager 语义）、
 * 退出/撤权清理或墓碑化（LOGIN-003 合同）、并发刷新不产生双键并存绕过（LOGIN-002 合同）、
 * 构造旧名前缀凭据串调受保护 API 必须 401（旧键不可用）。
 *
 * 用法：node scripts/brand/run-brand-004b-runtime.mjs [--skip-build] [--keep]
 *   --skip-build  复用既有 zszj-server/target/zszj-server.jar（调试用；默认先 mvn package）
 *   --keep        保留容器/进程供排查（不用于 CI；正常退出码仍按断言结果）
 *
 * 语义（对齐 scripts/db/test-pg-fixture.mjs 夹具约定）：
 *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理；
 *   - 失败非零：任一断言失败 → 非零退出；
 *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束。
 *
 * 证据报告：JSON 输出 stdout，并落 outputs/brand-004b/runtime-report.json（不入库）。
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { existsSync, mkdirSync, openSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const args = process.argv.slice(2);
const skipBuild = args.includes('--skip-build');
const keep = args.includes('--keep');
const outDir = join(root, 'outputs', 'brand-004b');
mkdirSync(outDir, { recursive: true });

const OLD_NAME_RE = /(yudao|ruoyi|iocoder|youdao|芋道|unibest)/i; // 键空间旧名判定（与静态门禁同口径）
const TENANT_ID = 1; // V1 基线种子租户「众墅之家」
const ADMIN_ID = 900001;
const USER_ID = 900002;
const PASSWORD = 'admin123'; // 与基线种子同源的演示口令（仅一次性夹具，不入任何持久环境）
const BCRYPT_HASH = '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG'; // sql/postgresql 种子 admin123 哈希
const MIGRATIONS = [
  'V20260909.001__system_infra_baseline.sql',
  'V20260909.002__infra_quartz_backfill.sql',
  'V20260909.003__system_dict_unique_constraints.sql',
  'V20260913.001__infra_config_optimistic_version.sql',
];

// ---------------------------------------------------------------------------
// 断言与报告
// ---------------------------------------------------------------------------
const checks = [];
function check(id, name, pass, detail) {
  checks.push({ id, name, pass: !!pass, detail: detail ?? '' });
  console.log(`  [${pass ? 'PASS' : 'FAIL'}] ${id} ${name}${pass ? '' : ` — ${detail ?? ''}`}`);
  return !!pass;
}
function fatal(id, name, detail) {
  check(id, name, false, detail);
  cleanup();
  throw new Error(`致命失败：${name} ${detail ?? ''}`);
}

// ---------------------------------------------------------------------------
// 基础设施（清理状态与资源标识先于 Docker 依赖检查初始化，保证缺 Docker 时仍能以退出码 3 干净退出）
// ---------------------------------------------------------------------------
const stamp = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const pgContainer = `zszj-brand004b-pg-${stamp}`;
const redisContainer = `zszj-brand004b-redis-${stamp}`;
const pgPort = 15432 + Math.floor(Math.random() * 1000); // 15432..16431
const redisPort = 17000 + Math.floor(Math.random() * 2000); // 17000..18999
const serverPort = 28000 + Math.floor(Math.random() * 1600); // 28000..29599，避开并行会话常用端口
const pgPassword = 'fixture-' + Math.random().toString(36).slice(2);
const dbName = 'zszj_brand004b';

let serverProcess = null;
let serverLogFd = null;
let cleaned = false;
function cleanup() {
  if (cleaned) return;
  cleaned = true;
  if (serverProcess?.pid && !keep) {
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/PID', String(serverProcess.pid), '/T', '/F'], { stdio: 'ignore' });
    } else {
      try { serverProcess.kill('SIGTERM'); } catch { /* 已退出 */ }
    }
  }
  if (!keep) {
    for (const c of [pgContainer, redisContainer]) {
      try { execFileSync('docker', ['rm', '-f', c], { stdio: 'ignore' }); } catch { /* 已不存在 */ }
    }
  } else {
    console.error(`[runtime] --keep：容器 ${pgContainer} / ${redisContainer} 与日志保留供排查`);
  }
}
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(130); });

const dockerVersion = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerVersion.error || dockerVersion.status !== 0) {
  fail(3, `[runtime] Docker 不可用（${dockerVersion.error?.message ?? `exit=${dockerVersion.status}`}）：运行期证明无法在共享环境执行，不得静默跳过`);
}
console.log(`[runtime] docker server ${dockerVersion.stdout.trim()}`);

function dockerRunRetry(image, name, runArgs) {
  let lastErr = '';
  for (let attempt = 1; attempt <= 2; attempt++) { // Docker 负载 flaky 约定：失败重试一次后再判定
    const r = spawnSync('docker', ['run', '-d', '--name', name, ...runArgs, image], { encoding: 'utf8' });
    if (r.status === 0) return true;
    lastErr = (r.stderr || r.stdout || '').trim();
    try { execFileSync('docker', ['rm', '-f', name], { stdio: 'ignore' }); } catch { /* ignore */ }
    console.error(`[runtime] 容器 ${name} 第 ${attempt} 次启动失败：${lastErr}`);
  }
  return false;
}
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// Redis 助手（容器内 redis-cli，键空间即被测命名空间）
const rcli = (...cliArgs) => {
  const r = spawnSync('docker', ['exec', redisContainer, 'redis-cli', '--raw', ...cliArgs], { encoding: 'utf8' });
  return r.status === 0 ? (r.stdout ?? '').replace(/\r?\n$/, '') : null;
};
const redisKeys = (pattern) => (rcli('KEYS', pattern) ?? '').split('\n').filter(Boolean);
const redisGet = (key) => { const v = rcli('GET', key); return v === null || v === '' ? null : v; }; // redis-cli --raw 对 nil 输出空串
const redisInfo = () => rcli('INFO', 'server') ?? '';
const redisVersion = () => redisInfo().match(/redis_version:([^\r\n]+)/)?.[1] ?? '';
const redisSet = (key, value) => rcli('SET', key, value);

// HTTP 助手（真实进程 HTTP 面）
const BASE = `http://127.0.0.1:${serverPort}`;
async function api(method, path, { token, body, raw } = {}) {
  const headers = { 'tenant-id': String(TENANT_ID), 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20000),
  });
  const text = await res.text();
  if (raw) return { status: res.status, text };
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非 JSON（如 401 HTML/空体） */ }
  return { status: res.status, body: json, text };
}

// ---------------------------------------------------------------------------
// 步骤 1：构建 + 拉起容器 + 初始化库 + 启动真实 server
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 1/7 构建与夹具拉起');
const toolsDir = join(root, 'tools');
const jdkDir = join(toolsDir, 'jdk-17.0.20.1+1');
const mavenBin = join(toolsDir, 'apache-maven-3.9.9', 'bin');
if (!existsSync(jdkDir) || !existsSync(mavenBin)) fatal('A0', '工具链就绪', `缺 ${jdkDir} 或 ${mavenBin}（worktree tools 供给）`);
const childEnv = {
  ...process.env,
  JAVA_HOME: jdkDir,
  PATH: process.platform === 'win32' ? `${jdkDir}\\bin;${mavenBin};${process.env.PATH}` : `${jdkDir}/bin:${mavenBin}:${process.env.PATH}`,
};
const javaExe = process.platform === 'win32' ? join(jdkDir, 'bin', 'java.exe') : join(jdkDir, 'bin', 'java');

const jarPath = join(core, 'zszj-server', 'target', 'zszj-server.jar');
if (!skipBuild || !existsSync(jarPath)) {
  console.log('[runtime] mvn package zszj-server（首次约数分钟，日志见 outputs/brand-004b/build.log）…');
  const buildLog = openSync(join(outDir, 'build.log'), 'w');
  // Node 对 .cmd 的 spawnSync 有 EINVAL 防护，须经 cmd.exe；命令串固定，无引号注入面
  const buildCmd = process.platform === 'win32'
    ? ['cmd.exe', ['/d', '/s', '/c', 'mvn -B -pl zszj-server -am package -DskipTests']]
    : ['mvn', ['-B', '-pl', 'zszj-server', '-am', 'package', '-DskipTests']];
  const build = spawnSync(buildCmd[0], buildCmd[1],
    { cwd: core, env: childEnv, stdio: ['ignore', buildLog, buildLog], timeout: 15 * 60 * 1000 });
  if (build.status !== 0 || !existsSync(jarPath)) {
    fatal('A1', 'zszj-server 构建', `exit=${build.status}${build.error ? ` ${build.error.message}` : ''}（详见 outputs/brand-004b/build.log）`);
  }
}

if (!dockerRunRetry('postgres:17-alpine', pgContainer, ['-e', `POSTGRES_PASSWORD=${pgPassword}`, '-p', `127.0.0.1:${pgPort}:5432`])) {
  fail(3, `[runtime] PG 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）`);
}
if (!dockerRunRetry('redis:7.4.11-alpine', redisContainer, ['-p', `127.0.0.1:${redisPort}:6379`])) {
  fail(3, `[runtime] Redis 夹具容器两次启动失败（Docker 负载竞态按约定重试一次后仍失败）`);
}

const psql = (sql, db = 'postgres') => {
  const r = spawnSync('docker', ['exec', '-i', pgContainer, 'psql', '-U', 'postgres', '-v', 'ON_ERROR_STOP=1', '-q', '-d', db],
    { input: sql, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  if (r.status !== 0) fatal('A2', '夹具 SQL 执行', (r.stderr || r.stdout || '').slice(0, 500));
};
let ready = false;
for (let i = 0; i < 60 && !ready; i++) {
  ready = spawnSync('docker', ['exec', pgContainer, 'pg_isready', '-U', 'postgres'], { encoding: 'utf8' }).status === 0;
  if (!ready) await sleep(500);
}
if (!ready) fatal('A2', 'PG 就绪', 'pg_isready 30s 未就绪');
for (let i = 0; i < 30; i++) {
  if (rcli('PING') === 'PONG') break;
  await sleep(500);
}
psql(`CREATE DATABASE ${dbName};`);
psql(MIGRATIONS.map((f) => readFileSync(join(core, 'zszj-server', 'src', 'main', 'resources', 'db', 'migration', f), 'utf8')).join('\n\n'), dbName);
psql(`
INSERT INTO system_users (id, username, password, nickname, dept_id, post_ids, email, mobile, sex, status, creator, updater, deleted, tenant_id) VALUES
  (${ADMIN_ID}, 'brand004badmin', '${BCRYPT_HASH}', '品牌联验管理员', 103, '[4]', 'brand004b_admin@fixture.zszj.invalid', '17900000001', 1, 0, '1', '1', 0, ${TENANT_ID}),
  (${USER_ID},  'brand004buser',  '${BCRYPT_HASH}', '品牌联验普通用户', 103, '[4]', 'brand004b_user@fixture.zszj.invalid',  '17900000002', 1, 0, '1', '1', 0, ${TENANT_ID});
INSERT INTO system_user_role (id, user_id, role_id, creator, updater, deleted, tenant_id) VALUES
  (900001, ${ADMIN_ID}, 1, '1', '1', 0, ${TENANT_ID}),
  (900002, ${USER_ID}, 2, '1', '1', 0, ${TENANT_ID});
`, dbName);
const pgVerRow = spawnSync('docker', ['exec', pgContainer, 'psql', '-U', 'postgres', '-t', '-A', '-c', 'SHOW server_version;'], { encoding: 'utf8' });

console.log(`[runtime] 启动 zszj-server @ 127.0.0.1:${serverPort}（PG ${pgPort} / Redis ${redisPort}，mock-enable=false）…`);
serverLogFd = openSync(join(outDir, 'server.log'), 'w');
serverProcess = spawn(javaExe, [
  '-jar', jarPath,
  '--spring.profiles.active=local',
  `--server.port=${serverPort}`,
  `--spring.datasource.dynamic.datasource.master.url=jdbc:postgresql://127.0.0.1:${pgPort}/${dbName}`,
  '--spring.datasource.dynamic.datasource.master.username=postgres',
  `--spring.datasource.dynamic.datasource.master.password=${pgPassword}`,
  '--spring.data.redis.host=127.0.0.1',
  `--spring.data.redis.port=${redisPort}`,
  '--zszj.security.mock-enable=false',
  '--zszj.captcha.enable=false',
  '--spring.flyway.enabled=false', // 迁移已按 V1 基线序经 psql 施行（与 PG 回归同路径）
  // ZS-DB-001.B 已登记缺陷的夹具级规避：Druid PSCache + PG 游标查询（MyBatis-Plus selectWithCursor）
  // 在重复执行同一语句时报「statement 已经被关闭」，显式关闭 PSCache（不改产品 yaml 语义）
  '--spring.datasource.dynamic.druid.pool-prepared-statements=false',
  '--spring.datasource.dynamic.druid.max-pool-prepared-statement-per-connection-size=-1',
], { env: childEnv, stdio: ['ignore', serverLogFd, serverLogFd], windowsHide: true });

const serverLogPath = join(outDir, 'server.log');
let up = false;
for (let i = 0; i < 160 && !up; i++) {
  await sleep(1500);
  if (serverProcess.exitCode !== null || serverProcess.signalCode) break; // 进程已退出
  try {
    const res = await fetch(`${BASE}/actuator/health`, { signal: AbortSignal.timeout(3000) });
    const text = await res.text();
    if (res.status === 200 && text.includes('UP')) up = true;
  } catch { /* 尚未就绪 */ }
}
if (!up) {
  const tail = existsSync(serverLogPath) ? readFileSync(serverLogPath, 'utf8').slice(-4000) : '(无日志)';
  fatal('A3', 'zszj-server 启动', `健康检查未通过。\n--- server.log 尾部 ---\n${tail}`);
}

// ---------------------------------------------------------------------------
// 步骤 2：基线键空间 + 登录建会话（新命名空间 + 值完整性 + 代际键）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 2/7 基线键空间与账密登录');
check('B0', '基线键空间无旧名键', redisKeys('*').every((k) => !OLD_NAME_RE.test(k)), redisKeys('*').filter((k) => OLD_NAME_RE.test(k)).join(', '));

const loginAdmin = await api('POST', '/admin-api/system/auth/login', { body: { username: 'brand004badmin', password: PASSWORD } });
if (loginAdmin.body?.code !== 0) fatal('B1', '账密登录（管理员）', `HTTP ${loginAdmin.status} body=${loginAdmin.text.slice(0, 300)}`);
const adminToken = loginAdmin.body.data.accessToken;
const adminRefresh = loginAdmin.body.data.refreshToken;
check('B1', '账密登录签发令牌（LOGIN-004 门控下账密可用）', !!adminToken && !!adminRefresh);

const accessKey = (t) => `oauth2_access_token:${t}`;
const tombKey = (t) => `oauth2_access_token_revoke_tomb:${t}`;
const refreshGenKey = (rt) => `oauth2_refresh_session_generation:${rt}`;
const accessGenKey = (t) => `oauth2_access_session_generation:${t}`;

const adminCached = redisGet(accessKey(adminToken));
check('B2', '访问令牌落新命名空间键（oauth2_access_token:%s）', adminCached !== null && adminCached !== undefined && adminCached !== '');
let adminTokenDo = null;
try { adminTokenDo = JSON.parse(adminCached); } catch { /* 值非 JSON */ }
check('B3', '新命名空间键值主体/租户正确（userId=900001, tenantId=1）',
  adminTokenDo?.userId === ADMIN_ID && adminTokenDo?.tenantId === TENANT_ID, `value=${String(adminCached).slice(0, 200)}`);
check('B4', 'LOGIN-002 会话代际键合同（登录未刷新前不登记，代际自首次刷新起 INCR）', redisGet(refreshGenKey(adminRefresh)) === null,
  `value=${redisGet(refreshGenKey(adminRefresh))}`);
check('B5', 'LOGIN-002 访问令牌代际键合同（登录未刷新前不登记）', redisGet(accessGenKey(adminToken)) === null, `value=${redisGet(accessGenKey(adminToken))}`);
check('B6', 'SEC-010 限流键在新命名空间（rate_limiter:%s）', redisKeys('rate_limiter:*').length > 0, redisKeys('rate_limiter:*').join(', '));

// ---------------------------------------------------------------------------
// 步骤 3：权限缓存键与租户后缀（TenantRedisCacheManager：name:tenantId:key）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 3/7 权限缓存键与租户后缀');
const perm = await api('GET', '/admin-api/system/auth/get-permission-info', { token: adminToken });
check('C1', 'get-permission-info 可用（会话在新命名空间工作）', perm.body?.code === 0, `HTTP ${perm.status}`);
await api('GET', '/admin-api/system/user/page?pageNo=1&pageSize=1', { token: adminToken }); // 触发 hasPermission → user_role_ids/role 缓存

const userRoleKeys = redisKeys('user_role_ids:*');
check('C2', '豁免租户后缀缓存键形态正确（user_role_ids:userId，无租户段）',
  userRoleKeys.includes(`user_role_ids:${ADMIN_ID}`) && userRoleKeys.every((k) => /^user_role_ids:\d+$/.test(k)),
  userRoleKeys.join(', '));
const roleKeys = redisKeys('role:*');
check('C3', '租户感知缓存键带租户后缀（role:1:roleId）', roleKeys.includes('role:1:1'), roleKeys.join(', ') || '(空)');
check('C4', '租户感知缓存键无「缺租户段」形态', roleKeys.every((k) => /^role:\d+:\d+$/.test(k)), roleKeys.join(', '));
const oauthClientKeys = redisKeys('oauth_client:*');
check('C5', 'OAuth2 客户端缓存键形态正确（oauth_client:clientId，clientId 为字符串标识）', oauthClientKeys.every((k) => /^oauth_client:[A-Za-z0-9_-]+$/.test(k)), oauthClientKeys.join(', '));

// ---------------------------------------------------------------------------
// 步骤 4：并发刷新（LOGIN-002 行锁+代际 → 不产生双键并存绕过）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 4/7 并发刷新不绕过');
const loginUser = await api('POST', '/admin-api/system/auth/login', { body: { username: 'brand004buser', password: PASSWORD } });
if (loginUser.body?.code !== 0) fatal('D0', '账密登录（普通用户）', `HTTP ${loginUser.status} body=${loginUser.text.slice(0, 300)}`);
const userTokenGen1 = loginUser.body.data.accessToken;
const userRefresh = loginUser.body.data.refreshToken;

const burst = await Promise.all([1, 2, 3].map(() => api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${encodeURIComponent(userRefresh)}`)));
const okRefreshes = burst.filter((r) => r.body?.code === 0);
check('D1', '3 路并发刷新全部成功（行锁串行化，不互相失败）', okRefreshes.length === 3, burst.map((r) => `code=${r.body?.code}`).join(' '));
const genAfter = redisGet(refreshGenKey(userRefresh));
check('D2', '会话代际号 == 成功刷新次数（无丢失更新）', genAfter === '3', `generation=${genAfter}`);
const issuedTokens = [userTokenGen1, ...okRefreshes.map((r) => r.body.data.accessToken)];
const uniqueTokens = [...new Set(issuedTokens)];
check('D3', '刷新沿用同一刷新令牌（不轮换，前端契约不破）', okRefreshes.every((r) => r.body.data.refreshToken === userRefresh),
  `refreshTokens=${[...new Set(okRefreshes.map((r) => r.body?.data?.refreshToken))].join(',') || '(空)'}`);

// 并发完成序 ≠ 服务端提交序：以访问令牌代际键定位真实最高代际（LOGIN-002 合同：代际键即权威标识）
const genOf = (t) => redisGet(accessGenKey(t));
const topGenTokens = uniqueTokens.filter((t) => genOf(t) === genAfter);
check('D4', '恰有一个访问令牌携带最高代际（代际键权威定位）', topGenTokens.length === 1,
  uniqueTokens.map((t) => `${t.slice(0, 8)}=${genOf(t)}`).join(' '));
const newestToken = topGenTokens[0];
const alive = [];
const rejected = [];
for (const t of uniqueTokens) {
  const r = await api('GET', '/admin-api/system/auth/get-permission-info', { token: t });
  (r.body?.code === 0 ? alive : rejected).push(t);
}
check('D5', '同一会话恰好 1 个有效访问令牌（无双键并存绕过形态）', alive.length === 1 && alive[0] === newestToken,
  `alive=${alive.length} newest=${newestToken?.slice(0, 8)} rejected=${rejected.length}`);

// ---------------------------------------------------------------------------
// 步骤 5：管理员撤权（LOGIN-003 用户级撤销：清理 + 墓碑 + 无旧键残留复活）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 5/7 管理员撤权不可绕过');
const revoke = await api('PUT', '/admin-api/system/user/update-status', { token: adminToken, body: { id: USER_ID, status: 1 } });
check('E1', '管理员禁用用户（update-status）成功', revoke.body?.code === 0, `HTTP ${revoke.status} body=${revoke.text.slice(0, 200)}`);
await sleep(300); // 事务提交后缓存失效异步落地
// E2 的 401 请求会触发 checkAccessToken 自愈（回源核验→落墓碑+清缓存），故先于该请求取证 Redis 原子状态，
// 避免「撤销时未清键」被自愈修复掩盖
check('E3', '撤权后访问令牌键被清理（自愈请求前取证，无旧键残留）', !redisKeys('oauth2_access_token:*').some((k) => uniqueTokens.some((t2) => k === accessKey(t2))),
  redisKeys('oauth2_access_token:*').join(', '));
check('E4', '撤权落撤销墓碑（自愈请求前取证；LOGIN-003：阻塞并发鉴权缓存回填复活）', !!redisGet(tombKey(newestToken)), `tomb=${redisGet(tombKey(newestToken))}`);
check('E5', '撤权清理会话代际键（会话终结）', !redisKeys(refreshGenKey(userRefresh)).length, `value=${redisGet(refreshGenKey(userRefresh))}`);
const newestAfterRevoke = await api('GET', '/admin-api/system/auth/get-permission-info', { token: newestToken });
check('E2', '撤权后最新访问令牌立即 401', newestAfterRevoke.status === 401 || newestAfterRevoke.body?.code === 401, `HTTP ${newestAfterRevoke.status}`);
const lateRefresh = await api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${encodeURIComponent(userRefresh)}`);
check('E6', '撤权后凭刷新令牌无法复活会话', !(lateRefresh.body?.code === 0), `code=${lateRefresh.body?.code}`);

// ---------------------------------------------------------------------------
// 步骤 6：反向——旧名前缀键不可用（键存在 + 命名空间隔离直接取证）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 6/7 旧名前缀键不可用（反向）');
// 阳性对照：把管理员真实有效令牌的凭据值复制到旧名前缀键——若任何代码路径读旧命名空间，
// 该键即为「能通过鉴权的真凭据」；配合 MONITOR 取证直接证明服务端从未读旧前缀键。
const ctrlLegacyKey = `yudao_oauth2_access_token:${adminToken}`;
redisSet(ctrlLegacyKey, redisGet(accessKey(adminToken)));
const fakeOld = [
  { key: 'yudao_oauth2_access_token:brand004b-old-cred-1', token: 'brand004b-old-cred-1' },
  { key: 'ruoyi_oauth2_access_token:brand004b-old-cred-2', token: 'brand004b-old-cred-2' },
];
for (const f of fakeOld) {
  redisSet(f.key, JSON.stringify({
    id: 999999, userId: USER_ID, userType: 2, accessToken: f.token, refreshToken: 'brand004b-old-refresh',
    clientId: 'default', expiresTime: Date.now() + 3600_000, tenantId: TENANT_ID,
  }));
}
check('F0', '旧名前缀键已注入 Redis（含阳性对照真凭据副本）', redisGet(ctrlLegacyKey) !== null && fakeOld.every((f) => redisGet(f.key) !== null));

// MONITOR 取证：捕获窗口内发起「新命名空间 200 请求 + 旧名凭据 401 请求」，
// 断言窗口内服务端命令流不出现任何旧名前缀键的读取（命名空间隔离的直接证据）
const mon = spawn('docker', ['exec', redisContainer, 'redis-cli', 'MONITOR'], { stdio: ['ignore', 'pipe', 'pipe'] });
let monText = '';
mon.stdout.on('data', (d) => { monText += d.toString(); });
mon.stderr.on('data', (d) => { monText += d.toString(); });
await sleep(600);
const ctrlCall = await api('GET', '/admin-api/system/auth/get-permission-info', { token: adminToken });
const fakeCall = await api('GET', '/admin-api/system/auth/get-permission-info', { token: fakeOld[0].token });
await sleep(800);
try { mon.kill(); } catch { /* 已退出 */ }
await sleep(300);
const monLines = monText.split('\n').filter((l) => l.includes('"GET"') || l.includes('"EXISTS"') || l.includes('oauth2_access_token'));
check('F1', 'MONITOR 捕获有效（窗口内观测到对新命名空间键的读取）',
  monLines.some((l) => l.includes(`"GET" "oauth2_access_token:${adminToken}"`)), `capturedLines=${monLines.length}`);
check('F2', '命名空间隔离直证：命令流无任何旧名前缀键读取（yudao_/ruoyi_ 零命中）',
  !/(yudao|ruoyi|iocoder)_oauth2/i.test(monText), monLines.filter((l) => /yudao_|ruoyi_/i.test(l)).slice(0, 3).join(' | '));
check('F-ctrl', '阳性对照成立：新命名空间读取正常放行（旧名副本存在期间）', ctrlCall.body?.code === 0, `HTTP ${ctrlCall.status} code=${ctrlCall.body?.code}`);
check('F-rej', '旧名前缀键凭据串调受保护 API 必须 401', fakeCall.status === 401 || fakeCall.body?.code === 401, `HTTP ${fakeCall.status} code=${fakeCall.body?.code}`);
const oldRefreshTry = await api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${encodeURIComponent('yudao_oauth2_refresh_token:brand004b-old')}`);
check('F3', '旧名前缀刷新凭据串无法换新令牌', !(oldRefreshTry.body?.code === 0), `code=${oldRefreshTry.body?.code}`);
rcli('DEL', ctrlLegacyKey, ...fakeOld.map((f) => f.key));
check('F4', '注入的旧名键已清理（不污染终态键空间）', redisGet(ctrlLegacyKey) === null && fakeOld.every((f) => redisGet(f.key) === null));

// ---------------------------------------------------------------------------
// 步骤 7：退出（LOGIN-003 令牌级撤销）与终态键空间
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 7/7 退出清理与终态键空间');
const logout = await api('POST', '/admin-api/system/auth/logout', { token: adminToken });
check('G1', '管理员退出成功', logout.body?.code === 0, `HTTP ${logout.status}`);
await sleep(300);
check('G2', '退出后访问令牌键清理（oauth2_access_token:%s 消失）', !redisKeys(accessKey(adminToken)).length);
check('G3', '退出落撤销墓碑（LOGIN-003）', !!redisGet(tombKey(adminToken)));
check('G4', '退出清理会话代际键（refresh 代际键消失；本会话未刷新故无访问代际键）',
  !redisKeys(refreshGenKey(adminRefresh)).length && redisGet(accessGenKey(adminToken)) === null);
const adminAfterLogout = await api('GET', '/admin-api/system/auth/get-permission-info', { token: adminToken });
check('G5', '退出后原令牌调受保护 API 401', adminAfterLogout.status === 401 || adminAfterLogout.body?.code === 401, `HTTP ${adminAfterLogout.status}`);
const adminLateRefresh = await api('POST', `/admin-api/system/auth/refresh-token?refreshToken=${encodeURIComponent(adminRefresh)}`);
check('G6', '退出后刷新令牌无法复活会话', !(adminLateRefresh.body?.code === 0), `code=${adminLateRefresh.body?.code}`);

const finalKeys = redisKeys('*');
check('H1', '终态键空间无任何旧名键', finalKeys.every((k) => !OLD_NAME_RE.test(k)), finalKeys.filter((k) => OLD_NAME_RE.test(k)).join(', '));
check('H2', '终态无存活的已撤销会话访问令牌键', !redisKeys('oauth2_access_token:*').length, redisKeys('oauth2_access_token:*').join(', '));

// ---------------------------------------------------------------------------
// 报告
// ---------------------------------------------------------------------------
const failed = checks.filter((c) => !c.pass);
const report = {
  task: 'ZS-BRAND-004.B 运行期隔离证明',
  commit: (() => { try { return execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(); } catch { return 'unknown'; } })(),
  environment: {
    dockerServer: dockerVersion.stdout.trim(),
    redisImage: 'redis:7.4.11-alpine',
    redisVersion: rcli('INFO', 'server')?.match(/redis_version:([^\r\n]+)/)?.[1] ?? '',
    pgImage: 'postgres:17-alpine',
    pgVersion: (pgVerRow.stdout || '').trim(),
    serverPort, pgPort, redisPort,
    profiles: 'local', mockEnable: false, captchaEnable: false, flyway: 'off（迁移经 psql 按 V1 基线序施行）',
  },
  redisKeySpace: {
    finalKeyCount: finalKeys.length,
    finalKeys: finalKeys.slice(0, 400),
  },
  checks,
  pass: failed.length === 0,
};
writeFileSync(join(outDir, 'runtime-report.json'), JSON.stringify(report, null, 2));
console.log(`\n[runtime] ${checks.length - failed.length}/${checks.length} 项断言通过；报告：outputs/brand-004b/runtime-report.json`);
cleanup(); // 显式收尾：kill server + 删容器（java 子进程存活会阻止 exit 事件及时触发，故不可只依赖 exit 兜底）
process.exitCode = failed.length ? 1 : 0;
