# 评审交接：ZS-PERM-001.A（右窗口 → 左窗口 codex 评审）

> 本文件为**评审交接单**，非评审结论。左窗口据此对下列提交执行 `codex review`，产出
> `codex-ZS-PERM-001.A.md`（人工复核 + codex 原始结论）与 `codex-ZS-PERM-001.A.raw.md`（完整 stdout），
> 并将本交接单与评审文档一并入库（遵循 [README.md](README.md) 「后续处理约定」）。

## 1. 评审目标

| 项 | 值 |
|---|---|
| 任务 | ZS-PERM-001.A（05 文档 16.1 节，B03 批次；前置 ZS-SEC-001.A、ZS-SEC-012.A 均已完成） |
| 交付 | 现有技术主体的授权目标/上限校验（服务层写入前统一显式校验） |
| 待评审提交 | `6eb81717` |
| 评审命令 | `codex review --commit 6eb81717` |
| 变更规模 | 4 files changed, 370 insertions(+) |
| 本地验证（后端） | BUILD SUCCESS，Tests run: 33, Failures: 0, Errors: 0, Skipped: 0（2026-09-09T14:05:26+08:00；24 既有 + 9 新增） |
| 复现入口（后端） | `source tools/env.sh && cd services/zhongshu-core && mvn -pl :zszj-module-system "-Dtest=PermissionServiceTest" test` |
| 解锁 | 本子项退出后解锁 ZS-PERM-004.A（角色/菜单缓存与停用撤权一致性）；ZS-PERM-001.B（跨组织授权范围）须 D-09 |

## 2. 变更文件清单（仅这 4 个，未含其他窗口改动）

后端主代码（`zszj-module-system/src/main/`）：
- `java/cn/zszj/module/system/service/permission/PermissionServiceImpl.java`（三个 assign 方法写入前补齐统一校验 + 7 个私有 helper：validateUserForAssign / validateRolesForAssign / validateRoleForAssign / validateDeptListForAssign / validateUserRoleGrantCeiling / isSuperAdminUser / validateTenantScope + isTenantScopeValidationEnabled）
- `java/cn/zszj/module/system/enums/ErrorCodeConstants.java`（新增错误码段 1-002-009-000~004：PERMISSION_ASSIGN_USER/ROLE/DEPT_OTHER_TENANT、PERMISSION_GRANT_EXCEED_CEILING、PERMISSION_SELF_ELEVATION）

后端测试（`zszj-module-system/src/test/`）：
- `java/cn/zszj/module/system/service/permission/PermissionServiceTest.java`（更新 3 个既有 assign 用例 mock 合法归属路径 + @AfterEach 清理租户上下文；新增 9 用例）

文档：
- `docs/05-底座模块分析与开发任务清单.md`（ZS-PERM-001 卡追加 .A 开发记录）

> ⚠️ 工作树中 `scripts/brand/*`、`services/zhongshu-core/sql/**`、`docs/reviews/README.md`、`_dry*.mjs`/`_ev*.cjs`/`_mut.mjs`/`_fixseed.mjs` 等（M/??）**不属于本提交**，为左窗口 brand hotfix-B 工作，评审时勿纳入 `6eb81717` 范围。

## 3. 请重点核查的判断点（按验收红线 line 487/488）

1. **归属校验是否真正堵住跨租户外键盲写**（核心）：原 `assignUserRole` 直接写 `UserRoleDO(userId, roleId)`，
   **从不 fetch 角色**，篡改的他租户 roleId 被盲写（关系行 tenant_id=操作者租户，role_id 指向他租户角色）。
   请核查：① 三个 assign 方法是否**全部**覆盖校验（assignUserRole/assignRoleMenu/assignRoleDataScope）；
   ② 是否还有**其它**写入 user_role/role_menu 关联的路径（如 RoleServiceImpl、TenantServiceImpl 直调 mapper）
   绕过本校验；③ 显式比对 `role.getTenantId()` 与 `TenantContextHolder.getTenantId()` 是否践行 line 488
   「不能只凭请求 tenant_id 为关系表填值就认定外键安全」。

2. **供给路径兼容性**（关键，不可破坏）：`TenantServiceImpl.createTenant`/`updateTenantRoleMenu` 经
   `TenantUtils.execute(newTenantId)`（设具体租户 + `setIgnore(false)`）运行。请核查：① 新建 user/role 的
   tenantId==环境租户，归属校验天然通过；② 供给分配 TENANT_ADMIN（非 super_admin）且目标用户≠平台登录用户，
   上限/自我提权**不触发**；③ createRole 先于 assignRoleMenu、createUser 先于 assignUserRole，校验时目标对象
   已存在。确认供给链未被新校验误伤。

