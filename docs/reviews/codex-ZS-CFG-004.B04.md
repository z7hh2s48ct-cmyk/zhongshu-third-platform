# codex 评审处置：ZS-CFG-004 B04 复验部分——配置变更审计接线 AuditPort 与审查后恢复流程

> 被审对象：worktree 隔离分支 `feat/cfg-004-b04` 的渐进提交（impl `a4365897` → r0 处置 `86ba7a91` → r1 处置 `060d4366`），三弧均以 `codex review --commit <SHA>` 评审。`--no-ff` 合并 main（`1b67af21`）。
> 背景：ZS-CFG-004 B03 值校验部分已于 2026-09-13 交付（`e218fe04`，见 [codex-ZS-CFG-004.md](codex-ZS-CFG-004.md)）；本批次交付卡片归 B04 的「复验审计」部分——变更审计（旧/新值摘要、版本、操作者，敏感旧值不写审计原文）与恢复流程（不绕过权限、不复活关闭模块、并发冲突明确），并首次接线 ZS-AUDIT-001 的统一 `AuditPort`（B04 同库事务内审计）。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：三轮保留于 `outputs/cfg004b04-codex-r{0,1,2}.log`（UTF-16，属 gitignored 工作痕迹未入库；各轮结论已逐字引用于本文）。
> 说明：`read-only` sandbox 未重跑 Maven（r1 曾尝试读取 ~/.m2 PG 驱动做运行期复现，被沙箱拒绝，改为静态推演——结论仍成立），测试通过性以本机真实构建为准（见「验证」）。

## 交付内容（impl `a4365897`，20 files）

- **变更历史表** `infra_config_history`（迁移 `V20260914.011` + H2 schema 同步）——create/update/delete/restore 仅追加留痕：config_id/变更时 key/变更类型/前后值/前后乐观锁版本/操作者/审查依据；`@TenantIgnore` + `@KeySequence` 对齐 `ConfigDO` 惯例。
- **双轨留痕记录器** `ConfigChangeRecorder`（infra/config）——①历史行随业务事务落库；②SUCCESS 审计经 `AuditPort`（OBJECT_CREATED/OBJECT_UPDATED/OBJECT_DELETED + 新增 `CONFIG_PARAM_RESTORED` 事件类型）随事务提交、失败 fail-closed 整体回滚；③恢复拒绝以 ACCESS_DENIED + DENIED 独立事务留痕，其失败不阻断业务拒绝返回。审计 detail 只装脱敏摘要（key/oldKey/前后值掩码/前后版本/reason/historyId）。
- **敏感脱敏**——SECRET/SENSITIVE（`ConfigSensitiveClassifier` 判定）在历史与审计 detail 一律落掩码 `******`；key 改名前后各按当时 key 判定；历史分页对敏感配置整页掩码输出。
- **恢复流程** `PUT /infra/config/restore`（权限复用 `infra:config:update`）——仅回写既有行 value（不插行、不改 key/visible/category/name），走同一 `ConfigValueValidator` 值校验与乐观锁契约；已删除配置拒绝（结构性排除「复活」路径：模块启停归 `ModuleCatalog` 构建期白名单，infra_config 无模块开关键）；跨配置历史匹配拒绝；秘密/敏感历史拒绝自动恢复（须手工重填）；reason 必填并落两轨。
- **历史查询** `GET /infra/config/history/page`（`infra:config:query`）+ 错误码 `1-001-000-011~013`（`CONFIG_RESTORE_HISTORY_NOT_EXISTS`/`CONFIG_RESTORE_NOT_RESTORABLE`/`CONFIG_RESTORE_HISTORY_MISMATCH`）。
- **测试** `ConfigChangeAuditRestoreTest`（17 用例，H2）+ 既有三测试类装配补齐 + `DatabaseTableServiceImplTest` 表清单断言同步；策略文档 [配置变更审计与恢复流程](../../services/zhongshu-core/docs/配置变更审计与恢复流程.md)。

