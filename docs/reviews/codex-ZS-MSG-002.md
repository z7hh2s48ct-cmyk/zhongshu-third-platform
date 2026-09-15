# ZS-MSG-002 codex 评审处置（r0→r3 四弧）

- 评审工具：codex（gpt-6-astra / xhigh，`exec -s read-only` 读评工作区）
- 评审对象：分支 `feat/msg-002`（ZS-MSG-002 区分站内消息与业务待办生命周期，B05/B09 核心使能器——前置 ZS-MSG-001 待验收、首链规则待 D-07）
- 结论：**r3 PASS / 0×P0/P1/P2/P3 全清**（四弧收敛：r0 FAIL 2×P1+5×P2+2×P3 → r1 PASS 3×P2+1×P3 → r2 PASS 1×P2 → r3 PASS 零发现）
- 说明：r0~r3 四弧 raw 输出保留于开发窗 `outputs/msg002-codex-r{0,1,2,3}.log`（未入库，outputs/ 已 gitignore），各弧裁定行与发现已逐条摘录如下，如实登记。

## 交付内容（impl commit `61f8192f`，18 files +1975/-1；合并 main `cf1e7f83`，merge-base `3bfe8bfb`）

1. **业务待办数据模型 `system_notify_todo`**：稳定任务 ID `todo_key` + 业务来源 `source_type` + 通用处理状态 `status`
   + 业务版本 `biz_version` / 待办版本 `todo_version` + 失效原因 `invalid_reason` + 转派目标 `assignee` + 松耦合
   关联站内信 `message_id`（可空，与已读态解耦）；唯一约束 `uk_notify_todo_key` + 状态 CHECK
   `ck_notify_todo_status`（PENDING / COMPLETED / WITHDRAWN / REASSIGNED / INVALID）。迁移
   `V20260915.004__system_notify_todo.sql`（PG17）+ H2 `create_tables.sql` / `clean.sql` 对齐；
   `ErrorCodeConstants` 新增 7 码 `1_002_030_001~007`（todoKey/sourceType/status/version/transition/writeFailed/fieldRequired）。
2. **待办生命周期服务 `NotifyTodoService` / `NotifyTodoServiceImpl`**：
   - `registerTodo` 注册幂等三层闭合——①幂等预检 `selectByTodoKey` → ②DB 唯一约束 `uk_notify_todo_key` 硬兜底 →
     ③`catch DuplicateKeyException` 撞键后 **F1 同连接 SAVEPOINT 回滚复判**（PG 撞 23505 后事务即中止，不回滚保存点
     则复判报 25P02；循 MSG-001/JOB-003 保存点先例）+ **R4 复判后释放保存点**（与 rollback 对称，释放失败即抛
     WRITE_FAILED，不在不可信事务上继续掩盖恢复失败）。
   - `applyTransition` 完成 / 撤回 / 转派经可靠事件更新——**F2 待办行版本护栏**（`todo_version` 拒乱序，旧版本
     返回 STALE_VERSION 不覆盖较新待办）+ **终态守卫**（COMPLETED/WITHDRAWN/INVALID 拒复活）+ **F6/R3 重放首次
     结果与 reason**（重复事件不产生第二次副作用，回放首次结论含原因）。
   - **已读独立**：待办生命周期不触碰 `readStatus`/`readTime`（阅读消息不完成业务任务）。
   - 复用 JOB-003 `ConsumerInboxPort` 业务事务内 `tryBegin` 抢位（MANDATORY 参与，业务回滚则待办更新 + 抢位一并回滚）。
3. **事件消费适配 `NotifyTodoEventSink`**：`supports(NOTIFY_TODO_TRANSITION)`（主库暂无生产者，休眠待 D-07 首链 /
   BPM 事件接入）；**F3 可见失败**——错误载荷 / 解析失败抛异常触发重投（不静默视为成功丢弃）；**F4 转派目标**
   经事件载荷透传；**R1 / R1' 转派 ID 无损解析**——Sink 派生**专用 `ObjectReader`（`USE_BIG_DECIMAL_FOR_FLOATS`）**
   从原始 JSON 解码 `BigDecimal` 保留十进制精度（不改全局 mapper）+ `BigDecimal.longValueExact` 拒小数 / 越界，
   杜绝默认 `Double` 解码先丢精度后再校验已舍入值的缺口。
4. **D-07 红线（不复制第二套审批引擎）**：`NotifyTodoStatus` / `TodoTransitionType` / `DefaultTodoStatusMapper`
   仅表达**通用机制态**，未知态返 `null` 不静默映射；Flowable 任务权威状态由 BPM 持有，本服务只做业务待办投影。

