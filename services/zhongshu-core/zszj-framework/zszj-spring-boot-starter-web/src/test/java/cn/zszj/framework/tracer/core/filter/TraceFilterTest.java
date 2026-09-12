package cn.zszj.framework.tracer.core.filter;

import cn.zszj.framework.common.util.monitor.TracerUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-SEC-006：验证 TraceFilter 在无 OTel、外部合法/畸形 trace-id、过滤器早退等场景下的行为。
 *
 * RED 现状：TraceFilter 直接写入空串（TracerUtils.getTraceId() 无 Span 返回 ""），且不校验外部传入。
 */
class TraceFilterTest {

    private final TraceFilter traceFilter = new TraceFilter();

    // ========== ①无 OTel 时响应头 trace-id 非空且为合法 fallback ==========

    @Test
    void noOtel_responseHeader_nonEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        String headerValue = response.getHeader("trace-id");
        assertNotNull(headerValue, "响应头 trace-id 不应为 null");
        assertFalse(headerValue.isEmpty(), "无 OTel 时响应头 trace-id 不应为空串（核心缺口）");
        assertTrue(headerValue.matches("[0-9a-fA-F]{32}"),
                "响应头应为 32 字符十六进制关联 ID，实际=" + headerValue);
    }

    @Test
    void noOtel_requestAttributeSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        Object attr = request.getAttribute(TracerUtils.ATTR_CORRELATION_ID);
        assertNotNull(attr, "TraceFilter 应将关联 ID 绑定到请求属性");
        assertEquals(response.getHeader("trace-id"), attr,
                "响应头与请求属性应为同一关联 ID");
    }

    // ========== ②外部传入合法 trace-id：按信任边界复用 ==========

    @Test
    void externalValidTraceId_reused() throws Exception {
        String validExternal = "abcdef0123456789abcdef0123456789";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", validExternal);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        // 合法外部 trace-id 可复用为关联 ID（便于端到端串联）
        assertEquals(validExternal, response.getHeader("trace-id"),
                "合法外部 trace-id 应被复用为关联 ID");
    }

    @Test
    void externalValidTraceId_upperCaseNormalized() throws Exception {
        String upperCase = "ABCDEF0123456789ABCDEF0123456789";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", upperCase);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        // 大写合法 hex 应归一化为小写
        assertEquals(upperCase.toLowerCase(), response.getHeader("trace-id"),
                "大写合法 trace-id 应归一化为小写");
    }

    // ========== ③外部畸形/超长 trace-id：拒绝并替换为服务端生成值，不回显 ==========

    @Test
    void externalMalformedTraceId_rejected() throws Exception {
        String malformed = "<script>alert('xss')</script>";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", malformed);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        String headerValue = response.getHeader("trace-id");
        assertNotNull(headerValue);
        assertFalse(headerValue.contains(malformed), "畸形外部 trace-id 不得回显");
        assertFalse(headerValue.contains("<script>"), "不得回显注入内容");
        assertTrue(headerValue.matches("[0-9a-fA-F]{32}"),
                "应替换为服务端生成的合法关联 ID，实际=" + headerValue);
    }

    @Test
    void externalOversizedTraceId_rejected() throws Exception {
        String oversized = "a".repeat(256); // 超长
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", oversized);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        String headerValue = response.getHeader("trace-id");
        assertNotEquals(oversized, headerValue, "超长 trace-id 不得回显");
        assertTrue(headerValue.matches("[0-9a-fA-F]{32}"),
                "超长时应替换为服务端生成值，实际=" + headerValue);
    }

    @Test
    void externalEmptyTraceId_rejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        String headerValue = response.getHeader("trace-id");
        assertFalse(headerValue.isEmpty(), "空串外部 trace-id 应被替换为服务端生成值");
        assertTrue(headerValue.matches("[0-9a-fA-F]{32}"));
    }

    // ========== ④过滤器链早退（异常）时响应头仍有关联 ID ==========

    @Test
    void filterChainException_responseHeaderStillSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 模拟过滤器链中抛异常（早退）
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws IOException, ServletException {
                throw new ServletException("模拟早退");
            }
        };

        assertThrows(ServletException.class, () ->
                traceFilter.doFilter(request, response, chain));

        // 即使链抛异常，响应头应已设置关联 ID（因为设置在 chain.doFilter 之前）
        String headerValue = response.getHeader("trace-id");
        assertNotNull(headerValue, "过滤器早退时响应头仍应有关联 ID");
        assertFalse(headerValue.isEmpty(), "过滤器早退时关联 ID 不应为空");
    }

    // ========== ⑤不同请求不串号 ==========

    @Test
    void differentRequests_differentCorrelationIds() throws Exception {
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        traceFilter.doFilter(req1, res1, new MockFilterChain());
        traceFilter.doFilter(req2, res2, new MockFilterChain());

        assertNotEquals(res1.getHeader("trace-id"), res2.getHeader("trace-id"),
                "不同请求的关联 ID 不应相同（不串号）");
    }

    // ========== ⑥关联 ID 不可承载权限（纯随机，非授权凭据） ==========

    @Test
    void correlationId_notUsedAsCredential() throws Exception {
        // 验证：即使外部传入了一个"看起来像"有效 trace-id 的值，它只被用于关联，
        // 不会赋予任何额外权限。此处验证格式正确且不回显畸形值。
        String externalId = "1234567890abcdef1234567890abcdef";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("trace-id", externalId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        traceFilter.doFilter(request, response, chain);

        // 值被复用但不代表任何授权
        assertEquals(externalId, response.getHeader("trace-id"));
        // 请求属性中也是同一值（供下游日志使用）
        assertEquals(externalId, request.getAttribute(TracerUtils.ATTR_CORRELATION_ID));
    }
}
