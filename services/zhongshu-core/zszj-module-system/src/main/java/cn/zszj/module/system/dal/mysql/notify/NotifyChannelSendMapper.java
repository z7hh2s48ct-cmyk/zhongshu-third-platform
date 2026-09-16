package cn.zszj.module.system.dal.mysql.notify;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 渠道发送生命周期台账 Mapper（ZS-MSG-004）。
 *
 * <p>状态推进全部走<b>定向 CAS UPDATE</b>（WHERE 携带期望原状态）：Outbox 投递是 at-least-once（租约过期重领、
 * 确认丢失重投），CAS 保证并发/重放只有一次真实推进，落败方按幂等吸收；计数列（attempt/receipt/manual_retry）
 * 只增不减。回执按 (tenant_id, channel, channel_message_id) 唯一键定位（uk_notify_channel_send_receipt）。
 */
@Mapper
public interface NotifyChannelSendMapper extends BaseMapperX<NotifyChannelSendDO> {

    /** 按渠道幂等键查询（回执定位 + 回查入口）；tenant 过滤依赖租户拦截器 + 显式谓词双保险。
     *  手写 @Select 不经 MyBatis-Plus 逻辑删除拦截器，显式 deleted = FALSE 对齐唯一约束同域惯例。 */
    @Select("SELECT * FROM system_notify_channel_send WHERE tenant_id = #{tenantId} "
            + "AND channel = #{channel} AND channel_message_id = #{channelMessageId} AND deleted = FALSE LIMIT 1")
    NotifyChannelSendDO selectByChannelMessageId(@Param("tenantId") Long tenantId,
                                                 @Param("channel") String channel,
                                                 @Param("channelMessageId") String channelMessageId);

    /** 按 Outbox 事件 ID 查询（投递事件 → 台账记录；每次投递必经，r1 P3 补 deleted 过滤与覆盖索引）。 */
    @Select("SELECT * FROM system_notify_channel_send WHERE tenant_id = #{tenantId} "
            + "AND outbox_event_id = #{outboxEventId} AND deleted = FALSE LIMIT 1")
    NotifyChannelSendDO selectByOutboxEventId(@Param("tenantId") Long tenantId,
                                              @Param("outboxEventId") Long outboxEventId);

    /** 按状态列台账（回查/人工处置扫描，id 升序有界）。 */
    @Select("SELECT * FROM system_notify_channel_send WHERE tenant_id = #{tenantId} AND status = #{status} "
            + "AND deleted = FALSE ORDER BY id LIMIT #{limit}")
    List<NotifyChannelSendDO> selectListByStatus(@Param("tenantId") Long tenantId,
                                                 @Param("status") String status,
                                                 @Param("limit") int limit);

    /**
     * CAS：提交受理（PENDING → ACCEPTED）。
     *
     * @return 影响行数；0 = 状态已被并发改变（幂等吸收）
     */
    @Update("UPDATE system_notify_channel_send SET status = 'ACCEPTED', channel_serial_no = #{channelSerialNo}, "
            + "status_reason = NULL, failed_code = NULL, last_error = NULL, attempt_count = attempt_count + 1, "
            + "accepted_at = #{now}, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status = 'PENDING' AND deleted = FALSE")
    int casAccept(@Param("id") Long id, @Param("tenantId") Long tenantId,
                  @Param("channelSerialNo") String channelSerialNo, @Param("now") Date now);

    /**
     * CAS：提交被明确拒绝（PENDING/UNKNOWN → FAILED）。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'FAILED', failed_code = #{failedCode}, "
            + "status_reason = #{reason}, attempt_count = attempt_count + 1, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','UNKNOWN') AND deleted = FALSE")
    int casReject(@Param("id") Long id, @Param("tenantId") Long tenantId,
                  @Param("failedCode") String failedCode, @Param("reason") String reason, @Param("now") Date now);

    /**
     * CAS：提交结果未知（PENDING/UNKNOWN → UNKNOWN）——先回查再重发，不盲目重发。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'UNKNOWN', last_error = #{error}, "
            + "attempt_count = attempt_count + 1, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','UNKNOWN') AND deleted = FALSE")
    int casMarkUnknown(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("error") String error, @Param("now") Date now);

    /**
     * CAS：送达（任意非终态 → DELIVERED；回执权威——乱序送达回执不丢事实；回执计数同步累加）。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'DELIVERED', delivered_at = #{now}, "
            + "last_receipt_at = #{now}, receipt_count = receipt_count + 1, "
            + "status_reason = NULL, failed_code = NULL, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','ACCEPTED','UNKNOWN') AND deleted = FALSE")
    int casDeliver(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("now") Date now);

    /**
     * CAS：失败回执（PENDING/ACCEPTED/UNKNOWN → FAILED；回执计数同步累加）。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'FAILED', failed_code = #{failedCode}, "
            + "status_reason = #{reason}, last_receipt_at = #{now}, receipt_count = receipt_count + 1, "
            + "update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','ACCEPTED','UNKNOWN') AND deleted = FALSE")
    int casReceiptFail(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("failedCode") String failedCode, @Param("reason") String reason, @Param("now") Date now);

    /**
     * CAS：终态/受理态下重复回执登记（receipt_count 只增，重复回执可追踪不报错）。
     */
    @Update("UPDATE system_notify_channel_send SET receipt_count = receipt_count + 1, last_receipt_at = #{now}, "
            + "update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','ACCEPTED','UNKNOWN','DELIVERED','FAILED') AND deleted = FALSE")
    int casCountReceipt(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("now") Date now);

