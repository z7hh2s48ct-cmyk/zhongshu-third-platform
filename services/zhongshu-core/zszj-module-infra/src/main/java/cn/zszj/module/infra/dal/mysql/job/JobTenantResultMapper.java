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
     * 物理删除父执行日志已过期的租户级明细
     *
     * 与父执行日志（infra_job_log）共用同一保留期：父日志被 JobLogCleanJob 物理删除后，
     * 明细既无法回连 handler/param，也没有任何查询入口，会成为永久留存的死数据。
     *
     * codex r1 指出：明细在执行结束后创建，父日志在执行开始前创建，两者 create_time
     * 可能跨越保留截止日期（父日志已过期、明细尚未过期），按明细自身 create_time 清理
     * 会留下孤儿。改为 JOIN 父日志按父日志 create_time 判定过期，确保级联语义。
     *
     * @param createTime 父日志的最大时间
     * @param limit      删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM infra_job_tenant_result WHERE id IN "
            + "(SELECT t.id FROM infra_job_tenant_result t "
            + "INNER JOIN infra_job_log l ON t.job_log_id = l.id "
            + "WHERE l.create_time < #{createTime} LIMIT #{limit})")
    Integer deleteByCreateTimeLt(@Param("createTime") LocalDateTime createTime, @Param("limit") Integer limit);

    /**
     * 物理删除父执行日志已不存在的孤儿明细
     *
     * codex r2 指出：JOIN 级联清理与父日志删除是两步操作，长执行任务可能在两步之间
     * 写入明细（父日志随后被删），该明细因 JOIN 找不到父日志而永久留存。
     * 本方法按明细自身 create_time 兜底回收孤儿，与旧版按自身时间清理的语义对齐。
     *
     * @param createTime 明细的最大时间（与父日志共用同一保留期）
     * @param limit      删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM infra_job_tenant_result WHERE id IN "
            + "(SELECT t.id FROM infra_job_tenant_result t "
            + "WHERE NOT EXISTS (SELECT 1 FROM infra_job_log l WHERE l.id = t.job_log_id) "
            + "AND t.create_time < #{createTime} LIMIT #{limit})")
    Integer deleteOrphanByCreateTimeLt(@Param("createTime") LocalDateTime createTime, @Param("limit") Integer limit);

}
