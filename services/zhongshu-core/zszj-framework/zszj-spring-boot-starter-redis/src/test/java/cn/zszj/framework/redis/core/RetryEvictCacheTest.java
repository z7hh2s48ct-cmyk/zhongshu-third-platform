package cn.zszj.framework.redis.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ZS-PERM-004.C：{@link RetryEvictCache} 驱逐失败补偿记录点 + 版本校验（防旧值写回）单测。
 *
 * <p><b>RED 依据</b>：骨架阶段 recorder/guard 已存储但未接线——记录点用例（用例 1/3）记录 0 次、
 * 晚到回填拦截用例（用例 7/9）put 直写、版本顺序用例（用例 12）未 bump → 全部失败；
 * GREEN 阶段接线后转绿。保护性用例（用例 2/6/8/10）骨架与 GREEN 均须通过——锁定
 * 「重试语义不破坏」与「非受管零变化」两条红线（计划 §1/§5-10）。
 *
 * <p><b>契约映射</b>（开发计划 §3 表 1~12 + §2 D1~D4）：
 * <ul>
 *   <li>D1 记录点唯一化：重试耗尽分支恰记录 1 条（op=EVICT/CLEAR，key=null 表示 CLEAR）；</li>
 *   <li>D3 重放抑制：replay scope 内不再记录（防「记录→重放→再记录」自我循环）；</li>
 *   <li>D4 版本机制：get miss 快照 → evict 先 bump 后操作 → put 对比丢弃（晚到回填拦截）/
 *       哨兵 fail-safe（读失败丢弃）/ 无快照直写（@CachePut 边界）。</li>
 * </ul>
 *
 * @author ZS-PERM-004.C
 */
public class RetryEvictCacheTest {

    private static final String CACHE_NAME = "role";

    private Cache delegate;
    private CacheEvictionFailureRecorder recorder;
    private CacheVersionGuard guard;
    private RetryEvictCache cache;

    @BeforeEach
    public void setUp() {
        delegate = mock(Cache.class);
        recorder = mock(CacheEvictionFailureRecorder.class);
        guard = mock(CacheVersionGuard.class);
        when(delegate.getName()).thenReturn(CACHE_NAME);
        cache = new RetryEvictCache(delegate, recorder, guard);
    }

    // ========== 用例 1：重试耗尽 → 恰记录 1 条（记录点主证据） ==========

    /**
     * RED：delegate.evict 恒抛 → 有界重试（总 3 次）耗尽后必须经 recorder 记录 1 条补偿证据
     * （cacheName=role / key=1 / op=EVICT / 携带根因）；骨架未接线 → 0 次 → 失败。
     */
    @Test
    public void evict_exhaustedRetries_recordsOnce() {
        doThrow(new RuntimeException("redis down")).when(delegate).evict(1L);

        cache.evict(1L);

        verify(delegate, times(3)).evict(1L); // 1 + MAX_RETRIES(2)
        verify(recorder, times(1)).record(eq(CACHE_NAME), eq("1"),
                eq(CacheEvictionOperation.EVICT), any(RuntimeException.class));
    }

    // ========== 用例 2：重试后成功 → 不记录（保护性：重试语义不破坏） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：第 2 次尝试成功 → 无补偿记录（WARN 路径不记录）。
     */
    @Test
    public void evict_retryThenSuccess_noRecord() {
        doThrow(new RuntimeException("transient")).doNothing().when(delegate).evict(1L);

        cache.evict(1L);

        verify(delegate, times(2)).evict(1L);
        verifyNoInteractions(recorder);
    }

    // ========== 用例 3：clear 重试耗尽 → key=null 记录 ==========

    /**
     * RED：clear 恒抛 → 记录 1 条（op=CLEAR / key=null 表示清空整个缓存）。
     */
    @Test
    public void clear_exhaustedRetries_recordsWithNullKey() {
        doThrow(new RuntimeException("redis down")).when(delegate).clear();

        cache.clear();

        verify(recorder, times(1)).record(eq(CACHE_NAME), isNull(),
                eq(CacheEvictionOperation.CLEAR), any(RuntimeException.class));
    }

    // ========== 用例 4：记录器抛异常 → 不传播（记录故障隔离） ==========

    /**
     * RED：recorder 抛异常时驱逐方法必须正常返回（记录失败仅降级，不影响驱逐语义与返回值）。
     */
    @Test
    public void recorderThrows_notPropagated() {
        doThrow(new RuntimeException("redis down")).when(delegate).evict(1L);
        doThrow(new RuntimeException("recorder boom")).when(recorder)
                .record(anyString(), any(), any(), any());

        assertDoesNotThrow(() -> cache.evict(1L));

        verify(delegate, times(3)).evict(1L);
        verify(recorder, times(1)).record(anyString(), any(), any(), any());
    }

    // ========== 用例 5：replay scope 内 → 不再记录（防自我循环） ==========

