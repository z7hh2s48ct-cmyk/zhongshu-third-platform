# Codex 评审：hotfix-C — ZS-BRAND-004.A 迁移工具重写冻结脚本与租户同域冲突修复

## 评审元信息

| 项目 | 值 |
|---|---|
| 评审对象 | `27b5f459`（r0）→ `35a04d78`（r1） |
| 评审工具 | codex-cli 0.153.4, model gpt-6-astra, reasoning effort xhigh |
| 评审轮次 | 2 轮 codex 评审（r0 发现 4 项 → r1 0 发现） |
| 关联评审 | [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) |
| 修改文件 | r0：13 files, +311/-41；r1：4 files, +94/-17 |
| 原始日志 | [codex-hotfix-C.raw.md](codex-hotfix-C.raw.md)（r0）、[codex-hotfix-C-r1.raw.md](codex-hotfix-C-r1.raw.md)（r1） |
| 最终状态 | ✅ 通过（r1 codex 0 发现；G2b 单测 9/9；命名门禁单测 15/15；本地门禁 11/11） |

## 修复的原始缺陷（r0：`27b5f459`）

本 hotfix 修复 [codex-ZS-BRAND-004.A.md](codex-ZS-BRAND-004.A.md) 结论中「仍有效」的 2 项发现，二者同属
`naming-rules.mjs` / `apply-naming-migration.mjs` 工具链，评审建议合并为同一 hotfix：

| 内部编号 | 优先级 | 原始缺陷 | 修复方式 |
|---|---|---|---|
| P2-1 | P2 | `apply-naming-migration.mjs sql` 的 `exclusions.sql` 为空数组，会选中并改写**已冻结**的存量升级脚本 `upgrades/20260908_brand_rename_zszj.sql`——旧值谓词被改成新值，脚本从此无法升级旧基线库（违反 docs/06 第 4.1 节「迁移脚本冻结」） | 引入跨全部 scope 生效的 `PRESERVED`（冻结/必要保留）清单，排除发生在**选文件阶段**（`applyRules` 之前）；把选文件与试算导出为纯函数并新增 `--dry-run` |
| P2-2 | P2 | `zsxq.iocoder.cn` 与 `www.iocoder.cn` 映射到同一目标 `www.zszj.example.com`，使租户 1（众墅之家）与租户 121（小租户）持有相同 websites 分词：`getTenantByWebsite()` 取首条且无 ORDER BY、`validTenantWebsiteDuplicate()` 拒绝租户 121 后续更新 | 映射目标改为独立占位域 `zsxq.zszj.example.com`，三处同步（`naming-rules.mjs` + 种子文件租户 121 行 + 升级脚本 REPLACE 链），并补一条幂等定点还原语句 |

`PRESERVED` 另含两条自查发现的同类缺口（codex r0 未列出，其范围限于 sql scope）：Flyway `db/migration/V*__*.sql`
（受 `validate-on-migrate=true` 校验和保护）、codegen `README.md`（命名门禁说明文档，故意字面引用旧名作为禁用清单，
重写会把「不得出现 yudao」反转成「不得出现 zszj」）。

### 编号对照说明

| 缺陷 | ZS-BRAND-004.A 评审文档内编号（本文与代码注释采用） | [README.md](README.md) 汇总表内编号 |
|---|---|---|
| 迁移工具重写冻结升级脚本 | P2-1 | 本表 P2 #4 |
| zsxq/www 域名映射同目标 | P2-2 | 本表 P2 #5 |

代码注释一律引用**来源评审文档**的编号（左列）；README 内一律写作「本表 P2 #n」。

## Codex 评审迭代过程

### 第 1 轮（`27b5f459`，r0）— 发现 4 项（3 × P2 + 1 × P3）

codex 对 r0 提交的复审确认 P2-1/P2-2 主体修复成立、G2b 单测通过，但发现实现层 4 处缺口。
其中 C-r0-1 直接暴露了 r0「P2-2 已闭合」结论的**假阴性**：

