# ZS-MSG-004 codex 评审处置（r0→r2 三弧）

- 评审工具：codex 风格严苛只读评审弧（xhigh，read-only subagent，逐弧独立会话）
- 评审对象：分支 `feat/msg-004`（ZS-MSG-004 渠道发送状态、失败重试与回执对账，B05——前置 MSG-001/JOB-002/JOB-003 均待验收；真实外部能力按 D-10 门禁）
- 结论：**r2 PASS / 0×P0/P1/P3；1×P2 为域级既有发现（非本卡引入，建议立卡跟踪）**（三弧收敛：r0 FAIL 1×P1+4×P2+6×P3 → r1 FAIL 1×P1+4×P3 → r2 PASS 1×P2 域级）
- 说明：各弧裁定行与发现已逐条摘录如下，如实登记；raw 输出保留于开发窗会话产物（未入库，outputs/ 已 gitignore）。

## 交付内容（impl commit `775c2690`，27 files +2497/-31；迁移改号 `4a7cb8b6`；r0 处置 `fdb12546` +206/-66；r1 处置 `9dc44ce9` +48/-41；r2 自愈 `3d95d3c8` +9）

1. **渠道发送生命周期台账 `system_notify_channel_send`**（迁移 `V20260916.002`，原 `.001` 因 feat/job-004 已提交同名迁移循「未合并迁移可改号」先例避让改号）：五态 `PENDING/ACCEPTED/DELIVERED/FAILED/UNKNOWN` 独立承载渠道侧投递（与站内消息已读、业务待办、派发日志状态分别建模——众墅要求「不能仅由入队成功推定送达」）；渠道幂等键 `channel_message_id` 创建即生成、全链路一致（`uk_notify_channel_send_receipt` 回执唯一定位）；`attempt_count/receipt_count/manual_retry_count` 只增不减（attempt 口径=状态推进次数，r0 P3 订正）；`uk_notify_channel_send_log` 一对一挂接派发日志；r1 补 `idx_notify_channel_send_outbox(tenant_id,outbox_event_id)` 投递定位覆盖索引。派发日志 CHECK 增补 `CHANNEL_NOT_CONFIGURED` 九态。
2. **NotifyChannelSender SPI + 注册器**：提交三态（受理携流水号 / 明确拒绝 / 未知）+ 按幂等键回查；注册器空 = 渠道未配置 → 派发侧 `CHANNEL_NOT_CONFIGURED` 明确阻断（区别 `NO_CHANNEL`「未指定」，不静默丢弃）；渠道幂等键在 SPI 合同为**强制条款**（实现必须按键在渠道侧去重——at-least-once 残余窗口的兜底前提）；Mock 实现只存在于测试装配（D-10 门禁，Mock 不作为真实渠道验收）。联系方式随 `NotifyRecipientContext` 携带（同一 AdminUserRespDTO 不增加查询），`NotifyChannelContacts.contactFor` 统一渠道映射（SMS→mobile / EMAIL→email / PUSH 待 D-10），缺失由台账 `FAILED(RECIPIENT_CONTACT_MISSING)` 明确阻断不入管道。
3. **可靠投递复用 JOB-002**：派发同事务建台账 PENDING + 追加 `NOTIFY_CHANNEL_SEND` Outbox 事件（`ReliableEventPort` MANDATORY），`NotifyChannelSendEventSink` 消费——**事件身份吸收（r0 P1）**：服务按 `(租户, outbox_event_id)` 定位台账，陈旧/换绑/外来事件幂等吸收不提交；PENDING 提交 / UNKNOWN **先回查**（确认未发出才重发，重发携带同一幂等键）/ 受理·送达·失败重投幂等吸收绝不重复发件；提交未知/技术异常转 UNKNOWN 抛可重试异常交 Outbox 退避（退避与 DEAD 归 outbox_event，本表不重复记账）；服务内部短事务 CAS 推进（外部调用严格事务外）。
4. **回执校验与对账**：回执按幂等键定位 + 流水号比对（未知/错配显式拒绝）；**回执权威**——任意非终态直接推进（乱序不丢事实），终态重复回执 DUPLICATE 吸收且计数可追踪，矛盾回执 CONTRADICTION 留人工（r0 P2：CAS 落败回退路径同判）；`applyReceipt` 永不触发发件。`manualRetry` 仅 PENDING/FAILED/UNKNOWN 可复位（旧事件绑定条件 CAS 真互斥——并发双击仅一次成功），操作者/原因/次数留痕 + 新投递事件；联系方式缺失记录复位前经 Resolver **重新解析**（补绑后可恢复），仍缺失以新码 `1_002_031_004` 拒绝；`reconcile` 对 ACCEPTED/UNKNOWN 主动回查对齐台账与渠道事实（对账出口不自动重发）。
5. **r1 P1 方言修复**：casManualRetry NULL 安全子句补 `jdbcType=BIGINT`（MyBatis 默认 `jdbcTypeForNull=OTHER` 经 pgjdbc 绑定 OID UNSPECIFIED，`? IS NULL` 无列上下文报 42P18，恰命中无事件绑定的阻断记录恢复路径）；真实 PG 探针实证三路（NULL 绑定/非 NULL 匹配/不匹配）UPDATE 计数符合预期。r2 P2 自愈：本卡 DO 字段级覆写 `@TableLogic(value="FALSE", delval="TRUE")`（MSG 域 boolean deleted 列 vs 全局 0/1 字面量在 PG 的 42883 错配，本卡表自洽；域级处置立卡）。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0 | FAIL 1×P1+4×P2+6×P3 | **P1** 陈旧/重叠投递事件无吸收机制（manualRetry 换绑后旧事件重投与在途投递重叠→重复外部提交，selectByOutboxEventId 备而未接）→ 事件身份吸收接线 + SPI 幂等键升强制条款。P2：REJECTED 拒绝码未截断列宽 64（终局失败落不了地退化为反复重提）→ truncate；casManualRetry PENDING→PENDING 无条件空转无并发保护 → 旧事件绑定条件 CAS；RECIPIENT_CONTACT_MISSING 记录人工重试死端（联系方式冻结不重解析）→ 复位前重解析 + 新错误码；回执 CAS 落败回退把矛盾回执误分类 DUPLICATE → 复用终态一致性判定。P3：attempt_count 口径订正 / selectByOutboxEventId 接线 / 重试事件 actor 透传 / 移除未用 DataSource 参数 / getNotifyChannelSend 租户校验 / casQueryNotSent 清 last_error / 文档漂移订正 |
| r1（r0 处置核验） | FAIL 1×P1+4×P3；九项落地、两项机制正确但 NULL 分支不可执行 | **P1** NULL 安全子句 PG 42P18（`? IS NULL` 占位符 jdbcTypeForNull=OTHER 绑定 UNSPECIFIED 无法推断类型，恰命中补绑后人工重试恢复路径；H2 单测不解析该缺陷）→ `jdbcType=BIGINT` 双处显式声明 + 真实 PG PREPARE 探针三路实证。P3：selectByOutboxEventId 必经查询缺覆盖索引 → 迁移补 (tenant_id,outbox_event_id)；手写 @Select 缺 deleted=FALSE → 三处补齐；resolveChannelContact 双份静态拷贝 → 提取 NotifyChannelContacts 去重；提交信息用例计数口径订正 |
| r2（r1 处置核验） | **PASS** 0×P0/P1/P3；1×P2 域级既有 | r1 五项处置全部真实落地且语义一致（P1 修复经 MyBatis DefaultParameterHandler / pgjdbc setNull 源码级四层证据链证实：显式 jdbcType 永远优先、BIGINT→Oid.INT8 显式映射、非 null 路径零副作用）；事件身份吸收/claim-first/回执权威/MANDATORY/租户双保险抽查完好。**新增 P2（域级，非本卡引入）**：MSG 域三表（MSG-001 send_log / MSG-002 todo / 本卡 channel_send）`deleted boolean` 列与全局 `@TableLogic` 0/1 数值字面量在 PG 方言错配——MyBatis-Plus 注入方法在 PG 报 42883（audit_event 无 deleted 列、outbox 纯 JdbcTemplate、DB-018 时代 int2 表恰好绕开注入层；ORM 注入路径在真实 PG 从未端到端验证，H2 MODE=MYSQL 宽容掩盖）→ 本卡 DO 字段级覆写 `@TableLogic(FALSE/TRUE)` 自愈；**MSG-001/002 同款表 + DB-018 ORM 级真实 PG 回归扩展，登记立卡跟踪（归 MSG 域统一处置）** |

