package cn.zszj.module.firstchain.service.opening;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.bpm.firstchain.FirstChainProcessPort;
import cn.zszj.module.bpm.firstchain.FirstChainStateTransitionExecutor;
import cn.zszj.module.bpm.firstchain.FranchiseeApplicationStatus;
import cn.zszj.module.firstchain.enums.ErrorCodeConstants;
import cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyDispatcher;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.user.AdminUserService;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_STATUS_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_REJECT_REASON_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_TENANT_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainOpeningService} 的单元测试（ZS-FC-001，H2）——审批开通链 PILOT-REQ-002/003 服务端。
 *
 * <p>覆盖 docs/05 ZS-FC-001 验收（PILOT-REQ-002/003）：通过/拒绝均记录意见人员时间；拒绝不建任何主体；
 * 重复回调或重复处理只产生一个组织、一个负责人身份和一套默认授权（幂等键=申请编号）；开通内失败整体回滚；
 * 审计 SUCCESS 随事务 / DENIED 独立留痕；无 PLATFORM 任职不能审批；跨租户不可见；租户缺失写侧拒绝。
 * 流程引擎经 {@link FirstChainProcessPort} 桩隔离（循 {@code FirstChainApplicationServiceTest} 基建范式）；
 * system 侧以 Mockito API 层 mock + 最小桩表（幂等兜底查询面）隔离，不改 system 模块。
 *
 * @author ZS-FC-001
 */
@Import({FirstchainOpeningService.class, FirstchainDefaultRoleRegistry.class, FirstchainNotifyWiringService.class,
        FirstChainApplicationService.class,
        FirstChainProcessBindingService.class, FirstChainStateTransitionExecutor.class, JdbcAuditPort.class,
        FirstchainOpeningServiceTest.StubProcessPort.class})
class FirstchainOpeningServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    private static final Long OTHER_TENANT_ID = 2L;

    private static final Long OPERATOR_ID = 100L;

    private static final Long PLATFORM_ORG_ID = 10L;

    private static final Long LEADER_USER_ID = 900L;

    private static final Long OPENED_ORG_ID = 700L;

    private static final Long ROLE_LEADER_ID = 501L;

    private static final Long ROLE_MEMBER_ID = 502L;

    @Resource
    private FirstchainOpeningService openingService;

    @Resource
    private FirstChainApplicationService applicationService;

    @Resource
    private FirstChainProcessPort processPort;

    @Resource
    private DataSource dataSource;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private MembershipService membershipService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private NotifyTodoService notifyTodoService;

    @MockitoBean
    private NotifyDispatcher notifyDispatcher;

    @MockitoBean
    private ReliableEventPort reliableEventPort;

    private JdbcTemplate jdbcTemplate;

    /** 组织创建入参捕获（断言 FRANCHISEE 语义 + mock 副作用落 system_organization 桩表） */
    private final AtomicReference<OrganizationDO> capturedOrganization = new AtomicReference<>();

    /** 负责人账号创建入参捕获（断言 M5-A 用户名派生与初始密码形态） */
    private final AtomicReference<UserSaveReqVO> capturedLeaderUser = new AtomicReference<>();

    /** 负责人任职创建入参捕获（断言默认授权绑定） */
    private final AtomicReference<MembershipDO> capturedMembership = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        ((StubProcessPort) processPort).reset();
        TenantContextHolder.setTenantId(TENANT_ID);
        capturedOrganization.set(null);
        capturedLeaderUser.set(null);
        capturedMembership.set(null);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ========== ① 审批通过 → 幂等开通（PILOT-REQ-003；M4-A） ==========

    @Test
    void approveAndOpen_createsFranchiseePrincipalWithDefaults() {
        Long id = createSubmittedApplication("APP-O1");
        mockPlatformOperator();
        mockOrganizationCreation();
        mockLeaderUserCreation();
        mockMembershipCreation();
        mockRoleCreation();

        FirstchainOpeningService.OpeningResult opening =
                openingService.approveAndOpen(approveCmd("APP-O1", "资质齐备，同意开通"));

        // 审批生效：领域 APPROVED、绑定 COMPLETED（bpm 幂等门真实路径）
        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.APPROVED.name());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(2L);
        // 开通主体：FRANCHISEE 组织（编码=申请编号=幂等键）+ 负责人（leaderUserId 回填）
        assertThat(opening.organizationId()).isEqualTo(OPENED_ORG_ID);
        OrganizationDO organization = capturedOrganization.get();
        assertThat(organization.getType()).isEqualTo(OrganizationTypeEnum.FRANCHISEE.getType());
        assertThat(organization.getCode()).isEqualTo("APP-O1");
        assertThat(organization.getLeaderUserId()).isEqualTo(LEADER_USER_ID);
        assertThat(organization.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(organization.getParentId()).isEqualTo(OrganizationDO.PARENT_ID_ROOT);
        // 负责人账号（M5-A：用户名 trim+小写派生自申请编号；初始密码随机形态不落审计）
        UserSaveReqVO leaderUser = capturedLeaderUser.get();
        assertThat(leaderUser).isNotNull();
        assertThat(leaderUser.getUsername()).isEqualTo("fcappo1");
        assertThat(leaderUser.getNickname()).isEqualTo("联系人-APP-O1");
        assertThat(leaderUser.getPassword()).hasSize(16);
        // M5-A 初始密码一次性下发：开通结果携带的初始密码与建账密码同源（明文不落审计面）
        assertThat(opening.initialPassword()).isEqualTo(leaderUser.getPassword());
        // 负责人任职：绑定目标组织 + 一套默认授权（员工只进目标组织）
        MembershipDO membership = capturedMembership.get();
        assertThat(membership.getUserId()).isEqualTo(LEADER_USER_ID);
        assertThat(membership.getOrganizationId()).isEqualTo(OPENED_ORG_ID);
        assertThat(membership.getRoleIds()).containsExactlyInAnyOrder(ROLE_LEADER_ID, ROLE_MEMBER_ID);
        // 用户→角色绑定（权限引擎的权威来源 system_user_role；仅写 membership.role_ids 时负责人登录后零权限——
        // 真实 server E2E 暴露：所有首链接口 403）：负责人须同时被授予负责人 + 员工两个默认角色
        verify(permissionService).assignUserRole(LEADER_USER_ID, Set.of(ROLE_LEADER_ID, ROLE_MEMBER_ID));
        // 默认角色 create ×2 + 默认菜单面绑定 ×2（FirstchainMenus 编号合同，空绑定即补绑）
        verify(roleService, times(2)).createRole(any(), any());
        verify(permissionService, times(2)).assignRoleMenu(any(), anySet());
        // 开通审计 SUCCESS（bizId=申请编号，可回查）
        assertThat(queryAuditCount("OBJECT_CREATED", "OPEN", "APP-O1", "SUCCESS")).isEqualTo(1);
        // 接线（ZS-FC-003）：审批完成 → 待办流转事件预写 + 通过结果通知（提交人+新负责人）
        verify(reliableEventPort, times(1)).append(any());
        org.mockito.ArgumentCaptor<NotifyCommand> notifyCaptor =
                org.mockito.ArgumentCaptor.forClass(NotifyCommand.class);
        verify(notifyDispatcher, times(1)).dispatch(notifyCaptor.capture());
        assertThat(notifyCaptor.getValue().getTemplateCode()).isEqualTo("firstchain_application_approved");
    }

    @Test
    void approveAndOpen_duplicateCallback_idempotentAbsorbed() {
        createSubmittedApplication("APP-DUP");
        mockPlatformOperator();
        mockOrganizationCreation();
        mockLeaderUserCreation();
        mockMembershipCreation();
        mockRoleCreation();

        FirstchainOpeningService.OpeningResult first =
                openingService.approveAndOpen(approveCmd("APP-DUP", null));
        FirstchainOpeningService.OpeningResult second =
                openingService.approveAndOpen(approveCmd("APP-DUP", null));

        // 重复处理返回既有结果（M4-A），不产生第二个组织/负责人/一套授权；不重发初始密码
        assertThat(second.organizationId()).isEqualTo(first.organizationId());
        assertThat(first.initialPassword()).isNotNull();
        assertThat(second.initialPassword()).isNull();
        verify(organizationService, times(1)).createOrganization(any());
        verify(adminUserService, times(1)).createUser(any());
        verify(membershipService, times(1)).createMembership(any(), any());
        verify(roleService, times(2)).createRole(any(), any());
        // 重复审批不重复接线（不重发通知、不重复预写待办流转事件）
        verify(reliableEventPort, times(1)).append(any());
        verify(notifyDispatcher, times(1)).dispatch(any());
        assertThat(queryAuditCount("OBJECT_CREATED", "OPEN", "APP-DUP", "SUCCESS")).isEqualTo(1);
    }

    @Test
    void approveAndOpen_rejectedApplication_statusConflictNoPrincipal() {
        Long id = createSubmittedApplication("APP-RJ1");
        mockPlatformOperator();
        openingService.reject(rejectCmd("APP-RJ1", "资质材料不全"));

        // 拒绝后再审批：终态锁定（fail-closed），不建任何主体
        assertServiceException(() -> openingService.approveAndOpen(approveCmd("APP-RJ1", null)),
                FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, FranchiseeApplicationStatus.REJECTED.name());
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.REJECTED.name());
        verify(organizationService, times(0)).createOrganization(any());
        verify(adminUserService, times(0)).createUser(any());
        verify(roleService, times(0)).createRole(any(), any());
    }

    // ========== ② 拒绝路径（PILOT-REQ-002：意见必填、不建主体） ==========

    @Test
    void reject_persistsReasonWithoutPrincipal() {
        Long id = createSubmittedApplication("APP-RJ2");
        mockPlatformOperator();

        openingService.reject(rejectCmd("APP-RJ2", "申请方主体资格存疑"));

        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.REJECTED.name());
        assertThat(row.get("reject_reason")).isEqualTo("申请方主体资格存疑");
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(2L);
        // REJECTED 不建任何主体（M4-A）：组织/账号/角色/任职创建面零调用
        //（资格校验的只读触碰 getPrimaryMembership/getOrganization 合法，不在断言面）
        verify(organizationService, times(0)).createOrganization(any());
        verify(adminUserService, times(0)).createUser(any());
        verify(membershipService, times(0)).createMembership(any(), any());
        verify(roleService, times(0)).createRole(any(), any());
        assertThat(queryAuditCount("OBJECT_CREATED", "OPEN", "APP-RJ2", "SUCCESS")).isZero();
    }

    @Test
    void reject_withoutReason_rejectedAndNothingChanged() {
        Long id = createSubmittedApplication("APP-RJ3");
        mockPlatformOperator();

        assertServiceException(() -> openingService.reject(rejectCmd("APP-RJ3", " ")),
                FIRSTCHAIN_REJECT_REASON_REQUIRED);
        // 意见缺失整单拒绝：领域仍 SUBMITTED、绑定仍 BOUND（可重试补意见）
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(queryLatestBinding(id).get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_BOUND);
    }

    // ========== ③ 归属与资格合同（接入合同 §1.3/§1.6；M2） ==========

    @Test
    void approveAndOpen_withoutTenant_failClosed() {
        createSubmittedApplication("APP-T1");
        TenantContextHolder.clear();

        assertServiceException(() -> openingService.approveAndOpen(approveCmd("APP-T1", null)),
                FIRSTCHAIN_TENANT_REQUIRED);
        verifyNoInteractions(organizationService, adminUserService, membershipService, roleService);
    }

    @Test
    void approveAndOpen_withoutPlatformOperatorQualification_denied() {
        createSubmittedApplication("APP-Q1");
        // 操作人无默认任职 → 无 PLATFORM 有效任职 → 审批资格拒绝（M2：审批人=任意 PLATFORM 有效任职）
        when(membershipService.getPrimaryMembership(OPERATOR_ID)).thenReturn(null);

        assertServiceException(() -> openingService.approveAndOpen(approveCmd("APP-Q1", null)),
                FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED);
        assertThat(queryApplicationByAppKey("APP-Q1").get("status"))
                .isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        verify(organizationService, times(0)).createOrganization(any());
        verify(adminUserService, times(0)).createUser(any());
        verify(roleService, times(0)).createRole(any(), any());
    }

    @Test
    void reject_withNonPlatformOrganizationQualification_denied() {
        createSubmittedApplication("APP-Q2");
        // 默认任职挂在非 PLATFORM 组织（如加盟商）→ 审批资格拒绝
        MembershipDO membership = new MembershipDO();
        membership.setUserId(OPERATOR_ID);
        membership.setOrganizationId(PLATFORM_ORG_ID);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(OPERATOR_ID)).thenReturn(membership);
        OrganizationDO franchiseeOrg = new OrganizationDO();
        franchiseeOrg.setId(PLATFORM_ORG_ID);
        franchiseeOrg.setType(OrganizationTypeEnum.FRANCHISEE.getType());
        when(organizationService.getOrganization(PLATFORM_ORG_ID)).thenReturn(franchiseeOrg);

        assertServiceException(() -> openingService.reject(rejectCmd("APP-Q2", "意见")),
                FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED);
        assertThat(queryApplicationByAppKey("APP-Q2").get("status"))
                .isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
    }

    // ========== ④ 开通失败整体回滚（fail-closed：组织不留、申请状态不留） ==========

    @Test
    void approveAndOpen_openingFailure_rollsBackWholeChain() {
        Long id = createSubmittedApplication("APP-F1");
        mockPlatformOperator();
        mockOrganizationCreation();
        mockLeaderUserCreation();
        mockMembershipCreation();
        mockRoleCreation();
        // 任职创建失败（开通中段）→ 同事务整体回滚（覆盖捕获桩：Mockito 后置 stubbing 优先）
        when(membershipService.createMembership(any(), any())).thenThrow(new RuntimeException("任职创建失败"));

        assertThatThrownBy(() -> openingService.approveAndOpen(approveCmd("APP-F1", null)))
                .isInstanceOf(RuntimeException.class);

        // 审批不生效（幂等门写回同事务回滚）、组织不残留（mock 副作用同事务落桩表，随回滚清除）、无成功审计
        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(1L);
        assertThat(queryLatestBinding(id).get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_BOUND);
        assertThat(queryOrganizationCountByCode("APP-F1")).isZero();
        assertThat(queryAuditCount("OBJECT_CREATED", "OPEN", "APP-F1", "SUCCESS")).isZero();
    }

    // ========== ⑤ 审计合同（接入合同 §1.7：SUCCESS 随事务 / DENIED 独立留痕） ==========

    @Test
    void approveAndOpen_auditSuccessFollowsTransaction() {
        Long id = createSubmittedApplication("APP-AU1");
        mockPlatformOperator();
        mockOrganizationCreation();
        mockLeaderUserCreation();
        mockMembershipCreation();
        mockRoleCreation();

        openingService.approveAndOpen(approveCmd("APP-AU1", "同意开通"));

        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "SELECT * FROM audit_event WHERE biz_id = ? AND action = 'OPEN' AND result = 'SUCCESS'", "APP-AU1");
        assertThat(audit.get("event_type")).isEqualTo("OBJECT_CREATED");
        assertThat(audit.get("biz_type")).isEqualTo(FirstChainObjectType.APPLICATION.getKey());
        assertThat(audit.get("actor_id")).isEqualTo("approver-1");
        // bizVersion=审批后的领域新版本（携对象版本基线）
        assertThat(audit.get("biz_version")).isEqualTo("2");
        assertThat(audit.get("tenant_id")).isEqualTo(TENANT_ID);
    }

    @Test
    void approve_onWithdrawnApproval_deniedAuditSurvivesRollback() {
        Long id = createSubmittedApplication("APP-DN1");
        mockPlatformOperator();
        // 审批流已撤回（无活跃绑定）→ 审批显式冲突
        applicationService.withdrawApproval(id, 1L, "creator-1");

        assertServiceException(() -> openingService.approveAndOpen(approveCmd("APP-DN1", null)),
                FIRSTCHAIN_APPLICATION_STATUS_CONFLICT, FranchiseeApplicationStatus.SUBMITTED.name());

        // DENIED 独立事务留痕（业务回滚不丢失留痕，接入合同 §1.7/§4）
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(queryAuditCount("ACCESS_DENIED", "APPROVE", "APP-DN1", "DENIED")).isEqualTo(1);
    }

    // ========== ⑥ 跨租户不可见（接入合同 §1.3） ==========

    @Test
    void approveAndOpen_crossTenant_notExists() {
        createSubmittedApplication("APP-X1");
        TenantContextHolder.setTenantId(OTHER_TENANT_ID);
        mockPlatformOperator();

        assertServiceException(() -> openingService.approveAndOpen(approveCmd("APP-X1", null)),
                FIRSTCHAIN_APPLICATION_NOT_EXISTS);
    }

    // ========== 测试辅助 ==========

    private FirstchainOpeningService.ApproveCmd approveCmd(String appKey, String reason) {
        return FirstchainOpeningService.ApproveCmd.builder()
                .appKey(appKey)
                .reason(reason)
                .actorId("approver-1")
                .operatorUserId(OPERATOR_ID)
                .build();
    }

    private FirstchainOpeningService.ApproveCmd rejectCmd(String appKey, String reason) {
        return approveCmd(appKey, reason);
    }

    /** 平台操作人资格桩：OPERATOR 的默认任职挂 PLATFORM 组织（ACTIVE） */
    private void mockPlatformOperator() {
        MembershipDO membership = new MembershipDO();
        membership.setUserId(OPERATOR_ID);
        membership.setOrganizationId(PLATFORM_ORG_ID);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(OPERATOR_ID)).thenReturn(membership);
        OrganizationDO platformOrg = new OrganizationDO();
        platformOrg.setId(PLATFORM_ORG_ID);
        platformOrg.setType(OrganizationTypeEnum.PLATFORM.getType());
        when(organizationService.getOrganization(PLATFORM_ORG_ID)).thenReturn(platformOrg);
    }

    /** 组织创建桩：捕获入参 + 以真实语义落 system_organization 桩表（幂等兜底查询面，同事务随回滚清除） */
    private void mockOrganizationCreation() {
        when(organizationService.createOrganization(any())).thenAnswer(invocation -> {
            OrganizationDO organization = invocation.getArgument(0);
            capturedOrganization.set(organization);
            jdbcTemplate.update(
                    "INSERT INTO system_organization (id, code, name, type, parent_id, leader_user_id, status, "
                            + "tenant_id, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE)",
                    OPENED_ORG_ID, organization.getCode(), organization.getName(), organization.getType(),
                    organization.getParentId(), organization.getLeaderUserId(), organization.getStatus(), TENANT_ID);
            return OPENED_ORG_ID;
        });
    }

    /** 负责人账号创建桩：捕获入参（账号落库/唯一性语义由 system 模块自身测试守护，此处只证接线） */
    private void mockLeaderUserCreation() {
        when(adminUserService.createUser(any())).thenAnswer(invocation -> {
            capturedLeaderUser.set(invocation.getArgument(0));
            return LEADER_USER_ID;
        });
    }

    /** 负责人任职创建桩：捕获入参（任职归属/授权语义由 system 模块自身测试守护，此处只证接线与默认授权绑定） */
    private void mockMembershipCreation() {
        when(membershipService.createMembership(any(), any())).thenAnswer(invocation -> {
            capturedMembership.set(invocation.getArgument(0));
            return null;
        });
    }

    /** 默认角色创建桩：捕获编码 + 落 system_role 桩表（create-or-reuse 查询面） */
    private void mockRoleCreation() {
        AtomicLong roleIdSeq = new AtomicLong(ROLE_LEADER_ID - 1);
        when(roleService.createRole(any(), any())).thenAnswer(invocation -> {
            RoleSaveReqVO saveReqVO = invocation.getArgument(0);
            long roleId = roleIdSeq.incrementAndGet();
            jdbcTemplate.update(
                    "INSERT INTO system_role (id, code, name, tenant_id, deleted) VALUES (?, ?, ?, ?, FALSE)",
                    roleId, saveReqVO.getCode(), saveReqVO.getName(), TENANT_ID);
            return roleId;
        });
    }

    private Long createSubmittedApplication(String appKey) {
        Long id = applicationService.createApplication(CreateApplicationCmd.builder()
                .appKey(appKey)
                .applicantName("申请方-" + appKey)
                .contactName("联系人-" + appKey)
                .contactPhone("13800000000")
                .attachmentFileIds("[101]")
                .actorId("creator-1")
                .build());
        applicationService.submitApplication(id, 0L, 200L, "creator-1");
        return id;
    }

    private Map<String, Object> queryApplication(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_application WHERE id = ? AND tenant_id = ?", id, TENANT_ID);
    }

    private Map<String, Object> queryApplicationByAppKey(String appKey) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_application WHERE app_key = ? AND tenant_id = ?", appKey, TENANT_ID);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> queryLatestBinding(Long applicationId) {
        return (Map<String, Object>) jdbcTemplate.queryForList(
                        "SELECT * FROM bpm_first_chain_process_binding WHERE domain_type = ? AND domain_id = ? "
                                + "ORDER BY id DESC LIMIT 1",
                        FirstChainObjectType.APPLICATION.getKey(), applicationId)
                .get(0);
    }

    private long queryAuditCount(String eventType, String action, String bizId, String result) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = ? AND action = ? AND biz_id = ? AND result = ?",
                Long.class, eventType, action, bizId, result);
        return count == null ? 0 : count;
    }

    private long queryOrganizationCountByCode(String code) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_organization WHERE code = ? AND tenant_id = ? AND deleted = FALSE",
                Long.class, code, TENANT_ID);
        return count == null ? 0 : count;
    }

    /** 流程端口桩：循 FirstChainApplicationServiceTest 同款（引擎隔离，真实 Flowable 归 PG 套件） */
    public static class StubProcessPort implements FirstChainProcessPort {

        private final java.util.List<String> startCalls = new java.util.ArrayList<>();
        private final java.util.List<String> withdrawCalls = new java.util.ArrayList<>();
        private int sequence = 0;

        void reset() {
            startCalls.clear();
            withdrawCalls.clear();
            sequence = 0;
        }

        @Override
        public String startApprovalProcess(Long tenantId, String appKey, Long approverUserId) {
            sequence++;
            startCalls.add("PINST-" + sequence);
            return "PINST-" + sequence;
        }

        @Override
        public boolean withdrawProcess(String processInstanceId) {
            withdrawCalls.add(processInstanceId);
            return true;
        }

    }

}
