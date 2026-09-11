$ chcp 65001 > $null; [Console]::OutputEncoding=[System.Text.Encoding]::UTF8; Set-Location 'e:\众墅之家AI赋能平台底座'; & "$env:APPDATA\npm\codex.cmd" review --commit 5e7c4e1e 2>&1 | Out-String -Width 4096 | Select-Object -Last 1
codex.cmd : OpenAI Codex v0.154.0
所在位置 行:1 字符: 109
+ ... 家AI赋能平台底座'; & "$env:APPDATA\npm\codex.cmd" review --commit 5e7c4e1e 2 ...
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
session id: 01a08f2d-53c0-7a93-9695-cec07c9b0f5d
--------
user
commit 5e7c4e1
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git status --short; git show --stat --oneline 5e7c4e1e; Get-ChildItem -Force -Name AGENTS*' in E:\众墅之家AI赋能平台底座
 succeeded in 27ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git show 5e7c4e1e; Get-ChildItem -Path . -Filter AGENTS.md -Recurse -File | Select-Object -ExpandProperty FullName; Get-ChildItem -Path . -Filter AGENTS.override.md -Recurse -File | Select-Object -ExpandProperty FullName' in E:\众墅之家AI赋能平台底座
 succeeded in 4980ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Content scripts/ops/worktree-orchestrate.mjs; Get-Content scripts/ops/worktree-orchestrate.test.mjs; Get-ChildItem -Force -Name; git config --get core.abbrev; Get-ChildItem E:\\AGENTS* -ErrorAction SilentlyContinue" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
/**
 * ZS-GOV-001 提效方案 步骤⑤：git worktree 并行编排器（试点 ZS-CFG-001.B / ZS-CFG-002.B / ZS-SEC-010）�?
 *
 * 目标：让多个 B03 子项在各自隔离的 worktree + feature 分支上并行推进，主树保持干净、专用于
 *   「合�?+ 文档串行同步」，从根上规避三任务并行�?docs/05(§2/§16.1/§19/README) 的合并冲�?
 *   （见「代码并行、文档串行」策略：worktree 只承载代码，文档只在主树串行编辑）�?
 *
 * 本仓已核实的关键事实（决定了本工具的形态）�?
 *   - run-local-gates.mjs �?root �?import.meta.url 推导 �?跑各 worktree「自己的副本」即自动锁定该树�?
 *     �?gates 子命令用 <wtPath>/scripts/ops/run-local-gates.mjs 而非主树副本�?
 *   - tools/、node_modules/、dist/ 均被 gitignore（tools/ 0 个跟踪文件）�?`git worktree add` 出来的新�?
 *     天生�?JDK/Maven，直接跑 mvn 会失败，须「供给」：把主�?tools/ 以目�?junction 挂进新树�?
 *   - tools/env.sh �?TOOLS 是「绝对路径指主树」→ junction �?source/编译均正确，�?mvn 二进制跨树共用�?
 *     编译的却是各树自己的源码（cwd=各树 services/zhongshu-core），不重复占盘；
 *   - --fast 门禁(G1~G9) 全是 node/git/PG，不需�?tools/；只�?G11(--mvn) 与直�?mvn 需要供给�?
 *
 * 本工具是「主树枢纽」：mainRoot �?import.meta.url 推导（脚本所在仓库），与调用�?cwd 无关�?
 *   天然免疫「终�?cwd/sandbox workspace-root 落到别的项目」的陷阱；始终用主树副本调用本工具�?
 *
 * 子命令：
 *   plan    <taskId> [--base <ref>] [--wt-root <dir>] [--node]   只打�?create 将执行的步骤，零副作�?
 *   create  <taskId> [--base <ref>] [--wt-root <dir>] [--node] [--no-provision]  �?worktree+分支+供给+隔离校验
 *   list                                                        列出全部 worktree + 分支 + 供给状�?
 *   gates   <taskId> [--fast] [--mvn] [--incremental]           在该 worktree 内跑门禁（调用其自身副本�?
 *   merge   <taskId> [--into <branch>] [--ff-only]              合并分支回主�?+ 打印文档串行同步清单
 *   cleanup <taskId> [--keep-branch] [--force]                  移除 worktree�?删已合并分支�?
 *
 * 纯函数（slugify/branchName/worktreeDirName/resolvePaths/provisionTargets/planCreate/planMerge/
 *   planCleanup）不执行 git/fs，供 worktree-orchestrate.test.mjs 静态单测；副作用只�?main() 落地�?
 * 用法：node scripts/ops/worktree-orchestrate.mjs <子命�? <taskId> [选项]
 */
