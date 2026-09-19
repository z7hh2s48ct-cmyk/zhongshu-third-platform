/**
 * ZS-BRAND-004.C 运行期证明：一次性 Docker PG17 + Redis 夹具拉起真实 zszj-server，
 * 在任务与消息持久化引用上证明「恢复/重放无静默丢失、不重复业务副作用、旧类引用受控迁移、
 * 无旧类找不到且不放开越权」：
 *   任务域：infra_job 种子任务经 job/sync 全量重登记进 Quartz（QRTZ_JOB_DETAILS.JOB_CLASS_NAME
 *   必须为 cn.zszj FQCN）；预置「旧 FQCN + 旧名」的遗留 QRTZ 行被对账器按孤儿触发器受控清理
 *   （旧类引用受控迁移直证）；暂停任务手动触发先经状态守卫，开启后 trigger 恰产生 1 条成功执行
 *   日志且不重复执行（不重复副作用）；未注册/旧式 handler 名被白名单与 Bean 存在性双重受控拒绝
 *   （不放开越权）。
 *   消息域：预置 DEAD×2 + PENDING×1 经 JOB-004 健康端点可见（无静默丢失）；授权 retry/skip
 *   受状态守卫与台账约束（二次 retry 被拒、台账/审计恰 1 行，重复恢复无重复副作用）；SKIPPED
 *   终态不再可领取；payload/headers/event_type 恢复前后字节不变（业务内容不做文本替换）。
 *   全程 server.log 零 ClassNotFoundException/NoClassDefFoundError/包形旧 FQCN（无旧类找不到）。
 *
 * 用法：node scripts/brand/run-brand-004c-runtime.mjs [--skip-build] [--keep]
 *   --skip-build  复用既有 zszj-server/target/zszj-server.jar（调试用；默认先 mvn package）
 *   --keep        保留容器/进程供排查（不用于 CI；正常退出码仍按断言结果）
 *
 * 语义（循 scripts/brand/run-brand-004b-runtime.mjs 与 scripts/db/run-job004-verify.mjs 夹具约定）：
 *   - 实例隔离：容器名/端口随机唯一，进程退出自动清理（含 java 子进程 taskkill 双路径）；
 *   - 失败非零：任一断言失败 → 非零退出；
 *   - 缺依赖不静默跳过：Docker 不可用 → 明确报错并以退出码 3 结束。
 *
 * 证据报告：JSON 输出 stdout，并落 outputs/brand-004c/runtime-report.json（不入库）。
 */
