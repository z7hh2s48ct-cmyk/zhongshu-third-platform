# 评审交接：ZS-SEC-002（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-002.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-002.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-002（05 文档 M03，B03 批次；前置 ZS-ENG-001/004/005 均已完成） |
| 交付 | 接口分类、匿名白名单与方法权限清单——静态扫描器 + 漂移基线 + 文档 + 运行时合同复用 SEC-012.A 夹具 |
| 交付形态 | 用户选定「静态清单 + 漂移基线」：遍历启用模块 Controller 源码生成全量清单、与已提交基线比对，漂移即失败要求评审 |
| 待评审提交 | `3fe87023` |
| 评审命令 | `codex review --commit 3fe87023` |
| 变更规模 | 9 files changed, 1017 insertions(+), 7 deletions(-) |
| 本地验证（后端） | zszj-server `Tests run: 8`（ApiInventoryTest 4 + ModuleWhitelistTest 4）；biz-tenant 夹具 `Tests run: 35`（含新增第 8 组 4 例），均 BUILD SUCCESS |
| 本地验证（门禁） | `node scripts/ops/run-local-gates.mjs --fast` 10/10；`node scripts/gov/verify-docs.mjs` issueCount 0 |
| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-server "-Dtest=ApiInventoryTest,ModuleWhitelistTest" test && mvn -pl :zszj-spring-boot-starter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest" test`（PowerShell 下按 tools/env.sh 复制 `$env:JAVA_HOME` 与 `$env:PATH`） |
| 解锁 | SEC-002 转待验收后，以其为前置的 ZS-SEC-003/004/005/008/011.A、ZS-CFG-002.B 获得开工条件；ZS-SEC-012.B（安全与双端联合验收）亦以其为前置 |

## 2. 变更文件清单（仅这 9 个，未含其他窗口改动）

清单扫描器与基线（`zszj-server/src/test/`）：
- `java/cn/zszj/server/ApiInventoryTest.java`（**新增**，413 行：遍历 `ModuleWhitelist.ENABLED_MODULES`=system/infra 的 Controller 源码，每端点编码「HTTP 方法｜路径｜主体｜授权类别｜权限｜模块｜Controller」；4 个 @Test——`inventoryMatchesBaseline` 漂移门禁 + 3 项结构断言 `everyEndpointHasKnownSubjectAndApiPrefix`/`permissionEndpointsHaveWellFormedPermission`/`anonymousWhitelistMatchesCatalogue`；`ANONYMOUS_CATALOGUE` 登记 25 匿名端点及理由）
- `resources/api-inventory-baseline.txt`（**新增**，331 行 = 3 行注释头 + 328 端点基线；`-Dapi.inventory.update=true` 评审通过后重新生成）

SEC-012.A 夹具扩展（`zszj-spring-boot-starter-biz-tenant/src/test/`）：
- `java/cn/zszj/framework/security/fixture/TestAppController.java`（**新增**，41 行：`/app-api/fixture/member/profile` MEMBER 主体端点，仅需登录无 @PreAuthorize，供双向串用验证）
- `java/cn/zszj/framework/security/fixture/TestControllers.java`（**+19**：`import Callable` + `/admin-api/fixture/async/profile` 受保护异步端点，返回 `Callable` 触发 ASYNC 派发）
- `java/cn/zszj/framework/security/SecurityFilterChainFixtureTest.java`（**+64**：`import request` + 第 8 组 `ApiClassificationContract` 4 例——无 token 异步端点首次 REQUEST 派发 401、有效 token `asyncStarted()`、ADMIN→/app-api 403、MEMBER→/app-api 200）

文档：
- `services/zhongshu-core/docs/接口清单与匿名白名单.md`（**新增**，136 行：主体推导机制/匿名三来源/328 端点统计与命令/25 匿名端点目录与理由/5 审查点/SEC-012.A 覆盖映射/漂移门禁评审流程/验收对齐/边界）
- `docs/05-底座模块分析与开发任务清单.md`（SEC-002 卡追加开发记录 + 状态 **待开发→待验收** + 第 2 节 V1.10 记录与统计 47/25→46/26 + 第 19 节 V1.10 行 + ENG-001 卡「工具链复验补录」）
- `README.md`（docs/05 索引版本 V1.9→V1.10）

