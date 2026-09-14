package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OutboxDispatcherService} 单元测试（ZS-JOB-002，H2）。
 *
 * <p>覆盖 docs/05 ZS-JOB-002 验收的单实例可证部分：领取/租约回收、每次领取唯一凭证栅栏（过期旧执行者不能
 * 确认新领取）、退避与 DEAD、无 Sink 进入可见失败而非丢弃、派发与业务事务分离。双实例 SKIP LOCKED 并发、
 * 真实 PG 行锁语义由 {@code scripts/db/run-job002-verify.mjs}（真实 PG17 容器）承载，H2 不作数据库放行门禁。
 */
@Import({OutboxDispatcherService.class, OutboxDispatcherServiceTest.RecordingSink.class})
public class OutboxDispatcherServiceTest extends BaseDbUnitTest {

    private static final String DISPATCHER = "ut-dispatcher";

    private static final String INSTANCE_A = "instance-a";

    private static final String INSTANCE_B = "instance-b";

    @Resource
    private OutboxDispatcherService dispatcher;

    @Resource
    private RecordingSink sink;

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        sink.reset(Set.of("USER_MESSAGE_SEND"));
        TenantContextHolder.setTenantId(1L);
    }

    /** 测试投递 Sink：可配置支持的事件类型与失败模式，记录投递内容供断言。 */
    @org.springframework.stereotype.Component
    public static class RecordingSink implements OutboxEventSink {

        private final List<OutboxEventRecord> delivered = new ArrayList<>();

        private volatile Set<String> supportedTypes = Set.of();

        private volatile boolean failOnDeliver = false;

        @Override
        public boolean supports(String eventType) {
            return supportedTypes.contains(eventType);
        }

        @Override
        public void deliver(OutboxEventRecord event) throws Exception {
            if (failOnDeliver) {
                throw new IllegalStateException("sink 模拟投递失败");
            }
            delivered.add(event);
        }

        public void reset(Set<String> supportedTypes) {
            this.supportedTypes = supportedTypes;
            this.failOnDeliver = false;
            this.delivered.clear();
        }

    }

    /** 插入一条立即可领取的 PENDING 事件，返回事件 ID。next_retry_at 显式置过去：DB 默认值是微秒精度，
     *  与 Java 侧毫秒参数比较在同毫秒内会出现「刚插入即不可领取」的粒度假阴性。 */
    private long insertPendingEvent(String eventType) {
        jdbcTemplate.update("INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, tenant_id, actor_type, "
                        + "next_retry_at) VALUES (?, 'infra_file', '2048', '{}', 1, 'SYSTEM', ?)",
                eventType, new Timestamp(System.currentTimeMillis() - 1000));
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM outbox_event WHERE event_type = ? ORDER BY id DESC LIMIT 1", Long.class, eventType);
        assertNotNull(id);
        return id;
    }

    /** 把事件改为立即可领取（模拟退避时间已流逝）。 */
    private void makeDue(long eventId) {
        jdbcTemplate.update("UPDATE outbox_event SET next_retry_at = ? WHERE id = ?",
                new Timestamp(System.currentTimeMillis() - 1000), eventId);
    }

    /** 把事件租约改为已过期（模拟进程崩溃/租约到期）。 */
    private void expireLease(long eventId) {
        jdbcTemplate.update("UPDATE outbox_event SET claim_expires_at = ? WHERE id = ?",
                new Timestamp(System.currentTimeMillis() - 1000), eventId);
    }

    private Map<String, Object> loadRow(long eventId) {
        return jdbcTemplate.queryForMap("SELECT status, retry_count, claimed_by, claim_token, claim_expires_at, "
                + "last_error, dispatched_at, next_retry_at FROM outbox_event WHERE id = ?", eventId);
    }

    /** 用例 1：领取回填领取者/唯一凭证/租约，且两次领取凭证互异（每次领取唯一凭证）。 */
    @Test
    public void testClaim_backfillsLeaseAndIssuesUniqueToken() {
        long first = insertPendingEvent("USER_MESSAGE_SEND");
        long second = insertPendingEvent("USER_MESSAGE_SEND");

        List<OutboxEventRecord> batch = dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10);

        assertEquals(2, batch.size(), "两条 PENDING 事件都应被领取");
        assertNotEquals(batch.get(0).getClaimToken(), batch.get(1).getClaimToken(), "每次领取应签发唯一凭证");
        for (OutboxEventRecord record : batch) {
            assertEquals(INSTANCE_A + "@" + DISPATCHER, record.getClaimedBy());
            assertNotNull(record.getClaimToken());
            Map<String, Object> row = loadRow(record.getEventId());
            assertEquals(INSTANCE_A + "@" + DISPATCHER, row.get("claimed_by"));
            assertEquals(record.getClaimToken(), row.get("claim_token"));
            assertNotNull(row.get("claim_expires_at"), "领取应写入租约到期时间");
        }
        assertTrue(first > 0 && second > 0);
    }

    /** 用例 2：租约未过期不重领（防多实例重复投递）；租约过期后可被重领（进程崩溃可恢复）。 */
    @Test
    public void testClaim_leaseBlocksUntilExpired() {
        insertPendingEvent("USER_MESSAGE_SEND");

        assertEquals(1, dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).size(), "首次应领取");
        assertTrue(dispatcher.claim(DISPATCHER, INSTANCE_B, 30, 10).isEmpty(), "租约未过期不得重领");

        long eventId = jdbcTemplate.queryForObject("SELECT id FROM outbox_event", Long.class);
        expireLease(eventId);
        List<OutboxEventRecord> reclaimed = dispatcher.claim(DISPATCHER, INSTANCE_B, 30, 10);
        assertEquals(1, reclaimed.size(), "租约过期后应可重领（崩溃恢复）");
        assertEquals(INSTANCE_B + "@" + DISPATCHER, reclaimed.get(0).getClaimedBy(), "重领应更新领取者");
    }

    /** 用例 3（栅栏合同）：租约过期重领后，旧执行者的旧凭证不能 complete/fail 新领取——旧领取不能覆盖新状态。 */
    @Test
    public void testComplete_staleTokenCannotConfirmNewClaim() {
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");

        String staleToken = dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).get(0).getClaimToken();
        expireLease(eventId);
        String freshToken = dispatcher.claim(DISPATCHER, INSTANCE_B, 30, 10).get(0).getClaimToken();
        assertNotEquals(staleToken, freshToken, "重领应签发新凭证");

        assertFalse(dispatcher.complete(eventId, staleToken), "旧凭证不得确认新领取");
        assertFalse(dispatcher.fail(eventId, staleToken, "stale", 60), "旧凭证不得推进新领取的失败状态");
        assertEquals("PENDING", loadRow(eventId).get("status"), "旧凭证操作后状态不得变化");

        assertTrue(dispatcher.complete(eventId, freshToken), "当前凭证应可确认");
        assertEquals("DISPATCHED", loadRow(eventId).get("status"));
        assertNotNull(loadRow(eventId).get("dispatched_at"));
    }

    /** 用例 4：失败退避推进 retry_count/next_retry_at 并释放租约；达上限转 DEAD，DEAD 不再被领取。 */
    @Test
    public void testFail_backoffThenDeadAndNeverClaimedAgain() {
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");

        for (int round = 1; round <= 4; round++) {
            makeDue(eventId);
            String token = dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).get(0).getClaimToken();
            assertTrue(dispatcher.fail(eventId, token, "模拟失败-" + round, 60));
            Map<String, Object> row = loadRow(eventId);
            assertEquals(round, ((Number) row.get("retry_count")).intValue(), "第 " + round + " 次失败应推进计数");
            assertEquals("PENDING", row.get("status"), "未达上限不得转 DEAD");
            assertNull(row.get("claimed_by"), "失败后应释放租约");
            assertNull(row.get("claim_token"));
            assertTrue(((Timestamp) row.get("next_retry_at")).after(new Timestamp(System.currentTimeMillis())),
                    "失败应设置未来退避点");
        }

        makeDue(eventId);
        String token = dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).get(0).getClaimToken();
        assertTrue(dispatcher.fail(eventId, token, "模拟失败-5", 60));
        assertEquals("DEAD", loadRow(eventId).get("status"), "第 5 次失败应转 DEAD");

        makeDue(eventId);
        assertTrue(dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).isEmpty(), "DEAD 不得再被领取");
    }

    /** 用例 5：无 Sink 声明支持的事件按失败退避进入可见失败（不丢弃、不静默），最终可到 DEAD。 */
    @Test
    public void testDispatchOnce_noSinkGoesVisibleFailureNotDiscard() {
        sink.reset(Set.of("OTHER_EVENT_TYPE")); // 无 Sink 支持该类型
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");

        int claimed = dispatcher.dispatchOnce(DISPATCHER, INSTANCE_A, 30, 10, 60);
        assertEquals(1, claimed);
        Map<String, Object> row = loadRow(eventId);
        assertEquals("PENDING", row.get("status"), "无 Sink 不等于丢弃，应可重试");
        assertEquals(1, ((Number) row.get("retry_count")).intValue());
        assertTrue(String.valueOf(row.get("last_error")).contains("NO_SINK_SUPPORTS_EVENT_TYPE"));

        for (int round = 0; round < 4; round++) {
            makeDue(eventId);
            dispatcher.dispatchOnce(DISPATCHER, INSTANCE_A, 30, 10, 60);
        }
        assertEquals("DEAD", loadRow(eventId).get("status"), "连续无 Sink 达上限应转 DEAD 进入可见失败");
    }

    /** 用例 6：有 Sink 时投递并确认；Sink 失败按退避重试；Record 携带 tenant/header 上下文。 */
    @Test
    public void testDispatchOnce_deliversWithContextAndRecoversFromSinkFailure() {
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");
        jdbcTemplate.update("UPDATE outbox_event SET payload = ?, headers = ? WHERE id = ?",
                "{\"fileId\":2048}", "{\"hint\":\"ut\"}", eventId);

        // 第一轮：Sink 失败 → 退避，事件不丢
        sink.reset(Set.of("USER_MESSAGE_SEND"));
        sink.failOnDeliver = true;
        assertEquals(1, dispatcher.dispatchOnce(DISPATCHER, INSTANCE_A, 30, 10, 60));
        assertEquals("PENDING", loadRow(eventId).get("status"));
        assertEquals(1, ((Number) loadRow(eventId).get("retry_count")).intValue());
        assertTrue(String.valueOf(loadRow(eventId).get("last_error")).contains("sink 模拟投递失败"));

        // 第二轮：租约过期后 Sink 成功 → 确认 DISPATCHED（投递成功但确认丢失的场景由 PG 套件 + Sink 幂等合同承接）
        expireLease(eventId);
        makeDue(eventId);
        sink.failOnDeliver = false;
        assertEquals(1, dispatcher.dispatchOnce(DISPATCHER, INSTANCE_A, 30, 10, 60));
        assertEquals(1, sink.delivered.size());
        OutboxEventRecord record = sink.delivered.get(0);
        assertEquals(eventId, record.getEventId());
        assertEquals(1L, record.getTenantId().longValue(), "Record 应带回技术租户上下文");
        assertEquals("{\"fileId\":2048}", record.getPayload());
        assertEquals("{\"hint\":\"ut\"}", record.getHeaders(), "Record 应带回投递附带头");
        assertEquals("DISPATCHED", loadRow(eventId).get("status"));
    }

    /** 用例 7（派发/业务事务分离）：dispatchOnce 在事务上下文中调用必须 fail-fast 拒绝。 */
    @Test
    public void testDispatchOnce_rejectsTransactionContext() {
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");
        transactionTemplate.executeWithoutResult(status -> assertThrows(IllegalStateException.class,
                () -> dispatcher.dispatchOnce(DISPATCHER, INSTANCE_A, 30, 10, 60)));
        assertEquals(0, ((Number) loadRow(eventId).get("retry_count")).intValue(), "被拒绝的一轮不得推进事件状态");
    }

    /** 用例 8：heartbeat 幂等登记实例租约，重复心跳不冲突、只保留一行。 */
    @Test
    public void testHeartbeat_upsertsSingleRowPerInstance() {
        dispatcher.heartbeat(DISPATCHER, INSTANCE_A, 30);
        dispatcher.heartbeat(DISPATCHER, INSTANCE_A, 30);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM dispatcher_lease", Integer.class);
        assertEquals(1, count, "同实例重复心跳应 upsert 为一行");
        Timestamp leaseExpires = jdbcTemplate.queryForObject(
                "SELECT lease_expires_at FROM dispatcher_lease", Timestamp.class);
        assertNotNull(leaseExpires);
        assertTrue(leaseExpires.after(new Timestamp(System.currentTimeMillis())), "租约应指向未来");
    }

    /** 用例 9：claim 不触碰未到期（next_retry_at 在未来）的事件。 */
    @Test
    public void testClaim_skipsNotYetDueEvents() {
        long eventId = insertPendingEvent("USER_MESSAGE_SEND");
        jdbcTemplate.update("UPDATE outbox_event SET next_retry_at = ? WHERE id = ?",
                new Timestamp(System.currentTimeMillis() + 60_000), eventId);

        assertTrue(dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).isEmpty(), "未到退避点的事件不得领取");
        makeDue(eventId);
        assertEquals(1, dispatcher.claim(DISPATCHER, INSTANCE_A, 30, 10).size(), "到期后应可领取");
    }

}
