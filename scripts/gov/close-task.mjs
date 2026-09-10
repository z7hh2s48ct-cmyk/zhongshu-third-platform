/**
 * ZS-GOV-001 扩展：任务收口一键工具（本地）。
 *
 * 把每个任务收口时的“改卡片状态 → 重算 §2 统计 → 同步 README 分布 →（可选）升版”
 * 压成一条命令，替代 5~6 步手工文档同步；回填与 task-stats 同源，结果由 verify-docs
 * 的 R6/R7 门禁兜底校验，从结构上消除统计漂移。
 *
 * 用法：
 *   node scripts/gov/close-task.mjs <ZS-ID> <新状态> [--note "说明"] [--date 2026-09-10] [--bump V1.15] [--dry-run]
 *   node scripts/gov/close-task.mjs --sync-only [--dry-run]   # 不改卡片，只重算并回填 §2 + README
 *
 * 安全：先加 --dry-run 预览所有改动，确认无误再去掉写盘；写盘后自动跑 verify-docs 复核。
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { countStatus, STATUS_ENUM, backfillSection2 } from './task-stats.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOC05 = 'docs/05-底座模块分析与开发任务清单.md';
const README = 'README.md';
const ORDER = ['待开发', '开发中', '待验收', '待决策', '待前置', '已验收', '暂缓'];

// ---- 参数解析 ----
const argv = process.argv.slice(2);
const dryRun = argv.includes('--dry-run');
const syncOnly = argv.includes('--sync-only');
const getOpt = (n) => { const i = argv.indexOf(n); return i >= 0 ? argv[i + 1] : undefined; };
const note = getOpt('--note');
const date = getOpt('--date') ?? new Date().toISOString().slice(0, 10);
const bump = getOpt('--bump');
const OPTVALS = new Set(['--note', '--date', '--bump']);
const positional = argv.filter((a, i) => !a.startsWith('--') && !OPTVALS.has(argv[i - 1]));
const zsId = syncOnly ? undefined : positional[0];
const newStatus = syncOnly ? undefined : positional[1];

if (!syncOnly) {
  if (!zsId || !newStatus) { console.error('用法：close-task.mjs <ZS-ID> <新状态> [--dry-run]，或 --sync-only [--dry-run]'); process.exit(2); }
  if (!STATUS_ENUM.includes(newStatus)) { console.error(`非法状态：${newStatus}（允许：${STATUS_ENUM.join('/')}）`); process.exit(2); }
}

let doc05 = readFileSync(join(root, DOC05), 'utf8');
let readme = readFileSync(join(root, README), 'utf8');
const changes = [];

// ---- 1. 改卡片状态（定位 ### <ZS-ID> 卡内首个“- 关联…状态 X”行）----
if (zsId) {
  const lines = doc05.split('\n');
  let inCard = false, done = false, oldStatus = null;
  const cardRe = new RegExp(`^### ${zsId}(：|\\s|$)`);
  for (let i = 0; i < lines.length && !done; i++) {
    if (/^### ZS-/.test(lines[i])) inCard = cardRe.test(lines[i]);
    if (inCard && /^-\s*关联/.test(lines[i])) {
      const m = lines[i].match(/状态\s*([^\s；;，]+)/);
      if (m) { oldStatus = m[1]; lines[i] = lines[i].replace(/(状态\s*)[^\s；;，]+/, `$1${newStatus}`); done = true; }
    }
  }
  if (!done) { console.error(`未找到 ${zsId} 的“- 关联…状态”行`); process.exit(1); }
  doc05 = lines.join('\n');
  changes.push(`${zsId} 状态：${oldStatus} → ${newStatus}`);
}

// ---- 2. 重算实际分布（权威）----
const actual = countStatus(doc05);

// ---- 3. 回填 §2 统计句（改数字 + 补插句中缺失的非零类别，保持结构与措辞；幂等）----
const b05 = doc05;
doc05 = backfillSection2(doc05, actual.counts, ORDER);
if (doc05 !== b05) changes.push(`§2 统计回填：${ORDER.filter((s) => actual.counts[s] > 0).map((s) => `${s}${actual.counts[s]}`).join('/')}`);

// ---- 4. 回填 README 摘要（重建为全部非零状态；幂等）----
const dist = ORDER.filter((s) => actual.counts[s] > 0).map((s) => `${actual.counts[s]} ${s}`).join('、');
const brm = readme;
readme = readme.replace(/累计\s*\d+\s*项主任务（[^）]*）/, `累计 ${actual.cardCount} 项主任务（${dist}）`);
if (readme !== brm) changes.push(`README 摘要回填：累计 ${actual.cardCount}（${dist}）`);

// ---- 5. 可选升版（docs/05 头 + README 索引）----
if (bump) {
  const v05 = doc05;
  doc05 = doc05.replace(/(文档版本：\s*)[A-Za-z0-9.]+/, `$1${bump}`);
  if (doc05 !== v05) changes.push(`docs/05 版本 → ${bump}`);
  const vrm = readme;
  readme = readme.replace(/(\(docs\/05-[^)]*\)\s*\|\s*)[A-Za-z0-9.]+/, `$1${bump}`);
  if (readme !== vrm) changes.push(`README 索引 docs/05 版本 → ${bump}`);
}

// ---- 6. 变更记录建议（半自动：打印，人工确认后贴入 §19 变更记录表，降低误插风险）----
if (zsId) {
  changes.push(`变更记录建议（贴入 §19 变更记录表）：${bump ?? 'V<新版本>'}（${date}）：${zsId} 转「${newStatus}」，§2/README 统计按卡片实际聚合回填${note ? `；${note}` : ''}。`);
}

// ---- 输出 / 写盘 ----
if (!changes.length) {
  console.log('无需修改：docs/05 §2 与 README 已与卡片实际聚合一致（幂等）。');
} else {
  console.log(`${dryRun ? '[dry-run] 将做' : '已做'} ${changes.length} 项改动：`);
  for (const c of changes) console.log('  - ' + c);
  if (dryRun) {
    console.log('\n未写盘。确认无误后去掉 --dry-run 重跑。');
  } else {
    writeFileSync(join(root, DOC05), doc05, 'utf8');
    writeFileSync(join(root, README), readme, 'utf8');
    console.log('\n写盘完成，复核 verify-docs：');
    try {
      console.log(execFileSync('node', ['scripts/gov/verify-docs.mjs'], { cwd: root, encoding: 'utf8' }).trim());
    } catch (e) {
      console.log(String(e.stdout ?? '') + String(e.stderr ?? ''));
      process.exitCode = 1;
    }
  }
}