> ⚠️ **drive-by 修复（改的是他任务既有测试，请重点核查）**：`zszj-server/src/test/java/cn/zszj/server/ModuleWhitelistTest.java`（**+6/-2**）——修复 ZS-ENG-001 既有 `enabledModulesMatchServerPomDependencies` 断言 bug（根因见 §3 判断点 6），**非 SEC-002 引入**，首次以工具链运行 zszj-server 全测试时暴露。
>
> ⚠️ 工作树中 `docs/reviews/codex-hotfix-*.raw.md`、`outputs/` 等（`??`）**不属于本提交**，为左窗口 codex 评审产物，评审时勿纳入 `3fe87023` 范围。

## 3. 请重点核查的判断点

1. **扫描器完整性（核心）**：`ApiInventoryTest` 静态遍历是否覆盖**全部**启用模块 Controller、无遗漏端点（328 是否为真实全集）？`@RequestMapping` 各简写（`@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`）、类级 `@RequestMapping` 前缀 + 方法级 path 拼接、`value=`/`path=` 两种写法是否都正确识别？有无多方法映射（`method={GET,POST}`）被漏计或重复计？

2. **授权类别三分准确性**：`ANONYMOUS`（@PermitAll）/`PERMISSION`（@PreAuthorize hasPermission/hasRole/hasScope）/`AUTHENTICATED`（两者皆无）判定是否准确？关键证据——`@PreAuthorize` 裸 grep=253 但扫描 PERMISSION=251，差 2 为 [OAuth2UserController](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/oauth2/OAuth2UserController.java) L32-33 **Javadoc 注释中的示例**（扫描器按行首 `*` 忽略）——请确认此忽略逻辑正确、无其他注释/字符串中的注解被误计，且无**真实**注解因换行/多注解同行被漏计。

3. **漂移门禁非空洞**：`inventoryMatchesBaseline` 是否真能检测新增/移除/变更端点并给出可供评审的差异（而非恒等通过）？`anonymousWhitelistMatchesCatalogue` 的**双向一致**（每个真实 @PermitAll 端点必在 `ANONYMOUS_CATALOGUE` 登记、目录每项必真实存在）是否成立？可否构造一个未登记匿名端点使断言失败以证非空洞？

4. **主体类型静态判定与运行期一致**：包路径 `**.controller.admin.**`→/admin-api→ADMIN、`**.controller.app.**`→/app-api→MEMBER 的静态判定，是否与运行期 [WebFrameworkUtils](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java)#getLoginUserType 依 servletPath 前缀推导一致？`everyEndpointHasKnownSubjectAndApiPrefix` 是否真验证了「主体 ∈ {ADMIN,MEMBER} 且与前缀一致」？

5. **匿名白名单收紧正当性 + 不越界**：25 匿名端点逐项理由是否成立、有无本应受保护却被误归 ANONYMOUS/AUTHENTICATED 的端点？租户 `simple-list`/`get-by-website` 仅 set `id`+`name`（TenantRespVO 敏感联系字段不填充）的判断是否准确？5 个残余审查点归属（SEC-006/LOGIN-004/FILE-001.A/领域/SEC-003）是否正确、**未越界替他任务下结论**？

6. **drive-by 修复 ModuleWhitelistTest 的正确性（重点，因改的是 ENG-001 既有测试）**：根因判断是否准确——`activeModuleArtifacts` 已用 `.replace("zszj-module-","")` 剥前缀返回 `{system,infra}`，而原断言期望侧 `ENABLED_MODULES.map(m->"zszj-module-"+m)`、实际侧却又 `serverDeps.filter(startsWith("zszj-module-"))` 在**已剥前缀**值上过滤致 actual 恒空 `[]`、断言必然失败？修复 `assertEquals(new LinkedHashSet<>(ModuleWhitelist.ENABLED_MODULES), serverDeps)` 是否与姊妹测试 `enabledModulesMatchRootPomActiveModules` 的剥前缀比对约定一致、**仍能验证「未启用模块不进 zszj-server 依赖闭包」**而非退化为空洞断言？此修复归属 ENG-001 是否恰当（SEC-002 顺带、已在 05 文档双卡记录）？

