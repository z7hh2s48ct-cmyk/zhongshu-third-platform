# 评审交接：ZS-SEC-012.A（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-012.A.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-012.A.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-012.A（05 文档 16.1 节，B03 批次） |
| 交付 | 真实 Filter/MVC/方法权限失败夹具 |
| 待评审提交 | `a4d9c6e1` |
| 评审命令 | `codex review --commit a4d9c6e1` |
| 变更规模 | 10 files changed, 1159 insertions(+), 7 deletions(-) |
| 本地验证 | BUILD SUCCESS，Tests run: 31, Failures: 0, Errors: 0, Skipped: 0（2026-09-09T12:52:10+08:00） |
| 复现入口 | `mvn -f services/zhongshu-core/pom.xml -pl :zszj-spring-boot-starter-biz-tenant -Dtest=SecurityFilterChainFixtureTest test` |

## 2. 变更文件清单（仅这 10 个，未含其他窗口改动）

新增（`zszj-spring-boot-starter-biz-tenant/src/test/`）：
- `java/cn/zszj/framework/security/SecurityFilterChainFixtureTest.java`（主测试，7 组 31 用例）
- `java/cn/zszj/framework/security/fixture/SecurityFixtureApplication.java`（装配入口）
- `java/cn/zszj/framework/security/fixture/TenantFixtureConfiguration.java`
- `java/cn/zszj/framework/security/fixture/TestControllers.java`
- `java/cn/zszj/framework/security/fixture/MockOAuth2TokenApi.java`
- `java/cn/zszj/framework/security/fixture/MockPermissionApi.java`
- `java/cn/zszj/framework/security/fixture/MockTenantFrameworkService.java`
- `resources/application-fixture.yaml`（`spring.autoconfigure.exclude` 清单 + `mock-enable=false`）

修改：
- `zszj-spring-boot-starter-security/pom.xml`（回退死依赖 `spring-boot-starter-test`，来自上一提交 `45c21323`）
- `docs/05-底座模块分析与开发任务清单.md`（ZS-SEC-012 卡追加 .A 开发记录）

> ⚠️ 工作树中 `scripts/brand/*`（M）与 `_chk.mjs`/`_probe.mjs`/`_v.json`（??）**不属于本提交**，为其他窗口 hotfix-B 工作，评审时勿纳入 `a4d9c6e1` 范围。

## 3. 请重点核查的判断点（按验收红线 line 389）

1. **安全过滤器是否被为方便测试禁用**（红线）：确认 `zszj.security.mock-enable=false`，且
   TokenAuthenticationFilter / TenantSecurityWebFilter / Spring Security 链 / `@PreAuthorize` /
   GlobalExceptionHandler 均真实装配；`spring.autoconfigure.exclude` 排除的是**重型基础设施**
   （数据源/MyBatis/Redis/Quartz/租户自动配置等），而非安全过滤器本身。
2. **缺依赖是否静默跳过后报通过**（红线）：`ApiErrorLogCommonApi` 以 no-op lambda bean 提供
   （生产由 infra 提供，夹具无 infra）。请判断：no-op 是否掩盖真实行为？是否应改为断言其被调用？
   缺失该 bean 时上下文加载失败（非静默跳过）——此语义是否成立。
3. **夹具置于 biz-tenant 而非 security 的合理性**：为规避
   `security(test)→biz-tenant(compile)→security(compile)` 的 Maven reactor 循环。请核查该理由是否成立、
   是否有更合适的落点、security/pom.xml 回退是否干净（无残留未用依赖）。
4. **MockMvcBuilderCustomizer 设 `servletPath=requestURI`**：这是为还原生产 DispatcherServlet 行为
   （MockMvc 默认 servletPath 为空串，致 `WebFrameworkUtils.getLoginUserType` 返回 null、userType 校验被跳过）。
   请判断：此修正是**忠实还原生产**，还是**掩盖了产品在 servletPath 为空时的真实缺陷**？
5. **两个重写的租户校验用例**（disabled/expired/unknown 改用无登录公开端点 `/open/tenant-required`）：
   因 `TenantSecurityWebFilter` 对登录用户先做越权比对（403）再到 `validTenant`。请核查改用公开端点
   是否真正触达 `validTenant` 的租户合法性分支，断言的 code+msg 是否与生产一致。
6. **暴露的基线失败**：`crossTenantWithVisitPermission` 记录持 `system:tenant:visit` 后
   `SecurityFrameworkUtils.skipPermissionCheck()=true` 的越权放大现状。请确认该用例是「如实记录现状」
   而非「断言现状正确」，且注释明确指向 ZS-SEC-001.A 待收紧。

## 4. 已知边界（非缺陷，属分批范围）

- 本子项（.A）仅覆盖安全链主干 + 双技术租户；CORS / 文件错误 / 异步派发 / trace / 畸形 JSON /
  Web、移动端合同同步 / 真实 PG·Redis 场景归 **ZS-SEC-012.B**。
- 业务组织模型待 **D-09** 决策后追加，不在本夹具范围。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（31/31）+ 提交（`a4d9c6e1`）+ 回填 05 文档开发记录
- [ ] 左窗口：`codex review --commit a4d9c6e1` → 产出 `codex-ZS-SEC-012.A.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复
