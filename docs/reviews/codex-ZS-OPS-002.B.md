# ZS-OPS-002.B codex 评审处置（r0→r1 两弧）

- 评审工具：codex（gpt-6-astra / xhigh，`codex review --commit <SHA>`，运行时探针实测：真实拉起 zszj-server 验证匿名健康端点可见性、Java 单测探针验证日志脱敏、复现 PG init-server 就绪竞态）
- 评审对象：分支 `feat/ops-002-b`（ZS-OPS-002.B〔B05 Wave4，P0 血统 OPS-002 主卡，类别补建〕已启用依赖/队列告警与运行说明——JOB-004 健康监测之上的自动发现面）
- 结论：**r1 PASS / 0×P0/P1/P2/P3（0 发现）**（两弧收敛：r0 FAIL 1×P1+2×P2 → 全部处置 → r1 PASS「No actionable regressions were found. All 14 focused Java tests, 7 PostgreSQL monitoring checks, and both SQL-only regression suites exercising the updated database bootstrap passed.」）
- 说明：各弧 raw 输出以 `outputs/ops002b-codex-r0.log` / `outputs/ops002b-codex-r1.log` 留存（本地，outputs/ 不入库），发现与处置逐条摘录如下，如实登记。

## 交付内容（impl commit `a34c1834`，11 files +906；r0 处置 `ab8b9971`，7 files +61/-27）

1. **周期告警探针 `OutboxHealthAlertScheduler`**（`@Scheduled` + `@ConditionalOnProperty(infra.outbox.health.alert.enabled)` 门控）：周期消费 JOB-004 `OutboxHealthMonitor.snapshot()`，越阈按严重度分级告警（`PENDING_BACKLOG_CRITICAL`/`DEAD_EVENTS_PRESENT`/`STALE_LEASE_PRESENT` → ERROR；WARN 类 → WARN；未知码保守降级 WARN）；探针自身异常 ERROR 明示「探针失败」且就地隔离不击穿调度线程（失败不静默三层之①③）。纯消费者：不新造阈值、不重算越阈（JOB-004 `evaluateBreaches` 单一真源）。
2. **队列健康指示器 `OutboxQueueHealthIndicator`**（贡献者名 `outboxQueue`）：`breaches` 空 → UP；非空 → DOWN 携越阈码+五项指标；`snapshot()` 抛出 → DOWN + `reason=MONITOR_PROBE_FAILED` + 异常类名（绝不伪装 UP）。details 仅指标值/码/异常类名，无 payload 与异常原文（脱敏）。
3. **monitoring 健康组**（application.yaml）：`management.endpoint.health.group.monitoring.include: outboxQueue,db,redis,ping`——队列越阈 + DB/Redis 依赖状态汇入专用监测面；**liveness/readiness 保持 Spring Boot 默认，`outboxQueue` 绝不进 liveness**（积压≠进程死亡，反重启风暴铁律）；**组内 `show-details: never`**（r0 P1 处置，见下）。
4. **告警配置**：`infra.outbox.health.alert.{enabled,interval-ms}`（默认 true/60000ms，保守占位待 WP-19 实测回填）；`OutboxHealthProperties` 增嵌套 `Alert`（relaxed binding，与 `@ConditionalOnProperty`/`@Scheduled` 同源）。
5. **运行说明** `services/zhongshu-core/docs/运行监控与告警响应.md`：已启用告警清单（6 越阈码→严重度）、越阈码→响应动作（链接 JOB-004 恢复控制台 `/infra/outbox-event` page/detail/retry/skip）、健康组边界（liveness 纯净红线 + 匿名详情不外泄）、阈值实测回填状态（占位声明，禁容量承诺）、依赖故障处置、边界声明（外部告警系统/容量/RPO-RTO 归 .C/B11）。
6. **定向 PG17 验证** `scripts/db/run-ops002b-verify.mjs`：一次性容器重放 outbox 迁移链，镜像监测聚合 SQL（`SUM(CASE WHEN)` 四状态计数/`MIN(next_retry_at)` 最长等待/参数化过期租约）+ 分级/健康映射 **7 断言**（B1~B7）；**注册为 `run-pg-regression.mjs` 第 13 套件**（r0 处置顺带修复回归跑批器 caseFile 引导的就绪探测）。

## 评审弧（运行时探针逐轮实锤）