import { spawnSync } from 'node:child_process';
import { existsSync, symlinkSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve, join } from 'node:path';

// 主树根：由脚本自身位置推导（scripts/ops/ 的上两级），与调用时 cwd 无关 �?免疫 sandbox cwd 陷阱�?
export const mainRoot = resolve(fileURLToPath(new URL('../../', import.meta.url)));
// 默认 worktree 父目录：与主树同级（�?E:/），对齐既有 E:/zszj-base-check 的兄弟布局�?
export const DEFAULT_WT_ROOT = dirname(mainRoot);

// 步骤�?试点任务（工具本身对任意 ZS 任务 ID 通用；此表仅用于 list 标注与演示）�?
export const PILOT_TASKS = ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010'];

/**
 * 供给目标：新树因 gitignore 而缺失、需从主�?junction 的工具链目录�?
 *   tools 恒定（env.sh/JDK/Maven，直�?mvn �?G11 依赖）；
 *   node_modules 仅前端门�?G10 vue-tsc)需要——B03 三任务后端为主，默认不供给，--node 显式开启�?
 * @returns {string[]}
 */
export function provisionTargets({ node = false } = {}) {
  // node_modules 在「前�?app 目录」下（apps/zhongshu-admin-web/node_modules），仓库根无 node_modules�?
  // G10 verify-ts-baseline.mjs �?app �?cwd �?./node_modules/vue-tsc，故必须供给 app 级依赖目录�?
  return node ? ['tools', 'apps/zhongshu-admin-web/node_modules'] : ['tools'];
}

/**
 * 任务 ID �?稳定 slug：去 ZS- 前缀、小写、非字母数字折叠为单 -、首尾去 -�?
 * 例：ZS-CFG-002.B �?cfg-002-b；ZS-SEC-010 �?sec-010�?
 */
