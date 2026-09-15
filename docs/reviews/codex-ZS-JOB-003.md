# ZS-JOB-003 codex 评审处置（r0→r6 六弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--dangerously-bypass-approvals-and-sandbox` 读评工作区，多轮以
  **Java-on-PG 隔离探针实锤并发交错**）
- 评审对象：分支 `feat/job-003`（ZS-JOB-003 消费者业务幂等、乱序与回查——Inbox 机制，JOB-002 Outbox 的读侧对称件）
- 结论：**r6 PASS / 0×P0/P1**（六弧收敛：r0 FAIL 4×P1+4×P2+P3 → r1 FAIL 3×P1+P2+2×P3 → r2 FAIL →
  r3 FAIL 2 功能+6×P3 → r4 FAIL 1×P2+4×P3 → r5 FAIL 1×P2+3×P3 → r6 PASS）
- 说明：各弧 raw 输出未随暂存保留（会话内仅保留 tailed 摘要），发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `aaaa6eba`，13 files；处置至 `4510a003`）

1. **处理键唯一抢占**：`ConsumerInboxPort` / `JdbcConsumerInboxPort`（`cn.zszj.module.infra.framework.inbox`）
   以 (tenant_id, consumer, event_key) 唯一键在业务事务内抢位（MANDATORY 同源配对 + ConnectionHolder 毒化
   fail-closed，循 JOB-002 同款缺陷预防，包内独立成 `InboxTransactions`）；业务回滚抢位一并回滚——同事件
   并发、ACK 丢失、重启重放由 DB 硬兜底只产生一次业务副作用。
2. **七态合同**：CLAIMED / DUPLICATE_COMPLETED（携可重放首次结果）/ DUPLICATE_IN_FLIGHT /
   DUPLICATE_RESULT_UNKNOWN（须先回查不得盲目重处理）/ RETRIED_CLAIMED（FAILED 重入即重领）/
   PARAM_CONFLICT（同键不同载荷指纹不当相同成功）/ STALE_VERSION（旧版本不覆盖新状态）。
3. **对象版本水位表** `inbox_object_watermark`（codex r1→r5 演进定稿）：唯一键
   (tenant, consumer, biz_type, biz_id) INSERT-or-撞键（保存点回滚）+ FOR UPDATE 行锁持至业务事务提交；
   水位以首次尝试版本初始化（支持负数）；**占坑语义**——新版本已尝试即封锁更旧版本（含已提交的失败占位），
   水位与副作用同生共死、回滚即回落；等值属重试/重放不拒绝。
4. **可回查中间态与确认出口**：RESULT_UNKNOWN 经 `resolveAfterVerification` 按回查依据推进
   （executed→COMPLETED / 未执行→FAILED 可重试）；`find` / `listByStatus` 回查端口。
5. **推进/回查全量租户强制**（跨租户运维显式接口登记待办）；失败留痕受控描述不落原文（循 JOB-002 惯例）；
   迁移 `V20260915.002__infra_inbox_event.sql`（inbox_event + inbox_object_watermark，唯一键含租户 + CHECK）。