import { execFileSync, spawnSync, spawn } from 'node:child_process';
import { existsSync, mkdirSync, openSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const core = join(root, 'services', 'zhongshu-core');
const args = process.argv.slice(2);
const skipBuild = args.includes('--skip-build');
const keep = args.includes('--keep');
const outDir = join(root, 'outputs', 'brand-004c');
mkdirSync(outDir, { recursive: true });

const OLD_NAME_RE = /(yudao|ruoyi|iocoder|youdao|芋道|unibest)/i; // 键空间旧名判定（与静态门禁同口径）
const OLD_FQCN_RE = /(ClassNotFoundException|NoClassDefFoundError|cn\.iocoder|cn\.yudao|yudao\.(?:module|framework|server))/i; // 日志旧类引用判定（包形；fixture 任务名不误伤）
const TENANT_ID = 1;
const ADMIN_ID = 900001;
const USER_ID = 900002;
const PASSWORD = 'admin123';
const BCRYPT_HASH = '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG';
const SCHED_NAME = 'schedulerName'; // application-local.yaml spring.quartz.scheduler-name
const JOB_CLASS_NEW = 'cn.zszj.framework.quartz.core.handler.JobHandlerInvoker';
const JOB_CLASS_LEGACY = 'cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker'; // 受控注入的旧类引用夹具
const MIGRATIONS = [
  'V20260909.001__system_infra_baseline.sql',
  'V20260909.002__infra_quartz_backfill.sql',
  'V20260909.003__system_dict_unique_constraints.sql',
  'V20260913.001__infra_config_optimistic_version.sql',
  'V20260914.010__system_audit_event.sql',
  'V20260915.001__infra_outbox_event.sql',
  'V20260915.002__infra_inbox_event.sql',
  'V20260915.020__infra_job_tenant_result.sql',
  'V20260915.021__infra_outbox_recovery.sql',
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
/** 基础设施失败：清理已建资源后按约定退出码退出（缺 Docker=3，其余非零）。 */
function fail(code, message) {
  cleanup();
  console.error(message);
  process.exit(code);
}

// ---------------------------------------------------------------------------
// 基础设施（清理状态与资源标识先于 Docker 依赖检查初始化，保证缺 Docker 时仍能以退出码 3 干净退出）
// ---------------------------------------------------------------------------
const stamp = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const pgContainer = `zszj-brand004c-pg-${stamp}`;
const redisContainer = `zszj-brand004c-redis-${stamp}`;
const pgPort = 15432 + Math.floor(Math.random() * 1000);
const redisPort = 17000 + Math.floor(Math.random() * 2000);
const serverPort = 29600 + Math.floor(Math.random() * 1400); // 29600..30999，避开 .B 常用段
const pgPassword = 'fixture-' + Math.random().toString(36).slice(2);
const dbName = 'zszj_brand004c';

let serverProcess = null;
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
  for (let attempt = 1; attempt <= 2; attempt++) {
    const r = spawnSync('docker', ['run', '-d', '--name', name, ...runArgs, image], { encoding: 'utf8' });
    if (r.status === 0) return true;
    lastErr = (r.stderr || r.stdout || '').trim();
    try { execFileSync('docker', ['rm', '-f', name], { stdio: 'ignore' }); } catch { /* ignore */ }
    console.error(`[runtime] 容器 ${name} 第 ${attempt} 次启动失败：${lastErr}`);
  }
  return false;
}
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const rcli = (...cliArgs) => {
  const r = spawnSync('docker', ['exec', redisContainer, 'redis-cli', '--raw', ...cliArgs], { encoding: 'utf8' });
  return r.status === 0 ? (r.stdout ?? '').replace(/\r?\n$/, '') : null;
};
const redisKeys = (pattern) => (rcli('KEYS', pattern) ?? '').split('\n').filter(Boolean);

const BASE = `http://127.0.0.1:${serverPort}`;
async function api(method, path, { token, body } = {}) {
  const headers = { 'tenant-id': String(TENANT_ID), 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20000),
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非 JSON（如 401 HTML/空体） */ }
  return { status: res.status, body: json, text };
}

