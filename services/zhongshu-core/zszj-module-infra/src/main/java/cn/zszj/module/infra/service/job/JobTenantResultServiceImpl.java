package cn.zszj.module.infra.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import cn.zszj.module.infra.dal.mysql.job.JobTenantResultMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 定时任务租户级执行结果 Service 实现类（ZS-JOB-001）
 *
 * 落库前对摘要做两件事，且顺序不可颠倒：
 * 1. 脱敏：异常根因里常带口令/令牌，先按"键=值"结构把值替换为 {@value #MASK}；
 * 2. 截断：再截到 {@link #SUMMARY_MAX_LENGTH} 字符，与 DB 列宽对齐。
 * 先截断再脱敏会让密钥被截成半截后残留在库里，因此必须先脱敏。
 */
@Service
@Validated
@Slf4j
public class JobTenantResultServiceImpl implements JobTenantResultService {

    /**
     * 摘要最大长度，与 infra_job_tenant_result 的 result_summary / error_summary 列宽一致
     */
    static final int SUMMARY_MAX_LENGTH = 512;
    /**
     * 脱敏占位符
     */
    static final String MASK = "***";
    /**
     * 敏感"键=值"结构，命中后把值替换为 {@link #MASK}
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token|access[-_]?key|api[-_]?key|private[-_]?key|authorization)"
                    + "(\\s*[=:]\\s*)(\\S+)");

    @Resource
    private JobTenantResultMapper jobTenantResultMapper;

    @Override
    @Async
    public void saveTenantResultsAsync(Long jobLogId, TenantJobExecutionResult result) {
        if (jobLogId == null || result == null || CollUtil.isEmpty(result.getPerTenantResults())) {
            return;
        }
        try {
            List<JobTenantResultDO> details = new ArrayList<>(result.getPerTenantResults().size());
            for (Map.Entry<Long, TenantJobExecutionResult.TenantItem> entry : result.getPerTenantResults().entrySet()) {
                TenantJobExecutionResult.TenantItem item = entry.getValue();
                if (item == null) {
                    continue;
                }
                details.add(JobTenantResultDO.builder()
                        .jobLogId(jobLogId)
                        .tenantId(entry.getKey())
                        .success(item.isSuccess())
                        .durationMs(item.getDurationMs())
                        .resultSummary(summarize(item.getResult()))
                        .errorSummary(summarize(item.getError()))
                        .build());
            }
            if (CollUtil.isEmpty(details)) {
                return;
            }
            jobTenantResultMapper.insertBatch(details);
        } catch (Exception ex) {
            // 明细落库失败不能影响任务本身：执行日志中仍保留完整的结构化结果
            log.error("[saveTenantResultsAsync][jobLogId({}) 记录租户级执行明细失败]", jobLogId, ex);
        }
    }

    @Override
    public List<JobTenantResultDO> getJobTenantResultList(Long jobLogId) {
        return jobTenantResultMapper.selectListByJobLogId(jobLogId);
    }

    @Override
    public Long getJobTenantFailureCount(Long jobLogId) {
        return jobTenantResultMapper.selectCountByJobLogIdAndSuccess(jobLogId, false);
    }

    /**
     * 先脱敏再截断，产出可安全入库、可安全回显的摘要
     *
     * @param text 原始文本
     * @return 摘要，入参为空时返回空串
     */
    private static String summarize(String text) {
        if (StrUtil.isBlank(text)) {
            return StrUtil.EMPTY;
        }
        String masked = SENSITIVE_PATTERN.matcher(text).replaceAll("$1$2" + MASK);
        return masked.length() <= SUMMARY_MAX_LENGTH ? masked : masked.substring(0, SUMMARY_MAX_LENGTH);
    }

}
