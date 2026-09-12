# ZS-IAM-003 codex 评审结论与处置

> 被审提交（r0）：`94a183a5`（ZS-IAM-003 补齐部门与人员引用及层级变更保护，B03/B07；前置 B02；worktree 隔离分支 `feat/login-001`，`--no-ff` 合并 main `b6b1e4a2`——同分支另含 ZS-LOGIN-001，两任务分别评审）。
> r0 P2 修复提交：`2d149a2f`（负责人引用计数在忽略数据权限作用域内执行，2 files +35/-1）。
> 二次复核（r1）：`codex review --commit 2d149a2f`，结论 **CLEAN / 0 发现**。
> 评审工具：`codex-cli 0.154.0`，模型 `gpt-6-astra`，reasoning effort `xhigh`，sandbox `read-only`，approval `never`。
> 完整 stdout：r0 [codex-ZS-IAM-003.raw.md](codex-ZS-IAM-003.raw.md)、r1 [codex-ZS-IAM-003.r1.raw.md](codex-ZS-IAM-003.r1.raw.md)。
> 说明：本任务评审直接对提交执行，未走事前 HANDOFF 交接单；本文件即评审产物 + 处置，遵循 [README.md](README.md)「后续处理约定」。评审弧呈 **r0 揪「负责人引用计数受数据权限过滤」P2 → 用户选定 Path A 现在 TDD 修复（`executeIgnore` 包裹计数、保留租户过滤）→ r1 确认归零**。ZS-IAM-003 为整卡任务（非分批子项），本轮技术层引用保护交付、卡片 待开发→待验收；业务组织语义（任职生命周期与责任历史归属）仍依赖 ZS-IAM-001/D-09，归 ZS-IAM-002/004。

## IAM-003 交付背景

ZS-IAM-003 的目标是「补齐部门与人员引用及层级变更保护」——挂有成员/引用的删除按批准规则处理、并发层级更新不成环、批量与单条具有一致校验和审计。底座现状是 [DeptServiceImpl](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/dept/DeptServiceImpl.java) 删除只显式校验子部门、[AdminUserServiceImpl](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/user/AdminUserServiceImpl.java) 删除会清角色与岗位关联但不校验负责人引用。

本轮以**技术层引用保护**形态交付（`94a183a5`）：

- **部门侧**：新增 `DeptMapper.selectCountByLeaderUserId` + `AdminUserMapper.selectCountByDeptId` 两个计数原语；`DeptServiceImpl` 注入 `AdminUserMapper`（**Mapper 级注入而非 AdminUserService，断服务层循环依赖**），`deleteDept`/`deleteDeptList` 在子部门校验后加「部门下挂有成员则拒删」（新错误码 `DEPT_EXITS_USERS` 1_002_004_005）。
- **人员侧**：`AdminUserServiceImpl` 注入 `DeptMapper`，`deleteUser`/`deleteUserList` 在 `validateUserExists` 后加 `validateUserNotDeptLeader`（新错误码 `USER_IS_DEPT_LEADER` 1_002_003_007）。
- **批量一致性**：单条与批量共用同一校验，批量**先完成全部校验再删除**，避免部分删除。
- 完整任职生命周期与责任历史归属迁移，待 D-09 任职模型后由 ZS-IAM-002/004 承接。

## Codex 原始结论（r0，被审提交 `94a183a5`）

判定发现 **1 项 P2**（无 P0 / P1 / P3）：

> The new leader-reference guard can miss existing references hidden by data permissions, allowing deletion to leave dangling department leaders.

### Review comments

- **[P2] Count leader references outside the caller's data scope** — `services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/user/AdminUserServiceImpl.java:452-455`
  > When a user belongs to visible department A but leads department B outside the caller's data scope, this count excludes B because `DataPermissionConfiguration` registers `DeptDO.id` for data-permission filtering. Both single and batch deletion therefore pass the check and leave B's `leaderUserId` dangling. Run this reference check inside `DataPermissionUtils.executeIgnore`, retaining tenant filtering, and add a regression test with a restricted-scope caller.

