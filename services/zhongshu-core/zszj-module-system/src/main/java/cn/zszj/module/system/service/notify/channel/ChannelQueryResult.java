package cn.zszj.module.system.service.notify.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道回查三态结论（ZS-MSG-004）——「对超时不确定结果先回查再重发」的回查出口。
 */
@Getter
@AllArgsConstructor
public class ChannelQueryResult {

    public enum Outcome {
        /** 渠道确认已送达 */
        DELIVERED,
        /** 渠道确认未发出（无该幂等键的发送记录）——安全重发 */
        NOT_SENT,
        /** 仍无法确定——维持 UNKNOWN，退避后重查 */
        UNKNOWN
    }

    private final Outcome outcome;

    /** 回查依据（受控限长：渠道侧状态码/描述的受控摘要，落 last_query 留痕） */
    private final String evidence;

    public static ChannelQueryResult delivered(String evidence) {
        return new ChannelQueryResult(Outcome.DELIVERED, evidence);
    }

    public static ChannelQueryResult notSent(String evidence) {
        return new ChannelQueryResult(Outcome.NOT_SENT, evidence);
    }

    public static ChannelQueryResult unknown(String evidence) {
        return new ChannelQueryResult(Outcome.UNKNOWN, evidence);
    }

}
