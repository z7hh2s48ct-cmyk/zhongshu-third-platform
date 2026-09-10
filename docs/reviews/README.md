# 众墅之家 AI 赋能平台底座 — 本地 Codex 代码评审汇总

本目录记录针对 [05-底座模块分析与开发任务清单.md](../05-底座模块分析与开发任务清单.md) 各任务提交的本地 codex 代码评审结论。

- 评审工具：`codex-cli 0.153.4`（`codex review --commit <SHA>`，模型 `gpt-6-astra`，reasoning effort `xhigh`）
- 评审范围：① 15.2 节「众墅之家品牌与代码命名统一专项」已完成子项（首轮，见下方各表）；② B03「接口与安全链路」批次（SEC/PERM/DB 等，见文末「B03 接口与安全链路专项评审状态」）。
- 原始日志：每份评审的完整 stdout 保存为 `codex-<TASK>.raw.md`；本目录 `README.md` 汇总所有结论。

## 15.2 节已完成任务清单与评审状态

| 任务 | 提交 | 文件规模 | 状态 | 评审文档 | 结论摘要 |
|---|---|---|---|---|---|
| ZS-BRAND-001 冻结品牌素材台账与 zszj 命名映射 | `887bbc4f` | 2 files, +125 | ✅ 已评审 | [codex-ZS-BRAND-001.md](codex-ZS-BRAND-001.md) | 1 × P2（文档级执行顺序与批次前置冲突）——已由 hotfix-E `ec92cb85` 修复 |
| ZS-BRAND-002 后端模块/包/配置/构建产物统一 zszj 命名 | `43d71fad` | 10,059 files | ✅ 已评审 | [codex-ZS-BRAND-002.md](codex-ZS-BRAND-002.md) | 2 × P2（demo 表映射失配已由 004 修复；verify-backend-naming.mjs 未排除 target/ 已由 hotfix-E `ec92cb85` 修复） |
| ZS-BRAND-003.A 两端静态品牌与产品包名统一 zszj | `ea580b1f` | 134 files | ✅ 已评审 | [codex-ZS-BRAND-003.A.md](codex-ZS-BRAND-003.A.md) | 2 × P2 + 1 × P3（upload-weixin.js 版本字段丢失；FAQ 上游链接；logo.png 未纳入生成脚本）——均已由 hotfix-D `4c679e18` 修复 |
| ZS-BRAND-004.A 配置/种子/持久化引用迁移 | `581927d0` | 25 files | ✅ 已评审 | [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) | 1 × P1 + 3 × P2（username 迁移波及所有租户；apply-naming-migration.mjs 重写升级脚本；zsxq/www 域名冲突；email 映射不完整） |
| ZS-BRAND-005 统一代码生成模板与后续新模块命名 | `08ad8e78` | 113 files | ✅ 已评审 | [codex-ZS-BRAND-005.md](codex-ZS-BRAND-005.md) | 1 × P1（41 个 XML 快照与模板失配，已由 `89af39fc` 回退修复） |
| ZS-BRAND-006.A 品牌命名残留扫描门禁与全仓残留清理 | `b2c26ea9` | 136 files | ✅ 已评审 | [codex-ZS-BRAND-006.A.md](codex-ZS-BRAND-006.A.md) | 3 × P1 + 4 × P2（分页 fixture 已由 `52a537f5` 修复；README 例外已由 `fa40af00` 修复；pathPass 忽略 scope、例外匹配未验证覆盖、署名例外过宽、文件名前过滤不全、SVG 被跳过 均已由 hotfix-B 修复） |
| hotfix-A ZS-BRAND-004.A 演示账号迁移安全修正 | `45ab83e3` | 1 file, +22/-2 | ✅ 评审通过 | [codex-hotfix-A.md](codex-hotfix-A.md) | 3 轮迭代（r1 发现 2 × P2、r2 发现 1 × P2，r3 通过） |
| hotfix-B ZS-BRAND-006.A 门禁失效缺口修复 | `86bca7f7`→`5d11b205` | 6 files, +257/-38 | ✅ 评审通过 | [codex-hotfix-B.md](codex-hotfix-B.md) | 4 轮迭代（r0 结论通过但自带探针证据显示 3 项未闭合→未采信；r1 发现 2 × P2；r2/r3 连续 0 发现） |
| hotfix-C ZS-BRAND-004.A 迁移工具重写冻结脚本与租户同域冲突 | `27b5f459`→`35a04d78` | r0 13 files；r1 4 files | ✅ 评审通过 | [codex-hotfix-C.md](codex-hotfix-C.md) | 2 轮迭代（r0 发现 3 × P2 + 1 × P3：MySQL 种子被同域门禁漏扫、历史修复 LIKE/REPLACE 误伤端口变体、未校验目标域归属、planScope 路径拼接；r1 0 发现） |
| hotfix-D ZS-BRAND-003.A 上传版本字段/FAQ 上游链接/logo 生成 | `4c679e18` | 4 files, +11/-2 | ✅ 评审通过 | [codex-hotfix-D.md](codex-hotfix-D.md) | 1 轮迭代（r0 直接 0 发现；codex 自验上传版本解析为 2026.07.0-snapshot、23 项派生资产逐字节匹配已提交文件） |
| hotfix-E ZS-BRAND-002 命名验证器漏排 target/ + ZS-BRAND-001 docs/06 执行顺序 | `ec92cb85` | 2 files, +8/-1 | ✅ 评审通过 | [codex-hotfix-E.md](codex-hotfix-E.md) | 1 轮迭代（r0 直接 0 发现；codex A/B 自验修复前 8 假阳性/exit 1 → 修复后 0/exit 0，且未误伤已跟踪源码） |

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
| 1 | ZS-BRAND-002 | verify-backend-naming.mjs 未排除 target/，mvn compile 后误报 | `scripts/brand/verify-backend-naming.mjs:32-34` | ✅ 已修复（`ec92cb85` hotfix-E） | — |
| 2 | ZS-BRAND-003.A | upload-weixin.js 读取不存在的 zszj-version 字段 | `apps/zhongshu-miniapp/scripts/upload-weixin.js:65` | ✅ 已修复（`4c679e18` hotfix-D P2-1） | — |
| 3 | ZS-BRAND-003.A | FAQ 呈现上游链接为产品官网/文档 | `apps/zhongshu-miniapp/src/pages-core/user/faq/data.ts:35-40` | ✅ 已修复（`4c679e18` hotfix-D P2-2） | — |
| 4 | ZS-BRAND-004.A | apply-naming-migration.mjs sql scope 无排除，重写升级脚本 | `scripts/brand/apply-naming-migration.mjs:42` | ✅ 已修复（`27b5f459` hotfix-C P2-1；r1 `35a04d78` 补 path.join） | — |
| 5 | ZS-BRAND-004.A | zsxq.iocoder.cn 与 www.iocoder.cn 映射到同一目标 | `scripts/brand/naming-rules.mjs:78` | ✅ 已修复（`27b5f459` hotfix-C P2-2；r1 `35a04d78` 补齐漏改的 MySQL 种子 + 整词修复谓词） | — |
| 6 | ZS-BRAND-004.A | 升级脚本 email 映射不完整，yudao@iocoder.cn → yudao@example.com | `sql/postgresql/upgrades/20260908_brand_rename_zszj.sql:45-47` | ✅ 已修复（`45ab83e3` hotfix-A） | — |
| 7 | ZS-BRAND-006.A | 例外匹配未验证覆盖检测命中，40 字符窗内任一白名单串庇护不相关命中 | `scripts/brand/verify-brand-naming.mjs:59-63` | ✅ 已修复（`86bca7f7` hotfix-B P2-4） | — |
| 8 | ZS-BRAND-006.A | 署名例外过宽，`芋道源码` 无上下文限定 | `scripts/brand/brand-naming-allowlist.json:133-134` | ✅ 已修复（hotfix-B P2-5：`86bca7f7`/`c21fbea5`/`a5ce1032`） | — |
| 9 | ZS-BRAND-006.A | 文件名前过滤只检查 basename + 3 个模式 | `scripts/brand/verify-brand-naming.mjs:78-82` | ✅ 已修复（`86bca7f7` hotfix-B P2-6） | — |
| 10 | ZS-BRAND-006.A | SVG 资产被作为二进制跳过 | `scripts/brand/verify-brand-naming.mjs:29` | ✅ 已修复（`86bca7f7` hotfix-B P2-7） | — |
| 11 | ZS-BRAND-001 | docs/06 第 110 行执行顺序与批次前置冲突 | `docs/06-品牌素材与命名映射.md:110` | ✅ 已修复（`ec92cb85` hotfix-E） | — |
| 12 | ZS-BRAND-002 | demo 表映射失配（demo01_contact SQL 不一致） | `sql/postgresql/ruoyi-vue-pro.sql` | ✅ 已修复（`581927d0`） | — |

