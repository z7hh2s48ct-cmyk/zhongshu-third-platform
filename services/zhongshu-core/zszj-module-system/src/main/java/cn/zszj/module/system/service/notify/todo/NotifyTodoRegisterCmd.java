package cn.zszj.module.system.service.notify.todo;

import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import lombok.Builder;
import lombok.Value;

/**
 * 业务待办注册命令（ZS-MSG-002）。
 *
 * <p>{@code todoKey}（稳定业务任务 ID）+ {@code sourceType}（业务来源）+ {@code recipient}（收件人）必填；
 * 幂等键为 (技术租户, sourceType, todoKey)——重复注册返回既有待办，不重复建。
 * {@code messageId} 可空：待办与站内信松耦合关联（待办可无消息，消息可无待办）。
 */
@Value
@Builder(toBuilder = true)
public class NotifyTodoRegisterCmd {

    /** 稳定业务任务 ID（幂等键，必填） */
    String todoKey;

    /** 业务来源（BPM / FILE / GENERIC …，机制级，必填） */
    String sourceType;

    /** 关联业务类型（可空） */
    String bizType;

    /** 关联业务编号（可空） */
    String bizId;

    /** 对象版本（可空；乱序护栏依据） */
    String bizVersion;

    /** 待办标题（可空） */
    String title;

    /** 关联站内信 ID（可空，松耦合） */
    Long messageId;

    /** 收件人（必填） */
    NotifyRecipient recipient;

    /** 初始处理人（可空，转派目标） */
    NotifyRecipient assignee;

}
