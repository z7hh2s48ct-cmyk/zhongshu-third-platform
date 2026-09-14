package cn.zszj.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.permission.MenuDO;
import cn.zszj.module.system.dal.dataobject.permission.RoleDO;
import cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.zszj.module.system.dal.dataobject.permission.UserRoleDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.zszj.module.system.dal.mysql.permission.UserRoleMapper;
import cn.zszj.module.system.enums.permission.DataScopeEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static cn.hutool.core.collection.ListUtil.toList;
import static cn.zszj.framework.common.util.collection.SetUtils.asSet;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import({PermissionServiceImpl.class})
public class PermissionServiceTest extends BaseDbUnitTest {

    @BeforeEach
    public void setUpMenuExistenceStub() {
        // ZS-CFG-003.B GAP-3 修复后 assignRoleMenu 显式校验菜单存在性；默认桩=入参 ID 全视为存在，
        // 存在性拒绝由专项用例 testAssignRoleMenu_menuNotExists 覆盖
        lenient().when(menuService.getMenuList(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<Long> ids = inv.getArgument(0);
            java.util.List<cn.zszj.module.system.dal.dataobject.permission.MenuDO> menus = new java.util.ArrayList<>();
            if (ids != null) {
                for (Long id : ids) {
                    cn.zszj.module.system.dal.dataobject.permission.MenuDO m =
                            new cn.zszj.module.system.dal.dataobject.permission.MenuDO();
                    m.setId(id);
                    menus.add(m);
                }
            }
            return menus;
        });
    }

    @Resource
    private PermissionServiceImpl permissionService;

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private cn.zszj.module.system.dal.mysql.tenant.TenantMapper tenantMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService userService;

    @AfterEach
    public void tearDownTenantContext() {
        // 清理租户上下文，避免 ThreadLocal 泄漏到其它用例
        TenantContextHolder.clear();
    }

