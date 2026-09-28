package cn.zszj.module.bpm.firstchain;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.task.api.Task;

import java.util.List;
import java.util.Map;

/**
 * Flowable 实现的首链审批流程端口（ZS-BPM-003，D-07 M10=B 单节点审批流）。
 *
 * <p>流程形状：start → 单审批人 UserTask（assignee=${approverUserId}）→ end；部署幂等
 * （按 processDefinitionKey 判重，引擎内自升级交由 Flowable 版本机制）。审批完成由调用方编排：
 * <b>先经 {@link #completeApprovalTask} 完成 Flowable 任务，再同事务调用
 * {@code FirstChainApplicationService#onApprovalCompleted}</b>（两者共享应用数据源与事务管理器，
 * 领域幂等门吸收重复）；转派经 {@code TaskService#setAssignee} 属流程侧操作，不影响领域状态（验收①）。
 *
 * <p>真实 PostgreSQL + 真实引擎联验（通过/拒绝/撤回/转派四操作与幂等）归
 * {@code scripts/db/run-bpm003-verify.mjs} 套件。
 *
 * @author ZS-BPM-003
 */
@Slf4j
public class FlowableFirstChainProcessAdapter implements FirstChainProcessPort {

    /** 流程定义 key（单节点加盟商申请审批） */
    public static final String PROCESS_KEY = "firstChainApplicationApproval";

    /** 流程变量：审批人（UserTask assignee 表达式取值） */
    public static final String VAR_APPROVER = "approverUserId";

    private final RepositoryService repositoryService;

    private final RuntimeService runtimeService;

    private final TaskService taskService;

    /** 已完成（按租户）幂等部署的租户标记（引擎定义按 key+tenant 查找，部署须同租户） */
    private final java.util.Set<String> deployedTenants = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public FlowableFirstChainProcessAdapter(RepositoryService repositoryService, RuntimeService runtimeService,
                                            TaskService taskService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public synchronized String startApprovalProcess(Long tenantId, String appKey, Long approverUserId) {
        ensureDeployed(String.valueOf(tenantId));
        // 引擎级租户标签（循模块惯例 BpmProcessInstanceServiceImpl.processInstanceTenantId）：
        // 引擎查询按租户分组可见；领域租户权威仍为绑定行
        return runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(PROCESS_KEY)
                .businessKey(appKey)
                .tenantId(String.valueOf(tenantId))
                .variable("tenantId", tenantId)
                .variable(VAR_APPROVER, approverUserId)
                .start()
                .getId();
    }

    @Override
    public boolean withdrawProcess(String processInstanceId) {
        try {
            runtimeService.deleteProcessInstance(processInstanceId, "WITHDRAWN_BY_APPLICANT");
            return true;
        } catch (org.flowable.common.engine.api.FlowableObjectNotFoundException alreadyGone) {
            // 实例已不存在（并发完成/取消先到）：交由绑定幂等门分类（冲突或吸收），不在此处抛引擎原生异常
            return false;
        }
    }

    /**
     * 完成审批任务（流程侧事实操作；领域写回由调用方同事务接 {@code onApprovalCompleted}）。
     *
     * @return 已完成的任务编号
     */
    public String completeApprovalTask(String processInstanceId, boolean approved, String reason) {
        Task task = requireActiveTask(processInstanceId);
        taskService.complete(task.getId(), Map.of(
                "approved", approved,
                "reason", reason == null ? "" : reason,
                "outcome", approved ? FirstChainProcessBindingService.OUTCOME_APPROVED
                        : FirstChainProcessBindingService.OUTCOME_REJECTED));
        return task.getId();
    }

    /**
     * 转派审批任务（流程侧操作；D-07 验收「转派与业务状态一致」= 领域状态不变）。
     *
     * @return 被转派的任务编号
     */
    public String transferApprovalTask(String processInstanceId, String targetAssignee) {
        Task task = requireActiveTask(processInstanceId);
        taskService.setAssignee(task.getId(), targetAssignee);
        return task.getId();
    }

    private Task requireActiveTask(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("流程无活跃审批任务：processInstanceId=" + processInstanceId);
        }
        return tasks.get(0);
    }

    /**
     * 部署幂等（按流程定义 key + 租户判重）：定义查找按 (key, tenantId) 匹配，部署必须与实例同租户；
     * 并发部署由 Flowable 定义版本机制兜底，重复部署无业务影响。
     */
    private void ensureDeployed(String tenantId) {
        if (deployedTenants.contains(tenantId)) {
            return;
        }
        synchronized (this) {
            if (deployedTenants.contains(tenantId)) {
                return;
            }
            long existing = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(PROCESS_KEY)
                    .processDefinitionTenantId(tenantId)
                    .count();
            if (existing == 0) {
                repositoryService.createDeployment()
                        .tenantId(tenantId)
                        .addBpmnModel(PROCESS_KEY + ".bpmn20.xml", buildModel())
                        .name("首链加盟商申请审批（ZS-BPM-003）")
                        .deploy();
                log.info("[ensureDeployed][首链审批流程已部署：key={} tenant={}]", PROCESS_KEY, tenantId);
            }
            deployedTenants.add(tenantId);
        }
    }

    private BpmnModel buildModel() {
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId(PROCESS_KEY);
        process.setName("首链加盟商申请审批");
        StartEvent start = new StartEvent();
        start.setId("start");
        UserTask task = new UserTask();
        task.setId("approveTask");
        task.setName("加盟商申请审批");
        task.setAssignee("${" + VAR_APPROVER + "}");
        EndEvent end = new EndEvent();
        end.setId("end");
        SequenceFlow flow1 = new SequenceFlow("start", "approveTask");
        flow1.setId("flow-start-task");
        SequenceFlow flow2 = new SequenceFlow("approveTask", "end");
        flow2.setId("flow-task-end");
        process.addFlowElement(start);
        process.addFlowElement(flow1);
        process.addFlowElement(task);
        process.addFlowElement(flow2);
        process.addFlowElement(end);
        model.addProcess(process);
        return model;
    }

}
