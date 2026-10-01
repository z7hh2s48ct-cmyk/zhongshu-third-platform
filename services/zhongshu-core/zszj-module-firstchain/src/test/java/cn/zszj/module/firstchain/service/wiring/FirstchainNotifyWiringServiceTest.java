package cn.zszj.module.firstchain.service.wiring;

import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyDispatcher;
import cn.zszj.module.system.service.notify.todo.NotifyTodoRegisterCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.organization.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainNotifyWiringService} 的单元测试（ZS-FC-003 服务端接线 wave）——六节点接线合同。
 *
 * <p>覆盖：审批节点待办注册 + 通知（致审批人）；审批完成走 Outbox 事件驱动（COMPLETE，载荷键与
 * {@code NotifyTodoTransitionCmd} 对齐）+ 结果通知（通过致提交人与新负责人、拒绝仅致提交人携意见）；
 * 撤回走 WITHDRAW 事件且不发结果通知；线索四类事件通知收件人与模板参数；负责人缺失 WARN 跳过不派发。
 *
 * @author ZS-FC-003
 */
class FirstchainNotifyWiringServiceTest extends BaseMockitoUnitTest {

    private static final String APP_KEY = "FC20261001-ABCD1234";

    private static final String PROCESS_INSTANCE_ID = "PINST-42";

    private NotifyTodoService notifyTodoService;

    private NotifyDispatcher notifyDispatcher;

    private ReliableEventPort reliableEventPort;

    private OrganizationService organizationService;

    private FirstchainNotifyWiringService wiringService;

    @BeforeEach
    void setUp() {
        notifyTodoService = mock(NotifyTodoService.class);
        notifyDispatcher = mock(NotifyDispatcher.class);
        reliableEventPort = mock(ReliableEventPort.class);
        organizationService = mock(OrganizationService.class);
        wiringService = new FirstchainNotifyWiringService();
        ReflectionTestUtils.setField(wiringService, "notifyTodoService", notifyTodoService);
        ReflectionTestUtils.setField(wiringService, "notifyDispatcher", notifyDispatcher);
        ReflectionTestUtils.setField(wiringService, "reliableEventPort", reliableEventPort);
        ReflectionTestUtils.setField(wiringService, "organizationService", organizationService);
    }

    // ========== 审批节点 ==========

