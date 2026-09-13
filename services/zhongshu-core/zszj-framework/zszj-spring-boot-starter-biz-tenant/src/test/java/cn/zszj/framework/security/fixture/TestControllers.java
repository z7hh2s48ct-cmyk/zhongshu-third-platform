package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * ZS-SEC-012.A：测试用 Controller，提供公开/认证/权限三种端点。
 *
 * 端点分类：
 * 1. 公开端点（@PermitAll）：/fixture/public/**
 * 2. 认证端点（需登录）：/fixture/auth/**
 * 3. 权限端点（@PreAuthorize）：/fixture/perm/**
 * 4. 对象授权端点：/fixture/obj/**
 * 6. 异步端点（ZS-SEC-002）：/fixture/async/**，验证 ASYNC permitAll 不使首次 REQUEST 派发免认证
 *
 * 所有端点均位于 /admin-api 前缀下，触发 ADMIN userType 推导。
 * （/app-api MEMBER 主体端点见 {@link TestAppController}，用于 ZS-SEC-002 的 ADMIN/MEMBER 双向串用验证。）
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

    /**
     * ZS-SEC-005：接收 JSON 请求体的公开端点，用于验证畸形 JSON / 请求体类型错误的客户端错误出口。
     * secretPin 期望数字类型，传入非数字字符串会触发 InvalidFormatException（验证不回显敏感入参值）。
     */
    @PostMapping("/public/body")
    @PermitAll
    public CommonResult<Map<String, Object>> publicBody(@RequestBody BodyReq req) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", req.getName());
        data.put("secretPin", req.getSecretPin());
        return CommonResult.success(data);
    }

    /** ZS-SEC-005：请求体夹具 DTO（secretPin 为 Integer，用于触发类型转换失败）。 */
    public static class BodyReq {

        private String name;
        private Integer secretPin;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSecretPin() {
            return secretPin;
        }

        public void setSecretPin(Integer secretPin) {
            this.secretPin = secretPin;
        }
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

    // ========== 6. 异步端点（ZS-SEC-002：ASYNC 派发不免首次认证） ==========

    /**
     * 受保护的异步端点（无 {@code @PermitAll}，返回 {@link Callable} 触发 ASYNC 派发）。
     *
     * <p>ZS-SEC-002 验收「ASYNC（异步派发）不使首次受保护请求免认证」：安全链的
     * {@code dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()}（见 ZszjWebSecurityConfigurerAdapter）
     * 仅放行「异步二次派发」（SSE 场景续接），首次 REQUEST 派发仍受 {@code anyRequest().authenticated()} 约束。
     * 故本端点在静态接口清单中被归类为 AUTHENTICATED（非匿名），运行时也必须：无 token → 首次派发 401，
     * 有效 token → 首次派发通过认证并进入异步。全面 async/SSE 合同（流式、异步异常出口、跨线程租户上下文）归 ZS-SEC-012.B。
     */
    @GetMapping("/async/profile")
    public Callable<CommonResult<String>> asyncProfile() {
        return () -> CommonResult.success("async-profile");
    }

    // ========== 7. 异步端点扩展（ZS-SEC-012.B：全面 async 运行时合同） ==========

    /**
     * 受保护异步端点：在异步线程回读【租户上下文 + 登录用户】——验证跨线程上下文传播合同
     * （异步线程能看到与请求一致的租户编号与登录用户，而非空值/串号）。
     */
    @GetMapping("/async/tenant-context")
    public Callable<CommonResult<String>> asyncTenantContext() {
        return () -> CommonResult.success("tenant=" + cn.zszj.framework.tenant.core.context.TenantContextHolder.getTenantId()
                + ";user=" + cn.zszj.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
    }

    /**
     * 受保护异步端点：异步阶段抛出运行时异常——验证【异步异常出口】仍走统一异常处理器
     * （GlobalExceptionHandler 经 ASYNC 派发输出 CommonResult，而非原始堆栈/连接重置）。
     */
    @GetMapping("/async/error")
    public Callable<CommonResult<String>> asyncError() {
        return () -> {
            throw new IllegalStateException("async-boom");
        };
    }

    /**
     * 受保护流式端点（{@link StreamingResponseBody}）：验证流式响应同样经 ASYNC 派发执行，
     * 且首次 REQUEST 派发受认证约束。
     */
    @GetMapping("/async/stream")
    public StreamingResponseBody asyncStream() {
        return outputStream -> {
            outputStream.write("stream-ok-part1;".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.write("stream-ok-part2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();
        };
    }
}
