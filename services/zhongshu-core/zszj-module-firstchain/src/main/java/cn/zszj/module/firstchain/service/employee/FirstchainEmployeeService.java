package cn.zszj.module.firstchain.service.employee;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreatedRespVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeMemberRespVO;
import cn.zszj.module.firstchain.framework.FirstchainMenus;
import cn.zszj.module.firstchain.service.opening.FirstchainDefaultRoleRegistry;
import cn.zszj.module.firstchain.service.opening.FirstchainOpeningService;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_TENANT_REQUIRED;

/**
 * 首链员工账号服务（ZS-FC-001 员工授权 wave；D-07 M5-A「负责人直接创建账号」）。
 *
 * <p>拍板口径（docs/09 §2 M5 = A）：负责人直接创建账号，循 D-09 唯一性细则（M1 全平台唯一、
 * M2 统一 trim+小写存储——归一化在 {@code AdminUserService#createUser} 内聚，本服务透传）；
 * <b>初始密码随机生成并经返回值一次性下发</b>给创建操作人（负责人转交员工本人；明文禁落审计/日志/
 * 持久层）；强制首改后置登记；停用即失权循 IAM-004 既有链（{@code updateUserStatus}/任职停用撤销会话），
 * 本服务不重复建设。
 *
 * <p>对象级资格（接入合同 §1.6 服务层二次校验）：操作人须为 FRANCHISEE 组织负责人
 * （默认任职 ACTIVE + {@code organization.leaderUserId = 操作人}）——动作级权限由
 * Controller 层 {@code @PreAuthorize('firstchain:employee:create')} 承担，两层不可互相替代。
 * 员工只进负责人所在目标组织（PILOT-REQ-004），授权=一套默认授权的员工模板角色
 * （{@link FirstchainOpeningService#ROLE_CODE_MEMBER}，菜单面循 {@link FirstchainMenus#MEMBER_ROLE_MENUS}）。
 *
 * <p>同事务 fail-closed：账号创建 + 任职 + 审计（SUCCESS 随事务，接入合同 §1.7）任一失败整体回滚。
 *
 * @author ZS-FC-001
 */
@Service
@Slf4j
public class FirstchainEmployeeService {

    /** 审计 bizType：员工账号创建（首链域扩展对象，不属 FirstChainObjectType 状态机管辖） */
    private static final String BIZ_TYPE_EMPLOYEE = "firstchain_employee";

    /** 初始密码长度（与负责人账号同规格：16 位随机，M5-A） */
    private static final int INITIAL_PASSWORD_LENGTH = 16;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private MembershipService membershipService;

    @Resource
    private OrganizationService organizationService;

    @Resource
    private FirstchainDefaultRoleRegistry defaultRoleRegistry;

