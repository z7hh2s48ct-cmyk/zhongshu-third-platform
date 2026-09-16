package cn.zszj.module.system.service.notify.channel;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyChannelSendMapper;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * {@link NotifyChannelSendService} 单元测试（ZS-MSG-004，H2）。
 *
 * <p>覆盖 27 个用例：台账创建(入管道/明确阻断/事务回滚) / 提交三态(受理/明确拒绝/未知) / 技术异常转 UNKNOWN
 * / UNKNOWN 先回查(送达推进/确认未发出才重发且幂等键一致/仍未知退避) / 受理-送达-失败重投不重复提交
 * / 提交时发送器缺失明确阻断 / 回执(送达/失败/乱序权威/重复吸收计数/未知拒绝/流水号错配/终态矛盾)
 * / 人工重试(复位+新事件+留痕+受理送达拒绝+重发同一幂等键) / 对账(送达推进/未发出仅证据/仍未知/终态不适用)
 * / 租户强制 / 台账扫描。
 *
 * <p>循 NotifyDispatcherTest 先例：H2 + BaseDbUnitTest + @Import 显式装配（含 JdbcReliableEventPort 与
 * 可编程 {@link TestSmsChannelSender}——Mock 只存在于测试装配，不进入生产上下文，Mock 不作为真实渠道验收）。
 * 创建走 MANDATORY（transactionTemplate 包裹）；投递/回执/对账走服务内部短事务，测试直接调用（无外层事务）。
 */
@Import({NotifyChannelSendServiceImpl.class, NotifyChannelSenderRegistry.class, JdbcReliableEventPort.class,
        NotifyChannelSendServiceTest.TestSmsChannelSender.class})
public class NotifyChannelSendServiceTest extends BaseDbUnitTest {

