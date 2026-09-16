package cn.zszj.module.infra.framework.outbox.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OutboxQueueHealthIndicator} 单元测试（ZS-OPS-002.B）。
 *
 * <p>本指示器是 JOB-004 {@link OutboxHealthMonitor} 的<b>纯消费者</b>——越阈计算（积压/DEAD/失败率/租约 → breaches）
 * 已由 {@code OutboxHealthMonitorTest}（H2 真实种子）验证为单一真源；本测试聚焦 OPS-002.B 新增的<b>健康态映射</b>：
 * ①健康（breaches 空）→ {@link Status#UP}②越阈 → {@link Status#DOWN} 且 details 携 breaches/指标值
 * ③<b>失败不静默</b>（{@code snapshot()} 抛出 → {@link Status#DOWN} 携 {@code MONITOR_PROBE_FAILED}，<b>绝不</b>伪装 UP）
 * ④details 脱敏（不含 payload/异常原文，仅指标 + 码 + 异常类名）。
 *
 * <p>故以 lambda stub 注入固定 {@link OutboxHealthMetrics}（{@code OutboxHealthMonitor} 为单方法接口），隔离被测单元。
 *
 * <p><b>liveness 纯净红线</b>：本指示器贡献者名为 {@code outboxQueue}，仅汇入聚合 {@code /actuator/health} 与
 * 专用 {@code monitoring} 组；配置层<b>绝不</b>将其纳入 {@code liveness} 组（避免队列积压触发 K8s 重启风暴）。
 * 该边界由 application.yaml 分组配置 + 运行说明 + codex 评审保证，非本单测范畴。
 *
 * <p>覆盖 docs/05 ZS-OPS-002.B 退出条件「依赖故障和积压被发现、失败不静默」+ §16.1 L1033。
 */
public class OutboxQueueHealthIndicatorTest {

    /** 构造返回固定越阈清单与指标的 stub 监测器（系统级，无租户）。 */
    private OutboxHealthMonitor monitorWith(List<String> breaches, int pendingBacklog, int deadCount,
                                            long longestWaitSeconds, double failureRatePercent, int staleLeaseCount) {
        return () -> OutboxHealthMetrics.builder()
                .pendingBacklog(pendingBacklog)
                .deadCount(deadCount)
                .longestWaitSeconds(longestWaitSeconds)
                .failureRatePercent(failureRatePercent)
                .staleLeaseCount(staleLeaseCount)
                .breaches(breaches)
                .build();
    }

    /** 用例 H1：健康快照（breaches 空）→ {@link Status#UP}，details 携指标供观测。 */
    @Test
    public void test_健康_UP() {
        OutboxQueueHealthIndicator indicator =
                new OutboxQueueHealthIndicator(monitorWith(List.of(), 0, 0, 0L, 0.0, 0));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus(), "无越阈时健康态应为 UP");
        assertNotNull(health.getDetails().get("pendingBacklog"), "details 应携积压指标供观测");
    }

    /** 用例 H2：越阈（DEAD 存在）→ {@link Status#DOWN}，details 携 breaches 清单 + 指标值供运维定位。 */
    @Test
    public void test_越阈_DOWN携详情() {
        OutboxQueueHealthIndicator indicator =
                new OutboxQueueHealthIndicator(monitorWith(List.of("DEAD_EVENTS_PRESENT"), 0, 3, 0L, 12.5, 0));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus(), "存在越阈（DEAD）时健康态应为 DOWN（被发现）");
        Map<String, Object> details = health.getDetails();
        assertTrue(String.valueOf(details.get("breaches")).contains("DEAD_EVENTS_PRESENT"),
                "details.breaches 应携越阈码，实际=" + details.get("breaches"));
        assertEquals(3, details.get("deadCount"), "details 应携 DEAD 计数供定位");
    }

    /**
     * 用例 H3（退出条件「失败不静默」核心）：{@code snapshot()} 抛出（依赖不可用）→
     * {@link Status#DOWN} 携 {@code MONITOR_PROBE_FAILED}，<b>绝不</b>返回 UP（不吞异常、不伪装健康）。
     */
    @Test
    public void test_探针失败_DOWN不静默UP() {
        OutboxHealthMonitor failing = () -> {
            throw new IllegalStateException("db unavailable");
        };
        OutboxQueueHealthIndicator indicator = new OutboxQueueHealthIndicator(failing);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus(), "监测探针失败时健康态必须 DOWN（失败不静默，绝不伪装 UP）");
        assertEquals("MONITOR_PROBE_FAILED", health.getDetails().get("reason"),
                "details.reason 应明示探针失败，实际=" + health.getDetails());
    }

    /** 用例 H4：探针失败时 details 脱敏——仅携异常类名，不含异常原文消息（防依赖内部信息外泄）。 */
    @Test
    public void test_探针失败_details脱敏() {
        OutboxHealthMonitor failing = () -> {
            throw new IllegalStateException("jdbc pool exhausted: user=secret password=hunter2");
        };
        OutboxQueueHealthIndicator indicator = new OutboxQueueHealthIndicator(failing);

        Health health = indicator.health();
        String detailsText = String.valueOf(health.getDetails());

        assertTrue(detailsText.contains("IllegalStateException"), "details 应携异常类名供定位");
        assertFalse(detailsText.contains("hunter2"), "details 不得外泄异常原文中的敏感信息（脱敏：类名而非原文）");
    }

}
