# ZS-GOV-001（提效方案 P0）codex 评审结论与处置

> 被审提交：`8c9b6082`（ZS-GOV-001 任务仪式自动化 P0——task-stats 实时聚合 + verify-docs R6/R7 + close-task 一键收口；纯治理工具、无任务状态变更，对应 docs/05 §19 V1.15）。
> 评审工具：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`（`codex review --commit 8c9b6082`）。
> 完整 stdout：[codex-ZS-GOV-001-P0.raw.md](codex-ZS-GOV-001-P0.raw.md)。
> 命名说明：文件名中的「P0」是 **ZS-GOV-001 任务的优先级标签**（提效方案 P0），非缺陷严重度；本轮 codex 发现均为 **P2**。
> 说明：本轮评审当时完成但未收口，处置文档于 2026-09-10 补写入库，遵循 [README.md](README.md)「后续处理约定」。
> **修复更新（2026-09-10）**：本文所列 2 × P2 已由提交 `ea739b9c` 合并修复并补回归测试（与 [codex-ZS-OPS-001-step4.md](codex-ZS-OPS-001-step4.md) 的 3 × P2 同批闭合，共 5 × P2）；下方「复核与处置」「结论」保留评审当时的原始判定，逐条修复证据见各 P2 的「✅ 已由 `ea739b9c` 修复」段。

## Codex 原始结论（被审提交 `8c9b6082`）

codex 判定发现 **2 项 P2**（无 P0 / P1 / P3），结论摘要：

> All 11 existing tests pass, but valid status transitions can generate incomplete statistics, and the new consistency gates accept omitted nonzero categories. These defects undermine the synchronization guarantees introduced by this commit.

### Review comments

- **[P2] Compare every status when validating summary distributions** — `scripts/gov/verify-docs.mjs:140-142`
  > When a nonzero category is omitted, this loop checks only the remaining declared keys. Removing `、31 待验收` from the committed README therefore passes validation despite its distribution accounting for only 60 of 91 tasks. R6 has the same omission problem for §2. Compare all status-enum entries, treating omitted categories as zero, while preserving the intentional skip for entirely absent declarations.

- **[P2] Add newly populated states to the section 2 summary** — `scripts/gov/close-task.mjs:69-71`
  > Running `node scripts/gov/close-task.mjs ZS-SEC-006 暂缓` against the committed documents changes the card and README correctly, but §2 only changes 待开发 from 43 to 42: it never inserts `1 项暂缓` because that category is absent from the original sentence. Consequently, the generated §2 distribution accounts for only 90 tasks. Rebuild the distribution or insert missing nonzero categories rather than only replacing existing numbers.

## 复核与处置

两项均**独立复核确认成立（非误报）**，属治理工具链自身的健壮性盲区；评审当时判 **P2 非阻塞、列入待办分批处置**（依 README「后续处理约定」第 2 条「P2/P3 可分批处置」）。**该 2 项已于提交 `ea739b9c` 合并修复闭合**（与步骤④评审的 3 × P2 同批，共 5 × P2 + 回归测试），逐条修复证据见下。

### P2-1 — verify-docs R6/R7 只比对已声明的状态键（✅ 确认成立）

**复核结论：成立，且代码违背其自身文档契约。** [verify-docs.mjs](../../scripts/gov/verify-docs.mjs) 的 R7（L140-142）与 R6（L124）均以 `for (const s of Object.keys(rm.declared))` / `Object.keys(declared)` 遍历——**只比对文档已声明的状态键**。而 L12 的 R7 规则注释明写「**省略的类别按 0 计**」：若 README 摘要删去某个非零类目（如 `、31 待验收`），按契约应判 `声明 0 ≠ 实际 31` 而 FAIL，但实现因该键根本不在 `rm.declared` 中而**从不进入比对** → 放行；此时 L137 的总数校验（`rm.total !== actual05.cardCount`）仍等于 91、不受影响，于是「分布只覆盖 60/91」的自相矛盾摘要能通过全部门禁。**实现与文档契约不一致，确认成立。**

**处置：✅ 已由 `ea739b9c` 修复。** 触发前提是**人工**编辑 §2/README 摘要时漏写某个非零类目——工具自身（close-task）不会主动制造该形态，且总数校验仍能兜住「总数错」，仅漏「分布内部缺项」，实际发生频率低、影响面限于文档统计口径。**修复实现**：[verify-docs.mjs](../../scripts/gov/verify-docs.mjs) 的 R6/R7 比对循环由 `for (const s of Object.keys(declared))` / `Object.keys(rm.declared)` 改为 `for (const s of STATUS_ENUM) { const d = declared[s] ?? 0; … }`——遍历完整状态枚举、省略键取 `?? 0` 参与比对，落实文件头「省略的类别按 0 计」契约；`if (declared)` / `if (rm)` 的「无声明句整体跳过」豁免保留不变（不误伤构造文本）。**回归测试**：`verify-docs.test.mjs` 新增 2 例——「R6：§2 省略实际非零的类别（漏计）会失败」「R7：README 省略实际非零的类别（漏计）会失败」。

### P2-2 — close-task §2 回填只替换已存在状态、不插入新出现的非零状态（✅ 确认成立）

**复核结论：成立，且与同文件 README 回填逻辑自相矛盾。** [close-task.mjs](../../scripts/gov/close-task.mjs) 的 §2 回填（L69-71）用 `dist.replace(/(\d+)(\s*项\s*(待开发|…|暂缓))/g, …)` **只替换句中已存在的 `N 项 <状态>` token**；当某状态由 0 转为非零（如任务转「暂缓」而原句无该类目）时，无对应 token 可替换 → §2 永不插入该类目、分布少计。对照同文件 README 回填（L74-78）用 `ORDER.filter((s) => actual.counts[s] > 0).map(...)` **整段重建**（正确）——**两处逻辑不一致**，正是 codex 所指。更隐蔽的是 L72 的变更记录提示用 `ORDER.filter(>0)` 打印的是**完整正确分布**，而实际写入 §2 的文本却是 replace-only 的**残缺分布**，即"变更日志声称的分布"与"§2 实际文本"可能不符。

**处置：✅ 已由 `ea739b9c` 修复。** 仅在向一个**当前为空（0 项）的状态**转入任务时才显现（如首次出现「暂缓」/「已验收」）；日常在既有非零状态间流转（待开发→开发中→待验收）不受影响，故至今未触发实际问题。**修复实现**：把 §2 回填逻辑抽为纯函数 `backfillSection2(text, counts, order)` 移入 [task-stats.mjs](../../scripts/gov/task-stats.mjs)（脱离 close-task 顶层 I/O 以便单测），在「替换句中已有类别数字」之外**补插句中缺失但实际非零的类别**（`for (const s of order) if ((counts[s] ?? 0) > 0 && 句中未含) d += 、N 项<s>`）；[close-task.mjs](../../scripts/gov/close-task.mjs) 步骤 3 改为调用 `backfillSection2(doc05, actual.counts, ORDER)`，与步骤 4 README 回填的 `ORDER.filter(>0)` 整段重建语义对齐、消除两处矛盾。**回归测试**：`verify-docs.test.mjs` 新增 4 例——改数字保留其后说明、补插缺失非零类别（含合计 = 91 断言）、幂等、无 §2 声明句时原样返回。

## 结论

**评审完成，2 项 P2 全部确认成立、判非阻塞；已于提交 `ea739b9c` 全部修复闭合（与步骤④评审的 3 × P2 同批，共 5 × P2 + 回归测试）。**

- 被审 `8c9b6082`（ZS-GOV-001 提效方案 P0，纯工具、无任务状态变更）：codex 发现 2 × P2——verify-docs R6/R7 只比对已声明键（违背 L12「省略按 0 计」契约）、close-task §2 回填 replace-only（与 README 整段重建逻辑矛盾）。
- 两项均为**治理工具自身的健壮性盲区**：触发前提分别是「人工漏写非零类目」「向空状态转入任务」，日常路径不触发，实际风险低，符合 P2 定级。
- codex 发现总数 **2**（2 × P2），确认成立 **2**，评审当时列入待办 **2**；**现已由 `ea739b9c` 全部修复（本轮修复 2、遗留待办 0）**。
- **建议落实**：本项与 [codex-ZS-OPS-001-step4.md](codex-ZS-OPS-001-step4.md) 的 3 × P2 同属「治理/门禁工具链健壮性」缺口，评审建议合并为一个专项修复批次（5 × P2 + 回归测试）一次性闭合，因其保护的正是所有后续任务依赖的文档一致性门禁与本地基线门禁——**该建议已由提交 `ea739b9c` 落实**：5 × P2 同批修复，`verify-docs.test.mjs` +6、`run-local-gates.test.mjs` +3（另 4 存量断言随 G5 入安全守卫集更新），`node --test` 17/17 + 17/17、`run-local-gates --fast` 10/10、`verify-docs` 全量 0 issue 全绿。
