# ZS-BPM-002 评审处置（统一审批资格与对象授权校验：CodeReview 独立复审两轮，R2 PASSED / 0×P0/P1）

- 评审对象：ZS-BPM-002 统一审批资格、空候选人和对象访问校验——审批链资格重检（无审批人/租户条件/禁用用户/有效主体）+ 流程实例对象授权（发起人/历史任务参与人/抄送人可见性 + 流程管理员豁免）+ ASSIGN_EMPTY 兜底禁用过滤（B09 技术准备 Wave 首卡；docs/05 §13 ZS-BPM-002 卡「有效审批人正常办理；停用/其他租户/无资格人员不能被兜底或转派选中；无 assignee 任务不能被任意登录人处理；猜流程/任务 ID 不能越权看历史；不绕过获批自动节点规则」，前置 ZS-BPM-001〔BPM 独立装配与 PostgreSQL 验收，已验收〕+ ZS-PERM-002.B〔组织级数据授权范围 org 轴，已交付〕+ 任职资格 D-09 均已满足）
- 隔离分支：worktree `.wt/zszj-wt-bpm-002` 分支 `feat/bpm-002`（自 main `841e1d4e`；RED `9a347a8b` + GREEN `a96770a2` + R1 修复 `04fff832`）
- 合并：`--no-ff` main `51c62beb`（11 files +1052/-20，feat 分支自 `841e1d4e` 线性领先、干净合并）
- 卡片性质：**产线安全校验落地卡**——审批链资格重检 + 对象授权门，属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B / ZS-PERM-001.B / ZS-FILE-001.B / ZS-MSG-003.C / ZS-CLIENT-002.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**R1 报 1×P1（0×P0；4×P2）→ 修复（流程管理员豁免分支，`04fff832`）→ R2 独立复审 PASSED（0×P0 / 0×P1）**
- 原始日志：`outputs/_bpm002-*.log`、`outputs/_bpm002-r1-review.md`、`outputs/_bpm002-r2-review.md`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（卡片边界 + 前置移交）

ZS-BPM-001（B09，已验收）已交付 BPM 独立装配与 PG 验收（中性夹具、引擎表迁移责任、启停边界），但其登记边界「ZS-BPM-002 待 ZS-PERM-002.B（B08）」与卡片本体三项差距（[BpmTaskCandidateInvoker](../../services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/framework/flowable/core/candidate/BpmTaskCandidateInvoker.java) 移除停用用户后 ASSIGN_EMPTY 兜底不再过滤；[BpmTaskServiceImpl](../../services/zhongshu-core/zszj-module-bpm/src/main/java/cn/zszj/module/bpm/service/task/BpmTaskServiceImpl.java) validateTask 只在 assignee 非空时比较本人、按 taskId 查询未显式 tenant 条件、转派路径只显式检查目标用户存在）指向本卡；B08 Wave 的 ZS-PERM-002.B（org 轴）已交付提供权限策略。本卡在既有审批链上落地两层收敛：①**资格重检**——无审批人任务拒外部命令（内部自动节点放行）、任务/历史任务查询租户条件、转派/委托/加签有效主体重检、兜底候选人禁用过滤；②**对象授权**——流程实例详情可见性判定（发起人/历史任务参与人/抄送人 + 流程管理员豁免）与父子任务参与人门。

## 交付内容（RED `9a347a8b` + GREEN `a96770a2`）

**审批链资格重检**（`BpmTaskServiceImpl` / `BpmTaskCandidateInvoker` / `BpmTaskService`）：
- `validateTask`：审批人为空时外部调用拒绝（`TASK_OPERATE_FAIL_NO_ASSIGNEE` 1_009_005_021），内部调用（userId 为空）放行（保住自动节点，卡片「不绕过获批自动节点规则」）；
- `getTask`/`getHistoricTask`：存在租户上下文时追加 `taskTenantId` 条件（isNotBlank 守卫，无租户上下文行为与旧版一致）；
- `delegateTask`/`transferTask`：目标用户禁用拦截（`TASK_DELEGATE_FAIL_USER_DISABLED` 023 / `TASK_TRANSFER_FAIL_USER_DISABLED` 024）；
- `createSignTask`：加签用户须全部存在 + 禁用拦截（`TASK_SIGN_CREATE_USER_DISABLED` 025）；
- `BpmTaskCandidateInvoker`：ASSIGN_EMPTY 兜底候选人纳入禁用过滤（兜底不再以停用账号补位）。

