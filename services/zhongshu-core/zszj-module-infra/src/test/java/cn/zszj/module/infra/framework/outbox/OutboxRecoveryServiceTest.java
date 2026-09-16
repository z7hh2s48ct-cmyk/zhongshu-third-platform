package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthProperties;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxEventRecoveryDetail;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryAction;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryCmd;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryResult;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryService;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OutboxRecoveryServiceImpl} 单元测试（ZS-JOB-004，H2）。
 *
 * <p>覆盖 docs/05 ZS-JOB-004 验收：②「有权人员恢复后状态和审计可追踪」（用例 5/6/7）、
 * ③「无权重放、无限重试、修改历史被拒」（用例 8/9/10/11/12）、敏感参数与异常脱敏 + 恢复前后关联
 * （用例 13/14）、跨租户隔离（用例 15）。DEAD 事件的「连续失败达上限」转移机制由 JOB-002 承担，
 * 本测试直接种子 DEAD 事件聚焦恢复语义（真实 DEAD 转移在 {@link OutboxHealthMonitorTest} 用例 1 验证）。
 *
 * <p>{@link AuditPort} 以 Mockito Bean 替身（真实 JdbcAuditPort 的独立事务语义已在 ZS-AUDIT-001 验证）；
 * 恢复四法经 {@code @Import} 装配为 Spring 代理 bean，其 {@code @Transactional} 在无环境事务的
 * {@link BaseDbUnitTest} 中各自提交，故可用 {@link JdbcTemplate} 直读已提交的库表断言前后关联。
 */
@Import({OutboxRecoveryServiceImpl.class})
public class OutboxRecoveryServiceTest extends BaseDbUnitTest {

    /** 人工重试上限（与 {@code OutboxHealthProperties} 默认一致）；用例 20 以此桩兼作并发栅栏。 */
    private static final int MAX_MANUAL_RETRY = 3;

    /**
     * 用例 20 中「先到达线程」持 {@code FOR UPDATE} 行锁等待「后到达线程」读到过期计数的上限（毫秒）。
     * 必须 &lt; H2 默认 {@code LOCK_TIMEOUT}(1000ms)：修复实现下后到达线程阻塞在载入事件处、永不到达栅栏，
     * 先到达线程据此短超时放行并提交，后到达线程只等这一个短事务、不会锁超时。
     */
    private static final long A_LOCK_HOLD_MS = 250;

    @Resource
    private OutboxRecoveryService recoveryService;

    @MockitoBean
    private AuditPort auditPort;

