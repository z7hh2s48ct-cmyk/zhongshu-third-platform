package cn.zszj.module.system.service.membership;

import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipHistoryMapper;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.membership.MembershipActionEnum;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.service.organization.OrganizationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 任职 Service 实现类
 *
 * <p>ZS-IAM-002（D-09 最小模型 / FND-IAM-001、003）：一个账号可含多个任职，每账号至多一条默认任职
 * （DB 部分唯一索引 uk_system_membership_primary 兜底）；同账号同组织在职去重（uk_system_membership_user_org）。
 * 每次创建/转岗/状态流转均写 {@code system_membership_history} 流水，历史归属只增不改。
 *
 * @author ZS-IAM-002
 */
@Service
@Validated
public class MembershipServiceImpl implements MembershipService {

    @Resource
    private MembershipMapper membershipMapper;

    @Resource
    private MembershipHistoryMapper membershipHistoryMapper;

    @Resource
    private OrganizationService organizationService;

    /**
     * ZS-IAM-002（codex r0 P2）：创建任职前校验账号存在与租户归属。
     * 只读依赖，不改 {@code AdminUserServiceImpl}（与 ZS-DB-010 改动区隔离）；
     * {@code selectById} 受租户插件限定于当前租户，跨租户/不存在账号自然返回 null → fail-closed。
     */
    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMembership(MembershipDO membership, Long operatorId) {
        // 校验账号存在且归属当前租户（codex r0 P2：租户过滤只限定任职查询，不校验被引用账号；
        // 跨租户/不存在账号若放行，会因 primary 唯一性按 userId 全局生效而阻断他租户合法主职）
        validateUserExists(membership.getUserId());
        // 校验组织存在且开启，并锁组织行（codex r1 P2：与组织删除串行化，杜绝孤引用）
        organizationService.validateOrganizationEnabledForUpdate(membership.getOrganizationId());
        // 校验同账号同组织不重复「在任」（与 uk_system_membership_user_org 同口径：仅 ACTIVE/SUSPENDED 占位，
        // 历史 EXPIRED/TERMINATED 行不阻断再入职，codex r0 P2）
        MembershipDO exists = membershipMapper.selectInServiceByUserIdAndOrganizationId(
                membership.getUserId(), membership.getOrganizationId());
        if (exists != null) {
            throw exception(MEMBERSHIP_ALREADY_EXISTS);
        }
        // 默认值：状态 ACTIVE、生效时间 now
        if (membership.getStatus() == null) {
            membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        }
        if (membership.getValidFrom() == null) {
            membership.setValidFrom(LocalDateTime.now());
        }
        // 默认任职归属（codex r0 P2 显式主职替换）：显式指定 is_primary=1，或当前无「在任」默认任职时自动承接。
        // 承接前先原子降级既有默认任职行（含已离任 primary，保留历史行不物理删除），释放 uk_system_membership_primary 占位；
        // 离任本身不自动切换身份——须由本显式替换承接。
        MembershipDO currentPrimary = membershipMapper.selectPrimaryByUserId(membership.getUserId());
        boolean hasInServicePrimary = currentPrimary != null && isInService(currentPrimary.getStatus());
        boolean wantPrimary = Objects.equals(membership.getIsPrimary(), 1) || !hasInServicePrimary;
        if (wantPrimary) {
            membershipMapper.clearPrimaryByUserId(membership.getUserId());
            membership.setIsPrimary(1);
        } else {
            membership.setIsPrimary(0);
        }
        membershipMapper.insert(membership);
        // 写历史流水：入职
        writeHistory(membership, MembershipActionEnum.CREATE, null, membership.getOrganizationId(),
                null, membership.getStatus(), operatorId, null);
        return membership.getId();
    }

