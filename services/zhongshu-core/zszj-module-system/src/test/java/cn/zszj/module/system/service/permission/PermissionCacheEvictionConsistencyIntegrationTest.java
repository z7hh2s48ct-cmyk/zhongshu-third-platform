package cn.zszj.module.system.service.permission;

import cn.zszj.framework.redis.config.ZszjCacheAutoConfiguration;
import cn.zszj.framework.redis.core.CacheEvictionOperation;
import cn.zszj.framework.redis.core.RetryEvictCache;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxDispatcherService;
import cn.zszj.module.system.framework.outbox.CacheEvictionCompensationSink;
import cn.zszj.module.system.framework.outbox.OutboxCacheEvictionFailureRecorder;
import cn.zszj.module.system.framework.outbox.SystemOutboxEventTypes;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-PERM-004.C：权限缓存驱逐失败补偿 + 防旧值写回端到端一致性测试
 * （真实 H2 + 内嵌 Redis + 真实 CacheManager/dispatcher/Sink/Recorder 装配）。
 *
 * <p><b>RED 依据</b>：骨架阶段全链未接线——用例 25（outbox 0 行）/26（晚到回填写入）失败；
 * GREEN 落地「记录点 → outbox 持久化 → dispatcher 重放」与「版本校验防旧值写回」后转绿。
 *
 * <p><b>白名单</b>：类级 {@code zszj.cache.consistency-guarded-cache-names=role}——受管缓存为
 * {@code role}，{@code demo} 为非受管对照组（白名单边界证据）。
 *
 * <p><b>半合成链路说明</b>（用例 25）：框架层记录点用「恒失败 delegate 的合成
 * {@link RetryEvictCache}」触发真实驱逐失败（真实 cacheManager 的装饰链无法注入恒失败底层），
 * 记录/持久化/派发/重放全链均用真实装配——对齐开发计划 §3 表 25 的设计。
 *
 * @author ZS-PERM-004.C
 */
@Import({ZszjCacheAutoConfiguration.class, OutboxCacheEvictionFailureRecorder.class,
        JdbcReliableEventPort.class, OutboxDispatcherService.class, CacheEvictionCompensationSink.class})
@TestPropertySource(properties = {
        "zszj.cache.consistency-guarded-cache-names=role",
        "spring.main.allow-circular-references=true"})
public class PermissionCacheEvictionConsistencyIntegrationTest extends BaseDbAndRedisUnitTest {

    private static final String EVENT_TYPE = SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION;

    @Resource
    private CacheManager cacheManager;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OutboxCacheEvictionFailureRecorder recorder;
    @Resource
    private OutboxDispatcherService outboxDispatcherService;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        jdbcTemplate = new JdbcTemplate(dataSource);
        // 干净起点：内嵌 Redis 跨用例共享，显式清业务键与版本键（版本基线归零）
        stringRedisTemplate.delete(List.of(
                "role:1", "role:2", "demo:1", "__cachever__:role", "__cachever__:demo"));
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== 查询工具 ==========

