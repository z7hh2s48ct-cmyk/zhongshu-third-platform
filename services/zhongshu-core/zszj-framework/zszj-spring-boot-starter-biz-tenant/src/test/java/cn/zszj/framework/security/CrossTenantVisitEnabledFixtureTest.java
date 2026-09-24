package cn.zszj.framework.security;

import cn.zszj.framework.security.fixture.MockOAuth2TokenApi;
import cn.zszj.framework.security.fixture.SecurityFixtureApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZS-SEC-001.B：验证 visit-enable=true 时跨租户访问已由「服务端授权记录/策略」受控裁决，
 * <b>取代</b> ZS-SEC-001.A 之前记录的旧越权放大行为（持 system:tenant:visit 即整体跳过功能权限）。
 *
 * <p>与 {@link SecurityFilterChainFixtureTest}（默认 zszj.tenant.visit-enable=false，跨租户一律拒绝）互补：
 * 本类以 properties 覆盖 visit-enable=true，证明开启后门控并非无条件放行，而是经
 * {@code TenantVisitContextInterceptor → CrossOrgVisitApi}（此处由 {@code MockCrossOrgVisitApi} 提供确定性裁决）
 * 按授权记录限定目标租户/动作/字段——落实 docs/05 line330-331「即使具备访问入口权限，也不能自动获得所有目标动作」。
 *
 * <p>Mock 授权口径：仅 {@code USER_T1_VISITOR(104) → TENANT_2} 获批，受控动作范围 {@code {system:user:query}}；
 * 其余主体/目标一律 {@code NO_GRANT} 拒绝。据此本类固化 4 条正向/负向矩阵：
 * <ol>
 *   <li>获批访问者跨租户 → 切换成功（visitTenantId=TENANT_2、skipPermissionCheck=true）；</li>
 *   <li>visit 上下文下访问<b>范围内</b>动作（system:user:query）→ 放行（D6 按 allowedActions 收敛）；</li>
 *   <li>visit 上下文下访问<b>范围外</b>动作（system:user:create）→ 403 拒绝
 *       （<b>关键回归护栏</b>：旧放大行为此处会返回 200，SEC-001.B 收敛后必须拒绝）；</li>
 *   <li>无授权记录的主体（t1-admin）跨租户 → 403 拒绝且不切换。</li>
 * </ol>
 *
 * <p>范围快照的 {@code afterCompletion} 清理与功能权限收敛的单元级验证见
 * {@code SecurityFrameworkServiceImplCrossOrgVisitTest}（security starter）。
 *
 * <p>归属说明：与 SecurityFilterChainFixtureTest 同置于 biz-tenant/src/test，规避 security↔biz-tenant 的 reactor 循环。
 *
 * @author ZS-SEC-001.B
 */
@SpringBootTest(classes = SecurityFixtureApplication.class,
        properties = {"zszj.tenant.visit-enable=true"},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("fixture")
@DisplayName("ZS-SEC-001.B：跨租户访问由服务端授权记录/策略受控裁决（取代旧放大）")
class CrossTenantVisitEnabledFixtureTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("获批访问者跨租户 → 200 切换成功（visitTenantId=TENANT_2、skipPermissionCheck=true）")
    void enabledGrantedVisitorSwitches() throws Exception {
        // visit-enable=true 且 MockCrossOrgVisitApi 对 USER_T1_VISITOR(104)→TENANT_2 返回 GRANT：
        // 拦截器切换租户并写入受控授权范围快照 → visitTenantId=TENANT_2，skipPermissionCheck()=true。
        // 注意：skipPermissionCheck=true 不再意味「整体跳过」，功能权限改由授权范围收敛裁决（见下两条用例）。
        mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                        .header("Authorization", "Bearer token-t1-visitor")
                        .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                        .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.visitTenantId").value(MockOAuth2TokenApi.TENANT_2))
                .andExpect(jsonPath("$.data.skipPermissionCheck").value(true));
    }

    @Test
    @DisplayName("visit 上下文访问范围内动作 system:user:query → 200（D6 按 allowedActions 收敛放行）")
    void enabledVisitScopeAllowsInScopeAction() throws Exception {
        // 获批范围的 allowedActions={system:user:query}：@PreAuthorize('system:user:query') 经
        // SecurityFrameworkServiceImpl.hasAnyPermissions → CrossOrgVisitScopeHolder.isAnyActionAllowed → true → 放行。
        mockMvc.perform(get("/admin-api/fixture/perm/user-query")
                        .header("Authorization", "Bearer token-t1-visitor")
                        .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                        .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("perm-user-query"));
    }

    @Test
    @DisplayName("visit 上下文访问范围外动作 system:user:create → 403（关键回归护栏：不再整体放大）")
    void enabledVisitScopeDeniesOutOfScopeAction() throws Exception {
        // 关键回归护栏：获批范围 allowedActions 仅 {system:user:query}，不含 system:user:create。
        // 旧放大行为（skipPermissionCheck()→hasAnyPermissions 一律 true）此处会返回 200；
        // SEC-001.B 收敛后 isAnyActionAllowed("system:user:create")=false → AccessDeniedException → 403「没有该操作权限」。
        mockMvc.perform(get("/admin-api/fixture/perm/user-create")
                        .header("Authorization", "Bearer token-t1-visitor")
                        .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                        .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有该操作权限"));
    }

    @Test
    @DisplayName("无授权记录的主体跨租户 → 403 您无权跨组织访问目标租户（不切换）")
    void enabledNoGrantRejected() throws Exception {
        // t1-admin(101) 无对应授权记录：MockCrossOrgVisitApi 返回 NO_GRANT → 拦截器 DENY，
        // 抛 ServiceException(403, "您无权跨组织访问目标租户")，不设置 visitTenantId、不切换租户上下文。
        // 证明开启 visit-enable 后并非无条件放行，旧 system:tenant:visit 粗粒度权限不再被当作授权。
        mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                        .header("Authorization", "Bearer token-t1-admin")
                        .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                        .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("您无权跨组织访问目标租户"));
    }
}
