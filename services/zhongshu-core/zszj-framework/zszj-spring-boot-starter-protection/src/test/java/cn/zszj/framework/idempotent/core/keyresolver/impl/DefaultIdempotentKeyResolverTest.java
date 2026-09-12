package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link DefaultIdempotentKeyResolver} 单元测试。
 *
 * ZS-SEC-011.A：验证幂等键包含租户 + 用户主体作用域，
 * 不同主体/租户不得共用幂等键（主体隔离合同）；并显式钉住 null 作用域塌缩的危险语义。
 */
class DefaultIdempotentKeyResolverTest {

    private static final String METHOD_DESC = "OrderController.createOrder(..)";

    @Test
    void resolver_differentTenant_shouldProduceDifferentKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // 第一次调用：租户1，用户100
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // 第二次调用：租户2，用户100（同用户不同租户）
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(2L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // C7（MIN-8）：删无效 assertNotNull（MD5 永不 null），assertNotEquals 已足够
            assertNotEquals(k1, k2, "不同租户不得共用幂等键（主体隔离）");
        }
    }

    @Test
    void resolver_differentUser_shouldProduceDifferentKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // 第一次调用：租户1，用户100
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // 第二次调用：租户1，用户200（同租户不同用户）
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(200L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // C7（MIN-8）：删无效 assertNotNull（MD5 永不 null），assertNotEquals 已足够
            assertNotEquals(k1, k2, "不同用户不得共用幂等键（主体隔离）");
        }
    }

    @Test
    void resolver_differentUserType_shouldProduceDifferentKey() {
        // C7（MIN-8）：补 userType 隔离用例——三要素此前只验了 tenant/user，缺 userType
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // 第一次调用：租户1，用户100，userType=1（如管理员）
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(1);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // 第二次调用：同租户、同 userId，但 userType=2（如会员）
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            assertNotEquals(k1, k2, "同 tenant+userId 不同 userType 不得共用幂等键（主体隔离）");
        }
    }

    @Test
    void resolver_differentArgs_shouldProduceDifferentKey() {
        // C7（MIN-8）：补不同入参 → 不同 Key 用例（主体隔离合同组成部分：Key 已烘入 argsStr）
        HttpServletRequest req = mock(HttpServletRequest.class);
        JoinPoint jp1 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        JoinPoint jp2 = mockJoinPoint(METHOD_DESC, new Object[]{"arg2"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            servletMs.when(ServletUtils::getRequest).thenReturn(req);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp1, mockIdempotent());
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp2, mockIdempotent());

            assertNotEquals(k1, k2, "同主体不同入参不得共用幂等键（Key 已烘入 argsStr）");
        }
    }

    @Test
    void resolver_sameSubject_shouldProduceSameKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // 两次调用：同租户、同用户、同参数 -> 应产生相同 Key
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            assertEquals(k1, k2, "同主体同参数应产生相同幂等键");
        }
    }

    @Test
    void resolver_noRequestContext_shouldNotCrash() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(null);
            String key = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());
            // C6（IMP-7）：锁定确切 Key 格式（tenant/user/type 均 null 占位），
            // 而非仅 assertNotNull（MD5 恒非 null，对格式无约束力，无效断言）
            String argsStr = StrUtils.joinMethodArgs(jp);
            String expected = SecureUtil.md5(METHOD_DESC + ":null:null:null:" + argsStr);
            assertEquals(expected, key, "无 request 上下文应以 null 占位拼接、口径确定的 Key");
        }
    }

    @Test
    void resolver_nullScope_differentAnonymousSubjects_collapseToSameKey() {
        // C6（IMP-7 危险语义显式契约）：无 request 上下文时 tenant/user/type 均 null，
        // 两个「不同匿名主体」（同一 method/args、都无上下文 → 主体因子不可区分，全塌缩为 null）→ Key 相同。
        // 把"null 作用域塌缩 → 不同匿名主体共用命名空间"钉为契约，后续改动会立刻红灯。
        // 风险边界：本批无结果回放，危害限于可用性；SEC-011.B 引入持久化 + 回放后，
        // 此处升级为跨主体结果重放风险，须先堵（补 clientIP 或强制上下文）。
        JoinPoint jp1 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        JoinPoint jp2 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(null);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp1, mockIdempotent());
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp2, mockIdempotent());
            assertEquals(k1, k2,
                    "null 作用域塌缩：不同匿名主体（同 method/args、均无上下文）共用同一幂等键命名空间");
        }
    }

    // ========== Helper methods ==========

    private JoinPoint mockJoinPoint(String signatureText, Object[] args) {
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getSignature()).thenReturn(new FixedSignature(signatureText));
        when(jp.getArgs()).thenReturn(args);
        return jp;
    }

    private Idempotent mockIdempotent() {
        // C7（MIN-8）：删死 stub when(idem.keyArg())——Default 解析器从不读 keyArg
        return mock(Idempotent.class);
    }

    /** 测试用固定方法签名（Mockito 无法 stub toString，故用真实实现） */
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
