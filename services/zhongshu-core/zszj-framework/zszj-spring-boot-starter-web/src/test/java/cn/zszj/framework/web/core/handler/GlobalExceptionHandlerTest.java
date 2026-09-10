package cn.zszj.framework.web.core.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GlobalExceptionHandler} 的单元测试。
 *
 * 覆盖 SEC-007：系统异常落库前，query/body 必须经 {@code LogSanitizeUtils} 脱敏，秘密值不得进入 requestParams；
 * 保留可定位的错误码与 url；错误日志写入失败时只记 url+traceId，不再输出未净化的错误日志对象。
 */
public class GlobalExceptionHandlerTest {

    private static final String SECRET_QUERY = "QUERY-SECRET-DoNotLog";
    private static final String SECRET_BODY = "BODY-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String REQUEST_URI = "/admin-api/test/boom";

    private ListAppender<ILoggingEvent> listAppender;
    private Logger handlerLogger;

    @BeforeEach
    public void setUp() {
        // 初始化 WebFrameworkUtils 的静态 WebProperties，避免 getLoginUserType 访问空静态字段抛 NPE
        new WebFrameworkUtils(new WebProperties());
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        handlerLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        handlerLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        handlerLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void testDefaultExceptionHandler_requestParamsSanitized() {
        // 捕获写入的错误日志（无 Spring 代理，@Async 默认方法同步执行）
        AtomicReference<ApiErrorLogCreateReqDTO> captured = new AtomicReference<>();
        ApiErrorLogCommonApi apiErrorLogApi = captured::set;
        GlobalExceptionHandler handler = new GlobalExceptionHandler("test-app", apiErrorLogApi);

        CommonResult<?> result = handler.defaultExceptionHandler(buildRequest(), new RuntimeException("boom"));

        // 系统异常统一翻译为 INTERNAL_SERVER_ERROR，错误码保留
        assertEquals(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        ApiErrorLogCreateReqDTO errorLog = captured.get();
        assertNotNull(errorLog, "错误日志应被记录");
        String params = errorLog.getRequestParams();
        assertNotNull(params, "请求参数应被记录");
        assertFalse(params.contains(SECRET_BODY), "body 秘密不应出现在请求参数");
        assertFalse(params.contains(SECRET_QUERY), "query 秘密不应出现在请求参数");
        assertTrue(params.contains(SAFE_USERNAME), "非敏感 username 应保留");
        assertTrue(params.contains("***"), "敏感字段应被掩码");
        assertEquals(REQUEST_URI, errorLog.getRequestUrl(), "url 应保留以便定位");
    }

    @Test
    public void testCreateExceptionLog_writeFailure_doesNotLogRawErrorLog() {
        // 写库抛异常 -> createExceptionLog catch 只记 url+traceId，不再输出未净化的 errorLog 对象
        ApiErrorLogCommonApi apiErrorLogApi = dto -> {
            throw new RuntimeException("db-down");
        };
        GlobalExceptionHandler handler = new GlobalExceptionHandler("test-app", apiErrorLogApi);

        handler.defaultExceptionHandler(buildRequest(), new RuntimeException("boom"));

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
