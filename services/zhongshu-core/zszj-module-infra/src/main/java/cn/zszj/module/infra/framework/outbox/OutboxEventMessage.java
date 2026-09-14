package cn.zszj.module.infra.framework.outbox;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Outbox 事件消息（ZS-JOB-002）——业务事务内经 {@link ReliableEventPort#append} 追加，随业务事务一起提交/回滚。
 *
 * <p>适配自供体 OutboxEventMessage（供体快照隔离于 {@code reference/donors/}，命名映射见 docs/06），按 docs/05
 * ZS-JOB-002 事件合同补齐供体缺口：增加 {@code bizVersion}（对象版本，消费者乱序/冲突策略依据，语义由事件类型
 * 解释，ZS-JOB-003 落地）、{@code actorType/actorId}（事件主体）、{@code traceId}（链路追踪）。
 *
 * <p>与供体的关键差异：<b>不含 tenantId 字段</b>——技术租户一律取自 {@code TenantContextHolder} 当前上下文
 * （append 时强制要求存在，缺失即拒绝，不默认写 0）；调用方无法传值，也就无法跨租户串用。
 */
@Value
@Builder
public class OutboxEventMessage {

    /** 事件类型（必填），如 USER_MESSAGE_SEND */
    String eventType;

    /** 关联业务类型（可空），如 infra_file */
    String bizType;

    /** 关联业务编号（可空） */
    String bizId;

    /** 对象版本（可空；消费者乱序/冲突策略依据，语义由事件类型解释） */
    String bizVersion;

    /** 事件载荷（消费者按事件类型解释；null 视为空对象 {}） */
    Map<String, Object> payload;

    /** 投递附带头（可空） */
    Map<String, String> headers;

    /** 事件主体类型（必填） */
    OutboxActorType actorType;

    /** 事件主体编号（可空，如用户 ID / 实例名） */
    String actorId;

    /** 链路追踪 ID（可空） */
    String traceId;

    /**
     * 事件主体类型——语义对齐 ZS-AUDIT-001 AuditPort 主体分类（USER/ADMIN/SYSTEM/WORKER）。
     */
    public enum OutboxActorType {
        USER, ADMIN, SYSTEM, WORKER
    }

}
