package cn.zszj.framework.common.biz.system.audit;

/**
 * 统一业务审计端口（ZS-AUDIT-001，架构 §6.10）。
 *
 * <p>与「通用操作日志」（{@link cn.zszj.framework.common.biz.system.logger.OperateLogCommonApi}，异步、可丢失、
 * 不阻塞业务）不同，本端口语义是<b>同步落库的不可改写业务审计历史</b>：
 * <ul>
 *   <li>业务成功审计（{@link AuditEventMessage.AuditResult#SUCCESS}）随调用方事务提交——业务回滚则不留成功审计；</li>
 *   <li>拒绝 / 失败审计（{@link AuditEventMessage.AuditResult#DENIED} / {@link AuditEventMessage.AuditResult#FAILURE}）
 *       以独立事务记录，不随业务事务回滚而丢失；</li>
 *   <li>携带幂等键的重复事件不重复入账。</li>
 * </ul>
 *
 * <p>合同：审计写入不得携带敏感明文（授权码明文、签名 URL、session_key、密码等），敏感信息须在调用方
 * 掩码 / 哈希后再放入 {@link AuditEventMessage#getDetail()}。B04 阶段仅同库事务内审计，<b>不依赖 B05 Outbox</b>；
 * 需补偿的异步交付待 B05 可靠机制验收后启用（见 docs/05 §16.1 尾注）。
 *
 * <p>接口置于 zszj-common（framework 层，循 {@link cn.zszj.framework.common.biz.system.logger.OperateLogCommonApi} 先例），
 * 实现落 system 模块，不落 infra/file 或 infra/job。
 */
public interface AuditPort {

    /**
     * 落一条审计事件，返回事件 ID（幂等命中时返回已存在事件 ID）。
     *
     * <p>{@code eventType}、{@code actorType}、{@code result} 为必填；缺失即抛
     * {@link cn.zszj.framework.common.exception.ServiceException}（fail-closed）。对 {@code SUCCESS} 事件，实现须在抛出前
     * 将所参与的业务事务标记 rollback-only——纵使调用方吞掉异常，业务也无法「无成功审计而提交」，杜绝「无主体的模糊审计」。
     *
     * @param message 审计事件消息
     * @return 事件 ID；无法取得生成键时返回 {@code -1}
     */
    long record(AuditEventMessage message);

}