// ---------------------------------------------------------------------------
// 步骤 1：构建 + 拉起容器 + 初始化库 + 种子 + 启动真实 server
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 1/6 构建与夹具拉起');
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
  console.log('[runtime] mvn package zszj-server（首次约数分钟，日志见 outputs/brand-004c/build.log）…');
  const buildLog = openSync(join(outDir, 'build.log'), 'w');
  const buildCmd = process.platform === 'win32'
    ? ['cmd.exe', ['/d', '/s', '/c', 'mvn -B -pl zszj-server -am package -DskipTests']]
    : ['mvn', ['-B', '-pl', 'zszj-server', '-am', 'package', '-DskipTests']];
  const build = spawnSync(buildCmd[0], buildCmd[1],
    { cwd: core, env: childEnv, stdio: ['ignore', buildLog, buildLog], timeout: 15 * 60 * 1000 });
  if (build.status !== 0 || !existsSync(jarPath)) {
    fatal('A1', 'zszj-server 构建', `exit=${build.status}${build.error ? ` ${build.error.message}` : ''}（详见 outputs/brand-004c/build.log）`);
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
const psqlOut = (sql, db = dbName) => {
  const r = spawnSync('docker', ['exec', pgContainer, 'psql', '-U', 'postgres', '-At', '-d', db, '-c', sql], { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
  return r.status === 0 ? (r.stdout ?? '').trim() : `__SQL_ERR__${(r.stderr || '').slice(0, 200)}`;
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
  (${ADMIN_ID}, 'brand004cadmin', '${BCRYPT_HASH}', '品牌联验管理员', 103, '[4]', 'brand004c_admin@fixture.zszj.invalid', '17900000003', 1, 0, '1', '1', 0, ${TENANT_ID}),
  (${USER_ID},  'brand004cuser',  '${BCRYPT_HASH}', '品牌联验普通用户', 103, '[4]', 'brand004c_user@fixture.zszj.invalid',  '17900000004', 1, 0, '1', '1', 0, ${TENANT_ID});
INSERT INTO system_user_role (id, user_id, role_id, creator, updater, deleted, tenant_id) VALUES
  (910001, ${ADMIN_ID}, 1, '1', '1', 0, ${TENANT_ID}),
  (910002, ${USER_ID}, 2, '1', '1', 0, ${TENANT_ID});
`, dbName);

// 旧类引用受控注入夹具：遗留 QRTZ 行（旧 FQCN + 旧名任务），SCHED_NAME 与真实调度器一致、
// 触发器 PAUSED 且 NEXT_FIRE_TIME 远期（绝不触发执行），专用于证明对账器受控清理
const legacyFireTime = Date.now() + 86400_000;
psql(`
INSERT INTO QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)
VALUES ('${SCHED_NAME}', 'yudaoLegacyJob', 'DEFAULT', 'brand004c legacy fixture', '${JOB_CLASS_LEGACY}', false, false, false, false, NULL);
INSERT INTO QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, PRIORITY, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA)
VALUES ('${SCHED_NAME}', 'yudaoLegacyJob', 'DEFAULT', 'yudaoLegacyJob', 'DEFAULT', 'brand004c legacy fixture', ${legacyFireTime}, NULL, 5, 'PAUSED', 'CRON', ${Date.now()}, NULL, NULL, 0, NULL);
INSERT INTO QRTZ_CRON_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID)
VALUES ('${SCHED_NAME}', 'yudaoLegacyJob', 'DEFAULT', '0 0 3 * * ?', 'Asia/Shanghai');
`, dbName);

// Outbox 预置事件：DEAD×2（供授权 retry/skip）+ PENDING×1（积压可见性），payload 带指纹供字节比对
const seedEvent = (eventType, status, payload) => psqlOut(
  `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, headers, status, retry_count, next_retry_at, last_error, tenant_id, actor_type)
