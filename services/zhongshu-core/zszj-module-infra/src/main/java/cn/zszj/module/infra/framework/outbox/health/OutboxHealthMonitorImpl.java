package cn.zszj.module.infra.framework.outbox.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link OutboxHealthMonitor} 实现（ZS-JOB-004）：JdbcTemplate 聚合 {@code outbox_event} / {@code dispatcher_lease}，
 * 计算积压 / 最长等待 / 失败率 / 租约健康 + 越阈清单（阈值经 {@link OutboxHealthProperties}，保守占位待 WP-19 实测回填）。
 *
 * <p><b>系统级快照</b>：不加租户过滤（服务「统一告警」诉求，与租户过滤的恢复操作入口分工，开发计划 §0.3-9）。
 * 双方言可移植：计数用 {@code SUM(CASE WHEN...)}（H2/PG 兼容），最长等待取 {@code MIN(next_retry_at)} 在 Java 侧算差
 * （时间比较应用参数化，循 {@code OutboxDispatcherService} 时钟基准）；越阈仅 WARN 告警，<b>不</b>声称任何未经实测的容量。
 */
@Service
@Slf4j
public class OutboxHealthMonitorImpl implements OutboxHealthMonitor {

    /** 各状态计数（H2/PG 兼容 {@code SUM(CASE WHEN...)}；系统级不加租户过滤）。 */
    private static final String STATUS_COUNT_SQL = "SELECT "
            + "COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_cnt, "
            + "COALESCE(SUM(CASE WHEN status = 'DEAD' THEN 1 ELSE 0 END), 0) AS dead_cnt, "
            + "COALESCE(SUM(CASE WHEN status = 'DISPATCHED' THEN 1 ELSE 0 END), 0) AS dispatched_cnt, "
            + "COALESCE(SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END), 0) AS skipped_cnt "
            + "FROM outbox_event";

    /** 最老 PENDING 的 {@code next_retry_at}（最长等待基准；无 PENDING 时聚合返回 NULL）。 */
    private static final String OLDEST_PENDING_SQL =
            "SELECT MIN(next_retry_at) FROM outbox_event WHERE status = 'PENDING'";

    /** 过期（含宽限）租约计数——dispatcher 停摆征兆。 */
    private static final String STALE_LEASE_SQL =
            "SELECT COUNT(*) FROM dispatcher_lease WHERE lease_expires_at < ?";

    private final JdbcTemplate jdbcTemplate;

    private final OutboxHealthProperties properties;

    public OutboxHealthMonitorImpl(DataSource dataSource, OutboxHealthProperties properties) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.properties = properties;
    }

    @Override
    public OutboxHealthMetrics snapshot() {
        long now = System.currentTimeMillis();
        // 各状态计数（RowMapper 按列标签取值，规避 H2 列名大小写与 queryForMap 键不一致）
        int[] counts = jdbcTemplate.queryForObject(STATUS_COUNT_SQL, (rs, n) -> new int[]{
                rs.getInt("pending_cnt"), rs.getInt("dead_cnt"),
                rs.getInt("dispatched_cnt"), rs.getInt("skipped_cnt")});
        int pendingBacklog = counts[0];
        int deadCount = counts[1];
        // 失败率 = DEAD /（DISPATCHED + DEAD + SKIPPED）× 100；分母 0 取 0（不编造）
        int failureDenominator = counts[1] + counts[2] + counts[3];
        double failureRatePercent = failureDenominator == 0 ? 0.0
                : Math.round(deadCount * 10000.0 / failureDenominator) / 100.0;
        // 最长等待：最老 PENDING 的 next_retry_at 距今秒数（Java 侧算差，双方言可移植）
        Timestamp oldestPending = jdbcTemplate.queryForObject(OLDEST_PENDING_SQL, Timestamp.class);
        long longestWaitSeconds = oldestPending == null ? 0L
                : Math.max(0L, (now - oldestPending.getTime()) / 1000L);
        // 租约健康：过期（含 leaseStaleSeconds 宽限）租约计数
        long staleBeforeMillis = now - properties.getLeaseStaleSeconds() * 1000L;
        Integer stale = jdbcTemplate.queryForObject(STALE_LEASE_SQL, Integer.class, new Timestamp(staleBeforeMillis));
        int staleLeaseCount = stale == null ? 0 : stale;

        List<String> breaches = evaluateBreaches(pendingBacklog, deadCount, longestWaitSeconds,
                failureRatePercent, staleLeaseCount);
        if (!breaches.isEmpty()) {
            // 阈值为保守占位（待 WP-19 实测回填）；越阈仅告警，绝不据此声称容量/TPS 达标（验收④红线）
            log.warn("[snapshot][Outbox 健康越阈 pendingBacklog={} deadCount={} longestWaitSeconds={} "
                            + "failureRatePercent={} staleLeaseCount={} breaches={}]",
                    pendingBacklog, deadCount, longestWaitSeconds, failureRatePercent, staleLeaseCount, breaches);
        }
        return OutboxHealthMetrics.builder()
                .pendingBacklog(pendingBacklog)
                .deadCount(deadCount)
                .longestWaitSeconds(longestWaitSeconds)
                .failureRatePercent(failureRatePercent)
                .staleLeaseCount(staleLeaseCount)
                .breaches(breaches)
                .build();
    }

    /** 比对阈值得越阈清单（机器可读常量码）。DEAD / 过期租约为「存在即告警」（无阈值属性，硬编码 &gt; 0）。 */
    private List<String> evaluateBreaches(int pendingBacklog, int deadCount, long longestWaitSeconds,
                                          double failureRatePercent, int staleLeaseCount) {
        List<String> breaches = new ArrayList<>();
        if (pendingBacklog > properties.getBacklogCritical()) {
            breaches.add("PENDING_BACKLOG_CRITICAL");
        } else if (pendingBacklog > properties.getBacklogWarn()) {
            breaches.add("PENDING_BACKLOG_WARN");
        }
        if (deadCount > 0) {
            breaches.add("DEAD_EVENTS_PRESENT");
        }
        if (longestWaitSeconds > properties.getLongestWaitWarnSeconds()) {
            breaches.add("LONGEST_WAIT_WARN");
        }
        if (failureRatePercent > properties.getFailureRateWarnPercent()) {
            breaches.add("FAILURE_RATE_WARN");
        }
        if (staleLeaseCount > 0) {
            breaches.add("STALE_LEASE_PRESENT");
        }
        return breaches;
    }

}
