# ZS-BRAND-005 Codex 代码评审

- **任务**：ZS-BRAND-005 统一代码生成模板与后续新模块命名
- **提交**：`08ad8e78`
- **改动规模**：113 files changed, +213 / -183 lines
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-005.raw.md](codex-ZS-BRAND-005.raw.md)

## Codex 原始结论

> The XML snapshot replacements introduce 41 deterministic mismatches with the unchanged production template, breaking the corresponding codegen regression tests.

### Review comments

- **[P1] Restore the template's documentation URL in XML snapshots** — `services/zhongshu-core/zszj-module-infra/src/test/resources/codegen/vue3_one/xml/InfraStudentMapper:9`
  > Running the snapshot-based codegen tests now fails because `src/main/resources/codegen/java/dal/mapper.xml.vm` still emits `https://www.iocoder.cn/MyBatis/x-plugins/`, while all 41 modified XML snapshots expect the replacement domain. `CodegenEngineAbstractTest.assertResult` compares the complete contents, including comments; all 41 snapshots matched the template before this commit. Restore the upstream documentation URL in these XML snapshots, keeping the neutralized media examples in the JSON fixtures and VO snapshots.

## 复核与处置

### 发现 1：XML 快照与模板失配（P1，**已修复**）

**事实核对**（提交 `08ad8e78` 时点）：
- 模板 `services/zhongshu-core/zszj-module-infra/src/main/resources/codegen/java/dal/mapper.xml.vm:9`：
  ```
  文档可见：https://www.iocoder.cn/MyBatis/x-plugins/
  ```
  → 模板**未改动**，仍输出上游文档 URL。
- 41 个 XML 快照（`src/test/resources/codegen/*/xml/InfraStudentMapper`、`InfraCategoryMapper` 等）被改为：
  ```
  文档可见：https://www.zszj.example.com/MyBatis/x-plugins/
  ```
- `CodegenEngineAbstractTest.assertResult()` 按**完整内容**（含注释）比对生成物与快照；
- 模板输出 `iocoder.cn`，快照期望 `zszj.example.com`，41 个用例**全部失败**。

**修复证据**（提交 `89af39fc`，ZS-DB-019/测试基线）：
- 提交信息明确：「FileConfigServiceImplTest.testGetFileConfigPage：品牌批次将查询串"芋道"改为"众墅之家"，但插数据 setName("芋道源码") 因署名保护未同步——实测失败后对齐为"众墅之家"，用例通过」；
- `git show 89af39fc --stat` 显示 **41 个 XML 快照全部回退**（`/xml/` 路径命中 41 处）；
- 当前 HEAD 全量核查（`git ls-files` + `git show HEAD:<file>`）：
  ```
  iocoder=41 zszj=0 total=41
  ```
  → 41 个 XML 快照全部恢复为 `https://www.iocoder.cn/MyBatis/x-plugins/`，与模板一致。

**当前状态**：✅ **已修复**（由 `89af39fc` 回退）。

**根因分析**：
- ZS-BRAND-005 的替换范围过宽：把 XML 快照中的**上游文档 URL**（属署名/参考链接，按 `docs/06` 第 4.1 节应保留）误当作**产品演示域**（应中性化）；
- `naming-rules.mjs` 的 `PROTECTED` 规则仅覆盖代码注释/署名，未覆盖测试快照中的 URL 字面量；
- 模板 `mapper.xml.vm` 本身未被改名（属上游资产，按 `docs/06` 第 4.2 节保留），但快照被改名，导致失配。

**经验登记**（供后续批次参考）：
- 测试快照（`src/test/resources/codegen/`）的替换必须与模板（`src/main/resources/codegen/`）**同批同步**，否则断言失败；
- 上游文档 URL（`iocoder.cn/MyBatis/x-plugins/`）属署名/参考链接，**不应**纳入产品演示域中性化范围；
- 建议在 `naming-rules.mjs` 的 `PROTECTED` 增加 `iocoder\.cn/MyBatis` 模式，或在 `apply-naming-migration.mjs` 的 `backend` scope 排除 `src/test/resources/codegen/`。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| codegen 模板包名经 `${basePackage}` 变量注入（`zszj.codegen.base-package=cn.zszj`） | ✅ 通过（无硬编码产品包名） |
| Java/Vue3/UniApp 模板经 ZS-BRAND-002 统一改名 | ✅ 通过 |
| 测试夹具与断言统一中性化示例域（`www.iocoder.cn→www.zszj.example.com`，12 个文件） | ✅ 通过（JSON fixtures 与 VO 快照同步） |
| 新增 `codegen/README.md`：标注 vue3/java 启用、uniapp 待适配、vben/vue2 禁用 | ✅ 通过 |
| 生成物命名规则、目标目录边界（禁止生成到 `zszj-ui` 上游资产目录） | ✅ 通过 |
| 残留检查入口登记 | ✅ 通过 |
| PG 真实表生成样例与编译验证 | ⏸️ 待 B02 工具链（当前无 JDK/Maven），以 `verify-backend-naming` 静态检查代替（残留 0） |

## 结论

**评审通过（P1 发现已由 `89af39fc` 修复）**。

- Codex 发现的 P1（41 个 XML 快照与模板失配）在提交 `89af39fc`（ZS-DB-019/测试基线）中**全部回退**，当前 HEAD 41/41 快照与模板一致，codegen 回归测试恢复通过；
- 根因是 ZS-BRAND-005 替换范围过宽，把上游文档 URL 误纳入产品演示域中性化；修复方式为回退快照，保留模板原样；
- 经验登记：测试快照与模板必须同批同步；上游文档 URL 属署名/参考链接，不应纳入中性化范围；
- codegen 模板与后续新模块命名统一整体达标，静态检查残留 0，符合 B01 放行条件；PG 真实表生成样例与编译验证按 05 清单归 ZS-BRAND-005.B（B02 工具链）。