VALUES ('${eventType}', 'brand004c', 'biz-${eventType}', '${payload}', '{"trace":"brand004c"}', '${status}', ${status === 'DEAD' ? 5 : 0}, now() - interval '1 second', ${status === 'DEAD' ? `'{"errorClass":"IllegalStateException","messageLength":18}'` : 'NULL'}, ${TENANT_ID}, 'SYSTEM') RETURNING id`).split('\n').filter((l) => /^\d+$/.test(l))[0];
const evtDeadA = seedEvent('BRAND004C_PROBE_RETRY', 'DEAD', '{"probe":"retry-me","n":1}');
const evtDeadB = seedEvent('BRAND004C_PROBE_SKIP', 'DEAD', '{"probe":"skip-me","n":2}');
const evtPending = seedEvent('BRAND004C_PROBE_PENDING', 'PENDING', '{"probe":"backlog","n":3}');
if (!evtDeadA || !evtDeadB || !evtPending) fatal('A2', 'Outbox 预置事件种子', `deadA=${evtDeadA} deadB=${evtDeadB} pending=${evtPending}`);
const fingerprint = (id) => psqlOut(`SELECT event_type || '|' || coalesce(headers, '-') || '|' || payload FROM outbox_event WHERE id = ${id}`);
const fpA = fingerprint(evtDeadA);
const fpB = fingerprint(evtDeadB);

console.log(`[runtime] 启动 zszj-server @ 127.0.0.1:${serverPort}（PG ${pgPort} / Redis ${redisPort}，quartz on + 白名单 3 Handler）…`);
const serverLogFd = openSync(join(outDir, 'server.log'), 'w');
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
  // CFG-1 收敛（2026-09-19，方向②）：local 恒无 Scheduler 为既定合同（application-local.yaml exclude 保留），
  // 本卡被测面是 Quartz 持久化全链——CLI 覆盖 exclude 清单（保留 AI 向量库两项排除）+ 显式开启 auto-startup
  '--spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration,org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration',
  '--spring.quartz.auto-startup=true',
  '--zszj.job.handler-whitelist[0]=accessLogCleanJob', // ZS-JOB-001 白名单：只有种子任务可登记/执行
  '--zszj.job.handler-whitelist[1]=errorLogCleanJob',
  '--zszj.job.handler-whitelist[2]=jobLogCleanJob',
  // ZS-DB-001.B 已登记缺陷的夹具级规避（循 .B）：Druid PSCache + PG 游标重复执行缺陷，显式关闭
  '--spring.datasource.dynamic.druid.pool-prepared-statements=false',
  '--spring.datasource.dynamic.druid.max-pool-prepared-statement-per-connection-size=-1',
], { env: childEnv, stdio: ['ignore', serverLogFd, serverLogFd], windowsHide: true });

const serverLogPath = join(outDir, 'server.log');
let up = false;
for (let i = 0; i < 160 && !up; i++) {
  await sleep(1500);
  if (serverProcess.exitCode !== null || serverProcess.signalCode) break;
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
// 步骤 2：基线（夹具完整性 + 键空间）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 2/6 夹具与基线');
const legacyQrtzBefore = psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = 'yudaoLegacyJob' AND JOB_CLASS_NAME = '${JOB_CLASS_LEGACY}'`);
check('A4', '旧类引用夹具预置完成（yudaoLegacyJob 携旧 FQCN 入 QRTZ，PAUSED 不触发）', legacyQrtzBefore === '1', `count=${legacyQrtzBefore}`);
check('A5', '基线键空间无旧名键', redisKeys('*').every((k) => !OLD_NAME_RE.test(k)), redisKeys('*').filter((k) => OLD_NAME_RE.test(k)).join(', '));

const loginAdmin = await api('POST', '/admin-api/system/auth/login', { body: { username: 'brand004cadmin', password: PASSWORD } });
if (loginAdmin.body?.code !== 0) fatal('B0', '账密登录（管理员）', `HTTP ${loginAdmin.status} body=${loginAdmin.text.slice(0, 300)}`);
const adminToken = loginAdmin.body.data.accessToken;
check('B1', '账密登录签发令牌', !!adminToken);

const infraJobBefore = psqlOut(`SELECT id || ':' || handler_name || ':' || status FROM infra_job ORDER BY id`);
const logCountBefore = psqlOut(`SELECT count(*) FROM infra_job_log`);

// ---------------------------------------------------------------------------
// 步骤 3：job/sync 全量重登记（QRTZ 持久化引用 + 旧类受控清理 + 表不被改写）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 3/6 任务重登记与旧类引用受控清理');
const syncRes = await api('POST', '/admin-api/infra/job/sync', { token: adminToken });
check('B2', 'POST /infra/job/sync 成功（进程重启同步入口）', syncRes.body?.code === 0, `HTTP ${syncRes.status} body=${syncRes.text.slice(0, 200)}`);

