# ZS-BPM-003 评审处置（领域状态与幂等写回：CodeReview 独立复审 R1 1×P1+1×P2+6×P3 → 处置 60f4de3f → R2 PASS（余 1×P3 处置 212fc7d7））

- 评审对象：ZS-BPM-003 批准首链后实现领域状态与幂等写回（B09 主卡，D-07 门禁 2026-09-28 解除；前置 D-07〔M1~M10，docs/09 §4〕+ D-09 + B05/B08 均已交付或放行）
- 隔离分支：worktree `.wt/zszj-wt-bpm-003` 分支 `feat/bpm-003`（自 main `ee8d59c9`；RED `71a8a7ce`〔15 files +856，骨架+27 测试〕→ GREEN `2ef04749`〔11 files +793/-56〕→ R1 处置 `60f4de3f`〔8 files +645/-22，含 PG 证据链入库〕→ R2 处置 `212fc7d7`）
- 卡片性质：**产线机制+首链领域卡**——D-07 M3 申请三态/M6 线索五态状态机、版本条件更新、Flowable 单节点审批流绑定（M10=B）、幂等命令门、原因审计、指标派生；Lead 业务表与待办/事件接线随首链业务模块落卡（登记边界）
- 评审工具：codex 账号用量上限阻断（承 PERM-002.B 起 15 卡先例），改以 **CodeReview 独立复审替代**（R1 → 处置 → R2 两轮）
- 结论：**R1 1×P1+1×P2+6×P3（幂等/崩溃窗口/租户隔离/方言/迁移/约定六域判 clean）→ P1 证据入库 + P2 撤回竞争业务化分类 + P3×5 修复 + P3-6 登记不改 `60f4de3f` → R2 增量复审 PASS（确认全部闭环，余 1×P3 审计断言精度 → 处置 `212fc7d7`）**

## R1 findings 与处置（`60f4de3f`）

| 级别 | 编号 | 缺陷 | 处置（`60f4de3f`） |
|---|---|---|---|
| P1 | P1-1 | PG/Flowable 运行时套件三件（`BpmFirstChainPgRuntimeTest` 6 例 + `run-bpm003-verify.mjs` 编排器 + `run-pg-regression.mjs` #21 注册）停留工作区未提交——验收锚点「真实引擎四操作与幂等门联验」在已提交 diff 中无覆盖，且主代码 javadoc 引用了未入库脚本路径 | 三件随处置提交入库；首跑 6/6 全绿 + 修正 `EXPECTED_TESTS` 笔误（7→6）后正式重跑 PASS（tests=6 failures=0 errors=0 skipped=0，outputs/bpm-003/runtime-report.json） |
| P2 | P2-1 | 撤回竞争败方暴露裸引擎异常：withdrawApproval 先调 `processPort.withdrawProcess` 再落绑定门——Flowable 8 `DeleteProcessInstanceCmd` 对已完结实例抛 `FlowableObjectNotFoundException`，先到完成的竞争下撤回在门分类前即以引擎原生异常失败，「先到者生效、后来者显式冲突」合同只在门行竞争窄路径成立 | 端口契约改 `withdrawProcess` 返回 boolean（false=实例已被并发处理，适配器捕获 `FlowableObjectNotFoundException` 翻译）；撤回**无条件落绑定幂等门分类**（门赢=撤回生效/门输=业务冲突/重复=吸收）；新增韧性用例（端口 false 仍落门、领域不变） |
| P3 | P3-1 | restartApproval 错误状态抛 `APPLICATION_NOT_EXISTS`（语义=不存在/不可见），违背执行器 NOT_EXISTS 严格语义 | 改 `FIRST_CHAIN_STATE_CONFLICT`（附当前状态，可回查分类） |
| P3 | P3-2 | javadoc 称「弃单审计由 JdbcAuditPort 独立事务落库」与实现不符（弃单为 SUCCESS 随事务；REQUIRES_NEW 仅 DENIED/FAILURE） | javadoc 订正（弃单分支正常返回故事务提交，语义成立） |
| P3 | P3-3 | 绑定门 updater 硬编码 "system" 丢弃操作者归因 | `transitionGate` 参数化 updater，两调用点传真实 actorId |
| P3 | P3-4 | 流程实例无引擎级租户标签（tenantId 仅作流程变量），偏离模块惯例（BpmProcessInstanceServiceImpl.processInstanceTenantId） | 适配器改 `createProcessInstanceBuilder().tenantId(...)`（领域租户权威仍为绑定行，javadoc 登记） |
| P3 | P3-5 | 并发同 appKey 创建：COUNT 快路径竞态下第二插入以裸 `DuplicateKeyException`（500 类）失败 | 捕获转译 `FIRST_CHAIN_APP_KEY_EXISTS`（uk 兜底，插入即回滚干净） |
| P3 | P3-6 | H2 测试 schema 与 PG 迁移两处分歧（app_key 全列唯一 vs 部分唯一；活跃单绑定部分唯一索引缺失）——删除重建/单活跃不变量的索引兜底在 H2 不可表达 | **登记不改**（PG 迁移与 PG 套件 DDL 覆盖兜底；PG runtime setUp 建 `uk_bfc_binding_active` 部分唯一索引） |

