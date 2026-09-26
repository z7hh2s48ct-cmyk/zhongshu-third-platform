# ZS-PERM-003.A 评审处置（机制交付卡：两轮 CodeReview 独立复审 R1 PASS + 4×P3 → 处置 → R2 PASS with notes + 1×P3 随 amend 纳入）

- 评审对象：ZS-PERM-003.A 统一动作与字段授权输出机制与扩展点（B08 Wave3 收官链前置卡；§16.1 子项，父卡 ZS-PERM-003；用户拍板「拆 .A 机制先行」；前置 ZS-PERM-002.B〔org 轴对象检查入口〕+ ZS-SEC-001.B〔跨组织 visit 上下文〕+ ZS-IAM-002〔服务端组织上下文〕均已交付）
- 隔离分支：worktree `.wt/zszj-wt-perm-003-a` 分支 `feat/perm-003-a`（自 main `ad0a5024`；impl `2207f284`〔7 files +872/-1〕→ CodeReview P3 处置 `82d2122d`〔4 files +97/-14〕）
- 合并：`--no-ff` main `9b16b7f6`（净 7 files +955/-1；feat 分支自 `ad0a5024` 线性领先，merge-base `ad0a5024` 干净合并）
- 卡片性质：**产线机制交付卡**——交付统一「动作 / 字段」授权输出与执行共用机制与扩展点（allowedActions / authorized_fields 合同 + 服务端裁决共用，默认空 = 零变化，不定义商业字段等级；字段等级目录 F0～F3 归 .B）
- 评审工具：codex 账号用量上限阻断（承 PERM-002.B/PERM-004.B/LOGIN-003.B/SEC-001.B/PERM-001.B/FILE-001.B/MSG-003.C/CLIENT-002.B/BPM-002/BPM-004/PERM-004.C 先例），改以 **CodeReview 独立复审替代**（R1 → 处置 → R2 两轮）
- 结论：**R1 PASS（0×P0/P1/P2 + 4×P3）→ P3-1/2/3 修复 + P3-4 登记不改 `82d2122d` → R2 增量复审 PASS with notes（0×P0/P1/P2 + 1×P3〔javadoc 口径未同步〕）随 amend 纳入 `82d2122d`**（评审门实质闭环）
- 原始日志：`outputs/_perm003a-*.log`（本地未入库，gitignore；本档案为处置入库）

## 交付内容（impl `2207f284` + P3 处置 `82d2122d`）

**裁决四件套**（框架层新包 `cn.zszj.framework.datapermission.core.authorize`，biz-data-permission starter）：

- `ObjectAuthorizationRespDTO`（输出合同）：`allowedActions` 恒非 null（空集 = 无任何动作可用）；`authorizedFields` **null = 未启用字段级输出**（provider 未声明候选字段，零变化）、非 null（可为空集）= 已启用——语义区分防「未启用域被误判全隐」；
- `ObjectAuthorizationProvider`（SPI 扩展点）：`getObjectType()` 唯一路由键（非空白且同容器唯一，参装配 fail-fast）+ `getCandidateActions()`/`getCandidateFields()` 候选集（默认空 = 该维不输出/未启用）+ `isActionAllowedInStatus(action, object)` 状态维钩子（默认放行）；
- `ObjectAuthorizationRequest`（输入）：objectType/orgId/ownerUserId/object + 两个静态工厂 `of(...)`；不承载用户身份（登录主体始终来自服务端安全上下文）；
- `ObjectAuthorizationService`（裁决服务）：`authorize`（未注册返回 null = 零变化；对象维 fail-closed〔visit 请求经 `CrossOrgVisitScopeHolder.isObjectAllowed` 收敛、不落 home 组织 checker；非 visit 经 `OrgDataPermissionChecker.isObjectVisible`〕；动作维 = 状态钩子 + `securityFrameworkService.hasAnyPermissions`〔与 @PreAuthorize 同一实现，visit 请求自动经 D6 收敛〕恒返回非 null；字段维候选空 = null、visit 逐字段 `areFieldsAllowed` 收敛、非 visit = 全候选〔.A 零变化〕）+ `checkActionAllowed`/`checkFieldsAllowed`（**复用 authorize 同一裁决核心**；动作空/超集、字段非空超集、未接入或未启用字段级时 fail-closed 统一 FORBIDDEN、不泄露对象/字段存在性）。

**装配**：`ZszjDeptDataPermissionAutoConfiguration` 新增 `objectAuthorizationService` Bean（`List<ObjectAuthorizationProvider>` 注入，空 = 空转；不新增 AutoConfiguration 类/imports/依赖/迁移/脚本）。

**文档**：数据权限授权矩阵 §8 机制登记（合同语义 / 裁决流水线 / 执行入口 / 边界）+ P3 处置同步用例数 23→27。

## CodeReview R1 findings（PASS，0×P0/P1/P2 + 4×P3）处置（`82d2122d`）