| # | 优先级 | 发现 | 位置 | 根因 |
|---|---|---|---|---|
| C-r0-1 | P2 | G2b 租户同域门禁正则 `insert into "?system_tenant"?` 只接受双引号/裸写；MySQL 种子以**反引号**包裹表名（`` `system_tenant` ``）故被整体跳过，租户 1/121 同持 `www.zszj.example.com` 的真实同域逃过门禁 | `apply-naming-migration.test.mjs:131-133`、`sql/mysql/ruoyi-vue-pro.sql:6696` | r0 只改了 7 方言 + demo-data-optional 共 8 个种子，恰好漏掉第 8 个方言 MySQL——因为门禁从未扫到它 |
| C-r0-2 | P2 | 历史修复用 `LIKE '%www.zszj.example.com%'` 子串判定 + `REPLACE(websites, ...)` 子串替换，会把 `www.zszj.example.com:3000` 等**端口变体**误判为冲突并一并改写，破坏运维手工配置的既有路由 | `upgrades/20260908_brand_rename_zszj.sql:38-46` | websites 是逗号分隔列表，`selectListByWebsite` 以整词匹配（`POSITION(',v,' IN ','||col||',')`），子串语义与之不一致 |
| C-r0-3 | P2 | 定点还原只校验**源**冲突（租户 1 持 www.zszj），未校验**目标**归属；若另一未删除租户已持有 `zsxq.zszj.example.com`，还原会把该域再赋给租户 121，制造新的同域冲突 | `upgrades/20260908_brand_rename_zszj.sql:42-46` | 守卫条件不完整，只覆盖冲突来源、未覆盖冲突去向 |
| C-r0-4 | P3 | `planScope(scope, cwd)` 用 `readFileSync(cwd + file)` 拼接；默认 `root` 恰好以分隔符结尾掩盖了缺陷，传入 `process.cwd()` 等无尾分隔符的常规目录路径会拼出 ENOENT 路径 | `apply-naming-migration.mjs:118` | 字符串拼接依赖尾分隔符，未用 `path.join` |

### 第 2 轮（`35a04d78`，r1 修正）— ✅ 通过（0 发现）

r1 修正内容（逐项核对 `TenantServiceImpl` / `DbTypeEnum` 源码而非仅采信评审结论）：

