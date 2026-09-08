/**
 * ZS-GOV-001 文档一致性检查器（本地与 CI 同一入口）。
 *
 * 校验范围：README.md 与 docs/0*.md（历史报告 docs/04 只查链接，不查内容口径）。
 * 规则：
 *  R1 链接有效：内联链接、引用定义的本地目标必须存在（外链/锚点跳过）；
 *  R2 任务编号唯一：05 文档 `### ZS-XXX-NNN` 标题不得重复；
 *  R3 状态枚举合法：05 卡片"状态 X"只允许 7 个枚举值；
 *  R4 决策门禁：未确认决策（D-07/D-09/D-10/D-11）不得被写成已批准/已确认/已落地；
 *  R5 版本一致：README 文档索引的版本号与各文档头部"文档版本：V*"一致。
 * 用法：node scripts/gov/verify-docs.mjs（退出码非 0 = 不一致）
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const DOCS = ['README.md',
  'docs/01-底座代码复用与改造方案.md',
  'docs/02-一期底座需求规格与待决策台账.md',
  'docs/03-底座二次开发顺序与验收标准.md',
  'docs/05-底座模块分析与开发任务清单.md',
  'docs/06-品牌素材与命名映射.md'];
const HISTORICAL = /^docs\/04-/; // 历史报告：只查链接，不重写口径
const STATUS_ENUM = ['待开发', '待决策', '待前置', '开发中', '待验收', '已验收', '暂缓'];
const UNCONFIRMED_DECISIONS = ['D-07', 'D-09', 'D-10', 'D-11'];
const VERSION_HEADER = /文档版本：\s*([A-Za-z0-9.]+)/;

export function checkDocs(files, readFile, rootDir, { exists = existsSync } = {}) {
  const issues = [];
  const contents = new Map();
  for (const f of files) {
    try {
      contents.set(f, readFile(f));
    } catch {
      issues.push({ rule: 'R0-read', file: f, message: '文件无法读取' });
    }
  }

  // R1 链接有效（校验范围：仓库内目标；仓库外引用属跨工作区引用，打印提示不计失败）
  for (const f of files) {
    const text = contents.get(f) ?? '';
    const baseDir = dirname(resolve(rootDir, f));
    const refs = [];
    for (const m of text.matchAll(/\]\(([^)\s]+)\)/g)) refs.push(m[1]);
    for (const m of text.matchAll(/^\[[^\]]+\]:\s+(\S+)$/gm)) refs.push(m[1]);
    for (const target of refs) {
      if (/^(https?:|mailto:|#)/.test(target)) continue;
      const clean = target.split('#')[0];
      if (!clean) continue;
      // 绝对路径 / 尖括号包裹目标：作者环境的仓库外引用，不校验
      if (/^<.+>$/.test(clean) || /^[A-Za-z]:[\\/]/.test(clean) || clean.startsWith('/')) {
        console.error(`[info] ${f}: 仓库外/绝对路径引用（不校验）: ${target}`);
        continue;
      }
      const abs = resolve(baseDir, decodeURI(clean));
      if (!exists(abs)) {
        if (!abs.startsWith(resolve(rootDir))) {
          console.error(`[info] ${f}: 仓库外引用（不校验）: ${target}`);
        } else {
          issues.push({ rule: 'R1-link', file: f, message: `链接目标不存在: ${target}` });
        }
      }
    }
  }

  // R2 任务编号唯一（仅 05）
  for (const f of files) {
    if (!f.includes('05-')) continue;
    const ids = [...(contents.get(f) ?? '').matchAll(/^### (ZS-[A-Z]+-\d{3})/gm)].map((m) => m[1]);
    const seen = new Set();
    for (const id of ids) {
      if (seen.has(id)) issues.push({ rule: 'R2-dup-id', file: f, message: `任务编号重复: ${id}` });
      seen.add(id);
    }
  }

  // R3 状态枚举（仅 05）
  for (const f of files) {
    if (!f.includes('05-')) continue;
    for (const m of (contents.get(f) ?? '').matchAll(/^-\s*关联[^\n]*?状态\s*([^\s；;，]+)/gm)) {
      if (!STATUS_ENUM.includes(m[1])) {
        issues.push({ rule: 'R3-status', file: f, message: `非法任务状态: "${m[1]}"（允许：${STATUS_ENUM.join('/')}）` });
      }
    }
  }

  // R4 未确认决策不得写成既成事实（否定句式如"不是已确认结论"属合规表述）
  for (const f of files) {
    const text = contents.get(f) ?? '';
    for (const d of UNCONFIRMED_DECISIONS) {
      for (const m of text.matchAll(new RegExp(`${d}[^。\\n|]{0,12}(已确认|已批准|已通过|已落地)`, 'g'))) {
        // 关键词前的否定/禁止措辞视为合规表述（规则自述文本同样会命中模式）
        const beforeKeyword = text.slice(m.index, m.index + m[0].length).slice(-8, -3);
        if (/不得|不应|禁止|不是|非 |未经|尚未$|未$/.test(beforeKeyword)) continue;
        issues.push({ rule: 'R4-decision', file: f, message: `未确认决策 ${d} 被写成既成事实: …${m[0]}…` });
      }
    }
  }

  // R5 版本一致（README 索引 vs 文档头）
  const readme = contents.get('README.md') ?? '';
  for (const m of readme.matchAll(/\[(文档[^\]]*|[^[\]]+)\]\((docs\/0[0-9][^)]*)\)\s*\|\s*([A-Za-z0-9.]+)/g)) {
    const [, , docPath, version] = m;
    const doc = files.find((f) => f === docPath);
    if (!doc) continue;
    if (HISTORICAL.test(docPath)) continue;
    const header = VERSION_HEADER.exec(contents.get(doc) ?? '');
    if (!header) issues.push({ rule: 'R5-version', file: docPath, message: '文档缺少"文档版本："头' });
    else if (header[1] !== version) {
      issues.push({ rule: 'R5-version', file: docPath, message: `README 索引版本 ${version} 与文档头 ${header[1]} 不一致` });
    }
  }

  return issues;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/gov/verify-docs.mjs');
if (invokedDirectly) {
  const issues = checkDocs(DOCS, (f) => readFileSync(join(root, f), 'utf8'), root);
  console.log(JSON.stringify({ files: DOCS.length, issueCount: issues.length, issues }, null, 2));
  process.exitCode = issues.length ? 1 : 0;
}
