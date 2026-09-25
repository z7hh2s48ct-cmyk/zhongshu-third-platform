package cn.zszj.framework.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
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
 * <p>ZS-PERM-004.C：重试耗尽分支新增「失败键清单持久化」记录点（{@link CacheEvictionFailureRecorder}，
 * SPI 反转——框架层仅调用，实现方在应用层复用 outbox）；并对受管缓存启用「版本护栏」
 * （{@link CacheVersionGuard}）：evict/clear 先 bump 版本再驱逐；get miss 快照版本、put 写回前对比——
 * 拦截「驱逐推进代际后晚到写回的旧值」。两者均以构造参数注入，null = 非受管缓存/未装配（行为与现状一致）。
 *
 * @author ZS-PERM-004.A
 */
@Slf4j
public class RetryEvictCache implements Cache {

    /**
     * 恰好重试次数（总执行次数 = 1 + MAX_RETRIES）。
     */
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_INTERVAL_MS = 100L;

    /**
     * ZS-PERM-004.C：快照不可校验哨兵——get miss 时读版本失败则存入本哨兵，
     * put 侧识别后一律丢弃写回（fail-safe 不缓存，回源 DB 自愈）。
     */
    private static final Object UNVERIFIABLE_SENTINEL = new Object();

    private final Cache delegate;

    /** ZS-PERM-004.C：驱逐失败记录点（null = 非受管缓存/未装配，行为与现状一致）。 */
    private final CacheEvictionFailureRecorder failureRecorder;

    /** ZS-PERM-004.C：版本护栏——防晚到回填旧值写回（null = 非受管缓存/未装配）。 */
    private final CacheVersionGuard versionGuard;

    /**
     * ZS-PERM-004.C：本线程 get miss 时的版本快照（per-instance ThreadLocal，防跨线程串扰）。
     * 生命周期：get miss → 写入；get 命中 → 清除；put → 一次性消费（无论放行与否均移除）。
     */
    private final ThreadLocal<Map<Object, Object>> versionSnapshots = ThreadLocal.withInitial(HashMap::new);

    public RetryEvictCache(Cache delegate) {
        this(delegate, null, null);
    }

