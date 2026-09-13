# ZS-SEC-006 codex 评审结论与处置

> 被审提交（r0）：`8785b9f3`（ZS-SEC-006 无链路代理时的请求关联标识 fallback + 外部 trace-id 信任边界，B03/B04；前置 ZS-SEC-004、ZS-SEC-005 合同确定；worktree 隔离分支 `feat/sec-006`，`--no-ff` 合并 main `e9daa398`）。
> r0 P2 修复提交：`30e77350`（CORS 放行 trace-id 请求头 + 无参 getCorrelationId 复用绑定关联 ID，4 files +106/-5）。
> 二次复核（r1）：`codex review --commit 30e77350`，结论 **CLEAN / 0 发现**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-SEC-006.raw.md](codex-ZS-SEC-006.raw.md)、r1 [codex-ZS-SEC-006.r1.raw.md](codex-ZS-SEC-006.r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈 **r0 揪 2×P2（CORS 未放行新 trace-id 请求头 + 无参 getCorrelationId 每次生成新 ID 破坏关联一致性）→ 现在 TDD 修复 → r1 确认归零**。ZS-SEC-006 为整卡任务（非分批子项），本轮请求关联标识主干交付、卡片 待开发→待验收；文件下载错误/SSE 关联归 ZS-SEC-012.B/FILE-004，跨域 trace-id 端到端读取归环境验收。

## SEC-006 交付背景

ZS-SEC-006 的目标是「保证无链路代理时也有可关联的请求标识」——有/无 OTel 两种环境下，正常与认证/参数/系统失败都能从前端定位同一请求日志；异常和线程复用不串号；跨域客户端能读取批准响应头；不通过任意客户端 ID 授权或关联到他人敏感信息。底座现状是 [TraceFilter][E40] 已写 trace-id、[TracerUtils][E41] 无有效 Span 时返回空串、[Web CORS 配置][E32] 未声明暴露 trace-id，两个请求层尚无本项目统一的关联处理。

本轮交付（`8785b9f3`）：

- **关联 ID 统一入口**（[TracerUtils](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/monitor/TracerUtils.java)，zszj-common）：保留 `getTraceId()` 纯 OTel 语义（无 Span 返空串、向后兼容既有消费方）；新增 `getCorrelationId()`/`getCorrelationId(request)` 统一关联入口（优先级 OTel traceId → 请求作用域已绑定 fallback → 新生成），保证非空、有界（32 hex）、不可承载权限；`generateCorrelationId()` 基于 `UUID.randomUUID()`；`isValidTraceIdFormat()` 校验外部传入恰 32 hex（信任边界）。
- **请求入口绑定 + 响应头**（[TraceFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/main/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java)，monitor）：`resolveCorrelationId` 按「OTel traceId > 外部合法 trace-id（归一化小写、仅关联不授权）> 服务端生成 fallback（畸形/超长/缺失均不回显攻击者可控原文）」解析；绑定到请求属性 `zszj.correlation.id`；响应头在 `chain.doFilter` **之前**设置 → 后续过滤器早退/异常时客户端仍可读到关联 ID。
- **下游统一取值**：[ApiAccessLogFilter][E39] 访问日志、[GlobalExceptionHandler][E37] 异常日志/响应改用 correlationId；web starter `pom.xml` 补依赖。
- 测试：`TracerUtilsFallbackTest`（common 14）+ `TraceFilterTest`（monitor 10）+ `CorsConfigTest`（web 7，含 P2-1 预检）。

## Codex 原始结论（r0，被审提交 `8785b9f3`）

判定发现 **2 项 P2**（无 P0 / P1 / P3）：

> The basic fallback path works, but the no-argument accessor breaks request correlation and the default CORS policy blocks the newly supported incoming header. Both issues were reproduced using the existing compiled classes.

### Review comments

- **[P2] Allow the new trace-id request header through CORS** — `services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/main/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java:61-63`
  > When a cross-origin browser client supplies the newly supported `trace-id` header, `WebProperties.Cors.allowedHeaders` rejects it before this filter runs. An OPTIONS request from `http://localhost` to port 48080 with `Access-Control-Request-Headers: trace-id` returns 403. The existing `exposedHeaders` entry only permits reading response headers. Add `trace-id` to the request-header allowlist and cover this with a preflight test.

- **[P2] Reuse the bound request ID in the no-argument accessor** — `services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/monitor/TracerUtils.java:70-72`
  > Without an OTel span, this accessor generates a fresh ID on every call even when `TraceFilter` has already bound an ID and the request is available through `RequestContextHolder`. Its results therefore differ from both the response header and the access/error logs, contradicting its documented request-attribute fallback. Resolve the current request through the existing `ServletUtils.getRequest()` and delegate to the request overload, generating a one-off ID only outside a request.

## 复核与处置（r0 2×P2）

### P2-1 — CORS 未放行新的 trace-id 请求头（✅ 已修复，r1 确认归零）

**复核结论：成立（SEC-006 与 SEC-004 的联动缺口）。** ZS-SEC-004 已在 `WebProperties.Cors.exposedHeaders` 暴露 trace-id（允许跨域客户端**读响应头**），但 SEC-006 新增「客户端可**发** trace-id 请求头以端到端串联」后，`allowedHeaders` 白名单未含 trace-id → 跨域浏览器预检 `Access-Control-Request-Headers: trace-id` 被 403 拦截，请求根本到不了 `TraceFilter`。

**修复（commit `30e77350`，RED→GREEN）**：`WebProperties.Cors.allowedHeaders` 精确追加 `trace-id`（不回退通配符、不放松其他头）；`CorsConfigTest` 新增预检测试断言 trace-id 在放行集内（web 7 测试全绿）。

### P2-2 — 无参 getCorrelationId 每次生成新 ID（✅ 已修复，r1 确认归零）

**复核结论：成立（关联一致性缺口）。** 原 `getCorrelationId()` 无参版在无 OTel Span 时**每次调用都 `generateCorrelationId()`**，即使 `TraceFilter` 已把 ID 绑定到请求属性、且请求可经 `RequestContextHolder` 获取 → 其结果与响应头、访问日志、错误日志**各不相同**，违背「同一请求可关联」的核心目标与其自身 javadoc 承诺的请求属性 fallback。

**修复（commit `30e77350`，RED→GREEN）**：无参 `getCorrelationId()` 改为三级委托——① OTel Span 有效 → traceId；② `ServletUtils.getRequest()` 非 null → 委托 `getCorrelationId(request)` 复用 `TraceFilter` 已绑定值；③ 请求上下文之外（后台任务/消息消费线程）→ 才 `generateCorrelationId()` 生成一次性 ID。`ServletUtils` 与 `TracerUtils` 同在 zszj-common、无新增跨模块依赖（反应堆编译安全）。`TracerUtilsFallbackTest` 覆盖三级路径（common 14 测试全绿）。

## 二次复核（r1，被审提交 `30e77350`）

对 r0 P2 修复提交重跑 `codex review --commit 30e77350`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **CLEAN，0 发现**（无 P0 / P1 / P2 / P3）：

> The changes correctly allow trace-id in CORS preflights and reuse request-bound correlation IDs while preserving OpenTelemetry precedence. No actionable regressions were identified. Tests were not executed in the read-only environment.

codex r1 在 exec trace 中自核 `TraceFilter.java`、`CorsConfigTest.java`、`WebProperties.Cors.allowedHeaders` 与全仓 `TracerUtils.getCorrelationId()` 消费方，独立确认「CORS 预检放行 trace-id、无参访问器复用请求绑定 ID、OTel 优先级保留、无新回归」。**r0 的 2×P2 已消除。**（codex sandbox `read-only` 且 Java/Maven 不在 PATH，两轮均未执行测试，通过性以本机 framework 测试 BUILD SUCCESS 为准。）

## 结论

**评审通过（r0 发现 2×P2 均已修，r1 复核 0 发现确认消除）。**

- **r0**（被审 `8785b9f3`）：2×P2——① CORS `allowedHeaders` 未放行 SEC-006 新增的 trace-id 请求头（跨域预检 403）；② 无参 `getCorrelationId()` 每次生成新 ID，与响应头/访问日志/错误日志不一致（破坏关联）。
- **r0 P2 修复**（`30e77350`，4 files +106/-5）：① `WebProperties.Cors.allowedHeaders` 追加 trace-id + `CorsConfigTest` 预检断言；② 无参 `getCorrelationId()` 改三级委托（OTel → `ServletUtils.getRequest()` 复用绑定 → 请求外一次性）+ `TracerUtilsFallbackTest` 覆盖；RED→GREEN。
- **r1**（被审 `30e77350`）：CLEAN，0 发现，codex 自核过滤器/配置/消费方独立确认放行 trace-id、复用绑定 ID、保留 OTel 优先级、无新回归。
- **本机复验证据**：`TracerUtilsFallbackTest` 14/0/0（common）+ `TraceFilterTest` 10/0/0（monitor）+ `CorsConfigTest` 7/0/0（web），framework 相关 starter `BUILD SUCCESS`；合并 main 后全反应堆 `mvn -T1C test-compile` `REACTOR_EXIT=0`（21 模块编译通过、`TracerUtils` 上游改动下游编译干净）。
- **延后/边界（非阻塞，见 reviews/README「后续处理约定」第 2 条）**：①文件下载错误、SSE/流式响应的关联标识端到端归 ZS-SEC-012.B/FILE-004；②跨域客户端实际读取 trace-id 响应头的浏览器端到端验证归环境验收（本轮为 `CorsConfigTest` 单元级 + `exposedHeaders` 声明）；③关联 ID ≠ 分布式 Trace，仅作前端/日志定位、绝不作授权凭据（已在 `TracerUtils` javadoc 与 `resolveCorrelationId` 信任边界固化）。
- codex 发现总数 **2**（2×P2），已修复 **2**，延后 **0**，仍有效阻塞项 **0**。
- **本记录不表示任何主任务已验收。**
