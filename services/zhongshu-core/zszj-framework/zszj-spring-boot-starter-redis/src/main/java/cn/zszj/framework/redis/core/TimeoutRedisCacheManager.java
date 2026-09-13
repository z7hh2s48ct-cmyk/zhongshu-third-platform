package cn.zszj.framework.redis.core;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;

/**
 * 支持自定义过期时间的 {@link RedisCacheManager} 实现类
 *
 * 在 {@link Cacheable#cacheNames()} 格式为 "key#ttl" 时，# 后面的 ttl 为过期时间。
 * 单位为最后一个字母（支持的单位有：d 天，h 小时，m 分钟，s 秒），默认单位为 s 秒
 *
 * @author 芋道源码
 */
public class TimeoutRedisCacheManager extends RedisCacheManager {

    private static final String SPLIT = "#";

    public TimeoutRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        if (StrUtil.isEmpty(name)) {
            return super.createRedisCache(name, cacheConfig);
        }
        // 如果使用 # 分隔，大小不为 2，则说明不使用自定义过期时间
        String[] names = StrUtil.splitToArray(name, SPLIT);
        if (names.length != 2) {
            return super.createRedisCache(name, cacheConfig);
        }

        // 核心：通过修改 cacheConfig 的过期时间，实现自定义过期时间
        if (cacheConfig != null) {
            // 移除 # 后面的 : 以及后面的内容，避免影响解析
            String ttlStr = StrUtil.subBefore(names[1], StrUtil.COLON, false); // 获得 ttlStr 时间部分
            names[1] = StrUtil.subAfter(names[1], ttlStr, false); // 移除掉 ttlStr 时间部分
            // 解析时间
            Duration duration = parseDuration(ttlStr);
            cacheConfig = cacheConfig.entryTtl(duration);
        }

        // 创建 RedisCache 对象，需要忽略掉 ttlStr
        return super.createRedisCache(names[0] + names[1], cacheConfig);
    }

    /**
     * 解析过期时间 Duration
     *
     * @param ttlStr 过期时间字符串
     * @return 过期时间 Duration
     */
    private Duration parseDuration(String ttlStr) {
        String timeUnit = StrUtil.subSuf(ttlStr, -1);
        switch (timeUnit) {
            case "d":
                return Duration.ofDays(removeDurationSuffix(ttlStr));
            case "h":
                return Duration.ofHours(removeDurationSuffix(ttlStr));
            case "m":
                return Duration.ofMinutes(removeDurationSuffix(ttlStr));
            case "s":
                return Duration.ofSeconds(removeDurationSuffix(ttlStr));
            default:
                return Duration.ofSeconds(Long.parseLong(ttlStr));
        }
    }

    /**
     * 移除多余的后缀，返回具体的时间
     *
     * @param ttlStr 过期时间字符串
     * @return 时间
     */
    private Long removeDurationSuffix(String ttlStr) {
        return NumberUtil.parseLong(StrUtil.sub(ttlStr, 0, ttlStr.length() - 1));
    }


    /**
     * ZS-PERM-004.A codex r2 P1：在【事务装饰器内侧】包「驱逐/清空失败重试 + ERROR 证据」装饰器——
     * decorateCache 先以 RetryEvictCache 包住原始 RedisCache，再由 super 应用事务感知装饰；
     * 最终链为 TransactionAwareCacheDecorator → RetryEvictCache → RedisCache，
     * 事务内 evict/clear 延迟到 afterCommit 后仍落在 RetryEvictCache 上，重试与证据由此生效。
     * （codex r2 修正：此前包装在 getCache 外侧，afterCommit 直接调底层 RedisCache 绕过重试。）
     */
    @Override
    protected org.springframework.cache.Cache decorateCache(org.springframework.cache.Cache cache) {
        return super.decorateCache(new RetryEvictCache(cache));
    }
}
