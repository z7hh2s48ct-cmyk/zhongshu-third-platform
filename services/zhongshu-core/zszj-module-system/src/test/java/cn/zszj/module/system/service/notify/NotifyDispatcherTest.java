package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.service.notify.dispatch.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotifyDispatcher} 单元测试（ZS-MSG-001，H2）。
 *
 * <p>覆盖 17 个用例：幂等重试 / 多收件人 / 禁用模板 / 无渠道(持久化+幂等) / 非INBOX渠道 / 停用收件人 /
 * 租户上下文缺失 / 模板不存在 / 参数缺失 / 成功+Outbox / 事务回滚 / eventId空 / recipients空 /
 * 成功重试副作用抑制 / 失败状态重试幂等 / 收件人查询技术异常回滚不占键 / null参数归一化。
 *
 * <p>循 JOB-002 OutboxEventPortTest 先例：H2 + BaseDbUnitTest + @Import 显式装配
 * （含 {@link JdbcReliableEventPort} 以提供 ReliableEventPort）+ 注入 DataSource/PlatformTransactionManager
 * 手动构建 JdbcTemplate/TransactionTemplate（BaseDbUnitTest 不直接暴露二者为 Bean）+
 * TransactionTemplate 显式控制事务（提交 vs 回滚）+ JdbcTemplate 直查断言。
 *
 * <p>claim-first 副作用验证：消息服务为 MockBean（无真实消息行），故以 Mockito verify 断言
 * createNotifyMessage 的<b>调用次数</b>证明「重试/失败/并发不重复产生副作用」；发送日志与 Outbox
 * 为真实表，以 JdbcTemplate 直查行数 + 关联 ID 证明落库与幂等。
 *
 * <p>跨租户收件人语义说明：AdminUserRespDTO 无 tenantId 字段，跨租户过滤由 MyBatis-Plus
 * 租户拦截器在生产环境完成（返回 null）；单元测试中 adminUserApi 为 MockBean，无法复现拦截器
 * 行为，故 RECIPIENT_TENANT_MISMATCH 为预留状态（无触发路径），「用户不存在或跨租户」合并到
 * RECIPIENT_INVALID（adminUserApi 返回 null）；租户上下文缺失循 JOB-002 fail-closed 先例抛异常。
 */
@Import({NotifyDispatcherImpl.class, AdminUserNotifyRecipientContextResolver.class, JdbcReliableEventPort.class})
public class NotifyDispatcherTest extends BaseDbUnitTest {

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
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(user);
        when(notifyMessageService.createNotifyMessage(anyLong(), anyInt(), any(), any(), any())).thenReturn(1000L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        jdbcTemplate.execute("DELETE FROM system_notify_send_log");
        jdbcTemplate.execute("DELETE FROM system_notify_message");
        jdbcTemplate.execute("DELETE FROM outbox_event");
    }

