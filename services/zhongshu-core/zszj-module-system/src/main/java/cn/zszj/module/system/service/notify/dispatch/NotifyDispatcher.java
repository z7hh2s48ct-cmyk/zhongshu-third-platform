package cn.zszj.module.system.service.notify.dispatch;

import java.util.List;

/**
 * 统一通知派发入口（ZS-MSG-001）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「建设统一通知命令/结果，绑定事件、业务对象、技术租户和接收主体；
 * 明确禁用模板、无渠道、接收人失效的可查询状态；复用模板渲染」。
 *
 * <p>合同：
 * <ol>
 *   <li><b>事务内幂等</b>：同一 {@code eventId} + 收件人 + 渠道重试不重复生成消息
 *       （claim-first 抢占 DB 唯一索引 {@code uk_notify_send_log_idempotent}，仅抢到键者执行副作用）；</li>
 *   <li><b>事务参与</b>：必须在调用方事务内（{@code @Transactional} REQUIRED）；
 *       业务回滚 → 抢位日志 + 消息 + Outbox 事件一并回滚（复用 JOB-002 {@code ReliableEventPort} 保证）；</li>
 *   <li><b>明确状态</b>：非 SUCCESS 状态（禁用模板/无渠道/收件人失效等）返回 {@link NotifyDispatchStatus}，
 *       不抛异常、不返回 null；所有状态（含无渠道，以哨兵渠道落库）均写入 {@code system_notify_send_log}（可追溯）；</li>
 *   <li><b>Outbox 集成</b>：SUCCESS 状态追加 {@code OutboxEventMessage{eventType=NOTIFY_DISPATCHED}}，
 *       供 MSG-002（业务待办生命周期）与 MSG-004（渠道发送状态/回执对账）消费。</li>
 * </ol>
 *
 * <p>与既有 {@link cn.zszj.module.system.service.notify.NotifySendService} 的关系：后者当前仍直接创建消息
 * （尚未接入本入口）；将其迁移为委托本接口（生成稳定 eventId 保证幂等）属后续接入任务，本卡只交付统一入口
 * 与幂等/状态/事务保证，不改动既有调用方。
 */
public interface NotifyDispatcher {

    /**
     * 统一通知派发——逐收件人 × 逐渠道处理，返回明确可查询结果列表。
     *
     * @param command 通知命令（eventId 必填，幂等键）
     * @return 派发结果列表（每个收件人 × 每个渠道一条结果）
     */
    List<NotifyDispatchResult> dispatch(NotifyCommand command);

}
