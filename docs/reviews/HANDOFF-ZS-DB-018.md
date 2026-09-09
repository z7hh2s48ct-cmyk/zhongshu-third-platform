# 评审交接：ZS-DB-018（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-DB-018.md`（人工复核 + codex 原始结论）与 `codex-ZS-DB-018.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-DB-018（05 文档 M02，B02 技术回归 + B08 业务组织放行；前置 ZS-DB-004 已完成；业务组织语义须 D-09） |
| 交付 | 补齐 ORM、手写 SQL 与租户隔离的**真实 PG 回归**——SQL 级两技术租户隔离用例集（数据层技术租户链证明） |
| 待评审提交 | `c3ae2e8e` |
| 评审命令 | `codex review --commit c3ae2e8e` |
| 变更规模 | 4 files changed, 257 insertions(+), 6 deletions(-) |
| 本地验证 | `run-db018-verify.mjs` 10/10 PASS；`--self-test` 11/11 PASS；`run-pg-regression.mjs` 8 套件全绿；`run-local-gates.mjs --fast` 10/10 PASS（2026-09-09） |
| 复现入口 | `node scripts/db/run-db018-verify.mjs [--self-test]`；聚合 `node scripts/db/run-pg-regression.mjs`（均需 Docker；一次性 PG17 容器验后自动清理） |
| 承接 | `07ff751b`（TwoTenantIsolationTest，H2 内存库 DeptMapper CRUD+逻辑删除；已登记「H2 未装配 TenantLineInnerInterceptor，拦截器级隔离待 B02 真实 PG」）——本提交补齐该真实 PG 数据层缺口 |
| 解锁 | ZS-PERM-002.A（B03）开工前置之一；ZS-DB-019.B 前置链。业务组织/跨组织授权（B08）仍须 D-09 |

## 2. 变更文件清单（仅这 4 个，未含其他窗口改动）

脚本（`scripts/db/`）：
- `run-db018-verify.mjs`（**新增**，245 行）：一次性 Docker PG17 + env-setup-test.sql + V1/V2 迁移；System 技术租户 tenant_id=1/2 双租户夹具；C1~C10 用例 + `--self-test` 负向对照；仿 `run-db006-verify.mjs` 的容器/清理/记录/退出语义
- `run-pg-regression.mjs`（**修改**，+4/-2）：cases 数组新增 DB-018 条目；头部注释补第 6 项并将 DB-016/017 顺延为 7/8

文档：
- `docs/05-底座模块分析与开发任务清单.md`（**修改**）：ZS-DB-018 卡状态 待开发→待验收 + 追加开发记录；第 2 节 V1.8 变更记录 + L45 统计 48/24→47/25；第 19 节 V1.8 行；头部版本 V1.7→V1.8
- `README.md`（**修改**，+1/-1）：docs/05 索引版本 V1.7→V1.8

> ⚠️ 工作树中 `docs/reviews/codex-hotfix-*.raw.md`、`outputs/`（?? 未跟踪）**不属于本提交**，为左窗口评审产物/其他工件，评审时勿纳入 `c3ae2e8e` 范围。

## 3. 请重点核查的判断点（按卡片验收红线）

1. **等效 SQL 是否忠实反映拦截器改写**（核心）：本套件用手写 `AND tenant_id = ctx` 模拟 `TenantLineInnerInterceptor`
   对继承 `TenantBaseDO` 表的注入（拦截器逻辑见 `TenantDatabaseInterceptor.computeIgnoreTable`）。请核查：
   ① SELECT/UPDATE/DELETE/JOIN 的 tenant_id 条件位置与逻辑是否与拦截器实际改写一致；② C3 关联 JOIN **两端**均注入
   是否符合拦截器对多表的处理；③ 逻辑删除 `deleted = 0` 与 MyBatis-Plus logic-delete 注入是否一致；
   ④ 是否遗漏拦截器会改写的语句形态（INSERT 自动填充 tenant_id、子查询、UNION、`@TenantIgnore` 方法级 AOP 的 setIgnore 时序）。

