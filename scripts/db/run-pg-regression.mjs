/**
 * ZS-DB-019.B 地基：PG 技术回归聚合入口（本地与 CI 同一规则）。
 *
 * 串联执行全部真实 PG 验证套件（每套件自管理一次性容器，验后清理）：
 *   1. ZS-DB-006 序列主键/批量/并发/回滚/低权限（6 用例）
 *   2. ZS-DB-007 字段映射与逻辑删除合同（7 用例）
 *   3. ZS-DB-008 事务原子性与锁行为（6 用例）
 *   4. ZS-CFG-002.A 字典编码唯一约束（8 用例）
 *   5. ZS-DB-011~015 有界删除（5 表 × 有界/边界/空集）
 *   6. ZS-DB-018 ORM/手写 SQL/租户隔离（两技术租户 CRUD/分页/关联/批量/逻辑删除 + 全局表/忽略注解/系统清理合法范围）
 *   7. DB-016 Quartz 调度表结构级验证（委托核心语句）
 *   8. DB-017 PG 元数据测试表与注释/索引核对
 *   9. ZS-BPM-001 BPM 独立装配与 PG 验收（两阶段：owner 建表引导 + app 低权限运行）
 *  10. ZS-JOB-002 事务 Outbox 领取/租约/栅栏/DEAD（双实例 SKIP LOCKED、崩溃重领、旧凭证栅栏）
 *  11. ZS-JOB-003 消费者幂等 Inbox（唯一键抢占并发语义、租户隔离、状态机硬约束）
 *  12. ZS-JOB-004 人工恢复台账（V20260915.021 迁移重放、DEAD→retry/skip、SKIPPED 终态、双轨审计落地、payload 只读）
 *  13. ZS-OPS-002.B 健康监测聚合 PG 可移植性 + 告警分级/健康态映射（镜像 OutboxHealthMonitorImpl SQL 与 breaches 消费）
 *  14. ZS-SEC-011.B 持久化幂等（V20260916.101 迁移重放、ON CONFLICT 抢占、8 连接并发兜底、状态机 CHECK、跨会话重放不重复写、>2048 快照完整性）
 *  15. ZS-FILE-005.B 超时补偿（V20260916.102 重放+存量回填、并发领取 CAS 串行化、重复清理不误删、孤儿清点全局核验）
 *  16. ZS-LOGIN-005.B 补偿事件持久化（outbox_event 结构、SKIP LOCKED 双实例领取、秘密扩散防护、过期事件静默 skip）
 *  17. ZS-DB-010 账号唯一约束与并发兜底（复现回归、结构核对、历史冲突 fail-loudly、并发兜底 23505、大小写归一、删除重建、空值多行、跨租户全局唯一）
 * 任一套件失败退出非零。
 * 用法：node scripts/db/run-pg-regression.mjs
 */
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const cases = [
  { id: 'ZS-DB-006 序列/批量/续号', cmd: ['node', 'scripts/db/run-db006-verify.mjs'] },
  { id: 'ZS-DB-007 字段映射与逻辑删除', cmd: ['node', 'scripts/db/run-db007-verify.mjs'] },
  { id: 'ZS-DB-008 事务与锁', cmd: ['node', 'scripts/db/run-db008-verify.mjs'] },
  { id: 'ZS-CFG-002.A 字典约束', cmd: ['node', 'scripts/db/run-cfg002-verify.mjs'] },
  { id: 'ZS-DB-011~015 有界删除', cmd: ['node', 'scripts/db/run-db011-015-verify.mjs'] },
  { id: 'ZS-DB-018 ORM/手写SQL/租户隔离', cmd: ['node', 'scripts/db/run-db018-verify.mjs'] },
  { id: 'DB-016 Quartz 调度表结构级验证', caseFile: 'scripts/db/cases/db016-quartz-schema.sql' },
  { id: 'DB-017 元数据测试表', caseFile: 'scripts/db/cases/db017-metadata-testtable.sql' },
  { id: 'ZS-BPM-001 BPM 独立装配与PG验收', cmd: ['node', 'scripts/db/run-bpm001-verify.mjs'] },
  { id: 'ZS-JOB-002 事务Outbox领取/租约/栅栏', cmd: ['node', 'scripts/db/run-job002-verify.mjs'] },
  { id: 'ZS-JOB-003 消费者幂等Inbox唯一键抢占', cmd: ['node', 'scripts/db/run-job003-verify.mjs'] },
  { id: 'ZS-JOB-004 人工恢复台账/DEAD跳过/双轨审计', cmd: ['node', 'scripts/db/run-job004-verify.mjs'] },
  { id: 'ZS-OPS-002.B 健康监测聚合PG可移植/告警分级/健康态', cmd: ['node', 'scripts/db/run-ops002b-verify.mjs'] },
  { id: 'ZS-SEC-011.B 持久化幂等唯一约束/重放', cmd: ['node', 'scripts/db/run-sec011b-verify.mjs'] },
  { id: 'ZS-FILE-005.B 超时补偿CAS串行/孤儿清点/重复清理', cmd: ['node', 'scripts/db/run-file005b-verify.mjs'] },
  { id: 'ZS-LOGIN-005.B 补偿事件持久化/SKIP LOCKED/秘密扩散防护', cmd: ['node', 'scripts/db/run-login005b-verify.mjs'] },
  { id: 'ZS-DB-010 账号唯一约束/并发兜底/历史冲突fail-loudly', cmd: ['node', 'scripts/db/run-db010-verify.mjs'] },
];

