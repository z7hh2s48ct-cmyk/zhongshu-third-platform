# 评审交接：ZS-PERM-002.A（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-PERM-002.A.md`（人工复核 + codex 原始结论）与 `codex-ZS-PERM-002.A.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-PERM-002.A（05 文档 16.1 节，B03 批次；前置 ZS-DB-018、ZS-SEC-012.A 均已完成） |
| 交付 | 现有用户/部门对象的技术授权矩阵 + 通用对象级数据范围检查入口（SQL 静默过滤的显式拒绝补充） |
| 待评审提交 | `1644070c` |
| 评审命令 | `codex review --commit 1644070c` |
| 变更规模 | 6 files changed, 501 insertions(+), 2 deletions(-) |
| 本地验证（后端） | 数据权限 starter 全模块 Tests run: 56, Failures: 0, Errors: 0, Skipped: 0（43 既有含 DeptDataPermissionRuleTest 8 + 新增 13），BUILD SUCCESS |
| 本地验证（门禁） | `node scripts/ops/run-local-gates.mjs --fast` 10/10；`node scripts/gov/verify-docs.mjs` issueCount 0 |
| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-spring-boot-starter-biz-data-permission test`（PowerShell 下按 tools/env.sh 复制 `$env:JAVA_HOME` 与 `$env:PATH`） |
| 解锁 | 本子项退出后，以 ZS-PERM-002.A 为前置的 ZS-FILE-001.A、ZS-MSG-003.A 获得开工条件；ZS-PERM-002.B（业务组织授权矩阵）须 D-09 + ZS-IAM-002 |

## 2. 变更文件清单（仅这 6 个，未含其他窗口改动）

框架主代码（`zszj-spring-boot-starter-biz-data-permission/src/main/`）：
- `java/cn/zszj/framework/datapermission/core/rule/dept/DeptDataPermissionChecker.java`（**新增**，163 行：isObjectVisible/isDeptVisible 只读判定 + checkObjectVisible 单对象拒绝 + checkBatchVisible 批量混入整批拒绝 + 私有 getDeptDataPermission 复用 LoginUser 缓存）
- `java/cn/zszj/framework/datapermission/config/ZszjDeptDataPermissionAutoConfiguration.java`（**+12**：新增 `deptDataPermissionChecker(PermissionCommonApi)` @Bean，与既有 `deptDataPermissionRule` 同条件装配）

框架测试（`zszj-spring-boot-starter-biz-data-permission/src/test/`）：
- `java/cn/zszj/framework/datapermission/core/rule/dept/DeptDataPermissionCheckerTest.java`（**新增**，234 行，13 用例）

文档：
- `services/zhongshu-core/docs/数据权限授权矩阵.md`（**新增**，86 行：现状机制/多角色并集合并/已启用表列矩阵/通用检查入口/访问路径覆盖/验收对齐/边界）
- `docs/05-底座模块分析与开发任务清单.md`（V1.9 变更记录 + ZS-PERM-002 卡追加 .A 开发记录 + 第 19 节 V1.9 行；主卡状态与第 2 节统计**未变**）
- `README.md`（docs/05 索引版本 V1.8→V1.9）

> ⚠️ 工作树中 `docs/reviews/codex-hotfix-*.raw.md`、`outputs/` 等（`??`）**不属于本提交**，为左窗口 codex 评审产物，评审时勿纳入 `1644070c` 范围。

## 3. 请重点核查的判断点

1. **语义一致性（核心）**：`DeptDataPermissionChecker.isObjectVisible` 的判定是否与
   [DeptDataPermissionRule.getExpression](../../services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-data-permission/src/main/java/cn/zszj/framework/datapermission/core/rule/dept/DeptDataPermissionRule.java)
   的 ALL / `deptIds` 命中 / self 命中三分支**完全一致**（含「无部门又不可看本人=100% 无权限」）；
   两者共享同一 `CONTEXT_KEY` 的 LoginUser 上下文缓存，是否会导致范围串用或脏读（应为同一 userId 同一请求内复用同一 DTO，语义一致）。

2. **护栏是否留下绕过面**：`loginUser==null` 与非 ADMIN（MEMBER）返回 `visible=true`，是否与 rule 的
   「无登录用户/非 ADMIN 返回 null（不追加条件）」一致，且仅发生于**受信任系统上下文**（系统内部/租户供给/定时任务）；
   Web 请求恒有登录用户（同 ZS-PERM-001.A 判断点 3），攻击者能否构造无登录态或 MEMBER 态规避对象授权；
   `getDeptDataPermission` 返回 null 时 **fail-closed 抛 FORBIDDEN** 是否正确（不放行）。

3. **批量混入拒绝的正确性**：`checkBatchVisible` 是否对**任一**越权项**整批**抛 FORBIDDEN（而非静默丢弃/部分通过），
   践行「批量混入拒绝」；空集合是否安全通过；`deptIdGetter`/`ownerUserIdGetter` 传 null 时降级为「该维度不命中」是否正确、
   不会 NPE。

4. **与既有机制的分工（不冲突/不重复）**：checker（显式拒绝）与 rule（SQL 静默过滤）是否互补；
   `@DataPermission(enable=false)`/`DataPermissionUtils.executeIgnore` 关闭的是 **SQL 过滤**，checker 独立取
   `getDeptDataPermission` 故**不受其影响**（矩阵第 6.4 点声明）——请确认此声明成立；跨租户 `tenant_id` 轴是否
   正确复用 ZS-DB-018 而未在本项重复编写 SQL 回归。

5. **矩阵文档准确性**：`数据权限授权矩阵.md` 中 system_users（dept_id/id）、system_dept（id）的表列注册是否与
   [DataPermissionConfiguration](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/framework/datapermission/config/DataPermissionConfiguration.java)
   一致；多角色**并集**合并语义是否与 `PermissionServiceImpl#getDeptDataPermission` 一致；「未注册即未适配、须显式接入
   或按缺陷处理」是否会被误读为「未注册=全量放行」。

