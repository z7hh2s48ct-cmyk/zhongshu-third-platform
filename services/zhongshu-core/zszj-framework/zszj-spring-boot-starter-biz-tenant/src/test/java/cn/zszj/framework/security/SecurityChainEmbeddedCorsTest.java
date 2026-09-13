package cn.zszj.framework.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static cn.zszj.framework.security.fixture.MockOAuth2TokenApi.TENANT_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-012.B codex r0 P1：CORS 端到端【嵌入式真实容器】测试。
 *
 * <p>为什么需要：MockMvc 不按容器顺序应用 FilterRegistrationBean——安全链
 * {@code .cors(withDefaults())} 的 fallback 会先回显任意 Origin，掩盖 SEC-004 白名单
 * CorsFilter 的失败关闭行为。本类以 {@code RANDOM_PORT} 启动真实 Servlet 容器，
 * 过滤器按注册顺序（CORS_FILTER=Integer.MIN_VALUE 最先）真实执行：
 * 非批准源预检/实际请求必须 403 短路，批准源放行且携带 allowed/exposed headers。
 *
 * @author ZS-SEC-012.B
 */
@SpringBootTest(classes = cn.zszj.framework.security.fixture.SecurityFixtureApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("fixture")
public class SecurityChainEmbeddedCorsTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://evil.example";
    private static final String PUBLIC_URL = "/admin-api/fixture/public/hello";

    @Test
    public void preflight_disallowedOrigin_rejected403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", DISALLOWED_ORIGIN);
        headers.set("Access-Control-Request-Method", "GET");
        ResponseEntity<String> resp = restTemplate.exchange(PUBLIC_URL, HttpMethod.OPTIONS,
                new HttpEntity<>(headers), String.class);
        assertEquals(403, resp.getStatusCode().value(), "非批准源预检必须 403 短路");
        assertNull(resp.getHeaders().getAccessControlAllowOrigin(), "非批准源不得获得许可头");
    }

    @Test
    public void preflight_allowedOrigin_accepted() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);
        headers.set("Access-Control-Request-Method", "GET");
        headers.set("Access-Control-Request-Headers", "trace-id,authorization,tenant-id");
        ResponseEntity<String> resp = restTemplate.exchange(PUBLIC_URL, HttpMethod.OPTIONS,
                new HttpEntity<>(headers), String.class);
        assertEquals(200, resp.getStatusCode().value(), "批准源预检必须放行");
        assertEquals(ALLOWED_ORIGIN, resp.getHeaders().getAccessControlAllowOrigin());
        assertTrue(resp.getHeaders().getAccessControlAllowHeaders().stream()
                        .anyMatch(h -> h.toLowerCase().contains("trace-id")),
                "trace-id 必须在 allowed headers，实际: " + resp.getHeaders().getAccessControlAllowHeaders());
    }

    @Test
    public void actualRequest_allowedOrigin_allowedWithExposedTraceId() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);
        headers.set("tenant-id", String.valueOf(TENANT_1));
        ResponseEntity<String> resp = restTemplate.exchange(PUBLIC_URL, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(ALLOWED_ORIGIN, resp.getHeaders().getAccessControlAllowOrigin());
        assertTrue(resp.getHeaders().getAccessControlExposeHeaders().stream()
                        .anyMatch(h -> h.toLowerCase().contains("trace-id")),
                "SEC-006：trace-id 必须在 exposed headers 供跨源读取，实际: "
                        + resp.getHeaders().getAccessControlExposeHeaders());
        assertTrue(resp.getBody() != null && resp.getBody().contains("\"code\":0"));
    }

    @Test
    public void actualRequest_disallowedOrigin_rejected403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", DISALLOWED_ORIGIN);
        headers.set("tenant-id", String.valueOf(TENANT_1));
        ResponseEntity<String> resp = restTemplate.exchange(PUBLIC_URL, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(403, resp.getStatusCode().value(), "非批准源实际请求必须被白名单过滤器 403 短路");
        assertNull(resp.getHeaders().getAccessControlAllowOrigin(), "非批准源不得获得许可头");
    }
}
