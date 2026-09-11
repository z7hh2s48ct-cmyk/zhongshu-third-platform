package cn.zszj.framework.web.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-004：CORS 由全通配收紧为精确白名单 + frameOptions 可配置
 *
 * 纯单元测试：不启动 Spring 上下文，直接校验 {@link WebProperties.Cors} 的默认值与约束。
 */
class CorsConfigTest {

    @Test
    void allowedOrigins_shouldNotBeWildcardWhenCredentialsTrue() {
        WebProperties props = new WebProperties();
        WebProperties.Cors cors = new WebProperties.Cors();
        cors.setAllowedOriginPatterns(List.of("http://localhost:*"));
        cors.setAllowCredentials(true);
        props.setCors(cors);
        assertFalse(props.getCors().getAllowedOriginPatterns().contains("*"),
                "credentials=true 时禁止 allowedOriginPattern=*");
        assertEquals(List.of("http://localhost:*"), props.getCors().getAllowedOriginPatterns());
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

}
