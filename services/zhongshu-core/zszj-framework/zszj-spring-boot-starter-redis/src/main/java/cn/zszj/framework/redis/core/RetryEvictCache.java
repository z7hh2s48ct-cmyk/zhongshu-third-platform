package cn.zszj.framework.redis.core;

import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * ZS-PERM-004.A codex r1 P1：带「驱逐/清空失败重试 + ERROR 证据」的 Cache 装饰器。
 *
 * <p>为什么需要：缓存管理器开启了事务感知（{@code setTransactionAware(true)}），
 * 事务内 evict/clear 由 {@code TransactionAwareCacheDecorator} 延迟到 afterCommit 直接调用底层 Cache——
 * 该路径<b>不经过</b> {@code CacheErrorHandler}（{@link RetryCacheErrorHandler} 的重试与证据逻辑失效）。
 * 故在 Cache 创建层（{@code TimeoutRedisCacheManager#createRedisCache}）先包一层本装饰器：
 * 事务感知装饰器 afterCommit 回调最终落在本类的 evict/clear 上，失败重试与 ERROR 证据由此生效。
 *
 * @author ZS-PERM-004.A
 */
public class RetryEvictCache implements Cache {

    /**
     * 共享的错误处理器（含重试策略与结构化证据日志）。
     */
    private static final RetryCacheErrorHandler ERROR_HANDLER = RetryCacheErrorHandler.INSTANCE;

    private final Cache delegate;

    public RetryEvictCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    @NonNull
    public String getName() {
        return delegate.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    @Nullable
    public ValueWrapper get(@Nullable Object key) {
        return delegate.get(key);
    }

    @Override
    @Nullable
    public <T> T get(@Nullable Object key, @Nullable Class<T> type) {
        return delegate.get(key, type);
    }

    @Override
    @Nullable
    public <T> T get(@Nullable Object key, @NonNull Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(@Nullable Object key, @Nullable Object value) {
        delegate.put(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(@Nullable Object key, @Nullable Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(@Nullable Object key) {
        try {
            delegate.evict(key);
        } catch (RuntimeException ex) {
            // 事务感知装饰器 afterCommit 路径不经过 CacheErrorHandler——在此统一接入重试与证据
            ERROR_HANDLER.handleCacheEvictError(ex, this, key);
        }
    }

    @Override
    public boolean evictIfPresent(@Nullable Object key) {
        try {
            return delegate.evictIfPresent(key);
        } catch (RuntimeException ex) {
            ERROR_HANDLER.handleCacheEvictError(ex, this, key);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            delegate.clear();
        } catch (RuntimeException ex) {
            ERROR_HANDLER.handleCacheClearError(ex, this);
        }
    }

}
