BEFORE_HEAD=1f6bbb1c START=2026-09-11T12:39:32.8683977+08:00
codex.cmd : OpenAI Codex v0.154.0
所在位置 行:1 字符: 278
+ ... oding utf8; & "$env:APPDATA\npm\codex.cmd" review --commit ea739b9c 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: none
reasoning summaries: none
session id: 01a08ec3-8758-7733-a0af-8d21b67acac9
--------
user
commit ea739b9
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
status --short; git show --stat --oneline ea739b9c; Get-ChildItem -Force -Name 
AGENTS*' in E:\众墅之家AI赋能平台底座
 succeeded in 34ms:
ea739b9c ZS-GOV-001/ZS-OPS-001.A: 修复 codex 双评审 5 条 P2（增量门禁依赖映射 + 统计校验全枚举 + §2 补
插）
 scripts/gov/close-task.mjs           |  8 ++---
 scripts/gov/task-stats.mjs           | 27 +++++++++++++-
 scripts/gov/verify-docs.mjs          | 21 ++++++-----
 scripts/gov/verify-docs.test.mjs     | 70 ++++++++++++++++++++++++++++++++++++
 scripts/ops/run-local-gates.mjs      | 11 +++---
 scripts/ops/run-local-gates.test.mjs | 46 +++++++++++++++++++-----
 6 files changed, 156 insertions(+), 27 deletions(-)

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
show ea739b9c --; Get-ChildItem -Path . -Filter AGENTS* -Recurse -File | Select
-Object -ExpandProperty FullName' in E:\众墅之家AI赋能平台底座
 succeeded in 1967ms:
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@
1.0.30001775\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@
1.0.30001775\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@
1.0.30001791\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@
1.0.30001791\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.
0.30001739\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.
0.30001739\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.
0.30001749\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.
0.30001749\node_modules\caniuse-lite\dist\unpacker\agents.js

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content scripts/gov/task-stats.mjs; Get-Content scripts/gov/close-task.mjs; Get
-Content scripts/ops/run-local-gates.mjs; git rev-parse HEAD; Get-Item E:/AGENT
S.md,E:/AGENTS.override.md -ErrorAction SilentlyContinue' in E:\众墅之家AI赋能平台底座
 exited 1 in 0ms:
/**
 * ZS-GOV-001 扩展：任务状态统计聚合器（本地与 CI 共用）�?
 *
 * 背景：docs/05 §2 �?README 的主任务状态分布长期靠手工数卡维护，已出现漂移
 * （README 停在旧快照、�? 需人工重算），且既�?verify-docs 只校验版本号、不校验
 * 统计数字。本模块从卡片“状�?X”字段实时聚合真实分布，�?verify-docs R6/R7 校验
 * �?close-task 自动回填共用，替代手工计数，从结构上消除统计漂移�?
 *
 * 本模块只读磁盘、不写文件；backfillSection2 为纯文本变换（写盘由 close-task 负责）�?
 * 用法：node scripts/gov/task-stats.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOC05 = 'docs/05-底座模块分析与开发任务清�?md';
const README = 'README.md';

// 状态枚举：�?verify-docs.mjs �?STATUS_ENUM 保持一�?
export const STATUS_ENUM = ['待开�?, '待决�?, '待前�?, '开发中', '待验�?, '已验�?, '暂缓'];

// 主任务卡标题（复�?verify-docs R2 正则�?
const CARD_RE = /^### (ZS-[A-Z]+-\d{3})/gm;
// 卡片状态：内联在�? 关联…状�?X；”行（复�?verify-docs R3 正则）�?
// §16.1 分批子项在表格里、不以�? 关联”开头，天然不计�?�?符合“子项不�?91”�?
const STATUS_RE = /^-\s*关联[^\n]*?状态\s*([^\s�?，]+)/gm;
// §2 当前统计句：“…统计（日期，…）�?3 项待开发、…；0 项已验收（…）”，截到首个左括号前�?
// 以此排除历史变更记录里�?8 待开发”（无“项”字）等旧数字噪声�?
const SECTION2_DECL_RE = /统计（[^）]*）：([^�?]*)/;
const DECL_ITEM_RE = /(\d+)\s*项\s*(待开发|开发中|待验收|待决策|待前置|已验收|暂缓)/g;
// README：“累�?91 项主任务�?3 待开发�? 待决策�? 待前置）�?
const README_DECL_RE = /累计\s*(\d+)\s*项主任务�?[^）]*)�?;
const README_ITEM_RE = /(\d+)\s*(待开发|开发中|待验收|待决策|待前置|已验收|暂缓)/g;

/** 聚合 docs/05 卡片的真实状态分布（实际计数，权威） */
export function countStatus(doc05Text) {
  const counts = Object.fromEntries(STATUS_ENUM.map((s) => [s, 0]));
  const cards = [...doc05Text.matchAll(CARD_RE)].map((m) => m[1]);
  const illegal = [];
  for (const m of doc05Text.matchAll(STATUS_RE)) {
    if (m[1] in counts) counts[m[1]] += 1;
    else illegal.push(m[1]);
  }
  const statusTotal = Object.values(counts).reduce((a, b) => a + b, 0);
  return { counts, cardCount: cards.length, statusTotal, illegal };
}

