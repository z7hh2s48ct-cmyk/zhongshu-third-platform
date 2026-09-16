package cn.zszj.module.infra.framework.outbox.recovery;

import lombok.Builder;
import lombok.Value;

/**
 * Outbox 人工恢复命令（ZS-JOB-004）。
 *
 * <p>不可变命令对象。{@code retryDeadEvent}/{@code skipDeadEvent} 各自决定 {@link OutboxRecoveryAction}，
 * 故本命令不携带 action（由被调方法确定），只带恢复目标、<b>必填理由</b>与操作者上下文。
 *
 * <p>护栏（服务层校验，缺失即 fail-closed 拒绝）：
 * <ul>
 *   <li>{@code reason} 空白 → {@code OUTBOX_RECOVERY_REASON_REQUIRED}（拒绝无据操作）；</li>
 *   <li>{@code operatorId} 缺失 → 记 {@code ACCESS_DENIED}（独立事务）+ 抛
 *       {@code OUTBOX_RECOVERY_OPERATOR_REQUIRED}（拒绝匿名重放，验收「无权重放被拒」的服务层纵深防御；
 *       controller {@code @PreAuthorize('infra:job:recover')} 为第一道闸）；</li>
 *   <li>无 payload/headers/event_type 字段——恢复 API <b>结构性</b>不提供编辑历史入口（验收「修改历史被拒」）。</li>
 * </ul>
 */
@Value
@Builder
public class OutboxRecoveryCmd {

    /** 目标事件编号（{@code outbox_event.id}）。 */
    long eventId;

    /** 人工恢复理由（必填，落台账 + 审计；不得含敏感明文）。 */
    String reason;

    /** 操作者类型（ADMIN / SYSTEM，对齐 {@code AuditEventMessage.ActorType} 命名）。 */
    String operatorType;

    /** 操作者标识（缺失即拒绝匿名重放）。 */
    String operatorId;

    /** 链路追踪 ID（可空，关联请求链路）。 */
    String traceId;

}
