node.exe : OpenAI Codex v0.154.0
所在位置 C:\Users\Administrator\AppData\Roaming\npm\codex.ps1:24 字符: 5
+     & "node$exe"  "$basedir/node_modules/@openai/codex/bin/codex.js"  ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: ultra
reasoning summaries: none
session id: 01a094c5-1e29-7ad0-8140-53405b11c091
--------
user
commit 94a183a
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Location; git status --short; git show --stat --oneline --decorate --no-renames
 94a183a5; Get-ChildItem -Force -Name AGENTS*; if (Test-Path 'E:\\AGENTS.md') {
 Get-Content 'E:\\AGENTS.md' }; if (Test-Path 'E:\\AGENTS.override.md') { Get-C
ontent 'E:\\AGENTS.override.md' }" in E:\众墅之家AI赋能平台底座
 succeeded in 64ms:
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\24
1\214\345\274\200\345\217\221\350\256\241\345\210\222.md"
?? outputs/
94a183a5 (feat/login-001) feat(iam-003): 部门/人员删除引用保护（成员占用 + 负责人悬空）[ZS-IAM-003]
 .../module/system/dal/mysql/dept/DeptMapper.java   | 12 +++++
 .../system/dal/mysql/user/AdminUserMapper.java     | 12 +++++
 .../module/system/enums/ErrorCodeConstants.java    |  2 +
 .../system/service/dept/DeptServiceImpl.java       | 12 ++++-
 .../system/service/user/AdminUserServiceImpl.java  | 28 +++++++++-
 .../system/service/dept/DeptServiceImplTest.java   | 48 +++++++++++++++++
 .../service/user/AdminUserServiceImplTest.java     | 63 ++++++++++++++++++++++
 7 files changed, 174 insertions(+), 3 deletions(-)
Path                     
----                     
E:\����֮��AI����ƽ̨����



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '[Con
sole]::OutputEncoding = [System.Text.Encoding]::UTF8; git rev-parse --show-topl
evel; git log -4 --oneline; git show --format=fuller --no-ext-diff --no-renames
 94a183a5; Get-ChildItem -Force | Select-Object Mode,Name; git config --get cor
e.quotepath; Get-ChildItem -Recurse -File -Force -Include AGENTS.md,AGENTS.over
ride.md -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName'
 in E:\众墅之家AI赋能平台底座
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
ChildItem -Force -Name; rg --files --hidden -g AGENTS.md -g AGENTS.override.md 
-g '"'!node_modules'"' -g '"'!.git'"' -g '"'!target'"' -g '"'!dist'"'; git ls-t
ree -r --name-only 94a183a5 | Select-String -Pattern '("'^|/)(AGENTS('"\\.overr
ide)?\\.md|CLAUDE\\.md|\\.codex/.*)"'$'"'" in E:\众墅之家AI赋能平台底座
 succeeded in 157ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Content -Encoding UTF8 services/zhongshu-core/zszj-module-system/src/main/java/
cn/zszj/module/system/service/dept/DeptServiceImpl.java; Get-Content -Encoding 
UTF8 services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sys
tem/service/user/AdminUserServiceImpl.java' in E:\众墅之家AI赋能平台底座
 succeeded in 3ms:
 succeeded in 42176ms:
1f689831 docs(login-001): ZS-LOGIN-001 收口文档同步（选项 C 部分交付）+ codex r0→r1 两弧评审处置入库 
+ docs/05 V1.27
678bd221 fix(login-001): 门控关闭时拒绝并自愈清除缓存的合成访问令牌，堵 Redis 命中绕过 gate [ZS-LOGIN-001]
b6b1e4a2 Merge branch 'feat/login-001' (ZS-LOGIN-001 + ZS-IAM-003)
8de88edf docs(sec-011-a): ZS-SEC-011.A 收口文档同步 + codex r0→r1→r2→r3 四弧评审处置入库 + do
cs/05 V1.26
commit 94a183a5a110d79f3065ab12ab12bb47f668ae5e
Author:     hlong026 <peihr85@gmail.com>
AuthorDate: Sat Sep 12 09:32:43 2026 +0800
Commit:     hlong026 <peihr85@gmail.com>
CommitDate: Sat Sep 12 09:57:50 2026 +0800

    feat(iam-003): 部门/人员删除引用保护（成员占用 + 负责人悬空）[ZS-IAM-003]

diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/dal/mysql/dept/DeptMapper.java b/services/zhongshu-core/zszj-module
-system/src/main/java/cn/zszj/module/system/dal/mysql/dept/DeptMapper.java
index 8fc9b7e2..97826c8b 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/dal/mysql/dept/DeptMapper.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/dal/mysql/dept/DeptMapper.java
@@ -26,6 +26,18 @@ public interface DeptMapper extends BaseMapperX<DeptDO> {
         return selectCount(DeptDO::getParentId, parentId);
     }
 
+    /**
+     * 统计以指定用户为负责人的部门数量
+     *
+     * 用于删除用户前的引用保护：用户是部门负责人时，禁止删除，避免 {@link DeptDO#getLeaderUserId()} 悬空
+     *
+     * @param leaderUserId 负责人用户编号
+     * @return 部门数量
+     */
+    default Long selectCountByLeaderUserId(Long leaderUserId) {
+        return selectCount(DeptDO::getLeaderUserId, leaderUserId);
+    }
+
     default List<DeptDO> selectListByParentId(Collection<Long> parentIds) {
         return selectList(DeptDO::getParentId, parentIds);
     }
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/dal/mysql/user/AdminUserMapper.java b/services/zhongshu-core/zszj-m
odule-system/src/main/java/cn/zszj/module/system/dal/mysql/user/AdminUserMapper
.java
index 076f6a8b..caca60b2 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/dal/mysql/user/AdminUserMapper.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/dal/mysql/user/AdminUserMapper.java
@@ -54,4 +54,16 @@ public interface AdminUserMapper extends BaseMapperX<AdminUs
erDO> {
         return selectList(AdminUserDO::getDeptId, deptIds);
     }
 
