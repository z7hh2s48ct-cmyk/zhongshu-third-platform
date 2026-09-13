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
 * 故由 {@code TimeoutRedisCacheManager#decorateCache} 在【事务装饰器内侧】包一层本装饰器：
 * afterCommit 回调最终落在本类的 evict/clear 上，有界重试与 ERROR 证据由此生效。
 *
 * <p>重试直接对 {@code delegate} 有界循环（恰好重试 2 次），不经处理器回调自身——
 * 杜绝无界递归（codex r2 P1）。
 *
 * @author ZS-PERM-004.A
 */
public class RetryEvictCache implements Cache {

    /**
     * 恰好重试次数（总执行次数 = 1 + MAX_RETRIES）。
     */
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_INTERVAL_MS = 100L;

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
        boundedRetry("evict cache(" + delegate.getName() + ") key(" + key + ")",
                () -> delegate.evict(key));
    }

    @Override
    public boolean evictIfPresent(@Nullable Object key) {
        boundedRetry("evictIfPresent cache(" + delegate.getName() + ") key(" + key + ")",
                () -> delegate.evictIfPresent(key));
        return false; // 重试语义下无法可靠判定本次是否确有删除，保守返回 false
    }

    @Override
    public void clear() {
        boundedRetry("clear cache(" + delegate.getName() + ")", delegate::clear);
    }

    /**
     * 对 delegate 有界重试（总执行 1 + MAX_RETRIES 次），不经处理器回调自身——杜绝无界递归（codex r2 P1）。
     */
    private void boundedRetry(String desc, Runnable operation) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                operation.run();
                if (attempt > 0) {
                    org.slf4j.LoggerFactory.getLogger(RetryEvictCache.class)
                            .warn("[boundedRetry][{} 第 {} 次重试成功]", desc, attempt);
                }
                return;
            } catch (RuntimeException ex) {
                if (attempt >= MAX_RETRIES) {
                    org.slf4j.LoggerFactory.getLogger(RetryEvictCache.class)
                            .error("[boundedRetry][{} 重试 {} 次仍失败——旧授权条目可能残留，鉴权可能继续放行！"
                                            + "修复动作：人工 DEL 该键/清空该 cache 或等待 TTL；可靠重放补偿归 ZS-LOGIN-005.B]",
                                    desc, MAX_RETRIES, ex);
                } else {
                    try {
                        Thread.sleep(RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
    }
}
