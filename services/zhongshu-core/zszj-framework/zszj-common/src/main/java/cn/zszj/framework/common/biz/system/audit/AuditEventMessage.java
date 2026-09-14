package cn.zszj.framework.common.biz.system.audit;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 审计事件消息（ZS-AUDIT-001）。
 *
 * <p>不可变值对象，经 {@link AuditPort#record(AuditEventMessage)} 同步落库。字段覆盖统一审计要求的
 * actor / 技术租户 / 对象 / 版本 / 原因 / 结果 / trace；其中 {@code bizVersion}、{@code reason}、{@code tenantId}、
 * {@code traceId}、{@code idempotencyKey} 为可空扩展点——{@code tenantId} 尤其<b>不默认 0</b>
 * （D-09 技术租户语义未定前只留扩展点，不伪造归属）。
 */
@Value
@Builder
public class AuditEventMessage {

    /** 操作主体类型。 */
    public enum ActorType {
        /** 前台用户（会员 / C 端）。 */
        USER,
        /** 后台管理员。 */
        ADMIN,
        /** 系统自身（无自然人触发的内部动作）。 */
        SYSTEM,
        /** 后台任务 / 调度 worker（{@code actorId} 记实例名）。 */
        WORKER
    }

    /** 审计结果。 */
    public enum AuditResult {
        /** 成功——随业务事务提交。 */
        SUCCESS,
        /** 失败——独立事务记录。 */
        FAILURE,
        /** 被拒绝——独立事务记录。 */
        DENIED
    }

    /** 事件类型，取自 {@link AuditEventTypes} 目录，如 {@code OBJECT_CREATED}（必填）。 */
    String eventType;

    /** 主体类型（必填）。 */
    ActorType actorType;

    /** 主体标识：用户 ID / 管理员 ID / worker 实例名。 */
    String actorId;

    /** 动作，如 CREATE、DISABLE、CONSUME、REVOKE。 */
    String action;

    /** 对象类型（业务实体名）。 */
    String bizType;

    /** 对象标识。 */
    String bizId;

    /** 对象版本（可空扩展点：用于前后值 / 乐观锁版本追溯，读取脱敏归 AUDIT-002）。 */
    String bizVersion;

    /** 原因（可空：拒绝 / 失败的原因说明，不含敏感明文）。 */
    String reason;

    /** 结果（必填）。 */
    AuditResult result;

    /** 审计明细（可空；不含敏感明文：掩码、哈希、过滤条件等）。 */
    Map<String, Object> detail;

    /** 技术租户（可空扩展点，<b>不默认 0</b>；D-09 未定前不伪造归属）。 */
    Long tenantId;

    /** 追踪 ID（可空：与请求链路 / 操作日志 trace 关联）。 */
    String traceId;

    /** 幂等键（可空；非空时携带同一键的重复事件不重复入账）。 */
    String idempotencyKey;

}
