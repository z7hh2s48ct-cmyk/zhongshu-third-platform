/**
 * ZS-BRAND 系列一次性文本迁移执行器（ZS-BRAND-002/003.A/004 复用）。
 *
 * 用法：node scripts/brand/apply-naming-migration.mjs <scope>
 *   backend : services/zhongshu-core（排除 sql/、yudao-ui/ 内部、.github/、.image/、
 *             上游教程《芋道 …》.md 与上游 README，均按 docs/06 第 4 节保留）
 *   sql     : services/zhongshu-core/sql（ZS-BRAND-004，种子新装基线）
 *   web     : apps/zhongshu-admin-web（ZS-BRAND-003.A）
 *   miniapp : apps/zhongshu-miniapp（ZS-BRAND-003.A）
 *
 * 只处理文本文件（8KB 内含 NUL 判定二进制跳过）；UTF-8 解码出现替换符的文件跳过并报告，
 * 不做半套替换。执行后必须运行 scripts/brand/verify-backend-naming.mjs 等对应检查。
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { applyRules, matchesChineseBrandScope, matchesSqlSeedScope, SQL_SEED_REPLACEMENTS } from './naming-rules.mjs';

const root = fileURLToPath(new URL('../../', import.meta.url));
const scope = process.argv[2];
if (!['backend', 'sql', 'web', 'miniapp'].includes(scope)) {
  console.error('usage: node scripts/brand/apply-naming-migration.mjs <backend|sql|web|miniapp>');
  process.exit(2);
}

const prefixes = {
  backend: ['services/zhongshu-core/'],
  sql: ['services/zhongshu-core/sql/'],
  web: ['apps/zhongshu-admin-web/'],
  miniapp: ['apps/zhongshu-miniapp/'],
}[scope];

const exclusions = {
  backend: [
    /^services\/zhongshu-core\/sql\//,
    /^services\/zhongshu-core\/yudao-ui\//,
    /^services\/zhongshu-core\/\.github\//,
    /^services\/zhongshu-core\/\.image\//,
    /^services\/zhongshu-core\/README\.md$/,
    /\/《[^/]*》\.md$/,
  ],
  sql: [],
  web: [/^apps\/zhongshu-admin-web\/\.image\//, /pnpm-lock\.yaml$/],
  miniapp: [
    /^apps\/zhongshu-miniapp\/\.image\//,
    /pnpm-lock\.yaml$/,
    /^apps\/zhongshu-miniapp\/docs\//,
    /^apps\/zhongshu-miniapp\/README\.md$/,
  ],
}[scope];

const files = execFileSync('git', ['ls-files', '-z', ...prefixes], { cwd: root, maxBuffer: 64 * 1024 * 1024 })
  .toString('utf8')
  .split('\0')
  .filter(Boolean)
  .filter((f) => !exclusions.some((re) => re.test(f)));

let changed = 0;
const skipped = [];
for (const file of files) {
  const buf = readFileSync(root + file);
  if (buf.subarray(0, 8192).includes(0)) { skipped.push([file, 'binary']); continue; }
  const text = buf.toString('utf8');
  if (text.includes('\uFFFD') && !buf.includes(0)) { skipped.push([file, 'non-utf8']); continue; }
  let next = applyRules(text, { chinese: matchesChineseBrandScope(file) });
  if (scope === 'sql' && matchesSqlSeedScope(file)) {
    for (const [from, to] of SQL_SEED_REPLACEMENTS) next = next.split(from).join(to);
  }
  if (next !== text) { writeFileSync(root + file, next, 'utf8'); changed++; }
}
console.log(JSON.stringify({ scope, scanned: files.length, changed, skipped }, null, 2));
