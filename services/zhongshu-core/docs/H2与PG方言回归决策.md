# H2 与 PG 方言回归决策（ZS-DB-019.B）

> 文档日期：2026-09-13（ZS-DB-019.B 收口补丁交付）
> 上位依据：[03-底座二次开发顺序与验收标准](../../../docs/03-底座二次开发顺序与验收标准.md) §2（PostgreSQL 是唯一核心关系数据库）、[05-任务清单](../../../docs/05-底座模块分析与开发任务清单.md) ZS-DB-019 卡（「保留有价值的快速单测，不允许 H2 成为唯一数据库门禁」）
> 执行入口：`node scripts/db/run-pg-regression.mjs`（本地与 CI `.github/workflows/pg-regression.yml` 同一入口）

## 1. 决策结论

1. **H2 单测保留为「快速反馈层」，永不作为数据库放行门禁。** `BaseDbUnitTest`（H2 MySQL 兼容模式）继续承载服务层/批量业务逻辑的快速单测；其通过只证明 Java 逻辑与 SQL 形状，不证明 PostgreSQL 行为。
2. **PG 技术回归以 `run-pg-regression.mjs` 为唯一持续入口。** 8 套件（DB-006 序列/批量/续号、DB-007 字段映射与逻辑删除、DB-008 事务与锁、CFG-002.A 字典约束、DB-011~015 有界删除、DB-018 ORM/手写SQL/租户隔离、DB-016 Quartz 表结构、DB-017 元数据测试表）以一次性 Docker PG17 容器实跑；本地与 CI 同规则，失败退出非零并保留报告，缺 Docker 以退出码 3 明确报错（ZS-DB-019.A 夹具 `--self-test` 自证），不静默回退 H2。
3. **方言敏感变更必须同步补 PG 回归用例**（准入规则，新增代码评审检查项）：
   - 迁移 SQL（Flyway `V*__*.sql`）——任何新增迁移须能在 PG 空库/升级库重放（并入 DB-019.B 套件或 DB-004 双库验证路径）；
   - 分批/有界删除（`DELETE ... LIMIT` 类）——MySQL 专用语法在 PG 必须走「主键集有界删除」等价改写（ZS-DB-011~015 模式）；
   - 行锁/并发语义（`FOR UPDATE`、锁序、等待/超时）——DB-008 套件覆盖 PG 行为；新增锁路径须补 PG 用例或在卡片登记归 SYS-001.A 真实 PG 批次；
   - 序列/批量插入/续号、JSON 列 TypeHandler、boolean/int 布尔映射、Quartz 持久化、代码生成元数据——分别由 DB-006/007/016/017 套件守护。
4. **纯业务逻辑测试保留 H2/Mockito 即可**：不触 SQL 方言的服务层单测（`BaseMockitoUnitTest`）、静态源码扫描（`ApiInventoryTest`/`ValidationContractTest` 等）不强制 PG 化，避免回归套件膨胀稀释价值。

## 2. H2 ≠ PG 已知差异清单（本项目实测登记）

| 差异点 | H2（MySQL 模式）行为 | PG17 行为 | 守护位置 |
|---|---|---|---|
| 逻辑删除列 | `deleted = 0`（int）语义成立 | `deleted` 为 boolean，`deleted = 0` 不成立 | DB-007 套件 |
| 分批删除 | `DELETE ... LIMIT` 可用 | 语法不支持，须「主键集有界删除」 | DB-011~015 套件 |
| 行锁等待/超时 | `LOCK_TIMEOUT` 默认 1000ms | 默认无限等待（`lock_timeout` 未设时） | DB-008 套件（C4 行锁超时）；OAuth2 刷新路径端到端并发复验归 ZS-SYS-001.A |
| 序列/自增 | 自增列语义 | 序列（`nextval`）+ 续号 | DB-006 套件 |
| JSON 列 | 需 `autoResultMap` + JacksonTypeHandler | 同机制，类型映射差异由 DB-007 守护 | DB-007/DB-018 套件 |
| Quartz 表结构 | 内存/兼容建表 | PG 持久化脚本结构级校验 | DB-016 套件 |

## 3. 运行注意（2026-09-13 登记）

`run-pg-regression.mjs` 在本机与 Maven 构建/codex 评审**并行高负载**下偶发单套件 Docker 容器启动/CREATE DATABASE 竞态失败（三次复跑失败套件各不相同，套件单独运行均绿，空载聚合复跑 8/8 全绿）——属环境负载性 flaky、非套件缺陷。**CI 与收口验证建议空载执行，或失败重试一次后再判定**；连续两次同套件失败按真实缺陷 triage。

## 4. 边界与后续

- 本决策为 ZS-DB-019.B「本地先行件」：`.B` 的持续执行入口（本地 Docker PG + CI 同入口）至此闭环；`.A` 的完整验收（Maven 测试链接入 `ZS-DB-001.B` 真实连接与 B02 正式环境授权）仍按 16.1 前置另行收口，父卡 ZS-DB-019 维持「开发中」。
- H2 单测不被本决策废弃；新增「方言敏感」判定的争议项由评审（codex）按 §1.3 准入规则裁决并在任务卡登记。
