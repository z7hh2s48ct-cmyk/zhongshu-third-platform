package cn.zszj.framework.web.core.util;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link WebFrameworkUtils} 上下文头严格解析的单元测试（ZS-SEC-008 调整 #2）。
 *
 * <p>锁定行为：缺失 / 空白 → {@code null}（视为未传递）；合法十进制非负整数 → 对应 {@link Long}；
 * present-but-malformed（小数、十六进制、科学计数、符号、非数字、超出 Long 范围）→ 受控 {@link ServiceException}（业务码 400），
 * 不再抛 {@link NumberFormatException} 逃逸为容器 500 + 栈泄露。
 */
public class WebFrameworkUtilsTest {

    private static MockHttpServletRequest tenantRequest(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, value);
        }
        return request;
    }

    private static MockHttpServletRequest visitTenantRequest(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(WebFrameworkUtils.HEADER_VISIT_TENANT_ID, value);
        }
        return request;
    }

    private static void assertBadRequest(MockHttpServletRequest request) {
        ServiceException ex = assertThrows(ServiceException.class, () -> WebFrameworkUtils.getTenantId(request));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode(), "畸形上下文头应归 400");
    }

    // ========== 缺失 / 空白 → null（视为未传递，保持既有兜底语义） ==========

    @Test
    @DisplayName("缺失 tenant-id 头 → null")
    public void testAbsentReturnsNull() {
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest(null)));
        assertNull(WebFrameworkUtils.getVisitTenantId(visitTenantRequest(null)));
    }

    @Test
    @DisplayName("空白 tenant-id 头 → null")
    public void testBlankReturnsNull() {
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("")));
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("   ")));
    }

    // ========== 合法十进制非负整数 → Long ==========

    @Test
    @DisplayName("合法整数 tenant-id → 对应 Long（含首尾空白 trim）")
    public void testValidIntegerParsed() {
        assertEquals(1L, WebFrameworkUtils.getTenantId(tenantRequest("1")));
        assertEquals(2L, WebFrameworkUtils.getTenantId(tenantRequest("  2  ")));
        assertEquals(Long.MAX_VALUE, WebFrameworkUtils.getTenantId(
                tenantRequest(String.valueOf(Long.MAX_VALUE))));
        assertEquals(9L, WebFrameworkUtils.getVisitTenantId(visitTenantRequest("9")));
    }

    // ========== present-but-malformed → 受控 400（原缺陷：isNumber 通过但 Long.valueOf 抛 NumberFormatException） ==========

    @Test
    @DisplayName("小数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
    public void testDecimalRejected() {
        assertBadRequest(tenantRequest("1.5"));
    }

    @Test
    @DisplayName("十六进制 tenant-id → 受控 400")
    public void testHexRejected() {
        assertBadRequest(tenantRequest("0x1F"));
    }

    @Test
    @DisplayName("科学计数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
    public void testScientificRejected() {
        assertBadRequest(tenantRequest("1e5"));
    }

    @Test
    @DisplayName("非数字 tenant-id → 受控 400")
    public void testNonNumericRejected() {
        assertBadRequest(tenantRequest("abc"));
    }

    @Test
    @DisplayName("带符号 tenant-id → 受控 400（拒绝负号 / 正号）")
    public void testSignedRejected() {
        assertBadRequest(tenantRequest("-1"));
        assertBadRequest(tenantRequest("+1"));
    }

    @Test
    @DisplayName("内嵌空白 tenant-id → 受控 400")
    public void testEmbeddedWhitespaceRejected() {
        assertBadRequest(tenantRequest("1 2"));
    }

    @Test
    @DisplayName("超出 Long 范围的数字串 → 受控 400（不抛 NumberFormatException）")
    public void testOverflowRejected() {
        assertBadRequest(tenantRequest("99999999999999999999"));
    }

    @Test
    @DisplayName("畸形 visit-tenant-id 同样受控 400")
    public void testVisitTenantMalformedRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> WebFrameworkUtils.getVisitTenantId(visitTenantRequest("1.5")));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }

}