/** 解析 docs/05 §2 声明的分布（无声明句时返�?null�?*/
export function parseSection2Declared(doc05Text) {
  const m = SECTION2_DECL_RE.exec(doc05Text);
  if (!m) return null;
  const declared = {};
  for (const it of m[1].matchAll(DECL_ITEM_RE)) declared[it[2]] = Number(it[1])
;
  return declared;
}

/** 解析 README 声明的总数与分布（无声明句时返�?null�?*/
export function parseReadmeDeclared(readmeText) {
  const m = README_DECL_RE.exec(readmeText);
  if (!m) return null;
  const declared = {};
  for (const it of m[2].matchAll(README_ITEM_RE)) declared[it[2]] = Number(it[1
]);
  return { total: Number(m[1]), declared };
}

// §2 分布串里「N �?状�?」的数字替换模式（与 close-task 原内联逻辑逐字一致，仅挪位以便复�?测试�?
const SECTION2_ITEM_RE = /(\d+)(\s*项\s*(待开发|开发中|待验收|待决策|待前置|已验收|暂缓))/g;

/**
 * 回填 docs/05 §2 统计句（纯函数，幂等）：
 *  �?把句中已有类别的数字替换为实际计数；
 *  �?补插句中缺失但实际非零的类别——修复「某状态首次出现（如暂缓）时只改已有数字�?
 *     从不插入新类别，导致 §2 分布漏计、合计对不上卡片总数」的缺陷（codex P2）�?
 * 只改「：」到首个「（/」之间的分布串，保留其后说明；无 §2 声明句时原样返回�?
 * @param {string} text docs/05 全文
 * @param {Record<string, number>} counts 各状态实际计数（countStatus().counts�?
 * @param {string[]} order 补插缺失类别时的遍历顺序（默�?STATUS_ENUM�?
 * @returns {string} 回填后的全文
 */
