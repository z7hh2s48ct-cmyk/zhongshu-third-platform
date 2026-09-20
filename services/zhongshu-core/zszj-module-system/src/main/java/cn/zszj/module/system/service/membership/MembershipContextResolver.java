package cn.zszj.module.system.service.membership;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.datapermission.core.util.DataPermissionUtils;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 服务端组织上下文解析器
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-004）核心：组织/身份上下文由服务端依据账号的<b>默认任职</b>解析得出，
 * 写入 token userInfo（签名保护），<b>绝不接受客户端入参指定的组织</b>——从根上杜绝「客户端切换到未授权身份」。
 * 方法签名不含任何 orgId 入参，即为该保证的结构性证明。
 *
 * <p>校验链（fail-closed）：
 * <ol>
 *   <li>无默认任职 → 返回 {@link OrganizationContext#empty()} 降级（orgId=null），登录不因无任职失败，兼容历史无部门账号；</li>
 *   <li>任职状态非在职 → {@code MEMBERSHIP_INVALID_STATUS}；</li>
 *   <li>任职有效期以同一 now 校验起止两端：未到生效期 → {@code MEMBERSHIP_NOT_EFFECTIVE}；已过有效期 → {@code MEMBERSHIP_EXPIRED}（过期上下文拒绝）；</li>
 *   <li>组织不存在或未显式开启 → {@code MEMBERSHIP_ORGANIZATION_DISABLED}。</li>
 * </ol>
 *
 * <p>数据访问在 {@link DataPermissionUtils#executeIgnore} 内执行：关闭数据权限（避免部门范围收窄），保留租户过滤。
 *
 * @author ZS-IAM-002
 */
@Component
public class MembershipContextResolver {

    @Resource
    private MembershipMapper membershipMapper;

    @Resource
    private OrganizationMapper organizationMapper;

    /**
     * 解析账号的服务端组织上下文
     *
     * @param userId 账号编号
     * @return 组织上下文；无默认任职时为降级空上下文
     */
    public OrganizationContext resolve(Long userId) {
        if (userId == null || userId <= 0) {
            return OrganizationContext.empty();
        }
        // 1. 取默认任职（关闭数据权限、保留租户过滤）
        MembershipDO primary = DataPermissionUtils.executeIgnore(() -> membershipMapper.selectPrimaryByUserId(userId));
        if (primary == null) {
            // 无默认任职：降级空上下文（历史无部门账号兼容），不阻断登录
            return OrganizationContext.empty();
        }
        // 2. 状态校验：仅在职任职可进入上下文
        if (!MembershipStatusEnum.isActive(primary.getStatus())) {
            throw exception(MEMBERSHIP_INVALID_STATUS, primary.getStatus());
        }
        // 3. 有效期校验：以同一 now 同时校验起止两端（codex r0 P1：生效期未到的任职不得提前进入上下文）
        LocalDateTime now = LocalDateTime.now();
        // 3a. valid_from 非空且尚未到达 → 任职未生效，拒绝（防止 token 提前携带未生效任职）
        if (primary.getValidFrom() != null && primary.getValidFrom().isAfter(now)) {
            throw exception(MEMBERSHIP_NOT_EFFECTIVE);
        }
        // 3b. valid_to 非空且已过 → 过期上下文拒绝
        if (primary.getValidTo() != null && primary.getValidTo().isBefore(now)) {
            throw exception(MEMBERSHIP_EXPIRED);
        }
        // 4. 组织校验：组织必须存在且显式处于开启态（codex r0 P2 fail-closed：require ENABLE 而非仅排除 DISABLE，
        //    否则 status=2 等非法值会被 isDisable 漏放，与 validateOrganizationEnabled 口径不一致）
        OrganizationDO organization = DataPermissionUtils.executeIgnore(
                () -> organizationMapper.selectById(primary.getOrganizationId()));
        if (organization == null || !CommonStatusEnum.isEnable(organization.getStatus())) {
            throw exception(MEMBERSHIP_ORGANIZATION_DISABLED, primary.getOrganizationId());
        }
        return new OrganizationContext(organization.getId(), organization.getType(), primary.getId());
    }

}