    @Resource
    private AuditPort auditPort;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    void initJdbcTemplate() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 负责人创建员工账号（PILOT-REQ-004 服务端；幂等键=用户名——重复用户名由 system 账号唯一约束
     * 显式分类，不静默吸收）。
     *
     * @return 员工用户编号 + 用户名 + 一次性初始密码
     */
    @Transactional(rollbackFor = Exception.class)
    public EmployeeCreatedRespVO createEmployee(EmployeeCreateReqVO reqVO) {
        Long tenantId = requireTenantId();
        Long operatorUserId = SecurityFrameworkUtils.getLoginUserId();
        // ① 对象级资格：操作人=FRANCHISEE 组织负责人（默认任职 ACTIVE + leaderUserId 即本人）
        OrganizationDO organization = requireLeaderOrganization(operatorUserId);
        // ② 员工模板角色（create-or-reuse + 默认菜单面幂等绑定，与开通共用一套注册中心）
        Long memberRoleId = defaultRoleRegistry.ensureRoleWithMenus(tenantId,
                FirstchainOpeningService.ROLE_CODE_MEMBER, "首链加盟商员工", 2,
                FirstchainMenus.MEMBER_ROLE_MENUS);
        // ③ 员工账号（用户名归一化/唯一性在 AdminUserService 内聚循 D-09；初始密码随机生成、一次性下发）
        String initialPassword = RandomUtil.randomString(INITIAL_PASSWORD_LENGTH);
        UserSaveReqVO user = new UserSaveReqVO();
        user.setUsername(reqVO.getUsername());
        user.setNickname(reqVO.getNickname());
        user.setPassword(initialPassword);
        Long userId = adminUserService.createUser(user);
        // ④ 任职（员工只进负责人所在目标组织；角色=员工默认授权；首任职自动承接默认任职）
        MembershipDO membership = new MembershipDO();
        membership.setUserId(userId);
        membership.setOrganizationId(organization.getId());
        membership.setRoleIds(Set.of(memberRoleId));
        membershipService.createMembership(membership, operatorUserId);
        // ⑤ 审计 SUCCESS 随事务（授权变化有审计——验收条款；密码明文禁落审计，只记账号与组织）
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED)
                .actorType(ActorType.ADMIN)
                .actorId(String.valueOf(operatorUserId))
                .action("CREATE_EMPLOYEE")
                .bizType(BIZ_TYPE_EMPLOYEE)
                .bizId(String.valueOf(userId))
                .detail(Map.of("username", user.getUsername(), "organizationId", organization.getId()))
                .result(AuditResult.SUCCESS)
                .tenantId(tenantId)
                .build());
        log.info("[createEmployee][员工账号创建完成：userId={} organizationId={} operator={}]",
                userId, organization.getId(), operatorUserId);
        EmployeeCreatedRespVO respVO = new EmployeeCreatedRespVO();
        respVO.setUserId(userId);
        respVO.setUsername(user.getUsername());
        respVO.setInitialPassword(initialPassword);
        return respVO;
    }

    /**
     * 本组织在职成员列表（ZS-FC-003 员工选择器数据源；分配/改派的目标员工只能来自本组织）。
     *
     * <p>对象级资格同创建：仅 FRANCHISEE 组织负责人（实时重算）；返回负责人本人在内的全部 ACTIVE
     * 任职成员（含停用账号排除——账号 status 禁用一并排除，与「停用即失权」口径一致）。
     */
    public List<EmployeeMemberRespVO> listOrgMembers() {
        Long tenantId = requireTenantId();
        Long operatorUserId = SecurityFrameworkUtils.getLoginUserId();
        OrganizationDO organization = requireLeaderOrganization(operatorUserId);
        // 显式租户 + deleted=0（基线表 int2 形态；双表同过滤）；仅 ACTIVE 任职 + 启用账号
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, u.nickname FROM system_membership m "
                        + "JOIN system_users u ON u.id = m.user_id AND u.deleted = 0 "
                        + "AND u.status = ? AND u.tenant_id = ? "
                        + "WHERE m.organization_id = ? AND m.status = ? AND m.deleted = 0 AND m.tenant_id = ? "
                        + "ORDER BY u.id",
                CommonStatusEnum.ENABLE.getStatus(), tenantId,
                organization.getId(), MembershipStatusEnum.ACTIVE.getStatus(), tenantId);
        List<EmployeeMemberRespVO> members = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            EmployeeMemberRespVO member = new EmployeeMemberRespVO();
            member.setUserId(((Number) row.get("id")).longValue());
            member.setUsername((String) row.get("username"));
            member.setNickname((String) row.get("nickname"));
            members.add(member);
        }
        return members;
    }

    /**
     * 操作人组织解析与负责人资格判定（实时重算不缓存——停用/撤职即失创建权，IAM-004 同源口径）。
     */
    private OrganizationDO requireLeaderOrganization(Long operatorUserId) {
        if (operatorUserId == null) {
            throw exception(FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
        }
        MembershipDO membership = membershipService.getPrimaryMembership(operatorUserId);
        if (membership == null || !MembershipStatusEnum.ACTIVE.getStatus().equals(membership.getStatus())) {
            throw exception(FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
        }
        OrganizationDO organization = organizationService.getOrganization(membership.getOrganizationId());
        if (organization == null || !OrganizationTypeEnum.FRANCHISEE.getType().equals(organization.getType())
                || !CommonStatusEnum.ENABLE.getStatus().equals(organization.getStatus())
                || !operatorUserId.equals(organization.getLeaderUserId())) {
            throw exception(FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
        }
        return organization;
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(FIRSTCHAIN_TENANT_REQUIRED);
        }
        return tenantId;
    }

}