+    /**
+     * 统计指定部门下的用户数量
+     *
+     * 用于删除部门前的引用保护：部门下挂有成员时，禁止删除，避免 {@link AdminUserDO#getDeptId()} 悬空
+     *
+     * @param deptId 部门编号
+     * @return 用户数量
+     */
+    default Long selectCountByDeptId(Long deptId) {
+        return selectCount(AdminUserDO::getDeptId, deptId);
+    }
+
 }
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/enums/ErrorCodeConstants.java b/services/zhongshu-core/zszj-module-
system/src/main/java/cn/zszj/module/system/enums/ErrorCodeConstants.java
index 9619d225..17f90f97 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/enums/ErrorCodeConstants.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/enums/ErrorCodeConstants.java
@@ -42,6 +42,7 @@ public interface ErrorCodeConstants {
     ErrorCode USER_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_002_003_004, "导入用户数据
不能为空！");
     ErrorCode USER_PASSWORD_FAILED = new ErrorCode(1_002_003_005, "用户密码校验失败");
     ErrorCode USER_IS_DISABLE = new ErrorCode(1_002_003_006, "名字为【{}】的用户已被禁用")
;
+    ErrorCode USER_IS_DEPT_LEADER = new ErrorCode(1_002_003_007, "用户是部门负责人，请先变
更负责人再删除");
     ErrorCode USER_COUNT_MAX = new ErrorCode(1_002_003_008, "创建用户失败，原因：超过租户最大租
户配额({})！");
     ErrorCode USER_IMPORT_INIT_PASSWORD = new ErrorCode(1_002_003_009, "初始密码不能
为空");
     ErrorCode USER_MOBILE_NOT_EXISTS = new ErrorCode(1_002_003_010, "该手机号尚未注册"
);
@@ -53,6 +54,7 @@ public interface ErrorCodeConstants {
     ErrorCode DEPT_NOT_FOUND = new ErrorCode(1_002_004_002, "当前部门不存在");
     ErrorCode DEPT_EXITS_CHILDREN = new ErrorCode(1_002_004_003, "存在子部门，无法删除")
;
     ErrorCode DEPT_PARENT_ERROR = new ErrorCode(1_002_004_004, "不能设置自己为父部门");
+    ErrorCode DEPT_EXITS_USERS = new ErrorCode(1_002_004_005, "存在成员，无法删除");
     ErrorCode DEPT_NOT_ENABLE = new ErrorCode(1_002_004_006, "部门({})不处于开启状态，不允
许选择");
     ErrorCode DEPT_PARENT_IS_CHILD = new ErrorCode(1_002_004_007, "不能设置自己的子部门为
父部门");
 
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/service/dept/DeptServiceImpl.java b/services/zhongshu-core/zszj-mod
ule-system/src/main/java/cn/zszj/module/system/service/dept/DeptServiceImpl.jav
a
index c34252d5..309e693c 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/service/dept/DeptServiceImpl.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/service/dept/DeptServiceImpl.java
@@ -9,6 +9,7 @@ import cn.zszj.module.system.controller.admin.dept.vo.dept.Dept
ListReqVO;
 import cn.zszj.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
 import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
 import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
+import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
 import cn.zszj.module.system.dal.redis.RedisKeyConstants;
 import com.google.common.annotations.VisibleForTesting;
 import lombok.extern.slf4j.Slf4j;
@@ -36,6 +37,8 @@ public class DeptServiceImpl implements DeptService {
 
     @Resource
     private DeptMapper deptMapper;
+    @Resource
+    private AdminUserMapper adminUserMapper; // 注入 Mapper 而非 AdminUserService，
避免服务层循环依赖
 
     @Override
     @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
@@ -84,6 +87,10 @@ public class DeptServiceImpl implements DeptService {
         if (deptMapper.selectCountByParentId(id) > 0) {
             throw exception(DEPT_EXITS_CHILDREN);
         }
+        // 校验部门下是否挂有成员，避免删除后 AdminUserDO.deptId 悬空；受控迁移待 D-09 任职模型
+        if (adminUserMapper.selectCountByDeptId(id) > 0) {
+            throw exception(DEPT_EXITS_USERS);
+        }
         // 删除部门
         deptMapper.deleteById(id);
     }
@@ -92,11 +99,14 @@ public class DeptServiceImpl implements DeptService {
     @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
             allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
     public void deleteDeptList(List<Long> ids) {
-        // 校验是否有子部门
+        // 校验是否有子部门、部门下是否挂有成员；与单条删除保持一致，且先完成全部校验再删除，避免部分删除
         for (Long id : ids) {
             if (deptMapper.selectCountByParentId(id) > 0) {
                 throw exception(DEPT_EXITS_CHILDREN);
             }
+            if (adminUserMapper.selectCountByDeptId(id) > 0) {
+                throw exception(DEPT_EXITS_USERS);
+            }
         }
 
         // 批量删除部门
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/service/user/AdminUserServiceImpl.java b/services/zhongshu-core/zsz
j-module-system/src/main/java/cn/zszj/module/system/service/user/AdminUserServi
ceImpl.java
index 9f6ea1a4..450226d6 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/service/user/AdminUserServiceImpl.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/service/user/AdminUserServiceImpl.java
@@ -23,6 +23,7 @@ import cn.zszj.module.system.controller.admin.user.vo.user.Us
erSaveReqVO;
 import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
 import cn.zszj.module.system.dal.dataobject.dept.UserPostDO;
 import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
+import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
 import cn.zszj.module.system.dal.mysql.dept.UserPostMapper;
 import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
 import cn.zszj.module.system.mq.producer.user.AdminUserProducer;
@@ -86,6 +87,9 @@ public class AdminUserServiceImpl implements AdminUserService
 {
     @Resource
     private UserPostMapper userPostMapper;
 
+    @Resource
+    private DeptMapper deptMapper; // 负责人计数走 DeptMapper 原语即可，无需为一次计数在 DeptServ
ice 接口扩方法（本类已注入 DeptService）
+
     @Resource
     private ConfigApi configApi;
 
@@ -271,6 +275,8 @@ public class AdminUserServiceImpl implements AdminUserServi
ce {
     public void deleteUser(Long id) {
         // 1. 校验用户存在
         AdminUserDO user = validateUserExists(id);
+        // 1.1 校验用户不是部门负责人
+        validateUserNotDeptLeader(id);
 
         // 2.1 删除用户
         userMapper.deleteById(id);
@@ -286,10 +292,13 @@ public class AdminUserServiceImpl implements AdminUserSer
vice {
     @Override
     @Transactional(rollbackFor = Exception.class)
     public void deleteUserList(List<Long> ids) {
-        // 1. 批量删除用户
+        // 1. 校验用户都不是部门负责人；与单条删除保持一致，且先完成全部校验再删除，避免部分删除
+        ids.forEach(this::validateUserNotDeptLeader);
+
+        // 2.1 批量删除用户
         userMapper.deleteByIds(ids);
 
-        // 2. 批量删除用户关联数据
+        // 2.2 批量删除用户关联数据
         ids.forEach(id -> {
             permissionService.processUserDeleted(id);
             userPostMapper.deleteByUserId(id);
@@ -431,6 +440,21 @@ public class AdminUserServiceImpl implements AdminUserServ
ice {
         return user;
     }
 
+    /**
+     * 校验用户不是部门负责人，避免删除后 {@link DeptDO#getLeaderUserId()} 悬空
+     *
+     * 说明：此处只做技术层的引用保护，要求先变更负责人再删除用户，不自动级联抹除负责人历史；
+     * 完整的任职生命周期与责任历史归属迁移，待 D-09 任职模型后由 ZS-IAM-002/004 承接
+     *
+     * @param id 用户编号
+     */
+    @VisibleForTesting
+    void validateUserNotDeptLeader(Long id) {
+        if (deptMapper.selectCountByLeaderUserId(id) > 0) {
+            throw exception(USER_IS_DEPT_LEADER);
+        }
+    }
+
     @VisibleForTesting
     void validateUsernameUnique(Long id, String username) {
         if (StrUtil.isBlank(username)) {
diff --git a/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/mo
dule/system/service/dept/DeptServiceImplTest.java b/services/zhongshu-core/zszj
-module-system/src/test/java/cn/zszj/module/system/service/dept/DeptServiceImpl
Test.java
index 18a15aad..ccf89648 100644
--- a/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/sy
stem/service/dept/DeptServiceImplTest.java
+++ b/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/sy
stem/service/dept/DeptServiceImplTest.java
@@ -6,7 +6,10 @@ import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
 import cn.zszj.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
 import cn.zszj.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
 import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
+import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
 import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
+import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
+import cn.zszj.module.system.enums.common.SexEnum;
 import org.junit.jupiter.api.Test;
 import org.springframework.context.annotation.Import;
 
@@ -34,6 +37,8 @@ public class DeptServiceImplTest extends BaseDbUnitTest {
     private DeptServiceImpl deptService;
     @Resource
     private DeptMapper deptMapper;
+    @Resource
+    private AdminUserMapper adminUserMapper;
 
     @Test
     public void testCreateDept() {
@@ -141,6 +146,49 @@ public class DeptServiceImplTest extends BaseDbUnitTest {
         assertServiceException(() -> deptService.deleteDeptList(ids), DEPT_EXI
TS_CHILDREN);
     }
 
+    @Test
+    public void testDeleteDept_exitsUsers() {
+        // mock 数据：部门下挂有成员
+        DeptDO dbDeptDO = randomPojo(DeptDO.class);
+        deptMapper.insert(dbDeptDO);// @Sql: 先插入出一条存在的数据
+        AdminUserDO memberUser = randomPojo(AdminUserDO.class, o -> {
+            o.setStatus(randomCommonStatus()); // 保证 status 的范围
+            o.setSex(SexEnum.MALE.getSex()); // 保证 sex 的范围
+            o.setDeptId(dbDeptDO.getId());
+        });
+        adminUserMapper.insert(memberUser);
+        // 准备参数
+        Long id = dbDeptDO.getId();
+
+        // 调用, 并断言异常
+        assertServiceException(() -> deptService.deleteDept(id), DEPT_EXITS_US
ERS);
+        // 校验部门未被删除
+        assertNotNull(deptMapper.selectById(id));
+    }
+
+    @Test
+    public void testDeleteDeptList_exitsUsers() {
+        // mock 数据：其中一个部门下挂有成员
+        DeptDO deptDO1 = randomPojo(DeptDO.class);
+        deptMapper.insert(deptDO1);
+        AdminUserDO memberUser = randomPojo(AdminUserDO.class, o -> {
+            o.setStatus(randomCommonStatus()); // 保证 status 的范围
+            o.setSex(SexEnum.MALE.getSex()); // 保证 sex 的范围
+            o.setDeptId(deptDO1.getId());
+        });
+        adminUserMapper.insert(memberUser);
+        DeptDO deptDO2 = randomPojo(DeptDO.class);
+        deptMapper.insert(deptDO2);
+        // 准备参数
+        List<Long> ids = Arrays.asList(deptDO1.getId(), deptDO2.getId());
+
+        // 调用, 并断言异常（批量删除前先完成全部校验，避免部分删除）
+        assertServiceException(() -> deptService.deleteDeptList(ids), DEPT_EXI
TS_USERS);
+        // 校验两个部门都未被删除
+        assertNotNull(deptMapper.selectById(deptDO1.getId()));
+        assertNotNull(deptMapper.selectById(deptDO2.getId()));
+    }
+
     @Test
     public void testValidateParentDept_parentError() {
         // 准备参数
diff --git a/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/mo
dule/system/service/user/AdminUserServiceImplTest.java b/services/zhongshu-core
/zszj-module-system/src/test/java/cn/zszj/module/system/service/user/AdminUserS
erviceImplTest.java
index 58c55a9f..5487418a 100644
--- a/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/sy
stem/service/user/AdminUserServiceImplTest.java
+++ b/services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/sy
stem/service/user/AdminUserServiceImplTest.java
@@ -21,6 +21,7 @@ import cn.zszj.module.system.dal.dataobject.dept.PostDO;
 import cn.zszj.module.system.dal.dataobject.dept.UserPostDO;
 import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
 import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
+import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
 import cn.zszj.module.system.dal.mysql.dept.UserPostMapper;
 import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
 import cn.zszj.module.system.enums.common.SexEnum;
@@ -70,6 +71,8 @@ public class AdminUserServiceImplTest extends BaseDbUnitTest 
{
     private AdminUserMapper userMapper;
     @Resource
     private UserPostMapper userPostMapper;
+    @Resource
+    private DeptMapper deptMapper;
 
     @MockitoBean
     private DeptService deptService;
@@ -305,6 +308,66 @@ public class AdminUserServiceImplTest extends BaseDbUnitTe
st {
         verify(permissionService, times(1)).processUserDeleted(eq(userId));
     }
 
+    @Test
+    public void testDeleteUser_isDeptLeader() {
+        // mock 数据
+        AdminUserDO dbUser = randomAdminUserDO();
+        userMapper.insert(dbUser);
+        // mock 数据：用户是部门负责人
+        DeptDO dbDept = randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser
.getId()));
+        deptMapper.insert(dbDept);
+        // 准备参数
+        Long userId = dbUser.getId();
+
+        // 调用数据, 并断言异常
+        assertServiceException(() -> userService.deleteUser(userId), USER_IS_D
EPT_LEADER);
+        // 校验用户未被删除
+        assertNotNull(userMapper.selectById(userId));
+        // 校验未触发用户关联数据的清理
+        verify(permissionService, never()).processUserDeleted(any());
+    }
+
+    @Test
+    public void testDeleteUserList_success() {
+        // mock 数据：两个用户都不是部门负责人
+        AdminUserDO dbUser1 = randomAdminUserDO();
+        userMapper.insert(dbUser1);
+        AdminUserDO dbUser2 = randomAdminUserDO();
+        userMapper.insert(dbUser2);
+        // 准备参数
+        List<Long> ids = newArrayList(dbUser1.getId(), dbUser2.getId());
+
+        // 调用数据
+        userService.deleteUserList(ids);
+        // 校验结果
+        assertNull(userMapper.selectById(dbUser1.getId()));
+        assertNull(userMapper.selectById(dbUser2.getId()));
+        // 校验调用次数
+        verify(permissionService, times(1)).processUserDeleted(eq(dbUser1.getI
d()));
+        verify(permissionService, times(1)).processUserDeleted(eq(dbUser2.getI
d()));
+    }
+
+    @Test
+    public void testDeleteUserList_isDeptLeader() {
+        // mock 数据：其中一个用户是部门负责人
+        AdminUserDO dbUser1 = randomAdminUserDO();
+        userMapper.insert(dbUser1);
+        DeptDO dbDept = randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser
1.getId()));
+        deptMapper.insert(dbDept);
+        AdminUserDO dbUser2 = randomAdminUserDO();
+        userMapper.insert(dbUser2);
+        // 准备参数
+        List<Long> ids = newArrayList(dbUser1.getId(), dbUser2.getId());
+
+        // 调用数据, 并断言异常（批量删除前先完成全部校验，避免部分删除）
+        assertServiceException(() -> userService.deleteUserList(ids), USER_IS_
DEPT_LEADER);
+        // 校验两个用户都未被删除
+        assertNotNull(userMapper.selectById(dbUser1.getId()));
+        assertNotNull(userMapper.selectById(dbUser2.getId()));
+        // 校验未触发用户关联数据的清理
+        verify(permissionService, never()).processUserDeleted(any());
+    }
+
     @Test
     public void testGetUserByUsername() {
         // mock 数据

Mode   Name                  
----   ----                  
d--h-- .git                  
d----- .github               
d----- .omx                  
d----- .tmp-iam003           
d----- .tmp-sec011           
d----- apps                  
d----- docs                  
d----- outputs               
d----- reference             
d----- scripts               
d----- services              
d----- third_party           
d----- tools                 
-a---- .gitignore            
-a---- README.md             
-a---- THIRD_PARTY_NOTICES.md



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
grep -n -E 'DeptDataPermissionRuleCustomizer|addDeptColumn|addUserColumn|system
_dept|system_users' 94a183a5 -- services/zhongshu-core/zszj-module-system/src/m
ain/java/cn/zszj/module/system/framework; git show 94a183a5:services/zhongshu-c
ore/zszj-module-system/src/main/java/cn/zszj/module/system/dal/mysql/dept/DeptM
apper.java; git show 94a183a5:services/zhongshu-core/zszj-module-system/src/mai
n/java/cn/zszj/module/system/dal/mysql/user/AdminUserMapper.java; git show 94a1
83a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sys
tem/controller/admin/dept/DeptController.java; git show 94a183a5:services/zhong
shu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admi
n/user/UserController.java; git show 94a183a5:services/zhongshu-core/zszj-modul
e-system/src/test/java/cn/zszj/module/system/service/user/AdminUserServiceImplT
est.java | Select-Object -First 140; git diff 94a183a5 HEAD -- services/zhongsh
u-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/dept/Dept
ServiceImpl.java services/zhongshu-core/zszj-module-system/src/main/java/cn/zsz
j/module/system/service/user/AdminUserServiceImpl.java" in E:\众墅之家AI赋能平台底座
 succeeded in 93ms:
package cn.zszj.module.system.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.apilog.core.annotation.ApiAccessLog;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageParam;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.excel.core.util.ExcelUtils;
import cn.zszj.module.system.controller.admin.user.vo.user.*;
import cn.zszj.module.system.convert.user.UserConvert;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Collections;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertL
ist;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertS
et;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqV
O) {
        Long id = userService.createUser(reqVO);
        return success(id);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO r
eqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "102
4")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @Operation(summary = "批量删除用户")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUserList(@RequestParam("ids") List<Long>
 ids) {
        userService.deleteUserList(ids);
        return success(true);
    }

    @PutMapping("/update-password")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpd
atePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdat
eStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqV
O pageReqVO) {
        // 获得用户分页列表
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO)
;
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(pageResult.getList(), AdminUserDO::getDeptId));
        return success(new PageResult<>(UserConvert.INSTANCE.convertList(pageRe
sult.getList(), deptMap),
                pageResult.getTotal()));
    }

    @GetMapping("/list")
    @Operation(summary = "获得用户详情列表")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "
[1024]")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<List<UserRespVO>> getUserList(@RequestParam("ids") List
<Long> ids) {
        List<AdminUserDO> list = userService.getUserList(ids);
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(convertSet(list, Adm
inUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含被开启的用户，主要用于前端的下拉选项")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList(
            @RequestParam(value = "deptId", required = false) Long deptId) {
        List<AdminUserDO> list = userService.getUserListByStatus(
                CommonStatusEnum.ENABLE.getStatus(), deptId);

        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertSimpleList(list, deptMap));
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "102
4")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        return success(UserConvert.INSTANCE.convert(user, dept));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Validated UserPageReqVO exportReqVO,
                               HttpServletResponse response) throws IOException
 {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList()
;
        // 输出 Excel
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class,
                UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException
 {
        // 手动创建导出 demo
        List<UserImportExcelVO> list = Arrays.asList(
                UserImportExcelVO.builder().username("yunai").deptId(1L).email(
"yunai@iocoder.cn").mobile("15601691300")
                        .nickname("众墅之家").status(CommonStatusEnum.ENABLE.getSta
tus()).sex(SexEnum.MALE.getSex()).build(),
                UserImportExcelVO.builder().username("yuanma").deptId(2L).email
("yuanma@iocoder.cn").mobile("15601701300")
                        .nickname("源码").status(CommonStatusEnum.DISABLE.getStat
us()).sex(SexEnum.FEMALE.getSex()).build()
        );
        // 输出
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserImportExcelVO.clas
s, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true
),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false"
, example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") Mul
tipartFile file,
                                                      @RequestParam(value = "up
dateSupport", required = false, defaultValue = "false") Boolean updateSupport) 
throws Exception {
        List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.
class);
        return success(userService.importUserList(list, updateSupport));
    }

    // ==================== 免鉴权接口（用于 IM 点头像弹名片、加好友搜索等场景） ====================

    @GetMapping("/get-simple")
    @Operation(summary = "获得用户精简信息", description = "用于点头像弹名片等场景；免鉴权")
    @Parameter(name = "id", description = "用户编号", required = true, example = "1
024")
    public CommonResult<UserSimpleRespVO> getSimpleUser(@RequestParam("id") Lon
g id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDe
ptId()) : null;
        Map<Long, DeptDO> deptMap = dept != null ? Collections.singletonMap(dep
t.getId(), dept) : Collections.emptyMap();
        return success(CollUtil.getFirst(UserConvert.INSTANCE.convertSimpleList
(
                Collections.singletonList(user), deptMap)));
    }

    @GetMapping("/list-by-nickname")
    @Operation(summary = "按昵称模糊搜索用户精简信息", description = "用于加好友等场景；免鉴权；当前仅按昵称匹配"
)
    @Parameter(name = "nickname", description = "昵称关键词", required = true, examp
le = "众墅之家")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserListByNickname(@Re
questParam("nickname") String nickname) {
        if (StrUtil.isBlank(nickname)) {
            return success(Collections.emptyList());
        }
        // 拼接数据
        List<AdminUserDO> list = userService.getUserListByNickname(nickname.tri
m());
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(convertList(list, Ad
minUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertSimpleList(list, deptMap));
    }

}
package cn.zszj.module.system.service.user;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.collection.ArrayUtils;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.api.config.ConfigApi;
import cn.zszj.module.infra.api.file.FileApi;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdate
PasswordReqVO;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdate
ReqVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.dept.PostDO;
import cn.zszj.module.system.dal.dataobject.dept.UserPostDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
import cn.zszj.module.system.dal.mysql.dept.UserPostMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.mq.producer.user.AdminUserProducer;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.dept.PostService;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.common.util.collection.SetUtils.asSet;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildBetwee
nTime;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.zszj.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceExcepti
on;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static cn.zszj.module.system.service.user.AdminUserServiceImpl.USER_INIT
_PASSWORD_KEY;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(AdminUserServiceImpl.class)
public class AdminUserServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AdminUserServiceImpl userService;

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private DeptMapper deptMapper;

    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private PostService postService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private FileApi fileApi;
    @MockitoBean
    private ConfigApi configApi;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;
    @MockitoBean
    private AdminUserProducer adminUserProducer;

    @BeforeEach
    public void before() {
        // mock 初始化密�?        when(configApi.getConfigValueByKey(USER_INIT_PASS
WORD_KEY)).thenReturn("zszjyuanma");
    }

    @Test
    public void testCreatUser_success() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setPostIds(asSet(1L, 2L));
        }).setId(null); // 避免 id 被赋�?        // mock 账户额度充足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(1))
;
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));
        // mock deptService 的方�?        DeptDO dept = randomPojo(DeptDO.class, 
o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock postService 的方�?        List<PostDO> posts = CollectionUtils.co
nvertList(reqVO.getPostIds(), postId ->
                randomPojo(PostDO.class, o -> {
                    o.setId(postId);
                    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
                }));
        when(postService.getPostList(eq(reqVO.getPostIds()), isNull())).thenRet
urn(posts);
        // mock passwordEncoder 的方�?        when(passwordEncoder.encode(eq(reqV
O.getPassword()))).thenReturn("zszjyuanma");

        // 调用
        Long userId = userService.createUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user, "password", "id");
        assertEquals("zszjyuanma", user.getPassword());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus());
        // 断言关联岗位
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.get
Id());
        assertEquals(1L, userPosts.get(0).getPostId());
        assertEquals(2L, userPosts.get(1).getPostId());
    }

    @Test

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$dir
s = @('"'.', 'services','services/zhongshu-core','services/zhongshu-core/zszj-m
odule-system'); "'$changed = git diff-tree --no-commit-id --name-only -r 94a183
a5; foreach ($f in $changed) { $dir = Split-Path $f; while ($dir) { $dirs += $d
ir; $dir = Split-Path $dir } }; $dirs | Sort-Object -Unique | ForEach-Object { 
foreach ($name in @('"'AGENTS.override.md','AGENTS.md','.codex/config.toml')) {
 "'$p = Join-Path $_ $name; if (Test-Path -LiteralPath $p) { Write-Output "--- 
$p"; Get-Content -Encoding UTF8 -LiteralPath $p } } }; git show 94a183a5:servic
es/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/framewo
rk/datapermission/config/DataPermissionConfiguration.java; git show 94a183a5:se
rvices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-data-permissio
n/src/main/java/cn/zszj/framework/datapermission/core/rule/dept/DeptDataPermiss
ionRule.java; git show 94a183a5:services/zhongshu-core/zszj-framework/zszj-spri
ng-boot-starter-biz-data-permission/src/main/java/cn/zszj/framework/datapermiss
ion/core/db/DataPermissionDatabaseInterceptor.java' in E:\众墅之家AI赋能平台底座
 exited 1 in 57ms:
package cn.zszj.module.system.framework.datapermission.config;

import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCu
stomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的数据权限 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class DataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer sysDeptDataPermissionRuleCustomizer
() {
        return rule -> {
            // dept
            rule.addDeptColumn(AdminUserDO.class);
            rule.addDeptColumn(DeptDO.class, "id");
            // user
            rule.addUserColumn(AdminUserDO.class, "id");
        };
    }

}
package cn.zszj.framework.datapermission.core.rule.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRes
pDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRule;
import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.framework.mybatis.core.util.MyBatisUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionL
ist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 基于部门的 {@link DataPermissionRule} 数据权限规则实现
 *
 * 注意，使用 DeptDataPermissionRule 时，需要保证表中有 dept_id 部门编号的字段，可自定义。
 *
 * 实际业务场景下，会存在一个经典的问题？当用户修改部门时，冗余的 dept_id 是否需要修改？
 * 1. 一般情况下，dept_id 不进行修改，则会导致用户看不到之前的数据。【zszj-server 采用该方案】
 * 2. 部分情况下，希望该用户还是能看到之前的数据，则有两种方式解决：【需要你改造该 DeptDataPermissionRule 的实现代码】
 *  1）编写洗数据的脚本，将 dept_id 修改成新部门的编号；【建议】
 *      最终过滤条件是 WHERE dept_id = ?
 *  2）洗数据的话，可能涉及的数据量较大，也可以采用 user_id 进行过滤的方式，此时需要获取到 dept_id 对应的所有 user_id 用户编号
；
 *      最终过滤条件是 WHERE user_id IN (?, ?, ? ...)
 *  3）想要保证原 dept_id 和 user_id 都可以看的到，此时使用 dept_id 和 user_id 一起过滤；
 *      最终过滤条件是 WHERE dept_id = ? OR user_id IN (?, ?, ? ...)
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Slf4j
public class DeptDataPermissionRule implements DataPermissionRule {

    /**
     * LoginUser 的 Context 缓存 Key
     */
    protected static final String CONTEXT_KEY = DeptDataPermissionRule.class.ge
tSimpleName();

    private static final String DEPT_COLUMN_NAME = "dept_id";
    private static final String USER_COLUMN_NAME = "user_id";

    private final PermissionCommonApi permissionApi;

    /**
     * 基于部门的表字段配置
     * 一般情况下，每个表的部门编号字段是 dept_id，通过该配置自定义。
     *
     * key：表名
     * value：字段名
     */
    private final Map<String, String> deptColumns = new HashMap<>();
    /**
     * 基于用户的表字段配置
     * 一般情况下，每个表的部门编号字段是 dept_id，通过该配置自定义。
     *
     * key：表名
     * value：字段名
     */
    private final Map<String, String> userColumns = new HashMap<>();
    /**
     * 所有表名，是 {@link #deptColumns} 和 {@link #userColumns} 的合集
     */
    private final Set<String> TABLE_NAMES = new HashSet<>();

    @Override
    public Set<String> getTableNames() {
        return TABLE_NAMES;
    }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        // 只有有登陆用户的情况下，才进行数据权限的处理
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        // 只有管理员类型的用户，才进行数据权限的处理
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.get
Value())) {
            return null;
        }

        // 获得数据权限
        DeptDataPermissionRespDTO deptDataPermission = loginUser.getContext(CON
TEXT_KEY, DeptDataPermissionRespDTO.class);
        // 从上下文中拿不到，则调用逻辑进行获取
        if (deptDataPermission == null) {
            deptDataPermission = permissionApi.getDeptDataPermission(loginUser.
getId());
            if (deptDataPermission == null) {
                log.error("[getExpression][LoginUser({}) 获取数据权限为 null]", JsonUt
ils.toJsonString(loginUser));
                throw new NullPointerException(String.format("LoginUser(%d) Tab
le(%s/%s) 未返回数据权限",
                        loginUser.getId(), tableName, tableAlias.getName()));
            }
            // 添加到上下文中，避免重复计算
            loginUser.setContext(CONTEXT_KEY, deptDataPermission);
        }

        // 情况一，如果是 ALL 可查看全部，则无需拼接条件
        if (deptDataPermission.getAll()) {
            return null;
        }

        // 情况二，即不能查看部门，又不能查看自己，则说明 100% 无权限
        if (CollUtil.isEmpty(deptDataPermission.getDeptIds())
            && Boolean.FALSE.equals(deptDataPermission.getSelf())) {
            return new EqualsTo(null, null); // WHERE null = null，可以保证返回的数据为空
        }

        // 情况三，拼接 Dept 和 User 的条件，最后组合
        Expression deptExpression = buildDeptExpression(tableName,tableAlias, d
eptDataPermission.getDeptIds());
        Expression userExpression = buildUserExpression(tableName, tableAlias, 
deptDataPermission.getSelf(), loginUser.getId());
        if (deptExpression == null && userExpression == null) {
            // TODO 芋艿：获得不到条件的时候，暂时不抛出异常，而是不返回数据
            log.warn("[getExpression][LoginUser({}) Table({}/{}) DeptDataPermis
sion({}) 构建的条件为空]",
                    JsonUtils.toJsonString(loginUser), tableName, tableAlias, J
sonUtils.toJsonString(deptDataPermission));
//            throw new NullPointerException(String.format("LoginUser(%d) Table
(%s/%s) 构建的条件为空",
//                    loginUser.getId(), tableName, tableAlias.getName()));
            return new EqualsTo(null, null); // WHERE null = null，可以保证返回的数据为空
        }
        if (deptExpression == null) {
            return userExpression;
        }
        if (userExpression == null) {
            return deptExpression;
        }
        // 目前，如果有指定部门 + 可查看自己，采用 OR 条件。即，WHERE (dept_id IN ? OR user_id = ?)
        return new ParenthesedExpressionList(new OrExpression(deptExpression, u
serExpression));
    }

    private Expression buildDeptExpression(String tableName, Alias tableAlias, 
Set<Long> deptIds) {
        // 如果不存在配置，则无需作为条件
        String columnName = deptColumns.get(tableName);
        if (StrUtil.isEmpty(columnName)) {
            return null;
        }
        // 如果为空，则无条件
        if (CollUtil.isEmpty(deptIds)) {
            return null;
        }
        // 拼接条件
        return new InExpression(MyBatisUtils.buildColumn(tableName, tableAlias,
 columnName),
                // Parenthesis 的目的，是提供 (1,2,3) 的 () 左右括号
                new ParenthesedExpressionList(new ExpressionList<LongValue>(Col
lectionUtils.convertList(deptIds, LongValue::new))));
    }

    private Expression buildUserExpression(String tableName, Alias tableAlias, 
Boolean self, Long userId) {
        // 如果不查看自己，则无需作为条件
        if (Boolean.FALSE.equals(self)) {
            return null;
        }
        String columnName = userColumns.get(tableName);
        if (StrUtil.isEmpty(columnName)) {
            return null;
        }
        // 拼接条件
        return new EqualsTo(MyBatisUtils.buildColumn(tableName, tableAlias, col
umnName), new LongValue(userId));
    }

    // ==================== 添加配置 ====================

    public void addDeptColumn(Class<? extends BaseDO> entityClass) {
        addDeptColumn(entityClass, DEPT_COLUMN_NAME);
    }

    public void addDeptColumn(Class<? extends BaseDO> entityClass, String colum
nName) {
        String tableName = TableInfoHelper.getTableInfo(entityClass).getTableNa
me();
       addDeptColumn(tableName, columnName);
    }

    public void addDeptColumn(String tableName, String columnName) {
        deptColumns.put(tableName, columnName);
        TABLE_NAMES.add(tableName);
    }

    public void addUserColumn(Class<? extends BaseDO> entityClass) {
        addUserColumn(entityClass, USER_COLUMN_NAME);
    }

    public void addUserColumn(Class<? extends BaseDO> entityClass, String colum
nName) {
        String tableName = TableInfoHelper.getTableInfo(entityClass).getTableNa
me();
        addUserColumn(tableName, columnName);
    }

    public void addUserColumn(String tableName, String columnName) {
        userColumns.put(tableName, columnName);
        TABLE_NAMES.add(tableName);
    }

}
fatal: path 'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz
-data-permission/src/main/java/cn/zszj/framework/datapermission/core/db/DataPer
missionDatabaseInterceptor.java' does not exist in '94a183a5'

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
ls-tree -r --name-only 94a183a5 services/zhongshu-core/zszj-framework/zszj-spri
ng-boot-starter-biz-data-permission; git grep -n -E 'AdminUserServiceImpl.class
|new AdminUserServiceImpl|new DeptServiceImpl|DeptServiceImpl.class|selectCount
ByDeptId|selectCountByLeaderUserId|1_002_003_007|1_002_004_005' 94a183a5 -- ser
vices; git show 94a183a5:services/zhongshu-core/zszj-module-system/src/main/jav
a/cn/zszj/module/system/service/permission/PermissionServiceImpl.java | Select-
String -Pattern 'getDeptDataPermission' -Context 10,105; git show 94a183a5:serv
ices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-data-permission/
src/main/java/cn/zszj/framework/datapermission/config/ZszjDataPermissionAutoCon
figuration.java; git show 94a183a5:services/zhongshu-core/zszj-framework/zszj-s
pring-boot-starter-biz-data-permission/src/main/java/cn/zszj/framework/dataperm
ission/core/db/DataPermissionRuleHandler.java" in E:\众墅之家AI赋能平台底座
 succeeded in 205ms:
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/dept/DeptMapper.java:37:    default Long selectCountByLeaderU
serId(Long leaderUserId) {
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/user/AdminUserMapper.java:65:    default Long selectCountByDe
ptId(Long deptId) {
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/enums/ErrorCodeConstants.java:45:    ErrorCode USER_IS_DEPT_LEADER = ne
w ErrorCode(1_002_003_007, "用户是部门负责人，请先变更负责人再删除");
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/enums/ErrorCodeConstants.java:57:    ErrorCode DEPT_EXITS_USERS = new E
rrorCode(1_002_004_005, "存在成员，无法删除");
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/dept/DeptServiceImpl.java:91:        if (adminUserMapper.select
CountByDeptId(id) > 0) {
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/dept/DeptServiceImpl.java:107:            if (adminUserMapper.s
electCountByDeptId(id) > 0) {
94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/user/AdminUserServiceImpl.java:453:        if (deptMapper.selec
tCountByLeaderUserId(id) > 0) {
94a183a5:services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module
/system/service/dept/DeptServiceImplTest.java:33:@Import(DeptServiceImpl.class)
94a183a5:services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module
/system/service/user/AdminUserServiceImplTest.java:64:@Import(AdminUserServiceI
mpl.class)

  
      @Override
      public void assignRoleDataScope(Long roleId, Integer dataScope, Set<Long>
 dataScopeDeptIds) {
          // ZS-PERM-001.A：校验角色与数据权限部门归属，防止篡改他租户 角色/部门 ID
          validateRoleForAssign(roleId);
          validateDeptListForAssign(dataScopeDeptIds);
          roleService.updateRoleDataScope(roleId, dataScope, dataScopeDeptIds);
      }
  
      @Override
>     @DataPermission(enable = false) // 关闭数据权限，不然就会出现递归获取数据权限的问�?    pu
blic DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
          // 获得用户的角�?        List<RoleDO> roles = getEnableUserRoleListByUserId
FromCache(userId);
  
          // 如果角色为空，则只能查看自己
          DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
          if (CollUtil.isEmpty(roles)) {
              result.setSelf(true);
              return result;
          }
  
          // 获得用户的部门编号的缓存，通过 Guava �?Suppliers 惰性求值，即有且仅有第一次发�
?DB 的查�?        Supplier<Long> userDeptId = Suppliers.memoize(() -> userService
.getUser(userId).getDeptId());
          // 遍历每个角色，计�?        for (RoleDO role : roles) {
              // 为空时，跳过
              if (role.getDataScope() == null) {
                  continue;
              }
              // 情况一，ALL
              if (Objects.equals(role.getDataScope(), DataScopeEnum.ALL.getScop
e())) {
                  result.setAll(true);
                  continue;
              }
              // 情况二，DEPT_CUSTOM
              if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM
.getScope())) {
                  CollUtil.addAll(result.getDeptIds(), role.getDataScopeDeptIds
());
                  // 自定义可见部门时，保证可以看到自己所在的部门。否则，一些场景下可能�
�有问题�?                // 例如说，登录时，基于 t_user �?username 查询会可能被 dept_id 过滤�?      
          CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.get());
                  continue;
              }
              // 情况三，DEPT_ONLY
              if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.g
etScope())) {
                  CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.
get());
                  continue;
              }
              // 情况四，DEPT_DEPT_AND_CHILD
              if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_AND_CH
ILD.getScope())) {
                  Long deptId = userDeptId.get();
                  // 用户未设置部门，直接跳过；否�?getChildDeptIdListFromCache 走缓存注解会�?null k
ey
 报错
                  if (deptId == null) {
                      continue;
                  }
                  CollUtil.addAll(result.getDeptIds(), deptService.getChildDept
IdListFromCache(deptId));
                  // 添加本身部门编号
                  result.getDeptIds().add(deptId);
                  continue;
              }
              // 情况五，SELF
              if (Objects.equals(role.getDataScope(), DataScopeEnum.SELF.getSco
pe())) {
                  result.setSelf(true);
                  continue;
              }
              // 未知情况，error log 即可
