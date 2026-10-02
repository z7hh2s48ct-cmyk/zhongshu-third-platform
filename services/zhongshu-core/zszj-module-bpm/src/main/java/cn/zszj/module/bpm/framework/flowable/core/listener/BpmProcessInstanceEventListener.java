package cn.zszj.module.bpm.framework.flowable.core.listener;

import cn.zszj.module.bpm.firstchain.FlowableFirstChainProcessAdapter;
import cn.zszj.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.zszj.module.bpm.service.task.BpmProcessInstanceService;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 监听 {@link ProcessInstance} 的状态变更，更新其对应的 status 状态
 *
 * @author jason
 */
@Component
public class BpmProcessInstanceEventListener extends AbstractFlowableEngineEventListener {

    public static final Set<FlowableEngineEventType> PROCESS_INSTANCE_EVENTS = ImmutableSet.<FlowableEngineEventType>builder()
            .add(FlowableEngineEventType.PROCESS_CREATED)
            .add(FlowableEngineEventType.PROCESS_COMPLETED)
            .add(FlowableEngineEventType.PROCESS_CANCELLED)
            .build();

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmProcessInstanceService processInstanceService;

    public BpmProcessInstanceEventListener(){
        super(PROCESS_INSTANCE_EVENTS);
    }

    @Override
    protected void processCreated(FlowableEngineEntityEvent event) {
        ProcessInstance processInstance = (ProcessInstance) event.getEntity();
        if (isFirstChain(event, processInstance)) {
            return; // 首链流程不走上游模型管理监听（其扩展表在 PG 基线不存在），见 isFirstChainDefinition
        }
        FlowableUtils.execute(processInstance.getTenantId(),
                () -> processInstanceService.processProcessInstanceCreated(processInstance));
    }

    @Override
    protected void processCompleted(FlowableEngineEntityEvent event) {
        ProcessInstance processInstance = (ProcessInstance) event.getEntity();
        if (isFirstChain(event, processInstance)) {
            return;
        }
        FlowableUtils.execute(processInstance.getTenantId(),
                () -> processInstanceService.processProcessInstanceCompleted(processInstance));
    }

    @Override
    protected void processCancelled(FlowableCancelledEvent event) {
        if (FlowableFirstChainProcessAdapter.isFirstChainDefinition(event.getProcessDefinitionId())) {
            return;
        }
        // 特殊情况：当跳转到 EndEvent 流程实例未结束, 会执行 deleteProcessInstance 方法
        ProcessInstance processInstance = processInstanceService.getProcessInstance(event.getProcessInstanceId());
        if (processInstance != null && !FlowableFirstChainProcessAdapter.isFirstChainInstance(processInstance)) {
            FlowableUtils.execute(processInstance.getTenantId(),
                    () -> processInstanceService.processProcessInstanceCompleted(processInstance));
        }
    }

    /**
     * 是否首链流程：真实引擎的实体事件（createEntityEvent(type, entity)）不携带流程定义编号，
     * 判定取自实体（ProcessInstance 的 key/编号），事件编号作兜底；定义编号常为裸 UUID，不能只看前缀。
     */
    private static boolean isFirstChain(FlowableEngineEntityEvent event, ProcessInstance processInstance) {
        return FlowableFirstChainProcessAdapter.isFirstChainInstance(processInstance)
                || FlowableFirstChainProcessAdapter.isFirstChainDefinition(event.getProcessDefinitionId());
    }

}
