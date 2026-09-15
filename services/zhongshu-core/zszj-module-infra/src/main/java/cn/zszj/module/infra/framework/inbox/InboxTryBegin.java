package cn.zszj.module.infra.framework.inbox;

import lombok.Value;

/**
 * Inbox 抢位结果（ZS-JOB-003）。
 *
 * <ul>
 *   <li>{@code CLAIMED}——本次抢到（同事务登记 PROCESSING），调用方执行业务副作用后经
 *       {@link ConsumerInboxPort#complete}/{@code fail}/{@code markResultUnknown} 推进；</li>
 *   <li>{@code DUPLICATE_COMPLETED}——同键已完成（可重放业务结果在 {@code record.result} 中，
 *       调用方直接返回首次结果，不重复副作用）；</li>
 *   <li>{@code DUPLICATE_IN_FLIGHT}——同键处理中（并发抢占或先前事务未提交）；</li>
 *   <li>{@code DUPLICATE_RESULT_UNKNOWN}——先前结果未知（外部结果不确定），必须先回查
 *       （{@code record} 携带现场），不得盲目重处理；</li>
 *   <li>{@code RETRIED_CLAIMED}——先前 FAILED，本次重领（retry_count 为已记录失败次数：由失败登记递增，
 *       重领本身不递增，可重新处理）；</li>
 *   <li>{@code PARAM_CONFLICT}——同键不同载荷指纹（参数冲突不得当作相同成功）；</li>
 *   <li>{@code STALE_VERSION}——旧版本事件（同对象版本水位已不低于其版本——水位由处理尝试占坑抬升，
 *       含已提交的失败占位；旧版本不得覆盖新状态，处置归人工/补偿，D-07 登记）。</li>
 * </ul>
 */
@Value
public class InboxTryBegin {

    public enum Outcome {
        CLAIMED, DUPLICATE_COMPLETED, DUPLICATE_IN_FLIGHT, DUPLICATE_RESULT_UNKNOWN, RETRIED_CLAIMED,
        PARAM_CONFLICT, STALE_VERSION
    }

    Outcome outcome;

    /** 关联记录：CLAIMED/RETRIED_CLAIMED 为本次登记结果；DUPLICATE_*/PARAM_CONFLICT 为既有记录现场；
     * STALE_VERSION 在首抢路径为 {@code null}（占位行已随护栏拒绝回滚，无现场可携）。
     */
    InboxRecord record;

}