### P3（可选优化）

| # | 任务 | 缺陷 | 文件:行 | 状态 | 归入批次 |
|---|---|---|---|---|---|
| 1 | ZS-BRAND-003.A | derive-brand-assets.mjs 未生成 public/logo.png | `scripts/brand/derive-brand-assets.mjs:149-155` | ✅ 已修复（`4c679e18` hotfix-D P3） | — |

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

- **已评审任务**：6 个（ZS-BRAND-001 / 002 / 003.A / 004.A / 005 / 006.A）+ 5 个 hotfix（hotfix-A / hotfix-B / hotfix-C / hotfix-D / hotfix-E）
- **发现总数**：18 项原始 + 9 项 hotfix 迭代新增（hotfix-A 3 项、hotfix-B 2 项、hotfix-C 4 项；hotfix-D、hotfix-E r0 直接 0 发现，无新增）= **27 项**（5 × P1 + 20 × P2 + 2 × P3）
- **已修复**：**27 项**（5 × P1 + 20 × P2 + 2 × P3）——全部清零
  - P1 全部清零：`45ab83e3`（hotfix-A）/ `89af39fc` / `52a537f5` / `fa40af00` / `86bca7f7`（hotfix-B）
  - P2：`581927d0` + hotfix-B（P2-4/5/6/7）+ hotfix-C（本表 P2 #4/#5）+ hotfix-D（本表 P2 #2/#3）+ hotfix-E（本表 P2 #1/#11）+ hotfix 迭代新增 8 项（均已在同轮内修复）
  - P3：hotfix-C 迭代新增 1 项（planScope 路径拼接，已在 r1 修复）+ ZS-BRAND-003.A 原始 1 项（本表 P3 #1 logo.png 纳入生成，已由 hotfix-D 修复）
