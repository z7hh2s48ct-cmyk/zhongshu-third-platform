package cn.zszj.framework.signature.core.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.signature.core.annotation.ApiSignature;
import cn.zszj.framework.signature.core.redis.ApiSignatureRedisDAO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ApiSignatureAspect} 的单元测试。
 *
 * 覆盖 SEC-007：签名校验失败被拒绝时，方法参数必须先经 {@code LogSanitizeUtils.sanitizeArgs} 脱敏再写日志，
 * 秘密值（password/token/嵌套 apiKey）不得出现在应用日志中；
 * 同时保留可定位的方法描述与非敏感字段，并抛出携带正确错误码的 ServiceException。
 */
@ExtendWith(MockitoExtension.class)
public class ApiSignatureAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String SAFE_NOTE = "hello-note";
    private static final String METHOD_DESC = "OpenApiController.queryOrder(..)";

    @Mock
    private ApiSignatureRedisDAO signatureRedisDAO;

    private ApiSignatureAspect apiSignatureAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    public void setUp() {
        apiSignatureAspect = new ApiSignatureAspect(signatureRedisDAO);
        aspectLogger = (Logger) LoggerFactory.getLogger(ApiSignatureAspect.class);
        aspectLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        aspectLogger.detachAppender(listAppender);
        listAppender.stop();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testBeforePointCut_signatureFailed_argsSanitizedInLog() {
        // 准备请求上下文：缺失加签 Header -> verifyHeaders 直接失败 -> verifySignature 返回 false
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        // 准备签名注解：仅失败路径会用到的属性
        ApiSignature signature = mock(ApiSignature.class);
        when(signature.appId()).thenReturn("appId");
        // 准备 joinPoint：可定位的方法描述 + 含秘密的参数
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{buildSensitiveArgs()});

        // 调用，断言抛出 ServiceException 且错误码保留
        ServiceException ex = assertThrows(ServiceException.class,
                () -> apiSignatureAspect.beforePointCut(joinPoint, signature));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());

        // 断言日志：秘密值不出现，非敏感字段与方法描述保留，敏感字段被掩码
        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_PASSWORD), "password 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_TOKEN), "token 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_APIKEY), "嵌套 apiKey 秘密不应出现在日志");
        assertTrue(logText.contains(SAFE_USERNAME), "非敏感 username 应保留");
        assertTrue(logText.contains(SAFE_NOTE), "非敏感 note 应保留");
        assertTrue(logText.contains(METHOD_DESC), "方法描述应保留以便定位");
        assertTrue(logText.contains("***"), "敏感字段应被掩码");
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
        return req;
    }

    private String capturedLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
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
