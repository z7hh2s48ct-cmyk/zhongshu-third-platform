package cn.zszj.framework.common.util.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LogSanitizeUtils} 单元测试
 *
 * 覆盖 SEC-007 验收：正常 / 大小写与分隔符别名 / 嵌套数组 / 端点追加键 / 畸形 JSON /
 * 脱敏器失败 / 超长截断 等场景下，秘密值均不出现在返回文本与应用日志中；
 * 同时保留字段名、状态码等可定位信息。
 */
class LogSanitizeUtilsTest {

    private static final String SECRET = "SuperSecret123";

    // ========== sanitizeJson：正常脱敏 ==========

    @Test
    void sanitizeJson_shouldMaskSensitiveKeys() {
        String json = "{\"username\":\"tom\",\"password\":\"" + SECRET + "\",\"accessToken\":\"" + SECRET + "\"}";

        String result = LogSanitizeUtils.sanitizeJson(json);

        assertFalse(result.contains(SECRET)); // 秘密值绝不出现
        assertTrue(result.contains("\"password\":\"***\"")); // 命中键掩码，字段名保留
        assertTrue(result.contains("\"accessToken\":\"***\""));
        assertTrue(result.contains("\"username\":\"tom\"")); // 非敏感字段原样保留
    }

    @Test
    void sanitizeJson_shouldBeCaseInsensitiveAndSeparatorAgnostic() {
        String json = "{\"PASSWORD\":\"" + SECRET + "\",\"user_password\":\"" + SECRET + "\",\"X-Api-Key\":\""
                + SECRET + "\",\"Authorization\":\"" + SECRET + "\"}";

        String result = LogSanitizeUtils.sanitizeJson(json);

        assertFalse(result.contains(SECRET));
        assertTrue(result.contains("\"PASSWORD\":\"***\"")); // 大小写不敏感
        assertTrue(result.contains("\"user_password\":\"***\"")); // 下划线归一
        assertTrue(result.contains("\"X-Api-Key\":\"***\"")); // 中划线归一 + 别名 apikey
        assertTrue(result.contains("\"Authorization\":\"***\""));
    }

    @Test
    void sanitizeJson_shouldMaskNestedObjectAndArray() {
        String json = "{\"user\":{\"name\":\"tom\",\"secret\":\"" + SECRET + "\"},\"list\":[{\"token\":\""
                + SECRET + "\"},{\"ok\":\"fine\"}]}";

        String result = LogSanitizeUtils.sanitizeJson(json);

        assertFalse(result.contains(SECRET));
        assertTrue(result.contains("\"secret\":\"***\"")); // 嵌套对象
        assertTrue(result.contains("\"token\":\"***\"")); // 数组元素对象
        assertTrue(result.contains("\"name\":\"tom\""));
        assertTrue(result.contains("\"ok\":\"fine\""));
    }

    @Test
    void sanitizeJson_shouldMaskEndpointExtraKeys() {
        String json = "{\"pinCode\":\"" + SECRET + "\",\"name\":\"tom\"}";

        String result = LogSanitizeUtils.sanitizeJson(json, "pin_code"); // 归一后精确匹配 pinCode

        assertFalse(result.contains(SECRET));
        assertTrue(result.contains("\"pinCode\":\"***\""));
        assertTrue(result.contains("\"name\":\"tom\""));
    }

    @Test
    void sanitizeJson_shouldReturnNullOnEmpty() {
        assertNull(LogSanitizeUtils.sanitizeJson(null));
        assertNull(LogSanitizeUtils.sanitizeJson(""));
    }

    // ========== sanitizeJson：畸形 JSON 失败降级，且不泄露原文到日志 ==========

    @Test
    void sanitizeJson_shouldDegradeOnMalformedWithoutLeakingRaw() {
        String malformed = "{\"password\":\"" + SECRET + "\""; // 缺失右括号，无法解析
        Logger logger = (Logger) LoggerFactory.getLogger(LogSanitizeUtils.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String result = LogSanitizeUtils.sanitizeJson(malformed);

            // 返回值：降级为摘要占位，绝不含原文秘密
            assertTrue(result.startsWith("<unparseable:"));
            assertTrue(result.contains("len="));
            assertFalse(result.contains(SECRET));
            // 应用日志：只记摘要（长度 / 原因），不打印未净化原文
            boolean leakedToLog = appender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains(SECRET));
            assertFalse(leakedToLog);
        } finally {
            logger.detachAppender(appender);
        }
    }

    // ========== sanitizeJson：体积上限截断 ==========

    @Test
    void sanitizeJson_shouldTruncateOversizedText() {
        String json = "{\"note\":\"" + "x".repeat(3000) + "\"}";

        String result = LogSanitizeUtils.sanitizeJson(json);

        assertTrue(result.contains("...[truncated,total="));
        assertTrue(result.length() < 2200); // 上限 2048 + 截断标记
    }

    // ========== sanitizeMap ==========

    @Test
    void sanitizeMap_shouldMaskSensitiveKeys() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", "tom");
        map.put("password", SECRET);

        String result = LogSanitizeUtils.sanitizeMap(map);

        assertFalse(result.contains(SECRET));
        assertTrue(result.contains("\"password\":\"***\""));
        assertTrue(result.contains("\"username\":\"tom\""));
        assertNull(LogSanitizeUtils.sanitizeMap(null));
    }

    // ========== sanitizeArgs：保护切面方法参数 ==========

    @Test
    void sanitizeArgs_shouldMaskSecretAndPlaceholderNonSerializable() {
        Object[] args = new Object[]{new LoginReq("tom", SECRET), new Boom(), null, 42};

        String result = LogSanitizeUtils.sanitizeArgs(args);

        assertFalse(result.contains(SECRET)); // 参数对象内的秘密被掩码
        assertTrue(result.contains("\"password\":\"***\""));
        assertTrue(result.contains("<Boom>")); // 不可序列化参数降级为类型占位
        assertTrue(result.contains("null"));
        assertTrue(result.contains("42"));
        assertEquals("[]", LogSanitizeUtils.sanitizeArgs(null));
    }

    // ========== sanitizeResponseBody：保留可定位状态码 ==========

    @Test
    void sanitizeResponseBody_shouldMaskSecretAndPreserveCode() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 1);
        data.put("refreshToken", SECRET);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("msg", "success");
        body.put("data", data);

        String result = LogSanitizeUtils.sanitizeResponseBody(body);

        assertFalse(result.contains(SECRET));
        assertTrue(result.contains("\"refreshToken\":\"***\""));
        assertTrue(result.contains("\"code\":0")); // 状态码保留，可定位
        assertTrue(result.contains("\"msg\":\"success\""));
        assertTrue(result.contains("\"id\":1"));
        assertNull(LogSanitizeUtils.sanitizeResponseBody(null));
    }

    // ========== 测试辅助 POJO ==========

    static class LoginReq {
        public String username;
        public String password;

        LoginReq(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * 序列化时 getter 抛异常，用于验证不可序列化对象的降级占位
     */
    static class Boom {
        public String getValue() {
            throw new IllegalStateException("boom");
        }
    }

}
