/**
 * ZS-CLIENT-005.A Web 类型检查基线门禁。
 *
 * 语义：当前 vue-tsc 错误必须 ⊆ 已冻结基线（scripts/client/ts-baseline.json）；
 * 出现任何基线之外的新错误即失败。基线条目修复后应从 JSON 中移除（缩减基线）。
 *
 * 前置：src/types/auto-imports.d.ts 由 dev 模式生成——若无该文件，本脚本先以
 *       60 秒超时拉起 `pnpm dev` 生成，再执行检查。
 * 用法：node scripts/client/verify-ts-baseline.mjs
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join, dirname } from 'node:path';
import { execFileSync } from 'node:child_process';

const root = fileURLToPath(new URL('../../', import.meta.url));
const app = join(root, 'apps/zhongshu-admin-web');
const baseline = JSON.parse(readFileSync(join(root, 'scripts/client/ts-baseline.json'), 'utf8'));
const logPath = join(app, 'node_modules/.cache/vue-tsc/tsconfig.tsbuildinfo');
const errTextPath = join(app, 'vue-tsc-errors.txt');

// 前置：auto-imports.d.ts（unplugin 在 dev serve 时生成）
const dts = join(app, 'src/types/auto-imports.d.ts');
if (!existsSync(dts)) {
  console.log('auto-imports.d.ts 缺失，拉起 dev server 生成（最多 60s）…');
  const child = spawn('pnpm', ['dev'], { cwd: app, shell: true, stdio: 'ignore' });
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline && !existsSync(dts)) {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000);
  }
  try { child.kill(); } catch { /* 已退出 */ }
  if (!existsSync(dts)) {
    console.error('无法生成 src/types/auto-imports.d.ts：请先运行一次 pnpm dev');
    process.exit(2);
  }
}

let errorsText = '';
try {
  errorsText = execFileSync(
    'node',
    ['--max_old_space_size=8192', './node_modules/vue-tsc/bin/vue-tsc.js', '--noEmit', '--incremental',
     '--tsBuildInfoFile', 'node_modules/.cache/vue-tsc/tsconfig.tsbuildinfo'],
    { cwd: app, maxBuffer: 64 * 1024 * 1024, encoding: 'utf8' },
  );
} catch (e) {
  errorsText = String(e.stdout ?? '') + String(e.stderr ?? '');
}
writeFileSync(join(app, 'node_modules/.cache/vue-tsc-errors.txt'), errorsText);

const current = [...errorsText.matchAll(/^(src\/[^(]+)\((\d+),\d+\): error (TS\d+): (.+)$/gm)]
  .map((m) => ({ file: m[1], line: Number(m[2]), code: m[3], message: m[4] }));

// 按 file+code 计数比对：消息文本（联合类型顺序等）在 vue-tsc 输出中可能不稳定，
// 计数比对对格式稳定、同时能发现同类新增错误
const countBy = (list) => {
  const map = new Map();
  for (const e of list) {
    const key = `${e.file}|${e.code}`;
    map.set(key, (map.get(key) ?? 0) + 1);
  }
  return map;
};
const baseCount = countBy(baseline);
const currCount = countBy(current);
const newErrors = [];
for (const [key, n] of currCount) {
  const base = baseCount.get(key) ?? 0;
  if (n > base) {
    newErrors.push({ key, count: n, baseline: base });
  }
}
const recoveredBaselineEntries = [...baseCount.keys()].filter((k) => !currCount.has(k));

console.log(JSON.stringify({
  currentErrors: current.length,
  baselineEntries: baseline.length,
  newErrors: newErrors.slice(0, 20),
  recoveredBaselineEntries,
}, null, 2));

if (newErrors.length) {
  console.error('存在基线之外的新类型错误，禁止放行（修复后或经评审更新基线后再提交）');
  process.exitCode = 1;
} else {
  console.error('通过：当前类型错误全部位于已冻结基线内');
  if (recoveredBaselineEntries.length) {
    console.error(`提示：基线中 ${recoveredBaselineEntries.length} 类已不再出现，可从 ts-baseline.json 移除`);
  }
}
