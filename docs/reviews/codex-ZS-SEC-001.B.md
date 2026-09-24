# ZS-SEC-001.B 评审处置（获批跨组织授权记录/策略：CodeReview 独立复审 PASSED / 0×P0/P1）

- 评审对象：ZS-SEC-001.B 获批跨组织授权实现与正向矩阵——以服务端授权记录/策略取代 ZS-SEC-001.A 关闭的旧越权放大链（B08 Wave3；docs/05 line326-333 + §16.1 line1014 子项，前置 ZS-SEC-001.A〔配置门控关闭旧放大〕+ ZS-IAM-002〔组织/任职模型 + 服务端组织上下文〕+ ZS-PERM-002.B〔org 轴范围解析/对象检查入口〕+ D-09 均已交付）
- 隔离分支：worktree `.wt/zszj-wt-sec-001-b` 分支 `feat/sec-001-b`（自 main `1337c19f`；feat `ba7ee11b` + test `6ea90774`）
- 合并：`--no-ff` main `53e05826`（25 files +1993/-41，feat 分支自 `1337c19f` 线性领先、干净合并）
- 卡片性质：**产线安全能力实现卡**——新增持久化授权记录表 + 10 步 fail-closed 判定引擎 + 拦截器/功能权限层收敛接线，属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**CodeReview 独立复审 PASSED（0×P0 / 0×P1）**——六大安全不变量全部确认落实；余 1×P2（pre-existing 边界，本 PR 未改）+ 2×P3（后续硬化项，reviewer 明确不阻塞合并）
- 原始日志：`outputs/_sec001b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因（ZS-SEC-001.A line21 移交 + CrossTenantVisitEnabledFixtureTest line24-26 技术债登记）

旧越权放大链：`TenantVisitContextInterceptor.preHandle`（biz-tenant，全仓唯一 `setVisitTenantId` 点）→ visit-enable=true 且持粗粒度权限 `system:tenant:visit` → `loginUser.setVisitTenantId` + `TenantContextHolder.setTenantId` → `SecurityFrameworkUtils.skipPermissionCheck()`（visitTenantId≠tenantId 即 true）→ `SecurityFrameworkServiceImpl.hasAnyPermissions/hasAnyRoles/hasAnyScopes` **一律 return true**（跳过全部功能权限）→ 数据范围不追加。ZS-SEC-001.A 已默认关闭（`visit-enable=false` 时任何跨租户切换 403），但**未改** `skipPermissionCheck`/`SecurityFrameworkServiceImpl`（显式留给 .B）。即「持访问入口权限 = 自动获得目标租户全部动作/字段」，违背 docs/05 line331「即使具备访问入口权限，也不能自动获得所有目标动作/敏感字段」。

## 交付内容（feat `ba7ee11b`）

**framework-common（SPI 契约，循 PermissionCommonApi/AuditPort 先例）**：
- `CrossOrgVisitApi#authorizeCrossOrgVisit(CrossOrgVisitCheckReqDTO) → CrossOrgVisitDecisionDTO`（包 `cn.zszj.framework.common.biz.system.permission`）；
- `CrossOrgVisitCheckReqDTO`（visitorUserId/visitorTenantId/targetTenantId/action/objectOrgId/requestedFields）、`CrossOrgVisitDecisionDTO`（authorized/reason/targetTenantId/targetOrgIds/allowedActions/allowedFields，授权范围快照折叠进决策体）；
- `AuditEventTypes` 增 `CROSS_ORG_VISIT_GRANTED` / `CROSS_ORG_VISIT_DENIED`。

**security starter**：
- `CrossOrgVisitScopeHolder`（新，util）：`LoginUser.context`（CONTEXT_KEY=`crossOrgVisitScope`）授权范围快照 `set/get/clear` + 动作/对象/字段三维裁决（`isAnyActionAllowed`/`isObjectAllowed`/`areFieldsAllowed`），全部 **fail-closed**（无 scope/未授权/空集合 → false）；`clear()` null-safe；
- `SecurityFrameworkServiceImpl`（D6，改）：`skipPermissionCheck()` 语义不变（仍 visitTenantId≠tenantId），但**后果**由「整体 return true」收敛为——`hasAnyPermissions` 按 `allowedActions` 裁决（`CrossOrgVisitScopeHolder.isAnyActionAllowed`）、`hasAnyRoles`/`hasAnyScopes` fail-closed false；非 visit 上下文照常委托 RBAC。

