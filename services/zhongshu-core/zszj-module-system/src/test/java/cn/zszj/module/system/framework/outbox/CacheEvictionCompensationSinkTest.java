package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.redis.config.ZszjCacheAutoConfiguration;
import cn.zszj.framework.redis.core.RetryEvictCache;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-PERM-004.C：{@link CacheEvictionCompensationSink} 补偿重放测试（真实 H2 + 内嵌 Redis + 真实 CacheManager）。
 *
 * <p><b>RED 依据</b>：骨架阶段 {@code deliver} 为空实现——用例 19/20（重放驱逐/清空）断言 Redis 键被清
 * 会失败、用例 22/23（失败/损坏必须抛出触发 dispatcher 退避）断言抛异常会失败；GREEN 阶段落地
 * 「解析 payload → replay scope → evict/clear → 失败抛出」后转绿。
 *
 * <p><b>保护性用例</b>：用例 24（未知缓存静默）骨架与 GREEN 均须通过（物理清理容错语义）；
 * 用例 21（幂等）随驱逐语义落地验证——骨架空实现下键未清 → RED，GREEN 转绿。
 *
 * <p><b>装配</b>：@Import {@link ZszjCacheAutoConfiguration} 提供真实 cacheManager（本类不配置白名单，
 * Sink 的 evict/clear 语义与是否受管无关）。
 *
 * @author ZS-PERM-004.C
 */
@Import({ZszjCacheAutoConfiguration.class, CacheEvictionCompensationSink.class})
public class CacheEvictionCompensationSinkTest extends BaseDbAndRedisUnitTest {

    @Resource
    private CacheEvictionCompensationSink sink;
    @Resource
    private CacheManager cacheManager;

    private Cache roleCache;

    @BeforeEach
    public void beforeEach() {
        roleCache = cacheManager.getCache("role");
        roleCache.clear(); // 干净起点（内嵌 Redis 跨用例共享）
    }

    // ========== 构造工具 ==========

    private OutboxEventRecord buildEvent(CacheEvictionCompensationPayload payload) {
        return new OutboxEventRecord(
                1L,
                SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION,
                SystemOutboxEventTypes.BIZ_TYPE_CACHE_EVICTION,
                payload.getCacheName() + ":" + (payload.getKey() == null ? "__clear__" : payload.getKey()),
                null,
                JsonUtils.toJsonString(payload),
                null,
                1L,
                0,
                "SYSTEM",
                null,
                null,
                "ut-instance@ut-dispatcher",
                "ut-claim-token");
    }

    private CacheEvictionCompensationPayload evictPayload(String cacheName, String key) {
        return CacheEvictionCompensationPayload.builder()
                .cacheName(cacheName).key(key).operation("EVICT").failedAt(DateUtils.now()).build();
    }

    private CacheEvictionCompensationPayload clearPayload(String cacheName) {
        return CacheEvictionCompensationPayload.builder()
                .cacheName(cacheName).key(null).operation("CLEAR").failedAt(DateUtils.now()).build();
    }

    // ========== 用例 19：重放驱逐单键 ==========

    /**
     * RED：预置 role:1 → deliver(EVICT key=1) → 键必须被清除（重放驱逐生效）；
     * 骨架空实现 → 键仍在 → 失败。
     */
    @Test
    public void deliver_evict_removesRedisKey() throws Exception {
        roleCache.put(1L, "stale-value");
        assertNotNull(roleCache.get(1L), "前置：role:1 已缓存");

        sink.deliver(buildEvent(evictPayload("role", "1")));

        assertNull(roleCache.get(1L), "重放驱逐后 role:1 必须被清除");
    }

    // ========== 用例 20：重放清空整个缓存 ==========

    /**
     * RED：预置多键 → deliver(CLEAR key=null) → 全部清空；骨架空实现 → 键仍在 → 失败。
     */
    @Test
    public void deliver_clear_clearsCache() throws Exception {
        roleCache.put(1L, "v1");
        roleCache.put(2L, "v2");

        sink.deliver(buildEvent(clearPayload("role")));

        assertNull(roleCache.get(1L), "重放清空后 role:1 必须被清除");
        assertNull(roleCache.get(2L), "重放清空后 role:2 必须被清除");
    }