const legacyAfter = psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = 'yudaoLegacyJob'`);
const legacyTriggerAfter = psqlOut(`SELECT count(*) FROM QRTZ_TRIGGERS WHERE TRIGGER_NAME = 'yudaoLegacyJob'`);
check('B3', '旧类引用受控清理：遗留 QRTZ 行（旧 FQCN + 旧名任务）被对账器孤儿清理', legacyAfter === '0' && legacyTriggerAfter === '0',
  `jobDetails=${legacyAfter} trigger=${legacyTriggerAfter}`);

const jobClassList = psqlOut(`SELECT JOB_NAME || ' -> ' || JOB_CLASS_NAME FROM QRTZ_JOB_DETAILS ORDER BY JOB_NAME`);
const qrtzCount = psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS`);
const allNewFqcn = psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS WHERE JOB_CLASS_NAME = '${JOB_CLASS_NEW}'`);
check('B4', 'QRTZ_JOB_DETAILS.JOB_CLASS_NAME 全为新包根 FQCN（cn.zszj…JobHandlerInvoker）', qrtzCount === '3' && allNewFqcn === '3',
  jobClassList.split('\n').join(' ; '));
const qrtzNames = psqlOut(`SELECT JOB_NAME FROM QRTZ_JOB_DETAILS ORDER BY JOB_NAME`).split('\n');
const seedNames = infraJobBefore.split('\n').map((l) => l.split(':')[1]).sort();
check('B5', 'Quartz 登记名与 infra_job 种子 handler_name 一一对应', JSON.stringify(qrtzNames) === JSON.stringify(seedNames),
  `qrtz=${qrtzNames.join(',')} seed=${seedNames.join(',')}`);
const infraJobAfter = psqlOut(`SELECT id || ':' || handler_name || ':' || status FROM infra_job ORDER BY id`);
check('B6', '对账不改写任务表：infra_job 行前后一致（权威源恒为任务表）', infraJobBefore === infraJobAfter,
  `before=[${infraJobBefore.split('\n').join(';')}] after=[${infraJobAfter.split('\n').join(';')}]`);
check('B7', '暂停任务未被调度：同步后执行日志仍为 0 条', psqlOut(`SELECT count(*) FROM infra_job_log`) === logCountBefore && logCountBefore === '0',
  `logCount=${logCountBefore}`);

// ---------------------------------------------------------------------------
// 步骤 4：暂停守卫 → 开启 → 手动触发恰一次（不重复副作用）
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 4/6 暂停守卫与单次触发');
const triggerPaused = await api('PUT', `/admin-api/infra/job/trigger?id=26`, { token: adminToken });
check('C1', '暂停任务手动触发被状态守卫拒绝（不绕过运维意图）', triggerPaused.body?.code !== 0, `code=${triggerPaused.body?.code}`);
const openJob = await api('PUT', '/admin-api/infra/job/update-status?id=26&status=1', { token: adminToken });
check('C2', '任务 26（errorLogCleanJob）置为开启（恢复链：表状态与调度器同步恢复）', openJob.body?.code === 0, `HTTP ${openJob.status} code=${openJob.body?.code}`);
const triggerRes = await api('PUT', '/admin-api/infra/job/trigger?id=26', { token: adminToken });
check('C3', '开启后手动触发成功（立即触发一次）', triggerRes.body?.code === 0, `HTTP ${triggerRes.status} code=${triggerRes.body?.code}`);

let logRow = null;
for (let i = 0; i < 60; i++) {
  await sleep(1000);
  const row = psqlOut(`SELECT status || '|' || coalesce(cast(end_time as text), '-') || '|' || coalesce(result, '') FROM infra_job_log WHERE job_id = 26 AND handler_name = 'errorLogCleanJob'`);
  if (row && !row.startsWith('__SQL_ERR__')) {
    const [st, end] = row.split('|');
    if (st === '1' && end !== '-') { logRow = row; break; } // status=SUCCESS 且执行已收尾
  }
}
check('C4', '触发恰产生 1 条执行日志且成功（status=1，恢复/执行无丢失）',
  psqlOut(`SELECT count(*) FROM infra_job_log WHERE job_id = 26`) === '1' && logRow?.startsWith('1|'),
  `rows=${psqlOut(`SELECT count(*) FROM infra_job_log WHERE job_id = 26`)} row=${logRow}`);
await sleep(3000);
check('C5', '完成窗口后仍恰 1 条（无重放/重复执行副作用）', psqlOut(`SELECT count(*) FROM infra_job_log WHERE job_id = 26`) === '1',
  `rows=${psqlOut(`SELECT count(*) FROM infra_job_log WHERE job_id = 26`)}`);
// 一次性触发（MT_*）的 FIRED 行由调度器执行完成路径回收，集群 checkin（15s）内完成，轮询等待
let firedCount = '-1';
for (let i = 0; i < 30; i++) {
  firedCount = psqlOut(`SELECT count(*) FROM QRTZ_FIRED_TRIGGERS`);
  if (firedCount === '0') break;
  await sleep(1000);
}
check('C6', '无滞留执行中的触发（QRTZ_FIRED_TRIGGERS 回收为空）', firedCount === '0', `fired=${firedCount}`);

// ---------------------------------------------------------------------------
// 步骤 5：登记侧两层受控拒绝（不放开越权）+ Outbox 恢复/重放
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 5/6 受控拒绝与 Outbox 恢复');
// 登记侧受控拒绝是两层串行（JobServiceImpl#validateJobHandlerExists：先 Bean 存在性/类型，
// 后白名单）。两层各自独立取证（codex r0 P2-1）：
//   层 1 对照：yudaoDemoJob 非 Spring Bean → JOB_HANDLER_BEAN_NOT_EXISTS(1_001_001_006)——
//             即使白名单层被整个移除，本用例仍绿，故不能单凭它证明白名单生效；
//   层 2 主用例：demoJob 是真实容器 Bean（system 模块 @Component DemoJob）但不在白名单 →
//             JOB_HANDLER_NOT_WHITELISTED(1_001_009_005)——唯此用例能锤定白名单层自身生效。
const CODE_BEAN_NOT_EXISTS = 1001001006; // 1_001_001_006
const CODE_NOT_WHITELISTED = 1001009005; // 1_001_009_005
const createNoBean = await api('POST', '/admin-api/infra/job/create', {
  token: adminToken,
  body: { name: '旧式演示任务', handlerName: 'yudaoDemoJob', handlerParam: '', cronExpression: '0 0 0 * * ?', retryCount: 0, retryInterval: 0, monitorTimeout: 0 },
});
check('D1', '层1对照：未注册 handler（非 Bean）被 Bean 存在性层拒绝（JOB_HANDLER_BEAN_NOT_EXISTS）',
  createNoBean.body?.code === CODE_BEAN_NOT_EXISTS, `code=${createNoBean.body?.code} expected=${CODE_BEAN_NOT_EXISTS}`);
const createWhitelist = await api('POST', '/admin-api/infra/job/create', {
  token: adminToken,
  body: { name: '白名单外真实任务', handlerName: 'demoJob', handlerParam: '', cronExpression: '0 0 0 * * ?', retryCount: 0, retryInterval: 0, monitorTimeout: 0 },
});
check('D3', '层2主用例：真实 Bean（demoJob）被白名单排除 → 登记被拒（JOB_HANDLER_NOT_WHITELISTED）',
  createWhitelist.body?.code === CODE_NOT_WHITELISTED, `code=${createWhitelist.body?.code} expected=${CODE_NOT_WHITELISTED}`);
check('D2', '两例被拒后任务表与 QRTZ 均无新增 handler（无越权登记）',
  psqlOut(`SELECT count(*) FROM infra_job WHERE handler_name IN ('yudaoDemoJob', 'demoJob')`) === '0' && psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS`) === '3',
  `job=${psqlOut(`SELECT count(*) FROM infra_job WHERE handler_name IN ('yudaoDemoJob', 'demoJob')`)} qrtz=${psqlOut(`SELECT count(*) FROM QRTZ_JOB_DETAILS`)}`);

