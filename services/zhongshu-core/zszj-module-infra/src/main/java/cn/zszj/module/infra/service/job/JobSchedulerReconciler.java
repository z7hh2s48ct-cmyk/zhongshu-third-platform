package cn.zszj.module.infra.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.quartz.core.scheduler.SchedulerManager;
import cn.zszj.module.infra.dal.dataobject.job.JobDO;
import cn.zszj.module.infra.dal.mysql.job.JobMapper;
import cn.zszj.module.infra.enums.job.JobStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.JOB_SCHEDULER_STATE_DRIFT;

/**
 * 定时任务调度器对账器（ZS-JOB-001）
 *
 * 解决的问题：任务表 infra_job 与 Quartz 调度器是两份独立状态，
 * 手工改动、实例重启中断、Quartz 自身异常都可能让二者漂移——
 * 表现为"任务表写着开启，实际不跑"或"任务表写着暂停，实际在跑"，两种都会造成生产事故。
 *
 * 对账约定：
 * 1. 权威源恒为任务表，调度器是被修正方；对账不会改写任务表；
 * 2. 只有任务表 status=NORMAL 才认为"应当可跑"，其余状态（INIT/STOP）一律要求调度器处于暂停；
 * 3. 触发器缺失或已进入终态（COMPLETE/ERROR）时按任务表重建，重建后按任务表状态决定是否暂停；
 * 4. 孤儿触发器按「触发器归属的 JobKey」判定，而不是按触发器名：「立即触发」会为已注册任务
 *    生成一次性触发器（DEFAULT.MT_*），它的名字不是任何 handler 名，按名字比会把它误当孤儿删掉；
 * 5. 定时任务被禁用（无 Scheduler Bean）时整体 fail-safe：不检查、不修正、不抛异常。
 *
 * 触发入口：{@link JobService#syncJob()}（进程重启同步后自动对账）与 {@link #reconcileJob(Long)}（运维手动对账单个任务）。
 */
@Component
@Slf4j
public class JobSchedulerReconciler {

    /**
     * 漂移类型：任务表状态与调度器触发器的启停语义不一致
     */
    public static final String DRIFT_STATUS_MISMATCH = "STATUS_MISMATCH";
    /**
     * 漂移类型：触发器缺失或已进入终态，需按任务表重建
     */
    public static final String DRIFT_TRIGGER_REBUILD = "TRIGGER_MISSING_OR_TERMINAL";
    /**
     * 漂移类型：调度器中存在孤儿触发器（任务表已无对应任务）
     */
    public static final String DRIFT_ORPHAN_TRIGGER = "ORPHAN_TRIGGER";

    /**
     * 视为“已暂停”的触发器状态
     *
     * Quartz 2.5 的 {@link Trigger.TriggerState} 只有 NONE/NORMAL/PAUSED/COMPLETE/ERROR/BLOCKED 六个值，
     * 存储层的 PAUSED_BLOCKED 由 JobStore 归一为 {@code PAUSED} 后返回，因此这里只需 PAUSED。
     * BLOCKED 表示“正在执行且不允许并发”，属于可跑语义，不算暂停。
     */
    private static final Set<Trigger.TriggerState> PAUSED_STATES = EnumSet.of(Trigger.TriggerState.PAUSED);
    /**
     * 视为"已终止、不会再触发"的触发器状态，需重建
     */
    private static final Set<Trigger.TriggerState> TERMINAL_STATES =
            EnumSet.of(Trigger.TriggerState.COMPLETE, Trigger.TriggerState.ERROR);

    @Resource
    private JobMapper jobMapper;

    @Resource
    private SchedulerManager schedulerManager;

    /**
     * 以 ObjectProvider 持有：定时任务被禁用时容器中没有 Scheduler Bean，此处为 fail-safe 的判定依据
     */
    @Autowired
    private ObjectProvider<Scheduler> schedulerProvider;

    /**
     * 全量对账：遍历任务表并清理调度器孤儿触发器
     *
     * @return 对账报告；调度器不可用时返回 schedulerAvailable=false 的空报告
     */
    public JobSchedulerReconcileReport reconcile() {
        JobSchedulerReconcileReport report = new JobSchedulerReconcileReport();
        Scheduler scheduler = currentScheduler();
        if (scheduler == null) {
            log.warn("[reconcile][调度器不可用（定时任务已禁用），跳过本轮对账]");
            return report;
        }
        report.setSchedulerAvailable(true);

        // 1. 以任务表为权威，逐个任务比对并修正调度器
        List<JobDO> jobs = jobMapper.selectList();
        Set<String> handlerNames = new HashSet<>(jobs.size());
        for (JobDO job : jobs) {
            handlerNames.add(job.getHandlerName());
            reconcileJob(scheduler, job, report);
        }
        // 2. 清理调度器中任务表已不存在的孤儿触发器
        reconcileOrphanTriggers(scheduler, handlerNames, report);

        log.info("[reconcile][对账完成：{}]", report.getSummary());
        for (JobSchedulerReconcileReport.Drift drift : report.getDrifts()) {
            log.warn("[reconcile][漂移明细：handlerName({}) 类型({}) 任务表状态({}) 调度器状态({})]",
                    drift.getHandlerName(), drift.getType(), drift.getJobStatus(), drift.getTriggerState());
        }
        return report;
    }

