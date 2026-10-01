package cn.zszj.module.firstchain.service.opening;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.bpm.firstchain.FranchiseeApplicationStatus;
import cn.zszj.module.firstchain.framework.FirstchainMenus;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_NOT_BOUND;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_STATUS_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_OPENING_ORG_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_REJECT_REASON_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_TENANT_REQUIRED;

/**
 * 首链审批开通服务（ZS-FC-001，PILOT-REQ-002/003；D-07 M4 同事务幂等开通 + M2 审批资格二次校验 + M5-A 负责人账号）。
 *
 * <p><b>幂等门行为结论（先读 bpm 域 {@code FirstChainApplicationService#onApprovalCompleted} 实现确认，
 * 以其代码为准）</b>：{@code onApprovalCompleted} 内部「绑定条件迁移 BOUND→COMPLETED」即幂等命令门——
 * <ul>
 *     <li>重复同结果回调：<b>静默吸收</b>（正常返回，不抛错、不重复留痕）；</li>
 *     <li>异结果/审批撤回竞争：抛 {@code FIRST_CHAIN_PROCESS_BINDING_CONFLICT}（先到者生效）；</li>
 *     <li>门赢但领域非 SUBMITTED（旧流程晚到）：弃单留痕后正常返回。</li>
 * </ul>
 * 该方法返回 void、不向调用方区分「本次生效/已吸收」，且拒绝意见必填校验在其内部（本服务先做同口径快检，
 * fail-fast 于任何写动作之前）。因此本服务以「申请状态预分类 + {@code app_key} 查已开通组织兜底幂等」
 * 判定是否执行开通（M4-A：重复处理返回既有结果，<b>幂等键=申请编号</b>）。
 *
 * <p><b>同事务 fail-closed</b>：外层 {@code @Transactional(REQUIRED)}——bpm 幂等写回（其内部
 * TransactionTemplate REQUIRED 加入本事务）与开通动作（角色/账号/组织/任职）同生共死，任一失败整体回滚：
 * 审批不生效（领域回 SUBMITTED、绑定回 BOUND 可重试）、组织不残留。审计循接入合同 §1.7/§4：
 * 开通 SUCCESS 随本事务（回滚则不留）；拒绝留痕由 bpm 域落（reject_reason + REJECT 审计）；
 * 冲突/资格 DENIED 走 {@code JdbcAuditPort} 独立事务（业务回滚不丢失拒绝留痕）。
 *
 * <p><b>开通内容（M4-A + M5-A）</b>：FRANCHISEE 组织（{@link OrganizationTypeEnum#FRANCHISEE}，
 * 编码=申请编号，负责人 leaderUserId 回填）+ 负责人账号（用户名 trim+小写派生自申请编号，初始密码随机
 * 生成——密码明文禁落审计，经开通结果<b>一次性下发</b>给审批操作人，强制首改按 M5 后置登记）+
 * 负责人任职（Membership 搥一套默认授权；员工只进目标组织）+ 租户级默认角色（create-or-reuse，编码
 * {@link #ROLE_CODE_LEADER}/{@link #ROLE_CODE_MEMBER}，经 {@link FirstchainDefaultRoleRegistry}）+
 * 默认角色绑菜单（{@code FirstchainMenus} 编号合同菜单面，迁移 V20261001.001 种子——接入合同 §1.1 第⑤步）。
 *
 * <p><b>REJECTED 路径</b>：意见必填（缺失即拒）；不建任何主体（无组织/账号/角色/任职）；
 * reject_reason 与 REJECT 审计由 bpm 域落。
 *
 * <p><b>跨模块边界</b>：只调 system 模块既有 Service/API（OrganizationService/MembershipService/
 * RoleService/AdminUserService/PermissionService），<b>不改 system 模块</b>；system 侧幂等兜底查询
 * （组织编码/角色编码）走 JdbcTemplate 显式租户 + deleted=FALSE（循 bpm firstchain 域层同款手法）。
 *
 * @author ZS-FC-001
 */
@Slf4j
@Service
public class FirstchainOpeningService {

    /** 默认角色编码：加盟商负责人（租户级授权模板；组织归属经任职承载——ZS-IAM-002 模型，角色不随组织增殖） */
    public static final String ROLE_CODE_LEADER = "firstchain:franchisee:leader";

