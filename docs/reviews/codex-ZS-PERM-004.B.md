# ZS-PERM-004.B 评审处置（验证优先卡：CodeReview 独立复审 PASS-with-suggestions P0=0/P1=1/P2=2/P3=3 → 全部闭环）

- 评审对象：ZS-PERM-004.B 组织任职变更后的撤权审计——org 轴授权一致性与审计完整性（B08 Wave2；§16.1 子项，前置 ZS-PERM-004.A + ZS-IAM-004 + ZS-PERM-002.B 均已交付）
- 隔离分支：worktree `.wt/zszj-wt-perm-004-b` 分支 `feat/perm-004-b`（impl `7142defa` → CodeReview 闭环修复 `526c9599`）
- 合并：`--no-ff` main `9887f5d0`（2 files +458/-1；feat 分支自 `90f81155`〔ZS-PERM-002.B 文档同步〕线性领先，merge-base `90f81155` 干净合并）
- 卡片性质：**验证优先卡（verification-first）**——交付一致性验证测试套件 + 授权矩阵登记，**无产线代码变更**；org 轴范围按构造每请求实时重派生（无跨请求缓存），套件首跑即 GREEN，坐实无产线缺陷
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**CodeReview 独立复审 PASS-with-suggestions（P0=0 / P1=1 / P2=2 / P3=3），全部 findings 已闭环修复 `526c9599`**（评审门实质闭环）
- 原始日志：`outputs/_perm004b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 交付内容（impl `7142defa` + CodeReview 闭环 `526c9599`）

新增 `OrgDataPermissionRevocationConsistencyTest`（module-system test，8 用例，`BaseDbAndRedisUnitTest`：真实 H2 + 内嵌 Redis + 真实 Spring Cache/事务），驱动真实 `MembershipService.changeStatus`/`transferMembership` 生命周期流转，经**真实门面** `PermissionCommonApi`（`PermissionApiImpl` → `PermissionService.getOrgDataPermission`，`@EnableCaching` 代理下无 `@Cacheable`）+ `OrgDataPermissionChecker` 断言即时后果，锁定 §16.1 退出条件「任职失效及时拒绝，恢复不自动恢复越界授权」与主卡验收「合法恢复可重新取权；审计含操作者/目标/变更/结果/trace」。

**六维度回归锁**：①任职失效即时收缩 ②次级任职失权独立于会话撤销 ③转岗改授权 ④恢复不复活越界授权（组织停用 fail-closed）⑤无陈旧缓存（经真实门面 + 缓存代理）⑥撤权审计流水完整性。

`数据权限授权矩阵.md` 新增 §7.8：登记本卡撤权一致性验证与审计「结果」口径（D3：会话侧结果 + 生命周期流水 + 每请求实时重派生合成完整审计链，**无需新增审计字段**）。

## CodeReview 独立复审 findings（PASS-with-suggestions，P0=0/P1=1/P2=2/P3=3）全部闭环

| 级别 | 编号 | 缺陷 | 闭环（`526c9599`） |
|---|---|---|---|
| P1 | P1-1 | `newChecker` 以 mock 桥接跳过 `PermissionApiImpl`，org 轴范围断言实际覆盖面 < §7.8 宣称面（门面层未被回归锁覆盖） | 改注入**真实门面** `PermissionCommonApi`（`@Import PermissionApiImpl`），org 轴范围断言全部走 `permissionApi.getOrgDataPermission`（门面→服务→解析器，与产线 org 轴读路径同构），「无跨请求缓存」回归锁覆盖至门面层 |
| P2 | P2-1 | `testSuspendSecondary` 未坐实「停用次级任职不撤会话」半句，方法名与 §7.8 登记口径不自洽 | 补 `verify(oauth2TokenService, never()).removeAccessToken`，坐实次级任职失权独立于会话撤销 |
| P2 | P2-2 | 文档/注释宣称断言 trace，但单测不装配 `@LogRecord` 切面，实际未断言（overclaim） | 收敛 trace 口径——§7.8 与测试注释明确 trace-id 由操作日志层 `LogRecordServiceImpl` 承载，本套件不断言 trace（消除 overclaim） |
| P3 | P3-1 | 审计流水用例以恒真断言 `allMatch` 检验，检出力不足 | 改为动作序列**精确匹配**（CREATE→TRANSFER→SUSPEND→RESUME→TERMINATE） |
| P3 | P3-2 | `testSuspendPrimary` 注释含未驱动的 EXPIRE overclaim | 去除 EXPIRE overclaim |
| P3 | P3-3 | `newRequest` 缓存作用域边界未注明 | 补边界注释（仅锁 LoginUser 作用域缓存失效，线程/请求作用域缓存不在本锁范围） |

## 验证（交付时点证据）

- 模块测试全绿：module-system 相关 **36**（含本套件 8，经真实门面装配）+ biz-data-permission 全量（`OrgDataPermissionCheckerTest` **15**）BUILD SUCCESS；
- `node scripts/ops/run-local-gates.mjs --fast` **10/10**（合并前 feat + 合并后 main 均绿）；
- org 轴范围每请求实时重派生（无跨请求缓存），一致性套件首跑即 GREEN，无产线代码缺陷。

## 卡片状态与统计

主卡 ZS-PERM-004 维持**开发中**（循子项拆分卡先例 ZS-PERM-002/ZS-SEC-011/ZS-DB-019/ZS-MSG-003「子项交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」）。理由：①ZS-PERM-004.A 延后的驱逐失败可靠补偿已登记移交 ZS-PERM-004.C（reviews/README 移交登记 `46fd2370`），尚未交付；②主卡完整验收（撤权后旧 Token/旧菜单/缓存不继续执行受限动作、多节点缓存广播、审计端到端）须 B08 批次真实环境放行。第 2 节统计不变（2/20/3/0/3/63，合计 91）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- 本卡为验证优先卡，交付一致性回归锁 + 授权矩阵登记，无产线代码变更；org 轴 SQL 列表静默过滤规则 `OrgDataPermissionRule` + 业务表 `org_id` 列注册随 B08 下游领域模块接入落地（当前无业务表注册 `org_id` 列，套件以真实门面 + 解析器构造范围）；
- 撤权审计以「会话侧结果 + 生命周期流水 + 每请求实时重派生」合成，无需新增审计字段；trace-id 由操作日志层 `LogRecordServiceImpl` 承载，非本套件断言面；
- 驱逐失败可靠补偿归 ZS-PERM-004.C；真实多节点缓存广播联验归环境批次；字段级授权归 ZS-PERM-003。
