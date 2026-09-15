package cn.zszj.module.system.service.notify.todo;

import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import lombok.Builder;
import lombok.Value;

/**
 * 业务待办流转命令（ZS-MSG-002）——事件驱动更新待办生命周期。
 *
 * <p>{@code eventId} 作 ZS-JOB-003 Inbox 幂等键（处理键 = 技术租户 + consumer({@code notify_todo}) + eventId）：
 * 同事件重复投递命中幂等不重复副作用；{@code bizVersion}（可解析整数时）经对象版本水位做乱序护栏，
 * 旧版本事件被拒（STALE_VERSION），<b>不复活已失效待办</b>。必须在业务事务内调用（Inbox MANDATORY）。
 */
@Value
@Builder(toBuilder = true)
public class NotifyTodoTransitionCmd {

    /** 事件 ID（Inbox 幂等键，必填） */
    String eventId;

    /** 流转类型（必填） */
    TodoTransitionType transitionType;

    /** 稳定业务任务 ID（定位待办，必填） */
    String todoKey;

    /** 业务来源（与 todoKey 共同定位待办，必填） */
    String sourceType;

    /** 关联业务类型（可空；版本乱序护栏按此 + bizId 分组） */
    String bizType;

    /** 关联业务编号（可空） */
    String bizId;

    /** 对象版本（可空；可解析整数时启用版本乱序护栏） */
    String bizVersion;

    /** 状态原因（脱敏，落 status_reason，可空） */
    String reason;

    /** 转派目标（REASSIGN 时更新 assignee，可空） */
    NotifyRecipient assignee;

    /** 事件主体类型（预留字段，当前不持久化；D-07 生产者接线时用于审计留痕） */
    OutboxActorType actorType;

    /** 事件主体编号（预留字段，当前不持久化；D-07 生产者接线时用于审计留痕） */
    String actorId;

    /** 链路追踪 ID（可空） */
    String traceId;

}
