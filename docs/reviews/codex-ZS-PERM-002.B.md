# ZS-PERM-002.B codex 评审处置（r0 FAIL 1×P1+1×P2 → 修复 → r1 用量阻断 → CodeReview 独立复审替代 PASS/0）

- 评审对象：ZS-PERM-002.B 组织级（跨组织）数据授权范围 org 轴（B08 Wave1；§16.1 子项，前置 ZS-PERM-002.A + ZS-IAM-002 + D-09 均已交付）
- 隔离分支：worktree `.wt/zszj-wt-perm-002-b` 分支 `feat/perm-002-b`（impl `e895c5b2` → 修复 `d9243f1e`）
- 合并：`--no-ff` main `e64831b9`（15 files +1018/-3；与 main `46e58c50`〔ZS-IAM-004 文档同步〕零文件重叠干净合并，merge-base `98d2e959`）
- 评审工具：codex（`gpt-6-astra`/`high`，read-only sandbox，r0）；r1 验证轮因账号用量上限阻断，由 **CodeReview 独立复审替代**
- 结论：**r0 FAIL 1×P1+1×P2 → 修复 + 补 4 回归测试 `d9243f1e` → CodeReview 独立复审 PASS / 0 findings**（评审门实质闭环）
- 原始日志：`outputs/_perm002b-r0-*.log`、`outputs/_perm002b-r1b-verdict.log`（本地未入库，gitignore；本档案为处置入库）

## r0 发现（FAIL，1×P1 + 1×P2）

### P1 — OrgDataPermissionChecker self 兜底跨组织泄漏（严重）

`OrgDataPermissionChecker` 的 self 兜底分支照搬 dept 轴语义：对象 `owner==登录用户` 即可见，**未要求对象无组织归属**。后果——某对象归属**他组织**（`orgId≠null` 且不在登录主体授权 `orgIds` 内）但 `owner` 恰为本人时仍被放行，**绕过 FND-AUTH-004「已知其他组织的对象 ID 也不能读取/修改/下载/导出」**；典型场景为转岗/离任成员仍可访问旧组织内本人曾创建的对象，违反 D-09「跨组织只允许显式平台角色」。

### P2 — OrgDataScopeResolver 后代组织未校验启用状态（中）

`collectSelfAndDescendants` 的 BFS 下钻经 `organizationMapper.selectListByParentId` 仅按父组织编号过滤、**不校验子组织状态**。后果——启用组织 A 的**禁用**子组织 B 被纳入授权范围，与「直接任职须组织 `CommonStatusEnum.isEnable`」（`resolve` 第 3 步）口径自相矛盾：直接任职禁用组织被排除，却经父组织后代下钻把同一禁用组织放进来。

## 修复（`d9243f1e`，含 4 回归测试）

- **P1**：self 兜底加 `orgId==null` 前置守卫——对象一旦归属某组织（`orgId≠null`）即由组织范围**独占裁决**，本人所有权不得凌驾组织排除；self 兜底**仅对无组织列对象**（`orgId==null`）生效。**与 dept 轴的有意分歧**：dept 轴 self 是技术租户内合法可见层（本人数据权限，可在可见部门集之外放行本人对象）；org 轴受 D-09 FND-AUTH-004 约束，语义必须收窄。补 2 回归测试（他组织对象 owner=本人须拒绝 / 无组织列对象 owner=本人须放行）。
- **P2**：BFS 下钻对每个子组织施加 `CommonStatusEnum.isEnable`——禁用组织既不入范围、也不继续下钻（其子树 **fail-closed 级联排除**）。补 2 回归测试（禁用子组织不入范围 / 禁用组织的启用孙组织随子树级联排除）。

## r1 验证轮：账号用量上限阻断（基础设施故障，非代码缺陷）

修复提交 `d9243f1e` 后的 r1 复评验证轮因 codex 账号用量上限遭遇**三重基础设施故障**，无法产出复审判定：

1. 默认模型 `gpt-6-astra` 已下线（HTTP 400）；
2. 备选 `gpt-5.6-terra` 请求停滞无响应；
3. 回退 `gpt-5.5` 命中 usage limit（提示约 Oct 20 2026 恢复）。

该阻断为**外部长期不可控**因素，非本卡代码/修复缺陷。按「不擅自跳过评审门」原则，改以 **CodeReview 独立复审**作等价替代验证。

## CodeReview 独立复审替代 = PASS / 0 findings

对 worktree 内 `git diff main...HEAD` 做等价独立复审（`review_scope=explicit_target`），返回 **PASS / 0 findings**，逐面坐实：

- **P1 闭环正确完整**：`OrgDataPermissionChecker` self 兜底 `orgId==null` 守卫，与 `DeptDataPermissionChecker` self 语义为**有意分歧**（非疏漏）；
- **P2 闭环正确完整**：`OrgDataScopeResolver` BFS 下钻施加 `isEnable`，禁用子树 fail-closed 级联排除；
- **org 轴全面无新缺陷**：`CONTEXT_KEY` 与 dept 轴不串扰、`@DataPermission(enable=false)` 防递归、租户隔离、超管豁免（复用 `isSuperAdminUser` 仅认启用态）、BFS 防环（visited 去重）+ 深度上限（`MAX_DESCENDANT_DEPTH=256`）、取不到权限 fail-closed、null 边界、并发、D-09「跨组织只允许显式平台角色」口径、批量「任一越权整批拒绝」、API 链 4 层完整性、28 测试覆盖均无新缺陷。

## 验证（交付时点证据）

- 定向单测 **28/28**（`OrgDataPermissionCheckerTest` 15 + `OrgDataScopeResolverTest` 13）BUILD SUCCESS；
- `node scripts/ops/run-local-gates.mjs --fast` **10/10**（合并前 feat 分支 + 合并后 main 均绿）；
- 模块全量与 main 基线同源、零新增失败（既存 2F OAuth2 + 3E SMS 为负载 flaky，与本卡无关）。

## 卡片状态与统计

主卡 ZS-PERM-002 维持**开发中**（循子项拆分卡先例 ZS-SEC-011/ZS-DB-019/ZS-MSG-003「.A+.B 均交付、待验收须真实环境放行」+ §16.1 PERM-002.B 验收口径「不能把技术夹具算业务策略完成」；本次交付对象级检查入口 + 范围解析 + 夹具〔同 .A 入口先行〕，业务表 `org_id` 列注册与 SQL 列表过滤规则 `OrgDataPermissionRule` 随 B08 下游领域模块）。第 2 节统计不变（2/20/3/0/3/63，合计 91）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- org 轴 SQL 列表静默过滤规则 `OrgDataPermissionRule` + 业务表 `org_id` 列注册随 B08 下游领域模块（SEC-001.B/PERM-004.B/FILE-001.B/MSG-003.C/CLIENT-002.B）接入落地——当前无业务表注册 `org_id` 列；
- 临时跨组织授权（双人审批 + 到期回收）后置 D-09；`ORG_ONLY(4)` / 值 2（`ORG_ASSIGNED`）为保留位，一期未启用；
- 字段级授权归 ZS-PERM-003；真实 PG + HTTP 端到端跨组织对象授权联验随 B08 下游领域模块与批次真实环境放行收口。
