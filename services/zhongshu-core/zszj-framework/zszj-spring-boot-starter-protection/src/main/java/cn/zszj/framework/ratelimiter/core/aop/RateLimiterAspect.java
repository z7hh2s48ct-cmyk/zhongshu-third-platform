package cn.zszj.framework.ratelimiter.core.aop;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拦截声明了 {@link RateLimiter} 注解的方法，实现限流操作
 *
 * @author 芋道源码
 */
@Aspect
@Slf4j
public class RateLimiterAspect {

    /**
     * RateLimiterKeyResolver 集合
     */
    private final Map<Class<? extends RateLimiterKeyResolver>, RateLimiterKeyResolver> keyResolvers;

    private final RateLimiterRedisDAO rateLimiterRedisDAO;

    public RateLimiterAspect(List<RateLimiterKeyResolver> keyResolvers, RateLimiterRedisDAO rateLimiterRedisDAO) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, RateLimiterKeyResolver::getClass);
        this.rateLimiterRedisDAO = rateLimiterRedisDAO;
    }

    @Before("@annotation(rateLimiter)")
    public void beforePointCut(JoinPoint joinPoint, RateLimiter rateLimiter) {
        // 获得 RateLimiterKeyResolver 对象
        RateLimiterKeyResolver keyResolver = keyResolvers.get(rateLimiter.keyResolver());
        Assert.notNull(keyResolver, "找不到对应的 RateLimiterKeyResolver");
        // 解析 Key
        String key = keyResolver.resolver(joinPoint, rateLimiter);

        // 获取 1 次限流
        boolean success = rateLimiterRedisDAO.tryAcquire(key,
                rateLimiter.count(), rateLimiter.time(), rateLimiter.timeUnit());
        if (!success) {
            // SEC-010：限流拒绝日志按「参数名 -> 值」脱敏，标量凭据（如 refreshToken）借参数名命中根集掩码，
            // 端点级敏感字段（如短信 code）经 @RateLimiter#maskKeys 精确掩码，避免明文凭据落日志
            log.info("[beforePointCut][方法({}) 参数({}) 请求过于频繁]", joinPoint.getSignature().toString(),
                    LogSanitizeUtils.sanitizeMap(buildArgMap(joinPoint), rateLimiter.maskKeys()));
            String message = StrUtil.blankToDefault(rateLimiter.message(),
                    GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getMsg());
            throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), message);
        }
    }

    /**
     * 将 joinPoint 入参构建为「参数名 -> 参数值」的有序映射，供 {@link LogSanitizeUtils#sanitizeMap} 做参数名感知脱敏。
     *
     * <p>标量入参（如 refreshToken）无法靠值本身识别敏感性，但借参数名（归一后含 token 根集）即可被自动掩码；
     * 拿不到参数名（签名非 {@link MethodSignature} 或名称缺失）时退化为位置名 {@code argN}，不阻断脱敏。
     */
    private static Map<String, Object> buildArgMap(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }
        Signature signature = joinPoint.getSignature();
        String[] parameterNames = signature instanceof MethodSignature
                ? ((MethodSignature) signature).getParameterNames() : null;
        Map<String, Object> argMap = new LinkedHashMap<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String name = parameterNames != null && i < parameterNames.length && StrUtil.isNotEmpty(parameterNames[i])
                    ? parameterNames[i] : "arg" + i;
            argMap.put(name, args[i]);
        }
        return argMap;
    }

}

