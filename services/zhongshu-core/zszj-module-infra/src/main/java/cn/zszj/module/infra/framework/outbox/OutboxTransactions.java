package cn.zszj.module.infra.framework.outbox;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Outbox 事务模板构造（ZS-JOB-002，codex r2）：强制 DataSource 与 TransactionManager <b>同源配对</b>。
 *
 * <p>MANDATORY 传播只证明「该 TM 存在事务」，不能证明 JDBC 写入参与其中——注入 DataSource=A、TM=B 的
 * 错配下，B 事务中经 A 数据源的写入会走 autocommit 连接静默自提交，业务回滚而事件留存（无异常可捕获）。
 * 故构造期即校验：TM 必须是管理<b>同一数据源</b>的 {@link DataSourceTransactionManager}，否则启动即失败
 * （fail-fast），杜绝运行期静默分叉。
 */
final class OutboxTransactions {

    private OutboxTransactions() {
    }

    /**
     * 构造 MANDATORY 事务模板（写入端口用）：必须加入调用方在本数据源上的真实事务，无即抛
     * {@code IllegalTransactionStateException}（REQUIRED 在他数据源事务/仅普通查询绑定连接的情形下
     * 会静默另开事务自提交，丢失「业务回滚无事件」保证）。
     *
     * @throws IllegalArgumentException TM 与数据源非同源配对
     */
    static TransactionTemplate mandatoryTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        TransactionTemplate template = pairedTemplate(dataSource, transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
        return template;
    }

    /**
     * 构造 REQUIRED 事务模板（派发器领取用）：无外部事务则自开独立短事务（dispatchOnce 已强制
     * 无外部事务上下文），有则加入。
     *
     * @throws IllegalArgumentException TM 与数据源非同源配对
     */
    static TransactionTemplate requiredTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        TransactionTemplate template = pairedTemplate(dataSource, transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return template;
    }

    /** 同源配对校验：TM 必须是管理同一数据源的 {@link DataSourceTransactionManager}。 */
    private static TransactionTemplate pairedTemplate(DataSource dataSource, PlatformTransactionManager transactionManager) {
        if (!(transactionManager instanceof DataSourceTransactionManager dataSourceTxManager)
                || !Objects.equals(dataSourceTxManager.getDataSource(), dataSource)) {
            throw new IllegalArgumentException(
                    "Outbox 要求 TransactionManager 与业务 DataSource 同源配对（DataSourceTransactionManager 管理同一数据源），"
                            + "实际 TM=" + transactionManager.getClass().getName());
        }
        return new TransactionTemplate(transactionManager);
    }

}
