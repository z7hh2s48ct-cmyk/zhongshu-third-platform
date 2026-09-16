package cn.zszj.module.infra.framework.outbox.recovery;

import lombok.Builder;
import lombok.Value;

/**
 * Outbox 人工恢复结果（ZS-JOB-004）。
 *
 * <p>不可变结果对象，保留「恢复前后关联」供控制台回显与台账核对：{@code beforeStatus}→{@code afterStatus}、
 * 本事件第几次人工重试 {@code manualRetrySeq}（无限重试护栏依据）。
 *
 * <p>非法恢复（理由缺失 / 无权 / 非 DEAD / 超限 / 不存在）一律抛 {@code ServiceException}，不返回本对象；
 * 唯一「未生效但不抛」的情形是乐观并发落空（载入后状态被他实例改写，UPDATE 命中 0 行）——
 * 此时 {@code changed=false}、{@code outcome=CONCURRENT}，调用方可据此重试或放弃。
 */
@Value
@Builder
public class OutboxRecoveryResult {

    /** 目标事件编号。 */
    long eventId;

    /** 本次恢复动作（RETRY / SKIP）。 */
    OutboxRecoveryAction action;

    /** 恢复前状态（应为 DEAD）。 */
    String beforeStatus;

    /** 恢复后状态（RETRY→PENDING / SKIP→SKIPPED）。 */
    String afterStatus;

    /** 本事件第几次人工重试（从 1 起；SKIP 沿用当前序号）。 */
    int manualRetrySeq;

    /** 是否真实生效（false 仅表示乐观并发落空的 no-op）。 */
    boolean changed;

    /** 机器可读结果：RETRIED / SKIPPED / CONCURRENT。 */
    String outcome;

}
