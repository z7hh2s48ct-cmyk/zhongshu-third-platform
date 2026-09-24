# ZS-PERM-001.B 评审处置（批准组织模型下的授权范围：CodeReview 独立复审两轮 PASSED / 0×P0/P1）

- 评审对象：ZS-PERM-001.B 批准组织模型下的授权范围——在 ZS-PERM-001.A 同技术租户归属/上限校验之上，落实 D-09「批准组织模型下的授权范围」（B08；docs/05 §16.1 line1026 子项，前置 ZS-PERM-001.A〔授权目标归属与可授予上限校验〕+ ZS-IAM-002〔组织/任职模型 + 服务端组织上下文〕+ ZS-SEC-001.B〔获批跨组织 visit 上下文 CrossOrgVisitScopeHolder〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-perm-001-b` 分支 `feat/perm-001-b`（自 main `d48cd4d8`；feat `e2287109` + test `3b04666d` + 评审响应 feat `eea961d9` / test `4fadc765`）
- 合并：`--no-ff` main `e6c4c124`（7 files +466/-22，feat 分支自 `d48cd4d8` 线性领先、干净合并）
- 卡片性质：**产线授权写入路径收敛卡**——在 `assignUserRole` 授权写入路径接入获批跨组织 visit 上下文范围收敛（org 维 + 角色上限维），属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**两轮 CodeReview 独立复审均 PASSED（0×P0 / 0×P1）**——r0 PASSED（附条件）余 P2-1 + 5×P3，全部于 r1 闭合（P2-1 角色上限维交付 + P3 测试/注释精确化）
- 原始日志：`outputs/_perm001b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（ZS-PERM-001.A HANDOFF 移交 + §16.1 line1026 验收）

ZS-PERM-001.A（2026-09-09）已在 `PermissionServiceImpl` 三个授权写入方法补齐**同技术租户**归属/上限/自我提权校验，但显式移交：「获准跨组织范围（D-09）的授权目标/上限映射与正向矩阵归 ZS-PERM-001.B」。ZS-SEC-001.B 交付获批跨组织 visit 上下文（`CrossOrgVisitScopeHolder.getScope()` → `CrossOrgVisitDecisionDTO`：authorized/targetTenantId/targetOrgIds〔null=whole-tenant〕/allowedActions/allowedFields）后，授权写入路径尚未消费该范围快照——即访客在获批 visit 上下文下 `assignUserRole` 仍只受 .A 的同技术租户校验，未收敛到批准组织范围；且既有上限只拦 super_admin（`isSuperAdminRole` 只认 super_admin），访客可把 tenant_admin 授予获批组织内用户，残余提权面。§16.1 line1026 验收：「两组织互授/越界被拒，批准管理员操作成功」。

## 交付内容（feat `e2287109` + 评审响应 `eea961d9`）