**biz-tenant starter**：
- `TenantVisitContextInterceptor`（D5，改）：注入 `ObjectProvider<CrossOrgVisitApi>` 取代粗粒度 `hasAnyPermissions("system:tenant:visit")`；visit-enable 门控（SEC-001.A 保留，最先）+ loginUser 空检查在前；provider 为空（无实现）→ **fail-closed 拒绝不切换**；调 `authorizeCrossOrgVisit`，DENY/null → 抛 FORBIDDEN 不切换；GRANT → `setVisitTenantId`（**全库唯一生产路径**）+ `TenantContextHolder.setTenantId` + `CrossOrgVisitScopeHolder.setScope(decision)`；`afterCompletion` 末尾 `CrossOrgVisitScopeHolder.clear()`（上下文清理有效）；
- `ZszjTenantAutoConfiguration`（改）：`tenantVisitContextInterceptor` Bean 签名改注入 `ObjectProvider<CrossOrgVisitApi>`。

**module-system（授权记录 + 判定引擎）**：
- `CrossOrgVisitGrantDO`（平台级跨租户表 `system_cross_org_visit_grant`，`extends BaseDO` + `@TenantIgnore`，循 `TenantDO` 范式）+ `CrossOrgVisitGrantMapper` + `CrossOrgVisitStatusEnum`（ACTIVE/REVOKED）/`CrossOrgVisitDenyReasonEnum`；
- `CrossOrgVisitService(+Impl)`：**10 步 fail-closed 判定序**（参数校验→取记录→状态→有效期→D-09 平台角色→目标租户/组织状态→动作→对象→字段→GRANTED）+ 发放/撤销（`@Transactional`）+ **每次判定（GRANTED/DENIED）落审计**（`AuditPort.record`，DENIED 走 REQUIRES_NEW 独立事务不随业务回滚丢失，记原主体·目标·理由·结果）；
- `CrossOrgVisitApiImpl`（facade，`@Service`）+ `ErrorCodeConstants` 增补。

**迁移/schema**：`V20260924.001__system_cross_org_visit_grant.sql`（PG，含 `idx_(visitor_user_id,target_tenant_id)` + 序列 START 100000）；H2 `create_tables.sql` + `clean.sql`。

## 新增/改测试（test `6ea90774`）

| 测试 | 模块 | 用例 | 断言 |
|---|---|---|---|
| `CrossOrgVisitServiceImplTest`（真实 H2） | system | 24 | 正向矩阵：正常授权/限定组织范围/无记录/撤销/过期/未生效/非平台角色/目标停用/目标不存在/目标组织无效/错动作/空动作/错对象/错字段/字段在范围/同租户非法/空主体非法/发放成功/发放拒绝(非平台·目标无效·窗口非法)/撤销后拒绝/撤销不存在/审计留痕（原主体·目标·理由·结果） |
| `SecurityFrameworkServiceImplCrossOrgVisitTest` | security | 11 | D6 收敛：visit 内动作放行（verify permissionApi never）/visit 外动作拒绝（**关键回归护栏**）/多动作任一放行/无 scope fail-closed/authorized=false fail-closed/空 allowedActions fail-closed/hasAnyRoles fail-closed/hasAnyScopes fail-closed/非 visit 委托 RBAC（权限·角色·scope） |
| `CrossOrgVisitScopeHolderTest` | security | 11 | D7 三维入口：动作∈/∉、对象 whole-tenant 放行/限定组织范围外拒绝、字段⊆/超出、无 scope 三维 fail-closed、`clear()` 后不残留、authorized=false 三维 fail-closed、setThenGet 同一快照 |
| `CrossTenantVisitEnabledFixtureTest`（重写） | biz-tenant | 4 | visit-enable=true 由旧放大改受控授权：获批切换/范围内动作放行(`/perm/user-query`)/范围外动作 403(`/perm/user-create`，**回归护栏**：旧放大会返 200)/无记录拒绝不切换 |
| `MockCrossOrgVisitApi` + `TenantFixtureConfiguration` | biz-tenant | — | 夹具装配确定性授权 SPI（获批 visitor→TENANT_2 whole-tenant + `system:user:query`/`contactMobile`；余 NO_GRANT） |

## RED → GREEN

