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
 * ZS-SEC-001.A：验证跨租户访问门控是「配置开关」而非「硬删除」。
 *
 * 与 {@link SecurityFilterChainFixtureTest}（默认 zszj.tenant.visit-enable=false，跨租户一律拒绝）互补：
 * 本类以 properties 覆盖 visit-enable=true，证明门控确由配置驱动——显式开启后，
 * TenantVisitContextInterceptor 恢复原有的「权限校验 + 切换」链路。
 *
 * 【重要】visit-enable=true 时恢复的是**旧的**越权放大行为（持 system:tenant:visit 即 skipPermissionCheck()=true，
 * 权限/角色/scope 与数据范围被整体跳过）。这正是 ZS-SEC-001.B（依赖 D-09 组织模型）需要用「服务端授权记录/策略
 * 限定目标租户/对象/动作/字段/有效期」替换的技术债。本类将该现状显式固化为基线，不代表其为已批准的安全方案。
 *
 * 归属说明：与 SecurityFilterChainFixtureTest 同置于 biz-tenant/src/test，规避 security↔biz-tenant 的 reactor 循环。
 *
 * @author ZS-SEC-001.A
 */
@SpringBootTest(classes = SecurityFixtureApplication.class,
        properties = {"zszj.tenant.visit-enable=true"},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("fixture")
@DisplayName("ZS-SEC-001.A：跨租户门控为配置开关（visit-enable=true 时旧链路恢复）")
class CrossTenantVisitEnabledFixtureTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("开启后：持 system:tenant:visit 跨租户 → 200 且切换成功（旧放大行为，待 .B 替换）")
    void enabledPermittedVisitorSwitches() throws Exception {
        // visit-enable=true 时门控放行，t1-visitor 持 system:tenant:visit 通过权限校验 → 切换成功：
        // visitTenantId=TENANT_2，skipPermissionCheck()=true。此为旧越权放大现状，ZS-SEC-001.B 将以受控授权替换。
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
    @DisplayName("开启后：无 system:tenant:visit 权限跨租户 → 403 您无权切换租户（原权限校验仍生效）")
    void enabledNoPermissionStillRejected() throws Exception {
        // visit-enable=true 时门控放行到权限校验，t1-admin 无 system:tenant:visit → 抛
        // ServiceException(403, "您无权切换租户")。证明开启后并非无条件放行，权限前置依旧存在。
        mockMvc.perform(get("/admin-api/fixture/auth/cross-tenant")
                        .header("Authorization", "Bearer token-t1-admin")
                        .header("tenant-id", MockOAuth2TokenApi.TENANT_1)
                        .header("visit-tenant-id", MockOAuth2TokenApi.TENANT_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("您无权切换租户"));
    }
}
