# ZS-JOB-001 codex 评审处置（r0→r6 七弧）

- 评审工具：codex（gpt-6-astra / xhigh，`--sandbox read-only` 读评工作区，多轮以
  **Java 内存探针实锤脱敏边界与迭代扫描正确性**）
- 评审对象：分支 `feat/job-001`（ZS-JOB-001 补齐任务启停一致性与租户级结果——Handler 白名单 /
  任务表-调度对账 / TenantJobAspect 结构化结果 / 迭代扫描脱敏 O(n) / 级联清理与孤儿回收）
- 结论：**r6 APPROVED / 0×P0/P1**（七弧收敛：r0 FAIL 1×P1+3×P2 → r1 FAIL 1×P1+1×P2 → r2 FAIL 1×P1+2×P2 →
  r3 FAIL 2×P1 → r4 FAIL 2×P1 → r5 FAIL 1×P1 REGRESSION → r6 APPROVED）
- 说明：各弧 raw 输出保留于 `outputs/job-001-codex-r{0..6}.md`（本卡完整保留 codex 输出以便复核），
  发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `8f1dd299`，25 files +2806/-28；处置至 `f8f5994f`）

1. **Handler 白名单**：`JobHandlerInvoker` 引入 `JobHandlerWhitelistValidator` + `JobHandlerWhitelistProperties`
   （`zszj.job.handler-whitelist.enabled/list`），未注册 handler 直接拒绝调度并抛
   `JOB_HANDLER_NOT_WHITELISTED`（`ErrorCodeConstants` 1_002_022_001~003）；`ZszjQuartzAutoConfiguration`
   注册白名单 bean。
2. **任务表-调度对账**：`JobSchedulerReconciler` + `JobSchedulerReconcileReport`——启动时同步 `infra_job`
   表状态与 Quartz Scheduler，按 JobKey 归属消除孤儿触发器（`scheduler.getTriggerKeys(groupStartsWith("JOB_"))`
   对比 `jobMapper.selectList()`），报告 orphan/paused/missing 三类差异；`JobServiceImpl` 集成对账入口。
3. **TenantJobAspect 结构化结果**：`TenantJobExecutionResult` DTO（`zszj-common` 层，
   totalTenants/successCount/failureCount/perTenantResults Map<tenantId, TenantItem{success,durationMs,error}>）；
   AOP 环绕收集每技术租户执行结果，替代原 JSON 字符串汇总，避免部分失败被汇总为成功。
4. **租户级结果聚合**：`JobTenantResultService` / `JobTenantResultServiceImpl` + `JobTenantResultDO`（BaseDO）
   + `JobTenantResultMapper`（`selectListByJobLogId` / `selectOrphanDetailsBefore` / `deleteByJobLogIds`）；
   `@Async saveTenantResultsAsync` 异步落库 `infra_job_tenant_result` 明细表；`errorSummary` 上限 4000 字符
   超出截断加省略号；`JobTenantResultFrameworkService` 接口在 starter-job 层暴露给 aspect 调用。
5. **暂停任务手动触发拒**：`JobServiceImpl#triggerJob` 校验 `JobStatusEnum`，PAUSED 状态返回
   `JOB_TRIGGER_PAUSED`；`JobStatusEnum` 新增枚举项对齐 Quartz 状态语义。
6. **迭代扫描脱敏 O(n)**：`JobTenantResultServiceImpl#summarize` 主循环——`SENSITIVE_KEY_PATTERN.find()`
   定位敏感键 → `scanUnquotedValueEnd` / `scanQuotedValueEnd` 扫描 value 边界 → 替换为 `***`；
   `isSpaceLike` = `Character.isWhitespace` ∪ `Character.isSpaceChar` ∪ U+200B ∪ U+FEFF；
   `NEXT_KEY_PATTERN` 前导 `["']?` 识别引号包裹键；分隔符 lookahead（`,` `;` `&`）+ quote-after-whitespace
   消费（`lastNonWs == start` 守卫）；`scanQuotedValueEnd` 处理转义字符与嵌套引号；消除嵌套量词正则避免
   catastrophic backtracking。
7. **级联清理与孤儿回收**：`JobLogServiceImpl#cleanJobLog` 按父日志 `create_time` 过期删除明细
   （`deleteByJobLogIds`），孤儿明细（父日志已消失）兜底回收（`selectOrphanDetailsBefore` + `deleteByJobLogIds`）。
