package cn.zszj.module.infra.service.job;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.zszj.module.infra.dal.dataobject.job.JobLogDO;
import cn.zszj.module.infra.dal.mysql.job.JobLogMapper;
import cn.zszj.module.infra.dal.mysql.job.JobTenantResultMapper;
import cn.zszj.module.infra.enums.job.JobLogStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * Job 日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class JobLogServiceImpl implements JobLogService {

    @Resource
    private JobLogMapper jobLogMapper;
    /**
     * 租户级执行明细（ZS-JOB-001）：只为清理服务，查询仍走 {@link JobTenantResultService}
     */
    @Resource
    private JobTenantResultMapper jobTenantResultMapper;

    @Override
    public Long createJobLog(Long jobId, LocalDateTime beginTime,
                             String jobHandlerName, String jobHandlerParam, Integer executeIndex) {
        JobLogDO log = JobLogDO.builder().jobId(jobId).handlerName(jobHandlerName)
                .handlerParam(jobHandlerParam).executeIndex(executeIndex)
                .beginTime(beginTime).status(JobLogStatusEnum.RUNNING.getStatus()).build();
        jobLogMapper.insert(log);
        return log.getId();
    }

    @Override
    @Async
    public void updateJobLogResultAsync(Long logId, LocalDateTime endTime, Integer duration, boolean success, String result) {
        try {
            JobLogDO updateObj = JobLogDO.builder().id(logId).endTime(endTime).duration(duration)
                    .status(success ? JobLogStatusEnum.SUCCESS.getStatus() : JobLogStatusEnum.FAILURE.getStatus())
                    .result(result).build();
            jobLogMapper.updateById(updateObj);
        } catch (Exception ex) {
            log.error("[updateJobLogResultAsync][logId({}) endTime({}) duration({}) success({}) result({})]",
                    logId, endTime, duration, success, result);
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Integer cleanJobLog(Integer exceedDay, Integer deleteLimit) {
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        // 先清租户级执行明细（ZS-JOB-001）：它与父执行日志同生命周期，父日志被物理删除后，
        // 明细既无法回连 handler/param，也没有任何查询入口，会成为永久留存的死数据
        cleanJobTenantResult(expireDate, deleteLimit);
        int count = 0;
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = jobLogMapper.deleteByCreateTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }

    /**
     * 清理保留期外的租户级执行明细
     *
     * 一次执行会产生“租户数”条明细，行数多于父日志，因此不能搭在父日志的分批循环里（会提前 break 而漏删），
     * 这里单独循环删到底。
     *
     * @param expireDate  过期时间点
     * @param deleteLimit 单批删除上限
     */
    private void cleanJobTenantResult(LocalDateTime expireDate, Integer deleteLimit) {
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = jobTenantResultMapper.deleteByCreateTimeLt(expireDate, deleteLimit);
            if (deleteCount < deleteLimit) {
                break;
            }
        }
    
        // 兜底：回收父日志已被删除的孤儿明细（codex r2 [P2] 长执行任务在两步操作间写入的场景）
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = jobTenantResultMapper.deleteOrphanByCreateTimeLt(expireDate, deleteLimit);
            if (deleteCount < deleteLimit) {
                break;
            }
        }
    }

    @Override
    public JobLogDO getJobLog(Long id) {
        return jobLogMapper.selectById(id);
    }

    @Override
    public PageResult<JobLogDO> getJobLogPage(JobLogPageReqVO pageReqVO) {
        return jobLogMapper.selectPage(pageReqVO);
    }

}