**对象授权门**（`BpmInstanceVisibilityChecker` / `BpmTaskServiceImpl`）：
- `BpmInstanceVisibilityChecker`（新）：发起人 / 历史任务 assignee+owner / 抄送人三重可见性判定 + 流程管理员豁免（R1 修复补入），越权抛 `PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE`（1_009_004_009）；
- `BpmProcessInstanceServiceImpl#getApprovalDetail` 接线（对象授权校验，内部调用不传 processInstanceId 不触发）；
- `getTaskListByParentTaskId`/`deleteSignTask`：父/子任务参与人对象授权门（`TASK_OPERATE_FAIL_NOT_PARTICIPANT` 022）；
- `BpmProcessInstanceCopyMapper.selectCountByUserIdAndProcessInstanceId`（抄送人计数新原语）；`BpmTaskService#getTaskListByParentTaskId` 加 userId 入参（内部查询提取 `queryChildTasks`）。

## 新增/改测试

| 测试 | 用例 | 断言 |
|---|---|---|
| `BpmTaskServiceImplSecurityTest`（新） | 18 | 无审批人拒外部/放行内部、租户条件、委托/转派/加签禁用拦截、父子任务参与人门 |
| `BpmInstanceVisibilityCheckerTest`（新） | 6→7 | 发起人/历史 assignee/历史 owner/抄送人/非参与人拒绝/内部调用短路 +（R1 修复）流程管理员豁免 |
| `BpmTaskCandidateInvokerTest`（+3 例） | 3 | ASSIGN_EMPTY 兜底候选人禁用过滤 |

## RED → GREEN → R1 修复

- **RED**（实现前）：Tests run: 32, Failures: 13, Errors: 9（22 预期失败含 9 UnnecessaryStubbing；10 正向回归通过）；
- **GREEN**（实现后）：32/32 全绿（BPM 模块独立构建，surefire 三测试类）；
- **R1 修复后**：33/33 全绿（+1 管理员豁免用例）；
- **反向探针**：`git stash push -- BpmInstanceVisibilityChecker.java`（保留测试改动）→ 管理员用例 NPE 失败（旧代码继续走 history 查询）→ 证明新用例非恒真；`stash pop` 后 33/33 复绿；
- **模块全量**：85 run（本卡定向 3 类全绿），残留 1F+2E（`BpmFormServiceTest.testGetFormPage` + 2 harness env errors〔缺 `ZSZJ_BPM_HARNESS_SCHEMA_UPDATE` 环境变量〕）经基线 `841e1d4e` 对照确认预存在、非本卡引入。

## CodeReview 独立复审发现（R1 → 修复 → R2 PASSED，0×P0/P1）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P1-1 | **P1** | `BpmInstanceVisibilityChecker` / `/get-approval-detail` / 前端 manager/index.vue | 可见性校验缺少「流程管理员」豁免分支：持 `bpm:process-instance:manager-query` 的管理员从「管理流程」菜单点详情（共用 Detail 页调 `/get-approval-detail`）查看未参与流程实例时抛 NOT_VISIBLE（功能回归，与 manager-page 列表「可以看全部的流程实例」口径矛盾） | **修复** `04fff832`：判定链前端（null 短路之后）增加流程管理员豁免分支（注入 `PermissionApi.hasAnyPermissions(loginUserId, "bpm:process-instance:manager-query")`）+ 1 管理员豁免用例；内部调用用例 `verifyNoInteractions` 扩展至 permissionApi（守护「null 短路先于权限判断」顺序） |
| P2-1 | P2 | `ErrorCodeConstants` 024 | 文案「转办人已被禁用」歧义（建议「被转办人」） | **核实后不改**：既有 014「转办人不存在」等族内文案均指目标用户（`reqVO.getAssigneeUserId()`），单改 024 破坏族内一致 |
| P2-2 | P2 | `getHistoricProcessInstance` / `/get` | 入口租户条件缺口（预存在，非本卡引入） | 登记后续卡片 |
| P2-3 | P2 | ASSIGN_EMPTY 兜底 | 兜底候选人全禁用后的空审批人最终态 | 建议产品/运维确认 + 监控（登记） |
| P2-4 | P2 | 子流程详情 | 子流程详情对父流程参与人可见性 | 建议产品确认（登记） |

