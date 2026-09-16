# ZS-JOB-004 codex 评审处置（r0→r2 三弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--dangerously-bypass-approvals-and-sandbox` 读评工作区，多轮以
  **Java-on-PG 隔离探针 + 工作树 `git diff HEAD` 全量核读实锤并发交错与脱敏边界**）
- 评审对象：分支 `feat/job-004-clean`（ZS-JOB-004〔B05 Wave3，P1，类别补建〕建设重试、DEAD 与人工恢复台账——
  JOB-002 Outbox / JOB-003 Inbox 之上的运维恢复控制台，隔离于并行 agent 的 `feat/job-004`）
- 结论：**r2 PASS / 0×P0/P1**（三弧收敛：r0 FAIL 2×P1+P2+P3 → r1 PASS 0×P0/P1〔P1/P3 RESOLVED，P2 PARTIAL 登记 2×P2〕→
  r2 PASS 0×P0/P1〔P2-1/P2-2 PARTIAL 登记，EXIT=0〕）
- 说明：各弧 raw 输出以 `outputs/job004-codex-r0/r1/r2.log` 入库（tail 摘要 + CONCERN 表实锤），发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `1161cc90`，26 files +2363/-2；合并 `--no-ff` main `3ea24436`）

1. **DEAD 事件分页/详情回查**：`OutboxEventController`（`admin/job`，`OutboxEventPageReqVO` / `OutboxHealthRespVO` /
   `OutboxRecoveryReqVO` 三 VO，`@Valid` + `@PreAuthorize` 授权）—— DEAD 列表分页（`OutboxEventPageReqVO` 入 `PageParam` 分页契约）
   + 单事件详情回查，`toDetail` 保留 payload/headers/event_type 原文不可编辑（循「不允许直接编辑 payload 掩盖历史」）。
2. **授权人工恢复（重试 / 跳过）**：`OutboxRecoveryService` / `OutboxRecoveryServiceImpl`（470 行，`cn.zszj.module.infra.framework.outbox.recovery`）
   + `OutboxRecoveryCmd` / `OutboxRecoveryAction`（RETRY / SKIP）/ `OutboxRecoveryResult` / `OutboxEventRecoveryDetail` /
   `OutboxRecoveryLogRecord`。`recover()` 以 **FOR UPDATE 行锁**串行「状态守卫 → `countManualRetry` → 上限校验 → UPDATE → 台账 INSERT →
   `AuditPort.recordSuccess`」持锁至提交（P1 修复范式）；SKIP 落 SKIPPED 终态、claimable=0 不可再领取；缺失 operator 拒绝。
3. **无限重试护栏**：`manual_retry_seq`（identity/sequence 自动生成）+ 人工重试次数上限校验，超限受控拒绝
   （`OUTBOX_RECOVERY_*` 业务码，`ErrorCodeConstants` +9），杜绝并发窗口内无界重放。
4. **`last_error` 脱敏（逐段验证白名单）**：`JAVA_IDENTIFIER_PATTERN`（`^[A-Za-z_][A-Za-z0-9_]*$`）+ `THROWABLE_NAME_SUFFIXES`
   （Exception/Error/Throwable）+ `KNOWN_ERROR_CONSTANTS`（NO_SINK_SUPPORTS_EVENT_TYPE）；助手 `isControlledExceptionName`
   （instanceof String → `isQualifiedJavaName` 逐段 split `"\\.",-1` 每段匹配标识符 → simpleName 以 Throwable 后缀结尾）；
   畸形/非法限定名降级 `UNPARSEABLE_ERROR` 不回显原文（P2-1 收严）。
5. **双轨审计（前后关联）**：`outbox_recovery_log` 台账（恢复前后状态/retry_count 关联）+ `audit_event`（`AuditEventTypes` +6，
   `AuditPort` 幂等 recordSuccess/recordFailure，REQUIRES_NEW），保留恢复前后可追踪链。
