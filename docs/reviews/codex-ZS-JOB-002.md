# ZS-JOB-002 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--dangerously-bypass-approvals-and-sandbox` 读评工作区）
- 评审对象：分支 `feat/job-002`（ZS-JOB-002 事务 Outbox 与可恢复投递机制，B05 核心使能器）
- 结论：**r3 PASS / 0×P0/P1**（四弧收敛：r0 FAIL 4×P1+4×P2 → r1 FAIL 2×P1+3×P2 → r2 FAIL 1×P1+4×P2 → r3 PASS）
- 说明：四弧 raw 输出未随暂存保留（会话内仅保留 tailed 摘要），各弧裁定行与发现已逐条摘录如下，如实登记。

## 交付内容（impl commit `b1d40292`，14 files +1266）

1. **event 端口**（`cn.zszj.module.infra.framework.outbox`）：`ReliableEventPort` / `JdbcReliableEventPort`——
   业务数据源同事务追加；`GeneratedKeyHolder` 替代供体 PG 专用 `RETURNING id`、`payload/headers` 落 `text`
   替代 `jsonb`（循 ZS-AUDIT-001 双方言惯例）；fail-closed：无真实事务拒绝（MANDATORY，不代开不静默自提交）、
   缺租户上下文拒绝（去除供体「缺 tenant 默认写 0」）、必填字段校验、守卫/序列化/写入失败统一标记
   所参与事务 rollback-only（吞异常也无法「无事件而提交」）。
2. **事件合同**（docs/05 调整条款）：eventId、bizVersion（对象版本）、tenant（取自上下文，消息合同不含租户字段）、
   actorType/actorId/traceId、bizType/bizId（对象引用）、payload/headers；领取记录全量回传（修供体
   「返回值未带回 tenant/header 上下文」缺口）。
3. **派发器** `OutboxDispatcherService`：`FOR UPDATE SKIP LOCKED` + 事件级租约领取；**每事件每次领取唯一
   UUID 凭证（claim_token）栅栏**——complete/fail 必须携带，过期旧执行者（含同实例旧线程）无法确认新领取
   （修供体仅 claimed_by=instanceId、同实例重领即失效的缺口）；两步领取（SELECT 锁行 + 同事务标记）等价
   供体单语句 `UPDATE..RETURNING`（H2 不支持后者，双方言改写，行锁保证无竞争）；`dispatchOnce` 无事务
   上下文强制（派发/业务事务分离）；Sink 在**事件自身租户上下文**内投递（`TenantUtils.execute` finally
   恢复调用线程）；无 Sink 按失败退避至 DEAD（可见失败不丢弃）；逐事件异常隔离；`last_error`/日志只存
   受控描述（errorClass/messageLength/常量码）不落异常原文；`heartbeat` upsert。
4. **同源配对强制**（`OutboxTransactions`，codex r2 产物）：DataSource 与 TransactionManager 构造期校验
   同源配对（TM 必须是管理同一数据源的 `DataSourceTransactionManager`），错配启动即失败——端口 MANDATORY、
   派发器 REQUIRED 两模板共用。
5. **迁移** `V20260915.001__infra_outbox_event.sql`：`outbox_event` + `dispatcher_lease`（SEQUENCE 主键惯例、
   `CHECK` status/retry_count、tenant_id NOT NULL）；**仅迁入通用机制**——供体票据/导出表归 ZS-FILE-004/005
   （他区在制）不迁，业务驱动器（design/AI/支付专属）不迁。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（HEAD `b1d40292`） | FAIL 4×P1 + 4×P2 | P1 必填/租户守卫在事务模板外、吞异常可无事件提交 → 守卫移入模板统一 rollback-only；P1 REQUIRED 在他数据源事务下另开事务静默自提交 → 事务守卫加资源绑定校验（r1 再收紧）；P1 Sink 投递未按事件租户 → `TenantUtils.execute` 隔离；P1 actor/trace 落库未回传 Record → 领取查询/Record 补齐。P2（脱敏/supports 批次隔离/时钟基准/方法可见性+约束）→ 同弧修复 `b59e5692` |
