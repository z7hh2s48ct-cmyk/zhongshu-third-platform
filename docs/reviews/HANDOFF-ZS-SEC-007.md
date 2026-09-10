# 评审交接：ZS-SEC-007（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-007.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-007.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-007（统一访问、异常与保护切面的日志脱敏；关联 FND-AUTH-007/FND-CLIENT-001，WP-13/19，B03/B04 批次；优先级 P0，类别 改造；前置 ZS-ENG-004 已完成，复用 ZS-SEC-005 失败夹具思路） |
| 交付 | 共享脱敏工具 `LogSanitizeUtils`（四入口）+ 5 写点集成（卡片 4 泄露点 E39/E37/E49/E50 + 同类第 5 写点 ApiSignatureAspect 范围扩展）+ 日志脱敏策略文档 |
| 交付形态 | 建立单一共享脱敏策略：① 各处「脱敏失败回退原文」收敛为「只记摘要绝不回退」；② 敏感字段由「删除键」改为「掩码 `***` 保留字段名」以兼顾可定位性；③ 两处 filter 的「日志写入失败」外层 catch 不再 dump 整个未净化 DTO，改记 `url + traceId`（对应验收红线三） |
| 待评审提交 | `332b4d7c` |
| 评审命令 | `codex review --commit 332b4d7c` |
| 变更规模 | 15 files changed, 1345 insertions(+), 91 deletions(-) |
| 本地验证（后端） | 反应堆 9 模块 **BUILD SUCCESS**。SEC-007 新增测试 **19** 例全绿（Failures 0 / Errors 0 / Skipped 0）：common `LogSanitizeUtilsTest 10`（模块合计 41）、web `ApiAccessLogFilterTest 2 + GlobalExceptionHandlerTest 2`（模块合计 8）、protection `IdempotentAspectTest 2 + RateLimiterAspectTest 2 + ApiSignatureAspectTest 1`（模块合计 6，另既有 `ApiSignatureTest 1`） |
| 本地验证（门禁） | `node scripts/ops/run-local-gates.mjs --fast` 10/10；`node scripts/gov/verify-docs.mjs` issueCount 0 |
| 复现入口（后端） | `mvn -pl ':zszj-common,:zszj-spring-boot-starter-protection,:zszj-spring-boot-starter-web' -am test`（PowerShell 下按 [tools/env.sh](../../tools/env.sh) 设 `$env:JAVA_HOME`/`$env:PATH`；`-pl` 冒号列表须整体加引号防逗号被拆成数组） |
| 解锁 | SEC-007 转待验收后，「失败请求可定位且不泄密」的字段级日志脱敏能力就绪，与 SEC-005（结果码登记）、SEC-006（trace 关联）共同构成失败日志基线；异常日志的权限与保留期归 M08 ZS-AUDIT-002 |

## 2. 变更文件清单（仅这 15 个，未含其他窗口改动）

共享脱敏工具（`zszj-common/src/main/`）：
- [`java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java`](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java)（**新增 +248**：`@Slf4j` + 私有构造，不依赖 servlet/web，供 web 与 protection 两 starter 复用。四公开入口 `sanitizeJson(String,String...)` / `sanitizeMap(Map,String...)` / `sanitizeArgs(Object[],String...)` / `sanitizeResponseBody(Object,String...)`；`MASK="***"`（**本项由 private 提升为 public**，供切面就地掩码单个凭据派生值）、`MAX_LENGTH=2048`、`SENSITIVE_KEY_ROOTS`（password/passwd/pwd/token/secret/authorization/credential/privatekey/apikey/accesskey/secretkey/cookie/session/otp）；键归一 `normalizeKey`=小写去 `_` 去 `-`，根集 `contains` 模糊匹配 + `extraKeys` 精确匹配；`sanitizeNode` 数组逐元素 / 对象逐字段递归掩码；解析/序列化失败经 unparseable/unserializable 只 `log.warn` 摘要（原因类别 + 长度）**绝不回退原文**；超 2048 截断附 `...[truncated,total=N]`；`sanitizeResponseBody` 仅净化 `data` 保留 `code`/`msg`）

