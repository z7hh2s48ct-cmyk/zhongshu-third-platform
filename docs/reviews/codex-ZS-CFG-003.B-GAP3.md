# ZS-CFG-003.B GAP-3 修复 codex 评审处置（r0→r1→r2 三弧，0×P0/P1 收敛）

- 评审工具：codex（gpt-6-astra / xhigh，read-only sandbox，`codex review --commit <SHA>` @ worktree `E:/zszj-wt-cfg-003-b`）
- 评审对象：分支 `feat/cfg-003-b`（ZS-SYS-001.A 登记缺口 GAP-3 的根因修复；分支名沿用 CFG-003.B 归口）
- 结论：**r2 无 P0/P1**（2×P2 登记处置，归「套餐菜单不变量维护」后续小卡）
- 原始日志：worktree `outputs/gap3-codex-r{0,1,2}.txt`（未入库；本档案为处置入库）

## 根因（更正 SYS-001.A 初判）

**「JacksonTypeHandler 泛型擦除 × MP 3.5.17」假说不成立。** 真相：[PermissionController.assignRoleMenu](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin/permission/PermissionController.java) 存在上游 yudao 遗留的 `handleTenantMenu` 套餐静默过滤——`reqVO.getMenuIds().removeIf(不在套餐)` 在**服务端校验之前**吞掉越界菜单，把 CFG-003.B 的「越界显式拒绝（TENANT_PACKAGE_MENU_EXCEED 1002016005）」降级为「部分成功」（code=0、无 INSERT、越界行=0）。H2 服务层测试直调 service 不经控制器，故「同路径守卫通过」属**测试层级差异**而非数据库差异——依赖升级（ZS-DB-001）对本项不再需要。定性：**无越权升级**（服务端从未写入越界行），但违反 CFG-003.B 显式拒绝合同、掩盖越权企图、产生「部分成功」不一致。

## 修复链（3 提交）

| 提交 | 内容 | 评审 |
|---|---|---|
| `c44e1d51` | 删除控制器静默过滤（失效 TenantService 注入/import 一并清理）；harness：SYS-ROLE-N1 摘出 REGISTERED_GAPS 转正式断言 | r0 **2×P2** |
| `bc67fcd2` | r0 两项 P2 修复：①`assignRoleMenu` 补**菜单存在性显式校验**（`MENU_NOT_EXISTS`，堵系统租户伪造 ID 写入无外键的 system_role_menu 形成悬空记录——上游静默过滤原本兼担此职）+ PermissionServiceTest 专项用例；②harness 报告 GAP-3 转已修复/根因更正（删除「仅剩 GAP-3」与依赖升级误导措辞） | r1 **1×P1** |
| `675f62de` | r1 P1 修复：存在性校验误伤**内部供给链**（deleteMenu 只清 role_menu 不维护 `TenantPackageDO.menuIds` 不变量，残留陈旧 ID 使 createTenant/updateTenantRoleMenu 传入即被拒、开通回滚）→ TenantServiceImpl 新增 `sanitizeExistingMenuIds`（按现有菜单交集消毒）套用 createRole/updateTenantRoleMenu 共 3 内部点；外部伪造拒绝语义保持；TenantServiceImplTest 合成桩使消毒结果=原集合、既有 verify 不变 | r2 **2×P2（登记处置）** |

## r2 两项 P2（登记处置，归「套餐菜单不变量维护」小卡）

1. **Sanitize package menus once before the role loop**：收敛循环逐角色各查一次 getMenuList（消毒+assignRoleMenu 自身各一次），持锁期间放大查询量与锁时长——后续小卡改为循环前消毒一次。
2. **Validate package menu IDs before sanitizing them**：`/system/tenant-package/update` 可持久化不存在的菜单 ID（validateTenantPackageMenus 不查存在性；消毒使收敛不再抛 MENU_NOT_EXISTS）→ 悬空套餐 ID 静默入库，若该 ID 日后创建会意外生效。注：该洞先于本修复即存在（修复前收敛会把伪造 ID 写进 role_menu，更糟）；后续小卡在套餐 create/update 层补存在性校验。

## 验证

- **真实 PG 矩阵 49/49 PASS，EXIT=0**（修复前 48/49；SYS-ROLE-N1：`code=1002016005`「菜单【[102]】超出租户套餐【SYS001套餐】的许可范围」，PG 越界行=0）
- H2：PermissionServiceTest **34/0**（+menuNotExists 专项）、TenantPackageMenuIntersectionTest 8/0、TenantServiceImplTest 23/0、TenantPackageServiceImplTest 14/0
- 合并 main 后主树 G14 门禁复跑（本日，终证）
- 经验登记：①「H2 通过 ≠ 同路径通过」可以是测试层级差异（service vs HTTP），不必然是方言差异——修依赖前先验证假说；②给写路径加显式拒绝时，必须清点上游是否已有静默兜底在竞争同一职责；③内部可信路径与外部输入的存在性校验语义须分离（消毒 vs 拒绝）。
