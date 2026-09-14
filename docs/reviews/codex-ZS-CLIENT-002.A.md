# codex 评审处置：ZS-CLIENT-002.A 移动端服务端授权导航注册表与直达页守卫

> 被审对象：worktree 隔离分支 `feat/client-002-a` 的渐进未提交/未跟踪改动，四弧均以 `codex review --uncommitted` 就当前工作树状态评审（r3 被审状态即最终提交内容）。
> impl 提交 `8b5ac0c0`（6 files，+571/-1：新增 `access.ts`/`403.vue`/`router-access.spec.ts` + 改 `interceptor.ts`/`tabbar/store.ts`/`tests/unit/setup.ts`），`--no-ff` 合并 main `57363732`。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`（经 `-c model_reasoning_effort="xhigh"` 覆盖 `config.toml` 默认 `high`），sandbox `read-only`，approval `never`。
> 完整 stdout：四轮保留于 `outputs/client002a-codex-r{0,1,2,3}.log`（UTF-16，属 gitignored 工作痕迹未入库；各轮结论已逐字引用于本文，循 ZS-CLIENT-003 移动域先例不另存 raw）。
> 说明：`read-only` sandbox 无法建临时文件跑 Vitest，codex 改用**直接 node transpile 执行** `access.ts` 在内存复现每条判定（各轮 stdout 附 `{"route":...,"allowed":...}` 复现证据）；单测通过性以本机 vitest 75/75 为准。本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。

## 交付内容

移动端「服务端授权导航」单一真相源，客户端只做**权限集合成员判断**、不在前端重算业务权限（服务端 `@PreAuthorize` 为最终边界）：

- **[access.ts](../../apps/zhongshu-miniapp/src/router/access.ts)**（195 行，授权判定核心）：从 [menu.json](../../apps/zhongshu-miniapp/src/pages/index/menu.json) 单一真相源构建「路由→权限」注册表（`exactRegistry` 精确 + `prefixRegistry` 祖先前缀），`EXTRA_ROUTE_ACCESS` 补登 menu.json 未覆盖的跨包/共享页（IM 通讯录、PAY 收银台、BPM 审批详情/发起共享页）；`hasRouteAccess(route, permissions)` 四级判定——① 公共外壳（`/pages-core/`、`/pages/index/`、`/pages/message/`、`/pages/user/` 及根路径）放行 ② 精确命中强制其声明权限 ③ 最近祖先前缀继承**命中即返回判定**（未通过则拒，不回退更宽模块并集）④ 皆未命中→拒绝。
- **[403.vue](../../apps/zhongshu-miniapp/src/pages-core/error/403.vue)**：无授权/未知落点页（`UNAUTHORIZED_PAGE = '/pages-core/error/403'`）。
- **[interceptor.ts](../../apps/zhongshu-miniapp/src/router/interceptor.ts)**（+9）：`navigateTo` 直达守卫——登录后对非登录页调 `hasRouteAccess`，未授权 `reLaunch` 至 403。
- **[tabbar/store.ts](../../apps/zhongshu-miniapp/src/tabbar/store.ts)**（+8/-1）：`isTabbarItemVisible` 先委托 `hasRouteAccess(item.pagePath, userStore.permissions)` 再走既有 roles 检查，TabBar 授权化。
- **[router-access.spec.ts](../../apps/zhongshu-miniapp/tests/unit/router-access.spec.ts)**（39 测试）+ **[setup.ts](../../apps/zhongshu-miniapp/tests/unit/setup.ts)**（+1 `switchTab` mock）。

## 评审弧（r0→r3，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 被审状态 | 结论 | 要点（access.ts 行号为当轮被审版本） |
|---|---|---|---|
| **r0** | 初版注册表 | **FAIL 1×P1 + 1×P2** | P1 未注册业务路由默认放行（`:60-61`）；P2 BPM 各页权限被压平为 `task:query`（`:33-34`） |
| **r1** | r0 修复后（引入默认拒绝） | **FAIL 1×P1** | 默认拒绝误伤 BPM detail/audit/create、IM contact 等兄弟/跨包页（`:83-84`） |
| **r2** | r1 修复后（补 EXTRA 登记） | **0×P0/P1 + 2×P2** | P2-1 子目录拒绝被模块并集兜底覆盖（`:190-192`，**已修**）；P2-2 停用模块页空权限集放行撞后端 501（`:141-142`，**延后**） |
| **r3** | r2-P2-1 修复后（= `8b5ac0c0`） | **0×P0/P1 + 2×P2** | 后代言权限并入父作用域（`:120-121`）；共享表单动作权限被 query 覆盖（`:188-190`）——**均延后** |

**收口依据**：r3 达 plan §1.1 第 7 步「0×P0/P1 才收口」阀值（r1-P1 + r2-P2-1 双归零）；剩余 3×P2（r2-P2-2 + r3-P2-1 + r3-P2-2）依 [README](README.md)「后续处理约定」第 2 条「P2/P3 可分批处置」登记 **ZS-CLIENT-002.B**（B08）后续批次，循 ZS-FILE-003（r0→r4）、ZS-SEC-010（P2 延后）先例，避免同一「前缀继承精度」主题的无限 P2 循环。

## Codex 原始结论（逐轮逐字引用）

### r0（FAIL，1×P1 + 1×P2）

> The registry allows unregistered business routes while rejecting valid BPM permission combinations. Both behaviors were reproduced in memory; the unit suite was blocked by Vitest's temporary-file writes in the read-only sandbox.

- **[P1] Reject unregistered business routes by default** — `access.ts:60-61`
  > With no permissions, `/pages-system/user/index` and `/pages-crm/customer/index` both pass this check. These pages are included by `vite.config.ts`, so the interceptor's existence check also passes, allowing unauthorized business navigation rather than the required rejection. Explicitly allowlist public landing/error pages and reject other unregistered routes.
- **[P2] Preserve the distinct permissions for BPM pages** — `access.ts:33-34`
  > Users granted `bpm:process-instance:query` or `bpm:oa-leave:query` without `bpm:task:query` now receive 403 when opening their authorized My Processes or Leave entries. Existing `pages/index/menu.json` exposes those entries using their respective permissions, and the backend controllers enforce those same distinct permissions. Map BPM pages to their actual grants and support the shared approval page's multiple entry permissions instead of requiring task-query permission throughout.

### r1（FAIL，1×P1）

> The registry blocks existing authorized navigation flows, including BPM approvals and IM contacts. Direct execution confirmed these failures; the full Vitest suite could not run because the read-only sandbox denied temporary-file creation.

- **[P1] Register reachable sibling pages before enabling default-deny** — `access.ts:83-84`
  > Authorized BPM users can open the approval list, but its detail, audit, and create actions now redirect to 403. The menu-derived prefixes `/pages/bpm/` and `/pages-bpm/processInstance/manager/` do not cover `/pages-bpm/processInstance/detail/` or `/pages-bpm/processInstance/create/`. These existing destinations fail authorization even with every menu permission. Likewise, the IM tab bar links to `/pages-im/home/contact/index`, outside the registered conversation prefix. Add explicit permission mappings for existing sibling and cross-package destinations, with tests covering the actual navigation targets.

### r2（0×P0/P1 + 2×P2）

> The new guard permits protected child pages through unrelated permissions and does not reject permission-free entries belonging to disabled modules. Both were reproduced in memory; the full Vitest run was blocked by temporary-directory restrictions.

- **[P2] Preserve specific child-route denials before module fallback** — `access.ts:190-192`
  > With only `system:dept:query`, `/pages-system/user/index` is denied, but `/pages-system/user/detail/index` and `/pages-system/user/form/index` are allowed: a failed user-directory permission check falls through to the entire system-module permission union. Direct links therefore bypass the new page-level restriction. Reject failed specific matches and explicitly register genuinely shared pages instead of granting access through unrelated permissions.
- **[P2] Check module availability before allowing permission-free routes** — `access.ts:141-142`
  > The current backend's `ModuleCatalog` enables only system and infra, yet this rule allows `/pages-ai/chat/index`, IM pages, and the payment cashier even with an empty permission set. These disabled-module pages consequently open through direct links and encounter backend 501 responses instead of the required unavailable landing page. An omitted permission code cannot establish that the server enabled an entry; check server-granted entry/module availability separately.

### r3（0×P0/P1 + 2×P2，被审 = `8b5ac0c0`）

> Permission inheritance admits unauthorized child routes and blocks an existing authorized creation flow. Both decisions were reproduced in memory; the read-only sandbox prevented Vitest execution.

- **[P2] Keep descendant permissions out of parent resource scopes** — `access.ts:120-121`
  > Registering every ancestor with a union of descendant permissions broadens access incorrectly. For example, holding only `hrm:employee:config:query` denies `/pages-hrm/employee/index` but allows `/pages-hrm/employee/detail/index`, because the configuration permission was merged into `/pages-hrm/employee/`. Customer configuration permissions similarly unlock customer detail routes. Inherit the nearest declared resource's permissions without merging permissions from its independently protected descendants.
- **[P2] Preserve action-authorized access to shared forms** — `access.ts:188-190`
  > A user holding `hrm:employee:query` and `system:dept:create`, but not `system:dept:query`, can open HRM organization management and see its explicitly permission-guarded Add button. Its `handleAdd` navigates to `/pages-system/dept/form/index`, which now inherits the list permission and redirects to 403. This previously working creation flow uses unrestricted simple-list endpoints and a create endpoint requiring only `system:dept:create`. Register the shared form's action permissions instead of unconditionally requiring its menu's query permission.

## 复核与处置

### 已修复（弧内闭合，r3 确认归零）

- **r0-P1（未注册业务路由默认放行）✅**：`hasRouteAccess` 由「存在即放行」改为**四级默认拒绝**——仅公共外壳前缀 + 根路径放行，业务页须经注册表命中判定，皆未命中即拒（步骤 4 `return false`）。r1/r2/r3 未再复现。
- **r0-P2（BPM 各页权限压平）✅**：新增 `BPM_SHARED_APPROVAL_PERMISSIONS`（`process-instance:query`/`task:query`/`process-instance-cc:query`/`process-instance:manager-query`），共享审批页按**多入口权限**判定（持任一则放行），不再全程强制 `task:query`；menu.json 中各 BPM 页保留其各自权限。
- **r1-P1（默认拒绝误伤兄弟/跨包页）✅**：`EXTRA_ROUTE_ACCESS` 显式补登 menu.json 未覆盖的既有可达页——`/pages/contact/index`（`system:user:list`）、`/pages-pay/cashier/index`（`[]` 登录即用）、BPM `processInstance/detail/index` 与 `create/index`（共享审批权限）；`ancestorPrefixes` 使子页继承最近声明祖先前缀。spec 补「跨包/中转继承」describe（6 测试）看守。
- **r2-P2-1（子目录拒绝被模块并集兜底覆盖）✅**：**移除步骤 4 的模块并集兜底**（删 `moduleOf`/`moduleUnion`），步骤 3 最近祖先前缀**命中即返回判定**（未通过则拒、不回退更宽模块并集）；`/pages-system/user/detail/index` 仅持 `system:dept:query` 时正确拒绝。spec 补「具体子目录拒绝不被更宽兜底覆盖」describe（2 测试）看守。r3 未再提及 = 确认归零。

### 延后（3×P2，登记 ZS-CLIENT-002.B / B08，附理由与归属）

三项 P2 同属**前缀继承模型的精度**主题（服务端 `@PreAuthorize` 为真实安全边界，客户端守卫为纵深防御 UX 层），宜在 .B「获批业务组织导航」批次以完整业务组织页/动作注册表整体重构处置，避免逐轮 P2 碎片化返工：

- **r2-P2-2 — 停用模块页空权限集放行撞后端 501（`access.ts` 免权限路由）**：收银台/公共前缀等 `permissions: []` 路由对空权限集放行，`ModuleCatalog` 仅启用 system/infra 时 `/pages-ai/chat/index` 等停用模块页可经直达打开、撞后端 501 而非「不可用落点」。**延后理由**：① 正解需服务端下发**模块/入口启用信号**（`AuthPermissionInfo.menus` 或 ModuleCatalog 接线至客户端 store），当前客户端只持权限集合、无模块启用集合，属**跨栈接线**（后端 DTO + 客户端 store + access.ts）超出 .A「技术权限移动导航」范围；② 停用模块页的后端接口本就 501/拒绝，不构成越权（服务端为最终边界）；③ §16.1 ZS-CLIENT-002.B 退出条件已含「关闭模块落点」，与该批次「获批业务组织导航」同源。**归属**：CLIENT-002.B，前置接线 `AuthPermissionInfo.menus`。
- **r3-P2-1 — 后代言权限并入父资源作用域（`access.ts:120-121` `registerRoute` 祖先前缀循环）**：`ancestorPrefixes` 将每条路由权限并入其**全部**祖先前缀，致后代言权限（如 `hrm:employee:config:query`）上溢至 `/pages-hrm/employee/`，再经前缀继承放行兄弟后代 `/pages-hrm/employee/detail/index`。**延后理由**：① 属**继承精度**问题非安全绕过——`employee/detail` 的数据接口要求 `hrm:employee:query`，仅持 `config:query` 者导航后仍被服务端拒（UX 精度：先导航后见接口 403）；② 正解「继承最近声明资源权限、不并入独立受保护后代」需将前缀注册表重构为「最近声明资源」语义，与 r3-P2-2 共享表单动作权限同属继承模型重构，宜合并处置；③ 完整业务组织页注册表是 .B 交付物。**归属**：CLIENT-002.B。
- **r3-P2-2 — 共享表单动作权限被 query 覆盖（`access.ts:188-190` 步骤 3 前缀继承）**：`/pages-system/dept/form/index` 经前缀继承列表权限 `system:dept:query`，致仅持 `system:dept:create`（无 `query`）者点 HRM Add 按钮跳表单被拦 403，阻断原可用创建流（后端 create 端点仅要求 `dept:create`）。**延后理由**：① 共享表单（create/edit 复用 `form/index`）需**动作级权限登记**（create vs update vs query），须逐模块枚举共享表单动作权限=跨模块 scope；② menu.json 单一真相源暴露列表/query 权限、不含每动作表单权限，正解需服务端下发 `allowedActions`（卡片调整明列「消费 allowedActions/字段授权，不在前端重算业务权限」）或共享表单动作注册表，属 .B 范围；③ 后端创建流本就可用（仅要求 `dept:create`），客户端守卫过严=UX 回归非安全漏洞，但与 r3-P2-1 同属继承模型精度，合并重构更稳。**归属**：CLIENT-002.B。

## 验证

- **vitest**：`Tests 75 passed (75)`，`Test Files 3 passed (3)`（`router-access.spec.ts` 39 + `interceptor.spec.ts` 26 + `http.spec.ts` 10），`EXIT=0`。
- **lint**（`eslint --cache`）`EXIT=0`；**type-check**（`vue-tsc --noEmit`）`EXIT=0`；**build:h5**（`uni build`）`DONE Build complete.` `EXIT=0`；**build:mp**（`uni build -p mp-weixin`）`DONE Build complete.` `EXIT=0`（微信开发者工具 CLI 未装仅影响 IDE 打开、不影响构建产物；`useLiveKitRoom`/browserslist 数据陈旧为既有非阻塞警告）。
- codex 四轮均 `read-only` 未执行 vitest，以 node transpile 内存复现各判定（stdout 附 `{"route":...,"allowed":...}` 证据）；通过性以上述本机门禁为准。

## 结论

**评审收口（r3 达 0×P0/P1；r0-P1/r0-P2/r1-P1/r2-P2-1 四项弧内修复并确认归零；3×P2 延后 ZS-CLIENT-002.B）。**

- **r0**（初版）：FAIL 1×P1+1×P2——未注册业务路由默认放行 + BPM 各页权限压平。
- **r1**（r0 修复后）：FAIL 1×P1——默认拒绝误伤 BPM detail/audit/create、IM contact 等兄弟/跨包页。
- **r2**（r1 修复后）：0×P0/P1 + 2×P2——子目录拒绝被模块并集兜底覆盖（P2-1，已修）+ 停用模块页空权限集放行（P2-2，延后）。
- **r3**（r2-P2-1 修复后 = `8b5ac0c0`）：0×P0/P1 + 2×P2——后代言权限并入父作用域 + 共享表单动作权限被 query 覆盖（均延后）。
- codex 发现总数 **7**（3×P1 + 4×P2），弧内已修复 **4**（r0-P1/r0-P2/r1-P1/r2-P2-1），延后 **3**（r2-P2-2/r3-P2-1/r3-P2-2，附理由与归属），仍有效阻塞项 **0**。
- ZS-CLIENT-002.A 为分批子项（父卡 ZS-CLIENT-002，B06）：本轮交付技术权限移动导航注册表 + TabBar 授权化 + 直达守卫 + 403 落点，父卡状态 待开发→**开发中**（.A 交付、.B 归 B08）；获批业务组织导航 + 模块启用信号 + 继承模型精度重构 + 身份切换（依 D-09）归 **ZS-CLIENT-002.B**。本记录不表示任何主任务已验收。
- **延后 Minor**（非阻塞，归 CLIENT-002.B）：① 停用模块页需服务端模块/入口启用信号（接线 `AuthPermissionInfo.menus`）；② 前缀继承「最近声明资源」语义重构（后代权限不上溢父作用域）；③ 共享表单动作级权限登记（消费 `allowedActions`，create/edit 复用表单不误拦创建流）。
