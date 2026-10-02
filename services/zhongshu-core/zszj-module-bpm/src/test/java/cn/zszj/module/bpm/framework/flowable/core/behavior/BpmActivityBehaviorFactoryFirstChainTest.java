package cn.zszj.module.bpm.framework.flowable.core.behavior;

import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BpmActivityBehaviorFactory} 对首链审批节点的豁免合同（真实 server 暴露的缺陷回归锚点）。
 *
 * <p>背景：上游 {@link BpmUserTaskActivityBehavior} 覆写 {@code handleAssignments}，一律经
 * {@code BpmTaskCandidateInvoker} 按节点上的「候选人策略」扩展属性计算审批人；首链审批流程由代码生成，
 * 用户任务只声明 {@code assignee="${approverUserId}"}，没有候选人策略配置——真实 server 提交申请时任务创建走到
 * {@code getCandidateStrategy(null)}，断言「策略(null) 不存在」失败，申请提交 500。mvn 单测/PG 套件的夹具不装配该
 * 自定义工厂（只验引擎与探针监听器），故此前从未暴露。
 *
 * <p>合同：首链流程（{@code firstChainApplicationApproval}）的用户任务使用 Flowable 默认行为（assignee 表达式取流程变量）；
 * 其他流程仍使用上游自定义行为，零变化。
 *
 * @author ZS-FC-001
 */
class BpmActivityBehaviorFactoryFirstChainTest {

    private final BpmActivityBehaviorFactory factory = new BpmActivityBehaviorFactory();

    @Test
    void firstChainUserTask_usesDefaultBehavior() {
        UserTask task = userTaskIn("firstChainApplicationApproval");

        UserTaskActivityBehavior behavior = factory.createUserTaskActivityBehavior(task);

        assertThat(behavior).isNotInstanceOf(BpmUserTaskActivityBehavior.class);
    }

    @Test
    void otherProcessUserTask_keepsBpmCustomBehavior() {
        UserTask task = userTaskIn("leaveApproval");

        UserTaskActivityBehavior behavior = factory.createUserTaskActivityBehavior(task);

        assertThat(behavior).isInstanceOf(BpmUserTaskActivityBehavior.class);
    }

    @Test
    void userTaskWithoutParentContainer_keepsBpmCustomBehavior() {
        // 无父容器（理论上不会发生）按「非首链」处理，保持上游行为，不因判定缺失而误豁免
        UserTask task = new UserTask();
        task.setId("orphan");

        UserTaskActivityBehavior behavior = factory.createUserTaskActivityBehavior(task);

        assertThat(behavior).isInstanceOf(BpmUserTaskActivityBehavior.class);
    }

    private static UserTask userTaskIn(String processKey) {
        Process process = new Process();
        process.setId(processKey);
        UserTask task = new UserTask();
        task.setId("approveTask");
        task.setAssignee("${approverUserId}");
        process.addFlowElement(task);
        return task;
    }

}
