package cn.zszj.module.infra.framework.outbox.health;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OutboxHealthAlertScheduler} 单元测试（ZS-OPS-002.B）。
 *
 * <p>本探针是 JOB-004 {@link OutboxHealthMonitor} 的<b>纯消费者</b>——越阈计算（积压/DEAD/失败率/租约 → breaches）
 * 已由 {@code OutboxHealthMonitorTest}（H2 真实种子）验证为单一真源；本测试聚焦 OPS-002.B 新增的<b>告警消费逻辑</b>：
 * ①按越阈严重度分级（CRITICAL/DEAD/STALE → ERROR；WARN 类 → WARN）②<b>失败不静默</b>（{@code snapshot()} 抛出 →
 * ERROR 明示「探针失败」，且不击穿调度线程）③健康时无告警。故以 lambda stub 注入固定 {@link OutboxHealthMetrics}
 * （{@code OutboxHealthMonitor} 为单方法接口），隔离被测单元、不重复验证 JOB-004 聚合逻辑。
 *
 * <p>日志断言用 logback {@link ListAppender} 捕获（Spring Boot 默认 logback-classic 在测试类路径），
 * 避免为可测性引入额外 {@code AlertSink} 抽象（YAGNI）。
 *
 * <p>覆盖 docs/05 ZS-OPS-002.B 退出条件「积压被发现、失败不静默」+ §16.1 L1033。
 */
public class OutboxHealthAlertSchedulerTest {

    private ListAppender<ILoggingEvent> appender;

    private Logger schedulerLogger;

    @BeforeEach
    public void setUp() {
        schedulerLogger = (Logger) LoggerFactory.getLogger(OutboxHealthAlertScheduler.class);
        appender = new ListAppender<>();
        appender.start();
        schedulerLogger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        schedulerLogger.detachAppender(appender);
        appender.stop();
    }

    /** 构造返回固定越阈清单的 stub 监测器（系统级，无租户）。 */
    private OutboxHealthMonitor monitorWith(List<String> breaches) {
        return () -> OutboxHealthMetrics.builder()
                .pendingBacklog(0)
                .deadCount(breaches.contains("DEAD_EVENTS_PRESENT") ? 1 : 0)
                .longestWaitSeconds(0L)
                .failureRatePercent(0.0)
                .staleLeaseCount(breaches.contains("STALE_LEASE_PRESENT") ? 1 : 0)
                .breaches(breaches)
                .build();
    }

    private List<ILoggingEvent> eventsAt(Level level) {
        return appender.list.stream().filter(e -> e.getLevel() == level).toList();
    }

    /** 用例 S1：含 CRITICAL 级越阈码（DEAD_EVENTS_PRESENT）→ ERROR 告警，日志携越阈码。 */
    @Test
    public void test_严重越阈_ERROR告警() {
        OutboxHealthAlertScheduler scheduler = new OutboxHealthAlertScheduler(monitorWith(List.of("DEAD_EVENTS_PRESENT")));

        scheduler.probe();

        List<ILoggingEvent> errors = eventsAt(Level.ERROR);
        assertEquals(1, errors.size(), "DEAD 存在（CRITICAL 级）应产出一条 ERROR 告警");
        assertTrue(errors.get(0).getFormattedMessage().contains("DEAD_EVENTS_PRESENT"),
                "ERROR 告警应携越阈码供运维定位，实际=" + errors.get(0).getFormattedMessage());
    }

    /** 用例 S2：仅 WARN 级越阈码（LONGEST_WAIT_WARN）→ WARN 告警（不升级为 ERROR）。 */
    @Test
    public void test_预警越阈_WARN告警() {
        OutboxHealthAlertScheduler scheduler = new OutboxHealthAlertScheduler(monitorWith(List.of("LONGEST_WAIT_WARN")));

        scheduler.probe();

        assertEquals(1, eventsAt(Level.WARN).size(), "软阈越界应产出 WARN 预警");
        assertEquals(0, eventsAt(Level.ERROR).size(), "仅 WARN 类越阈不应升级为 ERROR");
    }

