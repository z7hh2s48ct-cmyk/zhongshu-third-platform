# B09 批次放行验证收口报告（D-07 首条薄链、Flowable 和领域状态）

> 验收日期：2026-09-29
> 验收基线提交：`46c87aab`（main，docs/05 V2.05——B09 实现侧五件（BPM-001 先行件 / BPM-002 / BPM-003 / BPM-004 / OPS-001.E）全部交付后的最新文档收口）
> 上位依据：[docs/03 §7 批次放行规则 + §B09 条款](../03-底座二次开发顺序与验收标准.md)、[docs/05 V2.05→V2.06](../05-底座模块分析与开发任务清单.md)、[docs/09 首链业务闭环决策输入（D-07 M1~M10 拍板）](../09-首链业务闭环决策输入.md)
> 原始日志：`outputs/acceptance/B09-*.log`、`outputs/b09-gate/gate-report.json`（本地保留，不入库）

## 1. 验收环境（与 B02～B08 同基线）

| 组件 | 版本 | 供给方式 |
|---|---|---|
| JDK | OpenJDK 17.0.20.1 | `tools/jdk-17.0.20.1+1`（bpm 定向测试注入 `JAVA_HOME`；PG 套件脚本内同源解析） |
| Maven | 3.9.9 | `tools/apache-maven-3.9.9` |
| PostgreSQL | 16/17-alpine | 各 PG 套件自管理一次性容器（验后清理）；G14/G15/G16 自举 compose 环境 |
| Redis | 内嵌（单测）/ Docker 7.4（G14/G15） | — |
| Node.js | v24.19.0 | 系统 PATH |
| Docker | 29.7.2 | Windows 22H2 主机 |

## 2. 验证执行矩阵（全部实跑，非引用旧证据）

| # | 执行入口 | 覆盖 | 结果 | 日志 |
|---|---|---|---|---|
| 1 | `run-local-gates.mjs` 全量 15 门禁 | G1～G10/G13～G16（含 G10 类型基线 29817ms / G13 双端 vitest / G14 真实 server 七类矩阵 63156ms / G15 双端联调 CLIENT-005.B E2E 18 用例 420050ms / **G16 B09 联合门禁 48924ms**；并发总耗时 561992ms） | **PASS 15/15，失败 0，exit 0** | `B09-full-gates.log` |
| 2 | `run-pg-regression.mjs`（**21 套件**） | DB-006~008/010~018、CFG-002.A、BPM-001、JOB-002~004、OPS-002.B、SEC-011.B、FILE-005.B、LOGIN-005.B、IAM-002、IAM-004、FILE-004.B、**BPM-003（第 21 套件，真实 Flowable+PG 引擎 7 例含跨租户反向）** | **PASS 21/21 套件，失败 0，exit 0** | `B09-pg-regression.log` |
| 3 | **G16 专项复跑**（`run-b09-joint-gate.mjs`，独立于 full 门禁再取一轮证据） | P1 BPM-001 PG 流程（tests=9 failures=0 + S1 零 ACT_/FLW_ 基线表 + S2 运行期零 DDL〔ACT_ 39→39、schema.version 8.0.0.0 不变〕+ S3 异步回声探针落库=3）+ P2 BPM-003 首链幂等写回与跨组织反向（tests=7 failures=0；基线迁移 28 个含 V20260928.001 首链两表）；结构化产物 `gate-report.json` pass=2/failCount=0 | **PASS 2/2，exit 0** | `B09-g16-gate.log` |
| 4 | bpm 模块定向测试批（`mvn -f zszj-module-bpm/pom.xml`，JDK17） | BPM-002 面：`BpmTaskServiceImplSecurityTest` 18（无审批人拒外部/放行内部、租户条件、委托/转派/加签禁用拦截、父子任务参与人门）+ `BpmTaskCandidateInvokerTest` 8 + `BpmTaskCandidateAssignEmptyStrategyTest` 2（兜底候选人禁用过滤）+ `BpmInstanceVisibilityCheckerTest` 7（三重可见性+管理员豁免）= 35；BPM-004 面：`TechNeutralContractSampleTest` 11（提交/完成/派发/绕过版本/终态守卫/乱序水位/无租户双入口/跨租户隔离/重复注册/重复事件）；BPM-003 H2 面：`FirstChainStateMachinesTest` 9 + `FirstChainApplicationServiceTest` 19 = 28（与交付轮口径一致） | **74/74，BUILD SUCCESS** | `B09-bpm-targeted.log` |

