# ZS-MSG-001 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`exec -s read-only` 读评工作区）
- 评审对象：分支 `feat/msg-001`（ZS-MSG-001 统一通知入口与接收人上下文，B05/B10 核心使能器——MSG-002/MSG-004 前置）
- 结论：**r3 PASS / 0×P0/P1/P2/P3**（四弧收敛：r0 FAIL 1×P1 → r1 PASS 2×P2+1×P3 → r2 PASS 1×P2 → r3 PASS 零发现）
- 说明：r0~r3 四弧 raw 输出保留于开发窗 `outputs/msg001-codex-r{0,1,2,3}.log`（未入库，outputs/ 已 gitignore），各弧裁定行与发现已逐条摘录如下，如实登记。

## 交付内容（impl commit `4f6ce7fb`，17 files +1425；合并 main `c7d1ad57`）

1. **dispatch 子包（10 类，`cn.zszj.module.system.service.notify.dispatch`）**：`NotifyDispatcher`/`NotifyDispatcherImpl`
   + 命令/结果合同 `NotifyCommand`（eventId 幂等键、bizType/bizId 对象引用、actorType/actorId、templateCode、
   templateParams、channels、recipients）、`NotifyDispatchResult`、`NotifyDispatchStatus`（八态：SUCCESS /
   DISABLED_TEMPLATE / NO_CHANNEL / RECIPIENT_INVALID / RECIPIENT_TENANT_MISMATCH / TEMPLATE_PARAM_MISSING /
   TEMPLATE_NOT_FOUND / DUPLICATE_IGNORED）、`NotifyChannel`、`NotifyRecipient`；收件人上下文
   `NotifyRecipientContext` + `NotifyRecipientContextResolver` 扩展点 + `AdminUserNotifyRecipientContextResolver`
   （B05 只解析 ADMIN，不查任职关系，任职动态路由归 MSG-001.B）。
2. **事务内幂等（claim-first，循 JOB-003 `JdbcConsumerInboxPort.claimNew` 先例）**：`dispatchOne` 顺序为
   ①幂等预检 `selectByIdempotentKey`（**前移到所有状态分支之前**，覆盖成功/失败，失败命令第二次直接命中
   DUPLICATE_IGNORED 不再产生副作用）→ ②判定状态（渠道/模板/收件人，**均无副作用**）→ ③`claimIdempotentKey`
   原子抢位（保存点内 INSERT `system_notify_send_log` 抢 `uk_notify_send_log_idempotent`，**只有抢到键的请求才
   建消息 + append Outbox**）→ ④SUCCESS 才建消息 + 追加 `OutboxEventMessage{eventType=NOTIFY_DISPATCHED}`
   （复用 JOB-002 `ReliableEventPort` MANDATORY 事务参与）+ `fillDispatchSideEffects` 定向 UPDATE 回填
   messageId/outboxEventId；并发撞键经 SAVEPOINT 回滚恢复事务可用性后复判既有记录返回 DUPLICATE_IGNORED
   （PG 撞 23505 后事务即中止，不回滚保存点则复判报 25P02）。
3. **保存点异常归一化（codex r1/r2/r3 收敛）**：`claimIdempotentKey` 以 `DataSourceUtils.getConnection` 取事务
   绑定连接（与 MyBatis 同一连接）设/回滚/释放保存点；catch 顺序 `DuplicateKeyException →（其余）
   DataAccessException → SQLException`——唯一键冲突走幂等复判，非唯一键写入失败（字段超长/非空/CHECK）归一化
   `WRITE_FAILED`；`rollbackSavepointOrFail` / `releaseSavepointOrFail` **回滚或释放任一失败即抛 WRITE_FAILED**
   （不在不可信事务上继续复判掩盖恢复失败；辅助方法在 catch(DuplicateKeyException) 内抛 ServiceException，
   不被同级后续 catch 捕获，正确外抛触发 `@Transactional` 整体回滚）；连接经 `finally` 与 getConnection 配对释放。
