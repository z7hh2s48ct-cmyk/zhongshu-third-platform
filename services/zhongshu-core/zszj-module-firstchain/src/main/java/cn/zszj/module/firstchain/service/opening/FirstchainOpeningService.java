package cn.zszj.module.firstchain.service.opening;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * 首链审批开通服务（ZS-FC-001，PILOT-REQ-002/003；D-07 M4 同事务幂等开通 + M2 审批权限串落点）。
 *
 * <p>RED 骨架：方法体 {@code UnsupportedOperationException}，GREEN 提交实现（feat 提交转绿）。
 *
 * <p>GREEN 合同（先读 {@code FirstChainApplicationService#onApprovalCompleted} 实现后落定的行为结论）：
 * <ul>
 *     <li>幂等门行为（以 bpm 域代码为准）：{@code onApprovalCompleted} 内部「绑定条件迁移 BOUND→COMPLETED」
 *     为幂等门——重复同结果回调被<b>静默吸收</b>（正常返回，不抛错不重复留痕）；异结果/撤回竞争抛
 *     {@code FIRST_CHAIN_PROCESS_BINDING_CONFLICT}（先到者生效）；门赢但领域非 SUBMITTED=旧流程晚到 →
 *     弃单留痕正常返回。方法返回 void、不区分「本次生效/已吸收」——故本服务以「申请状态预分类 +
 *     app_key 查已开通组织兜底幂等」判定是否执行开通（M4-A：重复处理返回既有结果，幂等键=申请编号）；</li>
 *     <li>同事务 fail-closed：外层 {@code @Transactional(REQUIRED)}，bpm 幂等写回（内部 TransactionTemplate
 *     REQUIRED）与开通动作（组织/负责人/角色/任职）同事务，任一失败整体回滚——审批不生效、组织不残留；
 *     审计 SUCCESS 随事务（接入合同 §1.7），DENIED 走 JdbcAuditPort 独立事务（业务回滚不丢留痕）；</li>
 *     <li>REJECTED 路径：拒绝必填意见（缺失即拒）；不建任何主体（无组织/账号/角色/任职）；</li>
 *     <li>开通内容（M4-A）：FRANCHISEE 组织（编码=申请编号）+ 负责人账号（M5-A 用户名 trim+小写）+
 *     负责人任职（Membership 携默认授权）+ 租户级默认角色（create-or-reuse）+ 菜单绑定（本 wave 空集登记，
 *     菜单增量迁移归后续 wave）。</li>
 * </ul>
 *
 * @author ZS-FC-001
 */
@Service
public class FirstchainOpeningService {

    /** 默认角色编码：加盟商负责人（租户级授权模板；组织归属经任职承载，ZS-IAM-002 模型） */
    public static final String ROLE_CODE_LEADER = "firstchain:franchisee:leader";

    /** 默认角色编码：加盟商员工（同上） */
    public static final String ROLE_CODE_MEMBER = "firstchain:franchisee:member";

    @Resource
    private FirstChainApplicationService firstChainApplicationService;

    @Resource
    private FirstChainProcessBindingService processBindingService;

    @Resource
    private AuditPort auditPort;

    @Resource
    private OrganizationService organizationService;

    @Resource
    private MembershipService membershipService;

    @Resource
    private RoleService roleService;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private DataSource dataSource;

    /**
     * 审批通过并幂等开通（PILOT-REQ-002/003；M4-A）。
     *
     * @return 已开通（或既有）的加盟商组织编号——重复处理返回既有结果（M4-A 幂等键=申请编号）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long approveAndOpen(ApproveCmd cmd) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 审批拒绝（PILOT-REQ-002；M4-A：REJECTED 不建任何主体，拒绝意见必填）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(ApproveCmd cmd) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    /**
     * 审批命令（管理端审批入口：appKey 为审批可见业务键；通过时 reason 为可空意见，拒绝时必填；
     * operatorUserId 供 M2 对象级资格校验〔审批人=任意 PLATFORM 有效任职〕与任职 operatorId）。
     */
    @lombok.Builder
    public record ApproveCmd(String appKey, String reason, String actorId, Long operatorUserId) {
    }

}
