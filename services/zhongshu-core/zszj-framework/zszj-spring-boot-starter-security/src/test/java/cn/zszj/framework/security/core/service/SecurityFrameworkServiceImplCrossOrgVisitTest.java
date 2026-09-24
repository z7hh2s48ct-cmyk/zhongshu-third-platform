package cn.zszj.framework.security.core.service;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SecurityFrameworkServiceImpl} 跨组织访问收敛单元测试（ZS-SEC-001.B，D6）。
 *
 * <p>核心回归护栏：visit 上下文（{@code skipPermissionCheck()=true}）下，功能权限<b>不再整体跳过</b>，
 * 而是按 {@link CrossOrgVisitScopeHolder} 中服务端授权记录的 {@code allowedActions} 收敛裁决；
 * 角色/scope 一律 fail-closed（一期跨组织授权不发放角色/scope）；无 scope（异常态）→ fail-closed false。
 * 非 visit 上下文则照常委托 {@link PermissionCommonApi}（不改变既有 RBAC 行为）。
 *
 * @author ZS-SEC-001.B
 */
@DisplayName("SecurityFrameworkServiceImpl - 跨组织访问收敛（ZS-SEC-001.B / D6）")
class SecurityFrameworkServiceImplCrossOrgVisitTest {

    private static final Long VISITOR_USER_ID = 104L;
    private static final Long HOME_TENANT_ID = 1L;
    private static final Long TARGET_TENANT_ID = 2L;
    private static final String ACTION_QUERY = "system:user:query";
    private static final String ACTION_CREATE = "system:user:create";

    private PermissionCommonApi permissionApi;
    private SecurityFrameworkServiceImpl service;

