package cn.zszj.module.bpm.firstchain;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首链流程部署的「已部署租户」缓存与事务边界合同（真实 server 暴露的健壮性缺陷回归锚点）。
 *
 * <p>背景：{@code ensureDeployed} 在<b>调用方事务内</b>部署流程定义，却在部署后立刻把租户记入内存缓存。
 * 若该事务随后回滚（例如提交申请时后续接线失败），Flowable 的部署一并回滚、定义不存在，但缓存仍认为「已部署」——
 * 之后<b>每一次</b>提交都跳过部署并报 {@code Process definition with key ... and tenantId ... was not found}，
 * 直到服务重启。合同：缓存只在事务提交后才记录（无事务时立即记录）；回滚则不记录，下次提交重新部署。
 *
 * @author ZS-FC-001
 */
class FlowableFirstChainProcessAdapterDeployTest {

    private RepositoryService repositoryService;

    private RuntimeService runtimeService;

    private DeploymentBuilder deploymentBuilder;

    private FlowableFirstChainProcessAdapter adapter;

    @BeforeEach
    void setUp() {
        repositoryService = mock(RepositoryService.class);
        runtimeService = mock(RuntimeService.class);
        deploymentBuilder = mock(DeploymentBuilder.class, RETURNS_SELF);
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class, RETURNS_SELF);
        when(query.count()).thenReturn(0L); // 引擎里始终查不到定义（部署随回滚消失 / 或尚未提交）
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
        ProcessInstanceBuilder builder = mock(ProcessInstanceBuilder.class, RETURNS_SELF);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("pi-1");
        when(builder.start()).thenReturn(instance);
        when(runtimeService.createProcessInstanceBuilder()).thenReturn(builder);
        adapter = new FlowableFirstChainProcessAdapter(repositoryService, runtimeService, mock(TaskService.class));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rolledBackTransaction_doesNotMarkTenantDeployed_nextSubmitRedeploys() {
        // 第 1 次提交：事务内部署，随后回滚（afterCommit 不触发）
        TransactionSynchronizationManager.initSynchronization();
        adapter.startApprovalProcess(1L, "FC-1", 100L);
        TransactionSynchronizationManager.clearSynchronization(); // 回滚：同步器被丢弃，从未 afterCommit

        // 第 2 次提交：缓存不得残留「已部署」，须重新部署
        adapter.startApprovalProcess(1L, "FC-2", 100L);

        verify(deploymentBuilder, times(2)).deploy();
    }

    @Test
    void committedTransaction_marksTenantDeployed_nextSubmitSkipsDeploy() {
        TransactionSynchronizationManager.initSynchronization();
        adapter.startApprovalProcess(1L, "FC-1", 100L);
        List<TransactionSynchronization> synchronizations =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        TransactionSynchronizationManager.clearSynchronization();
        synchronizations.forEach(TransactionSynchronization::afterCommit); // 提交

        adapter.startApprovalProcess(1L, "FC-2", 100L);

        verify(deploymentBuilder, times(1)).deploy(); // 已提交的部署被缓存，不重复部署
    }

    @Test
    void noTransaction_marksTenantDeployedImmediately() {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        adapter.startApprovalProcess(1L, "FC-1", 100L);
        adapter.startApprovalProcess(1L, "FC-2", 100L);

        verify(deploymentBuilder, times(1)).deploy();
    }

}