写点集成 — filter 双修复（`zszj-spring-boot-starter-web/src/main/`）：
- [`java/cn/zszj/framework/apilog/core/filter/ApiAccessLogFilter.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/apilog/core/filter/ApiAccessLogFilter.java)（**+5/-80 净删 75**：① 删除 73 行私有 `sanitizeMap`/`sanitizeJson(String)`/`sanitizeJson(CommonResult)`/`sanitizeJson(JsonNode)`——旧版脱敏失败时 `log.error("[sanitizeJson][脱敏({}) 发生异常]", jsonString, e)` 打印原文**且** `return jsonString` 回退原文写入访问日志 `requestParams`（**E39 泄露点**）——改调 `LogSanitizeUtils.sanitizeMap/sanitizeJson/sanitizeResponseBody`；② `createApiAccessLog` 外层 catch 由 `log.error("[createApiAccessLog][url({}) log({}) 发生异常]", uri, toJsonString(accessLog), th)`（dump 整个未净化 accessLog DTO）改为只记 `url + TracerUtils.getTraceId()`；③ 删除私有 `SANITIZE_KEYS` 常量与 CollUtil/ArrayUtil/JsonNode/Iterator/JsonUtils 相关导入）
- [`java/cn/zszj/framework/web/core/handler/GlobalExceptionHandler.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/handler/GlobalExceptionHandler.java)（**+4/-3**：① `buildExceptionLog` 的 `.put("query", ServletUtils.getParamMap(request))` / `.put("body", ServletUtils.getBody(request))`——原始 query/body 直写错误日志（**E37 泄露点**）——改为 `LogSanitizeUtils.sanitizeMap(...)` / `sanitizeJson(...)`；② `createExceptionLog` 外层 catch 由 `JsonUtils.toJsonString(errorLog)` dump 整个未净化 errorLog 改为只记 `url + TracerUtils.getTraceId()`；+import LogSanitizeUtils，TracerUtils 已有导入）

