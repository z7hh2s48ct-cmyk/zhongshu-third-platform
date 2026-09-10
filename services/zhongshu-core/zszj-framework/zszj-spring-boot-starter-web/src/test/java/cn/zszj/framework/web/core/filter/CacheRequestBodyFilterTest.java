package cn.zszj.framework.web.core.filter;

import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CacheRequestBodyFilter} 请求体大小上限的单元测试（ZS-SEC-008 调整 #3）。
 *
 * <p>锁定行为：JSON 请求声明的 Content-Length 超过上限时，在缓冲前受控拒绝（业务码 400、不进入过滤链）；
 * 合法大小 JSON 正常缓冲并放行；上限 {@code <= 0} 时不限制；上传 / 流式等非 JSON 请求被 shouldNotFilter 排除、不受此限影响。
 */
public class CacheRequestBodyFilterTest {

    private static final String JSON_URI = "/admin-api/test/echo";

    @BeforeEach
    public void setUp() {
        // 初始化 WebFrameworkUtils 的静态 WebProperties（与既有 web 测试约定一致）
        new WebFrameworkUtils(new WebProperties());
    }

    private static MockHttpServletRequest jsonRequest(int bodySize) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(JSON_URI);
        request.setContentType("application/json");
        byte[] body = new byte[bodySize];
        Arrays.fill(body, (byte) 'a');
        request.setContent(body);
        return request;
    }

    @Test
    @DisplayName("超大 JSON（Content-Length 超上限）→ 缓冲前受控拒绝 400，不进入过滤链")
    public void testOversizedJsonRejected() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(jsonRequest(100), response, chain);

        assertNull(chain.getRequest(), "超大 body 不应进入过滤链（缓冲前拒绝）");
        String content = response.getContentAsString();
        assertTrue(content.contains("400"), "应写业务码 400，实际：" + content);
        assertTrue(content.contains("请求体大小超过上限"), "应提示请求体超限，实际：" + content);
    }

    @Test
    @DisplayName("合法大小 JSON → 正常缓冲为 Wrapper 并放行")
    public void testNormalJsonPasses() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(1024);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(jsonRequest(20), new MockHttpServletResponse(), chain);

        ServletRequest passed = chain.getRequest();
        assertNotNull(passed, "合法 JSON 应放行");
        assertTrue(passed instanceof CacheRequestBodyWrapper, "放行请求应被包装为可重复读取的 Wrapper");
        assertEquals20Bytes(passed);
    }

    private static void assertEquals20Bytes(ServletRequest passed) {
        assertTrue(passed.getContentLength() == 20, "缓冲后 Content-Length 应保持原大小");
    }

    @Test
    @DisplayName("上限 <= 0 → 不施加额外大小限制（向后兼容）")
    public void testNoLimitWhenMaxNonPositive() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(-1L);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(jsonRequest(5000), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "不限制时超大 body 仍放行");
        assertTrue(chain.getRequest() instanceof CacheRequestBodyWrapper);
    }

    @Test
    @DisplayName("非 JSON（上传 / 流式）→ shouldNotFilter 排除，超大也不受此限、原样放行")
    public void testNonJsonSkipped() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/admin-api/test/upload");
        request.setContentType("multipart/form-data");
        byte[] body = new byte[5000];
        Arrays.fill(body, (byte) 'b');
        request.setContent(body);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        ServletRequest passed = chain.getRequest();
        assertNotNull(passed, "非 JSON 请求应放行（不被大小上限拦截）");
        assertFalse(passed instanceof CacheRequestBodyWrapper, "非 JSON 请求不缓冲、不包装");
    }

    @Test
    @DisplayName("被排除 URI（/actuator/）→ shouldNotFilter 排除，不缓冲")
    public void testIgnoredUriSkipped() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
        MockHttpServletRequest request = jsonRequest(100);
        request.setRequestURI("/actuator/health");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "被排除 URI 应放行");
        assertFalse(chain.getRequest() instanceof CacheRequestBodyWrapper, "被排除 URI 不缓冲");
    }

    @Test
    @DisplayName("字节大小写死校验：UTF-8 编码不影响上限判定")
    public void testUtf8BodyStillMeasuredByBytes() throws Exception {
        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(8);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(JSON_URI);
        request.setContentType("application/json");
        // 4 个中文字符 = 12 UTF-8 字节 > 8 上限
        request.setContent("中文中文".getBytes(StandardCharsets.UTF_8));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(chain.getRequest(), "按字节数（非字符数）判定超限");
    }

}