## 验证

- H2：新增 NotifyChannelSendServiceTest 33 用例 + NotifyChannelSendEventSinkTest 5 + NotifyDispatcherChannelTest 6；NotifyDispatcherTest 17 用例随 `CHANNEL_NOT_CONFIGURED` 契约变更同步更新；合计 **61/61 PASS**（r0/r1/r2 各处置后复跑均绿）。system 模块全量 801 跑，5 失败均为主树基线同点复现的 OAuth2×2/SmsCode×3 顺序依赖既有问题（base 同样 5 失败），与本卡无关。
- fast 门禁：`run-local-gates --fast` **10/10 PASS**（G8 Flyway 迁移规范含改号后 V20260916.002 与补索引复验）。
- 真实 PG：迁移链四轮聚合中凡跑全链的 BPM-001/JOB-002/JOB-003 套件全绿；聚合器临时容器启动存在随机 flaky（四轮失败套件轮换：CFG-002/DB-018/DB-016/DB-017→DB-017→0→DB-006，均与 MSG 域无交集，单套件复跑即绿——DB-006 复跑 6/6）；r1 方言探针（迁移应用 + PREPARE 显式 bigint 三路 UPDATE）实锤通过。

## 登记边界（非阻塞）

1. **真实渠道适配器、渠道回调 HTTP 端点（含签名校验与租户解析）、投递驱动生产接线**归 D-10/B11——本卡为中性机制（无生产发送器时全链路休眠，循 MSG-002 先例）；Mock 不作为真实渠道验收，真实回执与费用相关验证单独登记。
2. 同事件租约过期同窗重领的残余 at-least-once 窗口由 Outbox 租约机制 + 渠道幂等键强制条款兜底（r0 P1 处置后仅剩该窗口）。
3. MSG 域 boolean deleted 列 vs @TableLogic 0/1 全局字面量的域级方言错配（r2 P2）：本卡已自愈，MSG-001/002 表与 DB-018 ORM 级真实 PG 回归扩展立卡跟踪。
4. 手写 @Select 均带显式 tenant 谓词 + deleted=FALSE；跨租户运维显式接口归 JOB-003 已登记项。