写点集成 — 保护切面（`zszj-spring-boot-starter-protection/src/main/`）：
- [`java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java)（**+2/-1**：重复请求 `log.info("[aroundPointCut][方法({}) 参数({}) 存在重复请求]", ...)` 由 `joinPoint.getArgs()` 改 `LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs())`（**E49**）；+import）
- [`java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect.java)（**+2/-1**：请求过于频繁 `log.info("[beforePointCut][方法({}) 参数({}) 请求过于频繁]", ...)` 同上改 `sanitizeArgs`（**E50**）；+import）
- [`java/cn/zszj/framework/signature/core/aop/ApiSignatureAspect.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/signature/core/aop/ApiSignatureAspect.java)（**+5/-2**，**同类第 5 写点·范围扩展**：① 签名失败拒绝路径 `log.error("[beforePointCut][方法{} 参数({}) 签名失败]", ...)` 由 `joinPoint.getArgs()` 改 `LogSanitizeUtils.sanitizeArgs(...)`；② 重复请求路径 `log.info("[verifySignature][appId({}) timestamp({}) nonce({}) sign({}) 存在重复请求]", ...)` 由 `clientSignature`（凭据派生值）改 `LogSanitizeUtils.MASK` 掩码，appId/timestamp/nonce 保留用于定位；+import + 安全注释。**仅改日志、不动验签逻辑**）

测试（6 新增测试类，共 19 例）：
- `zszj-common/src/test/`：[`LogSanitizeUtilsTest.java`](../../services/zhongshu-core/zszj-framework/zszj-common/src/test/java/cn/zszj/framework/common/util/log/LogSanitizeUtilsTest.java)（**新增 +199 / 10 例**：正常 JSON / 嵌套对象数组 / 大小写与下划线驼峰别名 / 凭据根集 / extraKeys 精确 / 非 JSON / 畸形 JSON 脱敏失败只记摘要不回退 / 超长截断 / sanitizeArgs / sanitizeResponseBody 仅净化 data 保留 code/msg）
- `zszj-spring-boot-starter-web/src/test/`：[`ApiAccessLogFilterTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/apilog/core/filter/ApiAccessLogFilterTest.java)（**新增 +121 / 2 例**：日志表 DTO 净化 + 写库失败不输出未净化对象）、[`GlobalExceptionHandlerTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/core/handler/GlobalExceptionHandlerTest.java)（**新增 +117 / 2 例**）
- `zszj-spring-boot-starter-protection/src/test/`：[`IdempotentAspectTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest.java)（**新增 +200 / 2 例**）、[`RateLimiterAspectTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest.java)（**新增 +198 / 2 例**）、[`ApiSignatureAspectTest.java`](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/signature/core/aop/ApiSignatureAspectTest.java)（**新增 +168 / 1 例**）——均 `@ExtendWith(MockitoExtension.class)` + Logback `ListAppender` 捕获日志，断言秘密值（`P@ssw0rd-SECRET-DoNotLog` / TOKEN / APIKEY 等）不出现、非敏感字段与方法描述保留、掩码 `***` 出现

文档：
- [`services/zhongshu-core/docs/日志脱敏策略.md`](../../services/zhongshu-core/docs/日志脱敏策略.md)（**新增 +68**：§1 共享工具四入口 / §2 脱敏算法五步 / §3 五写点落地矩阵 / §4 extraKeys 端点级 / §5 验收对齐 / §6 测试证据 / §7 边界与待验收；引用姊妹文档 [错误响应与状态码矩阵.md](../../services/zhongshu-core/docs/错误响应与状态码矩阵.md)）
- [`docs/05-底座模块分析与开发任务清单.md`](../05-底座模块分析与开发任务清单.md)（**+7/-3**：SEC-007 卡追加开发记录 + 状态 **待开发→待验收** + 第 2 节 V1.12 记录与统计 45/27→44/28 + 第 19 节 V1.12 行 + 版本头 V1.11→V1.12）
- [`README.md`](../../README.md)（**+1/-1**：docs/05 索引版本 V1.11→V1.12）

> ⚠️ 工作树中 `docs/reviews/codex-hotfix-*.raw.md`（左窗口 codex 评审产物）、`outputs/sketchup_villa_20260909/`（无关 3D 渲染输出）、`zszj-common/.../util/logging/.probe`（探针残留）等 `??` 文件**不属于本提交**，评审时勿纳入 `332b4d7c` 范围。

## 3. 请重点核查的判断点

1. **第 5 写点 ApiSignatureAspect 范围扩展的合理性（核心）**：卡片明列 4 泄露点（E39/E37/E49/E50），本项另纳入同属 protection 的 ApiSignatureAspect（签名失败 `log.error` 记 `getArgs`、重复请求 `log.info` 记 `sign`）。请判断：此扩展属「同模块同缺陷模式应一并收敛」的合理范围，还是应留独立卡片？签名失败路径 `getArgs` 是否确会含凭据（如 body 里的 password/token）？仅改日志不动验签逻辑是否确实无功能副作用？

2. **掩码 vs 删除的语义变更**：旧 ApiAccessLogFilter 对敏感键是 `iterator.remove()` / `MapUtil.removeAny`（键从日志消失），新 `LogSanitizeUtils` 是掩码 `***`（保留字段名、隐藏值）。请判断保留字段名是否更利于定位（可见「哪些字段被传了」）且不泄露值；有无场景删除更安全（如字段名本身即敏感）？

3. **「脱敏失败绝不回退原文」的完备性**：`LogSanitizeUtils` 在 JSON 解析失败（unparseable）/ 对象序列化失败（unserializable）时只 `log.warn` 摘要（原因类别 + 长度）。请核查所有失败分支是否都不返回原始入参；被脱敏源本身含秘密时，摘要（长度/原因）是否可能间接泄露（长度侧信道）？`sanitizeArgs` 中单个元素序列化失败是否降级为占位而非抛出、不影响其余元素？

4. **两处 filter 外层 catch 改记 traceId 的充分性**：ApiAccessLogFilter 与 GlobalExceptionHandler 的「日志写入失败」catch 由 dump 整个 accessLog/errorLog 对象改为只记 `url + TracerUtils.getTraceId()`。请判断：(a) 是否确实满足验收红线「日志写入失败不再次输出未净化对象」；(b) 仅凭 url+traceId 是否足以定位失败（关联 ID 是否够，是否丢失了必要的排障信息）；(c) traceId 在无有效 Span 时返回空串（见 SEC-006 卡片 E41），此时可定位性是否退化——该边界是否应明确归 SEC-006？

5. **键归一化与模糊匹配的覆盖与误伤**：`normalizeKey`（小写去 `_`/`-`）+ 根集 `contains` 模糊匹配。请核查：(a) 大小写/驼峰/下划线别名（`Password`/`accessToken`/`user_password`/`X-Api-Key`）是否都被覆盖；(b) `contains` 模糊匹配是否会误伤合法非敏感字段（如字段名恰含 `token` 子串的 `tokenizer`、含 `ass` 的 `assignment`、含 `pwd` 的... ），造成过度脱敏影响可诊断性？根集词表的选择是否需在策略文档标注误伤权衡？

6. **extraKeys 端点级扩展的匹配语义一致性**：四入口均接受可变 `String... extraKeys` 作**精确**匹配补充（ApiAccessLogFilter 传入注解 `sanitizeKeys`），而根集是**归一 contains 模糊**匹配。请判断两套匹配语义并存是否会造成理解偏差？extraKeys 是否也应归一化后再匹配（否则 `Password` 作 extraKey 无法命中 `password` 字段）？

7. **测试有效性与护栏成立性**：19 例是否真验证目标语义（秘密不出现 + 安全字段保留 + 掩码出现 + 失败不回退 + 截断 + responseBody 仅净化 data）？protection/web 用 Logback `ListAppender` 捕获日志断言——是否覆盖了「脱敏器自身抛异常」的分支？common 的 10 例是否覆盖了非 JSON / 畸形 JSON / 深层嵌套数组边界？`ApiSignatureAspectTest` 用真实 `FixedSignature` 实现（Mockito 无法 stub `toString`）是否确实验证了签名失败路径？

8. **文档与代码一致性 + 边界**：[日志脱敏策略.md](../../services/zhongshu-core/docs/日志脱敏策略.md) 的四入口/五步算法/五写点矩阵/测试证据是否与 `LogSanitizeUtils` 实际实现、5 写点 diff 一致？「日志体积上限 2048」「敏感端点默认不记正文（`responseEnable` 默认 false）」是否与代码一致？docs/05 V1.12 统计 45/27→44/28 与卡片状态是否自洽？

## 4. 已知边界（非缺陷，属分批范围）

- **运行时端到端验证**：真实 PG/Redis 与完整多模块启用下、经真实 HTTP 请求触发正常/畸形 JSON/脱敏器失败/业务系统错误/限流/重复提交时「秘密不落应用日志与日志表与响应」的运行时验收归 **B03/B04** 批次；本项以组件级 `ListAppender` 捕获 + 静态实证证明，未读取真实请求日志或认定生产已泄露（与卡片现状描述一致）。
- **异常日志的权限与保留期**：错误日志/访问日志表的访问权限控制与保留期策略归 **M08 ZS-AUDIT-002**，本项只做字段级脱敏，不涉日志存储的权限/留存（卡片调整项已明示）。
- **trace 关联 ID**：无链路代理时可关联请求标识的完整方案归 **ZS-SEC-006**；本项外层 catch 复用既有 `TracerUtils.getTraceId()` 作为关联锚点，不重写 trace 注入，亦不处理无 Span 时 traceId 为空的兜底。
- **参数校验（query/path）**：`MethodArgumentTypeMismatchException` 等参数校验、上下文头解析与请求资源限制归 **ZS-SEC-008**；本项只脱敏「已被记录的参数值」，不改校验/回显/资源限制逻辑。
- **MASK 可见性提升的兼容面**：`LogSanitizeUtils.MASK` 由 private→public 仅增访问性、严格向后兼容；唯一消费方为 protection 的 ApiSignatureAspect，已由 protection 重编译 + 6 测试验证。biz-tenant 等其它模块非写点、零改动，未单独重跑（避免触发其 DB/Redis 环境依赖的无关失败）。
- **待验收语义**：本项未拆 .A/.B 子项，主卡状态由 待开发 转 待验收（同 SEC-005 模式）；真实 PG/Redis 与完整多模块启用下的端到端联验随后续批次收口。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（反应堆 9 模块 BUILD SUCCESS，SEC-007 新增 19 测试全绿：common LogSanitizeUtilsTest 10 / web 4 / protection 5；门禁 `--fast` 10/10 + verify-docs 0 issue）+ 提交（`332b4d7c`）+ 回填 05 文档 SEC-007 开发记录、V1.12 变更记录/统计 45/27→44/28 + README 索引同步 + 新增日志脱敏策略文档
- [ ] 左窗口：`codex review --commit 332b4d7c` → 产出 `codex-ZS-SEC-007.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复

## 6. 处置（左窗口回填）

> 待 codex 评审结论。P0/P1 缺陷在此登记处置方式与复验提交；无缺陷则记「r0 直接通过」。