const health = await api('GET', '/admin-api/infra/outbox-event/health', { token: adminToken });
check('E1', '预置事件经健康端点可见（DEAD≥2 且积压≥1，无静默丢失）',
  health.body?.code === 0 && (health.body?.data?.deadCount ?? 0) >= 2 && (health.body?.data?.pendingBacklog ?? 0) >= 1,
  `HTTP ${health.status} body=${health.text.slice(0, 200)}`);

const retry = await api('PUT', '/admin-api/infra/outbox-event/retry', {
  token: adminToken,
  body: { eventId: Number(evtDeadA), reason: 'brand004c 联验：根因排除后授权重试' },
});
const deadARow = psqlOut(`SELECT status || '|' || coalesce(claimed_by, '-') || '|' || coalesce(claim_token, '-') FROM outbox_event WHERE id = ${evtDeadA}`);
const retryLogCount = psqlOut(`SELECT count(*) FROM outbox_recovery_log WHERE event_id = ${evtDeadA} AND action = 'RETRY'`);
const auditCount = psqlOut(`SELECT count(*) FROM audit_event WHERE event_type = 'OUTBOX_EVENT_RETRIED' AND biz_id = '${evtDeadA}'`);
check('E2', '授权重试 DEAD→PENDING：清租约 + 台账 RETRY 恰 1 行 + 审计恰 1 行（恢复可追踪）',
  retry.body?.code === 0 && deadARow === 'PENDING|-|-' && retryLogCount === '1' && auditCount === '1',
  `code=${retry.body?.code} row=${deadARow} log=${retryLogCount} audit=${auditCount}`);
