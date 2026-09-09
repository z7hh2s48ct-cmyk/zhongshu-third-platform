/**
 * ZS-BRAND-006.A 品牌/代码命名残留门禁（本地与 CI 同一入口）。
 *
 * 用法：node scripts/brand/verify-brand-naming.mjs
 * 扫描 git 跟踪文件中的旧产品标识（文件名 + 文本内容）：
 *   cn.iocoder / yudao / Yudao / YUDAO / youdao / 芋道 / unibest（上游模板标识）
 * 按 scripts/brand/brand-naming-allowlist.json 判定：
 *   path 条目整文件放行；content 条目仅放行命中其 content 正则的匹配；
 *   限期兼容条目 expires 到期后视同未放行（门禁失败）。
 * 未放行的残留使进程以非零退出；不追求全仓零命中，不抹去第三方来源。
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const allowlistFile = 'scripts/brand/brand-naming-allowlist.json';

// 旧产品标识模式（docs/06 第 2 节；unibest 为登记的上游模板标识；
// yd- 为登记缩写（docs/06 第 4.3 节限期兼容），纳入模式使到期撤除机制生效）
const PATTERNS = [
  { name: 'cn.iocoder', re: /cn\.iocoder/g },
  { name: 'yudao', re: /yudao/gi },
  { name: 'youdao', re: /youdao/gi },
  { name: '芋道', re: /芋道/g },
  { name: 'unibest', re: /unibest/gi },
  { name: 'yd-prefix', re: /\byd-[a-z]/g },
];
// hotfix-B: SVG 是 XML 文本，可含 <text>/<title> 等品牌串，不再作为二进制跳过（导出以供单测固化）
export const BINARY = /\.(png|jpe?g|gif|ico|bmp|webp|ttf|woff2?|eot|mp3|mp4|xdb|jar|zip|gz)$/i;
const SKIP_DIRS = /(^|\/)(\.git|node_modules|dist|dist-prod|target|unpackage|\.vite|\.idea)(\/|$)/;

export function loadAllowlist(read = (p) => readFileSync(p, 'utf8')) {
  const parsed = JSON.parse(read(allowlistFile));
  if (!Array.isArray(parsed.entries)) throw new Error('invalid allowlist');
  return parsed.entries.map((e) => ({
    ...e,
    pathRe: e.path ? new RegExp(e.path) : null,
    contentRes: (e.content ?? []).map((c) => new RegExp(c, 'g')), // g 标志用于 coverage check 遍历所有匹配；每次使用前重置 lastIndex
    expiresAt: e.expires ? new Date(`${e.expires}T23:59:59Z`) : null,
  }));
}

function entryExpired(entry, now = new Date()) {
  return entry.expiresAt !== null && now > entry.expiresAt;
}

/** 对单个文件的命中做白名单判定；返回未放行命中。 */
export function judge(relativePath, text, entries, now = new Date()) {
  // hotfix-B P1-3: pathPass 仅对无 scope 或 scope=path 的条目生效；
  // scope=content 的条目必须经内容匹配判定，不得整文件豁免
  const pathPass = entries.some((e) =>
    e.pathRe && e.pathRe.test(relativePath) &&
    (!e.scope || e.scope === 'path') &&
    !entryExpired(e, now)
  );
  const hits = [];
  for (const { name, re } of PATTERNS) {
    re.lastIndex = 0; // 模块级 /g 正则，逐文件重置
    let m;
    while ((m = re.exec(text))) hits.push({ pattern: name, index: m.index, text: m[0] });
  }
  if (pathPass) return { allowed: hits, violations: [] };
  const violations = [];
  for (const hit of hits) {
    const fragment = text.slice(Math.max(0, hit.index - 40), hit.index + 40);
    // hotfix-B P2-7: 白名单 content 正则的匹配区间必须覆盖命中点，
    // 防止邻近 40 字符内的不相关白名单串庇护产品可见品牌残留
    const hitOffset = Math.min(40, hit.index);
    const ok = entries.some((e) => {
      if (!e.scope || e.scope !== 'content' || entryExpired(e, now)) return false;
      if (e.pathRe && !e.pathRe.test(relativePath)) return false;
      return e.contentRes.some((cre) => {
        cre.lastIndex = 0;
        let cm;
        while ((cm = cre.exec(fragment))) {
          if (cm.index <= hitOffset && hitOffset < cm.index + cm[0].length) return true;
        }
        return false;
      });
    });
    if (!ok) violations.push(hit);
  }
  return { allowed: hits.filter((h) => !violations.includes(h)), violations };
}

export function scanTree(entries, now = new Date()) {
  const files = execFileSync('git', ['ls-files', '-z'], { cwd: root, maxBuffer: 64 * 1024 * 1024 })
    .toString()
    .split('\0')
    .filter(Boolean);
  const report = { scanned: 0, allowedHits: 0, violations: [] };
  for (const rel of files) {
    if (SKIP_DIRS.test(rel)) continue;
    // hotfix-B P2-9: 文件/目录名检查——对完整相对路径应用全部 PATTERNS，
    // 不仅检查 basename + 3 个模式，避免目录残留与遗漏模式逃逸
    const nameHits = [];
    for (const { name, re } of PATTERNS) {
      re.lastIndex = 0;
      let m;
      while ((m = re.exec(rel))) nameHits.push({ pattern: name, index: m.index, text: m[0] });
    }
    const abs = `${root}${rel.replaceAll('/', process.platform === 'win32' ? '\\' : '/')}`;
    if (nameHits.length) {
      const { violations } = judge(rel, rel, entries, now); // 用路径文本自身做判定（含路径白名单）
      for (const v of violations) report.violations.push({ path: rel, pattern: v.pattern, sample: rel });
      if (!violations.length) report.allowedHits += nameHits.length;
    }
    if (BINARY.test(rel) || !existsSync(abs) || !statSync(abs).isFile()) continue;
    let text;
    try {
      const buf = readFileSync(abs);
      if (buf.subarray(0, 8192).includes(0)) continue;
      text = buf.toString('utf8');
    } catch {
      continue;
    }
    report.scanned++;
    const { allowed, violations } = judge(rel, text, entries, now);
    report.allowedHits += allowed.length;
    for (const v of violations) {
      report.violations.push({ path: rel, pattern: v.pattern, sample: text.slice(Math.max(0, v.index - 30), v.index + 30).replaceAll('\n', ' ') });
    }
  }
  return report;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/brand/verify-brand-naming.mjs');
if (invokedDirectly) {
  try {
    const entries = loadAllowlist();
    const report = scanTree(entries);
    console.log(JSON.stringify({ ...report, violations: report.violations.slice(0, 50) }, null, 2));
    console.error(`scanned=${report.scanned} allowedHits=${report.allowedHits} violations=${report.violations.length}`);
    process.exitCode = report.violations.length ? 1 : 0;
  } catch (error) {
    console.error(`brand naming check failed: ${error.message}`);
    process.exitCode = 2;
  }
}
