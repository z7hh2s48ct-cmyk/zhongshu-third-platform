package cn.zszj.module.infra.framework.inbox;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Inbox 事务模板构造（ZS-JOB-003）——与 outbox 包 OutboxTransactions 同合同（同源配对 + MANDATORY），
 * 包内独立以保持两个机制独立可评审（互为写侧/读侧对称件）。
 *
 * <p>MANDATORY 传播只证明「该 TM 存在事务」，不能证明 JDBC 写入参与其中——注入 DataSource=A、TM=B 的
 * 错配下，B 事务中经 A 数据源的写入会走 autocommit 连接静默自提交，业务回滚而幂等记录留存（无异常可捕获）。
 * 故构造期即校验：TM 必须是管理<b>同一数据源</b>的 {@link DataSourceTransactionManager}，否则启动即失败。
 */
final class InboxTransactions {

    private InboxTransactions() {
    }

    /**
     * 构造 MANDATORY 事务模板：必须加入调用方在本数据源上的真实事务，无即抛
     * {@code IllegalTransactionStateException}（幂等记录与业务副作用同事务，不代开、不静默自提交）。
     *
     * @throws IllegalArgumentException TM 与数据源非同源配对
     */
    static TransactionTemplate mandatoryTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        if (!(transactionManager instanceof DataSourceTransactionManager dataSourceTxManager)
                || !Objects.equals(dataSourceTxManager.getDataSource(), dataSource)) {
            throw new IllegalArgumentException(
                    "Inbox 要求 TransactionManager 与业务 DataSource 同源配对（DataSourceTransactionManager 管理同一数据源），"
                            + "实际 TM=" + transactionManager.getClass().getName());
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
        return template;
    }

}
