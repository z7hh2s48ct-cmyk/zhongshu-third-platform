/**
 * ZS-OPS-001.D：B06 双端技术联调门禁（G15）。
 *
 * 自举夹具 → 跑 CLIENT-005.B E2E 套件（登录/导航/文件/待办 + 首链申请域五域 21 用例，真实接口/PG）→ 清理。
 * 「界面操作和接口一致」由 SYS-001.B 页面走查报告（scripts/sys001/web-walkthrough-report.md）
 * 与本套件共用同一真实服务端边界承载；「跳过必测场景不得放行」由本门禁非零退出语义承载。
 *
 * 两种运行方式：
 *   1. 自举（默认）：临时拉起 sys001 夹具（--serve 驻留模式，一次性 Docker PG17+Redis+真实 server），
 *      解析 API 地址后执行 E2E，结束清理容器（Windows kill 信号不可靠，按 stdout 解析的容器名强制移除）。
 *   2. 长驻联验环境（deploy/README-local.md）：预设 E2E_BASE_URL/E2E_ADMIN_USER/E2E_ADMIN_PASS 直连。
 *
 * 环境变量：B06_GATE_KEEP=1 保留夹具（调试用）。报告落 outputs/client005b/（E2E 自写）。
 * 退出码：E2E 任一 FAIL 非零；夹具启动失败非零——不静默跳过。
 */
import { spawn, spawnSync, execFileSync } from 'node:child_process';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const fail = (code, msg) => { console.error(msg); process.exit(code); };

const externalBase = process.env.E2E_BASE_URL;
let harness = null;
let apiBase = externalBase ?? null;
const containerNames = [];

if (!apiBase) {
  console.log('[b06-gate] 未设 E2E_BASE_URL：自举 sys001 夹具（--serve）…');
  harness = spawn(process.execPath, ['scripts/sys001/run-sys001-regression.mjs', '--serve'], {
    cwd: root,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  const deadline = Date.now() + 420_000; // 夹具含 jar 重建（mvn clean package），上限 7 分钟
  let buf = '';
  const ready = await new Promise((resolve) => {
    const onData = (d) => {
      buf += String(d);
      const apiMatch = buf.match(/\[sys001\] API: (http:\/\/\S+)/);
      if (apiMatch && !apiBase) {
        apiBase = apiMatch[1];
        for (const m of buf.matchAll(/zszj-sys001-(?:pg|redis)-\S+/g)) {
          if (!containerNames.includes(m[0])) containerNames.push(m[0]);
        }
      }
      if (apiBase && /--serve 驻留模式/.test(buf)) { harness.stdout.off('data', onData); harness.stderr.off('data', onData); resolve(true); }
      if (/夹具|构建失败|未就绪|Docker 不可用/.test(buf) && /fail|\[sys001\].*失败/.test(buf) && !apiBase) { harness.stdout.off('data', onData); harness.stderr.off('data', onData); resolve(false); }
    };
    harness.stdout.on('data', onData);
    harness.stderr.on('data', onData);
    harness.on('exit', (code) => { if (!apiBase) resolve(false); });
    setTimeout(() => resolve(!!apiBase), deadline - Date.now() > 0 ? deadline - Date.now() : 0);
  });
  if (!ready || !apiBase) {
    try { harness.kill(); } catch { }
    fail(1, '[b06-gate] 夹具启动失败/超时——不静默跳过');
  }
  console.log(`[b06-gate] 夹具就绪：${apiBase}`);
}

// 就绪探活（长驻环境模式下健康检查；自举模式 server 已自证就绪）——脚本内直连，避免子进程引号问题
{
  let up = false;
  try {
    const res = await fetch(`${apiBase}/admin-api/system/tenant/simple-list`, { signal: AbortSignal.timeout(15_000) });
    const text = await res.text();
    up = text.includes('"code":0');
  } catch { /* DOWN */ }
  if (!up) {
    if (harness) cleanupHarness();
    fail(1, `[b06-gate] 联验环境不可用（${apiBase}）：请按 deploy/README-local.md 启动，或让门禁自举夹具`);
  }
}

function cleanupHarness() {
  if (!harness) return;
  try { harness.kill(); } catch { }
  // Windows 下 kill 后 exit 钩子的 docker 清理不可靠：按解析到的容器名强制移除
  for (const name of containerNames) {
    try { execFileSync('docker', ['rm', '-f', name], { stdio: 'ignore' }); } catch { }
  }
}

let e2eCode = 1;
try {
  const r = spawnSync(process.execPath, ['scripts/client005b/run-client005b-e2e.mjs'], {
    cwd: root,
    env: { ...process.env, E2E_BASE_URL: apiBase, E2E_ADMIN_USER: process.env.E2E_ADMIN_USER ?? 'admin', E2E_ADMIN_PASS: process.env.E2E_ADMIN_PASS ?? 'Sys001Pass' },
    encoding: 'utf8',
    timeout: 600_000,
    stdio: 'inherit',
  });
  e2eCode = r.status ?? 1;
} finally {
  if (process.env.B06_GATE_KEEP === '1') {
    console.log('[b06-gate] B06_GATE_KEEP=1：夹具保留（调试）');
  } else {
    cleanupHarness();
  }
}

if (e2eCode !== 0) fail(e2eCode, '[b06-gate] E2E 套件存在 FAIL——B06 联调门禁不放行');
console.log('[b06-gate] G15 双端技术联调门禁 PASS（21 用例）');
