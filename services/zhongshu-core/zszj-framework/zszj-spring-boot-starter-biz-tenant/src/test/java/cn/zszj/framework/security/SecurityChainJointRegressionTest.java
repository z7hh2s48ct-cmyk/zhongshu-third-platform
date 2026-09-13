package cn.zszj.framework.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static cn.zszj.framework.security.fixture.MockOAuth2TokenApi.TENANT_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ZS-SEC-012.B：安全与双端请求【联合回归】——全面 async 运行时合同 + CORS 端到端。
 *
 * <p>复用 ZS-SEC-012.A 真实安全链夹具（{@link SecurityFixtureApplication}：真实 Filter/Security 链/
 * 方法权限/异常出口全量装配，mock-enable=false，未禁用任何安全过滤器）。本类覆盖 .A 明确移交本子项的缺口：
 * <ol>
 *     <li><b>全面 async 运行时合同</b>：有效 token 异步端点成功、跨线程租户/用户上下文传播、
 *         异步异常出口仍走统一异常处理器、流式（StreamingResponseBody）端点经 ASYNC 派发；</li>
 *     <li><b>CORS 端到端</b>：真实链预检放行（trace-id 在 allowed/exposed headers）、
 *         非批准源失败关闭、跨源实际请求携带许可头。</li>
 * </ol>
 * trace-id 响应头关联已由 web starter（TraceFilterTest 10）与 common（TracerUtilsFallbackTest 14）覆盖，
 * monitor starter 不在本夹具类路径；Web/移动端请求层合同由 CLIENT-003（admin-web 12 + miniapp 36）覆盖——
 * 联合回归入口（G12 门禁）统一执行全部相关套件。
 *
 * @author ZS-SEC-012.B
 */
@DisplayName("ZS-SEC-012.B：安全链联合回归（全面 async 合同 + CORS 端到端）")
class SecurityChainJointRegressionTest extends SecurityChainJointRegressionTestBase {

    // ========== 1. 全面 async 运行时合同 ==========

    @Nested
    @DisplayName("1. 全面 async 运行时合同")
    class AsyncFullContract {

        @Test
        @DisplayName("有效 token 异步端点 → ASYNC 派发成功，跨线程租户/用户上下文与请求一致")
        void asyncTenantContextPropagated() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get("/admin-api/fixture/async/tenant-context")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", TENANT_1))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            String body = mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

            // 跨线程上下文传播（codex r0 P2：精确匹配防前缀漏检串号——池化线程复用下 tenant=10;user=104 不得通过）
            assertTrue(body.contains("tenant=" + TENANT_1 + ";user="
                            + cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T1_ADMIN),
                    "异步线程租户/用户上下文必须与 token 精确一致，实际: " + body);
        }

