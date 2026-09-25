package cn.zszj.module.bpm.contract;

import cn.zszj.module.bpm.contract.TechNeutralContractSample.TechNeutralContractCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 同事务内部事件监听器（ZS-BPM-004 接入合同样例）——演示「同事务内部事件」通道的接线与边界。
 *
 * <p>与 Outbox 跨进程事件的边界（合同条款「同事务内部事件与跨进程事件分开」）：
 * <ul>
 *   <li>内部事件（本监听器）：进程内同步发布，监听发生在发布方事务内（Spring 默认同步派发）——可用于同事务
 *       扩展点；不落库、不跨进程、事务回滚则无对外副作用，<b>不得</b>承载跨系统通知；</li>
 *   <li>跨进程事件（Outbox）：经 {@code ReliableEventPort#append} 与业务写同事务落库，由 dispatcher
 *       at-least-once 投递，适用于跨系统 / 跨进程 / 需重试的通知（本样例的待办流转事件即走此通道）。</li>
 * </ul>
 *
 * <p>本类以内存列表记录收到的事件与「接收时是否处于真实事务中」，供样例测试断言——内部事件监听确实发生在
 * 发布方事务内（而非事务提交后）。
 */
public class TechNeutralContractInnerEventListener {

    private final List<TechNeutralContractCreatedEvent> received = new CopyOnWriteArrayList<>();

    /** 每个事件被监听时线程是否处于真实事务中（与 received 对齐）。 */
    private final List<Boolean> receivedInsideTransaction = new CopyOnWriteArrayList<>();

    @EventListener
    public void onCreated(TechNeutralContractCreatedEvent event) {
        receivedInsideTransaction.add(TransactionSynchronizationManager.isActualTransactionActive());
        received.add(event);
    }

    /** 已收到的事件（按接收顺序）。 */
    public List<TechNeutralContractCreatedEvent> received() {
        return List.copyOf(received);
    }

    /** 最近一次监听是否发生在真实事务内（同事务内部事件的直接证据）。 */
    public boolean lastReceivedInsideTransaction() {
        return !receivedInsideTransaction.isEmpty()
                && receivedInsideTransaction.get(receivedInsideTransaction.size() - 1);
    }

    /** 清空记录（测试用例间隔离；clean.sql 无法清理内存状态）。 */
    public void reset() {
        received.clear();
        receivedInsideTransaction.clear();
    }

}