6. **夹具断言有效性**：13 用例是否用 `assertServiceException(..., FORBIDDEN)` 校验 code(403)+msg（而非仅「抛异常」）；
   缓存复用用例是否以 `verify(permissionApi, times(1))` 确证 `getDeptDataPermission` 仅计算一次；护栏两例是否
   `verify(..., times(0))` 确证未触发权限获取；批量混入用例是否确为「一在范围 + 一越权」的整批拒绝。

7. **装配影响面**：新增 `deptDataPermissionChecker` @Bean 是否影响既有上下文加载——所在
   `ZszjDeptDataPermissionAutoConfiguration` 的 `@ConditionalOnClass(LoginUser)`/`@ConditionalOnBean(DeptDataPermissionRuleCustomizer)`
   未变，checker 仅依赖 `PermissionCommonApi`（与 rule 同一依赖），应不引入新条件或循环依赖；请确认全模块 56/56 无回归。

## 4. 已知边界（非缺陷，属分批范围）

- 本子项（.A）只覆盖**同一技术租户内**的 dept/self 数据范围轴；跨租户隔离（tenant_id 轴）复用 ZS-DB-018 数据层回归，不重写。
- 检查器为**通用入口**：对具体业务表/访问路径的逐一接入（哪些 Controller/Service 调用 `checkObjectVisible`/`checkBatchVisible`）
  随各领域模块开发落地。**本子项未改动任何业务 Controller/Service 去调用它**，仅交付入口 + 现有 user/dept 对象矩阵 + 夹具。
- 业务组织 SELF/ASSIGNED 等**跨组织**授权范围、两组织互授/越界判定与**正向业务授权矩阵**归 **ZS-PERM-002.B**
  （B08，前置 ZS-PERM-002.A + ZS-IAM-002，须 D-09）。
- 真实 PG + HTTP 端到端对象授权联验随 **ZS-SEC-012.B / ZS-SYS-001.A** 在 B03 收口；本子项以框架单测证明判定/拒绝/护栏/缓存语义。
- ZS-PERM-002 **主卡状态维持「待开发」**（沿用 ZS-PERM-001.A 惯例：分批子项 .A 交付、主卡待全部适用子项收口后再变更），
  第 2 节统计不变。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（数据权限 starter 56/56 BUILD SUCCESS + 门禁 10/10 + verify-docs 0）+ 提交（`1644070c`）+ 回填 05 文档开发记录与 V1.9 变更记录
- [ ] 左窗口：`codex review --commit 1644070c` → 产出 `codex-ZS-PERM-002.A.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复

## 6. 处置（左窗口回填）

> 待 codex 评审结论。P0/P1 缺陷在此登记处置方式与复验提交；无缺陷则记「r0 直接通过」。
