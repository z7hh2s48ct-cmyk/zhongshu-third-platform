package cn.zszj.module.bpm.harness;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * ZS-BPM-001 批准的 BPM 测试装配：手工装配 SpringProcessEngineConfiguration + Spring 事务管理器，
 * 与 BpmFlowableConfiguration 所依赖的引擎装配机制同构（同一扩展点），但不引入 System 模块与
 * 候选人策略（审批资格归 ZS-BPM-002），专验引擎在真实 PostgreSQL 上的建表、事务、租户标签、
 * 异步执行器与启停。
 *
 * 环境变量（由 scripts/db/run-bpm001-verify.mjs 按阶段注入，缺失即快速失败、不静默跳过）：
 *   ZSZJ_BPM_HARNESS_JDBC_URL      必须 jdbc:postgresql:// 开头；
 *   ZSZJ_BPM_HARNESS_USERNAME      bootstrap 阶段=zhongshu_owner，runtime 阶段=zhongshu_app（低权限）；
 *   ZSZJ_BPM_HARNESS_PASSWORD
 *   ZSZJ_BPM_HARNESS_SCHEMA_UPDATE true=引擎自管建表/升级（仅 owner 迁移阶段），false=运行期不改结构；
 *   ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR true=启动异步执行器（runtime 积压恢复），false=挂起留积压（bootstrap）。
 */
@Configuration(proxyBeanMethods = false)
public class BpmPgHarnessConfiguration {

    static final String ENV_URL = "ZSZJ_BPM_HARNESS_JDBC_URL";
    static final String ENV_USERNAME = "ZSZJ_BPM_HARNESS_USERNAME";
    static final String ENV_PASSWORD = "ZSZJ_BPM_HARNESS_PASSWORD";
    static final String ENV_SCHEMA_UPDATE = "ZSZJ_BPM_HARNESS_SCHEMA_UPDATE";
    static final String ENV_ASYNC_EXECUTOR = "ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR";

    static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[bpm-pg-harness] 缺少环境变量 " + key + "：验证不得静默跳过，须由 run-bpm001-verify.mjs 注入");
        }
        return value.trim();
    }

    static boolean requireFlag(String key) {
        String value = requireEnv(key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException("[bpm-pg-harness] 环境变量 " + key + " 须为 true/false，实际=" + value);
        }
        return Boolean.parseBoolean(value);
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource bpmHarnessDataSource() {
        String url = requireEnv(ENV_URL);
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("[bpm-pg-harness] 夹具仅面向真实 PostgreSQL，收到 " + url);
        }
        // 全部环境参数在创建连接池前完成校验；密码缺失同样快速失败（不静默回退空口令）
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(requireEnv(ENV_USERNAME));
        config.setPassword(requireEnv(ENV_PASSWORD));
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setPoolName("bpm-pg-harness");
        // 驱动由 zszj-spring-boot-starter-mybatis 传入 PG 驱动
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }

    @Bean
    public PlatformTransactionManager bpmHarnessTransactionManager(DataSource dataSource) {
        // 与应用运行形态同构：引擎与业务共用 Spring DataSourceTransactionManager 的事务边界
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate bpmHarnessTransactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    @Bean
    public SpringProcessEngineConfiguration bpmHarnessProcessEngineConfiguration(
            DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        // 引擎表迁移责任：bootstrap 阶段（owner 账号）允许引擎自建/升级；runtime 阶段（app 低权限）禁止改结构
        configuration.setDatabaseSchemaUpdate(requireFlag(ENV_SCHEMA_UPDATE)
                ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
        configuration.setAsyncExecutorActivate(requireFlag(ENV_ASYNC_EXECUTOR));
        configuration.setDeploymentName("bpmPgHarness");
        // 与 BpmFlowableConfiguration 相同的扩展点：注册引擎事件监听
        configuration.setEventListeners(java.util.List.of(new BpmPgHarness.EngineEventRecorder()));
        return configuration;
    }

    @Bean
    public ProcessEngineFactoryBean bpmHarnessProcessEngine(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public org.flowable.job.service.impl.asyncexecutor.AsyncExecutor bpmHarnessAsyncExecutor(
            SpringProcessEngineConfiguration configuration) {
        // 与引擎构建使用同一实例（configuration 惰性创建默认执行器），供测试显式启停（isActive/start/shutdown）
        return configuration.getAsyncExecutor();
    }

    @Bean
    public JdbcTemplate bpmHarnessJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NeutralEchoDelegate neutralEchoDelegate(DataSource dataSource) {
        return new NeutralEchoDelegate(dataSource);
    }
}