    @MockitoBean
    private OutboxHealthProperties properties;

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(1L);
        when(properties.getMaxManualRetry()).thenReturn(MAX_MANUAL_RETRY);
    }

    // ========== 夹具与助手 ==========

    /** 种子一条指定状态事件；DEAD 事件带受控异常摘要（errorClass/messageLength），retry_count=5（已达上限）。 */
    private long insertEvent(String status, String payload, long tenantId) {
        boolean dead = "DEAD".equals(status);
        jdbcTemplate.update("INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, headers, status, "
                        + "retry_count, next_retry_at, last_error, tenant_id, actor_type, actor_id, trace_id) "
                        + "VALUES ('USER_MESSAGE_SEND', 'infra_file', '2048', ?, 'ut-headers', ?, ?, ?, ?, ?, "
                        + "'SYSTEM', 'ut-worker', 'trace-ut')",
                payload, status, dead ? 5 : 0, new Timestamp(System.currentTimeMillis() - 1000),
                dead ? "{\"errorClass\":\"IllegalStateException\",\"messageLength\":18}" : null, tenantId);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM outbox_event WHERE tenant_id = ? ORDER BY id DESC LIMIT 1", Long.class, tenantId);
        return id == null ? -1 : id;
    }

    private long insertDead(long tenantId) {
        return insertEvent("DEAD", "{}", tenantId);
    }

    /** 读回事件当前库表快照（RowMapper 按列标签取值，规避 H2 列名大小写与 queryForMap 键不一致）。 */
    private Map<String, Object> loadRow(long eventId) {
        return jdbcTemplate.queryForObject("SELECT status, payload, headers, event_type, claimed_by, claim_token, "
                        + "claim_expires_at, next_retry_at FROM outbox_event WHERE id = ?",
                (rs, n) -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("status", rs.getString("status"));
                    m.put("payload", rs.getString("payload"));
                    m.put("headers", rs.getString("headers"));
                    m.put("event_type", rs.getString("event_type"));
                    m.put("claimed_by", rs.getString("claimed_by"));
                    m.put("claim_token", rs.getString("claim_token"));
                    m.put("claim_expires_at", rs.getTimestamp("claim_expires_at"));
                    m.put("next_retry_at", rs.getTimestamp("next_retry_at"));
                    return m;
                }, eventId);
    }

    private int countRecoveryLog(long eventId, String action) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_recovery_log WHERE event_id = ? AND action = ?",
                Integer.class, eventId, action);
        return n == null ? 0 : n;
    }

    private Map<String, Object> loadRecoveryLog(long eventId, String action) {
        return jdbcTemplate.queryForObject("SELECT before_status, after_status, before_retry_count, "
                        + "manual_retry_seq, operator_type, operator_id, reason FROM outbox_recovery_log "
                        + "WHERE event_id = ? AND action = ? ORDER BY id DESC LIMIT 1",
                (rs, n) -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("before_status", rs.getString("before_status"));
                    m.put("after_status", rs.getString("after_status"));
                    m.put("before_retry_count", rs.getInt("before_retry_count"));
                    m.put("manual_retry_seq", rs.getInt("manual_retry_seq"));
                    m.put("operator_type", rs.getString("operator_type"));
                    m.put("operator_id", rs.getString("operator_id"));
                    m.put("reason", rs.getString("reason"));
                    return m;
                }, eventId, action);
    }

    /** 预置 times 条 RETRY 台账，模拟本事件已发生的人工重试次数（无限重试护栏依据）。 */
    private void seedRetryLog(long eventId, int times, long tenantId) {
        for (int i = 1; i <= times; i++) {
            jdbcTemplate.update("INSERT INTO outbox_recovery_log (event_id, action, reason, before_status, "
                            + "after_status, before_retry_count, manual_retry_seq, operator_type, operator_id, "
                            + "tenant_id, trace_id) VALUES (?, 'RETRY', 'seed-prior', 'DEAD', 'PENDING', 5, ?, "
                            + "'ADMIN', '1', ?, 'trace-ut')",
                    eventId, i, tenantId);
        }
    }

    private OutboxRecoveryCmd cmd(long eventId, String reason, String operatorId) {
        return OutboxRecoveryCmd.builder()
                .eventId(eventId).reason(reason).operatorType("ADMIN").operatorId(operatorId)
                .traceId("trace-ut").build();
    }

    // ========== 用例 5/6/7：授权恢复 + 前后关联 + 双轨审计 ==========

    /** 用例 5（验收②）：授权重试 DEAD→PENDING，清租约、重置退避，台账保留恢复前后关联。 */
    @Test
    public void test_授权重试DEAD_回转PENDING_台账记录前后关联() {
        long eventId = insertDead(1L);

        OutboxRecoveryResult result = recoveryService.retryDeadEvent(cmd(eventId, "根因已修复，授权重试", "1"));

        assertTrue(result.isChanged(), "授权重试应真实生效");
        assertEquals(OutboxRecoveryAction.RETRY, result.getAction());
        assertEquals("DEAD", result.getBeforeStatus());
        assertEquals("PENDING", result.getAfterStatus());
        assertEquals(1, result.getManualRetrySeq(), "首次人工重试序号应为 1");
        assertEquals("RETRIED", result.getOutcome());

        Map<String, Object> row = loadRow(eventId);
        assertEquals("PENDING", row.get("status"));
        assertNull(row.get("claimed_by"), "重试应释放租约 claimed_by");
        assertNull(row.get("claim_token"), "重试应释放租约 claim_token");
        assertNull(row.get("claim_expires_at"), "重试应释放租约 claim_expires_at");
        Timestamp nextRetry = (Timestamp) row.get("next_retry_at");
        assertTrue(nextRetry.getTime() <= System.currentTimeMillis() + 5_000,
                "重试应重置退避至≈now，使下一轮派发可立即重领");

        Map<String, Object> log = loadRecoveryLog(eventId, "RETRY");
        assertEquals("DEAD", log.get("before_status"));
        assertEquals("PENDING", log.get("after_status"));
        assertEquals(5, log.get("before_retry_count"), "台账应保留恢复前自动重试计数（前后关联）");
        assertEquals(1, log.get("manual_retry_seq"));
        assertEquals("1", log.get("operator_id"));
    }

    /** 用例 6（验收②）：授权跳过 DEAD→SKIPPED（放弃终态），台账记录，且不再被派发器领取。 */
    @Test
    public void test_授权跳过DEAD_转SKIPPED_台账记录且不再被领取() {
        long eventId = insertDead(1L);

        OutboxRecoveryResult result = recoveryService.skipDeadEvent(cmd(eventId, "外部依赖已下线，授权放弃", "1"));

        assertTrue(result.isChanged());
        assertEquals(OutboxRecoveryAction.SKIP, result.getAction());
        assertEquals("DEAD", result.getBeforeStatus());
        assertEquals("SKIPPED", result.getAfterStatus());
        assertEquals("SKIPPED", loadRow(eventId).get("status"));
        assertEquals(1, countRecoveryLog(eventId, "SKIP"));

        // SKIPPED 不再被 CLAIM_SELECT_SQL（status='PENDING'）领取——放弃终态
        OutboxDispatcherService dispatcher = new OutboxDispatcherService(dataSource, transactionManager, List.of());
        List<OutboxEventRecord> claimed = dispatcher.claim("ut-dispatcher", "instance-a", 30, 10);
        assertTrue(claimed.stream().noneMatch(r -> r.getEventId() == eventId),
                "SKIPPED 事件不应被派发器领取");
    }

    /** 用例 7（验收②）：恢复动作双轨审计可追踪——重试/跳过各记一条 SUCCESS 审计，主体与租户正确。 */
    @Test
    public void test_恢复动作双轨审计可追踪() {
        long retryId = insertDead(1L);
        long skipId = insertDead(1L);

        recoveryService.retryDeadEvent(cmd(retryId, "根因已修复", "1"));
        recoveryService.skipDeadEvent(cmd(skipId, "确认放弃", "1"));

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort, times(2)).record(captor.capture());
        List<AuditEventMessage> all = captor.getAllValues();

        AuditEventMessage retried = all.stream()
                .filter(m -> AuditEventTypes.OUTBOX_EVENT_RETRIED.equals(m.getEventType()))
                .findFirst().orElseThrow(() -> new AssertionError("缺少 OUTBOX_EVENT_RETRIED 审计"));
        assertEquals(AuditEventMessage.AuditResult.SUCCESS, retried.getResult());
        assertEquals(AuditEventMessage.ActorType.ADMIN, retried.getActorType());
        assertEquals("1", retried.getActorId());
        assertEquals(String.valueOf(retryId), retried.getBizId());
        assertEquals(Long.valueOf(1L), retried.getTenantId(), "审计租户应来自上下文，不默认 0");

        AuditEventMessage skipped = all.stream()
                .filter(m -> AuditEventTypes.OUTBOX_EVENT_SKIPPED.equals(m.getEventType()))
                .findFirst().orElseThrow(() -> new AssertionError("缺少 OUTBOX_EVENT_SKIPPED 审计"));
        assertEquals(AuditEventMessage.AuditResult.SUCCESS, skipped.getResult());
        assertEquals(String.valueOf(skipId), skipped.getBizId());
    }

    // ========== 用例 8/9/10/11/12：无权重放 / 无限重试 / 修改历史 / 理由 / 状态守卫被拒 ==========

    /** 用例 8（验收③）：无权重放被拒——operatorId 缺失即拒匿名重放，DENIED 独立留痕，状态与 payload 不变。 */
    @Test
    public void test_无权重放被拒_DENIED独立留痕且状态不变() {
        long eventId = insertDead(1L);
        String payloadBefore = (String) loadRow(eventId).get("payload");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> recoveryService.retryDeadEvent(cmd(eventId, "疑似重放", null)));
        assertEquals(OUTBOX_RECOVERY_OPERATOR_REQUIRED.getCode(), ex.getCode());

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        AuditEventMessage denied = captor.getValue();
        assertEquals(AuditEventTypes.ACCESS_DENIED, denied.getEventType());
        assertEquals(AuditEventMessage.AuditResult.DENIED, denied.getResult(), "无权操作应 DENIED 独立留痕");

        assertEquals("DEAD", loadRow(eventId).get("status"), "拒绝后状态不变");
        assertEquals(payloadBefore, loadRow(eventId).get("payload"), "拒绝后 payload 不变");
        assertEquals(0, countRecoveryLog(eventId, "RETRY"), "拒绝后不产生重试台账");
    }

    /** 用例 9（验收③）：无限重试被拒——人工重试超上限（默认 3）拒绝，保持 DEAD，不新增台账。 */
    @Test
    public void test_无限重试被拒_超上限护栏() {
        long eventId = insertDead(1L);
        seedRetryLog(eventId, 3, 1L); // 已达默认 maxManualRetry=3 上限

        ServiceException ex = assertThrows(ServiceException.class,
                () -> recoveryService.retryDeadEvent(cmd(eventId, "再次重试", "1")));
        assertEquals(OUTBOX_RECOVERY_RETRY_LIMIT_EXCEEDED.getCode(), ex.getCode());

        assertEquals("DEAD", loadRow(eventId).get("status"), "超限拒绝应保持 DEAD");
        assertEquals(3, countRecoveryLog(eventId, "RETRY"), "超限拒绝不应新增台账");
    }

    /** 用例 10（验收③）：修改历史被拒——重试/跳过均不改 payload/headers/event_type（历史事实不可掩盖）。 */
    @Test
    public void test_修改历史被拒_payload与headers字节不变() {
        String payload = "{\"fileId\":2048,\"note\":\"原始事实\"}";
        long eventId = insertEvent("DEAD", payload, 1L);

        recoveryService.retryDeadEvent(cmd(eventId, "根因已修复", "1"));
        Map<String, Object> afterRetry = loadRow(eventId);
        assertEquals(payload, afterRetry.get("payload"), "重试绝不改 payload");
        assertEquals("ut-headers", afterRetry.get("headers"), "重试绝不改 headers");
        assertEquals("USER_MESSAGE_SEND", afterRetry.get("event_type"), "重试绝不改 event_type");

        // 复位 DEAD 后跳过，同样不改历史（恢复 API 结构性不提供编辑入口）
        jdbcTemplate.update("UPDATE outbox_event SET status = 'DEAD' WHERE id = ?", eventId);
        recoveryService.skipDeadEvent(cmd(eventId, "确认放弃", "1"));
        Map<String, Object> afterSkip = loadRow(eventId);
        assertEquals(payload, afterSkip.get("payload"), "跳过绝不改 payload");
        assertEquals("ut-headers", afterSkip.get("headers"), "跳过绝不改 headers");
        assertEquals("SKIPPED", afterSkip.get("status"));
    }

    /** 用例 11（验收③）：理由必填——空白/缺失理由被拒，保持 DEAD，不产生台账。 */
    @Test
    public void test_理由必填_空白或缺失被拒() {
        long retryId = insertDead(1L);
        long skipId = insertDead(1L);

        ServiceException retryEx = assertThrows(ServiceException.class,
                () -> recoveryService.retryDeadEvent(cmd(retryId, "   ", "1")));
        assertEquals(OUTBOX_RECOVERY_REASON_REQUIRED.getCode(), retryEx.getCode());

        ServiceException skipEx = assertThrows(ServiceException.class,
                () -> recoveryService.skipDeadEvent(cmd(skipId, null, "1")));
        assertEquals(OUTBOX_RECOVERY_REASON_REQUIRED.getCode(), skipEx.getCode());

        assertEquals("DEAD", loadRow(retryId).get("status"), "理由缺失拒绝应保持 DEAD");
        assertEquals(0, countRecoveryLog(retryId, "RETRY"));
        assertEquals(0, countRecoveryLog(skipId, "SKIP"));
    }

    /** 用例 12（验收③）：非 DEAD 状态恢复被拒——仅 DEAD 可人工恢复，PENDING/DISPATCHED 均拒。 */
    @Test
    public void test_非DEAD状态恢复被拒() {
        long pendingId = insertEvent("PENDING", "{}", 1L);
        long dispatchedId = insertEvent("DISPATCHED", "{}", 1L);

        ServiceException retryEx = assertThrows(ServiceException.class,
                () -> recoveryService.retryDeadEvent(cmd(pendingId, "误操作", "1")));
        assertEquals(OUTBOX_RECOVERY_NOT_DEAD.getCode(), retryEx.getCode());

        ServiceException skipEx = assertThrows(ServiceException.class,
                () -> recoveryService.skipDeadEvent(cmd(dispatchedId, "误操作", "1")));
        assertEquals(OUTBOX_RECOVERY_NOT_DEAD.getCode(), skipEx.getCode());

        assertEquals("PENDING", loadRow(pendingId).get("status"), "拒绝后状态不变");
        assertEquals("DISPATCHED", loadRow(dispatchedId).get("status"), "拒绝后状态不变");
    }

    // ========== 用例 13/14/15：脱敏回查 + 跨租户隔离 ==========

    /** 用例 13：回查详情对敏感 payload 键掩码——敏感值不可见，非敏感键保留（精准脱敏而非全掩）。 */
    @Test
    public void test_回查详情_敏感payload脱敏() {
        String payload = "{\"fileId\":2048,\"password\":\"real-secret-123\",\"apiKey\":\"ak-live-xyz\"}";
        long eventId = insertEvent("DEAD", payload, 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertNotNull(detail.getMaskedPayload());
        assertFalse(detail.getMaskedPayload().contains("real-secret-123"), "敏感 password 值必须掩码");
        assertFalse(detail.getMaskedPayload().contains("ak-live-xyz"), "敏感 apiKey 值必须掩码");
        assertTrue(detail.getMaskedPayload().contains("fileId"), "非敏感键应保留可见（精准脱敏）");
    }

    /** 用例 14：回查详情的异常摘要受控——只回显 errorClass/messageLength + 派生类别，绝不落异常原文。 */
    @Test
    public void test_回查详情_异常受控摘要不落原文() {
        long eventId = insertDead(1L); // last_error = {"errorClass":"IllegalStateException","messageLength":18}

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("IllegalStateException", detail.getErrorClass(), "受控异常类名可回显");
        assertEquals(18, detail.getMessageLength(), "受控异常消息长度可回显");
        assertNotNull(detail.getErrorCategory(), "异常类别应派生（非空），不回退原文");
    }

    /** 用例 15：跨租户隔离——他租户 DEAD 事件不可见、不可回查、不可恢复（租户 fail-closed）。 */
    @Test
    public void test_跨租户隔离_不可见不可恢复() {
        long tenantAEvent = insertDead(7L); // 租户 A=7

        TenantContextHolder.setTenantId(7L);
        assertTrue(recoveryService.pageDeadEvents(1, 20).getList().stream()
                        .anyMatch(d -> d.getEventId() == tenantAEvent),
                "租户 A 应能在分页中看到自己的 DEAD 事件");
        assertNotNull(recoveryService.getRecoveryDetail(tenantAEvent));

        try {
            TenantContextHolder.setTenantId(8L); // 切换到租户 B=8
            assertTrue(recoveryService.pageDeadEvents(1, 20).getList().stream()
                            .noneMatch(d -> d.getEventId() == tenantAEvent),
                    "租户 B 不应看到租户 A 的事件");

            ServiceException detailEx = assertThrows(ServiceException.class,
                    () -> recoveryService.getRecoveryDetail(tenantAEvent));
            assertEquals(OUTBOX_EVENT_NOT_FOUND.getCode(), detailEx.getCode());

            ServiceException retryEx = assertThrows(ServiceException.class,
                    () -> recoveryService.retryDeadEvent(cmd(tenantAEvent, "越权重试", "1")));
            assertEquals(OUTBOX_EVENT_NOT_FOUND.getCode(), retryEx.getCode());
        } finally {
            TenantContextHolder.setTenantId(1L);
        }
    }

    // ========== 用例 16-19：异常摘要脱敏补强（P2，codex r0） ==========

    /**
     * 用例 16（P2）：裸文本 {@code last_error} 含敏感内容不得回显——非受控常量裸值一律降级 {@code UNPARSEABLE_ERROR}，
     * 详情接口的 errorCategory/errorClass 均不含敏感原文。
     */
    @Test
    public void test_回查详情_裸文本敏感内容降级不回显() {
        long eventId = insertEventWithLastError("DEAD", "{}", "timeout password=real-secret-123", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "非受控常量裸值必须降级为 UNPARSEABLE_ERROR");
        assertNull(detail.getErrorClass(), "裸值不得被当作异常类名回显");
        assertFalse(String.valueOf(detail.getErrorCategory()).contains("real-secret-123"), "敏感内容绝不可回显");
    }

    /** 用例 17（P2 回归护栏）：合法受控常量裸值仍直显（派发器写入的 {@code NO_SINK_SUPPORTS_EVENT_TYPE}），不被过度降级。 */
    @Test
    public void test_回查详情_合法受控常量裸值直显() {
        long eventId = insertEventWithLastError("DEAD", "{}", "NO_SINK_SUPPORTS_EVENT_TYPE", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("NO_SINK_SUPPORTS_EVENT_TYPE", detail.getErrorCategory(), "受控常量码应直显（白名单放行）");
    }

    /** 用例 18（P2）：JSON 摘要的 {@code errorClass} 非类名格式（含空格/敏感串）时降级，畸形值不原样回显，合法 messageLength 仍保留。 */
    @Test
    public void test_回查详情_JSON异常类名格式非法降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"oops password=real-secret-123\",\"messageLength\":5}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "非法类名格式必须降级为 UNPARSEABLE_ERROR");
        assertNull(detail.getErrorClass(), "非法类名不得回显");
        assertEquals(5, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /** 用例 19（P2）：恢复成功审计的 detail 与详情接口一致脱敏——errorCategory 降级，绝不含敏感裸文本原文。 */
    @Test
    public void test_恢复成功审计detail不含敏感裸文本() {
        long eventId = insertEventWithLastError("DEAD", "{}", "timeout password=real-secret-123", 1L);

        recoveryService.retryDeadEvent(cmd(eventId, "根因已修复", "1"));

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        Map<String, Object> detail = captor.getValue().getDetail();
        assertEquals("UNPARSEABLE_ERROR", detail.get("errorCategory"), "审计 detail 的异常类别须降级");
        assertFalse(String.valueOf(detail).contains("real-secret-123"), "审计 detail 绝不含敏感原文");
    }

    // ========== 用例 20：并发恢复「状态往返」交错（P1，codex r0） ==========

    /**
     * 用例 20（验收③/P1，codex r0）：并发恢复「DEAD→PENDING→DEAD 状态往返」交错下，人工重试上限、
     * {@code manual_retry_seq} 唯一性、以及每次成功恢复的审计幂等键一一对应，均不得破裂。
     *
     * <p>确定性复现 codex r0 P1 交错（不靠 sleep 猜测，用栅栏对齐 + 独立编排线程注入往返）：已两次 RETRY
     * （序号 1、2）、上限 3；A、B 并发恢复同一 DEAD 事件——
     * <ol>
     *   <li>{@code getMaxManualRetry()} 桩作<b>非对称栅栏</b>：先到达者(A)等后到达者(B)也读到<b>过期计数=2</b>后再提交，
     *       锁定「双方都算出序号 3」的交错；B 读到过期计数后，等编排线程注入往返再执行 UPDATE；</li>
     *   <li>编排线程（独立连接，忠实模拟派发器为独立执行体）：A 提交（状态转 PENDING、序号 3 落台账）后，
     *       模拟派发器领取失败使事件 DEAD→PENDING→DEAD（{@code retry_count} 递增）；</li>
     *   <li><b>缺陷实现</b>：B 的 {@code UPDATE ... WHERE status='DEAD'} 因往返再次命中，写入<b>第四条</b> RETRY 台账，
     *       却仍用过期序号 3（与 A 碰撞），并以同一幂等键 {@code eventId:RETRY:3} 触发 {@code JdbcAuditPort} 去重丢失；</li>
     *   <li><b>修复实现</b>（{@code SELECT ... FOR UPDATE}）：B 阻塞在载入事件处，A 提交后 B 读到<b>最新</b>状态与计数，
     *       要么因状态非 DEAD 被拒，要么因序号超限被拒——绝不产生碰撞序号或超限台账。</li>
     * </ol>
     *
     * <p>FOR UPDATE 下 A 持锁等待被限制在 {@link #A_LOCK_HOLD_MS}（&lt; H2 默认锁超时 1000ms），B 只等 A 的短事务、
     * 不会锁超时；即便极端拥塞下 B 锁超时，B 亦被拒（不写台账），三条不变量断言仍成立。
     */
    @Test
    public void test_并发恢复状态往返_上限序号审计不破裂() throws Exception {
        long eventId = insertDead(1L);   // status=DEAD, retry_count=5（已达自动重试上限）
        seedRetryLog(eventId, 2, 1L);    // 已两次人工重试（序号 1、2），逼近上限 3

        AtomicInteger arrivals = new AtomicInteger();
        CountDownLatch bReadStaleCount = new CountDownLatch(1);
        CountDownLatch roundTripDone = new CountDownLatch(1);
        AtomicReference<Throwable> orchestratorError = new AtomicReference<>();
        AtomicInteger roundTripHits = new AtomicInteger();
        // JOB-004-P2-2：记录 A 栅栏 await 的实际结果——不再忽略返回值（原实现无法保证「实现被移除时必然 RED」）
        AtomicBoolean aWaitInterleaved = new AtomicBoolean(false);
        when(properties.getMaxManualRetry()).thenAnswer(inv -> {
            if (arrivals.incrementAndGet() == 1) {
                // A：等 B 也读到过期计数后再提交（FOR UPDATE 下 B 阻塞于载入、永不到达 → 短超时放行，< H2 锁超时）
                boolean interleaved = bReadStaleCount.await(A_LOCK_HOLD_MS, TimeUnit.MILLISECONDS);
                aWaitInterleaved.set(interleaved);
            } else {
                // B：已持过期计数=2；放行 A 提交，再等编排线程注入状态往返后 UPDATE（复现交错第 3-4 步）
                bReadStaleCount.countDown();
                if (!roundTripDone.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("编排线程未在超时内完成状态往返注入");
                }
            }
            return MAX_MANUAL_RETRY;
        });

        Thread orchestrator = new Thread(() -> {
            try {
                awaitCommittedRetrySeq(eventId, MAX_MANUAL_RETRY);   // 等 A 提交序号 3 的恢复台账
                roundTripHits.set(jdbcTemplate.update("UPDATE outbox_event SET status = 'DEAD', retry_count = retry_count + 1, "
                        + "claimed_by = NULL, claim_token = NULL, claim_expires_at = NULL "
                        + "WHERE id = ? AND status = 'PENDING'", eventId));
            } catch (Throwable e) {
                orchestratorError.set(e);
            } finally {
                roundTripDone.countDown();
            }
        }, "job004-roundtrip-orchestrator");
        orchestrator.start();

        Throwable[] results = runConcurrently(2, i ->
                recoveryService.retryDeadEvent(cmd(eventId, "并发恢复-" + i, String.valueOf(100 + i))));
        orchestrator.join(TimeUnit.SECONDS.toMillis(40));
        if (orchestratorError.get() != null) {
            fail("编排线程注入状态往返失败：" + orchestratorError.get());
        }

        // 断言 0：往返确实发生（编排线程 UPDATE 命中 1 行）——否则未真正复现「DEAD→PENDING→DEAD」交错，视为非确定性假绿
        assertEquals(1, roundTripHits.get(),
                "编排线程的 DEAD→PENDING→DEAD 往返 UPDATE 必须命中 1 行，否则未真正复现并发交错（非确定性假绿）");
        // 断言 0b（JOB-004-P2-2）：A 的栅栏 await 必须以超时收场（返回 false）——修复实现（FOR UPDATE 行锁）下
        // B 被阻塞在「载入事件」处，不可能在 A 持锁期间到达计数栅栏；await 提前返回（true）= B 在 A 提交前读到了
        // 过期计数 = 行锁串行化被破坏（缺陷实现回归的直接信号；250ms 窗口 ≫ 缺陷实现下 B 到达栅栏的毫秒级耗时）。
        assertFalse(aWaitInterleaved.get(),
                "A 的栅栏 await 必须超时返回（B 应被 FOR UPDATE 阻塞在载入处，无法在 A 持锁期间先读计数）；"
                        + "await 提前返回说明行锁串行化被破坏（缺陷实现回归）");
        // 断言 1：恰好一个线程成功、另一个被业务规则拒绝；拒绝码必须是 NOT_DEAD 或 RETRY_LIMIT_EXCEEDED
        //         （不得把锁超时/基础设施异常当作「安全拒绝」——那意味着交错未按设计发生）
        int successes = 0;
        List<Integer> rejectedCodes = new ArrayList<>();
        for (Throwable t : results) {
            if (t == null) {
                successes++;
            } else if (t instanceof ServiceException) {
                rejectedCodes.add(((ServiceException) t).getCode());
            } else {
                fail("并发恢复出现非业务异常（不得当作安全拒绝，可能是锁超时/交错失败）：" + t);
            }
        }
        assertEquals(1, successes,
                "修复实现（FOR UPDATE 串行化）下恰好一个恢复成功；线程结果=" + describe(results));
        assertEquals(1, rejectedCodes.size(),
                "恰好一个线程被拒绝；线程结果=" + describe(results));
        assertTrue(rejectedCodes.contains(OUTBOX_RECOVERY_NOT_DEAD.getCode())
                        || rejectedCodes.contains(OUTBOX_RECOVERY_RETRY_LIMIT_EXCEEDED.getCode()),
                "被拒线程必须命中业务码 NOT_DEAD（读到往返后非 DEAD）或 RETRY_LIMIT_EXCEEDED（读到 fresh 计数超限），"
                        + "而非锁超时/基础设施异常；实际拒绝码=" + rejectedCodes);
        // 断言 2：人工重试台账恰为种子的 [1,2] + 成功线程的 [3]（缺陷实现会写入第 4 条、序号碰撞为 [1,2,3,3]）
        List<Integer> retrySeqs = jdbcTemplate.queryForList("SELECT manual_retry_seq FROM outbox_recovery_log "
                + "WHERE event_id = ? AND action = 'RETRY' ORDER BY id", Integer.class, eventId);
        assertEquals(List.of(1, 2, MAX_MANUAL_RETRY), retrySeqs,
                "并发状态往返下人工重试台账必须恰为 [1,2,3]（不突破上限、序号唯一、无碰撞）；实际=" + retrySeqs
                        + "，线程结果=" + describe(results));
        // 断言 3：仅成功线程写审计，幂等键恰为 eventId:RETRY:3（缺陷实现下 A、B 同键碰撞、审计被去重丢失一条）
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort, atLeastOnce()).record(captor.capture());
        List<String> retriedKeys = captor.getAllValues().stream()
                .filter(m -> AuditEventTypes.OUTBOX_EVENT_RETRIED.equals(m.getEventType()))
                .map(AuditEventMessage::getIdempotencyKey).collect(Collectors.toList());
        assertEquals(List.of(eventId + ":RETRY:" + MAX_MANUAL_RETRY), retriedKeys,
                "仅一个成功恢复、且幂等键唯一为 eventId:RETRY:3（无碰撞、无去重丢失）；实际=" + retriedKeys);
    }

    // ========== 用例 21-25：异常摘要脱敏白名单收严（P2-1，codex r1） ==========

    /**
     * 用例 21（P2-1）：全大写敏感裸值不得被当作内部常量回显——仅明确常量集合（派发器写入的固定码）可直显，
     * {@code PASSWORD_REAL_SECRET_123} 虽为全大写标识符形态，但不在白名单集合，必须降级。
     */
    @Test
    public void test_回查详情_全大写敏感裸值不当常量回显() {
        long eventId = insertEventWithLastError("DEAD", "{}", "PASSWORD_REAL_SECRET_123", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "非白名单集合的裸值必须降级");
        assertNull(detail.getErrorClass(), "裸值不得被当作异常类名回显");
        assertFalse(String.valueOf(detail.getErrorCategory()).contains("PASSWORD_REAL_SECRET_123"), "敏感裸值绝不可回显");
    }

    /** 用例 22（P2-1）：JSON {@code errorClass} 非字符串类型（布尔/数字）时降级——字段类型检查，合法 messageLength 仍保留。 */
    @Test
    public void test_回查详情_JSON非字符串errorClass降级() {
        long eventId = insertEventWithLastError("DEAD", "{}", "{\"errorClass\":true,\"messageLength\":7}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "errorClass 非字符串必须降级");
        assertNull(detail.getErrorClass(), "非字符串 errorClass 不得回显");
        assertEquals(7, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /**
     * 用例 23（P2-1）：JSON {@code errorClass} 为标识符形态但不符合受控异常类名约定（无 Exception/Error/Throwable 后缀，
     * 如 {@code password_real_secret_123}、{@code a..b}）时降级——字符形态不足以证明值来自受控异常类型。
     */
    @Test
    public void test_回查详情_JSON标识符形态敏感串降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"password_real_secret_123\",\"messageLength\":9}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "标识符形态但非异常类名约定必须降级");
        assertNull(detail.getErrorClass(), "非受控异常类名不得回显");
        assertEquals(9, detail.getMessageLength(), "合法 messageLength 仍保留");
        assertFalse(String.valueOf(detail.getErrorClass()).contains("password_real_secret_123"), "敏感串绝不可回显");
    }

    /** 用例 24（P2-1 回归护栏）：合法受控异常类名（含全限定名）仍直显，不被过度降级。 */
    @Test
    public void test_回查详情_合法受控异常类名保留() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"java.lang.RuntimeException\",\"messageLength\":10}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("java.lang.RuntimeException", detail.getErrorClass(), "受控异常类名（全限定）应直显");
        assertEquals("java.lang.RuntimeException", detail.getErrorCategory(), "类别以受控类名呈现");
        assertEquals(10, detail.getMessageLength());
    }

    /**
     * 用例 26（P2-1）：畸形限定名 + Throwable 后缀（连续点 {@code a..bException}）仍须降级——
     * 字符形态白名单若仅整体匹配会放行非法 Java 限定名，逐段验证方可拦截（codex r2 P2-1）。
     */
    @Test
    public void test_回查详情_畸形限定名连续点降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"a..bException\",\"messageLength\":6}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "连续点畸形限定名即便含后缀也须降级");
        assertNull(detail.getErrorClass(), "非法 Java 限定名不得回显");
        assertEquals(6, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /**
     * 用例 27（P2-1）：畸形限定名 + Throwable 后缀（数字开头段 {@code a.1Exception}）仍须降级——
     * 逐段验证要求每段为合法 Java 标识符（字母/下划线开头），拦截数字开头段（codex r2 P2-1）。
     */
    @Test
    public void test_回查详情_畸形限定名数字开头段降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"a.1Exception\",\"messageLength\":6}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "数字开头段畸形限定名即便含后缀也须降级");
        assertNull(detail.getErrorClass(), "非法 Java 限定名不得回显");
        assertEquals(6, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /** 用例 25（P2-1）：审计出口与响应出口一致脱敏——敏感类名在恢复成功审计 detail 中同样降级，绝不回显。 */
    @Test
    public void test_恢复成功审计detail敏感类名降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"password_real_secret_123\",\"messageLength\":9}", 1L);

        recoveryService.retryDeadEvent(cmd(eventId, "根因已修复", "1"));

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        Map<String, Object> detail = captor.getValue().getDetail();
        assertEquals("UNPARSEABLE_ERROR", detail.get("errorCategory"), "审计 detail 的异常类别须降级");
        assertFalse(String.valueOf(detail).contains("password_real_secret_123"), "审计 detail 绝不含敏感类名原文");
    }

    /**
     * 用例 28（JOB-004-P2-1 收敛）：「合法 Java 限定名 + Throwable 后缀」但嵌入凭据词根的形态
     * （如 {@code password_real_secret_123Exception}）必须降级——派发器 {@code describeThrowable} 恒写真实异常
     * SimpleName，真实异常类名不会嵌入凭据词根；该形状几乎必然是异常消息/凭据被误格式化为类名，
     * 直显即泄露敏感内容形态（codex r2 登记的极窄来源信任残留收敛）。
     */
    @Test
    public void test_回查详情_凭据词根异常名降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"password_real_secret_123Exception\",\"messageLength\":18}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(),
                "合法限定名+Throwable 后缀但嵌入凭据词根必须降级（来源不可信）");
        assertNull(detail.getErrorClass(), "凭据词根异常名不得回显");
        assertEquals(18, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /** 用例 29（JOB-004-P2-1）：凭据词根出现在<b>包段</b>（非 simpleName）同样降级——逐段任一命中即不可信。 */
    @Test
    public void test_回查详情_包段凭据词根降级() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"com.evil.password.hunterException\",\"messageLength\":4}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("UNPARSEABLE_ERROR", detail.getErrorCategory(), "包段嵌入凭据词根同样须降级");
        assertNull(detail.getErrorClass(), "凭据词根限定名不得回显");
        assertEquals(4, detail.getMessageLength(), "合法 messageLength 仍保留");
    }

    /**
     * 用例 30（JOB-004-P2-1 过度拦截护栏）：含 {@code Token} 等业务常见词根的<b>真实异常类名</b>
     * （如 {@code TokenExpiredException}）不受凭据词根过滤影响仍直显——词根集合刻意不收
     * {@code token}（Token*Exception 是常见真实异常族，误杀会损失运维定位信息；降级方向安全但非必要）。
     */
    @Test
    public void test_回查详情_业务常见词根异常名不受影响() {
        long eventId = insertEventWithLastError("DEAD", "{}",
                "{\"errorClass\":\"java.lang.TokenExpiredException\",\"messageLength\":7}", 1L);

        OutboxEventRecoveryDetail detail = recoveryService.getRecoveryDetail(eventId);

        assertEquals("java.lang.TokenExpiredException", detail.getErrorClass(),
                "不含凭据词根的受控异常类名应直显（防过度拦截）");
        assertEquals("java.lang.TokenExpiredException", detail.getErrorCategory());
        assertEquals(7, detail.getMessageLength());
    }

    // ========== 夹具与并发助手（用例 16-25） ==========

    /** 种子一条指定 {@code last_error} 的事件（P2 脱敏用例：裸文本 / 畸形 JSON / 合法常量）。 */
    private long insertEventWithLastError(String status, String payload, String lastError, long tenantId) {
        jdbcTemplate.update("INSERT INTO outbox_event (event_type, biz_type, biz_id, payload, headers, status, "
                        + "retry_count, next_retry_at, last_error, tenant_id, actor_type, actor_id, trace_id) "
                        + "VALUES ('USER_MESSAGE_SEND', 'infra_file', '2048', ?, 'ut-headers', ?, ?, ?, ?, ?, "
                        + "'SYSTEM', 'ut-worker', 'trace-ut')",
                payload, status, "DEAD".equals(status) ? 5 : 0,
                new Timestamp(System.currentTimeMillis() - 1000), lastError, tenantId);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM outbox_event WHERE tenant_id = ? ORDER BY id DESC LIMIT 1", Long.class, tenantId);
        return id == null ? -1 : id;
    }

    /** 编排线程轮询：直到出现序号 ≥ {@code expectedSeq} 的 RETRY 台账（即先到达线程的恢复已提交），超时抛出以显性失败。 */
    private void awaitCommittedRetrySeq(long eventId, int expectedSeq) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            Integer maxSeq = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(manual_retry_seq), 0) "
                    + "FROM outbox_recovery_log WHERE event_id = ? AND action = 'RETRY'", Integer.class, eventId);
            if (maxSeq != null && maxSeq >= expectedSeq) {
                return;
            }
            Thread.sleep(5);
        }
        throw new IllegalStateException("超时未观测到先到达线程提交序号 ≥ " + expectedSeq + " 的人工重试台账");
    }

    /** 并发执行 {@code threads} 份恢复动作（startGate 同时放行；每线程独立租户上下文）；返回逐线程异常（null=成功）。 */
    private Throwable[] runConcurrently(int threads, IntConsumer action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            Throwable[] results = new Throwable[threads];
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    TenantContextHolder.setTenantId(1L);   // 线程池线程不继承 TTL，须显式设置租户上下文
                    try {
                        startGate.await();
                        action.accept(index);
                    } catch (Throwable ex) {
                        results[index] = ex;
                    } finally {
                        TenantContextHolder.clear();
                    }
                }));
            }
            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /** 逐线程结果的可读描述（断言失败时定位交错产物）。 */
    private static String describe(Throwable[] results) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(i).append('=');
            if (results[i] == null) {
                sb.append("SUCCESS");
            } else if (results[i] instanceof ServiceException) {
                sb.append("SE(").append(((ServiceException) results[i]).getCode()).append(')');
            } else {
                sb.append(results[i].getClass().getSimpleName()).append(':').append(results[i].getMessage());
            }
        }
        return sb.append(']').toString();
    }

}
