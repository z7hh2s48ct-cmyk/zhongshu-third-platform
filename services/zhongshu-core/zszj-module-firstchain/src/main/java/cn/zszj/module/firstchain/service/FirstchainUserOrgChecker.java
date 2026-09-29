package cn.zszj.module.firstchain.service;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import lombok.AllArgsConstructor;

/**
 * 「用户是否属于组织」的生产实现（ZS-FC-002，接入合同 §1.6 服务层对象级资格二次校验）。
 *
 * <p>判定口径：目标用户存在、启用（停用即失权——PILOT-REQ-004/D-07 M5，IAM-004 已验语义）、
 * 且其部门即目标组织（FRANCHISEE 组织由 IAM-002 模型承载，员工 deptId 即归属）。
 * 每次调用实时查询，不缓存授权结论（撤权/停用即时生效）。
 *
 * @author ZS-FC-002
 */
@AllArgsConstructor
public class FirstchainUserOrgChecker implements FirstchainLeadService.UserOrgChecker {

    private final AdminUserApi adminUserApi;

    @Override
    public boolean isUserInOrg(Long tenantId, Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            return false;
        }
        return CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())
                && orgId.equals(user.getDeptId());
    }

}
