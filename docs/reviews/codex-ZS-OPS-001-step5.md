# codex 评审处置：ZS-OPS-001.A 提效方案 步骤⑤（git worktree 并行编排器）

> 被审提交：`2fa36a9e`（编排器初版）→ `5e7c4e1e`（r0 1×P1+1×P2 修复）→ `f6b019b0`（Phase2 隔离校验修复）；纯工具、无任务状态变更，对应 docs/05 §19 V1.21（评审入库随 V1.22 补收口）。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `none`，sandbox `read-only`（`codex review --commit <SHA>`）。
> 完整 stdout：r0 [codex-ZS-OPS-001-step5.raw.md](codex-ZS-OPS-001-step5.raw.md)、r1 [codex-ZS-OPS-001-step5-r1.raw.md](codex-ZS-OPS-001-step5-r1.raw.md)、r2 [codex-ZS-OPS-001-step5-r2.raw.md](codex-ZS-OPS-001-step5-r2.raw.md)。
> 命名说明：文件名中的「step5」是 **ZS-OPS-001.A 提效方案的步骤⑤**（git worktree 并行编排），非缺陷严重度；r0 发现为 **1×P1 + 1×P2**。
> 说明：r0/r1 于编排器落地时评审；r2（`f6b019b0`）于 2026-09-11 重跑捕获干净 raw，判定与首轮一致。处置文档同批入库，遵循 [README.md](README.md)「后续处理约定」。步骤④（并发 + 增量）的工具弧见 [codex-ZS-OPS-001-step4.md](codex-ZS-OPS-001-step4.md)。

## Codex 原始结论

工具弧跨三个提交、三轮 codex 评审：r0 审初版发现 1×P1+1×P2 → `5e7c4e1e` 修复 → r1 复评 PASS/0；供给演示自查另暴露 Phase2 隔离校验假阴性 → `f6b019b0` 修复 → r2 复评 PASS/0。

### r0 — 被审 `2fa36a9e`（编排器初版）：1×P1 + 1×P2

> The merge command ignores its destination branch, and frontend provisioning targets the wrong directory. All 16 static tests pass, but they do not catch these execution-path defects.

**Review comments**

- **[P1] Enforce the requested destination branch before merging** — `scripts/ops/worktree-orchestrate.mjs:106-107`
  > When `merge ... --into release` is invoked while the main worktree has `main` checked out, this command merges into `main`, not `release`: `into` only affects metadata and printed messages. The default also silently merges into any other currently checked-out branch. Verify that the main worktree is on `into` and abort on mismatch, or explicitly switch branches safely before merging.

- **[P2] Provision dependencies at the frontend application's path** — `scripts/ops/worktree-orchestrate.mjs:51-52`
  > For `create ... --node`, provisioning targets `<mainRoot>/node_modules`, but this repository's frontend dependencies live under `apps/zhongshu-admin-web/node_modules`. Consequently, provisioning fails in the existing layout; even if root-level dependencies exist, G10 still cannot use them because `scripts/client/verify-ts-baseline.mjs` explicitly executes `./node_modules/vue-tsc/bin/vue-tsc.js` with the application directory as its working directory. Provision the application-level dependency directory instead.

codex 自主执行：`git show --stat`/全量 diff、读取 [worktree-orchestrate.mjs](../../scripts/ops/worktree-orchestrate.mjs) 全文、`node --test`（16 pass / 0 fail），判定 16 个静态规划测试全绿但**未覆盖这两条执行路径缺陷**——planner 只断言「生成的步骤序列」，未跑真实 `git merge` / junction 副作用，故 `--into` 被忽略、供给指向错目录都能通过静态测试。

### r1 — 被审 `5e7c4e1e`（r0 P1+P2 修复）：PASS / 0 发现

> No actionable regressions were identified. The branch assertion stops merges when the checked-out branch differs from the requested target, and dependency provisioning now matches the frontend gate’s application-level path. All 16 orchestrator tests pass.

### r2 — 被审 `f6b019b0`（Phase2 隔离校验修复）：PASS / 0 发现

> No actionable regressions were identified in the commit. All 17 unit tests passed, and the list command correctly displayed Chinese worktree paths and provisioning status.

codex 亲跑 `node --test`（17 pass / 0 fail）+ `worktree-orchestrate.mjs list`，确认中文 worktree 路径（`众墅之家AI赋能平台底座/.wt/...`）与供给状态显示正确。

## 复核与处置

三项均**独立复核确认成立**，且**均已修复闭合**：P1/P2 由 codex r0 发现、`5e7c4e1e` 修复、r1 复评确认；Phase2 由供给演示自查发现、`f6b019b0` 修复、r2 复评确认。

### P1 — merge 未强制目标分支，实际并入当前 checkout 分支（✅ 确认成立，已修 `5e7c4e1e`）

