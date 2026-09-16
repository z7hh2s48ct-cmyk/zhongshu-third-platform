package cn.zszj.module.infra.framework.outbox.recovery;

import cn.zszj.framework.common.pojo.PageResult;

/**
 * Outbox 人工恢复服务（ZS-JOB-004）：授权的查看 / 回查 / 重试 / 跳过 + 恢复台账双轨留痕。
 *
 * <p>循 {@code ConfigChangeRecorder}（ZS-CFG-004）恢复留痕范式：恢复成功 → 台账行（随业务事务）+
 * {@code AuditPort} SUCCESS 审计（随事务）；越权 → {@code ACCESS_DENIED} 独立事务留痕 + 抛
 * {@code ServiceException}（业务拒绝原样返回，吞审计自身失败并告警）。
 *
 * <p>租户强制：{@code pageDeadEvents/getRecoveryDetail/retryDeadEvent/skipDeadEvent} 全量取
 * {@code TenantContextHolder}（缺失 fail-closed，不默认 0），DEAD 事件查询/恢复按租户过滤，跨租户不可见/不可恢复。
 *
 * <p><b>payload 只读红线</b>：所有恢复动作仅改 {@code status/next_retry_at/claim_*} + 写台账，
 * <b>永不</b> {@code UPDATE payload/headers/event_type}（历史事实不可掩盖，验收「修改历史被拒」）。
 */
public interface OutboxRecoveryService {

    /**
     * 查看 DEAD 事件分页（当前租户，逐条脱敏）——恢复控制台工作清单。
     *
     * @param pageNo   页码（从 1 起）
     * @param pageSize 每页条数
     * @return DEAD 事件回查详情分页（payload 掩码 + 受控异常摘要）
     */
    PageResult<OutboxEventRecoveryDetail> pageDeadEvents(int pageNo, int pageSize);

    /**
     * 回查单事件详情（当前租户）：payload 敏感键掩码 + {@code last_error} 受控摘要
     * （{@code errorCategory/errorClass/messageLength}，无异常原文）+ 恢复台账历史。
     *
     * @param eventId 事件编号
     * @return 回查详情
     * @throws cn.zszj.framework.common.exception.ServiceException 事件不存在或不属于当前租户（{@code OUTBOX_EVENT_NOT_FOUND}）
     */
    OutboxEventRecoveryDetail getRecoveryDetail(long eventId);

    /**
     * 授权人工重试：DEAD→PENDING，重置退避（{@code next_retry_at=now}）、清租约，写台账(RETRY)，
     * {@code AuditPort.record(OUTBOX_EVENT_RETRIED, SUCCESS 随事务)}。
     *
     * <p>护栏：租户/reason/operator 缺失、事件不存在/跨租户、非 DEAD、{@code manual_retry_seq > maxManualRetry}
     * 均抛 {@code ServiceException}；payload/headers/event_type 字节不变。
     *
     * @param cmd 恢复命令（reason 必填、operator 必填）
     * @return 恢复结果（前后关联 + manualRetrySeq）
     */
    OutboxRecoveryResult retryDeadEvent(OutboxRecoveryCmd cmd);

    /**
     * 授权人工跳过：DEAD→SKIPPED（人工放弃终态），写台账(SKIP)，{@code AuditPort.record(OUTBOX_EVENT_SKIPPED)}。
     * 理由必填；payload 不变；SKIPPED 不再被 {@code CLAIM_SELECT_SQL}（{@code status='PENDING'}）领取。
     *
     * @param cmd 恢复命令（reason 必填、operator 必填）
     * @return 恢复结果（前后关联）
     */
    OutboxRecoveryResult skipDeadEvent(OutboxRecoveryCmd cmd);

}
