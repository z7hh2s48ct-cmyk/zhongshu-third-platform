package cn.zszj.module.infra.service.job;

import cn.hutool.extra.spring.SpringUtil;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.quartz.core.handler.JobHandler;
import cn.zszj.framework.quartz.core.scheduler.SchedulerManager;
import cn.zszj.framework.quartz.core.util.CronUtils;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistValidator;
import cn.zszj.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.zszj.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.job.JobDO;
import cn.zszj.module.infra.dal.mysql.job.JobMapper;
import cn.zszj.module.infra.enums.job.JobStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.util.collection.CollectionUtils.containsAny;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;

/**
 * 定时任务 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class JobServiceImpl implements JobService {

    @Resource
    private JobMapper jobMapper;

    @Resource
    private SchedulerManager schedulerManager;

    /**
     * JobHandler 白名单校验器（ZS-JOB-001）：任务登记侧的固化入口，与执行侧共用同一份判定
     */
    @Resource
    private JobHandlerWhitelistValidator jobHandlerWhitelistValidator;

    /**
     * 调度器对账器（ZS-JOB-001）：以任务表为权威修正调度器残留漂移
     */
    @Resource
    private JobSchedulerReconciler jobSchedulerReconciler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createJob(JobSaveReqVO createReqVO) throws SchedulerException {
        validateCronExpression(createReqVO.getCronExpression());
        // 1.1 校验唯一性
        if (jobMapper.selectByHandlerName(createReqVO.getHandlerName()) != null) {
            throw exception(JOB_HANDLER_EXISTS);
        }
        // 1.2 校验 JobHandler 是否存在
        validateJobHandlerExists(createReqVO.getHandlerName());

        // 2. 插入 JobDO
        JobDO job = BeanUtils.toBean(createReqVO, JobDO.class);
        job.setStatus(JobStatusEnum.INIT.getStatus());
        fillJobMonitorTimeoutEmpty(job);
        jobMapper.insert(job);

        // 3.1 添加 Job 到 Quartz 中
        schedulerManager.addJob(job.getId(), job.getHandlerName(), job.getHandlerParam(), job.getCronExpression(),
                createReqVO.getRetryCount(), createReqVO.getRetryInterval());
        // 3.2 更新 JobDO
        JobDO updateObj = JobDO.builder().id(job.getId()).status(JobStatusEnum.NORMAL.getStatus()).build();
        jobMapper.updateById(updateObj);
        return job.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobSaveReqVO updateReqVO) throws SchedulerException {
        validateCronExpression(updateReqVO.getCronExpression());
        // 1.1 校验存在
        JobDO job = validateJobExists(updateReqVO.getId());
        // 1.2 只有开启状态，才可以修改.原因是，如果出暂停状态，修改 Quartz Job 时，会导致任务又开始执行
        if (!job.getStatus().equals(JobStatusEnum.NORMAL.getStatus())) {
            throw exception(JOB_UPDATE_ONLY_NORMAL_STATUS);
        }
        // 1.3 校验 JobHandler 是否存在
        validateJobHandlerExists(updateReqVO.getHandlerName());

        // 2. 更新 JobDO
        JobDO updateObj = BeanUtils.toBean(updateReqVO, JobDO.class);
        fillJobMonitorTimeoutEmpty(updateObj);
        jobMapper.updateById(updateObj);

        // 3. 更新 Job 到 Quartz 中
        schedulerManager.updateJob(job.getHandlerName(), updateReqVO.getHandlerParam(), updateReqVO.getCronExpression(),
                updateReqVO.getRetryCount(), updateReqVO.getRetryInterval());
    }

    private void validateJobHandlerExists(String handlerName) {
        try {
            Object handler = SpringUtil.getBean(handlerName);
            assert handler != null;
            if (!(handler instanceof JobHandler)) {
                throw exception(JOB_HANDLER_BEAN_TYPE_ERROR);
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw exception(JOB_HANDLER_BEAN_NOT_EXISTS);
        }
        // 白名单校验（ZS-JOB-001）：Bean 存在不等于允许登记。“容器里恰好有这个 Bean”不能作为任务可登记的依据，
        // 否则模块关闭/依赖变化会静默改变可执行任务集。校验放在 Bean 存在性与类型校验之后，保留原有错误码语义。
        jobHandlerWhitelistValidator.validate(handlerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJobStatus(Long id, Integer status) throws SchedulerException {
        // 校验 status
        if (!containsAny(status, JobStatusEnum.NORMAL.getStatus(), JobStatusEnum.STOP.getStatus())) {
            throw exception(JOB_CHANGE_STATUS_INVALID);
        }
        // 校验存在
        JobDO job = validateJobExists(id);
        // 校验是否已经为当前状态
        if (job.getStatus().equals(status)) {
            throw exception(JOB_CHANGE_STATUS_EQUALS);
        }
        // 更新 Job 状态
        JobDO updateObj = JobDO.builder().id(id).status(status).build();
        jobMapper.updateById(updateObj);

        // 更新状态 Job 到 Quartz 中
        if (JobStatusEnum.NORMAL.getStatus().equals(status)) { // 开启
            schedulerManager.resumeJob(job.getHandlerName());
        } else { // 暂停
            schedulerManager.pauseJob(job.getHandlerName());
        }
    }

    @Override
    public void triggerJob(Long id) throws SchedulerException {
        // 校验存在
        JobDO job = validateJobExists(id);
        // 校验状态（ZS-JOB-001）：只有开启状态的任务才允许手动触发。
        // 暂停中的任务被手动触发，会绕过“暂停”这个运维意图产生副作用，且执行日志与任务表状态相互矛盾，因此直接拒绝。
        if (!JobStatusEnum.shouldRun(job.getStatus())) {
            throw exception(JOB_TRIGGER_ON_PAUSED, job.getStatus());
        }
        // 校验白名单（ZS-JOB-001）：手动触发是第四个入口，与登记侧（createJob/updateJob）、执行侧（JobHandlerInvoker）
        // 共用同一份判定。执行侧虽已兜底，但那里拒绝会先写一条执行日志再失败，形成「已触发但被拦」的矛盾记录；
        // 这里提前拒绝，覆盖「任务登记时白名单未启用、之后 Handler 被移出清单」这一时间差。
        jobHandlerWhitelistValidator.validate(job.getHandlerName());

        // 触发 Quartz 中的 Job
        schedulerManager.triggerJob(job.getId(), job.getHandlerName(), job.getHandlerParam());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncJob() throws SchedulerException {
        // 1. 查询 Job 配置
        List<JobDO> jobList = jobMapper.selectList();

        // 2. 遍历处理
        for (JobDO job : jobList) {
            // 2.1 先删除，再创建
            schedulerManager.deleteJob(job.getHandlerName());
            schedulerManager.addJob(job.getId(), job.getHandlerName(), job.getHandlerParam(), job.getCronExpression(),
                    job.getRetryCount(), job.getRetryInterval());
            // 2.2 只要不是「应当可跑」的状态就暂停。INIT（尚未开启）与 STOP 同样不该跑：
            // 此处与 JobSchedulerReconciler 共用 JobStatusEnum#shouldRun 单一真源，
            // 若各自判断（这里只看 STOP、对账看非 NORMAL），INIT 任务会在重启同步后被留在可跑状态，
            // 并由对账再补一刀，每次重启都制造一次无意义的「漂移→修正」噪音。
            if (!JobStatusEnum.shouldRun(job.getStatus())) {
                schedulerManager.pauseJob(job.getHandlerName());
            }
            log.info("[syncJob][id({}) handlerName({}) 同步完成]", job.getId(), job.getHandlerName());
        }

        // 3. 对账（ZS-JOB-001）：重建之后再校一次，确认任务表与调度器已对齐，并清掉调度器侧的孤儿触发器
        JobSchedulerReconcileReport report = jobSchedulerReconciler.reconcile();
        log.info("[syncJob][对账完成：{}]", report.getSummary());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long id) throws SchedulerException {
        // 校验存在
        JobDO job = validateJobExists(id);
        // 更新
        jobMapper.deleteById(id);

        // 删除 Job 到 Quartz 中
        schedulerManager.deleteJob(job.getHandlerName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobList(List<Long> ids) throws SchedulerException {
        // 批量删除
        List<JobDO> jobs = jobMapper.selectByIds(ids);
        jobMapper.deleteByIds(ids);

        // 删除 Job 到 Quartz 中
        for (JobDO job : jobs) {
            schedulerManager.deleteJob(job.getHandlerName());
        }
    }

    private JobDO validateJobExists(Long id) {
        JobDO job = jobMapper.selectById(id);
        if (job == null) {
            throw exception(JOB_NOT_EXISTS);
        }
        return job;
    }

    private void validateCronExpression(String cronExpression) {
        if (!CronUtils.isValid(cronExpression)) {
            throw exception(JOB_CRON_EXPRESSION_VALID);
        }
    }

    @Override
    public JobDO getJob(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public PageResult<JobDO> getJobPage(JobPageReqVO pageReqVO) {
        return jobMapper.selectPage(pageReqVO);
    }

    private static void fillJobMonitorTimeoutEmpty(JobDO job) {
        if (job.getMonitorTimeout() == null) {
            job.setMonitorTimeout(0);
        }
    }

}
