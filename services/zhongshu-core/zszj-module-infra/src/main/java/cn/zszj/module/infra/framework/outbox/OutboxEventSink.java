package cn.zszj.module.infra.framework.outbox;

/**
 * Outbox 事件 Sink（ZS-JOB-002，适配自供体同名接口）：每种投递形态一个实现（站内消息、通知等）。
 *
 * <p>合同：
 * 1. Sink 必须幂等——dispatcher 是 at-least-once 语义（租约过期重领、投递成功但确认丢失都会重投）；
 * 2. Sink 只做投递动作，禁止回写业务事实。
 */
public interface OutboxEventSink {

    /**
     * @return 是否声明消费该事件类型；false 表示本 Sink 不处理（由其他 Sink 处理）
     */
    boolean supports(String eventType);

    void deliver(OutboxEventRecord event) throws Exception;

}
