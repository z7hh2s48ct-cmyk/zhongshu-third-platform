package cn.zszj.framework.tenant.core.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;
import cn.zszj.framework.tenant.core.service.TenantFrameworkService;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多租户 JobHandler AOP
 * 任务执行时，会按照租户逐个执行 Job 的逻辑
 *
 * 返回值为 {@link TenantJobExecutionResult} 的 JSON 序列化结果（ZS-JOB-001）：
 * 1. 顶层给出 totalTenants/successCount/failureCount，使调用者能把"某租户失败"判为整体失败，不再汇总为成功；
 * 2. perTenantResults 保留逐租户的成功标记、耗时与异常根因，支撑按租户单独查询与补偿。
 *
 * 注意，需要保证 JobHandler 的幂等性。虽然调用者已不会因租户级失败而重放整个任务，
 * 但任务本身的异常重试、手工重跑与多实例部署仍可能让同一租户被重复执行。
 *
 * @author 芋道源码
 */
@Aspect
@RequiredArgsConstructor
@Slf4j
public class TenantJobAspect {

    private final TenantFrameworkService tenantFrameworkService;

    @Around("@annotation(tenantJob)")
    public String around(ProceedingJoinPoint joinPoint, TenantJob tenantJob) {
        // 获得租户列表
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return null;
        }

        // 逐个租户，执行 Job。多个租户汇总为一条执行日志，但每个租户的成败与耗时单独记录
        Map<Long, TenantJobExecutionResult.TenantItem> perTenantResults = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        tenantIds.parallelStream().forEach(tenantId -> TenantUtils.execute(tenantId, () -> {
            long beginMillis = System.currentTimeMillis();
            try {
                Object result = joinPoint.proceed();
                perTenantResults.put(tenantId, TenantJobExecutionResult.TenantItem.builder()
                        .success(true)
                        .durationMs(System.currentTimeMillis() - beginMillis)
                        .result(StrUtil.toStringOrEmpty(result))
                        .build());
                successCount.incrementAndGet();
            } catch (Throwable e) {
                log.error("[execute][租户({}) 执行 Job 发生异常]", tenantId, e);
                perTenantResults.put(tenantId, TenantJobExecutionResult.TenantItem.builder()
                        .success(false)
                        .durationMs(System.currentTimeMillis() - beginMillis)
                        .error(ExceptionUtil.getRootCauseMessage(e))
                        .build());
                failureCount.incrementAndGet();
            }
        }));

        // 汇总结构化结果：存在租户级失败时留痕，不掩盖为成功
        TenantJobExecutionResult executionResult = TenantJobExecutionResult.builder()
                .totalTenants(tenantIds.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .perTenantResults(perTenantResults)
                .build();
        if (failureCount.get() > 0) {
            log.warn("[around][租户 Job 执行存在失败：总数({}) 成功({}) 失败({}) 失败租户({})]",
                    tenantIds.size(), successCount.get(), failureCount.get(), failedTenantIds(perTenantResults));
        }
        return executionResult.toJsonString();
    }

    private static List<Long> failedTenantIds(Map<Long, TenantJobExecutionResult.TenantItem> perTenantResults) {
        return perTenantResults.entrySet().stream()
                .filter(entry -> !entry.getValue().isSuccess())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

}
