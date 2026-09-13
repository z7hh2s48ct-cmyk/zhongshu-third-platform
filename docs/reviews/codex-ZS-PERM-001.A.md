# ZS-PERM-001.A codex 评审处置（HANDOFF 补评，1×P2 登记处置）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit 6eb81717` @ main）
- 评审对象：ZS-PERM-001.A 授权目标归属与可授予上限校验（HANDOFF-ZS-PERM-001.A 交接单的待评审提交；2026-09-14 补评入库）
- 结论：**0×P0/P1，1×P2 登记处置**（按 reviews/README「后续处理约定」第 2 条，非阻塞）
- 原始日志：`outputs/handoff/ZS-PERM-001.A.txt`（未入库；本档案为处置入库）

## 发现与处置

| 级别 | 发现 | 处置 |
|---|---|---|
| P2 | **Exclude disabled roles from the super-admin exemption**（PermissionServiceImpl L460-462）：`validateUserRoleGrantCeiling` 的超管豁免经 `RoleServiceImpl.hasAnySuperAdmin()` 只查角色编码、不查角色状态——操作者保留已禁用的 super_admin 角色 + 经另一启用角色持 `assign-user-role` 权限时，豁免仍生效可绕过自我提权上限（条件式绕过，与 `hasAnyPermissions()` 过滤禁用角色的语义不一致） | 登记处置：超管豁免应在判定前过滤禁用角色（对齐 hasAnyPermissions 语义）。触发需「禁用超管角色仍挂载 + 双角色组合」双重条件，实际风险受控；归 ZS-PERM-001 后续小卡（与 ZS-IAM-003 岗位引用校验 hotfix 同窗处置） |

## 验证（交付时点证据，沿用卡片开发记录）

- `-pl :zszj-module-system "-Dtest=PermissionServiceTest" test`：33 tests / 0 failures（2026-09-09T14:05:26+08:00，24 既有 + 9 新增：错租户 user/role/dept 拒绝、批量混入、禁用角色拒绝、自我提权、超上限、重复授权幂等等）

## 边界

本子项仅同技术租户归属校验；获准跨组织范围归 ZS-PERM-001.B（B08/D-09）。前置 ZS-LOGIN-005.A、ZS-PERM-004.A 已按 §16.1 满足并分别评审（codex-ZS-LOGIN-005.A.md / codex-ZS-PERM-004.A.md）。
