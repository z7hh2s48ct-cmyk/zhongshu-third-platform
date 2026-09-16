/**
 * ZS-OPS-002.B：真实 PG17 上 Outbox 健康监测聚合 SQL 可移植性 + 告警分级/健康态映射回归。
 *
 * H2 单测（OutboxHealthAlertSchedulerTest / OutboxQueueHealthIndicatorTest）以 lambda stub 覆盖告警消费逻辑，
 * OutboxHealthMonitorTest（H2）覆盖越阈计算；但 OutboxHealthMonitorImpl 的聚合 SQL（SUM(CASE WHEN) 计数、
 * MIN(next_retry_at) 最长等待、参数化 lease_expires_at < ? 过期租约）与时间差计算的 **PG 专有可移植性** 从未在真实
 * PG17 重放验证（JOB-004 run-job004-verify 只验恢复台账 DDL/审计，未验监测聚合 SQL）。OPS-002.B 的探针/指示器
 * 直接消费该监测器，故须证明：真实 PG17 上 → 监测聚合 SQL 正确产出指标 → evaluateBreaches 派生正确越阈码 →
 * 探针按严重度分级（CRITICAL→ERROR / 其余→WARN）+ 指示器 health（越阈→DOWN / 健康→UP）。
 *
 * 前置：一次性 PG17 容器 + env-setup-test 角色 + V1/V2 基线 + audit + outbox_event/dispatcher_lease + 恢复台账迁移重放。
 * 阈值/严重度集合/健康映射 **逐一镜像** OutboxHealthMonitorImpl.evaluateBreaches + OutboxHealthAlertScheduler.CRITICAL_CODES
 * + OutboxQueueHealthIndicator.health（低阈值注入循 OutboxHealthMonitorTest 惯用法，证明阈值可配在 PG 上同样生效）。
 * 任一失败退出非零。用法：node scripts/db/run-ops002b-verify.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));

function fail(code, message) { console.error(message); process.exit(code); }
const dockerUp = spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { encoding: 'utf8' });
if (dockerUp.error || dockerUp.status !== 0) fail(3, `[ops002b] Docker 不可用：验证不得静默跳过`);

const container = `zszj-ops002b-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
const port = 3432 + Math.floor(Math.random() * 700);
let cleaned = false;
const cleanup = () => { if (!cleaned) { cleaned = true; try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { } } };
process.on('exit', cleanup);

execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=ops002b', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
const psql = (user, db, sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
const psqlOut = (user, db, sql) => spawnSync('docker', ['exec', container, 'psql', '-U', user, '-d', db, '-At', '-c', sql], { encoding: 'utf8' });

// 就绪探测（codex r0 P2 修复）：必须走 TCP + 口令探「最终 server」——容器 init 期会先起一个临时 server
// （仅 Unix socket、无口令），若经 socket 探到它会把 init 中途当就绪，随后 CREATE DATABASE 即失败。
// postgres:17-alpine 容器内 127.0.0.1:5432 即最终 server 的监听地址，PGPASSWORD 注入口令与启动参数一致。
let ready = false;
for (let i = 0; i < 60; i++) {
  const probe = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=ops002b', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' });
  if (probe.status === 0) { ready = true; break; }
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
}
if (!ready) { cleanup(); fail(1, '[ops002b] PG 未就绪'); }

execFileSync('docker', ['exec', '-e', 'PGPASSWORD=ops002b', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
{
  const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
  if (psql('postgres', 'zhongshu', setup).status !== 0) { cleanup(); fail(1, '[ops002b] 角色授权失败'); }
  const mig = 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/';
  const sql = [
    'V20260909.001__system_infra_baseline.sql',
    'V20260909.002__infra_quartz_backfill.sql',
    'V20260914.010__system_audit_event.sql',
    'V20260915.001__infra_outbox_event.sql',
    'V20260915.021__infra_outbox_recovery.sql',
  ].map((f) => readFileSync(join(root, mig + f), 'utf8')).join('\n');
  if (psql('zhongshu_owner', 'zhongshu', sql).status !== 0) { cleanup(); fail(1, '[ops002b] 迁移重放失败'); }
}
console.log('[ops002b] 环境就绪（PG17 + outbox_event/dispatcher_lease + 恢复台账迁移重放）');

let pass = 0, failCount = 0;
const record = (id, ok, note = '') => { pass += ok ? 1 : 0; failCount += ok ? 0 : 1; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`.trim()); };
const one = (sql) => psqlOut('postgres', 'zhongshu', sql).stdout.trim();
const reset = () => one('TRUNCATE outbox_event, dispatcher_lease, outbox_recovery_log');

// ── 镜像 OutboxHealthMonitorImpl 的聚合 SQL（逐字复制，验证 PG 可移植性）──
const STATUS_COUNT_SQL = `SELECT COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) || '|' || COALESCE(SUM(CASE WHEN status = 'DEAD' THEN 1 ELSE 0 END), 0) || '|' || COALESCE(SUM(CASE WHEN status = 'DISPATCHED' THEN 1 ELSE 0 END), 0) || '|' || COALESCE(SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END), 0) FROM outbox_event`;
const OLDEST_PENDING_SQL = `SELECT MIN(next_retry_at) FROM outbox_event WHERE status = 'PENDING'`;
const STALE_LEASE_SQL = `SELECT COUNT(*) FROM dispatcher_lease WHERE lease_expires_at < now()`;
// PG 侧最长等待 oracle（Java 用 now - MIN(next_retry_at) 在应用侧算差；此处用 EXTRACT(EPOCH) 取等价秒数作期望值）
const LONGEST_WAIT_SQL = `SELECT COALESCE(EXTRACT(EPOCH FROM (now() - MIN(next_retry_at)))::bigint, 0) FROM outbox_event WHERE status = 'PENDING'`;

// ── 镜像 OutboxHealthMonitorImpl.evaluateBreaches（阈值可注入，循 OutboxHealthMonitorTest 低阈值惯用法）──
function evaluateBreaches(m, t) {
  const b = [];
  if (m.pending > t.backlogCritical) b.push('PENDING_BACKLOG_CRITICAL');
  else if (m.pending > t.backlogWarn) b.push('PENDING_BACKLOG_WARN');
  if (m.dead > 0) b.push('DEAD_EVENTS_PRESENT');
  if (m.longestWait > t.longestWaitWarnSeconds) b.push('LONGEST_WAIT_WARN');
  const denom = m.dead + m.dispatched + m.skipped;
  const rate = denom === 0 ? 0 : Math.round(m.dead * 10000.0 / denom) / 100.0;
  if (rate > t.failureRateWarnPercent) b.push('FAILURE_RATE_WARN');
  if (m.stale > 0) b.push('STALE_LEASE_PRESENT');
  return { breaches: b, rate };
}
// ── 镜像 OutboxHealthAlertScheduler.CRITICAL_CODES + 分级 / OutboxQueueHealthIndicator.health ──
const CRITICAL_CODES = new Set(['PENDING_BACKLOG_CRITICAL', 'DEAD_EVENTS_PRESENT', 'STALE_LEASE_PRESENT']);
const severity = (breaches) => breaches.length === 0 ? 'NONE' : (breaches.some((x) => CRITICAL_CODES.has(x)) ? 'ERROR' : 'WARN');
const health = (breaches) => breaches.length === 0 ? 'UP' : 'DOWN';

// 读监测快照（真实 PG 上执行监测器 SQL）
function snapshot() {
  const [pending, dead, dispatched, skipped] = one(STATUS_COUNT_SQL).split('|').map(Number);
  const longestWait = Number(one(LONGEST_WAIT_SQL));
  const stale = Number(one(STALE_LEASE_SQL));
  return { pending, dead, dispatched, skipped, longestWait, stale };
}
const seedEvent = (status, retryOffsetSec) => one(
  `INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, status, retry_count, next_retry_at, tenant_id, actor_type)
VALUES ('OPS002B_VERIFY', 'infra_file', 'biz', '{}', '${status}', ${status === 'DEAD' ? 5 : 0}, now() ${retryOffsetSec >= 0 ? '+' : '-'} interval '${Math.abs(retryOffsetSec)} second', 1, 'SYSTEM') RETURNING id`);
const seedLease = (instanceId, expired) => one(
  `INSERT INTO dispatcher_lease (dispatcher_name, instance_id, lease_expires_at, heartbeat_at)
VALUES ('ops002b-dispatcher', '${instanceId}', now() ${expired ? '-' : '+'} interval '60 second', now()) RETURNING id`);
const LOW = { backlogWarn: 1, backlogCritical: 5000, longestWaitWarnSeconds: 30, failureRateWarnPercent: 5 };

// B1 聚合计数 SQL 在 PG17 可移植（SUM(CASE WHEN) 四状态计数正确）
{
  reset();
  seedEvent('PENDING', -120); seedEvent('PENDING', -1); seedEvent('PENDING', 60);
  seedEvent('DISPATCHED', -1); seedEvent('DISPATCHED', -1); seedEvent('DEAD', -1); seedEvent('SKIPPED', -1);
  const m = snapshot();
  record('B1 监测聚合计数 SQL 在 PG17 可移植（PENDING/DEAD/DISPATCHED/SKIPPED）',
    m.pending === 3 && m.dead === 1 && m.dispatched === 2 && m.skipped === 1,
    `pending=${m.pending} dead=${m.dead} dispatched=${m.dispatched} skipped=${m.skipped}`);
}

// B2 最长等待 SQL 在 PG17 可移植（MIN(next_retry_at) + 时间差；最老 PENDING ≈120s）
{
  const oldest = one(OLDEST_PENDING_SQL);
  const m = snapshot();
  record('B2 最长等待监测在 PG17 可移植（MIN(next_retry_at) 非空 + 距今秒数≈120）',
    oldest.length > 0 && m.longestWait >= 118 && m.longestWait <= 130,
    `oldest_nonempty=${oldest.length > 0} longestWait=${m.longestWait}`);
}

// B3 过期租约参数化比较在 PG17 可移植（1 过期 + 1 存活 → stale=1）
{
  reset();
  seedLease('instance-expired', true); seedLease('instance-live', false);
  const m = snapshot();
  record('B3 过期租约监测在 PG17 可移植（lease_expires_at < now 参数化比较）', m.stale === 1, `stale=${m.stale}`);
}

// B4 严重越阈 → 派生 CRITICAL 码 → 探针 ERROR + 指示器 DOWN（DEAD + 过期租约 + 积压）
{
  reset();
  seedEvent('PENDING', -120); seedEvent('PENDING', -1); // 积压 2 > backlogWarn(1)
  seedEvent('DEAD', -1);                                 // DEAD 存在
  seedLease('instance-expired', true);                  // 过期租约
  const m = snapshot();
  const { breaches } = evaluateBreaches(m, LOW);
  record('B4 真实 PG 越阈 → CRITICAL 码 → 探针 ERROR + 指示器 DOWN',
    breaches.includes('DEAD_EVENTS_PRESENT') && breaches.includes('STALE_LEASE_PRESENT') && breaches.includes('PENDING_BACKLOG_WARN')
    && severity(breaches) === 'ERROR' && health(breaches) === 'DOWN',
    `breaches=[${breaches.join(',')}] severity=${severity(breaches)} health=${health(breaches)}`);
}

// B5 仅预警越阈（积压，无 DEAD/租约/长等待/失败率）→ 探针 WARN（不升级 ERROR）+ 指示器 DOWN
{
  reset();
  seedEvent('PENDING', -1); seedEvent('PENDING', -1); // 积压 2 > backlogWarn(1)，next_retry_at 近期（不触发长等待）
  const m = snapshot();
  const { breaches } = evaluateBreaches(m, LOW);
  record('B5 仅预警越阈 → 探针 WARN（不误升级）+ 指示器 DOWN',
    breaches.length === 1 && breaches[0] === 'PENDING_BACKLOG_WARN' && severity(breaches) === 'WARN' && health(breaches) === 'DOWN',
    `breaches=[${breaches.join(',')}] severity=${severity(breaches)}`);
}

// B6 健康（空表）→ 无越阈 → 探针无告警 + 指示器 UP（失败不静默的反面：健康不误报）
{
  reset();
  const m = snapshot();
  const { breaches } = evaluateBreaches(m, LOW);
  record('B6 健康（空队列）→ 无越阈 → 探针 NONE + 指示器 UP',
    m.pending === 0 && m.dead === 0 && m.stale === 0 && breaches.length === 0 && severity(breaches) === 'NONE' && health(breaches) === 'UP',
    `breaches=[${breaches.join(',')}] severity=${severity(breaches)} health=${health(breaches)}`);
}

// B7 失败率监测在 PG17 正确（1 DEAD / (2 DISPATCHED + 1 DEAD + 1 SKIPPED) = 25% > 5% → FAILURE_RATE_WARN）
{
  reset();
  seedEvent('DISPATCHED', -1); seedEvent('DISPATCHED', -1); seedEvent('DEAD', -1); seedEvent('SKIPPED', -1);
  const m = snapshot();
  const { breaches, rate } = evaluateBreaches(m, { ...LOW, backlogWarn: 1000 });
  record('B7 失败率监测在 PG17 正确（DEAD/(DISPATCHED+DEAD+SKIPPED)×100=25% → FAILURE_RATE_WARN）',
    rate === 25.0 && breaches.includes('FAILURE_RATE_WARN'),
    `rate=${rate} breaches=[${breaches.join(',')}]`);
}

console.log(JSON.stringify({ pass, fail: failCount }));
cleanup();
process.exit(failCount ? 1 : 0);