    /**
     * 单任务对账，供运维在怀疑某个任务状态不一致时手动触发
     *
     * @param id 任务编号
     */
    public void reconcileJob(Long id) {
        Scheduler scheduler = currentScheduler();
        if (scheduler == null) {
            log.warn("[reconcileJob][id({}) 调度器不可用（定时任务已禁用），跳过对账]", id);
            return;
        }
        JobDO job = jobMapper.selectById(id);
        if (job == null) {
            log.warn("[reconcileJob][id({}) 任务不存在，跳过对账]", id);
            return;
        }
        JobSchedulerReconcileReport report = new JobSchedulerReconcileReport();
        report.setSchedulerAvailable(true);
        reconcileJob(scheduler, job, report);
        if (report.getFailed() > 0) {
            // 修正失败意味着漂移仍然存在，必须让调用方知道，而不是静默返回
            throw exception(JOB_SCHEDULER_STATE_DRIFT, job.getHandlerName(), job.getStatus(),
                    firstDriftTriggerState(report));
        }
        log.info("[reconcileJob][id({}) 单任务对账完成：{}]", id, report.getSummary());
    }

    /**
     * 比对单个任务并修正调度器
     *
     * @param scheduler 调度器
     * @param job       任务（权威侧）
     * @param report    对账报告
     */
    private void reconcileJob(Scheduler scheduler, JobDO job, JobSchedulerReconcileReport report) {
        report.recordChecked();
        String handlerName = job.getHandlerName();
        try {
            Trigger.TriggerState state = scheduler.getTriggerState(new TriggerKey(handlerName));
            // 情况一：触发器缺失或已进入终态，按任务表重建
            if (state == null || state == Trigger.TriggerState.NONE || TERMINAL_STATES.contains(state)) {
                report.recordDrift(handlerName, DRIFT_TRIGGER_REBUILD, job.getStatus(), stateName(state));
                rebuildTrigger(job, handlerName);
                report.recordCorrected();
                log.warn("[reconcileJob][handlerName({}) 调度器触发器状态({})，已按任务表状态({}) 重建]",
                        handlerName, stateName(state), job.getStatus());
                return;
            }
            // 情况二：启停语义不一致，以任务表为权威修正调度器
            boolean actualPaused = PAUSED_STATES.contains(state);
            boolean expectPause = expectPaused(job.getStatus());
            if (actualPaused == expectPause) {
                return;
            }
            report.recordDrift(handlerName, DRIFT_STATUS_MISMATCH, job.getStatus(), stateName(state));
            if (expectPause) {
                schedulerManager.pauseJob(handlerName);
            } else {
                schedulerManager.resumeJob(handlerName);
            }
            report.recordCorrected();
            log.warn("[reconcileJob][handlerName({}) 任务表状态({}) 与调度器状态({}) 不一致，已修正为{}]",
                    handlerName, job.getStatus(), stateName(state), expectPause ? "暂停" : "开启");
        } catch (SchedulerException ex) {
            report.recordFailed();
            log.error("[reconcileJob][handlerName({}) 对账失败]", handlerName, ex);
        }
    }

    /**
     * 按任务表重建触发器
     *
     * 多实例同时启动、或运维同时对账时，另一实例可能已经完成重建，此时 addJob 会抛
     * {@link ObjectAlreadyExistsException}。这不是「漂移未被修正」，而是「目标状态已达成」，
     * 因此降级为提示、不计入失败——否则单任务对账入口会把已经正确的状态误报成 JOB_SCHEDULER_STATE_DRIFT。
     *
     * @param job         任务（权威侧）
     * @param handlerName Handler 名字
     * @throws SchedulerException 重建失败（并发重复除外）
     */
    private void rebuildTrigger(JobDO job, String handlerName) throws SchedulerException {
        schedulerManager.deleteJob(handlerName);
        try {
            schedulerManager.addJob(job.getId(), handlerName, job.getHandlerParam(), job.getCronExpression(),
                    job.getRetryCount(), job.getRetryInterval());
        } catch (ObjectAlreadyExistsException ex) {
            log.warn("[rebuildTrigger][handlerName({}) 重建时触发器已存在，视为其它实例已完成重建]", handlerName);
        }
        if (expectPaused(job.getStatus())) {
            schedulerManager.pauseJob(handlerName);
        }
    }

