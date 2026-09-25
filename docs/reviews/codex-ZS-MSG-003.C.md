# ZS-MSG-003.C 评审处置（业务组织/任职消息可见范围：CodeReview 独立复审两轮，R2 PASSED / 0×P0/P1）

- 评审对象：ZS-MSG-003.C 业务组织/任职消息可见范围——消息域 org 轴可见范围 + 落点对象门（B08；docs/05 §16.1 line1047 子项「任职失权后旧正文与落点受限；D-09 后验收」，前置 ZS-MSG-003.A〔技术收件箱与业务落点接口鉴权〕+ ZS-IAM-002〔组织/任职模型 + 服务端组织上下文〕+ ZS-PERM-002.B〔org 轴范围解析/对象检查入口 OrgDataPermissionChecker〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-msg-003-c` 分支 `feat/msg-003-c`（自 main `0e7cd995`；feat `26a773e5` + test `c08cfe65`）
- 合并：`--no-ff` main `0430a01a`（11 files +864/-26，feat 分支自 `0e7cd995` 线性领先、干净合并）
- 卡片性质：**产线消息授权路径落地卡**——消息域首个 org 轴生产消费方（归属载体 + 四读方法显式收敛 + 对象门 + 落点门），属真实产线代码变更，走完整 9 步周期
- 评审工具：codex 账号用量上限阻断（承 ZS-PERM-002.B / ZS-PERM-004.B / ZS-LOGIN-003.B / ZS-SEC-001.B / ZS-PERM-001.B / ZS-FILE-001.B 先例），改以 **CodeReview 独立复审替代**
- 结论：**R1 报 2×P2 + 2×P3 → 全部修复并补回归用例 → R2 独立复审 PASSED（0×P0 / 0×P1；R1 四项全 CLOSED）**
- 原始日志：`outputs/_msg003c-*.log`（本地未入库，gitignore；本档案为处置入库）

## 根因/背景（§16.1 line1047 验收 + 前置移交）

ZS-MSG-003.A（B05）已交付技术收件箱与业务落点二次授权（落点注册 SPI + 归属校验 + `authorize` 重授权硬合同），ZS-IAM-002 交付组织/任职模型与服务端组织上下文，ZS-PERM-002.B 交付 org 轴对象级检查入口（`OrgDataPermissionChecker`）——但消息域仍是**纯本人轴**（userId + userType）：`NotifyMessageDO` 无组织载体，任职失效/转岗后旧消息正文与落点不受 org 范围收敛，同一技术租户内异组织消息互相可见；SEC-001.B D7 登记边界「数据范围收敛 + 各业务路径逐一接入随下游 FILE-001.B/MSG-003.C/CLIENT-002.B」的消息域部分待落。§16.1 line1047 验收：「任职失权后旧正文与落点受限；D-09 后验收」。

## 交付内容（feat `26a773e5`）

**module-system（消息域 org 轴落地）**：
- 数据载体：`NotifyMessageDO.organizationId`（org 轴载体，正交于 tenant 轴）+ 迁移 `V20260925.001__system_notify_message_organization.sql`（int8 + idx_system_notify_message_02）+ H2 测试 schema（`create_tables.sql`）同步；组织归属 = **派发时刻收件人的服务端组织上下文**；**仅 ADMIN 命名空间参与任职解析**（`resolveRecipientOrganizationId`——MEMBER 编号空间独立可同号，防同号错配，归属恒 NULL）；无任职/已失效/系统上下文为 NULL（仍由 tenant 轴 + 本人轴治理）；
- 读路径收敛：`NotifyMessageMapper` 新增 `OrgScope` record（`unrestricted()` / `of(visibleOrgIds, includeNullOrg)` 工厂）+ 双版本 `applyOrgScope`（`LambdaQueryWrapperX` + `QueryWrapperX`）——四读方法（管理分页 / 我的分页 / 未读列表 / 未读数）统一收敛：visibleOrgIds=null 不受限、空集且不含 NULL 恒假（`1=0` fail-closed）、空集含 NULL 仅 `isNull`、非空 `in` 或 `in-or-isNull`；
- `NotifyMessageServiceImpl#resolveOrgFilter` 顺序 = visit 快照优先 → 无登录/非 ADMIN 护栏 → 门面未装配 fail-closed → ORG_ALL 放行 → orgIds 集合；
- 对象门（新组件 `NotifyMessageOrgAuthorizer`）：获批 visit 优先（`CrossOrgVisitScopeHolder.isObjectAllowed` 对象维收敛，不叠加 home 组织范围）→ `OrgDataPermissionChecker.isObjectVisible`（无组织列消息本人兜底；对象一旦归属组织，本人所有权不凌驾组织排除——D-09 FND-AUTH-004）；checker 未装配 fail-closed（仅 NULL 归属可读）；`getNotifyMessage` 正文按 `NOT_EXISTS` 返回（不泄露存在性）；
- 落点门：`NotifyLandingServiceImpl` 落点解析 org 门（归属校验后、注册判定前）——越界判 REVOKED（不泄露未注册/模块关闭信息）；
- 写路径（标记已读）不施 org 门：docs/05 line709 既定口径（SQL 已按本人 + userType 约束，javadoc 登记）。

## 新增/改测试（test `c08cfe65`）

