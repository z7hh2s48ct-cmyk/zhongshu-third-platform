package cn.zszj.module.bpm.harness;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ZS-BPM-001 异步回声委托：异步服务任务执行时向夹具探针表写入一行（流程实例 ID），
 * 供「异步执行器积压恢复/实时消费」断言。探针表 bpm_harness_probe 由编排器以 owner 账号
 * 预建（默认权限自动授权 app 账号 DML），属一次性测试脚手架，随容器销毁。
 */
public class NeutralEchoDelegate implements JavaDelegate {

    private final DataSource dataSource;

    public NeutralEchoDelegate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String instanceId = execution.getProcessInstanceId();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO bpm_harness_probe(note) VALUES (?)")) {
            statement.setString(1, instanceId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new FlowableException("[bpm-pg-harness] 探针写入失败", e);
        }
    }
}
