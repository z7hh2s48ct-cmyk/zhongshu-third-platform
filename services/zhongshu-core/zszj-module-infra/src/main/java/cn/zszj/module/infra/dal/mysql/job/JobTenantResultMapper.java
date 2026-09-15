package cn.zszj.module.infra.dal.mysql.job;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 定时任务租户级执行结果 Mapper（ZS-JOB-001）
 */
@Mapper
public interface JobTenantResultMapper extends BaseMapperX<JobTenantResultDO> {

    /**
     * 查询一次执行的全部租户级明细
     *
     * @param jobLogId 执行日志编号
     * @return 明细列表，按租户编号升序，便于按租户定位失败项
     */
    default List<JobTenantResultDO> selectListByJobLogId(Long jobLogId) {
        return selectList(new LambdaQueryWrapperX<JobTenantResultDO>()
                .eq(JobTenantResultDO::getJobLogId, jobLogId)
                .orderByAsc(JobTenantResultDO::getTenantId));
    }

    /**
     * 统计一次执行中指定成败标记的租户数量
     *
     * @param jobLogId 执行日志编号
     * @param success  成败标记
     * @return 租户数量
     */
    default Long selectCountByJobLogIdAndSuccess(Long jobLogId, Boolean success) {
        return selectCount(new LambdaQueryWrapperX<JobTenantResultDO>()
                .eq(JobTenantResultDO::getJobLogId, jobLogId)
                .eq(JobTenantResultDO::getSuccess, success));
    }

}