## R2 复审（增量 `2ef04749..60f4de3f`）

**PASS / 0×P0/P1/P2**：P2 处置控制流全交错验证无新洞（门条件 UPDATE 保证恰一方生效；败方静默吸收或显式冲突；引擎操作随调用方事务回滚）；签名变更全调用点传播（全仓 grep 无遗漏）；韧性用例对「处置回退」「跳门」均有检出力；PG 证据文件与实跑一致（EXPECTED_TESTS=6 = 6 个 @Test）。余 1×P3：PG 测试 `auditCount` 未按 biz_id 过滤（靠 @Order 确定性假绿）→ 处置 `212fc7d7`（biz_id=appKey 精确过滤）。

## 验证（交付时点证据）

- **RED 双证**：编译红（找不到符号 FIRST_CHAIN_PROCESS_RESULT_DISCARDED——上游 zszj-common 补常量后过编译；测试侧符号缺插画红两轮）+ 断言红 27 例（26E UOE + 1F 空状态机表）；
- **GREEN（H2）**：`FirstChainStateMachinesTest` **9/9** + `FirstChainApplicationServiceTest` **18/18**（含提交链路/幂等门三守卫/晚到弃单/撤回重发/指标派生）；R1 处置后 **28/28**（+韧性 1 例）；
- **bpm 模块全量（H2）**：124 例，失败集合与主树基线（97 例 1F+2E：harness 环境变量 by design + BpmFormServiceTest 既有失败）完全一致，**零回归**（主树同命令对照实证）；
- **GREEN（真实 Flowable+PG）**：`run-bpm003-verify.mjs` **6/6 PASS**（tests=6 failures=0 errors=0 skipped=0）——提交/通过/拒绝/撤回/转派四操作与业务状态一致、重复回调吸收、晚到弃单留痕、并发审批/撤回真实双线程恰一方生效；R1/R2 处置后各重跑取证，最终 `212fc7d7` 上 PASS；
- **PG 回归聚合**：套件 #21 注册（20→21），随合并后主树全量聚合取证。

## 边界（登记，不随本卡提升）

1. Lead 业务表（M6 五态落地载体）与线索域指标随首链业务模块落卡（执行器已备表名合同 `bpm_first_chain_lead`）；
2. 待办/事件接线（PILOT-REQ-010）随首链业务模块按 BPM-004 接入合同落卡（避免 bpm→system 主代码依赖倒转）；
3. M8 申请联系人电话 F2 挂级随域接入走 docs/08 §7 回填；M9 线索超时回收/平台撤回一期不做（D-07 已确认）；
4. H2 部分唯一索引不可表达（P3-6）：两索引兜底由 PG 迁移 + PG 套件覆盖；
5. 主卡 ZS-BPM-003 维持**开发中**（实现交付，须 B09 批次真实环境放行验收），本记录不表示任何主任务已验收。
