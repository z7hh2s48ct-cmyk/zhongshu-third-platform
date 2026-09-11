# 治理/门禁工具链 5×P2 修复提交（ea739b9c）codex 评审结论与处置

> 被审提交（r0）：`ea739b9c`（ZS-GOV-001 / ZS-OPS-001.A：修复 codex 双评审 5 条 P2——增量门禁依赖映射 + 统计校验全枚举 + §2 补插；6 files, +156/-27）。
> P2 修复提交（响应 r0）：`19b736f4`（backfillSection2 §2 分布串限界到句/行；2 files, +32/-5）。
> 复评：r1 `codex review --commit 19b736f4`（PASS / 0 发现）。
> 评审工具：`codex-cli 0.154.0`（OpenAI Codex v0.154.0），模型 `gpt-6-astra`，reasoning effort `xhigh`。
> 完整 stdout：r0 [codex-fix-5p2-ea739b9c.raw.md](codex-fix-5p2-ea739b9c.raw.md)、r1 [codex-fix-5p2-ea739b9c-r1.raw.md](codex-fix-5p2-ea739b9c-r1.raw.md)。
> 背景：本评审审的是「治理/门禁工具链自身」的修复提交 `ea739b9c`——它闭合了 [codex-ZS-GOV-001-P0.md](codex-ZS-GOV-001-P0.md)（2×P2）与 [codex-ZS-OPS-001-step4.md](codex-ZS-OPS-001-step4.md)（3×P2）两份评审的发现；本文件是对 `ea739b9c` 的复核评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。

## Codex 原始结论（r0，被审提交 `ea739b9c`）

判定 **PASS**（无 P0 / P1），发现 **1 项 P2**：5 项 P2 修复本身正确，但 `backfillSection2` 的分布串匹配存在边界缺陷。codex 沙箱亲跑 `node --test scripts/gov/*.test.mjs` **34 pass / 0 fail**，并现场复现该 P2——对「无尾随括注的 §2 声明句 + 后续 task cards」调用 `backfillSection2`（各状态计数设为 1）：

```
统计（2026-09-10）：1 项待开发。

## 3. Tasks
### ZS-ENG-001：A
- 关联：WP；状态 待开发；前置 无。
、1 项待决策、1 项待前置、1 项开发中、1 项待验收、1 项已验收、1 项暂缓   ← 补插类别被追加到卡片之后（§2 之外）
```

### Review comments

- **[P2] Bound missing-status insertion to the statistics sentence** — `scripts/gov/task-stats.mjs:82-85`
  > When the §2 statistics sentence has no trailing parenthetical explanation, `[^（(]*` consumes subsequent sections until another opening parenthesis or EOF. Appending missing statuses to `d` therefore writes them into unrelated document content instead of §2. For example, a declaration ending in `1 项待开发。\n` followed by task cards receives the new categories after those cards. Bound the distribution to its sentence or line and insert missing categories before its trailing punctuation.

> The missing-status backfill can insert statistics outside §2 when the declaration lacks a parenthetical explanation. All 34 existing tests pass, but they do not cover this insertion case.

## 复核与处置

发现经本机独立复现**成立**（非误报）：`backfillSection2` 的分布串正则 `([^（(]*)`（原 `task-stats.mjs:82`）从「：」后贪婪匹配到下一个 `（/(` 或 EOF；当 §2 声明句**无尾随括注**时，会越过 `。\n` 吞掉后续 task cards，`d += 、N 项<状态>`（原 `:85`）遂把缺失类别写进 §2 之外的卡片内容。本机以同场景临时脚本复现，旧正则下输出末卡之后被追加 `、1 项暂缓`（`endsWith(卡片) = false`），与 codex 复现一致。

现有 34 条测试的 §2 声明句均带 `（说明）` 尾注（分布串止于首个 `（`），故未覆盖此无括注插入场景——codex 指出的测试盲区属实。

修复（`19b736f4`）：

- 抽出共用跨度常量 `DIST_SPAN = '[^（(\n。]*'`，止于首个左括注 `（/(`、句号 `。` 或换行——**限界到句/行**；同时用于 `parseSection2Declared`（读）与 `backfillSection2`（写），杜绝读写分歧。
- 缺失类别因此插在分布串末尾（尾随标点之前），落在 §2 内；对真实 §2 句（有 `（说明）` 尾注、分布内无 `。`）行为零变化。
- `verify-docs.test.mjs` 新增回归「无尾注时缺失类别插在句号前、不越界污染后续卡片」，精确覆盖 codex 指出的盲区（已验证：旧正则下该测试失败——暂缓被追到末卡之后 `endsWith = false`；新正则下通过）。

## 复评（r1，被审提交 `19b736f4`）

`codex review --commit 19b736f4`（`gpt-6-astra`/`xhigh`）判定 **PASS：0 发现**（无 P0 / P1 / P2 / P3）。codex 沙箱亲跑 `node --test scripts/gov/*.test.mjs` **18 pass / 0 fail**（含新增回归），并确认共用边界同时约束读写两端：

> The shared regex boundary consistently limits parsing and backfilling to the declaration's sentence or line, preventing additions from spilling into subsequent task cards. No actionable regressions were found, and all 18 governance tests pass.

即 codex 确认：共用正则边界一致地把**解析与回填**都限定在声明句/行内，防止补插溢出到后续 task cards，无可行回归。**r0 的 1 项 P2 已消除。**

## 结论

**评审通过（r0 PASS 0×P1 + 1×P2 → 修复 → r1 PASS 0 发现）。**

- **r0**（`ea739b9c`）：PASS，0×P1 + 1×P2——5 项治理 P2 修复正确，唯 `backfillSection2` 分布串正则在「§2 无尾随括注」时越界，把补插类别写进后续卡片（34 测试未覆盖此插入场景）。
- **修复**（`19b736f4`）：抽 `DIST_SPAN` 共用常量限界到句/行（parse + backfill 同源）+ 1 条无括注插入回归测试。
- **r1**（`19b736f4`）：PASS，0 发现——codex 亲跑 18 测试全绿、确认共用边界同时约束读写、P2 消除。
- codex 发现总数 **1**（r0：1×P2）；已修复/闭合 **1**；仍有效 **0**。
