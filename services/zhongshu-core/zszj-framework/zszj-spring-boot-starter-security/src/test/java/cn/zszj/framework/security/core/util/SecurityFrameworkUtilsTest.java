package cn.zszj.framework.security.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link SecurityFrameworkUtils} 的单元测试（ZS-SEC-003：Token 传输与连接凭据规范）。
 *
 * 覆盖：严格 Bearer 前缀解析、子串误解析回归护栏、缺失/畸形、重复冲突拒绝、Header 优先、
 * URL 参数通道开关（WebSocket 获准连接 vs 服务端禁用），验证「缺失、畸形、重复结果一致」。
 */
@DisplayName("SecurityFrameworkUtils - Token 解析（ZS-SEC-003）")
class SecurityFrameworkUtilsTest {

    private static final String HEADER = "Authorization";
    private static final String PARAM = "token";

    private static String obtain(MockHttpServletRequest request, boolean parameterEnabled) {
        return SecurityFrameworkUtils.obtainAuthorization(request, HEADER, PARAM, parameterEnabled);
    }

    @Test
    @DisplayName("Header 携带标准 Bearer 前缀 -> 剥离前缀返回 Token")
    void headerWithBearerPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer abc123");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("Bearer scheme 大小写不敏感（RFC 7235）-> 正常解析")
    void bearerSchemeCaseInsensitive() {
        MockHttpServletRequest lower = new MockHttpServletRequest();
        lower.addHeader(HEADER, "bearer abc123");
        assertEquals("abc123", obtain(lower, true));

        MockHttpServletRequest mixed = new MockHttpServletRequest();
        mixed.addHeader(HEADER, "BeArEr abc123");
        assertEquals("abc123", obtain(mixed, true));
    }

    @Test
    @DisplayName("Header 裸 Token（无 Bearer 前缀）-> 向后兼容返回原值")
    void headerBareToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "abc123");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("回归护栏：子串 'xBearer y' 不再被误解析为 'y'（旧 indexOf 缺陷）")
    void substringBearerNotMisparsed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "xBearer y");
        // 旧实现 indexOf("Bearer ") 命中位置 1 后 substring(+7) 误返回 "y"；严格前缀解析下按裸 Token 返回原值
        assertEquals("xBearer y", obtain(request, true));
    }

    @Test
    @DisplayName("畸形：'Bearer '（前缀后空 Token）-> null")
    void malformedEmptyAfterBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer ");
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("畸形：'Bearer'（scheme 单独出现）-> null")
    void malformedBearerAlone() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer");
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("畸形：'BearerXyz'（scheme 后无空白分隔）-> null")
    void malformedNoWhitespaceSeparator() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "BearerXyz");
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("缺失：无 Header 无 Parameter -> null")
    void missingCredential() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("多空白 trim：'Bearer   abc123   ' -> 'abc123'")
    void trimsSurroundingWhitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer   abc123   ");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("Parameter 回退（WebSocket 获准连接）：无 Header + token 参数 -> 返回 Token")
    void parameterFallbackWhenHeaderAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(PARAM, "abc123");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("Parameter 携带 Bearer 前缀 -> 剥离返回 Token")
    void parameterWithBearerPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(PARAM, "Bearer abc123");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("参数通道关闭：无 Header + token 参数 + parameterEnabled=false -> null（禁止 URL 凭据）")
    void parameterChannelDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(PARAM, "abc123");
        assertNull(obtain(request, false));
    }

    @Test
    @DisplayName("Header 优先于 Parameter：两者并存 -> 取 Header，忽略 Parameter")
    void headerTakesPrecedenceOverParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer headerToken");
        request.setParameter(PARAM, "paramToken");
        assertEquals("headerToken", obtain(request, true));
    }

    @Test
    @DisplayName("重复冲突：两个不同 Authorization 头 -> null（凭据走私拒绝）")
    void conflictingDuplicateHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer a");
        request.addHeader(HEADER, "Bearer b");
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("重复归一：两个相同 Authorization 头 -> 正常返回")
    void identicalDuplicateHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "Bearer same");
        request.addHeader(HEADER, "Bearer same");
        assertEquals("same", obtain(request, true));
    }

    @Test
    @DisplayName("重复冲突：两个不同 token 参数 -> null")
    void conflictingDuplicateParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(PARAM, "a");
        request.addParameter(PARAM, "b");
        assertNull(obtain(request, true));
    }

    @Test
    @DisplayName("空 Header 回退 Parameter：Header 为空白 + token 参数 -> 取 Parameter")
    void blankHeaderFallsBackToParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "   ");
        request.setParameter(PARAM, "abc123");
        assertEquals("abc123", obtain(request, true));
    }

    @Test
    @DisplayName("parameterName 为空时忽略参数通道：仅 Header 生效")
    void blankParameterNameDisablesParameterChannel() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(PARAM, "abc123");
        // parameterName 传空 -> 即便 parameterEnabled=true 也不读取参数
        assertNull(SecurityFrameworkUtils.obtainAuthorization(request, HEADER, "", true));
    }
}
