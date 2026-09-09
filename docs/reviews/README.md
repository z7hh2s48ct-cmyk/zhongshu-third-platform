# 众墅之家 AI 赋能平台底座 — 本地 Codex 代码评审汇总

本目录记录针对 [05-底座模块分析与开发任务清单.md](../05-底座模块分析与开发任务清单.md) 各任务提交的本地 codex 代码评审结论。

- 评审工具：`codex-cli 0.153.4`（`codex review --commit <SHA>`，模型 `gpt-6-astra`，reasoning effort `xhigh`）
- 评审范围：本轮聚焦 15.2 节「众墅之家品牌与代码命名统一专项」已完成子项。
- 原始日志：每份评审的完整 stdout 保存为 `codex-<TASK>.raw.md`；本目录 `README.md` 汇总所有结论。

## 15.2 节已完成任务清单与评审状态

| 任务 | 提交 | 文件规模 | 状态 | 评审文档 | 结论摘要 |
|---|---|---|---|---|---|
| ZS-BRAND-001 冻结品牌素材台账与 zszj 命名映射 | `887bbc4f` | 2 files, +125 | ✅ 已评审 | [codex-ZS-BRAND-001.md](codex-ZS-BRAND-001.md) | 1 × P2（文档级执行顺序与批次前置冲突） |
| ZS-BRAND-002 后端模块/包/配置/构建产物统一 zszj 命名 | `43d71fad` | 10,059 files | ✅ 已评审 | [codex-ZS-BRAND-002.md](codex-ZS-BRAND-002.md) | 2 × P2（demo 表映射失配已由 004 修复；verify-backend-naming.mjs 未排除 target/ 仍有效） |
| ZS-BRAND-003.A 两端静态品牌与产品包名统一 zszj | `ea580b1f` | 134 files | ✅ 已评审 | [codex-ZS-BRAND-003.A.md](codex-ZS-BRAND-003.A.md) | 2 × P2 + 1 × P3（upload-weixin.js 版本字段丢失；FAQ 上游链接；logo.png 未纳入生成脚本） |
| ZS-BRAND-004.A 配置/种子/持久化引用迁移 | `581927d0` | 25 files | ✅ 已评审 | [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) | 1 × P1 + 3 × P2（username 迁移波及所有租户；apply-naming-migration.mjs 重写升级脚本；zsxq/www 域名冲突；email 映射不完整） |
| ZS-BRAND-005 统一代码生成模板与后续新模块命名 | `08ad8e78` | 113 files | ✅ 已评审 | [codex-ZS-BRAND-005.md](codex-ZS-BRAND-005.md) | 1 × P1（41 个 XML 快照与模板失配，已由 `89af39fc` 回退修复） |
| ZS-BRAND-006.A 品牌命名残留扫描门禁与全仓残留清理 | `b2c26ea9` | 136 files | ✅ 已评审 | [codex-ZS-BRAND-006.A.md](codex-ZS-BRAND-006.A.md) | 3 × P1 + 4 × P2（分页 fixture 已由 `52a537f5` 修复；README 例外已由 `fa40af00` 修复；pathPass 忽略 scope、例外匹配未验证覆盖、署名例外过宽、文件名前过滤不全、SVG 被跳过 均已由 hotfix-B 修复） |
| hotfix-A ZS-BRAND-004.A 演示账号迁移安全修正 | `45ab83e3` | 1 file, +22/-2 | ✅ 评审通过 | [codex-hotfix-A.md](codex-hotfix-A.md) | 3 轮迭代（r1 发现 2 × P2、r2 发现 1 × P2，r3 通过） |
| hotfix-B ZS-BRAND-006.A 门禁失效缺口修复 | `86bca7f7`→`5d11b205` | 6 files, +257/-38 | ✅ 评审通过 | [codex-hotfix-B.md](codex-hotfix-B.md) | 4 轮迭代（r0 结论通过但自带探针证据显示 3 项未闭合→未采信；r1 发现 2 × P2；r2/r3 连续 0 发现） |

## 待修复缺陷汇总（按优先级）

### P1（需修复后合入）

