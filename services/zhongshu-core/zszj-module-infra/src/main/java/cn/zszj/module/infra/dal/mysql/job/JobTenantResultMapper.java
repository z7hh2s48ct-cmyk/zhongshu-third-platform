package cn.zszj.module.infra.dal.mysql.job;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    /**
     * 物理删除指定时间之前的租户级明细
     *
     * 与父执行日志（infra_job_log）共用同一保留期：父日志被 JobLogCleanJob 物理删除后，
     * 明细既无法回连 handler/param，也没有任何查询入口，会成为永久留存的死数据。
     * 明细与父日志同时创建，create_time 基本一致，因此按自身 create_time 清理即等价于级联删除。
     *
     * @param createTime 最大时间
     * @param limit      删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM infra_job_tenant_result WHERE id IN "
            + "(SELECT id FROM infra_job_tenant_result WHERE create_time < #{createTime} LIMIT #{limit})")
    Integer deleteByCreateTimeLt(@Param("createTime") LocalDateTime createTime, @Param("limit") Integer limit);

}
