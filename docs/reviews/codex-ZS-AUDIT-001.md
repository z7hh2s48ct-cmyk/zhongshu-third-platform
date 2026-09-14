# codex 评审处置：ZS-AUDIT-001 适配统一业务审计事件与事务边界

> 被审对象：worktree 隔离分支 `feat/audit-001` 的渐进未提交/未跟踪改动，两弧均以 `codex review --uncommitted` 就当前工作树状态评审（r1 被审状态即最终提交内容）。
> impl 提交 `0576af74`（10 files，+676：新增 `AuditPort`/`AuditEventMessage`/`AuditEventTypes`（zszj-common framework 层）+ `JdbcAuditPort`/`package-info`（system 模块）+ `JdbcAuditPortTest` + 迁移 `V20260914.010__system_audit_event.sql` + H2 `create_tables.sql`/`clean.sql` + `ErrorCodeConstants` 补 3 码），`--no-ff` 合并 main `d171307e`；合并后门禁 G3 捕获 Javadoc 品牌残留，品牌修复 fixup `fc894236`（1 file，+1/-1）重合并 main `03375de2`。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：两轮保留于 `outputs/audit001-codex-r{0,1}.log`（UTF-16，属 gitignored 工作痕迹未入库；各轮结论已逐字引用于本文）。
> 说明：`read-only` sandbox 未重跑 Maven，codex 以定向内存探针复现每条判定（r0 stdout 附 `BUSINESS_ROWS=1 AUDIT_ROWS=0`、空白键二次写 `AUDIT_EVENT_WRITE_FAILED` 复现证据），并读取 surefire 报告确认测试通过性；单测通过性以本机 `JdbcAuditPortTest` 9/9 为准。本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。

## 交付内容

统一业务审计端口——**同步落库、不可改写的业务审计历史**（区别于异步可丢失的通用操作日志 `OperateLogCommonApi`），B04 阶段仅同库事务内审计、不依赖 B05 Outbox：

- **[AuditPort.java](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/biz/system/audit/AuditPort.java)**（36 行，接口）：置 zszj-common framework 层（循 `OperateLogCommonApi` 先例），实现落 system 模块（不落 infra/file·infra/job）；`record(AuditEventMessage)` 合同——`eventType`/`actorType`/`result` 必填 fail-closed，`SUCCESS` 事件失败须在抛出前将所参与业务事务标记 rollback-only。
- **[AuditEventMessage.java](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/biz/system/audit/AuditEventMessage.java)**（81 行，`@Value`/`@Builder` 不可变值对象）：字段覆盖 actor / 技术租户 / 对象 / 版本 / 原因 / 结果 / trace；`ActorType`（USER/ADMIN/SYSTEM/WORKER）、`AuditResult`（SUCCESS/FAILURE/DENIED）枚举；`tenantId` 可空扩展点、**不默认 0**（D-09 技术租户语义未定前不伪造归属）。
- **[AuditEventTypes.java](../../services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/biz/system/audit/AuditEventTypes.java)**（32 行，事件目录骨架）：交付「机制 + 目录」，5 个跨模块种子事件类型（`OBJECT_CREATED`/`OBJECT_UPDATED`/`OBJECT_DELETED`/`ACCESS_DENIED`/`BACKGROUND_EXECUTION`）；用常量而非枚举，各业务模块接线时按 `<域>_<对象>_<动作>` 增补，目录随接线增长而非一次臆造。
- **[JdbcAuditPort.java](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/framework/audit/core/JdbcAuditPort.java)**（208 行，核心实现）：忠实 `JdbcTemplate` 同步落库（可移植 H2/PG、显式技术租户控制，规避 MyBatis-Plus 租户插件自动注入与 BaseDO 逻辑删除语义——审计只追加不改写）。**双事务模板**：`SUCCESS` 经 `PROPAGATION_REQUIRED` 的 `requiredTemplate` 随调用方事务提交（业务回滚则不留成功审计）+ fail-closed 兜底（校验/序列化/写入任一失败即 `status.setRollbackOnly()` 再抛出）；`DENIED`/`FAILURE` 经 `PROPAGATION_REQUIRES_NEW` 的 `requiresNewTemplate` 独立事务记录（业务回滚不丢失拒绝/失败留痕，其失败不牵连业务事务）。**幂等**：空白键归一化为 SQL NULL + pre-check（`SELECT id WHERE idempotency_key=?`）+ `uk_audit_event_idempotency` 唯一约束 DB 硬兜底。**主键**由 DB 生成（`GeneratedKeyHolder`），`detail` 经 `JsonUtils` 落 `text`（替代供体 PG 专用 `jsonb`/`RETURNING`，双方言可移植）。
- **[V20260914.010__system_audit_event.sql](../../services/zhongshu-core/zszj-server/src/main/resources/db/migration/V20260914.010__system_audit_event.sql)**（28 行）：`audit_event` 表 + `audit_event_seq` + `uk_audit_event_idempotency` 唯一索引（NULL 视为互异）+ 3 查询索引（biz/trace/tenant）；`tenant_id int8 NULL` 不默认 0、无 `deleted/updater/update_time`；H2 `create_tables.sql`（+21）/`clean.sql`（+1）同步。
- **`ErrorCodeConstants`**（+9）：`AUDIT_EVENT_FIELD_MISSING`/`AUDIT_EVENT_DETAIL_SERIALIZE_FAILED`/`AUDIT_EVENT_WRITE_FAILED`（`1-002-010-xxx` 号段隔离）。
- **[JdbcAuditPortTest.java](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/framework/audit/core/JdbcAuditPortTest.java)**（252 行，9 用例 H2）。

