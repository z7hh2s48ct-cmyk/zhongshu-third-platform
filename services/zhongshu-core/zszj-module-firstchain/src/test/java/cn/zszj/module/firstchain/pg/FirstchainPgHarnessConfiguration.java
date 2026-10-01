package cn.zszj.module.firstchain.pg;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cn.zszj.module.firstchain.service.FirstchainLeadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * ZS-FC-001/002 真实 PostgreSQL 运行期夹具装配（由 scripts/db/run-firstchain-verify.mjs 注入
 * {@code ZSZJ_FIRSTCHAIN_HARNESS_*} 环境变量调用；缺环境快速失败，不静默跳过）。
 *
 * <p>循 {@code BpmPgHarnessConfiguration} 形态但不装配 Flowable 引擎——首链申请域经 bpm 域
 * {@code FirstChainApplicationService}（StubProcessPort 隔离流程引擎，真实引擎已由 bpm003 套件直证），
 * 本夹具专验 firstchain 模块业务域在真实 PG 上的运行期语义：开通同事务幂等/回滚、线索五态链、
 * 分发幂等（COALESCE 方言路径）、并发领取唯一、终态锁定、跨租户隔离。
 *
 * @author ZS-FC-002
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class FirstchainPgHarnessConfiguration {

    static final String ENV_URL = "ZSZJ_FIRSTCHAIN_HARNESS_JDBC_URL";
    static final String ENV_USERNAME = "ZSZJ_FIRSTCHAIN_HARNESS_USERNAME";
    static final String ENV_PASSWORD = "ZSZJ_FIRSTCHAIN_HARNESS_PASSWORD";

    static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[firstchain-pg-harness] 缺少环境变量 " + key
                    + "：验证不得静默跳过，须由 run-firstchain-verify.mjs 注入");
        }
        return value.trim();
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource firstchainHarnessDataSource() {
        String url = requireEnv(ENV_URL);
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("[firstchain-pg-harness] 夹具仅面向真实 PostgreSQL，收到 " + url);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(requireEnv(ENV_USERNAME));
        config.setPassword(requireEnv(ENV_PASSWORD));
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setPoolName("firstchain-pg-harness");
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }

    @Bean
    public PlatformTransactionManager firstchainHarnessTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate firstchainHarnessTransactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    @Bean
    public JdbcTemplate firstchainHarnessJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /** 归属校验桩（真实 PG 套件自持，不查 system）：300/301 属于组织 100，其余全拒。 */
    @Bean
    public FirstchainLeadService.UserOrgChecker firstchainHarnessUserOrgChecker() {
        return (tenantId, userId, orgId) -> Long.valueOf(1L).equals(tenantId) && Long.valueOf(100L).equals(orgId)
                && (Long.valueOf(300L).equals(userId) || Long.valueOf(301L).equals(userId));
    }

}