    /**
     * 校验账号存在且归属当前租户（codex r0 P2）。
     * {@code selectById} 受租户插件限定，跨租户或不存在账号均返回 null → 拒绝创建（fail-closed）。
     */
    private void validateUserExists(Long userId) {
        if (userId == null || adminUserMapper.selectById(userId) == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    /**
     * 是否「在任」（ACTIVE/SUSPENDED）——与 uk_system_membership_user_org 部分唯一索引谓词同口径。
     */
    private boolean isInService(Integer status) {
        return MembershipStatusEnum.ACTIVE.getStatus().equals(status)
                || MembershipStatusEnum.SUSPENDED.getStatus().equals(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferMembership(Long membershipId, Long toOrganizationId, Long operatorId, String reason) {
        // 先锁目标组织行再锁任职行：与 createMembership 一致的「组织→任职」全局锁序，防交叉死锁（codex r1 P2）。
        // 同时锁组织行与组织删除串行化，杜绝孤引用。
        organizationService.validateOrganizationEnabledForUpdate(toOrganizationId);
        MembershipDO membership = validateMembershipExists(membershipId);
        Long fromOrganizationId = membership.getOrganizationId();
        // 转岗后目标组织不得与该账号既有「在任」任职重复（与部分唯一索引同口径，codex r0 P2）
        if (!Objects.equals(fromOrganizationId, toOrganizationId)) {
            MembershipDO exists = membershipMapper.selectInServiceByUserIdAndOrganizationId(membership.getUserId(), toOrganizationId);
            if (exists != null) {
                throw exception(MEMBERSHIP_ALREADY_EXISTS);
            }
        }
        MembershipDO update = new MembershipDO();
        update.setId(membershipId);
        update.setOrganizationId(toOrganizationId);
        membershipMapper.updateById(update);
        // 写历史流水：转岗
        writeHistory(membership, MembershipActionEnum.TRANSFER, fromOrganizationId, toOrganizationId,
                membership.getStatus(), membership.getStatus(), operatorId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long membershipId, Integer toStatus, Long operatorId, String reason) {
        MembershipDO membership = validateMembershipExists(membershipId);
        Integer fromStatus = membership.getStatus();
        MembershipDO update = new MembershipDO();
        update.setId(membershipId);
        update.setStatus(toStatus);
        // 过期/离职以状态承载，保留行不物理删除，承载历史归属
        membershipMapper.updateById(update);
        // 写历史流水：动作按目标状态映射
        writeHistory(membership, resolveAction(toStatus), membership.getOrganizationId(), membership.getOrganizationId(),
                fromStatus, toStatus, operatorId, reason);
    }

    private MembershipActionEnum resolveAction(Integer toStatus) {
        if (Objects.equals(toStatus, MembershipStatusEnum.SUSPENDED.getStatus())) {
            return MembershipActionEnum.SUSPEND;
        }
        if (Objects.equals(toStatus, MembershipStatusEnum.ACTIVE.getStatus())) {
            return MembershipActionEnum.RESUME;
        }
        if (Objects.equals(toStatus, MembershipStatusEnum.TERMINATED.getStatus())) {
            return MembershipActionEnum.TERMINATE;
        }
        if (Objects.equals(toStatus, MembershipStatusEnum.EXPIRED.getStatus())) {
            return MembershipActionEnum.EXPIRE;
        }
        return MembershipActionEnum.UPDATE;
    }

    private void writeHistory(MembershipDO membership, MembershipActionEnum action,
                              Long fromOrganizationId, Long toOrganizationId,
                              Integer fromStatus, Integer toStatus, Long operatorId, String reason) {
        MembershipHistoryDO history = new MembershipHistoryDO();
        history.setMembershipId(membership.getId());
        history.setUserId(membership.getUserId());
        history.setAction(action.getAction());
        history.setFromOrganizationId(fromOrganizationId);
        history.setToOrganizationId(toOrganizationId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId);
        history.setReason(reason);
        history.setTenantId(membership.getTenantId());
        membershipHistoryMapper.insert(history);
    }

    /**
     * 加行锁校验任职存在并返回（SELECT ... FOR UPDATE）。
     *
     * <p>ZS-IAM-002（codex r0 P2）：状态流转/转岗先锁行再读审计态，串行化并发变更。
     * {@code @Transactional} 仅保证「更新+history」原子，不串行化初始读取；无锁时两个并发事务
     * 可读到同一原始行，导致 suspend/terminate 都记 ACTIVE 为 fromStatus。锁行使后到事务阻塞至前者提交，
     * 再读到的是最新态，fromStatus/fromOrganization 正确。需在 {@code @Transactional} 内调用。
     */
    private MembershipDO validateMembershipExists(Long id) {
        MembershipDO membership = membershipMapper.selectByIdForUpdate(id);
        if (membership == null) {
            throw exception(MEMBERSHIP_NOT_EXISTS);
        }
        return membership;
    }

    @Override
    public MembershipDO getMembership(Long id) {
        return membershipMapper.selectById(id);
    }

    @Override
    public MembershipDO getPrimaryMembership(Long userId) {
        return membershipMapper.selectPrimaryByUserId(userId);
    }

    @Override
    public List<MembershipDO> getMembershipListByUserId(Long userId) {
        return membershipMapper.selectListByUserId(userId);
    }

    @Override
    public List<MembershipHistoryDO> getMembershipHistoryList(Long membershipId) {
        return membershipHistoryMapper.selectListByMembershipId(membershipId);
    }

}
