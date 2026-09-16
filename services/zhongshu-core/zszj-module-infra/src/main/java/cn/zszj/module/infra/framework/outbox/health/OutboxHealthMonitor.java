package cn.zszj.module.infra.framework.outbox.health;

/**
 * Outbox 健康监测器（ZS-JOB-004）：积压 / 最长等待 / 失败率 / 租约健康的系统级快照 + 越阈告警。
 *
 * <p>与恢复服务（{@code OutboxRecoveryService}，租户过滤的操作入口）分工：本监测器提供<b>系统级</b>运维/告警
 * 视图（不逐租户过滤，服务「统一告警」诉求，开发计划 §0.3-9 未将其列入租户过滤清单）。阈值经
 * {@link OutboxHealthProperties} 可配（保守占位，待 WP-19 实测回填），越阈项汇入 {@code breaches} 并由实现 WARN 告警。
 */
public interface OutboxHealthMonitor {

    /**
     * 计算当前健康快照：PENDING 积压 / DEAD 计数 / 最长等待 / 失败率 / 过期租约 + 越阈清单。
     *
     * @return 健康指标快照（{@code breaches} 非空即应告警）
     */
    OutboxHealthMetrics snapshot();

}
