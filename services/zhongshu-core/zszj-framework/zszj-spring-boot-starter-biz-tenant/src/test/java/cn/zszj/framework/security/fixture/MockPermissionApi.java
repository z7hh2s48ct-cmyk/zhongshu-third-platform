package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ZS-SEC-012.A：Mock PermissionCommonApi，模拟双技术租户的权限校验。
 *
 * 权限配置（与 MockOAuth2TokenApi 的 scopes 对应）：
 * - 租户 1 管理员（userId=101）：system:user:query, system:user:create, system:user:update + super_admin/system_admin
 * - 租户 2 管理员（userId=201）：system:user:query + system_admin
 * - 租户 1 会员（userId=102）：无权限
 * - 租户 1 无权限管理员（userId=103）：无权限（ADMIN 类型，用于纯粹的 403 权限拒绝）
 * - 租户 1 跨租户访问者（userId=104）：system:tenant:visit（供 TenantVisitContextInterceptor 校验通过）
 *
 * 该 Mock 提供确定性的权限判定，使测试可以专注于 Security 过滤器链行为。
 */
public class MockPermissionApi implements PermissionCommonApi {

    private static final Map<Long, Set<String>> USER_PERMISSIONS = new HashMap<>();
    private static final Map<Long, Set<String>> USER_ROLES = new HashMap<>();

    static {
        // 租户 1 管理员
        USER_PERMISSIONS.put(MockOAuth2TokenApi.USER_T1_ADMIN,
                new HashSet<>(Arrays.asList("system:user:query", "system:user:create", "system:user:update")));
        USER_ROLES.put(MockOAuth2TokenApi.USER_T1_ADMIN,
                new HashSet<>(Arrays.asList("super_admin", "system_admin")));

        // 租户 2 管理员（权限较少，用于验证租户隔离）
        USER_PERMISSIONS.put(MockOAuth2TokenApi.USER_T2_ADMIN,
                new HashSet<>(Arrays.asList("system:user:query")));
        USER_ROLES.put(MockOAuth2TokenApi.USER_T2_ADMIN,
                new HashSet<>(Arrays.asList("system_admin")));

        // 租户 1 会员（MEMBER 类型，无 admin 权限）
        USER_PERMISSIONS.put(MockOAuth2TokenApi.USER_T1_MEMBER, new HashSet<>());
        USER_ROLES.put(MockOAuth2TokenApi.USER_T1_MEMBER, new HashSet<>());

        // 租户 1 无权限管理员（ADMIN 类型，但无任何权限/角色：用于纯粹的 403 权限拒绝）
        USER_PERMISSIONS.put(MockOAuth2TokenApi.USER_T1_NOPERM, new HashSet<>());
        USER_ROLES.put(MockOAuth2TokenApi.USER_T1_NOPERM, new HashSet<>());

        // 租户 1 跨租户访问者（持有 system:tenant:visit，供 TenantVisitContextInterceptor 校验通过）
        USER_PERMISSIONS.put(MockOAuth2TokenApi.USER_T1_VISITOR,
                new HashSet<>(Arrays.asList("system:tenant:visit")));
        USER_ROLES.put(MockOAuth2TokenApi.USER_T1_VISITOR, new HashSet<>());
    }

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        Set<String> userPerms = USER_PERMISSIONS.get(userId);
        if (userPerms == null || userPerms.isEmpty()) {
            return false;
        }
        for (String perm : permissions) {
            if (userPerms.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        Set<String> userRoles = USER_ROLES.get(userId);
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        // 返回全部数据权限（简化测试）
        DeptDataPermissionRespDTO dto = new DeptDataPermissionRespDTO();
        dto.setAll(true);
        dto.setSelf(false);
        dto.setDeptIds(new HashSet<>());
        return dto;
    }
}
