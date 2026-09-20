package cn.zszj.module.system.service.membership;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipHistoryMapper;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.membership.MembershipActionEnum;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.organization.OrganizationService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static cn.zszj.module.system.enums.LogRecordConstants.*;

/**
 * 任职 Service 实现类
 *
 * <p>ZS-IAM-002（D-09 最小模型 / FND-IAM-001、003）：一个账号可含多个任职，每账号至多一条默认任职
 * （DB 部分唯一索引 uk_system_membership_primary 兜底）；同账号同组织在职去重（uk_system_membership_user_org）。
 * 每次创建/转岗/状态流转均写 {@code system_membership_history} 流水，历史归属只增不改。
 *
 * <p>ZS-IAM-004（D-09 FND-IAM-003/005/006、FND-AUTH-007）：生命周期入口（入职/转岗/状态流转）携 {@link LogRecord}
 * 操作审计；停用/离职/过期致默认任职上下文丧失时，复用 LOGIN-003 撤销链失效全部登录会话（及时失权）；
 * 复职不复活旧越界授权（只能经重新登录 + 上下文重解析）。
 *
 * @author ZS-IAM-002
 */
@Service
@Validated
@Slf4j
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

    /**
     * ZS-IAM-004：停用/离职/过期致默认任职上下文丧失时，复用 LOGIN-003 撤销链失效全部会话。
     * {@code @Lazy} 懒加载避免循环依赖（镜像 {@code AdminUserServiceImpl} 对 {@code OAuth2TokenService} 的注入）；
     * <b>不</b>另造锁、<b>不</b>另造撤销机制。
     */
    @Resource
    @Lazy
    private OAuth2TokenService oauth2TokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_MEMBERSHIP_TYPE, subType = SYSTEM_MEMBERSHIP_CREATE_SUB_TYPE, bizNo = "{{#membership.userId}}",
            success = SYSTEM_MEMBERSHIP_CREATE_SUCCESS)
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
    @LogRecord(type = SYSTEM_MEMBERSHIP_TYPE, subType = SYSTEM_MEMBERSHIP_TRANSFER_SUB_TYPE, bizNo = "{{#membershipId}}",
            success = SYSTEM_MEMBERSHIP_TRANSFER_SUCCESS)
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
    @LogRecord(type = SYSTEM_MEMBERSHIP_TYPE, subType = SYSTEM_MEMBERSHIP_CHANGE_STATUS_SUB_TYPE, bizNo = "{{#membershipId}}",
            success = SYSTEM_MEMBERSHIP_CHANGE_STATUS_SUCCESS)
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
        // ZS-IAM-004（D1 方案 B 精确失权）：失活类流转（停用/离职/过期）致默认任职上下文丧失时，
        // 复用 LOGIN-003 撤销链失效全部会话；在 @Transactional 内调用——撤销失败则回滚状态变更，
        // 宁可显式失败，也不留「已停用/离职但仍在线」窗口。复职默认任职时亦撤销（强制重新登录，杜绝旧令牌静默复活，codex r0 P2）。
        // 残余：直接登录签发不取用户行锁的幻影写窗口属登录层既有跨切面竞态，越界授权已被 checkAccessToken 每请求精确拦截，根因串行化修复归后续 LOGIN 卡（详见 shouldRevokeSessions javadoc）。
        if (shouldRevokeSessions(membership.getUserId(), toStatus, Objects.equals(membership.getIsPrimary(), 1))) {
            revokeSessions(membership.getUserId(), resolveAction(toStatus).getName());
        }
    }

    /**
     * ZS-IAM-004（D1 方案 B）：判定状态流转是否应失效该账号全部会话。
     *
     * <p>{@link MembershipContextResolver} 仅依据<b>默认任职</b>（primary）解析并签名 token 组织上下文，且
     * {@code checkAccessToken} <b>每请求</b> fail-closed 复验，故：
     * <ol>
     *   <li><b>复职（→ACTIVE）</b>：仅当复职的是默认任职时撤销，强制重新登录。codex r0 P2——并发登录签发的
     *       令牌可能逃逸停用时的撤销；停用期间每请求 fail-closed 复验会拒绝它，但复职后同一令牌会被重新接受
     *       而无需再次登录。<b>codex r1 复核（已验证）</b>：复职撤销可关闭绝大多数窗口，但<b>直接登录签发路径</b>
     *       （{@code AdminAuthServiceImpl.createTokenAfterLoginSuccess → OAuth2TokenService.createAccessToken}，无
     *       {@code @Transactional}）<b>不取</b> {@code removeAccessToken(userId)} 所持的 {@code selectByIdForUpdate(userId)}
     *       用户行锁，故一条「签发事务在停用/复职两次撤销扫描之后才提交」的并发令牌理论上仍可逃逸（幻影写）。
     *       此为<b>登录签发层既有跨切面竞态</b>——对 ZS-LOGIN-003 管理员禁用撤销同样存在、且早于 IAM-004，
     *       其根因「签发与用户级撤销串行化」修复归属后续 LOGIN 层跟进卡（见 IAM-004 开发记录 / 变更台账），
     *       本卡<b>不越界改动登录关键路径</b>的事务与锁序语义。残余风险已收窄为「并发签发 + suspend→resume 回到
     *       <b>完全相同</b>上下文」的低危会话卫生问题：任何<b>越界授权</b>（组织 / 角色 / 主职变更致上下文不一致）
     *       都被 {@code checkAccessToken} 每请求 {@code organizationContextMatches} 精确拦截并 401，不会复活旧越界授权；</li>
     *   <li><b>失活类（停用/离职/过期）条件①</b>：被变更任职是默认任职——其失活即上下文丧失；</li>
     *   <li><b>条件②</b>：变更后账号已无任何 ACTIVE 任职——即便被变更行非 primary，上下文亦彻底丧失。</li>
     * </ol>
     * 停用/复职一条不影响上下文的次级任职（primary 仍在职）不强制全端下线，避免过度中断。
     *
     * @param userId            账号编号
     * @param toStatus          目标状态
     * @param affectedIsPrimary 被变更任职是否为默认任职
     */
    private boolean shouldRevokeSessions(Long userId, Integer toStatus, boolean affectedIsPrimary) {
        // 复职（→ACTIVE）：仅默认任职复职时撤销，强制重新登录以全新解析上下文（codex r0 P2）
        if (Objects.equals(toStatus, MembershipStatusEnum.ACTIVE.getStatus())) {
            return affectedIsPrimary;
        }
        // 非失活类流转（如 UPDATE）不触发失权
        if (!isDeactivation(toStatus)) {
            return false;
        }
        // 失活类条件①：默认任职失活 → 上下文丧失
        if (affectedIsPrimary) {
            return true;
        }
        // 条件②：变更后账号已无任何 ACTIVE 任职（当前行已在本事务内被改为失活态，重查即反映）
        boolean anyActiveLeft = membershipMapper.selectListByUserId(userId).stream()
                .anyMatch(m -> MembershipStatusEnum.isActive(m.getStatus()));
        return !anyActiveLeft;
    }

    /**
     * 是否失活类状态（停用/离职/过期）——与 {@link #resolveAction} 的非 RESUME 分支同口径。
     */
    private boolean isDeactivation(Integer toStatus) {
        return Objects.equals(toStatus, MembershipStatusEnum.SUSPENDED.getStatus())
                || Objects.equals(toStatus, MembershipStatusEnum.TERMINATED.getStatus())
                || Objects.equals(toStatus, MembershipStatusEnum.EXPIRED.getStatus());
    }

    /**
     * ZS-IAM-004：失效账号全部登录会话，完全复用 {@link OAuth2TokenService#removeAccessToken(Long, Integer)}
     * （ZS-LOGIN-002 行锁 + 固定锁序 + 会话代际键，ZS-LOGIN-003 孤立刷新凭据全集 + 已缓存转换凭据清理）。
     * 调用方<b>必须</b>处于 {@code @Transactional} 中：撤销失败则回滚状态变更。
     */
    private void revokeSessions(Long userId, String reason) {
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        log.info("[revokeSessions][ZS-IAM-004 账号({}) 因任职生命周期变更({}) 全部登录会话已失效]", userId, reason);
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
