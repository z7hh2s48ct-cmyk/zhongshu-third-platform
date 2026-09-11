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

}
