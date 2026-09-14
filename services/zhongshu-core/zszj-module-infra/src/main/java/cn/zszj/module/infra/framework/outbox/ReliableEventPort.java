package cn.zszj.module.infra.framework.outbox;

/**
 * 可靠事件端口（ZS-JOB-002，适配自供体同名接口，供体快照隔离于 {@code reference/donors/}）。
 *
 * <p>合同：
 * 1. append 必须在与业务写<b>同一数据库事务</b>内调用——事务回滚则事件一并回滚，提交则事件至少一次投递；
 *    无事务上下文即拒绝（不代开事务，否则「业务回滚无事件」无从保证）；
 * 2. 业务模块只经本端口写事件，禁止直接读写 outbox_event 表；
 * 3. 消费方必须幂等：dispatcher 语义为 at-least-once（ZS-JOB-003 承接消费侧幂等）。
 */
public interface ReliableEventPort {

    /**
     * 在当前事务内追加一条待投递事件，返回事件 ID（技术租户取自当前上下文，缺失即拒绝）
     */
    long append(OutboxEventMessage message);

}