    @Test
    void test_同一eventId_同一收件人_重试不重复生成消息() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        List<NotifyDispatchResult> r1 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r1).hasSize(1);
        assertThat(r1.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        Long sendLogId1 = r1.get(0).getSendLogId();
        List<NotifyDispatchResult> r2 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r2).hasSize(1);
        assertThat(r2.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DUPLICATE_IGNORED);
        assertThat(r2.get(0).getSendLogId()).isEqualTo(sendLogId1);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isEqualTo(1);
    }

    @Test
    void test_同一eventId_不同收件人_分别生成消息() {
        String eventId = "EVT_" + UUID.randomUUID();
        Long user2Id = 200L;
        AdminUserRespDTO user2 = new AdminUserRespDTO();
        user2.setId(user2Id);
        user2.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUser(user2Id)).thenReturn(user2);
        NotifyCommand cmd = NotifyCommand.builder().eventId(eventId).templateCode(TEMPLATE_CODE)
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID), NotifyRecipient.admin(user2Id)))
                .templateParams(Map.of("param", "value")).actorType(OutboxActorType.SYSTEM)
                .channels(Set.of(NotifyChannel.INBOX)).build();
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getStatus() == NotifyDispatchStatus.SUCCESS);
        assertThat(countSendLogs()).isEqualTo(2);
        assertThat(countOutboxEvents()).isEqualTo(2);
    }

    @Test
    void test_禁用模板_返回DISABLED_TEMPLATE_不生成消息_不入outbox() {
        stubDisabledTemplate();
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DISABLED_TEMPLATE);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void test_无渠道_返回NO_CHANNEL_持久化可查询且幂等() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of());
        List<NotifyDispatchResult> r1 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r1).hasSize(1);
        assertThat(r1.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.NO_CHANNEL);
        // 以哨兵渠道落库（可查询、可幂等），不入 outbox、不建消息
        assertThat(r1.get(0).getSendLogId()).isNotNull();
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isZero();
        verify(notifyMessageService, never()).createNotifyMessage(anyLong(), anyInt(), any(), any(), any());
        // 重试命中幂等，不重复落库
        List<NotifyDispatchResult> r2 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r2.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DUPLICATE_IGNORED);
        assertThat(countSendLogs()).isEqualTo(1);
    }

    @Test
    void test_渠道非INBOX_返回NO_CHANNEL() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.SMS));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.NO_CHANNEL);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void test_停用收件人_返回RECIPIENT_INVALID() {
        AdminUserRespDTO disabledUser = new AdminUserRespDTO();
        disabledUser.setId(ADMIN_USER_ID);
        disabledUser.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(disabledUser);
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.RECIPIENT_INVALID);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void test_租户上下文缺失_抛异常拒绝派发() {
        // 循 JOB-002 outbox_event fail-closed 先例：TenantContextHolder 缺失时抛异常拒绝派发（系统级错误，非业务状态）
        TenantContextHolder.clear();
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        assertThatThrownBy(() -> transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd)))
                .isInstanceOf(Exception.class);
        assertThat(countOutboxEvents()).isZero();
        assertThat(countSendLogs()).isZero();
        // 恢复租户上下文供 tearDown 使用
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @Test
    void test_模板不存在_返回TEMPLATE_NOT_FOUND() {
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(TEMPLATE_CODE)).thenReturn(null);
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.TEMPLATE_NOT_FOUND);
    }

    @Test
    void test_模板参数缺失_返回TEMPLATE_PARAM_MISSING() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = NotifyCommand.builder().eventId(eventId).templateCode(TEMPLATE_CODE)
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID))).templateParams(Map.of())
                .actorType(OutboxActorType.SYSTEM).channels(Set.of(NotifyChannel.INBOX)).build();
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.TEMPLATE_PARAM_MISSING);
    }

    @Test
    void test_成功发送_同事务内追加outbox事件() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = NotifyCommand.builder().eventId(eventId).templateCode(TEMPLATE_CODE)
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID))).templateParams(Map.of("param", "value"))
                .bizType("test_biz").bizId("biz_123").bizVersion("v1")
                .actorType(OutboxActorType.ADMIN).actorId("actor_1").traceId("trace_abc")
                .channels(Set.of(NotifyChannel.INBOX)).build();
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        NotifyDispatchResult r = results.get(0);
        assertThat(r.getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        assertThat(r.getOutboxEventId()).isNotNull();
        assertThat(r.getSendLogId()).isNotNull();
        Map<String, Object> outbox = jdbcTemplate.queryForMap(
                "SELECT event_type, biz_type, biz_id, biz_version, actor_type, actor_id, trace_id, tenant_id FROM outbox_event WHERE id = ?",
                r.getOutboxEventId());
        assertThat(outbox.get("event_type")).isEqualTo("NOTIFY_DISPATCHED");
        assertThat(outbox.get("biz_type")).isEqualTo("test_biz");
        assertThat(outbox.get("biz_id")).isEqualTo("biz_123");
        assertThat(outbox.get("actor_type")).isEqualTo("ADMIN");
        assertThat(((Number) outbox.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        // 抢位日志回填了 messageId/outboxEventId（claim-first 副作用后置回填）
        Map<String, Object> log = jdbcTemplate.queryForMap(
                "SELECT status, message_id, outbox_event_id FROM system_notify_send_log WHERE id = ?", r.getSendLogId());
        assertThat(log.get("status")).isEqualTo("SUCCESS");
        assertThat(((Number) log.get("outbox_event_id")).longValue()).isEqualTo(r.getOutboxEventId());
        assertThat(log.get("message_id")).isNotNull();
    }

    @Test
    void test_业务事务回滚_不生成消息_不入outbox() {
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        transactionTemplate.execute(s -> {
            notifyDispatcher.dispatch(cmd);
            s.setRollbackOnly();
            return null;
        });
        assertThat(countSendLogs()).isZero();
        assertThat(countOutboxEvents()).isZero();
    }

    @Test
    void test_eventId为空_抛异常() {
        NotifyCommand cmd = NotifyCommand.builder().eventId(null).templateCode(TEMPLATE_CODE)
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID))).templateParams(Map.of("param", "value"))
                .actorType(OutboxActorType.SYSTEM).channels(Set.of(NotifyChannel.INBOX)).build();
        assertThatThrownBy(() -> transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd)))
                .isInstanceOf(Exception.class).hasMessageContaining("eventId");
    }

    @Test
    void test_收件人为空_抛异常() {
        NotifyCommand cmd = NotifyCommand.builder().eventId("EVT_" + UUID.randomUUID()).templateCode(TEMPLATE_CODE)
                .recipients(List.of()).templateParams(Map.of("param", "value"))
                .actorType(OutboxActorType.SYSTEM).channels(Set.of(NotifyChannel.INBOX)).build();
        assertThatThrownBy(() -> transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd)))
                .isInstanceOf(Exception.class).hasMessageContaining("收件人");
    }

    @Test
    void test_成功重试_第二次不再建消息也不入outbox() {
        // P1 核心性质：claim-first 使副作用只在首次抢到键时发生一次
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        verify(notifyMessageService, times(1)).createNotifyMessage(anyLong(), anyInt(), any(), any(), any());
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isEqualTo(1);
    }

    @Test
    void test_失败状态重试_第二次命中幂等_不建消息() {
        // P1「无需并发也可触发」修复点：失败状态命令第二次调用命中预检，不再重复写日志
        stubDisabledTemplate();
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        List<NotifyDispatchResult> r1 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r1.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DISABLED_TEMPLATE);
        List<NotifyDispatchResult> r2 = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(r2.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.DUPLICATE_IGNORED);
        assertThat(r2.get(0).getSendLogId()).isEqualTo(r1.get(0).getSendLogId());
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isZero();
        verify(notifyMessageService, never()).createNotifyMessage(anyLong(), anyInt(), any(), any(), any());
    }

    @Test
    void test_收件人查询技术异常_向上抛回滚_不占用幂等键_恢复后可成功() {
        // P2：技术故障不固化为 RECIPIENT_INVALID；向上抛触发回滚，不写日志、不占用幂等键
        String eventId = "EVT_" + UUID.randomUUID();
        doThrow(new RuntimeException("模拟用户查询技术故障")).when(adminUserApi).getUser(ADMIN_USER_ID);
        NotifyCommand cmd = command(eventId, NotifyRecipient.admin(ADMIN_USER_ID), Set.of(NotifyChannel.INBOX));
        assertThatThrownBy(() -> transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd)))
                .isInstanceOf(RuntimeException.class);
        assertThat(countSendLogs()).isZero();
        assertThat(countOutboxEvents()).isZero();
        verify(notifyMessageService, never()).createNotifyMessage(anyLong(), anyInt(), any(), any(), any());
        // 故障恢复后同一 eventId 重试可成功（幂等键未被技术故障占用）
        AdminUserRespDTO recovered = new AdminUserRespDTO();
        recovered.setId(ADMIN_USER_ID);
        recovered.setStatus(CommonStatusEnum.ENABLE.getStatus());
        doReturn(recovered).when(adminUserApi).getUser(ADMIN_USER_ID);
        List<NotifyDispatchResult> retry = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(retry).hasSize(1);
        assertThat(retry.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        assertThat(countSendLogs()).isEqualTo(1);
        assertThat(countOutboxEvents()).isEqualTo(1);
    }

    @Test
    void test_无参数模板_null参数归一化_成功派发() {
        // P2：合同允许 templateParams=null，入口归一化为空 Map，不把 null 透传给真实消息服务（其列 NOT NULL）
        NotifyTemplateDO noParam = new NotifyTemplateDO();
        noParam.setId(3L);
        noParam.setCode(TEMPLATE_CODE);
        noParam.setStatus(CommonStatusEnum.ENABLE.getStatus());
        noParam.setContent("无占位符内容");
        noParam.setParams(List.of());
        noParam.setType(1);
        noParam.setNickname("无参模板");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(TEMPLATE_CODE)).thenReturn(noParam);
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyCommand cmd = NotifyCommand.builder().eventId(eventId).templateCode(TEMPLATE_CODE)
                .recipients(List.of(NotifyRecipient.admin(ADMIN_USER_ID))).templateParams(null)
                .actorType(OutboxActorType.SYSTEM).channels(Set.of(NotifyChannel.INBOX)).build();
        List<NotifyDispatchResult> results = transactionTemplate.execute(s -> notifyDispatcher.dispatch(cmd));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(NotifyDispatchStatus.SUCCESS);
        // 归一化后以空 Map（非 null）调用消息服务
        verify(notifyMessageService).createNotifyMessage(anyLong(), anyInt(), any(), any(), eq(Collections.emptyMap()));
    }

    private void stubDisabledTemplate() {
        NotifyTemplateDO disabled = new NotifyTemplateDO();
        disabled.setId(2L);
        disabled.setCode(TEMPLATE_CODE);
        disabled.setStatus(CommonStatusEnum.DISABLE.getStatus());
        disabled.setContent("禁用");
        disabled.setParams(List.of());
        disabled.setType(1);
        disabled.setNickname("禁用模板");
        when(notifyTemplateService.getNotifyTemplateByCodeFromCache(TEMPLATE_CODE)).thenReturn(disabled);
    }

    private NotifyCommand command(String eventId, NotifyRecipient recipient, Set<NotifyChannel> channels) {
        return NotifyCommand.builder()
                .eventId(eventId).templateCode(TEMPLATE_CODE)
                .recipients(List.of(recipient)).templateParams(Map.of("param", "value"))
                .actorType(OutboxActorType.SYSTEM).channels(channels).build();
    }

    private int countSendLogs() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM system_notify_send_log", Integer.class);
        return c != null ? c : 0;
    }

    private int countOutboxEvents() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM outbox_event", Integer.class);
        return c != null ? c : 0;
    }
}
