# ZS-SEC-005 codex 评审处置（HANDOFF 补评，r0 一次通过）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit e2b40185` @ main）
- 评审对象：ZS-SEC-005 统一错误响应、HTTP 状态与失败日志结果（HANDOFF-ZS-SEC-005 交接单的待评审提交；2026-09-14 补评入库）
- 结论：**r0 CLEAN / 0 发现**（评审通过，无需修复弧）
- 原始日志：`outputs/handoff/ZS-SEC-005.txt`（未入库；本档案为处置入库）

## 结论原文

> "No actionable regressions were identified in the response-writer migration, exception handling, or added tests. Tests were not run because the environment is read-only."

codex 确认 `WebFrameworkUtils.writeJSON` 统一出口的 6 文件 8 处 filter-direct 写点迁移（TokenAuthenticationFilter 401、AccessDeniedHandler 403、AuthenticationEntryPoint 401、TenantSecurityWebFilter 403/400/异常 3 点、DemoFilter 901、ApiEncryptFilter）、畸形 JSON 归 400 与 InvalidFormatException 分支不回显原始入参、访问日志按业务码记录（401/403/429/5xx 不再误记成功）均无回归。

## 验证（交付时点证据，沿用卡片开发记录）

- 复用 ZS-SEC-012.A 夹具第 9 组 `UnifiedErrorResponse`（6 用例），biz-tenant 41/41、web+biz-tenant Maven 全绿
- 方案 B（改真实 HTTP 状态码）因会断 Web axios 令牌刷新链经评审否决，保留 HTTP 200+业务码契约并登记于 [错误响应与状态码矩阵](../../services/zhongshu-core/docs/错误响应与状态码矩阵.md)

## 边界

真实环境两端刷新/跳转/下载全面验收归 ZS-SEC-012.B（已收口）；日志脱敏归 ZS-SEC-007（本轮补评另见 codex-ZS-SEC-007.md，其 P1 已由 hotfix 修复）；trace 关联归 ZS-SEC-006。
