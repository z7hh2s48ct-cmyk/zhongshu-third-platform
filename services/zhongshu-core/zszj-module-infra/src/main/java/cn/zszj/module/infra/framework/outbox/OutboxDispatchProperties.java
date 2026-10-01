package cn.zszj.module.infra.framework.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Outbox 派发驱动配置（ZS-FC-003 首链接线；与 {@link OutboxDispatchScheduler} 的
 * {@code @ConditionalOnProperty}/{@code @Scheduled} 占位符同源消费）。
 *
 * @author ZS-FC-003
 */
@Component
@ConfigurationProperties(prefix = "infra.outbox.dispatch")
@Data
public class OutboxDispatchProperties {

    /** 事件租约时长（秒）：领取后未确认的宽限窗口，过期可被其他实例重领 */
    private long leaseSeconds = 60;

    /** 单轮最大领取事件数 */
    private int maxEvents = 100;

    /** 投递失败的退避基数（秒，派发器按轮次推进） */
    private long backoffSeconds = 10;

}
