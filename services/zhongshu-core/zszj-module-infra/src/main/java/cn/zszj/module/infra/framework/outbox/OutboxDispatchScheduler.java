package cn.zszj.module.infra.framework.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Outbox 派发驱动（ZS-FC-003 首链接线落位；循 {@code OutboxHealthAlertScheduler} 门控范式）。
 *
 * <p>BPM-004 接入合同 §1.11/§5：首链接线事项——事件驱动待办流转须有生产投递轮询，否则业务事务内
 * 预写的 Outbox 事件永远停留 PENDING（待办不流转、通知不出站）。本驱动按固定周期执行一轮
 * {@link OutboxDispatcherService#dispatchOnce}：
 * <ul>
 *   <li><b>门控</b>：{@code @ConditionalOnProperty(infra.outbox.dispatch.enabled=true)}——未启用时
 *       零调度开销（既有验证链/单测显式驱动 dispatchOnce 的路径不受影响）；</li>
 *   <li><b>事务边界</b>：{@code @Scheduled} 线程无事务上下文，满足 {@code dispatchOnce}
 *       「派发事务与业务事务分离」的 fail-fast 前置（领取/确认/Sink 投递各自独立短事务，
 *       Sink 投递事务由派发器统一包裹）；</li>
 *   <li><b>异常隔离</b>：整轮异常就地隔离（依赖不可用只记 errorClass，不落原文——循 JOB-004
 *       脱敏红线），不击穿调度线程，下一轮自然重试；逐事件异常由 dispatchOnce 内部隔离；</li>
 *   <li><b>多实例</b>：instanceId 取进程级 UUID，租约/凭证机制由派发器承担；实例租约经
 *       heartbeat 登记（可观测）；批量/租约/退避参数可经配置覆盖。</li>
 * </ul>
 *
 * @author ZS-FC-003
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "infra.outbox.dispatch", name = "enabled", havingValue = "true")
public class OutboxDispatchScheduler {

    private static final String DISPATCHER_NAME = "outbox-dispatch";

    private final OutboxDispatcherService dispatcherService;

    /** 进程级实例标识（重启即新实例：旧租约到期后事件可被安全重领） */
    private final String instanceId = UUID.randomUUID().toString();

    private final long leaseSeconds;

    private final int maxEvents;

    private final long backoffSeconds;

    public OutboxDispatchScheduler(OutboxDispatcherService dispatcherService,
                                   OutboxDispatchProperties properties) {
        this.dispatcherService = dispatcherService;
        this.leaseSeconds = properties.getLeaseSeconds();
        this.maxEvents = properties.getMaxEvents();
        this.backoffSeconds = properties.getBackoffSeconds();
    }

    /** 实例租约登记（可观测；与事件级抢占机制互补）。异常就地隔离，不击穿调度线程。 */
    @Scheduled(fixedRateString = "${infra.outbox.dispatch.lease-heartbeat-ms:30000}",
            initialDelayString = "${infra.outbox.dispatch.lease-heartbeat-ms:30000}")
    public void heartbeat() {
        try {
            dispatcherService.heartbeat(DISPATCHER_NAME, instanceId, leaseSeconds);
        } catch (RuntimeException ex) {
            log.error("[heartbeat][Outbox 派发实例租约登记失败 errorClass={}]", ex.getClass().getName());
        }
    }

    /** 执行一轮投递（领取 → Sink 投递〔独立事务〕→ 确认/退避）。异常就地隔离，不击穿调度线程。 */
    @Scheduled(fixedRateString = "${infra.outbox.dispatch.interval-ms:5000}",
            initialDelayString = "${infra.outbox.dispatch.initial-delay-ms:10000}")
    public void dispatchRound() {
        int claimed;
        try {
            claimed = dispatcherService.dispatchOnce(DISPATCHER_NAME, instanceId,
                    leaseSeconds, maxEvents, backoffSeconds);
        } catch (RuntimeException ex) {
            log.error("[dispatchRound][Outbox 投递轮执行失败 errorClass={}]", ex.getClass().getName());
            return;
        }
        if (claimed > 0) {
            log.info("[dispatchRound][Outbox 投递轮完成 claimed={}]", claimed);
        }
    }

}
