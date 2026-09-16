package cn.zszj.module.infra.framework.outbox.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 队列/依赖<b>健康指示器</b>（ZS-OPS-002.B），贡献者名 {@code outboxQueue}（Spring Boot 剥 {@code HealthIndicator} 后缀）。
 *
 * <p>把 JOB-004 {@link OutboxHealthMonitor} 的越阈快照映射为 Spring Boot Actuator {@link Health} 状态，使
 * {@code /actuator/health}（聚合）与专用 {@code monitoring} 组能反映「队列积压 / DEAD / 依赖故障」——填补 JOB-004
 * 「仅按需拉取、健康端点不反映越阈」的缺口（docs/05 §16.1 L1033 退出条件「依赖故障和积压被发现」）。
 *
 * <p><b>状态映射</b>：无越阈 → {@link Health#up()}；有越阈 → {@link Health#down()} 携 breaches + 指标详情；
 * 探针抛出（依赖不可用）→ {@link Health#down()} 携 {@code MONITOR_PROBE_FAILED}，<b>绝不</b>伪装 UP（失败不静默，铁律 10）。
 *
 * <p><b>脱敏</b>：details 仅含指标值、机器可读越阈码与异常<b>类名</b>，<b>不</b>含 payload、异常原文消息（防依赖内部信息外泄）。
 *
 * <p><b>liveness 纯净红线</b>（铁律 9）：本指示器<b>只</b>汇入聚合 {@code /actuator/health} 与 {@code monitoring} 组，
 * 配置层<b>绝不</b>将 {@code outboxQueue} 纳入 {@code liveness} 组——队列积压是<b>可恢复</b>的运维信号而非进程失活，
 * 若进 liveness 会触发 K8s/编排器<b>重启风暴</b>（越积压越重启，恶性循环）。该边界由 application.yaml 分组配置保证。
 */
@Component
public class OutboxQueueHealthIndicator implements HealthIndicator {

    private final OutboxHealthMonitor monitor;

    public OutboxQueueHealthIndicator(OutboxHealthMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Health health() {
        OutboxHealthMetrics metrics;
        try {
            metrics = monitor.snapshot();
        } catch (RuntimeException ex) {
            // 失败不静默：依赖不可用 → DOWN 携探针失败标识 + 异常类名（脱敏，绝不含原文消息，绝不返回 UP）
            return Health.down()
                    .withDetail("reason", "MONITOR_PROBE_FAILED")
                    .withDetail("errorClass", ex.getClass().getName())
                    .build();
        }
        List<String> breaches = metrics.getBreaches();
        Health.Builder builder = (breaches == null || breaches.isEmpty()) ? Health.up() : Health.down();
        return builder
                .withDetail("pendingBacklog", metrics.getPendingBacklog())
                .withDetail("deadCount", metrics.getDeadCount())
                .withDetail("longestWaitSeconds", metrics.getLongestWaitSeconds())
                .withDetail("failureRatePercent", metrics.getFailureRatePercent())
                .withDetail("staleLeaseCount", metrics.getStaleLeaseCount())
                .withDetail("breaches", breaches)
                .build();
    }

}