## 评审弧（r0→r1→r2，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 被审提交 | 结论 | 要点与处置 |
|---|---|---|---|
| **r0** | `a4365897`（impl） | **1×P1 + 2×P2** | P1 新端点未登记 `api-inventory-baseline.txt`（`ApiInventoryTest` 拦截新增路由，server 套件会红）→ r1 重生成基线；P2 删除留痕未绑定真实删除行（并发双删会给未删方记 SUCCESS 审计）→ r1 逐行条件删除 + `affected>0` 才留痕、单删 0 行拒绝；P2 NORMAL 配置字面 `******` 与脱敏哨兵同形被误拒恢复 → r1 引入显式 `old/new_value_redacted` 标志列，可恢复性以标志判定 |
| **r1** | `86ba7a91`（r0 处置） | **2×P1** | P1 脱敏标志列 `int2` 与 Java `Boolean` 的 `setBoolean` 绑定在 PG 拒收（H2 `bit` 掩盖方言差异）→ r2 改 `bool`（对齐 `infra_config.visible` 惯例）；P1 改写已发布迁移致 Flyway 校验和失配 → **核实 `V20260914.011` 未合入 main、无任何已应用环境**（本会话未跑过 PG 回归，CI 仅跑 main），按弧内改型在未合并分支定型、不另发 ALTER 前向迁移（循 ZS-CFG-004 B03 r2 先例），迁移头注明理由 |
| **r2** | `060d4366`（r1 处置） | **PASS / 0 发现** | 「The PostgreSQL boolean columns correctly match the existing Java Boolean fields and their consumers. No actionable regressions were found; the static migration gate and diff checks passed, but PostgreSQL runtime validation was not performed.」——PG 运行时验证于合并后主树补做（见「验证」） |

**收口依据**：r2 达 0×P0/P1 阀值；r0 两项 P2 均**弧内修复**并补看守用例（17 用例含 literal-mask 恢复）；r1 两项 P1 弧内改型（bool 方言 + 未发布迁移定型）；无延后项、无仍有效阻塞项。

## 交叉工作区观察（非本卡缺陷，一并登记）

- **`ApiInventoryTest` 在 main 上原为红**：ZS-FILE-003 收口（`677d46e6`）新增 `/infra/file/upload-credential`、`/infra/file/upload-complete` 两路由但**漏登 API 清单基线**——与历史上「收口漏 reviews/README 行」同类系统性坑。`86ba7a91` 重生成基线时一并补登（`ApiInventoryTest` 4/4 转绿），FILE-003 主卡为待验收、登记方应知悉；建议后续收口十步将「server 套件 ApiInventoryTest」纳入变更涉及 controller 时的必跑项。
- **docs/05 版本交错**：本卡文档串行同步基线为头部 V1.49（ZS-AUDIT-002 收口行自称 V1.45、gov 统计提交推进头部至 V1.49，编号交错但行内容未冲突）；本卡行取 V1.50。

## 验证

