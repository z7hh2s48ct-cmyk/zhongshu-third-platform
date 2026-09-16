package cn.zszj.module.system.service.notify.dispatch;

/**
 * 通知渠道枚举（ZS-MSG-001；ZS-MSG-004 扩展渠道发送生命周期）。
 *
 * <p>{@link #INBOX} 为同步站内信（建消息即达）。{@link #SMS}/{@link #EMAIL}/{@link #PUSH} 自 ZS-MSG-004
 * 起接入统一投递管道：派发时经 {@code NotifyChannelSenderRegistry} 查找渠道发送器——
 * <ul>
 *   <li>有实现 → 建渠道发送台账（PENDING）+ 追加 {@code NOTIFY_CHANNEL_SEND} Outbox 事件，
 *       由可靠投递机制驱动「已受理→送达/失败」生命周期；</li>
 *   <li>无实现（B05 生产容器常态，真实渠道受 D-10 门禁）→ {@link NotifyDispatchStatus#CHANNEL_NOT_CONFIGURED}
 *       明确阻断（可查询、不静默丢弃）。</li>
 * </ul>
 * channels 为空 → {@link NotifyDispatchStatus#NO_CHANNEL}（未指定渠道，与「指定但未配置」语义区分）。
 */
public enum NotifyChannel {

    /** 站内信（同步建消息，无渠道发送生命周期） */
    INBOX,

    /** 短信（ZS-MSG-004 接入投递管道；真实发送器待 D-10 门禁后另立） */
    SMS,

    /** 邮件（ZS-MSG-004 接入投递管道；真实发送器待 D-10 门禁后另立） */
    EMAIL,

    /** 推送（ZS-MSG-004 接入投递管道；真实发送器待 D-10 门禁后另立） */
    PUSH

}
