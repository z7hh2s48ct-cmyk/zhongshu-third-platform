package cn.zszj.module.bpm.framework.flowable.core.listener;

import cn.zszj.module.bpm.service.definition.BpmModelService;
import cn.zszj.module.bpm.service.task.BpmProcessInstanceService;
import cn.zszj.module.bpm.service.task.BpmTaskService;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BPM 全局 Flowable 事件监听器对首链流程的豁免合同（真实 server 暴露的缺陷回归锚点）。
 *
 * <p>背景：{@code BpmProcessInstanceEventListener}/{@code BpmTaskEventListener} 是上游 BPM 模型管理能力的监听器，
 * 事件处理会查询 {@code bpm_process_definition_info} 等上游业务扩展表——这些表在本产品的 PG 基线中<b>不存在</b>
 * （接入合同 §3 不为未来域预建表；BPM 仅迁移首链业务表）。首链流程（{@code firstChainApplicationApproval}）由代码生成部署，
 * 自有领域状态机/绑定/通知接线，不依赖这些表。监听器在真实 server（{@code BpmFlowableConfiguration} 注册全部监听器）
 * 对首链流程的 PROCESS_CREATED 查询缺表，抛 {@code relation "bpm_process_definition_info" does not exist}，
 * 整个提交事务回滚——申请提交 500。mvn 单测/PG 套件的夹具只注册探针监听器，故此前从未暴露。
 *
 * <p>合同：首链流程（按流程定义编号前缀判定，无需查库）的事件<b>一律不进入</b>上游服务；其他流程原样委托。
 *
 * <p>夹具真实性：真实引擎的流程实例类与任务类<b>实体事件</b>（{@code createEntityEvent(type, entity)}）不携带流程定义编号，
 * 编号只在实体上（{@code ProcessInstance/Task#getProcessDefinitionId}）；本测试据此构造事件，判定必须取自实体。
 * （首版夹具把编号塞进事件，掩盖了该差异，真实 server 上豁免未生效。）
 *
 * @author ZS-FC-001
 */
class BpmEventListenerFirstChainFilterTest {

    private static final String FIRST_CHAIN_DEFINITION_ID = "firstChainApplicationApproval:1:7a1b2c3d";

    private static final String OTHER_DEFINITION_ID = "leaveApproval:2:9f8e7d6c";

    private BpmProcessInstanceService processInstanceService;

    private BpmTaskService taskService;

    private BpmModelService modelService;

    private BpmProcessInstanceEventListener processListener;

    private BpmTaskEventListener taskListener;

    @BeforeEach
    void setUp() {
        processInstanceService = mock(BpmProcessInstanceService.class);
        taskService = mock(BpmTaskService.class);
        modelService = mock(BpmModelService.class);
        processListener = new BpmProcessInstanceEventListener();
        ReflectionTestUtils.setField(processListener, "processInstanceService", processInstanceService);
        taskListener = new BpmTaskEventListener();
        ReflectionTestUtils.setField(taskListener, "taskService", taskService);
        ReflectionTestUtils.setField(taskListener, "modelService", modelService);
    }

    // ========== 首链流程：不进入上游服务 ==========

    @Test
    void processCreated_firstChain_isSkipped() {
        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, processInstance(FIRST_CHAIN_DEFINITION_ID)));

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void processCompleted_firstChain_isSkipped() {
        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED, processInstance(FIRST_CHAIN_DEFINITION_ID)));

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void processCancelled_firstChain_isSkippedBeforeAnyLookup() {
        processListener.onEvent(FlowableEventBuilder.createCancelledEvent("e1", "p1", FIRST_CHAIN_DEFINITION_ID,
                "cancel"));

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void processCancelled_firstChain_resolvedViaInstanceLookup_isSkipped() {
        // 取消事件不携带定义编号时，经实例回查得到首链实例仍须豁免（不进入 processProcessInstanceCompleted）
        ProcessInstance firstChain = processInstance(FIRST_CHAIN_DEFINITION_ID);
        when(processInstanceService.getProcessInstance("p1")).thenReturn(firstChain);

        processListener.onEvent(FlowableEventBuilder.createCancelledEvent("e1", "p1", null, "cancel"));

        verify(processInstanceService, org.mockito.Mockito.never()).processProcessInstanceCompleted(any());
    }

    @Test
    void processCancelled_otherProcess_resolvedViaInstanceLookup_delegates() {
        ProcessInstance instance = processInstance(OTHER_DEFINITION_ID);
        when(processInstanceService.getProcessInstance("p1")).thenReturn(instance);

        processListener.onEvent(FlowableEventBuilder.createCancelledEvent("e1", "p1", null, "cancel"));

        verify(processInstanceService).processProcessInstanceCompleted(instance);
    }

    @Test
    void processCreated_firstChain_bareUuidDefinitionId_isSkippedByInstanceKey() {
        // 真实引擎：key:version:uuid 超 64 字符时定义编号退化为纯 UUID（首链 key 29 + UUID 36 必然超长），前缀判定失效，
        // 须按实例携带的流程定义 key 判定——这是真实 server 上豁免首版未生效的根因
        ProcessInstance instance = processInstance("3f2a9c1e-7b4d-4e8a-9d11-0c5b6a7e8f90", "firstChainApplicationApproval");

        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, instance));

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void processCreated_otherProcess_bareUuidDefinitionId_delegates() {
        ProcessInstance instance = processInstance("9c1e3f2a-4d7b-8a4e-11d9-90f0c5b6a7e8", "leaveApproval");

        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, instance));

        verify(processInstanceService).processProcessInstanceCreated(instance);
    }

    @Test
    void taskCreatedAssignedCompleted_firstChain_areSkipped() {
        Task task = task(FIRST_CHAIN_DEFINITION_ID);

        taskListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
        taskListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, task));
        taskListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_COMPLETED, task));

        verifyNoInteractions(taskService);
    }

    // ========== 其他流程：原样委托（上游行为零变化） ==========

    @Test
    void processCreated_otherProcess_delegates() {
        ProcessInstance instance = processInstance(OTHER_DEFINITION_ID);

        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, instance));

        verify(processInstanceService).processProcessInstanceCreated(instance);
    }

    @Test
    void processCompleted_otherProcess_delegates() {
        ProcessInstance instance = processInstance(OTHER_DEFINITION_ID);

        processListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED, instance));

        verify(processInstanceService).processProcessInstanceCompleted(instance);
    }

    @Test
    void taskCreated_otherProcess_delegates() {
        Task task = task(OTHER_DEFINITION_ID);

        taskListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));

        verify(taskService).processTaskCreated(task);
    }

    @Test
    void firstChainKeyAsPrefixOfAnotherKey_isNotMisjudged() {
        // 判定按「key + ':'」前缀：key 本身是他流程 key 的前缀（如 firstChainApplicationApprovalV2）不得被误豁免
        String lookalike = "firstChainApplicationApprovalV2:1:abc";
        Task task = task(lookalike);

        taskListener.onEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));

        verify(taskService).processTaskCreated(any(Task.class));
    }

    // ========== 夹具 ==========

    private static ProcessInstance processInstance(String definitionId) {
        return processInstance(definitionId, definitionId.split(":")[0]);
    }

    private static ProcessInstance processInstance(String definitionId, String definitionKey) {
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessDefinitionId()).thenReturn(definitionId);
        when(instance.getProcessDefinitionKey()).thenReturn(definitionKey);
        when(instance.getTenantId()).thenReturn("1");
        return instance;
    }

    private static Task task(String definitionId) {
        Task task = mock(Task.class);
        when(task.getProcessDefinitionId()).thenReturn(definitionId);
        when(task.getTenantId()).thenReturn("1");
        return task;
    }

}
