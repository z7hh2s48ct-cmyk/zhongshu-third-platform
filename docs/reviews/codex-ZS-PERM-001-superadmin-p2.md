# codex 评审处置：ZS-PERM-001.A 小卡——超管豁免仅认启用状态角色

> 被审对象：worktree 隔离分支 `feat/perm-001-superadmin-p2` 的 impl 提交 `8726f6c5`（5 files，+158/-16），`codex review --commit` 单弧评审；`--no-ff` 合并 main（`1923a1e5`）。
> 背景：处置 2026-09-14 HANDOFF 交接单补评（[codex-ZS-PERM-001.A.md](codex-ZS-PERM-001.A.md)）登记的 1×P2——「Exclude disabled roles from the super-admin exemption」：`validateUserRoleGrantCeiling` 的超管豁免经 `RoleServiceImpl.hasAnySuperAdmin()` 只查角色编码、不查角色状态，操作者保留已禁用的 super_admin 角色 + 经另一启用角色持 `assign-user-role` 权限时，豁免仍生效、可条件式绕过自我提权上限（与 `hasAnyPermissions()` 过滤禁用角色的语义不一致）。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：`outputs/perm001-codex-r0.log`（UTF-16，gitignored 工作痕迹未入库；结论逐字引用于本文）。

## 交付内容（`8726f6c5`）

- **语义分型**（核心设计）：`hasAnySuperAdmin`（不区分状态）与新增 `hasAnyEnabledSuperAdmin`（仅启用状态）按调用点语义分离——
  - **豁免点切换为启用语义**（禁用角色不产生任何豁免）：①`PermissionServiceImpl#isSuperAdminUser`（本次缺陷主路径，`validateUserRoleGrantCeiling` 的自我提权豁免）；②`getRoleMenuListByRoleId`（菜单全量豁免）；③`hasAnyPermissions` 情况二（原已上游启用过滤，改为显式语义声明）；
  - **授予上限点保持不区分状态**：`validateUserRoleGrantCeiling` 对目标角色的 `hasAnySuperAdmin` 检查保持原样并注明理由——禁用超管角色同样不可由非超管授予，堵「先授禁用超管角色、待解禁生效」的搁置提权；
- **可测性**：单角色谓词提取为 `RoleServiceImpl` 静态包可见方法（`isSuperAdminRole`/`isEnabledSuperAdminRole`，循 ConfigSensitiveClassifier 惯例），绕开 `getSelf()` Spring 代理；
- **测试**：新增 `RoleSuperAdminSemanticsTest`（7 用例，谓词语义分型看守）；`PermissionServiceTest` 新增接线看守 `testAssignUserRole_disabledSuperAdminRole_noExemption`（操作者持禁用超管角色 + 旧语义 mock 判 true、新语义判 false → 须按自我提权拒绝——**旧实现下该用例必失败**，即真回归看守），存量 3 处 mock 同步语义（2 处豁免路径改新方法、1 处上限路径保持）。

## 评审弧（r0，`gpt-6-astra`/`xhigh`/`read-only`）

| 轮次 | 被审提交 | 结论 | 要点 |
|---|---|---|---|
| **r0** | `8726f6c5` | **CLEAN / 0 发现** | 「The change consistently requires enabled super-admin roles for exemptions while preserving status-independent grant-ceiling checks. No actionable regressions were identified. Existing relevant test reports show no failures; tests were not rerun in the read-only environment.」——codex 复核 surefire 报告（PermissionServiceTest 35 + RoleSuperAdminSemanticsTest 7 + RoleServiceImplTest 22 全绿）与 RoleCodeEnum/CommonStatusEnum 定义后确认语义一致 |

**收口依据**：r0 达 0×P0/P1 阀值，一弧 CLEAN；登记缺陷与修复一一对应，无延后项。

## 验证

- **合并前（worktree）**：`PermissionServiceTest` 35/35 + `RoleSuperAdminSemanticsTest` 7/7（含接线看守用例）；system 模块全量 685 tests 中 OAuth2 令牌（2）与短信尝试限（3）共 5 例在全量并发负载下失败，**单独复跑 29/29 全绿**——时序敏感用例偶发，与本卡无关（本卡未触碰 oauth2/sms 域）。
- **合并后（main `1923a1e5`）**：`run-local-gates --fast` **10/10 EXIT=0**；权限语义变更涉及 G14 矩阵角色用例，`run-sys001-regression.mjs` 复跑 **49/49 EXIT=0**（真实 server+PG）。
- **收口文档**：docs/05 V1.51（PERM-001 卡片开发记录 + 变更行）、reviews/README B03 专项表索引行、根 README 索引同步。

## Codex 原始结论（r0 逐字引用）

> The change consistently requires enabled super-admin roles for exemptions while preserving status-independent grant-ceiling checks. No actionable regressions were identified. Existing relevant test reports show no failures; tests were not rerun in the read-only environment.