4. **明确状态可查询**：非 SUCCESS 状态返回 `NotifyDispatchStatus`，不抛异常、不返回 null；所有状态（含无渠道）
   均写入 `system_notify_send_log`（可追溯、可按事件回查）；无渠道以哨兵 `NONE`（NO_CHANNEL_SENTINEL）落库，
   使「无渠道」成为可查询、可幂等的持久化状态（不建消息、不入 Outbox）。
5. **收件人上下文与租户**：仅「不存在(null)/停用(DISABLE)」归业务态 RECIPIENT_INVALID，技术查询异常向上抛
   触发回滚 + 可重试（不占用幂等键）；跨租户过滤依赖 MyBatis-Plus 租户拦截器（生产跨租户查询返回 null，
   合并入 RECIPIENT_INVALID）；RECIPIENT_TENANT_MISMATCH 为预留状态；`TenantContextHolder` 缺失由 dispatch
   顶层 fail-closed 抛异常（循 JOB-002 outbox_event 先例，不默认 0）。
6. **DAL / 迁移 / 错误码**：`NotifySendLogDO`（BaseDO）+ `NotifySendLogMapper`（`selectByIdempotentKey` 含逻辑
   删除行、与唯一约束同域；`fillDispatchSideEffects` 只回填 message_id/outbox_event_id）；迁移
   `V20260915.003__system_notify_send_log.sql`（唯一键 `uk_notify_send_log_idempotent` + 八态 CHECK）+ H2
   `create_tables.sql` 对齐 + `clean.sql`；`ErrorCodeConstants` 新增 5 码 `1_002_028_001~005`（eventId/recipients/
   write_failed/tenant/actorType）。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0 | FAIL 1×P1 | P1 唯一键冲突发生在副作用（建消息 + append Outbox）之后，catch 无法完成幂等恢复（PG 撞 23505 后事务中止，catch 内 SELECT 报 25P02；此前建的消息/Outbox 未撤销）；失败分支跳过预检，同一失败命令第二次即触发异常链。另提 P2×7、P3×2 → claim-first + SAVEPOINT 重排（循 JOB-003 先例）：预检前移覆盖所有状态、只有抢到键才执行副作用；同弧修复 P2（Mapper 去 deleted 过滤 + fillDispatchSideEffects 定向回填、params null 归一化 emptyMap、Resolver 技术异常向上抛、无渠道 NONE 哨兵持久化、H2 create_tables 八态 CHECK）+ P3（错误码 +004/005、接口注释修正）；测试补强至 17 用例 |
| r1（claim-first 落地） | **PASS 0×P0/P1**；另 2×P2+1×P3 | P1 已闭合（先抢唯一键成功后才建消息/追加 Outbox，成功与失败命令顺序重试均经预检）。P2#1 `con.rollback(savepoint)` 抛 SQLException 只记录日志仍返回 false 继续复判 → `rollbackSavepointOrFail` 回滚失败即抛；P2#2 非唯一键 Mapper 写入异常（已是 DataAccessException）未被 `catch(SQLException)` 捕获、绕过 WRITE_FAILED → 新增 `catch(DataAccessException)` 归一化；P3#3 冲突路径只 rollback 未 release savepoint → 释放保存点 + `finally` 配对 `DataSourceUtils.releaseConnection` |
| r2（r1 P2/P3 处置） | **PASS 0×P0/P1**；新增 1×P2 | r1-P2#1/#2、P3#3 均闭合。新增 P2：`releaseSavepointQuietly` 捕获 SQLException 后仅 log.debug（软失败），随后仍返回 false 继续复判——若 PG `RELEASE SAVEPOINT` 被取消或连接中断，事务可能再次中止，继续复判以 25P02/连接异常掩盖释放失败并绕过 WRITE_FAILED → 建议改 `releaseSavepointOrFail` 失败即抛（与 rollback 对称） |
| r3（r2-P2 处置） | **PASS 0×P0/P1/P2/P3** | `releaseSavepointQuietly` → `releaseSavepointOrFail`（释放失败即抛 WRITE_FAILED，与 rollbackSavepointOrFail 对称；辅助方法在 catch(DuplicateKeyException) 内抛 ServiceException 不被同级 catch 捕获，正确外抛触发整体回滚）。r2-P2 已闭合；异常传播/连接释放/claim-first/预检前移/副作用抑制/定向回填/NONE 哨兵/参数归一化/Resolver 技术异常传播/错误码与脱敏日志全部复核 PASS，零发现正式收敛 |

