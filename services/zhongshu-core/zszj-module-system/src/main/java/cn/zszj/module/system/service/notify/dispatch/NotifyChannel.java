package cn.zszj.module.system.service.notify.dispatch;

/**
 * 通知渠道枚举（ZS-MSG-001）。
 *
 * <p>B05 只实现 {@link #INBOX}（站内信）；SMS/EMAIL/PUSH 待 MSG-004 + D-10 门禁后另立。
 * 调用方传入不支持的渠道 → {@link NotifyDispatchStatus#NO_CHANNEL}（明确可查询状态，不抛异常）。
 */
public enum NotifyChannel {

    /** 站内信（B05 唯一支持的渠道） */
    INBOX,

    /** 短信（待 MSG-004 + D-10） */
    SMS,

    /** 邮件（待 MSG-004 + D-10） */
    EMAIL,

    /** 推送（待 MSG-004 + D-10） */
    PUSH

}
