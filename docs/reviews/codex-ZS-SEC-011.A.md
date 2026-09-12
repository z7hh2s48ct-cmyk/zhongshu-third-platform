# ZS-SEC-011.A codex 评审结论与处置

> 被审提交（r0）：`3b5d3b8f`（ZS-SEC-011.A 幂等「同键异参冲突检测」，B03；前置 ZS-SEC-002、ZS-SEC-005（§16.1 登记），并复用 ZS-SEC-007 的 LogSanitizeUtils 脱敏工具；worktree 隔离分支 `feat/sec-011-a`，`--no-ff` 合并 main `957cbc95`）。
> r0 P1 修复提交：`b62af7d3`（幂等摘要按包名前缀排除 servlet 入参，堵首次放行即序列化响应破坏二进制输出，2 files +72/-3）。
> 二次复核（r1）：`codex review --commit b62af7d3`，揪出**新 P1**——包名前缀漏排 Tomcat 运行时实现类 `ResponseFacade`。
> r1 P1 修复提交：`d02f8129`（改按类型 `instanceof Servlet/ServletRequest/ServletResponse` 排除 + 前缀兜底，覆盖容器实现与包装器，2 files +40/-16）。
> 三次复核（r2）：`codex review --commit d02f8129`，确认生产类型排除正确（**P1 归零**），揪出 1×P2——回归测试断言保真度不足。
> r2 P2 修复提交：`c42b15c6`（回归测试改 `spy` + `verify(never())` 直接检测响应 getter，1 file +11/-8）。
> 四次复核（r3）：`codex review --commit c42b15c6`，结论 **CLEAN / 0 发现（No introduced defects were found）**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-SEC-011.A.raw.md](codex-ZS-SEC-011.A.raw.md)、r1 [codex-ZS-SEC-011.A.r1.raw.md](codex-ZS-SEC-011.A.r1.raw.md)、r2 [codex-ZS-SEC-011.A.r2.raw.md](codex-ZS-SEC-011.A.r2.raw.md)、r3 [codex-ZS-SEC-011.A.r3.raw.md](codex-ZS-SEC-011.A.r3.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈 **r0 揪 servlet 序列化 P1 + 2×P2 → 前缀修复 → r1 揪前缀漏排 Tomcat 新 P1 → 类型排除修复 → r2 确认 P1 归零 + 揪测试保真 P2 → spy 修复 → r3 CLEAN** 的四弧级联——与 ZS-CFG-001.B 同为「每轮修复引出下一轮更深问题、须对每个修复提交持续复评至归零，而非修一轮即收口」的范例；r0 的 2×P2 依约定第 2 条延后（附理由与归属）。

## Codex 原始结论（r0，被审提交 `3b5d3b8f`）

发现 **1 项 P1 + 2 项 P2**（无 P0 / P3）：

> The digest calculation can break servlet response handling and miss conflicts for long payloads. The new Redis-error warning also exposes potentially sensitive expression keys.

### Review comments

- **[P1] Exclude servlet objects from eager digest serialization** — `.../idempotent/core/aop/IdempotentAspect.java:76`
  > For an annotated controller accepting `HttpServletResponse`, this now serializes the live response before business execution, including on the first request. Jackson invokes its getters: with Tomcat's `ResponseFacade`, this calls `getWriter()` before falling back to a placeholder. Subsequent binary output, such as `ServletUtils.writeAttachment()`, then throws `IllegalStateException` because the writer was already selected. Exclude servlet request/response objects and other infrastructure arguments before computing the digest.

- **[P2] Compute the digest before applying log-length truncation** — `.../idempotent/core/aop/IdempotentAspect.java:76`
  > With an expression-based key, equal-length payloads differing only after character 2,048 are incorrectly classified as identical requests. `LogSanitizeUtils.sanitizeArgs()` truncates the serialized content and appends only its original length, so these distinct payloads produce identical MD5 inputs. This also affects non-sensitive fields and defeats the new conflict detection. Hash an untruncated sanitized representation and add a regression test covering differing suffixes beyond the logging limit.

- **[P2] Avoid logging raw expression-resolver keys** — `.../idempotent/core/aop/IdempotentAspect.java:88`
  > When an expression uses a sensitive field such as `#request.token` as its key and the follow-up Redis GET fails, this warning writes that token directly to application logs. `ExpressionIdempotentKeyResolver` returns the expression value unchanged, so keys are not necessarily hashes, and argument sanitization elsewhere does not protect this path. Omit the key or replace it with a safe opaque identifier.

## 复核与处置（P1：servlet 摘要序列化，历经 r0→r1→r2 三弧收口）

### P1 — 摘要「每请求必算」序列化 servlet 响应，破坏后续二进制输出（✅ 已修复：r0 前缀版 → r1 类型版两轮，r2 确认生产排除正确、r3 CLEAN）

**复核结论：成立（SEC-011.A 新引入的回归）。** SEC-011.A 把参数摘要从「仅拒绝分支计算」提升为「每请求必算」（含首次放行，[IdempotentAspect.java:83](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java) 的 `argsDigest`），以支持「同键异参冲突检测」。[LogSanitizeUtils#sanitizeArgs](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java) 内部用 Jackson `valueToTree` 序列化每个入参——会调用对象全部 getter。对 `HttpServletResponse`，Tomcat 运行时实现 `org.apache.catalina.connector.ResponseFacade` 的 `getWriter()` 被提前触发、选定响应字符输出模式；随后 `ServletUtils.writeAttachment()` 等二进制输出因 writer 已选定抛 `IllegalStateException`。SEC-011.A 之前摘要仅在拒绝分支计算（绝大多数请求不触发），提升为每请求必算后，该副作用对**所有**带 `HttpServletResponse` 入参的 `@Idempotent` 控制器首次放行即显现。

**r0 修复（`b62af7d3`，包名前缀排除版）**：新增 `serializableArgs(Object[])` helper，在计算摘要 / 脱敏日志前先剔除基础设施入参——镜像 [StrUtils#joinMethodArgs](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/string/StrUtils.java) 口径，按包名前缀 `StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")` 排除。回归测试 `testAroundPointCut_firstRequest_servletArgsExcludedFromDigest` 用 `mock(HttpServletResponse.class)`，断言摘要仅由业务入参计算。**RED→GREEN，protection 25 全绿。**

**r1 揪出新 P1（`codex review --commit b62af7d3`）**：前缀排除**不完整**——

> [P1] Exclude servlet implementations by type rather than package — `IdempotentAspect.java:147-149`: For an `@Idempotent` method receiving `HttpServletResponse` on Tomcat, the runtime class is `org.apache.catalina.connector.ResponseFacade`, which matches none of these prefixes. The new filter therefore retains it, allowing digest serialization to call `getWriter()` before the method executes. With a real Tomcat response, this causes subsequent binary output through `ServletUtils.writeAttachment()` to throw `IllegalStateException`. Use servlet interface/type checks that also cover wrappers, and test a concrete container response rather than only an interface mock.

即 Tomcat 运行时实现类 `ResponseFacade`（及各类 `HttpServlet*Wrapper`）**不以任何 servlet 包名开头**（落在 `org.apache.catalina.*`），前缀过滤漏排它们，P1 未真修。且 r0 测试用 `mock(HttpServletResponse.class)`——Mockito ByteBuddy 生成类名以被 mock 类型包名 `jakarta.servlet` 开头、恰被前缀命中，是**假 GREEN**，覆盖不到生产 ResponseFacade 场景。

**r1 修复（`d02f8129`，类型排除版）**：`serializableArgs` 改按**类型**判定——`arg instanceof Servlet || arg instanceof ServletRequest || arg instanceof ServletResponse`（覆盖所有容器实现与包装器，运行时类名不在 servlet 包下也命中）+ 保留包名前缀作兜底（排除 `MultipartFile` 等其余 spring-web 基础设施对象，口径仍与 `StrUtils.joinMethodArgs` 一致）；新增 `jakarta.servlet.{Servlet,ServletRequest,ServletResponse}` imports。回归测试改用**具体容器响应** `ContainerLikeResponse extends MockHttpServletResponse`（运行时类名落业务包 `cn.zszj` 下 + IS-A `ServletResponse`，精确复现 ResponseFacade「非 servlet 包名 + servlet 类型」特征）；RED（对 `b62af7d3` 前缀版，`Wanted but not invoked`@digest 断言，实证前缀漏排 ContainerLikeResponse）→GREEN。**protection 25 全绿（IdempotentAspectTest 7→8）。**

## 复核与处置（r0 2×P2，延后 SEC-011.B）

### P2-1 — 摘要在 2048 截断后计算，等长异尾入参碰撞（⏸ 延后，附理由与归属）

**复核结论：观察成立，依「后续处理约定」第 2 条延后。** `LogSanitizeUtils.sanitizeArgs()` 对序列化内容按 `MAX_LENGTH=2048` 截断并仅追加原始长度，故等长、仅第 2048 字符后不同的入参产生相同 MD5 输入 → 冲突检测漏判（亦波及非敏感字段）。**归属 ZS-SEC-011.B**：契合 .B 已 scope 的「Key/Value 职责切分」（Key 用业务幂等号、Value 存**全量**入参摘要）——正解是摘要走「未截断的脱敏表示」，与 .B 的 Value 全量摘要设计同源；单独在 .A 打补丁会与 .B 重构冲突，故合并处置。

### P2-2 — Redis 读回失败告警裸 expression key（⏸ 延后，附理由与归属）

**复核结论：观察成立，触发面极窄，依约定第 2 条延后。** [IdempotentAspect.java:95](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java) 的 `log.warn(...key...)` 在「`ExpressionIdempotentKeyResolver` 的 Key = 裸 SpEL 值（如 `#request.token`）+ 后续 Redis GET 失败」时把该值写日志。**归属 SEC-011.B / REC-1**：仅 Expression 解析器路径 + Redis 故障 + 敏感 SpEL 三重窄条件同时成立才触发；`DefaultIdempotentKeyResolver`/`UserIdempotentKeyResolver` 的 Key 已是 md5 哈希（不含秘密），不受影响。与 SEC-011.A 已登记的 IMP-4（Expression 解析器缺租户作用域）同归 REC-1 的 SubjectScope 统一抽取（SEC-010 + 011.A 合并后跟进）。

## 三次复核（r2，被审提交 `d02f8129`）

对 r1 类型排除修复重跑 `codex review --commit d02f8129`，**确认生产修复正确（P1 归零）**，另揪出 1×P2（测试断言保真）：

> The production type-based exclusion appears correct, but the replacement test assertion cannot detect the response-output side effect it claims to verify and removes the previous explicit checks.
>
> [P2] Preserve assertions that detect response output access — `IdempotentAspectTest.java:345-346`: With Spring 6.2.19's `MockHttpServletResponse`, calling `getWriter()` does not prevent subsequent `getOutputStream()` access: their access flags are independent. Consequently, this assertion passes even if the aspect has already accessed the writer. The digest assertion checks only which arguments contribute to the digest, so replacing the previous `never()` verifications loses the explicit protection against response side effects. Use a spy on the concrete response and retain those verifications, or use a response double that enforces writer/output-stream mutual exclusion.

即 r1 修复把 C2 断言从 `verify(never())` 换成了 `assertDoesNotThrow(response::getOutputStream)`，但 Spring 6.2 的 `MockHttpServletResponse` 中 `getWriter()`/`getOutputStream()` 访问标志相互独立（不互斥抛异常）——该断言即便 aspect 已调 `getWriter()` 也照样通过，检测不到它声称要防的副作用，且丢失了原有的显式保护。

**r2 修复（`c42b15c6`）**：`response` 改为 `spy(new ContainerLikeResponse())`，C2 恢复 `verify(response, never()).getWriter()` / `verify(response, never()).getOutputStream()` **直接检测** getter 是否被调用——类型排除生效后 response 未进入 `sanitizeArgs`、Jackson 从不序列化它，两个 getter 从未被调用。**纯测试保真强化（生产代码未改），protection 25 全绿（IdempotentAspectTest 8）。**

## 四次复核（r3，被审提交 `c42b15c6`）

对 r2 测试修正重跑 `codex review --commit c42b15c6`，结论 **CLEAN，0 发现**（无 P0 / P1 / P2 / P3）：

> The change strengthens the regression test by directly verifying that neither response getter is called, while preserving the servlet-exclusion scenario. No introduced defects were found. Tests were not run in the read-only environment.

**r0 的 P1（servlet 序列化副作用）经 r0 前缀版 → r1 类型版两轮修复、r2 确认生产排除正确后彻底消除；r2 的 P2（测试保真）经 spy + verify(never) 修复、r3 确认归零。**

## 结论

**评审通过（四弧级联：r0 1×P1 + 2×P2 → r1 新 1×P1 → r2 P1 归零 + 1×P2 → r3 CLEAN 0 发现）。**

- **r0**（被审 `3b5d3b8f`）：1×P1 + 2×P2——P1：摘要每请求必算，Jackson 序列化 servlet 响应提前触发 `getWriter()` 破坏二进制输出；P2-1：摘要在 2048 截断后计算致等长异尾碰撞；P2-2：Redis 读回失败告警裸 expression key。
- **r0 P1 修复**（`b62af7d3`）：`serializableArgs` 按包名前缀排除 servlet（镜像 `StrUtils`）；RED→GREEN，protection 25/0。
- **r1**（被审 `b62af7d3`）：**新 1×P1**——前缀漏排 Tomcat `ResponseFacade`（`org.apache.catalina.*` 非 servlet 包名），且 `mock(HttpServletResponse.class)` 假 GREEN 覆盖不到。
- **r1 P1 修复**（`d02f8129`）：改按类型 `instanceof Servlet/ServletRequest/ServletResponse` 排除 + 前缀兜底；测试改用具体容器响应 `ContainerLikeResponse extends MockHttpServletResponse`（cn.zszj 包名复现 ResponseFacade 特征）；RED→GREEN，protection 25/0。
- **r2**（被审 `d02f8129`）：**P1 归零**（"production type-based exclusion appears correct"）+ 1×P2——Spring 6.2 `MockHttpServletResponse` 的 `getWriter`/`getOutputStream` 标志独立，`assertDoesNotThrow` 检测不到副作用。
- **r2 P2 修复**（`c42b15c6`）：`spy(new ContainerLikeResponse())` + `verify(never()).getWriter()/getOutputStream()` 直接检测；protection 25/0（IdempotentAspectTest 8）。
- **r3**（被审 `c42b15c6`）：CLEAN，0 发现，"No introduced defects were found"。
- **本机复验证据**：protection 套件 **25 测试全绿**（IdempotentAspectTest 8 + DefaultIdempotentKeyResolverTest 7 + RateLimiterAspectTest 5 + ExpressionRateLimiterKeyResolverTest 3 + ApiSignatureAspectTest 1 + ApiSignatureTest 1），`PROT_MVN_EXIT=0`；上游 `-am` web **37**（Skipped 1）/ security **18** / mybatis **13** / common 全绿，`Reactor Summary for zszj 2026.08-SNAPSHOT ... BUILD SUCCESS`。
- codex 发现总数 **5**（2×P1 为同一 servlet 排除问题的「前缀版→类型版」两轮深挖 + 3×P2），已修复 **3**（P1 经两轮 + P2-3 测试保真），延后 **2**（P2-1 截断碰撞、P2-2 裸 key，附理由与归属 SEC-011.B / REC-1），仍有效阻塞项 **0**。
- ZS-SEC-011.A 为**分批子项**（父卡 ZS-SEC-011 循 ZS-SEC-009 惯例，本轮交付后父卡 待开发→**开发中**；.B 持久化幂等 + Key/Value 职责切分留待 B05 批次）。本轮交付：幂等「同键异参冲突检测」（区分「重复请求」与「幂等键冲突」两种拒绝原因）+ 摘要每请求必算下的 servlet 基础设施入参**按类型**排除。
- **延后 Minor（非阻塞）**：①**P2-1** 摘要未截断表示（归 SEC-011.B Key/Value 切分）；②**P2-2** Expression key 裸日志（归 SEC-011.B / REC-1，三重窄触发）；③spec/质量评审阶段已登记的延后项——REC-1（SubjectScope 统一抽取，含 IMP-4 Expression/User 解析器缺租户作用域、IMP-5 租户因子取自可选头非权威 `TenantContextHolder`、MIN-12）、REC-2（`keyIncludesArgs` 归 SEC-011.B）、IMP-7（生产端 clientIP 归 SEC-011.B）、MIN-9（`getTenantId` 畸形头返 400 而非降级）、2 项观察（servlet 排除口径已随本 P1 收口、2048 截断即 P2-1）。
