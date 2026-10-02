package cn.zszj.module.firstchain.service;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 「用户是否属于组织」的生产实现（ZS-FC-002，接入合同 §1.6 服务层对象级资格二次校验）。
 *
 * <p>判定口径：目标用户存在、启用（停用即失权——PILOT-REQ-004/D-07 M5，IAM-004 已验语义），
 * 且在目标组织持有<b>有效任职</b>（D-09 模型：{@code system_membership} 状态 ACTIVE 且在有效期内）。
 * <b>不看 {@code deptId}</b>：开通/建员工链路创建账号时不设部门，归属由任职承载——首版按 deptId 判定，
 * 真实 server 上负责人把线索分配给本组织合法员工恒被拒绝（首链 E2E 暴露）。
 * 每次调用实时查询，不缓存授权结论（撤权/停用即时生效）。
 *
 * @author ZS-FC-002
 */
@AllArgsConstructor
public class FirstchainUserOrgChecker implements FirstchainLeadService.UserOrgChecker {

    private final AdminUserApi adminUserApi;

    private final MembershipService membershipService;

    @Override
    public boolean isUserInOrg(Long tenantId, Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        List<MembershipDO> memberships = membershipService.getMembershipListByUserId(userId);
        return memberships != null && memberships.stream().anyMatch(m ->
                orgId.equals(m.getOrganizationId())
                        && MembershipStatusEnum.ACTIVE.getStatus().equals(m.getStatus())
                        && (m.getValidFrom() == null || !m.getValidFrom().isAfter(now))
                        && (m.getValidTo() == null || m.getValidTo().isAfter(now)));
    }

}
