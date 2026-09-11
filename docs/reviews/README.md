# 众墅之家 AI 赋能平台底座 — 本地 Codex 代码评审汇总

本目录记录针对 [05-底座模块分析与开发任务清单.md](../05-底座模块分析与开发任务清单.md) 各任务提交的本地 codex 代码评审结论。

- 评审工具：`codex review --commit <SHA>`（OpenAI Codex CLI，模型 `gpt-6-astra`，reasoning effort `xhigh`；具体 CLI 版本以各评审文档头部标注为准，近期评审为 `0.154.0`）
- 评审范围：① 15.2 节「众墅之家品牌与代码命名统一专项」已完成子项（首轮，见下方各表）；② B03「接口与安全链路」批次（SEC/PERM/DB 等，见文末「B03 接口与安全链路专项评审状态」）；③ 治理与门禁工具链自身（task-stats/verify-docs/close-task/run-local-gates 等治理脚本，见文末「治理与门禁工具链专项评审状态」）。
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
| ZS-SEC-008 补齐参数校验、上下文头解析与请求资源限制 | `76da2a2f`（首提）→ `3f4fa736`（P1 修复） | r0 13 files, +745/-15；r1 修复 4 files, +130/-8 | ✅ 评审通过（r0 发现 1×P1 → 修复 → r1 复评 0 发现） | [codex-ZS-SEC-008.md](codex-ZS-SEC-008.md) | r0 codex（`gpt-6-astra`/`xhigh`）发现 1×P1：chunked `application/json`（`Content-Length=-1`）绕过声明式早拒、`CacheRequestBodyWrapper` 全量缓冲耗尽堆；修复以「声明式早拒 + 限界读取（8192 buffer 边读边累加超限抛 `TooLargeException`）」两道防线闭合 + 2 例未知长度回归护栏（`CacheRequestBodyFilterTest` 6→8）；r1 `codex review --commit 3f4fa736` 复评，codex 沙箱内亲跑 19 测试全绿、判定「No actionable regressions were found」0 发现通过。附加复核 2 判断点（`parseTenantIdHeader` Unicode Nd 数字归一非越权、过滤器异常出口 catch 面完备）均非缺陷 |
| ZS-SEC-009 固定接口边界 ID/时间/分页/校验四合同 | `77ad9531`（首提）→ `d13a50b4`（返工）→ `1f6bbb1c`（P2 修复） | r0 11 files, +647/-9；返工 5 files, +62/-14；P2 修复 2 files, +3/-4 | ✅ 评审通过（r0 FAIL 2×P1+1×P2 → 分批返工 → r1 PASS 0×P1+1×P2 → P2 修复 → r2 PASS 0 发现） | [codex-ZS-SEC-009.md](codex-ZS-SEC-009.md) | r0 codex（`gpt-6-astra`/`xhigh`）发现 2×P1+1×P2：① 时区生产端 `LocalDateTime.now()` 未对齐固定 GMT+8 序列化端（UTC 令牌过期前移 8h、miniapp 拒收）；② 全局字符串 ID 断裂两端前端 `parentId===0` 等 ~23 处数值比较；③ `CommonResult<Set<Long>>` 裸集合 ID 未覆盖命名规则。用户拍板 **Option B 分批**：SEC-009.A 修好时间合同（`DateUtils.now()` 生产/比较/序列化三端对齐 + bootstrap TZ + 令牌生命周期测试）、unwire ID 序列化器（`IdToStringAnnotationIntrospector` 脚手架保留、wire 退回 `NumberSerializer` 兜底），破坏性 ID 合同拆 **SEC-009.B** 跨栈批次；r1 复评 PASS 但发现新 P2（授权码生产端未对齐共享 `isExpired`，`OAuth2CodeServiceImplTest` UTC 回归），`1f6bbb1c` 迁移 OAuth2Code/OAuth2Approve 过期生产端 → `DateUtils.now()`；r2 复评 PASS / 0 发现确认消除。本机 UTC+GMT+8 双时区 26×2 tests 全绿 |
| ZS-CFG-002.B 字典类型改码受控（worktree 编排试点首个交付子项） | `35ad3754`（隔离分支 feat/cfg-002-b）→ `8f9b9fb0`（--no-ff 合并 main） | 4 files, +110/-1 | ✅ 评审通过（r0 直接 0 发现） | [codex-ZS-CFG-002.B.md](codex-ZS-CFG-002.B.md) | codex（`gpt-6-astra`/`none`）判定「拒绝对仍被字典项引用的旧编码改码、保留不改编码的更新」0 发现；独立复核四判断点成立（触发条件精确、孤儿引用保护与既有 `deleteDictType` 同源、错误码 1-002-006-006 编号连续、5 契约测试覆盖受控/放行/边界）。codex sandbox `read-only` 未执行测试，通过性以本机 mvn RED→GREEN 41 tests / 0 failures、反应堆 BUILD SUCCESS 为准 |

