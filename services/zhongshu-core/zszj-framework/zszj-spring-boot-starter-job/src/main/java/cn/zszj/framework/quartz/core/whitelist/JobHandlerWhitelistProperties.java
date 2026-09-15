package cn.zszj.framework.quartz.core.whitelist;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务配置项（ZS-JOB-001）
 */
@Data
@ConfigurationProperties(prefix = "zszj.job")
public class JobHandlerWhitelistProperties {

    /**
     * JobHandler 白名单，元素为 Spring Bean 名字（即 infra_job.handler_name）
     *
     * 语义：
     * 1. 为空表示"未启用白名单管控"，放行全部 Handler——用于尚未固化清单的环境批次，避免升级即中断既有任务；
     * 2. 非空表示"已启用"，只有清单内的 Handler 才允许被登记（创建/修改）与执行，其余一律拒绝。
     *
     * 生产环境应显式配置，把可执行的 Handler 固化在配置里，而不是依赖"容器里恰好有这个 Bean"。
     */
    private List<String> handlerWhitelist = new ArrayList<>();

}