R2 独立复审（worktree 内独立执行 git diff / 读源码 / 检索调用点 / 定向复跑单测）确认 P1 修复闭环成立：①判定顺序正确（loginUserId 空短路先于权限豁免，内部调用不触发 permissionApi 查询）；②参数与权限串正确（豁免用传入 loginUserId、非 SecurityContext 隐式取用；常量与 controller `/manager-page` 注解及菜单 SQL id 2722/755 逐字符一致）；③无旁路（`PermissionServiceImpl.hasAnyPermissions` 角色-菜单交集严格判定，无 manager-query 者必须命中发起人/历史 assignee/历史 owner/抄送人之一）；④测试真实守护（7 用例 + `verifyNoInteractions` 顺序守护）。全量 11 文件独立复核（validateTask 无 assignee 拒外部、租户条件守卫、授权门、isTaskParticipant null 安全、禁用拦截、兜底过滤、调用方兼容性、任务分配模型 `TaskHelper.changeTaskAssignee` 责任到人）与风险复评（checker 生产调用点唯一、`PermissionApi` 注入有 `BpmTaskCandidateRoleStrategy` 同类先例、PG harness 不组件扫描服务包零耦合）均通过。**PASSED / 0×P0 / 0×P1**。

## 验证（交付时点证据）

- TDD RED：32 run / 22 预期失败（13F+9E）+ 10 正向回归（`outputs/_bpm002-red.log`）；
- GREEN：32/32（`outputs/_bpm002-green.log`）；R1 修复后：**33/33** BUILD SUCCESS（`outputs/_bpm002-fix1.log` / `_bpm002-fix1b.log`）；
- 反向探针：移除修复后管理员用例 NPE 失败（`outputs/_bpm002-fix1-probe.log`）→ 守护力直证；
- 模块全量：85 run 残留 1F+2E 经基线 `841e1d4e` 对照预存在（`outputs/_bpm002-modulefull.log`）；
- CodeReview 独立复审替代：R1（1×P1+4×P2 → 修复 `04fff832`）→ R2 PASSED / 0×P0/P1（`outputs/_bpm002-r1-review.md` / `_bpm002-r2-review.md`）。

## 卡片状态与统计

主卡 ZS-BPM-002 由待开发转**待验收**（B09 技术准备 Wave 首卡，技术夹具级交付；B09 批次真实环境放行前，循 ZS-DB-010/IAM-002/IAM-004/BPM-001 先例）。第 2 节统计随之待开发 2→1、待验收 3→4（1/20/4/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- **实际 Flowable 审批资格与 B09 真实环境联验**（卡片放行边界：B08 只交付消费的组织/对象权限策略，不等待流程运行验收）；
- **P2-2 `/get` 入口租户条件缺口**（预存在）随后续卡片；
- **P2-3 空审批人最终态**（兜底候选人全禁用）建议产品/运维确认 + 监控；
- **P2-4 子流程详情可见性**建议产品确认；
- **P2-1 错误码 024 文案**维持族内一致不改（014/024「转办人」即目标用户语义）；
- 本卡为 BPM 域内资格/对象授权收敛，不涉及 org 轴数据授权注册（该边界随 B08 下游或后续卡）。
