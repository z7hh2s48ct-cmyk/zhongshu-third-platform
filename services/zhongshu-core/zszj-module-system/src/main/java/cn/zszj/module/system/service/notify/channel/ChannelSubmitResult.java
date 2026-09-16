package cn.zszj.module.system.service.notify.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道提交三态结果（ZS-MSG-004）。
 *
 * <p>ACCEPTED 仅表示<b>渠道受理</b>（入队/网关接收），送达以回执/回查为准——「不能仅由入队成功推定送达」
 * （docs/05 ZS-MSG-004 现状条款）。
 */
@Getter
@AllArgsConstructor
public class ChannelSubmitResult {

    public enum Outcome {
        /** 渠道已受理：channelSerialNo 应非空（回执校验比对依据） */
        ACCEPTED,
        /** 渠道明确拒绝：终局失败（如号码无效、模板未报备），不再自动重试，走人工处置 */
        REJECTED,
        /** 结果未知：超时/响应不可判——必须先回查再决定重发 */
        UNKNOWN
    }

    private final Outcome outcome;

    /** 渠道流水号（ACCEPTED 时） */
    private final String channelSerialNo;

    /** 渠道拒绝码 / 未知原因受控码（REJECTED/UNKNOWN 时；脱敏后的我方或渠道受控码，不落响应原文） */
    private final String code;

    /** 受控说明（脱敏后，不落渠道响应原文——自由文本无可靠值级脱敏，循 JOB-002 先例） */
    private final String reason;

    public static ChannelSubmitResult accepted(String channelSerialNo) {
        return new ChannelSubmitResult(Outcome.ACCEPTED, channelSerialNo, null, null);
    }

    public static ChannelSubmitResult rejected(String code, String reason) {
        return new ChannelSubmitResult(Outcome.REJECTED, null, code, reason);
    }

    public static ChannelSubmitResult unknown(String code, String reason) {
        return new ChannelSubmitResult(Outcome.UNKNOWN, null, code, reason);
    }

}