    /** 默认角色编码：加盟商员工（同上） */
    public static final String ROLE_CODE_MEMBER = "firstchain:franchisee:member";

    /** 默认角色名：加盟商负责人 */
    private static final String ROLE_NAME_LEADER = "首链加盟商负责人";

    /** 默认角色名：加盟商员工 */
    private static final String ROLE_NAME_MEMBER = "首链加盟商员工";

    /** 审计 bizType：与 bpm 域 {@code FirstChainObjectType.APPLICATION} 对齐（全链审计 bizId=申请编号可回查） */
    private static final String BIZ_TYPE_APPLICATION = FirstChainObjectType.APPLICATION.getKey();

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
    private AdminUserService adminUserService;

    @Resource
    private FirstchainDefaultRoleRegistry defaultRoleRegistry;

    @Resource
    private FirstchainNotifyWiringService notifyWiringService;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    void initJdbcTemplate() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 审批通过并幂等开通（PILOT-REQ-002/003；M4-A）。
     *
     * <p>守卫次序：①写侧租户缺失即拒（归属合同）；②M2 对象级资格二次校验（操作人须有 PLATFORM 有效
     * 任职，与 @PreAuthorize 动作权限两层不可互相替代，接入合同 §1.6）；③申请行定位（跨租户 0 行即
     * NOT_EXISTS）；④状态预分类（重复处理兜底幂等 / 终态冲突 fail-closed）；⑤审批回调（幂等门在 bpm 域
     * 内聚）；⑥幂等开通（app_key 查既有组织兜底，缺失才建主体）。
     *
     * <p>初始密码下发（M5-A wave，D-07 M5「初始密码下发、强制首改后置登记」）：仅<b>本次实际创建负责人
     * 账号</b>时经返回值一次性下发给审批操作人（平台运营转交加盟商负责人；明文禁落审计/日志/持久层）；
     * 重复处理返回既有组织时 {@code initialPassword=null}（密码仅在下发生成时可见，重发需走重置通道）。
     * 强制首改仍为后置登记（随 LOGIN 域首改合同排期）。
     *
     * @return 开通结果：加盟商组织编号 + 本次新建负责人的一次性初始密码（重复处理为 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public OpeningResult approveAndOpen(ApproveCmd cmd) {
        Long tenantId = requireTenantId();
        // ① M2 对象级资格：审批人=任意 PLATFORM 有效任职（@PreAuthorize 之外的服务层二次校验）
        assertApproverQualification(cmd.operatorUserId(), "APPROVE");
        // ② 申请行定位（按 appKey 业务键 + 显式租户，跨租户 0 行即拒）
        Map<String, Object> application = requireApplication(tenantId, cmd.appKey());
        Long applicationId = ((Number) application.get("id")).longValue();
        String appKey = (String) application.get("app_key");
        String status = (String) application.get("status");
        // ③ 重复处理兜底幂等（M4-A）：已 APPROVED 即查既有组织（幂等键=申请编号=组织编码）
        OrganizationDO opened = findOpenedOrganization(tenantId, appKey);
        String processInstanceId = null;
        if (FranchiseeApplicationStatus.APPROVED.name().equals(status)) {
            if (opened != null) {
                // 重复审批（重复回调/重复处理）：返回既有结果，不产生第二个组织/负责人/一套授权；
                // 不重发密码（初始密码仅下发生成时一次，重发走重置通道）
                log.info("[approveAndOpen][重复开通请求被吸收：appKey={}，返回既有组织({})]", appKey, opened.getId());
                return new OpeningResult(opened.getId(), null);
            }
            // APPROVED 而组织缺失（幂等门已终结的修复窗口，如未来 M4-B 异步解耦的投递间隙）：
            // 审批回调将被幂等门吸收（COMPLETED 同结果），随后走开通自愈——不抛错不重建审批
        } else if (!FranchiseeApplicationStatus.SUBMITTED.name().equals(status)) {
            // DRAFT（未提交）/REJECTED（终态）上审批：显式冲突 + DENIED 留痕（独立事务）
            recordDeniedAudit(tenantId, appKey, "APPROVE", status, cmd.actorId());
            throw exception(FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, status);
        } else {
            // SUBMITTED：审批必须经活跃绑定（BOUND）；WITHDRAWN=审批流已撤回须 restart，不静默
            Map<String, Object> binding = processBindingService.findLatestByDomain(tenantId,
                    FirstChainObjectType.APPLICATION, applicationId);
            if (binding == null) {
                recordDeniedAudit(tenantId, appKey, "APPROVE", status, cmd.actorId());
                throw exception(FIRST_CHAIN_PROCESS_NOT_BOUND);
            }
            if (!FirstChainProcessBindingService.STATUS_BOUND.equals(binding.get("status"))) {
                recordDeniedAudit(tenantId, appKey, "APPROVE", status, cmd.actorId());
                throw exception(FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, status);
            }
            // ④ 审批回调（幂等门在 bpm 域内聚：重复同结果吸收/异结果冲突/晚到弃单；通过意见可空）
            firstChainApplicationService.onApprovalCompleted(
                    (String) binding.get("process_instance_id"), true, cmd.reason(), cmd.actorId());
            processInstanceId = (String) binding.get("process_instance_id");
        }
        // ⑤ 幂等开通（M4-A）：app_key 查既有组织兜底（见类注释），缺失才建主体
        Long organizationId;
        String initialPassword;
        Long leaderUserId = null;
        if (opened != null) {
            organizationId = opened.getId();
            initialPassword = null;
        } else {
            FranchiseeOpening opening = openFranchisee(tenantId, appKey, application, cmd.operatorUserId());
            organizationId = opening.organizationId();
            initialPassword = opening.leaderAccount().initialPassword();
            leaderUserId = opening.leaderAccount().userId();
        }
        // ⑥ 开通审计 SUCCESS（随本事务：整体回滚则不留，fail-closed）
        recordOpeningAudit(tenantId, appKey, cmd.actorId(), cmd.reason(), organizationId);
        // ⑦ 接线（ZS-FC-003，同事务）：审批待办经 Outbox 事件流转（COMPLETE）+ 结果/开通通知
        if (processInstanceId != null) {
            Long submitterUserId = parseCreatorUserId(application);
            notifyWiringService.onApprovalCompleted(appKey, applicationId, processInstanceId,
                    currentApplicationVersion(tenantId, appKey), (String) application.get("applicant_name"),
                    true, submitterUserId, leaderUserId, cmd.reason(), cmd.actorId());
        } else {
            log.warn("[approveAndOpen][审批实例缺失（自愈窗口），跳过待办/通知接线：appKey={}]", appKey);
        }
        return new OpeningResult(organizationId, initialPassword);
    }