3. **护栏条件是否留下绕过面**：`isTenantScopeValidationEnabled() = !isIgnore() && getTenantId()!=null`；
   上限/自我提权仅在 `getLoginUserId()!=null` 时执行。请判断：① `isIgnore()==true` 跳过归属校验是否仅发生于
   **受信任的系统级操作**（而非普通 Web 请求可触发）；② Web 请求恒有 loginUserId，故 `getLoginUserId()==null`
   跳过上限/自我提权是否只对**系统内部/供给**调用生效，不构成 Web 绕过；③ 攻击者能否通过构造 ignore 上下文
   或无登录态来规避校验。

4. **production 租户过滤 vs 显式比对的关系**：生产中 `roleService.getRoleList`/`getUser`/`getDeptList` 受 MyBatis
   租户拦截器过滤，他租户对象→查询返回 null→抛 *_NOT_EXISTS；显式 tenantId 比对此时不触发（对象已被过滤）。
   请判断：① 显式比对是否为**纵深防御**（在 ignore 边界/单测中生效），而非生产唯一防线；② 他租户 FK 在生产
   表现为「不存在」拒绝、在单测表现为「OTHER_TENANT」拒绝，二者均满足 line 488「拒绝」，是否可接受。

5. **自我提权/上限的判定边界**：状态、上限、自我提权均只校验 `createRoleIds`（新授予），非全量 roleIds。
   请核查：① 只校验新授予是否留下「保留既有超管角色」的缺口（攻击者能否先合法获得再保留）；② 自我提权仅拦截
   **新增**（createRoleIds 非空），是否**不影响**合法的自我**降权**（deleteRoleIds）；③ 上限判定依赖
   `isSuperAdminUser(loginUserId)`，其内部 `getUserRoleIdListByUserId` 在生产受租户过滤——切换租户上下文时
   操作者角色可能解析为空→判为非超管，请确认这不会导致**误放行**（供给只授 TENANT_ADMIN，故不触发上限）。

6. **新增错误码段无冲突**：1-002-009-000~004（008=通知公告、011=短信渠道之间为空档）。请核查段号未与既有
   冲突、消息占位符 `{}` 与 `exception(code, targetId)` 传参一致。

7. **夹具覆盖充分性与断言有效性**：9 新增用例（错租户 user/role/dept、批量混入、禁用角色、自我提权、超上限、
   重复授权幂等、assignRoleMenu 错租户角色）。请核查：① 断言用 `assertServiceException(..., 错误码, 参数)`
   校验 code+msg，而非仅「抛异常」；② `MockedStatic<SecurityFrameworkUtils>` 注入 loginUserId 的用例是否正确
   隔离（try-with-resources）；③ 幂等用例是否验证「无新增写入且无报错」；④ @AfterEach 清理 TenantContextHolder
   是否避免 ThreadLocal 泄漏到其它用例；⑤ 拒绝后是否断言关联表**未写入**（userRoleMapper/roleMenuMapper 为空）。

## 4. 已知边界（非缺陷，属分批范围）

- 本子项（.A）仅做**同技术租户**归属校验（line 927「现有技术主体的授权目标/上限校验」）。
- **获准跨组织范围（D-09）**的授权目标/上限映射、两组织互授/越界判定与**正向授权矩阵**归 **ZS-PERM-001.B**
  （line 928，B08 批次，前置 ZS-PERM-001.A + ZS-IAM-002）。
- 「可授予上限」当前实现为**特权角色单列**（非超管不得授予超管）+ 自我提权拦截；更细粒度的「操作者可授予
  集合」（如基于角色层级的授予范围）属业务组织模型范畴，待 .B/D-09。
- 缓存与停用撤权的一致性（角色/菜单缓存、旧 Token 撤权）归 **ZS-PERM-004.A**，本子项不涉及。

## 5. 状态

- [x] 右窗口：开发 + 本地验证（后端 33/33 BUILD SUCCESS）+ 提交（`6eb81717`）+ 回填 05 文档开发记录
- [ ] 左窗口：`codex review --commit 6eb81717` → 产出 `codex-ZS-PERM-001.A.md` + `.raw.md`
- [ ] 左窗口：P0/P1 缺陷回写本文件「处置」段或通知右窗口修复
