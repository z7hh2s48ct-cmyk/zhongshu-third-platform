package cn.zszj.module.bpm.service.task;

import cn.hutool.core.util.StrUtil;
import cn.zszj.module.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import cn.zszj.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE;

/**
 * 流程实例对象可见性校验器
 *
 * 用于校验指定用户是否有权限查看流程实例的审批详情，避免通过猜测流程实例编号越权访问。
 *
 * @author zszj
 */
@Component
public class BpmInstanceVisibilityChecker {

    /**
     * 「管理流程」菜单权限：拥有该权限的流程管理员，可以查看全部的流程实例
     */
    private static final String PERMISSION_PROCESS_INSTANCE_MANAGER_QUERY = "bpm:process-instance:manager-query";

    @Resource
    private HistoryService historyService;
    @Resource
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;
    @Resource
    private PermissionApi permissionApi;

    /**
     * 校验指定用户是否可见该流程实例的审批详情；不可见时抛出异常
     *
     * @param loginUserId     当前登录用户编号（为空表示内部调用，跳过校验）
     * @param processInstance 流程实例
     */
    public void checkProcessInstanceVisible(Long loginUserId, HistoricProcessInstance processInstance) {
        if (!isProcessInstanceVisible(loginUserId, processInstance)) {
            throw exception(PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE);
        }
    }

    /**
     * 判断指定用户是否可见该流程实例：
     * 1. 流程管理员（「管理流程」菜单权限），可见全部流程实例
     * 2. 流程发起人
     * 3. 流程历史任务参与人（审批人 assignee、拥有者 owner）
     * 4. 流程抄送人
     *
     * @param loginUserId     当前登录用户编号（为空表示内部调用，视为可见）
     * @param processInstance 流程实例
     * @return 是否可见
     */
    public boolean isProcessInstanceVisible(Long loginUserId, HistoricProcessInstance processInstance) {
        // 内部调用（loginUserId 为空），跳过校验
        if (loginUserId == null) {
            return true;
        }
        // 1. 流程管理员（拥有「管理流程」菜单权限），可见。与 manager-page 列表「可以看全部的流程实例」的口径一致，
        //    避免「管理流程」菜单的详情功能对未参与的流程实例失效
        if (permissionApi.hasAnyPermissions(loginUserId, PERMISSION_PROCESS_INSTANCE_MANAGER_QUERY)) {
            return true;
        }
        // 2. 流程发起人，可见
        if (StrUtil.equals(String.valueOf(loginUserId), processInstance.getStartUserId())) {
            return true;
        }
        // 3. 流程历史任务的参与人（审批人、拥有者），可见
        String processInstanceId = processInstance.getId();
        String userIdStr = String.valueOf(loginUserId);
        if (historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId)
                .taskAssignee(userIdStr).count() > 0) {
            return true;
        }
        if (historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId)
                .taskOwner(userIdStr).count() > 0) {
            return true;
        }
        // 4. 流程抄送人，可见
        return processInstanceCopyMapper.selectCountByUserIdAndProcessInstanceId(loginUserId, processInstanceId) > 0;
    }

}