    /**
     * 审批拒绝（PILOT-REQ-002；M4-A：REJECTED 不建任何主体，意见必填）。
     *
     * <p>重复拒绝（绑定 COMPLETED+REJECTED 后再次拒绝）幂等吸收返回；DRAFT/APPROVED 上拒绝显式冲突；
     * reject_reason 与 REJECT 审计由 bpm 域 {@code onApprovalCompleted} 落（本服务不双写）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(ApproveCmd cmd) {
        Long tenantId = requireTenantId();
        // ① 拒绝意见必填（与 bpm 域同口径快检：fail-fast 于任何写动作之前）
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw exception(FIRSTCHAIN_REJECT_REASON_REQUIRED);
        }
        // ② M2 对象级资格（同 approveAndOpen）
        assertApproverQualification(cmd.operatorUserId(), "REJECT");
        // ③ 申请行定位 + 状态预分类
        Map<String, Object> application = requireApplication(tenantId, cmd.appKey());
        Long applicationId = ((Number) application.get("id")).longValue();
        String appKey = (String) application.get("app_key");
        String status = (String) application.get("status");
        if (FranchiseeApplicationStatus.REJECTED.name().equals(status)) {
            // 重复拒绝：幂等吸收返回既有结果（M4-A 同语义），不重复留痕
            log.info("[reject][重复拒绝请求被吸收：appKey={}]", appKey);
            return;
        }
        if (!FranchiseeApplicationStatus.SUBMITTED.name().equals(status)) {
            // DRAFT（未提交）/APPROVED（已通过）上拒绝：显式冲突 + DENIED 留痕
            recordDeniedAudit(tenantId, appKey, "REJECT", status, cmd.actorId());
            throw exception(FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, status);
        }
        Map<String, Object> binding = processBindingService.findLatestByDomain(tenantId,
                FirstChainObjectType.APPLICATION, applicationId);
        if (binding == null) {
            recordDeniedAudit(tenantId, appKey, "REJECT", status, cmd.actorId());
            throw exception(FIRST_CHAIN_PROCESS_NOT_BOUND);
        }
        if (!FirstChainProcessBindingService.STATUS_BOUND.equals(binding.get("status"))) {
            recordDeniedAudit(tenantId, appKey, "REJECT", status, cmd.actorId());
            throw exception(FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, status);
        }
        // ④ 审批回调（bpm 域落 reject_reason + REJECT 审计；REJECTED 不建任何主体）
        firstChainApplicationService.onApprovalCompleted(
                (String) binding.get("process_instance_id"), false, cmd.reason(), cmd.actorId());
        // ⑤ 接线（ZS-FC-003，同事务）：审批待办经 Outbox 事件流转（COMPLETE）+ 拒绝结果通知致提交人
        notifyWiringService.onApprovalCompleted(appKey, applicationId,
                (String) binding.get("process_instance_id"), currentApplicationVersion(tenantId, appKey),
                (String) application.get("applicant_name"), false, parseCreatorUserId(application),
                null, cmd.reason(), cmd.actorId());
    }

    // ========== 内部：开通主体（M4-A/M5-A） ==========

    /**
     * 开通加盟商主体：一套默认授权（租户级模板 + 默认菜单面）→ 负责人账号 → FRANCHISEE 组织 → 负责人任职。
     *
     * <p>顺序依据：任职引用组织与角色须先就位；组织 leaderUserId 引用负责人账号。全部动作加入
     * 本服务事务（system 服务 @Transactional REQUIRED 同源），任一失败整体回滚（M4-A fail-closed）。
     */
    private FranchiseeOpening openFranchisee(Long tenantId, String appKey, Map<String, Object> application,
                                             Long operatorUserId) {
        // 1. 一套默认授权：租户级模板 create-or-reuse（角色定义「能做什么」，组织归属经任职承载）；
        //    默认菜单面循 FirstchainMenus 编号合同（空集登记期结束，§1.1 第⑤步落位）
        Long leaderRoleId = defaultRoleRegistry.ensureRoleWithMenus(tenantId, ROLE_CODE_LEADER, ROLE_NAME_LEADER,
                1, FirstchainMenus.LEADER_ROLE_MENUS);
        Long memberRoleId = defaultRoleRegistry.ensureRoleWithMenus(tenantId, ROLE_CODE_MEMBER, ROLE_NAME_MEMBER,
                2, FirstchainMenus.MEMBER_ROLE_MENUS);
        // 2. 负责人账号（M5-A：用户名 trim+小写派生自申请编号，租户内唯一；初始密码随机生成并随开通结果
        //    一次性下发，明文禁落审计）
        LeaderAccount leaderAccount = createLeaderUser(application, appKey);
        // 3. FRANCHISEE 组织（编码=申请编号=开通幂等键；负责人回填 leaderUserId）
        OrganizationDO organization = new OrganizationDO();
        organization.setName((String) application.get("applicant_name"));
        organization.setCode(appKey);
        organization.setType(OrganizationTypeEnum.FRANCHISEE.getType());
        organization.setParentId(OrganizationDO.PARENT_ID_ROOT);
        organization.setSort(0);
        organization.setStatus(CommonStatusEnum.ENABLE.getStatus());
        organization.setLeaderUserId(leaderAccount.userId());
        organization.setRemark("首链申请开通自动创建（app_key=" + appKey + "）");
        Long organizationId;
        try {
            organizationId = organizationService.createOrganization(organization);
        } catch (DuplicateKeyException concurrentOpening) {
            // 并发双开兜底（M4-A：重复处理只产生一个组织）：uk_system_organization_code 先到者生效，
            // 后来者显式冲突（审批门串行化后的残余竞态窗口，PILOT-REQ-003 唯一性由 DB 约束兜底）
            throw exception(FIRSTCHAIN_OPENING_ORG_CONFLICT, appKey);
        }
        // 4. 负责人任职（员工只进目标组织；角色=一套默认授权；首任职自动承接默认任职=服务端上下文来源）
        MembershipDO membership = new MembershipDO();
        membership.setUserId(leaderAccount.userId());
        membership.setOrganizationId(organizationId);
        membership.setRoleIds(Set.of(leaderRoleId, memberRoleId));
        membershipService.createMembership(membership, operatorUserId);
        log.info("[openFranchisee][加盟商开通完成：appKey={} organizationId={} leaderUserId={} roles={}/{}]",
                appKey, organizationId, leaderAccount.userId(), ROLE_CODE_LEADER, ROLE_CODE_MEMBER);
        return new FranchiseeOpening(organizationId, leaderAccount);
    }

