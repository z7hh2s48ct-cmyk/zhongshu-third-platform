package cn.zszj.module.system.service.membership;

import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;

import java.util.List;

/**
 * 任职 Service 接口
 *
 * <p>ZS-IAM-002（D-09 最小模型 / FND-IAM-001、003）：账号与组织的任职关系创建、转岗、状态流转，
 * 每次变更写 {@code system_membership_history} 流水留痕，承载历史归属。
 *
 * @author ZS-IAM-002
 */
public interface MembershipService {

    /**
     * 创建任职；账号首条任职自动置为默认任职（is_primary=1）
     *
     * @param membership 任职
     * @param operatorId 操作者用户编号（服务端上下文取）
     * @return 任职编号
     */
    Long createMembership(MembershipDO membership, Long operatorId);

    /**
     * 转岗：变更任职所属组织，记录 from/to 组织流水
     *
     * @param membershipId     任职编号
     * @param toOrganizationId 目标组织编号
     * @param operatorId       操作者用户编号
     * @param reason           变更原因
     */
    void transferMembership(Long membershipId, Long toOrganizationId, Long operatorId, String reason);

    /**
     * 变更任职状态（停用/复职/离职/过期），记录 from/to 状态流水
     *
     * @param membershipId 任职编号
     * @param toStatus     目标状态（{@code MembershipStatusEnum}）
     * @param operatorId   操作者用户编号
     * @param reason       变更原因
     */
    void changeStatus(Long membershipId, Integer toStatus, Long operatorId, String reason);

    /**
     * 获得任职
     *
     * @param id 任职编号
     * @return 任职
     */
    MembershipDO getMembership(Long id);

    /**
     * 获得账号的默认任职
     *
     * @param userId 账号编号
     * @return 默认任职，无则 null
     */
    MembershipDO getPrimaryMembership(Long userId);

    /**
     * 获得账号的全部任职
     *
     * @param userId 账号编号
     * @return 任职列表
     */
    List<MembershipDO> getMembershipListByUserId(Long userId);

    /**
     * 获得任职的历史流水
     *
     * @param membershipId 任职编号
     * @return 历史流水列表
     */
    List<MembershipHistoryDO> getMembershipHistoryList(Long membershipId);

}
