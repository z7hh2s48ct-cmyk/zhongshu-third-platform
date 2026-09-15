package cn.zszj.framework.quartz.config;

import cn.zszj.framework.quartz.core.scheduler.SchedulerManager;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistProperties;
import cn.zszj.framework.quartz.core.whitelist.JobHandlerWhitelistValidator;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

/**
 * 定时任务 Configuration
 */
@AutoConfiguration
@EnableScheduling // 开启 Spring 自带的定时任务
@EnableConfigurationProperties(JobHandlerWhitelistProperties.class)
@Slf4j
public class ZszjQuartzAutoConfiguration {

    @Bean
    public SchedulerManager schedulerManager(Optional<Scheduler> scheduler) {
        if (!scheduler.isPresent()) {
            log.info("[定时任务 - 已禁用][参考 https://doc.iocoder.cn/job/ 开启]");
            return new SchedulerManager(null);
        }
        return new SchedulerManager(scheduler.get());
    }

    /**
     * JobHandler 白名单校验器（ZS-JOB-001）
     *
     * 由 zszj.job.handler-whitelist 配置驱动，供任务登记与任务执行两侧共用同一份判定。
     */
    @Bean
    public JobHandlerWhitelistValidator jobHandlerWhitelistValidator(JobHandlerWhitelistProperties properties) {
        JobHandlerWhitelistValidator validator = new JobHandlerWhitelistValidator(properties);
        if (validator.isEnabled()) {
            log.info("[jobHandlerWhitelistValidator][JobHandler 白名单已启用，共({})个：{}]",
                    properties.getHandlerWhitelist().size(), properties.getHandlerWhitelist());
        } else {
            log.warn("[jobHandlerWhitelistValidator][未配置 zszj.job.handler-whitelist，当前放行全部 JobHandler；"
                    + "生产环境请显式固化可执行的 Handler 清单]");
        }
        return validator;
    }

}
