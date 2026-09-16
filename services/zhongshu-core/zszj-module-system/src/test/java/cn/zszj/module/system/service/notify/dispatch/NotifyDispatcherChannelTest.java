package cn.zszj.module.system.service.notify.dispatch;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.service.notify.NotifyMessageService;
import cn.zszj.module.system.service.notify.NotifyTemplateService;
import cn.zszj.module.system.service.notify.channel.ChannelQueryResult;
import cn.zszj.module.system.service.notify.channel.ChannelSubmitResult;
import cn.zszj.module.system.service.notify.channel.NotifyChannelSendServiceImpl;
import cn.zszj.module.system.service.notify.channel.NotifyChannelSender;
import cn.zszj.module.system.service.notify.channel.NotifyChannelSenderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotifyDispatcher} 渠道发送集成测试（ZS-MSG-004，H2）——配置齐备渠道的派发路径。
 *
 * <p>与 {@code NotifyDispatcherTest}（无发送器上下文，验证 CHANNEL_NOT_CONFIGURED 阻断）互补，本类装配
 * SMS 发送器桩（仅注册渠道可用性，派发路径不触发 submit），验证：①SMS 派发 SUCCESS → 渠道发送台账
 * PENDING + NOTIFY_CHANNEL_SEND Outbox 事件 + send_log 回填关联；②渠道发送状态与站内消息状态分开
 * （INBOX+SMS 双渠道并存，各自副作用独立）；③派发重试幂等（不重复建台账/事件）；④派发事务回滚 →
 * 台账与事件一并回滚；⑤联系方式缺失 → 台账 FAILED(RECIPIENT_CONTACT_MISSING) 明确阻断不入管道；
 * ⑥EMAIL 未配置（无实现）→ CHANNEL_NOT_CONFIGURED；⑦渲染内容随台账留档（重试不改写）。
 */
@Import({NotifyDispatcherImpl.class, AdminUserNotifyRecipientContextResolver.class, JdbcReliableEventPort.class,
        NotifyChannelSenderRegistry.class, NotifyChannelSendServiceImpl.class,
        NotifyDispatcherChannelTest.TestSmsChannelSender.class})
public class NotifyDispatcherChannelTest extends BaseDbUnitTest {

    @Resource
    private NotifyDispatcher notifyDispatcher;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;
    @MockBean
    private AdminUserApi adminUserApi;
    @MockBean
    private NotifyTemplateService notifyTemplateService;
    @MockBean
    private NotifyMessageService notifyMessageService;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    private static final Long TENANT_ID = 1L;
    private static final Long ADMIN_USER_ID = 100L;
    private static final String TEMPLATE_CODE = "TEST_TEMPLATE";

    /** SMS 发送器桩：仅证明「渠道已配置」（派发路径只查注册器，不触发 submit/query）。 */
    public static class TestSmsChannelSender implements NotifyChannelSender {

        @Override
        public NotifyChannel channel() {
            return NotifyChannel.SMS;
        }

        @Override
        public ChannelSubmitResult submit(NotifyChannelSendDO record) {
            throw new AssertionError("派发路径不应触发 submit（提交由 Outbox Sink 驱动）");
        }

