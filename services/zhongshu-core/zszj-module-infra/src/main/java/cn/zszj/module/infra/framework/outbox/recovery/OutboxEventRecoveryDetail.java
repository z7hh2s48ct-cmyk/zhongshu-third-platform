package cn.zszj.module.infra.framework.outbox.recovery;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 事件恢复回查详情（ZS-JOB-004，只读 VO）。
 *
 * <p>供恢复控制台「查看 / 回查」呈现，两处脱敏红线（对齐 {@code OutboxDispatcherService#describeThrowable}）：
 * <ul>
 *   <li><b>异常摘要不落原文</b>：{@code errorCategory/errorClass/messageLength} 为受控摘要——我方常量码
 *       （如 {@code NO_SINK_SUPPORTS_EVENT_TYPE}）直显为 {@code errorCategory}，未知异常显
 *       {@code errorClass} + {@code messageLength}，<b>绝不</b>回退到落异常原文/堆栈；</li>
 *   <li><b>敏感 payload 掩码</b>：{@code maskedPayload} 对命中敏感键模式的字段掩码
 *       （{@code OutboxPayloadDesensitizer}），原文不出现在回查结果。</li>
 * </ul>
 *
 * <p>{@code recoveryHistory} 为该事件的恢复台账（前后关联），供追溯历次人工处置。
 */
@Value
@Builder
public class OutboxEventRecoveryDetail {

    /** 事件编号。 */
    long eventId;

    /** 事件类型。 */
    String eventType;

    /** 业务对象类型。 */
    String bizType;

    /** 业务对象标识。 */
    String bizId;

    /** 当前状态（PENDING / DISPATCHED / DEAD / SKIPPED）。 */
    String status;

    /** 自动重试计数（历史保留，人工恢复不清零）。 */
    int retryCount;

    /** 本事件累计人工重试次数（无限重试护栏依据）。 */
    int manualRetrySeq;

    /** 受控异常类别（常量码直显 / 未知异常派生类别），无异常原文。 */
    String errorCategory;

    /** 受控异常类名（{@code describeThrowable} 落库的 errorClass），无异常原文。 */
    String errorClass;

    /** 受控异常消息长度（{@code describeThrowable} 落库的 messageLength）。 */
    int messageLength;

    /** 脱敏后的 payload（敏感键掩码；解析失败则整体掩码，绝不回显原文）。 */
    String maskedPayload;

    /** 恢复台账历史（前后关联）。 */
    List<OutboxRecoveryLogRecord> recoveryHistory;

    /** 技术租户。 */
    Long tenantId;

    /** 事件创建时间。 */
    LocalDateTime createTime;

}
