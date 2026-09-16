package cn.zszj.module.system.service.notify.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 回执对账结果（ZS-MSG-004）——人工/运维对账回查的显式结论。
 *
 * <p>对账（docs/05 ZS-MSG-004「回执对账」）：对长期停留 ACCEPTED（回执迟迟未达）或 UNKNOWN 的记录
 * 主动向渠道回查，将渠道侧事实与台账对齐。与 Outbox 驱动的 UNKNOWN 自动回查不同，对账出口<b>不自动重发</b>：
 * 回查确认送达则推进 DELIVERED；确认未发出/仍未知则只留证据，由人工决定是否重试（避免绕过人工重试留痕）。
 */
@Getter
@AllArgsConstructor
public class NotifyChannelReconcileResult {

    public enum Outcome {
        /** 对账推进：渠道确认送达 → 台账 DELIVERED */
        RECONCILED_DELIVERED,
        /** 渠道确认未发出：证据已登记（ACCEPTED 态下属受理-未发矛盾，需人工核实渠道侧） */
        CONFIRMED_NOT_SENT,
        /** 仍未知：证据已登记，状态不变（可退避重查或人工处置） */
        STILL_UNKNOWN,
        /** 回查技术失败：状态不变 */
        QUERY_FAILED,
        /** 当前状态不适用对账（PENDING/DELIVERED/FAILED/记录不存在） */
        NOT_APPLICABLE
    }

    private final Outcome outcome;

    /** 台账记录 ID（NOT_APPLICABLE 因记录不存在时为 null） */
    private final Long sendId;

    /** 处理时台账状态 */
    private final String sendStatus;

    /** 受控依据/失败说明 */
    private final String evidence;

}
