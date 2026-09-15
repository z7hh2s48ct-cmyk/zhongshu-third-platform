package cn.zszj.module.system.dal.mysql.notify;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.system.dal.dataobject.notify.NotifySendLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通知发送日志 Mapper（ZS-MSG-001）。
 *
 * <p>幂等查询：按 (tenant_id, event_id, recipient_type, recipient_id, channel) 查既有日志，
 * 供 {@link cn.zszj.module.system.service.notify.dispatch.NotifyDispatcher} 判断 DUPLICATE_IGNORED。
 */
@Mapper
public interface NotifySendLogMapper extends BaseMapperX<NotifySendLogDO> {

    /**
     * 按幂等键查询既有发送日志（DB 唯一索引硬兜底的应用层预检 + 并发撞键后复判）。
     *
     * <p>刻意<b>不过滤</b> {@code deleted}：唯一约束 {@code uk_notify_send_log_idempotent} 覆盖全部行
     * （含逻辑删除），幂等查询必须与之同域——否则日志逻辑删除后同一事件重试查不到旧记录、却又始终无法插入
     * （codex r0 P2）。发送日志循 AUDIT-001/JOB-002 惯例只追加不改写，正常不做逻辑删除。
     *
     * @return 既有日志，不存在返回 null
     */
    @Select("SELECT * FROM system_notify_send_log WHERE tenant_id = #{tenantId} AND event_id = #{eventId} "
            + "AND recipient_type = #{recipientType} AND recipient_id = #{recipientId} AND channel = #{channel} "
            + "LIMIT 1")
    NotifySendLogDO selectByIdempotentKey(@Param("tenantId") Long tenantId,
                                          @Param("eventId") String eventId,
                                          @Param("recipientType") String recipientType,
                                          @Param("recipientId") Long recipientId,
                                          @Param("channel") String channel);

    /**
     * 回填派发副作用关联 ID（claim-first：抢位日志先落库，SUCCESS 建消息 + append Outbox 后回填）。
     *
     * <p>定向 UPDATE 只改 message_id/outbox_event_id，避免 BaseDO 全字段更新触发 update_time 自动填充的
     * 不确定性；仅按主键更新，幂等键与状态在抢位时已确定。
     *
     * @return 影响行数（应为 1）
     */
    @Update("UPDATE system_notify_send_log SET message_id = #{messageId}, outbox_event_id = #{outboxEventId} "
            + "WHERE id = #{id}")
    int fillDispatchSideEffects(@Param("id") Long id,
                                @Param("messageId") Long messageId,
                                @Param("outboxEventId") Long outboxEventId);

}
