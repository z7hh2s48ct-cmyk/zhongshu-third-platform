package cn.zszj.framework.quartz.core.handler;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;
import cn.zszj.framework.quartz.core.enums.JobDataKeyEnum;
import cn.zszj.framework.quartz.core.service.JobLogFrameworkService;
import cn.zszj.framework.quartz.core.service.JobTenantResultFrameworkService;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistValidator;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static cn.hutool.core.exceptions.ExceptionUtil.getRootCauseMessage;
import static cn.zszj.framework.quartz.core.enums.JobFrameworkErrorCodes.TENANT_PARTIAL_FAILURE_CODE;

/**
 * 基础 Job 调用者，负责调用 {@link JobHandler#execute(String)} 执行任务
 *
 * @author 芋道源码
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Slf4j
public class JobHandlerInvoker extends QuartzJobBean {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private JobLogFrameworkService jobLogFrameworkService;

    /**
     * JobHandler 白名单校验器（ZS-JOB-001）：执行前拦截，未列入白名单的 Handler 不进入执行
     */
    @Resource
    private JobHandlerWhitelistValidator jobHandlerWhitelistValidator;

    /**
     * 租户级执行明细的持久化端口（ZS-JOB-001）
     *
     * 以可选依赖方式持有：未装配 infra 模块时为空，此时执行日志仍保留完整的结构化结果，只是不落明细表。
     */
    @Autowired(required = false)
    private JobTenantResultFrameworkService jobTenantResultFrameworkService;

    @Override
    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        // 第一步，获得 Job 数据
        Long jobId = executionContext.getMergedJobDataMap().getLong(JobDataKeyEnum.JOB_ID.name());
        String jobHandlerName = executionContext.getMergedJobDataMap().getString(JobDataKeyEnum.JOB_HANDLER_NAME.name());
        String jobHandlerParam = executionContext.getMergedJobDataMap().getString(JobDataKeyEnum.JOB_HANDLER_PARAM.name());
        int refireCount  = executionContext.getRefireCount();
        int retryCount = (Integer) executionContext.getMergedJobDataMap().getOrDefault(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 0);
        int retryInterval = (Integer) executionContext.getMergedJobDataMap().getOrDefault(JobDataKeyEnum.JOB_RETRY_INTERVAL.name(), 0);

        // 第二步，执行任务
        Long jobLogId = null;
        LocalDateTime startTime = LocalDateTime.now();
        String data = null;
        Throwable exception = null;
        int tenantFailureCount = 0;
        try {
            // 记录 Job 日志（初始）
            jobLogId = jobLogFrameworkService.createJobLog(jobId, startTime, jobHandlerName, jobHandlerParam, refireCount + 1);
            // 校验 Handler 白名单：模块关闭或 Handler 未固化到配置时，直接失败并留下执行日志
            jobHandlerWhitelistValidator.validate(jobHandlerName);
            // 执行任务
            data = this.executeInternal(jobHandlerName, jobHandlerParam);
            // 解析租户级执行结果：某租户失败不能被汇总为整体成功
            TenantJobExecutionResult tenantResult = TenantJobExecutionResult.tryParse(data);
            if (tenantResult != null) {
                tenantFailureCount = tenantResult.getFailureCount();
                this.handleTenantResult(jobHandlerName, jobLogId, tenantResult);
            }
        } catch (Throwable ex) {
            exception = ex;
        }

        // 第三步，记录执行日志
        this.updateJobLogResultAsync(jobLogId, startTime, data, exception, tenantFailureCount, executionContext);

        // 第四步，处理有异常的情况
        handleException(exception, refireCount, retryCount, retryInterval);
    }

    /**
     * 处理租户级执行结果：落库逐租户明细，并对存在失败的情况留痕
     *
     * 注意，这里刻意不把"租户级部分失败"升级为异常抛出：抛出会让 Quartz 立即重放整个任务，
     * 已成功租户的副作用会被重复执行。失败明细落库后，由补偿/恢复台账按租户粒度处理。
     */
    private void handleTenantResult(String jobHandlerName, Long jobLogId, TenantJobExecutionResult tenantResult) {
        if (tenantResult.getFailureCount() > 0) {
            log.warn("[handleTenantResult][Job({}) logId({}) 存在租户级失败，错误码({})：总数({}) 成功({}) 失败({})；"
                            + "整体判失败，但不触发调度器重放，避免已成功租户的副作用被重复执行]",
                    jobHandlerName, jobLogId, TENANT_PARTIAL_FAILURE_CODE, tenantResult.getTotalTenants(),
                    tenantResult.getSuccessCount(), tenantResult.getFailureCount());
        }
        if (jobLogId == null || jobTenantResultFrameworkService == null) {
            return;
        }
        try {
            jobTenantResultFrameworkService.saveTenantResultsAsync(jobLogId, tenantResult);
        } catch (Exception ex) {
            // 明细落库失败不影响任务执行结果的判定，执行日志中仍保留完整的结构化结果
            log.error("[handleTenantResult][Job({}) logId({}) 记录租户级执行明细失败]", jobHandlerName, jobLogId, ex);
        }
    }

    private String executeInternal(String jobHandlerName, String jobHandlerParam) throws Exception {
        // 获得 JobHandler 对象
        JobHandler jobHandler = applicationContext.getBean(jobHandlerName, JobHandler.class);
        Assert.notNull(jobHandler, "JobHandler 不会为空");
        // 执行任务
        return jobHandler.execute(jobHandlerParam);
    }

    private void updateJobLogResultAsync(Long jobLogId, LocalDateTime startTime, String data, Throwable exception,
                                         int tenantFailureCount, JobExecutionContext executionContext) {
        LocalDateTime endTime = LocalDateTime.now();
        // 处理是否成功：既没有异常，也不存在租户级失败。
        // 仅看 exception 会把"部分租户失败"汇总成整体成功，这里以 tenantFailureCount 兜住这一类漏判。
        boolean success = exception == null && tenantFailureCount == 0;
        if (exception != null) {
            data = getRootCauseMessage(exception);
        }
        // 更新日志
        try {
            jobLogFrameworkService.updateJobLogResultAsync(jobLogId, endTime, (int) LocalDateTimeUtil.between(startTime, endTime).toMillis(), success, data);
        } catch (Exception ex) {
            log.error("[executeInternal][Job({}) logId({}) 记录执行日志失败({}/{})]",
                    executionContext.getJobDetail().getKey(), jobLogId, success, data);
        }
    }

    private void handleException(Throwable exception,
                                 int refireCount, int retryCount, int retryInterval) throws JobExecutionException {
        // 如果有异常，则进行重试
        if (exception == null) {
            return;
        }
        // 情况一：如果到达重试上限，则直接抛出异常即可
        if (refireCount >= retryCount) {
            throw new JobExecutionException(exception);
        }

        // 情况二：如果未到达重试上限，则 sleep 一定间隔时间，然后重试
        // 这里使用 sleep 来实现，主要还是希望实现比较简单。因为，同一时间，不会存在大量失败的 Job。
        if (retryInterval > 0) {
            ThreadUtil.sleep(retryInterval);
        }
        // 第二个参数，refireImmediately = true，表示立即重试
        throw new JobExecutionException(exception, true);
    }

}
