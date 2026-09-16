package cn.zszj.module.infra.framework.outbox.recovery;

/**
 * Outbox 人工恢复动作（ZS-JOB-004，机制级动作而非业务态）。
 *
 * <ul>
 *   <li>{@link #RETRY}：授权人工重试——DEAD→PENDING，重置退避（next_retry_at=now）+ 释放租约，
 *       下一轮 {@code OutboxDispatcherService#dispatchOnce} 重领；</li>
 *   <li>{@link #SKIP}：授权人工跳过——DEAD→SKIPPED（人工放弃终态），不再被 {@code CLAIM_SELECT_SQL}
 *       （{@code status='PENDING'}）领取，历史保留。</li>
 * </ul>
 *
 * <p>两种动作都必须填写理由并落「恢复台账 + 审计」双轨留痕，且<b>永不</b>修改
 * {@code payload/headers/event_type}（历史事实不可掩盖，见开发计划铁律 9）。
 */
public enum OutboxRecoveryAction {

    /** 人工重试：DEAD→PENDING。 */
    RETRY,

    /** 人工跳过：DEAD→SKIPPED（放弃终态）。 */
    SKIP

}