    /**
     * 清理调度器中任务表已不存在的孤儿触发器
     *
     * 典型成因：任务被直接删库（绕过 Service）、或 Quartz 表被手工回滚，
     * 遗留的触发器会继续按旧配置执行，属于"关不掉的任务"。
     *
     * 归属判定必须看触发器指向的 JobKey，不能拿触发器名去比 handler 名：
     * 「立即触发」生成的一次性触发器（DEFAULT.MT_*）归属于一个仍然在册的任务，
     * 按名字比会把它误判为孤儿，连带取消运维已接受的那一次手动执行。
     *
     * @param scheduler    调度器
     * @param handlerNames 任务表中全部的 Handler 名字
     * @param report       对账报告
     */
    private void reconcileOrphanTriggers(Scheduler scheduler, Set<String> handlerNames,
                                        JobSchedulerReconcileReport report) {
        Set<TriggerKey> triggerKeys;
        try {
            triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException ex) {
            report.recordFailed();
            log.error("[reconcileOrphanTriggers][读取调度器触发器清单失败，跳过孤儿触发器清理]", ex);
            return;
        }
        if (CollUtil.isEmpty(triggerKeys)) {
            return;
        }
        for (TriggerKey triggerKey : triggerKeys) {
            Trigger trigger;
            try {
                trigger = scheduler.getTrigger(triggerKey);
            } catch (SchedulerException ex) {
                report.recordFailed();
                log.error("[reconcileOrphanTriggers][triggerKey({}) 读取触发器详情失败]", triggerKey, ex);
                continue;
            }
            if (trigger == null) {
                // 本轮对账期间触发器已消失（例如一次性手动触发执行完毕），不是孤儿
                continue;
            }
            JobKey jobKey = trigger.getJobKey();
            String owningHandlerName = jobKey == null ? triggerKey.getName() : jobKey.getName();
            if (handlerNames.contains(owningHandlerName)) {
                // 归属任务仍在任务表中：cron 触发器与「立即触发」产生的一次性触发器都属于这一类，不能删
                continue;
            }
            report.recordOrphanTrigger();
            try {
                report.recordDrift(owningHandlerName, DRIFT_ORPHAN_TRIGGER, null,
                        stateName(scheduler.getTriggerState(triggerKey)));
                schedulerManager.deleteOrphanTrigger(triggerKey, jobKey);
                report.recordCorrected();
                log.warn("[reconcileOrphanTriggers][triggerKey({}) 归属任务({}) 已不在任务表，判定为孤儿触发器，已清理]",
                        triggerKey, owningHandlerName);
            } catch (SchedulerException ex) {
                report.recordFailed();
                log.error("[reconcileOrphanTriggers][triggerKey({}) 孤儿触发器清理失败]", triggerKey, ex);
            }
        }
    }

    /**
     * 任务表状态是否要求调度器处于暂停。只有 NORMAL 才认为"应当可跑"
     *
     * 判定必须与 JobServiceImpl#syncJob 同源（{@link JobStatusEnum#shouldRun(Integer)}），
     * 否则 INIT 任务会在同步后被留在可跑状态、再由对账暂停，每次重启都制造一次无意义的漂移噪音。
     *
     * @param status 任务表状态，见 {@link JobStatusEnum}
     * @return 是否应当暂停
     */
    private static boolean expectPaused(Integer status) {
        return !JobStatusEnum.shouldRun(status);
    }

    /**
     * 取出首条漂移记录的调度器状态，供修正失败时的错误信息使用
     *
     * @param report 对账报告
     * @return 调度器状态名；读取状态本身就失败（尚无漂移记录）时返回 UNKNOWN
     */
    private static String firstDriftTriggerState(JobSchedulerReconcileReport report) {
        List<JobSchedulerReconcileReport.Drift> drifts = report.getDrifts();
        return CollUtil.isEmpty(drifts) ? "UNKNOWN" : drifts.get(0).getTriggerState();
    }

    private static String stateName(Trigger.TriggerState state) {
        return state == null ? Trigger.TriggerState.NONE.name() : state.name();
    }

    private Scheduler currentScheduler() {
        return schedulerProvider == null ? null : schedulerProvider.getIfAvailable();
    }

}
