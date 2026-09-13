# ZS-SEC-007 hotfix codex 评审处置（r0→r1→r2 三弧，首个「修复自身引入新 P1 被复评揪出」的主会话 hotfix）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ worktree `E:/zszj-wt-sec-007`）
- 评审对象：分支 `feat/sec-007`（HANDOFF 补评 ZS-SEC-007 所发现 1×P1+1×P2 的修复）
- 结论：**r2 CLEAN / 0 发现**（评审收敛）
- 原始日志：worktree `outputs/sec007-codex-r1.txt`/`r2.txt`（未入库；本档案为处置入库）

## 修复弧总览

| 弧 | 提交 | 结论 | 说明 |
|---|---|---|---|
| 补评 r0 | `332b4d7c`（被审对象） | FAIL 1×P1+1×P2 | 见 [codex-ZS-SEC-007.md](codex-ZS-SEC-007.md) |
| hotfix r1 | `9728b369`（修复提交） | FAIL 1×P1（新引入） | 见下 |
| hotfix r2 | `39c8b8dd`（fail-closed 修正） | **CLEAN / 0 发现** | 「Materialization failures now reach the existing safe-summary handlers instead of returning potentially sensitive raw text.」 |

## r0 发现 → 修复（`9728b369`，2 files +125/-4）

| 级别 | 发现 | 修复 |
|---|---|---|
| P1 | `sanitizeNode` 跳过 POJONode——`@JsonRawValue` 字段经 `valueToTree` 保留为 POJONode，内嵌原文穿透响应日志脱敏 | 新增 `toSanitizableTree`/`materializeRaw`：脱敏前统一物化（对象/数组递归复制；POJONode 解包），三处 `valueToTree` 入口（sanitizeObject/sanitizeArgValue/sanitizeResponseBody）全接。**实现中发现 POJONode 内包的是 Jackson `RawValue` 包装器而非裸 String**（RED 调试实证输出 `[RawValue of type java.lang.String]`），须经 `rawValue()` 解包后再 `readTree` |
| P2 | `normalizeKey` 的 `toLowerCase()` 缺 `Locale.ROOT`，土耳其 locale 下大写敏感键（AUTHORIZATION/PRIVATEKEY/X-API-KEY）不命中 | `toLowerCase(Locale.ROOT)` |

## r1 新发现（修复自身引入）→ 修复（`39c8b8dd`，1 file +3/-7）

| 级别 | 发现 | 修复 |
|---|---|---|
| P1 | **物化失败 fallback 携带原文**：初版 `catch → TextNode.valueOf(String.valueOf(effective))` 把对象 `toString()`（可含 `password=...` 形态凭据）转成 TextNode，而 `sanitizeNode` 跳过 TextNode——该路径此前走「序列化失败 → 安全摘要」，修复反而引入可复现泄露（sanitizeMap/sanitizeArgs/sanitizeResponseBody 三入口可复现） | **fail-closed**：物化失败一律上抛 `IllegalStateException`，交既有外层 catch 降级为安全摘要/占位（恢复修复前的失败语义） |

## 验证

- TDD：3 回归测试先行（responseBody raw 字段掩码 / sanitizeMap raw 字段掩码 / 土耳其 locale 大写敏感键），RED 3 失败实证两项发现 → GREEN
- `zszj-common` 全量：**13 tests / 0 failures，BUILD SUCCESS**（10 既有 + 3 新增）
- 合并：`--no-ff` 合并回 main（`a1e3a505`）

## 经验登记

1. **Jackson `@JsonRawValue` × `valueToTree` 的真实形态**：POJONode 内包 `RawValue` 包装器（非裸 String），须经 `rawValue()` 解包——凭 RED 调试输出 `[RawValue of type java.lang.String]` 实证，纯推理会漏。
2. **脱敏器的失败路径也是安全面**：任何「无法处理 → 保留原文」的 fallback 都违反 fail-closed 合同；正确语义是上抛交安全摘要。
3. 与 ZS-SEC-011.A r0→r1「mock 假 GREEN」、ZS-LOGIN-001 r1「缓存态门控」同类：**安全修复本身须经复评直至归零**，本例为主会话直接 hotfix 流程首次触发 r1 揪出新 P1。
