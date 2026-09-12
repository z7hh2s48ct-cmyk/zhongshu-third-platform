package cn.zszj.framework.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-004：CORS 由全通配收紧为精确白名单 + frameOptions 可配置
 *
 * 纯单元测试：不启动 Spring 上下文，直接校验 {@link WebProperties.Cors} 的默认值与约束。
 */
class CorsConfigTest {

    @Test
    void defaultAllowedOrigins_shouldNotContainBareWildcard() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // 回归护栏：默认白名单绝不能含裸 "*"（credentials=true 下通配源是安全缺陷，会被浏览器拒绝且扩大攻击面）
        assertFalse(cors.getAllowedOriginPatterns().contains("*"),
                "默认 CORS 白名单禁止裸 *（credentials=true 下通配是安全缺陷）");
        // credentials 默认为 true，与"精确源白名单"搭配才合法
        assertTrue(cors.isAllowCredentials(),
                "默认 allowCredentials 应为 true（与精确白名单搭配）");
    }

    @Test
    void exposedHeaders_shouldContainTraceId() {
        WebProperties.Cors cors = new WebProperties.Cors();
        assertTrue(cors.getExposedHeaders().contains("trace-id"),
                "ZS-SEC-006 联动：须暴露 trace-id");
    }

    @Test
    void frameOptions_defaultShouldBeSameOriginNotDisabled() {
        assertEquals("SAMEORIGIN", new WebProperties.Cors().getFrameOptions());
    }

    @Test
    void allowedHeaders_shouldCoverAllFrontendSentHeaders() {
        WebProperties.Cors cors = new WebProperties.Cors();
        // ZS-SEC-004 P1-2：admin-web service.ts 对每个 GET 注入 Cache-Control/Pragma（防缓存），跨租户开启注入
        // visit-tenant-id，API 加密注入 X-Api-Encrypt。收窄 allowedHeaders 后若缺这些头，浏览器预检
        // Access-Control-Request-Headers 校验不通过 → 拦截所有 admin GET（含租户查询/权限加载）。
        assertTrue(cors.getAllowedHeaders().containsAll(List.of(
                        "Authorization", "Content-Type", "X-Requested-With", "tenant-id",
                        "Cache-Control", "Pragma", "visit-tenant-id", "X-Api-Encrypt")),
                "allowedHeaders 必须覆盖前端 service.ts 实际发送的全部自定义头，否则预检失败拦截请求");
    }

    @Test
    void allowedOriginPatterns_shouldMatchPortlessLocalhost() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // ZS-SEC-004 P1-1：admin-web 本地 VITE_PORT=80，浏览器发出的 Origin 为无端口的 http://localhost。
        // 用 Spring 真实匹配语义（CorsConfiguration.checkOrigin）验证白名单是否放行该无端口源；返回 null 即被拒。
        CorsConfiguration config = new CorsConfiguration();
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern);
        config.setAllowCredentials(cors.isAllowCredentials());
        assertEquals("http://localhost", config.checkOrigin("http://localhost"),
                "无端口 http://localhost 必须被 CORS 白名单放行（VITE_PORT=80 本地前端场景）");
        assertEquals("http://127.0.0.1", config.checkOrigin("http://127.0.0.1"),
                "无端口 http://127.0.0.1 必须被 CORS 白名单放行");
    }

    // ========== ZS-SEC-006 codex r0 P2-1: trace-id must be allowed as REQUEST header ==========

    @Test
    void allowedHeaders_shouldContainTraceId_forPreflight() {
        WebProperties.Cors cors = new WebProperties.Cors();
        // ZS-SEC-006 P2-1：跨域浏览器客户端发送 trace-id 请求头（由前端拦截器注入），
        // 预检 Access-Control-Request-Headers: trace-id 必须通过 allowedHeaders 校验，否则 403。
        // exposedHeaders 仅控制响应头可读性，不影响预检。
        assertTrue(cors.getAllowedHeaders().contains("trace-id"),
                "ZS-SEC-006 P2-1：allowedHeaders 必须包含 trace-id，否则跨域预检拒绝该请求头");
    }

    @Test
    void preflight_withTraceIdHeader_shouldPass() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // 构建真实 CorsConfiguration 并验证 preflight 语义
        CorsConfiguration config = new CorsConfiguration();
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern);
        config.setAllowedMethods(cors.getAllowedMethods());
        config.setAllowedHeaders(cors.getAllowedHeaders());
        config.setExposedHeaders(cors.getExposedHeaders());
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAge());

        // checkHeaders 返回 null 表示预检不通过
        List<String> result = config.checkHeaders(List.of("trace-id"));
        assertNotNull(result, "ZS-SEC-006 P2-1：预检 Access-Control-Request-Headers: trace-id 应通过");
        assertTrue(result.contains("trace-id"),
                "预检结果应包含 trace-id，实际=" + result);
    }
}
