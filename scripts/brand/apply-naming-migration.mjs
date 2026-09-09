/**
 * ZS-BRAND 系列一次性文本迁移执行器（ZS-BRAND-002/003.A/004 复用）。
 *
 * 用法：node scripts/brand/apply-naming-migration.mjs <scope> [--dry-run]
 *   backend : services/zhongshu-core（排除 sql/、yudao-ui/ 内部、.github/、.image/、
 *             上游教程《芋道 …》.md 与上游 README，均按 docs/06 第 4 节保留）
 *   sql     : services/zhongshu-core/sql（ZS-BRAND-004，种子新装基线）
 *   web     : apps/zhongshu-admin-web（ZS-BRAND-003.A）
 *   miniapp : apps/zhongshu-miniapp（ZS-BRAND-003.A）
 *   --dry-run : 只报告将被改写的文件，不写盘（供评审与单测复用）
 *
 * 只处理文本文件（8KB 内含 NUL 判定二进制跳过）；UTF-8 解码出现替换符的文件跳过并报告，
 * 不做半套替换。执行后必须运行 scripts/brand/verify-backend-naming.mjs 等对应检查。
 *
 * hotfix-C P2-1：新增跨 scope 生效的 PRESERVED（冻结/必要保留）排除。此前 exclusions.sql
 * 为空数组，`apply-naming-migration.mjs sql` 会把已冻结的存量迁移脚本
 * sql/postgresql/upgrades/20260908_brand_rename_zszj.sql 一并改写——旧值谓词被改成新值，
 * 脚本从此无法升级旧基线库，违反 docs/06 第 4.1 节「迁移脚本冻结，不随后续改名批次重写」。
 * 排除必须发生在选文件阶段（applyRules 之前），而非仅在额外的种子替换处绕过。
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { execFileSync } from 'node:child_process';
import { applyRules, matchesChineseBrandScope, matchesSqlSeedScope, SQL_SEED_REPLACEMENTS } from './naming-rules.mjs';

export const root = fileURLToPath(new URL('../../', import.meta.url));

export const SCOPES = ['backend', 'sql', 'web', 'miniapp'];

export const SCOPE_PREFIXES = {
  backend: ['services/zhongshu-core/'],
  sql: ['services/zhongshu-core/sql/'],
  web: ['apps/zhongshu-admin-web/'],
  miniapp: ['apps/zhongshu-miniapp/'],
};

/** 各 scope 自身的排除项（上游素材/锁文件/文档等，按 docs/06 第 4 节保留）。 */
export const SCOPE_EXCLUSIONS = {
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
};

/**
 * hotfix-C P2-1：冻结/必要保留文件，对**全部 scope** 生效。
 * 与 SCOPE_EXCLUSIONS 的区别：这里排除的不是「上游素材」，而是「本仓库自己产出、
 * 且一旦被重写就会失效或语义反转」的文件。每条附 reason 以便审计与单测断言。
 */
export const PRESERVED = [
  {
    re: /^services\/zhongshu-core\/sql\/[^/]+\/upgrades\//,
    reason: '存量迁移脚本按 docs/06 第 4.1 节冻结：其 WHERE 谓词以旧值为匹配条件，被改名工具重写后变成新值=新值，旧基线库将无法升级',
  },
  {
    re: /\/db\/migration\/V[\d.]+__[^/]+\.sql$/,
    reason: 'Flyway 迁移受 validate-on-migrate=true 的校验和保护（ZS-DB-003 门禁合同），任何重写都会使已部署库校验失败',
  },
  {
    re: /^services\/zhongshu-core\/zszj-module-infra\/src\/main\/resources\/codegen\/README\.md$/,
    reason: '命名门禁说明文档，故意以字面量引用旧名作为「不得出现」清单；重写会把禁用清单反转为禁用本项目新名',
  },
];

/** 返回命中 PRESERVED 的原因；未命中返回 null。 */
export function preservedReason(relativePath) {
  const hit = PRESERVED.find((p) => p.re.test(relativePath));
  return hit ? hit.reason : null;
}

/** 选出某 scope 实际会被处理的文件（已扣除 SCOPE_EXCLUSIONS 与 PRESERVED）。 */
export function selectFiles(scope, cwd = root) {
  if (!SCOPES.includes(scope)) throw new Error(`unknown scope: ${scope}`);
  const listed = execFileSync('git', ['ls-files', '-z', ...SCOPE_PREFIXES[scope]], { cwd, maxBuffer: 64 * 1024 * 1024 })
    .toString('utf8')
    .split('\0')
    .filter(Boolean);
  const files = [];
  const preserved = [];
  for (const f of listed) {
    if (SCOPE_EXCLUSIONS[scope].some((re) => re.test(f))) continue;
    if (preservedReason(f)) { preserved.push(f); continue; }
    files.push(f);
  }
  return { files, preserved };
}

/** 对单个文件文本执行本 scope 的全部替换规则（纯函数，便于单测）。 */
export function transform(text, scope, relativePath) {
  let next = applyRules(text, { chinese: matchesChineseBrandScope(relativePath) });
  if (scope === 'sql' && matchesSqlSeedScope(relativePath)) {
    for (const [from, to] of SQL_SEED_REPLACEMENTS) next = next.split(from).join(to);
  }
  return next;
}

/**
 * 只读试算：报告哪些文件会被改写。不写盘，供 --dry-run 与单测复用。
 * 返回 wouldChange（内容会变的文件）与 skipped（二进制/非 UTF-8）。
 */
export function planScope(scope, cwd = root) {
  const { files, preserved } = selectFiles(scope, cwd);
  const wouldChange = [];
  const skipped = [];
  for (const file of files) {
    const buf = readFileSync(cwd + file);
    if (buf.subarray(0, 8192).includes(0)) { skipped.push([file, 'binary']); continue; }
    const text = buf.toString('utf8');
    if (text.includes('\uFFFD') && !buf.includes(0)) { skipped.push([file, 'non-utf8']); continue; }
    if (transform(text, scope, file) !== text) wouldChange.push(file);
  }
  return { scope, scanned: files.length, preserved, wouldChange, skipped };
}

function main() {
  const args = process.argv.slice(2);
  const scope = args.find((a) => !a.startsWith('--'));
  const dryRun = args.includes('--dry-run');
  if (!SCOPES.includes(scope)) {
    console.error(`usage: node scripts/brand/apply-naming-migration.mjs <${SCOPES.join('|')}> [--dry-run]`);
    process.exit(2);
  }

  const plan = planScope(scope);
  if (dryRun) {
    console.log(JSON.stringify({ ...plan, dryRun: true }, null, 2));
    return;
  }

  let changed = 0;
  for (const file of plan.wouldChange) {
    const text = readFileSync(root + file, 'utf8');
    writeFileSync(root + file, transform(text, scope, file), 'utf8');
    changed++;
  }
  console.log(JSON.stringify({
    scope,
    scanned: plan.scanned,
    changed,
    preservedExcluded: plan.preserved,
    skipped: plan.skipped,
  }, null, 2));
}

// 仅在作为入口脚本直接执行时运行迁移；被单测 import 时只导出纯函数，不得产生副作用。
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
