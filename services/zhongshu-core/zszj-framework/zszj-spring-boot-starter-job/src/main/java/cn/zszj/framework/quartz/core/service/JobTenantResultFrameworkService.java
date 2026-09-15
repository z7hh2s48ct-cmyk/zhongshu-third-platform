package cn.zszj.framework.quartz.core.service;

import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;

/**
 * Job 租户级执行结果 Framework Service 接口（ZS-JOB-001）
 *
 * 由 infra 模块提供实现，把每技术租户的成功/失败/耗时明细持久化，支撑"某租户失败可单独查/补偿"。
 * 定时任务调用者以可选依赖的方式持有本接口：未装配 infra 模块时，执行日志中仍保留完整的结构化结果，
 * 只是不落明细表。
 */
public interface JobTenantResultFrameworkService {

    /**
     * 保存一次执行的租户级明细
     *
     * @param jobLogId 执行日志编号，关联 infra_job_log.id
     * @param result   租户级结构化执行结果
     */
    void saveTenantResultsAsync(Long jobLogId, TenantJobExecutionResult result);

}
