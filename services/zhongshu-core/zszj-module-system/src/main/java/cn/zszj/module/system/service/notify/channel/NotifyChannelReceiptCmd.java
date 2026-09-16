package cn.zszj.module.system.service.notify.channel;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * 渠道回执命令（ZS-MSG-004）——回执校验与状态推进的入参。
 *
 * <p>众墅要求：「回执校验」「模拟渠道…重复回执可追踪且不重复发件」。回执以
 * {@code channelMessageId}（渠道幂等键）定位台账记录，以 {@code channelSerialNo}（可选）做强校验；
 * 回执只推进台账状态，<b>永不触发发件</b>。真实渠道回调 HTTP 端点（含签名校验、租户解析）归
 * D-10/B11 真实渠道接入时另立——B05 以服务级合同 + 测试驱动锁定语义。
 */
@Value
@Builder
public class NotifyChannelReceiptCmd {

    /** 渠道（SMS / EMAIL / PUSH） */
    private String channel;

    /** 渠道幂等键（回执定位键，必填） */
    private String channelMessageId;

    /** 渠道流水号（可选；与台账受理流水号不一致 → 校验拒绝） */
    private String channelSerialNo;

    /** 是否送达：true=送达回执；false=失败回执 */
    private boolean delivered;

    /** 渠道失败码（失败回执时；受控码） */
    private String errorCode;

    /** 受控失败说明（脱敏后，不落回调原文） */
    private String errorMsg;

    /** 回执时间（可空，缺省取当前时间） */
    private Date receiptTime;

}
