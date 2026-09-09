# 评审交接：ZS-SEC-001.A（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-SEC-001.A.md`（人工复核 + codex 原始结论）与 `codex-ZS-SEC-001.A.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-SEC-001.A（05 文档 16.1 节，B03 批次；前置 ZS-DB-018、ZS-SEC-012.A 均已完成） |
| 交付 | 禁用旧跨租户权限跳过（config 门控默认关闭 + 前后端共同收口） |
| 待评审提交 | `5b8c702e` |
| 评审命令 | `codex review --commit 5b8c702e` |
| 变更规模 | 12 files changed, 145 insertions(+), 24 deletions(-) |
| 本地验证（后端） | BUILD SUCCESS，Tests run: 33, Failures: 0, Errors: 0, Skipped: 0（2026-09-09T13:26:09+08:00；31 fixture + 2 门控证明） |
| 本地验证（前端） | admin-web `verify-ts-baseline.mjs` 通过（currentErrors 11 = baseline 11，newErrors 0）；miniapp `vue-tsc --noEmit` 0 错误 |
| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-spring-boot-starter-biz-tenant "-Dtest=SecurityFilterChainFixtureTest,CrossTenantVisitEnabledFixtureTest" test` |
| 复现入口（前端） | `node scripts/client/verify-ts-baseline.mjs`（admin-web）；`apps/zhongshu-miniapp` 内 `vue-tsc --noEmit` |
| 解锁 | 本子项退出（无 D-09 依赖）后解锁 PERM 系列 → CLIENT 系列 → BRAND-003.B |

## 2. 变更文件清单（仅这 12 个，未含其他窗口改动）

后端修改（`zszj-spring-boot-starter-biz-tenant/src/main/`）：
- `java/cn/zszj/framework/tenant/config/TenantProperties.java`（新增 `visitEnable`，`zszj.tenant.visit-enable`，默认 **false**）
- `java/cn/zszj/framework/tenant/core/web/TenantVisitContextInterceptor.java`（全仓唯一 `setVisitTenantId` 点加门控，默认关闭时 403 + log.warn）

后端测试（`zszj-spring-boot-starter-biz-tenant/src/test/`）：
- `java/cn/zszj/framework/security/SecurityFilterChainFixtureTest.java`（翻转 CrossTenantAccess 组 2 用例：skip=true 基线 → 403 拒绝）
- `java/cn/zszj/framework/security/CrossTenantVisitEnabledFixtureTest.java`（**新增**，`visit-enable=true` 门控可逆性证明，2 用例）

前端 admin-web：
- `.env`（新增 `VITE_APP_TENANT_VISIT_ENABLE=false`）
- `types/env.d.ts`（补 `VITE_APP_TENANT_VISIT_ENABLE` 类型声明）
- `src/config/axios/service.ts`（门控 `visit-tenant-id` 头注入）
- `src/layout/components/ToolHeader.vue`（门控 `TenantVisit` 切换入口 UI）

前端 miniapp：
- `env/.env`（新增 `VITE_APP_TENANT_VISIT_ENABLE=false`）
- `src/http/interceptor.ts`（门控 `visit-tenant-id` 头注入）
- `src/pages/user/index.vue`（门控 `TenantVisitPicker` 切换入口 UI）

文档：
- `docs/05-底座模块分析与开发任务清单.md`（ZS-SEC-001 卡追加 .A 开发记录）

> ⚠️ 工作树中 `docs/reviews/codex-hotfix-B*.raw.md`（??）**不属于本提交**，为左窗口 hotfix-B 评审工作，评审时勿纳入 `5b8c702e` 范围。

## 3. 请重点核查的判断点（按验收红线 line 308/309）

1. **单一控制点是否真的掐断整条放大链**（核心）：`TenantVisitContextInterceptor.preHandle` 是否确为全仓
   唯一 `setVisitTenantId` 点？门控后 visitTenantId 永不设置 → `SecurityFrameworkUtils.skipPermissionCheck()`
   恒 false、`TenantContextHolder` 不切换 → `DeptDataPermissionRule` 数据范围正常。请核查是否存在**其它**
   设置 visitTenantId 或触发 skip 的路径（反序列化、其它拦截器/过滤器、LoginUser 构造点）被遗漏。
2. **是否落入 line 308 警告的陷阱**：本方案**未改** skipPermissionCheck / SecurityFrameworkServiceImpl /
   DeptDataPermissionRule，而是门控其上游触发条件。请确认这不是「简单把所有跳过改成原租户 RBAC 而误当完整
   跨组织方案」——默认关闭是「关闭旧放大能力」，而非「伪装成完整跨组织授权方案」。
3. **门控位置的正确性**：门控置于「visitTenantId==当前租户」早返回之后、loginUser 空检查之前。请核查：
   ① 同租户 visit 头（visit==login 租户）仍 200 放行（早返回先于门控）；② 匿名请求携伪造 differing visit 头
   是否也被门控 403（因门控在 loginUser 空检查之前）；③ 门控先于 `system:tenant:visit` 权限校验，故持旧权限者亦 403。
4. **403 vs 静默忽略的选择**：默认关闭时选择 REJECT(403)+log.warn 而非 IGNORE(丢弃头继续)。请判断是否符合
   line 309「伪造头…拒绝」与 line 389「拒绝并可追踪」；IGNORE 会更安全还是更危险（是否掩盖攻击探测）。
5. **门控是配置开关而非硬删除**：`CrossTenantVisitEnabledFixtureTest` 以 `visit-enable=true` 证明旧链路可恢复。
   请核查：① 证明用例是否恰当（获批 visitor → skip=true 旧放大行为；无权限者 → 仍 403「您无权切换租户」，
   原权限校验未失效）；② 保留可恢复开关是否有被生产误设为 true 的风险，默认 false + 文档警示是否足够。
6. **翻转 012.A 基线用例的正确性**：012.A 曾如实记录 `crossTenantWithVisitPermission`→skip=true 为「暴露的
   基线失败」。本子项收紧为 403。请确认翻转后断言（code+msg「跨租户访问能力未启用，禁止切换租户」）与生产
   拦截器抛出一致，且未削弱 012.A 其余 29 用例的覆盖。
7. **前端收口是否形成真纵深防御**：前端门控（不发头 + 隐藏 UI）在后端门控之外。请判断：① 即使前端被绕过
   （直接构造 `visit-tenant-id` 头），后端 403 是否兜底（应为是）；② 前端默认关闭是否影响合法多租户
   `tenant-id` 头（不应影响，仅门控 `visit-tenant-id`）；③ 关闭前遗留在 storage 的存量 visitTenantId 是否因
   头注入门控而失效（应为是）。

## 4. 已知边界（非缺陷，属分批范围）

- 本子项（.A）仅**关闭旧放大能力**，line 309 明确「不将关闭旧能力当作业务授权已完成」。
- 获批业务组织方案（**D-09**）后的**受控跨组织授权**（服务端授权记录/策略限制目标租户·对象·动作·字段·
  有效期 + 目标状态校验 + 原主体/目标/理由/结果审计）与**正向授权矩阵**归 **ZS-SEC-001.B**。
- `visit-enable=true` 时恢复的仍是**旧放大行为**（skipPermissionCheck 全放行），仅作门控可逆性证明，
  **非** .B 的受控授权，勿据此认为跨组织授权已实现。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（后端 33/33、前端 admin-web ts-baseline 0 新增 + miniapp vue-tsc 0 错）+ 提交（`5b8c702e`）+ 回填 05 文档开发记录
- [ ] 左窗口：`codex review --commit 5b8c702e` → 产出 `codex-ZS-SEC-001.A.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复
