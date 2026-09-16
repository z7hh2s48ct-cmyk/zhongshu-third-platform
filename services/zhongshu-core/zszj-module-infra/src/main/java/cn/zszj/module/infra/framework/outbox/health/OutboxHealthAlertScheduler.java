package cn.zszj.module.infra.framework.outbox.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Outbox 队列/依赖<b>周期告警探针</b>（ZS-OPS-002.B）。
 *
 * <p>JOB-004 {@link OutboxHealthMonitor} 的越阈计算（积压/DEAD/最长等待/失败率/租约 → breaches）已由
 * {@code OutboxHealthMonitorImpl} 落地，但仅被 {@code OutboxEventController} <b>按需</b>拉取——无人查询则越阈静默。
 * 本探针按 {@code infra.outbox.health.alert.interval-ms} 周期主动 {@code snapshot()}，把越阈<b>分级告警</b>到日志
 * （供采集/告警系统消费），填补「积压/依赖故障被发现」的退出条件（docs/05 §16.1 L1033）。
 *
 * <p><b>失败不静默</b>（退出条件核心）三层保证：
 * <ol>
 *   <li>{@code snapshot()} 抛出（依赖不可用）→ ERROR 明示「探针失败」携异常类名，<b>不吞、不伪装健康</b>；</li>
 *   <li>异常被就地隔离，<b>不击穿 {@code @Scheduled} 调度线程</b>（否则后续轮次永久静默）；</li>
 *   <li>越阈必告警：严重级（{@link #CRITICAL_CODES}）→ ERROR，预警级/未知码 → WARN（保守不误升级）。</li>
 * </ol>
 *
 * <p><b>系统级</b>：不加租户过滤（循 {@code OutboxHealthMonitorImpl} §0.3-9，服务统一告警诉求）。
 * <b>阈值红线</b>：越阈码与阈值均来自 JOB-004，本探针<b>不</b>新造阈值、<b>不</b>声称任何未经实测的容量/TPS（铁律 8）。
 * <b>门控</b>：{@code @ConditionalOnProperty(alert.enabled=true)} 使装配可关；未启用时不产任何调度开销。
 */
@Component
@ConditionalOnProperty(prefix = "infra.outbox.health.alert", name = "enabled", havingValue = "true")
@Slf4j
public class OutboxHealthAlertScheduler {

    /**
     * 严重级越阈码（→ ERROR）：需人工立即介入的停摆/丢数征兆。
     * 复用 JOB-004 {@code OutboxHealthMonitorImpl#evaluateBreaches} 产出的常量码，<b>不</b>新造。
     * 未列入本集合的码（含 JOB-004 未来新增）一律降级为 WARN，保守不误升级为 ERROR。
     */
    private static final Set<String> CRITICAL_CODES =
            Set.of("PENDING_BACKLOG_CRITICAL", "DEAD_EVENTS_PRESENT", "STALE_LEASE_PRESENT");

    private final OutboxHealthMonitor monitor;

    public OutboxHealthAlertScheduler(OutboxHealthMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * 周期探针：拉取健康快照 → 按越阈严重度分级告警。异常就地隔离，绝不向外抛出（保护调度线程）。
     */
    @Scheduled(fixedRateString = "${infra.outbox.health.alert.interval-ms:60000}")
    public void probe() {
        OutboxHealthMetrics metrics;
        try {
            metrics = monitor.snapshot();
        } catch (RuntimeException ex) {
            // 失败不静默：依赖不可用时明示探针失败（脱敏——仅异常类名，不外泄原文），且不击穿调度线程
            log.error("[probe][Outbox 健康监测探针失败，依赖可能不可用 errorClass={}]", ex.getClass().getName(), ex);
            return;
        }
        List<String> breaches = metrics.getBreaches();
        if (breaches == null || breaches.isEmpty()) {
            return; // 健康：不产告警噪声
        }
        if (hasCritical(breaches)) {
            log.error("[probe][Outbox 队列告警(严重) breaches={} pendingBacklog={} deadCount={} "
                            + "longestWaitSeconds={} failureRatePercent={} staleLeaseCount={}]",
                    breaches, metrics.getPendingBacklog(), metrics.getDeadCount(), metrics.getLongestWaitSeconds(),
                    metrics.getFailureRatePercent(), metrics.getStaleLeaseCount());
        } else {
            log.warn("[probe][Outbox 队列告警(预警) breaches={} pendingBacklog={} deadCount={} "
                            + "longestWaitSeconds={} failureRatePercent={} staleLeaseCount={}]",
                    breaches, metrics.getPendingBacklog(), metrics.getDeadCount(), metrics.getLongestWaitSeconds(),
                    metrics.getFailureRatePercent(), metrics.getStaleLeaseCount());
        }
    }

    /** 越阈清单是否含任一严重级码（取最高严重度告警，不漏报严重项）。 */
    private boolean hasCritical(List<String> breaches) {
        return breaches.stream().anyMatch(CRITICAL_CODES::contains);
    }

}