| 轮次 | 结论 | 发现 → 处置 |
|---|---|---|
| r0（`--commit a34c1834`） | **FAIL** 1×P1+2×P2 | **P1** monitoring 组 `show-details: always` + `/actuator/health/**` 匿名放行（ZS-ENG-005 既有合同）→ 内建 `db`/`redis` 指示器详情（依赖异常原文/内部地址）对匿名暴露（codex 真实拉起 server 未认证请求实锤 Redis 连接错误与内部地址回显）→ **修复**：组内 `show-details: never`，匿名监控面只暴露状态（越阈/依赖故障 → 组状态 DOWN 被发现）；越阈码/指标详情走探针结构化日志 + 授权端点 `GET /infra/outbox-event/health`；经 HTTP 携详情的授权监控面归后置外部接入（B11/D-10）。**P2-1** 探针失败日志把异常对象传给 SLF4J → 完整消息/堆栈/cause 链落日志（测试口令逐字回现）→ **修复**：仅记异常类名不传 throwable；测试 S4 升级断言日志事件 `getThrowableProxy()==null` 且渲染结果不含原文敏感信息。**P2-2** PG 就绪探测走 Unix socket 可命中 init 临时 server → `CREATE DATABASE` 前失败（codex 复现实锤）→ **修复**：`run-ops002b-verify.mjs` 与 `run-pg-regression.mjs` caseFile 引导改 **TCP+PGPASSWORD 探最终 server**（60×500ms）。 |
| r1（`--commit ab8b9971`） | **PASS / 0 发现** | 「No actionable regressions were found. All 14 focused Java tests, 7 PostgreSQL monitoring checks, and both SQL-only regression suites exercising the updated database bootstrap passed.」codex 自跑 zszj-module-infra/system/server 构建 BUILD SUCCESS + DB-016/017 两套件经修复后的就绪引导全绿。 |

## 验证

- TDD：探针/指示器/绑定/配置契约 4 测试类 **21 用例**（Scheduler 6 + Indicator 4 + AlertBinding 2 + 监测器回归 5 + server yaml 契约 4）全绿；r0 处置后 S4 强化（脱敏断言）与 C1 改断言（组详情不得 always）复跑全绿。
- H2：zszj-module-infra 全量 `-am` **BUILD SUCCESS**（436 tests 0 failures；既有 OutboxHealthMonitorTest 5 回归全绿）。
- 门禁：`run-local-gates.mjs --fast` **10/10**。
- PG：`run-ops002b-verify.mjs` **7/7**（TCP 就绪修复后复跑稳定）；`run-pg-regression.mjs` 13 套件（本卡注册第 13 套件）——合并前多轮跑批 12/13：每轮恰有一个**基线**套件被负载性 flaky 击中（JOB-004/DB-018/DB-007 各 1~2 次，均属 ZS-DB-019.B 已登记的容器启动竞态家族，逐套件隔离复跑全绿；本卡 OPS-002.B 与 JOB-004 套件在每轮均 PASS）。干净全量轮按「合并后集成验证」安排在全部 B05 Wave4/5 卡合并后的 main 上执行并回登 docs/05（验证的正是合并后状态）。
- 安全复验：`/actuator/health/monitoring` 匿名响应仅 `{"status":"UP|DOWN"}`（details 关闭）；liveness/readiness 组不含 `outboxQueue`（`OutboxMonitoringHealthGroupTest` C2 配置层冻结）。

## 登记边界

1. **阈值/探针周期保守占位**（`backlog-warn=1000`/`backlog-critical=5000`/`longest-wait=3600s`/`failure-rate=5%`/`interval-ms=60000`），待 WP-19 实测回填；不声称容量/TPS/告警时效达标（验收④红线）。
2. **容量指标/RPO/RTO 演练/备份恢复实操归 ZS-OPS-002.C（B11）**；Micrometer gauge 归 .C 容量实测阶段。
3. **外部告警系统集成**（Alertmanager/PagerDuty/钉钉/webhook/短信）归后置外部接入（B11/D-10）；本卡告警面 = actuator 状态 + 结构化日志（自足可被任意采集发现）。
4. **HTTP 携详情的授权监控面**（`show-details: when-authorized` 或经认证的监控专用凭据）归后置外部接入——r0 P1 处置将 monitoring 组详情关闭，越阈详情经日志/授权管理端点获取。
5. 对象存储深度可达性探测归真实存储接入时；Quartz 执行日志监测不在本卡（循 JOB-004 §0.2 边界）。
6. **负载性 flaky 登记**：`SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully` 在多构建并发负载下可闪失（本卡期间 3 次，隔离复跑均绿）——与 ZS-DB-019.B 已登记容器启动竞态同属环境负载性 flaky，建议后续小卡复稳（非本卡引入，本卡改动与其无交集）。
7. OPS-002 主卡维持「开发中」（.A/.B 交付、.C 归 B11），本卡不标主卡已验收。
