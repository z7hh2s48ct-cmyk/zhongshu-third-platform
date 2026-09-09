package cn.zszj.framework.datapermission.core.rule.dept;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.SetUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DeptDataPermissionChecker} 的单元测试（ZS-PERM-002.A）。
 *
 * <p>覆盖：ALL/指定部门/本人三类数据范围的可见判定；同技术租户无权对象显式拒绝；批量混入整批拒绝；
 * 无登录用户/非 ADMIN 护栏（与 {@link DeptDataPermissionRule} 一致，非新增绕过面）；取不到权限 fail-closed；
 * 复用 LoginUser 上下文缓存不重复计算。跨租户隔离轴由 ZS-DB-018 覆盖，此处不重复。
 *
 * @author 众墅之家
 */
class DeptDataPermissionCheckerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DeptDataPermissionChecker checker;

    @Mock
    private PermissionCommonApi permissionApi;

    private LoginUser adminUser(Long userId) {
        return randomPojo(LoginUser.class, o -> o.setId(userId).setUserType(UserTypeEnum.ADMIN.getValue()));
    }

    @Test // ALL 全量：任意部门/负责人均可见
    public void testIsObjectVisible_all() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(1L))).thenReturn(new DeptDataPermissionRespDTO().setAll(true));

            assertTrue(checker.isObjectVisible(999L, 888L));
            assertTrue(checker.isDeptVisible(999L));
        }
    }

    @Test // 指定部门：命中可见部门集合
    public void testIsObjectVisible_deptHit() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(SetUtils.asSet(10L, 20L)));

            assertTrue(checker.isObjectVisible(10L, null));
            assertTrue(checker.isObjectVisible(20L, 999L)); // 部门命中即可见，与负责人无关
        }
    }

    @Test // 指定部门：未命中且不可查看本人 → 不可见 + 显式拒绝
    public void testIsObjectVisible_deptMiss_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(SetUtils.asSet(10L, 20L)));

            assertFalse(checker.isObjectVisible(999L, 999L));
            // 同技术租户无权对象：详情路径显式拒绝（FORBIDDEN）
            assertServiceException(() -> checker.checkObjectVisible(999L, 999L), FORBIDDEN);
        }
    }

    @Test // 仅本人：对象负责人为登录用户 → 可见
    public void testIsObjectVisible_selfOwn() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setSelf(true));

            assertTrue(checker.isObjectVisible(null, 1L)); // 无部门列，仅凭负责人命中本人
        }
    }

    @Test // 仅本人：对象负责人为他人 → 不可见 + 拒绝
    public void testIsObjectVisible_selfOther_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setSelf(true));

            assertFalse(checker.isObjectVisible(null, 2L));
            assertServiceException(() -> checker.checkObjectVisible(null, 2L), FORBIDDEN);
        }
    }

    @Test // 100% 无权限（既无部门又不可查看本人）→ 不可见 + 拒绝
    public void testIsObjectVisible_noDeptNoSelf_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getDeptDataPermission(eq(1L))).thenReturn(new DeptDataPermissionRespDTO());

            assertFalse(checker.isObjectVisible(10L, 1L));
            assertServiceException(() -> checker.checkObjectVisible(10L, 1L), FORBIDDEN);
        }
    }

    @Test // 护栏一：无登录用户（系统/供给/任务）不施加数据范围限制，与 rule 一致
    public void testIsObjectVisible_noLoginUser_skip() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(null);

            assertTrue(checker.isObjectVisible(999L, 999L));
            // 不应触发数据权限获取
            verify(permissionApi, times(0)).getDeptDataPermission(org.mockito.ArgumentMatchers.anyLong());
        }
    }

    @Test // 护栏二：非 ADMIN（会员）不适用部门数据范围，与 rule 一致
    public void testIsObjectVisible_nonAdmin_skip() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser member = randomPojo(LoginUser.class,
                    o -> o.setId(2L).setUserType(UserTypeEnum.MEMBER.getValue()));
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(member);

            assertTrue(checker.isObjectVisible(999L, 999L));
            verify(permissionApi, times(0)).getDeptDataPermission(org.mockito.ArgumentMatchers.anyLong());
        }
    }

    @Test // 批量：全部在范围内 → 通过（不抛异常）
    public void testCheckBatchVisible_allVisible() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(SetUtils.asSet(10L, 20L)));

            List<DeptObj> objects = List.of(new DeptObj(10L, null), new DeptObj(20L, null));
            checker.checkBatchVisible(objects, DeptObj::getDeptId, DeptObj::getOwnerUserId); // 不抛即通过
        }
    }

    @Test // 批量混入：一个在范围内 + 一个越权 → 整批拒绝（FORBIDDEN），而非静默丢弃
    public void testCheckBatchVisible_mixedIn_rejectWholeBatch() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(SetUtils.asSet(10L, 20L)));

            List<DeptObj> objects = List.of(new DeptObj(10L, null), new DeptObj(999L, null)); // 999 越权混入
            assertServiceException(
                    () -> checker.checkBatchVisible(objects, DeptObj::getDeptId, DeptObj::getOwnerUserId),
                    FORBIDDEN);
        }
    }

    @Test // 批量：空集合直接通过
    public void testCheckBatchVisible_empty_pass() {
        checker.checkBatchVisible(CollUtil.newArrayList(), DeptObj::getDeptId, DeptObj::getOwnerUserId);
    }

    @Test // fail-closed：取不到数据权限（null）→ 拒绝，不放行
    public void testIsObjectVisible_nullPermission_failClosed() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getDeptDataPermission(eq(1L))).thenReturn(null);

            assertServiceException(() -> checker.checkObjectVisible(10L, 1L), FORBIDDEN);
        }
    }

    @Test // 复用 LoginUser 上下文缓存：同一登录用户多次检查只计算一次数据权限
    public void testGetDeptDataPermission_contextCacheReuse() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getDeptDataPermission(eq(1L)))
                    .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(SetUtils.asSet(10L)));

            checker.isObjectVisible(10L, null);
            checker.isObjectVisible(10L, null); // 第二次命中上下文缓存
            // 数据权限仅计算一次，证明与 rule 共享缓存、不重复计算
            verify(permissionApi, times(1)).getDeptDataPermission(eq(1L));
        }
    }

    /**
     * 批量校验用的轻量对象：携带部门编号与负责人编号。
     */
    private static class DeptObj {
        private final Long deptId;
        private final Long ownerUserId;

        DeptObj(Long deptId, Long ownerUserId) {
            this.deptId = deptId;
            this.ownerUserId = ownerUserId;
        }

        Long getDeptId() {
            return deptId;
        }

        Long getOwnerUserId() {
            return ownerUserId;
        }
    }

}