8. **迁移 V20260915.020**：`infra_job_tenant_result` 明细表（job_log_id / tenant_id / success / duration_ms /
   error_summary / create_time 索引）+ H2 `create_tables.sql` 对齐 + `clean.sql`。

## 评审弧（Java 内存探针逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`8f1dd299`） | FAIL 1×P1+3×P2 | P1 `errorSummary` 列宽无界聚合可击穿 4000 字符上限 → summarize 有界截断加省略号；P2 孤儿触发器全表扫描 → 按 JobKey 归属消除；P2 脱敏遗漏 JSON 引号键与 Bearer 完整凭据 → SENSITIVE_KEY_PATTERN 支持引号键 + CREDENTIAL_PATTERN；P2 cleanJobLog 未级联清理租户明细 → deleteByJobLogIds 级联。处置 `96a62257`（含 testScenario14 mock 状态转移） |
| r1（`96a62257`） | FAIL 1×P1+1×P2 | P1 脱敏 pattern 未匹配完整引号 value（含标点/空格/转义）→ scanQuotedValueEnd 处理转义与嵌套；P2 级联清理按父日志 id 而非 create_time → 改按 create_time 过期。补 scenario16/17 codex r1 回归。处置 `e0be3e87` |
| r2（`e0be3e87`） | FAIL 1×P1+2×P2 | P1 正则嵌套量词 catastrophic backtracking + StackOverflowError（1KB 混合引号输入即崩） → **引入迭代扫描消除嵌套正则**（Matcher.find + region + lookingAt 迭代）；P2 孤儿明细未兜底回收 → selectOrphanDetailsBefore + deleteByJobLogIds。补 scenario18/19/20 codex r2 回归。处置 `4364cddb` |
| r3（`4364cddb`） | FAIL 2×P1 | P1 Unicode 空白 EM SPACE (U+2003) / IDEOGRAPHIC SPACE (U+3000) 暴露未引号凭据（`password=\u2003alpha` → 未脱敏） → 合并 space/tab 与 Character.isWhitespace 分支为统一逻辑；P1 相邻敏感键被消费（`password=alpha,token="bravo"` → `password=***"bravo"`） → 分隔符 lookahead（`,` `;` `&`）。补 scenario21/22 codex r3 Unicode 空白与分隔符回归。处置 `e391913e` |
| r4（`e391913e`） | FAIL 2×P1 | P1 Unicode 空白后引号值完全未脱敏（`password=\u2003"alpha"` → 完全未脱敏，SENSITIVE_KEY_PATTERN 的 `\s*` 不匹配 Unicode 空白，openQuote 为空进入无引号路径，扫描器跳过空白后在引号处 break，valueEnd==valueStart） → 空白跳过后检测引号并调用 scanQuotedValueEnd 消费引号值（early return）；P1 NBSP/ZWSP 在分隔符后仍导致相邻键被消费（`password=alpha,\u00A0token="bravo"` → `password=***"bravo"`，Character.isWhitespace 不含 NBSP/FIGURE SPACE/ZWSP） → 新增 `isSpaceLike()` 辅助方法覆盖 `Character.isWhitespace` ∪ `Character.isSpaceChar` ∪ U+200B ∪ U+FEFF，所有空白跳过逻辑（主分支 + 分隔符 lookahead）统一使用 isSpaceLike。补 scenario23/24 codex r4 EM SPACE/NBSP/ZWSP/FIGURE SPACE 组合回归。处置 `c66b8575` |
| r5（`c66b8575`） | FAIL 1×P1 REGRESSION | P1 相邻引号包裹的敏感键被消费（`password=alpha "token"="bravo"` → HEAD `password=***="bravo"`，token 值暴露；父提交 r3 正确输出 `password=*** "token"="***"`）。**根因**：①r4 的 quote-after-whitespace 分支在**已消费值内容后**仍触发（主循环扫到 `alpha` 时 lastNonWs 已推进到 `start+5`，遇到空格跳过后再遇到 `"` 时误当作"值以引号开头"，把 `"token"` 整体消费掉）；②`NEXT_KEY_PATTERN` 不含前导 `["']?`，无法识别 `"token"=` 这种引号包裹的键（isSensitiveKeyAt 在 j 位置看到 `"` 直接返回 false，分隔符 lookahead 分支也无法在 `,"secret":` 后识别到敏感键）。**处置**：①quote-after-whitespace 分支加 `lastNonWs == start` 守卫（仅在尚未消费任何值内容时才允许把引号作为值起点）；②`NEXT_KEY_PATTERN` 添加前导 `["']?`（让 isSensitiveKeyAt 能识别 `"token"=` / `'api_key'=` 等引号包裹形式）。补 scenario25 codex r5 相邻引号包裹键回归（空格/逗号/分号三种分隔符 + 双引号/单引号两种键包裹）。处置 `f8f5994f` |
| r6（`f8f5994f`） | **APPROVED / 0×P0/P1** | 「Approve the r5 disposition. Track escaped-serialization handling separately.」r5 两个 finding 均 Resolved（P1-quoted-key-guard：`lastNonWs == start` 仅在消费值内容前为 true，空白可推进 `i` 但保持 `lastNonWs` 不变属预期语义；P1-NEXT_KEY_PATTERN：前导可选引号与 case-insensitive + `region()`/`lookingAt()` 正确协同，空白与分隔符 lookahead 均能识别相邻引号包裹敏感键）；r0-r4 无回归；83 + 288 断言通过；1M 转义字符 + 3M 未引号字符长输入无栈溢出；字节码验证两个 r5 修改；Territory isolation: PASS（`c66b8575..f8f5994f` 仅动 service +5/-2、test +42）；Tokens used: 72,243 |

