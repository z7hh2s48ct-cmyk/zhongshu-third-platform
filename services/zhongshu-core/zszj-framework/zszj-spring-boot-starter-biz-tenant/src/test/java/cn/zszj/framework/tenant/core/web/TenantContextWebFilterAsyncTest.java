package cn.zszj.framework.tenant.core.web;

import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TenantContextWebFilter} 异步派发上下文重建的单元测试（ZS-SEC-012.B codex r2 P1）。
 *
 * <p>合同：①首次 REQUEST 派发设置租户上下文，finally 清理；②ASYNC 派发同样参与
 * （{@code shouldNotFilterAsyncDispatch()=false}）——从 tenant-id 头重建上下文供派发期
 * 组件（如异步访问日志）使用，finally 再清理。修复前 ASYNC 派发被跳过，派发期组件
 * （异步访问日志）在无上下文下记录，日志归属 tenant_id=0。
 *
 * @author ZS-SEC-012.B
 */
public class TenantContextWebFilterAsyncTest {

    private final TenantContextWebFilter filter = new TenantContextWebFilter();

    @BeforeEach
    public void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void shouldNotSkipAsyncDispatch() {
        assertEquals(false, filter.shouldNotFilterAsyncDispatch(), "必须参与 ASYNC 派发（重建上下文）");
    }

    @Test
    public void asyncDispatch_rebuildsTenantContext_andClearsAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/fixture/async/profile");
        request.setAsyncSupported(true);
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader("tenant-id", 7L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 模拟 ASYNC 派发内、日志过滤器执行点：上下文必须已重建
        final Long[] seen = new Long[1];
        FilterChain chain = (req, res) -> {
            seen[0] = TenantContextHolder.getTenantId();
        };
        filter.doFilter(request, response, chain);

        assertEquals(7L, seen[0].longValue(), "ASYNC 派发内租户上下文必须已从 tenant-id 头重建");
        assertNull(TenantContextHolder.getTenantId(), "派发结束后必须清理上下文");
    }

    @Test
    public void requestDispatch_setsAndClears_unchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/fixture/auth/profile");
        request.setAsyncSupported(true);
        request.setDispatcherType(DispatcherType.REQUEST);
        request.addHeader("tenant-id", 3L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Long[] seen = new Long[1];
        filter.doFilter(request, response, (req, res) -> seen[0] = TenantContextHolder.getTenantId());

        assertEquals(3L, seen[0].longValue());
        assertNull(TenantContextHolder.getTenantId(), "首次派发结束后清理（既有行为不变）");
    }

    @Test
    public void malformedTenantId_onAsyncDispatch_rejectedWithUnifiedExit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/fixture/async/profile");
        request.setAsyncSupported(true);
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader("tenant-id", "not-a-number");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        // SEC-008：畸形 tenant-id 受控统一出口（就地 writeJSON，业务码 400），不逃逸为容器 500
        String body = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("400"), "畸形 tenant-id 必须以业务码 400 统一出口，实际: " + body);
    }
}
