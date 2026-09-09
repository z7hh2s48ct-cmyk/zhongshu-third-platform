package cn.zszj.framework.security;

import cn.zszj.framework.security.fixture.MockOAuth2TokenApi;
import cn.zszj.framework.security.fixture.MockTenantFrameworkService;
import cn.zszj.framework.security.fixture.SecurityFixtureApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZS-SEC-012.A：真实 Security 过滤器链与双技术租户夹具测试。
 *
 * 验收条件（来自 05 文档 16.1 节）：
 * - 真实安全链启用（TokenAuthenticationFilter + TenantSecurityWebFilter + 方法权限）
 * - 双技术租户可构造（租户 1 和租户 2）
 * - 先允许暴露基线失败（不遮蔽真问题）
 *
 * 归属说明：本测试置于 biz-tenant 模块的 src/test（而非 security），以避免 security↔biz-tenant 的
 * Maven reactor 循环（biz-tenant 已 compile 依赖 security）。夹具装配见 fixture 包与 application-fixture.yaml。
 *
 * 测试覆盖：
 * 1. 公开接口（@PermitAll）- 无 token 可访问
 * 2. 认证接口 - 无 token → 401；有效 token → 200；MEMBER token 访问 /admin-api → 403
 * 3. 权限接口（@PreAuthorize）- 无权限 → 403；有权限 → 200
 * 4. 租户校验 - 未传（兜底/400）/不匹配/禁用/过期/未知租户
 * 5. 跨租户访问（visit-tenant-id）- 无权 403 / 持权 200（skipPermissionCheck=true 基线）
 * 6. 对象授权 - 按 ID 归属校验
 * 7. 异常出口一致性 - CommonResult JSON 格式
 *
 * @author ZS-SEC-012.A
 */
