package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.redis.core.CacheEvictionOperation;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * ZS-PERM-004.C：{@link OutboxCacheEvictionFailureRecorder} 失败键清单持久化测试（真实 H2 + 内嵌 Redis）。
 *
 * <p><b>RED 依据</b>：骨架阶段 {@code record} 为空实现——用例 1/2/3 断言 {@code outbox_event}
 * 表行数（1/1/2）失败；GREEN 阶段落地「进程内去重 + 自开事务 + port 懒解析 append」后转绿。
 *
 * <p><b>保护性用例</b>（骨架与 GREEN 均须通过）：用例 4/5 锁定降级契约——租户上下文缺失 /
 * port 未装配时必须静默降级（不写行、不抛异常、不伪造归属），循 {@code JdbcReliableEventPort} 合同。
 *
 * <p><b>装配</b>：@Import {@link JdbcReliableEventPort} 让 {@code ObjectProvider<ReliableEventPort>}
 * 在测试上下文能解析到真实实现（循 {@code OAuth2TokenServiceImplOutboxPreWriteTest} 先例）。
 * 各用例使用独立 key，规避进程内 5 秒去重窗口的跨用例污染。
 *
 * @author ZS-PERM-004.C
 */
@Import({OutboxCacheEvictionFailureRecorder.class, JdbcReliableEventPort.class})
public class OutboxCacheEvictionFailureRecorderTest extends BaseDbAndRedisUnitTest {

    private static final String EVENT_TYPE = SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION;

    @Resource
    private OutboxCacheEvictionFailureRecorder recorder;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== 查询工具 ==========

    private long countEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM outbox_event WHERE event_type = ?", Long.class, EVENT_TYPE);
        return count == null ? 0L : count;
    }

    private List<Map<String, Object>> queryEvents() {
        return jdbcTemplate.queryForList(
                "SELECT event_type, biz_type, biz_id, payload, tenant_id, actor_type, status "
                        + "FROM outbox_event WHERE event_type = ? ORDER BY id ASC",
                EVENT_TYPE);
    }

    // ========== 用例 1：记录恰 1 行（清单持久化主证据） ==========

    /**
     * RED：evict 失败记录 → outbox_event 恰 1 行（PENDING），event_type/biz_type/biz_id/tenant_id/
     * actor_type 与 payload 逐字段正确；骨架空实现 → 0 行 → 失败。
     */
    @Test
    public void record_writesExactlyOneOutboxRow() {
        recorder.record("role", "1", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));

        List<Map<String, Object>> events = queryEvents();
        assertEquals(1, events.size(), "驱逐失败必须持久化恰 1 条补偿事件");
        Map<String, Object> event = events.get(0);
        assertEquals(SystemOutboxEventTypes.BIZ_TYPE_CACHE_EVICTION, event.get("biz_type"));
        assertEquals("role:1", event.get("biz_id"), "biz_id 必须为 cacheName:key 供人工排查定位");
        assertEquals("SYSTEM", event.get("actor_type"), "驱逐失败记录主体必须为 SYSTEM");
        assertEquals("PENDING", event.get("status"));
        assertEquals(1L, ((Number) event.get("tenant_id")).longValue(),
                "事件租户必须来自 TenantContextHolder");

        CacheEvictionCompensationPayload payload = JsonUtils.parseObject(
                String.valueOf(event.get("payload")), CacheEvictionCompensationPayload.class);
        assertNotNull(payload, "payload 必须可反序列化");
        assertEquals("role", payload.getCacheName());
        assertEquals("1", payload.getKey());
        assertEquals("EVICT", payload.getOperation());
        assertNotNull(payload.getFailedAt(), "payload 必须携带失败时间（ISO-8601 序列化）");
    }

    // ========== 用例 2：同键同操作窗口内去重 ==========

    /**
     * RED：同 cacheName+key+operation 2 次记录（框架双层重试可能重复调用）→ 5 秒窗口内去重 → 恰 1 行。
     */
    @Test
    public void record_sameKeyWithinWindow_deduplicated() {
        recorder.record("role", "dup-key", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));
        recorder.record("role", "dup-key", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));

        assertEquals(1L, countEvents(), "同键同操作 5 秒窗口内重复记录必须去重为 1 行");
    }

    // ========== 用例 3：不同键不去重 ==========

    /**
     * RED：不同 key 的记录不得被去重（去重不误伤）→ 2 行。
     */
    @Test
    public void record_differentKeys_notDeduplicated() {
        recorder.record("role", "k1", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));
        recorder.record("role", "k2", CacheEvictionOperation.EVICT, new RuntimeException("redis down"));

        assertEquals(2L, countEvents(), "不同键的记录必须各自持久化（去重不得误伤）");
    }

    // ========== 用例 4：租户上下文缺失 → 降级不抛 ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：TenantContextHolder 缺失时记录必须静默降级——
     * 不伪造归属（不写行、不抛异常、不毒化任何事务），循 JdbcReliableEventPort 租户强制合同。
     */
    @Test
    public void record_noTenantContext_degradesNoThrow() {
        TenantContextHolder.clear();

        assertDoesNotThrow(() -> recorder.record("role", "4", CacheEvictionOperation.EVICT,
                new RuntimeException("redis down")));

        assertEquals(0L, countEvents(), "租户缺失时不得写入事件（不伪造归属）");
    }

    // ========== 用例 5：port 未装配（infra 缺位）→ 降级不抛 ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：{@code ObjectProvider} 解析不到 port 时（infra 未装配）
     * 必须静默降级为「仅日志」语义，不抛异常不阻断驱逐调用方。
     */
    @Test
    public void record_portMissing_degradesNoThrow() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ReliableEventPort> emptyProvider = mock(ObjectProvider.class);
        OutboxCacheEvictionFailureRecorder degrading =
                new OutboxCacheEvictionFailureRecorder(emptyProvider, transactionManager);

        assertDoesNotThrow(() -> degrading.record("role", "5", CacheEvictionOperation.EVICT,
                new RuntimeException("redis down")));

        assertEquals(0L, countEvents(), "port 缺失时记录必须降级（不写行、不抛）");
    }

}