6. **健康监测（阈值实测回填）**：`OutboxHealthMonitor` / `OutboxHealthMonitorImpl` / `OutboxHealthMetrics` / `OutboxHealthProperties`
   ——积压 / 最长等待 / 失败率（DEAD/(DISPATCHED+DEAD+SKIPPED)×100，分母 0 取 0）/ 租约四维；阈值经 `application.yaml`（+15）
   按环境配置，`run-job004-verify.mjs` 真实 PG17 重放实测回填而非凭空声称容量达标（WP-19）。
7. **迁移与 schema 一致**：`V20260915.021__infra_outbox_recovery.sql`（PG，33 行）与 H2 `create_tables.sql`（+20/-）字段/NULL/CHECK/
   默认值一致（P3 RESOLVED）；`clean.sql` +1；`api-inventory-baseline.txt` +JOB-004 端点（合并后 ApiInventoryTest 4/4，端点 347）；
   `run-pg-regression.mjs` 接入 JOB-004 为第 12 套件。

## 评审弧（Java-on-PG 探针 + 工作树全量核读逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（初始 GREEN 工作树） | **FAIL** 0×P0，2×P1+P2+P3 | CONCERN 表实锤：**P1** 双轨审计前后关联走无锁快照，PG READ COMMITTED 下并发恢复可反超 → 行锁串行化；**P1** 原子性——恢复方法内 UPDATE/台账/审计非全持锁，SUCCESS 审计异常可致部分提交/状态台账不等价 → fail-closed 持锁至提交；**P2** `last_error` 脱敏 `errorCategory` 以 `{` 开头/畸形字符串未校验直接回显（`timeout password=secret` 类敏感穿透）；**P3** PG 迁移 vs H2 双 schema 默认值漂移。DEAD 分页/详情、历史不可修改、授权缺 DENIED、SKIPPED 不可领取、租户 fail-closed、范围隔离（7 修复+19 新增与清单一致）均 PASS |
| r1（P1/P3 修复后） | **PASS / 0×P0/P1** | **P1 RESOLVED**（`OutboxRecoveryServiceImpl` FOR UPDATE 行锁内串行状态守卫→计数→上限→UPDATE→台账 INSERT→SUCCESS，持锁至提交，覆盖 169/175 并发窗口）；**P3 RESOLVED**（`create_tables.sql` 与 PG 迁移 13 字段业务类型/长度/可空性/CHECK/默认值一致，`before_retry_count` 无默认、`manual_retry_seq` 默认 1，identity/sequence 自动生成，无业务字段漂移）；**P2 PARTIAL** → 登记 **2×P2**（P2-1 白名单可被畸形限定名/非异常字符串绕过；P2-2 并发测试 `await(250ms)` 返回值被忽略）。核读 `git status`/`git diff HEAD` 全量 19 未跟踪文件与 HEAD 基线一致，46/46 GREEN |
| r2（P2-1/P2-2 收严后） | **PASS / 0×P0/P1（EXIT=0）** | **P2-1 PARTIAL**：逐段验证白名单（`isQualifiedJavaName` 拦连续点/数字开头段 + Throwable 后缀 + KNOWN_ERROR_CONSTANTS）已拦 `a..bException`/`a.1Exception` 畸形限定名；`password_real_secret_123Exception`（合法 Java 限定名 + Throwable 后缀）仍通过 = **极窄来源信任残留**（派发器 `describeThrowable` 恒写真实异常 SimpleName，非受控字符串来源触发面窄）→ 登记。**P2-2 PARTIAL**：`bReadStaleCount.await(250ms)` 返回值忽略，无法保证实现必然 RED（测试完美主义，P1 实现已 RESOLVED）→ 登记。**P3 无回归**。达到 0×P0/P1 收口阈值，剩余 P2 均已登记，可安全合并 |

## 验证