| # | 任务 | 缺陷 | 文件:行 | 状态 | 归入批次 |
|---|---|---|---|---|---|
| 1 | ZS-BRAND-004.A | username 迁移波及所有租户，未限定演示账号 | `sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:37-39` | ✅ 已修复（`45ab83e3` hotfix-A） | — |
| 2 | ZS-BRAND-006.A | pathPass 忽略 scope，content 例外被误当 whole-file 豁免 | `scripts/brand/verify-brand-naming.mjs:49` | ✅ 已修复（`86bca7f7` hotfix-B） | — |
| 3 | ZS-BRAND-005 | 41 个 XML 快照与模板失配，codegen 测试失败 | `src/test/resources/codegen/*/xml/InfraStudentMapper:9` | ✅ 已修复（`89af39fc`） | — |
| 4 | ZS-BRAND-006.A | 分页测试正向 fixture 未随过滤器改名 | `TenantServiceImplTest.java:282,297` 等 6 处 | ✅ 已修复（`52a537f5`） | — |
| 5 | ZS-BRAND-006.A | README 段落未纳入白名单，门禁退出码 1 | `README.md:82` + `brand-naming-allowlist.json` | ✅ 已修复（`fa40af00`） | — |

### P2（建议改进）

| # | 任务 | 缺陷 | 文件:行 | 状态 | 归入批次 |
|---|---|---|---|---|---|
| 1 | ZS-BRAND-002 | verify-backend-naming.mjs 未排除 target/，mvn compile 后误报 | `scripts/brand/verify-backend-naming.mjs:32-34` | ❌ 仍有效 | ZS-BRAND-006.B 或独立 hotfix |
| 2 | ZS-BRAND-003.A | upload-weixin.js 读取不存在的 zszj-version 字段 | `apps/zhongshu-miniapp/scripts/upload-weixin.js:65` | ❌ 仍有效 | ZS-BRAND-003.B 或 ZS-CLIENT-005.C |
| 3 | ZS-BRAND-003.A | FAQ 呈现上游链接为产品官网/文档 | `apps/zhongshu-miniapp/src/pages-core/user/faq/data.ts:35-40` | ❌ 仍有效 | ZS-BRAND-003.B |
| 4 | ZS-BRAND-004.A | apply-naming-migration.mjs sql scope 无排除，重写升级脚本 | `scripts/brand/apply-naming-migration.mjs:42` | ❌ 仍有效 | ZS-BRAND-006.A 增强或独立 hotfix |
| 5 | ZS-BRAND-004.A | zsxq.iocoder.cn 与 www.iocoder.cn 映射到同一目标 | `scripts/brand/naming-rules.mjs:78` | ❌ 仍有效 | ZS-BRAND-004.B |
| 6 | ZS-BRAND-004.A | 升级脚本 email 映射不完整，yudao@iocoder.cn → yudao@example.com | `sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:45-47` | ✅ 已修复（`45ab83e3` hotfix-A） | — |
| 7 | ZS-BRAND-006.A | 例外匹配未验证覆盖检测命中，40 字符窗内任一白名单串庇护不相关命中 | `scripts/brand/verify-brand-naming.mjs:59-63` | ✅ 已修复（`86bca7f7` hotfix-B P2-4） | — |
| 8 | ZS-BRAND-006.A | 署名例外过宽，`芋道源码` 无上下文限定 | `scripts/brand/brand-naming-allowlist.json:133-134` | ✅ 已修复（hotfix-B P2-5：`86bca7f7`/`c21fbea5`/`a5ce1032`） | — |
| 9 | ZS-BRAND-006.A | 文件名前过滤只检查 basename + 3 个模式 | `scripts/brand/verify-brand-naming.mjs:78-82` | ✅ 已修复（`86bca7f7` hotfix-B P2-6） | — |
| 10 | ZS-BRAND-006.A | SVG 资产被作为二进制跳过 | `scripts/brand/verify-brand-naming.mjs:29` | ✅ 已修复（`86bca7f7` hotfix-B P2-7） | — |
| 11 | ZS-BRAND-001 | docs/06 第 110 行执行顺序与批次前置冲突 | `docs/06-品牌素材与命名映射.md:110` | ❌ 仍有效 | 文档级，随下轮批次同步 |
| 12 | ZS-BRAND-002 | demo 表映射失配（demo01_contact SQL 不一致） | `sql/postgresql/ruoyi-vue-pro.sql` | ✅ 已修复（`581927d0`） | — |

### P3（可选优化）

