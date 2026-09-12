# ZS-SEC-010 codex 评审结论与处置

> 被审提交（r0）：`712e0cee`（ZS-SEC-010 把限流能力落实到获准的高风险接口，B03；前置 ZS-SEC-002、ZS-SEC-005、ZS-SEC-007 已完成；worktree 隔离分支 `feat/sec-010`，`--no-ff` 合并 main `efbfc0b2`）。
> r0 P1 修复提交：`0eba03aa`（限流拒绝日志参数名感知脱敏，堵标量 refreshToken 与短信 code 明文泄露，4 files +110/-5）。
> 二次复核（r1）：`codex review --commit 0eba03aa`，结论 **CLEAN / 0 发现**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-SEC-010.raw.md](codex-ZS-SEC-010.raw.md)、r1 [codex-ZS-SEC-010-r1.raw.md](codex-ZS-SEC-010-r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈 **r0 揪凭据泄露 P1 + 租户隔离 P2 → TDD 修复 P1 → r1 确认归零**，P2 依约定第 2 条延后（附理由与归属）。

## Codex 原始结论（r0，被审提交 `712e0cee`）

判定 **FAIL**，发现 **1 项 P1 + 1 项 P2**（无 P0 / P3）：

> The new annotations expose authentication credentials through rejection logging. The tenant key also disagrees with the existing authenticated-user fallback, undermining quota isolation.

### Review comments

- **[P1] Redact authentication secrets in limiter rejection logs** — `services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/auth/AuthController.java:93`
  > When a refresh token exceeds five requests per minute, `RateLimiterAspect` logs `sanitizeArgs(joinPoint.getArgs())`. That sanitizer only masks object fields, so this method's scalar String argument exposes the complete, reusable refresh token. The newly limited SMS login/reset methods also expose their `code` fields because that name is not recognized as sensitive. Hashing the Redis key does not protect these argument logs. Suppress credential arguments or add parameter-aware and endpoint-specific masking, with rejection tests using the actual controller argument shapes.

- **[P2] Build limiter keys from the effective tenant** — `services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java:59-61`
  > For authenticated requests without `tenant-id`, `TenantSecurityWebFilter` legitimately populates `TenantContextHolder` from the login user's tenant. Reading the header again here instead produces null: identical subjects in different tenants share a bucket, while the same tenant can obtain another quota simply by adding its header. Resolve the effective tenant through a shared abstraction or request attribute populated by the tenant filter, and test both explicit-header and authenticated-fallback requests.

## 复核与处置（r0 P1）

### P1-1 — 限流拒绝日志泄露标量 refreshToken 与短信 code（✅ 已修复，r1 确认归零）

**复核结论：成立（SEC-010 新引入的回归）。** SEC-010 之前 `refresh-token` / `sms-login` / `reset-password` 无 `@RateLimiter`，[RateLimiterAspect](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect.java) 的拒绝日志路径从不为这些凭据端点触发；SEC-010 给它们加上限流注解后，一旦被拒即走旧 `log.info(..., LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()))`。而 [LogSanitizeUtils#sanitizeArgValue](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java) 对每个入参 `valueToTree` 后仅在 `node.isObject()` 时按敏感键掩码：①`refresh-token` 的入参是**标量 String** `refreshToken`，`valueToTree` 得 `TextNode`、`sanitizeNode` 遇 `!isObject()` 直接 return 不掩码 → 完整可复用刷新令牌明文落日志；②旧调用**未传端点级 extraKeys**，而 `code`（短信验证码）不在内置根集 `SENSITIVE_KEY_ROOTS`（无 `code`）→ `sms-login` / `reset-password` 的验证码明文落日志。codex 亦点明「Hashing the Redis key does not protect these argument logs」（Key 已 md5，但泄露在参数日志而非 Key）。既有测试 `testBeforePointCut_tooManyRequests_argsSanitizedInLog` 恰用含敏感键的对象 Map 入参，未覆盖标量 String 与 `code` 字段，故漏网。

**TDD 修复（commit `0eba03aa`，RED→GREEN）**：
- [@RateLimiter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/annotation/RateLimiter.java) 新增 `String[] maskKeys() default {}`——端点级附加脱敏键（归一后精确匹配），补齐内置根集覆盖不到的敏感字段（如 `code`）。
- [RateLimiterAspect](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect.java) 新增 `buildArgMap(JoinPoint)`，将入参构建为「参数名 -> 值」`LinkedHashMap`（`signature instanceof MethodSignature` 取 `getParameterNames()`，否则退化位置名 `argN`、缺名亦退化，不阻断脱敏）；拒绝日志由 `sanitizeArgs(args)` 改为 `LogSanitizeUtils.sanitizeMap(buildArgMap(joinPoint), rateLimiter.maskKeys())`——**参数名感知**使标量 `refreshToken`（归一 `refreshtoken` 含 `token` 根集）自动掩码，**端点级 maskKeys** 使 `code` 精确掩码。
- [AuthController](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/auth/AuthController.java) `sms-login` / `reset-password` 的 `@RateLimiter` 加 `maskKeys = {"code"}`（`reset-password` 的 `password` 已由内置根集掩码，无需重复）。
- [RateLimiterAspectTest](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest.java) 新增 2 例，**复刻 controller 真实入参形态**（codex 明确要求）：`scalarRefreshTokenMasked`（`MethodSignature.getParameterNames()=["refreshToken"]` + 标量 `SECRET_TOKEN`，断言日志不含令牌、含 `***`）、`smsCodeMaskedViaMaskKeys`（`paramNames=["reqVO"]` + `Map{mobile,code:SECRET_CODE}` + `maskKeys={"code"}`，断言不含验证码）。新代码独用的 `getParameterNames()`/`maskKeys()` 桩以 `lenient()` 标注，避免 RED 阶段 `UnnecessaryStubbingException` 掩盖真实断言失败。

**未把 `code` 加进全局 `SENSITIVE_KEY_ROOTS` 的理由**：`LogSanitizeUtils` 为访问日志 / 异常 / 幂等 / 限流 / 签名五写点共享，全局根集加 `code` 会经 `contains` 模糊匹配**过度掩码** `areaCode`/`zipCode`/`barcode`/`qrCode` 等全系统普通字段。端点级 `maskKeys` 精确匹配是更小副作用的正解。

**RED→GREEN 经验裁决**：2 新测试首跑 RED（`RateLimiterAspectTest.java:145`/`:178` 均 `expected: <false> but was: <true>`，实证标量令牌与验证码确实落日志）→ 修复后 GREEN。protection 套件 **8 测试 / 0 失败**（RateLimiterAspectTest 5：3 原 + 2 新；ExpressionRateLimiterKeyResolverTest 3），`PROT_MVN_EXIT=0`；system 模块 `-am test-compile` **19 模块 BUILD SUCCESS**（protection 带新 `maskKeys` + system AuthController 用 `maskKeys` 均编译通过），`SYS_MVN_EXIT=0`。

## 复核与处置（r0 P2，延后）

### P2-1 — 限流 Key 的 tenant 取自 header 而非 effective tenant（⏸ 延后，附理由与归属）

**复核结论：观察成立，但对本任务实际用法不显现，依「后续处理约定」第 2 条延后。** [ExpressionRateLimiterKeyResolver](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java) 经 `WebFrameworkUtils.getTenantId(request)`（只读 `tenant-id` 头）取租户拼进 Key。延后理由四条：
1. **前提不匹配本任务用法**：P2 的场景是「已认证请求无 `tenant-id` 头时，`TenantSecurityWebFilter` 从登录用户回填 `TenantContextHolder`」。而 SEC-010 落地的 5 个端点（`login`/`refresh-token`/`sms-login`/`send-sms-code`/`reset-password`）**全为 `@PermitAll` 预认证公开端点**，无登录用户，故不存在「认证回退租户」——对公开端点而言，header 就是 effective tenant 的唯一合法来源。
2. **非真实限流绕过**：Key 含 `tenantId` 正是为了「每租户每主体」独立配额。轮换 header 打到的是**不同租户的不同账户命名空间**（tenant-id 定义账户所属租户），要爆破某一租户的 `admin` 必须固定该租户 header → 仍受 5/min 约束，不构成对单一账户配额的绕过。
3. **正确通用修复是跨模块重构**：需一个「effective tenant 共享抽象 / 由租户过滤器写入的 request attribute」，而 `TenantContextHolder` 位于 `zszj-spring-boot-starter-biz-tenant`，protection **不能反向依赖 biz-tenant**（会破坏分层、且 protection 处于更底层）。这超出 SEC-010「把限流落到高风险接口」的范围。
4. **codex 自评 P2**（非 P0/P1），约定第 2 条允许 P2/P3 分批处置。

**归属**：记为 SEC-010 延后 Minor + follow-up——「protection 限流/幂等 Key 解析器的 effective-tenant 共享抽象」，触发条件为「限流/幂等开始用于**认证后**端点」；候选归 ZS-SEC-011.B（幂等 Key 同源问题）或独立小卡。

## 二次复核（r1，被审提交 `0eba03aa`）

对 r0 P1 修复提交重跑 `codex review --commit 0eba03aa`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **CLEAN，0 发现**（无 P0 / P1 / P2 / P3）：

> No actionable regressions were identified. The parameter-aware sanitization and endpoint-specific code masking are compatible with the existing sanitizer and configured parameter-name retention. Tests were inspected but not rerun in the read-only environment.

codex r1 在 exec trace 中自核 `git diff --name-only 0eba03aa^ 0eba03aa`（4 文件）、`git show` 新 `@RateLimiter`（含 `maskKeys`）与 `LogSanitizeUtils` 根集、`git grep maskKeys/getParameterNames`，确认参数名感知脱敏与端点级 `code` 掩码同既有 sanitizer 及参数名保留配置兼容、无新回归。**r0 的 P1 已消除。**（codex sandbox `read-only` 未执行测试，通过性以本机 protection 8/8 + system 19 模块 test-compile `BUILD SUCCESS` 为准。P2 不在 r1 diff 范围——本轮未改 `ExpressionRateLimiterKeyResolver`——按上文延后处置。）

## 结论

**评审通过（r0 发现 1×P1 凭据泄露已修 + 1×P2 租户隔离延后，r1 复核 0 发现确认 P1 消除）。**

- **r0**（被审 `712e0cee`）：FAIL，1×P1 + 1×P2——P1：SEC-010 给凭据端点加 `@RateLimiter` 激活拒绝日志路径，旧 `sanitizeArgs` 只掩码对象字段、放过标量且不接收端点级 extraKeys，致标量 `refreshToken` 与短信 `code` 明文落日志；P2：限流 Key 的 tenant 取自 header 而非 effective tenant。
- **r0 P1 修复**（`0eba03aa`）：`@RateLimiter` 加 `maskKeys` + `RateLimiterAspect` 加 `buildArgMap` 参数名感知、拒绝日志改 `sanitizeMap(argMap, maskKeys)` + AuthController `sms-login`/`reset-password` 加 `maskKeys={"code"}` + 2 复刻真实入参形态的回归测试；RED→GREEN，protection 8/0、system 19 模块 test-compile SUCCESS。
- **r1**（被审 `0eba03aa`）：CLEAN，0 发现，codex 自核 diff/注解/grep 独立确认参数名感知脱敏兼容、无新回归。
- **本机复验证据**：protection 套件 8 测试全绿（RateLimiterAspectTest 5 + ExpressionRateLimiterKeyResolverTest 3），`PROT_MVN_EXIT=0`；system `-am test-compile` 19 模块 `BUILD SUCCESS`，`SYS_MVN_EXIT=0`。
- ZS-SEC-010 为整卡任务（非分批子项），本轮交付：5 个高风险认证端点按固定主体（username / refreshToken / mobile）限流、改普通参数不换 Key、Redis 故障 fail-open 放行 + 告警、拒绝日志参数名感知脱敏。卡片 待开发→**待验收**。
- codex 发现总数 **2**（1×P1 + 1×P2），已修复 **1**（P1），延后 **1**（P2，附理由与归属），仍有效阻塞项 **0**。
- **延后 Minor**（非阻塞）：①**P2-1** effective-tenant 共享抽象（见上，触发于限流用于认证端点时，候选 ZS-SEC-011.B）；②`fail-open` 放行无 metric/告警指标（仅日志，运维感知弱）；③`register` / `social-login` 等其余公开写入口本轮未纳入限流（按「获准高风险接口」范围裁量，后续盘点补充）；④拒绝日志中文在 GBK 控制台乱码（既知日志编码问题，非本卡引入，归日志编码专项）。
