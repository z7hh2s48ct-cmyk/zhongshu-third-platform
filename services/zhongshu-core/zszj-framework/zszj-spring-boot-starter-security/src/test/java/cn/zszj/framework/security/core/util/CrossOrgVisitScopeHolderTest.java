package cn.zszj.framework.security.core.util;

import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.security.core.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CrossOrgVisitScopeHolder} 单元测试（ZS-SEC-001.B，D7 对象/字段访问入口 + 上下文清理）。
 *
 * <p>覆盖：授权范围快照读写、动作/对象/字段三维裁决（whole-tenant vs 限定组织）、
 * 无 scope/未获批一律 fail-closed、{@code clear()} 清理有效（正向矩阵§5「授权撤销与上下文清理有效」）。
 *
 * @author ZS-SEC-001.B
 */
@DisplayName("CrossOrgVisitScopeHolder - 授权范围读写与三维裁决（ZS-SEC-001.B / D7）")
class CrossOrgVisitScopeHolderTest {

    private static final String ACTION_QUERY = "system:user:query";
    private static final String ACTION_CREATE = "system:user:create";
    private static final Long ORG_A = 9001L;
    private static final Long ORG_B = 9002L;
    private static final String FIELD_MOBILE = "contactMobile";
    private static final String FIELD_EMAIL = "contactEmail";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== 读写 ==========

    @Test
    @DisplayName("无登录用户 → getScope()=null")
    void getScopeNullWhenNoLoginUser() {
        assertNull(CrossOrgVisitScopeHolder.getScope());
    }

    @Test
    @DisplayName("setScope 后 getScope 返回同一快照")
    void setThenGetScope() {
        loginVisitUser();
        CrossOrgVisitDecisionDTO scope = scope(true, null, setOf(ACTION_QUERY), setOf(FIELD_MOBILE));
        CrossOrgVisitScopeHolder.setScope(scope);
        assertSame(scope, CrossOrgVisitScopeHolder.getScope());
    }

    // ========== 动作维度 ==========

    @Test
    @DisplayName("动作维度：∈ allowedActions → true；∉ → false")
    void isAnyActionAllowedByScope() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(true, null, setOf(ACTION_QUERY), null));
        assertTrue(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_QUERY));
        assertFalse(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_CREATE));
        assertTrue(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_CREATE, ACTION_QUERY));
    }

    @Test
    @DisplayName("动作维度：无 scope → fail-closed false")
    void isAnyActionAllowedNoScopeFailClosed() {
        loginVisitUser();
        assertFalse(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_QUERY));
    }

    // ========== 对象维度 ==========

    @Test
    @DisplayName("对象维度：targetOrgIds=null（whole-tenant）→ 任意组织放行")
    void isObjectAllowedWholeTenant() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(true, null, setOf(ACTION_QUERY), null));
        assertTrue(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_A));
        assertTrue(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_B));
    }

    @Test
    @DisplayName("对象维度：限定组织 → 仅范围内组织放行，范围外/null 拒绝")
    void isObjectAllowedScoped() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(true, setOf(ORG_A), setOf(ACTION_QUERY), null));
        assertTrue(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_A));
        assertFalse(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_B));
        assertFalse(CrossOrgVisitScopeHolder.isObjectAllowed(null));
    }

    @Test
    @DisplayName("对象维度：无 scope → fail-closed false")
    void isObjectAllowedNoScopeFailClosed() {
        loginVisitUser();
        assertFalse(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_A));
    }

    // ========== 字段维度 ==========

    @Test
    @DisplayName("字段维度：请求字段 ⊆ allowedFields → true；超出 → false；空请求 → true")
    void areFieldsAllowedByScope() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(true, null, setOf(ACTION_QUERY), setOf(FIELD_MOBILE, FIELD_EMAIL)));
        assertTrue(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE)));
        assertTrue(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE, FIELD_EMAIL)));
        assertFalse(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE, "idCard")));
        assertTrue(CrossOrgVisitScopeHolder.areFieldsAllowed(new HashSet<>())); // 空请求 = 不校验字段维
    }

    @Test
    @DisplayName("字段维度：无 scope → fail-closed false")
    void areFieldsAllowedNoScopeFailClosed() {
        loginVisitUser();
        assertFalse(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE)));
    }

    // ========== 上下文清理（正向矩阵§5） ==========

    @Test
    @DisplayName("clear() 后 scope 不残留，三维裁决回到 fail-closed")
    void clearRemovesScope() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(true, setOf(ORG_A), setOf(ACTION_QUERY), setOf(FIELD_MOBILE)));
        assertTrue(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_QUERY));
        // 模拟 afterCompletion 清理
        CrossOrgVisitScopeHolder.clear();
        assertNull(CrossOrgVisitScopeHolder.getScope());
        assertFalse(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_QUERY));
        assertFalse(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_A));
        assertFalse(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE)));
    }

    @Test
    @DisplayName("未获批快照（authorized=false）→ 三维裁决一律 fail-closed")
    void unauthorizedScopeFailClosed() {
        loginVisitUser();
        CrossOrgVisitScopeHolder.setScope(scope(false, setOf(ORG_A), setOf(ACTION_QUERY), setOf(FIELD_MOBILE)));
        assertFalse(CrossOrgVisitScopeHolder.isAnyActionAllowed(ACTION_QUERY));
        assertFalse(CrossOrgVisitScopeHolder.isObjectAllowed(ORG_A));
        assertFalse(CrossOrgVisitScopeHolder.areFieldsAllowed(setOf(FIELD_MOBILE)));
    }

    // ========== 辅助 ==========

    private void loginVisitUser() {
        LoginUser user = new LoginUser();
        user.setId(104L);
        user.setUserType(2);
        user.setTenantId(1L);
        user.setVisitTenantId(2L);
        SecurityFrameworkUtils.setLoginUser(user, new MockHttpServletRequest());
    }

    private CrossOrgVisitDecisionDTO scope(boolean authorized, Set<Long> targetOrgIds,
                                           Set<String> allowedActions, Set<String> allowedFields) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(authorized);
        dto.setReason(authorized ? "AUTHORIZED" : "NO_GRANT");
        dto.setTargetTenantId(2L);
        dto.setTargetOrgIds(targetOrgIds);
        dto.setAllowedActions(allowedActions);
        dto.setAllowedFields(allowedFields);
        return dto;
    }

    private static <T> Set<T> setOf(T... values) {
        return new HashSet<>(List.of(values));
    }
}