export function slugify(taskId) {
  return String(taskId).trim().toLowerCase().replace(/^zs-/, '').replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

/** feature 分支名：feat/<slug>�?*/
export function branchName(taskId) {
  return `feat/${slugify(taskId)}`;
}

/** worktree 目录名：zszj-wt-<slug>（对齐既�?zszj-base-check 命名风格）�?*/
export function worktreeDirName(taskId) {
  return `zszj-wt-${slugify(taskId)}`;
}

/** 解析任务的路�?分支/基线为纯数据，不落盘、不执行 git�?*/
export function resolvePaths({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main' }) {
  const slug = slugify(taskId);
  return { taskId, slug, branch: `feat/${slug}`, wtPath: join(wtRoot, `zszj-wt-${slug}`), baseRef, mainRoot };
}

/**
 * 规划 create 步骤（纯数据，供单测�?plan 子命令）�?
 * steps[].kind �?{'git-worktree-add','junction'}；exec 阶段据此落副作用�?
 * @returns {{steps: Array<object>} & ReturnType<typeof resolvePaths>}
 */
export function planCreate({ taskId, wtRoot = DEFAULT_WT_ROOT, baseRef = 'main', provision = true, node = false }) {
  const p = resolvePaths({ taskId, wtRoot, baseRef });
  const steps = [
    { kind: 'git-worktree-add', desc: `�?worktree + 分支 ${p.branch} �?${baseRef}`,
      args: ['-C', mainRoot, 'worktree', 'add', '-b', p.branch, p.wtPath, baseRef] },
  ];
  if (provision) {
    for (const dir of provisionTargets({ node })) {
      steps.push({ kind: 'junction', desc: `供给 ${dir}/（junction �?主树）`, dir, link: join(p.wtPath, dir), target: join(mainRoot, dir) });
    }
  }
  return { ...p, provision, steps };
}

/**
 * 规划 merge 步骤（纯数据�? 文档串行同步清单�?
 * 合并只在主树做；工具「绝不自动改 docs」——文档同步由主树手动/close-task 串行完成，避免并行冲突回流�?
 * 首步为分支断言：git merge 合入的是「当前检出分支」，故先校验主树 HEAD==into，不符即中止（不擅自切分支）�?
 */
export function planMerge({ taskId, into = 'main', ffOnly = false, wtRoot = DEFAULT_WT_ROOT }) {
  const p = resolvePaths({ taskId, wtRoot });
  return {
    ...p, into,
    steps: [
      { kind: 'git-assert-branch', desc: `校验主树当前分支 == ${into}（否则中止，不擅自切分支/合错目标）`, expected: into,
        args: ['-C', mainRoot, 'rev-parse', '--abbrev-ref', 'HEAD'] },
      { kind: 'git-merge', desc: `合并 ${p.branch} �?${into}�?{ffOnly ? '--ff-only' : '--no-ff 保留任务边界'}）`,
        args: ['-C', mainRoot, 'merge', ffOnly ? '--ff-only' : '--no-ff', p.branch] },
    ],
    docSyncReminder: [
      `合并后在主树(${into})「串行」同步文档（不在 worktree 改，避免 §2/§16.1/§19/README 冲突）：`,
      `  1) �?${taskId} 卡片「开发记录」（docs/05 各卡独立行段，天然不冲突）`,
      `  2) �?§16.1 子项�?/ §19 变更记录行`,
      `  3) node scripts/gov/close-task.mjs ${taskId} <新状�? [--bump <VX.Y>]   # 重算并回�?§2/README`,
      `  4) node scripts/gov/verify-docs.mjs ; node scripts/ops/run-local-gates.mjs --fast`,
      `  5) pathspec 原子提交文档同步（排�?outputs/；提交后 diff 复核隔离）`,
    ],
  };
}

/** 规划 cleanup 步骤（纯数据）：移除 worktree，默认删「已合并」分支（-d 安全删，未合并会失败而非强删）�?*/
export function planCleanup({ taskId, wtRoot = DEFAULT_WT_ROOT, keepBranch = false, force = false }) {
  const p = resolvePaths({ taskId, wtRoot });
  const steps = [
    { kind: 'git-worktree-remove', desc: `移除 worktree ${p.wtPath}${force ? '�?-force�? : ''}`,
      args: ['-C', mainRoot, 'worktree', 'remove', ...(force ? ['--force'] : []), p.wtPath] },
  ];
  if (!keepBranch) {
    steps.push({ kind: 'git-branch-delete', desc: `删除${force ? '�?D 强删�? : '已合�?}分支 ${p.branch}`,
      args: ['-C', mainRoot, 'branch', force ? '-D' : '-d', p.branch] });
  }
  return { ...p, steps };
}

// ============================ 以下为副作用层（�?main 调用，不做静态单测） ============================

/** 在主树跑 git�?C mainRoot 已入 args；mainRoot 源自 import.meta.url，与 cwd 无关）�?*/
function runGit(args) {
  const r = spawnSync('git', args, { cwd: mainRoot, encoding: 'utf8' });
  return { ok: r.status === 0, out: String(r.stdout ?? ''), err: String(r.stderr ?? ''), status: r.status };
}

/** 建目�?junction（win32 �?junction、其余用 dir）；幂等：link 已存在则跳过�?*/
function doJunction(step) {
  if (!existsSync(step.target)) return { ok: false, msg: `供给目标不存在：${step.target}` };
  if (existsSync(step.link)) return { ok: true, skipped: true, msg: `已存在，跳过�?{step.link}` };
  try {
    symlinkSync(step.target, step.link, process.platform === 'win32' ? 'junction' : 'dir');
    return { ok: true, msg: `junction�?{step.link} �?${step.target}` };
  } catch (e) {
    return { ok: false, msg: `junction 失败�?{e.message}` };
  }
}

/** 逐步执行 plan �?steps；返回是否全绿。git-assert-branch 失败即「中止」后续步骤（防合错目标分支）�?*/
function execSteps(steps) {
  let allOk = true;
  for (const s of steps) {
    if (s.kind === 'junction') {
      const r = doJunction(s);
      console.log(`${r.ok ? '�? : '�?} ${s.desc} �?${r.msg}`);
      allOk = allOk && r.ok;
    } else if (s.kind === 'git-assert-branch') {
      const r = runGit(s.args);
      const cur = r.out.trim();
      if (!r.ok) { console.error(`�?${s.desc}：无法读取当前分支\n${r.err.trimEnd()}`); return false; }
      if (cur !== s.expected) { console.error(`�?${s.desc}：主树当前在�?{cur}」≠ 目标�?{s.expected}」，中止合并（请先手动切�?${s.expected}，或改用 --into ${cur}）`); return false; }
      console.log(`�?${s.desc}（当�?${cur}）`);
    } else {
      console.log(`�?git ${s.args.join(' ')}`);
      const r = runGit(s.args);
      if (r.out.trim()) console.log(r.out.trimEnd());
      if (!r.ok) { console.error(`�?${s.desc}\n${r.err.trimEnd()}`); allOk = false; }
      else console.log(`�?${s.desc}`);
    }
  }
  return allOk;
}

/** create 后隔离校验：新树在目标分支、主�?HEAD 未被动、worktree 已登记�?*/
function isolationReport(p) {
  const branchInWt = runGit(['-C', p.wtPath, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
  const mainHead = runGit(['-C', mainRoot, 'rev-parse', '--abbrev-ref', 'HEAD']).out.trim();
  const registered = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']).out.includes(p.wtPath.replaceAll('\\', '/'))
    || runGit(['-C', mainRoot, 'worktree', 'list']).out.includes(p.wtPath);
  const toolsOk = existsSync(join(p.wtPath, 'tools', 'env.sh'));
  const ok = branchInWt === p.branch && mainHead === 'main' && registered;
  console.log(`\n----- 隔离校验 -----\n新树分支=${branchInWt}（期�?${p.branch}）\n主树 HEAD=${mainHead}（期�?main 未被动）\nworktree 已登�?${registered}\ntools 供给=${toolsOk}\n结论�?{ok ? 'PASS 隔离完好' : 'FAIL 需排查'}`);
  return ok;
}

/** gates：调用「该 worktree 自己的」run-local-gates 副本 �?root 自动锁定该树�?*/
function doGates(p, flags) {
  const script = join(p.wtPath, 'scripts', 'ops', 'run-local-gates.mjs');
  if (!existsSync(script)) { console.error(`�?worktree 未建或缺门禁脚本�?{script}`); return false; }
  console.log(`�?�?${p.wtPath} 跑门禁（其自身副本，自动锁定本树）：node run-local-gates.mjs ${flags.join(' ')}`);
  const r = spawnSync('node', [script, ...flags], { cwd: p.wtPath, stdio: 'inherit' });
  return r.status === 0;
}

/** list：解�?worktree --porcelain，标注分�?供给/是否试点�?*/
function doList() {
  const r = runGit(['-C', mainRoot, 'worktree', 'list', '--porcelain']);
  if (!r.ok) { console.error(r.err.trimEnd()); return false; }
  const blocks = r.out.split('\n\n').map((b) => b.trim()).filter(Boolean);
  console.log(`主树�?{mainRoot}\n�?${blocks.length} �?worktree：`);
  for (const b of blocks) {
    const path = (b.match(/^worktree (.+)$/m) || [])[1] ?? '?';
    const head = (b.match(/^HEAD ([0-9a-f]+)/m) || [])[1]?.slice(0, 8) ?? '?';
    const branch = ((b.match(/^branch (.+)$/m) || [])[1] ?? 'detached').replace('refs/heads/', '');
    const isMain = path.replaceAll('\\', '/') === mainRoot.replaceAll('\\', '/');
    const tools = isMain ? '(主树)' : existsSync(join(path, 'tools', 'env.sh')) ? 'tools�? : 'tools✗未供给';
    console.log(`  ${head}  ${branch.padEnd(24)} ${path}  ${tools}`);
  }
  console.log(`试点任务�?{PILOT_TASKS.join('�?)}`);
  return true;
}

function printSteps(title, steps) {
  console.log(`${title}�?{steps.length} 步）：`);
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
    console.log('用法：node scripts/ops/worktree-orchestrate.mjs <plan|create|list|gates|merge|cleanup> <ZS-任务ID> [选项]\n详见文件头注释�?);
    return 0;
  }
  if (cmd === 'list') return doList() ? 0 : 1;
  if (!taskId) { console.error('�?缺少 ZS- 任务 ID'); return 2; }

  if (cmd === 'plan') {
    printSteps(`create ${taskId} 将执行`, planCreate({ taskId, wtRoot, baseRef, node }).steps);
    return 0;
  }
  if (cmd === 'create') {
    const plan = planCreate({ taskId, wtRoot, baseRef, provision: !has('--no-provision'), node });
    if (existsSync(plan.wtPath)) { console.error(`�?worktree 路径已存在：${plan.wtPath}（先 cleanup 或换 --wt-root）`); return 1; }
    if (runGit(['-C', mainRoot, 'rev-parse', '--verify', plan.branch]).ok) { console.error(`�?分支已存在：${plan.branch}`); return 1; }
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
  console.error(`�?未知子命令：${cmd}`);
  return 2;
}

const invokedDirectly = process.argv[1] && process.argv[1].replaceAll('\\', '/').endsWith('scripts/ops/worktree-orchestrate.mjs');
if (invokedDirectly) {
  main().then((code) => process.exit(code)).catch((e) => { console.error(e); process.exit(1); });
}
/**
 * worktree-orchestrate.mjs 的纯静态单测（提效方案 步骤⑤：git worktree 并行编排器）�?
 * 只验证路�?分支解析与步骤规划的纯函数，不执�?git、不�?junction、不落任何副作用
 * （符合纯静态单测规范，�?run-local-gates.test.mjs 同风格）�?
 * 运行：node --test scripts/ops/worktree-orchestrate.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { join } from 'node:path';
import {
  slugify, branchName, worktreeDirName, resolvePaths, provisionTargets,
  planCreate, planMerge, planCleanup, PILOT_TASKS, mainRoot, DEFAULT_WT_ROOT,
} from './worktree-orchestrate.mjs';

const norm = (p) => String(p).replaceAll('\\', '/'); // 跨平台断言：统一正斜�?

// ---- slug / 分支 / 目录名解�?----

test('slugify：去 ZS- 前缀、小写�? 与非字母数字折叠为单 -', () => {
  assert.equal(slugify('ZS-CFG-002.B'), 'cfg-002-b');
  assert.equal(slugify('ZS-SEC-010'), 'sec-010');
  assert.equal(slugify('ZS-CFG-001.B'), 'cfg-001-b');
  assert.equal(slugify('  ZS-CFG-002.B  '), 'cfg-002-b'); // 首尾空白
  assert.equal(slugify('zs-cfg-002.b'), 'cfg-002-b');      // 已小�?
});

test('slugify：无 ZS- 前缀也能规整（工具对任意 ID 通用�?, () => {
  assert.equal(slugify('CFG-002.B'), 'cfg-002-b');
  assert.equal(slugify('FOO__BAR'), 'foo-bar'); // 连续非字母数字折叠为�?-
});

test('branchName / worktreeDirName：feat/<slug> �?zszj-wt-<slug>', () => {
  assert.equal(branchName('ZS-CFG-002.B'), 'feat/cfg-002-b');
  assert.equal(worktreeDirName('ZS-CFG-002.B'), 'zszj-wt-cfg-002-b');
  assert.equal(branchName('ZS-SEC-010'), 'feat/sec-010');
});

test('PILOT_TASKS：步骤⑤ 三试点任�?, () => {
  assert.deepEqual(PILOT_TASKS, ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010']);
});

// ---- resolvePaths ----

test('resolvePaths：默�?wtRoot 与主树同级，分支/路径/基线一�?, () => {
  const p = resolvePaths({ taskId: 'ZS-CFG-002.B' });
  assert.equal(p.slug, 'cfg-002-b');
  assert.equal(p.branch, 'feat/cfg-002-b');
  assert.equal(p.baseRef, 'main');
  assert.equal(p.mainRoot, mainRoot);
  assert.equal(norm(p.wtPath), norm(join(DEFAULT_WT_ROOT, 'zszj-wt-cfg-002-b')));
});

test('resolvePaths：自定义 baseRef �?wtRoot 生效', () => {
  const p = resolvePaths({ taskId: 'ZS-SEC-010', baseRef: 'ae00486e', wtRoot: 'D:/wt' });
  assert.equal(p.baseRef, 'ae00486e');
  assert.equal(norm(p.wtPath), 'D:/wt/zszj-wt-sec-010');
});

// ---- provisionTargets ----

test('provisionTargets：默认只供给 tools�?-fast 门禁不需 node_modules�?, () => {
  assert.deepEqual(provisionTargets(), ['tools']);
  assert.deepEqual(provisionTargets({ node: false }), ['tools']);
});

test('P2 fix：provisionTargets --node 供给 app �?node_modules（仓库根�?node_modules；G10 �?app �?cwd �?vue-tsc�?, () => {
  assert.deepEqual(provisionTargets({ node: true }), ['tools', 'apps/zhongshu-admin-web/node_modules']);
});

// ---- planCreate ----

test('planCreate：首�?git worktree add -b，随�?junction 供给 tools', () => {
  const plan = planCreate({ taskId: 'ZS-CFG-002.B' });
  assert.equal(plan.provision, true);
  const add = plan.steps[0];
  assert.equal(add.kind, 'git-worktree-add');
  assert.deepEqual(add.args.slice(2, 6), ['worktree', 'add', '-b', 'feat/cfg-002-b']);
  assert.equal(norm(add.args[6]), norm(plan.wtPath)); // 路径参数指向解析出的 wtPath
  assert.equal(add.args[7], 'main');                 // 基线
  const junc = plan.steps[1];
  assert.equal(junc.kind, 'junction');
  assert.equal(junc.dir, 'tools');
  assert.equal(norm(junc.link), `${norm(plan.wtPath)}/tools`);
  assert.equal(norm(junc.target), `${norm(mainRoot)}/tools`);
});

test('planCreate：git worktree add �?-C 主树（mainRoot 源自 import.meta.url，免�?cwd 陷阱�?, () => {
  const plan = planCreate({ taskId: 'ZS-SEC-010', baseRef: 'main' });
  const add = plan.steps[0];
  assert.deepEqual(add.args.slice(0, 2), ['-C', mainRoot]);
  assert.equal(add.args[7], 'main');
});

test('planCreate�?-no-provision 只留 worktree add 一�?, () => {
  const plan = planCreate({ taskId: 'ZS-CFG-001.B', provision: false });
  assert.equal(plan.provision, false);
  assert.equal(plan.steps.length, 1);
  assert.equal(plan.steps[0].kind, 'git-worktree-add');
});

test('P2 fix：planCreate --node 供给 tools + app �?node_modules 两个 junction（link/target 指向 app 目录�?, () => {
  const plan = planCreate({ taskId: 'ZS-CFG-001.B', node: true });
  const junctions = plan.steps.filter((s) => s.kind === 'junction');
  assert.deepEqual(junctions.map((s) => s.dir), ['tools', 'apps/zhongshu-admin-web/node_modules']);
  const nm = junctions[1];
  assert.equal(norm(nm.link), `${norm(plan.wtPath)}/apps/zhongshu-admin-web/node_modules`);
  assert.equal(norm(nm.target), `${norm(mainRoot)}/apps/zhongshu-admin-web/node_modules`);
});

// ---- planMerge ----

test('planMerge：默认先断言主树分支==into，再 --no-ff 合并；含文档串行同步清单', () => {
  const plan = planMerge({ taskId: 'ZS-CFG-002.B' });
  assert.equal(plan.steps[0].kind, 'git-assert-branch');
  assert.equal(plan.steps[0].expected, 'main');
  const m = plan.steps[1];
  assert.equal(m.kind, 'git-merge');
  assert.deepEqual(m.args.slice(2), ['merge', '--no-ff', 'feat/cfg-002-b']);
  assert.equal(plan.into, 'main');
  assert.ok(Array.isArray(plan.docSyncReminder) && plan.docSyncReminder.length >= 5);
  assert.ok(plan.docSyncReminder.some((l) => l.includes('close-task.mjs')), '应提示用 close-task 重算 §2/README');
  assert.ok(plan.docSyncReminder.some((l) => l.includes('不在 worktree �?)), '应强调文档只在主树串行改');
});

test('P1 fix：planMerge --ff-only 切换合并策略�?-into 驱动分支断言目标（防合错分支�?, () => {
  const ff = planMerge({ taskId: 'ZS-SEC-010', ffOnly: true });
  assert.deepEqual(ff.steps[1].args.slice(2), ['merge', '--ff-only', 'feat/sec-010']);
  const into = planMerge({ taskId: 'ZS-SEC-010', into: 'release' });
  assert.equal(into.into, 'release');
  // P1 核心�?-into release 时首步断言主树必须�?release，否则中止（绝不静默合入当前检出的 main�?
  assert.equal(into.steps[0].kind, 'git-assert-branch');
  assert.equal(into.steps[0].expected, 'release');
  assert.deepEqual(into.steps[0].args.slice(2), ['rev-parse', '--abbrev-ref', 'HEAD']);
});

// ---- planCleanup ----

test('planCleanup：默认移�?worktree + 安全删分�?-d)', () => {
  const plan = planCleanup({ taskId: 'ZS-CFG-002.B' });
  assert.equal(plan.steps[0].kind, 'git-worktree-remove');
  assert.equal(norm(plan.steps[0].args[plan.steps[0].args.length - 1]), norm(plan.wtPath));
  assert.deepEqual(plan.steps[1].args.slice(2), ['branch', '-d', 'feat/cfg-002-b']);
});

test('planCleanup�?-keep-branch 不删分支�?-force �?remove --force + branch -D', () => {
  const keep = planCleanup({ taskId: 'ZS-SEC-010', keepBranch: true });
  assert.equal(keep.steps.length, 1);
  assert.equal(keep.steps[0].kind, 'git-worktree-remove');
  const force = planCleanup({ taskId: 'ZS-SEC-010', force: true });
  assert.ok(force.steps[0].args.includes('--force'));
  assert.deepEqual(force.steps[1].args.slice(2), ['branch', '-D', 'feat/sec-010']);
});
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Content -Encoding UTF8 scripts/ops/verify-ts-baseline.mjs; Get-Content -Encoding UTF8 apps/zhongshu-admin-web/package.json; node --test scripts/ops/worktree-orchestrate.test.mjs; git ls-files '*AGENTS*' '*CLAUDE*'; git log -1 --oneline" in E:\众墅之家AI赋能平台底座
 succeeded in 140ms:
    "@types/nprogress": "^0.2.3",
    "@types/qrcode": "^1.5.6",
    "@types/qs": "^6.15.0",
    "@unocss/eslint-config": "^66.6.8",
    "@unocss/eslint-plugin": "66.6.8",
    "@unocss/transformer-variant-group": "^66.6.8",
    "@vitejs/plugin-vue": "^6.0.6",
    "@vitejs/plugin-vue-jsx": "^5.1.5",
    "autoprefixer": "^10.5.0",
    "bpmn-js": "^18.16.1",
    "bpmn-js-properties-panel": "5.54.0",
    "consola": "^3.4.2",
    "eslint": "^10.3.0",
    "eslint-plugin-vue": "^10.9.1",
    "lint-staged": "^16.4.0",
    "postcss": "^8.5.14",
    "postcss-html": "^1.8.1",
    "postcss-scss": "^4.0.9",
    "prettier": "^3.8.3",
    "prettier-eslint": "^16.4.2",
    "rimraf": "^6.1.3",
    "rollup": "^4.60.3",
    "sass": "^1.99.0",
    "stylelint": "^17.11.0",
    "stylelint-config-html": "^1.1.0",
    "stylelint-config-recommended": "^18.0.0",
    "stylelint-config-standard": "^40.0.0",
    "stylelint-order": "^8.1.1",
    "typescript": "6.0.3",
    "typescript-eslint": "^8.59.2",
    "unocss": "^66.6.8",
    "unplugin-auto-import": "^21.0.0",
    "unplugin-element-plus": "^0.11.2",
    "unplugin-vue-components": "^32.0.0",
    "vite": "8.1.4",
    "vite-plugin-compression": "^0.5.1",
    "vite-plugin-svg-icons-ng": "^1.9.0",
    "vue-eslint-parser": "^10.4.0",
    "vue-tsc": "^3.2.8"
  },
  "license": "MIT",
  "repository": {
    "type": "git",
    "url": "git+https://gitee.com/yudaocode/yudao-ui-admin-vue3"
  },
  "bugs": {
    "url": "https://gitee.com/yudaocode/yudao-ui-admin-vue3/issues"
  },
  "homepage": "https://gitee.com/yudaocode/yudao-ui-admin-vue3",
  "web-types": "./web-types.json",
  "engines": {
    "node": ">= 20.19.0",
    "pnpm": ">=8.6.0"
  }
}
✔ slugify：去 ZS- 前缀、小写、. 与非字母数字折叠为单 - (0.713ms)
✔ slugify：无 ZS- 前缀也能规整（工具对任意 ID 通用） (0.0772ms)
✔ branchName / worktreeDirName：feat/<slug> 与 zszj-wt-<slug> (0.0637ms)
✔ PILOT_TASKS：步骤⑤ 三试点任务 (0.6866ms)
✔ resolvePaths：默认 wtRoot 与主树同级，分支/路径/基线一致 (0.2471ms)
✔ resolvePaths：自定义 baseRef 与 wtRoot 生效 (0.0705ms)
✔ provisionTargets：默认只供给 tools（--fast 门禁不需 node_modules） (0.0803ms)
✔ P2 fix：provisionTargets --node 供给 app 级 node_modules（仓库根无 node_modules；G10 以 app 为 cwd 跑 vue-tsc） (0.0448ms)
✔ planCreate：首步 git worktree add -b，随后 junction 供给 tools (0.163ms)
✔ planCreate：git worktree add 用 -C 主树（mainRoot 源自 import.meta.url，免疫 cwd 陷阱） (0.1487ms)
✔ planCreate：--no-provision 只留 worktree add 一步 (0.0687ms)
✔ P2 fix：planCreate --node 供给 tools + app 级 node_modules 两个 junction（link/target 指向 app 目录） (0.1213ms)
✔ planMerge：默认先断言主树分支==into，再 --no-ff 合并；含文档串行同步清单 (0.1557ms)
✔ P1 fix：planMerge --ff-only 切换合并策略；--into 驱动分支断言目标（防合错分支） (0.5236ms)
✔ planCleanup：默认移除 worktree + 安全删分支(-d) (0.1976ms)
✔ planCleanup：--keep-branch 不删分支；--force 用 remove --force + branch -D (0.0881ms)
ℹ tests 16
ℹ suites 0
ℹ pass 16
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 63.2555
5e7c4e1e ZS-GOV-001 步骤⑤ 编排器 codex P1+P2 修复（merge 分支断言 + app 级 node_modules 供给）

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git ls-files '*verify-ts-baseline*'; git grep -n 'tsBuildInfoFile\\|vue-tsc' -- scripts/gov/verify-ts-baseline.mjs; git grep -n 'worktree-orchestrate\\|planMerge' -- scripts ':"'!scripts/ops/worktree-orchestrate*'"'; Get-ChildItem -Force .github -Name" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
scripts/client/verify-ts-baseline.mjs
workflows

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git show HEAD:scripts/client/verify-ts-baseline.mjs' in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
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

codex
No actionable regressions were identified. The branch assertion stops merges when the checked-out branch differs from the requested target, and dependency provisioning now matches the frontend gate’s application-level path. All 16 orchestrator tests pass.
No actionable regressions were identified. The branch assertion stops merges when the checked-out branch differs from the requested target, and dependency provisioning now matches the frontend gate’s application-level path. All 16 orchestrator tests pass.