    /**
     * ZS-PERM-004.C：受管缓存构造（记录点 + 版本护栏；任一为 null 即对应能力关闭）。
     */
    public RetryEvictCache(Cache delegate, @Nullable CacheEvictionFailureRecorder failureRecorder,
                           @Nullable CacheVersionGuard versionGuard) {
        this.delegate = delegate;
        this.failureRecorder = failureRecorder;
        this.versionGuard = versionGuard;
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

    /**
     * ZS-PERM-004.C：miss 时快照版本（供后续 put 校验）；命中时清除快照（值仍新鲜，无需护栏）。
     */
    @Override
    @Nullable
    public ValueWrapper get(@Nullable Object key) {
        ValueWrapper wrapper = delegate.get(key);
        if (versionGuard != null) {
            if (wrapper != null) {
                versionSnapshots.get().remove(key);
            } else {
                captureVersionSnapshot(key);
            }
        }
        return wrapper;
    }

    /** ZS-PERM-004.C：与 {@link #get(Object)} 同逻辑（miss 快照 / 命中清除）。 */
    @Override
    @Nullable
    public <T> T get(@Nullable Object key, @Nullable Class<T> type) {
        T value = delegate.get(key, type);
        if (versionGuard != null) {
            if (value != null) {
                versionSnapshots.get().remove(key);
            } else {
                captureVersionSnapshot(key);
            }
        }
        return value;
    }

    /**
     * ZS-PERM-004.C 边界登记（防漂移）：{@code sync=true} 路径（get-with-loader）当前全仓零使用，
     * 透传不校验——loader 回调与写回均在 RedisCache 内部完成，装饰层无法拦截。
     */
    @Override
    @Nullable
    public <T> T get(@Nullable Object key, @NonNull Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    /**
     * ZS-PERM-004.C：写回校验——get miss 快照存在时对比当前版本：
     * 版本已推进（驱逐发生）→ 丢弃写回（晚到旧值拦截）；快照不可校验 → 丢弃（fail-safe）；
     * 版本读取异常 → 丢弃（fail-safe）；无快照（@CachePut 等非常规路径 / 非受管缓存）→ 直接写。
     */
    @Override
    public void put(@Nullable Object key, @Nullable Object value) {
        if (versionGuard != null && !allowWriteBack(key)) {
            return;
        }
        delegate.put(key, value);
    }

    /**
     * ZS-PERM-004.C 边界登记（防漂移）：{@code putIfAbsent} 当前全仓零使用，透传不校验。
     */
    @Override
    @Nullable
    public ValueWrapper putIfAbsent(@Nullable Object key, @Nullable Object value) {
        return delegate.putIfAbsent(key, value);
    }

    /**
     * ZS-PERM-004.C：受管缓存的驱逐 =「bump 版本 + delegate 驱逐」作为一个 operation 整体有界重试；
     * bump 成功即宣告代际推进——在途写回被 put 校验拦截；delegate 失败则由清单补偿重放兜底。
     */
    @Override
    public void evict(@Nullable Object key) {
        boundedRetry("evict cache(" + delegate.getName() + ") key(" + key + ")",
                CacheEvictionOperation.EVICT, key,
                () -> {
                    if (versionGuard != null) {
                        versionGuard.bumpVersion(delegate.getName());
                    }
                    delegate.evict(key);
                });
    }

    @Override
    public boolean evictIfPresent(@Nullable Object key) {
        // ZS-PERM-004.C：不记录（非 @CacheEvict 主路径）/ 不 bump（未涉及 @CacheEvict 语义）
        boundedRetry("evictIfPresent cache(" + delegate.getName() + ") key(" + key + ")",
                null, null,
                () -> delegate.evictIfPresent(key));
        return false; // 重试语义下无法可靠判定本次是否确有删除，保守返回 false
    }

    @Override
    public void clear() {
        boundedRetry("clear cache(" + delegate.getName() + ")",
                CacheEvictionOperation.CLEAR, null,
                () -> {
                    if (versionGuard != null) {
                        versionGuard.bumpVersion(delegate.getName());
                    }
                    delegate.clear();
                });
    }

    /**
     * 对 delegate 有界重试（总执行 1 + MAX_RETRIES 次），不经处理器回调自身——杜绝无界递归（codex r2 P1）。
     *
     * <p>ZS-PERM-004.C：重试耗尽分支新增 {@link #recordFailure}（失败键清单持久化）；
     * {@code operation == null}（evictIfPresent）或重放上下文内不记录。
     */
    private void boundedRetry(String desc, @Nullable CacheEvictionOperation operation, @Nullable Object key,
                              Runnable action) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                action.run();
                if (attempt > 0) {
                    log.warn("[boundedRetry][{} 第 {} 次重试成功]", desc, attempt);
                }
                return;
            } catch (RuntimeException ex) {
                if (attempt >= MAX_RETRIES) {
                    log.error("[boundedRetry][{} 重试 {} 次仍失败——旧授权条目可能残留，鉴权可能继续放行！"
                                    + "修复动作：人工 DEL 该键/清空该 cache 或等待 TTL；可靠重放补偿已由 ZS-PERM-004.C 交付"
                                    + "（失败键清单持久化 + dispatcher 重放；DEAD 兜底见 JOB-004 人工台账）]",
                            desc, MAX_RETRIES, ex);
                    recordFailure(operation, key, ex);
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

    /**
     * ZS-PERM-004.C：get miss 时捕获版本快照；读取失败存入哨兵（fail-safe：后续写回一律丢弃）。
     */
    private void captureVersionSnapshot(@Nullable Object key) {
        try {
            versionSnapshots.get().put(key, versionGuard.currentVersion(delegate.getName()));
        } catch (RuntimeException ex) {
            versionSnapshots.get().put(key, UNVERIFIABLE_SENTINEL);
            log.warn("[captureVersionSnapshot][cache({}) key({}) 版本读取失败，本次加载写回将被丢弃]",
                    delegate.getName(), key, ex);
        }
    }

    /**
     * ZS-PERM-004.C：写回放行判定——快照一次性消费（无论放行与否均移除，防复用于后续 put）。
     *
     * @return true = 允许 delegate.put；false = 丢弃写回（版本已推进 / 快照不可校验 / 版本读取异常）
     */
    private boolean allowWriteBack(@Nullable Object key) {
        Map<Object, Object> snapshots = versionSnapshots.get();
        Object snapshot = snapshots.remove(key); // 一次性消费
        if (snapshot == null) {
            return true; // 无快照：@CachePut 等非常规路径 / 非 miss 触发，直接写（边界登记）
        }
        if (snapshot == UNVERIFIABLE_SENTINEL) {
            log.debug("[allowWriteBack][cache({}) key({}) 快照不可校验，丢弃写回（回源自愈）]",
                    delegate.getName(), key);
            return false;
        }
        long currentVersion;
        try {
            currentVersion = versionGuard.currentVersion(delegate.getName());
        } catch (RuntimeException ex) {
            log.warn("[allowWriteBack][cache({}) key({}) 版本读取失败，丢弃写回（fail-safe）]",
                    delegate.getName(), key, ex);
            return false;
        }
        if (!(snapshot instanceof Long snapshotVersion) || snapshotVersion != currentVersion) {
            log.debug("[allowWriteBack][cache({}) key({}) 快照版本({}) vs 当前版本({}) 不一致，丢弃晚到写回]",
                    delegate.getName(), key, snapshot, currentVersion);
            return false;
        }
        return true;
    }

    /**
     * ZS-PERM-004.C：重试耗尽 → 调用记录点（SPI 反转）。不记录条件（互相正交）：
     * {@code operation == null}（evictIfPresent 非 @CacheEvict 主路径）/ 未装配记录点 /
     * 重放上下文（防「记录→重放→再记录」自我循环）。
     * 记录失败仅追加 ERROR 日志，不改变驱逐结果（D2 契约：记录故障隔离）。
     */
    private void recordFailure(@Nullable CacheEvictionOperation operation, @Nullable Object key, RuntimeException cause) {
        if (operation == null || failureRecorder == null || CacheEvictionReplayContext.isInReplay()) {
            return;
        }
        try {
            failureRecorder.record(delegate.getName(), key == null ? null : key.toString(), operation, cause);
        } catch (RuntimeException ex) {
            log.error("[recordFailure][cache({}) key({}) 失败记录写入异常（不影响驱逐结果）]",
                    delegate.getName(), key, ex);
        }
    }
}