>             log.error("[getDeptDataPermission][LoginUser({}) role({}) 无法处理]",
 userId, toJsonString(result));
          }
          return result;
      }
  
      // ========== ZS-PERM-001.A 授权目标归属与上限校�? ==========
  
      /**
       * 校验被授权用户：必须存在，且归属当前技术租户�?     *
       * 防止篡改他租户用�?ID 进行授权�?     */
      private void validateUserForAssign(Long userId) {
          AdminUserDO user = userService.getUser(userId);
          if (user == null) {
              throw exception(USER_NOT_EXISTS);
          }
          validateTenantScope(user.getTenantId(), userId, PERMISSION_ASSIGN_USE
R_OTHER_TENANT);
      }
  
      /**
       * 校验被授权角色集合：每个角色必须存在、归属当前技术租户；新授予的角色还必须处�
�开启状态�?     *
       * 防止篡改他租户角�?ID、批量混入无效或越权角色。状态仅校验新授予的角色�?     * �
�保证「重复授权幂等」时不会因为既有角色的状态变化而报错�?     */
      private void validateRolesForAssign(Collection<Long> roleIds, Collection<
Long> createRoleIds) {
          if (CollUtil.isEmpty(roleIds)) {
              return;
          }
          Map<Long, RoleDO> roleMap = convertMap(roleService.getRoleList(roleId
s), RoleDO::getId);
          Set<Long> createSet = new HashSet<>();
          if (CollUtil.isNotEmpty(createRoleIds)) {
              createSet.addAll(createRoleIds);
          }
          roleIds.forEach(roleId -> {
              RoleDO role = roleMap.get(roleId);
              // 存在性：角色不存在（含被租户过滤掉的他租户角色）则拒�?            if (role == nul
l) {
                  throw exception(ROLE_NOT_EXISTS);
              }
              // 归属：显式比对租户，不能只凭请求 tenant_id 为关系表填值就认定外键安全
              validateTenantScope(role.getTenantId(), roleId, PERMISSION_ASSIGN
_ROLE_OTHER_TENANT);
              // 状态：仅对新授予的角色校验，禁用角色不可被授予
              if (createSet.contains(roleId) && !CommonStatusEnum.ENABLE.getSta
tus().equals(role.getStatus())) {
                  throw exception(ROLE_IS_DISABLE, role.getName());
              }
          });
      }
  
      /**
       * 校验单个被授权角色：必须存在，且归属当前技术租户�?     */
      private void validateRoleForAssign(Long roleId) {
          RoleDO role = roleService.getRole(roleId);
          if (role == null) {
              throw exception(ROLE_NOT_EXISTS);
          }
          validateTenantScope(role.getTenantId(), roleId, PERMISSION_ASSIGN_ROL
E_OTHER_TENANT);
      }
  
      /**
       * 校验数据权限部门集合：每个部门必须存在，且归属当前技术租户�?     */
      private void validateDeptListForAssign(Collection<Long> deptIds) {
          if (CollUtil.isEmpty(deptIds)) {
              return;
          }
          Map<Long, DeptDO> deptMap = convertMap(deptService.getDeptList(deptId
s), DeptDO::getId);
          deptIds.forEach(deptId -> {
              DeptDO dept = deptMap.get(deptId);
              if (dept == null) {
                  throw exception(DEPT_NOT_FOUND);
              }
              validateTenantScope(dept.getTenantId(), deptId, PERMISSION_ASSIGN
_DEPT_OTHER_TENANT);
          });
      }
  
      /**
       * 校验授予上限与自我提权（仅针对新授予的角色）�?     *
       * 非超级管理员：既不能授予超级管理员等特权角色，也不能为自身新增角色�?     * 无�
��录上下文（如系统内部/租户供给路径）时跳过，仅保留归属校验；超级管理员保留完整管理
能力�?     */
      private void validateUserRoleGrantCeiling(Long userId, Collection<Long> c
reateRoleIds) {
          // 无新增授予时无需校验，保证重复授权幂�?        if (CollUtil.isEmpty(createRoleIds)) {
              return;
          }
          Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
          // 无登录上下文（系统内部调�?租户供给）时，跳过上限与自我提权校验
          if (loginUserId == null) {
              return;
          }
          // 超级管理员保留完整管理能�?        if (isSuperAdminUser(loginUserId)) {
              return;
          }
          // 上限：非超管不得授予超管等特权角�?        if (roleService.hasAnySuperAdmin(createR