- **合并前（worktree `feat/cfg-004-b04`）**：infra 模块全量 **295 tests / 0 failures，BUILD SUCCESS**（新增 ConfigChangeAuditRestoreTest 17 用例；既有 ConfigServiceImplTest/MaskTest/ValueValidationTest 装配补齐、DatabaseTableServiceImplTest 表清单断言同步 1→2）；全反应堆 `mvn -T1C test-compile` **EXIT=0**（21 模块）。
- **合并后（main `1b67af21`，含他区 AUDIT-002 合并态）**：全量门禁 13 项——**G1~G13 全 PASS**（含 G3 品牌全仓扫描 0 violation、`ApiInventoryTest` 4/4 绿，main 上的既有红由 `86ba7a91` 基线补登修复）；**G14 首跑 45/49**——SYS-CONFIG-P1/P2/N1~3/N4 四用例 code=500，服务端日志定位 `relation "infra_config_history_seq" does not exist`：sys001 夹具固定迁移清单停更于 `V20260913.001`（FILE-003 的 001~003、AUDIT-001 的 010 同样未入清单，仅因矩阵未触及未爆），夹具改**目录扫描全部迁移**（9 个）后重跑 **49/49 EXIT=0**——config 创建/修改/恢复经真实 server+PG 走通双轨留痕（bool 标志列 + MP insert + AuditPort），闭环 r2 注明的「PG 运行时验证未做」；`run-pg-regression.mjs` **9 套件 ALL PASS EXIT=0**（Flyway 应用 `V20260914.011`）。
- **flaky 登记（非本卡）**：全量门禁首轮 `SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully`（biz-tenant，SEC-012.B 领地）失败，单独复跑 10/10 GREEN——SSE 流式时序敏感用例在并发负载下偶发，与本卡改动无关（本卡未触碰 biz-tenant/security starter）。
- **收口文档**：docs/05 V1.50（卡片开发记录 + 变更行）、reviews/README B04 专项表新增本行；post-docs `run-local-gates --fast` 复跑（G3/G4/G5 对新文档内容复验）。

## Codex 原始结论（逐轮逐字引用）

### r0（`a4365897`）

> The new endpoints break the existing API inventory test. Deletion audits can record incorrect successful changes under concurrency, and restoration rejects a supported literal configuration value.
>
> - [P1] Update the API inventory baseline for both new endpoints — ConfigController.java:139-145：The new restore and history routes are absent from `zszj-server/src/test/resources/api-inventory-baseline.txt`. `ApiInventoryTest.inventoryMatchesBaseline()` scans these controller mappings and rejects any additions, so the server test suite now fails. […] Regenerate and commit the reviewed inventory baseline alongside the endpoints.
> - [P2] Tie deletion audits to the rows actually deleted — ConfigServiceImpl.java:149-151：If two requests read the same configuration before either deletes it, the second `deleteById` affects zero rows but still writes DELETE history and a SUCCESS audit attributed to that caller. […] `deleteConfigList` has the same issue […]. Lock or version-check the snapshots and verify affected rows before recording successful deletions.
> - [P2] Distinguish redacted history from literal asterisk values — ConfigServiceImpl.java:193-195：A NORMAL configuration legitimately containing `******` cannot be restored after an update: the recorder stores that literal value unchanged, but this check treats it as redacted history and rejects restoration. […] Persist an explicit redaction indicator and use it for restorability rather than inferring sensitivity solely from the stored string.

### r1（`86ba7a91`）

> The redaction columns are incompatible with JDBC Boolean binding on PostgreSQL, breaking configuration mutations. Rewriting the existing migration also breaks upgrades for databases that already applied it.
>
> - [P1] Use PostgreSQL boolean columns for the redaction flags — V20260914.011__infra_config_history.sql:13-15：On PostgreSQL, both new fields are Java `Boolean`s, so MyBatis binds them through `PreparedStatement.setBoolean`; PostgreSQL rejects these boolean parameters for `int2` columns. Since `ConfigChangeRecorder` inserts both flags during every configuration mutation, create/update/delete/restore transactions now fail and roll back. Use `boolean NOT NULL DEFAULT false` or an explicit numeric type handler. The H2 `bit` columns conceal this mismatch; add PostgreSQL coverage as required by the dialect regression rules.
> - [P1] Introduce a forward migration for existing history tables — V20260914.011__infra_config_history.sql:13-15：For any database that already applied the parent commit's migration, editing this version causes a Flyway checksum mismatch and prevents startup. […] Preserve the original migration and add a new `ALTER TABLE` migration with conservative backfilling […].（处置：经核实 011 未合入 main、无已应用环境，按弧内改型定型，见上表 r1 行）

### r2（`060d4366`）

> The PostgreSQL boolean columns correctly match the existing Java Boolean fields and their consumers. No actionable regressions were found; the static migration gate and diff checks passed, but PostgreSQL runtime validation was not performed.
