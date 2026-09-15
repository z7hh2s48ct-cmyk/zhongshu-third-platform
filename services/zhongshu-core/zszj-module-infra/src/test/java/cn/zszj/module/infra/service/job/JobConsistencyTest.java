package cn.zszj.module.infra.service.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;
import cn.zszj.framework.quartz.core.enums.JobDataKeyEnum;
import cn.zszj.framework.quartz.core.handler.JobHandler;
import cn.zszj.framework.quartz.core.handler.JobHandlerInvoker;
import cn.zszj.framework.quartz.core.scheduler.SchedulerManager;
import cn.zszj.framework.quartz.core.service.JobLogFrameworkService;
import cn.zszj.framework.quartz.core.service.JobTenantResultFrameworkService;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistProperties;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistValidator;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.job.TenantJob;
import cn.zszj.framework.tenant.core.job.TenantJobAspect;
import cn.zszj.framework.tenant.core.service.TenantFrameworkService;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.job.JobDO;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import cn.zszj.module.infra.dal.mysql.job.JobMapper;
import cn.zszj.module.infra.enums.job.JobStatusEnum;
import cn.zszj.module.infra.job.job.JobLogCleanJob;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.JOB_HANDLER_BEAN_NOT_EXISTS;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.JOB_HANDLER_NOT_WHITELISTED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.JOB_TRIGGER_ON_PAUSED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-JOB-001：任务启停一致性与租户级结果的一致性测试（H2）
 *
 * 覆盖验收场景：
 * 1. 创建/修改/暂停/重启恢复后，任务表状态与调度器状态一致；
 * 2. 无效 Handler 拒绝（Bean 未注册、Bean 已注册但未列入白名单）；
 * 3. 模块关闭（Bean 缺失）或未列入白名单时，任务不能进入执行；
 * 4. 暂停（含 INIT 非开启态）任务手动触发被拒；
 * 5. 某租户失败被单独记录，不被汇总为整体成功；
 * 6. 其他租户不被重复副作用影响（租户级部分失败不触发 Quartz 重放）；
 * 7. 任务表/调度器状态漂移由对账机制以任务表为权威修正（含 trigger 丢失、孤儿 trigger、调度器不可用）。
 */
@Import({JobServiceImpl.class, JobTenantResultServiceImpl.class, JobSchedulerReconciler.class,
        JobHandlerWhitelistValidator.class, JobConsistencyTest.JobWhitelistPropertiesConfiguration.class})
@TestPropertySource(properties = {
        "zszj.job.handler-whitelist[0]=" + JobConsistencyTest.WHITELISTED_HANDLER,
        "zszj.job.handler-whitelist[1]=jobLogCleanJob",
        "zszj.job.handler-whitelist[2]=" + JobConsistencyTest.MODULE_DISABLED_HANDLER,
        "zszj.job.handler-whitelist[3]=" + JobConsistencyTest.TENANT_JOB_HANDLER
})
public class JobConsistencyTest extends BaseDbUnitTest {

    /** 白名单内的 Handler 名字 */
    static final String WHITELISTED_HANDLER = "demoJobHandler";
    /** 白名单内、但所在模块已关闭（Bean 缺失）的 Handler 名字 */
    static final String MODULE_DISABLED_HANDLER = "moduleDisabledJobHandler";
    /** 白名单内、承载多租户执行的 Handler 名字 */
    static final String TENANT_JOB_HANDLER = "tenantDemoJobHandler";
    /** Bean 已注册、但未列入白名单的 Handler 名字 */
    private static final String OUTSIDE_WHITELIST_HANDLER = "outsideWhitelistJobHandler";

    private static final String VALID_CRON = "0 0/1 * * * ? *";

    @Resource
    private JobServiceImpl jobService;
    @Resource
    private JobMapper jobMapper;
    @Resource
    private JobSchedulerReconciler jobSchedulerReconciler;
    @Resource
    private JobTenantResultService jobTenantResultService;
    @Resource
    private JobHandlerWhitelistValidator jobHandlerWhitelistValidator;

    @MockitoBean
    private SchedulerManager schedulerManager;
    @MockitoBean
    private Scheduler scheduler;
    @MockitoBean
    private JobLogCleanJob demoJobHandlerBean;

