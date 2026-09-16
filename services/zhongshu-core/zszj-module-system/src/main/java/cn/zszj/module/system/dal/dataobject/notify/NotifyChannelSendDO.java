package cn.zszj.module.system.dal.dataobject.notify;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 渠道发送生命周期台账 DO（ZS-MSG-004）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-004）：「将待发送、已受理、送达、失败与站内消息状态分开；
 * 建立渠道幂等键、次数/退避、回执校验和人工重试」。本 DO 承载该要求：
 * <ul>
 *   <li>渠道幂等键：{@link #channelMessageId}（派发时生成、每次提交携带同一值）——超时重发/重试/重复事件
 *       到达渠道时可被按幂等键去重，回执按 (tenant, channel, channelMessageId) 唯一定位；</li>
 *   <li>次数：{@link #attemptCount}（实际提交渠道次数，只增不减）、{@link #receiptCount}（回执到达次数，
 *       重复回执可追踪）、{@link #manualRetryCount}（人工重试次数）；退避节奏由 Outbox
 *       {@code outbox_event.retry_count/next_retry_at} 承担（ZS-JOB-002 机制，本表不重复记账）；</li>
 *   <li>回执校验：回执按 {@link #channelMessageId} 定位 + {@link #channelSerialNo} 比对，未知/错配回执显式拒绝；</li>
 *   <li>与站内消息状态分开：本表状态只描述渠道投递，不含已读/未读、待办处理语义。</li>
 * </ul>
 *
 * <p>循 V20260915.003 NotifySendLogDO / V20260914.010 audit_event 惯用法：BaseDO、tenant_id 取自
 * TenantContextHolder（不默认 0）。{@code recipientContact}/{@code content} 落库（对齐 system_sms_log.content
 * 惯例），<b>日志输出一律经脱敏</b>（联系方式掩码、异常只留类别——循 JOB-002 describeThrowable 先例）。
 */
@TableName(value = "system_notify_channel_send")
@KeySequence("system_notify_channel_send_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyChannelSendDO extends BaseDO {

    /** 发送台账编号，自增 */
    @TableId
    private Long id;

    /** 技术租户（取自 TenantContextHolder） */
    private Long tenantId;

    /** 关联派发日志 system_notify_send_log.id（一比一） */
    private Long sendLogId;

    /** 业务事件号（继承自派发命令 eventId） */
    private String eventId;

    /** 渠道（SMS / EMAIL / PUSH） */
    private String channel;

    /** 收件人类型（ADMIN / MEMBER） */
    private String recipientType;

    /** 收件人编号 */
    private Long recipientId;

    /** 收件联系方式（短信=手机号 / 邮件=邮箱；缺失时记录直接 FAILED 且不入投递管道） */
    private String recipientContact;

    /** 模板编码 */
    private String templateCode;

    /** 渲染后的发送内容（派发时一次渲染，重试不改写） */
    private String content;

    /** 渠道幂等键（我方生成，提交/回查/回执统一携带；uk: tenant+channel+channel_message_id） */
    private String channelMessageId;

    /** 渠道发送状态（PENDING/ACCEPTED/DELIVERED/FAILED/UNKNOWN） */
    private String status;

    /** 状态说明（脱敏后） */
    private String statusReason;

    /** 渠道流水号（受理时返回，回执校验比对依据） */
    private String channelSerialNo;

    /** 失败码（渠道拒绝码 / 我方受控码，如 CHANNEL_NOT_CONFIGURED、RECIPIENT_CONTACT_MISSING） */
    private String failedCode;

    /** 实际提交渠道次数（只增不减） */
    private Integer attemptCount;

    /** 回执到达次数（重复回执可追踪，只增不减） */
    private Integer receiptCount;

    /** 人工重试次数（只增不减） */
    private Integer manualRetryCount;

    /** 受理时间 */
    private java.util.Date acceptedAt;

    /** 送达时间（回执或回查确认） */
    private java.util.Date deliveredAt;

    /** 最近一次回执到达时间 */
    private java.util.Date lastReceiptAt;

    /** 最近一次回查时间 */
    private java.util.Date lastQueryAt;

    /** 最近一次回查结论（受控枚举串：DELIVERED / NOT_SENT / UNKNOWN / QUERY_FAILED） */
    private String lastQueryResult;

    /** 最近一次技术失败描述（受控：异常类别等，不落原文） */
    private String lastError;

    /** 最近一次人工重试操作者类型 */
    private String lastRetryActorType;

    /** 最近一次人工重试操作者编号 */
    private String lastRetryActorId;

    /** 最近一次人工重试原因（脱敏后） */
    private String lastRetryReason;

    /** 最近一次投递 Outbox 事件 ID（NOTIFY_CHANNEL_SEND；人工重试后指向最新事件） */
    private Long outboxEventId;

    /** 派发主体类型（继承自命令，投递溯源） */
    private String actorType;

    /** 派发主体编号 */
    private String actorId;

    /** 链路追踪 ID */
    private String traceId;

}
