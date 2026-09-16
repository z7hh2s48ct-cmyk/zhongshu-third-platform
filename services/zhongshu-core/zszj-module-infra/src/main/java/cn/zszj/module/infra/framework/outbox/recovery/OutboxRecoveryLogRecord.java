package cn.zszj.module.infra.framework.outbox.recovery;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Outbox 人工恢复台账记录（ZS-JOB-004，映射 {@code outbox_recovery_log} 一行）。
 *
 * <p>不可变事实（只增不改），循 {@code outbox_event} 的 JdbcTemplate 映射范式（非 MyBatis DO）。
 * 每次人工 retry/skip 落一行，保留「恢复前后关联」：{@code beforeStatus}→{@code afterStatus}、
 * {@code beforeRetryCount}（恢复前自动重试计数）、{@code manualRetrySeq}（本事件第几次人工重试）。
 */
@Value
@Builder
public class OutboxRecoveryLogRecord {

    /** 台账主键。 */
    long id;

    /** 关联事件编号（{@code outbox_event.id}）。 */
    long eventId;

    /** 动作（RETRY / SKIP）。 */
    String action;

    /** 人工理由（必填，脱敏，不落敏感原文）。 */
    String reason;

    /** 恢复前状态（DEAD）。 */
    String beforeStatus;

    /** 恢复后状态（PENDING / SKIPPED）。 */
    String afterStatus;

    /** 恢复前自动重试计数（前后关联）。 */
    int beforeRetryCount;

    /** 本事件第几次人工重试（无限重试护栏依据）。 */
    int manualRetrySeq;

    /** 操作者类型（ADMIN / SYSTEM）。 */
    String operatorType;

    /** 操作者标识（可空）。 */
    String operatorId;

    /** 技术租户。 */
    Long tenantId;

    /** 链路追踪 ID（可空）。 */
    String traceId;

    /** 台账落库时间。 */
    LocalDateTime createTime;

}
