package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * 基于 Spring EL 表达式的 {@link RateLimiterKeyResolver} 实现类
 *
 * ZS-SEC-010：在表达式求得的「主体」（如手机号、用户名、令牌）之上，追加 methodName + tenantId 作用域，
 * 并做 MD5 压缩，堵死“跨端点、跨租户共用同一额度”的问题。
 * 由于 Key 只取主体字段、不含其余入参，改动密码 / 验证码等普通参数不会更换 Key（固定主体限流不可被规避），
 * 且与客户端 IP 无关（伪造转发 IP 不能绕过）。
 *
 * @author 芋道源码
 */
public class ExpressionRateLimiterKeyResolver implements RateLimiterKeyResolver {

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        // 获得被拦截方法参数名列表
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        // 准备 Spring EL 表达式解析的上下文
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        if (ArrayUtil.isNotEmpty(parameterNames)) {
            for (int i = 0; i < parameterNames.length; i++) {
                evaluationContext.setVariable(parameterNames[i], args[i]);
            }
        }

        // 解析参数，获得限流主体（如手机号、用户名、令牌）
        Expression expression = expressionParser.parseExpression(rateLimiter.keyArg());
        String subject = expression.getValue(evaluationContext, String.class);

        // ZS-SEC-010：Key 追加 methodName + tenantId 作用域，堵跨端点 / 跨租户共用额度。
        // 租户来源与 TenantContextHolder 同源（tenant-id 请求头），直接读头避免让轻量的 protection 反向依赖 biz-tenant（分层倒置）；
        // request 为 null（非 web 上下文）时 tenantId 以 null 占位，不崩溃。
        HttpServletRequest request = ServletUtils.getRequest();
        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
        String methodName = joinPoint.getSignature().toString();
        return SecureUtil.md5(methodName + ":" + tenantId + ":" + subject);
    }

    private static Method getMethod(JoinPoint point) {
        // 处理，声明在类上的情况
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        if (!method.getDeclaringClass().isInterface()) {
            return method;
        }

        // 处理，声明在接口上的情况
        try {
            return point.getTarget().getClass().getDeclaredMethod(
                    point.getSignature().getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
