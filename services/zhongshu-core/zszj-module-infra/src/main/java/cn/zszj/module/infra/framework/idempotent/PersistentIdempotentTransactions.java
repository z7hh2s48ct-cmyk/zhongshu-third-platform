package cn.zszj.module.infra.framework.idempotent;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * 持久化幂等事务模板构造（ZS-SEC-011.B，循 ZS-JOB-002 {@code OutboxTransactions} 同源配对先例）。
 *
 * <p>MANDATORY 传播只证明「该 TM 存在事务」，不能证明 JDBC 写入参与其中——注入 DataSource=A、TM=B 的
 * 错配下，B 事务中经 A 数据源的写入会走 autocommit 连接静默自提交。故构造期即校验：TM 必须是管理
 * <b>同一数据源</b>的 {@link DataSourceTransactionManager}，否则启动即失败（fail-fast）。
 * （未复用 OutboxTransactions 本体：其 package-private 属 outbox 包，为不动 JOB-002/004 既有文件面，
 * 在本包镜像最小实现，语义与配对校验逐行对齐。）
 */
final class PersistentIdempotentTransactions {

    private PersistentIdempotentTransactions() {
    }

    /**
     * 构造 MANDATORY 事务模板：必须加入调用方在本数据源上的真实事务，无即抛
     * {@code IllegalTransactionStateException}（不另开事务自提交）——
     * INSERT RUNNING → 业务 → markSuccess 与业务同事务提交，是「丢响应可复用原结果、
     * 重启不重复写、业务回滚无残留记录」的原子性根基。
     *
     * @throws IllegalArgumentException TM 与数据源非同源配对
     */
    static TransactionTemplate mandatoryTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        TransactionTemplate template = pairedTemplate(dataSource, transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
        return template;
    }

    /** 同源配对校验：TM 必须是管理同一数据源的 {@link DataSourceTransactionManager}。 */
    private static TransactionTemplate pairedTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        if (!(transactionManager instanceof DataSourceTransactionManager dataSourceTxManager)
                || !Objects.equals(dataSourceTxManager.getDataSource(), dataSource)) {
            throw new IllegalArgumentException(
                    "持久化幂等要求 TransactionManager 与业务 DataSource 同源配对（DataSourceTransactionManager 管理同一数据源），"
                            + "实际 TM=" + transactionManager.getClass().getName());
        }
        return new TransactionTemplate(transactionManager);
    }

}