    @Test
    void onApplicationSubmitted_registersTodoAndNotifiesApprover() {
        wiringService.onApplicationSubmitted(APP_KEY, 1024L, PROCESS_INSTANCE_ID, 1L,
                "众墅家装联盟（华东）", 200L, "1");

        // 待办注册（幂等键=租户+sourceType+todoKey；todoKey 携流程实例——撤回重发产新待办）
        ArgumentCaptor<NotifyTodoRegisterCmd> todoCaptor = ArgumentCaptor.forClass(NotifyTodoRegisterCmd.class);
        verify(notifyTodoService).registerTodo(todoCaptor.capture());
        NotifyTodoRegisterCmd todo = todoCaptor.getValue();
        assertThat(todo.getTodoKey()).isEqualTo("firstchain:approval:" + APP_KEY + ":" + PROCESS_INSTANCE_ID);
        assertThat(todo.getSourceType()).isEqualTo("BPM");
        assertThat(todo.getBizId()).isEqualTo(APP_KEY);
        assertThat(todo.getBizVersion()).isEqualTo("1");
        assertThat(todo.getRecipient().getId()).isEqualTo(200L);

        // 审批提醒通知（致审批人，INBOX）
        ArgumentCaptor<NotifyCommand> cmdCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher).dispatch(cmdCaptor.capture());
        NotifyCommand cmd = cmdCaptor.getValue();
        assertThat(cmd.getTemplateCode()).isEqualTo("firstchain_application_submitted");
        assertThat(cmd.getEventId()).isEqualTo("firstchain:approval:" + APP_KEY + ":submitted:v1");
        assertThat(cmd.getRecipients()).hasSize(1);
        assertThat(cmd.getRecipients().get(0).getId()).isEqualTo(200L);
        assertThat(cmd.getChannels()).containsExactly(NotifyChannel.INBOX);
        assertThat(cmd.getTemplateParams()).containsEntry("appKey", APP_KEY)
                .containsEntry("applicantName", "众墅家装联盟（华东）");
    }

    @Test
    void onApprovalCompleted_appendedOutboxTransitionAndNotifiesSubmitterAndLeader() {
        wiringService.onApprovalCompleted(APP_KEY, 1024L, PROCESS_INSTANCE_ID, 2L,
                "众墅家装联盟（华东）", true, 100L, 900L, "同意开通", "1");

        // 待办流转事件（COMPLETE，事件驱动不双写；载荷键与 NotifyTodoTransitionCmd 字段对齐）
        @SuppressWarnings("unchecked")
        ArgumentCaptor<OutboxEventMessage> eventCaptor = ArgumentCaptor.forClass(OutboxEventMessage.class);
        verify(reliableEventPort).append(eventCaptor.capture());
        OutboxEventMessage event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("NOTIFY_TODO_TRANSITION");
        assertThat(event.getBizId()).isEqualTo(APP_KEY);
        assertThat(event.getPayload())
                .containsEntry("transitionType", "COMPLETE")
                .containsEntry("todoKey", "firstchain:approval:" + APP_KEY + ":" + PROCESS_INSTANCE_ID)
                .containsEntry("sourceType", "BPM")
                .containsEntry("reason", "APPROVED");

        // 结果通知：通过支路致提交人与新任负责人（去重不重复发给同一人）
        ArgumentCaptor<NotifyCommand> cmdCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher).dispatch(cmdCaptor.capture());
        NotifyCommand cmd = cmdCaptor.getValue();
        assertThat(cmd.getTemplateCode()).isEqualTo("firstchain_application_approved");
        assertThat(cmd.getRecipients()).extracting(r -> r.getId()).containsExactlyInAnyOrder(100L, 900L);
    }

    @Test
    void onApprovalCompleted_rejectedNotifiesSubmitterOnlyWithReason() {
        wiringService.onApprovalCompleted(APP_KEY, 1024L, PROCESS_INSTANCE_ID, 2L,
                "众墅家装联盟（华东）", false, 100L, null, "资质材料不全", "1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OutboxEventMessage> eventCaptor = ArgumentCaptor.forClass(OutboxEventMessage.class);
        verify(reliableEventPort).append(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload())
                .containsEntry("transitionType", "COMPLETE")
                .containsEntry("reason", "REJECTED");

        ArgumentCaptor<NotifyCommand> cmdCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher).dispatch(cmdCaptor.capture());
        NotifyCommand cmd = cmdCaptor.getValue();
        assertThat(cmd.getTemplateCode()).isEqualTo("firstchain_application_rejected");
        assertThat(cmd.getRecipients()).extracting(r -> r.getId()).containsExactly(100L);
        assertThat(cmd.getTemplateParams()).containsEntry("reason", "资质材料不全");
    }

    @Test
    void onApprovalWithdrawn_appendsWithdrawEventWithoutNotify() {
        wiringService.onApprovalWithdrawn(APP_KEY, 1024L, PROCESS_INSTANCE_ID, 2L, "1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OutboxEventMessage> eventCaptor = ArgumentCaptor.forClass(OutboxEventMessage.class);
        verify(reliableEventPort).append(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload())
                .containsEntry("transitionType", "WITHDRAW")
                .containsEntry("todoKey", "firstchain:approval:" + APP_KEY + ":" + PROCESS_INSTANCE_ID);
        verify(notifyDispatcher, never()).dispatch(any());
    }

    // ========== 线索节点 ==========

    @Test
    void onLeadDistributed_notifiesOrgLeader() {
        stubLeader(300L, 900L);

        wiringService.onLeadDistributed(leadRow(), "1");

        ArgumentCaptor<NotifyCommand> cmdCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher).dispatch(cmdCaptor.capture());
        NotifyCommand cmd = cmdCaptor.getValue();
        assertThat(cmd.getTemplateCode()).isEqualTo("firstchain_lead_distributed");
        assertThat(cmd.getRecipients()).extracting(r -> r.getId()).containsExactly(900L);
        assertThat(cmd.getTemplateParams()).containsEntry("leadKey", "LC20261001-ABCD1234")
                .containsEntry("customerName", "李四");
    }

    @Test
    void onLeadDistributed_leaderMissing_skipsWithoutDispatch() {
        stubLeader(300L, null);

        wiringService.onLeadDistributed(leadRow(), "1");

        verify(notifyDispatcher, never()).dispatch(any());
    }

    @Test
    void onLeadAssigned_notifiesAssignee_onLeadClosedNotifiesLeaderWithCloseType() {
        stubLeader(300L, 900L);

        wiringService.onLeadAssigned(leadRow(), 200L, "1");
        ArgumentCaptor<NotifyCommand> assignedCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher).dispatch(assignedCaptor.capture());
        assertThat(assignedCaptor.getValue().getTemplateCode()).isEqualTo("firstchain_lead_assigned");
        assertThat(assignedCaptor.getValue().getRecipients()).extracting(r -> r.getId()).containsExactly(200L);

        wiringService.onLeadClosed(leadRow(), "转商机", "1");
        ArgumentCaptor<NotifyCommand> closedCaptor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher, org.mockito.Mockito.times(2)).dispatch(closedCaptor.capture());
        NotifyCommand closed = closedCaptor.getAllValues().get(1);
        assertThat(closed.getTemplateCode()).isEqualTo("firstchain_lead_closed");
        assertThat(closed.getRecipients()).extracting(r -> r.getId()).containsExactly(900L);
        assertThat(closed.getTemplateParams()).containsEntry("closeType", "转商机");
    }

    // ========== 夹具 ==========

    private static Map<String, Object> leadRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 2048L);
        row.put("lead_key", "LC20261001-ABCD1234");
        row.put("customer_name", "李四");
        row.put("org_id", 300L);
        row.put("assignee_user_id", 200L);
        row.put("status", "FOLLOWING");
        row.put("version", 2L);
        return row;
    }

    private void stubLeader(Long orgId, Long leaderUserId) {
        OrganizationDO organization = new OrganizationDO();
        organization.setId(orgId);
        organization.setType(OrganizationTypeEnum.FRANCHISEE.getType());
        organization.setLeaderUserId(leaderUserId);
        when(organizationService.getOrganization(anyLong())).thenReturn(organization);
    }

}
