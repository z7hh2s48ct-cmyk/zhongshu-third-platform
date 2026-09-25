# ZS-BPM-004 评审处置（建立新业务模块接入合同与示例验收：CodeReview 独立复审两轮，R2 PASSED / 0×P0/P1）

- 评审对象：ZS-BPM-004 建立新业务模块接入合同与示例验收——中性合同样例（提交→审计/事件/待办全链路可验证）+ 12 项接入合同沉淀 + 同事务内部事件与跨进程事件边界（B09 技术准备 Wave 次卡；docs/05 §13 ZS-BPM-004 卡「中性合同样例从提交到审计/事件/待办可验证；绕过版本、伪造对象归属、重复来源消息失败或幂等；真实系统接入逐系统回填合同与 UAT，不把示例通过算全部业务系统可用」，前置 B03～B05 技术合同〔审计/Outbox/Inbox/待办/文件交付设施均已交付〕）
- 隔离分支：worktree `.wt/zszj-wt-bpm-004` 分支 `feat/bpm-004`（自 main `af6b142a`；RED `1f1ef6cf` + GREEN `f77a040a` + R1 修复 `fcfdc574` + R2 修复 `c1ea58db`）
- 合并：`--no-ff` main `cc0d06ab`（6 files +1053，feat 分支自 `af6b142a` 线性领先、干净合并）
- 卡片性质：**技术准备沉淀卡**——test 作用域中性样例 + 文档沉淀，无 src/main 变更、无生产迁移
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B / ZS-PERM-001.B / ZS-FILE-001.B / ZS-MSG-003.C / ZS-CLIENT-002.B / ZS-BPM-002 先例），改以 **CodeReview 独立复审替代**
- 结论：**R1 报 1×P1（0×P0；2×P2）→ 修复（补交文档 + 终态守卫，`fcfdc574`）→ R2 独立复审 PASSED（0×P0 / 0×P1）**（R2 新发现文档用例编号漂移 P2 随修 `c1ea58db`）
- 原始日志：`outputs/_bpm004-*.log`、`outputs/_bpm004-r1-review.md`、`outputs/_bpm004-r2-review.md`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（卡片边界 + 中性红线）

B09 技术准备 Wave 第二卡（BPM-002 之后）：把「新业务模块接入合同」沉淀为 12 项可执行条款，并以中性样例作为未来模块接入方的参照实现。中性红线依据 docs/02 §6.5（D-07 确认前只允许 PG 适配与中性技术夹具、禁止固化业务字段/状态）与 docs/05 §13 卡「不为未来域预建业务表」。样例演示四条设施链路的真实事务语义（对照 B03~B05 已交付设施源码逐项坐实）：SUCCESS 审计随业务事务 fail-closed（`JdbcAuditPort` REQUIRED + rollback-only）/ DENIED 独立留痕（REQUIRES_NEW）/ Outbox 追加 MANDATORY（无真实事务即拒 + 租户缺失即拒）/ Inbox 抢位 MANDATORY（保存点 + 版本水位同事务 RAISE）；并直证 `OutboxDispatcherService` 投递路径不提供事务（D-07 首链接线任务，接线方须编程式事务包裹消费侧投递）。

## 交付内容（RED `1f1ef6cf` + GREEN `f77a040a` + R1 修复 `fcfdc574`）

**中性合同样例**（`cn.zszj.module.bpm.contract`，test 作用域）：

- `TechNeutralContractSample` 三链路：
  - `submit`：对象归属挂租户（`requireTenantId` 缺失即拒、无默认 0）+ `version=0` 起步 + SUCCESS 审计随业务事务 + 待办幂等注册（`(租户, sourceType, todoKey)`）+ 同事务内部事件（进程内同步，仅演示边界）；
  - `complete`：版本乐观锁（携带 `expectedVersion`，0 行即拒）+ **终态守卫**（`AND status = PENDING`，仅允许 PENDING→DONE，携带当前版本的重复完成 0 行即拒）+ DENIED 拒绝留痕（REQUIRES_NEW，业务回滚不丢失）+ 新版本 SUCCESS 审计 + Outbox 流转事件同事务预写（MANDATORY）；
  - `deliverPendingTodoEvents`：按租户直读 PENDING（`requireTenantId`）+ 事件自身租户上下文（`TenantUtils.execute`）+ 编程式事务包裹 Sink 投递（Inbox MANDATORY 合同，D-07 首链接线演示）+ 投递与确认同事务原子（失败不静默确认，事件保持 PENDING 待重试）。
