package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link ExpressionRateLimiterKeyResolver} 的纯单元测试（ZS-SEC-010）。
 *
 * <p>验证增强后的 Key 具备「主体绑定 + 方法作用域 + 租户作用域」三重隔离：
 * <ul>
 *     <li>不同租户下的同一主体必须得到不同 Key（跨租户不共用额度）；</li>
 *     <li>不同端点（方法）下的同一主体必须得到不同 Key（跨端点不共用额度）；</li>
 *     <li>同一主体、仅改动其它普通参数（如验证码）时 Key 不变（固定主体限流不可被规避）；</li>
 *     <li>请求上下文缺失（request 为 null）时不崩溃，tenantId 以 null 占位。</li>
 * </ul>
 *
 * <p>租户来源与框架 {@code TenantContextHolder} 同源——从 {@code tenant-id} 请求头读取，
 * 避免让轻量的 protection 反向依赖 biz-tenant（分层倒置）。
 */
public class ExpressionRateLimiterKeyResolverTest {

    private static final String MOBILE = "13800138000";

    private final ExpressionRateLimiterKeyResolver resolver = new ExpressionRateLimiterKeyResolver();

    // ========== 核心用例 ==========

    @Test
    public void resolver_shouldScopeByMethodAndTenant() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // 1) 租户隔离：同一手机号、同一端点，不同租户 -> 不同 Key
            JoinPoint jp = mockJoinPoint("send", MOBILE, "1234");
            RateLimiter rl = mockRateLimiter("#mobile");

            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
            String keyTenant1 = resolver.resolver(jp, rl);

            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("2"));
            String keyTenant2 = resolver.resolver(jp, rl);

            assertNotEquals(keyTenant1, keyTenant2, "不同租户不得共用限流额度");
        }

        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // 2) 方法隔离：同一手机号、同一租户，不同端点 -> 不同 Key
            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
            String keySend = resolver.resolver(mockJoinPoint("send", MOBILE, "1234"), mockRateLimiter("#mobile"));
            String keyOther = resolver.resolver(mockJoinPoint("other", MOBILE, "1234"), mockRateLimiter("#mobile"));

            assertNotEquals(keySend, keyOther, "不同端点(方法)不得共用限流额度");
        }
    }

    @Test
    public void resolver_sameSubjectDifferentArg_shouldNotChangeKey() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));

            // 同一手机号，仅改动验证码（普通参数）-> Key 不变，无法规避固定主体限流
            String key1 = resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile"));
            String key2 = resolver.resolver(mockJoinPoint("send", MOBILE, "2222"), mockRateLimiter("#mobile"));

            assertEquals(key1, key2, "改验证码等普通参数不得更换 Key（固定主体限流不可规避）");
            // Key 已做方法+租户作用域哈希，不等于主体原值（防跨端点/跨租户共用）
            assertNotEquals(MOBILE, key1, "Key 应含方法+租户作用域(MD5)，而非主体原值");
        }
    }

    @Test
    public void resolver_whenNoRequestContext_shouldNotCrash() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // 非 web 上下文（request 为 null）：tenantId 以 null 占位，仍应稳定产出 Key，不抛异常
            su.when(ServletUtils::getRequest).thenReturn(null);

            String key = assertDoesNotThrow(() ->
                    resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile")));

            assertNotNull(key, "无请求上下文时仍应产出非空 Key");
        }
    }

    // ========== 测试脚手架 ==========

    private JoinPoint mockJoinPoint(String methodName, Object... args) {
        Method method;
        try {
            method = Fixture.class.getMethod(methodName, String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        MethodSignature signature = new FixedMethodSignature(method);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private RateLimiter mockRateLimiter(String keyArg) {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.keyArg()).thenReturn(keyArg);
        return rateLimiter;
    }

    private MockHttpServletRequest requestWithTenant(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (tenantId != null) {
            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, tenantId);
        }
        return request;
    }

    /** 被测方法所在的固定夹具类，提供真实 Method 供参数名发现使用（依赖 -parameters 编译） */
    static class Fixture {
        public void send(String mobile, String code) {
        }

        public void other(String mobile, String code) {
        }
    }

    /**
     * 真实的 {@link MethodSignature} 实现：Mockito 无法 stub {@code toString()}，
     * 而解析器用 {@code getSignature().toString()} 作为方法作用域因子，故需返回确定性的方法描述。
     */
    @SuppressWarnings({"rawtypes"})
    static class FixedMethodSignature implements MethodSignature {
        private final Method method;

        FixedMethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Class[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public Class[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String[] getParameterNames() {
            java.lang.reflect.Parameter[] params = method.getParameters();
            String[] names = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                names[i] = params[i].getName();
            }
            return names;
        }

        @Override
        public String toString() {
            return method.toString();
        }

        @Override
        public String toShortString() {
            return method.toString();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }
    }

}
