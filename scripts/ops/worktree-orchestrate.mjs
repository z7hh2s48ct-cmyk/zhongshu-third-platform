/**
 * ZS-GOV-001 提效方案 步骤⑤：git worktree 并行编排器（试点 ZS-CFG-001.B / ZS-CFG-002.B / ZS-SEC-010）。
 *
 * 目标：让多个 B03 子项在各自隔离的 worktree + feature 分支上并行推进，主树保持干净、专用于
 *   「合并 + 文档串行同步」，从根上规避三任务并行改 docs/05(§2/§16.1/§19/README) 的合并冲突
 *   （见「代码并行、文档串行」策略：worktree 只承载代码，文档只在主树串行编辑）。
 *
 * 本仓已核实的关键事实（决定了本工具的形态）：
 *   - run-local-gates.mjs 的 root 由 import.meta.url 推导 → 跑各 worktree「自己的副本」即自动锁定该树，
 *     故 gates 子命令用 <wtPath>/scripts/ops/run-local-gates.mjs 而非主树副本；
 *   - tools/、node_modules/、dist/ 均被 gitignore（tools/ 0 个跟踪文件）→ `git worktree add` 出来的新树
 *     天生无 JDK/Maven，直接跑 mvn 会失败，须「供给」：把主树 tools/ 以目录 junction 挂进新树；
 *   - tools/env.sh 的 TOOLS 是「绝对路径指主树」→ junction 后 source/编译均正确，且 mvn 二进制跨树共用、
 *     编译的却是各树自己的源码（cwd=各树 services/zhongshu-core），不重复占盘；
 *   - --fast 门禁(G1~G9) 全是 node/git/PG，不需要 tools/；只有 G11(--mvn) 与直接 mvn 需要供给。
 *
 * 本工具是「主树枢纽」：mainRoot 由 import.meta.url 推导（脚本所在仓库），与调用时 cwd 无关，
 *   天然免疫「终端 cwd/sandbox workspace-root 落到别的项目」的陷阱；始终用主树副本调用本工具。
 *
 * 子命令：
 *   plan    <taskId> [--base <ref>] [--wt-root <dir>] [--node]   只打印 create 将执行的步骤，零副作用
 *   create  <taskId> [--base <ref>] [--wt-root <dir>] [--node] [--no-provision]  建 worktree+分支+供给+隔离校验
 *   list                                                        列出全部 worktree + 分支 + 供给状态
 *   gates   <taskId> [--fast] [--mvn] [--incremental]           在该 worktree 内跑门禁（调用其自身副本）
 *   merge   <taskId> [--into <branch>] [--ff-only]              合并分支回主树 + 打印文档串行同步清单
 *   cleanup <taskId> [--keep-branch] [--force]                  移除 worktree（+删已合并分支）
 *
 * 纯函数（slugify/branchName/worktreeDirName/resolvePaths/provisionTargets/planCreate/planMerge/
 *   planCleanup）不执行 git/fs，供 worktree-orchestrate.test.mjs 静态单测；副作用只在 main() 落地。
 * 用法：node scripts/ops/worktree-orchestrate.mjs <子命令> <taskId> [选项]
 */
import { spawnSync } from 'node:child_process';
import { existsSync, symlinkSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve, join } from 'node:path';

// 主树根：由脚本自身位置推导（scripts/ops/ 的上两级），与调用时 cwd 无关 → 免疫 sandbox cwd 陷阱。
export const mainRoot = resolve(fileURLToPath(new URL('../../', import.meta.url)));
// 默认 worktree 父目录：与主树同级（如 E:/），对齐既有 E:/zszj-base-check 的兄弟布局。
export const DEFAULT_WT_ROOT = dirname(mainRoot);

// 步骤⑤ 试点任务（工具本身对任意 ZS 任务 ID 通用；此表仅用于 list 标注与演示）。
export const PILOT_TASKS = ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010'];

/**
 * 供给目标：新树因 gitignore 而缺失、需从主树 junction 的工具链目录。
 *   tools 恒定（env.sh/JDK/Maven，直接 mvn 与 G11 依赖）；
 *   node_modules 仅前端门禁(G10 vue-tsc)需要——B03 三任务后端为主，默认不供给，--node 显式开启。
 * @returns {string[]}
 */
