package cn.zszj.framework.security.core.service;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import lombok.AllArgsConstructor;

import java.util.Arrays;

import static cn.zszj.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.zszj.framework.security.core.util.SecurityFrameworkUtils.skipPermissionCheck;

/**
 * 默认的 {@link SecurityFrameworkService} 实现类
 *
 * @author 芋道源码
 */
@AllArgsConstructor
public class SecurityFrameworkServiceImpl implements SecurityFrameworkService {

    private final PermissionCommonApi permissionApi;

    @Override
    public boolean hasPermission(String permission) {
        return hasAnyPermissions(permission);
    }

    @Override
    public boolean hasAnyPermissions(String... permissions) {
        // ZS-SEC-001.B：跨租户访问不再整体跳过功能权限，改为按服务端授权记录的 allowedActions 收敛裁决
        // （落实 docs/05 line331「即使具备访问入口权限，也不能自动获得所有目标动作」；无 scope → fail-closed）
        if (skipPermissionCheck()) {
            return CrossOrgVisitScopeHolder.isAnyActionAllowed(permissions);
        }

        // 权限校验
        Long userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        return permissionApi.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    @Override
    public boolean hasAnyRoles(String... roles) {
        // ZS-SEC-001.B：一期跨组织授权只发放动作/权限维度，不发放角色——visit 上下文 fail-closed 拒绝
        if (skipPermissionCheck()) {
            return false;
        }

        // 权限校验
        Long userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        return permissionApi.hasAnyRoles(userId, roles);
    }

    @Override
    public boolean hasScope(String scope) {
        return hasAnyScopes(scope);
    }

    @Override
    public boolean hasAnyScopes(String... scope) {
        // ZS-SEC-001.B：一期跨组织授权不发放 OAuth2 scope——visit 上下文 fail-closed 拒绝
        if (skipPermissionCheck()) {
            return false;
        }

        // 权限校验
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null) {
            return false;
        }
        return CollUtil.containsAny(user.getScopes(), Arrays.asList(scope));
    }

}