    /** 用例 S3：健康（breaches 空）→ 不产 ERROR/WARN 告警（避免噪声）。 */
    @Test
    public void test_健康无告警() {
        OutboxHealthAlertScheduler scheduler = new OutboxHealthAlertScheduler(monitorWith(List.of()));

        scheduler.probe();

        assertEquals(0, eventsAt(Level.ERROR).size(), "健康时不应有 ERROR");
        assertEquals(0, eventsAt(Level.WARN).size(), "健康时不应有 WARN 告警噪声");
    }

    /**
     * 用例 S4（退出条件「失败不静默」核心）：{@code snapshot()} 抛出（依赖不可用）→ 探针
     * ①<b>不击穿调度线程</b>（不向外抛异常）②ERROR 明示「探针失败」携异常类名（不吞、不伪装健康）
     * ③<b>脱敏</b>（codex r0 P2：日志事件不携异常对象——SLF4J 渲染 throwable 会输出完整消息/堆栈/cause 链，
     * 连接诊断文本可能携内部地址/凭据；断言 throwable 代理为空且原文消息不出现于渲染结果）。
     */
    @Test
    public void test_探针异常_不静默_不击穿调度() {
        OutboxHealthMonitor failing = () -> {
            throw new IllegalStateException("jdbc:secret@db-host-internal/db?password=hunter2");
        };
        OutboxHealthAlertScheduler scheduler = new OutboxHealthAlertScheduler(failing);

        assertDoesNotThrow(scheduler::probe, "探针异常必须被隔离，不得击穿 @Scheduled 调度线程（否则后续轮次永久静默）");

        List<ILoggingEvent> errors = eventsAt(Level.ERROR);
        assertEquals(1, errors.size(), "探针失败应产出 ERROR 明示（失败不静默）");
        ILoggingEvent event = errors.get(0);
        assertTrue(event.getFormattedMessage().contains("IllegalStateException"),
                "ERROR 应携异常类名供定位（脱敏：类名而非原文消息），实际=" + event.getFormattedMessage());
        assertNull(event.getThrowableProxy(),
                "日志事件不得携异常对象（渲染 throwable 会泄露完整消息/堆栈/cause 链）");
        assertFalse(event.getFormattedMessage().contains("hunter2"),
                "渲染结果不得包含异常原文中的敏感信息，实际=" + event.getFormattedMessage());
    }

    /** 用例 S5：CRITICAL 与 WARN 越阈码并存 → 取最高严重度 ERROR（不漏报严重项）。 */
    @Test
    public void test_严重与预警并存_取最高级ERROR() {
        OutboxHealthAlertScheduler scheduler =
                new OutboxHealthAlertScheduler(monitorWith(List.of("LONGEST_WAIT_WARN", "STALE_LEASE_PRESENT")));

        scheduler.probe();

        assertEquals(1, eventsAt(Level.ERROR).size(), "并存 STALE_LEASE_PRESENT（CRITICAL 级）应升级为 ERROR");
        assertEquals(0, eventsAt(Level.WARN).size(), "已按最高级 ERROR 告警，不重复产 WARN");
    }

    /** 用例 S6：未知越阈码（JOB-004 未来新增、未列入 CRITICAL 集合）→ 保守降级 WARN，不误升级 ERROR。 */
    @Test
    public void test_未知越阈码_降级为WARN不误判严重() {
        // 前向兼容：JOB-004 若新增未列入 CRITICAL 集合的码，默认按 WARN 处理（保守，不误升级为 ERROR）
        OutboxHealthAlertScheduler scheduler = new OutboxHealthAlertScheduler(monitorWith(List.of("SOME_FUTURE_WARN")));

        scheduler.probe();

        assertEquals(0, eventsAt(Level.ERROR).size(), "未知码不应被误判为 CRITICAL");
        assertFalse(eventsAt(Level.WARN).isEmpty(), "未知码仍应告警（被发现），降级为 WARN");
    }

}
