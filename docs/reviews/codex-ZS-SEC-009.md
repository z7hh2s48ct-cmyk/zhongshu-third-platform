# ZS-SEC-009 codex 评审结论与处置

> 被审提交（r0）：`77ad9531`（ZS-SEC-009 固定接口边界 ID/时间/分页/校验四合同，后端；11 files, +647/-9）。
> 返工提交（响应 r0 FAIL）：`d13a50b4`（修 P1-1 时区三端对齐 + ID 合同拆 SEC-009.B；5 files, +62/-14）。
> P2 修复提交（响应 r1）：`1f6bbb1c`（对齐剩余过期生产端到固定时钟；2 files, +3/-4）。
> 复评：r1 `codex review --commit d13a50b4`（PASS / 0 P1 + 1 P2）、r2 `codex review --commit 1f6bbb1c`（PASS / 0 发现）。
> 评审工具：`codex-cli 0.154.0`（OpenAI Codex v0.154.0），模型 `gpt-6-astra`，reasoning effort `xhigh`。
> 完整 stdout：r0 [codex-ZS-SEC-009.raw.md](codex-ZS-SEC-009.raw.md)、r1 [codex-ZS-SEC-009-r1.raw.md](codex-ZS-SEC-009-r1.raw.md)、r2 [codex-ZS-SEC-009-r2.raw.md](codex-ZS-SEC-009-r2.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。
> 分批决策：r0 的破坏性 ID 合同经用户拍板 **Option B「分批交付」**，拆出 **SEC-009.B**（全局 ID→string + 两端前端 ~23 处 ID 数值比较迁移 + `CommonResult<Set<Long>>` 集合覆盖 + OpenAPI/类型同步）；本次收口的是 **SEC-009.A**（时间/分页/校验三合同 + ID 合同脚手架就位但不激活）。

## Codex 原始结论（r0，被审提交 `77ad9531`）

判定 **FAIL**，发现 **2 项 P1 + 1 项 P2**（无 P0 / P3）：

### Review comments

- **[P1] #1 Align timestamp producers with the fixed serialization timezone** — `TimestampLocalDateTimeSerializer.java:62`
  > On a UTC JVM, `OAuth2TokenServiceImpl#createOAuth2AccessToken` still creates expiry timestamps using `LocalDateTime.now()`. Interpreting those values as GMT+8 shifts the actual expiry eight hours backward: the default 1,800-second token serializes as expiring 7.5 hours ago. The miniapp's `store/token.ts` consequently rejects the token, and its request interceptor omits Authorization even after refresh. Align timestamp creation and comparison with the fixed timezone, and test a newly issued token rather than only a constant wall-clock value.

- **[P1] #2 Migrate client root-ID comparisons before enabling string IDs** — `ZszjJacksonAutoConfiguration.java:37`
  > Existing root menus now return `parentId: "0"`, but `apps/zhongshu-miniapp/src/pages-system/menu/index.vue` still filters with `item.parentId === 0`, making the menu list empty. Both clients' menu forms also use numeric-zero comparisons, so editing an existing root menu with a valid `/system`-style path incorrectly triggers "路径不能以 / 开头". Update the clients' ID constants and comparisons alongside this global serializer change, or provide a compatibility boundary.

- **[P2] #3 Cover ID collections returned directly in CommonResult** — `IdToStringAnnotationIntrospector.java:57-60`
  > `PermissionController#listAdminRoles` returns `CommonResult<Set<Long>>`, whose `data` property does not match this naming rule, so `/system/permission/list-user-roles` still returns `[1]` while `/system/role/simple-list` now returns IDs such as `"1"`. `UserAssignRoleForm.vue` binds these responses directly to an Element Plus select, whose strict equality checks no longer recognize assigned roles.

## 复核与处置

三条发现经本机 Grep/Read 独立复核**全部成立**（非误报）：

- **P1-1**：`OAuth2TokenServiceImpl:185/203` 确用 `LocalDateTime.now()`；序列化端 `TimestampLocalDateTimeSerializer` 已固定 GMT+8 → UTC 部署下令牌过期 epoch 前移 8h，miniapp `store/token.ts` 拒收、拦截器省略 Authorization。
- **P1-2**：miniapp menu `parentId === 0` 三处（`detail/index.vue:110`、`form.vue:211`、`index.vue:97`）+ admin-web `MenuForm.vue:195`；全局字符串化后根菜单 `parentId:"0"` 使列表空、编辑误报路径校验。两端 ID 数值比较共 ~23 处（miniapp 7 + admin-web 16）。
- **P2-3**：`PermissionController:37 getRoleMenuList` + `:65 listAdminRoles` 均返回 `CommonResult<Set<Long>>`，裸集合 `data` 字段不匹配 `id/*Id/*Ids` 命名规则。

### 分批决策（用户拍板 Option B）

面对「破坏性 ID 合同会断裂两端前端」的现实，用户选择 **Option B 分批交付**：

- **SEC-009.A（本次收口）**：保留并修好不破坏前端的合同——时间（P1-1 后端直接修好）+ 分页 + 校验；ID 合同脚手架（`IdToStringAnnotationIntrospector` 类 + `IdToStringAnnotationIntrospectorTest`）就位但**不激活**（wire 退回 `NumberSerializer` 兜底：`|value| < 2^53-1` → number，超限大 ID → string，避免前端精度丢失）。
- **SEC-009.B（拆出，独立跨栈批次）**：全局 ID→string 合同 + 两端前端 ~23 处 ID 数值比较迁移 + `CommonResult<Set<Long>>` 集合覆盖 + OpenAPI/类型同步。

### P1-1 时区（✅ `d13a50b4` 修复 + r1 确认；r1 补全项 `1f6bbb1c` 修复 + r2 确认）

**根因**：改前「生产端 `LocalDateTime.now()`(systemDefault) + 序列化端 systemDefault」在任意 JVM 自洽正确；只把序列化端固定 GMT+8 而生产端不动 → UTC 部署下 creation 与 serialization 失配。**正确修法须同时对齐「生产端 creation + 比较端 comparison + 序列化端 serialization」到同一固定时区。**

修复（`d13a50b4`）：

- `DateUtils`：新增 `now()` = `LocalDateTime.now(ZONE_DEFAULT)`（GMT+8）；`isExpired` 改用 `now()` 比较（比较端对齐）。
- `OAuth2TokenServiceImpl:185/203/249/265`：4 处 `LocalDateTime.now()` → `DateUtils.now()`（令牌生产端对齐）。
- `ZszjServerApplication.main()`：`TimeZone.setDefault(GMT+8)` bootstrap 兜底（裸 jar / CI / 本地运行；Dockerfile 已 `ENV TZ=Asia/Shanghai`）。
- `ZszjJacksonContractTest.testTokenExpiryContract`：UTC 部署下新签发令牌过期 epoch 仍在未来（不被前移 8h），正是 codex 要求的「测新签发令牌而非常量墙钟值」。

**r1 补全项（P2，`1f6bbb1c` 修复）**：r1 指出共享 `isExpired` 改 GMT+8 后，`OAuth2CodeServiceImpl.createAuthorizationCode()` 仍 `LocalDateTime.now()`，UTC 下新签发 5 分钟授权码立即判过期。修复：`OAuth2CodeServiceImpl:41` + `OAuth2ApproveServiceImpl:48/72` 过期生产端 → `DateUtils.now()`。至此 `isExpired` 全部 now()-based 生产端（token/code/approve）统一对齐；`TenantServiceImpl` 的 `tenant.expireTime` 系管理员设定日期（非 now()-based），不在范围。

### P1-2 / P2-3 ID 合同（→ 拆入 SEC-009.B，`d13a50b4` unwire）

`d13a50b4` 从 `ZszjJacksonAutoConfiguration` 移除 `IdToStringAnnotationIntrospector` 的 `annotationIntrospector` 注册（unwire），wire 退回 `NumberSerializer` 安全网；`IdToStringAnnotationIntrospector` 类 + 单测保留待用；`ZszjJacksonContractTest.testIdContract` 标 `@Disabled("ZS-SEC-009.B：全局 ID→string 合同待两端前端 ID 数值比较迁移后激活")`。→ 破坏性 ID 字符串化不再在 SEC-009.A 激活，P1-2/P2-3 的前端断裂风险随之消除；完整 ID 合同迁移归 SEC-009.B。

## 复评（r1，被审提交 `d13a50b4`）

`codex review --commit d13a50b4`（`gpt-6-astra`/`xhigh`）判定 **PASS：0 P1 + 1 P2**——原 2 项 P1 全部消除（P1-1 时区已修，`OAuth2TokenServiceImplTest` 两时区 13✓；P1-2/P2-3 因 unwire ID 序列化器不再触发），新增 1 项 P2（授权码生产端未对齐）。codex 沙箱亲跑 `-Duser.timezone=UTC` 与 `GMT+8` 双向验证：UTC 下 `OAuth2CodeServiceImplTest.testCreateAuthorizationCode:61 expected:<false> but was:<true>` BUILD FAILURE，GMT+8 下 17 测试全绿——精确定位 P2。

> The shared expiry-clock change introduces a confirmed UTC regression in the existing authorization-code test. The startup-only timezone override does not cover that Spring test context.

## 复评（r2，被审提交 `1f6bbb1c`）

`codex review --commit 1f6bbb1c`（`gpt-6-astra`/`xhigh`）判定 **PASS：0 发现**（无 P0 / P1 / P2 / P3）：

> The changes align authorization-code and approval expiration timestamps with the fixed GMT+8 clock already used by DateUtils.isExpired. No actionable regressions were identified; tests were inspected but not executed.

即 codex 确认：授权码与批准过期生产端已对齐 `DateUtils.isExpired` 使用的固定 GMT+8 时钟，无可行回归。**r1 的 1 项 P2 已消除。**（codex r2 未跑测试；本机已补：`OAuth2CodeServiceImplTest` / `OAuth2ApproveServiceImplTest` / `OAuth2TokenServiceImplTest` 在 `-Duser.timezone=UTC` 与 `GMT+8` 双时区各 **26 tests 全绿** BUILD SUCCESS，修复前 `OAuth2CodeServiceImplTest` 在 UTC 失败。）

## 结论

**评审通过（r0 FAIL 2×P1+1×P2 → 分批返工 → r1 PASS 0×P1+1×P2 → P2 修复 → r2 PASS 0 发现）。**

- **r0**（`77ad9531`）：FAIL，2×P1 + 1×P2——时区生产端未对齐（UTC 令牌前移 8h）、全局字符串 ID 断裂两端前端菜单/角色、`CommonResult` 裸集合 ID 未覆盖。
- **分批决策**：用户拍板 Option B——SEC-009.A 收口时间/分页/校验（+ ID 脚手架不激活），破坏性 ID 合同拆 SEC-009.B 跨栈批次。
- **返工**（`d13a50b4`）：修 P1-1 时区三端对齐 + bootstrap TZ + 令牌生命周期测试；unwire ID 序列化器（P1-2/P2-3 → SEC-009.B）。
- **r1**（`d13a50b4`）：PASS，0×P1 + 1×P2——原 2×P1 消除；新 P2：授权码生产端未对齐共享 `isExpired`（UTC 回归）。
- **P2 修复**（`1f6bbb1c`）：OAuth2Code / OAuth2Approve 过期生产端 → `DateUtils.now()`；本机 UTC+GMT+8 双时区 26×2 tests 全绿。
- **r2**（`1f6bbb1c`）：PASS，0 发现，codex 确认 P2 消除。
- **SEC-009 主卡状态**：转**开发中**——SEC-009.A 后端合同已评审通过收口，SEC-009.B 破坏性 ID 跨栈迁移待启动（分批子项未全部收口的主卡状态）。见 docs/05 §16.1。
- codex 发现总数 **4**（r0：2×P1 + 1×P2；r1 新增：1×P2）；已修复/闭合 **4**（P1-1 修复、r1-P2 修复、P1-2/P2-3 拆 SEC-009.B 且 unwire 消除 SEC-009.A 范围风险），SEC-009.A 范围内仍有效 **0**。