> 本目录另存有同批次的评审交接单（`HANDOFF-ZS-SEC-001.A`/`002`/`005`/`007`/`012.A`、`HANDOFF-ZS-PERM-001.A`/`002.A`、`HANDOFF-ZS-DB-018`），其对应 `codex-<TASK>.md` 评审产物尚待补齐；ZS-SEC-003 为该批次首份完成的 codex 评审，ZS-SEC-008 为第二份（首个经 r0→修复→r1 两轮闭环），ZS-SEC-009 为第三份（首个经 r0→返工→r1→P2 修复→r2 三轮闭环、且触发 Option B 分批拆出 SEC-009.B 的评审），ZS-CFG-002.B 为第四份、也是**首个非 SEC 且首个经 worktree 并行编排试点（提效方案 P1 步骤⑤）在隔离分支交付的 B03 子项**（r0 直接 0 发现）。

## 治理与门禁工具链专项评审状态（ZS-GOV-001 / ZS-OPS-001.A）

> 又一轮评审：治理与门禁**工具链自身**（区别于上方 15.2 品牌专项、B03 接口与安全链路——后两者审的是业务/交付代码，这里审的是 task-stats/verify-docs/close-task/run-local-gates 等治理脚本）。评审工具、严重度定义、后续处理约定均沿用上文。前两轮评审（GOV-001-P0、步骤④）当时完成但未收口，处置文档于 2026-09-10 补写入库；步骤⑤（git worktree 并行编排器）三轮评审于 2026-09-11 随试点交付同批入库。

| 任务 | 提交 | 状态 | 评审文档 | 结论摘要 |
|---|---|---|---|---|
| ZS-GOV-001 任务仪式自动化（提效方案 P0） | `8c9b6082` | ✅ 评审完成（2×P2 确认成立 → 已由 `ea739b9c` 修复） | [codex-ZS-GOV-001-P0.md](codex-ZS-GOV-001-P0.md) | codex（`gpt-6-astra`/`xhigh`）发现 2×P2：verify-docs R6/R7 只比对已声明状态键（违背 L12「省略按 0 计」契约，人工漏写非零类目仍通过）、close-task §2 回填 replace-only（0→非零状态转换不插入新类目，与同文件 README 整段重建矛盾）；11 测试通过。左窗口独立复核 2 项均成立，**已由 `ea739b9c` 修复：R6/R7 遍历 `STATUS_ENUM` 省略按 0 计、§2 回填抽 `backfillSection2` 补插缺失非零类别，+6 回归测试** |
| ZS-OPS-001.A 聚合门禁提速（提效方案 P1 步骤④） | `c9d178b2` | ✅ 评审完成（3×P2 确认成立 → 已由 `ea739b9c` 修复） | [codex-ZS-OPS-001-step4.md](codex-ZS-OPS-001-step4.md) | codex（`gpt-6-astra`/`xhigh`）发现 3×P2：`--incremental` 增量选门在 SQL 种子编辑（G2b areas 未纳入种子数据）、跨目录 rename（`git diff --name-only` 只报目标路径、漏 G10）、删除 docs/ 外被链目标（漏 G5 断链校验）三类变更下漏选受影响门禁；14 planner 测试通过但未覆盖这三例。左窗口独立复核 3 项均成立，**已由 `ea739b9c` 修复：G2b areas 加 `services/zhongshu-core/sql/`、`git diff` 加 `--no-renames`、G5 标 `safety:true` 恒跑，+3 回归测试（另 4 存量断言更新）** |
| ZS-OPS-001.A worktree 并行编排（提效方案 P1 步骤⑤） | `2fa36a9e`→`5e7c4e1e`→`f6b019b0` | ✅ 评审完成（r0 1×P1+1×P2 → `5e7c4e1e` 修复 → r1 PASS/0；Phase2 自查 → `f6b019b0` 修复 → r2 PASS/0） | [codex-ZS-OPS-001-step5.md](codex-ZS-OPS-001-step5.md) | codex（`gpt-6-astra`/`none`）r0 发现 1×P1（`merge --into` 被忽略、实际并入当前 checkout 分支）+ 1×P2（`create --node` 供给仓库根 node_modules，与 G10 的 app 级 cwd 失配）；`5e7c4e1e` 修复（planMerge 首步 `git-assert-branch` 断言并中止、供给改 app 级），r1 复评「No actionable regressions」16 测试全绿；供给演示自查暴露 Phase2（isolationReport 对中文·大小写路径假阴性），`f6b019b0` 用 `--porcelain -z` 逐字解析 + `normPath` 归一修复、`.wt/` 入 gitignore，r2 复评 17 测试全绿 + list 中文路径显示正确 |