| 测试 | 模块 | 用例 | 断言 |
|---|---|---|---|
| `NotifyMessageOrgVisibilityTest`（新，15 例） | system | org 轴 + visit + 护栏（真实 H2 + 真实 Authorizer/Service） | A 写入归属 4（有效任职落组织归属/无任职 NULL/离职 NULL/MEMBER 同号收件人恒 NULL 防错配）；B 我的读 2（授权组织内可见 + 归属 NULL 本人兜底 / 离职即时收缩）；C 管理读 1（管理分页过滤 + 单条正文门；异组织/null 归属对无任职操作员 fail-closed 恒空）；D visit 5（限定组织命中放行/越界拒绝/whole-tenant 放行/落点 within 可用/落点越界 REVOKED）；E 护栏 3（无登录不施加/非 ADMIN 不施加/无任职管理分页 fail-closed 恒空不抛异常） |
| `NotifyMessageServiceImplTest`（基线适配） | system | 既有用例 | `@Import` 补 `NotifyMessageOrgAuthorizer` + `randomPojo` 显式 `.setOrganizationId(null)` 保持既有基线 |
| `NotifyTodoServiceTest` / `NotifyLandingServiceTest`（基线适配） | system | 既有用例 | 同上（`@Import` 补 Authorizer；NotifyLanding 2 处显式置空） |
| `create_tables.sql`（+1 列） | system | H2 schema | `organization_id` 列同步 |

## RED → GREEN

- **RED**（实现前）：org 轴用例编译失败（`organizationId` / `OrgScope` / `NotifyMessageOrgAuthorizer` 未定义 = 预期缺失符号）；
- **GREEN**（实现后）：targeted 隔离 **62/62 绿**；module-system 全量 **969 绿**（Failures 0 / Errors 0）BUILD SUCCESS；
- **flake 归因**：全量首跑命中 2 个 main 基线既有 flake——`SecurityChainJointRegressionTest$AsyncFullContract.streamingEndpointWithTokenStreamsFully`（MockHttpServletResponse 并发写头 CME）+ `SmsCodeServiceImplAttemptLimitTest` 3 用例（IP 短信限流 Redis 状态残留；单跑 12/12 全绿），单跑/基线对照证实非本卡回归。

## CodeReview 独立复审发现（R1 → 修复 → R2 PASSED，0×P0/P1）

| # | 严重度 | 位置 | 发现 | 处置 |
|---|---|---|---|---|
| P2-1 | **P2** | `NotifyMessageServiceImpl#resolveRecipientOrganizationId` | MEMBER 收件人若参与任职解析会与 ADMIN 编号空间同号错配（两编号空间独立、ID 可重复，按 userId 查任职会命中他人任职记录而错误归属） | **修复**：仅 ADMIN 命名空间参与任职解析、MEMBER 归属恒 NULL + A 组 MEMBER 同号恒 NULL 看守用例 |
| P2-2 | **P2** | 迁移 `V20260925.001` 注释 | 迁移注释口径与实现语义矛盾 | **修复**：订正注释 |
| P3-1 | P3 | 写路径（标记已读） | 未登记「不施 org 门」的口径依据 | **修复**：javadoc 登记 docs/05 line709 既定口径 |
| P3-2 | P3 | `applyOrgScope` 空集分支 | `1=0` fail-closed 分支缺测试看守 | **修复**：补 E 组 fail-closed 用例（无任职管理分页恒空不抛异常） |

R2 复审确认 R1 四项全 CLOSED，**PASSED / 0×P0 / 0×P1**。

## 验证（交付时点证据）

- targeted 隔离单测：**62/62** BUILD SUCCESS；
- module-system 全量：**969 绿**（Failures 0 / Errors 0），2 个 main 基线既有 flake 单跑/基线对照证实非本卡回归；
- 两轮 CodeReview 独立复审替代（R2 0×P0/P1）。

## 卡片状态与统计

主卡 ZS-MSG-003 维持**开发中**（.A 技术收件箱 + .B 两端落点 + .C 业务组织/任职可见范围均交付；循子项拆分卡先例 SEC-001/SEC-011/DB-019/PERM-001/PERM-002/PERM-004/FILE-001「.A+.B(+.C) 均交付、待验收须真实环境放行」+ §16.1「不能把技术夹具算业务策略完成」，须 B08 批次真实环境放行 + D-09 后验收才转待验收）。第 2 节统计不变（2/20/3/0/3/63，合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务验收状态变化。**

## 边界（登记）

- **org 轴 SQL 列表自动静默过滤规则 `OrgDataPermissionRule` 注册仍未落地**——本卡交付**显式收敛**（`OrgScope` 双版本 `applyOrgScope` 显式过滤 + 对象门 + 落点门），自动规则注册随 CLIENT-002.B 或后续；
- **写路径（标记已读）不施 org 门**：docs/05 line709 既定口径（SQL 已按本人 + userType 约束，「他人 IDs 不能改变已读状态」由该约束保证）；
- **MEMBER 收件人 org 归属恒 NULL**：MEMBER 与 ADMIN 编号空间独立（防同号错配），MEMBER 对象授权属会员模块另一轴；
- **真实 PG + HTTP 端到端跨组织消息联验**随 B08 批次真实环境放行——本卡以 H2 targeted 套件坐实归属写入 + 读路径收敛 + 对象门 + 落点门语义；
- 消息域接入接线详见 [跨组织访问授权矩阵.md](../../services/zhongshu-core/docs/跨组织访问授权矩阵.md) §5/§8 与 [数据权限授权矩阵.md](../../services/zhongshu-core/docs/数据权限授权矩阵.md) §7.7。