| r1（HEAD `b59e5692`） | FAIL 2×P1 + 3×P2 | P1 `getResource` 非空不可证明参与事务（他数据源事务下普通查询也绑定同步资源）→ 事务模板改 **PROPAGATION_MANDATORY**（TM 层校验真实事务，无即拒绝）；P1 TransactionAspectSupport 不覆盖编程式事务 → failClosed 改毒化绑定的 `ConnectionHolder`（@Transactional 与编程式同样生效）。P2 单键 JSON 绕过按键脱敏 → last_error 改受控描述不落原文；确认丢失重投补用例；时钟偏差登记 → 修复 `188345f4` |
| r2（HEAD `188345f4`） | FAIL 1×P1 + 4×P2 | P1 DataSource=A、TM=B 错配时 MANDATORY 加入他数据源事务、JDBC 写入走本数据源 autocommit 连接静默自提交（业务回滚而事件留存，无异常可捕获）→ 新增 `OutboxTransactions` 构造期同源配对强制（fail-fast），补 4 回归用例（TM 错配构造拒绝×2 / 假绑定拒绝 / 吞 DataAccessException 仍回滚）→ 修复 `47b77c35` |
| r3（HEAD `47b77c35`） | **PASS / 0×P0/P1** | 「r2 P1 已闭环，未发现新增 P0/P1。实跑 H2 25/25、PG17 8/8 通过」；余 6×P2 + 2×P3 建议登记（见下） |

## 验证

- TDD：RED 先行（骨架 17 用例全失败）→ GREEN；过程中定位并修复两处测试粒度缺陷（H2 微秒级
  `CURRENT_TIMESTAMP` 与毫秒参数的同毫秒假阴性；确认丢失用例的租约时序设计）。
- H2：infra 全量 **320/0（BUILD SUCCESS）**，其中新增 `OutboxEventPortTest` 12 + `OutboxDispatcherServiceTest` 13。
- PG：新增 `scripts/db/run-job002-verify.mjs` 8 用例（迁移重放+结构断言/业务回滚无事件提交必有/
  **双实例 SKIP LOCKED 领取不重复**/租约过期回收/**旧凭证栅栏**/DEAD 不再候选/CHECK+NOT NULL 硬约束），
  连续两轮全绿；`run-pg-regression.mjs` 聚合 **10 套件全绿**（ZS-JOB-002 已注册为第 10 套；BPM-001 因
  后台 shell 无 tools 报环境性失败，主树补跑 6/6 绿）。
- 门禁：`run-local-gates --fast` 10/10；迁移规范检查 0 issue。
- 合并：`--no-ff` 合并 main `b2266717`（无冲突；并行会话 P2 硬化收口 V1.52 先落，无迁移/文件交集）。

## 登记边界（codex r3 P2×6，非阻塞，如实登记）

1. **PG 套件为 SQL 级验证**（语句形状对齐生产实现），非调用生产 Java 类的两步领取/事务配对/参数绑定集成探针
   （codex 各弧自跑探针已实测互斥/栅栏/回滚正确）——Java-on-PG 集成测试待建，归 ZS-JOB-004 或批次真实环境。
2. 确认丢失用例以「省略 complete + 修改租约」模拟崩溃；complete 数据库异常、同实例双线程重领、旧 fail 与
   新终态并发的直接证据（故障注入/并发回归）待补，真实进程终止演练归批次真实环境。
3. **Sink 幂等目前为接口合同**，测试仅证明重复投递发生、未证明副作用去重——消费侧持久化幂等
   （tenant＋consumer＋eventId 唯一键 + 副作用同事务，不得以 claim_token/短 TTL 防重替代）归 **ZS-JOB-003**。
4. 各实例系统时钟独立（应用侧参数化≠共享时钟）：偏差会提前重领或延迟恢复，`timestamp` 无时区需统一
   JDBC/JVM 时区——时钟偏差容限、可注入 Clock 的偏差测试按批次真实环境复验。
5. `heartbeat` 在外部事务内首次插入唯一键竞争后，同事务重试 UPDATE 会遇事务已中止——建议调用约定
   强制无事务上下文（与 dispatchOnce 同规）或改独立事务重试，归 ZS-JOB-004 台账/运行说明。
6. claim 路径索引（status, next_retry_at, id）在大积压+大量有效租约下未做 `EXPLAIN ANALYZE` 实证，
   LIMIT 也不限制扫描量——归 ZS-JOB-004（积压/最长等待/健康监测）与批次真实环境容量复验。
   另 P3：actor_type 枚举与领取三字段一致性可补 CHECK（保持 H2 同步），随上述一并考虑。
