package cn.zszj.framework.common.util.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-SEC-006：验证无 OTel Span 时 TracerUtils.getCorrelationId() 提供有效 fallback 关联 ID。
 *
 * RED 现状：getCorrelationId() 方法不存在（编译失败）。
 */
class TracerUtilsFallbackTest {

    @Test
    void getCorrelationId_noSpan_returnsNonEmpty() {
        // 无有效 OTel Span 时，getCorrelationId() 必须返回非空关联 ID
        String id = TracerUtils.getCorrelationId();
        assertNotNull(id, "关联 ID 不应为 null");
        assertFalse(id.isEmpty(), "关联 ID 不应为空串（核心缺口修复）");
    }

    @Test
    void getCorrelationId_noSpan_boundedLength() {
        String id = TracerUtils.getCorrelationId();
        // 有界：长度不超过 64 字符（128-bit hex = 32 字符，留余量）
        assertTrue(id.length() <= 64, "关联 ID 长度应有界，实际=" + id.length());
        assertTrue(id.length() >= 16, "关联 ID 应有足够熵，实际长度=" + id.length());
    }

    @Test
    void getCorrelationId_noSpan_validHexFormat() {
        String id = TracerUtils.getCorrelationId();
        // 格式合法：仅含十六进制字符
        assertTrue(id.matches("[0-9a-fA-F]+"), "关联 ID 应为合法十六进制格式，实际=" + id);
    }

    @Test
    void getCorrelationId_sameRequest_stable() {
        // 同一请求上下文内，连续调用返回相同 ID
        MockHttpServletRequest request = new MockHttpServletRequest();
        String id1 = TracerUtils.getCorrelationId(request);
        String id2 = TracerUtils.getCorrelationId(request);
        assertEquals(id1, id2, "同一请求内连续调用应返回相同的关联 ID（稳定性）");
    }

    @Test
    void getCorrelationId_differentRequests_noCollision() {
        // 不同请求不串号
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        String id1 = TracerUtils.getCorrelationId(req1);
        String id2 = TracerUtils.getCorrelationId(req2);
        assertNotEquals(id1, id2, "不同请求应生成不同的关联 ID（不串号）");
    }

    @Test
    void getCorrelationId_withRequestAttribute_usesAttribute() {
        // 如果请求属性已设置关联 ID，应优先使用
        MockHttpServletRequest request = new MockHttpServletRequest();
        String preset = "abcdef0123456789abcdef0123456789";
        request.setAttribute(TracerUtils.ATTR_CORRELATION_ID, preset);
        String id = TracerUtils.getCorrelationId(request);
        assertEquals(preset, id, "应优先使用请求属性中已绑定的关联 ID");
    }

    @Test
    void getTraceId_noSpan_returnsEmpty_semanticsPreserved() {
        // 保留 getTraceId() 语义不变：无 OTel Span 时仍返回空串
        String traceId = TracerUtils.getTraceId();
        assertEquals("", traceId, "getTraceId() 语义不变：无 Span 时返回空串");
    }

    @Test
    void isValidTraceIdFormat_validId() {
        assertTrue(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef0123456789"));
        assertTrue(TracerUtils.isValidTraceIdFormat("ABCDEF0123456789ABCDEF0123456789"));
        assertTrue(TracerUtils.isValidTraceIdFormat("00000000000000000000000000000000"));
    }

    @Test
    void isValidTraceIdFormat_invalidId() {
        assertFalse(TracerUtils.isValidTraceIdFormat(null));
        assertFalse(TracerUtils.isValidTraceIdFormat(""));
        assertFalse(TracerUtils.isValidTraceIdFormat("short"));
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012345678900")); // 34 chars, too long
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012345678")); // 31 chars, too short
        assertFalse(TracerUtils.isValidTraceIdFormat("zzzzzz0123456789abcdef0123456789")); // invalid chars
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012345678<script>")); // injection attempt
    }

    @Test
    void generateCorrelationId_producesValidId() {
        String id = TracerUtils.generateCorrelationId();
        assertNotNull(id);
        assertEquals(32, id.length(), "生成的关联 ID 应为 32 字符（128-bit hex）");
        assertTrue(id.matches("[0-9a-f]+"), "生成的关联 ID 应为小写十六进制");
    }

    @Test
    void generateCorrelationId_unique() {
        String id1 = TracerUtils.generateCorrelationId();
        String id2 = TracerUtils.generateCorrelationId();
        assertNotEquals(id1, id2, "连续生成的关联 ID 不应重复");
    }
}