    /**
     * CAS：回查确认送达（ACCEPTED/UNKNOWN → DELIVERED，携回查依据——Outbox 回查只遇 UNKNOWN，
     * 对账可从 ACCEPTED 推进，共用此出口）。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'DELIVERED', delivered_at = #{now}, "
            + "last_query_at = #{now}, last_query_result = 'DELIVERED', status_reason = #{evidence}, "
            + "failed_code = NULL, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('ACCEPTED','UNKNOWN') AND deleted = FALSE")
    int casQueryDelivered(@Param("id") Long id, @Param("tenantId") Long tenantId,
                          @Param("evidence") String evidence, @Param("now") Date now);

    /**
     * CAS：回查确认未发出（UNKNOWN → PENDING，待重发——「先回查再重发」的唯一重发入口；
     * 复位同时清空 last_error，避免重发记录携带上一次技术失败描述误导检索）。
     */
    @Update("UPDATE system_notify_channel_send SET status = 'PENDING', last_error = NULL, "
            + "last_query_at = #{now}, last_query_result = 'NOT_SENT', status_reason = #{evidence}, "
            + "update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status = 'UNKNOWN' AND deleted = FALSE")
    int casQueryNotSent(@Param("id") Long id, @Param("tenantId") Long tenantId,
                        @Param("evidence") String evidence, @Param("now") Date now);

    /**
     * CAS：登记回查结论但不推进状态（仍未知 / 受理态对账证据 / 拒绝自动重发）。
     */
    @Update("UPDATE system_notify_channel_send SET last_query_at = #{now}, last_query_result = #{result}, "
            + "status_reason = #{evidence}, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status = #{expectedStatus} AND deleted = FALSE")
    int casRecordQueryEvidence(@Param("id") Long id, @Param("tenantId") Long tenantId,
                               @Param("expectedStatus") String expectedStatus, @Param("result") String result,
                               @Param("evidence") String evidence, @Param("now") Date now);

    /**
     * CAS：人工重试复位（PENDING/FAILED/UNKNOWN → PENDING）+ 操作留痕 + 联系方式刷新；
     * ACCEPTED/DELIVERED 不可复位（受理/送达状态由回执与回查推进，人工重发会造成重复发件）。
     *
     * <p>r0 P2 处置：以 <b>expectedOldEventId 条件换绑</b>——PENDING→PENDING 不再是无条件空转迁移，
     * 并发/双击人工重试只有一次成功（其余落败方由事务回滚连带撤销其追加的事件）。
     * r1 P1 处置：占位符显式声明 {@code jdbcType=BIGINT}——MyBatis 默认 jdbcTypeForNull=OTHER 在
     * pgjdbc 绑定为 OID UNSPECIFIED，{@code ? IS NULL} 占位符无列上下文可推断类型即报 42P18
     * （could not determine data type of parameter）；显式类型后 NULL 绑定为 bigint，双方言可移植。
     *
     * @return 影响行数；0 = 状态或事件绑定已被并发改变（真 CAS 落败）
     */
    @Update("UPDATE system_notify_channel_send SET status = 'PENDING', status_reason = '人工重试', "
            + "failed_code = NULL, last_error = NULL, manual_retry_count = manual_retry_count + 1, "
            + "recipient_contact = #{contact}, "
            + "last_retry_actor_type = #{actorType}, last_retry_actor_id = #{actorId}, "
            + "last_retry_reason = #{reason}, outbox_event_id = #{outboxEventId}, update_time = #{now} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND status IN ('PENDING','FAILED','UNKNOWN') "
            + "AND (outbox_event_id = #{expectedOldEventId,jdbcType=BIGINT} "
            + "OR (outbox_event_id IS NULL AND #{expectedOldEventId,jdbcType=BIGINT} IS NULL)) AND deleted = FALSE")
    int casManualRetry(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("expectedOldEventId") Long expectedOldEventId, @Param("contact") String contact,
                       @Param("actorType") String actorType, @Param("actorId") String actorId,
                       @Param("reason") String reason, @Param("outboxEventId") Long outboxEventId,
                       @Param("now") Date now);

    /**
     * 定向回填投递事件 ID（创建时 / 人工重试换绑最新事件；只改 outbox_event_id）。
     */
    @Update("UPDATE system_notify_channel_send SET outbox_event_id = #{outboxEventId} WHERE id = #{id}")
    int fillOutboxEventId(@Param("id") Long id, @Param("outboxEventId") Long outboxEventId);

}
