/**
 * ZS-GOV-001 扩展：任务状态统计聚合器（本地与 CI 共用）。
 *
 * 背景：docs/05 §2 与 README 的主任务状态分布长期靠手工数卡维护，已出现漂移
 * （README 停在旧快照、§2 需人工重算），且既有 verify-docs 只校验版本号、不校验
 * 统计数字。本模块从卡片“状态 X”字段实时聚合真实分布，供 verify-docs R6/R7 校验
 * 与 close-task 自动回填共用，替代手工计数，从结构上消除统计漂移。
 *
 * 本模块只读磁盘、不写文件；backfillSection2 为纯文本变换（写盘由 close-task 负责）。
 * 用法：node scripts/gov/task-stats.mjs
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
// §2 分布串匹配跨度（parseSection2Declared 与 backfillSection2 共用，杜绝读写分歧）：
// 从「：」后起，止于首个左括注「（/(」、句号「。」或换行。限界到句/行，避免「§2 声明无
// 尾随括注」时越界吞掉后续 task cards——否则补插的缺失类别会被写进 §2 之外的卡片内容
// （codex 对 ea739b9c 的 P2）。仍截到首个左括号前，排除历史变更里“48 待开发”（无“项”字）噪声。
const DIST_SPAN = '[^（(\\n。]*';
const SECTION2_DECL_RE = new RegExp(`统计（[^）]*）：(${DIST_SPAN})`);
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

// §2 分布串里「N 项<状态>」的数字替换模式（与 close-task 原内联逻辑逐字一致，仅挪位以便复用/测试）
const SECTION2_ITEM_RE = /(\d+)(\s*项\s*(待开发|开发中|待验收|待决策|待前置|已验收|暂缓))/g;

/**
 * 回填 docs/05 §2 统计句（纯函数，幂等）：
 *  ① 把句中已有类别的数字替换为实际计数；
 *  ② 补插句中缺失但实际非零的类别——修复「某状态首次出现（如暂缓）时只改已有数字、
 *     从不插入新类别，导致 §2 分布漏计、合计对不上卡片总数」的缺陷（codex P2）。
 * 只改「：」到首个「（/」、句号或换行之间的分布串（限界到句/行，防越界写入后续卡片），
 * 保留其后说明；缺失类别插在分布串末尾（即尾随标点之前）；无 §2 声明句时原样返回。
 * @param {string} text docs/05 全文
 * @param {Record<string, number>} counts 各状态实际计数（countStatus().counts）
 * @param {string[]} order 补插缺失类别时的遍历顺序（默认 STATUS_ENUM）
 * @returns {string} 回填后的全文
 */
export function backfillSection2(text, counts, order = STATUS_ENUM) {
  return text.replace(new RegExp(`(统计（[^）]*）：)(${DIST_SPAN})`), (m, head, dist) => {
    let d = dist.replace(SECTION2_ITEM_RE, (mm, num, rest, status) => `${counts[status] ?? 0}${rest}`);
    for (const s of order) {
      if ((counts[s] ?? 0) > 0 && !new RegExp(`项\\s*${s}`).test(d)) d += `、${counts[s]} 项${s}`;
    }
    return head + d;
  });
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
