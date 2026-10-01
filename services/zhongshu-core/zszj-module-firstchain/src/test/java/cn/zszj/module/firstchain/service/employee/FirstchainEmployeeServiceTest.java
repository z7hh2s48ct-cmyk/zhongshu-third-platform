package cn.zszj.module.firstchain.service.employee;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreatedRespVO;
import cn.zszj.module.firstchain.service.opening.FirstchainDefaultRoleRegistry;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_TENANT_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainEmployeeService} 的单元测试（ZS-FC-001 员工授权 wave，H2）——D-07 M5-A。
 *
 * <p>覆盖：负责人直接创建员工账号（账号+任职+审计同事务）；对象级资格 fail-closed
 * （仅 FRANCHISEE 组织负责人，成员/平台/无登录态均拒）；初始密码一次性下发且不落审计；
 * 员工只进负责人所在组织并绑定员工模板角色（PILOT-REQ-004）。
 *
 * @author ZS-FC-001
 */
@Import({FirstchainEmployeeService.class, FirstchainDefaultRoleRegistry.class, JdbcAuditPort.class})
class FirstchainEmployeeServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    private static final Long LEADER_USER_ID = 900L;

    private static final Long FRANCHISEE_ORG_ID = 700L;

    private static final Long NEW_EMPLOYEE_USER_ID = 950L;

    private static final Long ROLE_MEMBER_ID = 502L;

    @Resource
    private FirstchainEmployeeService employeeService;

    @Resource
    private DataSource dataSource;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private MembershipService membershipService;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private PermissionService permissionService;

    private JdbcTemplate jdbcTemplate;

    /** 员工账号创建入参捕获 */
    private final AtomicReference<UserSaveReqVO> capturedEmployeeUser = new AtomicReference<>();

    /** 员工任职创建入参捕获 */
    private final AtomicReference<MembershipDO> capturedMembership = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(TENANT_ID);
        capturedEmployeeUser.set(null);
        capturedMembership.set(null);
        // 默认登录态：FRANCHISEE 组织负责人
        loginAs(LEADER_USER_ID);
        stubLeaderMembership();
        // 角色创建桩：落 system_role 桩表（registry create-or-reuse 查询面）
        when(roleService.createRole(any(), any())).thenAnswer(invocation -> {
            jdbcTemplate.update(
                    "INSERT INTO system_role (id, code, name, tenant_id, deleted) VALUES (?, ?, ?, ?, FALSE)",
                    ROLE_MEMBER_ID, "firstchain:franchisee:member", "首链加盟商员工", TENANT_ID);
            return ROLE_MEMBER_ID;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    // ========== 主链：负责人创建员工账号 ==========

    @Test
    void createEmployee_byLeader_createsAccountMembershipAndAudit() {
        stubEmployeeUserCreation();
        stubMembershipCreation();

        EmployeeCreatedRespVO respVO = employeeService.createEmployee(createReqVO("zhangsan001", "张三"));

        // 账号：用户名透传（trim+小写归一化在 AdminUserService 内聚循 D-09）、初始密码 16 位且一次性下发
        UserSaveReqVO user = capturedEmployeeUser.get();
        assertThat(user.getUsername()).isEqualTo("zhangsan001");
        assertThat(user.getNickname()).isEqualTo("张三");
        assertThat(user.getPassword()).hasSize(16);
        assertThat(respVO.getUserId()).isEqualTo(NEW_EMPLOYEE_USER_ID);
        assertThat(respVO.getUsername()).isEqualTo("zhangsan001");
        assertThat(respVO.getInitialPassword()).isEqualTo(user.getPassword());
        // 任职：员工只进负责人所在组织 + 员工模板角色（PILOT-REQ-004）
        MembershipDO membership = capturedMembership.get();
        assertThat(membership.getUserId()).isEqualTo(NEW_EMPLOYEE_USER_ID);
        assertThat(membership.getOrganizationId()).isEqualTo(FRANCHISEE_ORG_ID);
        assertThat(membership.getRoleIds()).containsExactly(ROLE_MEMBER_ID);
        // 审计 SUCCESS（授权变化有审计）；密码明文不落审计（detail 仅账号名与组织）
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = 'OBJECT_CREATED' AND action = 'CREATE_EMPLOYEE' "
                        + "AND biz_id = ? AND result = 'SUCCESS'", Long.class, String.valueOf(NEW_EMPLOYEE_USER_ID));
        assertThat(auditCount).isEqualTo(1L);
        Long passwordInAudit = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE detail LIKE ?",
                Long.class, "%" + user.getPassword() + "%");
        assertThat(passwordInAudit).isZero();
    }

    // ========== 对象级资格 fail-closed ==========

    @Test
    void createEmployee_byNonLeaderMember_rejected() {
        // 同组织成员（非 leaderUserId）→ 拒绝（M5-A：负责人直接创建）
        OrganizationDO franchiseeOrg = franchiseeOrg(LEADER_USER_ID + 1);
        when(organizationService.getOrganization(FRANCHISEE_ORG_ID)).thenReturn(franchiseeOrg);

        assertServiceException(() -> employeeService.createEmployee(createReqVO("zhangsan001", "张三")),
                FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
        verifyNoInteractions(adminUserService);
    }

    @Test
    void createEmployee_byPlatformOperator_rejected() {
        // 平台运营（非 FRANCHISEE 组织）→ 拒绝
        MembershipDO membership = new MembershipDO();
        membership.setUserId(LEADER_USER_ID);
        membership.setOrganizationId(10L);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(LEADER_USER_ID)).thenReturn(membership);
        OrganizationDO platformOrg = new OrganizationDO();
        platformOrg.setId(10L);
        platformOrg.setType(OrganizationTypeEnum.PLATFORM.getType());
        platformOrg.setLeaderUserId(LEADER_USER_ID);
        when(organizationService.getOrganization(10L)).thenReturn(platformOrg);

        assertServiceException(() -> employeeService.createEmployee(createReqVO("zhangsan001", "张三")),
                FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
        verifyNoInteractions(adminUserService);
    }

    @Test
    void createEmployee_withoutLoginUser_rejected() {
        SecurityContextHolder.clearContext();

        assertServiceException(() -> employeeService.createEmployee(createReqVO("zhangsan001", "张三")),
                FIRSTCHAIN_EMPLOYEE_ACTOR_NOT_LEADER);
    }

    @Test
    void createEmployee_withoutTenant_failClosed() {
        TenantContextHolder.clear();

        assertServiceException(() -> employeeService.createEmployee(createReqVO("zhangsan001", "张三")),
                FIRSTCHAIN_TENANT_REQUIRED);
        verifyNoInteractions(adminUserService);
    }

    // ========== 账号创建失败同事务回滚（fail-closed） ==========

    @Test
    void createEmployee_userCreationFailure_nothingPersisted() {
        stubMembershipCreation();
        when(adminUserService.createUser(any())).thenThrow(new RuntimeException("用户名撞唯一约束"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> employeeService.createEmployee(createReqVO("zhangsan001", "张三")))
                .isInstanceOf(RuntimeException.class);
        // 任职未创建（异常先于任职）、审计未留痕（同事务 fail-closed）
        assertThat(capturedMembership.get()).isNull();
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE action = 'CREATE_EMPLOYEE'", Long.class);
        assertThat(auditCount).isZero();
    }

    // ========== 测试辅助 ==========

    private static EmployeeCreateReqVO createReqVO(String username, String nickname) {
        EmployeeCreateReqVO reqVO = new EmployeeCreateReqVO();
        reqVO.setUsername(username);
        reqVO.setNickname(nickname);
        return reqVO;
    }

    private void stubLeaderMembership() {
        MembershipDO membership = new MembershipDO();
        membership.setUserId(LEADER_USER_ID);
        membership.setOrganizationId(FRANCHISEE_ORG_ID);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(LEADER_USER_ID)).thenReturn(membership);
        when(organizationService.getOrganization(FRANCHISEE_ORG_ID)).thenReturn(franchiseeOrg(LEADER_USER_ID));
    }

    private static OrganizationDO franchiseeOrg(Long leaderUserId) {
        OrganizationDO organization = new OrganizationDO();
        organization.setId(FRANCHISEE_ORG_ID);
        organization.setType(OrganizationTypeEnum.FRANCHISEE.getType());
        organization.setStatus(cn.zszj.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());
        organization.setLeaderUserId(leaderUserId);
        return organization;
    }

    private void stubEmployeeUserCreation() {
        when(adminUserService.createUser(any())).thenAnswer(invocation -> {
            capturedEmployeeUser.set(invocation.getArgument(0));
            return NEW_EMPLOYEE_USER_ID;
        });
    }

    private void stubMembershipCreation() {
        when(membershipService.createMembership(any(), any())).thenAnswer(invocation -> {
            capturedMembership.set(invocation.getArgument(0));
            return null;
        });
    }

    private static void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

}
