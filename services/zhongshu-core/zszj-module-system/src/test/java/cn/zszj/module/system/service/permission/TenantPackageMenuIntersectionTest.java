package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.zszj.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.zszj.module.system.dal.mysql.tenant.TenantMapper;
import cn.zszj.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.zszj.module.system.dal.mysql.permission.UserRoleMapper;
import cn.zszj.module.system.enums.permission.DataScopeEnum;
import cn.zszj.module.system.enums.permission.RoleTypeEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.tenant.TenantServiceImpl;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.TENANT_NOT_EXISTS;
import static cn.zszj.module.system.enums.ErrorCodeConstants.TENANT_PACKAGE_MENU_EXCEED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.TENANT_PACKAGE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-CFG-003.B：套餐/角色权限交集与变更生效测试。
 *
 * <p>合同：①授权入口（assignRoleMenu）的菜单必须是其租户套餐菜单的子集——套餐回收后
 * 租户管理员也不能把套餐外菜单重新授予任何角色（服务端重检，写侧防线）；
 * ②套餐缩小 → 各角色菜单收敛为与套餐的交集，且鉴权立即拒绝越界菜单（热路径）；
 * ③套餐扩大 → 普通角色授权不自动扩大（交集不变），租户管理员随套餐全量；
 * ④系统租户（packageId=0，菜单全量）不受套餐约束。
 *
 * @author ZS-CFG-003.B
 */
@Import({PermissionServiceImpl.class, RoleServiceImpl.class, TenantServiceImpl.class})
@TestPropertySource(properties = "spring.main.allow-circular-references=true") // 与生产一致（三层循环依赖）
public class TenantPackageMenuIntersectionTest extends BaseDbUnitTest {

    @Resource
    private PermissionService permissionService;
    @Resource
    private TenantServiceImpl tenantService;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private cn.zszj.module.system.service.tenant.TenantPackageService tenantPackageService;

    private static final Long TENANT_ID = 5_300_001L;
    private static final Long PACKAGE_ID = 5_300_101L;

    @BeforeEach
    public void beforeEach() {
        when(adminUserService.getUser(anyLong())).thenReturn(randomPojo(cn.zszj.module.system.dal.dataobject.user.AdminUserDO.class));
    }

    @AfterEach
    public void afterEach() {
        cn.zszj.framework.tenant.core.context.TenantContextHolder.clear();
    }

    // ========== ① 授权入口的套餐子集校验（服务端重检） ==========

    @Test
    public void testAssignRoleMenu_menuOutsidePackage_rejected() {
        seedTenant(TENANT_ID, PACKAGE_ID);
        seedPackage(PACKAGE_ID, Set.of(1L, 2L));
        Long roleId = seedRole(TENANT_ID, "custom_role");

        // 菜单 3 不在套餐许可内——必须拒绝且不落库（按错误码断言：消息含格式化参数）
        ServiceException ex = assertThrows(ServiceException.class,
                () -> permissionService.assignRoleMenu(roleId, Set.of(1L, 3L)));
        assertEquals(TENANT_PACKAGE_MENU_EXCEED.getCode(), ex.getCode());
        assertTrue(roleMenuMapper.selectListByRoleId(roleId).isEmpty(), "越界授权不得落库");
    }

    @Test
    public void testAssignRoleMenu_menuInsidePackage_succeeds() {
        seedTenant(TENANT_ID, PACKAGE_ID);
        seedPackage(PACKAGE_ID, Set.of(1L, 2L));
        Long roleId = seedRole(TENANT_ID, "custom_role");

        permissionService.assignRoleMenu(roleId, Set.of(1L, 2L));

        assertEquals(2, roleMenuMapper.selectListByRoleId(roleId).size());
    }

    @Test
    public void testAssignRoleMenu_systemTenant_unrestricted() {
        seedTenant(5_300_002L, TenantDO.PACKAGE_ID_SYSTEM); // 系统租户：菜单全量
        Long roleId = seedRole(5_300_002L, "custom_role_sys");

        permissionService.assignRoleMenu(roleId, Set.of(9_001L, 9_002L));

        assertEquals(2, roleMenuMapper.selectListByRoleId(roleId).size());
    }

    // ========== ② 套餐缩小：角色菜单收敛为交集 + 鉴权立即拒绝 ==========

