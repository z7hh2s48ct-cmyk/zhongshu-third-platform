# ZS-CFG-004 codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox）
- 评审对象：分支 `feat/cfg-004`（ZS-CFG-004 B03 部分：参数目录 + 类型/范围/枚举校验 + 乐观锁并发冲突）
- 结论：**r4 PASS / 0 发现**（评审收敛）

## r0（commit 38dde380）FAIL 1×P1 + 2×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | 乐观锁用「保存请求开始时重读的 updateTime」作版本，请求/详情均不携带编辑时版本，无法检测旧表单覆盖（codex 以 H2 实证：注册开关改 false 后提交旧值 true 表单仍成功） | r1 修复 `1b71bb4c`：详情返回版本、更新必须回传（初版复用 updateTime 作版本） |
| P2 | 参数目录未与真实消费方对应：两个 sys.login 参数与 sys.file.upload-mode 无生产消费方；url.druid 实为页面读取却标需重启 | r1 修复：目录新增 `wired` 标注（false=预留），url.druid 改热生效 |
| P2 | 并发测试把非冲突 ServiceException 计入成功数、未核对最终落库值 | r1 修复：成功仅在正常返回后计数、非预期异常置失败、断言落库值=成功方提交值 |

## r1（commit 1b71bb4c）FAIL 2×P1 + 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P1 | ConfigRespVO.updateTime 经全局时间序列化丢毫秒以下精度——微秒版本回传即冲突拒绝，正常编辑被误拒 | r2 改型 `5134f3ca`：弃用时间作版本 |
| P1 | datetime 秒级精度下同一存储秒内两次更新版本不推进，旧表单仍可覆盖（H2 DATETIME(0) 复现）；提精度不解决 | r2 改型：改独立整数 `version` 列 |
| P2 | SaveReqVO 新增 updateTime 被 MapStruct 自动映射，createConfig 未清除 → 可伪造初始版本 | r2 改型：createConfig 显式 `setVersion(0)` |

**r2 改型内容**：迁移 `V20260913.001__infra_config_optimistic_version.sql`（`ADD COLUMN IF NOT EXISTS version int NOT NULL DEFAULT 0`，幂等）；H2 测试 schema 同步；`ConfigDO.version`；`ConfigSaveReqVO.version` / `ConfigRespVO.version`（updateTime 保留仅作展示）；`updateConfig` 以回传 version 条件 UPDATE、成功即 +1。

## r3（commit 5134f3ca）FAIL 1×P2

| 级别 | 发现 | 处置 |
|---|---|---|
| P2 | 递增依据 exists.version、条件却依据请求 version，两者未比对一致——请求预填 8/快照 7/他人推进至 8 时本请求仍以 7→8 成功（版本不推进+旧值回填） | r3 修复 `9ef79a7a`：先校验请求 version==快照 version 才递增，条件 UPDATE 保留兜底；新增预填版本冲突、成功恰 +1 两用例 |

## r4（commit 9ef79a7a）PASS / 0 发现

评审自核改型正确性（迁移幂等、H2/PG 方言、并发推进、masked echo/改 key 路径、测试覆盖），结论 PASS。

## 验证

- RED：`updateConfig_staleFormWithOldVersion_conflictAndNotOverwrite` / `updateConfig_missingVersion_rejectAsBlindWrite` 两用例先失败（24 run 2 fail）
- GREEN：ConfigServiceImplValueValidationTest 26 + MaskTest 12 + ConfigServiceImplTest 12 = **50 tests / 0 failures，BUILD SUCCESS**（outputs/cfg004-r3-green.log）
- 合并：`--no-ff` 合并回 main `e218fe04`

## 延后/边界（非阻塞）

- 变更审计（旧/新值摘要、操作者）与恢复流程按 §16.1 归 B04 复验（父卡 ZS-CFG-004 维持「开发中」至全部适用子项验收）
- 前端 ConfigForm.vue 需随 version 契约联调（详情取 version → 更新回传；409 冲突提示刷新），归 Web E2E 联验批次