    /**
     * RED/GREEN 契约：replay scope 内驱逐仍执行有界重试，但恒失败不再产生新事件——
     * 交由 dispatcher 退避重试 / 超限 DEAD（JOB-004 人工台账），闭环「记录→重放→再记录」。
     */
    @Test
    public void inReplayScope_exhaustedRetries_noRecord() {
        doThrow(new RuntimeException("redis down")).when(delegate).evict(1L);

        CacheEvictionReplayContext.runInReplayScope(() -> cache.evict(1L));

        verify(delegate, times(3)).evict(1L);
        verifyNoInteractions(recorder);
    }

    // ========== 用例 6：非受管缓存 → 现状行为（保护性） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：recorder/guard 均 null（非白名单/未装配）→
     * 行为与现状一致：仅 ERROR 日志、有界重试、无记录、无版本调用。
     */
    @Test
    public void unguardedCache_noRecorderNoVersionOps() {
        RetryEvictCache unguarded = new RetryEvictCache(delegate, null, null);
        doThrow(new RuntimeException("redis down")).when(delegate).evict(1L);

        assertDoesNotThrow(() -> unguarded.evict(1L));

        verify(delegate, times(3)).evict(1L);
        verifyNoInteractions(recorder);
        verifyNoInteractions(guard);
    }

    // ========== 用例 7：晚到回填拦截（版本 bump 后 put 丢弃） ==========

    /**
     * RED：get miss 建快照（版本 0）→ evict（bump 至 1）→ put 对比版本不等 → 丢弃写回，
     * delegate.put 不得被调用（晚到旧值回填拦截）；骨架直写 → 失败。
     */
    @Test
    public void lateBackfill_discardedWhenVersionBumped() {
        when(delegate.get(1L)).thenReturn(null);
        when(guard.currentVersion(CACHE_NAME)).thenReturn(0L, 1L); // 快照=0；put 校验时=1

        cache.get(1L);
        cache.evict(1L);
        cache.put(1L, "late-stale");

        verify(delegate, never()).put(eq(1L), any());
    }

    // ========== 用例 8：版本未变 → 正常回填（保护性：不误伤） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：get miss 建快照（版本 0）→ put 校验版本仍 0 →
     * 正常写回（正常回源回填不得被误伤）。
     */
    @Test
    public void normalBackfill_writtenWhenVersionUnchanged() {
        when(delegate.get(2L)).thenReturn(null);
        when(guard.currentVersion(CACHE_NAME)).thenReturn(0L);

        cache.get(2L);
        cache.put(2L, "fresh");

        verify(delegate).put(2L, "fresh");
    }

    // ========== 用例 9：快照不可校验（哨兵）→ put 丢弃 fail-safe ==========

    /**
     * RED：get miss 时读版本失败 → 存入不可校验哨兵 → put 必须丢弃（fail-safe 不缓存，
     * 回源可自愈）；骨架直写 → 失败。
     */
    @Test
    public void snapshotUnverifiable_putDiscarded() {
        when(delegate.get(3L)).thenReturn(null);
        when(guard.currentVersion(CACHE_NAME)).thenThrow(new RuntimeException("redis down"));

        cache.get(3L);
        cache.put(3L, "value");

        verify(delegate, never()).put(eq(3L), any());
    }

    // ========== 用例 10：无快照 → 直接写（@CachePut 边界） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：无 get 直接 put（@CachePut/sync 等非常规路径，
     * 当前仓库零使用）→ 无快照校验、直接写（登记边界 §5-2 合同）。
     */
    @Test
    public void putWithoutSnapshot_writesDirectly() {
        cache.put(4L, "value");

        verify(delegate).put(4L, "value");
    }

    // ========== 用例 11：get 命中 → 快照清除 ==========

    /**
     * RED：get 命中（非 null）时清除该键快照 → 后续 put 不校验直接写；版本读取仅发生在
     * miss 建快照那一次（guard 调用恰 1 次）；骨架 0 次 → 失败。
     */
    @Test
    public void getHit_clearsSnapshot() {
        when(delegate.get(5L)).thenReturn(null, new SimpleValueWrapper("cached"));
        when(guard.currentVersion(CACHE_NAME)).thenReturn(0L);

        cache.get(5L); // miss → 快照
        cache.get(5L); // hit → 清快照
        cache.put(5L, "value");

        verify(delegate).put(5L, "value");
        verify(guard, times(1)).currentVersion(CACHE_NAME);
    }

    // ========== 用例 12：evict 先 bump 后 delegate 操作（顺序语义） ==========

    /**
     * RED：受管缓存 evict 必须<b>先</b> bump 版本（宣告代际推进）<b>后</b> delegate.evict
     * （在途写回即刻被拦截）；骨架无 bump → InOrder 校验失败。
     */
    @Test
    public void guardedEvict_bumpsBeforeDelegateEvict() {
        InOrder inOrder = inOrder(guard, delegate);

        cache.evict(1L);

        inOrder.verify(guard).bumpVersion(CACHE_NAME);
        inOrder.verify(delegate).evict(1L);
    }

}
