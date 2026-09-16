package cn.zszj.framework.common.biz.system.audit;

/**
 * 关键审计事件目录（ZS-AUDIT-001）。
 *
 * <p>AUDIT-001 交付<b>机制 + 目录骨架</b>：以下为跨模块通用的种子事件类型，覆盖「对象生命周期 / 访问拒绝 /
 * 后台执行」三类必须留痕的动作。各业务模块接线审计时，在此按 {@code <域>_<对象>_<动作>} 命名增补自己的事件类型
 * 常量（如后续批次的 {@code ACCESS_CODE_ISSUED}、{@code TENANT_PACKAGE_CHANGED}），目录随接线增长而非一次臆造。
 *
 * <p>用常量而非枚举：便于跨模块扩展而不反向依赖，事件类型作为字符串落 {@code audit_event.event_type}。
 */
public final class AuditEventTypes {

    private AuditEventTypes() {
    }

    /** 业务对象创建成功。 */
    public static final String OBJECT_CREATED = "OBJECT_CREATED";

    /** 业务对象变更成功。 */
    public static final String OBJECT_UPDATED = "OBJECT_UPDATED";

    /** 业务对象删除 / 逻辑删除成功。 */
    public static final String OBJECT_DELETED = "OBJECT_DELETED";

    /** 访问 / 操作被拒绝（无权限、越权、门控关闭等）。 */
    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    /** 后台任务 / 调度执行（WORKER / SYSTEM 主体）。 */
    public static final String BACKGROUND_EXECUTION = "BACKGROUND_EXECUTION";

    /** 参数配置恢复成功（ZS-CFG-004 B04：配置变更审计与审查后恢复流程接线）。 */
    public static final String CONFIG_PARAM_RESTORED = "CONFIG_PARAM_RESTORED";

    /** Outbox 事件人工重试成功（ZS-JOB-004：DEAD→PENDING 授权恢复）。 */
    public static final String OUTBOX_EVENT_RETRIED = "OUTBOX_EVENT_RETRIED";

    /** Outbox 事件人工跳过成功（ZS-JOB-004：DEAD→SKIPPED 授权放弃）。 */
    public static final String OUTBOX_EVENT_SKIPPED = "OUTBOX_EVENT_SKIPPED";

}