## 评审弧（r0→r1，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 被审状态 | 结论 | 要点（JdbcAuditPort.java 行号为当轮被审版本） |
|---|---|---|---|
| **r0** | 初版实现（7 测试） | **0×P0/P1 + 2×P2** | P2-1 fail-closed 事务缺口——`record` 无参与事务边界/rollback-only，内存复现吞异常后 `BUSINESS_ROWS=1 AUDIT_ROWS=0`（`:85-89`）；P2-2 空白幂等键仍写唯一索引，首条成功后续全 `AUDIT_EVENT_WRITE_FAILED`（`:147`） |
| **r1** | 两项弧内修复后（9 测试，= `0576af74`） | **CLEAN 0×P0/P1** | 「No actionable regressions」；Flyway validation passed；all nine audit tests passing |

**收口依据**：r1 达 plan §1.1 第 7 步「0×P0/P1 才收口」阀值；r0 两项 P2 均**弧内修复**（补 test8 rollback-only 兜底 + test9 空白键归一化看守），无延后项、无仍有效阻塞项。

## Codex 原始结论（逐轮逐字引用）

### r0（0×P0/P1 + 2×P2）

> Targeted in-memory probes confirmed a fail-closed transaction gap and repeatable failures for blank idempotency keys. These cases are not covered by the seven existing audit tests.

- **[P2] Mark the caller transaction rollback-only on audit failure** — `JdbcAuditPort.java:85-89`
  > If a transactional caller catches a validation or serialization exception from `record`, the transaction remains committable because this method has no participating transaction boundary or rollback-only handling. An in-memory reproduction committed a business row with zero audit rows after catching a missing-eventType error. This contradicts `AuditPort.record`'s documented fail-closed guarantee; ensure these failures mark the enclosing transaction rollback-only.
- **[P2] Normalize blank idempotency keys before persisting them** — `JdbcAuditPort.java:147-147`
  > When `idempotencyKey` is empty or whitespace-only, `hasText` skips the lookup, but this binding still persists the blank string into the unique index. The first event succeeds; every subsequent event with that blank value fails with `AUDIT_EVENT_WRITE_FAILED`, including retries. Normalize blank keys to SQL NULL consistently before lookup and insertion, or reject them before writing.

### r1（CLEAN，0×P0/P1，被审 = `0576af74`）

> No actionable regressions were found in the changed and untracked files. Flyway validation passed, and existing reports show all nine audit tests passing; Maven tests were not rerun in the read-only environment.

## 复核与处置

### 已修复（弧内闭合，r1 确认归零）

- **r0-P2-1（fail-closed 事务缺口）✅**：`record()` `SUCCESS` 分支改经 `requiredTemplate`（`PROPAGATION_REQUIRED`）加入调用方事务，`try/catch RuntimeException → status.setRollbackOnly()` 再抛出——纵使调用方吞掉校验/序列化/写入异常，业务事务也无法「无成功审计而提交」，兑现 `AuditPort.record` 的 fail-closed 合同（`JdbcAuditPort.java:97-110`）。补 **test8「吞异常仍整体回滚」** 看守（断言业务行与审计行一致性、捕获异常后业务事务标记 rollback-only）。r1 未再复现 = 确认归零。
- **r0-P2-2（空白幂等键写唯一索引）✅**：`doRecord()` 归一化 `idempotencyKey = hasText(key) ? key : null`——空白（null/空串/纯空格）一律视作「无键」落 SQL NULL（唯一索引视 NULL 互异，多条无键事件不互相碰撞），插入时 `idempotencyKey==null → setNull(13, VARCHAR)`（`JdbcAuditPort.java:143-176`）。补 **test9「空白幂等键归一化」** 看守（多条空白键事件均成功、不撞 `uk_audit_event_idempotency`）。r1 未再复现 = 确认归零。