const retryAgain = await api('PUT', '/admin-api/infra/outbox-event/retry', {
  token: adminToken,
  body: { eventId: Number(evtDeadA), reason: 'brand004c 联验：二次恢复应被状态守卫拒绝' },
});
check('E3', '同事件二次重试被拒（非 DEAD 不可恢复，重复消费无重复副作用）', retryAgain.body?.code !== 0,
  `code=${retryAgain.body?.code}`);
// codex r0 P2-2：审计计数必须在被拒的二次重试「之后」重查并进断言谓词——
// 若被拒请求额外写了审计事件，此处计数变 2 即打红（先查后断会假绿）
const auditCountAfterRetryAgain = psqlOut(`SELECT count(*) FROM audit_event WHERE event_type = 'OUTBOX_EVENT_RETRIED' AND biz_id = '${evtDeadA}'`);
check('E4', '被拒后台账/审计仍各恰 1 行（重复恢复无重复留痕副作用）',
  psqlOut(`SELECT count(*) FROM outbox_recovery_log WHERE event_id = ${evtDeadA}`) === '1' && auditCountAfterRetryAgain === '1',
  `log=${psqlOut(`SELECT count(*) FROM outbox_recovery_log WHERE event_id = ${evtDeadA}`)} audit=${auditCountAfterRetryAgain}`);

const skip = await api('PUT', '/admin-api/infra/outbox-event/skip', {
  token: adminToken,
  body: { eventId: Number(evtDeadB), reason: 'brand004c 联验：放弃终态验证' },
});
const deadBStatus = psqlOut(`SELECT status FROM outbox_event WHERE id = ${evtDeadB}`);
const deadBClaimable = psqlOut(`SELECT count(*) FROM outbox_event WHERE id = ${evtDeadB} AND status = 'PENDING' AND next_retry_at <= now()`);
check('E5', '授权跳过 DEAD→SKIPPED 终态且不再进入领取候选', skip.body?.code === 0 && deadBStatus === 'SKIPPED' && deadBClaimable === '0',
  `code=${skip.body?.code} status=${deadBStatus} claimable=${deadBClaimable}`);

const fpAAfter = fingerprint(evtDeadA);
const fpBAfter = fingerprint(evtDeadB);
check('E6', '恢复/跳过前后 event_type/headers/payload 字节不变（业务内容不做文本替换）', fpAAfter === fpA && fpBAfter === fpB,
  `A_same=${fpAAfter === fpA} B_same=${fpBAfter === fpB}`);
check('E7', 'PENDING 预置事件原样保留（积压可见、不静默丢弃、不派发器缺席下被篡改）',
  psqlOut(`SELECT status FROM outbox_event WHERE id = ${evtPending}`) === 'PENDING');

