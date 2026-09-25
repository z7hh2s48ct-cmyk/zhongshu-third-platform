package cn.zszj.module.bpm.service.task;

import cn.zszj.module.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

/**
 * 流程实例对象可见性校验器
 *
 * 用于校验指定用户是否有权限查看流程实例的审批详情，避免通过猜测流程实例编号越权访问。
 *
 * @author zszj
 */
@Component
public class BpmInstanceVisibilityChecker {

    @Resource
    private HistoryService historyService;
    @Resource
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;

    /**
     * 校验指定用户是否可见该流程实例的审批详情；不可见时抛出异常
     *
     * @param loginUserId     当前登录用户编号（为空表示内部调用，跳过校验）
     * @param processInstance 流程实例
     */
    public void checkProcessInstanceVisible(Long loginUserId, HistoricProcessInstance processInstance) {
        // TODO BPM-002（RED）：可见性校验暂未启用
    }

    /**
     * 判断指定用户是否可见该流程实例：
     * 1. 流程发起人
     * 2. 流程历史任务参与人（审批人 assignee、拥有者 owner）
     * 3. 流程抄送人
     *
     * @param loginUserId     当前登录用户编号（为空表示内部调用，视为可见）
     * @param processInstance 流程实例
     * @return 是否可见
     */
    public boolean isProcessInstanceVisible(Long loginUserId, HistoricProcessInstance processInstance) {
        // TODO BPM-002（RED）：可见性校验暂未启用
        return true;
    }

}
