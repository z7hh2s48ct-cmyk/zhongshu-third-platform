# ZS-SEC-007 codex 评审处置（HANDOFF 补评，1×P1+1×P2 → hotfix 修复）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 332b4d7c` @ main）
- 评审对象：ZS-SEC-007 统一访问、异常与保护切面的日志脱敏（HANDOFF-ZS-SEC-007 交接单的待评审提交；2026-09-14 补评入库）
- 结论：**r0 发现 1×P1 + 1×P2 → hotfix `feat/sec-007-fix` 修复（见 codex-hotfix-SEC-007-P1.md）**
- 原始日志：`outputs/handoff/ZS-SEC-007.txt`（未入库；本档案为处置入库）

## r0 发现（2 项，均指向 `LogSanitizeUtils`）

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | **Materialize raw JSON values before redacting the response tree**（L126-129）：DTO 含 `@JsonRawValue` 字段（现网 `AppDiyPagePropertyRespVO.property` 已在用）时，`valueToTree` 将原文保留为 `POJONode`，`sanitizeNode` 跳过该节点类型——内嵌原文含 `{"password":"SECRET"}` 可原样进入日志。旧版「先序列化再解析树」实现本可遍历到 | hotfix 修复：POJONode 物化为普通 JSON 节点后再脱敏（覆盖全部 `valueToTree` 入口）+ `@JsonRawValue` 回归测试 |
| P2 | **Make sensitive-key normalization locale-independent**（L220）：`toLowerCase()` 未带 `Locale.ROOT`，土耳其/阿塞拜疆等默认 locale 的 JVM 上 ASCII `I` 变 ı，`AUTHORIZATION`/`PRIVATEKEY`/`X-API-KEY` 不再命中敏感根集、凭据明文落日志 | hotfix 一并修复：`toLowerCase(Locale.ROOT)` + 土耳其 locale 回归测试 |

codex 总述："The sanitizer can emit credentials from embedded raw JSON and from uppercase sensitive keys under certain JVM locales. Both cases bypass the intended redaction."

## 边界说明

- P1 的实际暴露面为 `responseBody` 日志（当前默认 `responseEnable=false` 不记响应体），且交付时点尚未有 DIU 页面数据；但脱敏器合同是「脱敏失败只记摘要、绝不回退原文」，POJONode 旁路违反合同本体，按 P1 修复不降级。
- 交付时点证据沿用卡片记录：`LogSanitizeUtilsTest`（common 10）+ 5 写点组件测试（Idempotent/RateLimiter/ApiSignature/ApiAccessLogFilter/GlobalExceptionHandler）全绿。
