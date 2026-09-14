package cn.zszj.module.system.dal.mysql.audit;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.module.system.dal.dataobject.audit.AuditEventDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计事件 Mapper（ZS-AUDIT-002 只读查询）
 *
 * 审计历史只追加、不可改写：本 Mapper 仅提供查询与受控清理（deleteExpired），
 * 不提供业务侧 update 方法。@TenantIgnore 关闭 MP 自动注入，租户范围由
 * {@link cn.zszj.module.system.service.audit.AuditEventQueryService} 显式控制。
 *
 * @author ZS-AUDIT-002
 */
@Mapper
public interface AuditEventMapper extends BaseMapperX<AuditEventDO> {

    /**
     * 受控清理：删除 create_time 早于截止时间的过期事件（保留期合同，范围=全部租户统一保留期）。
     *
     * @return 清理条数
     */
    default int deleteExpiredBefore(java.time.LocalDateTime deadline) {
        return delete(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<AuditEventDO>()
                .lt(AuditEventDO::getCreateTime, deadline));
    }

}
