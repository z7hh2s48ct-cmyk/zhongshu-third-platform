/**
 * ZS-GOV-001 扩展：任务状态统计聚合器（本地与 CI 共用）。
 *
 * 背景：docs/05 §2 与 README 的主任务状态分布长期靠手工数卡维护，已出现漂移
 * （README 停在旧快照、§2 需人工重算），且既有 verify-docs 只校验版本号、不校验
 * 统计数字。本模块从卡片“状态 X”字段实时聚合真实分布，供 verify-docs R6/R7 校验
 * 与 close-task 自动回填共用，替代手工计数，从结构上消除统计漂移。
 *
 * 只读，不改文档。用法：node scripts/gov/task-stats.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOC05 = 'docs/05-底座模块分析与开发任务清单.md';
const README = 'README.md';

// 状态枚举：与 verify-docs.mjs 的 STATUS_ENUM 保持一致
export const STATUS_ENUM = ['待开发', '待决策', '待前置', '开发中', '待验收', '已验收', '暂缓'];

// 主任务卡标题（复用 verify-docs R2 正则）
const CARD_RE = /^### (ZS-[A-Z]+-\d{3})/gm;
// 卡片状态：内联在“- 关联…状态 X；”行（复用 verify-docs R3 正则）。
// §16.1 分批子项在表格里、不以“- 关联”开头，天然不计入 → 符合“子项不进 91”。
const STATUS_RE = /^-\s*关联[^\n]*?状态\s*([^\s；;，]+)/gm;
// §2 当前统计句：“…统计（日期，…）：43 项待开发、…；0 项已验收（…）”，截到首个左括号前，
// 以此排除历史变更记录里“48 待开发”（无“项”字）等旧数字噪声。
const SECTION2_DECL_RE = /统计（[^）]*）：([^（(]*)/;
const DECL_ITEM_RE = /(\d+)\s*项\s*(待开发|开发中|待验收|待决策|待前置|已验收|暂缓)/g;
// README：“累计 91 项主任务（83 待开发、2 待决策、6 待前置）”
const README_DECL_RE = /累计\s*(\d+)\s*项主任务（([^）]*)）/;
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

/** 解析 docs/05 §2 声明的分布（无声明句时返回 null） */
export function parseSection2Declared(doc05Text) {
  const m = SECTION2_DECL_RE.exec(doc05Text);
  if (!m) return null;
  const declared = {};
  for (const it of m[1].matchAll(DECL_ITEM_RE)) declared[it[2]] = Number(it[1]);
  return declared;
}

/** 解析 README 声明的总数与分布（无声明句时返回 null） */
export function parseReadmeDeclared(readmeText) {
  const m = README_DECL_RE.exec(readmeText);
  if (!m) return null;
  const declared = {};
  for (const it of m[2].matchAll(README_ITEM_RE)) declared[it[2]] = Number(it[1]);
  return { total: Number(m[1]), declared };
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/gov/task-stats.mjs');
if (invokedDirectly) {
  const doc05 = readFileSync(join(root, DOC05), 'utf8');
  const readme = readFileSync(join(root, README), 'utf8');
  const actual = countStatus(doc05);
  console.log(JSON.stringify({
    卡片实际聚合: actual.counts,
    卡片总数: actual.cardCount,
    状态行总数: actual.statusTotal,
    非法状态: actual.illegal,
    '§2声明': parseSection2Declared(doc05),
    'README声明': parseReadmeDeclared(readme),
  }, null, 2));
}