- `TechNeutralContractInnerEventListener`：同事务内部事件记录（进程内扩展演示）。
- `TechNeutralContractSampleTest`：11 用例端到端（提交链路 / 完成推进 / 派发确认 / 绕过版本 / 重复完成终态守卫 / 乱序旧版本水位 / 无租户双入口 / 跨租户隔离 / 重复注册 / 重复事件）。
- BPM 测试库设施表 DDL：6 张表（audit_event〔含幂等唯一索引〕/ outbox_event / inbox_event / inbox_object_watermark / system_notify_todo / tech_neutral_contract_record，照抄 system 侧已验证 DDL）+ `clean.sql` 补齐清理保持用例隔离。

**接入合同沉淀件**（`services/zhongshu-core/docs/新业务模块接入合同.md`，125 行）：

- §1 十二项接入合同（模块注册 / API·DTO / 对象归属 / 版本并发 / 引用历史 / 动作与字段权限 / 审计 / Outbox / 待办 / 私有文件 / 流程适配 / 跨系统声明，每项含要求 / 对接点 / 验收方式）；
- §2 同事务内部事件与跨进程事件边界表 + 判定法则；§3 不为未来域预建业务表（约束）；§4 事务语义总表（B03~B05 设施实测口径）；§5 与 D-07 的关系与红线；§6 变更记录。

## 新增/改测试

| 测试 | 用例 | 断言 |
|---|---|---|
| `TechNeutralContractSampleTest`（新） | 11 | 三链路端到端：SUCCESS 审计随事务（3 条）、待办幂等、Outbox 事件字段齐备、绕过版本拒绝且无副作用（0 行 + DENIED 留痕）、**重复完成终态守卫**（状态保持 DONE / version 不变 / 成功审计仅 1 条 / Outbox 仅 1 条 / DENIED 留痕 biz_version=1）、乱序旧版本水位拒绝、无租户双入口拒绝、跨租户不可见不可改、重复注册返回既有、重复事件不重复副作用 |

## RED → GREEN → R1/R2 修复

- **RED**（实现前）：Tests run: 10, Failures: 1, Errors: 8（9 预期失败 = 8×UnsupportedOperationException + 1×断言类型不匹配；1 直证通过〔用例⑦设施无租户拒绝〕；编译零错误、Spring 上下文装配成功）；
- **GREEN**（实现后）：10/10 全绿（唯一失败为测试断言过宽〔用例④全表 SUCCESS 统计误含 submit 的 OBJECT_CREATED〕，修正为限定 OBJECT_UPDATED）；
- **R1 修复后**：**11/11** 全绿 BUILD SUCCESS（`outputs/_bpm004-r1fix.log`，+1「重复完成被拒」用例）；
- **R2 独立复跑**：11/11 + BUILD SUCCESS（评审方独立执行，未依赖修复方日志）；
- **模块全量**：97 run（= 基线 96 + 本卡 1）残留 1F+2E（`BpmFormServiceTest.testGetFormPage` + 2 harness env errors〔缺 `ZSZJ_BPM_HARNESS_SCHEMA_UPDATE` 环境变量〕）经基线对照确认预存在、非本卡引入（`outputs/_bpm004-module.log` / `_bpm004-module2.log`）。