## 3. 验收期发现与处置（如实登记）

### 3.1 一次全绿，无验收期编排适配

与 B07 §3.1（IAM-004 套件漏注册）不同、承 B08 §3.2 先例：B09 交付链的 PG 套件注册齐全（BPM-003 第 21 套件随 V2.04 交付时注册），full 门禁 15 项与 PG 回归 21 套件均首轮一次全绿，无验收期补丁、无失败复跑、无脚本适配。

### 3.2 既登记 flaky 家族未在轮内出现，边界延续

module-system 顺序依赖 flaky 家族（B08 放行报告 §3.1 实锤、`SmsCodeServiceImplAttemptLimitTest` 3 例为其稳定成员）本轮不在执行面内（G11 未启用、无 module-system 全量批），维持「测试基建小卡建议」登记状态，不因本批放行而变化或扩大。

## 4. 逐卡验收结论（三卡，待验收→已验收）

验收规则：以 docs/05 各卡「验收」条款逐条对照 §2 实跑证据。各卡开发期证据（RED/GREEN、CodeReview R1→R2 复审弧、基线对照）见 docs/05 卡内开发记录与 docs/reviews/ 各档案（codex-ZS-BPM-002.md、codex-ZS-BPM-004.md、codex-ZS-BPM-003.md），本节登记放行轮实证。

| 模块 | 卡 | 验收条款 → 本轮放行证据（§2 编号） | 结论 |
|---|---|---|---|
| BPM | ZS-BPM-002 | 有效审批人正常办理（内部自动节点放行）→ #4 SecurityTest 18（外部命令拒/内部放行分支直证）；停用/其他租户/无资格人员不能被兜底或转派选中 → #4 Invoker 8 + AssignEmpty 2（兜底候选人纳入禁用过滤，不以停用账号补位）+ #4 SecurityTest 委托/转办/加签目标禁用拦截〔023/024/025〕；无 assignee 任务不能被任意登录人处理 → #4 SecurityTest `TASK_OPERATE_FAIL_NO_ASSIGNEE`〔1_009_005_021〕；猜流程/任务 ID 不能越权看历史 → #4 VisibilityChecker 7（发起人/历史参与人/抄送人三重可见性 + 越权 `PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE`）+ #4 SecurityTest 租户条件用例；不绕过获批自动节点规则 → #4 SecurityTest 内部/外部操作分离直证 | **已验收** |
| BPM | ZS-BPM-004 | 中性合同样例从提交到审计/事件/待办可验证 → #4 合同样例 11（submit 归属挂租户 + version=0 + SUCCESS 审计随业务事务 + 待办幂等注册 + 同事务内部事件；complete 版本乐观锁/终态守卫/DENIED 留痕/Outbox 同事务预写；deliverPendingTodoEvents 按租户直读 + Inbox MANDATORY）；绕过版本、伪造对象归属、重复来源消息失败或幂等 → #4（绕过版本 0 行即拒 / 伪造对象归属 / 跨租户隔离 / 重复注册 / 重复事件幂等用例）；真实系统接入逐系统回填合同与 UAT、不把示例通过算全部业务系统可用 → 中性红线维持（样例常量仅技术语义），真实接入逐系统回填归 B10/D-07（边界登记 §6） | **已验收** |
| OPS | ZS-OPS-001 | 主卡按 §16.1 子项分别登记证据、全部适用子项验收后转已验收：.A 聚合门禁入口/.B PG 层流水线/.C 安全与基础管理 API 层/.D G15 双端联调（各自历史批次证据在案，本轮经 #1 full 门禁 15/15 回归复验仍全绿）；.E「PG 流程、幂等写回和跨组织反向通过」→ #1 G16 48.9s + #3 G16 专项 2/2（bpm001 tests=9 四操作/重启恢复/事务回滚/积压恢复 + S1~S3 结构检查；bpm003 tests=7 幂等/晚到/并发/跨租户反向）；正向样例通过→#1/#2/#3/#4 全绿；未执行外部验证保持未执行→多端业务 E2E 与真机/UAT 明确归 B10/B11 边界（§6） | **已验收**（.A~.E 全部子项随批验收） |

ZS-BPM-003 不在本轮翻卡（维持**开发中**）：V2.04 登记其「实现交付，须 B09 批次真实环境放行验收」——该真实环境放行验收的**技术面**已由本轮 #2 PG 第 21 套件（真实引擎 7 例）+ #3 G16 专项覆盖；其验收条款「首链正常通过/拒绝/撤回/转派**与业务状态一致**」的完整闭环依赖首链业务模块（lead 权威对象）落卡后复核，随边界登记 §6.1 处置。