## 验证

- TDD：RED 先行（NotifyDispatcherTest 骨架先失败）→ GREEN；补强至 **17 用例**（成功/禁用模板/无渠道 NONE
  持久化 + 重试幂等/收件人失效/参数缺失/模板不存在/缺 eventId/缺收件人/成功重试副作用抑制 verify times(1)/
  失败重试命中幂等 never 建消息/收件人查询技术异常向上抛回滚不占键且恢复后可成功/null 参数归一化 verify emptyMap）。
  过程中修复一处 Mockito 陷阱（对当前抛异常的桩再用 `when().thenReturn()` 重打桩会真实触发异常 → 改 `doThrow`/
  `doReturn(...).when(mock)`）。
- H2：`mvn -pl :zszj-module-system test -Dtest=NotifyDispatcherTest` **17/17 PASS**（BUILD SUCCESS，全模块 441
  源 + 74 测试源编译通过）；r2/r3 硬化后各复跑一次均 17/17。
- 门禁：`run-local-gates --fast` **10/10 PASS**（含 G7 数据源 PG 合同、G8 Flyway 迁移规范静态校验），r2/r3 后各复跑一次均 10/10。
- PG：迁移 `V20260915.003` 自 GREEN（t5）未变，`run-pg-regression.mjs` 聚合 **11/11**（结果沿用；CFG-002 聚合中
  flaky 单独复跑 PASS）。
- 合并：`--no-ff` 合并 main `c7d1ad57`（无冲突；merge-base `d52eda6f`，17 文件纯后端，与主树前端在途改动零重叠）。

## 登记边界（codex r1~r3 认可 + 需求约束，非阻塞，如实登记）

1. B05 只实现 **INBOX** 渠道；SMS/EMAIL/PUSH 指定时返回 NO_CHANNEL（真实渠道调用受 D-10 门禁后另立，归 MSG-004）。
2. B05 只解析 **ADMIN** 收件人，不查任职关系（组织/岗位/角色动态路由归 MSG-001.B，仅留 `NotifyRecipientContextResolver`
   扩展点）；D-09 后追加任职路由专项测试。
3. `AdminUserRespDTO` 无 tenantId 字段：跨租户过滤依赖 MyBatis-Plus 租户拦截器（生产跨租户查询返回 null，合并入
   RECIPIENT_INVALID）；`RECIPIENT_TENANT_MISMATCH` 为预留状态，待 AdminUserRespDTO 暴露 tenantId 或 MSG-001.B
   任职路由后启用显式比对。
4. 真并发竞态（两事务同时在途）的 SAVEPOINT 恢复路径无法在 H2 单连接单测中确定性复现，循 JOB-003 同款先例
   （该模式已在 `JdbcConsumerInboxPortTest` 覆盖）；本轮以「顺序重试副作用抑制 + verify 调用次数」确定性证明
   P1 修复核心性质（副作用只在抢到键时发生一次）。保存点回滚/释放失败、DataAccessException 归一化属故障注入
   路径，H2 单测不易确定性触发，以静态链路正确性 + 错误码合同核对为准（codex r3 已逐条静态复核 PASS）。
5. `NotifySendService` 当前仍直接建消息、尚未接入本统一入口——存量调用点迁移属后续任务（MSG-002 待办生命周期
   接入时统一收敛），接口注释已如实说明。
