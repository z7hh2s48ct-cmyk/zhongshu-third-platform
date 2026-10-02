package cn.zszj.module.bpm.framework.flowable.core.behavior;

import cn.zszj.module.bpm.firstchain.FlowableFirstChainProcessAdapter;
import cn.zszj.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import lombok.Setter;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;

/**
 * 自定义的 ActivityBehaviorFactory 实现类，目的如下：
 * 1. 自定义 {@link #createUserTaskActivityBehavior(UserTask)}：实现自定义的流程任务的 assignee 负责人的分配
 *
 * @author 芋道源码
 */
@Setter
public class BpmActivityBehaviorFactory extends DefaultActivityBehaviorFactory {

    private BpmTaskCandidateInvoker taskCandidateInvoker;

    @Override
    public UserTaskActivityBehavior createUserTaskActivityBehavior(UserTask userTask) {
        if (isFirstChainUserTask(userTask)) {
            // 首链审批节点由代码生成，只声明 assignee="${approverUserId}"，没有上游「候选人策略」扩展属性；
            // 走上游自定义行为会在 getCandidateStrategy(null) 断言失败（申请提交 500），故使用 Flowable 默认行为
            return super.createUserTaskActivityBehavior(userTask);
        }
        return new BpmUserTaskActivityBehavior(userTask)
                .setTaskCandidateInvoker(taskCandidateInvoker);
    }

    /** 是否首链审批流程的用户任务：按所属流程（父容器）的 key 判定，无父容器按「非首链」处理。 */
    private static boolean isFirstChainUserTask(UserTask userTask) {
        return userTask.getParentContainer() instanceof Process process
                && FlowableFirstChainProcessAdapter.PROCESS_KEY.equals(process.getId());
    }

    @Override
    public ParallelMultiInstanceBehavior createParallelMultiInstanceBehavior(Activity activity,
                                                                             AbstractBpmnActivityBehavior behavior) {
        return new BpmParallelMultiInstanceBehavior(activity, behavior)
                .setTaskCandidateInvoker(taskCandidateInvoker);
    }

    @Override
    public SequentialMultiInstanceBehavior createSequentialMultiInstanceBehavior(Activity activity,
                                                                                 AbstractBpmnActivityBehavior behavior) {
        return new BpmSequentialMultiInstanceBehavior(activity, behavior)
                .setTaskCandidateInvoker(taskCandidateInvoker);
    }

}
