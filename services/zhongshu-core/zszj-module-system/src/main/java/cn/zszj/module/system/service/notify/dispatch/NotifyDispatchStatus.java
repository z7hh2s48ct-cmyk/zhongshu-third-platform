package cn.zszj.module.system.service.notify.dispatch;

/**
 * 通知派发状态枚举（ZS-MSG-001）——明确可查询状态，区别于「null / 抛异常」的模糊语义。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「明确禁用模板、无渠道、接收人失效的可查询状态」。
 * 本枚举承载该要求：调用方通过 {@link NotifyDispatchResult#getStatus()} 查询结果，
 * 无需捕获异常或判 null；非 SUCCESS 状态均写入 {@code system_notify_send_log}（可追溯）。
 */
public enum NotifyDispatchStatus {

    /** 成功：消息已生成 + 发送日志已写 + Outbox 事件已追加（与业务写同事务） */
    SUCCESS,

    /** 模板已禁用：不生成消息、不入 Outbox；发送日志记录状态（可查询） */
    DISABLED_TEMPLATE,

    /** 无可用渠道：channels 为空或不含 B05 支持的 INBOX；不生成消息 */
    NO_CHANNEL,

    /** 收件人无效：不存在或已停用；不生成消息 */
    RECIPIENT_INVALID,

    /** 收件人租户不匹配：跨技术租户；不生成消息（循 JOB-002 outbox_event 租户强制先例） */
    RECIPIENT_TENANT_MISMATCH,

    /** 模板参数缺失：不生成消息 */
    TEMPLATE_PARAM_MISSING,

    /** 模板不存在：不生成消息 */
    TEMPLATE_NOT_FOUND,

    /** 重复忽略：同一 eventId + 收件人 + 渠道已存在发送日志（幂等硬兜底）；返回既有 sendLogId */
    DUPLICATE_IGNORED

}
