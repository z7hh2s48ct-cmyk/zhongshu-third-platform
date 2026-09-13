package cn.zszj.framework.apilog.core.filter;

import cn.zszj.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.web.config.WebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ApiAccessLogFilter} 异步请求日志行为的单元测试（ZS-SEC-012.B codex r0 P1）。
 *
 * <p>合同：①异步请求在首次派发返回时【不得】记录（此时结果未定，必然伪报成功）；
 * ②完成回调以最终结果记录，且全程恰好一条；③ASYNC 二次派发（过滤器再次执行）不产生重复记录；
 * ④错误回调以异常记录。修复前：异步成功/失败都被记为 SUCCESS（result=null 分支）。
 *
 * @author ZS-SEC-012.B
 */
public class ApiAccessLogFilterAsyncTest {

    private ApiAccessLogCommonApi apiAccessLogApi;
    private ApiAccessLogFilter filter;

    @BeforeEach
    public void setUp() {
        apiAccessLogApi = mock(ApiAccessLogCommonApi.class);
        WebProperties webProperties = new WebProperties();
        filter = new ApiAccessLogFilter(webProperties, "fixture-app", apiAccessLogApi);
        // WebFrameworkUtils 静态 properties（getLoginUserType 依赖）在非 Spring 环境需手动注入
        org.springframework.test.util.ReflectionTestUtils.setField(
                cn.zszj.framework.web.core.util.WebFrameworkUtils.class, "properties", webProperties);
    }

    private MockHttpServletRequest asyncRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/fixture/async/profile");
        request.addHeader("tenant-id", 1L);
        request.setAsyncSupported(true); // MockHttpServletRequest 默认不支持 async，需显式开启
        return request;
    }

    @Test
    public void asyncSuccess_logOnlyOnceAfterComplete_withFinalResult() throws Exception {
        MockHttpServletRequest request = asyncRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainReturned = {false};
        FilterChain chain = (req, res) -> {
            req.setAttribute("common_result", CommonResult.success("ok")); // 与 WebFrameworkUtils.REQUEST_ATTRIBUTE_COMMON_RESULT 一致（模拟异步阶段写入结果）
            req.startAsync(req, res);
            chainReturned[0] = true;
            // 注意：此处不 complete——真实世界完成发生在首次派发返回之后
        };

        filter.doFilter(request, response, chain);

        // ① 首次派发返回（未完成）时不得记录——伪报成功回归点
        verify(apiAccessLogApi, times(0)).createApiAccessLogAsync(any());
        assertTrue(chainReturned[0], "前置：异步已启动");

        // 异步完成 → 恰好一条成功日志（最终结果）
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();
        asyncContext.complete();
        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue(), "完成回调必须以最终成功码记录");
    }

    @Test
    public void asyncError_loggedWithFailure_notSuccess() throws Exception {
        MockHttpServletRequest request = asyncRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> req.startAsync(req, res);

        filter.doFilter(request, response, chain);
        verify(apiAccessLogApi, times(0)).createApiAccessLogAsync(any());

        // 异步错误回调（模拟异步阶段异常经 AsyncListener.onError 上报）
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();
        for (AsyncListener listener : asyncContext.getListeners()) {
            listener.onError(new AsyncEvent(asyncContext, request, response,
                    new IllegalStateException("async-boom")));
        }

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        List<ApiAccessLogCreateReqDTO> logged = captor.getAllValues();
        assertEquals(1, logged.size());
        assertTrue(logged.get(0).getResultCode() == null || logged.get(0).getResultCode() != 0L,
                "异步错误不得伪报成功");
    }

    @Test
    public void asyncRedispatch_logStillExactlyOnce() throws Exception {
        MockHttpServletRequest request = asyncRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            req.setAttribute("common_result", CommonResult.success("ok"));
            req.startAsync(req, res);
        };
        filter.doFilter(request, response, chain);

        // 模拟 ASYNC 二次派发：complete 后过滤器再次执行（真实容器行为）
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();
        asyncContext.complete();
        filter.doFilter(request, response, chain);

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue(), "全程恰好一条最终结果日志");
    }

    @Test
    public void syncRequest_logImmediately_unchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/fixture/public/hello");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute("common_result", CommonResult.success("ok"));

        filter.doFilter(request, response, (req, res) -> { });

        ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
        verify(apiAccessLogApi, times(1)).createApiAccessLogAsync(captor.capture());
        assertEquals(0L, captor.getValue().getResultCode().longValue());
    }
}
