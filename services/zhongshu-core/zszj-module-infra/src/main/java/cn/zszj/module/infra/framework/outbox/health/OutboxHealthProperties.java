package cn.zszj.module.infra.framework.outbox.health;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Outbox 健康监测与恢复护栏阈值（ZS-JOB-004，前缀 {@code infra.outbox.health}）。
 *
 * <p><b>阈值为保守占位，待 WP-19 运行监控阶段实测回填</b>——验收④要求「告警阈值经实测回填而非凭空声称
 * 容量达标」，故此处默认值仅保证「监测逻辑可运行 + 越阈可告警」，<b>禁止</b>被解读为生产容量承诺，
 * 亦禁止在代码/文档编造「支持 N 万 TPS」类未经实测的断言（开发计划铁律 8）。
 *
 * <p>各字段均被 {@code OutboxHealthMonitorImpl} / {@code OutboxRecoveryServiceImpl} 实际消费（无占位空转）：
 * <ul>
 *   <li>{@code backlogWarn/backlogCritical}：PENDING 积压预警 / 严重阈值；</li>
 *   <li>{@code longestWaitWarnSeconds}：最长等待预警阈值（秒）；</li>
 *   <li>{@code failureRateWarnPercent}：失败率预警阈值（百分比）；</li>
 *   <li>{@code leaseStaleSeconds}：租约过期宽限（秒，容忍时钟偏移；默认 0 即「已过期即视为停摆」）；</li>
 *   <li>{@code maxManualRetry}：单事件人工重试上限（无限重试护栏，超过即拒绝）。</li>
 *   <li>{@code alert}：周期告警探针配置（ZS-OPS-002.B，绑定 {@code infra.outbox.health.alert.*}）。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "infra.outbox.health")
public class OutboxHealthProperties {

    /** PENDING 积压预警阈值（保守占位，待 WP-19 实测回填）。 */
    private int backlogWarn = 1000;

    /** PENDING 积压严重阈值（保守占位，待 WP-19 实测回填）。 */
    private int backlogCritical = 5000;

    /** 最长等待预警阈值（秒，保守占位，待 WP-19 实测回填）。 */
    private long longestWaitWarnSeconds = 3600;

    /** 失败率预警阈值（百分比，保守占位，待 WP-19 实测回填）。 */
    private int failureRateWarnPercent = 5;

    /** 租约过期宽限（秒）：{@code lease_expires_at < now - 宽限} 方计为停摆，默认 0（已过期即计）。 */
    private long leaseStaleSeconds = 0;

    /** 单事件人工重试上限（无限重试护栏）：{@code manual_retry_seq > 上限} 即拒绝，默认 3。 */
    private int maxManualRetry = 3;

    /** 周期告警探针配置（ZS-OPS-002.B；与 {@code OutboxHealthAlertScheduler} 的 @ConditionalOnProperty/@Scheduled 同源消费）。 */
    private final Alert alert = new Alert();

    @Data
    public static class Alert {

        /** 是否启用周期告警探针；false 时探针不装配、不产任何调度开销。 */
        private boolean enabled = true;

        /** 探针周期（毫秒，保守占位，待 WP-19 实测回填；非容量/告警时效承诺）。 */
        private long intervalMs = 60000;

    }

}