## 复核与处置（r0 P2）

### P2-1 — 负责人引用计数受数据权限过滤（✅ 已修复，r1 确认归零）

**复核结论：成立（引用保护自身的完整性缺口）。** [DataPermissionConfiguration](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/framework/datapermission/config/DataPermissionConfiguration.java) 通过 `rule.addDeptColumn(DeptDO.class, "id")` 将 `DeptDO.id` 注册为数据权限过滤列，故 `validateUserNotDeptLeader` 的 `deptMapper.selectCountByLeaderUserId(id)`（查 DeptDO）在生产环境受**调用者数据范围**裁剪。当被删用户在调用者可见部门 A、却担任**范围外部门 B** 的负责人时，B 被数据权限过滤 → 计数漏判为 0 → 单条/批量删除均放行 → B 的 `leaderUserId` 悬空。这恰是 IAM-003 要防的「负责人引用悬空」，保护对范围外引用失效。

**用户选定 Path A（现在 TDD 修复）。修复（commit `2d149a2f`，RED→GREEN，2 files +35/-1）**：

- [AdminUserServiceImpl#validateUserNotDeptLeader](../../services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/user/AdminUserServiceImpl.java) 将计数包进 `DataPermissionUtils.executeIgnore`（`Callable<T>` 重载、返回 `Long`），与同文件 `validateUserForCreateOrUpdate`（L414）既有范式一致：

  ```java
  @VisibleForTesting
  void validateUserNotDeptLeader(Long id) {
      // 关闭数据权限，避免调用者数据范围外的部门（该用户担任负责人）被过滤掉，
      // 导致负责人引用保护漏判、删除用户后 DeptDO.leaderUserId 悬空；租户过滤仍保留
      Long leaderDeptCount = DataPermissionUtils.executeIgnore(() -> deptMapper.selectCountByLeaderUserId(id));
      if (leaderDeptCount > 0) {
          throw exception(USER_IS_DEPT_LEADER);
      }
  }
  ```

- `executeIgnore` 仅在本次引用计数期间压入 `@DataPermission(enable=false)` 标志、`finally` 恢复原上下文，故**只禁用数据权限过滤、租户过滤（MyBatis 租户拦截器）仍保留**，且不泄漏到后续查询。

**RED→GREEN 经验裁决（测试可行性关键）**：`BaseDbUnitTest`（H2）的 `@Import` 链**未装配数据权限拦截器**（仅 `ZszjMybatisAutoConfiguration`/`MybatisPlusAutoConfiguration` 等，无 `ZszjDeptDataPermissionAutoConfiguration`），故无法在 H2 里真复现「受限范围调用者」的 DB 级过滤。因此回归测试改为**直接断言计数查询发生在 `DataPermissionUtils.executeIgnore` 作用域内**（等价于对范围过滤免疫）：新增 `testValidateUserNotDeptLeader_ignoresDataPermission`，用**裸 `new AdminUserServiceImpl()`**（规避 `ZszjDataSourceAutoConfiguration` 的 `@EnableTransactionManagement` 使 Spring 注入的 `userService` 成 CGLIB 代理、反射注入 target 失效）+ `mock(DeptMapper.class)` + `ReflectionTestUtils.setField` 注入，`thenAnswer` 在计数被调用时捕获 `DataPermissionContextHolder.get().enable()`，断言其为 `false`（数据权限已禁用）。

- **RED**（修复前）：计数直接调用 → holder 空 → `get()==null` → 断言 `expected:<true> but was:<false>` FAIL（`Tests run: 1, Failures: 1`，实证计数未在忽略数据权限作用域内）。
- **GREEN**（修复后）：计数包进 `executeIgnore` → holder 栈顶为 disable 注解 → `enable()==false` → 断言 PASS。

**共存复验**：`AdminUserServiceImplTest` 40/0/0（39→40，+1 新测试；含既有 `testDeleteUser_isDeptLeader`/`testDeleteUserList_isDeptLeader` 等 IAM-003 用例不受影响）+ `DeptServiceImplTest` 19/0/0 + `OAuth2TokenServiceImplTest` 17/0/0（同分支 ZS-LOGIN-001），均 `MVN_EXIT=0`、`BUILD SUCCESS`。

## 二次复核（r1，被审提交 `2d149a2f`）

对 r0 P2 修复提交重跑 `codex review --commit 2d149a2f`（`gpt-6-astra`/`xhigh`/`read-only`），结论 **CLEAN，0 发现**（无 P0 / P1 / P2 / P3）：

> No actionable regressions were found. The change disables data-permission filtering only during the reference count, preserves tenant filtering, and restores the prior permission context. Tests were not run in the read-only environment.

codex r1 在 exec trace 中自核 `DeptDataPermissionRule`（`addDeptColumn`/`AndExpression` 构建逻辑）与 `DataPermissionUtilsTest`，独立确认「仅在引用计数期间禁用数据权限过滤、保留租户过滤、恢复先前权限上下文、无新回归」。**r0 的 P2 已消除。**（codex sandbox `read-only` 且 Java/Maven 不在 PATH，两轮均未执行测试，通过性以本机 40/19/17 BUILD SUCCESS 为准。）

## 结论

**评审通过（r0 发现 1×P2 负责人引用计数受数据权限过滤已修，r1 复核 0 发现确认消除）。**

- **r0**（被审 `94a183a5`）：1×P2——`validateUserNotDeptLeader` 的 `selectCountByLeaderUserId` 查 DeptDO 受数据权限过滤（`DataPermissionConfiguration` 注册 `DeptDO.id`），调用者范围外部门漏计 → 删除放行 → 悬空 `leaderUserId`。
- **r0 P2 修复**（`2d149a2f`）：计数包进 `DataPermissionUtils.executeIgnore`（保留租户过滤），+1 回归测试（裸实例 + mock + `ReflectionTestUtils` 断言 holder `enable()==false`）；RED（expected true but false）→ GREEN，共存 40/19/17 BUILD SUCCESS。
- **r1**（被审 `2d149a2f`）：CLEAN，0 发现，codex 自核数据权限规则链独立确认仅计数期禁用过滤、保留租户过滤、恢复上下文、无新回归。
- **本机复验证据**：`AdminUserServiceImplTest` 40/0/0 + `DeptServiceImplTest` 19/0/0 + `OAuth2TokenServiceImplTest` 17/0/0，`MVN_EXIT=0`，`BUILD SUCCESS`。
- **延后 Minor（非阻塞，见 reviews/README「后续处理约定」第 2 条）**：①`deleteDept`/`deleteDeptList` 校验两份拷贝（可选抽 `validateDeptDeletable`，批量须保「先全校验后删」）；②批量错误文案不含部门 id/名（base 模式，改动前端/测试引用消息 → UX 批次）；③验收「停用负责人无提示承担新任务」的字面「停用」路径 `updateUserStatus` 无提示——本轮 scope 到删除侧并交付，disable 提示归 B07/D-09；④`deleteUserList` 缺 `validateUserExists` + 缺 `@LogRecord` 批量日志（base 不对称，登记独立小卡）；⑤`validateParentDept` 并发 TOCTOU（读侧防死循环充分、写侧防环不足 → ZS-DB-019.B/PERM）。业务组织语义（任职生命周期与责任历史归属）归 ZS-IAM-002/004（前置 D-09）。
- codex 发现总数 **1**（1×P2），已修复 **1**，延后 **0**，仍有效阻塞项 **0**。
- **本记录不表示任何主任务已验收。**
