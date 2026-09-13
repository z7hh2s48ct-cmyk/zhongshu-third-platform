package cn.zszj.framework.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * ZS-PERM-004.A codex r0 P1 加固：缓存操作失败的「重试 + 显式证据」处理器。
 *
 * <p>背景：撤权/停用依赖「事务提交后驱逐缓存」；驱逐瞬时失败（Redis 抖动）会让旧授权条目残留。
 * 本处理器对 evict/clear 失败<b>同步重试 2 次</b>（覆盖瞬时抖动，权限链路宁可多等也不留旧授权）；
 * 仍失败则 ERROR 结构化日志显式登记【缓存名 + 键 + 风险 + 修复动作】——这是可重放修复证据，
 * 彻底的可靠补偿（失败键清单持久化 + 定时重放）归 ZS-LOGIN-005.B（B05 JOB-002）。
 *
 * <p>读/写失败不重试：get 异常等价未命中（回源 DB）、put 异常同理，均无安全风险。
 *
 * <p>实现 {@link org.springframework.cache.CacheErrorHandler}；经 {@code CachingConfigurer#errorHandler()}
 * 注册后对全项目 @Cacheable/@CacheEvict 生效。
 *
 * @author ZS-PERM-004.A
 */
@Slf4j
public class RetryCacheErrorHandler implements org.springframework.cache.interceptor.CacheErrorHandler {

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_INTERVAL_MS = 100L;

    @Override
    public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @Nullable Object key) {
        log.warn("[handleCacheGetError][cache({}) key({}) 读取失败按未命中回源处理]", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @Nullable Object key, @Nullable Object value) {
        log.warn("[handleCachePutError][cache({}) key({}) 写入失败（缓存 miss 仍可回源，无安全风险）]", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @Nullable Object key) {
        retryEvict(cache, key, 1);
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
        retryClear(cache, 1);
    }

    private void retryEvict(Cache cache, Object key, int attempt) {
        try {
            cache.evict(key);
            log.warn("[retryEvict][cache({}) key({}) 第 {} 次重试驱逐成功]", cache.getName(), key, attempt);
        } catch (RuntimeException ex) {
            if (attempt <= MAX_RETRIES) {
                sleep();
                retryEvict(cache, key, attempt + 1);
                return;
            }
            log.error("[retryEvict][cache({}) key({}) 驱逐重试 {} 次仍失败——旧授权条目残留，鉴权可能继续放行！"
                    + "修复动作：人工 DEL 该键或等待 TTL 过期；可靠重放补偿归 ZS-LOGIN-005.B]", cache.getName(), key, attempt, ex);
        }
    }

    private void retryClear(Cache cache, int attempt) {
        try {
            cache.clear();
            log.warn("[retryClear][cache({}) 第 {} 次重试清空成功]", cache.getName(), attempt);
        } catch (RuntimeException ex) {
            if (attempt <= MAX_RETRIES) {
                sleep();
                retryClear(cache, attempt + 1);
                return;
            }
            log.error("[retryClear][cache({}) 清空重试 {} 次仍失败——陈旧授权条目残留！"
                    + "修复动作：人工清空该 cache 前缀键或等待 TTL；可靠重放补偿归 ZS-LOGIN-005.B]", cache.getName(), attempt, ex);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_INTERVAL_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