## 评审弧（Java-on-PG 探针逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`aaaa6eba`） | FAIL 4×P1+4×P2+P3 | P1 首抢撞键后 PG 事务 23505 中止、复判查询 25P02 → INSERT 前 SAVEPOINT 撞键回滚复判；P1 版本护栏无锁快照可被并发反超 → 行锁串行化；P1 listByStatus 跨租户泄漏、推进可动他租户记录 → 全量租户强制；P1 RESULT_UNKNOWN 无出口永久悬挂 → resolveAfterVerification。P2 STALE 优先级/FAILED 竞争分类/批次隔离/时钟 → 同弧修复 `5d997c2e` |
| r1（`5d997c2e`） | FAIL 3×P1+P2+2×P3 | P1 行锁取「最新 id」非「最高版本」且空集无锁、快照读旧值 → **独立水位表**稳定串行化；P1 撞键复判跳过指纹 → 完整判定入口 handleExisting；P1 FAILED 竞争误报处理中 → 有界复判 |
| r2（`596ddec8`） | FAIL | P1 被拒请求（PARAM_CONFLICT/撞键）仍抬水位 → claimNew 重排「先占处理键后过护栏」，拒绝随保存点撤销占位与水位 `00e0461f` |
| r3（`00e0461f`） | FAIL 2 功能+6×P3 | P1 FAILED 分支基于过期快照（并发已推进为 COMPLETED/UNKNOWN 却返回 STALE 遮蔽回查/重放）→ 分支先 SELECT…FOR UPDATE 锁行重读再过护栏（锁序统一「记录行→水位行」）；P2 重领返回由过期快照构造 → 重读返回；P3×6 文档 → `6ce7ef46` |
| r4（`6ce7ef46`） | FAIL 1×P2+4×P3 | P2 撞键复判以 depth=1 进入、锁重读后正常状态因预算耗尽被抛异常 → 去掉 depth 参数化递归（分类有限收敛）；P3 文档×4 → `9830bd55`；**r4 补丁**：record 注释内嵌 `*/` 提前终止 javadoc 致编译阻塞 → `9a16ba7a` 修正（如实登记：该提交说明中「测试 16/16」不作为当时 HEAD 的通过证据，编译修复后才复跑通过） |
| r5（`4510a003` 前弧） | FAIL 1×P2+3×P3 | P2 水位固定初始 0 使首个负整数版本被误拒 → 水位以首次尝试版本初始化；P3 STALE「不低于」→「严格高于」、IN_FLIGHT 去「未提交」措辞、record 可空合同 |
| r6（`4510a003`+`9a16ba7a`） | **PASS / 0×P0/P1** | 「r4/r5 实现问题已闭环，未发现新增 P0/P1/P2 缺陷；已复跑 Inbox 17 用例、PG 7 项、fast 10/10」 |

## 验证

- TDD：RED（骨架 13 用例全败）→ GREEN。
- H2：`JdbcConsumerInboxPortTest` 17 用例（infra 全量 356/0 BUILD SUCCESS）——同事务回滚/可重放结果/
  并发抢占识别/参数冲突/租户隔离/版本水位单调与负版本/RESULT_UNKNOWN 回查与确认出口/推进租户强制/
  MANDATORY 事务合同/吞守卫异常回滚/同源配对。
- PG：`scripts/db/run-job003-verify.mjs` 7 用例×多轮全绿（唯一键 speculative insertion wait 并发抢占/
  A 回滚重放可登记/跨租户隔离/状态条件推进与重领计数单调/CHECK+NOT NULL），注册为 `run-pg-regression`
  第 11 套件。
- 门禁：fast 10/10；迁移规范 0 issue。合并 `--no-ff` main `21dacd7f`（并行 B04 批次登记 V1.55 先落，无冲突）。

## 登记边界（codex r6 非阻塞项，如实登记）

1. **F1 确定性并发回归待补**：首抢撞键→读到 FAILED→锁重读前被竞争者推进的三态交错（codex 探针已实测
   正确）尚未沉淀为正式回归用例——归后续小卡/批次真实环境。
2. **采用 Inbox 的 Outbox Sink 中性接线与 Java-on-PG 正式联验**归 D-07 领域接线/批次真实环境
   （本卡交付机制层，消费方按七态合同接入；SEC-011.B 联验前置本卡即解锁）。
3. **跨租户运维显式接口**登记待办（当前查询/推进全量按当前租户过滤）。
4. **非数值版本语义**由事件类型解释（数值可解析才比较），D-07 阶段 2 细化。
5. **Web 请求防重边界不变**：本卡是持久化可重放业务幂等，不替代 SEC-011.A 短窗口防重，联验归 SEC-011.B。
6. 水位占坑语义（新版本失败占位后，更旧版本拒绝、待人工/补偿处置）为登记的语义决策（D-07）。
