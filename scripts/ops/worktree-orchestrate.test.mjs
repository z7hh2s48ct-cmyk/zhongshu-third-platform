/**
 * worktree-orchestrate.mjs 的纯静态单测（提效方案 步骤⑤：git worktree 并行编排器）。
 * 只验证路径/分支解析与步骤规划的纯函数，不执行 git、不建 junction、不落任何副作用
 * （符合纯静态单测规范，与 run-local-gates.test.mjs 同风格）。
 * 运行：node --test scripts/ops/worktree-orchestrate.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { join } from 'node:path';
import {
  slugify, branchName, worktreeDirName, resolvePaths, provisionTargets, normPath,
  planCreate, planMerge, planCleanup, PILOT_TASKS, mainRoot, DEFAULT_WT_ROOT,
} from './worktree-orchestrate.mjs';

const norm = (p) => String(p).replaceAll('\\', '/'); // 跨平台断言：统一正斜杠

// ---- slug / 分支 / 目录名解析 ----

test('slugify：去 ZS- 前缀、小写、. 与非字母数字折叠为单 -', () => {
  assert.equal(slugify('ZS-CFG-002.B'), 'cfg-002-b');
  assert.equal(slugify('ZS-SEC-010'), 'sec-010');
  assert.equal(slugify('ZS-CFG-001.B'), 'cfg-001-b');
  assert.equal(slugify('  ZS-CFG-002.B  '), 'cfg-002-b'); // 首尾空白
  assert.equal(slugify('zs-cfg-002.b'), 'cfg-002-b');      // 已小写
});

test('slugify：无 ZS- 前缀也能规整（工具对任意 ID 通用）', () => {
  assert.equal(slugify('CFG-002.B'), 'cfg-002-b');
  assert.equal(slugify('FOO__BAR'), 'foo-bar'); // 连续非字母数字折叠为单 -
});

test('branchName / worktreeDirName：feat/<slug> 与 zszj-wt-<slug>', () => {
  assert.equal(branchName('ZS-CFG-002.B'), 'feat/cfg-002-b');
  assert.equal(worktreeDirName('ZS-CFG-002.B'), 'zszj-wt-cfg-002-b');
  assert.equal(branchName('ZS-SEC-010'), 'feat/sec-010');
});

test('PILOT_TASKS：步骤⑤ 三试点任务', () => {
  assert.deepEqual(PILOT_TASKS, ['ZS-CFG-001.B', 'ZS-CFG-002.B', 'ZS-SEC-010']);
});

test('normPath：统一正斜杠；win32 下大小写不敏感（修复 isolationReport 假阴性：--wt-root 传小写 e: 而 git 规范输出大写 E:/）', () => {
  assert.equal(normPath('a\\b\\c'), 'a/b/c'); // 反斜杠一律转正斜杠（跨平台，小写不变）
  if (process.platform === 'win32') {
    // 关键回归：盘符/大小写差异不应致「worktree 已登记=false」的假阴性
    assert.equal(normPath('E:/众墅之家AI赋能平台底座/.wt/zszj-wt-cfg-002-b'),
                 normPath('e:/众墅之家AI赋能平台底座/.wt/zszj-wt-cfg-002-b'));
  } else {
    assert.equal(normPath('E:/A/b'), 'E:/A/b'); // posix 大小写敏感、仅统一分隔符
  }
});

// ---- resolvePaths ----

test('resolvePaths：默认 wtRoot 与主树同级，分支/路径/基线一致', () => {
  const p = resolvePaths({ taskId: 'ZS-CFG-002.B' });
  assert.equal(p.slug, 'cfg-002-b');
  assert.equal(p.branch, 'feat/cfg-002-b');
  assert.equal(p.baseRef, 'main');
  assert.equal(p.mainRoot, mainRoot);
  assert.equal(norm(p.wtPath), norm(join(DEFAULT_WT_ROOT, 'zszj-wt-cfg-002-b')));
});

test('resolvePaths：自定义 baseRef 与 wtRoot 生效', () => {
  const p = resolvePaths({ taskId: 'ZS-SEC-010', baseRef: 'ae00486e', wtRoot: 'D:/wt' });
  assert.equal(p.baseRef, 'ae00486e');
  assert.equal(norm(p.wtPath), 'D:/wt/zszj-wt-sec-010');
});

// ---- provisionTargets ----

test('provisionTargets：默认只供给 tools（--fast 门禁不需 node_modules）', () => {
  assert.deepEqual(provisionTargets(), ['tools']);
  assert.deepEqual(provisionTargets({ node: false }), ['tools']);
});

test('P2 fix：provisionTargets --node 供给 app 级 node_modules（仓库根无 node_modules；G10 以 app 为 cwd 跑 vue-tsc）', () => {
  assert.deepEqual(provisionTargets({ node: true }), ['tools', 'apps/zhongshu-admin-web/node_modules']);
});

// ---- planCreate ----

test('planCreate：首步 git worktree add -b，随后 junction 供给 tools', () => {
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

test('planCreate：git worktree add 用 -C 主树（mainRoot 源自 import.meta.url，免疫 cwd 陷阱）', () => {
  const plan = planCreate({ taskId: 'ZS-SEC-010', baseRef: 'main' });
  const add = plan.steps[0];
  assert.deepEqual(add.args.slice(0, 2), ['-C', mainRoot]);
  assert.equal(add.args[7], 'main');
});

test('planCreate：--no-provision 只留 worktree add 一步', () => {
  const plan = planCreate({ taskId: 'ZS-CFG-001.B', provision: false });
  assert.equal(plan.provision, false);
  assert.equal(plan.steps.length, 1);
  assert.equal(plan.steps[0].kind, 'git-worktree-add');
});

test('P2 fix：planCreate --node 供给 tools + app 级 node_modules 两个 junction（link/target 指向 app 目录）', () => {
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
  assert.ok(plan.docSyncReminder.some((l) => l.includes('不在 worktree 改')), '应强调文档只在主树串行改');
});

test('P1 fix：planMerge --ff-only 切换合并策略；--into 驱动分支断言目标（防合错分支）', () => {
  const ff = planMerge({ taskId: 'ZS-SEC-010', ffOnly: true });
  assert.deepEqual(ff.steps[1].args.slice(2), ['merge', '--ff-only', 'feat/sec-010']);
  const into = planMerge({ taskId: 'ZS-SEC-010', into: 'release' });
  assert.equal(into.into, 'release');
  // P1 核心：--into release 时首步断言主树必须在 release，否则中止（绝不静默合入当前检出的 main）
  assert.equal(into.steps[0].kind, 'git-assert-branch');
  assert.equal(into.steps[0].expected, 'release');
  assert.deepEqual(into.steps[0].args.slice(2), ['rev-parse', '--abbrev-ref', 'HEAD']);
});

// ---- planCleanup ----

test('planCleanup：默认移除 worktree + 安全删分支(-d)', () => {
  const plan = planCleanup({ taskId: 'ZS-CFG-002.B' });
  assert.equal(plan.steps[0].kind, 'git-worktree-remove');
  assert.equal(norm(plan.steps[0].args[plan.steps[0].args.length - 1]), norm(plan.wtPath));
  assert.deepEqual(plan.steps[1].args.slice(2), ['branch', '-d', 'feat/cfg-002-b']);
});

test('planCleanup：--keep-branch 不删分支；--force 用 remove --force + branch -D', () => {
  const keep = planCleanup({ taskId: 'ZS-SEC-010', keepBranch: true });
  assert.equal(keep.steps.length, 1);
  assert.equal(keep.steps[0].kind, 'git-worktree-remove');
  const force = planCleanup({ taskId: 'ZS-SEC-010', force: true });
  assert.ok(force.steps[0].args.includes('--force'));
  assert.deepEqual(force.steps[1].args.slice(2), ['branch', '-D', 'feat/sec-010']);
});