2. **反向用例是否证明"拒绝"而非"空数据"**：C7 伪造他租户对象 ID 读/改/删均 0 行。请核查：
   ① `--self-test` S0 是否有效证明目标行存在（忽略路径可见=1），从而作用域内 0 行是**隔离**而非缺数据；
   ② C3 的 u3（tenant_id=1 但 dept_id 指向 tenant_id=2 的 900103）被 JOIN 排除，是否等价于生产拦截器行为；
   ③ C5/C7 跨租户逻辑删除/物理删除 0 行后，是否断言目标行状态未被篡改（deleted 仍=0、status 仍=0、角色仍存在）。

3. **三类合法范围（全局表/忽略注解/系统清理）边界是否准确**：请核查 C8/C9/C10 与源码一致性：
   ① C8 结构断言（`system_dict_data`/`infra_job_log` 无 tenant_id 列）是否准确对应 `DictDataDO`/`JobLogDO extends BaseDO + @TenantIgnore`；
   ② C9 对 `system_oauth2_access_token`「**有** tenant_id 列（`OAuth2AccessTokenDO extends TenantBaseDO`）但仅 `OAuth2AccessTokenMapper.selectByAccessToken`/`selectByRefreshToken` **方法级** `@TenantIgnore` 显式绕过」的正反路径演示是否成立；
   ③ C10 `TokenCleanJob.execute` `@TenantIgnore` 跨租户有界清理是否与 ZS-DB-014/015 一致，是否遗漏其它 `@TenantIgnore` 清理/维护路径。

4. **与既有夹具的分工是否清晰（无重复/无缺口）**：请核查：① `07ff751b`（H2，无拦截器）与本套件（真实 PG，等效 SQL）
   的分工是否在开发记录如实登记；② SEC-012.A `SecurityFilterChainFixtureTest`（HTTP 层双技术租户 + `TenantSecurityWebFilter`）
   与本套件（数据层）是否互补不重复；③「拦截器经 MyBatis ORM 对真实 PG 的端到端集成断言归 ZS-DB-019.B」的边界登记是否成立
   ——即本套件**确实未执行拦截器本身**，只验证其改写结果的等效 SQL 语义。

5. **夹具隔离/清理/失败非零语义**：请核查：① 容器名/端口随机唯一、进程退出（含 SIGINT）自动清理，对齐 `run-db006`；
   ② Docker 不可用 `exit 3`（不静默跳过）；③ 任一用例失败 → `failCount` → `exit 1`；④ 造数以 `zhongshu_app`（应用连接）执行、
   计数断言以 `postgres` 读取的分工是否合理（隔离语义与角色无关）；⑤ 高位 ID（900xxx）与 V1 种子（id<200）无 PK 冲突。

6. **注册与文档一致性**：请核查：① `run-pg-regression.mjs` cases 新增条目、头部注释与套件数（7→8）一致；② 8 套件本地全绿可复现；
   ③ docs/05 卡片状态、L45 统计 48/24→47/25、第 2 节 V1.8、第 19 节 V1.8、头部版本、README 索引六处一致；④ `verify-docs` issueCount=0。

## 4. 已知边界（非缺陷，属分批范围）

- 本项为 **SQL 级等效语句验证**，证明数据层技术租户链（拦截器改写后的隔离语义）；**非 PG 原生 RLS**，直接 JDBC 不自动继承 ORM 规则（卡片明示为检查边界，非已证实越权复现）。
- **拦截器经 MyBatis ORM 对真实 PG 的端到端集成断言**归 **ZS-DB-019.B**（PG 集成测试入口）；ORM 拦截器 Java 级装配与 HTTP 层普通 tenant-id 与 Token 不一致拒绝由 **ZS-SEC-012.A/001.A** 覆盖，本项复用其反向用例不重复。
- **两业务组织与跨组织服务授权**的完整验收须 **D-09** 决策后于 **B08** 放行；`tenant_id → tenant_org_id` 映射登记为 D-09 后任务，本项**未改名、未建身份表**。
- 此项通过**只证明既有技术租户链**，不等于两个业务组织和跨组织服务授权已通过。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（run-db018 10/10 + `--self-test` 11/11；run-pg-regression 8/8；run-local-gates --fast 10/10）+ 提交（`c3ae2e8e`）+ 回填 05 文档开发记录
- [ ] 左窗口：`codex review --commit c3ae2e8e` → 产出 `codex-ZS-DB-018.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复
