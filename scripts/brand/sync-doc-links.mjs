/**
 * ZS-BRAND 改名批次同步 README 与 docs 01/02/03/05/06 的链接目标与内联路径
 * （ZS-BRAND-006 要求"同步 README、01/02/03/05、运行/部署引用和当前代码链接；
 * 历史快照/04 报告不重写"，故 04 不在清单内）。
 *
 * 只改写三类片段，正文叙述（含讨论旧名的段落）不动：
 *   1. 内联链接目标 ](../path)
 *   2. 引用定义 [E01]: ../path
 *   3. 同时包含 "/" 与旧名的 quoting span `path`
 * 用法：node scripts/brand/sync-doc-links.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { applyRules } from './naming-rules.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const docs = [
  'README.md',
  'docs/01-底座代码复用与改造方案.md',
  'docs/02-一期底座需求规格与待决策台账.md',
  'docs/03-底座二次开发顺序与验收标准.md',
  'docs/05-底座模块分析与开发任务清单.md',
  'docs/06-品牌素材与命名映射.md',
];
const OLD = /yudao|cn\.iocoder/i;

let changedFiles = 0;
for (const doc of docs) {
  const text = readFileSync(root + doc, 'utf8');
  let changed = 0;
  const sub = (span) => {
    if (!OLD.test(span)) return span;
    const next = applyRules(span, { chinese: false });
    if (next !== span) changed++;
    return next;
  };
  const out = text
    .replace(/\]\(([^)\n]+)\)/g, (_, target) => `](${sub(target)})`)
    .replace(/^(\[[^\]]+\]:\s*)(\S+)$/gm, (_, head, target) => head + sub(target))
    .replace(/`([^`\n]*)`/g, (_, span) => (span.includes('/') && OLD.test(span) ? `\`${sub(span)}\`` : `\`${span}\``));
  if (out !== text) { writeFileSync(root + doc, out, 'utf8'); changedFiles++; console.log(`updated: ${doc} (${changed} spans)`); }
}
console.log(`done: ${changedFiles} file(s) updated`);
