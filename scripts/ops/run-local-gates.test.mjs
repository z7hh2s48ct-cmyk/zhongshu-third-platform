/**
 * run-local-gates.mjs 的纯静态单测（ZS-OPS-001.A 提速：并发 + 增量门禁）。
 * 只验证门禁候选/增量规划的纯函数，不执行 git、不跑任何门禁子进程（符合纯静态单测规范）。
 * 运行：node --test scripts/ops/run-local-gates.test.mjs
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GATES, candidateGates, planGates } from './run-local-gates.mjs';

const ids = (gs) => gs.map((g) => g.id.split(' ')[0]); // 取 G1/G2b/G10 等短标识便于断言

test('GATES 定义稳定：14 项，G3/G5/G9 为安全守卫，G10/G13 慢检查，G11/G12 需 mvn', () => {
  assert.equal(GATES.length, 14);
  assert.deepEqual(GATES.filter((g) => g.safety).map((g) => g.id.split(' ')[0]), ['G3', 'G5', 'G9']);
  assert.deepEqual(GATES.filter((g) => g.slow).map((g) => g.id.split(' ')[0]), ['G10', 'G13']);
  assert.deepEqual(GATES.filter((g) => g.mvn).map((g) => g.id.split(' ')[0]), ['G11', 'G12']);
});

test('candidateGates 默认（无 fast/mvn）：含 G10/G13、排除 mvn 门禁', () => {
  const c = ids(candidateGates());
  assert.ok(c.includes('G10') && c.includes('G13'));
  assert.ok(!c.includes('G11') && !c.includes('G12'));
  assert.equal(c.length, 12);
});

test('candidateGates --fast：跳过 G10/G13 与 mvn 门禁，恰为 10 项（与 CI 一致）', () => {
  const c = ids(candidateGates({ fast: true }));
  assert.equal(c.length, 10);
  assert.ok(!c.includes('G10') && !c.includes('G13'));
  assert.ok(!c.includes('G11') && !c.includes('G12'));
  assert.deepEqual(c, ['G1', 'G2', 'G2b', 'G3', 'G4', 'G5', 'G6', 'G7', 'G8', 'G9']);
});

test('candidateGates --mvn（非 fast）：G10/G13/G11/G12 均启用，共 14 项', () => {
  const c = ids(candidateGates({ mvn: true }));
  assert.ok(c.includes('G10') && c.includes('G13'));
  assert.ok(c.includes('G11') && c.includes('G12'));
  assert.equal(c.length, 14);
});

test('candidateGates --fast --mvn：跳过 G10/G13、启用 G11/G12', () => {
  const c = ids(candidateGates({ fast: true, mvn: true }));
  assert.ok(!c.includes('G10') && !c.includes('G13'));
  assert.ok(c.includes('G11') && c.includes('G12'));
});

test('planGates 非增量：mode=full，门禁等于候选', () => {
  const p = planGates({ fast: true });
  assert.equal(p.mode, 'full');
  assert.equal(p.gates.length, 10);
});

test('planGates 增量 + 仅文档变更：选中 G3/G4/G5/G9，跳过 G1/G2/G6/G7/G8', () => {
  const p = planGates({ fast: true, incremental: true, changedFiles: ['docs/05-底座模块分析与开发任务清单.md', 'README.md'] });
  assert.equal(p.mode, 'incremental');
  assert.deepEqual(ids(p.gates), ['G3', 'G4', 'G5', 'G9']);
});

test('planGates 增量 + 后端变更：选中 G3/G5/G6/G7/G8/G9（services 命中 + G5 安全守卫）', () => {
  const p = planGates({ fast: true, incremental: true, changedFiles: ['services/zhongshu-core/zszj-module-system/src/main/java/X.java'] });
  assert.equal(p.mode, 'incremental');
  assert.deepEqual(ids(p.gates), ['G3', 'G5', 'G6', 'G7', 'G8', 'G9']);
});

test('planGates 增量 + 后端变更 + mvn：追加 G11', () => {
  const p = planGates({ fast: true, mvn: true, incremental: true, changedFiles: ['services/zhongshu-core/pom.xml'] });
  assert.equal(p.mode, 'incremental');
  assert.ok(ids(p.gates).includes('G11'));
});

test('planGates 增量 fail-safe：变更含门禁脚本自身 scripts/ops/ → 回退全量', () => {
  const p = planGates({ fast: true, incremental: true, changedFiles: ['scripts/ops/run-local-gates.mjs'] });
  assert.equal(p.mode, 'full');
  assert.equal(p.gates.length, 10);
});

test('planGates 增量 fail-safe：无法归类且非良性路径 → 回退全量', () => {
  const p = planGates({ fast: true, incremental: true, changedFiles: ['some-random-file.txt'] });
  assert.equal(p.mode, 'full');
  assert.match(p.reason, /无法归类/);
});

test('planGates 增量 fail-safe：无变更/检测失败 → 回退全量', () => {
  assert.equal(planGates({ fast: true, incremental: true, changedFiles: [] }).mode, 'full');
  assert.equal(planGates({ fast: true, incremental: true, changedFiles: null }).mode, 'full');
});

test('planGates 增量 + 良性未归类路径 outputs/：不回退全量，仅跑安全守卫 G3/G5/G9', () => {
  const p = planGates({ fast: true, incremental: true, changedFiles: ['outputs/sketchup/x.png'] });
  assert.equal(p.mode, 'incremental');
  assert.deepEqual(ids(p.gates), ['G3', 'G5', 'G9']);
});

test('planGates 增量：安全守卫 G3/G5/G9 在任何选中集内恒定出现', () => {
  for (const changed of [['docs/x.md'], ['services/a/X.java'], ['scripts/brand/y.mjs'], ['outputs/z.png']]) {
    const got = ids(planGates({ fast: true, incremental: true, changedFiles: changed }).gates);
    assert.ok(got.includes('G3') && got.includes('G5') && got.includes('G9'), `变更 ${changed} 未恒定选中 G3/G5/G9`);
  }
});

// ---- codex P2 回归：增量门禁的依赖映射补全 ----

test('P2#1 planGates 增量 + SQL 种子变更：G2b 被选中（跨租户域名唯一性依赖种子数据）', () => {
  // apply-naming-migration.test.mjs 读全仓 SQL 种子核对 system_tenant.websites 唯一；
  // 改 PG 种子制造同域重复时 G2b 能测出，故其 areas 必须覆盖 services/zhongshu-core/sql/。
  const p = planGates({ fast: true, incremental: true, changedFiles: ['services/zhongshu-core/sql/postgresql/ruoyi-vue-pro.sql'] });
  assert.equal(p.mode, 'incremental');
  assert.ok(ids(p.gates).includes('G2b'), 'SQL 种子变更未选中 G2b（租户同域缺陷会漏检）');
});

test('P2#3 planGates 增量 + docs/ 之外被链接目标变更：G5 恒定选中（链接存在性依赖）', () => {
  // verify-docs R1 校验 docs/ 里链接的目标是否存在；删除 docs/ 之外的被链接文件（如 Dockerfile）
  // 会造成悬空链接，故 G5 须在增量模式恒跑（safety），不能因变更不在 docs/ 而跳过。
  const p = planGates({ fast: true, incremental: true, changedFiles: ['services/zhongshu-core/zszj-server/Dockerfile'] });
  assert.equal(p.mode, 'incremental');
  assert.ok(ids(p.gates).includes('G5'), 'docs/ 之外的被链接目标变更未选中 G5（悬空链接会漏检）');
});

test('P2#2 planGates 增量 + 跨目录 rename 源路径上报（--no-renames 效果）：源区域门禁被选中', () => {
  // detectChangedFiles 用 --no-renames，使 rename 上报为 源(删)+目标(增) 两条路径。
  // 非 fast：G10（Web 类型检查，area=apps/zhongshu-admin-web/）为候选；源路径命中 G10 area。
  // 若只报目标(miniapp)侧，把 .ts 移出 Web 会破坏 App.vue 等导入却漏选 G10。
  const p = planGates({ incremental: true, changedFiles: [
    'apps/zhongshu-admin-web/src/utils/is.ts', // 源（被删）
    'apps/zhongshu-miniapp/src/utils/is.ts',   // 目标（新增）
  ] });
  assert.equal(p.mode, 'incremental');
  assert.ok(ids(p.gates).includes('G10'), 'rename 源路径所属的 Web 区域门禁 G10 未选中');
});