// ---------------------------------------------------------------------------
// 步骤 6：全程无旧类找不到 + 终态
// ---------------------------------------------------------------------------
console.log('[runtime] 步骤 6/6 旧类引用零异常与终态');
// server.log 含 JVM 本地字符集中文（Windows JDK17 默认 GBK）与 UTF-8 混排：
// 以 latin1 逐字节读取保证 ASCII 锚点判定稳定（中文样貌不影响字节级匹配）
let serverLog = '';
for (let i = 0; i < 5; i++) {
  serverLog = existsSync(serverLogPath) ? readFileSync(serverLogPath, 'latin1') : '';
  if (serverLog.includes('yudaoLegacyJob') || i === 4) break;
  await sleep(1000); // 少量重试兜底 stdout 缓冲刷新
}
const cnfeHits = serverLog.split('\n').filter((l) => OLD_FQCN_RE.test(l));
check('F1', 'server.log 全程零 ClassNotFoundException/NoClassDefFoundError/包形旧 FQCN（无旧类找不到）', cnfeHits.length === 0,
  cnfeHits.slice(0, 3).map((l) => l.slice(0, 160)).join(' | '));
// 受控清理路径证据：对账器孤儿触发器日志行与旧名任务同行共现（ASCII 锚点，避开中文编码不确定性）
const orphanLine = serverLog.split('\n').find((l) => l.includes('yudaoLegacyJob') && l.includes('reconcileOrphanTriggers'));
check('F2', '旧类清理走了对账器受控路径（reconcileOrphanTriggers 日志行与旧名任务同现，非静默删行）',
  !!orphanLine, orphanLine?.slice(0, 200) ?? '(未找到孤儿清理日志行)');
const finalKeys = redisKeys('*');
check('F3', '终态键空间无任何旧名键', finalKeys.every((k) => !OLD_NAME_RE.test(k)), finalKeys.filter((k) => OLD_NAME_RE.test(k)).join(', '));
check('F4', 'dispatcher_lease 无实例登记（生产派发接线归 D-07 的登记边界如实反映）',
  psqlOut(`SELECT count(*) FROM dispatcher_lease`) === '0', `lease=${psqlOut(`SELECT count(*) FROM dispatcher_lease`)}`);

// ---------------------------------------------------------------------------
// 报告
// ---------------------------------------------------------------------------
const failed = checks.filter((c) => !c.pass);
const report = {
  task: 'ZS-BRAND-004.C 任务与消息持久化引用运行期证明',
  commit: (() => { try { return execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(); } catch { return 'unknown'; } })(),
  environment: {
    dockerServer: dockerVersion.stdout.trim(),
    pgImage: 'postgres:17-alpine',
    redisImage: 'redis:7.4.11-alpine',
    serverPort, pgPort, redisPort,
    profiles: 'local', quartzAutoStartup: true, handlerWhitelist: ['accessLogCleanJob', 'errorLogCleanJob', 'jobLogCleanJob'],
    mockEnable: false, captchaEnable: false, flyway: 'off（迁移经 psql 按 V1 基线序施行）',
    legacyFixture: { qrtzJob: 'yudaoLegacyJob', jobClassName: JOB_CLASS_LEGACY },
  },
  outboxFixture: { deadA: evtDeadA, deadB: evtDeadB, pending: evtPending },
  quartzAfterSync: psqlOut(`SELECT JOB_NAME || ' -> ' || JOB_CLASS_NAME FROM QRTZ_JOB_DETAILS ORDER BY JOB_NAME`).split('\n'),
  checks,
  pass: failed.length === 0,
};
writeFileSync(join(outDir, 'runtime-report.json'), JSON.stringify(report, null, 2));
console.log(`\n[runtime] ${checks.length - failed.length}/${checks.length} 项断言通过；报告：outputs/brand-004c/runtime-report.json`);
cleanup(); // 显式收尾：kill server + 删容器（java 子进程存活会阻止 exit 事件及时触发，故不可只依赖 exit 兜底）
process.exitCode = failed.length ? 1 : 0;
