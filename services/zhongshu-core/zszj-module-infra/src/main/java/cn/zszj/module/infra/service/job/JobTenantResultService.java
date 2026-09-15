package cn.zszj.module.infra.service.job;

import cn.zszj.framework.quartz.core.service.JobTenantResultFrameworkService;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;

import java.util.List;

/**
 * 定时任务租户级执行结果 Service 接口（ZS-JOB-001）
 *
 * 写入侧由 {@link JobTenantResultFrameworkService} 暴露给 framework 的 JobHandlerInvoker，
 * 查询侧供 infra 管理端与运维对账使用。
 */
public interface JobTenantResultService extends JobTenantResultFrameworkService {

    /**
     * 查询一次执行的全部租户级明细
     *
     * @param jobLogId 执行日志编号
     * @return 明细列表，按租户编号升序
     */
    List<JobTenantResultDO> getJobTenantResultList(Long jobLogId);

    /**
     * 统计一次执行中失败的租户数量
     *
     * @param jobLogId 执行日志编号
     * @return 失败租户数量，无明细时为 0
     */
    Long getJobTenantFailureCount(Long jobLogId);

}
