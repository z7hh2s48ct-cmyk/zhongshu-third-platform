$ chcp 65001 > $null; [Console]::OutputEncoding=[System.Text.Encoding]::UTF8; Set-Location 'e:\众墅之家AI赋能平台底座'; & "$env:APPDATA\npm\codex.cmd" review --commit 2fa36a9e 2>&1 | Out-String -Width 4096
codex.cmd : OpenAI Codex v0.154.0
所在位置 行:1 字符: 109
+ ... 家AI赋能平台底座'; & "$env:APPDATA\npm\codex.cmd" review --commit 2fa36a9e 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: none
reasoning summaries: none
session id: 01a08f26-fbcd-7472-80ca-2ab9fd23f5ac
--------
user
commit 2fa36a9
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git status --short; git show --stat --oneline 2fa36a9e; Get-ChildItem -Force -Name AGENTS*' in E:\众墅之家AI赋能平台底座
 succeeded in 41ms:
2fa36a9e ZS-GOV-001 提效方案 步骤⑤：git worktree 并行编排器（纯工具，无任务状态变更）
 scripts/ops/worktree-orchestrate.mjs      | 267 ++++++++++++++++++++++++++++++
 scripts/ops/worktree-orchestrate.test.mjs | 143 ++++++++++++++++
 2 files changed, 410 insertions(+)

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-ChildItem -Path . -Filter AGENTS* -Recurse -File | Select-Object -ExpandProperty FullName; git show 2fa36a9e -- scripts/ops/worktree-orchestrate.mjs scripts/ops/worktree-orchestrate.test.mjs' in E:\众墅之家AI赋能平台底座
 succeeded in 4055ms:
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.30001775\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.30001775\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.30001791\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.30001791\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001739\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001739\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001749\node_modules\caniuse-lite\data\agents.js
E:\����֮��AI����ƽ̨����\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001749\node_modules\caniuse-lite\dist\unpacker\agents.js
commit 2fa36a9ea5c80d60678787d17f00cf0936f15b63
Author: hlong026 <peihr85@gmail.com>
Date:   Fri Sep 11 14:25:00 2026 +0800

    ZS-GOV-001 提效方案 步骤⑤：git worktree 并行编排器（纯工具，无任务状态变更）
    
    新增 scripts/ops/worktree-orchestrate.mjs —— 主树枢纽工具，为 B03 子项并行推进提供
    隔离的 worktree + feature 分支编排，落实「代码并行、文档串行」策略以规避三任务并行改
    docs/05(§2/§16.1/§19/README) 的合并冲突。
    
    子命令 plan/create/list/gates/merge/cleanup：
    - create：git worktree add -b feat/<slug> + 供给 tools/ junction（新树因 tools/ 被
      gitignore 天生无 JDK/Maven；env.sh 的 TOOLS 为绝对路径指主树，junction 后 source 与
      编译均正确、mvn 二进制跨树共用而编译各树自己的源码、不重复占盘）+ 隔离校验（新树在
      目标分支、主树 HEAD 未被动、worktree 已登记）。
    - gates：调用该 worktree「自己的」run-local-gates 副本（root 由 import.meta.url 推导，
      自动锁定本树），--fast 门禁 G1~G9 无需 tools/，G11(--mvn) 与直接 mvn 依赖 junction。
    - merge：--no-ff 合并回主树 + 打印文档串行同步清单（工具绝不自动改 docs，同步在主树串行做）。
    - mainRoot 源自 import.meta.url，与调用 cwd 无关，免疫终端/sandbox workspace-root 落错项目陷阱。
    
    纯函数 slugify/branchName/worktreeDirName/resolvePaths/provisionTargets/planCreate/
    planMerge/planCleanup 导出供静态单测；git/junction 副作用只在 main() 落地。
    测试：scripts/ops/worktree-orchestrate.test.mjs 16 用例全绿（不执行 git/fs）。
    门禁：run-local-gates --fast 10/10 PASS（新脚本在 scripts/ops/ 不改变门禁定义与计数）。