    @BeforeEach
    void setUp() {
        permissionApi = Mockito.mock(PermissionCommonApi.class);
        service = new SecurityFrameworkServiceImpl(permissionApi);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== visit 上下文：动作维度按授权范围收敛 ==========

    @Test
    @DisplayName("visit 上下文：请求动作 ∈ allowedActions → 放行（不委托 RBAC）")
    void visitContextInScopeActionAllowed() {
        givenVisitContextWithScope(setOf(ACTION_QUERY));
        assertTrue(service.hasAnyPermissions(ACTION_QUERY));
        // 收敛路径完全绕开 permissionApi（不再委托 home 租户 RBAC）
        verify(permissionApi, never()).hasAnyPermissions(anyLong(), any());
    }

    @Test
    @DisplayName("visit 上下文：请求动作 ∉ allowedActions → 拒绝（关键回归护栏，旧放大此处为 true）")
    void visitContextOutOfScopeActionDenied() {
        givenVisitContextWithScope(setOf(ACTION_QUERY));
        // 旧行为：skipPermissionCheck()→hasAnyPermissions 一律 true（越权放大）；收敛后必须 false
        assertFalse(service.hasAnyPermissions(ACTION_CREATE));
        verify(permissionApi, never()).hasAnyPermissions(anyLong(), any());
    }

    @Test
    @DisplayName("visit 上下文：多动作任一 ∈ allowedActions → 放行（hasAnyPermissions 语义）")
    void visitContextAnyOfMultipleActionsAllowed() {
        givenVisitContextWithScope(setOf(ACTION_QUERY));
        assertTrue(service.hasAnyPermissions(ACTION_CREATE, ACTION_QUERY));
    }

    @Test
    @DisplayName("visit 上下文：无 scope（异常态）→ fail-closed 拒绝")
    void visitContextNoScopeFailClosed() {
        loginVisitUser(TARGET_TENANT_ID); // 进入 visit 上下文但不写 scope
        assertFalse(service.hasAnyPermissions(ACTION_QUERY));
        verify(permissionApi, never()).hasAnyPermissions(anyLong(), any());
    }

    @Test
    @DisplayName("visit 上下文：scope.authorized=false → fail-closed 拒绝")
    void visitContextUnauthorizedScopeFailClosed() {
        loginVisitUser(TARGET_TENANT_ID);
        CrossOrgVisitScopeHolder.setScope(scope(false, null, setOf(ACTION_QUERY), null));
        assertFalse(service.hasAnyPermissions(ACTION_QUERY));
    }

    @Test
    @DisplayName("visit 上下文：allowedActions 为空集 → fail-closed 拒绝任何动作")
    void visitContextEmptyAllowedActionsFailClosed() {
        loginVisitUser(TARGET_TENANT_ID);
        CrossOrgVisitScopeHolder.setScope(scope(true, null, new HashSet<>(), null));
        assertFalse(service.hasAnyPermissions(ACTION_QUERY));
    }

    // ========== visit 上下文：角色 / scope 一律 fail-closed ==========

    @Test
    @DisplayName("visit 上下文：hasAnyRoles → fail-closed false（一期不发放角色）")
    void visitContextRolesFailClosed() {
        givenVisitContextWithScope(setOf(ACTION_QUERY));
        assertFalse(service.hasAnyRoles("super_admin"));
        verify(permissionApi, never()).hasAnyRoles(anyLong(), any());
    }

    @Test
    @DisplayName("visit 上下文：hasAnyScopes → fail-closed false（一期不发放 OAuth2 scope）")
    void visitContextScopesFailClosed() {
        givenVisitContextWithScope(setOf(ACTION_QUERY));
        // 即便 LoginUser.scopes 含 "read"，visit 上下文也不放行（收敛为 fail-closed）
        assertFalse(service.hasAnyScopes("read"));
    }

    // ========== 非 visit 上下文：照常委托 RBAC（既有行为不变） ==========

    @Test
    @DisplayName("非 visit 上下文：hasAnyPermissions 委托 permissionApi")
    void nonVisitContextDelegatesToPermissionApi() {
        loginVisitUser(null); // visitTenantId=null → skipPermissionCheck()=false
        when(permissionApi.hasAnyPermissions(VISITOR_USER_ID, ACTION_QUERY)).thenReturn(true);
        assertTrue(service.hasAnyPermissions(ACTION_QUERY));
        verify(permissionApi).hasAnyPermissions(VISITOR_USER_ID, ACTION_QUERY);
    }

    @Test
    @DisplayName("非 visit 上下文：hasAnyRoles 委托 permissionApi")
    void nonVisitContextRolesDelegate() {
        loginVisitUser(null);
        when(permissionApi.hasAnyRoles(VISITOR_USER_ID, "super_admin")).thenReturn(true);
        assertTrue(service.hasAnyRoles("super_admin"));
        verify(permissionApi).hasAnyRoles(VISITOR_USER_ID, "super_admin");
    }

    @Test
    @DisplayName("非 visit 上下文：hasAnyScopes 读 LoginUser.scopes")
    void nonVisitContextScopesFromLoginUser() {
        loginVisitUser(null);
        assertTrue(service.hasAnyScopes("read"));
        assertFalse(service.hasAnyScopes("write"));
    }

    // ========== 辅助 ==========

    private void givenVisitContextWithScope(Set<String> allowedActions) {
        loginVisitUser(TARGET_TENANT_ID);
        CrossOrgVisitScopeHolder.setScope(scope(true, null, allowedActions, null));
    }

    private void loginVisitUser(Long visitTenantId) {
        LoginUser user = new LoginUser();
        user.setId(VISITOR_USER_ID);
        user.setUserType(2);
        user.setTenantId(HOME_TENANT_ID);
        user.setVisitTenantId(visitTenantId); // 非空且 ≠ tenantId → skipPermissionCheck()=true
        user.setScopes(List.of("read"));
        SecurityFrameworkUtils.setLoginUser(user, new MockHttpServletRequest());
    }

    private CrossOrgVisitDecisionDTO scope(boolean authorized, Set<Long> targetOrgIds,
                                           Set<String> allowedActions, Set<String> allowedFields) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(authorized);
        dto.setReason(authorized ? "AUTHORIZED" : "NO_GRANT");
        dto.setTargetTenantId(TARGET_TENANT_ID);
        dto.setTargetOrgIds(targetOrgIds);
        dto.setAllowedActions(allowedActions);
        dto.setAllowedFields(allowedFields);
        return dto;
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(List.of(values));
    }
}
