package cn.zszj.module.infra.framework.outbox.health;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Outbox 健康监测快照（ZS-JOB-004，系统级运维/告警视图）。
 *
 * <p>覆盖 docs/05 ZS-JOB-004「积压 / 最长等待 / 失败率 / 租约健康」四类指标：
 * <ul>
 *   <li>{@code pendingBacklog}：PENDING 积压计数；</li>
 *   <li>{@code deadCount}：DEAD 计数（连续失败达上限，需人工恢复——验收①「进入 DEAD 并被发现」）；</li>
 *   <li>{@code longestWaitSeconds}：最老 PENDING 事件 {@code next_retry_at} 距今秒数（积压时长）；</li>
 *   <li>{@code failureRatePercent}：DEAD /（DISPATCHED+DEAD+SKIPPED）× 100（失败率，分母为 0 时取 0）；</li>
 *   <li>{@code staleLeaseCount}：{@code dispatcher_lease} 已过期（{@code lease_expires_at < now}）计数；</li>
 *   <li>{@code breaches}：越阈清单（机器可读常量码，如 {@code PENDING_BACKLOG_WARN}），非空即应告警。</li>
 * </ul>
 *
 * <p><b>反模式规避</b>：阈值经 {@link OutboxHealthProperties} 可配且为保守占位（待 WP-19 实测回填）；
 * 本快照只反映「监测逻辑正确计算 + 越阈告警」，<b>不</b>声称任何未经实测的容量/TPS 达标（验收④）。
 */
@Value
@Builder
public class OutboxHealthMetrics {

    /** PENDING 积压计数。 */
    int pendingBacklog;

    /** DEAD 计数（需人工恢复）。 */
    int deadCount;

    /** 最老 PENDING 事件距今等待秒数（无 PENDING 时为 0）。 */
    long longestWaitSeconds;

    /** 失败率百分比（DEAD /（DISPATCHED+DEAD+SKIPPED）× 100；分母 0 时为 0）。 */
    double failureRatePercent;

    /** 过期租约计数（dispatcher 停摆征兆）。 */
    int staleLeaseCount;

    /** 越阈清单（机器可读常量码）；非空即应告警。 */
    List<String> breaches;

}
