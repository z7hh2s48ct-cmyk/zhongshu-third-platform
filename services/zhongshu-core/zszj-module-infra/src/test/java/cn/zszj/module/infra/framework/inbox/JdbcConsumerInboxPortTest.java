package cn.zszj.module.infra.framework.inbox;

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
import java.util.List;
import java.util.Map;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_COMMAND_FIELD_MISSING;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_TENANT_CONTEXT_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_TRANSACTION_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JdbcConsumerInboxPort} 单元测试（ZS-JOB-003，H2）。
 *
 * <p>覆盖 docs/05 ZS-JOB-003 验收：同事件并发/ACK 丢失/重启重放只产生一次业务副作用（唯一键抢位+同事务登记）、
 * 不同租户同业务号互不冲突（键含租户）、旧版本不覆盖新状态（版本护栏）、参数冲突不当相同成功（载荷指纹）、
 * 未知外部结果有可查中间态（RESULT_UNKNOWN+查询端口）；及 MANDATORY 事务合同与 fail-closed。
 * 真实 PG 并发唯一键行为由 {@code scripts/db/run-job003-verify.mjs} 承载。
 */
@Import({JdbcConsumerInboxPort.class})
public class JdbcConsumerInboxPortTest extends BaseDbUnitTest {

    private static final String CONSUMER = "ut-consumer";

    @Resource
    private ConsumerInboxPort inboxPort;

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