## CodeReview 独立复审发现（R1 → 修复 → R2 PASSED，0×P0/P1）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P1-1 | **P1** | `TechNeutralContractSample` 类级 javadoc / `services/zhongshu-core/docs/` | 本卡交付物「沉淀接入合同」未落盘：javadoc 引用的《新业务模块接入合同》文档在仓库中不存在（死链），卡片验收要求"沉淀 12 项接入合同清单"未随代码交付 | **修复** `fcfdc574`：补交文档（125 行：12 项接入合同 + 事件边界 + 预建约束 + 事务语义总表 + D-07 红线），javadoc 引用恢复有效 |
| P2-1 | P2 | `TechNeutralContractSample#complete` | 未守对象终态：UPDATE 仅以版本条件拒更新——对象已达 DONE 且调用方恰好携带当前版本号时 UPDATE 仍可命中（version 匹配），可重复完成并产生第二次成功审计与重复流转事件 | **修复** `fcfdc574`：UPDATE 增加 `AND status = ?`（STATUS_PENDING）终态守卫 + 配套用例「重复完成被拒」（钉住状态/版本不变、成功审计与 Outbox 各仅 1 条、DENIED 留痕） |
| P2-2 | P2 | `TechNeutralContractSampleTest` 用例⑪ | 重复事件幂等用例机制归因不唯一（黑盒口径无法区分"防重机制生效"与"事件本身未达消费条件"） | **登记不改**（黑盒口径可接受，R1 裁定） |
| P2-1（R2） | P2 | `新业务模块接入合同.md` §1.3/§1.8/§1.9 | 用例编号漂移：R1 插入「重复完成」用例为文件序⑤后，其余用例编号整体后移，文档三处引用仍为旧编号且指向错误用例 | **随修** `c1ea58db`：修正三处编号（§1.3 ⑥⑧→⑦⑨、§1.8 ⑦③⑩→⑧③⑪、§1.9 ⑨→⑩），采用 R2 建议方案 A；纯文档修复不涉及代码路径 |

R2 独立复审（worktree 内独立执行 git diff / 逐行通读 / 设施源码对照 / DDL 逐列比对 / 日志核验 / **独立复跑**）确认 P1-1 与 P2-1 闭环成立：①文档存在且 12 项齐备、javadoc 与文档头部双向引用均命中真实文件、§编号自洽、对接点文件抽查全部真实存在；②终态守卫代码闭环（0 行 → DENIED 独立留痕 + 抛出）、新增用例具真实守护力（反向推演：移除状态条件后用例立即失败）、无误伤（首次完成 PENDING 匹配命中、回滚重试仍可正常重试）、复跑证据可复现（R2 独立复跑 11/11）。全量 6 文件独立复核（事务语义合同 / 租户归属 / OutboxEventRecord 14 参数与表列 / 中性红线 / 测试有效性 / DDL 与 system 侧一致 / 变更范围仅 test·docs）均通过。**PASSED / 0×P0 / 0×P1**。

## 验证（交付时点证据）

- TDD RED：10 run / 9 预期失败 + 1 直证（`outputs/_bpm004-red.log`）；
- GREEN：10/10（`outputs/_bpm004-green.log`）；R1 修复后：**11/11** BUILD SUCCESS（`outputs/_bpm004-r1fix.log`）；
- R2 独立复跑：11/11（评审侧独立执行）；
- 模块全量：97 run（= 基线 96 + 本卡 1）残留 1F+2E 经基线对照预存在（`outputs/_bpm004-module.log` / `_bpm004-module2.log`）；
- CodeReview 独立复审替代：R1（0×P0+1×P1+2×P2 → 修复 `fcfdc574`）→ R2 PASSED / 0×P0/P1（P2 编号漂移随修 `c1ea58db`）（`outputs/_bpm004-r1-review.md` / `_bpm004-r2-review.md`）。

## 卡片状态与统计

主卡 ZS-BPM-004 由待开发转**待验收**（B09 技术准备 Wave 次卡，技术准备沉淀件；B09 批次真实环境放行前，循 ZS-DB-010/IAM-002/IAM-004/BPM-001/BPM-002 先例）。第 2 节统计随之待开发 1→0、待验收 4→5（0/20/5/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- **真实系统接入逐系统回填合同与 UAT**归 D-07 批准后（本件为技术准备沉淀，**不因样例通过而豁免**逐系统回填与 UAT）；
- **P2-2 用例⑪机制归因黑盒口径**登记不改（黑盒断言口径可接受，未触碰防重机制内部）；
- **`OutboxDispatcherService` 投递路径与消费侧事务包裹核实**为 D-07 首链接线任务（已由 `NotifyTodoEventSink` javadoc 与接入合同 §4/§5 双重登记）；
- **中性红线**：D-07 批准前不得固化任何业务字段、状态机与组织语义（样例常量/载荷/状态只含技术语义：key / PENDING / DONE / version）；
- 本卡为 B09 技术准备域内合同沉淀与样例，不涉及 org 轴数据授权注册与业务域建模（该边界随 D-07 下游）。