## 5. 正向/反向验收对照（docs/03 §B09 条款）

- **B09 正向**（「从入口、提交、审批、分配、处理、消息、交接到下一动作闭环；员工/负责人/总部指标来自同一事实」）：提交→审批→领域写回闭环 = #3 G16 串联（bpm001 PG 流程四操作〔通过/拒绝/撤回/转办〕+ bpm003 FirstChain `create/submit/onApprovalCompleted/withdraw/restart` 五命令与幂等门/版本条件更新/流程绑定同事务）；分配/处理/交接 = FirstChain 线索五态状态机（DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED/INVALID）fail-closed 迁移；消息/待办 = BPM-004 合同样例待办幂等注册与 Sink 投递（Inbox MANDATORY）；指标同源 = `FirstChainMetricsService` 从权威状态列 GROUP BY 派生（无独立计数器，员工/负责人/总部同表同口径）。
- **B09 反向**（四例）：重复审批回调不重复建对象 → 幂等命令门（process_instance_id 全局唯一键 + 重复同结果吸收、异结果显式冲突非覆盖）+ #3 bpm003 幂等用例；旧版本更新冲突 → 版本条件更新 0 行读回三分类（NOT_EXISTS/VERSION_CONFLICT/STATE_CONFLICT 可回查）+ #4 合同样例乱序旧版本水位用例；流程完成不能绕过领域校验 → 状态机唯一权威迁移表 fail-closed（未知对象/状态/自环恒 false、终态锁定）+ 非.SUBMITTED 晚到弃单留痕不覆盖新版本；跨组织对象和附件不可见 → #3 跨租户反向用例（tenant 2 上下文对 tenant A 申请 NOT_EXISTS 分类不泄露 + SQL 层 COUNT 直证独立于服务层，归属租户撤回不受影响）。
- **执行入口覆盖**（§B09 点名）：首链状态机测试 = #4 FirstChainStateMachines 9 + ApplicationService 19；Flowable PG 集成测试 = #2 #9 #21 双套件 + #3 G16；重复回调/并发更新测试 = #3 bpm003 幂等/并发/晚到组 + #4 合同样例；多端 E2E = #1 G15 18 用例（CLIENT-005.B 技术联调层）；多端**业务** E2E 与真实角色 UAT 归 B10（§6.3）。

## 6. 未验证项与登记边界（不随本轮放行提升）

1. **首链业务模块未落卡**：lead 权威业务表、双端工作台页面、待办接线（PILOT-REQ-010）循 V2.05 登记随 B09 排期新卡登记 docs/05 后实施；ZS-BPM-003 主卡完整闭环（验收条款「与业务状态一致」）随该落卡复核转验收。
2. **多端业务 E2E 与真实角色 UAT 归 B10**（循 §B10 「真实角色 UAT 报告单列，不能由自动化测试代替」）；B09 的 G15 为技术联调层 E2E（CLIENT-005.B 18 用例），不代替业务角色 UAT。
3. **B08 移交项延续**：`OrgDataPermissionRule` SQL 列表过滤 + 业务表 `org_id` 注册随首张领域业务表（lead）落地联验；字段等级目录业务域接入、CLIENT-001.B 页面渲染接线随域接入（B08 报告 §6.1~6.3 原边界不变）。
4. **测试基建小卡建议**（B08 §6.5 延续）：module-system 顺序依赖 flaky 家族根治（跨类上下文残留审计），不阻塞放行。
5. SERIALIZABLE 性能监控、容量/恢复演练、真机与发布归 B11（ZS-OPS-002.C / CLIENT-005.C / OPS-003）；微信/支付/AI 待 D-10/D-11。

## 7. 回滚路径

- 本轮验证全部使用一次性 Docker 容器与 `outputs/` 本地日志，**未触碰任何长驻数据**；G16 与 PG 套件容器验后自清理。
- 状态翻转（三卡）经 `scripts/gov/close-task.mjs` 幂等执行 + `verify-docs` 0 issue 复核；如需回退，`git revert` 放行提交即可整体还原（docs/05 §2/§19/头部状态行、docs/03 §9、README 统计随提交原子回退）。
- 本轮无产品代码变更、无迁移、无脚本适配（§3.1），回滚面仅文档。
