package cn.zszj.module.system.service.notify.dispatch;

import lombok.Value;

/**
 * 收件人上下文解析结果（ZS-MSG-001）。
 *
 * <p>B05 只解析 System 用户（AdminUserApi），校验存在/未停用/租户匹配；
 * 不查任职关系（D-09 前）。校验失败返回明确状态（不抛异常），供 {@link NotifyDispatcher} 消费。
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

    public static NotifyRecipientContext valid(NotifyRecipient recipient, Long tenantId) {
        return new NotifyRecipientContext(recipient, true, null, null, tenantId);
    }

    public static NotifyRecipientContext invalid(NotifyRecipient recipient, NotifyDispatchStatus status, String reason) {
        return new NotifyRecipientContext(recipient, false, status, reason, null);
    }

}