    @Resource
    private NotifyChannelSendService channelSendService;
    @Resource
    private NotifyChannelSendMapper channelSendMapper;
    @Resource
    private TestSmsChannelSender testSmsSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    private static final Long TENANT_ID = 1L;
    private static final Long RECIPIENT_ID = 100L;
    private static final String MOBILE = "13800138000";

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        TenantContextHolder.setTenantId(TENANT_ID);
        testSmsSender.reset();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        jdbcTemplate.execute("DELETE FROM system_notify_channel_send");
        jdbcTemplate.execute("DELETE FROM outbox_event");
    }

    // ========== TestSmsChannelSender：可编程 Mock（脚本队列弹出预置结果/异常） ==========

    public static class TestSmsChannelSender implements NotifyChannelSender {

        final Deque<Object> submitScript = new ArrayDeque<>();
        final Deque<Object> queryScript = new ArrayDeque<>();
        int submitCalls;
        final List<String> submittedMessageIds = new ArrayList<>();

        void reset() {
            submitScript.clear();
            queryScript.clear();
            submitCalls = 0;
            submittedMessageIds.clear();
        }

        @Override
        public NotifyChannel channel() {
            return NotifyChannel.SMS;
        }

        @Override
        public ChannelSubmitResult submit(NotifyChannelSendDO record) throws Exception {
            submitCalls++;
            submittedMessageIds.add(record.getChannelMessageId());
            return next(submitScript);
        }

        @Override
        public ChannelQueryResult queryByReceiptKey(String channelMessageId) throws Exception {
            return next(queryScript);
        }

        @SuppressWarnings("unchecked")
        private static <T> T next(Deque<Object> script) throws Exception {
            Object next = script.poll();
            if (next == null) {
                throw new AssertionError("Mock 无预置结果（不应发生的外部调用）");
            }
            if (next instanceof Exception) {
                throw (Exception) next;
            }
            return (T) next;
        }

    }

    // ========== 台账创建（派发事务内） ==========

    @Test
    void test_创建台账_有联系方式_PENDING加Outbox事件同事务落库() {
        NotifyChannelSendDO record = createPendingRecord("EVT_A1");
        assertThat(record.getId()).isNotNull();
        assertThat(record.getStatus()).isEqualTo("PENDING");
        assertThat(record.getChannelMessageId()).startsWith("NC-");
        assertThat(record.getRecipientContact()).isEqualTo(MOBILE);
        assertThat(record.getOutboxEventId()).isNotNull();
        // 同事务：Outbox 事件已落库且类型正确
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE id = ? AND event_type = 'NOTIFY_CHANNEL_SEND'",
                Integer.class, record.getOutboxEventId());
        assertThat(events).isEqualTo(1);
        // attempt/receipt/manual 计数从零开始
        assertThat(record.getAttemptCount()).isZero();
        assertThat(record.getReceiptCount()).isZero();
        assertThat(record.getManualRetryCount()).isZero();
    }

    @Test
    void test_创建台账_缺联系方式_FAILED明确阻断_不入管道() {
        NotifyChannelSendDO record = transactionTemplate.execute(s -> channelSendService.createFromDispatch(
                command("EVT_A2"), NotifyRecipient.admin(RECIPIENT_ID), NotifyChannel.SMS,
                "", "内容", 9001L));
        assertThat(record.getStatus()).isEqualTo("FAILED");
        assertThat(record.getFailedCode()).isEqualTo("RECIPIENT_CONTACT_MISSING");
        assertThat(record.getOutboxEventId()).isNull();
        // 不入投递管道：无 Outbox 事件（明确阻断可查询，不静默丢弃）
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void test_创建台账_派发事务回滚_台账与事件一并回滚() {
        transactionTemplate.execute(status -> {
            channelSendService.createFromDispatch(command("EVT_A3"), NotifyRecipient.admin(RECIPIENT_ID),
                    NotifyChannel.SMS, MOBILE, "内容", 9002L);
            status.setRollbackOnly();
            return null;
        });
        assertThat(countChannelSends()).isZero();
        assertThat(countOutboxEvents()).isZero();
    }

    // ========== 提交三态与先回查再重发 ==========

    @Test
    void test_提交受理_PENDING转ACCEPTED() {
        NotifyChannelSendDO record = createPendingRecord("EVT_B1");
        testSmsSender.submitScript.add(ChannelSubmitResult.accepted("SER-1"));
        channelSendService.processOutboxDelivery(record.getId());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("ACCEPTED");
        assertThat(after.getChannelSerialNo()).isEqualTo("SER-1");
        assertThat(after.getAttemptCount()).isEqualTo(1);
        assertThat(after.getAcceptedAt()).isNotNull();
    }

    @Test
    void test_提交明确拒绝_PENDING转FAILED() {
        NotifyChannelSendDO record = createPendingRecord("EVT_B2");
        testSmsSender.submitScript.add(ChannelSubmitResult.rejected("INVALID_MOBILE", "号码无效"));
        channelSendService.processOutboxDelivery(record.getId());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailedCode()).isEqualTo("INVALID_MOBILE");
        assertThat(after.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void test_提交结果未知_PENDING转UNKNOWN_抛可重试() {
        NotifyChannelSendDO record = createPendingRecord("EVT_B3");
        testSmsSender.submitScript.add(ChannelSubmitResult.unknown("TIMEOUT", "网关超时"));
        assertThatThrownBy(() -> channelSendService.processOutboxDelivery(record.getId()))
                .isInstanceOf(NotifyChannelSendRetryableException.class);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("UNKNOWN");
        assertThat(after.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void test_提交技术异常_PENDING转UNKNOWN_只留受控描述() {
        NotifyChannelSendDO record = createPendingRecord("EVT_B4");
        testSmsSender.submitScript.add(new RuntimeException("boom-with-mobile-" + MOBILE));
        assertThatThrownBy(() -> channelSendService.processOutboxDelivery(record.getId()))
                .isInstanceOf(NotifyChannelSendRetryableException.class);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("UNKNOWN");
        // 脱敏：last_error 只存受控描述（异常类别 JSON），不落异常原文
        assertThat(after.getLastError()).contains("RuntimeException");
        assertThat(after.getLastError()).doesNotContain(MOBILE);
    }

    @Test
    void test_UNKNOWN重投_回查送达_不重复提交() {
        NotifyChannelSendDO record = createUnknownRecord("EVT_B5");
        testSmsSender.queryScript.add(ChannelQueryResult.delivered("渠道状态:DELIVERED"));
        channelSendService.processOutboxDelivery(record.getId());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("DELIVERED");
        assertThat(after.getDeliveredAt()).isNotNull();
        assertThat(after.getLastQueryResult()).isEqualTo("DELIVERED");
        // 先回查：未重发（submit 次数不变=1，只发生在转 UNKNOWN 那次）
        assertThat(testSmsSender.submitCalls).isEqualTo(1);
    }

    @Test
    void test_UNKNOWN重投_回查确认未发出_才重发且幂等键一致() {
        NotifyChannelSendDO record = createUnknownRecord("EVT_B6");
        testSmsSender.queryScript.add(ChannelQueryResult.notSent("渠道无记录"));
        testSmsSender.submitScript.add(ChannelSubmitResult.accepted("SER-2"));
        assertDoesNotThrow(() -> channelSendService.processOutboxDelivery(record.getId()));
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("ACCEPTED");
        // 先回查再重发：attempt=2（初次 1 + 重发 1）；两次提交携带同一渠道幂等键（渠道侧可去重）
        assertThat(after.getAttemptCount()).isEqualTo(2);
        assertThat(testSmsSender.submitCalls).isEqualTo(2);
        assertThat(testSmsSender.submittedMessageIds.get(0))
                .isEqualTo(testSmsSender.submittedMessageIds.get(1))
                .isEqualTo(record.getChannelMessageId());
    }

    @Test
    void test_UNKNOWN重投_回查仍未知_退避重查() {
        NotifyChannelSendDO record = createUnknownRecord("EVT_B7");
        testSmsSender.queryScript.add(ChannelQueryResult.unknown("渠道仍无结论"));
        assertThatThrownBy(() -> channelSendService.processOutboxDelivery(record.getId()))
                .isInstanceOf(NotifyChannelSendRetryableException.class);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("UNKNOWN");
        assertThat(after.getLastQueryResult()).isEqualTo("UNKNOWN");
        assertThat(testSmsSender.submitCalls).isEqualTo(1);
    }

    @Test
    void test_ACCEPTED重投_吸收不重复提交() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_B8");
        channelSendService.processOutboxDelivery(record.getId());
        assertThat(channelSendService.getNotifyChannelSend(record.getId()).getStatus()).isEqualTo("ACCEPTED");
        assertThat(testSmsSender.submitCalls).isEqualTo(1); // 仅创建后的首次（本用例无新提交）
        assertThat(testSmsSender.submittedMessageIds).hasSize(1);
    }

    @Test
    void test_送达与失败重投_吸收不动作() {
        NotifyChannelSendDO delivered = createAcceptedRecord("EVT_B9");
        channelSendService.applyReceipt(receipt(delivered.getChannelMessageId(), true, null));
        channelSendService.processOutboxDelivery(delivered.getId());
        assertThat(channelSendService.getNotifyChannelSend(delivered.getId()).getStatus()).isEqualTo("DELIVERED");

        NotifyChannelSendDO failed = createFailedRecord("EVT_B10");
        channelSendService.processOutboxDelivery(failed.getId());
        assertThat(channelSendService.getNotifyChannelSend(failed.getId()).getStatus()).isEqualTo("FAILED");
        // 两笔记录重投全程无新的外部提交（2 = 两笔记录各自创建路径的那一次提交）
        assertThat(testSmsSender.submitCalls).isEqualTo(2);
    }

    @Test
    void test_提交时发送器缺失_明确阻断FAILED() {
        // EMAIL 渠道无发送器实现（B05 生产常态）：提交时明确阻断，不静默丢弃
        NotifyChannelSendDO record = NotifyChannelSendDO.builder()
                .tenantId(TENANT_ID).sendLogId(6001L).eventId("EVT_B11").channel("EMAIL")
                .recipientType("ADMIN").recipientId(RECIPIENT_ID).templateCode("T")
                .channelMessageId("NC-TEST-EMAIL").status("PENDING")
                .attemptCount(0).receiptCount(0).manualRetryCount(0)
                .actorType("SYSTEM").build();
        channelSendMapper.insert(record);
        channelSendService.processOutboxDelivery(record.getId());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailedCode()).isEqualTo("CHANNEL_NOT_CONFIGURED");
    }

    // ========== 回执：校验、权威推进、重复吸收 ==========

    @Test
    void test_送达回执_ACCEPTED推进DELIVERED() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_C1");
        NotifyChannelReceiptResult result =
                channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.APPLIED);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("DELIVERED");
        assertThat(after.getReceiptCount()).isEqualTo(1);
        assertThat(after.getDeliveredAt()).isNotNull();
    }

    @Test
    void test_失败回执_ACCEPTED推进FAILED() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_C2");
        channelSendService.applyReceipt(
                NotifyChannelReceiptCmd.builder().channel("SMS").channelMessageId(record.getChannelMessageId())
                        .delivered(false).errorCode("USER_REFUSED").errorMsg("用户拒收").build());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");
        assertThat(after.getFailedCode()).isEqualTo("USER_REFUSED");
    }

    @Test
    void test_乱序送达回执_PENDING直接推进_回执权威() {
        // 回执先于提交确认到达：渠道已有该幂等键的发送事实 → 权威推进 DELIVERED（乱序回执不丢事实）
        NotifyChannelSendDO record = createPendingRecord("EVT_C3");
        NotifyChannelReceiptResult result =
                channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.APPLIED);
        assertThat(channelSendService.getNotifyChannelSend(record.getId()).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void test_重复送达回执_DUPLICATE_计数可追踪() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_C4");
        channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        NotifyChannelReceiptResult again = channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        assertThat(again.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.DUPLICATE);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("DELIVERED"); // 状态不回退
        assertThat(after.getReceiptCount()).isEqualTo(2);      // 重复回执可追踪
    }

    @Test
    void test_未知幂等键回执_拒绝() {
        NotifyChannelReceiptResult result = channelSendService.applyReceipt(receipt("NC-NOT-EXIST", true, null));
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.REJECTED_UNKNOWN_RECEIPT);
        assertThat(countChannelSends()).isZero();
    }

    @Test
    void test_流水号错配回执_拒绝() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_C5"); // 受理流水号 SER-1
        NotifyChannelReceiptResult result = channelSendService.applyReceipt(
                NotifyChannelReceiptCmd.builder().channel("SMS").channelMessageId(record.getChannelMessageId())
                        .channelSerialNo("SER-WRONG").delivered(true).build());
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.REJECTED_SERIAL_MISMATCH);
        assertThat(channelSendService.getNotifyChannelSend(record.getId()).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void test_终态矛盾回执_CONTRADICTION() {
        NotifyChannelSendDO record = createFailedRecord("EVT_C6");
        NotifyChannelReceiptResult result = channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReceiptResult.Outcome.CONTRADICTION);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("FAILED");   // 不推进，留人工核实
        assertThat(after.getReceiptCount()).isEqualTo(1);    // 但事实已登记
    }

    // ========== 人工重试 ==========

    @Test
    void test_人工重试_FAILED复位PENDING_新事件留痕() {
        NotifyChannelSendDO record = createFailedRecord("EVT_D1");
        Long oldEventId = record.getOutboxEventId();
        NotifyChannelSendDO retried = channelSendService.manualRetry(record.getId(), "ADMIN", "9", "客户要求重发");
        assertThat(retried.getStatus()).isEqualTo("PENDING");
        assertThat(retried.getManualRetryCount()).isEqualTo(1);
        assertThat(retried.getLastRetryActorType()).isEqualTo("ADMIN");
        assertThat(retried.getLastRetryActorId()).isEqualTo("9");
        assertThat(retried.getLastRetryReason()).isEqualTo("客户要求重发");
        // 新投递事件已追加且换绑
        assertThat(retried.getOutboxEventId()).isNotNull().isNotEqualTo(oldEventId);
        Integer newEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_CHANNEL_SEND'", Integer.class);
        assertThat(newEvents).isEqualTo(2);
    }

    @Test
    void test_人工重试后投递_重发携带同一幂等键() {
        NotifyChannelSendDO record = createFailedRecord("EVT_D2");
        NotifyChannelSendDO retried = channelSendService.manualRetry(record.getId(), "ADMIN", "9", null);
        testSmsSender.submitScript.add(ChannelSubmitResult.accepted("SER-3"));
        channelSendService.processOutboxDelivery(retried.getId());
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        assertThat(after.getStatus()).isEqualTo("ACCEPTED");
        assertThat(after.getAttemptCount()).isEqualTo(2);
        // 不重复发件的兜底：重发仍携带原始渠道幂等键（渠道侧按键去重）
        assertThat(testSmsSender.submittedMessageIds).containsExactly(
                record.getChannelMessageId(), record.getChannelMessageId());
    }

    @Test
    void test_人工重试_DELIVERED拒绝() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_D3");
        channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null));
        assertThatThrownBy(() -> channelSendService.manualRetry(record.getId(), "ADMIN", "9", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许人工重试");
    }

    @Test
    void test_人工重试_ACCEPTED拒绝() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_D4");
        assertThatThrownBy(() -> channelSendService.manualRetry(record.getId(), "ADMIN", "9", null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void test_人工重试_记录不存在_异常() {
        assertThatThrownBy(() -> channelSendService.manualRetry(99999L, "ADMIN", "9", null))
                .isInstanceOf(ServiceException.class);
    }

    // ========== 回执对账 ==========

    @Test
    void test_对账_ACCEPTED回查送达_推进DELIVERED() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_E1");
        testSmsSender.queryScript.add(ChannelQueryResult.delivered("对账回查:DELIVERED"));
        NotifyChannelReconcileResult result = channelSendService.reconcile(record.getId());
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReconcileResult.Outcome.RECONCILED_DELIVERED);
        assertThat(channelSendService.getNotifyChannelSend(record.getId()).getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void test_对账_ACCEPTED回查未发出_仅登记证据不自动重发() {
        NotifyChannelSendDO record = createAcceptedRecord("EVT_E2");
        testSmsSender.queryScript.add(ChannelQueryResult.notSent("渠道无记录"));
        NotifyChannelReconcileResult result = channelSendService.reconcile(record.getId());
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReconcileResult.Outcome.CONFIRMED_NOT_SENT);
        NotifyChannelSendDO after = channelSendService.getNotifyChannelSend(record.getId());
        // 受理态 + 未发出属矛盾：不自动重发（人工决定），证据已留
        assertThat(after.getStatus()).isEqualTo("ACCEPTED");
        assertThat(after.getLastQueryResult()).isEqualTo("NOT_SENT");
        assertThat(testSmsSender.submitCalls).isEqualTo(1);
    }

    @Test
    void test_对账_UNKNOWN仍未知_留证据() {
        NotifyChannelSendDO record = createUnknownRecord("EVT_E3");
        testSmsSender.queryScript.add(ChannelQueryResult.unknown("仍无结论"));
        NotifyChannelReconcileResult result = channelSendService.reconcile(record.getId());
        assertThat(result.getOutcome()).isEqualTo(NotifyChannelReconcileResult.Outcome.STILL_UNKNOWN);
        assertThat(channelSendService.getNotifyChannelSend(record.getId()).getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void test_对账_终态与不存在_不适用() {
        NotifyChannelSendDO delivered = createAcceptedRecord("EVT_E4");
        channelSendService.applyReceipt(receipt(delivered.getChannelMessageId(), true, null));
        assertThat(channelSendService.reconcile(delivered.getId()).getOutcome())
                .isEqualTo(NotifyChannelReconcileResult.Outcome.NOT_APPLICABLE);
        assertThat(channelSendService.reconcile(99999L).getOutcome())
                .isEqualTo(NotifyChannelReconcileResult.Outcome.NOT_APPLICABLE);
    }

    // ========== 租户强制与台账扫描 ==========

    @Test
    void test_租户上下文缺失_拒绝执行() {
        NotifyChannelSendDO record = createPendingRecord("EVT_F1");
        TenantContextHolder.clear();
        assertThatThrownBy(() -> channelSendService.processOutboxDelivery(record.getId()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("租户上下文");
        assertThatThrownBy(() -> channelSendService.applyReceipt(receipt(record.getChannelMessageId(), true, null)))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> channelSendService.manualRetry(record.getId(), "ADMIN", "9", null))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> channelSendService.reconcile(record.getId()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void test_listByStatus_台账扫描与状态校验() {
        createPendingRecord("EVT_F2");
        createPendingRecord("EVT_F3");
        assertThat(channelSendService.listByStatus("PENDING", 100)).hasSize(2);
        assertThat(channelSendService.listByStatus("ACCEPTED", 100)).isEmpty();
        assertThatThrownBy(() -> channelSendService.listByStatus("NOT_A_STATUS", 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== 夹具 ==========

    private NotifyCommand command(String eventId) {
        return NotifyCommand.builder()
                .eventId(eventId)
                .templateCode("TEST_TPL")
                .recipients(List.of(NotifyRecipient.admin(RECIPIENT_ID)))
                .actorType(OutboxActorType.SYSTEM)
                .channels(Set.of(NotifyChannel.SMS))
                .build();
    }

    /** 经派发路径创建 PENDING 台账（MANDATORY 事务内，带联系方式），返回提交后的记录。
     *  send_log_id 每次递增（uk_notify_channel_send_log 唯一键约束）。 */
    private long sendLogIdSeq = 5000L;

    private NotifyChannelSendDO createPendingRecord(String eventId) {
        NotifyChannelSendDO record = transactionTemplate.execute(s -> channelSendService.createFromDispatch(
                command(eventId), NotifyRecipient.admin(RECIPIENT_ID), NotifyChannel.SMS, MOBILE, "内容",
                ++sendLogIdSeq));
        return reload(record.getId());
    }

    /** PENDING → 受理（SER-1），落地 ACCEPTED 记录。 */
    private NotifyChannelSendDO createAcceptedRecord(String eventId) {
        NotifyChannelSendDO record = createPendingRecord(eventId);
        testSmsSender.submitScript.add(ChannelSubmitResult.accepted("SER-1"));
        channelSendService.processOutboxDelivery(record.getId());
        return reload(record.getId());
    }

    /** PENDING → 明确拒绝，落地 FAILED 记录。 */
    private NotifyChannelSendDO createFailedRecord(String eventId) {
        NotifyChannelSendDO record = createPendingRecord(eventId);
        testSmsSender.submitScript.add(ChannelSubmitResult.rejected("INVALID_MOBILE", "号码无效"));
        channelSendService.processOutboxDelivery(record.getId());
        return reload(record.getId());
    }

    /** PENDING → 提交未知，落地 UNKNOWN 记录（吞掉可重试异常）。 */
    private NotifyChannelSendDO createUnknownRecord(String eventId) {
        NotifyChannelSendDO record = createPendingRecord(eventId);
        testSmsSender.submitScript.add(ChannelSubmitResult.unknown("TIMEOUT", "网关超时"));
        assertThatThrownBy(() -> channelSendService.processOutboxDelivery(record.getId()))
                .isInstanceOf(NotifyChannelSendRetryableException.class);
        return reload(record.getId());
    }

    private NotifyChannelSendDO reload(Long id) {
        return transactionTemplate.execute(s -> channelSendMapper.selectById(id));
    }

    private NotifyChannelReceiptCmd receipt(String channelMessageId, boolean delivered, String serialNo) {
        return NotifyChannelReceiptCmd.builder()
                .channel("SMS").channelMessageId(channelMessageId).channelSerialNo(serialNo)
                .delivered(delivered).build();
    }

    private int countChannelSends() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_channel_send", Integer.class);
        return count == null ? 0 : count;
    }

    private int countOutboxEvents() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_CHANNEL_SEND'", Integer.class);
        return count == null ? 0 : count;
    }

}