let failed = false;
const summary = [];
for (const c of cases) {
  let ok = true, note = '';
  try {
    if (c.caseFile) {
      // DB-016/017：独立容器 + V1/V2 + 用例 SQL
      const port = 5532 + Math.floor(Math.random() * 400);
      const container = `zszj-pgreg-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
      const boot = (sql) => spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
      execFileSync('docker', ['run', '-d', '--name', container, '-e', 'POSTGRES_PASSWORD=pgreg', '-p', `127.0.0.1:${port}:5432`, 'postgres:17-alpine'], { stdio: 'ignore' });
      try {
        let ready = false;
        // 就绪探测（循 run-ops002b-verify codex r0 P2 修复）：TCP + 口令探「最终 server」，避 init 临时 server（仅 socket）误判
        for (let i = 0; i < 60; i++) { const r = spawnSync('docker', ['exec', '-e', 'PGPASSWORD=pgreg', container, 'psql', '-h', '127.0.0.1', '-p', '5432', '-U', 'postgres', '-d', 'postgres', '-At', '-c', 'SELECT 1'], { encoding: 'utf8' }); if (r.status === 0) { ready = true; break; } Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500); }
        if (!ready) throw new Error('PG 未就绪');
        execFileSync('docker', ['exec', '-e', 'PGPASSWORD=pgreg', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-q', '-c', 'CREATE DATABASE zhongshu;'], { stdio: 'ignore' });
        const setup = readFileSync(join(root, 'services/zhongshu-core/sql/postgresql/env-setup-test.sql'), 'utf8');
        const s1 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: setup, encoding: 'utf8' });
        if (s1.status !== 0) throw new Error('角色授权失败');
        const v1 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.001__system_infra_baseline.sql'), 'utf8');
        const v2 = readFileSync(join(root, 'services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260909.002__infra_quartz_backfill.sql'), 'utf8');
        const s2 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: v1 + '\n' + v2, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
        if (s2.status !== 0) throw new Error('V1/V2 执行失败');
        const sql = readFileSync(join(root, c.caseFile), 'utf8');
        const s3 = spawnSync('docker', ['exec', '-i', container, 'psql', '-U', 'zhongshu_owner', '-d', 'zhongshu', '-v', 'ON_ERROR_STOP=1', '-q'], { input: sql, encoding: 'utf8' });
        if (s3.status !== 0) throw new Error('用例执行失败');
      } finally {
        try { execFileSync('docker', ['rm', '-f', container], { stdio: 'ignore' }); } catch { }
      }
    } else {
      const r = spawnSync(c.cmd[0], c.cmd.slice(1), { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
      if (r.status !== 0) throw new Error((r.stdout ?? '') + (r.stderr ?? ''));
    }
  } catch (e) {
    ok = false;
    note = String(e.message ?? e).split('\n')[0].slice(0, 160);
  }
  summary.push({ id: c.id, ok, note });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${c.id} ${note}`);
  if (!ok) failed = true;
}

console.log('\n===== PG 技术回归汇总（ZS-DB-019.B 地基）=====');
for (const s of summary) console.log(`${s.ok ? 'PASS' : 'FAIL'}  ${s.id} ${s.note}`);
console.log(`合计 ${summary.length} 套件，失败 ${summary.filter((s) => !s.ok).length} 套件`);
process.exit(failed ? 1 : 0);
