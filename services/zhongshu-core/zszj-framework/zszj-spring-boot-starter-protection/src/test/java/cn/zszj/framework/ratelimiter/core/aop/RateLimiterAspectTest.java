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
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimiterAspect} 的单元测试。
 *
 * 覆盖 SEC-007：限流被拒绝时，方法参数必须先经 {@code LogSanitizeUtils.sanitizeArgs} 脱敏再写日志，
 * 秘密值（password/token/嵌套 apiKey/数组内 secret）不得出现在应用日志中；
 * 同时保留可定位的方法描述与非敏感字段，并抛出携带正确错误码的 ServiceException。
 *
 * 覆盖 SEC-010：Redis 故障时，限流按 fail-open（放行 + 告警）降级，不得因缓存抖动锁死全站登录。
 */
@ExtendWith(MockitoExtension.class)
public class RateLimiterAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SECRET_LIST = "LIST-SECRET-DoNotLog";
    private static final String SECRET_CODE = "888888-CODE-SECRET-DoNotLog";
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

    /**
     * SEC-010：refresh-token 端点入参是标量 String refreshToken。
     * 旧 {@code sanitizeArgs} 只掩码对象字段、放过标量，导致完整可复用的刷新令牌明文落限流拒绝日志。
     * 修复后借「参数名感知」（refreshToken 归一后含 token 根集）自动掩码该标量凭据。
     */
    @Test
    public void testBeforePointCut_tooManyRequests_scalarRefreshTokenMasked() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        // joinPoint：MethodSignature 提供参数名 refreshToken，入参为标量令牌（复刻 controller 真实入参形态）
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"refreshToken"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{SECRET_TOKEN});
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));

        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_TOKEN), "标量 refreshToken 凭据不应出现在限流拒绝日志");
        assertTrue(logText.contains("***"), "标量凭据应经参数名感知被掩码");
    }

    /**
     * SEC-010：sms-login / reset-password 端点的 code（短信验证码）不在内置凭据根集，
     * 旧 {@code sanitizeArgs} 未接收端点级 extraKeys，导致验证码明文落限流拒绝日志。
     * 修复后经 {@code @RateLimiter(maskKeys = {"code"})} 端点级精确掩码（复刻 controller 真实入参形态）。
     */
    @Test
    public void testBeforePointCut_tooManyRequests_smsCodeMaskedViaMaskKeys() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        lenient().when(rateLimiter.maskKeys()).thenReturn(new String[]{"code"});
        // joinPoint：MethodSignature 参数名 reqVO，入参为含 code 的短信请求体
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"reqVO"});
        when(joinPoint.getSignature()).thenReturn(signature);
        Map<String, Object> smsReq = new LinkedHashMap<>();
        smsReq.put("mobile", "13800138000");
        smsReq.put("code", SECRET_CODE);
        when(joinPoint.getArgs()).thenReturn(new Object[]{smsReq});
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));

        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_CODE), "短信验证码 code 不应出现在限流拒绝日志");
        assertTrue(logText.contains("***"), "端点级 maskKeys 应将 code 掩码");
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

    /**
     * SEC-010：Redis 故障时按 fail-open 降级——放行请求（不抛 TOO_MANY_REQUESTS），并记录告警日志。
     *
     * <p>fail-open 策略实现在真实的 {@link RateLimiterRedisDAO#tryAcquire} 内（捕获 Redisson 异常返回 TRUE），
     * 故此处用「真实 DAO + 抛异常的 RedissonClient」贯穿切面链路验证，而非 mock DAO（mock 会绕过真实 catch）。
     */
    @Test
    public void testBeforePointCut_redisDown_shouldFailOpenByDefault() {
        // 真实 DAO 包裹一个访问即抛异常的 RedissonClient，模拟 Redis 连接故障
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        RateLimiterRedisDAO realDao = new RateLimiterRedisDAO(redissonClient);
        RateLimiterAspect aspectWithRealDao = new RateLimiterAspect(List.of(new FixedKeyResolver()), realDao);

        // 捕获 DAO 日志，验证 fail-open 告警到位
        Logger daoLogger = (Logger) LoggerFactory.getLogger(RateLimiterRedisDAO.class);
        Level originalLevel = daoLogger.getLevel();
        daoLogger.setLevel(Level.ERROR);
        ListAppender<ILoggingEvent> daoAppender = new ListAppender<>();
        daoAppender.start();
        daoLogger.addAppender(daoAppender);
        try {
            RateLimiter rateLimiter = mock(RateLimiter.class);
            doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
            when(rateLimiter.count()).thenReturn(2);
            when(rateLimiter.time()).thenReturn(60);
            when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
            JoinPoint joinPoint = mock(JoinPoint.class);

            // fail-open：Redis 故障不得抛 TOO_MANY_REQUESTS，应放行
            assertDoesNotThrow(() -> aspectWithRealDao.beforePointCut(joinPoint, rateLimiter),
                    "Redis 故障应 fail-open 放行，而非拒绝或异常逃逸");

            // 告警到位：DAO 记录 error 级日志，显式标明 fail-open
            String daoLog = daoAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertTrue(daoLog.contains("fail-open"), "Redis 故障应记录 fail-open 告警以便运维感知");
        } finally {
            daoLogger.detachAppender(daoAppender);
            daoAppender.stop();
            daoLogger.setLevel(originalLevel);
        }
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