    @Test
    public void testHasAnyPermissions_superAdmin() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            String[] roles = new String[]{"system:user:query", "system:user:create"};
            // mock 用户登录的角色
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(100L));
            RoleDO role = randomPojo(RoleDO.class, o -> o.setId(100L)
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(100L)))).thenReturn(toList(role));
            // mock 其它方法（ZS-PERM-001.A 小卡：超管豁免语义改走启用状态判定）
            when(roleService.hasAnyEnabledSuperAdmin(eq(asSet(100L)))).thenReturn(true);

            // 调用，并断言
            assertTrue(permissionService.hasAnyPermissions(userId, roles));
        }
    }

    @Test
    public void testHasAnyPermissions_normal() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            String[] roles = new String[]{"system:user:query", "system:user:create"};
            // mock 用户登录的角色
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(100L));
            RoleDO role = randomPojo(RoleDO.class, o -> o.setId(100L)
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(100L)))).thenReturn(toList(role));
            // mock 菜单
            Long menuId = 1000L;
            when(menuService.getMenuIdListByPermissionFromCache(
                    eq("system:user:create"))).thenReturn(singletonList(menuId));
            roleMenuMapper.insert(randomPojo(RoleMenuDO.class).setRoleId(100L).setMenuId(1000L));

            // 调用，并断言
            assertTrue(permissionService.hasAnyPermissions(userId, roles));
        }
    }

    @Test
    public void testHasAnyRoles() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            String[] roles = new String[]{"yunai", "tudou"};
            // mock 用户与角色的缓存
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(100L));
            RoleDO role = randomPojo(RoleDO.class, o -> o.setId(100L).setCode("tudou")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(100L)))).thenReturn(toList(role));

            // 调用，并断言
            assertTrue(permissionService.hasAnyRoles(userId, roles));
        }
    }

    // ========== 角色-菜单的相关方法  ==========

    @Test
    public void testAssignRoleMenu() {
        // 准备参数
        Long roleId = 1L;
        Set<Long> menuIds = asSet(200L, 300L);
        // mock 当前技术租户上下文 + 角色归属（ZS-PERM-001.A）
        TenantContextHolder.setTenantId(100L);
        when(roleService.getRole(eq(roleId))).thenReturn(randomPojo(RoleDO.class, o -> {
            o.setId(roleId);
            o.setTenantId(100L);
        }));
        // ZS-CFG-003.B：授权入口现校验租户套餐子集——植入租户 100（系统租户 packageId=0，菜单全量）使原断言路径可达
        cn.zszj.module.system.dal.dataobject.tenant.TenantDO tenant =
                randomPojo(cn.zszj.module.system.dal.dataobject.tenant.TenantDO.class);
        tenant.setId(100L).setPackageId(cn.zszj.module.system.dal.dataobject.tenant.TenantDO.PACKAGE_ID_SYSTEM);
        tenantMapper.insert(tenant);
        // mock 数据
        RoleMenuDO roleMenu01 = randomPojo(RoleMenuDO.class).setRoleId(1L).setMenuId(100L);
        roleMenuMapper.insert(roleMenu01);
        RoleMenuDO roleMenu02 = randomPojo(RoleMenuDO.class).setRoleId(1L).setMenuId(200L);
        roleMenuMapper.insert(roleMenu02);

        // 调用
        permissionService.assignRoleMenu(roleId, menuIds);
        // 断言
        List<RoleMenuDO> roleMenuList = roleMenuMapper.selectList();
        assertEquals(2, roleMenuList.size());
        assertEquals(1L, roleMenuList.get(0).getRoleId());
        assertEquals(200L, roleMenuList.get(0).getMenuId());
        assertEquals(1L, roleMenuList.get(1).getRoleId());
        assertEquals(300L, roleMenuList.get(1).getMenuId());
    }

    @Test
    public void testAssignRoleMenu_roleOtherTenant_rejected() {
        // 准备参数：当前技术租户为 100，被授权角色归属他租户 200
        Long roleId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(roleService.getRole(eq(roleId))).thenReturn(randomPojo(RoleDO.class, o -> {
            o.setId(roleId);
            o.setTenantId(200L); // 他租户
        }));

        // 调用，并断言拒绝
        assertServiceException(() -> permissionService.assignRoleMenu(roleId, asSet(200L)),
                PERMISSION_ASSIGN_ROLE_OTHER_TENANT, roleId);
        // 未写入任何关联
        assertTrue(CollUtil.isEmpty(roleMenuMapper.selectListByRoleId(roleId)));
    }

    @Test
    public void testProcessRoleDeleted() {
        // 准备参数
        Long roleId = randomLongId();
        // mock 数据 UserRole
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setRoleId(roleId)); // 被删除
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO userRoleDO02 = randomPojo(UserRoleDO.class); // 不被删除
        userRoleMapper.insert(userRoleDO02);
        // mock 数据 RoleMenu
        RoleMenuDO roleMenuDO01 = randomPojo(RoleMenuDO.class, o -> o.setRoleId(roleId)); // 被删除
        roleMenuMapper.insert(roleMenuDO01);
        RoleMenuDO roleMenuDO02 = randomPojo(RoleMenuDO.class); // 不被删除
        roleMenuMapper.insert(roleMenuDO02);

        // 调用
        permissionService.processRoleDeleted(roleId);
        // 断言数据 RoleMenuDO
        List<RoleMenuDO> dbRoleMenus = roleMenuMapper.selectList();
        assertEquals(1, dbRoleMenus.size());
        assertPojoEquals(dbRoleMenus.get(0), roleMenuDO02);
        // 断言数据 UserRoleDO
        List<UserRoleDO> dbUserRoles = userRoleMapper.selectList();
        assertEquals(1, dbUserRoles.size());
        assertPojoEquals(dbUserRoles.get(0), userRoleDO02);
    }

    @Test
    public void testProcessMenuDeleted() {
        // 准备参数
        Long menuId = randomLongId();
        // mock 数据
        RoleMenuDO roleMenuDO01 = randomPojo(RoleMenuDO.class, o -> o.setMenuId(menuId)); // 被删除
        roleMenuMapper.insert(roleMenuDO01);
        RoleMenuDO roleMenuDO02 = randomPojo(RoleMenuDO.class); // 不被删除
        roleMenuMapper.insert(roleMenuDO02);

        // 调用
        permissionService.processMenuDeleted(menuId);
        // 断言数据
        List<RoleMenuDO> dbRoleMenus = roleMenuMapper.selectList();
        assertEquals(1, dbRoleMenus.size());
        assertPojoEquals(dbRoleMenus.get(0), roleMenuDO02);
    }

    @Test
    public void testGetRoleMenuIds_superAdmin() {
        // 准备参数
        Long roleId = 100L;
        // mock 方法（ZS-PERM-001.A 小卡：菜单全量豁免改走启用状态判定）
        when(roleService.hasAnyEnabledSuperAdmin(eq(singleton(100L)))).thenReturn(true);
        List<MenuDO> menuList = singletonList(randomPojo(MenuDO.class).setId(1L));
        when(menuService.getMenuList()).thenReturn(menuList);

        // 调用
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(roleId);
        // 断言
        assertEquals(singleton(1L), menuIds);
    }

    @Test
    public void testGetRoleMenuIds_normal() {
        // 准备参数
        Long roleId = 100L;
        // mock 数据
        RoleMenuDO roleMenu01 = randomPojo(RoleMenuDO.class).setRoleId(100L).setMenuId(1L);
        roleMenuMapper.insert(roleMenu01);
        RoleMenuDO roleMenu02 = randomPojo(RoleMenuDO.class).setRoleId(100L).setMenuId(2L);
        roleMenuMapper.insert(roleMenu02);

        // 调用
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(roleId);
        // 断言
        assertEquals(asSet(1L, 2L), menuIds);
    }

    @Test
    public void testGetMenuRoleIdListByMenuIdFromCache() {
        // 准备参数
        Long menuId = 1L;
        // mock 数据
        RoleMenuDO roleMenu01 = randomPojo(RoleMenuDO.class).setRoleId(100L).setMenuId(1L);
        roleMenuMapper.insert(roleMenu01);
        RoleMenuDO roleMenu02 = randomPojo(RoleMenuDO.class).setRoleId(200L).setMenuId(1L);
        roleMenuMapper.insert(roleMenu02);

        // 调用
        Set<Long> roleIds = permissionService.getMenuRoleIdListByMenuIdFromCache(menuId);
        // 断言
        assertEquals(asSet(100L, 200L), roleIds);
    }

    // ========== 用户-角色的相关方法  ==========

    @Test
    public void testAssignUserRole() {
        // 准备参数
        Long userId = 1L;
        Set<Long> roleIds = asSet(200L, 300L);
        // mock 当前技术租户上下文 + 用户/角色归属（ZS-PERM-001.A）
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(100L);
        }));
        when(roleService.getRoleList(any())).thenReturn(toList(
                randomPojo(RoleDO.class, o -> { o.setId(200L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); }),
                randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));
        // mock 数据
        UserRoleDO userRole01 = randomPojo(UserRoleDO.class).setUserId(1L).setRoleId(100L);
        userRoleMapper.insert(userRole01);
        UserRoleDO userRole02 = randomPojo(UserRoleDO.class).setUserId(1L).setRoleId(200L);
        userRoleMapper.insert(userRole02);

        // 调用
        permissionService.assignUserRole(userId, roleIds);
        // 断言
        List<UserRoleDO> userRoleDOList = userRoleMapper.selectList();
        assertEquals(2, userRoleDOList.size());
        assertEquals(1L, userRoleDOList.get(0).getUserId());
        assertEquals(200L, userRoleDOList.get(0).getRoleId());
        assertEquals(1L, userRoleDOList.get(1).getUserId());
        assertEquals(300L, userRoleDOList.get(1).getRoleId());
    }

    @Test
    public void testAssignUserRole_userOtherTenant_rejected() {
        // 准备参数：当前技术租户为 100，被授权用户归属他租户 200
        Long userId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(200L); // 他租户
        }));

        // 调用，并断言拒绝
        assertServiceException(() -> permissionService.assignUserRole(userId, asSet(300L)),
                PERMISSION_ASSIGN_USER_OTHER_TENANT, userId);
        assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(userId)));
    }

    @Test
    public void testAssignUserRole_roleOtherTenant_rejected() {
        // 准备参数：当前技术租户为 100，被授权角色归属他租户 200
        Long userId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(100L);
        }));
        when(roleService.getRoleList(any())).thenReturn(toList(
                randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(200L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));

        // 调用，并断言拒绝
        assertServiceException(() -> permissionService.assignUserRole(userId, asSet(300L)),
                PERMISSION_ASSIGN_ROLE_OTHER_TENANT, 300L);
        assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(userId)));
    }

    @Test
    public void testAssignUserRole_batchMixed_rejected() {
        // 准备参数：批量混入——一个本租户合法角色 300 + 一个他租户角色 400
        Long userId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(100L);
        }));
        when(roleService.getRoleList(any())).thenReturn(toList(
                randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); }),
                randomPojo(RoleDO.class, o -> { o.setId(400L); o.setTenantId(200L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));

        // 调用，并断言拒绝：只要混入一个他租户角色，整批拒绝
        assertServiceException(() -> permissionService.assignUserRole(userId, asSet(300L, 400L)),
                PERMISSION_ASSIGN_ROLE_OTHER_TENANT, 400L);
        assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(userId)));
    }

    @Test
    public void testAssignUserRole_disabledRole_rejected() {
        // 准备参数：新授予的角色已被禁用
        Long userId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(100L);
        }));
        when(roleService.getRoleList(any())).thenReturn(toList(
                randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(100L); o.setName("禁用角色"); o.setStatus(CommonStatusEnum.DISABLE.getStatus()); })));

        // 调用，并断言拒绝
        assertServiceException(() -> permissionService.assignUserRole(userId, asSet(300L)),
                ROLE_IS_DISABLE, "禁用角色");
        assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(userId)));
    }

    @Test
    public void testAssignUserRole_selfElevation_rejected() {
        try (MockedStatic<SecurityFrameworkUtils> secMock = mockStatic(SecurityFrameworkUtils.class)) {
            // 准备参数：非超管操作者为自身新增角色
            Long loginUserId = 1L;
            secMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);
            TenantContextHolder.setTenantId(100L);
            when(userService.getUser(eq(loginUserId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
                o.setId(loginUserId);
                o.setTenantId(100L);
            }));
            when(roleService.getRoleList(any())).thenReturn(toList(
                    randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));
            // 操作者（=目标用户）无任何角色 → 非超管；授予的 300 也非超管角色
            when(roleService.hasAnySuperAdmin(any())).thenReturn(false);

            // 调用，并断言拒绝：自我提权
            assertServiceException(() -> permissionService.assignUserRole(loginUserId, asSet(300L)),
                    PERMISSION_SELF_ELEVATION);
            assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(loginUserId)));
        }
    }

    @Test
    public void testAssignUserRole_disabledSuperAdminRole_noExemption() {
        try (MockedStatic<SecurityFrameworkUtils> secMock = mockStatic(SecurityFrameworkUtils.class)) {
            // ZS-PERM-001.A 小卡回归看守：操作者持【禁用】super_admin 角色（挂载未清理）+ 双角色组合
            // （另一启用角色持 assign-user-role 权限）时，旧实现经 hasAnySuperAdmin（不查状态）仍产生超管豁免、
            // 绕过自我提权上限；修复后豁免只认【启用状态】超管角色（hasAnyEnabledSuperAdmin），
            // 授予上限仍保持不区分状态的 hasAnySuperAdmin 语义（禁用超管角色同样不可授予）
            Long loginUserId = 1L;
            secMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);
            TenantContextHolder.setTenantId(100L);
            when(userService.getUser(eq(loginUserId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
                o.setId(loginUserId);
                o.setTenantId(100L);
            }));
            // 操作者名下挂载【禁用】super_admin 角色（H2 落库夹具，使 isSuperAdminUser 的角色集合非空、真正走到豁免判定）
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(loginUserId).setRoleId(999L));
            // 旧语义（不查状态）判操作者角色集 {999} 为超管豁免；新语义（仅启用）判非超管——
            // 本用例在旧实现下会被豁免放行而失败。授予目标 {300} 非超管角色，不触发上限
            when(roleService.hasAnySuperAdmin(eq(asSet(999L)))).thenReturn(true);
            when(roleService.hasAnyEnabledSuperAdmin(eq(asSet(999L)))).thenReturn(false);
            // 授予目标是另一启用普通角色
            when(roleService.getRoleList(any())).thenReturn(toList(
                    randomPojo(RoleDO.class, o -> { o.setId(300L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));

            // 调用，并断言拒绝：为自身新增角色 → 自我提权（禁用超管角色不产生豁免）
            assertServiceException(() -> permissionService.assignUserRole(loginUserId, asSet(300L)),
                    PERMISSION_SELF_ELEVATION);
            // 仅有夹具行（禁用超管角色 999），目标角色 300 未被授予
            assertEquals(1, userRoleMapper.selectListByUserId(loginUserId).size());
        }
    }

    @Test
    public void testAssignUserRole_exceedCeiling_rejected() {
        try (MockedStatic<SecurityFrameworkUtils> secMock = mockStatic(SecurityFrameworkUtils.class)) {
            // 准备参数：非超管操作者授予超管角色
            Long loginUserId = 1L;
            Long targetUserId = 2L;
            secMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);
            TenantContextHolder.setTenantId(100L);
            when(userService.getUser(eq(targetUserId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
                o.setId(targetUserId);
                o.setTenantId(100L);
            }));
            when(roleService.getRoleList(any())).thenReturn(toList(
                    randomPojo(RoleDO.class, o -> { o.setId(900L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));
            // 操作者无任何角色 → 非超管（isSuperAdminUser 短路）；授予的 900 是超管角色
            when(roleService.hasAnySuperAdmin(any())).thenReturn(true);

            // 调用，并断言拒绝：超出可授予上限
            assertServiceException(() -> permissionService.assignUserRole(targetUserId, asSet(900L)),
                    PERMISSION_GRANT_EXCEED_CEILING);
            assertTrue(CollUtil.isEmpty(userRoleMapper.selectListByUserId(targetUserId)));
        }
    }

    @Test
    public void testAssignUserRole_idempotent() {
        // 准备参数：重复授予相同角色，应幂等（无新增、无报错）
        Long userId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(userService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setTenantId(100L);
        }));
        when(roleService.getRoleList(any())).thenReturn(toList(
                randomPojo(RoleDO.class, o -> { o.setId(200L); o.setTenantId(100L); o.setStatus(CommonStatusEnum.ENABLE.getStatus()); })));
        // 已存在 (1,200)
        userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(200L));

        // 调用：再次授予 {200}
        permissionService.assignUserRole(userId, asSet(200L));
        // 断言：仍只有一条，无重复
        List<UserRoleDO> list = userRoleMapper.selectListByUserId(userId);
        assertEquals(1, list.size());
        assertEquals(200L, list.get(0).getRoleId());
    }

    @Test
    public void testProcessUserDeleted() {
        // 准备参数
        Long userId = randomLongId();
        // mock 数据
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(userId)); // 被删除
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO userRoleDO02 = randomPojo(UserRoleDO.class); // 不被删除
        userRoleMapper.insert(userRoleDO02);

        // 调用
        permissionService.processUserDeleted(userId);
        // 断言数据
        List<UserRoleDO> dbUserRoles = userRoleMapper.selectList();
        assertEquals(1, dbUserRoles.size());
        assertPojoEquals(dbUserRoles.get(0), userRoleDO02);
    }

    @Test
    public void testGetUserRoleIdListByUserId() {
        // 准备参数
        Long userId = 1L;
        // mock 数据
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(10L));
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO roleMenuDO02 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(20L));
        userRoleMapper.insert(roleMenuDO02);

        // 调用
        Set<Long> result = permissionService.getUserRoleIdListByUserId(userId);
        // 断言
        assertEquals(asSet(10L, 20L), result);
    }

    @Test
    public void testGetUserRoleIdListByUserIdFromCache() {
        // 准备参数
        Long userId = 1L;
        // mock 数据
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(10L));
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO roleMenuDO02 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(20L));
        userRoleMapper.insert(roleMenuDO02);

        // 调用
        Set<Long> result = permissionService.getUserRoleIdListByUserIdFromCache(userId);
        // 断言
        assertEquals(asSet(10L, 20L), result);
    }

    @Test
    public void testGetUserRoleIdsFromCache() {
        // 准备参数
        Long userId = 1L;
        // mock 数据
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(10L));
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO roleMenuDO02 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(20L));
        userRoleMapper.insert(roleMenuDO02);

        // 调用
        Set<Long> result = permissionService.getUserRoleIdListByUserIdFromCache(userId);
        // 断言
        assertEquals(asSet(10L, 20L), result);
    }

    @Test
    public void testGetUserRoleIdListByRoleId() {
        // 准备参数
        Collection<Long> roleIds = asSet(10L, 20L);
        // mock 数据
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(1L).setRoleId(10L));
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO roleMenuDO02 = randomPojo(UserRoleDO.class, o -> o.setUserId(2L).setRoleId(20L));
        userRoleMapper.insert(roleMenuDO02);

        // 调用
        Set<Long> result = permissionService.getUserRoleIdListByRoleId(roleIds);
        // 断言
        assertEquals(asSet(1L, 2L), result);
    }

    @Test
    public void testGetEnableUserRoleListByUserIdFromCache() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户登录的角色
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(100L));
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(200L));
            RoleDO role01 = randomPojo(RoleDO.class, o -> o.setId(100L)
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            RoleDO role02 = randomPojo(RoleDO.class, o -> o.setId(200L)
                    .setStatus(CommonStatusEnum.DISABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(asSet(100L, 200L))))
                    .thenReturn(toList(role01, role02));

            // 调用
            List<RoleDO> result = permissionService.getEnableUserRoleListByUserIdFromCache(userId);
            // 断言
            assertEquals(1, result.size());
            assertPojoEquals(role01, result.get(0));
        }
    }

    // ========== 用户-部门的相关方法  ==========

    @Test
    public void testAssignRoleDataScope() {
        // 准备参数
        Long roleId = 1L;
        Integer dataScope = 2;
        Set<Long> dataScopeDeptIds = asSet(10L, 20L);
        // mock 当前技术租户上下文 + 角色/部门归属（ZS-PERM-001.A）
        TenantContextHolder.setTenantId(100L);
        when(roleService.getRole(eq(roleId))).thenReturn(randomPojo(RoleDO.class, o -> {
            o.setId(roleId);
            o.setTenantId(100L);
        }));
        when(deptService.getDeptList(anyCollection())).thenReturn(toList(
                randomPojo(DeptDO.class, o -> { o.setId(10L); o.setTenantId(100L); }),
                randomPojo(DeptDO.class, o -> { o.setId(20L); o.setTenantId(100L); })));

        // 调用
        permissionService.assignRoleDataScope(roleId, dataScope, dataScopeDeptIds);
        // 断言
        verify(roleService).updateRoleDataScope(eq(roleId), eq(dataScope), eq(dataScopeDeptIds));
    }

    @Test
    public void testAssignRoleDataScope_deptOtherTenant_rejected() {
        // 准备参数：当前技术租户为 100，数据权限部门归属他租户 200
        Long roleId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(roleService.getRole(eq(roleId))).thenReturn(randomPojo(RoleDO.class, o -> {
            o.setId(roleId);
            o.setTenantId(100L);
        }));
        when(deptService.getDeptList(anyCollection())).thenReturn(toList(
                randomPojo(DeptDO.class, o -> { o.setId(10L); o.setTenantId(200L); }))); // 他租户部门

        // 调用，并断言拒绝
        assertServiceException(() -> permissionService.assignRoleDataScope(roleId, 2, asSet(10L)),
                PERMISSION_ASSIGN_DEPT_OTHER_TENANT, 10L);
        verify(roleService, never()).updateRoleDataScope(any(), any(), any());
    }

    @Test
    public void testGetDeptDataPermission_All() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.ALL.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言
            assertTrue(result.getAll());
            assertFalse(result.getSelf());
            assertTrue(CollUtil.isEmpty(result.getDeptIds()));
        }
    }

    @Test
    public void testGetDeptDataPermission_DeptCustom() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));
            // mock 部门的返回
            when(userService.getUser(eq(1L))).thenReturn(new AdminUserDO().setDeptId(3L),
                    null, null); // 最后返回 null 的目的，看看会不会重复调用

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertEquals(roleDO.getDataScopeDeptIds().size() + 1, result.getDeptIds().size());
            assertTrue(CollUtil.containsAll(result.getDeptIds(), roleDO.getDataScopeDeptIds()));
            assertTrue(CollUtil.contains(result.getDeptIds(), 3L));
        }
    }

    @Test
    public void testGetDeptDataPermission_DeptCustom_userDeptIdNull() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));
            // mock 部门的返回：用户未设置部门
            when(userService.getUser(eq(1L))).thenReturn(new AdminUserDO()); // deptId 为 null

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言：角色配置的可见部门仍正常加入，但 null 不进集合
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertEquals(roleDO.getDataScopeDeptIds().size(), result.getDeptIds().size());
            assertTrue(CollUtil.containsAll(result.getDeptIds(), roleDO.getDataScopeDeptIds()));
            assertFalse(result.getDeptIds().contains(null));
        }
    }

    @Test
    public void testGetDeptDataPermission_DeptOnly() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_ONLY.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));
            // mock 部门的返回
            when(userService.getUser(eq(1L))).thenReturn(new AdminUserDO().setDeptId(3L),
                    null, null); // 最后返回 null 的目的，看看会不会重复调用

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertEquals(1, result.getDeptIds().size());
            assertTrue(CollUtil.contains(result.getDeptIds(), 3L));
        }
    }

    @Test
    public void testGetDeptDataPermission_DeptAndChild() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_AND_CHILD.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));
            // mock 部门的返回
            when(userService.getUser(eq(1L))).thenReturn(new AdminUserDO().setDeptId(3L),
                    null, null); // 最后返回 null 的目的，看看会不会重复调用
            // mock 方法（部门)
            DeptDO deptDO = randomPojo(DeptDO.class);
            when(deptService.getChildDeptIdListFromCache(eq(3L))).thenReturn(singleton(deptDO.getId()));

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertEquals(2, result.getDeptIds().size());
            assertTrue(CollUtil.contains(result.getDeptIds(), deptDO.getId()));
            assertTrue(CollUtil.contains(result.getDeptIds(), 3L));
        }
    }

    @Test
    public void testGetDeptDataPermission_DeptAndChild_userDeptIdNull() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_AND_CHILD.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));
            // mock 部门的返回：用户未设置部门
            when(userService.getUser(eq(1L))).thenReturn(new AdminUserDO()); // deptId 为 null

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言：deptId 为 null，整段跳过；deptIds 为空，子部门查询不被触发
            assertFalse(result.getAll());
            assertFalse(result.getSelf());
            assertTrue(CollUtil.isEmpty(result.getDeptIds()));
            verify(deptService, never()).getChildDeptIdListFromCache(any());
        }
    }

    @Test
    public void testGetDeptDataPermission_Self() {
        try (MockedStatic<SpringUtil> springUtilMockedStatic = mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(eq(PermissionServiceImpl.class)))
                    .thenReturn(permissionService);

            // 准备参数
            Long userId = 1L;
            // mock 用户的角色编号
            userRoleMapper.insert(randomPojo(UserRoleDO.class).setUserId(userId).setRoleId(2L));
            // mock 获得用户的角色
            RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.SELF.getScope())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(roleService.getRoleListFromCache(eq(singleton(2L)))).thenReturn(toList(roleDO));

            // 调用
            DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);
            // 断言
            assertFalse(result.getAll());
            assertTrue(result.getSelf());
            assertTrue(CollUtil.isEmpty(result.getDeptIds()));
        }
    }

    @Test
    public void testAssignRoleMenu_menuNotExists() {
        // GAP-3 codex r0 P2：移除控制器静默过滤后，伪造/不存在的菜单 ID 必须显式拒绝（MENU_NOT_EXISTS），
        // 防止 system_role_menu（无外键）出现悬空记录；植入系统租户豁免套餐校验以专测存在性分支
        Long roleId = 1L;
        TenantContextHolder.setTenantId(100L);
        when(roleService.getRole(eq(roleId))).thenReturn(randomPojo(RoleDO.class, o -> {
            o.setId(roleId);
            o.setTenantId(100L);
        }));
        cn.zszj.module.system.dal.dataobject.tenant.TenantDO tenant =
                randomPojo(cn.zszj.module.system.dal.dataobject.tenant.TenantDO.class);
        tenant.setId(100L).setPackageId(cn.zszj.module.system.dal.dataobject.tenant.TenantDO.PACKAGE_ID_SYSTEM);
        tenantMapper.insert(tenant);
        // mock：仅菜单 100 存在，300 为伪造 ID
        cn.zszj.module.system.dal.dataobject.permission.MenuDO menu =
                new cn.zszj.module.system.dal.dataobject.permission.MenuDO();
        menu.setId(100L);
        when(menuService.getMenuList(anyCollection())).thenReturn(java.util.Collections.singletonList(menu));

        assertServiceException(() -> permissionService.assignRoleMenu(roleId, asSet(100L, 300L)), MENU_NOT_EXISTS);
    }
}