@SpringBootTest(classes = SecurityFixtureApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("fixture")
@DisplayName("ZS-SEC-012.A：安全过滤器链夹具测试")
class SecurityFilterChainFixtureTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== 1. 公开接口（@PermitAll） ==========

    @Nested
    @DisplayName("1. 公开接口（@PermitAll）")
    class PublicEndpoints {

        @Test
        @DisplayName("无 token 可访问公开 GET 端点")
        void publicGetWithoutToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/public/hello")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("public-hello"));
        }

        @Test
        @DisplayName("无 token 可访问公开 POST 端点")
        void publicPostWithoutToken() throws Exception {
            mockMvc.perform(post("/admin-api/fixture/public/echo")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("public-echo"));
        }

        @Test
        @DisplayName("公开端点在 ignore-urls 中，无需 tenant-id")
        void publicEndpointWithoutTenantId() throws Exception {
            // /admin-api/fixture/public/** 在 tenant.ignore-urls 中
            mockMvc.perform(get("/admin-api/fixture/public/hello"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    // ========== 2. 认证接口 ==========

    @Nested
    @DisplayName("2. 认证接口（需登录）")
    class AuthEndpoints {

        @Test
        @DisplayName("无 token 访问认证端点 → 401")
        void authEndpointWithoutToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk()) // CommonResult 包装，HTTP 200
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.msg").value("账号未登录"));
        }

        @Test
        @DisplayName("无效 token 访问认证端点 → 401")
        void authEndpointWithInvalidToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-invalid")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("有效 token（租户 1 管理员）访问认证端点 → 200")
        void authEndpointWithValidTokenT1() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(MockOAuth2TokenApi.USER_T1_ADMIN))
                    .andExpect(jsonPath("$.data.tenantId").value(MockOAuth2TokenApi.TENANT_1))
                    .andExpect(jsonPath("$.data.nickname").value("T1-Admin"));
        }

        @Test
        @DisplayName("有效 token（租户 2 管理员）访问认证端点 → 200")
        void authEndpointWithValidTokenT2() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t2-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(MockOAuth2TokenApi.USER_T2_ADMIN))
                    .andExpect(jsonPath("$.data.tenantId").value(MockOAuth2TokenApi.TENANT_2))
                    .andExpect(jsonPath("$.data.nickname").value("T2-Admin"));
        }

        @Test
        @DisplayName("MEMBER 类型 token 访问 /admin-api → 403（用户类型不匹配）")
        void memberTokenOnAdminApiRejected() throws Exception {
            // token-t1-member 的 userType=MEMBER(1)，而 /admin-api 约定为 ADMIN(2)：
            // TokenAuthenticationFilter 抛 AccessDeniedException("错误的用户类型")，
            // 经 GlobalExceptionHandler.accessDeniedExceptionHandler → CommonResult.error(FORBIDDEN) → 403 "没有该操作权限"
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t1-member")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("没有该操作权限"));
        }

        @Test
        @DisplayName("过期 token 访问认证端点 → 401")
        void authEndpointWithExpiredToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-expired")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    // ========== 3. 权限接口（@PreAuthorize） ==========

    @Nested
    @DisplayName("3. 权限接口（@PreAuthorize）")
    class PermissionEndpoints {

        @Test
        @DisplayName("租户 1 管理员有 system:user:query 权限 → 200")
        void t1AdminHasUserQueryPermission() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/user-query")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("perm-user-query"));
        }

        @Test
        @DisplayName("租户 1 管理员有 system:user:create 权限 → 200")
        void t1AdminHasUserCreatePermission() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/user-create")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("perm-user-create"));
        }

        @Test
        @DisplayName("租户 2 管理员无 system:user:create 权限 → 403")
        void t2AdminLacksUserCreatePermission() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/user-create")
                            .header("Authorization", "Bearer token-t2-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("没有该操作权限"));
        }

        @Test
        @DisplayName("租户 1 管理员有 super_admin 角色 → 200")
        void t1AdminHasSuperAdminRole() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/admin-role")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("perm-admin-role"));
        }

        @Test
        @DisplayName("租户 2 管理员无 super_admin 角色 → 403")
        void t2AdminLacksSuperAdminRole() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/admin-role")
                            .header("Authorization", "Bearer token-t2-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
        }

        @Test
        @DisplayName("租户 1 无权限管理员（ADMIN 但无权限）→ 403 没有该操作权限")
        void t1NoPermAdminDenied() throws Exception {
            // token-t1-noperm 为 ADMIN 类型、通过认证，但在 MockPermissionApi 中无任何权限：
            // @PreAuthorize("@ss.hasPermission('system:user:query')") → false → AccessDeniedException → 403
            mockMvc.perform(get("/admin-api/fixture/perm/user-query")
                            .header("Authorization", "Bearer token-t1-noperm")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("没有该操作权限"));
        }
    }

    // ========== 4. 租户校验 ==========

    @Nested
    @DisplayName("4. 租户校验（TenantSecurityWebFilter）")
    class TenantValidation {

        @Test
        @DisplayName("有效登录用户未传 tenant-id → 回退到用户自身租户，200（真实兜底行为）")
        void missingTenantIdFallsBackToUserTenant() throws Exception {
            // TenantSecurityWebFilter：user!=null 且 TenantContextHolder 无租户时，用 user.getTenantId() 兜底，不报错。
            // 这是真实代码行为（原预期 400 有误，已按 line 388「如实记录」修正）。
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t1-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.tenantId").value(MockOAuth2TokenApi.TENANT_1));
        }

        @Test
        @DisplayName("无登录用户 + 未传 tenant-id + 非忽略 URL → 400 租户标识未传递")
        void missingTenantIdWithoutLogin() throws Exception {
            // /open/tenant-required 是 @PermitAll（通过安全链，user==null），但不在 zszj.tenant.ignore-urls 中，
            // 故 TenantSecurityWebFilter 要求必须携带 tenant-id，否则 400。
            mockMvc.perform(get("/admin-api/fixture/open/tenant-required"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.msg").value("请求的租户标识未传递，请进行排查"));
        }

        @Test
        @DisplayName("用户租户与 header 租户不匹配 → 403 越权")
        void tenantMismatch() throws Exception {
            // 租户 1 的用户尝试访问租户 2
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("您无权访问该租户的数据"));
        }

        @Test
        @DisplayName("禁用租户（公开端点、无登录用户）→ 403 租户已被禁用")
        void disabledTenant() throws Exception {
            // 用 @PermitAll 且非 ignore-url 的 /open/tenant-required（无 token → user==null），携带禁用租户 header：
            // TenantSecurityWebFilter 因 user==null 跳过「越权比对」，进入 validTenant(999) → ServiceException(403,"租户已被禁用")。
            // 注意：若用已登录用户 + 不同租户 header，会先命中越权比对 403「您无权访问该租户的数据」而非 validTenant，测不到租户合法性校验。
            mockMvc.perform(get("/admin-api/fixture/open/tenant-required")
                            .header("tenant-id", MockTenantFrameworkService.TENANT_DISABLED))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("租户已被禁用"));
        }

        @Test
        @DisplayName("过期租户（公开端点、无登录用户）→ 403 租户已过期")
        void expiredTenant() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/open/tenant-required")
                            .header("tenant-id", MockTenantFrameworkService.TENANT_EXPIRED))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("租户已过期"));
        }

        @Test
        @DisplayName("未知租户（公开端点、无登录用户）→ 400 租户不存在")
        void unknownTenant() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/open/tenant-required")
                            .header("tenant-id", 99999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.msg").value("租户不存在: 99999"));
        }
    }

    // ========== 5. 跨租户访问（visit-tenant-id） ==========

    @Nested
    @DisplayName("5. 跨租户访问（visit-tenant-id）")
    class CrossTenantAccess {

        @Test
        @DisplayName("无 visit-tenant-id 时 skipPermissionCheck=false")
        void noCrossTenant() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.skipPermissionCheck").value(false));
        }

        @Test
        @DisplayName("visit-tenant-id 与 tenant-id 相同时 skipPermissionCheck=false")
        void sameVisitTenant() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                            .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.skipPermissionCheck").value(false));
        }

        @Test
        @DisplayName("无 system:tenant:visit 权限跨租户 → 403 您无权切换租户")
        void crossTenantWithoutVisitPermission() throws Exception {
            // t1-admin 无 system:tenant:visit 权限：TenantVisitContextInterceptor.preHandle 抛
            // ServiceException(403, "您无权切换租户")，由 GlobalExceptionHandler 转 CommonResult。
            mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                            .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("您无权切换租户"));
        }

        @Test
        @DisplayName("持有 system:tenant:visit 跨租户 → 200 且 skipPermissionCheck=true（ZS-SEC-001 待收紧）")
        void crossTenantWithVisitPermission() throws Exception {
            // t1-visitor 持有 system:tenant:visit：切换成功后 LoginUser.visitTenantId=TENANT_2。
            // 此时 SecurityFrameworkUtils.skipPermissionCheck() 返回 true（权限校验被整体跳过）——
            // 这正是 ZS-SEC-001.A「禁用旧跨租户权限跳过」需要收紧的越权放大点，本用例将其显式固化为基线。
            mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                            .header("Authorization", "Bearer token-t1-visitor")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                            .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.visitTenantId").value(MockOAuth2TokenApi.TENANT_2))
                    .andExpect(jsonPath("$.data.skipPermissionCheck").value(true));
        }
    }

    // ========== 6. 对象授权 ==========

    @Nested
    @DisplayName("6. 对象授权（按 ID 访问）")
    class ObjectAuthorization {

        @Test
        @DisplayName("租户 1 用户访问租户 1 对象 → 200")
        void t1UserAccessT1Object() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/obj/user/150")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.allowed").value(true));
        }

        @Test
        @DisplayName("租户 1 用户访问租户 2 对象 → 403（业务层拒绝）")
        void t1UserAccessT2Object() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/obj/user/250")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("无权访问该对象"));
        }

        @Test
        @DisplayName("租户 2 用户访问租户 2 对象 → 200")
        void t2UserAccessT2Object() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/obj/user/250")
                            .header("Authorization", "Bearer token-t2-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.allowed").value(true));
        }
    }

    // ========== 7. 异常出口一致性 ==========

    @Nested
    @DisplayName("7. 异常出口一致性（CommonResult JSON）")
    class ExceptionConsistency {

        @Test
        @DisplayName("401 响应格式一致")
        void unauthorizedResponseFormat() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.msg").value("账号未登录"));
        }

        @Test
        @DisplayName("403 响应格式一致")
        void forbiddenResponseFormat() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/perm/user-create")
                            .header("Authorization", "Bearer token-t2-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("没有该操作权限"));
        }

        @Test
        @DisplayName("400 响应格式一致")
        void badRequestResponseFormat() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/open/tenant-required"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.msg").value("请求的租户标识未传递，请进行排查"));
        }
    }
}
