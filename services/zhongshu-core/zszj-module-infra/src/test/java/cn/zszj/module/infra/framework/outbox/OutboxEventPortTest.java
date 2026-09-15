package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_FIELD_MISSING;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_PAYLOAD_SERIALIZE_FAILED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_TRANSACTION_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JdbcReliableEventPort} 单元测试（ZS-JOB-002，H2）。
 *
 * <p>覆盖 docs/05 ZS-JOB-002 验收前两条：业务回滚无事件、提交必有事件；及调整条款的 fail-closed 三守卫
 * （无事务上下文拒绝 / 缺租户上下文拒绝不默认 0 / 必填字段校验）与序列化失败回滚兜底。事务边界经
 * {@link TransactionTemplate} 显式控制（提交 vs 回滚），断言经 {@link JdbcTemplate} 直查 {@code outbox_event}
 * （循 ZS-AUDIT-001 JdbcAuditPortTest 先例）。
 */
@Import({JdbcReliableEventPort.class})
public class OutboxEventPortTest extends BaseDbUnitTest {

    @Resource
    private ReliableEventPort eventPort;

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
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    private int countOutbox() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM outbox_event", Integer.class);
        return count == null ? 0 : count;
    }

    private OutboxEventMessage.OutboxEventMessageBuilder message() {
        return OutboxEventMessage.builder()
                .eventType("USER_MESSAGE_SEND")
                .bizType("infra_file")
                .bizId("2048")
                .bizVersion("3")
                .payload(Map.of("fileId", 2048, "action", "published"))
                .headers(Map.of("trace-source", "unit-test"))
                .actorType(OutboxEventMessage.OutboxActorType.ADMIN)
                .actorId("1024")
                .traceId("trace-job002-1");
    }

    /** 用例 1：业务提交必有事件——事件随业务事务提交，docs/05 事件合同字段完整落库、状态初始 PENDING。 */
    @Test
    public void testAppend_commitsWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            long id = eventPort.append(message().build());
            assertTrue(id > 0, "应返回生成的事件 ID");
            assertEquals(1, countOutbox(), "同事务内读己写应可见");
        });

        assertEquals(1, countOutbox(), "提交后事件应留存");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT event_type, biz_type, biz_id, biz_version, "
                + "payload, headers, status, retry_count, tenant_id, actor_type, actor_id, trace_id FROM outbox_event");
        assertEquals("USER_MESSAGE_SEND", row.get("event_type"));
        assertEquals("infra_file", row.get("biz_type"));
        assertEquals("2048", row.get("biz_id"));
        assertEquals("3", row.get("biz_version"));
        assertTrue(String.valueOf(row.get("payload")).contains("\"fileId\""), "载荷应以 JSON 落库");
        assertTrue(String.valueOf(row.get("headers")).contains("\"trace-source\""), "附带头应以 JSON 落库");
        assertEquals("PENDING", row.get("status"));
        assertEquals(0, ((Number) row.get("retry_count")).intValue());
        assertEquals(1L, ((Number) row.get("tenant_id")).longValue(), "租户取自上下文");
        assertEquals("ADMIN", row.get("actor_type"));
        assertEquals("1024", row.get("actor_id"));
        assertEquals("trace-job002-1", row.get("trace_id"));
    }

    /** 用例 2：业务回滚无事件——append 与业务写同事务，回滚即消失。 */
    @Test
    public void testAppend_rollsBackWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPort.append(message().build());
            assertEquals(1, countOutbox(), "回滚前同事务内可见");
            status.setRollbackOnly();
        });

        assertEquals(0, countOutbox(), "业务回滚后不应留下事件");
    }

    /**
     * 用例 3（fail-closed 事务守卫）：无事务上下文直接 append 必须拒绝且不代开事务——
     * 供体无此约束，静默自提交会丢失「业务回滚无事件」保证。
     */
    @Test
    public void testAppend_requiresActiveTransaction() {
        ServiceException e = assertThrows(ServiceException.class, () -> eventPort.append(message().build()));
        assertEquals(OUTBOX_EVENT_TRANSACTION_REQUIRED.getCode(), e.getCode());
        assertEquals(0, countOutbox(), "被拒绝的写入不应落库");
    }

    /**
     * 用例 4（fail-closed 租户守卫）：缺技术租户上下文必须拒绝——供体「缺 tenant 默认写 0」会伪造归属，已去除。
     */
    @Test
    public void testAppend_requiresTenantContext() {
        TenantContextHolder.clear();
        ServiceException e = assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> eventPort.append(message().build())));
        assertEquals(OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED.getCode(), e.getCode());
        assertEquals(0, countOutbox(), "被拒绝的写入不应落库");
    }

    /**
     * 用例 5（fail-closed 必填校验）：message/eventType/actorType 缺失即拒绝，事件不落库。
     * 每个守卫失败各自独立事务验证：异常传出即回滚（codex r0 后守卫失败会标记 rollback-only，
     * 同事务连续验证会在提交时升级为 UnexpectedRollbackException，故不再共事务）。
     */
    @Test
    public void testAppend_missingRequiredFieldsRejected() {
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> eventPort.append(null)));
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> eventPort.append(message().eventType(null).build())));
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> eventPort.append(message().eventType("  ").build())));
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> eventPort.append(message().actorType(null).build())));
        assertEquals(0, countOutbox(), "fail-closed 拒绝的事件不应落库");
    }

    /**
     * 用例 5b（codex r0 P1）：调用方在业务事务内吞掉必填/租户守卫异常，业务仍不能提交——
     * 守卫失败已将所参与事务标记 rollback-only，提交时升级为 UnexpectedRollbackException，
     * 杜绝「业务提交却无事件」。
     */
    @Test
    public void testAppend_swallowedGuardFailuresStillRollBackBusiness() {
        // 吞掉缺 actorType 的守卫异常 → 事务 rollback-only → 提交抛 UnexpectedRollbackException
        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            assertThrows(ServiceException.class, () -> eventPort.append(message().actorType(null).build()));
        }));
        assertEquals(0, countOutbox());

        // 吞掉缺租户上下文的守卫异常 → 同样 rollback-only
        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            TenantContextHolder.clear();
            try {
                assertThrows(ServiceException.class, () -> eventPort.append(message().build()));
            } finally {
                TenantContextHolder.setTenantId(1L);
            }
        }));
        assertEquals(0, countOutbox());
    }

    /**
     * 用例 6（fail-closed 序列化兜底）：载荷不可序列化时抛专用码；调用方吞掉异常后业务事务仍不能提交
     * ——杜绝「业务提交却无事件」（循 JdbcAuditPortTest P2-1 先例）。
     */
    @Test
    public void testAppend_serializeFailureMarksTransactionRollbackOnly() {
        Map<String, Object> evilPayload = new HashMap<>();
        evilPayload.put("self", evilPayload); // 自引用 Map，Jackson 序列化必失败

        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            assertThrows(ServiceException.class,
                    () -> eventPort.append(message().payload(evilPayload).build()));
            // 调用方吞掉序列化异常——但所参与事务已被标记 rollback-only
        }));

        assertEquals(0, countOutbox(), "吞掉序列化失败后业务事务仍须整体回滚，不留事件");
    }

    /** 用例 7：技术租户一律取自当前上下文（消息合同无租户字段），不同上下文不串用、不默认 0。 */
    @Test
    public void testAppend_tenantAlwaysFromContext() {
        TenantContextHolder.setTenantId(2L);
        transactionTemplate.executeWithoutResult(status -> eventPort.append(message().build()));

        Long tenantId = jdbcTemplate.queryForObject("SELECT tenant_id FROM outbox_event", Long.class);
        assertNotNull(tenantId);
        assertEquals(2L, tenantId, "租户应取自当前上下文");
    }

    /** 用例 8：payload 为 null 落空对象 {}，headers 为 null 落 SQL NULL（对齐供体语义、去除默认租户后其余保留）。 */
    @Test
    public void testAppend_nullPayloadAndHeadersNormalized() {
        transactionTemplate.executeWithoutResult(status -> eventPort.append(message()
                .payload(null).headers(null).bizType(null).bizId(null).bizVersion(null).build()));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT payload, headers, biz_type, biz_id, biz_version "
                + "FROM outbox_event");
        assertEquals("{}", String.valueOf(row.get("payload")).trim(), "null 载荷应归一化为空对象");
        assertNull(row.get("headers"), "null 附带头应落 SQL NULL");
        assertNull(row.get("biz_type"));
        assertNull(row.get("biz_id"));
        assertNull(row.get("biz_version"));
    }

    /**
     * 用例 9（codex r2 P1 同源配对）：TM 与数据源非同源配对（他数据源的 DataSourceTransactionManager）
     * 在构造期即拒绝——MANDATORY 只证明该 TM 存在事务，错配时 JDBC 写入会走本数据源 autocommit 连接
     * 静默自提交（业务回滚而事件留存，且无异常可捕获），必须启动即失败。
     */
    @Test
    public void testConstructor_rejectsMismatchedTransactionManager() {
        DataSource otherDataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:outbox-other;MODE=MySQL;DB_CLOSE_DELAY=-1");
        org.springframework.jdbc.datasource.DataSourceTransactionManager otherTxManager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(otherDataSource);
        assertThrows(IllegalArgumentException.class, () -> new JdbcReliableEventPort(dataSource, otherTxManager));
    }

    /**
     * 用例 10（codex r1 探针回归）：线程仅绑定本数据源的普通查询级连接（transactionActive=false，
     * 非真实事务）时 append 必须拒绝——MANDATORY 在 TM 层校验真实事务，不另开事务。
     */
    @Test
    public void testAppend_rejectsNonTransactionalConnectionBinding() throws Exception {
        java.sql.Connection connection = dataSource.getConnection();
        org.springframework.jdbc.datasource.ConnectionHolder holder =
                new org.springframework.jdbc.datasource.ConnectionHolder(connection);
        org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(dataSource, holder);
        try {
            ServiceException e = assertThrows(ServiceException.class, () -> eventPort.append(message().build()));
            assertEquals(OUTBOX_EVENT_TRANSACTION_REQUIRED.getCode(), e.getCode());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(dataSource);
            connection.close();
        }
        assertEquals(0, countOutbox(), "被拒绝的写入不应落库");
    }

    /**
     * 用例 11（持久回归）：调用方吞掉写入期 DataAccessException（超长字段触发），业务仍不能提交——
     * 写入失败路径同样标记 rollback-only。
     */
    @Test
    public void testAppend_swallowedWriteFailureStillRollsBackBusiness() {
        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            assertThrows(ServiceException.class,
                    () -> eventPort.append(message().eventType("x".repeat(200)).build()));
        }));
        assertEquals(0, countOutbox());
    }

}