## 验证

- TDD：RED（`JobConsistencyTest` 骨架 15 用例全败）→ GREEN。
- H2：`JobConsistencyTest` **27 用例**（infra 全量 **392/0/10** BUILD SUCCESS）——15 原始场景
  + scenario16/17（r1 回归：完整引号 value + 级联清理 create_time）
  + scenario18/19/20（r2 回归：迭代扫描 + 孤儿回收 + 混合引号）
  + scenario21/22（r3 回归：EM SPACE/IDEOGRAPHIC SPACE Unicode 空白 + 分隔符 `,` `;` `&` 相邻敏感键）
  + scenario23/24（r4 回归：EM SPACE + 双引号值 / IDEOGRAPHIC SPACE + 单引号值 / NBSP/ZWSP/FIGURE SPACE 在分隔符后）
  + scenario25（r5 回归：相邻引号包裹键 `password=alpha "token"="bravo"` / `password=alpha,"secret":"charlie"` / `pwd=x;'api_key'='delta'`）。
- 交叉模块：`mvn -pl :zszj-spring-boot-starter-biz-tenant,:zszj-spring-boot-starter-job test` **61/0/0**。
- 门禁：`run-local-gates --fast` **10/10 PASS**（每弧处置后各复跑一次均 10/10）；迁移规范 0 issue。
- 合并：`--no-ff` main `72dbf7cc`（merge-base `304cc2f2`，与主树 MSG-001 后续前端在途改动零重叠）。

## 登记边界（codex r6 记录的 pre-existing limitations，非本次回归，如实登记）

1. **`SENSITIVE_KEYS` 无 word-boundary 锚点**：`"foo_token"=` 主匹配器 `find()` 仍会匹配 token 后缀并脱敏其值
   （lookahead 在起始位置正确拒绝，但 `find()` 从任意位置扫描）。归后续小卡（如需精确边界，添加 `\b` 或
   显式键前后字符类）。
2. **不匹配的引号被接受**：`password=alpha "token'="bravo"` 两个都脱敏（宽松策略，不引入新泄漏）。
3. **未引号值中的转义引号不识别为引号语法**：`password=\"token\"=bravo` → `password=***"token\"=bravo`
   （转义序列化不完全保护）。Codex 建议：Track escaped-serialization handling separately。
4. **`JobTenantResultService` 存量调用点迁移**归 JOB-004 恢复台账接入时统一收敛。
5. **真并发竞态**（多实例同时调度同一 Job）依赖 Quartz PG 集群模式（ZS-DB-016 已交付），本轮以 H2 单实例
   顺序执行 + 迭代扫描 O(n) 确定性证明 P1 修复核心性质，多实例真并发联验归批次真实环境。
6. **Regression test scenario25 精度**（codex r6 记录的 optional improvement，非阻塞）：三个子用例中仅第一个
   显式检查两个 mask marker，其他两个只检查 leak 防止；可选强化为对三个都断言精确输出，并追加一个"守卫触发
   但敏感键 lookahead 失败"的用例。归后续小卡。
