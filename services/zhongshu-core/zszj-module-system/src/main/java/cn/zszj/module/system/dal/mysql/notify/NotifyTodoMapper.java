package cn.zszj.module.system.dal.mysql.notify;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTodoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 业务待办 Mapper（ZS-MSG-002）。
 *
 * <p>循 MSG-001 {@code NotifySendLogMapper} 先例：幂等查询按 (tenant_id, source_type, todo_key) 显式过滤，
 * 与唯一约束 {@code uk_notify_todo_key} 同域——{@link #selectByUniqueKey} 刻意<b>不过滤</b> {@code deleted}
 * （唯一约束覆盖含逻辑删除的全部行，注册幂等查询必须与之同域，否则逻辑删除后同 key 无法复判既有）。
 * 状态流转用定向 {@link #updateTransition}（乐观锁 todo_version + 显式 tenant_id），避免 BaseDO 全字段更新。
 */
@Mapper
public interface NotifyTodoMapper extends BaseMapperX<NotifyTodoDO> {

    /**
     * 按唯一键 (tenant_id, source_type, todo_key) 查既有待办（注册幂等预检 + 流转定位 + 并发撞键复判）。
     *
     * @return 既有待办，不存在返回 null
     */
    @Select("SELECT * FROM system_notify_todo WHERE tenant_id = #{tenantId} AND source_type = #{sourceType} "
            + "AND todo_key = #{todoKey} LIMIT 1")
    NotifyTodoDO selectByUniqueKey(@Param("tenantId") Long tenantId,
                                   @Param("sourceType") String sourceType,
                                   @Param("todoKey") String todoKey);

    /**
     * 定向状态流转（乐观锁）：仅当 todo_version 未变时更新状态/原因/转派目标并递增版本。
     *
     * <p>只改流转相关列，避免 BaseDO 全字段更新触发 update_time 自动填充的不确定性；
     * {@code biz_version} 以 COALESCE 保留既有值（本次事件无版本时不覆盖）。
     *
     * @return 影响行数（应为 1；0 表示并发已改变 todo_version，本次流转失败）
     */
    @Update("UPDATE system_notify_todo SET status = #{status}, status_reason = #{statusReason}, "
            + "assignee_type = #{assigneeType}, assignee_id = #{assigneeId}, "
            + "biz_version = COALESCE(#{bizVersion}, biz_version), todo_version = todo_version + 1, "
            + "update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND todo_version = #{expectedVersion}")
    int updateTransition(@Param("id") Long id,
                         @Param("tenantId") Long tenantId,
                         @Param("status") String status,
                         @Param("statusReason") String statusReason,
                         @Param("assigneeType") String assigneeType,
                         @Param("assigneeId") Long assigneeId,
                         @Param("bizVersion") String bizVersion,
                         @Param("expectedVersion") Long expectedVersion);

    /**
     * 按收件人 + 状态列待办（status 为空则不限状态），id 升序有界。
     */
    @Select("<script>SELECT * FROM system_notify_todo WHERE tenant_id = #{tenantId} "
            + "AND recipient_type = #{recipientType} AND recipient_id = #{recipientId} AND deleted = FALSE "
            + "<if test='status != null'>AND status = #{status} </if>"
            + "ORDER BY id LIMIT #{limit}</script>")
    List<NotifyTodoDO> selectByRecipientAndStatus(@Param("tenantId") Long tenantId,
                                                  @Param("recipientType") String recipientType,
                                                  @Param("recipientId") Long recipientId,
                                                  @Param("status") String status,
                                                  @Param("limit") int limit);

    /**
     * 统计收件人未完成待办数（非终态：PENDING / REASSIGNED）。
     */
    @Select("SELECT COUNT(1) FROM system_notify_todo WHERE tenant_id = #{tenantId} "
            + "AND recipient_type = #{recipientType} AND recipient_id = #{recipientId} "
            + "AND status IN ('PENDING', 'REASSIGNED') AND deleted = FALSE")
    long countUnfinished(@Param("tenantId") Long tenantId,
                         @Param("recipientType") String recipientType,
                         @Param("recipientId") Long recipientId);

}
