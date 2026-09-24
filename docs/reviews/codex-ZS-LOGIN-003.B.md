# ZS-LOGIN-003.B 评审处置（安全根因修复：CodeReview 独立复审 PASS / 0×P0/P1）

- 评审对象：ZS-LOGIN-003.B 登录签发与用户级撤销串行化根因修复——闭合幻影写逃逸窗口（B07；§16.1 子项，前置 ZS-LOGIN-003 父卡撤销路径/用户行锁已交付 + ZS-IAM-004 codex r1 P2 移交）
- 隔离分支：worktree `.wt/zszj-wt-login-003-b` 分支 `feat/login-003-b`（impl `5e1d1c77`，4 files +328）
- 合并：`--no-ff` main `a1d2737a`（feat 分支自 `e6035a1e`〔ZS-PERM-004.B 文档同步〕线性领先，干净合并）
- 卡片性质：**产线安全根因修复卡**——在唯一签发咽喉点 `createAccessToken` 纳入共享用户行锁协议，属真实产线代码变更（区别于 PERM-004.B 验证优先卡），走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**CodeReview 独立复审 PASS（0×P0 / 0×P1）**，仅 2 项低优先级 CONSIDER（登录日志原子性为既有非本卡引入、并发测试超时护栏 precaution），不构成合并阻断
- 原始日志：`outputs/_login003b-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因（ZS-IAM-004 codex r1 P2 移交）

唯一签发咽喉点 `OAuth2TokenServiceImpl#createAccessToken`（登录成功签发、implicit、授权码兑换、password、client_credentials 五路调用方全部汇聚于此）虽有 `@Transactional`，却**既不取共享用户行锁**（`selectByIdForUpdate(userId)`）**也不在锁内重读账号状态**；而用户级撤销 `removeAccessToken(userId, userType)`（`doRemoveAccessTokenByUser`，line ~528）与授权码兑换 `grantAuthorizationCodeForAccessToken`（`OAuth2GrantServiceImpl`，line ~96）均以「用户行锁」为最外层锁。于是签发与撤销**不互斥**：快速 suspend（禁用 + 撤销）先提交后，晚到的登录签发仍会为**已禁用**账号建出一条撤销扫描从未看见的幽灵凭据（幻影写逃逸），凭内嵌上下文重新匹配而被接受。

## 交付内容（impl `5e1d1c77`）

**产线修复**（`OAuth2TokenServiceImpl#createAccessToken`，+17 行）：起始处（事务内、锁序最外层）对 ADMIN 且 `userId > 0` 的真实账号取 `selectByIdForUpdate(userId)` 用户行锁并锁内重读状态，账号不存在 / 已禁用即抛 `USER_NOT_EXISTS`，不签发任何凭据。锁序与撤销、兑换一致（**用户行锁 → … → 令牌**），使签发与用户级撤销串行化：撤销先提交则签发被拒，签发先获锁则撤销在获锁后重读并删除新令牌——两种交错都不产可用幽灵凭据。`client_credentials` 机器令牌（`userId = 0`）与 MEMBER 沿既有边界不加锁。补 import `CommonStatusEnum` + static import `USER_NOT_EXISTS`。

**新增测试** `OAuth2TokenServiceImplIssuanceRevocationSerializationTest`（真实 token 服务 + 真实 H2 行锁，`BaseDbAndRedisUnitTest`，满足 §16.1「用真实 token 服务〔非仅 mock〕测该交错」），4 用例：

| 用例 | 类型 | 断言 |
|---|---|---|
| `testCreateAccessToken_suspendCommittedBeforeIssuance_rejectsAndNoGhostToken` | 确定性锚 | 撤销先提交 → 晚到签发抛 `USER_NOT_EXISTS` 且 DB 无幽灵令牌行 |
| `testCreateAccessToken_userRowAbsent_rejects` | 确定性锚 | 账号行缺失 → 拒绝签发 |
| `testCreateAccessToken_enabledUser_stillIssues` | 正向护栏 | 启用账号正常签发不被误伤 |
| `testConcurrentSuspendAndLogin_noUsableGhostCredential` | 真实双线程竞争 | 20 轮 suspend×login 交错，任何交错都无可用幽灵凭据 |

**既有测试契约同步**：`OAuth2TokenServiceImplTest#testCreateAccessToken` 与 `OAuth2TokenServiceImplCacheConsistencyTest`（×2）此前 mock `adminUserService.getUser` 但未种真实用户行，新契约下 `selectByIdForUpdate` 需真实启用态 ADMIN 行 → 为其补种真实启用账号行（保留各测试原意）。

## RED → GREEN

- **RED**（修复前）：2 确定性锚失败——`suspendCommittedBeforeIssuance_rejectsAndNoGhostToken`（Expected ServiceException, nothing thrown，为已禁用账号产出幽灵凭据）、`userRowAbsent_rejects`（Expected ServiceException, nothing thrown）。
- **GREEN**（修复后）：4/4 通过；token 相关 5 测试类隔离复跑 54/54 全绿（含授权码兑换路径的可重入锁、登录路径）。
- 全模块共享 JVM 跑批的 8 项基线失败经 clean `main` 对照确认为既有测试顺序污染（门禁 G11/G12 走 curated `-Dtest=` 子集，非全量共享 JVM），与本卡契约变更无关；本卡引入的 3 项新失败已随既有测试补种真实用户行修复。

## 验证（交付时点证据）

- 定向单测：`OAuth2TokenServiceImplIssuanceRevocationSerializationTest` 4/4 + token 相关 5 类隔离 54/54 BUILD SUCCESS；
- CodeReview 独立复审 PASS（0×P0/P1）；
- 环境坑：本地 `.m2` 框架 jar 陈旧（缺 PERM-002.B 的 `OrgDataPermissionRespDTO`），须先 `install` 上游模块（`-am`）再跑目标测试。

## 卡片状态与统计

主卡 ZS-LOGIN-003 状态维持**已验收**（父卡技术闭环早已验收；.B 为 ZS-IAM-004 codex r1 P2 移交的登录签发层根因加固子项，属既有跨切面竞态的收敛，不改变父卡验收结论）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- 越界授权复活防护已由 ZS-IAM-002 `checkAccessToken` 每请求 `organizationContextMatches`（orgId/orgType/membershipId 三维）fail-closed 拦截 401 兜底；本卡闭合的是「幽灵凭据产出」窗口，与之互补；
- MEMBER 账号状态校验归会员模块，沿既有边界不在本闭环；
- 登录日志写入原子性（CodeReview CONSIDER 项）为既有非本卡引入，归操作日志批次；
- 真实 PG 双物理连接并发交错联验归 ZS-SYS-001 真实 PG 并发扩展（reviews/README 移交登记 `46fd2370` M2）。
