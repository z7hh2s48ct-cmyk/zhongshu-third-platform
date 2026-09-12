package cn.zszj.module.system.service.user;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.collection.ArrayUtils;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.datapermission.core.annotation.DataPermission;
import cn.zszj.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.api.config.ConfigApi;
import cn.zszj.module.infra.api.file.FileApi;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.common.util.collection.SetUtils.asSet;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.zszj.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.zszj.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static cn.zszj.module.system.service.user.AdminUserServiceImpl.USER_INIT_PASSWORD_KEY;
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
        // mock 初始化密码
        when(configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY)).thenReturn("zszjyuanma");
    }

    @Test
    public void testCreatUser_success() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setPostIds(asSet(1L, 2L));
        }).setId(null); // 避免 id 被赋值
        // mock 账户额度充足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock postService 的方法
        List<PostDO> posts = CollectionUtils.convertList(reqVO.getPostIds(), postId ->
                randomPojo(PostDO.class, o -> {
                    o.setId(postId);
                    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
                }));
        when(postService.getPostList(eq(reqVO.getPostIds()), isNull())).thenReturn(posts);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq(reqVO.getPassword()))).thenReturn("zszjyuanma");

        // 调用
        Long userId = userService.createUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user, "password", "id");
        assertEquals("zszjyuanma", user.getPassword());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus());
        // 断言关联岗位
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.getId());
        assertEquals(1L, userPosts.get(0).getPostId());
        assertEquals(2L, userPosts.get(1).getPostId());
    }

    @Test
    public void testCreatUser_max() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class);
        // mock 账户额度不足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(-1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));

        // 调用，并断言异常
        assertServiceException(() -> userService.createUser(reqVO), USER_COUNT_MAX, -1);
    }

    @Test
    public void testUpdateUser_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPostIds(asSet(1L, 2L)));
        userMapper.insert(dbUser);
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(1L));
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(2L));
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setId(dbUser.getId());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setPostIds(asSet(2L, 3L));
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock postService 的方法
        List<PostDO> posts = CollectionUtils.convertList(reqVO.getPostIds(), postId ->
                randomPojo(PostDO.class, o -> {
                    o.setId(postId);
                    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
                }));
        when(postService.getPostList(eq(reqVO.getPostIds()), isNull())).thenReturn(posts);

        // 调用
        userService.updateUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, user, "password");
        // 断言关联岗位
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.getId());
        assertEquals(2L, userPosts.get(0).getPostId());
        assertEquals(3L, userPosts.get(1).getPostId());
    }

    @Test
    public void testUpdateUserLogin() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setLoginDate(null));
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String loginIp = randomString();

        // 调用
        userService.updateUserLogin(id, loginIp);
        // 断言
        AdminUserDO dbUser = userMapper.selectById(id);
        assertEquals(loginIp, dbUser.getLoginIp());
        assertNotNull(dbUser.getLoginDate());
    }

    @Test
    public void testUpdateUserProfile_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdateReqVO reqVO = randomPojo(UserProfileUpdateReqVO.class, o -> {
            o.setMobile(randomString());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setAvatar(randomURL());
        });

        // 调用
        userService.updateUserProfile(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user);
    }

    @Test
    public void testUpdateUserPassword_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("tudou");
            o.setNewPassword("yuanma");
        });
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(true);

        // 调用
        userService.updateUserPassword(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:yuanma", user.getPassword());
    }

    @Test
    public void testUpdateUserPassword02_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        String password = "zszj";
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));

        // 调用
        userService.updateUserPassword(userId, password);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:" + password, user.getPassword());
    }

    @Test
    public void testUpdateUserStatus() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        Integer status = CommonStatusEnum.DISABLE.getStatus();

        // 调用
        userService.updateUserStatus(userId, status);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals(status, user.getStatus());
        verify(oauth2TokenService).removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
    }

    @Test
    public void testDeleteUser_success(){
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用数据
        userService.deleteUser(userId);
        // 校验结果
        assertNull(userMapper.selectById(userId));
        // 校验调用次数
        verify(permissionService, times(1)).processUserDeleted(eq(userId));
    }

    @Test
    public void testDeleteUser_isDeptLeader() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // mock 数据：用户是部门负责人
        DeptDO dbDept = randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser.getId()));
        deptMapper.insert(dbDept);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用数据, 并断言异常
        assertServiceException(() -> userService.deleteUser(userId), USER_IS_DEPT_LEADER);
        // 校验用户未被删除
        assertNotNull(userMapper.selectById(userId));
        // 校验未触发用户关联数据的清理
        verify(permissionService, never()).processUserDeleted(any());
    }

    @Test
    public void testDeleteUserList_success() {
        // mock 数据：两个用户都不是部门负责人
        AdminUserDO dbUser1 = randomAdminUserDO();
        userMapper.insert(dbUser1);
        AdminUserDO dbUser2 = randomAdminUserDO();
        userMapper.insert(dbUser2);
        // 准备参数
        List<Long> ids = newArrayList(dbUser1.getId(), dbUser2.getId());

        // 调用数据
        userService.deleteUserList(ids);
        // 校验结果
        assertNull(userMapper.selectById(dbUser1.getId()));
        assertNull(userMapper.selectById(dbUser2.getId()));
        // 校验调用次数
        verify(permissionService, times(1)).processUserDeleted(eq(dbUser1.getId()));
        verify(permissionService, times(1)).processUserDeleted(eq(dbUser2.getId()));
    }

    @Test
    public void testDeleteUserList_isDeptLeader() {
        // mock 数据：其中一个用户是部门负责人
        AdminUserDO dbUser1 = randomAdminUserDO();
        userMapper.insert(dbUser1);
        DeptDO dbDept = randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser1.getId()));
        deptMapper.insert(dbDept);
        AdminUserDO dbUser2 = randomAdminUserDO();
        userMapper.insert(dbUser2);
        // 准备参数
        List<Long> ids = newArrayList(dbUser1.getId(), dbUser2.getId());

        // 调用数据, 并断言异常（批量删除前先完成全部校验，避免部分删除）
        assertServiceException(() -> userService.deleteUserList(ids), USER_IS_DEPT_LEADER);
        // 校验两个用户都未被删除
        assertNotNull(userMapper.selectById(dbUser1.getId()));
        assertNotNull(userMapper.selectById(dbUser2.getId()));
        // 校验未触发用户关联数据的清理
        verify(permissionService, never()).processUserDeleted(any());
    }

    @Test
    public void testValidateUserNotDeptLeader_ignoresDataPermission() {
        // 回归 codex ZS-IAM-003 P2：负责人引用计数必须在“忽略数据权限”作用域内执行。
        // DataPermissionConfiguration 将 DeptDO.id 注册为数据权限列，若计数受调用者数据范围过滤，
        // 则“用户在可见部门 A、却担任范围外部门 B 的负责人”时，B 被过滤 -> 计数漏判 -> 删除放行 -> B.leaderUserId 悬空。
        // 说明：BaseDbUnitTest 未装配数据权限拦截器，无法在 H2 里真复现范围过滤，
        // 故此处直接断言计数查询发生在 DataPermissionUtils.executeIgnore 作用域内（等价于对范围过滤免疫）。
        AdminUserServiceImpl targetService = new AdminUserServiceImpl();
        DeptMapper mockDeptMapper = mock(DeptMapper.class);
        AtomicBoolean dataPermissionDisabled = new AtomicBoolean(false);
        when(mockDeptMapper.selectCountByLeaderUserId(anyLong())).thenAnswer(invocation -> {
            DataPermission dataPermission = DataPermissionContextHolder.get();
            dataPermissionDisabled.set(dataPermission != null && !dataPermission.enable());
            return 0L;
        });
        ReflectionTestUtils.setField(targetService, "deptMapper", mockDeptMapper);

        // 调用
        targetService.validateUserNotDeptLeader(1L);

        // 断言：负责人引用计数在忽略数据权限的作用域内执行
        assertTrue(dataPermissionDisabled.get(),
                "负责人引用计数应在 DataPermissionUtils.executeIgnore 作用域内执行，避免被调用者数据权限过滤");
        // 清理数据权限上下文，避免污染其它用例
        DataPermissionContextHolder.clear();
    }

    @Test
    public void testGetUserByUsername() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String username = dbUser.getUsername();

        // 调用
        AdminUserDO user = userService.getUserByUsername(username);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserByMobile() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String mobile = dbUser.getMobile();

        // 调用
        AdminUserDO user = userService.getUserByMobile(mobile);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserPage() {
        // mock 数据
        AdminUserDO dbUser = initGetUserPageData();
        // 准备参数
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setUsername("tu");
        reqVO.setMobile("1560");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2020, 12, 1, 2020, 12, 24));
        reqVO.setDeptId(1L); // 其中，1L 是 2L 的父部门
        // mock 方法
        List<DeptDO> deptList = newArrayList(randomPojo(DeptDO.class, o -> o.setId(2L)));
        when(deptService.getChildDeptList(eq(reqVO.getDeptId()))).thenReturn(deptList);

        // 调用
        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbUser, pageResult.getList().get(0));
    }

    /**
     * 初始化 getUserPage 方法的测试数据
     */
    private AdminUserDO initGetUserPageData() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> { // 等会查询到
            o.setUsername("tudou");
            o.setMobile("15601691300");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2020, 12, 12));
            o.setDeptId(2L);
        });
        userMapper.insert(dbUser);
        // 测试 username 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setUsername("dou")));
        // 测试 mobile 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setMobile("18818260888")));
        // 测试 status 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 createTime 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setCreateTime(buildTime(2020, 11, 11))));
        // 测试 dept 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(0L)));
        return dbUser;
    }

    @Test
    public void testGetUser() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用
        AdminUserDO user = userService.getUser(userId);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserListByDeptIds() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setDeptId(1L));
        userMapper.insert(dbUser);
        // 测试 deptId 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(2L)));
        // 准备参数
        Collection<Long> deptIds = singleton(1L);

        // 调用
        List<AdminUserDO> list = userService.getUserListByDeptIds(deptIds);
        // 断言
        assertEquals(1, list.size());
        assertEquals(dbUser, list.get(0));
    }

    /**
     * 情况一，校验不通过，导致插入失败
     */
    @Test
    public void testImportUserList_01() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock 方法，模拟失败
        doThrow(new ServiceException(DEPT_NOT_FOUND)).when(deptService).validateDeptList(any());

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(DEPT_NOT_FOUND.getMsg(), respVO.getFailureUsernames().get(importUser.getUsername()));
    }

    /**
     * 情况二，不存在，进行插入
     */
    @Test
    public void testImportUserList_02() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq("zszjyuanma"))).thenReturn("java");

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(1, respVO.getCreateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getCreateUsernames().get(0));
        assertPojoEquals(importUser, user);
        assertEquals("java", user.getPassword());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    /**
     * 情况三，存在，但是不强制更新
     */
    @Test
    public void testImportUserList_03() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setUsername(dbUser.getUsername());
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), false);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(USER_USERNAME_EXISTS.getMsg(), respVO.getFailureUsernames().get(importUser.getUsername()));
    }

    /**
     * 情况四，存在，强制更新
     */
    @Test
    public void testImportUserList_04() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setUsername(dbUser.getUsername());
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(1, respVO.getUpdateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getUpdateUsernames().get(0));
        assertPojoEquals(importUser, user);
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    @Test
    public void testValidateUserExists_notExists() {
        assertServiceException(() -> userService.validateUserExists(randomLongId()), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForCreate() {
        // 准备参数
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(null, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(id, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForCreate() {
        // 准备参数
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(null, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(id, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForCreate() {
        // 准备参数
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(null, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(id, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateOldPassword_notExists() {
        assertServiceException(() -> userService.validateOldPassword(randomLongId(), randomString()),
                USER_NOT_EXISTS);
    }

    @Test
    public void testValidateOldPassword_passwordFailed() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String oldPassword = user.getPassword();

        // 调用，校验异常
        assertServiceException(() -> userService.validateOldPassword(id, oldPassword),
                USER_PASSWORD_FAILED);
        // 校验调用
        verify(passwordEncoder, times(1)).matches(eq(oldPassword), eq(user.getPassword()));
    }

    @Test
    public void testUserListByPostIds() {
        // 准备参数
        Collection<Long> postIds = asSet(10L, 20L);
        // mock user1 数据
        AdminUserDO user1 = randomAdminUserDO(o -> o.setPostIds(asSet(10L, 30L)));
        userMapper.insert(user1);
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(10L));
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(30L));
        // mock user2 数据
        AdminUserDO user2 = randomAdminUserDO(o -> o.setPostIds(singleton(100L)));
        userMapper.insert(user2);
        userPostMapper.insert(new UserPostDO().setUserId(user2.getId()).setPostId(100L));

        // 调用
        List<AdminUserDO> result = userService.getUserListByPostIds(postIds);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));
    }

    @Test
    public void testGetUserList() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        List<AdminUserDO> result = userService.getUserList(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserMap() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        Map<Long, AdminUserDO> result = userService.getUserMap(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(user.getId()));
    }

    @Test
    public void testGetUserListByNickname() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setNickname("芋头"));
        userMapper.insert(user);
        // 测试 nickname 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setNickname("源码")));
        // 准备参数
        String nickname = "芋";

        // 调用
        List<AdminUserDO> result = userService.getUserListByNickname(nickname);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserListByStatus() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        userMapper.insert(user);
        // 测试 status 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus())));
        // 准备参数
        Integer status = CommonStatusEnum.DISABLE.getStatus();

        // 调用
        List<AdminUserDO> result = userService.getUserListByStatus(status);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserListByStatusAndDeptId() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(1L));
        userMapper.insert(user);
        // 测试 status 不匹配
        userMapper.insert(cloneIgnoreId(user, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 deptId 不匹配
        userMapper.insert(cloneIgnoreId(user, o -> o.setDeptId(2L)));

        // 调用
        List<AdminUserDO> result = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus(), 1L);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testValidateUserList_success() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.ENABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用，无需断言
        userService.validateUserList(ids);
    }

    @Test
    public void testValidateUserList_notFound() {
        // 准备参数
        List<Long> ids = singletonList(randomLongId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUserList_notEnable() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.DISABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_IS_DISABLE,
                userDO.getNickname());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static AdminUserDO randomAdminUserDO(Consumer<AdminUserDO>... consumers) {
        Consumer<AdminUserDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
        };
        return randomPojo(AdminUserDO.class, ArrayUtils.append(consumer, consumers));
    }

}