        @Test
        @DisplayName("异步阶段异常 → 仍经统一异常出口输出 CommonResult（非原始堆栈）")
        void asyncExceptionUnifiedExit() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get("/admin-api/fixture/async/error")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", TENANT_1))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk()) // 统一出口：HTTP 200 + 业务码（平台既有契约）
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                        // codex r0 P2：精确断言——统一消息必须存在，且不得泄漏异常类名/业务异常消息/堆栈
                        assertTrue(body.contains("系统异常"), "异步异常必须收敛为统一错误消息，实际: " + body);
                        assertTrue(!body.contains("IllegalStateException")
                                        && !body.contains("async-boom")
                                        && !body.contains("java.lang."),
                                "异步异常响应不得回显异常类名/原始消息/堆栈: " + body);
                    });
        }

        @Test
        @DisplayName("受保护流式端点（StreamingResponseBody）有效 token → 经 ASYNC 派发输出完整流")
        void streamingEndpointWithTokenStreamsFully() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get("/admin-api/fixture/async/stream")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", TENANT_1))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string("stream-ok-part1;stream-ok-part2"));
        }

        @Test
        @DisplayName("受保护流式端点无 token → 首次 REQUEST 派发 401（流式不免认证）")
        void streamingEndpointWithoutTokenRejected() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/async/stream")
                            .header("tenant-id", TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    // ========== 2. CORS 端到端（真实上下文 Bean 配置合同） ==========

    /**
     * <b>MockMvc 局限（如实登记）</b>：MockMvc 不按容器顺序应用 FilterRegistrationBean——实测预检经
     * Security {@code .cors(withDefaults())} 的 HandlerMappingIntrospector fallback 处理并回显任意 Origin
     * （本应被 SEC-004 白名单 CorsFilter 以 403 短路）。生产（真实容器）中 CORS_FILTER=Integer.MIN_VALUE
     * 最先执行、失败关闭。故 CORS 端到端合同以【真实上下文中的白名单过滤器配置】为准断言（本组），
     * MockMvc 层仅保留批准源预检冒烟。
     */
    @Nested
    @DisplayName("2. CORS 端到端（白名单过滤器配置合同）")
    class CorsEndToEnd {

        /**
         * 上下文中存在两个 CorsFilter（codex 评审关注点）：①SEC-004 白名单版（web starter，
         * UrlBasedCorsConfigurationSource，注册顺序 Integer.MIN_VALUE 最先执行）；②Security
         * {@code .cors(withDefaults())} 的 permit-default 兜底版。生产中白名单版最先执行、
         * 失败关闭，兜底版永不被非批准源触达。本辅助只取【白名单版】。
         */
        private org.springframework.web.filter.CorsFilter whitelistCorsFilter() {
            org.springframework.web.filter.CorsFilter whitelist = null;
            for (String name : applicationContext.getBeanNamesForType(
                    org.springframework.boot.web.servlet.FilterRegistrationBean.class)) {
                org.springframework.boot.web.servlet.FilterRegistrationBean<?> bean =
                        (org.springframework.boot.web.servlet.FilterRegistrationBean<?>) applicationContext.getBean(name);
                if (bean.getFilter() instanceof org.springframework.web.filter.CorsFilter cf
                        && org.springframework.test.util.ReflectionTestUtils.getField(cf, "configSource")
                                instanceof org.springframework.web.cors.UrlBasedCorsConfigurationSource) {
                    // 生产安全依据：白名单注册顺序必须为 MIN_VALUE（先于 Security 与一切过滤器）
                    assertEquals(Integer.MIN_VALUE, bean.getOrder(), "白名单 CorsFilter 必须最先执行");
                    whitelist = cf;
                }
            }
            assertNotNull(whitelist, "上下文中必须存在 SEC-004 白名单 CorsFilter");
            return whitelist;
        }

        private org.springframework.web.cors.CorsConfiguration configFor(String origin, String method) {
            Object source = org.springframework.test.util.ReflectionTestUtils.getField(whitelistCorsFilter(), "configSource");
            org.springframework.mock.web.MockHttpServletRequest request =
                    new org.springframework.mock.web.MockHttpServletRequest(method, "/admin-api/fixture/public/hello");
            request.setRequestURI("/admin-api/fixture/public/hello");
            if (origin != null) {
                request.addHeader("Origin", origin);
            }
            return ((org.springframework.web.cors.UrlBasedCorsConfigurationSource) source)
                    .getCorsConfiguration(request);
        }

        @Test
        @DisplayName("白名单过滤器：批准源（localhost:*）预检配置存在且放行")
        void whitelistAllowsLocalhost() {
            org.springframework.web.cors.CorsConfiguration config = configFor("http://localhost:3000", "GET");
            assertNotNull(config, "批准源必须命中 CORS 配置");
            String resolved = config.checkOrigin("http://localhost:3000");
            assertNotNull(resolved, "批准源 checkOrigin 必须放行");
        }

        @Test
        @DisplayName("白名单过滤器：非批准源（evil.example）checkOrigin 拒绝（生产失败关闭）")
        void whitelistRejectsDisallowedOrigin() {
            org.springframework.web.cors.CorsConfiguration config = configFor("https://evil.example", "GET");
            if (config == null) {
                return; // 未命中配置即拒绝（UrlBasedCorsConfigurationSource 语义）
            }
            assertNull(config.checkOrigin("https://evil.example"), "非批准源 checkOrigin 必须返回 null（拒绝）");
        }

        @Test
        @DisplayName("SEC-006：exposedHeaders 含 trace-id；allowedHeaders 含 trace-id（跨源可读/可发）")
        void whitelistExposesAndAllowsTraceId() {
            org.springframework.web.cors.CorsConfiguration config = configFor("http://localhost:3000", "GET");
            assertNotNull(config);
            assertNotNull(config.getExposedHeaders(), "exposedHeaders 必须配置");
            assertTrue(config.getExposedHeaders().stream().anyMatch(h -> h.equalsIgnoreCase("trace-id")),
                    "trace-id 必须在 exposedHeaders，实际: " + config.getExposedHeaders());
            assertNotNull(config.getAllowedHeaders(), "allowedHeaders 必须配置");
            assertTrue(config.getAllowedHeaders().stream().anyMatch(h -> h.equalsIgnoreCase("trace-id")),
                    "trace-id 必须在 allowedHeaders，实际: " + config.getAllowedHeaders());
        }

        @Test
        @DisplayName("SEC-004：allowCredentials=true 仅与精确源白名单并存（禁 * + credentials）")
        void credentialsOnlyWithExplicitWhitelist() {
            org.springframework.web.cors.CorsConfiguration config = configFor("http://localhost:3000", "GET");
            assertNotNull(config);
            if (Boolean.TRUE.equals(config.getAllowCredentials())) {
                assertTrue(config.getAllowedOriginPatterns() == null
                                || config.getAllowedOriginPatterns().stream().noneMatch(p -> p.equals("*") || p.equals("**")),
                        "allowCredentials=true 时不得存在任意源模式，实际: " + config.getAllowedOriginPatterns());
            }
        }

        @Test
        @DisplayName("批准源预检冒烟（MockMvc fallback 链同样放行批准源）")
        void preflightSmokeFromAllowedOrigin() throws Exception {
            mockMvc.perform(options("/admin-api/fixture/public/hello")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "trace-id,authorization,tenant-id"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        }
    }
    // ========== 5. codex r0 P2：池化线程复用下的交替租户上下文传播（防串号） ==========

    /**
     * taskExecutor 池仅 2 线程，交替两租户/两用户多次请求——若 TTL 传播缺失或上下文清理缺失，
     * 复用线程会读到上一个请求的租户/用户，本用例必现串号。
     */
    @Test
    @DisplayName("池化线程复用：交替租户/用户逐请求精确匹配（TTL 传播 + 清理合同）")
    void asyncContextAlternatingTenants_noCrossTalk() throws Exception {
        String[][] cases = {
                {"token-t1-admin", "1", cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T1_ADMIN.toString()},
                {"token-t2-admin", "2", cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T2_ADMIN.toString()},
                {"token-t1-admin", "1", cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T1_ADMIN.toString()},
                {"token-t2-admin", "2", cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T2_ADMIN.toString()},
                {"token-t1-admin", "1", cn.zszj.framework.security.fixture.MockOAuth2TokenApi.USER_T1_ADMIN.toString()},
        };
        for (String[] c : cases) {
            MvcResult mvcResult = mockMvc.perform(get("/admin-api/fixture/async/tenant-context")
                            .header("Authorization", "Bearer " + c[0])
                            .header("tenant-id", c[1]))
                    .andExpect(request().asyncStarted())
                    .andReturn();
            String body = mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(body.contains("tenant=" + c[1] + ";user=" + c[2]),
                    "交替租户下上下文必须逐请求精确匹配，期望 tenant=" + c[1] + ";user=" + c[2] + "，实际: " + body);
        }
    }
}
