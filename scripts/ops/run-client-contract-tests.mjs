/**
 * ZS-SEC-012.B：双端请求层合同回归入口（G13）。
 *
 * 顺序执行 admin-web 与 zhongshu-miniapp 的 vitest 单测（ZS-CLIENT-003 交付：
 * admin-web service 12 用例 + miniapp http/interceptor 36 用例），任一失败即非零退出。
 * 与 run-local-gates 的 G13 门禁配合：本地与 CI 同一入口。
 *
 * 依赖：apps/*/node_modules 已由 pnpm install 安装（CI 由 ZS-CLIENT-005.A 流水线负责）。
 */
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const root = fileURLToPath(new URL('../../', import.meta.url));
const apps = [
  { name: 'zhongshu-admin-web', dir: 'apps/zhongshu-admin-web' },
  { name: 'zhongshu-miniapp', dir: 'apps/zhongshu-miniapp' },
];

let failed = false;
for (const app of apps) {
  console.log(`[client-contract] 运行 ${app.name} vitest …`);
  const r = spawnSync('npx', ['vitest', 'run'], { cwd: join(root, app.dir), encoding: 'utf8', stdio: 'inherit' });
  if (r.error || r.status !== 0) {
    console.error(`[client-contract] ${app.name} 失败（exit=${r.status ?? r.error?.code}）`);
    failed = true;
  }
}
if (failed) process.exit(1);
console.log('[client-contract] 双端请求层合同全部通过');
