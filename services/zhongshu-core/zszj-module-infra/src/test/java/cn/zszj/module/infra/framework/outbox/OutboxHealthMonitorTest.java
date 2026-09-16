package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthMetrics;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthMonitor;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthMonitorImpl;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OutboxHealthMonitorImpl} 单元测试（ZS-JOB-004，H2）。
 *
 * <p>覆盖 docs/05 ZS-JOB-004 验收①「模拟连续失败进入 DEAD 并被发现」+ 积压/最长等待/失败率/租约健康监测，
 * 及验收④「阈值经实测回填而非凭空声称容量」——用例 16 断言阈值可配、告警随阈值变化，<b>不</b>断言任何具体生产容量数值。
 *
 * <p>监测器手动构造（{@code new OutboxHealthMonitorImpl(dataSource, props)}）以便逐用例注入自定义阈值；
 * DEAD 事件的「连续失败达上限」转移由 JOB-002 {@link OutboxDispatcherService} 真实驱动（用例 1），
 * 其余用例直接种子各状态事件聚焦监测聚合逻辑。
 */
public class OutboxHealthMonitorTest extends BaseDbUnitTest {

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(1L);
    }

    private OutboxHealthMonitor monitor(OutboxHealthProperties props) {
        return new OutboxHealthMonitorImpl(dataSource, props);
    }

    private OutboxHealthProperties props() {
        return new OutboxHealthProperties();
    }

    /** 种子一条指定状态事件，next_retry_at = now + offsetMillis（负值表示已到期/等待中）。 */
    private long insertEvent(String status, long nextRetryOffsetMillis, long tenantId) {
        jdbcTemplate.update("INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type, "
                        + "actor_id, trace_id, next_retry_at, status, retry_count) "
                        + "VALUES ('USER_MESSAGE_SEND', 'infra_file', '2048', '{}', ?, 'SYSTEM', 'ut-worker', "
                        + "'trace-ut', ?, ?, ?)",
                tenantId, new Timestamp(System.currentTimeMillis() + nextRetryOffsetMillis), status,
                "DEAD".equals(status) ? 5 : 0);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM outbox_event WHERE tenant_id = ? ORDER BY id DESC LIMIT 1", Long.class, tenantId);
        return id == null ? -1 : id;
    }

    private void insertLease(String instanceId, boolean expired) {
        long offset = expired ? -60_000 : 60_000;
        jdbcTemplate.update("INSERT INTO dispatcher_lease (dispatcher_name, instance_id, lease_expires_at, "
                        + "heartbeat_at) VALUES ('ut-dispatcher', ?, ?, ?)",
                instanceId, new Timestamp(System.currentTimeMillis() + offset),
                new Timestamp(System.currentTimeMillis()));
    }

    private void makeDue(long eventId) {
        jdbcTemplate.update("UPDATE outbox_event SET next_retry_at = ? WHERE id = ?",
                new Timestamp(System.currentTimeMillis() - 1000), eventId);
    }

    private String statusOf(long eventId) {
        return jdbcTemplate.queryForObject("SELECT status FROM outbox_event WHERE id = ?", String.class, eventId);
    }

    /** 用例 1（验收①）：连续失败进入 DEAD 被健康监测发现——派发器真实驱动 5 次失败转 DEAD，快照应发现并告警。 */
    @Test
    public void test_连续失败进入DEAD_健康监测发现() {
        OutboxDispatcherService dispatcher = new OutboxDispatcherService(dataSource, transactionManager, List.of());
        long eventId = insertEvent("PENDING", -1000, 1L);
        for (int round = 0; round < 5; round++) {
            makeDue(eventId);
            dispatcher.dispatchOnce("ut-dispatcher", "instance-a", 30, 10, 60);
        }
        assertEquals("DEAD", statusOf(eventId), "连续失败达上限应转 DEAD（JOB-002 派发器机制）");

        OutboxHealthMetrics metrics = monitor(props()).snapshot();
        assertTrue(metrics.getDeadCount() >= 1, "健康监测应发现 DEAD 事件（验收①「被发现」）");
        assertTrue(metrics.getBreaches().stream().anyMatch(b -> b.contains("DEAD")),
                "DEAD 存在应汇入越阈告警清单，breaches=" + metrics.getBreaches());
    }

    /** 用例 2（验收①）：积压计数与最长等待监测正确，越阈告警。 */
    @Test
    public void test_积压与最长等待监测正确() {
        insertEvent("PENDING", -120_000, 1L); // 最老：已等待约 120s
        insertEvent("PENDING", -1000, 1L);
        insertEvent("PENDING", 60_000, 1L);   // 退避中（未来）

        OutboxHealthProperties low = props();
        low.setBacklogWarn(1);
        low.setLongestWaitWarnSeconds(30);
        OutboxHealthMetrics metrics = monitor(low).snapshot();

        assertEquals(3, metrics.getPendingBacklog(), "PENDING 积压应计数");
        assertTrue(metrics.getLongestWaitSeconds() >= 100,
                "最长等待应≈最老 PENDING 距今秒数，实际=" + metrics.getLongestWaitSeconds());
        assertTrue(metrics.getBreaches().stream().anyMatch(b -> b.contains("PENDING_BACKLOG")), "积压越阈应告警");
        assertTrue(metrics.getBreaches().stream().anyMatch(b -> b.contains("LONGEST_WAIT")), "最长等待越阈应告警");
    }

    /** 用例 3（验收①）：失败率 = DEAD /（DISPATCHED+DEAD+SKIPPED）× 100 计算正确。 */
    @Test
    public void test_失败率监测正确() {
        insertEvent("DISPATCHED", -1000, 1L);
        insertEvent("DISPATCHED", -1000, 1L);
        insertEvent("DEAD", -1000, 1L);

        OutboxHealthMetrics metrics = monitor(props()).snapshot();
        // 分母 = 2 DISPATCHED + 1 DEAD = 3；DEAD = 1 → 33.33%
        assertEquals(33.33, metrics.getFailureRatePercent(), 0.1, "失败率=DEAD/(DISPATCHED+DEAD+SKIPPED)×100");
    }

    /** 用例 4（验收①）：过期租约被计入 staleLeaseCount 并告警（dispatcher 停摆征兆）。 */
    @Test
    public void test_租约健康监测_过期租约被发现() {
        insertLease("instance-expired", true);
        insertLease("instance-live", false);

        OutboxHealthMetrics metrics = monitor(props()).snapshot();
        assertTrue(metrics.getStaleLeaseCount() >= 1, "过期租约应被计入 staleLeaseCount");
        assertTrue(metrics.getBreaches().stream().anyMatch(b -> b.contains("STALE_LEASE")), "过期租约应告警");
    }

    /**
     * 用例 16（验收④）：阈值可配 → 告警随阈值变化。断言监测<b>逻辑</b>正确（同一积压在低阈值告警、高阈值不告警），
     * <b>不</b>断言任何具体生产容量/TPS 数值——阈值待 WP-19 实测回填，禁止凭空声称容量达标。
     */
    @Test
    public void test_阈值可配_越阈告警_不编造容量() {
        insertEvent("PENDING", -1000, 1L);

        OutboxHealthProperties high = props();
        high.setBacklogWarn(1000);
        assertTrue(monitor(high).snapshot().getBreaches().stream().noneMatch(b -> b.contains("PENDING_BACKLOG")),
                "高阈值下 1 条积压不应告警");

        OutboxHealthProperties low = props();
        low.setBacklogWarn(0);
        assertTrue(monitor(low).snapshot().getBreaches().stream().anyMatch(b -> b.contains("PENDING_BACKLOG")),
                "低阈值下同样积压应告警——阈值可配、告警随阈值变化（非凭空声称容量）");
    }

}