    // ========== 用例 21：幂等（at-least-once 语义） ==========

    /**
     * RED（幂等语义随驱逐落地）：同一事件连续 deliver 3 次不抛异常且结果一致——骨架空实现
     * 首轮即无驱逐效果（键残留）→ 失败；GREEN 落地后锁定「重复重放=再驱动一次，多清无害」。
     */
    @Test
    public void deliver_idempotent_repeatInvocationsNoSideEffect() throws Exception {
        roleCache.put(1L, "v1");
        OutboxEventRecord event = buildEvent(evictPayload("role", "1"));

        sink.deliver(event);
        assertDoesNotThrow(() -> sink.deliver(event), "重复 deliver 必须幂等不抛");
        sink.deliver(event);

        assertNull(roleCache.get(1L), "多次重放后键必须被清除");
    }

    // ========== 用例 22：重放再次失败必须抛出（触发 dispatcher 退避/DEAD） ==========

    /**
     * 契约（CodeReview R1 P0 修正）：重放失败必须穿过<b>真实装饰链</b>上抛——
     * Sink → TransactionAwareCacheDecorator → RetryEvictCache → 恒失败底层；此前 mock 直抛
     * 绕过 RetryEvictCache 的吞异常路径，掩盖了「dispatcher 误标 DISPATCHED、补偿静默丢失」的缺口。
     */
    @Test
    public void deliver_evictFailsAgain_throwsForDispatcherRetry() {
        Cache failingRedis = mock(Cache.class);
        when(failingRedis.getName()).thenReturn("role");
        doThrow(new RuntimeException("redis down")).when(failingRedis).evict("1");
        Cache chained = new TransactionAwareCacheDecorator(new RetryEvictCache(failingRedis));
        CacheManager failingManager = mock(CacheManager.class);
        when(failingManager.getCache("role")).thenReturn(chained);
        CacheEvictionCompensationSink failingSink = new CacheEvictionCompensationSink(failingManager);

        assertThrows(RuntimeException.class,
                () -> failingSink.deliver(buildEvent(evictPayload("role", "1"))),
                "重放再次失败必须抛出以触发 dispatcher 退避重试（超限进 DEAD 台账）");
    }

    // ========== 用例 23：损坏 payload 必须抛出（进 DEAD 人工可见，不静默丢失） ==========

    /**
     * RED：payload 损坏（不可反序列化）时 Sink 必须抛出——静默跳过会让补偿事件被误判完成，
     * 失败键清单丢失；骨架空实现 → 不抛 → 失败。
     */
    @Test
    public void deliver_malformedPayload_throws() {
        OutboxEventRecord malformed = new OutboxEventRecord(
                1L,
                SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION,
                SystemOutboxEventTypes.BIZ_TYPE_CACHE_EVICTION,
                "role:1",
                null,
                "{not-json",
                null,
                1L,
                0,
                "SYSTEM",
                null,
                null,
                "ut-instance@ut-dispatcher",
                "ut-claim-token");

        assertThrows(RuntimeException.class, () -> sink.deliver(malformed),
                "损坏 payload 必须抛出进 DEAD 台账（防静默丢失补偿证据）");
    }

    // ========== 用例 24：未知缓存静默返回（物理清理容错） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：cacheName 对应缓存物理不存在（cacheManager 返回 null）时
     * 视为无补偿目标，静默返回不抛异常（否则 dispatcher 永久重试死循环）。
     */
    @Test
    public void deliver_unknownCache_silentReturn() {
        CacheManager nullManager = mock(CacheManager.class); // getCache 默认返回 null
        CacheEvictionCompensationSink nullSink = new CacheEvictionCompensationSink(nullManager);

        assertDoesNotThrow(() -> nullSink.deliver(buildEvent(evictPayload("nonexistent", "1"))),
                "缓存物理不存在时必须静默返回（无补偿目标视为完成）");
    }

}
