# codex 评审处置：ZS-CFG-002.B 字典类型改码受控（worktree 并行编排试点首个交付子项）

- **任务**：ZS-CFG-002.B 字典类型改码受控（提效方案 P1 步骤⑤ worktree 并行编排试点**首个交付子项**；父卡 ZS-CFG-002 字典管理，维持开发中至全部适用子项验收）
- **提交**：`35ad3754`（在隔离 worktree 分支 `feat/cfg-002-b` 提交 → `--no-ff` 合并回 main `8f9b9fb0`）
- **改动规模**：4 files changed, 110 insertions(+), 1 deletion(-)
- **改动文件**：[ErrorCodeConstants.java](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/enums/ErrorCodeConstants.java)（+1 新错误码 `DICT_TYPE_HAS_CHILDREN_ON_TYPE_CHANGE` = 1_002_006_006）、[DictTypeServiceImpl.java](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/dict/DictTypeServiceImpl.java)（+9/-1 `updateDictType` 改码受控）、[DictTypeServiceImplTest.java](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/dict/DictTypeServiceImplTest.java)（+56，3 测试）、[DictDataServiceImplTest.java](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/dict/DictDataServiceImplTest.java)（+45，2 契约测试）
- **评审时间**：2026-09-11
- **评审工具**：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `none`，sandbox `read-only`（`codex review --commit 35ad3754`，EXITCODE 0）
- **原始日志**：[codex-ZS-CFG-002.B.raw.md](codex-ZS-CFG-002.B.raw.md)

## Codex 原始结论

> The change rejects dictionary type-code updates when existing entries reference the old code, while preserving updates that leave the code unchanged. No actionable defects introduced by this commit were identified; tests were inspected but not executed.

**评审意见数：0**（原始日志中 `[P0]/[P1]/[P2]/[P3]` 标记数 = 0；`EXITCODE 0`）。日志含 codex 启动期 `codex_models_manager ... failed to refresh available models: timeout`（网络噪声，不影响判定产出）。

> **codex 侧测试未执行说明（非本提交缺陷）**：codex sandbox 为 `read-only`，明确 "tests were inspected but not executed"。测试通过性以本机 mvn 为准——worktree 内 RED→GREEN（`DictTypeServiceImplTest` 改码受控用例先红后绿）、`mvn -pl :zszj-module-system test` **41 tests / 0 failures**、反应堆 **BUILD SUCCESS**（唯一失败 `CodegenEngineUniappTest` 系 Windows CRLF 存量环境问题、与本提交无关）。

## 复核与处置

独立复核（读取合并后源码 + 5 契约测试 + 错误码编号 + 与既有 `deleteDictType` 保护对照），结论与 codex 一致：**代码层无 P0/P1/P2/P3 缺陷**。

### 判断点 1 — 改码受控触发条件精确 ✅ 成立

[DictTypeServiceImpl](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/dict/DictTypeServiceImpl.java) `updateDictType` L67 捕获 `oldDictType = validateDictTypeExists(id)`（原代码忽略该返回值），L75-78 三重与条件：`oldDictType != null`（空卫）`&& !StrUtil.equals(oldDictType.getType(), updateReqVO.getType())`（编码确实变化）`&& dictDataService.getDictDataCountByDictType(oldDictType.getType()) > 0`（原编码下仍有字典项）→ 抛 `DICT_TYPE_HAS_CHILDREN_ON_TYPE_CHANGE`。**仅当编码变化时才校验子项**——只改名/状态/备注等不动编码的更新（`StrUtil.equals` 为真）短路跳过，不受影响。

### 判断点 2 — 孤儿引用根因，与既有删除保护同源 ✅ 成立

字典项以字符串引用字典类型编码（`dict_data.dict_type`，**无外键约束**）。改码会使原编码下的字典项成为**孤儿引用**（指向不再存在的 type）——与 `deleteDictType`（L90-91）既有的 `DICT_TYPE_HAS_CHILDREN` 保护同源（删除会使子项孤儿 → 拒），本子项把同一保护扩展到「改码」这一等价破坏性操作。须先迁移/清理子项再改码。

### 判断点 3 — 错误码编号连续、文案准确 ✅ 成立

[ErrorCodeConstants](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/enums/ErrorCodeConstants.java) L71 `DICT_TYPE_HAS_CHILDREN_ON_TYPE_CHANGE = new ErrorCode(1_002_006_006, "无法修改字典类型编码，该类型下还有字典数据")`，紧接 L70 `DICT_TYPE_HAS_CHILDREN`（1_002_006_005），编号连续无冲突；文案与删除保护（"无法删除，该字典类型还有字典数据"）平行、语义准确。

### 判断点 4 — 5 契约测试覆盖受控/放行/边界 ✅ 三规则覆盖

- [DictTypeServiceImplTest](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/dict/DictTypeServiceImplTest.java)（+3）：`testUpdateDictType_typeChangeWithChildren_rejected`（改码 + 有子项 → 拒，RED→GREEN 关键用例）、`testUpdateDictType_typeChangeWithoutChildren_success`（改码 + 无子项 → 放行、断言新编码落库）、`testUpdateDictType_renameOnly_sameType_success`（编码不变仅改名 → 不受影响、无 stub 避 strict-stub）。
- [DictDataServiceImplTest](../../services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/service/dict/DictDataServiceImplTest.java)（+2，端到端契约）：`testCreateDictData_dictTypeNotEnable_rejected`（停用字典类型下建项 → `DICT_TYPE_NOT_ENABLE`）、`testGetDictDataListByDictType_historyReadable`（停用后历史字典项仍可读回，保「停用不清历史」语义）。

### 严重度评估与处置

| 编号 | 级别 | 观察 | 处置 |
|---|---|---|---|
| — | P0/P1/P2/P3 | **无** | codex r0 0 发现；独立复核亦未发现代码级缺陷 |

**P0/P1 缺陷：无。** 依 [README.md](README.md)「后续处理约定」#2，无合入前须修复的阻断项。

## 结论

**评审通过（r0 直接 0 发现）。** codex（`gpt-6-astra`/`none`）判定本提交「拒绝对仍被字典项引用的旧编码做改码、同时保留不改编码的更新」，无可归因缺陷；独立复核四判断点全部成立——改码受控触发条件精确（仅编码变化 + 有子项才拒）、孤儿引用保护与既有 `deleteDictType` 同源、错误码编号连续文案准确、5 契约测试覆盖受控/放行/边界三规则。codex sandbox `read-only` 未执行测试（环境限制，非本提交缺陷），通过性以本机 mvn RED→GREEN 41 tests / 0 failures、反应堆 BUILD SUCCESS 为准。

本子项系 worktree 并行编排试点（提效方案 P1 步骤⑤）**首个在隔离 worktree 走通「失败测试→实现→mvn→codex→合并→文档串行同步」全周期的交付**；`--no-ff` 合并保留任务边界。**待验收边界**：两端字典刷新联验（Web E2E）与真实 PG/API 回归归 ZS-SYS-001.A，Java 层并发用例归 ZS-DB-019.B；本子项契约层已交付，父卡 ZS-CFG-002 维持开发中至全部适用子项验收。工具弧（编排器自身）评审另见 [codex-ZS-OPS-001-step5.md](codex-ZS-OPS-001-step5.md)。
