# ZS-CFG-003.B codex 评审处置（r0→r4 五弧）

- 评审工具：codex（gpt-6-astra / xhigh、high，read-only sandbox）
- 评审对象：分支 `feat/cfg-003-b`（ZS-CFG-003.B 套餐/角色权限交集与变更生效）
- 结论：**r4 PASS / 0 发现**（评审收敛）

## 交付内容

1. **授权入口子集校验**：`PermissionServiceImpl.assignRoleMenu` 增 `validateMenusInTenantPackage`——租户角色的菜单授权必须是所属租户套餐菜单的子集（新错误码 `TENANT_PACKAGE_MENU_EXCEED` 1-002-016-005）；系统租户（packageId=0）菜单全量不受限；Mapper 级注入 TenantMapper/TenantPackageMapper 断服务层循环依赖。
2. **三流程统一锁协议**（r0→r2 三轮深化）：授权入口、套餐收缩收敛（updateTenantRoleMenu）、换套餐（updateTenant）共用租户行锁；updateTenant/createTenant 先锁目标套餐行——**统一「套餐→租户」全局锁序**（codex r1 P1：换套餐换入正在收缩的套餐会逃过收敛；r2 P1：锁前快照判断绑定/菜单变化失效）。收敛循环按租户 id 排序、锁内重查绑定、换出租户跳过。
3. **缺记录拒绝**（r0 P2）：缺租户/缺套餐记录抛 `TENANT_NOT_EXISTS`/`TENANT_PACKAGE_NOT_EXISTS`，不等同系统租户豁免（防御性放行被推翻）。
4. **锁内读纪律**（r2 P1/P2 + r3 P2）：updateTenantPackage 先锁本套餐行再比较菜单变化；createTenant/updateTenant 使用锁定套餐的**最新菜单**完成管理员授权（null 拒绝）。
5. **8 个契约测试**：越界拒绝不落库/许可内放行/系统租户全量/套餐收缩收敛+热路径拒绝/扩大不自动扩普通角色且租户管理员随套餐/缺租户拒绝/缺套餐拒绝/并发收缩×授权 FOR UPDATE 阻塞序列化（终态不变式：角色菜单 ⊆ 套餐菜单）。

## 评审弧

| 轮次 | 结论 | 要点 |
|---|---|---|
| r0 | FAIL 1×P1+2×P2 | P1 校验-写入 TOCTOU（锁协议）；P2 防御性放行被推翻（缺记录拒绝）；P2 收缩测试未证缓存立即失效（如实登记：真实入口+缓存联动由 PERM-004.A 证明） |
| r1 | FAIL 1×P1+2×P2 | P1 换套餐换入租户逃逸收缩（套餐行锁 + 锁内重查绑定）；P2 并发用例阻塞证明不足（登记边界）；P2 既有 PermissionServiceTest 回归（补系统租户行） |
| r2 | FAIL 2×P1+P2 | P1 换套餐以锁前绑定判断收敛（锁租户行+锁定读）；P1 并发套餐更新绕过收敛循环（先锁套餐行）；P2 锁定返回值被丢弃（使用最新菜单） |
| r3 | FAIL P2 | createTenant 锁定返回值仍被丢弃（前轮补丁丢失，补齐 + effectively-final 修正） |
| r4 | **PASS / 0 发现** | 全量五 commit 关键点复查 |

## 验证

- 回归：TenantPackageMenuIntersectionTest 8 + PermissionServiceTest + PermissionCacheConsistencyTest + RoleServiceImplTest + MenuServiceImplTest + TenantServiceImplTest + TenantPackageServiceImplTest = **123 tests / 0 failures，BUILD SUCCESS**。
- 合并：`--no-ff` 合并回 main（见 docs/05 V1.34 记录）。

## 延后/边界（非阻塞）

1. 并发用例的真实入口端到端（经 TenantPackageServiceImpl 收缩 + 真实 Spring Cache 预热命中）已部分覆盖；跨 H2/PG 双方言复验归 ZS-SYS-001.A 真实 PG 回归。
2. "关闭模块无接口/Job"的运行时拦截链归 SEC 链与 ZS-CFG-003.A 已交付的目录判定；本子项为套餐/角色交集的写侧闭环与变更生效。
3. 撤权审计字段仍归 ZS-PERM-004.B（B08）。
