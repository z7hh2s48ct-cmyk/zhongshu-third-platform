package cn.zszj.module.system.framework.audit.core;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_FIELD_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JdbcAuditPort} 单元测试（ZS-AUDIT-001，H2）。
 *
 * <p>覆盖 docs/05 ZS-AUDIT-001 验收六条：业务提交后有对应审计 / 业务回滚不留成功审计 / 拒绝有明确主体·结果 /
 * 后台执行有明确主体·结果 / 投递失败可恢复 / 重复事件不重复入账。事务边界经 {@link TransactionTemplate} 显式控制
 * （提交 vs 回滚），断言经 {@link JdbcTemplate} 直查 {@code audit_event}（循 bpm-pg-harness 事务测试先例）。
 */
@Import({JdbcAuditPort.class})
public class JdbcAuditPortTest extends BaseDbUnitTest {

    @Resource
    private AuditPort auditPort;

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
    }

    private int countAudit() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM audit_event", Integer.class);
        return count == null ? 0 : count;
    }

    private AuditEventMessage.AuditEventMessageBuilder successMessage() {
        return AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED)
                .actorType(ActorType.ADMIN)
                .actorId("1024")
                .action("CREATE")
                .bizType("system_user")
                .bizId("2048")
                .result(AuditResult.SUCCESS);
    }

    /** 用例 1：业务提交后有对应审计（成功审计随事务提交，字段完整、租户不默认 0）。 */
    @Test
    public void testRecord_successCommitsWithBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            long id = auditPort.record(successMessage()
                    .traceId("trace-commit-1")
                    .detail(Map.of("maskedPhone", "138****0000"))
                    .build());
            assertTrue(id > 0, "应返回生成的事件 ID");
            assertEquals(1, countAudit(), "同事务内读己写应可见");
        });

        assertEquals(1, countAudit(), "提交后成功审计应留存");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT event_type, actor_type, actor_id, action, "
                + "biz_type, biz_id, result, trace_id, tenant_id, detail FROM audit_event");
        assertEquals("OBJECT_CREATED", row.get("event_type"));
        assertEquals("ADMIN", row.get("actor_type"));
        assertEquals("1024", row.get("actor_id"));
        assertEquals("CREATE", row.get("action"));
        assertEquals("system_user", row.get("biz_type"));
        assertEquals("2048", row.get("biz_id"));
        assertEquals("SUCCESS", row.get("result"));
        assertEquals("trace-commit-1", row.get("trace_id"));
        assertNull(row.get("tenant_id"), "技术租户未提供时应为空，不默认 0");
        assertNotNull(row.get("detail"));
        assertTrue(String.valueOf(row.get("detail")).contains("maskedPhone"), "明细应以 JSON 落库");
    }

    /** 用例 2：业务回滚不留成功审计（成功审计与业务同事务，回滚即消失）。 */
    @Test
    public void testRecord_successRollbackLeavesNoAudit() {
        transactionTemplate.executeWithoutResult(status -> {
            auditPort.record(successMessage().build());
            assertEquals(1, countAudit(), "回滚前同事务内可见");
            status.setRollbackOnly();
        });

        assertEquals(0, countAudit(), "业务回滚后不应留下成功审计");
    }

    /** 用例 3：拒绝有明确主体·结果，且以独立事务记录（业务回滚仍留存拒绝审计）。 */
    @Test
    public void testRecord_deniedIsIndependentOfBusinessTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            long id = auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.ACCESS_DENIED)
                    .actorType(ActorType.USER)
                    .actorId("2048")
                    .action("DELETE")
                    .bizType("system_user")
                    .bizId("4096")
                    .reason("无删除权限")
                    .result(AuditResult.DENIED)
                    .build());
            assertTrue(id > 0);
            status.setRollbackOnly();
        });

        assertEquals(1, countAudit(), "拒绝审计独立于业务事务，业务回滚后仍留存");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT actor_type, actor_id, result, reason FROM audit_event");
        assertEquals("USER", row.get("actor_type"));
        assertEquals("2048", row.get("actor_id"));
        assertEquals("DENIED", row.get("result"));
        assertEquals("无删除权限", row.get("reason"));
    }

    /** 用例 4：后台执行有明确主体·结果（WORKER 主体、实例名、结果落库）。 */
    @Test
    public void testRecord_backgroundExecutionHasExplicitActorAndResult() {
        long id = auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.BACKGROUND_EXECUTION)
                .actorType(ActorType.WORKER)
                .actorId("job-worker-instance-1")
                .action("EXECUTE")
                .bizType("infra_job")
                .bizId("7")
                .result(AuditResult.SUCCESS)
                .build());

        assertTrue(id > 0);
        assertEquals(1, countAudit());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT actor_type, actor_id, result FROM audit_event");
        assertEquals("WORKER", row.get("actor_type"));
        assertEquals("job-worker-instance-1", row.get("actor_id"));
        assertEquals("SUCCESS", row.get("result"));
    }

    /** 用例 5：投递失败可恢复——失败（回滚）不留残迹，携同一幂等键重试后恰好一条。 */
    @Test
    public void testRecord_deliveryFailureIsRecoverable() {
        String key = "idem-recover-1";

        transactionTemplate.executeWithoutResult(status -> {
            auditPort.record(successMessage().idempotencyKey(key).build());
            status.setRollbackOnly();
        });
        assertEquals(0, countAudit(), "投递失败（回滚）后不应留残迹");

        transactionTemplate.executeWithoutResult(status ->
                auditPort.record(successMessage().idempotencyKey(key).build()));
        assertEquals(1, countAudit(), "重试恢复后应恰好留一条");
    }

    /** 用例 6：重复事件不重复入账（同一幂等键只入账一次，返回同一事件 ID）。 */
    @Test
    public void testRecord_duplicateEventNotDoubleRecorded() {
        String key = "idem-dup-1";

        long first = transactionTemplate.execute(status ->
                auditPort.record(successMessage().idempotencyKey(key).build()));
        long second = transactionTemplate.execute(status ->
                auditPort.record(successMessage().idempotencyKey(key).build()));

        assertEquals(1, countAudit(), "携同一幂等键的重复事件只入账一次");
        assertTrue(first > 0, "首次应返回生成的事件 ID");
        assertEquals(first, second, "幂等命中应返回同一事件 ID");
    }

    /** 用例 7（fail-closed 不变式）：缺失必填字段拒绝写入并抛 ServiceException，事件不落库。 */
    @Test
    public void testRecord_missingRequiredFieldIsRejectedFailClosed() {
        // 缺 eventType
        ServiceException missingEventType = assertThrows(ServiceException.class, () -> auditPort.record(
                AuditEventMessage.builder().actorType(ActorType.ADMIN).result(AuditResult.SUCCESS).build()));
        assertEquals(AUDIT_EVENT_FIELD_MISSING.getCode(), missingEventType.getCode());
        // 缺 actorType（无主体的模糊审计必须被拒）
        assertThrows(ServiceException.class, () -> auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED).result(AuditResult.SUCCESS).build()));
        // 缺 result
        assertThrows(ServiceException.class, () -> auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED).actorType(ActorType.ADMIN).build()));

        assertEquals(0, countAudit(), "fail-closed 拒绝的事件不应落库");
    }

    /**
     * 用例 8（P2-1 fail-closed 事务兜底）：调用方在业务事务内吞掉 SUCCESS 审计的 fail-closed 异常，业务仍不能提交。
     *
     * <p>外层业务事务先写一条合法成功审计，再触发一条缺 {@code eventType} 的成功审计并 catch 吞掉异常；
     * fail-closed 应已将所参与事务标记 rollback-only，故外层提交时整体回滚（{@link UnexpectedRollbackException}），
     * 连先前那条合法审计一并撤销——杜绝「业务提交却无成功审计」。
     */
    @Test
    public void testRecord_successFailClosedMarksCallerTransactionRollbackOnly() {
        assertThrows(UnexpectedRollbackException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            auditPort.record(successMessage().build());
            assertEquals(1, countAudit(), "回滚前同事务内可见");
            try {
                auditPort.record(AuditEventMessage.builder()
                        .actorType(ActorType.ADMIN)
                        .result(AuditResult.SUCCESS)
                        .build()); // 缺 eventType，fail-closed
            } catch (ServiceException swallowed) {
                // 调用方吞掉异常——但所参与事务已被标记 rollback-only
            }
        }));

        assertEquals(0, countAudit(), "吞掉 fail-closed 异常后业务事务仍须整体回滚，不留成功审计");
    }

    /**
     * 用例 9（P2-2 幂等键归一化）：空白幂等键（空串 / 纯空格）视作「无键」归一化为 NULL，
     * 多条无键成功事件不在唯一索引上互相碰撞（NULL 互异），各自入账。
     */
    @Test
    public void testRecord_blankIdempotencyKeysAreNormalizedToNull() {
        long emptyFirst = auditPort.record(successMessage().idempotencyKey("").build());
        long emptySecond = auditPort.record(successMessage().idempotencyKey("").build());
        long whitespace = auditPort.record(successMessage().idempotencyKey("   ").build());

        assertTrue(emptyFirst > 0 && emptySecond > 0 && whitespace > 0, "空白幂等键应视作无键，各自入账");
        assertNotEquals(emptyFirst, emptySecond, "无键事件应生成不同事件 ID（非幂等命中）");
        assertEquals(3, countAudit(), "空白幂等键归一化为 NULL，三条互不碰撞、各留一条");
    }

}
