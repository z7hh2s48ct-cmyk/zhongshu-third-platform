# BPM 引擎表与业务扩展表迁移责任决策（ZS-BPM-001）

> 文档日期：2026-09-14（ZS-BPM-001 技术准备先行件交付，B09 批次开工项）
> 上位依据：[03-底座二次开发顺序与验收标准](../../../docs/03-底座二次开发顺序与验收标准.md) B09（前置 B05/B08 与 D-07 批准，本卡「技术准备可先行」）、[05-任务清单](../../../docs/05-底座模块分析与开发任务清单.md) ZS-BPM-001 卡、[02-需求台账](../../../docs/02-一期底座需求规格与待决策台账.md) §6.5（D-07 确认前允许 PG 适配与中性技术夹具、禁止固化业务字段/状态）
> 执行入口：`node scripts/db/run-bpm001-verify.mjs`（已注册为 `run-pg-regression.mjs` 第 9 套件，本地与 CI 同规则）

## 1. 决策结论

1. **Flowable 引擎表（`ACT_*`）由引擎 schema 管理器自管，禁止任何手写 DDL 或 Flyway 接管。**
   建表/升级只允许发生在「迁移期」：以库 owner 账号（`zhongshu_owner`）在部署窗口执行
   `database-schema-update=true` 的引导（本验收阶段 1 即此形态）。引擎版本由
   `zszj-dependencies` 的 `flowable.version`（当前 8.0.0）钉住；引擎升级 = Flowable 自身
   schema 升级机制执行，升级前须在真实 PG 演练（同库 `schema-update=true` 幂等引导 +
   `ACT_GE_PROPERTY.schema.version` 前后比对，S2 检查守护）。
2. **运行期零 DDL：低权限运行账号（`zhongshu_app`）以 `database-schema-update=false` 启动。**
   运行账号对引擎表仅有 DML 权限（`env-setup-test.sql` 的 default privileges 自动授权
   owner 新建表），S1 检查直证其建表被拒。这同时满足 B02「低权限运行账号不能建库/越权」
   反向验收在 BPM 域的延伸。
3. **BPM 业务扩展表（`bpm_form_info`、`bpm_user_group`、`bpm_process_instance_ext` 等
   模块自有表）走 Flyway `V*__*.sql` 版本化迁移，与引擎表责任严格分离。** 未来启用
   zszj-server 的 BPM 装配（现在 server POM 与根 reactor 均保持注释=关闭模块，B01 白名单
   门禁守护）的批次，必须同时新增业务扩展表迁移；不得依赖引擎 schema 管理器创建业务表，
   也不得用 Flyway 创建/修改任何 `ACT_*` 表。当前 V1 基线不含任何 `ACT_*`/`bpm_*` 表
   （S0 检查：全基线零流程表 = 关闭 BPM 无流程路由/后台副作用的结构性证明）。
4. **夹具边界（D-07 门禁）：** 验收只使用中性技术夹具（`tech_neutral_approval` 人工审批、
   `tech_neutral_async_echo` 异步回声），不导入请假示例，不固化任何众墅业务对象、状态机、
   组织任职语义；引擎 `tenantId` 在夹具中验证为「技术标签、无 ACL」（跨租户不过滤查询可见），
   业务组织/对象隔离归 B08/D-07 后的 ZS-BPM-002/003。

## 2. 验收断言与证据位置（2026-09-14 实测）

| 验收项（ZS-BPM-001 卡） | 实现位置 | 结果 |
|---|---|---|
| PG 上部署、发起、通过、拒绝、撤回、转办、分页/历史查询 | `BpmPgHarnessBootstrapTest`（部署幂等/版本钉住/发起/租户标签）+ `BpmPgHarnessRuntimeTest` R20~R80 | 全部 PASS（见 outputs/bpm-001/runtime-report.json） |
| 重复部署可恢复（幂等） | bootstrap：同内容重复部署 deployment 数不变 | PASS |
| 重启可恢复 | 阶段 2 以独立 JVM、`schema-update=false`、低权限账号重启接入同库：定义/实例/历史/schema.version 全部一致（R10） | PASS |
| 事务失败可恢复 | R60：Spring `DataSourceTransactionManager` 回滚事务内的发起不留引擎痕迹；提交事务正常持久化 | PASS |
| 异步执行器与启停 | bootstrap 执行器挂起留积压（`ACT_RU_JOB`=1、探针零记录）→ runtime 重启后积压跨重启持久保留（执行器不自启），显式 `asyncExecutor.start()` 消化积压且无死信、实时路径再验、`shutdown()` 回到非活动态（R70，isActive 直证） | PASS |
| 关闭 BPM 无副作用 | S0：System/Infra 全基线零 `ACT_`/`FLW_` 表；server POM/根 reactor 保持 BPM 注释 + `ModuleWhitelistTest` 门禁不变 | PASS |
| 装配扩展点同构直证 | `BpmPgHarnessConfiguration` 与 `BpmFlowableConfiguration` 同用 `SpringProcessEngineConfiguration` + `setEventListeners` 扩展点，R90 证明监听器在真实 PG 引擎触发 | PASS |

两阶段账号/参数：阶段 1 `zhongshu_owner` + `SCHEMA_UPDATE=true` + `ASYNC_EXECUTOR=false`；
阶段 2 `zhongshu_app` + `SCHEMA_UPDATE=false` + `ASYNC_EXECUTOR=false`（执行器由 R70 显式
`start()`/`shutdown()` 控制生命周期，保证重启恢复断言的确定性）。编排器注入
`ZSZJ_BPM_HARNESS_*` 环境变量，缺失即快速失败，不静默跳过。

## 3. 运行注意

- 首次运行会联网拉取 Flowable 8.0.0 依赖树（此前 server 未启用 BPM，本地仓库无 Flowable）。
- zszj-module-bpm 不在默认 reactor（根 POM 注释态），套件以 `mvn -f zszj-module-bpm/pom.xml`
  独立构建，兄弟模块依赖取自本地仓库已安装产物；**不通过根 POM profile 启用 reactor 成员**
  （`ModuleWhitelistTest` 以根 POM 非注释 `<module>` 为门禁事实源，引入 profile 会被判违规）。
- 就绪探测必须走 TCP（`-h 127.0.0.1`）：postgres 镜像 initdb 期间的临时服务器只监听
  unix socket，socket 探测可能误判就绪导致建库落到临时服务器失败（2026-09-14 首跑实测）。

## 4. 边界与后续

- 本件为 ZS-BPM-001「技术准备先行」交付：**B09 主卡链路仍受门禁**——ZS-BPM-002 审批资格
  与对象授权待 ZS-PERM-002.B（B08）；ZS-BPM-003 首链领域状态机待 D-07 批准 + B05/B08；
  ZS-OPS-001.E 门禁待 ZS-OPS-001.D。B09 整批放行不因本件提前。
- 正式首链启用 BPM 时须：server POM/根 reactor 解注释 + `ModuleWhitelist.ENABLED_MODULES`
  同步 + 业务扩展表 Flyway 迁移（§1.3）+ 以 owner 账号执行引擎表引导。