| # | 任务 | 缺陷 | 文件:行 | 状态 | 归入批次 |
|---|---|---|---|---|---|
| 1 | ZS-BRAND-003.A | derive-brand-assets.mjs 未生成 public/logo.png | `scripts/brand/derive-brand-assets.mjs:149-155` | ❌ 仍有效 | ZS-BRAND-003.B 或 ZS-BRAND-006.A 增强 |

## 严重度定义（与 codex 输出对齐）

- **P0**：阻塞合入。安全漏洞、数据丢失、生产不可用、法律/合规风险。
- **P1**：需修复后合入。功能性缺陷、明确的正确性问题、可验证的破坏性行为。
- **P2**：建议改进。文档一致性、命名规范、可读性、次要逻辑边界。
- **P3**：可选优化。风格偏好、微优化、非阻塞建议。

## 后续处理约定

1. 每份评审文档同时保留 codex 原始结论（`## Codex 原始结论` 段）与人工复核（`## 复核与处置` 段）。
2. P0/P1 缺陷须在合并入 main 前修复或明确记入待办；P2/P3 可分批处置。
3. 评审文档随对应任务提交入库，与代码变更同源可追溯。
4. 剩余待开发子项（`ZS-BRAND-003.B`、`ZS-BRAND-004.B`、`ZS-BRAND-004.C`、`ZS-BRAND-006.B`）不在本轮评审范围内，其开发时按同一流程「开发 → codex 评审 → 提交」推进。

## 本轮评审统计

- **已评审任务**：6 个（ZS-BRAND-001 / 002 / 003.A / 004.A / 005 / 006.A）+ 2 个 hotfix（hotfix-A / hotfix-B）
- **发现总数**：18 项原始 + 5 项 hotfix 迭代新增（hotfix-A 3 项、hotfix-B 2 项）= **23 项**（5 × P1 + 17 × P2 + 1 × P3）
- **已修复**：**16 项**（5 × P1 + 11 × P2）
  - P1 全部清零：`45ab83e3`（hotfix-A）/ `89af39fc` / `52a537f5` / `fa40af00` / `86bca7f7`（hotfix-B）
  - P2：`581927d0` + hotfix-B（P2-4/5/6/7）+ hotfix 迭代新增 5 项（均已在同轮内修复）
- **仍有效**：**7 项**（0 × P1 + 6 × P2 + 1 × P3），已全部分派至 hotfix-C/D/E
- **hotfix-B 量化成果**：修复 P1-3 后，以修复后扫描器回测旧白名单暴露 **257 处命中**（旧门禁报告 0 违规，为假阴性），涵盖全部 5 个品牌模式；现已全部转为「逐命中由 content 例外匹配区间举证」或直接改名
- **hotfix-B 额外自查成果**：超出 codex 清单另修复 4 项门禁缺陷（缺 `g` 标志导致 970 处假阳性、`yd-*` 例外无 path 而全仓生效、`scope=content` 无模式的静默失效条目、codegen README 模式字面量假阳性）
- **建议合并 hotfix 进度**：
  - ~~hotfix-A：ZS-BRAND-004.A P1-1 + P2-6~~（✅ 已完成，`45ab83e3`，3 轮 codex 评审通过）
  - ~~hotfix-B：ZS-BRAND-006.A P1-3 + P2-4 + P2-5 + P2-6 + P2-7~~（✅ 已完成，`86bca7f7`→`5d11b205`，4 轮 codex 评审通过）
  - hotfix-C：ZS-BRAND-004.A P2-4 + P2-5（`apply-naming-migration.mjs` sql 排除、`naming-rules.mjs` zsxq/www 域名冲突）
  - hotfix-D：ZS-BRAND-003.A P2-2 + P2-3 + P3-1（upload-weixin 版本字段、FAQ 上游链接、logo.png 生成）
  - hotfix-E：ZS-BRAND-002 P2-1 + ZS-BRAND-001 P2-11（verify-backend-naming 排除 target/、docs/06 执行顺序）

### 编号口径约定

各任务评审文档内部的发现编号（如 ZS-BRAND-006.A 的 P1-3/P2-4…）与本 README 汇总表的序号（P1 表 #2、P2 表 #7…）**是两套独立编号**。代码注释与各 hotfix 评审文档一律引用**来源评审文档**的编号；对照表见 [codex-hotfix-B.md](codex-hotfix-B.md) 的「编号对照说明」节。