    /**
     * 创建负责人账号（M5-A：用户名 trim+小写；初始密码随机生成并随 {@link LeaderAccount} 一次性下发）。
     *
     * <p>账号手机号<b>不</b>入：联系人电话为申请域自由文本列（M8 F2 建议），与登录身份解耦——
     * 既有申请的手机号可能重复/固话，直接入账号会撞 system_users 手机号唯一约束致开通误伤；
     * 账号资料完善（手机号）随后续 wave 登记。
     */
    private LeaderAccount createLeaderUser(Map<String, Object> application, String appKey) {
        UserSaveReqVO user = new UserSaveReqVO();
        user.setUsername(deriveUsername(appKey));
        String contactName = (String) application.get("contact_name");
        user.setNickname(StrUtil.isBlank(contactName) ? (String) application.get("applicant_name") : contactName);
        // M5-A：初始密码随机生成（16 位），经开通结果一次性下发给审批操作人；密码明文禁落审计（审计明细脱敏合同）
        String initialPassword = RandomUtil.randomString(16);
        user.setPassword(initialPassword);
        Long userId = adminUserService.createUser(user);
        return new LeaderAccount(userId, initialPassword);
    }

    /**
     * 负责人用户名派生（M5-A）：{@code fc} + 申请编号归一化（trim + 小写 + 仅保留字母数字），
     * 补位至 4 位、截断至 30 位，满足 {@code system_users} 账号规则 {@code ^[a-zA-Z0-9]{4,30}$}。
     * 申请编号租户内唯一 → 用户名租户内唯一；极小概率归一化撞名（如 {@code APP-01} 与 {@code APP01}）
     * 以账号唯一约束显式冲突，整单回滚 fail-closed（不静默改造申请数据）。
     */
    private static String deriveUsername(String appKey) {
        String normalized = appKey == null ? "" : appKey.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        StringBuilder username = new StringBuilder("fc").append(normalized);
        while (username.length() < 4) {
            username.append('0');
        }
        if (username.length() > 30) {
            username.setLength(30);
        }
        return username.toString();
    }