diff --git a/scripts/ops/worktree-orchestrate.mjs b/scripts/ops/worktree-orchestrate.mjs
new file mode 100644
index 00000000..12c7ec78
--- /dev/null
+++ b/scripts/ops/worktree-orchestrate.mjs
@@ -0,0 +1,267 @@
+/**
+ * ZS-GOV-001 提效方案 步骤⑤：git worktree 并行编排器（试点 ZS-CFG-001.B / ZS-CFG-002.B / ZS-SEC-010）。
+ *
+ * 目标：让多个 B03 子项在各自隔离的 worktree + feature 分支上并行推进，主树保持干净、专用于
+ *   「合并 + 文档串行同步」，从根上规避三任务并行改 docs/05(§2/§16.1/§19/README) 的合并冲突
+ *   （见「代码并行、文档串行」策略：worktree 只承载代码，文档只在主树串行编辑）。
+ *
+ * 本仓已核实的关键事实（决定了本工具的形态）：
+ *   - run-local-gates.mjs 的 root 由 import.meta.url 推导 → 跑各 worktree「自己的副本」即自动锁定该树，
+ *     故 gates 子命令用 <wtPath>/scripts/ops/run-local-gates.mjs 而非主树副本；
+ *   - tools/、node_modules/、dist/ 均被 gitignore（tools/ 0 个跟踪文件）→ `git worktree add` 出来的新树
+ *     天生无 JDK/Maven，直接跑 mvn 会失败，须「供给」：把主树 tools/ 以目录 junction 挂进新树；
+ *   - tools/env.sh 的 TOOLS 是「绝对路径指主树」→ junction 后 source/编译均正确，且 mvn 二进制跨树共用、
+ *     编译的却是各树自己的源码（cwd=各树 services/zhongshu-core），不重复占盘；
+ *   - --fast 门禁(G1~G9) 全是 node/git/PG，不需要 tools/；只有 G11(--mvn) 与直接 mvn 需要供给。
+ *
+ * 本工具是「主树枢纽」：mainRoot 由 import.meta.url 推导（脚本所在仓库），与调用时 cwd 无关，
+ *   天然免疫「终端 cwd/sandbox workspace-root 落到别的项目」的陷阱；始终用主树副本调用本工具。
+ *
+ * 子命令：
+ *   plan    <taskId> [--base <ref>] [--wt-root <dir>] [--node]   只打印 create 将执行的步骤，零副作用
+ *   create  <taskId> [--base <ref>] [--wt-root <dir>] [--node] [--no-provision]  建 worktree+分支+供给+隔离校验
+ *   list                                                        列出全部 worktree + 分支 + 供给状态
+ *   gates   <taskId> [--fast] [--mvn] [--incremental]           在该 worktree 内跑门禁（调用其自身副本）
+ *   merge   <taskId> [--into <branch>] [--ff-only]              合并分支回主树 + 打印文档串行同步清单
+ *   cleanup <taskId> [--keep-branch] [--force]                  移除 worktree（+删已合并分支）
+ *
+ * 纯函数（slugify/branchName/worktreeDirName/resolvePaths/provisionTargets/planCreate/planMerge/
+ *   planCleanup）不执行 git/fs，供 worktree-orchestrate.test.mjs 静态单测；副作用只在 main() 落地。
+ * 用法：node scripts/ops/worktree-orchestrate.mjs <子命令> <taskId> [选项]
+ */
+import { spawnSync } from 'node:child_process';
+import { existsSync, symlinkSync } from 'node:fs';
+import { fileURLToPath } from 'node:url';
+import { dirname, resolve, join } from 'node:path';
+
+// 主树根：由脚本自身位置推导（scripts/ops/ 的上两级），与调用时 cwd 无关 → 免疫 sandbox cwd 陷阱。
+export const mainRoot = resolve(fileURLToPath(new URL('../../', import.meta.url)));
+// 默认 worktree 父目录：与主树同级（如 E:/），对齐既有 E:/zszj-base-check 的兄弟布局。
+export const DEFAULT_WT_ROOT = dirname(mainRoot);
+
+// 步骤⑤ 试点任务（工具本身对任意 ZS 任务 ID 通用；此表仅用于 list 标注与演示）。
+export const PILOT_TASKS = ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010'];
+
+/**
+ * 供给目标：新树因 gitignore 而缺失、需从主树 junction 的工具链目录。
+ *   tools 恒定（env.sh/JDK/Maven，直接 mvn 与 G11 依赖）；
+ *   node_modules 仅前端门禁(G10 vue-tsc)需要——B03 三任务后端为主，默认不供给，--node 显式开启。
+ * @returns {string[]}
+ */
+export function provisionTargets({ node = false } = {}) {
+  return node ? ['tools', 'node_modules'] : ['tools'];
+}
+
+/**
+ * 任务 ID → 稳定 slug：去 ZS- 前缀、小写、非字母数字折叠为单 -、首尾去 -。
+ * 例：ZS-CFG-002.B → cfg-002-b；ZS-SEC-010 → sec-010。
+ */
+export function slugify(taskId) {
+  return String(taskId).trim().toLowerCase().replace(/^zs-/, '').replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
+}
+
+/** feature 分支名：feat/<slug>。 */
+export function branchName(taskId) {
+  return `feat/${slugify(taskId)}`;
+}
+
+/** worktree 目录名：zszj-wt-<slug>（对齐既有 zszj-base-check 命名风格）。 */
+export function worktreeDirName(taskId) {
+  return `zszj-wt-${slugify(taskId)}`;
+}
+
+/** 解析任务的路径/分支/基线为纯数据，不落盘、不执行 git。 */
+export function resolvePaths({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main' }) {
+  const slug = slugify(taskId);
+  return { taskId, slug, branch: `feat/${slug}`, wtPath: join(wtRoot, `zszj-wt-${slug}`), baseRef, mainRoot };
+}
+
+/**
+ * 规划 create 步骤（纯数据，供单测与 plan 子命令）。
+ * steps[].kind ∈ {'git-worktree-add','junction'}；exec 阶段据此落副作用。
+ * @returns {{steps: Array<object>} & ReturnType<typeof resolvePaths>}
+ */
+export function planCreate({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main', provision = true, node = false }) {
+  const p = resolvePaths({ taskId, wtRoot, baseRef });
+  const steps = [
+    { kind: 'git-worktree-add', desc: `建 worktree + 分支 ${p.branch} ← ${baseRef}`,
+      args: ['-C', mainRoot, 'worktree', 'add', '-b', p.branch, p.wtPath, baseRef] },
+  ];
+  if (provision) {
+    for (const dir of provisionTargets({ node })) {
+      steps.push({ kind: 'junction', desc: `供给 ${dir}/（junction → 主树）`, dir, link: join(p.wtPath, dir), target: join(mainRoot, dir) });
+    }
+  }
+  return { ...p, provision, steps };
+}
+
+/**
+ * 规划 merge 步骤（纯数据）+ 文档串行同步清单。
+ * 合并只在主树做；工具「绝不自动改 docs」——文档同步由主树手动/close-task 串行完成，避免并行冲突回流。
+ */
+export function planMerge({ taskId, into = 'main', ffOnly = false, wtRoot = DEFAULT_WT_ROOT }) {
+  const p = resolvePaths({ taskId, wtRoot });
+  return {
+    ...p, into,
+    steps: [{ kind: 'git-merge', desc: `合并 ${p.branch} → ${into}（${ffOnly ? '--ff-only' : '--no-ff 保留任务边界'}）`,
+      args: ['-C', mainRoot, 'merge', ffOnly ? '--ff-only' : '--no-ff', p.branch] }],
+    docSyncReminder: [
+      `合并后在主树(${into})「串行」同步文档（不在 worktree 改，避免 §2/§16.1/§19/README 冲突）：`,
+      `  1) 写 ${taskId} 卡片「开发记录」（docs/05 各卡独立行段，天然不冲突）`,
+      `  2) 补 §16.1 子项行 / §19 变更记录行`,
+      `  3) node scripts/gov/close-task.mjs ${taskId} <新状态> [--bump <VX.Y>]   # 重算并回填 §2/README`,
+      `  4) node scripts/gov/verify-docs.mjs ; node scripts/ops/run-local-gates.mjs --fast`,
+      `  5) pathspec 原子提交文档同步（排除 outputs/；提交后 diff 复核隔离）`,
+    ],
+  };
+}
+
+/** 规划 cleanup 步骤（纯数据）：移除 worktree，默认删「已合并」分支（-d 安全删，未合并会失败而非强删）。 */
+export function planCleanup({ taskId, wtRoot = DEFAULT_WT_ROOT, keepBranch = false, force = false }) {
+  const p = resolvePaths({ taskId, wtRoot });
+  const steps = [
+    { kind: 'git-worktree-remove', desc: `移除 worktree ${p.wtPath}${force ? '（--force）' : ''}`,
+      args: ['-C', mainRoot, 'worktree', 'remove', ...(force ? ['--force'] : []), p.wtPath] },
+  ];
+  if (!keepBranch) {
+    steps.push({ kind: 'git-branch-delete', desc: `删除${force ? '（-D 强删）' : '已合并'}分支 ${p.branch}`,
+      args: ['-C', mainRoot, 'branch', force ? '-D' : '-d', p.branch] });
+  }
+  return { ...p, steps };
+}
+
+// ============================ 以下为副作用层（仅 main 调用，不做静态单测） ============================
+
+/** 在主树跑 git（-C mainRoot 已入 args；mainRoot 源自 import.meta.url，与 cwd 无关）。 */
+function runGit(args) {
+  const r = spawnSync('git', args, { cwd: mainRoot, encoding: 'utf8' });
+  return { ok: r.status === 0, out: String(r.stdout ?? ''), err: String(r.stderr ?? ''), status: r.status };
+}
+
+/** 建目录 junction（win32 用 junction、其余用 dir）；幂等：link 已存在则跳过。 */
+function doJunction(step) {
+  if (!existsSync(step.target)) return { ok: false, msg: `供给目标不存在：${step.target}` };
+  if (existsSync(step.link)) return { ok: true, skipped: true, msg: `已存在，跳过：${step.link}` };
+  try {
+    symlinkSync(step.target, step.link, process.platform === 'win32' ? 'junction' : 'dir');
+    return { ok: true, msg: `junction：${step.link} → ${step.target}` };
+  } catch (e) {
+    return { ok: false, msg: `junction 失败：${e.message}` };
+  }
+}
+
+/** 逐步执行 plan 的 steps；返回是否全绿。 */
+function execSteps(steps) {
+  let allOk = true;
+  for (const s of steps) {
+    if (s.kind === 'junction') {
+      const r = doJunction(s);
+      console.log(`${r.ok ? '✓' : '✗'} ${s.desc} — ${r.msg}`);
+      allOk = allOk && r.ok;
+    } else {
+      console.log(`→ git ${s.args.join(' ')}`);
+      const r = runGit(s.args);
+      if (r.out.trim()) console.log(r.out.trimEnd());
+      if (!r.ok) { console.error(`✗ ${s.desc}\n${r.err.trimEnd()}`); allOk = false; }
+      else console.log(`✓ ${s.desc}`);
+    }
+  }
+  return allOk;
+}
+
+/** create 后隔离校验：新树在目标分支、主树 HEAD 未被动、worktree 已登记。 */
+function isolationReport(p) {
+  const branchInWt = runGit(['-C', p.wtPath, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
+  const mainHead = runGit(['-C', mainRoot, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
+  const registered = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']).out.includes(p.wtPath.replaceAll('\\', '/'))
+    || runGit(['-C', mainRoot, 'worktree', 'list']).out.includes(p.wtPath);
+  const toolsOk = existsSync(join(p.wtPath, 'tools', 'env.sh'));
+  const ok = branchInWt === p.branch && mainHead === 'main' && registered;
+  console.log(`\n----- 隔离校验 -----\n新树分支=${branchInWt}（期望 ${p.branch}）\n主树 HEAD=${mainHead}（期望 main 未被动）\nworktree 已登记=${registered}\ntools 供给=${toolsOk}\n结论：${ok ? 'PASS 隔离完好' : 'FAIL 需排查'}`);
+  return ok;
+}
+
+/** gates：调用「该 worktree 自己的」run-local-gates 副本 → root 自动锁定该树。 */
+function doGates(p, flags) {
+  const script = join(p.wtPath, 'scripts', 'ops', 'run-local-gates.mjs');
+  if (!existsSync(script)) { console.error(`✗ worktree 未建或缺门禁脚本：${script}`); return false; }
+  console.log(`→ 在 ${p.wtPath} 跑门禁（其自身副本，自动锁定本树）：node run-local-gates.mjs ${flags.join(' ')}`);
+  const r = spawnSync('node', [script, ...flags], { cwd: p.wtPath, stdio: 'inherit' });
+  return r.status === 0;
+}
+
+/** list：解析 worktree --porcelain，标注分支/供给/是否试点。 */
+function doList() {
+  const r = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']);
+  if (!r.ok) { console.error(r.err.trimEnd()); return false; }
+  const blocks = r.out.split('\n\n').map((b) => b.trim()).filter(Boolean);
+  console.log(`主树：${mainRoot}\n共 ${blocks.length} 个 worktree：`);
+  for (const b of blocks) {
+    const path = (b.match(/^worktree (.+)$/m) || [])[1] ?? '?';
+    const head = (b.match(/^HEAD ([0-9a-f]+)/m) || [])[1]?.slice(0, 8) ?? '?';
+    const branch = ((b.match(/^branch (.+)$/m) || [])[1] ?? 'detached').replace('refs/heads/', '');
+    const isMain = path.replaceAll('\\', '/') === mainRoot.replaceAll('\\', '/');
+    const tools = isMain ? '(主树)' : existsSync(join(path, 'tools', 'env.sh')) ? 'tools✓' : 'tools✗未供给';
+    console.log(`  ${head}  ${branch.padEnd(24)} ${path}  ${tools}`);
+  }
+  console.log(`试点任务：${PILOT_TASKS.join('、')}`);
+  return true;
+}
+
+function printSteps(title, steps) {
+  console.log(`${title}（${steps.length} 步）：`);
+  for (const s of steps) console.log(`  - [${s.kind}] ${s.desc}`);
+}
+
+async function main() {
+  const argv = process.argv.slice(2);
+  const cmd = argv[0];
+  const taskId = argv.find((a) => /^ZS-/i.test(a));
+  const has = (f) => argv.includes(f);
+  const optVal = (f, dflt) => { const i = argv.indexOf(f); return i >= 0 && argv[i + 1] ? argv[i + 1] : dflt; };
+  const wtRoot = resolve(optVal('--wt-root', DEFAULT_WT_ROOT));
+  const baseRef = optVal('--base', 'main');
+  const node = has('--node');
+
+  if (!cmd || cmd === '--help' || cmd === '-h') {
+    console.log('用法：node scripts/ops/worktree-orchestrate.mjs <plan|create|list|gates|merge|cleanup> <ZS-任务ID> [选项]\n详见文件头注释。');
+    return 0;
+  }
+  if (cmd === 'list') return doList() ? 0 : 1;
+  if (!taskId) { console.error('✗ 缺少 ZS- 任务 ID'); return 2; }
+
+  if (cmd === 'plan') {
+    printSteps(`create ${taskId} 将执行`, planCreate({ taskId, wtRoot, baseRef, node }).steps);
+    return 0;
+  }
+  if (cmd === 'create') {
+    const plan = planCreate({ taskId, wtRoot, baseRef, provision: !has('--no-provision'), node });
+    if (existsSync(plan.wtPath)) { console.error(`✗ worktree 路径已存在：${plan.wtPath}（先 cleanup 或换 --wt-root）`); return 1; }
+    if (runGit(['-C', mainRoot, 'rev-parse', '--verify', plan.branch]).ok) { console.error(`✗ 分支已存在：${plan.branch}`); return 1; }
+    const ok = execSteps(plan.steps) && isolationReport(plan);
+    return ok ? 0 : 1;
+  }
+  if (cmd === 'gates') {
+    const p = resolvePaths({ taskId, wtRoot });
+    const flags = argv.filter((a) => a.startsWith('--') && a !== '--wt-root');
+    return doGates(p, flags) ? 0 : 1;
+  }
+  if (cmd === 'merge') {
+    const plan = planMerge({ taskId, into: optVal('--into', 'main'), ffOnly: has('--ff-only'), wtRoot });
+    const ok = execSteps(plan.steps);
+    console.log('\n----- 文档串行同步清单 -----');
+    for (const line of plan.docSyncReminder) console.log(line);
+    return ok ? 0 : 1;
+  }
+  if (cmd === 'cleanup') {
+    const plan = planCleanup({ taskId, wtRoot, keepBranch: has('--keep-branch'), force: has('--force') });
+    return execSteps(plan.steps) ? 0 : 1;
+  }
+  console.error(`✗ 未知子命令：${cmd}`);
+  return 2;
+}
+
+const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/worktree-orchestrate.mjs');
+if (invokedDirectly) {
+  main().then((code) => process.exit(code)).catch((e) => { console.error(e); process.exit(1); });
+}
diff --git a/scripts/ops/worktree-orchestrate.test.mjs b/scripts/ops/worktree-orchestrate.test.mjs
new file mode 100644
index 00000000..0e43b053
--- /dev/null
+++ b/scripts/ops/worktree-orchestrate.test.mjs
@@ -0,0 +1,143 @@
+/**
+ * worktree-orchestrate.mjs 的纯静态单测（提效方案 步骤⑤：git worktree 并行编排器）。
+ * 只验证路径/分支解析与步骤规划的纯函数，不执行 git、不建 junction、不落任何副作用
+ * （符合纯静态单测规范，与 run-local-gates.test.mjs 同风格）。
+ * 运行：node --test scripts/ops/worktree-orchestrate.test.mjs
+ */
+import { test } from 'node:test';
+import assert from 'node:assert/strict';
+import { join } from 'node:path';
+import {
+  slugify, branchName, worktreeDirName, resolvePaths, provisionTargets,
+  planCreate, planMerge, planCleanup, PILOT_TASKS, mainRoot, DEFAULT_WT_ROOT,
+} from './worktree-orchestrate.mjs';
+
+const norm = (p) => String(p).replaceAll('\\', '/'); // 跨平台断言：统一正斜杠
+
+// ---- slug / 分支 / 目录名解析 ----
+
+test('slugify：去 ZS- 前缀、小写、. 与非字母数字折叠为单 -', () => {
+  assert.equal(slugify('ZS-CFG-002.B'), 'cfg-002-b');
+  assert.equal(slugify('ZS-SEC-010'), 'sec-010');
+  assert.equal(slugify('ZS-CFG-001.B'), 'cfg-001-b');
+  assert.equal(slugify('  ZS-CFG-002.B  '), 'cfg-002-b'); // 首尾空白
+  assert.equal(slugify('zs-cfg-002.b'), 'cfg-002-b');      // 已小写
+});
+
+test('slugify：无 ZS- 前缀也能规整（工具对任意 ID 通用）', () => {
+  assert.equal(slugify('CFG-002.B'), 'cfg-002-b');
+  assert.equal(slugify('FOO__BAR'), 'foo-bar'); // 连续非字母数字折叠为单 -
+});
+
+test('branchName / worktreeDirName：feat/<slug> 与 zszj-wt-<slug>', () => {
+  assert.equal(branchName('ZS-CFG-002.B'), 'feat/cfg-002-b');
+  assert.equal(worktreeDirName('ZS-CFG-002.B'), 'zszj-wt-cfg-002-b');
+  assert.equal(branchName('ZS-SEC-010'), 'feat/sec-010');
+});
+
+test('PILOT_TASKS：步骤⑤ 三试点任务', () => {
+  assert.deepEqual(PILOT_TASKS, ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010']);
+});
+
+// ---- resolvePaths ----
+
+test('resolvePaths：默认 wtRoot 与主树同级，分支/路径/基线一致', () => {
+  const p = resolvePaths({ taskId: 'ZS-CFG-002.B' });
+  assert.equal(p.slug, 'cfg-002-b');
+  assert.equal(p.branch, 'feat/cfg-002-b');
+  assert.equal(p.baseRef, 'main');
+  assert.equal(p.mainRoot, mainRoot);
+  assert.equal(norm(p.wtPath), norm(join(DEFAULT_WT_ROOT, 'zszj-wt-cfg-002-b')));
+});
+
+test('resolvePaths：自定义 baseRef 与 wtRoot 生效', () => {
+  const p = resolvePaths({ taskId: 'ZS-SEC-010', baseRef: 'ae00486e', wtRoot: 'D:/wt' });
+  assert.equal(p.baseRef, 'ae00486e');
+  assert.equal(norm(p.wtPath), 'D:/wt/zszj-wt-sec-010');
+});
+
+// ---- provisionTargets ----
+
+test('provisionTargets：默认只供给 tools（--fast 门禁不需 node_modules）', () => {
+  assert.deepEqual(provisionTargets(), ['tools']);
+  assert.deepEqual(provisionTargets({ node: false }), ['tools']);
+});
+
+test('provisionTargets：--node 追加 node_modules（前端 G10 门禁需要）', () => {
+  assert.deepEqual(provisionTargets({ node: true }), ['tools', 'node_modules']);
+});
+
+// ---- planCreate ----
+
+test('planCreate：首步 git worktree add -b，随后 junction 供给 tools', () => {
+  const plan = planCreate({ taskId: 'ZS-CFG-002.B' });
+  assert.equal(plan.provision, true);
+  const add = plan.steps[0];
+  assert.equal(add.kind, 'git-worktree-add');
+  assert.deepEqual(add.args.slice(2, 6), ['worktree', 'add', '-b', 'feat/cfg-002-b']);
+  assert.equal(norm(add.args[6]), norm(plan.wtPath)); // 路径参数指向解析出的 wtPath
+  assert.equal(add.args[7], 'main');                 // 基线
+  const junc = plan.steps[1];
+  assert.equal(junc.kind, 'junction');
+  assert.equal(junc.dir, 'tools');
+  assert.equal(norm(junc.link), `${norm(plan.wtPath)}/tools`);
+  assert.equal(norm(junc.target), `${norm(mainRoot)}/tools`);
+});
+
+test('planCreate：git worktree add 用 -C 主树（mainRoot 源自 import.meta.url，免疫 cwd 陷阱）', () => {
+  const plan = planCreate({ taskId: 'ZS-SEC-010', baseRef: 'main' });
+  const add = plan.steps[0];
+  assert.deepEqual(add.args.slice(0, 2), ['-C', mainRoot]);
+  assert.equal(add.args[7], 'main');
+});
+
+test('planCreate：--no-provision 只留 worktree add 一步', () => {
+  const plan = planCreate({ taskId: 'ZS-CFG-001.B', provision: false });
+  assert.equal(plan.provision, false);
+  assert.equal(plan.steps.length, 1);
+  assert.equal(plan.steps[0].kind, 'git-worktree-add');
+});
+
+test('planCreate：--node 供给 tools + node_modules 两个 junction', () => {
+  const plan = planCreate({ taskId: 'ZS-CFG-001.B', node: true });
+  const junctions = plan.steps.filter((s) => s.kind === 'junction').map((s) => s.dir);
+  assert.deepEqual(junctions, ['tools', 'node_modules']);
+});
+
+// ---- planMerge ----
+
+test('planMerge：默认 --no-ff 保留任务边界，并给出文档串行同步清单', () => {
+  const plan = planMerge({ taskId: 'ZS-CFG-002.B' });
+  const m = plan.steps[0];
+  assert.equal(m.kind, 'git-merge');
+  assert.deepEqual(m.args.slice(2), ['merge', '--no-ff', 'feat/cfg-002-b']);
+  assert.equal(plan.into, 'main');
+  assert.ok(Array.isArray(plan.docSyncReminder) && plan.docSyncReminder.length >= 5);
+  assert.ok(plan.docSyncReminder.some((l) => l.includes('close-task.mjs')), '应提示用 close-task 重算 §2/README');
+  assert.ok(plan.docSyncReminder.some((l) => l.includes('不在 worktree 改')), '应强调文档只在主树串行改');
+});
+
+test('planMerge：--ff-only 切换合并策略；--into 切换目标分支', () => {
+  const ff = planMerge({ taskId: 'ZS-SEC-010', ffOnly: true });
+  assert.deepEqual(ff.steps[0].args.slice(2), ['merge', '--ff-only', 'feat/sec-010']);
+  const into = planMerge({ taskId: 'ZS-SEC-010', into: 'release' });
+  assert.equal(into.into, 'release');
+});
+
+// ---- planCleanup ----
+
+test('planCleanup：默认移除 worktree + 安全删分支(-d)', () => {
+  const plan = planCleanup({ taskId: 'ZS-CFG-002.B' });
+  assert.equal(plan.steps[0].kind, 'git-worktree-remove');
+  assert.equal(norm(plan.steps[0].args[plan.steps[0].args.length - 1]), norm(plan.wtPath));
+  assert.deepEqual(plan.steps[1].args.slice(2), ['branch', '-d', 'feat/cfg-002-b']);
+});
+
+test('planCleanup：--keep-branch 不删分支；--force 用 remove --force + branch -D', () => {
+  const keep = planCleanup({ taskId: 'ZS-SEC-010', keepBranch: true });
+  assert.equal(keep.steps.length, 1);
+  assert.equal(keep.steps[0].kind, 'git-worktree-remove');
+  const force = planCleanup({ taskId: 'ZS-SEC-010', force: true });
+  assert.ok(force.steps[0].args.includes('--force'));
+  assert.deepEqual(force.steps[1].args.slice(2), ['branch', '-D', 'feat/sec-010']);
+});

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-ChildItem -Force -Name; Get-Content scripts/ops/run-local-gates.mjs -Encoding UTF8; Get-Content package.json -Encoding UTF8; Get-ChildItem E:\\ -Force -Name AGENTS*" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
.git
.github
.omx
apps
docs
outputs
reference
scripts
services
third_party
tools
.gitignore
README.md
THIRD_PARTY_NOTICES.md
/**
 * ZS-OPS-001.A ���ػ����Ž��ۺ���ڣ������� CI ͬһ���򣩡�
 *
 * ����ִ��ȫ�����ؼ�飬��һʧ�ܼ�����ʧ�ܲ����ܣ�������Ž�����˳���ȶ��������
 *   G1 ��Դ����У�������� / G2 Ʒ�������Ž����� / G2b ����Ǩ�ƹ��ߵ��� / G3 Ʒ������ȫ��ɨ��
 *   G4 �ĵ�һ���Ե��� / G5 �ĵ�һ����ȫ����ZS-GOV-001��/ G6 ģ���������ZS-ENG-001��
 *   G7 ����Դ PG ��ͬ��ZS-DB-001.A��/ G8 Flyway Ǩ�ƹ淶��ZS-DB-003��/ G9 ���������Ž���ZS-CFG-001.A��
 *   G10 Web ���ͼ����ߣ�ZS-CLIENT-005.A��������--fast ������
 *   G11 ����ģ���˵��⣨--mvn ��ʽ���ã��� tools/env.sh ���������ų��ѵǼǵ����λ���ʧ�ܣ�
 * PG/��� E2E �Ž��� ZS-OPS-001.B~.E ���ν��룬���ڱ��Ǽܡ�
 *
 * ���٣�ZS-GOV-001 ��Ч���� P1����
 *   - ������Ĭ�ϰ� CPU �����������Ž���--jobs N ���ǣ�������ʱ��ӡ����Ž���ʱ֮�͡������������Ž�����
 *   - ������--incremental �� git ���·��ֻ����Ӱ���Ž���Ʒ��ȫ��ɨ�� G3�������Ž� G9 �㶨�ܣ�
 *     ������Ž��ű����� scripts/ops/��������޷������ҷ����Ե�·��ʱ fail-safe ����ȫ������
 *     �����ճ����ٷ����������տ��� CI ������ȫ����--fast �� G10/G11�����������������������С�
 *   - --fast �̻����ճ�����Ĭ�� --fast������������ G10 vue-tsc���������տ�ȥ�� --fast ��ȫ����
 * �÷���node scripts/ops/run-local-gates.mjs [--fast] [--mvn] [--incremental] [--jobs N] [--plan]
 */
import { execFile, spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { cpus } from 'node:os';

const root = fileURLToPath(new URL('../../', import.meta.url));

// �Ž����塣areas������ģʽ��������Щ·��ǰ׺���ܣ�safety������ģʽ�㶨�ܣ�ȫ�ֺϹ�/��ȫ������
//   ����У������ areas ֮���ȫ��״̬������ G5 �ĵ����Ӵ��������� docs/ ֮��ı�����Ŀ�꣩��
// slow��--fast ������mvn���� --mvn ���á�cmd/mvnArgs �� id ��������л���һ�£�CI��README ��������
export const GATES = [
  { id: 'G1 ��Դ����У��������', cmd: ['node', '--test', 'scripts/verify-source-copy.test.mjs'], areas: ['scripts/verify-source-copy', 'third_party/'] },
  { id: 'G2 Ʒ�������Ž�����', cmd: ['node', '--test', 'scripts/brand/verify-brand-naming.test.mjs'], areas: ['scripts/brand/'] },
  { id: 'G2b ����Ǩ�ƹ��ߵ��⣨�����ļ��ų� + �⻧����Ψһ��', cmd: ['node', '--test', 'scripts/brand/apply-naming-migration.test.mjs'], areas: ['scripts/brand/', 'services/zhongshu-core/sql/'] },
  { id: 'G3 Ʒ������ȫ��ɨ��', cmd: ['node', 'scripts/brand/verify-brand-naming.mjs'], areas: ['services/', 'apps/', 'scripts/', 'docs/'], safety: true },
  { id: 'G4 �ĵ�һ���Ե���', cmd: ['node', '--test', 'scripts/gov/verify-docs.test.mjs'], areas: ['scripts/gov/', 'docs/', 'README.md'] },
  { id: 'G5 �ĵ�һ����ȫ��', cmd: ['node', 'scripts/gov/verify-docs.mjs'], areas: ['scripts/gov/', 'docs/', 'README.md'], safety: true },
  { id: 'G6 ģ�������', cmd: ['node', 'scripts/eng/verify-module-whitelist.mjs'], areas: ['scripts/eng/', 'services/'] },
  { id: 'G7 ����Դ PG ��ͬ', cmd: ['node', 'scripts/db/verify-datasource-pg.mjs'], areas: ['scripts/db/', 'services/'] },
  { id: 'G8 Flyway Ǩ�ƹ淶', cmd: ['node', 'scripts/db/verify-flyway-migrations.mjs'], areas: ['scripts/db/', 'services/'] },
  { id: 'G9 ���������Ž�', cmd: ['node', 'scripts/cfg/verify-config-secrets.mjs'], areas: ['scripts/cfg/', 'services/', 'apps/'], safety: true },
  { id: 'G10 Web ���ͼ�����', cmd: ['node', 'scripts/client/verify-ts-baseline.mjs'], areas: ['apps/zhongshu-admin-web/', 'scripts/client/'], slow: true },
  { id: 'G11 ����ģ���˵��⣨common/infra���ų����λ���ʧ�ܣ�', areas: ['services/'], mvn: true,
    mvnArgs: '-pl zszj-framework/zszj-common,zszj-module-infra -am -Dtest=!CodegenEngineUniappTest#testExecute_treeSearch -Dsurefire.failIfNoSpecifiedTests=false test' },
];

// ����ģʽ����Ϊ�����ԡ�������ȫ�����ˡ���δ����·��ǰ׺��������/����ԭʼ��/�ƻ��壩
const BENIGN_IGNORE = ['outputs/', 'docs/reviews/', '.omx/'];

/** ���챾��Ҫ�ܵ��Ž���ѡ������ --fast/--mvn������������ɸѡ�� */
export function candidateGates({ fast = false, mvn = false } = {}) {
  return GATES.filter((g) => {
    if (g.slow && fast) return false; // G10��--fast ���������� Web ���ͼ��
    if (g.mvn && !mvn) return false;  // G11���� --mvn ��ʽ����
    return true;
  });
}

/**
 * ����ɸѡ�������·��ѡ�Ž����κβ�ȷ���� fail-safe ����ȫ���������ܲ�©�ܣ���
 * ���� { gates, mode: 'full'|'incremental', reason, changed }��
 * @param {object} o
 * @param {boolean} o.fast @param {boolean} o.mvn @param {boolean} o.incremental
 * @param {string[]|null} o.changedFiles ���·������Բֿ������б�ܣ���null ��ʾ�ɵ��÷����
 */
export function planGates({ fast = false, mvn = false, incremental = false, changedFiles = null }) {
  const candidates = candidateGates({ fast, mvn });
  if (!incremental) return { gates: candidates, mode: 'full', reason: 'δ���� --incremental����ȫ��', changed: changedFiles ?? [] };

  const changed = changedFiles ?? [];
  if (!changed.length) return { gates: candidates, mode: 'full', reason: '�������ޱ���������ʧ�ܣ�����ȫ��', changed };
  if (changed.some((f) => f.startsWith('scripts/ops/'))) return { gates: candidates, mode: 'full', reason: '������������Ž��ű�������scripts/ops/��������ȫ��', changed };

  // δ�����ҷ�����·�� �� ���ػ���ȫ�����޷��ж�Ӱ���棩
  const allAreas = candidates.flatMap((g) => g.areas ?? []);
  const unmapped = changed.filter((f) => !allAreas.some((a) => f.startsWith(a)) && !BENIGN_IGNORE.some((b) => f.startsWith(b)));
  if (unmapped.length) return { gates: candidates, mode: 'full', reason: `������${unmapped.length} �����·���޷����ࣨ${unmapped.slice(0, 3).join(', ')}${unmapped.length > 3 ? ' ��' : ''}��������ȫ��`, changed };

  const selected = candidates.filter((g) => g.safety || (g.areas ?? []).some((a) => changed.some((f) => f.startsWith(a))));
  return { gates: selected, mode: 'incremental', reason: `�������� ${changed.length} �����·��ѡ�� ${selected.length}/${candidates.length} �Ž�`, changed };
}

/** ��⹤������� HEAD �ı��·�������Ѹ����޸���δ�������ļ�����ʧ�ܷ��� []�� */
function detectChangedFiles() {
  try {
    const opts = { cwd: root, encoding: 'utf8' };
    // --no-renames����Ŀ¼ rename Ĭ��ֻ��Ŀ��·������©��Դ·������������Ž����� Web��miniapp
    // �ƶ� .ts ֻ�� miniapp �ࡢ©ѡУ Web ����� G10�����ر� rename ���ʹԴ(ɾ)+Ŀ��(��)���ϱ���
    const tracked = spawnSync('git', ['-c', 'core.quotepath=false', 'diff', '--name-only', '--no-renames', 'HEAD'], opts);
    const untracked = spawnSync('git', ['-c', 'core.quotepath=false', 'ls-files', '--others', '--exclude-standard'], opts);
    if (tracked.status !== 0 || untracked.status !== 0) return [];
    const paths = [...String(tracked.stdout ?? '').split('\n'), ...String(untracked.stdout ?? '').split('\n')]
      .map((s) => s.trim().replaceAll('\\', '/')).filter(Boolean);
    return [...new Set(paths)];
  } catch { return []; }
}

/** �����ܵ����Ž������� { id, status, ms, tail }�������쳣��ʧ���� status ����� */
function runGate(gate) {
  const started = Date.now();
  return new Promise((resolve) => {
    const done = (status, output) => resolve({
      id: gate.id, status, ms: Date.now() - started,
      tail: String(output ?? '').trimEnd().split('\n').slice(-4).join(' | ').slice(0, 400),
    });
    if (gate.mvnArgs) {
      // Maven �Ž�����ע�� tools ������������JDK17/Maven ����ϵͳ PATH��
      const child = spawn('bash', ['-c', 'source tools/env.sh && cd services/zhongshu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs],
        { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] });
      let out = '';
      child.stdout.on('data', (d) => { out += d; });
      child.stderr.on('data', (d) => { out += d; });
      child.on('error', (e) => done('FAIL', out + String(e)));
      child.on('close', (code) => done(code === 0 ? 'PASS' : 'FAIL', out));
    } else {
      execFile(gate.cmd[0], gate.cmd.slice(1), { cwd: root, maxBuffer: 64 * 1024 * 1024, encoding: 'utf8' },
        (err, stdout, stderr) => done(err ? 'FAIL' : 'PASS', err ? String(stdout ?? '') + String(stderr ?? '') : String(stdout ?? '')));
    }
  });
}

/** �� jobs ���޲������Ž�������� gates ����˳�򷵻أ������˳���޹أ�����ɼ�ʱ��ӡ���ȡ� */
async function runGates(gates, jobs) {
  const results = new Array(gates.length);
  let next = 0;
  const worker = async () => {
    while (next < gates.length) {
      const i = next++;
      const r = await runGate(gates[i]);
      results[i] = r;
      console.log(`[${r.status}] ${r.id} (${r.ms}ms)`);
    }
  };
  await Promise.all(Array.from({ length: Math.min(jobs, gates.length) }, worker));
  return results;
}

async function main() {
  const argv = process.argv.slice(2);
  const fast = argv.includes('--fast');
  const mvn = argv.includes('--mvn');
  const incremental = argv.includes('--incremental');
  const planOnly = argv.includes('--plan');
  const jobsIdx = argv.indexOf('--jobs');
  const jobs = Math.max(1, jobsIdx >= 0 ? (Number(argv[jobsIdx + 1]) || 1) : (cpus().length || 4));

  const changedFiles = incremental ? detectChangedFiles() : [];
  const plan = planGates({ fast, mvn, incremental, changedFiles });
  if (incremental) console.log(`[����] ${plan.reason}`);

  if (planOnly) {
    console.log(`������ ${plan.gates.length} ���Ž���--plan����ִ�У���`);
    for (const g of plan.gates) console.log('  - ' + g.id);
    return 0;
  }

  console.log(`���� ${plan.gates.length} ���Ž���ģʽ ${plan.mode}������ ${Math.min(jobs, plan.gates.length)}${fast ? '��--fast' : ''}${mvn ? '��--mvn' : ''}����`);
  const t0 = Date.now();
  const results = await runGates(plan.gates, jobs);
  const totalMs = Date.now() - t0;

  console.log('\n===== ���ػ����Ž����� =====');
  for (const r of results) console.log(`${r.status.padEnd(4)}  ${r.id}`);
  const failed = results.filter((r) => r.status === 'FAIL');
  if (failed.length) {
    console.log('\n----- ʧ�����飨���ĩβ��-----');
    for (const r of failed) console.log(`[FAIL] ${r.id}\n  ${r.tail}`);
  }
  console.log(`�ϼ� ${results.length} �ʧ�� ${failed.length} �������ʱ ${totalMs}ms��`);
  return failed.length ? 1 : 0;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/run-local-gates.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); process.exit(1); });
}
Get-Content : �Ҳ���·����E:\����֮��AI����ƽ̨����\package.json������Ϊ��·�������ڡ�
����λ�� ��:2 �ַ�: 89
+ ... s.mjs -Encoding UTF8; Get-Content package.json -Encoding UTF8; Get-Ch ...
+                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : ObjectNotFound: (E:\����֮��AI����ƽ̨����\package.json:String) [Get-Content], ItemNotFou
ndExcepti    on
    + FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetContentCommand
 

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show 2fa36a9e:scripts/client/verify-ts-baseline.mjs; git ls-files '*AGENTS*' '*CLAUDE*' '*instructions*'; node --test scripts/ops/worktree-orchestrate.test.mjs" in E:\众墅之家AI赋能平台底座
 succeeded in 52ms:
✔ slugify：去 ZS- 前缀、小写、. 与非字母数字折叠为单 - (0.7559ms)
✔ slugify：无 ZS- 前缀也能规整（工具对任意 ID 通用） (0.0741ms)
✔ branchName / worktreeDirName：feat/<slug> 与 zszj-wt-<slug> (0.0726ms)
✔ PILOT_TASKS：步骤⑤ 三试点任务 (0.7343ms)
✔ resolvePaths：默认 wtRoot 与主树同级，分支/路径/基线一致 (0.2539ms)
✔ resolvePaths：自定义 baseRef 与 wtRoot 生效 (0.0703ms)
✔ provisionTargets：默认只供给 tools（--fast 门禁不需 node_modules） (0.074ms)
✔ provisionTargets：--node 追加 node_modules（前端 G10 门禁需要） (0.0522ms)
✔ planCreate：首步 git worktree add -b，随后 junction 供给 tools (0.185ms)
✔ planCreate：git worktree add 用 -C 主树（mainRoot 源自 import.meta.url，免疫 cwd 陷阱） (0.1619ms)
✔ planCreate：--no-provision 只留 worktree add 一步 (0.0676ms)
✔ planCreate：--node 供给 tools + node_modules 两个 junction (0.0829ms)
✔ planMerge：默认 --no-ff 保留任务边界，并给出文档串行同步清单 (0.1923ms)
✔ planMerge：--ff-only 切换合并策略；--into 切换目标分支 (0.0737ms)
✔ planCleanup：默认移除 worktree + 安全删分支(-d) (0.7426ms)
✔ planCleanup：--keep-branch 不删分支；--force 用 remove --force + branch -D (0.1214ms)
ℹ tests 16
ℹ suites 0
ℹ pass 16
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 55.1508

codex
The merge command ignores its destination branch, and frontend provisioning targets the wrong directory. All 16 static tests pass, but they do not catch these execution-path defects.

Full review comments:

- [P1] Enforce the requested destination branch before merging — E:\众墅之家AI赋能平台底座\scripts\ops\worktree-orchestrate.mjs:106-107
  When `merge ... --into release` is invoked while the main worktree has `main` checked out, this command merges into `main`, not `release`: `into` only affects metadata and printed messages. The default also silently merges into any other currently checked-out branch. Verify that the main worktree is on `into` and abort on mismatch, or explicitly switch branches safely before merging.

- [P2] Provision dependencies at the frontend application's path — E:\众墅之家AI赋能平台底座\scripts\ops\worktree-orchestrate.mjs:51-52
  For `create ... --node`, provisioning targets `<mainRoot>/node_modules`, but this repository's frontend dependencies live under `apps/zhongshu-admin-web/node_modules`. Consequently, provisioning fails in the existing layout; even if root-level dependencies exist, G10 still cannot use them because `scripts/client/verify-ts-baseline.mjs` explicitly executes `./node_modules/vue-tsc/bin/vue-tsc.js` with the application directory as its working directory. Provision the application-level dependency directory instead.
The merge command ignores its destination branch, and frontend provisioning targets the wrong directory. All 16 static tests pass, but they do not catch these execution-path defects.

Full review comments:

- [P1] Enforce the requested destination branch before merging — E:\众墅之家AI赋能平台底座\scripts\ops\worktree-orchestrate.mjs:106-107
  When `merge ... --into release` is invoked while the main worktree has `main` checked out, this command merges into `main`, not `release`: `into` only affects metadata and printed messages. The default also silently merges into any other currently checked-out branch. Verify that the main worktree is on `into` and abort on mismatch, or explicitly switch branches safely before merging.

- [P2] Provision dependencies at the frontend application's path — E:\众墅之家AI赋能平台底座\scripts\ops\worktree-orchestrate.mjs:51-52
  For `create ... --node`, provisioning targets `<mainRoot>/node_modules`, but this repository's frontend dependencies live under `apps/zhongshu-admin-web/node_modules`. Consequently, provisioning fails in the existing layout; even if root-level dependencies exist, G10 still cannot use them because `scripts/client/verify-ts-baseline.mjs` explicitly executes `./node_modules/vue-tsc/bin/vue-tsc.js` with the application directory as its working directory. Provision the application-level dependency directory instead.

