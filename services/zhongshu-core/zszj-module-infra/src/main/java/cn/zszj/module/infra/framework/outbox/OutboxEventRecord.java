package cn.zszj.module.infra.framework.outbox;

import lombok.Value;

/**
 * dispatcher 领取到的事件（ZS-JOB-002，适配自供体同名类），at-least-once 投递给 {@link OutboxEventSink}。
 *
 * <p>补齐供体缺口：增加 {@code bizVersion}（对象版本）、{@code headers}（投递附带头 JSON 文本）、
 * {@code tenantId}（技术租户）、{@code retryCount}（已重试次数）、{@code actorType/actorId/traceId}
 * （主体与链路上下文）、{@code claimToken}（本次领取唯一凭证）——供体 RETURNING 未带回 tenant/header/
 * actor/trace 上下文，Sink 无法按事件合同投递，已按 docs/05 ZS-JOB-002 修复。
 */
@Value
public class OutboxEventRecord {

    long eventId;

    String eventType;

    String bizType;

    String bizId;

    /** 对象版本（可空） */
    String bizVersion;

    /** 载荷 JSON 文本 */
    String payload;

    /** 附带头 JSON 文本（可空） */
    String headers;

    /** 技术租户——dispatcher 投递时以此切换租户上下文（Sink 在事件租户内执行） */
    Long tenantId;

    /** 已重试次数（首次投递为 0） */
    int retryCount;

    /** 事件主体类型（USER/ADMIN/SYSTEM/WORKER） */
    String actorType;

    /** 事件主体编号（可空） */
    String actorId;

    /** 链路追踪 ID（可空） */
    String traceId;

    /** 领取者标识（instanceId@dispatcherName，可观测用） */
    String claimedBy;

    /** 本次领取唯一凭证——complete/fail 必须携带，过期旧执行者的旧凭证无法确认新领取（栅栏） */
    String claimToken;

}