    private int countInbox() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM inbox_event", Integer.class);
        return count == null ? 0 : count;
    }

    private InboxCommand.InboxCommandBuilder command() {
        return InboxCommand.builder()
                .consumer(CONSUMER)
                .eventKey("evt-1")
                .payloadHash("hash-1")
                .bizType("demo_order")
                .bizId("2048")
                .bizVersion("1")
                .traceId("trace-job003-1");
    }

    private Map<String, Object> loadRow(long inboxId) {
        return jdbcTemplate.queryForMap("SELECT status, retry_count, result, last_error, complete_time "
                + "FROM inbox_event WHERE id = ?", inboxId);
    }

    /** 用例 1：首抢 CLAIMED——同事务登记 PROCESSING，提交后字段完整留存（处理键含租户）。 */
    @Test
    public void testTryBegin_claimsAndCommitsWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin result = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, result.getOutcome());
            assertTrue(result.getRecord().getInboxId() > 0);
            assertEquals(1, countInbox(), "同事务内读己写应可见");
        });

        assertEquals(1, countInbox());
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT consumer, event_key, payload_hash, status, "
                + "tenant_id, biz_type, biz_id, biz_version, trace_id FROM inbox_event");
        assertEquals(CONSUMER, row.get("consumer"));
        assertEquals("evt-1", row.get("event_key"));
        assertEquals("hash-1", row.get("payload_hash"));
        assertEquals("PROCESSING", row.get("status"));
        assertEquals(1L, ((Number) row.get("tenant_id")).longValue(), "处理键含技术租户");
        assertEquals("demo_order", row.get("biz_type"));
        assertEquals("2048", row.get("biz_id"));
        assertEquals("1", row.get("biz_version"));
        assertEquals("trace-job003-1", row.get("trace_id"));
    }

    /** 用例 2：业务回滚抢位一并回滚（幂等记录与业务副作用同事务——ACK 丢失/重启重放不残留半态）。 */
    @Test
    public void testTryBegin_rollsBackWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            inboxPort.tryBegin(command().build());
            assertEquals(1, countInbox());
            status.setRollbackOnly();
        });
        assertEquals(0, countInbox(), "业务回滚后不应留下幂等记录");
    }

    /**
     * 用例 3（可重放业务结果）：COMPLETED 后同键重入返回首次结果——重复消费只产生一次业务副作用。
     * 模拟 ACK 丢失重放：首次处理副作用+complete，重放时 DUPLICATE_COMPLETED 携原结果。
     */
    @Test
    public void testTryBegin_duplicateCompletedReturnsReplayableResult() {
        long inboxId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, first.getOutcome());
            // 模拟业务副作用（副作用与抢位同事务）
            jdbcTemplate.update("INSERT INTO infra_config (category, type, name, config_key, value, visible) "
                    + "VALUES ('inbox-demo', 1, 'demo', 'inbox.demo.key', 'side-effect-1', TRUE)");
            assertTrue(inboxPort.complete(first.getRecord().getInboxId(), "{\"done\":true}"));
            return first.getRecord().getInboxId();
        });

        // 重放（新事务）：DUPLICATE_COMPLETED + 首次结果，不重复副作用
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin replay = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.DUPLICATE_COMPLETED, replay.getOutcome());
            assertEquals("{\"done\":true}", replay.getRecord().getResult(), "应返回首次可重放结果");
            assertEquals(inboxId, replay.getRecord().getInboxId());
        });
        Integer effects = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM infra_config WHERE config_key = 'inbox.demo.key'", Integer.class);
        assertEquals(1, effects, "重复消费不得产生第二次业务副作用");
        assertNotNull(loadRow(inboxId).get("complete_time"));
    }

    /** 用例 4：并发抢占识别——同键 PROCESSING 未提交/处理中 → DUPLICATE_IN_FLIGHT。 */
    @Test
    public void testTryBegin_duplicateInFlight() {
        transactionTemplate.executeWithoutResult(status -> inboxPort.tryBegin(command().build()));

        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin second = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.DUPLICATE_IN_FLIGHT, second.getOutcome());
            assertEquals("PROCESSING", second.getRecord().getStatus());
        });
        assertEquals(1, countInbox(), "并发抢占不得新增记录");
    }

    /** 用例 5（参数冲突）：同键不同载荷指纹 → PARAM_CONFLICT，不得当作相同成功。 */
    @Test
    public void testTryBegin_paramConflictRejected() {
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            inboxPort.complete(first.getRecord().getInboxId(), "{\"done\":true}");
        });

        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin conflict = inboxPort.tryBegin(command().payloadHash("hash-DIFFERENT").build());
            assertEquals(InboxTryBegin.Outcome.PARAM_CONFLICT, conflict.getOutcome(), "参数冲突不得当作相同成功");
        });
    }

    /** 用例 6（租户隔离）：不同租户同消费者同事件键互不冲突，各自处理。 */
    @Test
    public void testTryBegin_crossTenantKeysIndependent() {
        transactionTemplate.executeWithoutResult(status -> inboxPort.tryBegin(command().build()));

        TenantContextHolder.setTenantId(2L);
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin other = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, other.getOutcome(), "不同租户同业务号互不冲突");
        });

        Integer tenants = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT tenant_id) FROM inbox_event", Integer.class);
        assertEquals(2, tenants);
    }

    /** 用例 7（版本乱序护栏）：同对象已有更高已完成版本时，旧版本 STALE_VERSION 拒绝；非数值版本跳过比较。 */
    @Test
    public void testTryBegin_staleVersionRejected() {
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin v3 = inboxPort.tryBegin(command().eventKey("evt-v3").bizVersion("3")
                    .checkVersionStale(true).build());
            inboxPort.complete(v3.getRecord().getInboxId(), null);
        });

        transactionTemplate.executeWithoutResult(status -> {
            // 旧版本事件不得覆盖新状态
            InboxTryBegin stale = inboxPort.tryBegin(command().eventKey("evt-v2").bizVersion("2")
                    .checkVersionStale(true).build());
            assertEquals(InboxTryBegin.Outcome.STALE_VERSION, stale.getOutcome(), "旧版本不得覆盖新状态");
            // 更新版本可正常处理
            InboxTryBegin newer = inboxPort.tryBegin(command().eventKey("evt-v4").bizVersion("4")
                    .checkVersionStale(true).build());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, newer.getOutcome());
            // 未启用护栏时不判定（语义由消费者自行负责）
            InboxTryBegin unchecked = inboxPort.tryBegin(command().eventKey("evt-v1-old").bizVersion("1")
                    .checkVersionStale(false).build());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, unchecked.getOutcome());
        });
    }

    /** 用例 8（失败重试）：fail 后重入即重领（retry_count+1，状态回 PROCESSING），可重新处理。 */
    @Test
    public void testFail_thenRetriedClaimIncrementsRetryCount() {
        long inboxId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            assertTrue(inboxPort.fail(first.getRecord().getInboxId(),
                    new IllegalStateException("sink 失败-含 Bearer abc123XYZ 敏感样本")));
            return first.getRecord().getInboxId();
        });
        assertEquals("FAILED", loadRow(inboxId).get("status"));
        String lastError = String.valueOf(loadRow(inboxId).get("last_error"));
        assertTrue(lastError.contains("IllegalStateException"));
        assertFalse(lastError.contains("Bearer abc123XYZ"), "失败描述不得落异常原文");

        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin retry = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.RETRIED_CLAIMED, retry.getOutcome());
            assertEquals(1, retry.getRecord().getRetryCount(), "重领应推进重试计数");
        });
        assertEquals("PROCESSING", loadRow(inboxId).get("status"));
    }

    /** 用例 9（可查中间态）：markResultUnknown 留 RESULT_UNKNOWN，重入返回 DUPLICATE_RESULT_UNKNOWN 须先回查；查询端口可见。 */
    @Test
    public void testMarkResultUnknown_queryableIntermediateState() {
        long inboxId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            assertTrue(inboxPort.markResultUnknown(first.getRecord().getInboxId(), "外部渠道超时"));
            return first.getRecord().getInboxId();
        });

        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin reentry = inboxPort.tryBegin(command().build());
            assertEquals(InboxTryBegin.Outcome.DUPLICATE_RESULT_UNKNOWN, reentry.getOutcome(), "结果未知不得盲目重处理");
        });

        List<InboxRecord> unknown = inboxPort.listByStatus(CONSUMER, "RESULT_UNKNOWN", 10);
        assertEquals(1, unknown.size(), "回查端口应可见中间态");
        assertEquals(inboxId, unknown.get(0).getInboxId());
        // 回查端口单查
        assertTrue(inboxPort.find(CONSUMER, "evt-1").isPresent());
        assertTrue(inboxPort.find(CONSUMER, "evt-missing").isEmpty());
    }

    /** 用例 10（状态推进守卫）：仅 PROCESSING 可推进——COMPLETED 后再 complete/fail 返回 false。 */
    @Test
    public void testTransitions_onlyFromProcessing() {
        long inboxId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            assertTrue(inboxPort.complete(first.getRecord().getInboxId(), "{\"done\":true}"));
            return first.getRecord().getInboxId();
        });
        transactionTemplate.executeWithoutResult(status -> {
            assertFalse(inboxPort.complete(inboxId, "{\"again\":true}"), "终态不得重复推进");
            assertFalse(inboxPort.fail(inboxId, new RuntimeException("late")));
            assertFalse(inboxPort.markResultUnknown(inboxId, "late"));
        });
        assertEquals("{\"done\":true}", loadRow(inboxId).get("result"), "首次结果不被覆盖");
    }

    /** 用例 11（MANDATORY 合同）：无事务上下文拒绝；吞掉守卫异常业务仍回滚。 */
    @Test
    public void testTryBegin_requiresBusinessTransaction() {
        ServiceException e = assertThrows(ServiceException.class, () -> inboxPort.tryBegin(command().build()));
        assertEquals(INBOX_TRANSACTION_REQUIRED.getCode(), e.getCode());
        assertEquals(0, countInbox());

        // 吞掉缺租户守卫异常 → 事务 rollback-only → 提交抛 UnexpectedRollbackException
        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            TenantContextHolder.clear();
            try {
                assertThrows(ServiceException.class, () -> inboxPort.tryBegin(command().build()));
            } finally {
                TenantContextHolder.setTenantId(1L);
            }
        }));
        assertEquals(0, countInbox());
    }

    /** 用例 12（fail-closed 必填）：consumer/eventKey/payloadHash 缺失即拒绝（各自独立事务，异常传出即回滚）。 */
    @Test
    public void testTryBegin_missingRequiredFieldsRejected() {
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> inboxPort.tryBegin(command().consumer(null).build())));
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> inboxPort.tryBegin(command().eventKey("  ").build())));
        ServiceException missingHash = assertThrows(ServiceException.class,
                () -> transactionTemplate.executeWithoutResult(
                        status -> inboxPort.tryBegin(command().payloadHash(null).build())));
        assertEquals(INBOX_COMMAND_FIELD_MISSING.getCode(), missingHash.getCode());
        // 缺租户上下文拒绝（不默认 0）
        TenantContextHolder.clear();
        assertThrows(ServiceException.class, () -> transactionTemplate.executeWithoutResult(
                status -> inboxPort.tryBegin(command().build())));
        TenantContextHolder.setTenantId(1L);
        assertEquals(0, countInbox(), "fail-closed 拒绝不应落库");
    }

    /** 用例 13（同源配对）：TM 与数据源非同源配对在构造期即拒绝（codex JOB-002 r2 同款缺陷预防）。 */
    @Test
    public void testConstructor_rejectsMismatchedTransactionManager() {
        DataSource otherDataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:inbox-other;MODE=MySQL;DB_CLOSE_DELAY=-1");
        org.springframework.jdbc.datasource.DataSourceTransactionManager otherTxManager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(otherDataSource);
        assertThrows(IllegalArgumentException.class, () -> new JdbcConsumerInboxPort(dataSource, otherTxManager));
    }

    /**
     * 用例 16（codex r1 P1 版本水位）：水位按版本大小单调维护，与登记 id 序无关，且采用<b>占坑语义</b>——
     * tryBegin 通过护栏即抬水位（新版本已尝试即封锁更旧版本，防「旧版本后提交覆盖」乱序窗口；业务整体回滚
     * 则水位一并回落）。v10 先登记失败（占坑至 10），v2 被拒；v10 重领（10==10 非旧版本）完成；
     * 其后 v3 重放判旧版本、v11 可处理。失败占坑后的旧版本处置归人工/补偿（D-07 登记语义）。
     */
    @Test
    public void testTryBegin_watermarkMonotonicByVersionNotById() {
        // v10 登记（占坑水位=10）→失败：更旧版本自此被拒，等 v10 自身重试或人工处置
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin v10 = inboxPort.tryBegin(command().eventKey("evt-w10").bizVersion("10")
                    .checkVersionStale(true).build());
            inboxPort.fail(v10.getRecord().getInboxId(), new IllegalStateException("第一次失败"));
        });
        transactionTemplate.executeWithoutResult(status -> {
            assertEquals(InboxTryBegin.Outcome.STALE_VERSION, inboxPort.tryBegin(
                    command().eventKey("evt-w2").bizVersion("2").checkVersionStale(true).build())
                    .getOutcome());
        });
        // v10 重领（incoming=10 == 水位，非旧版本）→ 完成
        transactionTemplate.executeWithoutResult(status -> {
            InboxTryBegin v10retry = inboxPort.tryBegin(command().eventKey("evt-w10").bizVersion("10")
                    .checkVersionStale(true).build());
            assertEquals(InboxTryBegin.Outcome.RETRIED_CLAIMED, v10retry.getOutcome());
            inboxPort.complete(v10retry.getRecord().getInboxId(), "{\"v\":10}");
        });
        // 水位 10：v3 后续重放 → STALE；v11 → 可处理
        transactionTemplate.executeWithoutResult(status -> {
            assertEquals(InboxTryBegin.Outcome.STALE_VERSION, inboxPort.tryBegin(
                    command().eventKey("evt-w3-replay").bizVersion("3").checkVersionStale(true).build())
                    .getOutcome());
            assertEquals(InboxTryBegin.Outcome.CLAIMED, inboxPort.tryBegin(
                    command().eventKey("evt-w11").bizVersion("11").checkVersionStale(true).build())
                    .getOutcome());
        });
    }

    /** 用例 14（codex r0 P1 确认出口）：RESULT_UNKNOWN 经 resolveAfterVerification 按回查依据推进——
     *  核实已执行 → COMPLETED（携核实结果）；核实未执行 → FAILED（可重试）。 */
    @Test
    public void testResolveAfterVerification_exitsUnknownState() {
        long unknownId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().eventKey("evt-unknown").build());
            assertTrue(inboxPort.markResultUnknown(first.getRecord().getInboxId(), "外部渠道超时"));
            return first.getRecord().getInboxId();
        });

        // 回查核实：确已执行 → COMPLETED
        Boolean verifiedExecuted = transactionTemplate.execute(status -> {
            return inboxPort.resolveAfterVerification(unknownId, true, "{\"verified\":true}", "渠道对账单 #77");
        });
        assertTrue(verifiedExecuted);
        assertEquals("COMPLETED", loadRow(unknownId).get("status"));
        assertEquals("{\"verified\":true}", loadRow(unknownId).get("result"));

        // 第二条：核实未执行 → FAILED 可重试（重入即 RETRIED_CLAIMED）
        long unknown2 = transactionTemplate.execute(status -> {
            InboxTryBegin second = inboxPort.tryBegin(command().eventKey("evt-unknown-2").build());
            assertTrue(inboxPort.markResultUnknown(second.getRecord().getInboxId(), "响应丢失"));
            return second.getRecord().getInboxId();
        });
        Boolean verifiedNotExecuted = transactionTemplate.execute(status -> {
            return inboxPort.resolveAfterVerification(unknown2, false, null, "渠道侧无此事件");
        });
        assertTrue(verifiedNotExecuted);
        assertEquals("FAILED", loadRow(unknown2).get("status"));
        transactionTemplate.executeWithoutResult(status -> {
            assertEquals(InboxTryBegin.Outcome.RETRIED_CLAIMED,
                    inboxPort.tryBegin(command().eventKey("evt-unknown-2").build()).getOutcome());
        });
    }

    /** 用例 15（codex r0 P1 租户全量强制）：跨租户推进/回查一律拒绝——他租户记录不可推进、不可见。 */
    @Test
    public void testCrossTenant_advanceAndQueryRejected() {
        long inboxId = transactionTemplate.execute(status -> {
            InboxTryBegin first = inboxPort.tryBegin(command().build());
            return first.getRecord().getInboxId();
        });

        // 租户 2 上下文：不可推进租户 1 的记录，回查不可见
        TenantContextHolder.setTenantId(2L);
        transactionTemplate.executeWithoutResult(status -> {
            assertFalse(inboxPort.complete(inboxId, "{\"hijack\":true}"), "跨租户不得推进");
            assertFalse(inboxPort.fail(inboxId, new RuntimeException("x")));
            assertFalse(inboxPort.markResultUnknown(inboxId, "x"));
        });
        assertTrue(inboxPort.find(CONSUMER, "evt-1").isEmpty(), "跨租户回查不可见");
        assertEquals(0, inboxPort.listByStatus(CONSUMER, "PROCESSING", 10).size());

        // 回到属主租户：一切正常
        TenantContextHolder.setTenantId(1L);
        Boolean ownerComplete = transactionTemplate.execute(status -> {
            return inboxPort.complete(inboxId, "{\"ok\":true}");
        });
        assertTrue(ownerComplete);
        assertTrue(inboxPort.find(CONSUMER, "evt-1").isPresent());
    }

}
