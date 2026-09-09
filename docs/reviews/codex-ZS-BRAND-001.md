# ZS-BRAND-001 Codex 代码评审

- **任务**：ZS-BRAND-001 冻结品牌素材台账与 zszj 命名映射
- **提交**：`887bbc4f25c0e2efbadba498c107dc9162c5ccee`
- **改动规模**：2 files changed, +125 lines（纯文档）
- **改动文件**：`README.md`、`docs/06-品牌素材与命名映射.md`（新增）
- **评审时间**：2026-09-09
- **评审工具**：`codex-cli 0.153.4`，模型 `gpt-6-astra`，reasoning effort `xhigh`
- **原始日志**：[codex-ZS-BRAND-001.raw.md](codex-ZS-BRAND-001.raw.md)

## Codex 原始结论

> This documentation-only commit introduces an execution order that conflicts with the existing batch prerequisites. No runtime code changed, and the recorded logo hash, dimensions and transparency were verified.

### Review comments

- **[P2] Keep the B01 naming gate ahead of B02 migration work** — `docs/06-品牌素材与命名映射.md:110`
  > Following this execution order postpones ZS-BRAND-006.A until after ZS-BRAND-004 and ZS-BRAND-005, whose prerequisites belong to B02. However, 006.A is required to complete B01, and B02 requires B01, creating a circular batch dependency. The established task ordering (`docs/05-底座模块分析与开发任务清单.md#L853`) explicitly groups 002, 003.A and 006.A in B01. Move 006.A before 004.A/005 and identify the migration step as 004.A rather than the cross-batch parent task.

## 复核与处置

### 事实核对

1. **Codex 判定正确**：`docs/06-品牌素材与命名映射.md` 第 110 行当前内容仍为：
   > 执行顺序：ZS-BRAND-002（后端）→ ZS-BRAND-003.A（两端静态品牌）→ ZS-BRAND-004（种子与存量迁移）→ ZS-BRAND-005（代码生成）→ ZS-BRAND-006.A（残留门禁）。
2. **与 05 清单第 16.1 节批次前置冲突**：
   - ZS-BRAND-006.A 验收批次 = **B01**（前置：ZS-BRAND-002、ZS-BRAND-003.A、ZS-OPS-001.A）
   - ZS-BRAND-004.A 验收批次 = **B02**（前置：ZS-BRAND-001、ZS-BRAND-002、ZS-DB-003、ZS-DB-004、ZS-DB-019.A）
   - ZS-BRAND-005 关联批次 = **B02**（前置：ZS-BRAND-001、ZS-BRAND-002、ZS-DB-017）
   - B01 应先于 B02 放行；文档描述的顺序把 B01 的 006.A 排在 B02 的 004/005 之后。
3. **实际提交顺序验证了 codex 观察**：`git log` 显示 004（581927d0）、005（08ad8e78）在 006.A（b2c26ea9）之前提交，与文档一致，但与批次前置矛盾。

### 严重度评估

- **确认 P2**（建议改进，非阻塞）：
  - 未影响任何运行时行为（纯文档提交）；
  - 未导致实际功能缺陷；
  - 但确为文档一致性问题，会误导后续开发者按错误顺序推进批次。

### 处置建议

- **修复方式**：更新 `docs/06-品牌素材与命名映射.md` 第 110 行执行顺序为：
  > 执行顺序：ZS-BRAND-002（后端）→ ZS-BRAND-003.A（两端静态品牌）→ **ZS-BRAND-006.A（残留门禁，B01 收口）** → ZS-BRAND-004.A（种子与存量迁移，B02）→ ZS-BRAND-005（代码生成，B02）。ZS-BRAND-003.B、ZS-BRAND-004.B/.C、ZS-BRAND-006.B 属 B03/B05/B06 联验子项，按 16.1 节前置另行收口。
- **附加**：将「ZS-BRAND-004」明确为「ZS-BRAND-004.A」，避免与跨批次主编号混淆。
- **本任务不阻塞合入**：ZS-BRAND-001 已合入 main，此项修复归入下一轮文档整理批次（可与 ZS-BRAND-006.B 或 ZS-GOV-001 文档 CI 扩充一并处置）。

## 其他核对项（Codex 已验证）

| 项目 | 结果 |
|---|---|
| 原图 SHA-256 `275efbf794ea5e485bf49111d8681170dc3b07e3d858e07fb32be9d7aeaabe5a` | ✅ 与 `docs/assets/brand/zszj-logo-user-reference.png` 实测一致 |
| 原图尺寸 1060×228 RGBA 透明 PNG | ✅ 实测一致 |
| 冻结包根 `cn.zszj` / GroupId `cn.zszj` / 配置前缀 `zszj.*` / 环境变量 `ZSZJ_*` | ✅ 与后续 002/003.A/004/005/006.A 实施一致 |
| 未涉及运行时代码 | ✅ 确认（diff 仅 README + docs/06） |

## 结论

**评审通过（含 1 项 P2 待改进）**。ZS-BRAND-001 作为品牌专项的映射冻结文档，为后续所有改名批次提供了单一权威依据，事实核对全部通过；P2 文档顺序问题不阻塞合入，纳入后续文档整理批次修复。