- **RED**（实现前）：`CrossOrgVisitServiceImplTest` 24 用例编译失败/断言失败（DO/Service/SPI 未建）；`CrossTenantVisitEnabledFixtureTest` 旧断言（visit=整体放大）在 D5/D6 收敛后必失败。
- **GREEN**（实现后）：system 24 + security 40（11 服务 + 11 持有者 + 18 既有）+ biz-tenant 63 全绿。
- **flaky 排查**：biz-tenant 全量首跑 `SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully`（SSE 流式端点，token-t1-admin + tenant-id 无 visit-tenant-id，与本卡改动无关——无 visit 请求 preHandle 提前返回 true）偶发失败；隔离重跑该类 10 绿 + 全量重跑 biz-tenant 63 绿，确认为 reviews/README FLAKY-1 已登记的异步流式 timing flaky（多 Spring 上下文竞争资源），**非回归**。

## CodeReview 独立复审发现（PASSED，0×P0/P1）

六大安全不变量全部确认落实：① 无实现 fail-closed 拒绝不切换；② DENY 不切换（不设 visitTenantId/不切 TenantContextHolder）；③ `setVisitTenantId` 全库唯一生产路径在拦截器授权之后；④ 功能权限按 `allowedActions` 收敛（不再整体跳过）；⑤ 角色/scope visit 上下文 fail-closed false；⑥ `afterCompletion` 清 scope（上下文清理有效）。

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| 1 | **P2** | `DataPermissionRuleHandler.java#L33-35` | 数据范围（dept/org 轴 SQL 过滤）在 visit 上下文仍整体绕过——**pre-existing 代码，本 PR 未改** | 正是计划 D7 已登记边界「数据范围收敛随下游 FILE-001.B/MSG-003.C」；本卡"入口先行"，不代替各业务路径逐一接入 |
| 2 | P3 | `CrossOrgVisitServiceImpl#createGrant`（L149-166） | 发放时未校验 `allowedActions` 非空 | reviewer 明确不阻塞合并——D2.7 判定序 + D6 `isAnyActionAllowed` 空集合 fail-closed 已保证「空动作=不允许任何动作」，发放侧非空校验为纵深硬化，登记后续 |
| 3 | P3 | `CrossOrgVisitGrantMapper#selectLatestByVisitorAndTarget`（L29-35） | 查询未含 `visitorTenantId` 过滤 | reviewer 明确不阻塞合并——D2.5 D-09 平台角色 + D2.2 记录匹配已 fail-closed 兜底；`visitor_tenant_id` 过滤为查询精确性硬化，登记后续 |

## 验证（交付时点证据）

- 定向单测：system `CrossOrgVisitServiceImplTest` 24/24 + security 40/40 + biz-tenant 63/63 BUILD SUCCESS；
- CodeReview 独立复审 PASSED（0×P0/P1）；
- 环境坑：本地 `.m2` 框架 jar 陈旧（缺 framework-common 新增的 `CrossOrgVisitApi`/DTO），须先 `install` 上游模块（`-am`）再跑目标测试；compilerArgs 仅 `-parameters`（无 `-Werror`，泛型 varargs unchecked 警告不致失败）。

## 卡片状态与统计

主卡 ZS-SEC-001 状态维持**开发中**（.A 默认关闭旧放大链 + .B 获批跨组织授权记录/策略均交付；循子项拆分卡先例 SEC-011/DB-019/MSG-003/PERM-002/PERM-004「.A+.B 均交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」，须 B08 批次真实环境放行才转待验收）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- **双人审批 + 到期自动回收工作流**后置（D-09 line94 / PERM-002.B §7.7 / PERM-004.B §6）——本卡交付记录式受控授权（人工发放/撤销 + 有效期 + 状态 + 审计），不建审批流/回收 JOB；
- **字段目录 + 序列化层脱敏**归 ZS-PERM-003——本卡交付字段维度**判定机制**（allowed_fields/requestedFields）+ 矩阵用例，不接生产字段目录、不改序列化；
- **数据范围（dept/org 轴 SQL 过滤）在 visit 上下文的收敛**（CodeReview P2-1，pre-existing）+ **各业务路径逐一接入** `CrossOrgVisitScopeHolder` 对象/字段判定，随下游领域模块 FILE-001.B/MSG-003.C/CLIENT-002.B 落地（同 PERM-002.A/B「入口先行」口径）；
- **2×P3 硬化项**（createGrant allowedActions 非空校验、mapper visitorTenantId 过滤）登记后续，D6 fail-closed 已保证安全；
- **真实 PG + HTTP 端到端跨组织联验**随 B08 批次真实环境放行——本卡以 H2 + 内嵌 Redis 一致性套件 + 框架夹具坐实授权层语义；
- 正向授权矩阵、10 步判定序、三轴关系详见 [跨组织访问授权矩阵.md](../../services/zhongshu-core/docs/跨组织访问授权矩阵.md)。