    // ========== 内部：守卫与留痕 ==========

    /**
     * M2 对象级资格校验（接入合同 §1.6：动作权限之外，对象级资格在服务层二次校验）：
     * 操作人须持 PLATFORM 类型组织的 ACTIVE 默认任职（审批人=任意 PLATFORM 有效任职，D-07 M2-A）。
     * 不通过即 DENIED 留痕（独立事务）+ 显式拒绝。
     */
    private void assertApproverQualification(Long operatorUserId, String action) {
        if (operatorUserId == null) {
            throw exception(FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED);
        }
        MembershipDO membership = membershipService.getPrimaryMembership(operatorUserId);
        if (membership == null || !MembershipStatusEnum.ACTIVE.getStatus().equals(membership.getStatus())) {
            throw exception(FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED);
        }
        OrganizationDO organization = organizationService.getOrganization(membership.getOrganizationId());
        if (organization == null || !OrganizationTypeEnum.PLATFORM.getType().equals(organization.getType())) {
            throw exception(FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED);
        }
    }

    private Map<String, Object> requireApplication(Long tenantId, String appKey) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, app_key, applicant_name, contact_name, status, version, creator "
                        + "FROM bpm_first_chain_application WHERE tenant_id = ? AND app_key = ? AND deleted = FALSE",
                tenantId, appKey);
        if (rows.isEmpty()) {
            throw exception(FIRSTCHAIN_APPLICATION_NOT_EXISTS);
        }
        return rows.get(0);
    }

    /**
     * 开通幂等兜底查询（M4-A：幂等键=申请编号）：按（租户, 组织编码=app_key）查既有组织。
     *
     * <p>归属假设登记：组织编码 {@code code = app_key} 为开通域保留命名——同租户内命中该编码的组织
     * 即视为本申请已开通主体（申请编号租户内唯一 + {@code uk_system_organization_code} 共同守护）。
     * 真实建组织仍以 {@code OrganizationService#createOrganization} 的应用层校验 + DB 唯一约束为准，
     * 并发双开的残余窗口由 {@link #openFranchisee} 的 DuplicateKey 转译兜底。
     */
    private OrganizationDO findOpenedOrganization(Long tenantId, String appKey) {
        // system_organization.deleted 为 int2（基线形态）：谓词必须写 `= 0`（真实 PG 无 smallint = boolean 算符）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, code, type FROM system_organization "
                        + "WHERE tenant_id = ? AND code = ? AND deleted = 0",
                tenantId, appKey);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        OrganizationDO organization = new OrganizationDO();
        organization.setId(((Number) row.get("id")).longValue());
        organization.setCode((String) row.get("code"));
        return organization;
    }

    private Long queryRoleIdByCode(Long tenantId, String code) {
        // system_role.deleted 为 int2（基线形态）：谓词必须写 `= 0`（真实 PG 无 smallint = boolean 算符）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM system_role WHERE tenant_id = ? AND code = ? AND deleted = 0",
                tenantId, code);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

    /** 申请行 creator 列（提交人 actorId）→ 用户编号（接线收件人解析；历史脏数据降级 null）。 */
    private static Long parseCreatorUserId(Map<String, Object> application) {
        Object creator = application.get("creator");
        if (creator == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(creator));
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    /** 审批后申请版本（接线乱序护栏基线；随本事务已可见）。 */
    private Long currentApplicationVersion(Long tenantId, String appKey) {
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM bpm_first_chain_application WHERE tenant_id = ? AND app_key = ? "
                        + "AND deleted = FALSE", Long.class, tenantId, appKey);
        return version == null ? null : version;
    }

    /**
     * 开通审计 SUCCESS（bizId=申请编号，bizVersion=审批后领域新版本；随本事务提交）。
     */
    private void recordOpeningAudit(Long tenantId, String appKey, String actorId, String reason,
                                    Long organizationId) {
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM bpm_first_chain_application WHERE tenant_id = ? AND app_key = ? "
                        + "AND deleted = FALSE", Long.class, tenantId, appKey);
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED)
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action("OPEN")
                .bizType(BIZ_TYPE_APPLICATION)
                .bizId(appKey)
                .bizVersion(version == null ? null : String.valueOf(version))
                .reason(reason)
                .detail(Map.of("organizationId", organizationId))
                .result(AuditResult.SUCCESS)
                .tenantId(tenantId)
                .build());
    }

    /**
     * 冲突/资格 DENIED 审计（独立事务：业务回滚不丢失拒绝留痕，接入合同 §1.7/§4）。
     */
    private void recordDeniedAudit(Long tenantId, String appKey, String action, String status, String actorId) {
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.ACCESS_DENIED)
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action(action)
                .bizType(BIZ_TYPE_APPLICATION)
                .bizId(appKey)
                .reason("domainStatus=" + status)
                .result(AuditResult.DENIED)
                .tenantId(tenantId)
                .build());
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(FIRSTCHAIN_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 审批命令（管理端审批入口：appKey 为审批可见业务键；通过时 reason 为可空意见，拒绝时必填；
     * operatorUserId 供 M2 对象级资格校验〔审批人=任意 PLATFORM 有效任职〕与任职 operatorId）。
     */
    @lombok.Builder
    public record ApproveCmd(String appKey, String reason, String actorId, Long operatorUserId) {
    }

    /**
     * 开通结果（M5-A 初始密码下发载体）：organizationId=已开通（或既有）加盟商组织；
     * initialPassword=本次新建负责人账号的一次性初始密码（重复处理/既有组织路径为 null——
     * 密码仅在下发生成时可见，重发走重置通道；明文禁落审计/日志）。
     */
    public record OpeningResult(Long organizationId, String initialPassword) {
    }

    /**
     * 开通过程产物：新建负责人账号（userId + 一次性初始密码），供开通结果组装。
     */
    private record LeaderAccount(Long userId, String initialPassword) {
    }

    /**
     * 开通过程产物：组织编号 + 负责人账号。
     */
    private record FranchiseeOpening(Long organizationId, LeaderAccount leaderAccount) {
    }

}