### 已闭合：治理/门禁工具链健壮性专项（5 × P2，非阻塞，已由 `ea739b9c` 合并修复）

| # | 任务 | 缺陷 | 文件:行 | 状态 |
|---|---|---|---|---|
| 1 | ZS-GOV-001 | verify-docs R6/R7 只遍历已声明状态键，漏写非零类目仍通过（违背 L12「省略按 0 计」契约） | `scripts/gov/verify-docs.mjs:124,140-142` | ✅ 已修复（`ea739b9c`） |
| 2 | ZS-GOV-001 | close-task §2 回填 replace-only，0→非零状态转换不插入新类目（与 README 整段重建矛盾） | `scripts/gov/close-task.mjs:69-71` | ✅ 已修复（`ea739b9c`） |
| 3 | ZS-OPS-001.A | G2b areas 仅 `scripts/brand/`，未纳入其测试消费的 SQL 种子，增量下种子同域重复漏检 | `scripts/ops/run-local-gates.mjs:31` | ✅ 已修复（`ea739b9c`） |
| 4 | ZS-OPS-001.A | `detectChangedFiles` 对 rename 只取目标路径，跨目录移动漏选源目录门禁（如 G10） | `scripts/ops/run-local-gates.mjs:84` | ✅ 已修复（`ea739b9c`） |
| 5 | ZS-OPS-001.A | G5 areas 未覆盖 docs/ 外被链目标，删除该目标增量下漏触发断链校验 | `scripts/ops/run-local-gates.mjs:34` | ✅ 已修复（`ea739b9c`） |

> 五项 P2 均已由左窗口独立复核确认成立（非误报）。评审当时依「后续处理约定」第 2 条列入待办；因其保护的正是所有后续任务依赖的文档一致性门禁（verify-docs）与本地基线门禁（run-local-gates），建议合并为一个专项批次优先闭合。**该建议已由提交 `ea739b9c` 落实**——5 × P2 同批修复：#1 verify-docs R6/R7 遍历 `STATUS_ENUM`（省略按 0 计）、#2 §2 回填抽 `backfillSection2` 纯函数（补插缺失非零类别）、#3 G2b `areas` 加 `services/zhongshu-core/sql/`、#4 `git diff` 加 `--no-renames`（rename 源+目标双上报）、#5 G5 标 `safety: true`（增量恒跑）；`verify-docs.test.mjs` +6、`run-local-gates.test.mjs` +3（另 4 存量断言更新），`node --test` 17/17 + 17/17、`run-local-gates --fast` 10/10、`verify-docs` 全量 0 issue 全绿。

### 复核闭环：对 5×P2 修复提交 `ea739b9c` 的再评审（1×P2 → `19b736f4` 修复 → 0 发现）

`ea739b9c` 落实上述 5×P2 修复后，另行 `codex review --commit ea739b9c`（`gpt-6-astra`/`xhigh`）复核该修复提交本身：判定 **PASS（0×P1）+ 1×P2**——codex 亲跑 `node --test scripts/gov/*.test.mjs` 34 pass / 0 fail 确认 5 项修复正确，同时现场复现出 `backfillSection2` 的新边界缺陷：分布串正则 `[^（(]*` 在「§2 声明无尾随括注」时越过 `。\n` 吞掉后续 task cards，把补插的缺失类别写进 §2 之外（34 测试均带 `（说明）` 尾注，未覆盖此插入场景）。

已由 `19b736f4` 修复：抽出共用跨度常量 `DIST_SPAN = '[^（(\n。]*'`（止于首个左括注/句号/换行，限界到句/行），同时用于 `parseSection2Declared`（读）与 `backfillSection2`（写）杜绝读写分歧，缺失类别插在分布串末尾（尾随标点之前）落在 §2 内；对真实 §2 句（有 `（说明）` 尾注）行为零变化；`verify-docs.test.mjs` +1 无括注插入回归。r1 `codex review --commit 19b736f4` 复评 **PASS / 0 发现**（codex 亲跑 18 测试全绿、确认「共用边界一致约束解析与回填、防止溢出到后续卡片」）。详见 [codex-fix-5p2-ea739b9c.md](codex-fix-5p2-ea739b9c.md)（r0 [raw](codex-fix-5p2-ea739b9c.raw.md)、r1 [raw](codex-fix-5p2-ea739b9c-r1.raw.md)）。
