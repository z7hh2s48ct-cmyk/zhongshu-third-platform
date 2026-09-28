/**
 * ZS-OPS-001.E：B09 联合门禁（G16）——获批首链/流程及组织权限门禁。
 *
 * 串联两项真实 PostgreSQL 套件（各自自管理一次性容器，验后强制清理，本编排器不共享容器）：
 *   1. ZS-BPM-001 BPM 独立装配与 PG 验收（run-bpm001-verify.mjs，两阶段 owner/app：
 *      引擎建表/部署幂等/重启恢复/通过/拒绝/撤回/转办/事务回滚/积压恢复）——「PG 流程」；
 *   2. ZS-BPM-003 首链领域状态与幂等写回（run-bpm003-verify.mjs，真实 Flowable+PG：
 *      首链四操作与业务状态一致/重复回调吸收/旧流程晚到弃单/跨租户隔离反向）——「幂等写回 + 跨组织反向」。
 *
 * 验收对齐 docs/03 §B09 与 §16.1 ZS-OPS-001.E 行：「PG 流程、幂等写回和跨组织反向通过」。
 * D-07（M1~M10，docs/09 §4）与 D-09 已确认为前置；两套件缺一、任一失败、任一套件退出非零
 * → 本门禁非零退出，不静默跳过（批次放行轮以本入口跑 B09 门禁证据）。
 *
 * 与 G14/G15 同为 exclusive（Docker 重负载），--fast 跳过；报告落 outputs/b09-gate/。
 *
 * 用法：node scripts/ops/run-b09-joint-gate.mjs
 */
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const outDir = join(root, 'outputs', 'b09-gate');

const suites = [
  { id: 'P1 ZS-BPM-001 PG 流程（独立装配/重启恢复/四操作/事务回滚/积压恢复）', script: 'scripts/db/run-bpm001-verify.mjs' },
  { id: 'P2 ZS-BPM-003 首链幂等写回与跨组织反向（真实引擎，用例数由 EXPECTED_TESTS 权威）', script: 'scripts/db/run-bpm003-verify.mjs' },
];

let pass = 0, failCount = 0;
const results = [];
const record = (id, ok, note = '') => { results.push({ id, ok, note }); ok ? pass++ : failCount++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${note}`); };

for (const suite of suites) {
  console.log(`\n[b09-gate] 运行 ${suite.id} …`);
  const r = spawnSync(process.execPath, [suite.script], { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  // 失败诊断合并 stdout+stderr（套件早期失败——工具链/Docker/迁移——全走 stderr，R1 P2）
  const outTail = (r.stdout ?? '').split('\n').filter((l) => l.startsWith('[')).slice(-4).join(' | ');
  const errTail = String(r.stderr ?? '').trim().split('\n').slice(-2).join(' | ');
  record(suite.id, r.status === 0, r.status === 0 ? outTail.slice(0, 300) : `exit=${r.status ?? 'ERR'}；${(outTail + ' ' + errTail).slice(0, 300)}`);
}

mkdirSync(outDir, { recursive: true });
try {
  writeFileSync(join(outDir, 'gate-report.json'),
    JSON.stringify({ finishedAt: new Date().toISOString(), results, pass, failCount }, null, 2));
} catch { /* 报告落盘失败不影响退出码语义 */ }

console.log('\n===== ZS-OPS-001.E B09 联合门禁汇总 =====');
for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.id} ${r.note}`);
console.log(`合计 ${results.length} 项，失败 ${failCount} 项`);
process.exit(failCount === 0 ? 0 : 1);
