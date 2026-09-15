package cn.zszj.module.system.service.notify.dispatch;

import lombok.Builder;
import lombok.Value;

/**
 * 通知派发结果（ZS-MSG-001）——单收件人 × 单渠道的明确可查询结果。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「明确禁用模板、无渠道、接收人失效的可查询状态」。
 * 本结果承载该要求：调用方通过 {@link #getStatus()} 查询结果，无需捕获异常或判 null。
 *
 * <p>非 SUCCESS 状态均写入 {@code system_notify_send_log}（可追溯）；SUCCESS 状态额外关联
 * {@code messageId}（站内信）+ {@code outboxEventId}（Outbox 事件，供 MSG-002/004 消费）。
 */
@Value
@Builder
public class NotifyDispatchResult {

    /** 业务事件号（回传，供调用方关联） */
    String eventId;

    /** 收件人 */
    NotifyRecipient recipient;

    /** 渠道 */
    NotifyChannel channel;

    /** 派发状态（明确可查询） */
    NotifyDispatchStatus status;

    /** 状态原因（脱敏后，可空；不落敏感字段原文） */
    String statusReason;

    /** 发送日志 ID（system_notify_send_log.id，非空——所有状态均写日志） */
    Long sendLogId;

    /** 站内信消息 ID（system_notify_message.id，仅 SUCCESS 非空） */
    Long messageId;

    /** Outbox 事件 ID（outbox_event.id，仅 SUCCESS 非空；供 MSG-004 回执对账） */
    Long outboxEventId;

    public boolean isSuccess() {
        return status == NotifyDispatchStatus.SUCCESS;
    }

    public boolean isDuplicate() {
        return status == NotifyDispatchStatus.DUPLICATE_IGNORED;
    }

}