### 合并后门禁捕获（非 codex 发现，一并记入本处置）

- **G3 品牌命名残留 ✅**：合并入 main 后主树门禁 **G3**（`verify-brand-naming.mjs`，以 `git ls-files` 只枚举**跟踪文件**）FAIL `violations=2`——`JdbcAuditPort.java:31` class Javadoc 原引供体全限定名 `cn.iocoder.yudao.module.infra.zhongshu.audit.JdbcAuditPort`（同行命中 `cn.iocoder` + `yudao`）。**逃逸原因**：worktree 内 G3 首跑发生在提交前（审计文件尚未跟踪 → 跳过 → PASS），提交并合并入 main 转为跟踪文件后被扫出。**修复**：按 docs/06 §4 供体隔离约定，Javadoc 改引「适配自供体 infra 审计端口 JDBC 实现（供体快照隔离于 `reference/donors/`，命名映射见 docs/06）」，去除旧包路径字面量（fixup `fc894236`，1 file +1/-1）。worktree G3 `violations=0 scanned=13764` + 审计 9/9 GREEN；重合并 main `03375de2`（merge-base=`0576af74` 干净无冲突），主树 `gates --fast` 10/10 PASS（G3 814ms）。**教训**：门禁只扫跟踪文件时，worktree 内提交前的首跑对新增文件是假阴性，合并入 main 后须复跑门禁兜底。

## 验证

- **审计测试**：`JdbcAuditPortTest` 9 用例 H2 全绿——提交有审计 / 回滚不留 / 拒绝·后台独立记录 / 投递失败可恢复 / 重复不入账 / fail-closed 拒写 / 吞异常仍整体回滚（test8）/ 空白幂等键归一化（test9）；surefire 报告 `Tests run: 9, Failures: 0, Errors: 0`。
- **合并前复验**（worktree）：模块 672/672 + 门禁 10/10 + PG 回归 ALL PASS（含迁移 `V20260914.010`）+ 审计 9/9。
- **合并后主树复验**：`gates --fast` **10/10 PASS**（G3 814ms）+ 全反应堆 `mvn test-compile` `EXIT=0` + `verify-docs`（G5）0 issue。品牌修复为 Javadoc-only，迁移字节不变，pre-merge PG ALL PASS 仍成立。
- codex r1 `read-only` sandbox 未重跑 Maven，以本机 surefire 报告（`tests=9 errors=0 failures=0`）+ Flyway validation `issueCount=0` 为准。

## 结论

**评审收口（r1 CLEAN 0×P0/P1；r0 两项 P2 弧内修复并确认归零；无延后项）。**

- **r0**（初版，7 测试）：0×P0/P1 + 2×P2——fail-closed 事务缺口（吞异常后业务提交却无审计）+ 空白幂等键写唯一索引（后续事件全写失败）。
- **r1**（两项弧内修复后，9 测试，= `0576af74`）：**CLEAN 0 发现**「No actionable regressions were found」。
- codex 发现总数 **2**（2×P2），弧内已修复 **2**（P2-1/P2-2），延后 **0**，仍有效阻塞项 **0**；另合并后 G3 门禁捕获 1 项品牌残留（非 codex 发现）由 `fc894236` 修复归零。
- **计划 §3 codex 5 要点映射**：① 成功审计真随事务提交（回滚不留）→ `SUCCESS` 走 `REQUIRED` + fail-closed `setRollbackOnly`，test「提交有审计/回滚不留/吞异常仍整体回滚」✅；② 拒绝/失败独立记录 → `DENIED`/`FAILURE` 走 `REQUIRES_NEW`，test「拒绝·后台独立记录」✅；③ 不误依赖 B05 Outbox → `AuditPort` Javadoc 明列「B04 仅同库事务内审计、不依赖 B05 Outbox」，无 Outbox 依赖 ✅；④ D-09 字段只留扩展点不默认 tenant=0 → `tenantId` 可空不默认 0（供体 `tenantId==null?0L` 已去除）、迁移 `tenant_id int8 NULL` ✅；⑤ actor/trace 完整 → `ActorType`(USER/ADMIN/SYSTEM/WORKER)/`actorId`/`traceId`，`eventType`/`actorType`/`result` fail-closed 必填 ✅。
- ZS-AUDIT-001 为 **B04 整卡任务**（非分批子项）：本轮交付统一 `AuditPort` 机制 + 事件目录骨架 + 同库事务内业务审计 + fail-closed/幂等兜底，主卡状态 待开发→**待验收**；审计读取/脱敏/保留期归 ZS-AUDIT-002，需补偿的异步交付归 B05（可靠机制验收后启用），各业务模块接线审计随后续批次在 `AuditEventTypes` 增补事件类型。本记录不表示任何主任务已验收。
