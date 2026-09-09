# 评审交接：ZS-SEC-005（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-005.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-005.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-005（05 文档 M03，B03/B04 批次；前置 ZS-SEC-002 已完成） |
| 交付 | 统一错误响应、HTTP 状态与失败日志结果——统一 filter-direct 写出口 + 畸形 JSON 归 400 去敏感回显 + 状态码矩阵文档 + SEC-012.A 夹具第 9 组 |
| 交付形态 | 用户选定「方案A 非破坏」：保留平台既有 **HTTP 200 传输 + 业务码**契约（两端请求层依赖），**不改真实 HTTP 状态码**；仅统一 filter 直接写出错误的出口，使访问日志按业务码记录结果 |
| 待评审提交 | `e2b40185` |
| 评审命令 | `codex review --commit e2b40185` |
| 变更规模 | 13 files changed, 299 insertions(+), 20 deletions(-) |
| 本地验证（后端） | biz-tenant `Tests run: 43`（含新增第 9 组 `UnifiedErrorResponse` 6）、web `Tests run: 4`（ApiEncryptTest 3 + DesensitizeTest 1），均 BUILD SUCCESS |
| 本地验证（门禁） | `node scripts/ops/run-local-gates.mjs --fast` 10/10；`node scripts/gov/verify-docs.mjs` issueCount 0 |
| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-spring-boot-starter-web,:zszj-spring-boot-starter-biz-tenant -am test`（PowerShell 下按 tools/env.sh 复制 `$env:JAVA_HOME` 与 `$env:PATH`） |
| 解锁 | SEC-005 转待验收后，以其为前置/共用失败夹具的 ZS-SEC-006（trace 关联）、ZS-SEC-007（日志脱敏）、ZS-SEC-008（参数校验）、ZS-SEC-010（限流）获得开工条件 |

## 2. 变更文件清单（仅这 13 个，未含其他窗口改动）

统一出口助手（`zszj-spring-boot-starter-web/src/main/`）：
- `java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java`（**+18**：新增 `writeJSON(request, response, result)` 统一出口 = `setCommonResult` 登记 `common_result` 请求属性 + `ServletUtils.writeJSON` 写体；新增 `ServletUtils`/`HttpServletResponse` 导入。助手置于 web starter 而非 zszj-common，因 common 不能反向依赖 web，而 web 可同时依赖 common 的 `ServletUtils`+`CommonResult`）

filter-direct 写点迁移（6 文件 8 点，`ServletUtils.writeJSON(response, X)` → `WebFrameworkUtils.writeJSON(request, response, X)`）：
- `zszj-spring-boot-starter-security/src/main/`：`.../security/core/filter/TokenAuthenticationFilter.java`（**+1/-2**，认证 401，删 `ServletUtils` 导入）、`.../security/core/handler/AccessDeniedHandlerImpl.java`（**+2/-2**，权限 403 `FORBIDDEN`，换导入）、`.../security/core/handler/AuthenticationEntryPointImpl.java`（**+2/-2**，认证 401 `UNAUTHORIZED`，换导入）
- `zszj-spring-boot-starter-biz-tenant/src/main/`：`.../tenant/core/security/TenantSecurityWebFilter.java`（**+4/-4**，租户不匹配 403 / 租户标识缺失 400 / `allExceptionHandler` 三点，换导入）
- `zszj-spring-boot-starter-web/src/main/`：`.../web/core/filter/DemoFilter.java`（**+1/-2**，演示模式 901 `DEMO_DENY`，删导入）、`.../encrypt/core/filter/ApiEncryptFilter.java`（**+2/-2**，加密异常 `allExceptionHandler`，换导入）

畸形 JSON 归 400 + 去敏感回显（`zszj-spring-boot-starter-web/src/main/`）：
- `java/cn/zszj/framework/web/core/handler/GlobalExceptionHandler.java`（**+5/-2**：`methodArgumentTypeInvalidFormatExceptionHandler`——`InvalidFormatException` 分支改回显期望类型 `getTargetType().getSimpleName()` 而非原始入参 `getValue()`；fall-through 从 `defaultExceptionHandler`（500 系统异常）改 `BAD_REQUEST`（400）「无法解析请求体，请检查是否为合法 JSON」。`ServletUtils` 导入保留，其余分支仍用）

SEC-012.A 夹具扩展（`zszj-spring-boot-starter-biz-tenant/src/test/`）：
- `java/cn/zszj/framework/security/fixture/TestControllers.java`（**+37**：新增 `@PermitAll` 的 `POST /public/body` 端点 + `BodyReq` DTO{`String name`；`Integer secretPin`}，`secretPin` 传非数字字符串触发 `InvalidFormatException`）
- `java/cn/zszj/framework/security/SecurityFilterChainFixtureTest.java`（**+111**：第 9 组 `UnifiedErrorResponse` 6 例 + `commonResultCode(int)` 私有助手 + 5 导入）

文档：
- `services/zhongshu-core/docs/错误响应与状态码矩阵.md`（**新增 +108**：§1 错误响应契约 3 出口 / §2 业务码字典 14 码 / §3 HTTP 传输状态×业务码矩阵 13 场景含「访问日志修复前→后」列 / §4 filter-direct 出口统一 6 文件 8 点 / §5 畸形 JSON before-after / §6 两端请求层零改动映射 / §7 验收对齐 / §8 边界）
- `docs/05-底座模块分析与开发任务清单.md`（**+7/-3**：SEC-005 卡追加开发记录 + 状态 **待开发→待验收** + 第 2 节 V1.11 记录与统计 46/26→45/27 + 第 19 节 V1.11 行）
- `README.md`（**+1/-1**：docs/05 索引版本 V1.10→V1.11）

> ⚠️ 工作树中 `docs/reviews/codex-hotfix-*.raw.md`、`outputs/` 等（`??`）**不属于本提交**，为左窗口 codex 评审产物，评审时勿纳入 `e2b40185` 范围。

## 3. 请重点核查的判断点

1. **方案A 非破坏取舍的正确性（核心）**：保留 HTTP 200 传输 + 业务码、**不改真实 HTTP 状态码**是否是正确决策？关键证据——[Web service.ts](../../apps/zhongshu-admin-web/src/config/axios/service.ts) 的 `code===401` 无感刷新逻辑位于 axios **response 拦截器成功回调（仅 2xx 触发）**内，error 回调无刷新逻辑；若改真实 HTTP 401（方案B）则刷新链断裂。请确认此判断准确，且方案A 未牺牲任何应得的语义（业务码已刻意镜像 HTTP 语义，唯 SUCCESS=0）。

2. **统一出口覆盖完整性**：`WebFrameworkUtils.writeJSON` 迁移的 6 文件 8 点是否为**全部** filter-direct（过滤器/安全处理器内直接写响应、未经 MVC `GlobalResponseBodyHandler` 登记 `common_result`）写点？有无遗漏（如其它 starter 的 filter、限流/幂等切面的拒绝写出）？请全仓复核 `ServletUtils.writeJSON(response` 残留调用点是否都应迁移或确属不应登记访问日志结果的场景。

3. **访问日志记录链路真实性**：`setCommonResult` 登记的 `common_result` 请求属性是否真被 [ApiAccessLogFilter](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/apilog/core/filter/ApiAccessLogFilter.java)#buildApiAccessLog 读取（为 null 且无异常时才落 SUCCESS）？`ApiEncryptFilter` 用 `HttpServletRequestWrapper` 包装请求，其 `setAttribute` 是否委托根请求、不影响属性可见性？迁移后 401/403/429/5xx 是否确实不再误记为成功？

4. **畸形 JSON 归 400 的边界**：仅改 `HttpMessageNotReadableException`（JSON body 不可解析）处理器，**未动** `MethodArgumentTypeMismatchException`（query/path 参数类型不匹配，仍回显 `ex.getMessage()`）是否恰当？后者是否也应收敛（归属本项还是 ZS-SEC-007/008）？「Required request body is missing」分支保留 400 是否正确？

5. **去敏感回显充分性**：`getTargetType().getSimpleName()` 是否确实不含原始入参值？有无其它错误分支仍回显 `getValue()`/`ex.getMessage()`/原始报文而泄露敏感数据？400 文案「无法解析请求体」是否既不外泄报文细节、又足以指导客户端？

6. **夹具第 9 组有效性 + 护栏成立性**：6 用例（认证 401 / 租户缺失 400 / 租户不匹配 403 三点 filter-direct 登记 `common_result`、畸形 JSON 400、非法格式不回显敏感值 `PIN-*-SECRET`、403 跨层一致）是否真验证目标语义？`commonResultCode(int)` 助手以 `WebFrameworkUtils.getCommonResult(result.getRequest())` 断言——**ApiAccessLogFilter 不在 SEC-012.A 夹具链**，用「访问日志将读取的值」作回归护栏是否成立、是否需补真实 ApiAccessLogFilter 装配（归 ZS-SEC-012.B）？

7. **两端零改动结论的证据充分性**：[Web service.ts](../../apps/zhongshu-admin-web/src/config/axios/service.ts)（axios，`code!==0&&code!==200` 通用提示、`code===401` 刷新、`code===500/901` 特判）与 [移动端 http.ts](../../apps/zhongshu-miniapp/src/http/http.ts)（uni.request，`statusCode===401||code===401` 双向兼容）是否确实无需改动即兼容本项后端变更？有无被忽略的分支（如 blob 下载、SSE）？

8. **文档与代码一致性 + 边界**：[错误响应与状态码矩阵](../../services/zhongshu-core/docs/错误响应与状态码矩阵.md) 的业务码字典（14 码）、13 场景矩阵、6 文件 8 点表是否与 `GlobalErrorCodeConstants`、实际改动一致？「不承诺所有请求强行包 CommonResult（文件/二进制/SSE 单列）」的边界是否清晰不越界？

## 4. 已知边界（非缺陷，属分批范围）

- **真实 HTTP 状态语义**：方案A 刻意保留 HTTP 200，不改为真实 4xx/5xx；真实环境两端刷新/跳转/提示/下载的全面运行时验收归 **ZS-SEC-012.B**（安全与双端联合验收），本项以夹具 + 静态实证证明跨层一致语义与访问日志登记。
- **不强行包 CommonResult**：文件、二进制、SSE 流式响应单列，不承诺全部请求都包 `CommonResult`（与 05 文档 SEC-005 验收红线一致）。
- **日志脱敏 / trace 关联**：应用日志与日志表的字段级脱敏归 **ZS-SEC-007**；无链路代理时的可关联请求标识归 **ZS-SEC-006**；本项只统一「结果码登记」与「畸形 JSON 归类」，不重写脱敏/trace。
- **参数类型不匹配（query/path）**：`MethodArgumentTypeMismatchException` 未在本项收敛（属 query/path 参数、非 JSON body 畸形），留待 ZS-SEC-008 参数校验专项评估。
- **待验收语义**：本项未拆分批子项，主卡状态由 待开发 转 待验收（区别于 PERM-002.A/SEC-001.A 的 .A/.B 子项模式）；真实 PG/Redis 与完整多模块启用下的端到端联验随后续批次收口。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（biz-tenant 43/43 含 `UnifiedErrorResponse` 6/6 + web 4/4 + 门禁 10/10 + verify-docs 0 issue）+ 提交（`e2b40185`）+ 回填 05 文档 SEC-005 开发记录、V1.11 变更记录/统计 46/26→45/27 + README 索引同步
- [ ] 左窗口：`codex review --commit e2b40185` → 产出 `codex-ZS-SEC-005.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复

## 6. 处置（左窗口回填）

> 待 codex 评审结论。P0/P1 缺陷在此登记处置方式与复验提交；无缺陷则记「r0 直接通过」。