        @Override
        public ChannelQueryResult queryByReceiptKey(String channelMessageId) {
            throw new AssertionError("派发路径不应触发 query");
        }

    }

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        TenantContextHolder.setTenantId(TENANT_ID);
        NotifyTemplateDO template = new NotifyTemplateDO();
        template.setId(1L);
        template.setCode(TEMPLATE_CODE);
        template.setStatus(CommonStatusEnum.ENABLE.getStatus());
        template.setContent("测试内容 {param}");
        template.setParams(List.of("param"));
        template.setType(1);
        template.setNickname("测试模板");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(TEMPLATE_CODE)).thenReturn(template);
        when(notifyTemplateService.formatNotifyTemplateContent(template.getContent(), Map.of("param", "value")))
                .thenReturn("测试内容 value");
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(ADMIN_USER_ID);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setMobile("13800138000");
        user.setEmail("user@test.com");
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(user);
        when(notifyMessageService.createNotifyMessage(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1000L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        jdbcTemplate.execute("DELETE FROM system_notify_send_log");
        jdbcTemplate.execute("DELETE FROM system_notify_message");
        jdbcTemplate.execute("DELETE FROM system_notify_channel_send");
        jdbcTemplate.execute("DELETE FROM outbox_event");
    }

    @Test
    void test_SMS派发_SUCCESS_台账PENDING加事件加回填() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.SMS));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        // 渠道发送台账：PENDING + 手机号联系方式 + 渲染内容留档 + 渠道幂等键
        Integer sends = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_channel_send WHERE tenant_id = 1 AND channel = 'SMS' "
                        + "AND status = 'PENDING' AND recipient_contact = '13800138000' "
                        + "AND content = '测试内容 value' AND channel_message_id LIKE 'NC-%'",
                Integer.class);
        assertThat(sends).isEqualTo(1);
        // Outbox 投递事件：NOTIFY_CHANNEL_SEND 已同事务追加
        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_CHANNEL_SEND' "
                        + "AND tenant_id = 1 AND biz_id = 'BIZ-1'",
                Integer.class);
        assertThat(events).isEqualTo(1);
        // send_log 回填 outbox_event_id（message_id 为空——渠道发送与站内消息分开）
        Integer linked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_send_log sl JOIN system_notify_channel_send cs "
                        + "ON cs.send_log_id = sl.id AND cs.outbox_event_id = sl.outbox_event_id",
                Integer.class);
        assertThat(linked).isEqualTo(1);
    }

    @Test
    void test_INBOX加SMS双渠道_状态分开_各自副作用() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.INBOX, NotifyChannel.SMS));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(2);
        assertThat(results).extracting(NotifyDispatchResult::getStatus)
                .containsOnly(NotifyDispatchStatus.SUCCESS);
        // INBOX：站内消息副作用已发生（消息状态与渠道发送状态分开建模，互不推断；消息服务为 Mock 以调用断言）
        verify(notifyMessageService).createNotifyMessage(
                org.mockito.ArgumentMatchers.eq(ADMIN_USER_ID), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("测试内容 value"),
                org.mockito.ArgumentMatchers.any());
        // SMS：渠道发送台账 PENDING（投递状态在台账推进，站内消息不受影响）
        Integer sends = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_channel_send WHERE status = 'PENDING'", Integer.class);
        assertThat(sends).isEqualTo(1);
        // 两类 Outbox 事件并存
        Integer dispatched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_DISPATCHED'", Integer.class);
        Integer channelSend = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_CHANNEL_SEND'", Integer.class);
        assertThat(dispatched).isEqualTo(1);
        assertThat(channelSend).isEqualTo(1);
    }

    @Test
    void test_SMS派发重试_幂等_不重复建台账与事件() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.SMS));
        transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        List<NotifyDispatchResult> second = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(second.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DUPLICATE_IGNORED);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countChannelSends()).isEqualTo(1);
        assertThat(countChannelSendEvents()).isEqualTo(1);
    }

    @Test
    void test_派发事务回滚_台账与事件一并回滚() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.SMS));
        transactionTemplate.execute(status -> {
            notifyDispatcher.dispatch(cmd);
            status.setRollbackOnly();
            return null;
        });
        assertThat(countSendLogs()).isZero();
        assertThat(countChannelSends()).isZero();
        assertThat(countChannelSendEvents()).isZero();
    }

    @Test
    void test_联系方式缺失_台账FAILED明确阻断_不入管道() {
        AdminUserRespDTO noMobile = new AdminUserRespDTO();
        noMobile.setId(ADMIN_USER_ID);
        noMobile.setStatus(CommonStatusEnum.ENABLE.getStatus());
        // mobile 为 null：用户本身有效（RECIPIENT_INVALID 不适用），联系方式缺失由台账明确阻断
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(noMobile);
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.SMS));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        Integer blocked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_channel_send WHERE status = 'FAILED' "
                        + "AND failed_code = 'RECIPIENT_CONTACT_MISSING'",
                Integer.class);
        assertThat(blocked).isEqualTo(1);
        // 不入投递管道：无投递事件
        assertThat(countChannelSendEvents()).isZero();
    }

    @Test
    void test_EMAIL未配置_返回CHANNEL_NOT_CONFIGURED() {
        // 本上下文只注册 SMS 发送器：EMAIL 无实现 → 明确阻断（区别于 NO_CHANNEL）
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, Set.of(NotifyChannel.EMAIL));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.CHANNEL_NOT_CONFIGURED);
        assertThat(countChannelSends()).isZero();
        assertThat(countChannelSendEvents()).isZero();
    }

    // ========== 夹具 ==========

    private NotifyCommand command(String eventId, Set<NotifyChannel> channels) {
        return NotifyCommand.builder()
                .eventId(eventId)
                .templateCode(TEMPLATE_CODE)
                .templateParams(Map.of("param", "value"))
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID)))
                .bizType("test_biz")
                .bizId("BIZ-1")
                .actorType(OutboxActorType.SYSTEM)
                .channels(channels)
                .build();
    }

    private int countSendLogs() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_notify_send_log", Integer.class);
        return count == null ? 0 : count;
    }

    private int countChannelSends() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_notify_channel_send", Integer.class);
        return count == null ? 0 : count;
    }

    private int countChannelSendEvents() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'NOTIFY_CHANNEL_SEND'", Integer.class);
        return count == null ? 0 : count;
    }

}
