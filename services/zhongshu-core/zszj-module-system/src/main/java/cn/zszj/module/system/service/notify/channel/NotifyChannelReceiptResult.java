package cn.zszj.module.system.service.notify.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道回执处理结果（ZS-MSG-004）——回执校验的显式结论（不抛异常、不返回 null，循 MSG-001 明确状态惯例）。
 */
@Getter
@AllArgsConstructor
public class NotifyChannelReceiptResult {

    public enum Outcome {
        /** 已应用：台账状态按回执推进（→DELIVERED / →FAILED） */
        APPLIED,
        /** 重复回执：状态已是终态且与回执一致，幂等吸收；receipt_count 已累加（可追踪） */
        DUPLICATE,
        /** 矛盾回执：状态已是终态且与回执矛盾（如 FAILED 后收到送达回执），仅登记不推进（人工核实） */
        CONTRADICTION,
        /** 未知回执：channelMessageId 无法定位台账记录（伪造/他系统回执），拒绝 */
        REJECTED_UNKNOWN_RECEIPT,
        /** 流水号错配：回执流水号与台账受理流水号不一致，拒绝（回执校验） */
        REJECTED_SERIAL_MISMATCH
    }

    private final Outcome outcome;

    /** 命中的台账记录 ID（REJECTED_UNKNOWN_RECEIPT 时为 null） */
    private final Long sendId;

    /** 处理时台账状态（REJECTED_UNKNOWN_RECEIPT 时为 null） */
    private final String sendStatus;

    public static NotifyChannelReceiptResult applied(Long sendId, String status) {
        return new NotifyChannelReceiptResult(Outcome.APPLIED, sendId, status);
    }

    public static NotifyChannelReceiptResult duplicate(Long sendId, String status) {
        return new NotifyChannelReceiptResult(Outcome.DUPLICATE, sendId, status);
    }

    public static NotifyChannelReceiptResult contradiction(Long sendId, String status) {
        return new NotifyChannelReceiptResult(Outcome.CONTRADICTION, sendId, status);
    }

    public static NotifyChannelReceiptResult rejectedUnknownReceipt() {
        return new NotifyChannelReceiptResult(Outcome.REJECTED_UNKNOWN_RECEIPT, null, null);
    }

    public static NotifyChannelReceiptResult rejectedSerialMismatch(Long sendId, String status) {
        return new NotifyChannelReceiptResult(Outcome.REJECTED_SERIAL_MISMATCH, sendId, status);
    }

}
