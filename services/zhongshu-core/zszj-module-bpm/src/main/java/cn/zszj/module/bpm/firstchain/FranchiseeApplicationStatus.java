package cn.zszj.module.bpm.firstchain;

/**
 * 加盟商申请状态（ZS-BPM-003，D-07 M3 已批准：DRAFT→SUBMITTED→APPROVED/REJECTED）。
 *
 * <p>SUBMITTED 字段锁定；REJECTED 必填意见、可另行新建申请（旧件留痕）；APPROVED/REJECTED 终态。
 * 撤回审批流不改领域状态（仍 SUBMITTED，仅解绑流程实例，可重新发起审批）。
 *
 * @author ZS-BPM-003
 */
public enum FranchiseeApplicationStatus {

    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED;

}