7. **夹具第 8 组有效性**：4 个新用例是否真验证了目标语义——`asyncEndpointWithoutTokenStillAuthenticated`（无 token 首次 REQUEST 派发 401「账号未登录」，证 `dispatcherTypeMatchers(ASYNC).permitAll()` 不泄漏到首次派发）、`asyncEndpointWithValidTokenStartsAsync`（有效 token `request().asyncStarted()`）、`adminTokenOnAppApiRejected`（ADMIN→/app-api 403）、`memberTokenOnAppApiAccepted`（MEMBER→/app-api code=0）？断言是否校验 code+msg（而非仅 HTTP 状态）？是否确触达此前**零覆盖**的 /app-api→MEMBER 推导分支？

8. **文档与代码一致性 + 边界**：[接口清单与匿名白名单](../../services/zhongshu-core/docs/接口清单与匿名白名单.md) 的统计（328；ANONYMOUS25/AUTHENTICATED52/PERMISSION251；ADMIN322/MEMBER6；system212/infra116）是否与基线文件、扫描器输出一致？运行时合同「复用 SEC-012.A 不重写」、ASYNC「分类完整性」vs SEC-012.B「全面 async/SSE 合同」的划分是否清晰不越界？

## 4. 已知边界（非缺陷，属分批范围）

- **ASYNC/SSE**：SEC-002 的 2 个异步用例仅证「分类完整性」（静态归为 AUTHENTICATED 的异步端点运行时不因 ASYNC permitAll 降级为匿名，首次 REQUEST 派发仍强制认证）；**全面** async/SSE 运行时合同（流式响应、异步异常出口、跨线程租户上下文传播、[Web]/[移动端] 合同同步、真实 PG/Redis）归 **ZS-SEC-012.B**。
- **对象授权轴**：对象级数据范围授权（dept/self 轴）归 **ZS-PERM-002.A**（`DeptDataPermissionChecker`）；SEC-002 清单标「方法权限轴」，夹具 `ObjectAuthorization` 组复用 SEC-012.A，不重复。
- **5 个残余审查点**（非 SEC-002 阻断项）：租户匿名枚举面→限流/日志归 ZS-SEC-006；自助注册开放策略/发码频率→ZS-LOGIN-004；私有文件主体绑定→ZS-FILE-001.A（票据/取流→ZS-FILE-004.A，B04）；短信回调验签→领域校验；OAuth2 凭据传输规范→ZS-SEC-003。
- **清单范围**：为「启用模块（system/infra）」的静态源码事实来源；未启用模块兜底路由由 ZS-ENG-001 `ModuleWhitelistTest` 治理，不入清单；后续启用新业务模块（member/mall/…）时清单随 `ENABLED_MODULES` 自动扩展并经漂移门禁评审。WebSocket（`AuthorizeRequestsCustomizer` 唯一实现）非 REST、无 admin/app 前缀，不在清单。
- **待验收语义**：本子项以静态清单（328 端点、4/4 结构断言）+ 漂移门禁 + 运行时合同复用（SEC-012.A 夹具 35/35，含新增 4 例）证明接口分类/匿名白名单/方法权限清单**已建立并可门禁化**；真实 PG + 完整多模块启用下的端到端联验随后续模块开发收口。SEC-002 **未拆分批子项**，主卡状态由 待开发 转 待验收（区别于 PERM-002.A/SEC-001.A 的 .A/.B 子项模式）。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（zszj-server 8/8 + biz-tenant 夹具 35/35 + 门禁 10/10 + verify-docs 0）+ 提交（`3fe87023`）+ 回填 05 文档 SEC-002 开发记录、V1.10 变更记录/统计、ENG-001 工具链复验补录 + README 索引同步
- [ ] 左窗口：`codex review --commit 3fe87023` → 产出 `codex-ZS-SEC-002.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复

## 6. 处置（左窗口回填）

> 待 codex 评审结论。P0/P1 缺陷在此登记处置方式与复验提交；无缺陷则记「r0 直接通过」。
