package cn.zszj.framework.ratelimiter.core.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimiterAspect} 的单元测试。
 *
 * 覆盖 SEC-007：限流被拒绝时，方法参数必须先经 {@code LogSanitizeUtils.sanitizeArgs} 脱敏再写日志，
 * 秘密值（password/token/嵌套 apiKey/数组内 secret）不得出现在应用日志中；
 * 同时保留可定位的方法描述与非敏感字段，并抛出携带正确错误码的 ServiceException。
 */
@ExtendWith(MockitoExtension.class)
public class RateLimiterAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SECRET_LIST = "LIST-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String SAFE_NOTE = "hello-note";
    private static final String METHOD_DESC = "UserService.submitOrder(..)";

    @Mock
    private RateLimiterRedisDAO rateLimiterRedisDAO;

    private RateLimiterAspect rateLimiterAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    public void setUp() {
        rateLimiterAspect = new RateLimiterAspect(List.of(new FixedKeyResolver()), rateLimiterRedisDAO);
        aspectLogger = (Logger) LoggerFactory.getLogger(RateLimiterAspect.class);
        aspectLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        aspectLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void testBeforePointCut_tooManyRequests_argsSanitizedInLog() {
        // 准备限流注解：仅拒绝路径会用到的属性
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(100);
        when(rateLimiter.time()).thenReturn(1);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        // 准备 joinPoint：可定位的方法描述 + 含秘密的参数
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{buildSensitiveArgs()});
        // 获取限流失败 -> 触发请求过于频繁拒绝
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        // 调用，断言抛出 ServiceException 且错误码保留
        ServiceException ex = assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));
        assertEquals(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), ex.getCode());

        // 断言日志：秘密值不出现，非敏感字段与方法描述保留，敏感字段被掩码
        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_PASSWORD), "password 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_TOKEN), "token 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_APIKEY), "嵌套 apiKey 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_LIST), "数组内 secret 秘密不应出现在日志");
        assertTrue(logText.contains(SAFE_USERNAME), "非敏感 username 应保留");
        assertTrue(logText.contains(SAFE_NOTE), "非敏感 note 应保留");
        assertTrue(logText.contains(METHOD_DESC), "方法描述应保留以便定位");
        assertTrue(logText.contains("***"), "敏感字段应被掩码");
    }

    @Test
    public void testBeforePointCut_allowed_noRejectLog() {
        // 未触发限流：正常放行，不产生拒绝日志
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(true);

        rateLimiterAspect.beforePointCut(joinPoint, rateLimiter);

        assertTrue(listAppender.list.isEmpty(), "放行请求不应产生拒绝日志");
    }

    private static Map<String, Object> buildSensitiveArgs() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("username", SAFE_USERNAME);
        req.put("password", SECRET_PASSWORD);
        req.put("token", SECRET_TOKEN);
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("apiKey", SECRET_APIKEY);
        nested.put("note", SAFE_NOTE);
        req.put("nested", nested);
        req.put("list", List.of(Map.of("secret", SECRET_LIST)));
        return req;
    }

    private String capturedLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    /** 测试用固定 Key 解析器，返回常量 Key，避开对 JoinPoint 内容的依赖 */
    static class FixedKeyResolver implements RateLimiterKeyResolver {
        @Override
        public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
            return "rate-fixed-key";
        }
    }

    /** 测试用固定方法签名，toString 返回可定位的方法描述（Mockito 无法 stub toString，故用真实实现） */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class FixedSignature implements Signature {
        private final String text;

        FixedSignature(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public String toShortString() {
            return text;
        }

        @Override
        public String toLongString() {
            return text;
        }

        @Override
        public String getName() {
            return text;
        }

        @Override
        public int getModifiers() {
            return 1;
        }

        @Override
        public Class getDeclaringType() {
            return Object.class;
        }

        @Override
        public String getDeclaringTypeName() {
            return "Object";
        }
    }

}
