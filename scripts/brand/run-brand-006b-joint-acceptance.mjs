/**
 * ZS-BRAND-006.B：品牌、改名兼容与产品功能联合收口验收（B06）。
 *
 * 汇总三层证据并实跑品牌门禁，任一失败即非零退出：
 *   ① 品牌与命名门禁（实跑）：G2 命名单测 / G2b 命名迁移工具 / G3 全仓扫描 / 后端命名校验；
 *   ② 产品功能回归证据链（读最近报告，重跑见 --full）：
 *      - ZS-SYS-001.A 基础管理 API 矩阵（outputs/sys001/report.json 全 PASS）
 *      - ZS-CLIENT-005.B 四域 E2E（outputs/client005b 最新报告 18/18）
 *      - ZS-SYS-001.B 页面走查（scripts/sys001/web-walkthrough-report.md 在库）
 *      - G15 双端联调门禁脚本在位（scripts/ops/run-b06-joint-gate.mjs）
 *   ③ --full 追加实跑：G14 基础管理全量回归 + G15 联调 E2E 自举（慢，收口/CI 用）。
 *
 * 用法：node scripts/brand/run-brand-006b-joint-acceptance.mjs [--full]
 * 证据汇总落 outputs/brand006b/joint-acceptance-<ts>.json；固定提交经 git rev-parse 记录。
 */
import { spawnSync, execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync, mkdirSync, existsSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const full = process.argv.includes('--full');
const fail = (code, msg) => { console.error(msg); process.exit(code); };
const run = (cmd, opts = {}) => {
  const r = spawnSync(cmd[0], cmd.slice(1), { cwd: root, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, ...opts });
  return { code: r.status ?? 1, out: (r.stdout ?? '') + (r.stderr ?? '') };
};

const results = [];
const record = (id, name, ok, note = '') => {
  results.push({ id, name, ok, note: String(note).slice(0, 300) });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${id} ${name}${note ? ` — ${String(note).slice(0, 200)}` : ''}`);
  return ok;
};

const commit = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).stdout.trim();

// ---------- ① 品牌与命名门禁（实跑） ----------
{
  const g3 = run(['node', 'scripts/brand/verify-brand-naming.mjs']);
  record('B-G3', '品牌命名全仓扫描（0 违规）', g3.code === 0, g3.out.slice(-160));
}
{
  const g2 = run(['node', '--test', 'scripts/brand/verify-brand-naming.test.mjs']);
  record('B-G2', '品牌命名门禁单测', g2.code === 0, (g2.out.match(/# (pass|fail) \d+/g) ?? []).join(' '));
}
{
  const g2b = run(['node', '--test', 'scripts/brand/apply-naming-migration.test.mjs']);
  record('B-G2b', '命名迁移工具单测（冻结文件排除 + 租户域名唯一）', g2b.code === 0, (g2b.out.match(/# (pass|fail) \d+/g) ?? []).join(' '));
}
{
  const bn = run(['node', 'scripts/brand/verify-backend-naming.mjs']);
  record('B-BN', '后端模块/包命名 zszj 校验', bn.code === 0, bn.out.slice(-160));
}

// ---------- ② 产品功能回归证据链 ----------
const readLatestE2E = () => {
  const dir = join(root, 'outputs/client005b');
  if (!existsSync(dir)) return null;
  const files = readdirSync(dir).filter((f) => f.startsWith('e2e-report-') && f.endsWith('.json')).sort();
  return files.length ? JSON.parse(readFileSync(join(dir, files.at(-1)), 'utf8')) : null;
};
{
  const r = readLatestE2E();
  const pass = r?.results?.filter((x) => x.ok).length ?? 0;
  const total = r?.results?.length ?? 0;
  record('E-E2E', 'CLIENT-005.B 四域 E2E 最近报告（登录/导航/文件/待办）', total === 18 && pass === 18,
    `pass=${pass}/${total} commit=${r?.commit?.slice(0, 8)}`);
}
{
  const p = join(root, 'outputs/sys001/report.json');
  if (!existsSync(p)) { record('E-SYS001A', '基础管理 API 矩阵报告', false, 'outputs/sys001/report.json 缺失'); }
  else {
    const r = JSON.parse(readFileSync(p, 'utf8'));
    const pass = r.summary?.passed ?? r.passed ?? null;
    record('E-SYS001A', '基础管理 API 矩阵报告（七类正反向）', pass === 49, `pass=${pass}（GAP-3 修复后 49/49）`);
  }
}
{
  const p = join(root, 'scripts/sys001/web-walkthrough-report.md');
  record('E-WEB', 'SYS-001.B 页面走查报告在库（6/7 类页面操作全链 + 公告限制登记）', existsSync(p), p);
}
{
  const p = join(root, 'scripts/ops/run-b06-joint-gate.mjs');
  record('E-G15', 'OPS-001.D 双端联调门禁脚本在位（G15，slow+exclusive）', existsSync(p), p);
}

// ---------- ③ --full：实跑重产品套件 ----------
if (full) {
  const g14 = run(['node', 'scripts/sys001/run-sys001-regression.mjs'], { timeout: 2_400_000 });
  record('F-G14', '基础管理全量回归实跑', g14.code === 0, g14.out.slice(-160));
  const g15 = run(['node', 'scripts/ops/run-b06-joint-gate.mjs'], { timeout: 1_800_000 });
  record('F-G15', '双端联调 E2E 自举实跑', g15.code === 0, g15.out.slice(-160));
}

const passed = results.filter((r) => r.ok).length;
const failed = results.length - passed;
mkdirSync(join(root, 'outputs/brand006b'), { recursive: true });
const reportPath = join(root, 'outputs/brand006b', `joint-acceptance-${Date.now()}.json`);
writeFileSync(reportPath, JSON.stringify({ task: 'ZS-BRAND-006.B 联合收口', commit, timestamp: new Date().toISOString(), full, results }, null, 2));
console.log(`\n合计 ${results.length} 项：PASS ${passed} / FAIL ${failed}（报告 ${reportPath}）`);
if (failed > 0) fail(1, '[brand006b] 联合收口存在 FAIL——B06 品牌收口不放行');
console.log('[brand006b] ZS-BRAND-006.B 联合收口 PASS');
