package cn.zszj.module.system.dal.dataobject.notify;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyDispatchStatus;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 通知发送日志 DO（ZS-MSG-001）——幂等硬约束 + 明确状态 + 绑定 Outbox 事件。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「同一事件/接收人重试不重复生成消息；禁用模板或无渠道返回明确状态」。
 * 本 DO 承载该要求：
 * <ul>
 *   <li>幂等硬约束：DB 唯一索引 {@code uk_notify_send_log_idempotent (tenant_id, event_id, recipient_type, recipient_id, channel)}；</li>
 *   <li>明确状态：{@link #status} 枚举承载 SUCCESS / DISABLED_TEMPLATE / NO_CHANNEL / RECIPIENT_INVALID 等；</li>
 *   <li>Outbox 绑定：{@link #outboxEventId} 关联 {@code outbox_event.id}（供 MSG-004 回执对账）。</li>
 * </ul>
 *
 * <p>循 JOB-002 outbox_event / AUDIT-001 audit_event 先例：只追加、不改写（无 deleted/updater/update_time 语义）；
 * tenant_id 取自 TenantContextHolder（不默认 0）。
 */
@TableName(value = "system_notify_send_log")
@KeySequence("system_notify_send_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifySendLogDO extends BaseDO {

    /** 发送日志编号，自增 */
    @TableId
    private Long id;

    /** 业务事件号（幂等键） */
    private String eventId;

    /** 收件人类型（ADMIN / MEMBER） */
    private String recipientType;

    /** 收件人编号 */
    private Long recipientId;

    /** 渠道（INBOX / SMS / EMAIL / PUSH） */
    private String channel;

    /** 模板编码 */
    private String templateCode;

    /** 模板编号（成功时关联 system_notify_template.id） */
    private Long templateId;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务编号 */
    private String bizId;

    /** 对象版本 */
    private String bizVersion;

    /** 派发状态 */
    private String status;

    /** 状态原因（脱敏后） */
    private String statusReason;

    /** 站内信消息 ID（成功时关联 system_notify_message.id） */
    private Long messageId;

    /** Outbox 事件 ID（成功时关联 outbox_event.id） */
    private Long outboxEventId;

    /** 事件主体类型 */
    private String actorType;

    /** 事件主体编号 */
    private String actorId;

    /** 链路追踪 ID */
    private String traceId;

    /** 技术租户（取自 TenantContextHolder） */
    private Long tenantId;

    /**
     * 逻辑删除（覆写字面量为 FALSE/TRUE）——全局 {@code @TableLogic} 为 0/1 数值字面量（application.yaml
     * logic-delete-value），而本表 deleted 列为 boolean（V20260915.003/.004 MSG 域惯例），MyBatis-Plus 注入方法
     * （selectById 等）在 PG 拼 {@code deleted = 0} 将报 {@code boolean = integer}（H2 bit 接受 0 故单测不暴露）；
     * 字段级覆写优先于全局配置，PG/H2 双方言可移植。收口 ZS-MSG-004 评审 r2 P2 登记的 MSG 域系统性问题
     * （{@code NotifyChannelSendDO} 已先行自愈，本表同款）。
     */
    @com.baomidou.mybatisplus.annotation.TableLogic(value = "FALSE", delval = "TRUE")
    private Boolean deleted;

}
