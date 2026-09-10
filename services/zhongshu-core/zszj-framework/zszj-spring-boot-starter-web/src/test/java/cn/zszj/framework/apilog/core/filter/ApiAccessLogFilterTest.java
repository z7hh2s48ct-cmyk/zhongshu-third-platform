package cn.zszj.framework.apilog.core.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApiAccessLogFilter} 的单元测试。
 *
 * 覆盖 SEC-007：访问日志落库前，query/body 必须经 {@code LogSanitizeUtils} 脱敏，秘密值不得进入 requestParams；
 * 默认不记录响应正文；日志写入失败时只记 url+traceId，不再输出未净化的访问日志对象。
 */
public class ApiAccessLogFilterTest {

    private static final String SECRET_QUERY = "QUERY-SECRET-DoNotLog";
    private static final String SECRET_BODY = "BODY-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String REQUEST_URI = "/admin-api/test/hello";

    private ListAppender<ILoggingEvent> listAppender;
    private Logger filterLogger;

    @BeforeEach
    public void setUp() {
        // 初始化 WebFrameworkUtils 的静态 WebProperties，避免 getLoginUserType 访问空静态字段抛 NPE
        new WebFrameworkUtils(new WebProperties());
        filterLogger = (Logger) LoggerFactory.getLogger(ApiAccessLogFilter.class);
        filterLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        filterLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        filterLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void testDoFilter_requestParamsSanitized_responseBodyNotRecorded() throws Exception {
        // 捕获写入的访问日志（无 Spring 代理，@Async 默认方法同步执行）
        AtomicReference<ApiAccessLogCreateReqDTO> captured = new AtomicReference<>();
        ApiAccessLogCommonApi apiAccessLogApi = captured::set;
        ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "test-app", apiAccessLogApi);

        filter.doFilter(buildRequest(), new MockHttpServletResponse(), new MockFilterChain());

        ApiAccessLogCreateReqDTO accessLog = captured.get();
        assertNotNull(accessLog, "访问日志应被记录");
        String params = accessLog.getRequestParams();
        assertNotNull(params, "请求参数应被记录");
        assertFalse(params.contains(SECRET_BODY), "body 秘密不应出现在请求参数");
        assertFalse(params.contains(SECRET_QUERY), "query 秘密不应出现在请求参数");
        assertTrue(params.contains(SAFE_USERNAME), "非敏感 username 应保留");
        assertTrue(params.contains("***"), "敏感字段应被掩码");
        // responseEnable 默认 false -> 不记录响应正文
        assertNull(accessLog.getResponseBody(), "默认不应记录响应正文");
        // 无异常、无 CommonResult 属性 -> 结果码为成功；url 保留以便定位
        assertEquals(GlobalErrorCodeConstants.SUCCESS.getCode(), accessLog.getResultCode());
        assertEquals(REQUEST_URI, accessLog.getRequestUrl());
    }

    @Test
    public void testDoFilter_writeFailure_doesNotLogRawAccessLog() throws Exception {
        // 写库抛异常 -> filter catch 只记 url+traceId，不再输出未净化的 accessLog 对象
        ApiAccessLogCommonApi apiAccessLogApi = dto -> {
            throw new RuntimeException("db-down");
        };
        ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "test-app", apiAccessLogApi);

        filter.doFilter(buildRequest(), new MockHttpServletResponse(), new MockFilterChain());

        String logText = capturedLog();
        assertTrue(logText.contains(REQUEST_URI), "写入失败日志应保留 url 以便定位");
        assertFalse(logText.contains(SECRET_BODY), "写入失败日志不应包含 body 秘密");
        assertFalse(logText.contains(SECRET_QUERY), "写入失败日志不应包含 query 秘密");
        assertFalse(logText.contains(SAFE_USERNAME), "写入失败日志不应输出未净化的请求参数对象");
    }

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(REQUEST_URI);
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent(("{\"password\":\"" + SECRET_BODY + "\",\"username\":\"" + SAFE_USERNAME + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        request.addParameter("token", SECRET_QUERY);
        return request;
    }

    private String capturedLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

}
