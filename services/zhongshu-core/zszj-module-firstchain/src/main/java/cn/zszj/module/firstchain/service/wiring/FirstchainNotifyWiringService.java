package cn.zszj.module.firstchain.service.wiring;

import cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyDispatcher;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.todo.NotifyTodoRegisterCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.organization.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首链六节点事件接线服务（ZS-FC-003 服务端接线 wave；PILOT-REQ-010「审批、开通、下发、分配、
 * 领取和结束状态均可通过 trace/业务 ID 追踪；通知失败不静默」）。
 *
 * <p>接线合同（BPM-004 接入合同 §1.8/§1.9/§1.11）：
 * <ul>
 *   <li><b>站内通知</b>（六节点全量）：经 {@link NotifyDispatcher} 统一派发（eventId 幂等键含对象
 *       版本——撤回重提/重开场景产新消息；channels=INBOX；失败以持久化状态落
 *       {@code system_notify_send_log} 可查询，技术异常向上抛回滚业务事务，绝不静默）；</li>
 *   <li><b>待办</b>（审批节点）：提交时 {@link NotifyTodoService#registerTodo} 幂等注册（同事务直调）；
 *       流转（完成/撤销）<b>只经事件驱动</b>——业务事务内 {@link ReliableEventPort#append} 预写
 *       {@code NOTIFY_TODO_TRANSITION}，由 Outbox 派发器（Sink 投递已统一包裹编程式事务）异步应用，
 *       业务侧不直改待办状态双写。待办键 {@code firstchain:approval:<appKey>:<processInstanceId>}——
 *       流程实例 ID 由提交时发起的审批流签发，撤回解绑/重发新发起即产新实例与新待办，
 *       幂等门串行化后无键漂移；</li>
 *   <li><b>同事务</b>：本服务全部方法在调用方业务事务内执行（接线写随业务写同生共死，接入合同
 *       §1.8 Outbox MANDATORY）；追踪口径与审计一致：bizType/bizId=申请编号/线索 ID，可回查。</li>
 * </ul>
 *
 * <p>收件人解析（服务端、不取客户端声明）：审批人/提交人由调用方（业务门面）显式传入；
 * 负责人经 {@link OrganizationService} 实时解析——负责人缺失（未回填/历史数据）时记 WARN 跳过该路
 * 通知（可观测，不阻断业务；不在无收件人时伪造派发）。
 *
 * @author ZS-FC-003
 */
@Service
@Slf4j
public class FirstchainNotifyWiringService {

    /** Outbox 事件类型：待办流转（消费方 {@code NotifyTodoEventSink}，system 模块） */
    private static final String EVENT_TYPE_TODO_TRANSITION = "NOTIFY_TODO_TRANSITION";

    @Resource
    private NotifyTodoService notifyTodoService;

    @Resource
    private NotifyDispatcher notifyDispatcher;

    @Resource
    private ReliableEventPort reliableEventPort;

    @Resource
    private OrganizationService organizationService;

    // ========== 审批 / 开通节点（申请域） ==========

    /**
     * 申请提交（审批节点）：审批待办幂等注册 + 审批提醒通知（致审批人）。
     *
     * @param processInstanceId 本次提交发起的审批流实例（待办键组成部分，撤回重发即换新）
     * @param bizVersion        提交后申请版本（事件/待办的乱序护栏基线）
     */
    public void onApplicationSubmitted(String appKey, Long applicationId, String processInstanceId,
                                       Long bizVersion, String applicantName, Long approverUserId,
                                       String actorId) {
        notifyTodoService.registerTodo(NotifyTodoRegisterCmd.builder()
                .todoKey(approvalTodoKey(appKey, processInstanceId))
                .sourceType(FirstchainNotifyTemplates.TODO_SOURCE_TYPE)
                .bizType(FirstchainNotifyTemplates.BIZ_TYPE_APPLICATION)
                .bizId(appKey)
                .bizVersion(String.valueOf(bizVersion))
                .title("加盟商申请审批：" + applicantName)
                .recipient(NotifyRecipient.admin(approverUserId))
                .build());
        Map<String, Object> params = new HashMap<>();
        params.put("appKey", appKey);
        params.put("applicantName", applicantName);
        dispatch(FirstchainNotifyTemplates.APPLICATION_SUBMITTED,
                approvalEventId(appKey, "submitted", bizVersion),
                List.of(NotifyRecipient.admin(approverUserId)), params,
                FirstchainNotifyTemplates.BIZ_TYPE_APPLICATION, appKey, bizVersion, actorId);
    }

    /**
     * 审批完成（审批节点 + 开通节点）：审批待办经 Outbox 事件流转（COMPLETE，事件驱动不双写）；
     * 结果通知致提交人与新任负责人（拒绝支路无负责人）。
     *
     * @param processInstanceId 审批流实例（与注册时同一实例——幂等门保证审批回调串行于绑定实例）
     * @param bizVersion        审批后申请版本（> 注册时版本，乱序护栏单调）
     */
    public void onApprovalCompleted(String appKey, Long applicationId, String processInstanceId,
                                    Long bizVersion, String applicantName, boolean approved,
                                    Long submitterUserId, Long leaderUserId, String reason, String actorId) {
        appendTodoTransition(appKey, bizVersion, "COMPLETE",
                approved ? "APPROVED" : "REJECTED", processInstanceId, actorId);
        Map<String, Object> params = new HashMap<>();
        params.put("appKey", appKey);
        params.put("applicantName", applicantName);
        params.put("reason", approved ? "审批通过" : String.valueOf(reason));
        List<NotifyRecipient> recipients = new ArrayList<>();
        if (submitterUserId != null) {
            recipients.add(NotifyRecipient.admin(submitterUserId));
        }
        if (approved && leaderUserId != null && !leaderUserId.equals(submitterUserId)) {
            recipients.add(NotifyRecipient.admin(leaderUserId));
        }
        if (recipients.isEmpty()) {
            log.warn("[onApprovalCompleted][审批结果无可用收件人（提交人与负责人均缺失），仅待办事件已预写：appKey={}]", appKey);
            return;
        }
        dispatch(approved ? FirstchainNotifyTemplates.APPLICATION_APPROVED
                        : FirstchainNotifyTemplates.APPLICATION_REJECTED,
                approvalEventId(appKey, approved ? "approved" : "rejected", bizVersion),
                recipients, params, FirstchainNotifyTemplates.BIZ_TYPE_APPLICATION, appKey, bizVersion, actorId);
    }

    /**
     * 审批流撤回（审批节点）：审批待办经 Outbox 事件流转（WITHDRAW——重发新实例即产新待办）。
     */
    public void onApprovalWithdrawn(String appKey, Long applicationId, String processInstanceId,
                                    Long bizVersion, String actorId) {
        appendTodoTransition(appKey, bizVersion, "WITHDRAW", "WITHDRAWN", processInstanceId, actorId);
    }

    // ========== 下发 / 分配 / 领取 / 结束节点（线索域；leadRow 为领域层读回的行映射） ==========

    /** 下发节点：通知归属组织负责人（新线索待分配）。 */
    public void onLeadDistributed(Map<String, Object> leadRow, String actorId) {
        notifyLeader(leadRow, FirstchainNotifyTemplates.LEAD_DISTRIBUTED, "distributed",
                leadParams(leadRow, null), actorId);
    }

    /** 分配节点：通知被分配员工（含改派——新被分配人收到同一模板）。 */
    public void onLeadAssigned(Map<String, Object> leadRow, Long assigneeUserId, String actorId) {
        if (assigneeUserId == null) {
            log.warn("[onLeadAssigned][线索无被分配人，跳过分配通知：leadId={}]", leadRow.get("id"));
            return;
        }
        dispatch(FirstchainNotifyTemplates.LEAD_ASSIGNED, leadEventId(leadRow, "assigned"),
                List.of(NotifyRecipient.admin(assigneeUserId)), leadParams(leadRow, null),
                FirstchainNotifyTemplates.BIZ_TYPE_LEAD, leadBizId(leadRow), leadBizVersion(leadRow), actorId);
    }

    /** 领取节点：通知归属组织负责人（线索已进入跟进）。 */
    public void onLeadClaimed(Map<String, Object> leadRow, String actorId) {
        notifyLeader(leadRow, FirstchainNotifyTemplates.LEAD_CLAIMED, "claimed",
                leadParams(leadRow, null), actorId);
    }

    /** 结束节点：通知归属组织负责人（转商机/无效关闭，closeType 区分类型）。 */
    public void onLeadClosed(Map<String, Object> leadRow, String closeType, String actorId) {
        notifyLeader(leadRow, FirstchainNotifyTemplates.LEAD_CLOSED, "closed",
                leadParams(leadRow, closeType), actorId);
    }

    // ========== 内部 ==========

    /** 待办流转事件（业务事务内预写；载荷键与 {@code NotifyTodoTransitionCmd} 字段一一对应）。 */
    private void appendTodoTransition(String appKey, Long bizVersion, String transitionType, String reason,
                                      String processInstanceId, String actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", approvalEventId(appKey, "transition-" + transitionType, bizVersion));
        payload.put("transitionType", transitionType);
        payload.put("todoKey", approvalTodoKey(appKey, processInstanceId));
        payload.put("sourceType", FirstchainNotifyTemplates.TODO_SOURCE_TYPE);
        payload.put("bizType", FirstchainNotifyTemplates.BIZ_TYPE_APPLICATION);
        payload.put("bizId", appKey);
        payload.put("bizVersion", String.valueOf(bizVersion));
        payload.put("reason", reason);
        reliableEventPort.append(OutboxEventMessage.builder()
                .eventType(EVENT_TYPE_TODO_TRANSITION)
                .bizType(FirstchainNotifyTemplates.BIZ_TYPE_APPLICATION)
                .bizId(appKey)
                .bizVersion(String.valueOf(bizVersion))
                .payload(payload)
                .actorType(OutboxEventMessage.OutboxActorType.ADMIN)
                .actorId(actorId)
                .build());
    }

    /** 致归属组织负责人的一路通知（负责人实时解析；缺失 WARN 跳过——不伪造收件人）。 */
    private void notifyLeader(Map<String, Object> leadRow, String templateCode, String action,
                              Map<String, Object> params, String actorId) {
        Long leaderUserId = resolveLeaderUserId(leadRow);
        if (leaderUserId == null) {
            log.warn("[notifyLeader][线索归属组织负责人缺失，跳过 {} 通知（可观测不静默）：leadId={} orgId={}]",
                    action, leadRow.get("id"), leadRow.get("org_id"));
            return;
        }
        dispatch(templateCode, leadEventId(leadRow, action), List.of(NotifyRecipient.admin(leaderUserId)),
                params, FirstchainNotifyTemplates.BIZ_TYPE_LEAD, leadBizId(leadRow), leadBizVersion(leadRow),
                actorId);
    }

    private void dispatch(String templateCode, String eventId, List<NotifyRecipient> recipients,
                          Map<String, Object> templateParams, String bizType, String bizId, Long bizVersion,
                          String actorId) {
        notifyDispatcher.dispatch(NotifyCommand.builder()
                .eventId(eventId)
                .templateCode(templateCode)
                .recipients(recipients)
                .templateParams(templateParams)
                .bizType(bizType)
                .bizId(bizId)
                .bizVersion(bizVersion == null ? null : String.valueOf(bizVersion))
                .actorType(OutboxEventMessage.OutboxActorType.ADMIN)
                .actorId(actorId)
                .channels(Set.of(NotifyChannel.INBOX))
                .build());
    }

    private Long resolveLeaderUserId(Map<String, Object> leadRow) {
        Object orgId = leadRow.get("org_id");
        if (!(orgId instanceof Number orgIdNumber)) {
            return null;
        }
        OrganizationDO organization = organizationService.getOrganization(orgIdNumber.longValue());
        if (organization == null || !OrganizationTypeEnum.FRANCHISEE.getType().equals(organization.getType())) {
            return null;
        }
        return organization.getLeaderUserId();
    }

    private static String approvalTodoKey(String appKey, String processInstanceId) {
        return "firstchain:approval:" + appKey + ":" + processInstanceId;
    }

    private static String approvalEventId(String appKey, String action, Long version) {
        return "firstchain:approval:" + appKey + ":" + action + ":v" + version;
    }

    private static String leadEventId(Map<String, Object> leadRow, String action) {
        return "firstchain:lead:" + leadRow.get("id") + ":" + action + ":v" + leadBizVersion(leadRow);
    }

    private static String leadBizId(Map<String, Object> leadRow) {
        return String.valueOf(leadRow.get("id"));
    }

    private static Long leadBizVersion(Map<String, Object> leadRow) {
        Object version = leadRow.get("version");
        return version instanceof Number number ? number.longValue() : null;
    }

    /** 线索通知模板参数（leadId/leadKey/customerName [+closeType]；与 V20261001.002 种子 params 列一致）。 */
    private static Map<String, Object> leadParams(Map<String, Object> leadRow, String closeType) {
        Map<String, Object> params = new HashMap<>();
        params.put("leadId", String.valueOf(leadRow.get("id")));
        params.put("leadKey", String.valueOf(leadRow.get("lead_key")));
        params.put("customerName", String.valueOf(leadRow.get("customer_name")));
        if (closeType != null) {
            params.put("closeType", closeType);
        }
        return params;
    }

}
