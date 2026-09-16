package cn.zszj.module.system.service.notify.dispatch;

import lombok.Value;

/**
 * 收件人上下文解析结果（ZS-MSG-001）。
 *
 * <p>B05 只解析 System 用户（AdminUserApi），校验存在/未停用/租户匹配；
 * 不查任职关系（D-09 前）。校验失败返回明确状态（不抛异常），供 {@link NotifyDispatcher} 消费。
 *
 * <p>ZS-MSG-004 扩展：解析时顺带携带联系方式（{@code mobile}/{@code email}，同一 AdminUserRespDTO
 * 取得，不增加查询），供非 INBOX 渠道创建发送台账使用；联系方式缺失不是收件人无效（用户本身有效），
 * 由渠道发送台账以 RECIPIENT_CONTACT_MISSING 明确阻断。
 */
@Value
public class NotifyRecipientContext {

    /** 原始收件人 */
    NotifyRecipient recipient;

    /** 是否有效 */
    boolean valid;

    /** 无效状态（valid=false 时非空：RECIPIENT_INVALID / RECIPIENT_TENANT_MISMATCH） */
    NotifyDispatchStatus invalidStatus;

    /** 无效原因（脱敏后，可空） */
    String invalidReason;

    /** 收件人所属租户（valid=true 时非空） */
    Long tenantId;

    /** 收件人手机号（valid=true 时可空——用户未绑定；ZS-MSG-004 渠道发送用） */
    String contactMobile;

    /** 收件人邮箱（valid=true 时可空——用户未绑定；ZS-MSG-004 渠道发送用） */
    String contactEmail;

    public static NotifyRecipientContext valid(NotifyRecipient recipient, Long tenantId) {
        return new NotifyRecipientContext(recipient, true, null, null, tenantId, null, null);
    }

    /** ZS-MSG-004：携联系方式的 valid 工厂（联系方式可空——用户未绑定不是收件人无效） */
    public static NotifyRecipientContext validWithContacts(NotifyRecipient recipient, Long tenantId,
                                                           String mobile, String email) {
        return new NotifyRecipientContext(recipient, true, null, null, tenantId, mobile, email);
    }

    public static NotifyRecipientContext invalid(NotifyRecipient recipient, NotifyDispatchStatus status, String reason) {
        return new NotifyRecipientContext(recipient, false, status, reason, null, null, null);
    }

}
