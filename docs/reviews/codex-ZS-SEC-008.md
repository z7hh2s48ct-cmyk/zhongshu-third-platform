# ZS-SEC-008 codex 评审结论与处置

> 被审提交（r0）：`76da2a2f`（ZS-SEC-008 补齐参数校验、上下文头解析与请求资源限制，B03；前置 ZS-SEC-002、ZS-SEC-005 已完成）。
> P1 修复提交：`3f4fa736`（限界读取两道防线 + 2 回归测试 + 设计文档同步，4 files +130/-8）。
> 二次复核（r1）：`codex review --commit 3f4fa736`，结论 PASS / 0 发现。
> 评审工具：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`。
> 完整 stdout：r0 [codex-ZS-SEC-008.raw.md](codex-ZS-SEC-008.raw.md)、r1 [codex-ZS-SEC-008-r1.raw.md](codex-ZS-SEC-008-r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。

## Codex 原始结论（r0，被审提交 `76da2a2f`）

判定 **FAIL**，发现 **1 项 P1**（无 P0 / P2 / P3）：

> The new JSON buffering limit is bypassable through chunked requests, leaving unbounded pre-authentication memory allocation despite the configured cap.

### Review comments

- **[P1] Enforce the cache limit while reading unknown-length bodies** — `services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java:52-56`
  > For chunked `application/json` requests, `getContentLengthLong()` returns `-1`, so this check passes and `CacheRequestBodyWrapper` still reads the entire body into memory. A 2 MiB body with unknown length was fully cached despite the configured 1 MiB limit. Unauthenticated clients can therefore bypass the new resource limit and exhaust heap memory. Keep the Content-Length check as an early rejection, but also bound the actual stream read and reject once it exceeds `maxCacheSize`; add an unknown-length request regression test.

## 复核与处置

### P1 — chunked / 未知长度 body 绕过 JSON 缓冲上限（✅ 已修复 + r1 复核确认消除）

**复核结论：成立，非误报。** [CacheRequestBodyFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java) 原仅按声明式 `Content-Length` 早拒（`maxCacheSize > 0 && request.getContentLengthLong() > maxCacheSize`）。chunked `application/json`（`Transfer-Encoding: chunked`）无声明 Content-Length，`getContentLengthLong()` 返回 `-1`，`-1 > maxCacheSize` 恒 false → 绕过早拒；随后 `new CacheRequestBodyWrapper(request)` 构造时经 `ServletUtils.getBodyBytes`（委托 Hutool `JakartaServletUtil.getBodyBytes`）全量读入 `byte[]`，未认证客户端可用未知长度超大 body 无界缓冲耗尽堆内存。属 SEC-008 调整 #3（请求资源限制）**自身**的真实缺陷，非可归 ZS-SEC-012.B 的容器层问题。原 6 个测试全用 `MockHttpServletRequest.setContent(...)`（自动设 Content-Length），无一覆盖 chunked 路径 —— 此即 P1 盲区根因。

**修复（commit `3f4fa736`，逐条对应 codex 建议）**——构成「声明式早拒 + 限界读取」两道防线：

- [CacheRequestBodyWrapper](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyWrapper.java)：新增双参构造 `(request, maxCacheSize)` + `readBody()`——`maxCacheSize <= 0` 沿用原全量读取（向后兼容）；`> 0` 时以 8192 字节 buffer **边读边累加** `total`，`total > maxCacheSize` 立即抛新增 `TooLargeException extends RuntimeException`（不再继续读入堆）。保留无参构造委托 `-1L`。→ 对应 codex「bound the actual stream read and reject once it exceeds `maxCacheSize`」。
- [CacheRequestBodyFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java)：保留 ① Content-Length 声明式早拒（快速路径，不读 body），新增 ② `try { wrapper = new CacheRequestBodyWrapper(request, maxCacheSize); } catch (TooLargeException e) { writeJSON 400; return; }`；两道防线共用同一「请求体大小超过上限」业务码 400 出口。→ 对应 codex「Keep the Content-Length check as an early rejection」。
- [CacheRequestBodyFilterTest](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilterTest.java)：新增 2 例未知长度回归护栏——`unknownLengthJsonRequest(bodySize)` 以匿名 `MockHttpServletRequest` 子类覆盖 `getContentLength()`/`getContentLengthLong()` 返回 `-1`、`setContent` 大 body 模拟 chunked；`testUnknownLengthOversizedJsonRejected`（超限 → 拒绝 400、不进入过滤链）+ `testUnknownLengthNormalJsonPasses`（合法大小 → 限界读取内正常缓冲放行、保持实际字节数）。→ 对应 codex「add an unknown-length request regression test」。
- [参数校验与请求资源限制规范.md](../../services/zhongshu-core/docs/参数校验与请求资源限制规范.md)：§2 现状表标注「已修复」、§4 补 ② 限界读取（chunked）段、§6 测试计数 6→8，设计文档与代码同批同步。

**本机复验证据**：`mvn -pl :zszj-server -am test`（web + zszj-server 反应堆）**22 测试全绿**——`CacheRequestBodyFilterTest` 6→**8** + `WebFrameworkUtilsTest` 11（web 合计 19）+ `ValidationContractTest` 3，`BUILD SUCCESS`；`node scripts/ops/run-local-gates.mjs --fast` **10/10**；`node scripts/gov/verify-docs.mjs` **0 issue**。

## 二次复核（r1，被审提交 `3f4fa736`）

对 P1 修复提交重跑 `codex review --commit 3f4fa736`（`gpt-6-astra`/`xhigh`），结论 **PASS，0 发现**（无 P0 / P1 / P2 / P3）：

> No actionable regressions were found. The bounded-read path rejects oversized unknown-length bodies while preserving valid-request and unlimited-mode behavior; all 19 targeted tests passed.

即 codex 确认：限界读取路径正确拒绝超大未知长度 body，同时保留合法请求与不限制（`maxCacheSize <= 0`）模式行为，web 19 例通过。**r0 的 1 项 P1 已消除。**（codex 沙箱对部分模块 `.m2` 仍 `AccessDeniedException`，属 Windows 沙箱环境限制、非本提交缺陷；测试通过性以本机 22/22 `BUILD SUCCESS` 为准。）

## 其它判断点复核（评审关注项，非 codex 发现）

- **`parseTenantIdHeader` 对 Unicode 十进制数字的接受性**：`Character.isDigit(char)` 与 `Long.parseLong` 内部 `Character.digit` 均接受非 ASCII 的 Unicode Nd 数字（如阿拉伯-印度数字），故 `"١٢٣"` 会被解析为 `123L` 而非拒绝。复核判定：最终归一到同一 `Long`、不产生越权（租户 ID 语义不变），非安全缺陷；若后续要求上下文头严格 ASCII，可在 ZS-SEC-012.B 真实链夹具补 codepoint < 128 断言，本轮不改（避免过度设计）。
- **过滤器异常统一出口覆盖**：`TenantContextWebFilter` 就地 catch `ServiceException` 转 `writeJSON`；`parseTenantIdHeader` 只抛受控 `ServiceException`（不抛裸 `NumberFormatException`），故 catch 面完备。其余 MVC 外过滤器（认证 / 加密）的同类逃逸不在 SEC-008 范围。

## 结论

**评审通过（r0 发现 1 × P1，已修复；r1 复核 0 发现，确认消除）。**

- **r0**（被审 `76da2a2f`）：FAIL，1 × P1——chunked / 未知长度 body 绕过声明式 `Content-Length` 早拒，认证前无界缓冲可耗尽堆（DoS）。
- **修复**（`3f4fa736`）：限界读取两道防线 + 2 回归测试 + 设计文档同步，逐条对应 codex 建议；本机 22 测试全绿 + 门禁 10/10 + verify-docs 0 issue。
- **r1**（被审 `3f4fa736`）：PASS，0 发现，codex 确认 P1 已消除、合法与不限制模式行为无回归。
- SEC-008 主卡维持**待验收**：P1 修复属首轮交付的完善，并入 docs/05 V1.17 不升版；网关 / 容器层大小与超时限制、真实链端到端拒绝仍归 ZS-SEC-012.B，限流归 ZS-SEC-010、ID/时间/分页/版本合同归 ZS-SEC-009。
- codex 发现总数 **1**（1 × P1），已修复 **1**，仍有效 **0**。
