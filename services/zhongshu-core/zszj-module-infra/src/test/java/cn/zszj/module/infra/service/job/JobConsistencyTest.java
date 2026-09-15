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
import cn.zszj.module.infra.dal.dataobject.job.JobLogDO;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import cn.zszj.module.infra.dal.mysql.job.JobLogMapper;
import cn.zszj.module.infra.dal.mysql.job.JobMapper;
import cn.zszj.module.infra.dal.mysql.job.JobTenantResultMapper;
import cn.zszj.module.infra.enums.job.JobStatusEnum;
import cn.zszj.module.infra.job.job.JobLogCleanJob;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
 *
 * 另覆盖 codex 评审 r0 处置的一致性约束：
 * 11. 租户级结构化结果写入执行日志前被压到结果列宽内（否则写库失败会让执行记录永久停留在“运行中”）；
 * 12. 孤儿触发器按归属 JobKey 判定，“立即触发”生成的一次性触发器不被误删；
 * 13. 租户级明细与父执行日志同保留期清理，不留下失去归属的死数据；
 * 14. INIT 与 STOP 一样都不该跑，任务同步与对账共用同一份判定；
 * 15. 多实例并发重建触发器时不把“已达成目标状态”误报为漂移未修正。
 */
@Import({JobServiceImpl.class, JobLogServiceImpl.class, JobTenantResultServiceImpl.class,
        JobSchedulerReconciler.class,
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
    @Resource
    private JobLogServiceImpl jobLogService;
    @Resource
    private JobLogMapper jobLogMapper;
    @Resource
    private JobTenantResultMapper jobTenantResultMapper;

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
        stubOwningJob("vanishedJobHandler");
        stubOwningJob("orphanJobHandler");
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
        // 孤儿 trigger 被清理：按归属 JobKey 删，不走「删整个任务」那条路径
        verify(schedulerManager).deleteOrphanTrigger(eq(new TriggerKey("orphanJobHandler")),
                eq(new JobKey("orphanJobHandler")));
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

        // 带引号的 JSON 键值形式：键名两侧有引号时同样要脱敏，否则凭证会以明文进入摘要列
        String jsonSecret = "rejected payload {\"password\":\"Sup3rSecret\",\"api_key\":\"ak-9f8e7d\"}";
        // Authorization 头：Bearer/Basic 后面的完整凭证必须一并脱敏，只替换前缀会留下真实令牌
        String bearerSecret = "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.cGF5bG9hZA.c2lnbmF0dXJl";
        // 无敏感键名、单独出现的凭证前缀（异常栈里直接打印令牌的常见形式）
        String bareBearer = "upstream rejected credential Basic Zm9vOmJhclNlY3JldA==";
        jobTenantResultService.saveTenantResultsAsync(9011L, TenantJobExecutionResult.builder()
                .totalTenants(3).successCount(0).failureCount(3)
                .perTenantResults(Map.of(
                        701L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(jsonSecret).build(),
                        702L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(bearerSecret).build(),
                        703L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(bareBearer).build()))
                .build());
        List<JobTenantResultDO> secretDetails = jobTenantResultService.getJobTenantResultList(9011L);
        assertEquals(3, secretDetails.size());
        String jsonSummary = findByTenantId(secretDetails, 701L).getErrorSummary();
        assertFalse(jsonSummary.contains("Sup3rSecret"));
        assertFalse(jsonSummary.contains("ak-9f8e7d"));
        assertTrue(jsonSummary.contains("\"password\":\"***\""));
        assertTrue(jsonSummary.contains("\"api_key\":\"***\""));
        String bearerSummary = findByTenantId(secretDetails, 702L).getErrorSummary();
        assertFalse(bearerSummary.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertFalse(bearerSummary.contains("cGF5bG9hZA"));
        assertFalse(bearerSummary.contains("c2lnbmF0dXJl"));
        assertTrue(bearerSummary.contains("***"));
        String bareSummary = findByTenantId(secretDetails, 703L).getErrorSummary();
        assertFalse(bareSummary.contains("Zm9vOmJhclNlY3JldA"));
        assertTrue(bareSummary.contains("Basic ***"));

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

    // ============ codex r0 处置：结构化结果有界化、孤儿触发器归属、明细保留期 ============

    @Test
    public void testScenario11_structuredResultIsBoundedToFitJobLogColumn() throws Throwable {
        // 租户数很多时，逐租户明细展开后的 JSON 会超出 infra_job_log.result 的 varchar(4000)：
        // 溢出会让执行日志写库失败，而那里的异常被吞掉，执行记录将永久停留在“运行中”
        int tenantCount = 200;
        Map<Long, TenantJobExecutionResult.TenantItem> details = new LinkedHashMap<>();
        for (int i = 0; i < tenantCount; i++) {
            details.put(500L + i, TenantJobExecutionResult.TenantItem.builder()
                    .success(i != 7).durationMs(11L).result("ok")
                    .error(i == 7 ? "tenant-7-failure" : null).build());
        }
        TenantJobExecutionResult result = TenantJobExecutionResult.builder()
                .totalTenants(tenantCount).successCount(tenantCount - 1).failureCount(1)
                .perTenantResults(details).build();
        int maxLength = TenantJobExecutionResult.JOB_LOG_RESULT_MAX_LENGTH;
        assertTrue(result.toJsonString().length() > maxLength);

        String bounded = result.toBoundedJsonString(maxLength);
        // 有界：不超列宽，且仍是合法 JSON（直接截断字符串会产生半个 JSON，让执行日志不可解析）
        assertTrue(bounded.length() <= maxLength);
        TenantJobExecutionResult reparsed = TenantJobExecutionResult.tryParse(bounded);
        assertNotNull(reparsed);
        // 顶层计数不因裁剪而丢失：消费方靠 failureCount 判定整体成败
        assertEquals(tenantCount, reparsed.getTotalTenants());
        assertEquals(tenantCount - 1, reparsed.getSuccessCount());
        assertEquals(1, reparsed.getFailureCount());
        assertTrue(reparsed.isTruncated());
        assertTrue(reparsed.isPartialFailure());
        // 失败租户明细优先保留（运维排查失败需要它，成功明细在明细表里查）
        assertNotNull(reparsed.getPerTenantResults().get(507L));
        assertEquals("tenant-7-failure", reparsed.getPerTenantResults().get(507L).getError());

        // 未超长时不裁剪，明细完整保留
        TenantJobExecutionResult small = TenantJobExecutionResult.builder()
                .totalTenants(1).successCount(1).failureCount(0)
                .perTenantResults(Map.of(501L, TenantJobExecutionResult.TenantItem.builder()
                        .success(true).durationMs(1L).result("ok").build()))
                .build();
        TenantJobExecutionResult smallReparsed = TenantJobExecutionResult.tryParse(
                small.toBoundedJsonString(maxLength));
        assertNotNull(smallReparsed);
        assertFalse(smallReparsed.isTruncated());
        assertEquals(1, smallReparsed.getPerTenantResults().size());

        // 走一遍执行链：写入执行日志的结果被压到列宽内，且成败判定不受裁剪影响
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(eq(TENANT_JOB_HANDLER), eq(JobHandler.class)))
                .thenReturn(param -> result.toJsonString());
        JobLogFrameworkService jobLogFrameworkService = mock(JobLogFrameworkService.class);
        when(jobLogFrameworkService.createJobLog(any(), any(), any(), any(), any())).thenReturn(9012L);
        assertDoesNotThrow(() -> newInvoker(applicationContext, jobLogFrameworkService, jobTenantResultService)
                .execute(newContext(TENANT_JOB_HANDLER, null)));
        ArgumentCaptor<String> loggedResult = ArgumentCaptor.forClass(String.class);
        verify(jobLogFrameworkService).updateJobLogResultAsync(eq(9012L), any(), anyInt(), eq(false),
                loggedResult.capture());
        assertTrue(loggedResult.getValue().length() <= maxLength);
        // 明细仍按完整结果落库，不因执行日志有界化而丢租户
        assertEquals(tenantCount, jobTenantResultService.getJobTenantResultList(9012L).size());
    }

    @Test
    public void testScenario12_orphanTriggerIsIdentifiedByOwningJob() throws SchedulerException {
        // “立即触发”会为已注册任务生成一次性触发器（DEFAULT.MT_*），它的名字不是任何 handler 名。
        // 按触发器名判定孤儿会把它误删，连带取消已接受的手动执行
        JobDO job = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName("manualTriggerJobHandler");
            o.setCronExpression(VALID_CRON);
        });
        jobMapper.insert(job);
        when(scheduler.getTriggerState(new TriggerKey("manualTriggerJobHandler")))
                .thenReturn(Trigger.TriggerState.NORMAL);
        TriggerKey cronKey = new TriggerKey("manualTriggerJobHandler");
        TriggerKey manualKey = new TriggerKey("MT_6f1c2b3a-manual");
        stubOwningJob(cronKey, "manualTriggerJobHandler");
        stubOwningJob(manualKey, "manualTriggerJobHandler");
        // 真孤儿：归属任务已从任务表删除
        TriggerKey orphanKey = new TriggerKey("deletedJobHandler");
        stubOwningJob(orphanKey, "deletedJobHandler");
        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP)))
                .thenReturn(new LinkedHashSet<>(List.of(cronKey, manualKey, orphanKey)));

        JobSchedulerReconcileReport report = jobSchedulerReconciler.reconcile();
        assertEquals(1, report.getChecked());
        assertEquals(0, report.getFailed());
        // 只有归属任务不在表中的那一个被判为孤儿
        assertEquals(1, report.getOrphanTriggerCount());
        verify(schedulerManager).deleteOrphanTrigger(eq(orphanKey), eq(new JobKey("deletedJobHandler")));
        verify(schedulerManager, never()).deleteOrphanTrigger(eq(manualKey), any());
        verify(schedulerManager, never()).deleteOrphanTrigger(eq(cronKey), any());
        verify(schedulerManager, never()).deleteJob(any());
    }

    @Test
    public void testScenario13_tenantDetailsArePurgedWithParentJobLogs() {
        // 租户级明细与父执行日志同生命周期：父日志被物理删除后，明细既无法回连 handler/param，
        // 也没有任何查询入口，会成为永久留存的死数据
        LocalDateTime expired = LocalDateTime.now().minusDays(30);
        Long expiredLogId = jobLogService.createJobLog(1L, expired, WHITELISTED_HANDLER, null, 1);
        Long freshLogId = jobLogService.createJobLog(1L, LocalDateTime.now(), WHITELISTED_HANDLER, null, 1);
        // create_time 由 MyBatis-Plus 在 insert 时自动填充为当前时间，这里回拨成过期时间。
        // BaseDO 没有 @SuperBuilder，createTime 不在 builder 里，只能走 setter
        JobLogDO expiredLog = new JobLogDO();
        expiredLog.setId(expiredLogId);
        expiredLog.setCreateTime(expired);
        jobLogMapper.updateById(expiredLog);

        JobTenantResultDO expiredDetail = newTenantDetail(expiredLogId, 601L);
        JobTenantResultDO freshDetail = newTenantDetail(freshLogId, 602L);
        jobTenantResultMapper.insert(expiredDetail);
        jobTenantResultMapper.insert(freshDetail);
        JobTenantResultDO expiredDetailUpdate = new JobTenantResultDO();
        expiredDetailUpdate.setId(expiredDetail.getId());
        expiredDetailUpdate.setCreateTime(expired);
        jobTenantResultMapper.updateById(expiredDetailUpdate);

        // 清理保留期外的执行日志时，同批次的租户明细一并清理
        assertEquals(1, jobLogService.cleanJobLog(14, 100));
        assertNull(jobLogMapper.selectById(expiredLogId));
        assertNotNull(jobLogMapper.selectById(freshLogId));
        assertEquals(0, jobTenantResultService.getJobTenantResultList(expiredLogId).size());
        assertEquals(1, jobTenantResultService.getJobTenantResultList(freshLogId).size());
    }

    @Test
    public void testScenario14_initJobIsNotRunnableAfterSync() throws SchedulerException {
        // INIT（尚未开启）与 STOP 一样都不该跑。任务同步与对账必须共用同一份判定，
        // 否则 syncJob 放行、对账暂停，每次重启都会制造一次“漂移→修正”噪音
        assertTrue(JobStatusEnum.shouldRun(JobStatusEnum.NORMAL.getStatus()));
        assertFalse(JobStatusEnum.shouldRun(JobStatusEnum.INIT.getStatus()));
        assertFalse(JobStatusEnum.shouldRun(JobStatusEnum.STOP.getStatus()));

        JobDO initJob = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.INIT.getStatus());
            o.setHandlerName(WHITELISTED_HANDLER);
            o.setCronExpression(VALID_CRON);
            o.setRetryCount(0);
            o.setRetryInterval(0);
        });
        jobMapper.insert(initJob);
        // 让 mock 反映真实 Quartz 的状态转移：pauseJob 后 getTriggerState 返回 PAUSED。
        // 静态 thenReturn(NORMAL) 会让 syncJob 主体的 pauseJob 与末尾对账的 pauseJob 各触发一次，
        // 而真实调度器在第一次 pauseJob 后就已经是 PAUSED，对账识别为一致不会再动。
        AtomicReference<Trigger.TriggerState> stateRef = new AtomicReference<>(Trigger.TriggerState.NORMAL);
        when(scheduler.getTriggerState(new TriggerKey(WHITELISTED_HANDLER)))
                .thenAnswer(invocation -> stateRef.get());
        doAnswer(invocation -> {
            stateRef.set(Trigger.TriggerState.PAUSED);
            return null;
        }).when(schedulerManager).pauseJob(eq(WHITELISTED_HANDLER));

        jobService.syncJob();
        // syncJob 重建后就把 INIT 任务置为暂停，末尾对账看到 PAUSED 与任务表一致，不再重复 pause
        verify(schedulerManager).pauseJob(eq(WHITELISTED_HANDLER));
        // 显式对账：state 已 PAUSED，任务表 INIT 也要求 PAUSED，识别为一致
        assertEquals(Trigger.TriggerState.PAUSED, stateRef.get());
        assertEquals(0, jobSchedulerReconciler.reconcile().getDrifted());
    }

    @Test
    public void testScenario15_reconcileToleratesConcurrentTriggerRebuild() throws SchedulerException {
        // 多实例同时对账时，另一实例可能已完成重建，此时 addJob 抛 ObjectAlreadyExistsException。
        // 这不是漂移未被修正，而是目标状态已达成，不能计为失败——否则单任务对账入口会把
        // “已经正确”误报成 JOB_SCHEDULER_STATE_DRIFT 给运维
        JobDO job = randomPojo(JobDO.class, o -> {
            o.setStatus(JobStatusEnum.NORMAL.getStatus());
            o.setHandlerName("racingJobHandler");
            o.setCronExpression(VALID_CRON);
            o.setRetryCount(0);
            o.setRetryInterval(0);
        });
        jobMapper.insert(job);
        when(scheduler.getTriggerState(new TriggerKey("racingJobHandler")))
                .thenReturn(Trigger.TriggerState.NONE);
        doThrow(new ObjectAlreadyExistsException("racingJobHandler")).when(schedulerManager)
                .addJob(eq(job.getId()), eq("racingJobHandler"), any(), any(), any(), any());

        JobSchedulerReconcileReport report = jobSchedulerReconciler.reconcile();
        assertEquals(1, report.getDrifted());
        assertEquals(1, report.getCorrected());
        assertEquals(0, report.getFailed());
        // 单任务入口同样不抛
        assertDoesNotThrow(() -> jobSchedulerReconciler.reconcileJob(job.getId()));
    }

    @Test
    public void testScenario16_redactionHandlesPunctuationInsideQuotedValue() {
        // codex r1 [P1] regression:
        // 旧 pattern 将引号值中的标点/空格当停止符，只 mask 第一段，会在摘要中泄露凭证后缀
        String quotedWithComma = "{\"password\":\"alpha,beta\"}";
        String quotedWithSpace = "{\"password\":\"alpha beta gamma\"}";
        String unquotedWithComma = "password=alpha,beta";

        jobTenantResultService.saveTenantResultsAsync(9012L, TenantJobExecutionResult.builder()
                .totalTenants(3).successCount(0).failureCount(3)
                .perTenantResults(Map.of(
                        801L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(quotedWithComma).build(),
                        802L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(quotedWithSpace).build(),
                        803L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(unquotedWithComma).build()))
                .build());

        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9012L);
        assertEquals(3, details.size());

        // 引号值含逗号：必须整个引号内容都被 mask，不能留下 "beta"
        String commaSummary = findByTenantId(details, 801L).getErrorSummary();
        assertFalse(commaSummary.contains("alpha"));
        assertFalse(commaSummary.contains("beta"));
        assertTrue(commaSummary.contains("\"password\":\"***\""));

        // 引号值含空格：必须整个引号内容都被 mask，不能留下 "gamma"
        String spaceSummary = findByTenantId(details, 802L).getErrorSummary();
        assertFalse(spaceSummary.contains("alpha"));
        assertFalse(spaceSummary.contains("gamma"));
        assertTrue(spaceSummary.contains("\"password\":\"***\""));

        // 非引号值含逗号：恢复旧版 \S+ 对标点的支持，不能只 mask "alpha" 留下 ",beta"
        String bareSummary = findByTenantId(details, 803L).getErrorSummary();
        assertFalse(bareSummary.contains("alpha"));
        assertFalse(bareSummary.contains("beta"));
        assertTrue(bareSummary.contains("password=***"));
    }

    @Test
    public void testScenario17_tenantDetailsArePurgedEvenWhenFresherThanParentLog() {
        // codex r1 [P2] regression:
        // 明细在执行结束后创建，父日志在执行开始前创建，两者 create_time 可能跨越保留截止日：
        // 父日志已过期（到期删除）、明细尚未过期（未到期删除），旧 SQL 按明细 create_time 清理会留下孤儿。
        // 新 SQL JOIN 父日志按父日志 create_time 判定过期，确保级联语义。
        LocalDateTime expiredParent = LocalDateTime.now().minusDays(30);
        Long parentLogId = jobLogService.createJobLog(1L, expiredParent, WHITELISTED_HANDLER, null, 1);
        JobLogDO parentLog = new JobLogDO();
        parentLog.setId(parentLogId);
        parentLog.setCreateTime(expiredParent);
        jobLogMapper.updateById(parentLog);

        // 明细 create_time 保持默认（now），模拟“明细比父日志新”的真实时序
        JobTenantResultDO freshDetail = newTenantDetail(parentLogId, 901L);
        jobTenantResultMapper.insert(freshDetail);

        // 父日志过期→清理→明细必须一并消失（旧 SQL 会因为明细 create_time 新而留下孤儿）
        assertEquals(1, jobLogService.cleanJobLog(14, 100));
        assertNull(jobLogMapper.selectById(parentLogId));
        assertEquals(0, jobTenantResultService.getJobTenantResultList(parentLogId).size());
    }
    @Test
    public void testScenario18_redactionHandlesMixedQuotesInsideValue() {
        // codex r2 [P1] regression:
        // r1 pattern 用 [^"'\\]* 同时排除两种引号，导致值内含另一种引号时整体不匹配，凭证完全泄露
        String doubleQuoteWithApostrophe = "{\"password\":\"alpha'beta\"}";
        String singleQuoteWithDouble = "password='alpha\"beta'";

        jobTenantResultService.saveTenantResultsAsync(9013L, TenantJobExecutionResult.builder()
                .totalTenants(2).successCount(0).failureCount(2)
                .perTenantResults(Map.of(
                        811L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(doubleQuoteWithApostrophe).build(),
                        812L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(singleQuoteWithDouble).build()))
                .build());

        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9013L);
        assertEquals(2, details.size());

        // 双引号值内含单引号：必须整体 mask，不能留下 alpha 或 beta
        String mixedSummary = findByTenantId(details, 811L).getErrorSummary();
        assertFalse(mixedSummary.contains("alpha"));
        assertFalse(mixedSummary.contains("beta"));
        assertTrue(mixedSummary.contains("\"password\":\"***\""));

        // 单引号值内含双引号：必须整体 mask
        String singleMixed = findByTenantId(details, 812L).getErrorSummary();
        assertFalse(singleMixed.contains("alpha"));
        assertFalse(singleMixed.contains("beta"));
        assertTrue(singleMixed.contains("password='***'"));
    }

    @Test
    public void testScenario19_redactionIsStackSafeForLongValues() {
        // codex r2 [P2] regression:
        // r1 嵌套量词 (?:\\.[^"'\\]*)* 在 5000+ 转义时触发 StackOverflowError，
        // 迭代扫描方案必须对任意长度值安全
        String manyEscapes = "{\"password\":\"" + "\\n".repeat(5000) + "\"}";
        String manyWords = "password=" + "alpha ".repeat(5000);

        // 不抛 StackOverflowError 即为通过；同时验证脱敏效果
        jobTenantResultService.saveTenantResultsAsync(9014L, TenantJobExecutionResult.builder()
                .totalTenants(2).successCount(0).failureCount(2)
                .perTenantResults(Map.of(
                        821L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(manyEscapes).build(),
                        822L, TenantJobExecutionResult.TenantItem.builder()
                                .success(false).durationMs(1L).error(manyWords).build()))
                .build());

        List<JobTenantResultDO> details = jobTenantResultService.getJobTenantResultList(9014L);
        assertEquals(2, details.size());

        String escapeSummary = findByTenantId(details, 821L).getErrorSummary();
        assertTrue(escapeSummary.length() <= 512);
        assertTrue(escapeSummary.contains("\"password\":\"***\""));
        assertFalse(escapeSummary.contains("\\n"));

        String wordSummary = findByTenantId(details, 822L).getErrorSummary();
        assertTrue(wordSummary.length() <= 512);
        assertTrue(wordSummary.contains("password=***"));
        assertFalse(wordSummary.contains("alpha"));
    }

    @Test
    public void testScenario20_orphanDetailsAreCollectedAfterParentLogDeletion() {
        // codex r2 [P2] regression:
        // JOIN 级联清理与父日志删除是两步操作，长执行任务可能在两步之间写入明细，
        // 父日志随后被删，该明细因 JOIN 找不到父日志而永久留存。
        // 兜底孤儿回收按明细自身 create_time 清理，确保最终一致性。
        LocalDateTime expired = LocalDateTime.now().minusDays(30);

        // 模拟：父日志已被删除（不存在），明细的 create_time 也已过期
        Long orphanLogId = 99999L; // 不存在的父日志 ID
        JobTenantResultDO orphanDetail = newTenantDetail(orphanLogId, 950L);
        jobTenantResultMapper.insert(orphanDetail);
        // 回拨明细 create_time 到过期时间（模拟“已过保留期”）
        JobTenantResultDO orphanUpdate = new JobTenantResultDO();
        orphanUpdate.setId(orphanDetail.getId());
        orphanUpdate.setCreateTime(expired);
        jobTenantResultMapper.updateById(orphanUpdate);

        // 清理：主轮 JOIN 找不到父日志（已不存在），兜底轮 NOT EXISTS 回收孤儿
        jobLogService.cleanJobLog(14, 100);
        assertEquals(0, jobTenantResultService.getJobTenantResultList(orphanLogId).size());
    }

    // ==================== 测试辅助 ====================

    /**
     * 桩住“触发器归属哪个 JobDetail”，同名触发器（trigger 名 == handler 名）的场景
     */
    private void stubOwningJob(String handlerName) throws SchedulerException {
        stubOwningJob(new TriggerKey(handlerName), handlerName);
    }

    private void stubOwningJob(TriggerKey triggerKey, String owningHandlerName) throws SchedulerException {
        Trigger trigger = mock(Trigger.class);
        when(trigger.getJobKey()).thenReturn(new JobKey(owningHandlerName));
        when(scheduler.getTrigger(triggerKey)).thenReturn(trigger);
    }

    private static JobTenantResultDO newTenantDetail(Long jobLogId, long tenantId) {
        return JobTenantResultDO.builder().jobLogId(jobLogId).tenantId(tenantId)
                .success(true).durationMs(5L).resultSummary("ok").build();
    }

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