- **仍有效**：**0 项**——hotfix-E（`ec92cb85`）闭合本表 P2 #1/#11 后，15.2 品牌专项全部发现清零
- **hotfix-B 量化成果**：修复 P1-3 后，以修复后扫描器回测旧白名单暴露 **257 处命中**（旧门禁报告 0 违规，为假阴性），涵盖全部 5 个品牌模式；现已全部转为「逐命中由 content 例外匹配区间举证」或直接改名
- **hotfix-B 额外自查成果**：超出 codex 清单另修复 4 项门禁缺陷（缺 `g` 标志导致 970 处假阳性、`yd-*` 例外无 path 而全仓生效、`scope=content` 无模式的静默失效条目、codegen README 模式字面量假阳性）
- **hotfix-C 关键成果**：codex r0 揭示 r0「P2-2 已闭合」为**假阴性**——G2b 同域门禁正则不接受反引号，从未扫过 MySQL 种子（第 8 个方言被漏改），租户 1/121 真实同域逃过门禁；r1 补齐 MySQL 数据 + 加方言覆盖断言，并把历史修复从 LIKE/REPLACE 子串语义改为与应用 `selectListByWebsite` 一致的整词 `POSITION` 语义、增加目标域归属校验
- **hotfix-D 关键成果**：单轮 r0 直接 0 发现通过；根因是命名迁移的「刻意保留」未同步到消费端——`yudao-version` 在 `package.json` 被人工改名为 `upstream-version`（上游溯源元数据），而 `upload-weixin.js` 被通用规则自动改成读 `zszj-version`，两端失配致微信上传版本静默降级为模板版本 `4.1.0`；hotfix-D 补定义 `zszj-version` 使「定义—消费」一致，`upstream-version` 按 docs/06 映射表第 9 行保留
- **hotfix-E 关键成果**：单轮 r0 直接 0 发现通过；闭合最后 2 项 P2——(1) `verify-backend-naming.mjs` 的 `walk()` 未排除构建产物，本机真实 `target/` 下 8 个 MapStruct 生成类被 package-path 校验误判（codex A/B 自验：修复前 6784 javaFiles / 8 假阳性 / exit 1 → 修复后 6776 / 0 / exit 0），修复在唯一遍历原语统一跳过 `target/node_modules/.git/.idea`，并刻意不收 `out/build/dist`（`out` 实为 erp 出库 VO 合法源码包名，跳过会制造假阴性）；(2) `docs/06` L110 执行顺序把 B01 的 006.A 排在 B02 的 004/005 之后形成循环批次依赖，改为 002 → 003.A → 006.A（B01 收口）→ 004.A → 005（B02）
- **建议合并 hotfix 进度**：
  - ~~hotfix-A：ZS-BRAND-004.A P1-1 + P2-6~~（✅ 已完成，`45ab83e3`，3 轮 codex 评审通过）
  - ~~hotfix-B：ZS-BRAND-006.A P1-3 + P2-4 + P2-5 + P2-6 + P2-7~~（✅ 已完成，`86bca7f7`→`5d11b205`，4 轮 codex 评审通过）
  - ~~hotfix-C：本表 P2 #4 + P2 #5~~（✅ 已完成，`27b5f459`→`35a04d78`，2 轮 codex 评审通过）
  - ~~hotfix-D：本表 P2 #2 + P2 #3 + P3 #1~~（✅ 已完成，`4c679e18`，1 轮 codex 评审 r0 直接 0 发现）
  - ~~hotfix-E：本表 P2 #1 + P2 #11（verify-backend-naming 排除 target/、docs/06 执行顺序）~~（✅ 已完成，`ec92cb85`，1 轮 codex 评审 r0 直接 0 发现）

