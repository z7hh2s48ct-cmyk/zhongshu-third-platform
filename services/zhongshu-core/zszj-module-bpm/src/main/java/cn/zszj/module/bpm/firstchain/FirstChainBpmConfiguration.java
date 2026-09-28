package cn.zszj.module.bpm.firstchain;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 首链领域状态与幂等写回装配（ZS-BPM-003）。
 *
 * <p>BPM 模块启用时随组件扫描生效；引擎服务（RepositoryService/RuntimeService/TaskService）由
 * BPM 模块既有 Flowable 装配（与 BpmPgHarnessConfiguration 同一扩展点机制）供给。流程端口以
 * {@code FlowableFirstChainProcessAdapter} 适配真实引擎；领域服务全部走
 * DataSource + PlatformTransactionManager（与 BPM-004 接入合同「同源数据源/同源事务」一致）。
 *
 * @author ZS-BPM-003
 */
@Configuration(proxyBeanMethods = false)
public class FirstChainBpmConfiguration {

    @Bean
    public FirstChainStateTransitionExecutor firstChainStateTransitionExecutor(DataSource dataSource) {
        return new FirstChainStateTransitionExecutor(dataSource);
    }

    @Bean
    public FirstChainProcessBindingService firstChainProcessBindingService(
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            DataSource dataSource) {
        return new FirstChainProcessBindingService(dataSource, transactionManager);
    }

    @Bean
    public FirstChainMetricsService firstChainMetricsService(DataSource dataSource) {
        return new FirstChainMetricsService(dataSource);
    }

    @Bean
    public FirstChainProcessPort firstChainProcessPort(RepositoryService repositoryService,
                                                       RuntimeService runtimeService, TaskService taskService) {
        return new FlowableFirstChainProcessAdapter(repositoryService, runtimeService, taskService);
    }

    @Bean
    public FirstChainApplicationService firstChainApplicationService(
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            cn.zszj.framework.common.biz.system.audit.AuditPort auditPort,
            FirstChainStateTransitionExecutor stateTransitionExecutor,
            FirstChainProcessBindingService processBindingService,
            FirstChainProcessPort processPort,
            DataSource dataSource) {
        return new FirstChainApplicationService(dataSource, transactionManager, auditPort,
                stateTransitionExecutor, processBindingService, processPort);
    }

}
