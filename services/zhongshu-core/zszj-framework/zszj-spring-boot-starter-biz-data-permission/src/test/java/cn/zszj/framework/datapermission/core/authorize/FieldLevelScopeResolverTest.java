package cn.zszj.framework.datapermission.core.authorize;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link FieldLevelScopeResolver} 的单元测试（ZS-PERM-003.B，D-12 §3 等级×角色默认映射）。
 *
 * <p>覆盖：平台/超管（{@code all=true}）→ F3；目标组织负责人（{@code ledOrgIds} 命中对象所属组织）→ F2；
 * 普通成员/无任职 → F1；护栏（无登录用户、MEMBER 类型用户）→ 保守 F1 且不触达权限 API；
 * 组织数据权限取不到（null）→ 保守 F1；LoginUser 上下文缓存（同请求第二次解析不重复调 API）。
 *
 * @author ZS-PERM-003.B
 */
class FieldLevelScopeResolverTest extends BaseMockitoUnitTest {

    @Mock
    private PermissionCommonApi permissionApi;

    private FieldLevelScopeResolver newResolver() {
        return new FieldLevelScopeResolver(permissionApi);
    }

    private LoginUser adminUser() {
        return adminUser(1L);
    }

    private LoginUser adminUser(Long id) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(id);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        return loginUser;
    }

    private OrgDataPermissionRespDTO dto(boolean all, Set<Long> ledOrgIds) {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setAll(all);
        dto.setSelf(true);
        dto.setOrgIds(new java.util.HashSet<>());
        dto.setLedOrgIds(new java.util.HashSet<>(ledOrgIds));
        return dto;
    }

    @Test // 平台/超管（all=true）→ F3（D-12 §3：平台角色 ≤F3）
    void resolveMaxLevel_platform_f3() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(true, Set.of()));
            assertEquals(FieldLevel.F3, newResolver().resolveMaxLevel(100L));
        }
    }

    @Test // 目标组织负责人（ledOrgIds 命中对象所属组织）→ F2（D-12 §3：组织负责人 ≤F2）
    void resolveMaxLevel_orgLeader_f2() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of(200L)));
            assertEquals(FieldLevel.F2, newResolver().resolveMaxLevel(200L));
        }
    }

    @Test // 普通成员（非负责人、非平台）→ F1（D-12 §3：员工 ≤F1）
    void resolveMaxLevel_plainMember_f1() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of()));
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(100L));
        }
    }

    @Test // 负责人身份不跨组织：A 组织负责人查看 B 组织对象 → F1（F2 仅限本人负责的组织）
    void resolveMaxLevel_leaderOfOtherOrg_f1() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of(200L)));
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(300L));
        }
    }

    @Test // 无组织列对象（orgId=null）：负责人集合无从命中 → F1（个人对象按员工口径）
    void resolveMaxLevel_nullOrgId_f1() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of(200L)));
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(null));
        }
    }

    @Test // MEMBER 类型用户：不适用组织数据范围（与 org 轴 checker 护栏二同口径）→ 保守 F1，不触达权限 API
    void resolveMaxLevel_memberUserType_f1_noApiCall() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser member = adminUser();
            member.setUserType(UserTypeEnum.MEMBER.getValue());
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(member);
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(100L));
            verifyNoInteractions(permissionApi);
        }
    }

    @Test // 无登录用户（系统内部/定时任务上下文）→ 保守 F1，不触达权限 API
    void resolveMaxLevel_noLoginUser_f1_noApiCall() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(null);
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(100L));
            verifyNoInteractions(permissionApi);
        }
    }

    @Test // 组织数据权限取不到（API 返回 null）→ 保守 F1（降级方向=收紧可见性，不放大）
    void resolveMaxLevel_nullPermission_f1() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(null);
            assertEquals(FieldLevel.F1, newResolver().resolveMaxLevel(100L));
        }
    }

    @Test // LoginUser 上下文缓存：同一登录主体第二次解析不重复调用权限 API（与 org 轴 checker 同机制）
    void resolveMaxLevel_contextCached_singleApiCall() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser();
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of(200L)));

            FieldLevelScopeResolver resolver = newResolver();
            assertEquals(FieldLevel.F2, resolver.resolveMaxLevel(200L));
            assertEquals(FieldLevel.F1, resolver.resolveMaxLevel(300L));
            verify(permissionApi, times(1)).getOrgDataPermission(1L);
        }
    }

    @Test // 解析结果不因缓存串级：同一主体对不同组织对象返回各自等级（缓存的是范围 DTO，不是等级值）
    void resolveMaxLevel_cacheDoesNotLeakAcrossOrgs() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser();
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(1L)).thenReturn(dto(false, Set.of(200L)));

            FieldLevelScopeResolver resolver = newResolver();
            FieldLevel first = resolver.resolveMaxLevel(300L);
            FieldLevel second = resolver.resolveMaxLevel(200L);
            assertEquals(FieldLevel.F1, first);
            assertEquals(FieldLevel.F2, second);
            assertTrue(first != second);
        }
    }

}