export function provisionTargets({ node = false } = {}) {
  return node ? ['tools', 'node_modules'] : ['tools'];
}

/**
 * 任务 ID → 稳定 slug：去 ZS- 前缀、小写、非字母数字折叠为单 -、首尾去 -。
 * 例：ZS-CFG-002.B → cfg-002-b；ZS-SEC-010 → sec-010。
 */
export function slugify(taskId) {
  return String(taskId).trim().toLowerCase().replace(/^zs-/, '').replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

/** feature 分支名：feat/<slug>。 */
export function branchName(taskId) {
  return `feat/${slugify(taskId)}`;
}

/** worktree 目录名：zszj-wt-<slug>（对齐既有 zszj-base-check 命名风格）。 */
export function worktreeDirName(taskId) {
  return `zszj-wt-${slugify(taskId)}`;
}

/** 解析任务的路径/分支/基线为纯数据，不落盘、不执行 git。 */
export function resolvePaths({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main' }) {
  const slug = slugify(taskId);
  return { taskId, slug, branch: `feat/${slug}`, wtPath: join(wtRoot, `zszj-wt-${slug}`), baseRef, mainRoot };
}

/**
 * 规划 create 步骤（纯数据，供单测与 plan 子命令）。
 * steps[].kind ∈ {'git-worktree-add','junction'}；exec 阶段据此落副作用。
 * @returns {{steps: Array<object>} & ReturnType<typeof resolvePaths>}
 */
export function planCreate({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main', provision = true, node = false }) {
  const p = resolvePaths({ taskId, wtRoot, baseRef });
  const steps = [
    { kind: 'git-worktree-add', desc: `建 worktree + 分支 ${p.branch} ← ${baseRef}`,
      args: ['-C', mainRoot, 'worktree', 'add', '-b', p.branch, p.wtPath, baseRef] },
  ];
  if (provision) {
    for (const dir of provisionTargets({ node })) {
      steps.push({ kind: 'junction', desc: `供给 ${dir}/（junction → 主树）`, dir, link: join(p.wtPath, dir), target: join(mainRoot, dir) });
    }
  }
  return { ...p, provision, steps };
}

/**
 * 规划 merge 步骤（纯数据）+ 文档串行同步清单。
 * 合并只在主树做；工具「绝不自动改 docs」——文档同步由主树手动/close-task 串行完成，避免并行冲突回流。
 */
export function planMerge({ taskId, into = 'main', ffOnly = false, wtRoot = DEFAULT_WT_ROOT }) {
  const p = resolvePaths({ taskId, wtRoot });
  return {
    ...p, into,
    steps: [{ kind: 'git-merge', desc: `合并 ${p.branch} → ${into}（${ffOnly ? '--ff-only' : '--no-ff 保留任务边界'}）`,
      args: ['-C', mainRoot, 'merge', ffOnly ? '--ff-only' : '--no-ff', p.branch] }],
    docSyncReminder: [
      `合并后在主树(${into})「串行」同步文档（不在 worktree 改，避免 §2/§16.1/§19/README 冲突）：`,
      `  1) 写 ${taskId} 卡片「开发记录」（docs/05 各卡独立行段，天然不冲突）`,
      `  2) 补 §16.1 子项行 / §19 变更记录行`,
      `  3) node scripts/gov/close-task.mjs ${taskId} <新状态> [--bump <VX.Y>]   # 重算并回填 §2/README`,
      `  4) node scripts/gov/verify-docs.mjs ; node scripts/ops/run-local-gates.mjs --fast`,
      `  5) pathspec 原子提交文档同步（排除 outputs/；提交后 diff 复核隔离）`,
    ],
  };
}

/** 规划 cleanup 步骤（纯数据）：移除 worktree，默认删「已合并」分支（-d 安全删，未合并会失败而非强删）。 */
export function planCleanup({ taskId, wtRoot = DEFAULT_WT_ROOT, keepBranch = false, force = false }) {
  const p = resolvePaths({ taskId, wtRoot });
  const steps = [
    { kind: 'git-worktree-remove', desc: `移除 worktree ${p.wtPath}${force ? '（--force）' : ''}`,
      args: ['-C', mainRoot, 'worktree', 'remove', ...(force ? ['--force'] : []), p.wtPath] },
  ];
  if (!keepBranch) {
    steps.push({ kind: 'git-branch-delete', desc: `删除${force ? '（-D 强删）' : '已合并'}分支 ${p.branch}`,
      args: ['-C', mainRoot, 'branch', force ? '-D' : '-d', p.branch] });
  }
  return { ...p, steps };
}

// ============================ 以下为副作用层（仅 main 调用，不做静态单测） ============================

/** 在主树跑 git（-C mainRoot 已入 args；mainRoot 源自 import.meta.url，与 cwd 无关）。 */
function runGit(args) {
  const r = spawnSync('git', args, { cwd: mainRoot, encoding: 'utf8' });
  return { ok: r.status === 0, out: String(r.stdout ?? ''), err: String(r.stderr ?? ''), status: r.status };
}

/** 建目录 junction（win32 用 junction、其余用 dir）；幂等：link 已存在则跳过。 */
function doJunction(step) {
  if (!existsSync(step.target)) return { ok: false, msg: `供给目标不存在：${step.target}` };
  if (existsSync(step.link)) return { ok: true, skipped: true, msg: `已存在，跳过：${step.link}` };
  try {
    symlinkSync(step.target, step.link, process.platform === 'win32' ? 'junction' : 'dir');
    return { ok: true, msg: `junction：${step.link} → ${step.target}` };
  } catch (e) {
    return { ok: false, msg: `junction 失败：${e.message}` };
  }
}

/** 逐步执行 plan 的 steps；返回是否全绿。 */
function execSteps(steps) {
  let allOk = true;
  for (const s of steps) {
    if (s.kind === 'junction') {
      const r = doJunction(s);
      console.log(`${r.ok ? '✓' : '✗'} ${s.desc} — ${r.msg}`);
      allOk = allOk && r.ok;
    } else {
      console.log(`→ git ${s.args.join(' ')}`);
      const r = runGit(s.args);
      if (r.out.trim()) console.log(r.out.trimEnd());
      if (!r.ok) { console.error(`✗ ${s.desc}\n${r.err.trimEnd()}`); allOk = false; }
      else console.log(`✓ ${s.desc}`);
    }
  }
  return allOk;
}

/** create 后隔离校验：新树在目标分支、主树 HEAD 未被动、worktree 已登记。 */
function isolationReport(p) {
  const branchInWt = runGit(['-C', p.wtPath, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
  const mainHead = runGit(['-C', mainRoot, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
  const registered = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']).out.includes(p.wtPath.replaceAll('\\', '/'))
    || runGit(['-C', mainRoot, 'worktree', 'list']).out.includes(p.wtPath);
  const toolsOk = existsSync(join(p.wtPath, 'tools', 'env.sh'));
  const ok = branchInWt === p.branch && mainHead === 'main' && registered;
  console.log(`\n----- 隔离校验 -----\n新树分支=${branchInWt}（期望 ${p.branch}）\n主树 HEAD=${mainHead}（期望 main 未被动）\nworktree 已登记=${registered}\ntools 供给=${toolsOk}\n结论：${ok ? 'PASS 隔离完好' : 'FAIL 需排查'}`);
  return ok;
}

/** gates：调用「该 worktree 自己的」run-local-gates 副本 → root 自动锁定该树。 */
function doGates(p, flags) {
  const script = join(p.wtPath, 'scripts', 'ops', 'run-local-gates.mjs');
  if (!existsSync(script)) { console.error(`✗ worktree 未建或缺门禁脚本：${script}`); return false; }
  console.log(`→ 在 ${p.wtPath} 跑门禁（其自身副本，自动锁定本树）：node run-local-gates.mjs ${flags.join(' ')}`);
  const r = spawnSync('node', [script, ...flags], { cwd: p.wtPath, stdio: 'inherit' });
  return r.status === 0;
}

/** list：解析 worktree --porcelain，标注分支/供给/是否试点。 */
function doList() {
  const r = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']);
  if (!r.ok) { console.error(r.err.trimEnd()); return false; }
  const blocks = r.out.split('\n\n').map((b) => b.trim()).filter(Boolean);
  console.log(`主树：${mainRoot}\n共 ${blocks.length} 个 worktree：`);
  for (const b of blocks) {
    const path = (b.match(/^worktree (.+)$/m) || [])[1] ?? '?';
    const head = (b.match(/^HEAD ([0-9a-f]+)/m) || [])[1]?.slice(0, 8) ?? '?';
    const branch = ((b.match(/^branch (.+)$/m) || [])[1] ?? 'detached').replace('refs/heads/', '');
    const isMain = path.replaceAll('\\', '/') === mainRoot.replaceAll('\\', '/');
    const tools = isMain ? '(主树)' : existsSync(join(path, 'tools', 'env.sh')) ? 'tools✓' : 'tools✗未供给';
    console.log(`  ${head}  ${branch.padEnd(24)} ${path}  ${tools}`);
  }
  console.log(`试点任务：${PILOT_TASKS.join('、')}`);
  return true;
}

function printSteps(title, steps) {
  console.log(`${title}（${steps.length} 步）：`);
  for (const s of steps) console.log(`  - [${s.kind}] ${s.desc}`);
}

async function main() {
  const argv = process.argv.slice(2);
  const cmd = argv[0];
  const taskId = argv.find((a) => /^ZS-/i.test(a));
  const has = (f) => argv.includes(f);
  const optVal = (f, dflt) => { const i = argv.indexOf(f); return i >= 0 && argv[i + 1] ? argv[i + 1] : dflt; };
  const wtRoot = resolve(optVal('--wt-root', DEFAULT_WT_ROOT));
  const baseRef = optVal('--base', 'main');
  const node = has('--node');

  if (!cmd || cmd === '--help' || cmd === '-h') {
    console.log('用法：node scripts/ops/worktree-orchestrate.mjs <plan|create|list|gates|merge|cleanup> <ZS-任务ID> [选项]\n详见文件头注释。');
    return 0;
  }
  if (cmd === 'list') return doList() ? 0 : 1;
  if (!taskId) { console.error('✗ 缺少 ZS- 任务 ID'); return 2; }

  if (cmd === 'plan') {
    printSteps(`create ${taskId} 将执行`, planCreate({ taskId, wtRoot, baseRef, node }).steps);
    return 0;
  }
  if (cmd === 'create') {
    const plan = planCreate({ taskId, wtRoot, baseRef, provision: !has('--no-provision'), node });
    if (existsSync(plan.wtPath)) { console.error(`✗ worktree 路径已存在：${plan.wtPath}（先 cleanup 或换 --wt-root）`); return 1; }
    if (runGit(['-C', mainRoot, 'rev-parse', '--verify', plan.branch]).ok) { console.error(`✗ 分支已存在：${plan.branch}`); return 1; }
    const ok = execSteps(plan.steps) && isolationReport(plan);
    return ok ? 0 : 1;
  }
  if (cmd === 'gates') {
    const p = resolvePaths({ taskId, wtRoot });
    const flags = argv.filter((a) => a.startsWith('--') && a !== '--wt-root');
    return doGates(p, flags) ? 0 : 1;
  }
  if (cmd === 'merge') {
    const plan = planMerge({ taskId, into: optVal('--into', 'main'), ffOnly: has('--ff-only'), wtRoot });
    const ok = execSteps(plan.steps);
    console.log('\n----- 文档串行同步清单 -----');
    for (const line of plan.docSyncReminder) console.log(line);
    return ok ? 0 : 1;
  }
  if (cmd === 'cleanup') {
    const plan = planCleanup({ taskId, wtRoot, keepBranch: has('--keep-branch'), force: has('--force') });
    return execSteps(plan.steps) ? 0 : 1;
  }
  console.error(`✗ 未知子命令：${cmd}`);
  return 2;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/worktree-orchestrate.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); process.exit(1); });
}