oleIds)) {
              throw exception(PERMISSION_GRANT_EXCEED_CEILING);
          }
          // 自我提权：非超管不得为自身新增角�?        if (loginUserId.equals(userId)) {
              throw exception(PERMISSION_SELF_ELEVATION);
          }
      }
  
      /**
       * 判断用户是否为超级管理员�?     */
      private boolean isSuperAdminUser(Long userId) {
          Set<Long> roleIds = getUserRoleIdListByUserId(userId);
          return CollUtil.isNotEmpty(roleIds) && roleService.hasAnySuperAdmin(r
oleIds);
      }
  
      /**
       * 校验目标对象的租户归属：必须属于当前技术租户�?     *
       * 仅在存在明确技术租户上下文、且未忽略租户时执行。租户供给路径通过
       * {@link cn.zszj.framework.tenant.core.util.TenantUtils#execute} 设置具体租户且
不忽略�?     * 故新
建用�?角色的归属天然满足校验；系统级忽略租户的操作则跳过�?     */
      private void validateTenantScope(Long targetTenantId, Long targetId, Erro
rCode errorCode) {
          if (!isTenantScopeValidationEnabled()) {
package cn.zszj.framework.datapermission.config;

import cn.zszj.framework.datapermission.core.aop.DataPermissionAnnotationAdviso
r;
import cn.zszj.framework.datapermission.core.db.DataPermissionRuleHandler;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRule;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRuleFactory;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl
;
import cn.zszj.framework.mybatis.core.util.MyBatisUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionIntercept
or;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 数据权限的自动配置类
 *
 * @author 芋道源码
 */
@AutoConfiguration
public class ZszjDataPermissionAutoConfiguration {

    @Bean
    public DataPermissionRuleFactory dataPermissionRuleFactory(List<DataPermiss
ionRule> rules) {
        return new DataPermissionRuleFactoryImpl(rules);
    }

    @Bean
    public DataPermissionRuleHandler dataPermissionRuleHandler(MybatisPlusInter
ceptor interceptor,
                                                               DataPermissionRu
leFactory ruleFactory) {
        // 创建 DataPermissionInterceptor 拦截器
        DataPermissionRuleHandler handler = new DataPermissionRuleHandler(ruleF
actory);
        DataPermissionInterceptor inner = new DataPermissionInterceptor(handler
);
        // 添加到 interceptor 中
        // 需要加在首个，主要是为了在分页插件前面。这个是 MyBatis Plus 的规定
        MyBatisUtils.addInterceptor(interceptor, inner, 0);
        return handler;
    }

    @Bean
    public DataPermissionAnnotationAdvisor dataPermissionAnnotationAdvisor() {
        return new DataPermissionAnnotationAdvisor();
    }

}
package cn.zszj.framework.datapermission.core.db;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRule;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRuleFactory;
import cn.zszj.framework.mybatis.core.util.MyBatisUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHa
ndler;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Table;

import java.util.List;

import static cn.zszj.framework.security.core.util.SecurityFrameworkUtils.skipP
ermissionCheck;

/**
 * 基于 {@link DataPermissionRule} 的数据权限处理器
 *
 * 它的底层，是基于 MyBatis Plus 的 <a href="https://baomidou.com/plugins/data-permissio
n/">数据权限插件</a>
 * 核心原理：它会在 SQL 执行前拦截 SQL 语句，并根据用户权限动态添加权限相关的 SQL 片段。这样，只有用户有权限访问的数据才会被查询出来
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
public class DataPermissionRuleHandler implements MultiDataPermissionHandler {

    private final DataPermissionRuleFactory ruleFactory;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappe
dStatementId) {
        // 特殊：跨租户访问
        if (skipPermissionCheck()) {
            return null;
        }

        // 获得 Mapper 对应的数据权限的规则
        List<DataPermissionRule> rules = ruleFactory.getDataPermissionRule(mapp
edStatementId);
        if (CollUtil.isEmpty(rules)) {
            return null;
        }

        // 生成条件
        Expression allExpression = null;
        for (DataPermissionRule rule : rules) {
            // 判断表名是否匹配
            String tableName = MyBatisUtils.getTableName(table);
            if (!rule.getTableNames().contains(tableName)) {
                continue;
            }

            // 单条规则的条件
            Expression oneExpress = rule.getExpression(tableName, table.getAlia
s());
            if (oneExpress == null) {
                continue;
            }
            // 拼接到 allExpression 中
            allExpression = allExpression == null ? oneExpress
                    : new AndExpression(allExpression, oneExpress);
        }
        return allExpression;
    }

}



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
show 94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/m
odule/system/service/user/AdminUserServiceImpl.java | findstr /n /c:"validateUs
erNotDeptLeader" /c:"selectCountByLeaderUserId" /c:"USER_IS_DEPT_LEADER"; git s
how 94a183a5:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz
-data-permission/src/main/java/cn/zszj/framework/datapermission/core/util/DataP
ermissionUtils.java; git show 94a183a5:services/zhongshu-core/zszj-module-syste
m/src/main/java/cn/zszj/module/system/dal/dataobject/dept/DeptDO.java; git show
 94a183a5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/modul
e/system/controller/admin/dept/vo/dept/DeptSaveReqVO.java; git show 94a183a5:se
rvices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/java
/cn/zszj/framework/test/core/ut/BaseDbUnitTest.java' in E:\众墅之家AI赋能平台底座
 succeeded in 38ms:
package cn.zszj.module.system.controller.admin.dept.vo.dept;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "管理后台 - 部门创建/修改 Request VO")
@Data
public class DeptSaveReqVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, 
example = "众墅之家")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 30, message = "部门名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "父部门 ID", example = "1024")
    private Long parentId;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, 
example = "1024")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "联系电话", example = "15601691000")
    @Size(max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    @Schema(description = "邮箱", example = "zszj@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", requiredMode = Schema.Req
uiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "修改状态必须是 {value}")
    private Integer status;

}
package cn.zszj.framework.test.core.ut;

import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.framework.datasource.config.ZszjDataSourceAutoConfiguration;
import cn.zszj.framework.mybatis.config.ZszjMybatisAutoConfiguration;
import cn.zszj.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigur
e;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManager
AutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * 依赖内存 DB 的单元测试
 *
 * 注意，Service 层同样适用。对于 Service 层的单元测试，我们针对自己模块的 Mapper 走的是 H2 内存数据库，针对别的模块的 Ser
vice 走的是 Mock 方法
 *
 * @author 芋道源码
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = 
BaseDbUnitTest.Application.class)
@ActiveProfiles("unit-test") // 设置使用 application-unit-test 配置文件
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST
_METHOD) // 每个单元测试结束后，清理 DB
public class BaseDbUnitTest {

    @Import({
            // DB 配置类
            ZszjDataSourceAutoConfiguration.class, // 自己的 DB 配置类
            DataSourceAutoConfiguration.class, // Spring DB 自动配置类
            DataSourceTransactionManagerAutoConfiguration.class, // Spring 事务自动
配置类
            DruidDataSourceAutoConfigure.class, // Druid 自动配置类
            SqlInitializationTestConfiguration.class, // SQL 初始化
            // MyBatis 配置类
            ZszjMybatisAutoConfiguration.class, // 自己的 MyBatis 配置类
            MybatisPlusAutoConfiguration.class, // MyBatis 的自动配置类
            MybatisPlusJoinAutoConfiguration.class, // MyBatis 的Join配置类

            // 其它配置类
            SpringUtil.class
    })
    public static class Application {
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n -C 4 'void validateUserNotDeptLeader|selectCountByLeaderUserId' services/zhon
gshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/user/A
dminUserServiceImpl.java; git grep -n -E 'IAM-003|ZS-IAM-003' 94a183a5 -- docs 
':"'!docs/04*'"' ':"'!docs/05*'"' | Select-Object -First 25; git status --short
" in E:\众墅之家AI赋能平台底座
 succeeded in 52ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\24
1\214\345\274\200\345\217\221\350\256\241\345\210\222.md"
?? outputs/

codex
The new leader-reference guard can miss existing references hidden by data perm
issions, allowing deletion to leave dangling department leaders.

Review comment:

- [P2] Count leader references outside the caller's data scope — E:/众墅之家AI赋能平台底
座/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system
/service/user/AdminUserServiceImpl.java:452-455
  When a user belongs to visible department A but leads department B outside th
e caller's data scope, this count excludes B because `DataPermissionConfigurati
on` registers `DeptDO.id` for data-permission filtering. Both single and batch 
deletion therefore pass the check and leave B's `leaderUserId` dangling. Run th
is reference check inside `DataPermissionUtils.executeIgnore`, retaining tenant
 filtering, and add a regression test with a restricted-scope caller.
The new leader-reference guard can miss existing references hidden by data permissions, allowing deletion to leave dangling department leaders.

Review comment:

- [P2] Count leader references outside the caller's data scope — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/user/AdminUserServiceImpl.java:452-455
  When a user belongs to visible department A but leads department B outside the caller's data scope, this count excludes B because `DataPermissionConfiguration` registers `DeptDO.id` for data-permission filtering. Both single and batch deletion therefore pass the check and leave B's `leaderUserId` dangling. Run this reference check inside `DataPermissionUtils.executeIgnore`, retaining tenant filtering, and add a regression test with a restricted-scope caller.