- **C-r0-1**：门禁正则改为 `` insert\s+into\s+[`"]?system_tenant[`"]?\s ``，接受反引号/双引号/裸写；
  数据侧把 MySQL 种子租户 121 的 `www.zszj.example.com` 改为 `zsxq.zszj.example.com`（上游基线该行为
  `zsxq.iocoder.cn`，正确映射目标即 `zsxq.*`，与其余 8 处种子对齐）；新增**方言覆盖断言**，显式要求
  MySQL 与 PostgreSQL 主种子都进入逐行核对，杜绝「少扫一个方言」的静默漏检。
- **C-r0-2 / C-r0-3**：把 1.1 节从单条 `UPDATE ... REPLACE` 重写为 `DO` 块——
  ① 按整词 `POSITION(',www.zszj.example.com,' IN ',' || websites || ',')` 判定源冲突（排除端口变体）；
  ② 赋值前检查目标域 `zsxq.zszj.example.com` 跨未删除租户的整词归属，被占用则 `RAISE EXCEPTION`
  使事务整体回滚并给出可操作信息，绝不写入新重复；③ 仅重写整词相等的分词
  （`unnest(string_to_array(websites, ',')) WITH ORDINALITY` + `string_agg(... ORDER BY ord)`），
  保留其余分词与原顺序。幂等：还原后租户 121 不再持有该整词，源冲突判定为假即 `RETURN`。
- **C-r0-4**：改用 `path.join(cwd, file)`（写盘处一并对齐），并新增回归用例——以去尾分隔符的 `root`
  调用 `planScope('sql')`，断言与默认结果一致（修复前该调用抛 ENOENT）。

codex 复审结论：

> "No actionable regressions were found. All 24 naming and migration-tool tests passed. Live PostgreSQL validation was unavailable because Docker access was denied."

原始日志 `[P1]/[P2]/[P3]` 标记数：**0**。

> **PG 实测限制**：codex 与本环境均无 Docker，未能对 `DO` 块做真实 PostgreSQL 执行验证。已代之以
> ① 逐句核对 PL/pgSQL 语法（`SELECT ... INTO` / `RETURN` / `RAISE EXCEPTION` / `unnest(...) WITH ORDINALITY`
> 相关子查询均为标准惯用法）；② 整词匹配谓词直接复用应用侧 `DbTypeEnum.POSTGRE_SQL` 的 `POSITION` 模板，
> 与 `selectListByWebsite` 语义严格一致；③ G8 Flyway 迁移规范门禁通过。真实库执行归 ZS-DB-003 的 B02 验收。

## 验证证据

| 验证项 | 方法 | 结果 |
|---|---|---|
| G2b 迁移工具单测 | `node --test scripts/brand/apply-naming-migration.test.mjs` | **9/9 通过**（r0 为 8 项，r1 新增 P3 自定义根回归用例） |
| 命名门禁单测 | `node --test scripts/brand/verify-brand-naming.test.mjs` | **15/15 通过** |
| 本地基线门禁 | `node scripts/ops/run-local-gates.mjs` | **11/11 PASS**（含 G3 全仓扫描 0 违规、G8 Flyway 迁移规范） |
| 方言覆盖 | 修正后 G2b 逐行核对全部 `*.sql` 的 `system_tenant` 插入 | 9 个方言种子（含 MySQL）租户 1/121/122 域名两两不同、无 `iocoder.cn` 残留 |
| codex r1 复审 | `codex review --commit 35a04d78` | 0 发现（"No actionable regressions were found"） |

## 复核与处置

| 项 | 状态 | 说明 |
|---|---|---|
| P2-1 迁移工具重写冻结脚本 | ✅ 已修复 | `27b5f459`；`PRESERVED` 跨 scope 排除，选文件阶段生效，3 条规则均真实命中且被保护文件确有改写载荷 |
| P2-2 zsxq/www 域名同目标 | ✅ 已修复 | `27b5f459` 映射目标 + 8 处种子 + 升级脚本；`35a04d78` 补齐漏改的 MySQL 种子（第 9 处） |
| C-r0-1 MySQL 种子被门禁跳过 | ✅ 已修复 | `35a04d78`；正则接受反引号 + 数据订正 + 方言覆盖断言 |
| C-r0-2 LIKE/REPLACE 误伤端口变体 | ✅ 已修复 | `35a04d78`；改整词判定 + 仅重写整词相等分词 |
| C-r0-3 未校验目标域归属 | ✅ 已修复 | `35a04d78`；赋值前查目标归属，占用则 RAISE 回滚 |
| C-r0-4 planScope 无尾分隔符根 ENOENT | ✅ 已修复 | `35a04d78`；`path.join` + 回归用例 |
| DO 块真实 PG 执行验证 | ⏳ 待条件 | 本环境无 Docker；归 ZS-DB-003 B02 真实库验收（空库执行 / 旧库 baseline / 重跑幂等） |

## 经验登记

1. **门禁的扫描范围本身也需要被门禁**：C-r0-1 的根因不是「映射写错」，而是「验证映射的门禁少扫了一个方言」。
   断言「全部 SQL 种子无同域」的用例，因正则不接受反引号而**从未看过 MySQL**，于是 `checked>=8` 恒真、
   结论假阴性。凡声称「全量」的断言，必须同时断言**代表性样本确实进入了核对路径**（此处显式断言
   MySQL/PG 主种子都在 `checkedFiles` 中），否则「全量」只是措辞。
2. **同形数据要跨方言对齐核查**：9 个方言种子结构同形，改一处须核对全部。r0 数「7 方言 + demo」为 8 处即漏 MySQL，
   正因缺一条「方言清单」的显式覆盖断言。
3. **子串语义 ≠ 分词语义**：websites 是逗号分隔列表，应用侧以 `POSITION(',v,' IN ','||col||',')` 整词匹配。
   迁移脚本用 `LIKE '%v%'` / `REPLACE(str,...)` 的子串语义会误伤 `v:端口` 变体。凡操作被应用按整词消费的
   列表字段，脚本必须复用应用侧同一套整词谓词，而非就近取 `LIKE`。
4. **修复冲突要同时守住来源与去向**：把 A 的重复值改成 B 之前，必须先确认 B 未被他人占用（C-r0-3）。
   只校验源冲突的修复，会在「目标已被占」的库上制造新的同域。不可两全时（源冲突真、目标被占）应
   `RAISE` 让事务回滚、交人工裁决，而非静默写入任一结果。
5. **字符串拼接路径依赖尾分隔符是隐藏缺陷**：`cwd + file` 在默认根恰好以分隔符结尾时可用，掩盖了对
   `process.cwd()` 等常规路径的破坏。导出供外部传参的函数一律用 `path.join`，并以「去掉尾分隔符的等价根」
   写回归用例，才能把这类被默认值掩盖的缺陷固化为可捕获的断言。
