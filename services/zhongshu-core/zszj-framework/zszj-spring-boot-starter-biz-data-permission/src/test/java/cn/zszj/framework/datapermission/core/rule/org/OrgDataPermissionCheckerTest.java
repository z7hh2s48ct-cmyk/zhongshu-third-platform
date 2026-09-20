package cn.zszj.framework.datapermission.core.rule.org;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
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
 * {@link OrgDataPermissionChecker} 的单元测试（ZS-PERM-002.B，org 轴）。
 *
 * <p>覆盖：全部组织（平台/超管）/ 授权组织集合（本组织子树）/ 仅本人三类范围的可见判定；<b>两组织越界</b>——
 * 已知他组织对象 ID 显式拒绝（FND-AUTH-004）；批量混入越界整批拒绝；无登录用户 / 非 ADMIN 护栏（与
 * {@code DeptDataPermissionChecker} 一致，非新增绕过面）；取不到权限 fail-closed；复用 LoginUser 上下文缓存不重复计算。
 * 跨租户隔离轴由 ZS-DB-018 覆盖、dept/self 轴由 PERM-002.A 覆盖，此处均不重复。
 *
 * @author ZS-PERM-002.B
 */
class OrgDataPermissionCheckerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private OrgDataPermissionChecker checker;

    @Mock
    private PermissionCommonApi permissionApi;

    private LoginUser adminUser(Long userId) {
        return randomPojo(LoginUser.class, o -> o.setId(userId).setUserType(UserTypeEnum.ADMIN.getValue()));
    }

    private OrgDataPermissionRespDTO allOrgs() {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setAll(true);
        return dto;
    }

    private OrgDataPermissionRespDTO orgs(Long... orgIds) {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setOrgIds(SetUtils.asSet(orgIds));
        return dto;
    }

    private OrgDataPermissionRespDTO selfOnly() {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setSelf(true);
        return dto;
    }

    private OrgDataPermissionRespDTO selfAndOrgs(Long... orgIds) {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setSelf(true);
        dto.setOrgIds(SetUtils.asSet(orgIds));
        return dto;
    }

    @Test // 全部组织（平台/超管）：任意组织/负责人均可见
    public void testIsObjectVisible_all() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(allOrgs());

            assertTrue(checker.isObjectVisible(999L, 888L));
            assertTrue(checker.isOrgVisible(999L));
        }
    }

    @Test // 授权组织集合：命中本组织子树（含后代）可见
    public void testIsObjectVisible_orgHit() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(orgs(100L, 110L));

            assertTrue(checker.isObjectVisible(100L, null)); // 本组织
            assertTrue(checker.isObjectVisible(110L, 999L)); // 后代组织命中即可见，与负责人无关
        }
    }

    @Test // 两组织越界：目标属他组织（不在授权集合）→ 不可见 + 显式拒绝（FND-AUTH-004 已知他组织对象 ID）
    public void testIsObjectVisible_otherOrg_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(orgs(100L, 110L));

            assertFalse(checker.isObjectVisible(200L, 200L)); // 200 属他组织，越界
            assertServiceException(() -> checker.checkObjectVisible(200L, 200L), FORBIDDEN);
        }
    }

    @Test // 仅本人：无组织列对象、负责人为登录用户 → 可见
    public void testIsObjectVisible_selfOwn() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(selfOnly());

            assertTrue(checker.isObjectVisible(null, 1L)); // orgId=null，仅凭负责人命中本人
        }
    }

    @Test // 仅本人：负责人为他人 → 不可见 + 拒绝
    public void testIsObjectVisible_selfOther_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(selfOnly());

            assertFalse(checker.isObjectVisible(null, 2L));
            assertServiceException(() -> checker.checkObjectVisible(null, 2L), FORBIDDEN);
        }
    }

    @Test // P1 回归：对象属他组织（不在授权集合）但负责人为登录用户 → 仍不可见 + 拒绝（本人所有权不凌驾组织排除，FND-AUTH-004）
    public void testIsObjectVisible_ownedObjectInOtherOrg_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(selfAndOrgs(100L, 110L));

            // orgId=200 属他组织越界，即便 ownerUserId=1 为登录用户本人，也必须拒绝（转岗/离任不得凭所有权回访旧组织对象）
            assertFalse(checker.isObjectVisible(200L, 1L));
            assertServiceException(() -> checker.checkObjectVisible(200L, 1L), FORBIDDEN);
        }
    }

    @Test // 无组织列对象（orgId=null）在 ORG_AND_CHILD 范围下的本人兜底 → 可见
    public void testIsObjectVisible_orglessOwnedWithScope_visible() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(selfAndOrgs(100L, 110L));

            assertTrue(checker.isObjectVisible(null, 1L)); // 无组织列，仅凭负责人命中本人兜底
        }
    }

    @Test // 无组织上下文且非本人（既无授权组织又不可查看本人）→ 不可见 + 拒绝
    public void testIsObjectVisible_noOrgNoSelf_reject() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(new OrgDataPermissionRespDTO());

            assertFalse(checker.isObjectVisible(100L, 1L));
            assertServiceException(() -> checker.checkObjectVisible(100L, 1L), FORBIDDEN);
        }
    }

    @Test // 护栏一：无登录用户（系统/供给/任务）不施加组织范围限制，与 dept 检查器一致
    public void testIsObjectVisible_noLoginUser_skip() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(null);

            assertTrue(checker.isObjectVisible(999L, 999L));
            verify(permissionApi, times(0)).getOrgDataPermission(org.mockito.ArgumentMatchers.anyLong());
        }
    }

    @Test // 护栏二：非 ADMIN（会员）不适用组织数据范围，与 dept 检查器一致
    public void testIsObjectVisible_nonAdmin_skip() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser member = randomPojo(LoginUser.class,
                    o -> o.setId(2L).setUserType(UserTypeEnum.MEMBER.getValue()));
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(member);

            assertTrue(checker.isObjectVisible(999L, 999L));
            verify(permissionApi, times(0)).getOrgDataPermission(org.mockito.ArgumentMatchers.anyLong());
        }
    }

    @Test // 批量：全部在授权组织内 → 通过（不抛异常）
    public void testCheckBatchVisible_allVisible() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(orgs(100L, 110L));

            List<OrgObj> objects = List.of(new OrgObj(100L, null), new OrgObj(110L, null));
            checker.checkBatchVisible(objects, OrgObj::getOrgId, OrgObj::getOwnerUserId); // 不抛即通过
        }
    }

    @Test // 批量混入越界：一个在授权组织 + 一个他组织 → 整批拒绝（FORBIDDEN），而非静默丢弃
    public void testCheckBatchVisible_mixedIn_rejectWholeBatch() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(orgs(100L, 110L));

            List<OrgObj> objects = List.of(new OrgObj(100L, null), new OrgObj(200L, null)); // 200 他组织越界混入
            assertServiceException(
                    () -> checker.checkBatchVisible(objects, OrgObj::getOrgId, OrgObj::getOwnerUserId),
                    FORBIDDEN);
        }
    }

    @Test // 批量：空集合直接通过
    public void testCheckBatchVisible_empty_pass() {
        checker.checkBatchVisible(CollUtil.newArrayList(), OrgObj::getOrgId, OrgObj::getOwnerUserId);
    }

    @Test // fail-closed：取不到组织数据权限（null）→ 拒绝，不放行
    public void testIsObjectVisible_nullPermission_failClosed() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(null);

            assertServiceException(() -> checker.checkObjectVisible(100L, 1L), FORBIDDEN);
        }
    }

    @Test // 复用 LoginUser 上下文缓存：同一登录用户多次检查只计算一次组织数据权限
    public void testGetOrgDataPermission_contextCacheReuse() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(orgs(100L));

            checker.isObjectVisible(100L, null);
            checker.isObjectVisible(100L, null); // 第二次命中上下文缓存
            verify(permissionApi, times(1)).getOrgDataPermission(eq(1L));
        }
    }

    /**
     * 批量校验用的轻量对象：携带组织编号与负责人编号。
     */
    private static class OrgObj {
        private final Long orgId;
        private final Long ownerUserId;

        OrgObj(Long orgId, Long ownerUserId) {
            this.orgId = orgId;
            this.ownerUserId = ownerUserId;
        }

        Long getOrgId() {
            return orgId;
        }

        Long getOwnerUserId() {
            return ownerUserId;
        }
    }

}
