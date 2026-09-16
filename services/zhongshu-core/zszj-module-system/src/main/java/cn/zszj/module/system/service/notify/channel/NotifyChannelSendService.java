package cn.zszj.module.system.service.notify.channel;

import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;

import java.util.List;

/**
 * 渠道发送生命周期服务（ZS-MSG-004）——提交、回查、回执、人工重试与对账的状态机入口。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-004）：「待发送、已受理、送达、失败与站内消息状态分开；
 * 超时不确定先回查再重发；渠道幂等键、次数/退避、回执校验和人工重试；未配置渠道明确阻断」。
 *
 * <p>投递驱动：派发成功（台账 PENDING）→ 同事务追加 Outbox 事件 {@code NOTIFY_CHANNEL_SEND} →
 * {@link NotifyChannelSendEventSink} 消费（at-least-once，退避/DEAD 由 ZS-JOB-002 outbox_event 承担）；
 * 退避节奏不在本表重复记账，本表只记录实际提交次数（attempt_count）。
 */
public interface NotifyChannelSendService {

    /** Outbox 事件类型（Sink 与派发侧共同约定） */
    String OUTBOX_EVENT_TYPE = "NOTIFY_CHANNEL_SEND";

    /**
     * 派发事务内创建渠道发送台账（MANDATORY：必须加入派发方事务，业务回滚则台账与 Outbox 一并回滚）。
     *
     * <p>联系方式缺失 → 记录直接 FAILED（failedCode=RECIPIENT_CONTACT_MISSING，明确阻断可查询），
     * <b>不入投递管道</b>（不追加 Outbox 事件）；联系方式齐备 → PENDING + 追加投递事件。
     *
     * @return 创建的台账记录（含 id 与 outboxEventId）
     */
    NotifyChannelSendDO createFromDispatch(NotifyCommand command, NotifyRecipient recipient, NotifyChannel channel,
                                           String contact, String content, Long sendLogId);

    /**
     * 处理一轮 Outbox 投递（Sink 入口；r0 P1 事件身份吸收）：
     * 按 (租户, outboxEventId) 定位台账——定位不到即<b>陈旧/未知事件</b>（人工重试已换绑新事件、或外来事件）
     * 幂等吸收，绝不提交；命中则按台账状态处理：PENDING → 提交渠道、UNKNOWN → 先回查（确认未发出才重发）、
     * ACCEPTED/DELIVERED/FAILED → 幂等吸收。
     *
     * @param sendId        投递句柄（载荷 sendId）
     * @param outboxEventId 本次投递的 Outbox 事件 ID（台账 outbox_event_id 与之不等即陈旧事件——
     *                      人工重试换绑后，旧事件的重投在此被吸收，不产生重叠提交）
     * @throws NotifyChannelSendRetryableException 提交未知/仍未知/技术异常——触发 Outbox 失败退避重投
     */
    void processOutboxDelivery(long sendId, long outboxEventId);

    /**
     * 应用渠道回执（回执校验 + 状态推进；重复回执幂等吸收且计数可追踪；回执永不触发发件）。
     */
    NotifyChannelReceiptResult applyReceipt(NotifyChannelReceiptCmd cmd);

    /**
     * 人工重试：FAILED/UNKNOWN/PENDING → PENDING（复位并留痕操作者/原因/次数），并追加新投递事件；
     * ACCEPTED/DELIVERED 拒绝（受理/送达由回执与回查推进，人工重发会造成重复发件）。
     *
     * <p>r0 P2/P3 处置：复位为<b>旧事件 ID 条件 CAS</b>（expectedOldEventId，并发双击仅一次成功，
     * 落败方连随追加的事件一并回滚）；联系方式缺失记录（RECIPIENT_CONTACT_MISSING）复位前<b>重新解析</b>
     * 收件人联系方式（用户可能已补绑），仍缺失则拒绝——打通「补绑后人工重试」恢复路径。
     *
     * @throws cn.zszj.framework.common.exception.ServiceException 记录不存在 / 状态不允许 / 联系方式仍缺失
     */
    NotifyChannelSendDO manualRetry(long sendId, String actorType, String actorId, String reason);

    /**
     * 回执对账：对 ACCEPTED（回执未达）或 UNKNOWN 记录主动回查渠道，对齐台账与渠道侧事实。
     * 对账出口不自动重发（证据登记后由人工决定），区别于 Outbox 驱动的 UNKNOWN 自动回查重发。
     */
    NotifyChannelReconcileResult reconcile(long sendId);

    /** 按编号查询台账（当前租户）。 */
    NotifyChannelSendDO getNotifyChannelSend(long id);

    /** 按状态列台账（当前租户，id 升序有界——回查/人工处置扫描入口）。 */
    List<NotifyChannelSendDO> listByStatus(String status, int limit);

}