| 级别 | 编号 | 缺陷 | 处置（`82d2122d`） |
|---|---|---|---|
| P3 | P3-1 | 字段维候选过滤口径与动作维不一致：动作维过滤 `null/blank`、字段维仅过滤 `null`——空白字段名（`""`/全空白）会成为「有效候选」进入输出 | `resolveAuthorizedFields` 改为 `field == null \|\| field.isBlank()` 对齐动作维口径 |
| P3 | P3-2 | provider 空白 `objectType` 可静默注册：`""` 成为可路由键（`authorize("")` 可达）、路由歧义 | 构造器对 `StrUtil.isBlank(provider.getObjectType())` fail-fast 抛 `IllegalStateException`；新增用例 25 看守 |
| P3 | P3-3 | `checkFieldsAllowed` 分支用例缺口：候选通过路径与「候选外拒绝」路径无用例看守 | 新增用例 24-27（blank 候选过滤 / 空白 objectType fail-fast / unknown-type fail-closed / 非 visit 候选通过与候选外拒绝）；套件 23→27；矩阵 §8 同步 |
| P3 | P3-4 | Bean 定义于配置类内、继承类级 `@ConditionalOnBean(DeptDataPermissionRuleCustomizer.class)`——下游业务模块新增 provider 时其上下文若无该 customizer，整个装配不生效、provider 不被消费（authorize 恒 null 静默零变化） | **登记不改**（接入注意，见下节；语义与整类既有 Bean 一致，单独放宽会扩大装配面，不做） |

## CodeReview R2 findings（PASS with notes，0×P0/P1/P2 + 1×P3）随 amend 纳入（`82d2122d`）

| 级别 | 编号 | 建议（非阻塞） | 闭环（`82d2122d`） |
|---|---|---|---|
| P3 | F1 | SPI（`ObjectAuthorizationProvider`）与 `ObjectAuthorizationService` class javadoc 字段维条目未同步 P3-1 修复后口径（仍描述「过滤 null」旧行为） | javadoc 订正为 `field==null\|\|field.isBlank()` 过滤口径；随 fix 提交 amend 纳入（`git diff --numstat` 坐实 Provider.java 1+/1-、Service.java 变化均为 javadoc 与目标口径行） |

## 接入注意（P3-4 登记不改）

`ObjectAuthorizationService` Bean 继承 `ZszjDeptDataPermissionAutoConfiguration` 类级 `@ConditionalOnBean(value = {DeptDataPermissionRuleCustomizer.class})` 装配条件。下游业务模块新增 `ObjectAuthorizationProvider` 实现时，须确认该模块上下文存在 `DeptDataPermissionRuleCustomizer`（现有 system/infra 等装配上下文均具备），否则 provider 不被消费、`authorize` 恒返回 null——即「未接入 = 零变化」语义静默成立、不报错。集成联验时以此为首查项。

## 验证（交付时点证据）

- **RED 双证**：
  - 编译红：7 errors（找不到符号：`ObjectAuthorizationService` 等）BUILD FAILURE；
  - 断言红：`ObjectAuthorizationServiceTest` 23/23 红（11 Failures + 12 Errors），既有 71/71 保护性绿；
- **GREEN**：本类 **27/27**（P3-3 补强后）+ 模块全量 **98/98**（= 既有 71 + 新增 27）BUILD SUCCESS；starter-security **40/40**；module-system 定向 **55/55**（含 `PermissionServiceTest` 47）；
- `node scripts/db/run-pg-regression.mjs` **19/19**（零迁移零脚本变更）；
- `node scripts/ops/run-local-gates.mjs --fast` **10/10**（worktree 侧；主树合并后复跑同）。

## 卡片状态与统计

主卡 ZS-PERM-003 由**待前置转开发中**（.A 交付解除技术前置；.B 待敏感业务字段目录获批）。第 2 节统计 0/21/2/0/2/66（合计 91，权威来源 `node scripts/gov/task-stats.mjs`）。**本记录不表示任何主任务已验收。**

## 边界（登记）

- **字段等级目录 F0～F3 归 ZS-PERM-003.B**：本卡只交付「候选字段 → 授权字段」的机制通道，不定义任何商业字段等级；敏感业务字段目录获批后配置接入；
- **业务域逐域接入留后**（循「入口先行」）：已有消费端 `NotifyMessageOrgAuthorizer`/`FileServiceImpl` 保持现状不改造；
- **前端消费归 ZS-CLIENT-001.B**（Web/移动端只消费裁决输出）；
- **批量语义 = 调用方逐对象循环**（机制不新增批量 API）；
- **无跨请求缓存**（每次调用实时裁决；性能优化由调用方按需自担）；
- **真实 PG + HTTP 端到端联验随 B08 批次真实环境放行**；
- **P3-4 装配条件接入注意**：见上节（登记不改）。