- TDD：RED（重试/DEAD 监测 + 授权恢复台账失败测试，16 用例全 RED 确认）→ GREEN（监测/查看/回查/重试/跳过+理由 + 脱敏 +
  迁移 SQL + 错误码 + controller + yaml 阈值）；用户裁定 A（修明确缺陷后合并）后 P2-1 逐段验证 tests 26-27 RED→GREEN。
- H2：`OutboxHealthMonitorTest` + `OutboxRecoveryServiceTest`（757 行）—— infra Outbox **53/53**（Dispatcher 13 + EventPort 12 +
  HealthMonitor 5 + Recovery 23）；infra 全量 BUILD SUCCESS。
- PG：`scripts/db/run-job004-verify.mjs`（169 行，7 用例）真实 PG17 定向验证 **7/7×3 稳定**，注册为 `run-pg-regression` 第 12 套件；
  合并前清洁重跑 PG 全量回归 **12/12**（含 JOB-004）+ JOB-002/003 PG 绿。
- 门禁：fast 10/10；合并后集成验证（main `3ea24436`）**ApiInventoryTest 4/4**（端点 347 = infra 130 + system 217，与 MSG-004 共存）
  + ModuleWhitelistTest 4/4 + PaginationContractTest 3/3（`OutboxEventPageReqVO` 入 41 `PageParam`）+ ValidationContractTest 3/3。
- 合并：`git merge-tree --write-tree` 非破坏性预演 0 冲突 → `--no-ff` main `3ea24436`（'ort' 策略，26 files +2363/-2，零冲突，
  与 MSG-004 无文件重叠：`ErrorCodeConstants`/`clean.sql`/`create_tables.sql` 分属 infra vs system，迁移 V20260915.021 vs V20260916.002）。

## 登记边界（codex r2 非阻塞项 + 用户裁定 A，如实登记）

1. **P2-1 `password_real_secret_123Exception` 极窄来源信任残留**：逐段验证白名单已拦畸形限定名（`a..bException`/`a.1Exception`），
   但合法 Java 限定名 + Throwable 后缀（如 `password_real_secret_123Exception`）仍通过白名单——触发前提是派发器
   `describeThrowable` 之外存在写入非受控异常名的来源；当前派发器恒写真实异常 `getSimpleName()`，触发面极窄。用户裁定 A 接受
   （廉价拦住明确缺陷、保留 Throwable 后缀与诊断价值），残留登记归后续小卡（异常名来源白名单收敛 / 敏感词根集二次过滤）。
2. **P2-2 并发测试完美主义**：`OutboxRecoveryServiceTest` 并发用例 `bReadStaleCount.await(250ms)` 返回值被忽略，无法保证「实现被
   移除时必然 RED」的确定性锁阻塞观测（需 H2 确定性锁阻塞点）；P1 行锁实现本身已 RESOLVED（codex r1/r2 双确认），本项为测试强度
   改进，登记归后续小卡。
3. **验收映射**（四验收全绿，真实 PG17 重放实测）：① 连续失败进 DEAD 并被发现（HealthMonitor `DEAD_EVENTS_PRESENT` breach + WARN）；
   ② 授权恢复后状态/审计可追踪（双轨台账 `outbox_recovery_log` + `audit_event` 前后关联）；③ 无权重放 / 无限重试 / 改历史被拒
   （授权缺 DENIED + `manual_retry_seq` 上限护栏 + payload 不可编辑）；④ 告警阈值经 `run-job004-verify` 真实 PG17 重放实测回填
   （非凭空声称容量达标，WP-19）。
4. **跨租户运维显式接口**：当前查询/恢复全量按当前租户过滤（租户 fail-closed），跨租户运维接口沿用 JOB-002/003 登记待办。
5. **B05 主卡链路门禁不变**：本件为 JOB-002/003 之上的运维恢复控制台（机制层），正式首链启用另受 D-07 门禁；
   本记录不表示任何主任务已验收。