### 编号口径约定

各任务评审文档内部的发现编号（如 ZS-BRAND-006.A 的 P1-3/P2-4…）与本 README 汇总表的序号（P1 表 #2、P2 表 #7…）**是两套独立编号**。引用时须明示口径：代码注释与各 hotfix 评审文档一律引用**来源评审文档**的编号；本 README 内则一律写作「本表 P2 #n」。对照表见 [codex-hotfix-B.md](codex-hotfix-B.md) 的「编号对照说明」节。

## B03 接口与安全链路专项评审状态

> 新一轮评审：B03「接口与安全链路」批次（区别于上方 15.2 品牌专项首轮）。评审工具、严重度定义、后续处理约定均沿用上文。

| 任务 | 提交 | 文件规模 | 状态 | 评审文档 | 结论摘要 |
|---|---|---|---|---|---|
| ZS-SEC-003 规范 Token 传输与特殊连接凭据 | `59b59227` | 10 files, +378/-18 | ✅ 评审通过（r0 直接 0 发现） | [codex-ZS-SEC-003.md](codex-ZS-SEC-003.md) | codex（`gpt-6-astra`/`xhigh`）判定 token 解析 / 配置传播 / 三处调用方无可归因回归；左窗口独立复核交接单六判断点全部成立；2 × P3（交接单「新增常量」口径、docs/05 §2 括注日期串），均非代码缺陷、不阻塞。codex 沙箱跑测试因 `~/.m2` `AccessDeniedException` 受阻（环境限制，非本提交缺陷），测试通过性以右窗口本机 18/18 BUILD SUCCESS 为准 |

> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`），其对应 `codex-<TASK>.md` 评审产物尚待补齐；ZS-SEC-003 为该批次首份完成的 codex 评审。
