package cn.zszj.module.bpm.firstchain;

/**
 * 首链审批流程端口（ZS-BPM-003，D-07 M10=B Flowable 单节点审批流的引擎隔离面）。
 *
 * <p>领域服务只依赖本端口；{@code FlowableFirstChainProcessAdapter}（PG 套件联验）经 RuntimeService/
 * TaskService 实现真实引擎交互。端口方法均要求调用方事务语义由适配器保证（引擎与应用同源数据源）。
 *
 * @author ZS-BPM-003
 */
public interface FirstChainProcessPort {

    /**
     * 发起加盟商申请审批流程（单节点：审批人任务）。
     *
     * @param tenantId       申请归属租户（流程变量携带，保证回调可回溯租户）
     * @param appKey         申请业务键（businessKey）
     * @param approverUserId 审批人用户编号
     * @return 流程实例编号
     */
    String startApprovalProcess(Long tenantId, String appKey, Long approverUserId);

    /**
     * 撤回审批流程实例（流程侧取消；领域状态由 {@code FirstChainApplicationService#withdrawApproval} 处理）。
     *
     * @param processInstanceId 流程实例编号
     */
    void withdrawProcess(String processInstanceId);

}