**module-system（授权写入路径收敛）**：
- `ErrorCodeConstants`：新增 `PERMISSION_ASSIGN_USER_OUT_OF_VISIT_SCOPE`(1_002_009_005，被授权用户不在获批跨组织访问的组织范围内) + `PERMISSION_GRANT_ADMIN_ROLE_IN_VISIT`(1_002_009_006，visit 上下文禁止授予租户级管理员角色)；
- `PermissionServiceImpl#validateCrossOrgVisitScope(userId, createRoleIds)`（新，`assignUserRole` 调用点在 `validateRolesForAssign`/`validateUserRoleGrantCeiling` 之前）：非 visit 上下文（`scope==null || !authorized`）直接返回，保持 PERM-001.A 语义不施加 org 维收敛；visit 上下文下——
  - **角色上限维**（P2-1，.A HANDOFF「上限映射」）：`createRoleIds` 含 super_admin 或 tenant_admin（`roleService.hasAnySuperAdmin || hasAnyTenantAdmin`）即抛 `PERMISSION_GRANT_ADMIN_ROLE_IN_VISIT`，fail-closed、不受访客自身角色豁免（访客在目标租户本非超管，`isSuperAdminUser` 恒否）；
  - **org 维**：`targetOrgIds==null`（whole-tenant）放行目标租户内任意用户（租户维已由 `validateTenantScope` 校验）；否则目标用户须有**有效任职**落在获批组织集合内（`orgDataScopeResolver.resolveEffectiveOrgIds(userId)` 与 targetOrgIds 交集非空），越界/无任职/**空集**（非 null）fail-closed 抛 `PERMISSION_ASSIGN_USER_OUT_OF_VISIT_SCOPE`；
- `OrgDataScopeResolver#resolveEffectiveOrgIds(userId)`（DRY 抽取自 `resolve()`，行为严格等价）：返回用户有效任职（ACTIVE + 有效期 + 组织启用）落点组织 ID 集，与 org 轴数据范围/IAM-002 同口径；**刻意差异**（Javadoc 登记）：不下钻后代、不施加 PLATFORM→ORG_ALL 放大/超管豁免（授权写入路径要精确归属集，非数据范围放大）；调用不变式：只在 `@DataPermission(enable=false)` 或 `skipPermissionCheck()==true` 的 visit 上下文调用；
- `RoleCodeEnum#isTenantAdmin(code)` + `RoleService#hasAnyTenantAdmin(ids)` + `RoleServiceImpl#hasAnyTenantAdmin/isTenantAdminRole`（对称于既有 `hasAnySuperAdmin/isSuperAdminRole`，**不区分状态**——堵「先授禁用 tenant_admin 角色、待解禁生效」的搁置提权）。

## 新增/改测试（test `3b04666d` + 评审响应 `4fadc765`）

| 测试 | 模块 | 用例 | 断言 |
|---|---|---|---|
| `PermissionServiceTest`（真实 H2，`@Import` PermissionServiceImpl + OrgDataScopeResolver） | system | 12 visit（6 首弧 + 6 评审响应） | 首弧：越界拒绝/两组织互授成功/whole-tenant 放行/非 visit 放行等；评审响应新增：授予 tenant_admin 拒绝、授予 super_admin 拒绝（whole-tenant）、targetOrgIds 空集 fail-closed 拒绝、目标用户任职组织停用拒绝、目标用户任职过期拒绝、未获批 visit（authorized=false）org 门与上限门均跳过；既有 3 成功用例断言强化为 `roleId==300`（区分正确授予与残留行） |

夹具辅助 5：`insertVisitOrg(orgId,type)`、`insertVisitMembership(userId,orgId)`、`visitScope(targetTenantId,targetOrgIds)`、`insertVisitOrgWithStatus(orgId,type,status)`、`insertVisitMembershipWithValidTo(userId,orgId,validTo)`。

## RED → GREEN

- **RED**（实现前）：`PermissionServiceTest` visit 用例编译失败（`PERMISSION_ASSIGN_USER_OUT_OF_VISIT_SCOPE` / `PERMISSION_GRANT_ADMIN_ROLE_IN_VISIT` / `hasAnyTenantAdmin` 未定义）；
- **GREEN**（实现后）：targeted 隔离 PermissionServiceTest 47 + OrgDataScopeResolverTest 13 + RoleServiceImplTest 22 = **82 全绿** BUILD SUCCESS。
- **flake 归因**：system 模块全量首跑 OrgDataScopeResolverTest 等顺序依赖失败，经 clean main（`d48cd4d8`）基线对照——main 全量 956 tests 6 Failures（OrgDataScopeResolverTest 挂 4，比本分支更多）3 Errors，证实为 surefire 单 JVM 复用跨类静态状态污染的**既有 flake、非本卡回归**；`run-local-gates --fast` 10 门禁不跑 system 全量 Java 测试（G11 只 common+infra），targeted 隔离全绿即本卡验证标准。

## CodeReview 独立复审发现（两轮均 PASSED，0×P0/P1）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P2-1 | **P2** | `PermissionServiceImpl#validateCrossOrgVisitScope` | .A HANDOFF 明确交接给 .B 的「上限映射」未交付：visit 上下文下访客可把 tenant_admin 授予获批组织内用户（既有上限只拦 super_admin），残余提权面 | **采纳**（判属本卡应完成范围，非过度设计）：新增角色上限维（禁止跨组织铸造 super_admin/tenant_admin，fail-closed，不受访客自身角色豁免）+ grantTenantAdmin/grantSuperAdmin 拒绝用例 |
| P3-2 | P3 | 测试 | targetOrgIds 空集（非 null）无用例 | 采纳：`emptyTargetOrgIds_rejected`（fail-closed），org 限定分支显式覆盖空集 |
| P3-3 | P3 | `OrgDataScopeResolver#resolveEffectiveOrgIds` | Javadoc 未说明与 `resolve()` 的刻意差异与调用不变式 | 采纳：补两处刻意差异（不下钻后代、不施加 PLATFORM→ORG_ALL 放大/超管豁免）+ 调用不变式 |
| P3-5 | P3 | 测试 | 停用组织/过期任职/authorized=false 无用例、成功用例断言偏弱 | 采纳：`targetUserOrgDisabled`/`targetUserMembershipExpired`/`visitScopeNotAuthorized` 3 用例 + 既有成功用例断言强化 `roleId==300` |
| P3-6 | P3 | `PermissionServiceImpl` / `OrgDataScopeResolver` | 段注释/类 Javadoc `@author` 未反映第二消费方 | 采纳：段注释更新为「ZS-PERM-001.A/.B 授权目标归属、上限与获批跨组织范围校验」+ 类 Javadoc/@author |

r1 复审（`3b04666d..4fadc765`）确认 P2-1 + 全部 P3 闭合，PASSED 0×P0/P1。

## 验证（交付时点证据）

- targeted 隔离单测：PermissionServiceTest 47 + OrgDataScopeResolverTest 13 + RoleServiceImplTest 22 = 82/82 BUILD SUCCESS；
- 两轮 CodeReview 独立复审均 PASSED（0×P0/P1）；
- 全量 system flake 经 clean main 基线对照判定既有顺序污染、非回归（见 RED→GREEN）。

## 卡片状态与统计

主卡 ZS-PERM-001 状态维持**开发中**（.A 同技术租户归属/上限校验 + .B 批准组织范围收敛均交付；循子项拆分卡先例 SEC-001/SEC-011/DB-019/MSG-003/PERM-002/PERM-004「.A+.B 均交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」，须 B08 批次真实环境放行才转待验收）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- **org 轴 SQL 列表过滤规则 `OrgDataPermissionRule` + 业务表 `org_id` 列注册**随 B08 下游领域模块（同 PERM-002.B/PERM-004.B 口径）——本卡交付授权写入路径的范围收敛，不改数据范围 SQL 规则；
- **数据范围（dept/org 轴 SQL 过滤）在 visit 上下文的收敛**（SEC-001.B CodeReview P2-1，`DataPermissionRuleHandler` pre-existing）随下游 FILE-001.B/MSG-003.C；
- **双人审批 + 到期回收工作流**后置 D-09；**字段级授权/字段目录**归 ZS-PERM-003；
- **真实 PG + HTTP 端到端跨组织授权联验**随 B08 批次真实环境放行——本卡以 H2 targeted 套件坐实授权写入路径收敛语义；
- 授权写入路径收敛接线详见 [跨组织访问授权矩阵.md](../../services/zhongshu-core/docs/跨组织访问授权矩阵.md) §5。
