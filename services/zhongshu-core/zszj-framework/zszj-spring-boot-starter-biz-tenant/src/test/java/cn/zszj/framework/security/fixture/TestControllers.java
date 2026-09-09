package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * ZS-SEC-012.A：测试用 Controller，提供公开/认证/权限三种端点。
 *
 * 端点分类：
 * 1. 公开端点（@PermitAll）：/fixture/public/**
 * 2. 认证端点（需登录）：/fixture/auth/**
 * 3. 权限端点（@PreAuthorize）：/fixture/perm/**
 * 4. 对象授权端点：/fixture/obj/**
 *
 * 所有端点均位于 /admin-api 前缀下，触发 ADMIN userType 推导。
 */
@RestController
@RequestMapping("/admin-api/fixture")
public class TestControllers {

    // ========== 1. 公开端点（@PermitAll） ==========

    @GetMapping("/public/hello")
    @PermitAll
    public CommonResult<String> publicHello() {
        return CommonResult.success("public-hello");
    }

    @PostMapping("/public/echo")
    @PermitAll
    public CommonResult<String> publicEcho() {
        return CommonResult.success("public-echo");
    }

    /**
     * 公开（@PermitAll）但不在 zszj.tenant.ignore-urls 中的端点。
     *
     * 用于触发 TenantSecurityWebFilter 的「未传 tenant-id 且无登录用户」400 路径：
     * 因 @PermitAll 可通过安全链（user==null），但因不在 ignore-urls，租户过滤器要求必须携带 tenant-id。
     */
    @GetMapping("/open/tenant-required")
    @PermitAll
    public CommonResult<String> openTenantRequired() {
        return CommonResult.success("open-tenant-required");
    }

    // ========== 2. 认证端点（需登录，无 @PermitAll） ==========

    @GetMapping("/auth/profile")
    public CommonResult<Map<String, Object>> authProfile() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user != null ? user.getId() : null);
        profile.put("userType", user != null ? user.getUserType() : null);
        profile.put("tenantId", user != null ? user.getTenantId() : null);
        profile.put("nickname", user != null ? user.getInfo().get("nickname") : null);
        return CommonResult.success(profile);
    }

    @GetMapping("/auth/tenant-context")
    public CommonResult<Map<String, Object>> authTenantContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("tenantId", TenantContextHolder.getTenantId());
        ctx.put("isIgnore", TenantContextHolder.isIgnore());
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        ctx.put("userTenantId", user != null ? user.getTenantId() : null);
        ctx.put("visitTenantId", user != null ? user.getVisitTenantId() : null);
        return CommonResult.success(ctx);
    }

    // ========== 3. 权限端点（@PreAuthorize） ==========

    @GetMapping("/perm/user-query")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<String> permUserQuery() {
        return CommonResult.success("perm-user-query");
    }

    @GetMapping("/perm/user-create")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<String> permUserCreate() {
        return CommonResult.success("perm-user-create");
    }

    @GetMapping("/perm/admin-role")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<String> permAdminRole() {
        return CommonResult.success("perm-admin-role");
    }

    // ========== 4. 对象授权端点（模拟按 ID 访问对象） ==========

    @GetMapping("/obj/user/{id}")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<Map<String, Object>> objUser(@PathVariable("id") Long id) {
        // 模拟对象授权：租户 1 的用户只能访问 100-199 的 ID
        // 租户 2 的用户只能访问 200-299 的 ID
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        Long tenantId = user != null ? user.getTenantId() : null;

        Map<String, Object> result = new HashMap<>();
        result.put("requestedId", id);
        result.put("tenantId", tenantId);

        // 简单的对象归属校验（模拟）
        boolean allowed = false;
        if (tenantId != null) {
            if (tenantId == 1L && id >= 100 && id < 200) {
                allowed = true;
            } else if (tenantId == 2L && id >= 200 && id < 300) {
                allowed = true;
            }
        }
        result.put("allowed", allowed);

        if (!allowed) {
            // 对象授权失败（模拟 403）
            return CommonResult.error(403, "无权访问该对象");
        }
        return CommonResult.success(result);
    }

    // ========== 5. 跨租户访问端点（visit-tenant-id） ==========

    @GetMapping("/auth/cross-tenant")
    public CommonResult<Map<String, Object>> crossTenant() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user != null ? user.getId() : null);
        result.put("tenantId", user != null ? user.getTenantId() : null);
        result.put("visitTenantId", user != null ? user.getVisitTenantId() : null);
        result.put("skipPermissionCheck", SecurityFrameworkUtils.skipPermissionCheck());
        return CommonResult.success(result);
    }
}