## 评审弧

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0 | FAIL 2×P1+5×P2+2×P3 | **P1-F1** PG 并发注册撞唯一键后事务中止（25P02），catch 内复判未在保存点回滚后进行 → 保存点回滚恢复事务可用性后复判既有记录；**P1-F2** 数值版本乱序时旧版本覆盖较新待办 → 待办行 `todo_version` 护栏 + STALE_VERSION 拒乱序。P2：F3 Sink 错误载荷视为成功 → 可见失败抛异常触发重投；F4 转派未传目标 → 载荷透传 assignee；F5 幂等指纹分隔符碰撞 → 长度前缀指纹；F6 重复事件未重放首次结果 → 回放首次结论；F7 注册 sourceType 未校验 → 必填校验拆分。P3：F8 错误码复用 → 拆 007 FIELD_REQUIRED；F9 actor 命名误导 → javadoc 订正 |
| r1（F1~F9 处置） | **PASS 0×P0/P1**；另 3×P2+1×P3 | 两 P1 已闭合（保存点回滚复判 + 版本护栏）。P2-R1 转派 ID 经 `JsonUtils.parseMap` 静默截断为 Double → 数值无损转换；P2-R2 长度前缀指纹将 null 与空串合并 → null 哨兵区分；P2-R3 首次结果重放丢失 reason → 回放携带 reason；P3-R4 注册保存点 rollback 后未 release → 复判后释放保存点（与 rollback 对称）。tokens 98,025 |
| r2（R1~R4 处置） | **PASS 0×P0/P1**；新增 1×P2 | R1/R2/R3/R4 均闭合。新增 P2-R1'：`JsonUtils.parseMap` 默认 `Double` 解码在**校验前**即丢失十进制精度，`longValueExact` 只对已舍入值校验（大整数 / 高精度小数先被 Double 截断，校验形同虚设）→ 建议从原始 JSON 以 BigDecimal 解码再校验（codex 跑 Python 实验验证 BigDecimal 边界） |
| r3（R1' 处置） | **PASS 0×P0/P1/P2/P3 全清** | R1' 闭合：Sink 派生**专用 `ObjectReader`（`USE_BIG_DECIMAL_FOR_FLOATS`）**从原始 JSON 解码 `BigDecimal` 校验原始数值（不改全局 mapper），`longValueExact` 拒小数 / 越界；codex web search 验证 Jackson `ObjectReader.with` 语义 + 追 POM 依赖链确认版本；五项核查（保存点复判 / 版本护栏 / 终态守卫 / 可见失败 / 无损解码）全部复核 PASS，零发现正式收敛。tokens 94,408 |

## 验证

- TDD：RED 先行（NotifyTodoServiceTest 骨架先失败）→ GREEN；补强至 **NotifyTodoServiceTest 26 用例**（注册幂等
  预检 / 撞键保存点复判 / 重复注册回放 / 版本护栏 STALE_VERSION / 终态守卫拒复活 / 完成·撤回·转派 / 已读独立 /
  F5 长度前缀指纹 null≠空串 / F6·R3 首次结果与 reason 重放 / F7·F8 sourceType 校验与错误码拆分）+ **NotifyTodoEventSinkTest
  13 用例**（F3 可见失败抛异常 / F4 转派目标透传 / R1·R1' 转派 ID 无损解析含 3 判别性精度用例：大整数不截断 /
  高精度小数拒绝 / 越界拒绝）= **39/39 BUILD SUCCESS**。
- H2：`mvn -pl :zszj-module-system test -Dtest=NotifyTodoServiceTest,NotifyTodoEventSinkTest` **39/39 PASS**；
  R1' 硬化后复跑仍 39/39。
- 门禁：`run-local-gates --fast` **10/10 PASS**（含 G7 数据源 PG 合同、G8 Flyway 迁移规范静态校验），各弧处置后复跑均 10/10。
- PG：迁移 `V20260915.004`（PG17）定向 `msg002-pg-verify` **7/7 PASS**（P5 撞键保存点恢复 / P5b 负对照 /
  P6 `ROLLBACK TO` 后 `RELEASE` 事务可用性 + 唯一约束 / 状态 CHECK / 版本护栏硬约束）；`run-pg-regression.mjs` 聚合套件全绿。
- 合并：`--no-ff` 合并 main `cf1e7f83`（无冲突；merge-base `3bfe8bfb`，18 文件纯后端 services/zhongshu-core，
  与主树在途前端改动 apps/zhongshu-admin-web 零交集，不扰动前端在途工作区）。

## 登记边界（codex r1~r3 认可 + 需求约束，非阻塞，如实登记）

1. **B05 中性接线**：`NotifyTodoEventSink` 主库暂无 `NOTIFY_TODO_TRANSITION` 生产者，休眠待 D-07 首链规则确认 /
   BPM 状态事件接入后激活；本轮以 H2 单测 + 直接调用 Sink 证明消费侧正确性，端到端事件链归 D-07 领域接线。
2. **首链规则待 D-07**：`DefaultTodoStatusMapper` 仅通用机制态映射，业务对象正式状态 → 待办状态映射口径待 D-07；
   未知态返 null 不静默映射（fail-safe），不预设任何主任务已验收的既成事实。
3. **字符串数字 ID 路径**：转派 ID 无损解析针对 JSON 数值路径（BigDecimal），字符串形式数字 ID 走字符串分支，
   以静态链路核查为准、无专测。
4. **F1 / R4 真并发**：两事务同时在途撞唯一键的 SAVEPOINT 恢复 + 释放路径无法在 H2 单连接单测确定性复现，循
   MSG-001/JOB-003 同款先例（该模式已在 `JdbcConsumerInboxPortTest` / PG 定向套件覆盖）；本轮以 PG17 机制级
   定向验证（P5/P5b/P6）+ H2 顺序重试幂等（verify 调用次数）确定性证明 F1/R4 修复核心性质。
5. **Flowable 权威态归 BPM**：待办投影不复制审批引擎状态机，Flowable 任务状态由 ZS-BPM-001 持有权威；本服务
   仅消费业务事件更新待办，不承担流程路由。