    private long countEvents(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM outbox_event WHERE event_type = ? AND status = ?",
                Long.class, EVENT_TYPE, status);
        return count == null ? 0L : count;
    }

    private String singleEventStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_event WHERE event_type = ? ORDER BY id LIMIT 1",
                String.class, EVENT_TYPE);
    }

    // ========== 用例 25：端到端——记录 → 持久化 → 派发 → 重放驱逐（退出条件主证据） ==========

    /**
     * RED：合成恒失败驱逐 → 真实 recorder 持久化 1 条 PENDING 事件 → dispatchOnce 领取并投递 →
     * 真实 cacheManager 的 role:1 被清 + 事件标记 DISPATCHED + 二次派发无重复。
     */
    @Test
    public void endToEnd_record_thenDispatch_replayEvicts() {
        Cache roleCache = cacheManager.getCache("role");
        roleCache.put(1L, "stale-value");
        assertNotNull(roleCache.get(1L), "前置：role:1 已缓存");

        // 合成恒失败 delegate：框架层记录点 → 真实 recorder → outbox 持久化（重试 2 次后记录）
        Cache failing = mock(Cache.class);
        when(failing.getName()).thenReturn("role");
        doThrow(new RuntimeException("redis down")).when(failing).evict(1L);
        new RetryEvictCache(failing, recorder, null).evict(1L);

        assertEquals(1L, countEvents("PENDING"), "驱逐耗尽后必须持久化恰 1 条 PENDING 补偿事件");

        int dispatched = outboxDispatcherService.dispatchOnce("ut-dispatcher", "ut-instance", 60L, 10, 60L);
        assertEquals(1, dispatched, "dispatcher 必须领取并投递 1 条事件");

        assertNull(roleCache.get(1L), "重放驱逐后 role:1 必须被清除（登记 M1 收敛主证据）");
        assertEquals("DISPATCHED", singleEventStatus(), "投递成功后事件必须标记 DISPATCHED");
        assertEquals(0, outboxDispatcherService.dispatchOnce("ut-dispatcher", "ut-instance", 60L, 10, 60L),
                "二次派发不得重复投递（DISPATCHED 事件不再被领取）");
    }

    // ========== 用例 26：晚到回填端到端——驱逐 bump 后写回被丢弃 ==========

    /**
     * RED：真实受管缓存「get miss → evict（bump）→ put 旧值」= 晚到回填场景 → 写回必须被丢弃
     * （raw Redis 无 role:1）+ 版本键推进为 1；骨架直写 → 失败。
     */
    @Test
    public void lateBackfill_discardedEndToEnd() {
        Cache roleCache = cacheManager.getCache("role");
        assertNull(roleCache.get(1L), "前置：role:1 缓存 miss（建立版本快照）");

        roleCache.evict(1L);
        roleCache.put(1L, "late-stale");

        assertFalse(stringRedisTemplate.hasKey("role:1"), "驱逐 bump 后晚到回填必须被丢弃（防旧值写回）");
        assertEquals("1", stringRedisTemplate.opsForValue().get("__cachever__:role"),
                "受管缓存驱逐必须推进版本键");
    }

    // ========== 用例 27：正常回填不误伤（保护性） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：get miss → put（版本未变）→ 正常写入（raw Redis 有 role:2）。
     */
    @Test
    public void normalBackfill_writtenEndToEnd() {
        Cache roleCache = cacheManager.getCache("role");
        assertNull(roleCache.get(2L), "前置：role:2 缓存 miss");

        roleCache.put(2L, "fresh");

        assertTrue(stringRedisTemplate.hasKey("role:2"), "版本未变时正常回源回填必须写入（不误伤）");
    }

    // ========== 用例 28：非白名单缓存零变化（白名单边界） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：{@code demo} 不在白名单 → get/put/evict 行为如常、
     * 且不产生任何版本键（框架层零变化承诺）。
     */
    @Test
    public void unguardedCache_noVersionKey() {
        Cache demoCache = cacheManager.getCache("demo");
        assertNull(demoCache.get(1L), "前置：demo:1 miss");

        demoCache.put(1L, "v");
        assertTrue(stringRedisTemplate.hasKey("demo:1"), "非白名单 put 直写（行为如常）");

        demoCache.evict(1L);
        assertFalse(stringRedisTemplate.hasKey("__cachever__:demo"),
                "非白名单缓存不得产生版本键（框架层零变化）");
    }

    // ========== 用例 29：clear 的 SCAN 模式不误删版本键（前缀隔离） ==========

    /**
     * RED：预置业务键 + 版本键 → clear() → 业务键被清、版本键仍在（版本键独立前缀
     * {@code __cachever__:} 不被 {@code role:*} SCAN 模式匹配）；骨架不 bump 但也不误删 →
     * 本用例是结构契约为主，GREEN 后同时验证 bump 语义下版本键仍存活。
     */
    @Test
    public void clear_doesNotDeleteVersionKey() {
        Cache roleCache = cacheManager.getCache("role");
        roleCache.put(1L, "v1");
        roleCache.put(2L, "v2");
        stringRedisTemplate.opsForValue().set("__cachever__:role", "1");

        roleCache.clear();

        assertTrue(stringRedisTemplate.hasKey("__cachever__:role"),
                "clear 的 SCAN 模式（role:*）不得误删独立前缀的版本键（__cachever__:role）");
        assertFalse(stringRedisTemplate.hasKey("role:1"), "清空后业务键 role:1 必须被清除");
        assertFalse(stringRedisTemplate.hasKey("role:2"), "清空后业务键 role:2 必须被清除");
    }

    // ========== 用例 30：afterCommit 驱逐失败 → 补偿事件独立事务落库（CodeReview R1 P1） ==========

    /**
     * CodeReview R1 P1：生产主路径 = 事务内驱逐被 TransactionAwareCacheDecorator 延迟到 afterCommit，
     * 此时 ConnectionHolder 仍绑定且 isTransactionActive()==true——record 若走 REQUIRED 会静默并入
     * 已提交事务（无显式 commit，落库只剩连接归还副作用兜底）；改为 REQUIRES_NEW 后必须恰 1 条落库。
     */
    @Test
    public void afterCommitEvictionFailure_recordsExactlyOnePENDING() {
        Cache failingRedis = mock(Cache.class);
        when(failingRedis.getName()).thenReturn("role");
        doThrow(new RuntimeException("redis down")).when(failingRedis).evict("tx-1");
        // 合成真实装饰链：事务感知（延迟到 afterCommit）→ RetryEvictCache（有界重试 + 记录点）→ 恒失败底层
        Cache txAwareGuarded = new TransactionAwareCacheDecorator(new RetryEvictCache(failingRedis, recorder, null));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> txAwareGuarded.evict("tx-1"));

        assertEquals(1L, countEvents("PENDING"),
                "afterCommit 驱逐失败必须经 REQUIRES_NEW 独立事务恰落库 1 条补偿事件");
    }

    // ========== 用例 31：真实链重放失败 → retry_count+1 → 5 次进 DEAD（CodeReview R1 P0） ==========

    /**
     * CodeReview R1 P0：重放经真实装饰链（TransactionAware → RetryEvictCache → 恒失败底层）失败必须
     * 上抛给 dispatcher——每次投递失败 retry_count+1，连续 5 次转 DEAD 人工台账；此前 RetryEvictCache
     * 耗尽后吞异常会让事件被误标 DISPATCHED、补偿静默丢失（本用例锁定不静默）。
     */
    @Test
    public void replayFailure_propagatesToDispatcher_retriesThenDead() {
        // 前置：经真实 recorder 持久化 1 条 PENDING 补偿事件
        recorder.record("role", "dead-1", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));
        assertEquals(1L, countEvents("PENDING"), "前置：1 条 PENDING 补偿事件");

        // 合成真实装饰链：Sink → TransactionAware → RetryEvictCache(恒失败) → 恒失败底层
        Cache failingRedis = mock(Cache.class);
        when(failingRedis.getName()).thenReturn("role");
        doThrow(new RuntimeException("redis down")).when(failingRedis).evict("dead-1");
        Cache chained = new TransactionAwareCacheDecorator(new RetryEvictCache(failingRedis));
        CacheManager failingManager = mock(CacheManager.class);
        when(failingManager.getCache("role")).thenReturn(chained);
        CacheEvictionCompensationSink failingSink = new CacheEvictionCompensationSink(failingManager);

        // 手工装配 dispatcher（真实领取/失败 SQL + 失败链 Sink；退避 0 秒立即可重领）
        OutboxDispatcherService failingDispatcher =
                new OutboxDispatcherService(dataSource, transactionManager, List.of(failingSink));

        for (int i = 1; i <= 5; i++) {
            assertEquals(1, failingDispatcher.dispatchOnce("ut-failing", "ut-instance", 60L, 10, 0L),
                    "第 " + i + " 轮必须领取到事件（失败退避 0 秒立即可重领）");
        }

        assertEquals(0L, countEvents("PENDING"), "连续失败后不得残留 PENDING");
        assertEquals(1L, countEvents("DEAD"), "重放持续失败必须转 DEAD 人工台账（补偿不静默丢失）");
        Integer retryCount = jdbcTemplate.queryForObject(
                "SELECT retry_count FROM outbox_event WHERE event_type = ?", Integer.class, EVENT_TYPE);
        assertEquals(5, retryCount, "5 次失败后 retry_count 必须为 5");
    }

}