    @Test
    public void testPackageShrink_roleMenusConverged_andPermissionDenied() {
        seedTenant(TENANT_ID, PACKAGE_ID);
        seedPackage(PACKAGE_ID, Set.of(1L, 2L, 3L));
        Long roleId = seedRole(TENANT_ID, "custom_role");
        Long userId = 5_400_001L;
        insertUserRole(userId, roleId);
        // 角色原有菜单 {1,2,3}（含套餐稍后回收的 3）
        permissionService.assignRoleMenu(roleId, Set.of(1L, 2L, 3L));

        // 套餐回收：{1,2,3} → {1}，并触发各角色菜单收敛
        tenantPackageMapper.updateById(new TenantPackageDO().setId(PACKAGE_ID).setName("缩小后套餐")
                .setMenuIds(Set.of(1L)));
        tenantService.updateTenantRoleMenu(TENANT_ID, Set.of(1L));

        // DB 收敛：角色菜单只剩 {1}
        Set<Long> roleMenus = cn.zszj.framework.common.util.collection.CollectionUtils.convertSet(
                roleMenuMapper.selectListByRoleId(roleId), cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO::getMenuId);
        assertEquals(Set.of(1L), roleMenus, "套餐回收后角色菜单必须收敛为与套餐的交集");

        // 热路径：越界菜单的鉴权立即拒绝（旧 Token 不保留撤销权限）
        when(menuService.getMenuIdListByPermissionFromCache("zs:cfg3b:menu3")).thenReturn(List.of(3L));
        when(menuService.getMenuIdListByPermissionFromCache("zs:cfg3b:menu1")).thenReturn(List.of(1L));
        assertFalse(permissionService.hasAnyPermissions(userId, "zs:cfg3b:menu3"), "套餐回收后越界菜单必须立即拒绝");
        assertTrue(permissionService.hasAnyPermissions(userId, "zs:cfg3b:menu1"), "许可范围内菜单仍应放行");
    }

    // ========== ③ 套餐扩大：普通角色不自动扩大，租户管理员随套餐 ==========

    @Test
    public void testPackageExpand_normalRoleKept_tenantAdminFollows() {
        seedTenant(TENANT_ID, PACKAGE_ID);
        seedPackage(PACKAGE_ID, Set.of(1L));
        Long normalRoleId = seedRole(TENANT_ID, "custom_role");
        Long adminRoleId = seedRole(TENANT_ID, cn.zszj.module.system.enums.permission.RoleCodeEnum.TENANT_ADMIN.getCode());
        insertUserRole(5_400_002L, normalRoleId);
        permissionService.assignRoleMenu(normalRoleId, Set.of(1L));

        // 套餐扩大：{1} → {1,2,3}
        tenantPackageMapper.updateById(new TenantPackageDO().setId(PACKAGE_ID).setName("扩大后套餐")
                .setMenuIds(Set.of(1L, 2L, 3L)));
        tenantService.updateTenantRoleMenu(TENANT_ID, Set.of(1L, 2L, 3L));

        Set<Long> normalMenus = cn.zszj.framework.common.util.collection.CollectionUtils.convertSet(
                roleMenuMapper.selectListByRoleId(normalRoleId), cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO::getMenuId);
        assertEquals(Set.of(1L), normalMenus, "套餐扩大不得自动扩大普通角色授权");
        Set<Long> adminMenus = cn.zszj.framework.common.util.collection.CollectionUtils.convertSet(
                roleMenuMapper.selectListByRoleId(adminRoleId), cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO::getMenuId);
        assertEquals(Set.of(1L, 2L, 3L), adminMenus, "租户管理员角色随套餐全量");
    }

    // ========== codex r0 P1/P2：缺记录拒绝 + 授权×收缩并发序列化 ==========

    @Test
    public void testAssignRoleMenu_missingTenant_rejected() {
        Long roleId = seedRole(9_900_001L, "custom_role"); // 无对应租户行
        ServiceException ex = assertThrows(ServiceException.class,
                () -> permissionService.assignRoleMenu(roleId, Set.of(1L)));
        assertEquals(TENANT_NOT_EXISTS.getCode(), ex.getCode(), "缺租户记录不得豁免套餐校验");
    }