**复核结论：成立。** 初版 `planMerge` 的 `--into` 仅用于生成合并提交信息与打印提示，`git merge` 实际并入的是**主树当前 checkout 的分支**——若主树停在 `main` 而调用 `merge --into release`，会误并入 `main`；默认调用亦会静默并入任意当前分支。**修复方向 → ✅ `5e7c4e1e` 落实（选「断言并中止」而非「擅自切分支」）**：`planMerge` 首步插入 `git-assert-branch`（expected=`into`），`execSteps` 校验主树当前分支 ≠ `into` 即 `return false` 中止后续 `git merge`——绝不自动切分支（切分支属破坏性副作用，交调用方显式决定）；`--ff-only`/`--into` 驱动断言目标。**回归测试**：`planMerge：默认先断言主树分支==into，再 --no-ff 合并`、`P1 fix：planMerge --ff-only 切换合并策略；--into 驱动分支断言目标（防合错分支）`。

### P2 — 前端依赖供给指向仓库根，与 G10 的 app 级 cwd 失配（✅ 确认成立，已修 `5e7c4e1e`）

**复核结论：成立。** 初版 `create --node` 供给 `<mainRoot>/node_modules`，但本仓前端依赖实际位于 `apps/zhongshu-admin-web/node_modules`（仓库根无 `node_modules`）；且 G10（`scripts/client/verify-ts-baseline.mjs`）以 **app 目录为 cwd** 执行 `./node_modules/vue-tsc/bin/vue-tsc.js`，即便根级依赖存在 G10 也用不到 → 供给在现有布局下失败。**修复方向 → ✅ `5e7c4e1e` 落实（改供给 app 级）**：`provisionTargets`/`planCreate --node` 的 junction link/target 改指向 `apps/zhongshu-admin-web/node_modules`。**回归测试**：`P2 fix：provisionTargets --node 供给 app 级 node_modules（仓库根无 node_modules；G10 以 app 为 cwd 跑 vue-tsc）`、`P2 fix：planCreate --node 供给 tools + app 级 node_modules 两个 junction（link/target 指向 app 目录）`。

### Phase2（供给演示自查发现，非 codex r0 项）— isolationReport/list 对中文·大小写路径假阴性（✅ 已修 `f6b019b0`，r2 PASS/0）

**背景：成立（自查）。** 供给演示（建 3 试点 worktree）时暴露——`isolationReport` 比对 worktree 路径时，`--wt-root` 传入小写盘符 `e:` 而 git 规范输出大写 `E:/`，且中文路径经非 `-z` 的 `git worktree list` 输出被转义/引号包裹并换行截断 → 隔离校验**假阴性 FAIL**（实际隔离完好却报「需排查」）。**修复方向 → ✅ `f6b019b0` 落实**：`parseWorktrees` 改用 `git worktree list --porcelain -z` 逐字（NUL 分隔）UTF-8 解析，杜绝转义/换行截断；新增 `normPath`（统一正斜杠、win32 下大小写不敏感）归一后再比对；`.wt/` 入 `.gitignore`（worktree 目录永不入库）。**回归测试**：`normPath：统一正斜杠；win32 下大小写不敏感（修复 isolationReport 假阴性：--wt-root 传小写 e: 而 git 规范输出大写 E:/）`（用例 16→17）。

## 结论

**工具弧三轮评审闭合：r0（`2fa36a9e`）发现 1×P1 + 1×P2 → `5e7c4e1e` 修复 → r1 PASS/0；供给演示自查暴露 Phase2 隔离校验假阴性 → `f6b019b0` 修复 → r2 PASS/0。**

- 编排器 [worktree-orchestrate.mjs](../../scripts/ops/worktree-orchestrate.mjs)：`plan/create/list/gates/merge/cleanup` 六子命令，**纯函数（plan*）与副作用层（exec*）分离**，`merge` 首步 `git-assert-branch` 防合错分支、`--no-ff` 保留任务边界、**绝不自动改 docs**（文档同步交主树串行完成、merge 后打印 docSyncReminder 清单）；`node --test` 16→17 用例全绿。
- 发现总数 **2**（1×P1 + 1×P2，均 codex r0）+ 供给演示自查 Phase2 **1**；确认成立 **3**，已修复 **3**，遗留待办 **0**。
- 供给实证：3 试点 worktree（`feat/cfg-001-b`/`cfg-002-b`/`sec-010`）`isolationReport` 均 PASS、`list` 中文路径干净显示、`cfg-002-b` 树内 `gates --fast` 10/10（run-local-gates 各子命令 `{cwd:root}` 由 `import.meta.url` 推导，跑各树副本即自动锁定该树，已证）。
- 试点首个交付子项 ZS-CFG-002.B 的评审另见 [codex-ZS-CFG-002.B.md](codex-ZS-CFG-002.B.md)。