    /**
     * 白名单配置项的绑定入口。{@link BaseDbUnitTest} 只加载 DB/MyBatis 相关配置，
     * 不会加载定时任务的自动装配，因此这里显式开启配置属性绑定。
     */
    @EnableConfigurationProperties(JobHandlerWhitelistProperties.class)
    static class JobWhitelistPropertiesConfiguration {
    }

    /**
     * 供测试获取 {@link TenantJob} 注解实例（该注解为 RUNTIME 保留）
     */
    @TenantJob
    public String tenantJobFixture() {
        return "fixture";
    }

    // ==================== 场景 1：创建/修改/暂停/重启恢复状态一致 ====================

    @Test
    public void testScenario01_statusConsistentAcrossCreateUpdatePauseResumeAndSync() throws SchedulerException {
        // 1.1 创建：任务表落 NORMAL，调度器 addJob
        JobSaveReqVO createReqVO = randomPojo(JobSaveReqVO.class, o -> {
            o.setId(null);
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        Long jobId;
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(eq(WHITELISTED_HANDLER))).thenReturn(demoJobHandlerBean);
            jobId = jobService.createJob(createReqVO);
        }
        JobDO created = jobMapper.selectById(jobId);
        assertEquals(JobStatusEnum.NORMAL.getStatus(), created.getStatus());
        verify(schedulerManager).addJob(eq(jobId), eq(WHITELISTED_HANDLER), eq(created.getHandlerParam()),
                eq(VALID_CRON), eq(createReqVO.getRetryCount()), eq(createReqVO.getRetryInterval()));

        // 1.2 对账：调度器状态与任务表一致，0 漂移
        when(scheduler.getTriggerState(new TriggerKey(WHITELISTED_HANDLER))).thenReturn(Trigger.TriggerState.NORMAL);
        JobSchedulerReconcileReport consistentReport = jobSchedulerReconciler.reconcile();
        assertTrue(consistentReport.isSchedulerAvailable());
        assertEquals(1, consistentReport.getChecked());
        assertEquals(0, consistentReport.getDrifted());
        assertEquals(0, consistentReport.getCorrected());

        // 1.3 修改：任务表与调度器同步变更
        JobSaveReqVO updateReqVO = randomPojo(JobSaveReqVO.class, o -> {
            o.setId(jobId);
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression("0 0/2 * * * ? *");
        });
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(eq(WHITELISTED_HANDLER))).thenReturn(demoJobHandlerBean);
            jobService.updateJob(updateReqVO);
        }
        assertEquals("0 0/2 * * * ? *", jobMapper.selectById(jobId).getCronExpression());
        verify(schedulerManager).updateJob(eq(WHITELISTED_HANDLER), eq(updateReqVO.getHandlerParam()),
                eq("0 0/2 * * * ? *"), eq(updateReqVO.getRetryCount()), eq(updateReqVO.getRetryInterval()));

        // 1.4 暂停：任务表 STOP，调度器 pauseJob，对账一致
        jobService.updateJobStatus(jobId, JobStatusEnum.STOP.getStatus());
        assertEquals(JobStatusEnum.STOP.getStatus(), jobMapper.selectById(jobId).getStatus());
        verify(schedulerManager).pauseJob(eq(WHITELISTED_HANDLER));
        when(scheduler.getTriggerState(new TriggerKey(WHITELISTED_HANDLER))).thenReturn(Trigger.TriggerState.PAUSED);
        assertEquals(0, jobSchedulerReconciler.reconcile().getDrifted());

        // 1.5 重启恢复：任务表 NORMAL，调度器 resumeJob，对账一致
        jobService.updateJobStatus(jobId, JobStatusEnum.NORMAL.getStatus());
        assertEquals(JobStatusEnum.NORMAL.getStatus(), jobMapper.selectById(jobId).getStatus());
        verify(schedulerManager).resumeJob(eq(WHITELISTED_HANDLER));
        when(scheduler.getTriggerState(new TriggerKey(WHITELISTED_HANDLER))).thenReturn(Trigger.TriggerState.NORMAL);
        assertEquals(0, jobSchedulerReconciler.reconcile().getDrifted());

        // 1.6 进程重启：syncJob 以任务表为权威重建调度器，并对账确认无残留漂移
        jobService.syncJob();
        verify(schedulerManager).deleteJob(eq(WHITELISTED_HANDLER));
        verify(schedulerManager, atLeast(2)).addJob(eq(jobId), eq(WHITELISTED_HANDLER), any(), any(), any(), any());
    }

    // ==================== 场景 2：无效 Handler 拒绝 ====================

    @Test
    public void testScenario02_invalidHandlerRejectedOnCreateAndUpdate() throws SchedulerException {
        // 2.1 Handler Bean 未注册：JOB_HANDLER_BEAN_NOT_EXISTS
        String missingHandlerName = OUTSIDE_WHITELIST_HANDLER + "Missing";
        JobSaveReqVO missingReqVO = randomPojo(JobSaveReqVO.class, o -> {
            o.setId(null);
            o.setHandlerName(missingHandlerName);
            o.setCronExpression(VALID_CRON);
        });
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(eq(missingHandlerName)))
                    .thenThrow(new NoSuchBeanDefinitionException(missingHandlerName));
            assertServiceException(() -> jobService.createJob(missingReqVO), JOB_HANDLER_BEAN_NOT_EXISTS);
        }
        assertNull(jobMapper.selectByHandlerName(missingHandlerName));

        // 2.2 Handler Bean 已注册、但未列入白名单：JOB_HANDLER_NOT_WHITELISTED
        JobSaveReqVO outsideReqVO = randomPojo(JobSaveReqVO.class, o -> {
            o.setId(null);
            o.setHandlerName(OUTSIDE_WHITELIST_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(eq(OUTSIDE_WHITELIST_HANDLER))).thenReturn(demoJobHandlerBean);
            assertServiceException(() -> jobService.createJob(outsideReqVO),
                    JOB_HANDLER_NOT_WHITELISTED, OUTSIDE_WHITELIST_HANDLER);
        }
        assertNull(jobMapper.selectByHandlerName(OUTSIDE_WHITELIST_HANDLER));
        verify(schedulerManager, never()).addJob(any(), any(), any(), any(), any(), any());

        // 2.3 修改为未列入白名单的 Handler：同样拒绝，任务表与调度器均不变
        JobDO normalJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(normalJob);
        JobSaveReqVO updateReqVO = randomPojo(JobSaveReqVO.class, o -> {
            o.setId(normalJob.getId());
            o.setHandlerName(OUTSIDE_WHITELIST_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(eq(OUTSIDE_WHITELIST_HANDLER))).thenReturn(demoJobHandlerBean);
            assertServiceException(() -> jobService.updateJob(updateReqVO),
                    JOB_HANDLER_NOT_WHITELISTED, OUTSIDE_WHITELIST_HANDLER);
        }
        assertEquals(WHITELISTED_HANDLER, jobMapper.selectById(normalJob.getId()).getHandlerName());
        verify(schedulerManager, never()).updateJob(any(), any(), any(), any(), any());
    }

    // ==================== 场景 3：模块关闭 / 未白名单，不能进入执行 ====================

    @Test
    public void testScenario03_disabledModuleOrNotWhitelistedCannotExecute() {
        // 3.1 模块关闭：白名单内有名字，但容器中 Bean 缺失 → 执行失败并落执行日志
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(eq(MODULE_DISABLED_HANDLER), eq(JobHandler.class)))
                .thenThrow(new NoSuchBeanDefinitionException(MODULE_DISABLED_HANDLER));
        JobLogFrameworkService jobLogFrameworkService = mock(JobLogFrameworkService.class);
        when(jobLogFrameworkService.createJobLog(any(), any(), any(), any(), any())).thenReturn(9003L);
        JobHandlerInvoker invoker = newInvoker(applicationContext, jobLogFrameworkService, jobTenantResultService);

        assertThrows(JobExecutionException.class, () -> invoker.execute(newContext(MODULE_DISABLED_HANDLER, null)));
        verify(jobLogFrameworkService).updateJobLogResultAsync(eq(9003L), any(), anyInt(), eq(false),
                contains(MODULE_DISABLED_HANDLER));

        // 3.2 Bean 存在但未列入白名单：执行前即拦截，Handler 根本不被取出
        ApplicationContext outsideContext = mock(ApplicationContext.class);
        when(outsideContext.getBean(eq(OUTSIDE_WHITELIST_HANDLER), eq(JobHandler.class)))
                .thenReturn(param -> "should-never-run");
        JobLogFrameworkService outsideLogService = mock(JobLogFrameworkService.class);
        when(outsideLogService.createJobLog(any(), any(), any(), any(), any())).thenReturn(9004L);
        JobHandlerInvoker outsideInvoker = newInvoker(outsideContext, outsideLogService, jobTenantResultService);

        assertThrows(JobExecutionException.class,
                () -> outsideInvoker.execute(newContext(OUTSIDE_WHITELIST_HANDLER, null)));
        verify(outsideLogService).updateJobLogResultAsync(eq(9004L), any(), anyInt(), eq(false),
                contains(OUTSIDE_WHITELIST_HANDLER));
        verify(outsideContext, never()).getBean(eq(OUTSIDE_WHITELIST_HANDLER), eq(JobHandler.class));
    }

    // ==================== 场景 4：暂停任务手动触发被拒 ====================

    @Test
    public void testScenario04_triggerJobRejectedUnlessNormal() throws SchedulerException {
        // 4.1 STOP：拒绝
        JobDO stoppedJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.STOP.getStatus());
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(stoppedJob);
        assertServiceException(() -> jobService.triggerJob(stoppedJob.getId()),
                JOB_TRIGGER_ON_PAUSED, JobStatusEnum.STOP.getStatus());

        // 4.2 INIT（创建中间态，同样属于非开启态）：拒绝
        JobDO initJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.INIT.getStatus());
            o.setHandlerName("initStateJobHandler");
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(initJob);
        assertServiceException(() -> jobService.triggerJob(initJob.getId()),
                JOB_TRIGGER_ON_PAUSED, JobStatusEnum.INIT.getStatus());
        verify(schedulerManager, never()).triggerJob(any(), any(), any());

        // 4.3 NORMAL：放行
        JobDO normalJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(normalJob);
        jobService.triggerJob(normalJob.getId());
        verify(schedulerManager).triggerJob(eq(normalJob.getId()), eq(WHITELISTED_HANDLER),
                eq(normalJob.getHandlerParam()));

        // 4.4 NORMAL 但 Handler 不在白名单：手动触发作为第四个入口，同样在调度器接单前就拒
        JobDO outsideJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName(OUTSIDE_WHITELIST_HANDLER);
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(outsideJob);
        assertServiceException(() -> jobService.triggerJob(outsideJob.getId()),
                JOB_HANDLER_NOT_WHITELISTED, OUTSIDE_WHITELIST_HANDLER);
        verify(schedulerManager, never()).triggerJob(eq(outsideJob.getId()), any(), any());
    }

    // ==================== 场景 5：某租户失败单独记录，不汇总为成功 ====================

    // ProceedingJoinPoint#proceed 声明抛 Throwable（比 Exception 更宽），桩里要模拟业务异常就得让方法签名跟上
    @Test
    public void testScenario05_tenantFailureRecordedSeparatelyAndNotAggregatedAsSuccess() throws Throwable {
        long tenantA = 101L;
        long tenantB = 102L;
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            if (tenantA == tenantId) {
                throw new IllegalStateException("tenant-A-business-failure");
            }
            return "ok-" + tenantId;
        });
        String resultJson = newAspect(tenantA, tenantB).around(joinPoint, tenantJobAnnotation());

        // 5.1 切面自身：结构化结果区分成功/失败租户，不汇总为成功
        TenantJobExecutionResult parsed = TenantJobExecutionResult.tryParse(resultJson);
        assertNotNull(parsed);
        assertEquals(2, parsed.getTotalTenants());
        assertEquals(1, parsed.getSuccessCount());
        assertEquals(1, parsed.getFailureCount());
        assertFalse(parsed.isAllSuccess());
        assertTrue(parsed.isPartialFailure());
        TenantJobExecutionResult.TenantItem itemA = parsed.getPerTenantResults().get(tenantA);
        TenantJobExecutionResult.TenantItem itemB = parsed.getPerTenantResults().get(tenantB);
        assertFalse(itemA.isSuccess());
        assertTrue(itemA.getError().contains("tenant-A-business-failure"));
        assertTrue(itemA.getDurationMs() >= 0L);
        assertTrue(itemB.isSuccess());
        assertEquals("ok-" + tenantB, itemB.getResult());
        assertTrue(itemB.getDurationMs() >= 0L);

        // 5.2 调用者：执行日志判失败，且租户级明细逐行落库可单独查
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(eq(TENANT_JOB_HANDLER), eq(JobHandler.class)))
                .thenReturn(param -> resultJson);
        JobLogFrameworkService jobLogFrameworkService = mock(JobLogFrameworkService.class);
        when(jobLogFrameworkService.createJobLog(any(), any(), any(), any(), any())).thenReturn(9005L);
        newInvoker(applicationContext, jobLogFrameworkService, jobTenantResultService)
                .execute(newContext(TENANT_JOB_HANDLER, null));

        verify(jobLogFrameworkService).updateJobLogResultAsync(eq(9005L), any(), anyInt(), eq(false), eq(resultJson));
        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9005L);
        assertEquals(2, details.size());
        assertEquals(1L, jobTenantResultService.getJobTenantFailureCount(9005L).longValue());
        JobTenantResultDO detailA = findByTenantId(details, tenantA);
        JobTenantResultDO detailB = findByTenantId(details, tenantB);
        assertFalse(detailA.getSuccess());
        assertTrue(detailA.getErrorSummary().contains("tenant-A-business-failure"));
        assertTrue(detailB.getSuccess());
        assertEquals("ok-" + tenantB, detailB.getResultSummary());
    }

    // ==================== 场景 6：其他租户不被重复副作用影响 ====================

    @Test
    public void testScenario06_otherTenantsNotAffectedByReplayedSideEffects() throws Throwable {
        long tenantA = 201L;
        long tenantB = 202L;
        long tenantC = 203L;
        Map<Long, AtomicInteger> sideEffects = new ConcurrentHashMap<>();
        sideEffects.put(tenantA, new AtomicInteger());
        sideEffects.put(tenantB, new AtomicInteger());
        sideEffects.put(tenantC, new AtomicInteger());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            sideEffects.get(tenantId).incrementAndGet();
            if (tenantA == tenantId) {
                throw new IllegalStateException("tenant-A-business-failure");
            }
            return "ok";
        });
        String resultJson = newAspect(tenantA, tenantB, tenantC).around(joinPoint, tenantJobAnnotation());
        // 每租户副作用恰好发生一次
        assertEquals(1, sideEffects.get(tenantA).get());
        assertEquals(1, sideEffects.get(tenantB).get());
        assertEquals(1, sideEffects.get(tenantC).get());

        // 租户级部分失败不触发 Quartz 重放：成功租户的副作用不会被再次执行
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(eq(TENANT_JOB_HANDLER), eq(JobHandler.class)))
                .thenReturn(param -> resultJson);
        JobLogFrameworkService jobLogFrameworkService = mock(JobLogFrameworkService.class);
        when(jobLogFrameworkService.createJobLog(any(), any(), any(), any(), any())).thenReturn(9006L);
        assertDoesNotThrow(() -> newInvoker(applicationContext, jobLogFrameworkService, jobTenantResultService)
                .execute(newContext(TENANT_JOB_HANDLER, null)));

        assertEquals(1, sideEffects.get(tenantA).get());
        assertEquals(1, sideEffects.get(tenantB).get());
        assertEquals(1, sideEffects.get(tenantC).get());
        verify(jobLogFrameworkService).updateJobLogResultAsync(eq(9006L), any(), anyInt(), eq(false), any());
        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9006L);
        assertEquals(3, details.size());
        assertEquals(1L, details.stream().filter(detail -> !detail.getSuccess()).count());
        assertEquals(2L, details.stream().filter(JobTenantResultDO::getSuccess).count());
    }

    // ==================== 场景 7：任务表/调度器状态对账 ====================

    @Test
    public void testScenario07_reconcilerFixesPausedDriftWithJobTableAsAuthority() throws SchedulerException {
        // 人为制造漂移：任务表 NORMAL，调度器 trigger 处于 PAUSED
        JobDO job = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName("driftPausedJobHandler");
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(job);
        when(scheduler.getTriggerState(new TriggerKey("driftPausedJobHandler")))
                .thenReturn(Trigger.TriggerState.PAUSED);

        JobSchedulerReconcileReport report = jobSchedulerReconciler.reconcile();
        assertTrue(report.isSchedulerAvailable());
        assertEquals(1, report.getChecked());
        assertEquals(1, report.getDrifted());
        assertEquals(1, report.getCorrected());
        assertEquals(0, report.getFailed());
        assertEquals(0, report.getOrphanTriggerCount());
        JobSchedulerReconcileReport.Drift drift = report.getDrifts().get(0);
        assertEquals("driftPausedJobHandler", drift.getHandlerName());
        assertEquals(JobSchedulerReconciler.DRIFT_STATUS_MISMATCH, drift.getType());
        assertEquals(JobStatusEnum.NORMAL.getStatus(), drift.getJobStatus());
        assertEquals(Trigger.TriggerState.PAUSED.name(), drift.getTriggerState());
        // 以任务表为权威修正调度器
        verify(schedulerManager).resumeJob(eq("driftPausedJobHandler"));
        verify(schedulerManager, never()).pauseJob(any());
        // 任务表作为权威源，不被对账改写
        assertEquals(JobStatusEnum.NORMAL.getStatus(), jobMapper.selectById(job.getId()).getStatus());
        assertTrue(StrUtil.isNotBlank(report.getSummary()));
    }

    @Test
    public void testScenario07b_reconcilerRebuildsMissingTriggerAndRemovesOrphan() throws SchedulerException {
        // 任务表存在但调度器 trigger 丢失（STOP 状态），另有调度器孤儿 trigger
        JobDO job = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.STOP.getStatus());
            o.setHandlerName("vanishedJobHandler");
            o.setCronExpression(VALID_CRON);
            o.setRetryCount(0);
            o.setRetryInterval(0);
        });
        jobMapper.insert(job);
        when(scheduler.getTriggerState(new TriggerKey("vanishedJobHandler")))
                .thenReturn(Trigger.TriggerState.NONE);
        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP)))
                .thenReturn(new LinkedHashSet<>(List.of(new TriggerKey("vanishedJobHandler"),
                        new TriggerKey("orphanJobHandler"))));

        JobSchedulerReconcileReport report = jobSchedulerReconciler.reconcile();
        assertEquals(1, report.getChecked());
        assertEquals(2, report.getDrifted());
        assertEquals(2, report.getCorrected());
        assertEquals(1, report.getOrphanTriggerCount());
        assertEquals(JobSchedulerReconciler.DRIFT_TRIGGER_REBUILD, report.getDrifts().get(0).getType());
        assertEquals(JobSchedulerReconciler.DRIFT_ORPHAN_TRIGGER, report.getDrifts().get(1).getType());
        // 重建：先删后加，并按任务表 STOP 状态暂停
        verify(schedulerManager).deleteJob(eq("vanishedJobHandler"));
        verify(schedulerManager).addJob(eq(job.getId()), eq("vanishedJobHandler"), eq(job.getHandlerParam()),
                eq(VALID_CRON), eq(0), eq(0));
        verify(schedulerManager).pauseJob(eq("vanishedJobHandler"));
        // 孤儿 trigger 被清理
        verify(schedulerManager).deleteJob(eq("orphanJobHandler"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testScenario07c_reconcilerIsFailSafeWhenSchedulerUnavailable() throws SchedulerException {
        // 定时任务已禁用（无 Scheduler Bean）：对账返回不可用报告，不抛异常、不误改调度器
        ObjectProvider<Scheduler> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        JobSchedulerReconciler standalone = new JobSchedulerReconciler();
        ReflectionTestUtils.setField(standalone, "jobMapper", jobMapper);
        ReflectionTestUtils.setField(standalone, "schedulerManager", schedulerManager);
        ReflectionTestUtils.setField(standalone, "schedulerProvider", emptyProvider);
        JobDO job = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName("noSchedulerJobHandler");
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(job);

        JobSchedulerReconcileReport report = assertDoesNotThrow(standalone::reconcile);
        assertFalse(report.isSchedulerAvailable());
        assertEquals(0, report.getChecked());
        assertEquals(0, report.getDrifted());
        verify(schedulerManager, never()).pauseJob(any());
        verify(schedulerManager, never()).resumeJob(any());
        // 单任务对账入口同样 fail-safe
        assertDoesNotThrow(() -> standalone.reconcileJob(job.getId()));
    }

    // ==================== 补充：白名单语义与租户明细脱敏 ====================

    @Test
    public void testScenario08_handlerWhitelistSemanticsAreConfigurationDriven() {
        // 空白名单 = 未启用管控（放行）；显式配置后仅放行清单内的 Handler
        JobHandlerWhitelistProperties emptyProperties = new JobHandlerWhitelistProperties();
        JobHandlerWhitelistValidator disabledValidator = new JobHandlerWhitelistValidator(emptyProperties);
        assertFalse(disabledValidator.isEnabled());
        assertTrue(disabledValidator.isWhitelisted("whateverJobHandler"));
        assertDoesNotThrow(() -> disabledValidator.validate("whateverJobHandler"));

        JobHandlerWhitelistProperties properties = new JobHandlerWhitelistProperties();
        properties.setHandlerWhitelist(List.of("allowedJobHandler"));
        JobHandlerWhitelistValidator enabledValidator = new JobHandlerWhitelistValidator(properties);
        assertTrue(enabledValidator.isEnabled());
        assertTrue(enabledValidator.isWhitelisted("allowedJobHandler"));
        assertFalse(enabledValidator.isWhitelisted("deniedJobHandler"));
        assertDoesNotThrow(() -> enabledValidator.validate("allowedJobHandler"));
        assertServiceException(() -> enabledValidator.validate("deniedJobHandler"),
                JOB_HANDLER_NOT_WHITELISTED, "deniedJobHandler");

        // 容器内的校验器读到 @TestPropertySource 注入的白名单
        assertTrue(jobHandlerWhitelistValidator.isEnabled());
        assertTrue(jobHandlerWhitelistValidator.isWhitelisted(WHITELISTED_HANDLER));
        assertTrue(jobHandlerWhitelistValidator.isWhitelisted(MODULE_DISABLED_HANDLER));
        assertTrue(jobHandlerWhitelistValidator.isWhitelisted(TENANT_JOB_HANDLER));
        assertFalse(jobHandlerWhitelistValidator.isWhitelisted(OUTSIDE_WHITELIST_HANDLER));
    }

    @Test
    public void testScenario09_tenantResultSummaryIsTruncatedAndDesensitized() {
        String secretError = "auth failed password=Sup3rSecret token=abc.def.ghi " + StrUtil.repeat("x", 600);
        TenantJobExecutionResult result = TenantJobExecutionResult.builder()
                .totalTenants(1).successCount(0).failureCount(1)
                .perTenantResults(Map.of(301L, TenantJobExecutionResult.TenantItem.builder()
                        .success(false).durationMs(12L).error(secretError).build()))
                .build();

        jobTenantResultService.saveTenantResultsAsync(9009L, result);

        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9009L);
        assertEquals(1, details.size());
        JobTenantResultDO detail = details.get(0);
        assertEquals(9009L, detail.getJobLogId().longValue());
        assertEquals(301L, detail.getTenantId().longValue());
        assertFalse(detail.getSuccess());
        assertEquals(12L, detail.getDurationMs().longValue());
        // 脱敏 + 截断（先脱敏再截断，避免密钥被截断后残留半截）
        assertTrue(detail.getErrorSummary().length() <= 512);
        assertFalse(detail.getErrorSummary().contains("Sup3rSecret"));
        assertFalse(detail.getErrorSummary().contains("abc.def.ghi"));
        assertTrue(detail.getErrorSummary().contains("password=***"));
        assertTrue(detail.getErrorSummary().contains("token=***"));
        // 空结果不落明细
        jobTenantResultService.saveTenantResultsAsync(9010L, null);
        assertEquals(0, jobTenantResultService.getJobTenantResultList(9010L).size());
    }

    @Test
    public void testScenario10_tenantJobResultParsingOnlyAcceptsStructuredPayload() {
        // 仅结构化租户级结果被识别，普通 Handler 返回值不被误判
        assertNull(TenantJobExecutionResult.tryParse(null));
        assertNull(TenantJobExecutionResult.tryParse(""));
        assertNull(TenantJobExecutionResult.tryParse("plain text result"));
        assertNull(TenantJobExecutionResult.tryParse("{\"total\":1}"));
        assertNull(TenantJobExecutionResult.tryParse("{\"1\":\"ok\",\"2\":\"failed\"}"));

        TenantJobExecutionResult allSuccess = TenantJobExecutionResult.builder()
                .totalTenants(1).successCount(1).failureCount(0)
                .perTenantResults(Map.of(401L, TenantJobExecutionResult.TenantItem.builder()
                        .success(true).durationMs(3L).result("done").build()))
                .build();
        TenantJobExecutionResult reparsed = TenantJobExecutionResult.tryParse(allSuccess.toJsonString());
        assertNotNull(reparsed);
        assertTrue(reparsed.isAllSuccess());
        assertFalse(reparsed.isPartialFailure());
        assertEquals(1, reparsed.getTotalTenants());
        assertEquals(1, reparsed.getSuccessCount());
        assertEquals(0, reparsed.getFailureCount());
        assertEquals("done", reparsed.getPerTenantResults().get(401L).getResult());
        assertEquals(3L, reparsed.getPerTenantResults().get(401L).getDurationMs());
    }

    // ==================== 测试辅助 ====================

    private TenantJobAspect newAspect(Long... tenantIds) {
        TenantFrameworkService tenantFrameworkService = mock(TenantFrameworkService.class);
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(tenantIds));
        return new TenantJobAspect(tenantFrameworkService);
    }

    private TenantJob tenantJobAnnotation() throws NoSuchMethodException {
        return JobConsistencyTest.class.getMethod("tenantJobFixture").getAnnotation(TenantJob.class);
    }

    private JobHandlerInvoker newInvoker(ApplicationContext applicationContext,
                                         JobLogFrameworkService jobLogFrameworkService,
                                         JobTenantResultFrameworkService jobTenantResultFrameworkService) {
        JobHandlerInvoker invoker = new JobHandlerInvoker();
        ReflectionTestUtils.setField(invoker, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(invoker, "jobLogFrameworkService", jobLogFrameworkService);
        ReflectionTestUtils.setField(invoker, "jobHandlerWhitelistValidator", jobHandlerWhitelistValidator);
        ReflectionTestUtils.setField(invoker, "jobTenantResultFrameworkService", jobTenantResultFrameworkService);
        return invoker;
    }

    private JobExecutionContext newContext(String handlerName, String handlerParam) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JobDataKeyEnum.JOB_ID.name(), 1024L);
        jobDataMap.put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), handlerName);
        jobDataMap.put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), handlerParam);
        jobDataMap.put(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 0);
        jobDataMap.put(JobDataKeyEnum.JOB_RETRY_INTERVAL.name(), 0);
        JobExecutionContext executionContext = mock(JobExecutionContext.class);
        when(executionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(executionContext.getRefireCount()).thenReturn(0);
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getKey()).thenReturn(new JobKey(handlerName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);
        // QuartzJobBean#execute 会把 SchedulerContext 合并进属性集，缺了这个桩会直接 NPE 在框架层
        Scheduler contextScheduler = mock(Scheduler.class);
        when(contextScheduler.getContext()).thenReturn(new SchedulerContext());
        when(executionContext.getScheduler()).thenReturn(contextScheduler);
        return executionContext;
    }

    private static JobTenantResultDO findByTenantId(List<JobTenantResultDO> details, long tenantId) {
        return details.stream()
                .filter(detail -> detail.getTenantId() != null && detail.getTenantId() == tenantId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少租户 " + tenantId + " 的执行明细"));
    }

}