    @Test
    public void testAssignRoleMenu_missingPackage_rejected() {
        seedTenant(TENANT_ID, 9_900_100L); // 指向不存在的套餐
        Long roleId = seedRole(TENANT_ID, "custom_role");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> permissionService.assignRoleMenu(roleId, Set.of(1L)));
        assertEquals(TENANT_PACKAGE_NOT_EXISTS.getCode(), ex.getCode(), "缺套餐记录不得豁免");
    }

    /**
     * 授权入口与套餐收缩共用租户行锁——收缩先持锁提交后，迟到的授权必须看到新套餐并拒绝越界菜单；
     * 反向（先授权后收缩）由收敛逻辑删除越界菜单。两种交错结束后不变式：角色菜单 ⊆ 套餐菜单。
     */
    @Test
    public void testConcurrentGrantAndShrink_serialized_finalSubsetHolds() throws Exception {
        seedTenant(TENANT_ID, PACKAGE_ID);
        seedPackage(PACKAGE_ID, Set.of(1L, 3L));
        Long roleId = seedRole(TENANT_ID, "custom_role");
        permissionService.assignRoleMenu(roleId, Set.of(1L));

        CountDownLatch shrinkLocked = new CountDownLatch(1);
        CountDownLatch shrinkCommitted = new CountDownLatch(1);
        AtomicReference<Throwable> shrinkError = new AtomicReference<>();
        Thread shrink = new Thread(() -> {
            try {
                org.springframework.transaction.support.TransactionTemplate tt =
                        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
                tt.executeWithoutResult(status -> {
                    tenantMapper.selectByIdForUpdate(TENANT_ID); // 先持租户行锁
                    shrinkLocked.countDown();
                    tenantPackageMapper.updateById(new TenantPackageDO().setId(PACKAGE_ID)
                            .setName("收缩套餐").setMenuIds(Set.of(1L))
                            .setStatus(CommonStatusEnum.ENABLE.getStatus()));
                    // 收敛：删除越界菜单 3 的授权
                    roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, java.util.List.of(3L));
                });
            } catch (Throwable ex) {
                shrinkError.set(ex);
            } finally {
                shrinkCommitted.countDown();
            }
        });
        shrink.start();
        assertTrue(shrinkLocked.await(10, java.util.concurrent.TimeUnit.SECONDS), "前置：收缩事务已持锁");

        // 迟到的授权：必须阻塞到收缩提交后执行，看到收缩后的套餐 {1}，菜单 3 被拒绝
        ServiceException ex = assertThrows(ServiceException.class,
                () -> permissionService.assignRoleMenu(roleId, Set.of(1L, 3L)));
        assertEquals(TENANT_PACKAGE_MENU_EXCEED.getCode(), ex.getCode());
        assertTrue(shrinkCommitted.await(10, java.util.concurrent.TimeUnit.SECONDS));
        assertNull(shrinkError.get(), "收缩事务不得失败: " + shrinkError.get());

        // 终态不变式：角色菜单 ⊆ 套餐菜单
        Set<Long> roleMenus = cn.zszj.framework.common.util.collection.CollectionUtils.convertSet(
                roleMenuMapper.selectListByRoleId(roleId), cn.zszj.module.system.dal.dataobject.permission.RoleMenuDO::getMenuId);
        assertTrue(Set.of(1L).containsAll(roleMenus), "角色菜单必须为套餐子集，实际: " + roleMenus);
    }

    // ========== 造数辅助 ==========

    private void seedTenant(Long tenantId, Long packageId) {
        TenantDO tenant = randomPojo(TenantDO.class);
        tenant.setId(tenantId).setName("租户-" + tenantId).setPackageId(packageId)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        tenantMapper.insert(tenant);
    }

    private void seedPackage(Long packageId, Set<Long> menuIds) {
        TenantPackageDO pkg = new TenantPackageDO();
        pkg.setId(packageId).setName("套餐-" + packageId).setMenuIds(menuIds)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        tenantPackageMapper.insert(pkg);
    }

    private Long seedRole(Long tenantId, String code) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName("角色-" + code);
        reqVO.setCode(code); // code 须精确等于 RoleCodeEnum 值（tenant_admin 走管理员全量分支）
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        cn.zszj.module.system.dal.dataobject.permission.RoleDO role =
                cn.zszj.framework.common.util.object.BeanUtils.toBean(reqVO, cn.zszj.module.system.dal.dataobject.permission.RoleDO.class);
        role.setTenantId(tenantId);
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setDataScope(DataScopeEnum.ALL.getScope());
        role.setRemark(null);
        roleMapperInsert(role);
        return role.getId();
    }

    @Resource
    private cn.zszj.module.system.dal.mysql.permission.RoleMapper roleMapper;
    @Resource
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private void roleMapperInsert(cn.zszj.module.system.dal.dataobject.permission.RoleDO role) {
        roleMapper.insert(role);
    }

    private void insertUserRole(Long userId, Long roleId) {
        cn.zszj.module.system.dal.dataobject.permission.UserRoleDO userRole =
                new cn.zszj.module.system.dal.dataobject.permission.UserRoleDO();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }
}
