package cn.zszj.module.system.framework.audit.core;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-FILE-004/AUDIT 批次执行入口（docs/03 §B04）：审计历史不可改写测试（H2）。
 *
 * <p>合同：①审计仅可追加——AuditPort 公共合同只有 {@code record}，不存在任何改写/删除通道；
 * ②结构不可改写——audit_event 无 updater/update_time/deleted 改写语义列；③追加不扰动既有历史；
 * ④并发记录零丢失。</p>
 */
@Import({JdbcAuditPort.class})
public class AuditImmutabilityTest extends BaseDbUnitTest {

    @Resource
    private AuditPort auditPort;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private AuditEventMessage message(String eventType, String idempotencyKey) {
        return AuditEventMessage.builder()
                .eventType(eventType)
                .actorType(ActorType.ADMIN)
                .actorId("1024")
                .action("CREATE")
                .bizType("system_user")
                .bizId("2048")
                .result(AuditResult.SUCCESS)
                .detail(Map.of("k", "v"))
                .idempotencyKey(idempotencyKey)
                .build();
    }

    // ========== ① 合同不可改写：AuditPort 公共方法仅 record ==========

    @Test
    public void auditPortContract_isAppendOnly_noMutationChannel() {
        Set<String> methods = Arrays.stream(AuditPort.class.getMethods())
                .filter(m -> !m.isSynthetic() && !m.getDeclaringClass().equals(Object.class))
                .map(Method::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of("record"), methods,
                "审计端口不得暴露任何更新/删除通道（历史只可追加）");
    }

    // ========== ② 结构不可改写：audit_event 无改写语义列 ==========

    @Test
    public void auditEventSchema_hasNoMutationColumns() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        Integer mutationColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'AUDIT_EVENT' "
                        + "AND UPPER(COLUMN_NAME) IN ('UPDATER', 'UPDATE_TIME', 'DELETED')",
                Integer.class);
        assertEquals(0, mutationColumns == null ? 1 : mutationColumns,
                "审计表不得包含更新者/更新时间/逻辑删除等改写语义列");
    }

    // ========== ③ 追加不扰动既有历史 ==========

    @Test
    public void record_appendsWithoutMutatingPriorHistory() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        Long firstId = auditPort.record(message(AuditEventTypes.OBJECT_CREATED, null));
        Map<String, Object> before = jdbcTemplate.queryForMap(
                "SELECT \"id\", \"event_type\", \"actor_id\", \"detail\" FROM audit_event WHERE \"id\" = ?", firstId);

        auditPort.record(message(AuditEventTypes.OBJECT_UPDATED, null));
        auditPort.record(message(AuditEventTypes.OBJECT_DELETED, null));

        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT \"id\", \"event_type\", \"actor_id\", \"detail\" FROM audit_event WHERE \"id\" = ?", firstId);
        assertEquals(before, after, "追加新事件不得扰动既有历史行");
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM audit_event", Integer.class);
        assertEquals(3, total == null ? 0 : total);
    }

    // ========== ④ 幂等键：同一事件不重复入账（历史不被复制） ==========

    @Test
    public void record_sameIdempotencyKey_noDuplicateHistory() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        Long first = auditPort.record(message(AuditEventTypes.OBJECT_CREATED, "idem-key-1"));
        Long second = auditPort.record(message(AuditEventTypes.OBJECT_CREATED, "idem-key-1"));
        assertEquals(first, second, "同幂等键重复记录必须返回既有事件");
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM audit_event", Integer.class);
        assertEquals(1, total == null ? 0 : total, "重复事件不得复制历史");
    }

    // ========== ⑤ 并发记录零丢失 ==========

    @Test
    public void record_concurrent_noLoss() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentHashMap<Long, Boolean> ids = new ConcurrentHashMap<>();
        for (int i = 0; i < threads; i++) {
            final String key = "concurrent-" + i;
            pool.submit(() -> {
                start.await();
                ids.put(auditPort.record(message(AuditEventTypes.BACKGROUND_EXECUTION, key)), Boolean.TRUE);
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发记录应在时限内完成");
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM audit_event", Integer.class);
        assertEquals(threads, total == null ? 0 : total, "并发记录不得丢失（历史零丢失）");
        assertEquals(threads, ids.size(), "并发记录事件 ID 不得重复");
    }

}