export function backfillSection2(text, counts, order = STATUS_ENUM) {
  return text.replace(/(统计（[^）]*）：)([^�?]*)/, (m, head, dist) => {
    let d = dist.replace(SECTION2_ITEM_RE, (mm, num, rest, status) => `${counts
[status] ?? 0}${rest}`);
    for (const s of order) {
      if ((counts[s] ?? 0) > 0 && !new RegExp(`项\\s*${s}`).test(d)) d += `�?{co
unts[s]} �?{s}`;
    }
    return head + d;
  });
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/gov/task-stats.mjs');
if (invokedDirectly) {
  const doc05 = readFileSync(join(root, DOC05), 'utf8');
  const readme = readFileSync(join(root, README), 'utf8');
  const actual = countStatus(doc05);
  console.log(JSON.stringify({
    卡片实际聚合: actual.counts,
    卡片总数: actual.cardCount,
    状态行总数: actual.statusTotal,
    非法状�? actual.illegal,
    '§2声明': parseSection2Declared(doc05),
    'README声明': parseReadmeDeclared(readme),
  }, null, 2));
}
/**
 * ZS-GOV-001 扩展：任务收口一键工具（本地）�?
 *
 * 把每个任务收口时的“改卡片状�?�?重算 §2 统计 �?同步 README 分布 →（可选）升版�?
 * 压成一条命令，替代 5~6 步手工文档同步；回填�?task-stats 同源，结果由 verify-docs
 * �?R6/R7 门禁兜底校验，从结构上消除统计漂移�?
 *
 * 用法�?
 *   node scripts/gov/close-task.mjs <ZS-ID> <新状�? [--note "说明"] [--date 2026-0
9-10] [--bump V1.15] [--dry-run]
 *   node scripts/gov/close-task.mjs --sync-only [--dry-run]   # 不改卡片，只重算并回�?§2
 + README
 *
 * 安全：先�?--dry-run 预览所有改动，确认无误再去掉写盘；写盘后自动跑 verify-docs 复核�?
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { countStatus, STATUS_ENUM, backfillSection2 } from './task-stats.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOC05 = 'docs/05-底座模块分析与开发任务清�?md';
const README = 'README.md';
const ORDER = ['待开�?, '开发中', '待验�?, '待决�?, '待前�?, '已验�?, '暂缓'];

// ---- 参数解析 ----
const argv = process.argv.slice(2);
const dryRun = argv.includes('--dry-run');
const syncOnly = argv.includes('--sync-only');
const getOpt = (n) => { const i = argv.indexOf(n); return i >= 0 ? argv[i + 1] 
: undefined; };
const note = getOpt('--note');
const date = getOpt('--date') ?? new Date().toISOString().slice(0, 10);
const bump = getOpt('--bump');
const OPTVALS = new Set(['--note', '--date', '--bump']);
const positional = argv.filter((a, i) => !a.startsWith('--') && !OPTVALS.has(ar
gv[i - 1]));
const zsId = syncOnly ? undefined : positional[0];
const newStatus = syncOnly ? undefined : positional[1];

if (!syncOnly) {
  if (!zsId || !newStatus) { console.error('用法：close-task.mjs <ZS-ID> <新状�? [--
dry-run]，或 --sync-only [--dry-run]'); process.exit(2); }
  if (!STATUS_ENUM.includes(newStatus)) { console.error(`非法状态：${newStatus}（允许：$
{STATUS_ENUM.join('/')}）`); process.exit(2); }
}

let doc05 = readFileSync(join(root, DOC05), 'utf8');
let readme = readFileSync(join(root, README), 'utf8');
const changes = [];

// ---- 1. 改卡片状态（定位 ### <ZS-ID> 卡内首个�? 关联…状�?X”行�?---
if (zsId) {
  const lines = doc05.split('\n');
  let inCard = false, done = false, oldStatus = null;
  const cardRe = new RegExp(`^### ${zsId}(：|\\s|$)`);
  for (let i = 0; i < lines.length && !done; i++) {
    if (/^### ZS-/.test(lines[i])) inCard = cardRe.test(lines[i]);
    if (inCard && /^-\s*关联/.test(lines[i])) {
      const m = lines[i].match(/状态\s*([^\s�?，]+)/);
      if (m) { oldStatus = m[1]; lines[i] = lines[i].replace(/(状态\s*)[^\s�?，]+/
, `$1${newStatus}`); done = true; }
    }
  }
  if (!done) { console.error(`未找�?${zsId} 的�? 关联…状态”行`); process.exit(1); }
  doc05 = lines.join('\n');
  changes.push(`${zsId} 状态：${oldStatus} �?${newStatus}`);
}

// ---- 2. 重算实际分布（权威）----
const actual = countStatus(doc05);

// ---- 3. 回填 §2 统计句（改数�?+ 补插句中缺失的非零类别，保持结构与措辞；幂等�?---
const b05 = doc05;
doc05 = backfillSection2(doc05, actual.counts, ORDER);
if (doc05 !== b05) changes.push(`§2 统计回填�?{ORDER.filter((s) => actual.counts[s]
 > 0).map((s) => `${s}${actual.counts[s]}`).join('/')}`);

// ---- 4. 回填 README 摘要（重建为全部非零状态；幂等�?---
const dist = ORDER.filter((s) => actual.counts[s] > 0).map((s) => `${actual.cou
nts[s]} ${s}`).join('�?);
const brm = readme;
readme = readme.replace(/累计\s*\d+\s*项主任务（[^）]*�?, `累计 ${actual.cardCount} 项主任务�
?{dist}）`);
if (readme !== brm) changes.push(`README 摘要回填：累�?${actual.cardCount}�?{dist}）`)
;

// ---- 5. 可选升版（docs/05 �?+ README 索引�?---
if (bump) {
  const v05 = doc05;
  doc05 = doc05.replace(/(文档版本：\s*)[A-Za-z0-9.]+/, `$1${bump}`);
  if (doc05 !== v05) changes.push(`docs/05 版本 �?${bump}`);
  const vrm = readme;
  readme = readme.replace(/(\(docs\/05-[^)]*\)\s*\|\s*)[A-Za-z0-9.]+/, `$1${bum
p}`);
  if (readme !== vrm) changes.push(`README 索引 docs/05 版本 �?${bump}`);
}

// ---- 6. 变更记录建议（半自动：打印，人工确认后贴�?§19 变更记录表，降低误插风险�?---
if (zsId) {
  changes.push(`变更记录建议（贴�?§19 变更记录表）�?{bump ?? 'V<新版�?'}�?{date}）：${zsId} 转�?{n
ewStatus}」，§2/README 统计按卡片实际聚合回�?{note ? `�?{note}` : ''}。`);
}

// ---- 输出 / 写盘 ----
if (!changes.length) {
  console.log('无需修改：docs/05 §2 �?README 已与卡片实际聚合一致（幂等）�?);
} else {
  console.log(`${dryRun ? '[dry-run] 将做' : '已做'} ${changes.length} 项改动：`);
  for (const c of changes) console.log('  - ' + c);
  if (dryRun) {
    console.log('\n未写盘。确认无误后去掉 --dry-run 重跑�?);
  } else {
    writeFileSync(join(root, DOC05), doc05, 'utf8');
    writeFileSync(join(root, README), readme, 'utf8');
    console.log('\n写盘完成，复�?verify-docs�?);
    try {
      console.log(execFileSync('node', ['scripts/gov/verify-docs.mjs'], { cwd: 
root, encoding: 'utf8' }).trim());
    } catch (e) {
      console.log(String(e.stdout ?? '') + String(e.stderr ?? ''));
      process.exitCode = 1;
    }
  }
}
/**
 * ZS-OPS-001.A 本地基线门禁聚合入口（本地与 CI 同一规则）�? *
 * 并发执行全部本地检查，任一失败即整体失败并汇总（结果按门禁定义顺序稳定输出）�? *   G1 来源复制校验器单�?/ G2 品牌命名门禁单测 / G2
b 命名迁移工具单测 / G3 品牌命名全仓扫描
 *   G4 文档一致性单�?/ G5 文档一致性全量（ZS-GOV-001�? G6 模块白名单（ZS-ENG-001�? *   G7 数据�?PG 合
同（ZS-DB-001.A�? G8 Flyway 迁移规范（ZS-DB-003�? G9 配置秘密门禁（ZS-CFG-001.A�? *   G10 Web
 类型检查基线（ZS-CLIENT-005.A，较慢；--fast 跳过�? *   G11 启用模块后端单测�?-mvn 显式启用；需 tools/env.
sh 工具链，排除已登记的上游基线失败�? * PG/多端 E2E 门禁�?ZS-OPS-001.B~.E 批次接入，不在本骨架�? *
 * 提速（ZS-GOV-001 提效方案 P1）：
 *   - 并发：默认按 CPU 核数并发跑门禁（--jobs N 覆盖），反馈时间从「各门禁耗时之和」降到「最慢门禁」�? *   - 增量�?-incr
emental �?git 变更路径只跑受影响门禁（品牌全仓扫描 G3、秘密门�?G9 恒定跑；
 *     变更含门禁脚本自�?scripts/ops/、或存在无法归类且非良性的路径�?fail-safe 回退全量）�? *     仅供日常快速反馈；
批次收口�?CI 必须跑全量（--fast 或含 G10/G11），不得以增量结果代替放行�? *   - --fast 固化：日常开发默�?--fast（跳
过较慢的 G10 vue-tsc），批次收口去掉 --fast 跑全量�? * 用法：node scripts/ops/run-local-gates.mjs
 [--fast] [--mvn] [--incremental] [--jobs N] [--plan]
 */
import { execFile, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { cpus } from 'node:os';

const root = fileURLToPath(new URL('../../', import.meta.url));

// 门禁定义。areas：增量模式下命中这些路径前缀才跑；safety：增量模式恒定跑（全仓合�?安全守卫�?//   或其校验依赖 areas 之外的全仓
状态——如 G5 文档链接存在性依�?docs/ 之外的被链接目标）；
// slow�?-fast 跳过；mvn：仅 --mvn 启用。cmd/mvnArgs �?id 保持与既有基线一致（CI、README 依赖）�?expo
rt const GATES = [
  { id: 'G1 来源复制校验器单�?, cmd: ['node', '--test', 'scripts/verify-source-copy.tes
t.mjs'], areas: ['scripts/verify-source-copy', 'third_party/'] },
  { id: 'G2 品牌命名门禁单测', cmd: ['node', '--test', 'scripts/brand/verify-brand-nami
ng.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G2b 命名迁移工具单测（冻结文件排�?+ 租户域名唯一�?, cmd: ['node', '--test', 'scripts/brand
/apply-naming-migration.test.mjs'], areas: ['scripts/brand/', 'services/zhongsh
u-core/sql/'] },
  { id: 'G3 品牌命名全仓扫描', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs'], 
areas: ['services/', 'apps/', 'scripts/', 'docs/'], safety: true },
  { id: 'G4 文档一致性单�?, cmd: ['node', '--test', 'scripts/gov/verify-docs.test.mjs
'], areas: ['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G5 文档一致性全�?, cmd: ['node', 'scripts/gov/verify-docs.mjs'], areas: ['sc
ripts/gov/', 'docs/', 'README.md'], safety: true },
  { id: 'G6 模块白名�?, cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'], a
reas: ['scripts/eng/', 'services/'] },
  { id: 'G7 数据�?PG 合同', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'], a
reas: ['scripts/db/', 'services/'] },
  { id: 'G8 Flyway 迁移规范', cmd: ['node', 'scripts/db/verify-flyway-migrations.mj
s'], areas: ['scripts/db/', 'services/'] },
  { id: 'G9 配置秘密门禁', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'], ar
eas: ['scripts/cfg/', 'services/', 'apps/'], safety: true },
  { id: 'G10 Web 类型检查基�?, cmd: ['node', 'scripts/client/verify-ts-baseline.mjs'
], areas: ['apps/zhongshu-admin-web/', 'scripts/client/'], slow: true },
  { id: 'G11 启用模块后端单测（common/infra，排除上游基线失败）', areas: ['services/'], mvn: true,
    mvnArgs: '-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!Code
genEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=fa
lse test' },
];

// 增量模式下视为「良性、不触发全量回退」的未归类路径前缀（生成物/评审原始�?计划稿）
const BENIGN_IGNORE = ['outputs/', 'docs/reviews/', '.omx/'];

/** 构造本次要跑的门禁候选（尊重 --fast/--mvn），不含增量筛选�?*/
export function candidateGates({ fast = false, mvn = false } = {}) {
  return GATES.filter((g) => {
    if (g.slow && fast) return false; // G10�?-fast 跳过较慢�?Web 类型检�?    if (g.mv
n && !mvn) return false;  // G11：仅 --mvn 显式启用
    return true;
  });
}

/**
 * 增量筛选：按变更路径选门禁；任何不确定都 fail-safe 回退全量（宁多跑不漏跑）�? * 返回 { gates, mode: 'full'|'in
cremental', reason, changed }�? * @param {object} o
 * @param {boolean} o.fast @param {boolean} o.mvn @param {boolean} o.incrementa
l
 * @param {string[]|null} o.changedFiles 变更路径（相对仓库根，正斜杠）；null 表示由调用方检�? */
export function planGates({ fast = false, mvn = false, incremental = false, cha
ngedFiles = null }) {
  const candidates = candidateGates({ fast, mvn });
  if (!incremental) return { gates: candidates, mode: 'full', reason: '未启�?--in
cremental，跑全量', changed: changedFiles ?? [] };

  const changed = changedFiles ?? [];
  if (!changed.length) return { gates: candidates, mode: 'full', reason: '增量：无变
更或变更检测失败，回退全量', changed };
  if (changed.some((f) => f.startsWith('scripts/ops/'))) return { gates: candid
ates, mode: 'full', reason: '增量：变更含门禁脚本自身（scripts/ops/），回退全量', changed };

  // 未归类且非良性路�?�?保守回退全量（无法判断影响面�?  const allAreas = candidates.flatMap((g) => g
.areas ?? []);
  const unmapped = changed.filter((f) => !allAreas.some((a) => f.startsWith(a))
 && !BENIGN_IGNORE.some((b) => f.startsWith(b)));
  if (unmapped.length) return { gates: candidates, mode: 'full', reason: `增量�?{
unmapped.length} 个变更路径无法归类（${unmapped.slice(0, 3).join(', ')}${unmapped.length 
> 3 ? ' �? : ''}），回退全量`, changed };

  const selected = candidates.filter((g) => g.safety || (g.areas ?? []).some((a
) => changed.some((f) => f.startsWith(a))));
  return { gates: selected, mode: 'incremental', reason: `增量：按 ${changed.length
} 个变更路径选中 ${selected.length}/${candidates.length} 门禁`, changed };
}

/** 检测工作树相对 HEAD 的变更路径（含已跟踪修改与未跟踪新文件）；失败返�?[]�?*/
function detectChangedFiles() {
  try {
    const opts = { cwd: root, encoding: 'utf8' };
    // --no-renames：跨目录 rename 默认只报目标路径，会漏掉源路径所属区域的门禁（如 Web→miniapp
    // 移动 .ts 只报 miniapp 侧、漏选校 Web 导入�?G10）；关闭 rename 检测使�?�?+目标(�?都上报�?    con
st tracked = spawnSync('git', ['-c', 'core.quotepath=false', 'diff', '--name-on
ly', '--no-renames', 'HEAD'], opts);
    const untracked = spawnSync('git', ['-c', 'core.quotepath=false', 'ls-files
', '--others', '--exclude-standard'], opts);
    if (tracked.status !== 0 || untracked.status !== 0) return [];
    const paths = [...String(tracked.stdout ?? '').split('\n'), ...String(untra
cked.stdout ?? '').split('\n')]
      .map((s) => s.trim().replaceAll('\\', '/')).filter(Boolean);
    return [...new Set(paths)];
  } catch { return []; }
}

/** 并发跑单个门禁，返回 { id, status, ms, tail }（不抛异常，失败�?status 表达）�?*/
function runGate(gate) {
  const started = Date.now();
  return new Promise((resolve) => {
    const done = (status, output) => resolve({
      id: gate.id, status, ms: Date.now() - started,
      tail: String(output ?? '').trimEnd().split('\n').slice(-4).join(' | ').sl
ice(0, 400),
    });
    if (gate.mvnArgs) {
      // Maven 门禁需先注�?tools 工具链环境（JDK17/Maven 不在系统 PATH�?      const child = sp
awn('bash', ['-c', 'source tools/env.sh && cd services/zhongshu-core && MSYS_NO
_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs],
        { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] });
      let out = '';
      child.stdout.on('data', (d) => { out += d; });
      child.stderr.on('data', (d) => { out += d; });
      child.on('error', (e) => done('FAIL', out + String(e)));
      child.on('close', (code) => done(code === 0 ? 'PASS' : 'FAIL', out));
    } else {
      execFile(gate.cmd[0], gate.cmd.slice(1), { cwd: root, maxBuffer: 64 * 102
4 * 1024, encoding: 'utf8' },
        (err, stdout, stderr) => done(err ? 'FAIL' : 'PASS', err ? String(stdou
t ?? '') + String(stderr ?? '') : String(stdout ?? '')));
    }
  });
}

/** �?jobs 上限并发跑门禁；结果�?gates 定义顺序返回（与完成顺序无关），完成即时打印进度�?*/
async function runGates(gates, jobs) {
  const results = new Array(gates.length);
  let next = 0;
  const worker = async () => {
    while (next < gates.length) {
      const i = next++;
      const r = await runGate(gates[i]);
      results[i] = r;
      console.log(`[${r.status}] ${r.id} (${r.ms}ms)`);
    }
  };
  await Promise.all(Array.from({ length: Math.min(jobs, gates.length) }, worker
));
  return results;
}

async function main() {
  const argv = process.argv.slice(2);
  const fast = argv.includes('--fast');
  const mvn = argv.includes('--mvn');
  const incremental = argv.includes('--incremental');
  const planOnly = argv.includes('--plan');
  const jobsIdx = argv.indexOf('--jobs');
  const jobs = Math.max(1, jobsIdx >= 0 ? (Number(argv[jobsIdx + 1]) || 1) : (c
pus().length || 4));

  const changedFiles = incremental ? detectChangedFiles() : [];
  const plan = planGates({ fast, mvn, incremental, changedFiles });
  if (incremental) console.log(`[增量] ${plan.reason}`);

  if (planOnly) {
    console.log(`将运�?${plan.gates.length} 项门禁（--plan，不执行）：`);
    for (const g of plan.gates) console.log('  - ' + g.id);
    return 0;
  }

  console.log(`运行 ${plan.gates.length} 项门禁（模式 ${plan.mode}，并�?${Math.min(jobs, 
plan.gates.length)}${fast ? '�?-fast' : ''}${mvn ? '�?-mvn' : ''}）…`);
  const t0 = Date.now();
  const results = await runGates(plan.gates, jobs);
  const totalMs = Date.now() - t0;

  console.log('\n===== 本地基线门禁汇�?=====');
  for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
  const failed = results.filter((r) => r.status === 'FAIL');
  if (failed.length) {
    console.log('\n----- 失败详情（输出末尾）-----');
    for (const r of failed) console.log(`[FAIL] ${r.id}\n  ${r.tail}`);
  }
  console.log(`合计 ${results.length} 项，失败 ${failed.length} 项（并发耗时 ${totalMs}ms）`
);
  return failed.length ? 1 : 0;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/'
).endsWith('scripts/ops/run-local-gates.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); pr
ocess.exit(1); });
}
1f6bbb1c1241326763f5500de34f3f8d0c3f118f

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content -Encoding utf8 scripts/gov/verify-docs.mjs; Get-ChildItem -Force -Name;
 node --test scripts/gov/verify-docs.test.mjs scripts/ops/run-local-gates.test.
mjs' in E:\众墅之家AI赋能平台底座
 succeeded in 77ms:
[info] README.md: 仓库外/绝对路径引用（不校验）: /Users/x/a.md
✔ 正常文档通过（链接/编号/状态/决策/版本均一致） (2.075ms)
✔ 故意破坏链接会失败 (0.3126ms)
✔ 任务编号重复会失败 (0.194ms)
✔ 非法任务状态会失败，合法枚举通过 (0.1586ms)
✔ 未确认决策（D-10）被写成既成事实会失败，否定表述通过 (0.1796ms)
✔ README 索引版本与文档头不一致会失败 (0.4865ms)
✔ 仓库外引用与外链跳过，不误报 (0.5721ms)
✔ R6：§2 声明统计与卡片实际聚合不一致会失败 (0.2293ms)
✔ R6：§2 声明与卡片实际聚合一致时不报 R6 (0.118ms)
✔ R7：README 声明分布与 docs/05 实际不一致会失败 (0.2455ms)
✔ R6/R7：无声明句时跳过，不误报 (0.1131ms)
✔ R6：§2 省略实际非零的类别（漏计）会失败 (0.1534ms)
✔ R7：README 省略实际非零的类别（漏计）会失败 (0.0852ms)
✔ backfillSection2：替换句中已有类别的数字，保留其后说明 (0.3358ms)
✔ backfillSection2：补插句中缺失但实际非零的类别（P2#5 漏计修复） (0.2853ms)
✔ backfillSection2：幂等——已含全部非零类别时二次回填不重复补插 (0.1255ms)
✔ backfillSection2：无 §2 声明句时原样返回 (0.0641ms)
✔ GATES 定义稳定：12 项，G3/G5/G9 为安全守卫，G10 慢检查，G11 需 mvn (1.5012ms)
✔ candidateGates 默认（无 fast/mvn）：含 G10、排除 G11 (0.1547ms)
✔ candidateGates --fast：跳过 G10 与 G11，恰为 10 项（与 CI 一致） (0.1048ms)
✔ candidateGates --mvn（非 fast）：G10 与 G11 均启用，共 12 项 (0.0703ms)
✔ candidateGates --fast --mvn：跳过 G10、启用 G11 (0.1867ms)
✔ planGates 非增量：mode=full，门禁等于候选 (0.1117ms)
✔ planGates 增量 + 仅文档变更：选中 G3/G4/G5/G9，跳过 G1/G2/G6/G7/G8 (0.1326ms)
✔ planGates 增量 + 后端变更：选中 G3/G5/G6/G7/G8/G9（services 命中 + G5 安全守卫） (0.064ms)
✔ planGates 增量 + 后端变更 + mvn：追加 G11 (0.2041ms)
✔ planGates 增量 fail-safe：变更含门禁脚本自身 scripts/ops/ → 回退全量 (0.1411ms)
✔ planGates 增量 fail-safe：无法归类且非良性路径 → 回退全量 (0.2779ms)
✔ planGates 增量 fail-safe：无变更/检测失败 → 回退全量 (0.079ms)
✔ planGates 增量 + 良性未归类路径 outputs/：不回退全量，仅跑安全守卫 G3/G5/G9 (0.0572ms)
✔ planGates 增量：安全守卫 G3/G5/G9 在任何选中集内恒定出现 (0.0941ms)
✔ P2#1 planGates 增量 + SQL 种子变更：G2b 被选中（跨租户域名唯一性依赖种子数据） (0.0503ms)
✔ P2#3 planGates 增量 + docs/ 之外被链接目标变更：G5 恒定选中（链接存在性依赖） (0.8149ms)
✔ P2#2 planGates 增量 + 跨目录 rename 源路径上报（--no-renames 效果）：源区域门禁被选中 (0.1161ms)
ℹ tests 34
ℹ suites 0
ℹ pass 34
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 67.8114

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
diff ea739b9c HEAD -- scripts/gov scripts/ops; node --input-type=module -e \"im
port {backfillSection2,STATUS_ENUM} from './scripts/gov/task-stats.mjs'; const 
t='统计（2026-09-10）：1 项待开发。\\n\\n## 3. Tasks\\n### ZS-ENG-001：A\\n- 关联：WP；状态 待开发；
前置 无。\\n'; console.log(backfillSection2(t, Object.fromEntries(STATUS_ENUM.map(s
=>[s,1]))));\"" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
统计（2026-09-10）：1 项待开发。

## 3. Tasks
### ZS-ENG-001：A
- 关联：WP；状态 待开发；前置 无。
、1 项待决策、1 项待前置、1 项开发中、1 项待验收、1 项已验收、1 项暂缓

codex
The missing-status backfill can insert statistics outside §2 when the declarati
on lacks a parenthetical explanation. All 34 existing tests pass, but they do n
ot cover this insertion case.

Review comment:

- [P2] Bound missing-status insertion to the statistics sentence — E:\众墅之家AI赋能平
台底座\scripts\gov\task-stats.mjs:82-85
  When the §2 statistics sentence has no trailing parenthetical explanation, `[
^（(]*` consumes subsequent sections until another opening parenthesis or EOF. A
ppending missing statuses to `d` therefore writes them into unrelated document 
content instead of §2. For example, a declaration ending in `1 项待开发。\n` followe
d by task cards receives the new categories after those cards. Bound the distri
bution to its sentence or line and insert missing categories before its trailin
g punctuation.
The missing-status backfill can insert statistics outside §2 when the declaration lacks a parenthetical explanation. All 34 existing tests pass, but they do not cover this insertion case.

Review comment:

- [P2] Bound missing-status insertion to the statistics sentence — E:\众墅之家AI赋能平台底座\scripts\gov\task-stats.mjs:82-85
  When the §2 statistics sentence has no trailing parenthetical explanation, `[^（(]*` consumes subsequent sections until another opening parenthesis or EOF. Appending missing statuses to `d` therefore writes them into unrelated document content instead of §2. For example, a declaration ending in `1 项待开发。\n` followed by task cards receives the new categories after those cards. Bound the distribution to its sentence or line and insert missing categories before its trailing punctuation.
AFTER_HEAD=1f6bbb1c DONE=2026-09-11T12:40:20.1063858+08:00 CODEX_DONE
