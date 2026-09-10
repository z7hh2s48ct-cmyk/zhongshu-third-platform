# ZS-OPS-001.A（提效方案 P1 步骤④）codex 评审结论与处置

> 被审提交：`c9d178b2`（ZS-OPS-001.A 聚合门禁入口提速 P1 步骤④——run-local-gates 并发 + 增量；纯工具、无任务状态变更，对应 docs/05 §19 V1.16）。
> 评审工具：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`（`codex review --commit c9d178b2`）。
> 完整 stdout：[codex-ZS-OPS-001-step4.raw.md](codex-ZS-OPS-001-step4.raw.md)。
> 命名说明：文件名中的「step4」是 **ZS-OPS-001.A 提速方案的步骤④**（并发 + 增量），非缺陷严重度；本轮 codex 发现均为 **P2**。
> 说明：本轮评审当时完成但未收口，处置文档于 2026-09-10 补写入库，遵循 [README.md](README.md)「后续处理约定」。

## Codex 原始结论（被审提交 `c9d178b2`）

codex 判定发现 **3 项 P2**（无 P0 / P1 / P3），结论摘要：

> Incremental mode can omit demonstrably affected gates for seed edits, cross-directory renames, and linked-file deletions. The 14 planner tests pass, but they do not cover these cases.

### Review comments

- **[P2] Include G2b's repository data in its dependency mapping** — `scripts/ops/run-local-gates.mjs:31`
  > With `--incremental`, SQL seed edits skip G2b because its only area is `scripts/brand/`. However, `apply-naming-migration.test.mjs` reads the actual SQL seeds and checks cross-tenant website uniqueness. Changing `zsxq.zszj.example.com` to `www.zszj.example.com` in the PostgreSQL seed creates a duplicate that G2b detects, but the selected G3/G6/G7/G8/G9 gates do not. Include the repository data consumed by this test in its areas, or keep G2b always enabled.

- **[P2] Preserve both source and destination paths for renames** — `scripts/ops/run-local-gates.mjs:84`
  > For a staged cross-directory rename, `git diff --name-only HEAD` reports only the destination when rename detection succeeds. For example, moving `apps/zhongshu-admin-web/src/utils/is.ts` into the miniapp selects only G3/G9, even without `--fast`, skipping G10 despite breaking existing Web imports such as those in `App.vue`. Collect both sides of renames, using `--no-renames` or parsing rename-aware status output, before selecting gates.

- **[P2] Run document validation when linked targets disappear** — `scripts/ops/run-local-gates.mjs:34`
  > Deleting a linked file outside `docs/` skips G5 even though its link-existence checks are affected. For example, deleting `services/zhongshu-core/zszj-server/Dockerfile` selects G3/G6/G7/G8/G9, while `verify-docs.mjs` would report broken links in docs/05. Trigger G5 for deleted or renamed link targets, or keep it unconditional; changes to Markdown content are not its only dependency.

## 复核与处置

三项均**独立复核确认成立（非误报）**，同属 `--incremental` 增量选门的**依赖映射盲区**：某变更的真实影响面超出其路径前缀所映射的门禁集合。均判 **P2 非阻塞、列入待办分批处置**，本轮不改代码。

**关键降级依据（三项共通）**：`--incremental` 在设计上**仅供日常快速反馈、非放行权威**——[run-local-gates.mjs](../../scripts/ops/run-local-gates.mjs) 门禁文档串 L16 与 docs/05 卡片开发记录均明写「增量仅供日常快速反馈，**批次收口与 CI 必须跑全量**（`--fast` 或含 G10/G11），不得以增量结果代替放行」。故三处盲区只在「开发者用 `--incremental` 做本地快查、且恰好命中这三类变更、且跳过后续全量收口」时才可能漏检；批次收口与 CI 的全量运行始终覆盖。这是其定级 P2（而非 P1）的实质理由。同时 L73-74 的 fail-safe（未归类路径回退全量）已兜住大部分情形，本三项恰是「路径能被现有 area 前缀归类、故不触发 fail-safe，但真实影响面外溢到未选中门禁」的边角。

### P2-1 — G2b 的 areas 未纳入其测试实际消费的 SQL 种子数据（✅ 确认成立）

**复核结论：成立。** G2b（L31）`areas: ['scripts/brand/']`，但其测试 `apply-naming-migration.test.mjs` 读取 `services/…/sql/` 下的真实种子校验跨租户域名唯一。编辑该种子时路径命中 G3 的 `services/` 前缀（L73 判为已归类、不触发 fail-safe 全量），选中 G3/G6/G7/G8/G9，却**唯独不选 G2b**（其 area 仅 `scripts/brand/`）→ 种子制造的跨租户同域重复逃过增量门禁。**修复方向**：把 G2b 消费的种子数据路径（`services/zhongshu-core/sql/`）加入其 `areas`，或将 G2b 标 `safety: true` 恒定跑（其单测很快，恒跑代价低）。

### P2-2 — 增量选门对 rename 只取目标路径、丢失源路径（✅ 确认成立）

**复核结论：成立。** `detectChangedFiles`（L84）用 `git diff --name-only HEAD`，默认开启 rename 检测；跨目录移动在 git 判定为 rename 时**只报目标路径**，源路径丢失。例：把 `apps/zhongshu-admin-web/src/utils/is.ts` 移入小程序 → 只报小程序目标路径（命中 G3/G9 的 `apps/`），**源目录 `apps/zhongshu-admin-web/` 未报** → 覆盖它的 G10（Web 类型基线）被跳过，尽管删除 is.ts 会破坏 `App.vue` 等既有 Web import。**修复方向**：`git diff` 加 `--no-renames`（把 rename 拆成 delete+add 两条路径），或解析 rename-aware 的 `git status`/`--name-status` 输出同时收集源与目标两侧路径，再据以选门。

### P2-3 — 删除 docs/ 外的被链目标不触发 G5 文档校验（✅ 确认成立）

**复核结论：成立。** G5（L34）`areas: ['scripts/gov/', 'docs/', 'README.md']`，但 `verify-docs.mjs` 的 R1 链接有效性校验会解析 docs 内链接**指向全仓任意文件**的存在性。删除 docs/ 外的被链目标（如 `services/zhongshu-core/zszj-server/Dockerfile`）时路径命中 `services/` 前缀（不触发 fail-safe），选中 G3/G6/G7/G8/G9，却**不选 G5** → docs/05 因此产生的断链逃过增量门禁。**修复方向**：对任何**被删除或被 rename 的已跟踪路径**触发 G5（G5 的依赖不止 Markdown 内容变更，还包括全仓被链目标的存在性），或将 G5 与 G3/G9 一并列为 `safety: true` 恒定跑（G5 全量很快）。

## 结论

**评审完成，3 项 P2 全部确认成立、判非阻塞、列入门禁工具链改进待办（本轮不改代码）。**

- 被审 `c9d178b2`（ZS-OPS-001.A 提效方案 P1 步骤④，纯工具、无任务状态变更）：codex 发现 3 × P2——`--incremental` 增量选门在「SQL 种子编辑（G2b 未纳入数据依赖）」「跨目录 rename（只取目标路径漏 G10）」「删除 docs/ 外被链目标（漏 G5）」三类变更下会漏选本应受影响的门禁。
- 三项均为**增量快查路径的依赖映射盲区**；因增量**非放行权威**（批次收口与 CI 恒跑全量）、且 fail-safe 已兜住大部分情形，实际漏检需多重条件叠加，风险可控，符合 P2 定级。
- codex 发现总数 **3**（3 × P2），确认成立 **3**，本轮修复 **0**，列入待办 **3**（非阻塞）。
- **建议**：本项与 [codex-ZS-GOV-001-P0.md](codex-ZS-GOV-001-P0.md) 的 2 × P2 同属「治理/门禁工具链健壮性」缺口，宜合并为一个专项修复批次（5 × P2 + 回归测试）一次性闭合；G2b/G5 恒跑（`safety: true`）与 rename `--no-renames` 三处修复代价小、收益直接，可优先。
