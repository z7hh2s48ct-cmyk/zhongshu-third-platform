package cn.zszj.module.firstchain.service.lead;

import cn.zszj.framework.common.exception.util.ServiceExceptionUtil;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.datapermission.core.authorize.FieldLevel;
import cn.zszj.framework.datapermission.core.authorize.FieldLevelScopeResolver;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadAssignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadClaimReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadConvertReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadDistributeReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadFollowupReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadInvalidateReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadPageReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadReassignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadRespVO;
import cn.zszj.module.firstchain.framework.FirstchainLeadAuthorizationProvider;
import cn.zszj.module.firstchain.service.FirstchainLeadMetricsService;
import cn.zszj.module.firstchain.service.FirstchainLeadService;
import cn.zszj.module.firstchain.service.FirstchainLeadService.DistributeCmd;
import cn.zszj.module.firstchain.service.FirstchainLeadService.FollowupCmd;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ACTOR_NOT_LEADER;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ACTOR_QUALIFICATION_DENIED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_NOT_ASSIGNEE;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_DISTRIBUTE_NOT_PLATFORM;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_KEY_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_VISIBLE_DENIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainLeadAppService} 的单元测试（ZS-FC-002 REST wave）——REST 面门面语义。
 *
 * <p>验证四道职责（领域层行为已由 {@code FirstchainLeadServiceTest} 覆盖，本类 mock 领域层）：
 * <ul>
 *     <li><b>视角服务端解析</b>（PILOT-REQ-009）：平台/负责人/员工三视角的查询范围收敛与对象级判定，
 *     无任职/非加盟商组织 fail-closed；</li>
 *     <li><b>对象级资格</b>：下发限平台、分配/改派限归属组织负责人、转商机限被分配员工、
 *     无效关闭限被分配员工或负责人代操作；</li>
 *     <li><b>业务键服务端生成</b>：LC/OP 前缀 + 撞号有限重试；领取人/跟进人服务端绑定登录用户；</li>
 *     <li><b>D-12 出口裁剪</b>：真实 {@link ObjectAuthorizationService} + 真实
 *     {@link FirstchainLeadAuthorizationProvider} 裁决链——员工（上限 F1）F2 字段脱敏尾四位、
 *     负责人（上限 F2）清晰可见。</li>
 * </ul>
 *
 * @author ZS-FC-002
 */
class FirstchainLeadAppServiceTest {

    private static final Long TENANT_ID = 1L;

    private static final Long PLATFORM_USER_ID = 1L;

    private static final Long PLATFORM_ORG_ID = 10L;

    private static final Long LEADER_USER_ID = 100L;

    private static final Long EMPLOYEE_USER_ID = 200L;

    private static final Long FRANCHISEE_ORG_ID = 300L;

    private static final Long OTHER_ORG_ID = 900L;

    private static final Long LEAD_ID = 2048L;

    private FirstchainLeadService leadService;

    private FirstchainLeadMetricsService metricsService;

    private MembershipService membershipService;

    private OrganizationService organizationService;

    private FieldLevelScopeResolver fieldLevelScopeResolver;

    private FirstchainLeadAppService appService;

    @BeforeEach
    void setUp() {
        leadService = mock(FirstchainLeadService.class);
        metricsService = mock(FirstchainLeadMetricsService.class);
        membershipService = mock(MembershipService.class);
        organizationService = mock(OrganizationService.class);
        // 真实裁决服务 + 真实 firstchain Provider（D-12 裁决链集成面），仅桩掉外部依赖
        SecurityFrameworkService securityFrameworkService = mock(SecurityFrameworkService.class);
        OrgDataPermissionChecker orgDataPermissionChecker = mock(OrgDataPermissionChecker.class);
        fieldLevelScopeResolver = mock(FieldLevelScopeResolver.class);
        ObjectAuthorizationService objectAuthorizationService = new ObjectAuthorizationService(
                securityFrameworkService, orgDataPermissionChecker, fieldLevelScopeResolver,
                List.of(new FirstchainLeadAuthorizationProvider()));
        appService = new FirstchainLeadAppService();
        ReflectionTestUtils.setField(appService, "leadService", leadService);
        ReflectionTestUtils.setField(appService, "metricsService", metricsService);
        ReflectionTestUtils.setField(appService, "membershipService", membershipService);
        ReflectionTestUtils.setField(appService, "organizationService", organizationService);
        ReflectionTestUtils.setField(appService, "objectAuthorizationService", objectAuthorizationService);
        // 登录上下文与默认视角：负责人（各用例按需改绑）
        loginAs(LEADER_USER_ID);
        stubMembership(LEADER_USER_ID, FRANCHISEE_ORG_ID);
        stubOrganization(FRANCHISEE_ORG_ID, OrganizationTypeEnum.FRANCHISEE.getType(), LEADER_USER_ID);
        stubOrganization(PLATFORM_ORG_ID, OrganizationTypeEnum.PLATFORM.getType(), null);
        when(orgDataPermissionChecker.isObjectVisible(any(), any())).thenReturn(true);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    // ========== PILOT-REQ-005：平台下发（视角门 + 业务键生成 + 撞号重试） ==========

    @Test
    void distributeLead_byLeader_rejected() {
        LeadDistributeReqVO reqVO = distributeReq();
        assertServiceException(() -> appService.distributeLead(reqVO), LEAD_DISTRIBUTE_NOT_PLATFORM);
    }

    @Test
    void distributeLead_byPlatform_generatesKeyAndDelegates() {
        loginAs(PLATFORM_USER_ID);
        stubMembership(PLATFORM_USER_ID, PLATFORM_ORG_ID);
        when(leadService.distribute(any(DistributeCmd.class))).thenReturn(2048L);

        assertThat(appService.distributeLead(distributeReq())).isEqualTo(2048L);

        ArgumentCaptor<DistributeCmd> captor = ArgumentCaptor.forClass(DistributeCmd.class);
        verify(leadService).distribute(captor.capture());
        DistributeCmd cmd = captor.getValue();
        assertThat(cmd.leadKey()).startsWith("LC").contains("-").hasSize("LCyyyyMMdd-XXXXXXXX".length());
        assertThat(cmd.orgId()).isEqualTo(FRANCHISEE_ORG_ID);
        assertThat(cmd.customerName()).isEqualTo("李四");
        assertThat(cmd.actorId()).isEqualTo(String.valueOf(PLATFORM_USER_ID));
    }

    @Test
    void distributeLead_keyConflict_regeneratesAndRetries() {
        loginAs(PLATFORM_USER_ID);
        stubMembership(PLATFORM_USER_ID, PLATFORM_ORG_ID);
        when(leadService.distribute(any(DistributeCmd.class)))
                .thenThrow(ServiceExceptionUtil.exception(LEAD_KEY_CONFLICT, "LC"))
                .thenReturn(2048L);

        assertThat(appService.distributeLead(distributeReq())).isEqualTo(2048L);
        verify(leadService, times(2)).distribute(any(DistributeCmd.class));
    }

    // ========== PILOT-REQ-006：分配 / 领取 / 改派（对象级负责人门） ==========

    @Test
    void assignLead_byLeaderOfLeadOrg_allowed() {
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, null));
        LeadAssignReqVO reqVO = new LeadAssignReqVO();
        reqVO.setId(LEAD_ID);
        reqVO.setAssigneeUserId(EMPLOYEE_USER_ID);
        reqVO.setExpectedVersion(0L);

        appService.assignLead(reqVO);

        verify(leadService).assign(LEAD_ID, EMPLOYEE_USER_ID, 0L, String.valueOf(LEADER_USER_ID));
    }

    @Test
    void assignLead_byLeaderOfOtherOrg_rejected() {
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(OTHER_ORG_ID, null));
        LeadAssignReqVO reqVO = new LeadAssignReqVO();
        reqVO.setId(LEAD_ID);
        reqVO.setAssigneeUserId(EMPLOYEE_USER_ID);
        reqVO.setExpectedVersion(0L);
        assertServiceException(() -> appService.assignLead(reqVO), LEAD_ACTOR_NOT_LEADER);
    }

    @Test
    void reassignLead_byEmployee_rejected() {
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, LEADER_USER_ID));
        LeadReassignReqVO reqVO = new LeadReassignReqVO();
        reqVO.setId(LEAD_ID);
        reqVO.setNewAssigneeUserId(EMPLOYEE_USER_ID);
        reqVO.setExpectedVersion(1L);
        assertServiceException(() -> appService.reassignLead(reqVO), LEAD_ACTOR_NOT_LEADER);
    }

    @Test
    void claimLead_bindsLoginUserAsClaimer() {
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        LeadClaimReqVO reqVO = new LeadClaimReqVO();
        reqVO.setId(LEAD_ID);
        reqVO.setExpectedVersion(1L);

        appService.claimLead(reqVO);

        // 领取人服务端绑定登录用户：请求无法声明领取人（「只能领取分配给自己的」校验入参不可伪造）
        verify(leadService).claim(LEAD_ID, EMPLOYEE_USER_ID, 1L, String.valueOf(EMPLOYEE_USER_ID));
    }

    // ========== PILOT-REQ-007：跟进（跟进人服务端绑定） ==========

    @Test
    void followupLead_bindsLoginUserAsFollower() {
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        LeadFollowupReqVO reqVO = new LeadFollowupReqVO();
        reqVO.setLeadId(LEAD_ID);
        reqVO.setContent("电话沟通");
        reqVO.setFollowupTime(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(leadService.followup(eq(LEAD_ID), any(FollowupCmd.class))).thenReturn(9001L);

        assertThat(appService.followupLead(reqVO)).isEqualTo(9001L);

        ArgumentCaptor<FollowupCmd> captor = ArgumentCaptor.forClass(FollowupCmd.class);
        verify(leadService).followup(eq(LEAD_ID), captor.capture());
        assertThat(captor.getValue().followupUserId()).isEqualTo(EMPLOYEE_USER_ID);
        assertThat(captor.getValue().content()).isEqualTo("电话沟通");
    }

    // ========== PILOT-REQ-008：转商机 / 无效关闭（对象级发起人门） ==========

    @Test
    void convertLead_byAssignee_generatesOppKeyAndDelegates() {
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, EMPLOYEE_USER_ID));
        when(leadService.convertToOpportunity(eq(LEAD_ID), any(), eq(2L), any())).thenReturn(4001L);
        LeadConvertReqVO reqVO = new LeadConvertReqVO();
        reqVO.setLeadId(LEAD_ID);
        reqVO.setExpectedVersion(2L);

        assertThat(appService.convertLead(reqVO)).isEqualTo(4001L);

        ArgumentCaptor<String> oppKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(leadService).convertToOpportunity(eq(LEAD_ID), oppKeyCaptor.capture(), eq(2L),
                eq(String.valueOf(EMPLOYEE_USER_ID)));
        assertThat(oppKeyCaptor.getValue()).startsWith("OP").contains("-");
    }

    @Test
    void convertLead_byNonAssignee_rejected() {
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, LEADER_USER_ID));
        LeadConvertReqVO reqVO = new LeadConvertReqVO();
        reqVO.setLeadId(LEAD_ID);
        reqVO.setExpectedVersion(2L);
        assertServiceException(() -> appService.convertLead(reqVO), LEAD_CONVERT_NOT_ASSIGNEE);
    }

    @Test
    void invalidateLead_byAssigneeOrLeader_allowed_byOtherEmployee_rejected() {
        // 被分配员工本人可发起
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, EMPLOYEE_USER_ID));
        appService.invalidateLead(invalidateReq());
        verify(leadService).invalidate(eq(LEAD_ID), eq("UNREACHABLE"), isNull(), eq(2L),
                eq(String.valueOf(EMPLOYEE_USER_ID)));

        // 负责人可代操作
        loginAs(LEADER_USER_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, EMPLOYEE_USER_ID));
        appService.invalidateLead(invalidateReq());

        // 无关成员（非被分配人也非负责人）拒绝
        loginAs(EMPLOYEE_USER_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, LEADER_USER_ID));
        assertServiceException(() -> appService.invalidateLead(invalidateReq()), LEAD_ACTOR_NOT_LEADER);
    }

    // ========== PILOT-REQ-009：三视角查询 + D-12 出口裁剪 ==========

    @Test
    void getLeadPage_scopesByCallerView() {
        // 平台：全域（org/assignee 双空）
        loginAs(PLATFORM_USER_ID);
        stubMembership(PLATFORM_USER_ID, PLATFORM_ORG_ID);
        when(leadService.page(isNull(), isNull(), isNull(), eq(1L), eq(10L)))
                .thenReturn(new PageResult<>(List.of(leadRow(FRANCHISEE_ORG_ID, null)), 1L));
        PageResult<LeadRespVO> platformPage = appService.getLeadPage(new LeadPageReqVO());
        assertThat(platformPage.getTotal()).isEqualTo(1L);
        verify(leadService).page(isNull(), isNull(), isNull(), eq(1L), eq(10L));

        // 负责人：钉本组织
        loginAs(LEADER_USER_ID);
        when(leadService.page(eq(FRANCHISEE_ORG_ID), isNull(), isNull(), eq(1L), eq(10L)))
                .thenReturn(PageResult.empty());
        appService.getLeadPage(new LeadPageReqVO());
        verify(leadService).page(eq(FRANCHISEE_ORG_ID), isNull(), isNull(), eq(1L), eq(10L));

        // 员工：钉本人
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.page(isNull(), eq(EMPLOYEE_USER_ID), isNull(), eq(1L), eq(10L)))
                .thenReturn(PageResult.empty());
        appService.getLeadPage(new LeadPageReqVO());
        verify(leadService).page(isNull(), eq(EMPLOYEE_USER_ID), isNull(), eq(1L), eq(10L));
    }

    @Test
    void getLead_employeeSeesF2Masked_leaderSeesF2Plain() {
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, EMPLOYEE_USER_ID));

        // 员工（可读上限 F1，低于 F2）：F2 字段脱敏尾四位（D-12 §5.2），F1 不受影响
        when(fieldLevelScopeResolver.resolveMaxLevel(FRANCHISEE_ORG_ID)).thenReturn(FieldLevel.F1);
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        LeadRespVO employeeView = appService.getLead(LEAD_ID);
        assertThat(employeeView.getCustomerPhone()).isEqualTo("*******0000");
        assertThat(employeeView.getCustomerWechat()).isEqualTo("***i_wx");
        assertThat(employeeView.getCustomerAddress()).startsWith("*").endsWith("88号");
        assertThat(employeeView.getCustomerName()).isEqualTo("李四");
        assertThat(employeeView.getSource()).isEqualTo("展会获客");

        // 负责人（可读上限 F2）：F2 清晰可见
        when(fieldLevelScopeResolver.resolveMaxLevel(FRANCHISEE_ORG_ID)).thenReturn(FieldLevel.F2);
        LeadRespVO leaderView = appService.getLead(LEAD_ID);
        assertThat(leaderView.getCustomerPhone()).isEqualTo("13800000000");
        assertThat(leaderView.getCustomerWechat()).isEqualTo("lisi_wx");
        assertThat(leaderView.getCustomerAddress()).isEqualTo("上海市静安区××路88号");
    }

    @Test
    void getLead_crossScopeVisibilityDenied() {
        when(fieldLevelScopeResolver.resolveMaxLevel(any())).thenReturn(FieldLevel.F2);

        // 负责人看他组织线索：显式拒绝（PILOT-REQ-009 对象 ID 越权被拒绝）
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(OTHER_ORG_ID, EMPLOYEE_USER_ID));
        assertServiceException(() -> appService.getLead(LEAD_ID), LEAD_VISIBLE_DENIED);

        // 员工看非本人线索：显式拒绝
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        when(leadService.getLead(LEAD_ID)).thenReturn(leadRow(FRANCHISEE_ORG_ID, LEADER_USER_ID));
        assertServiceException(() -> appService.getLead(LEAD_ID), LEAD_VISIBLE_DENIED);
    }

    @Test
    void getLeadStatusMetrics_routesByCallerView() {
        when(metricsService.countByStatus(TENANT_ID)).thenReturn(Map.of("FOLLOWING", 3L));
        when(metricsService.countByStatusAndOrg(TENANT_ID, FRANCHISEE_ORG_ID)).thenReturn(Map.of("FOLLOWING", 2L));
        when(metricsService.countByStatusAndAssignee(TENANT_ID, EMPLOYEE_USER_ID)).thenReturn(Map.of("FOLLOWING", 1L));

        // 负责人 → 本组织口径
        assertThat(appService.getLeadStatusMetrics()).isEqualTo(Map.of("FOLLOWING", 2L));

        // 平台 → 全域口径
        loginAs(PLATFORM_USER_ID);
        stubMembership(PLATFORM_USER_ID, PLATFORM_ORG_ID);
        assertThat(appService.getLeadStatusMetrics()).isEqualTo(Map.of("FOLLOWING", 3L));

        // 员工 → 本人口径
        loginAs(EMPLOYEE_USER_ID);
        stubMembership(EMPLOYEE_USER_ID, FRANCHISEE_ORG_ID);
        assertThat(appService.getLeadStatusMetrics()).isEqualTo(Map.of("FOLLOWING", 1L));
    }

    // ========== 视角解析 fail-closed ==========

    @Test
    void caller_withoutActiveMembership_rejected() {
        when(membershipService.getPrimaryMembership(anyLong())).thenReturn(null);
        assertServiceException(() -> appService.getLeadPage(new LeadPageReqVO()), LEAD_ACTOR_QUALIFICATION_DENIED);
    }

    @Test
    void caller_ofNonFranchiseeOrg_rejected() {
        stubOrganization(FRANCHISEE_ORG_ID, OrganizationTypeEnum.DEPARTMENT.getType(), LEADER_USER_ID);
        assertServiceException(() -> appService.getLeadPage(new LeadPageReqVO()), LEAD_ACTOR_QUALIFICATION_DENIED);
    }

    // ========== 夹具 ==========

    private static LeadDistributeReqVO distributeReq() {
        LeadDistributeReqVO reqVO = new LeadDistributeReqVO();
        reqVO.setCustomerName("李四");
        reqVO.setCustomerPhone("13800000000");
        reqVO.setOrgId(FRANCHISEE_ORG_ID);
        return reqVO;
    }

    private static LeadInvalidateReqVO invalidateReq() {
        LeadInvalidateReqVO reqVO = new LeadInvalidateReqVO();
        reqVO.setLeadId(LEAD_ID);
        reqVO.setReasonName("UNREACHABLE");
        reqVO.setExpectedVersion(2L);
        return reqVO;
    }

    private static Map<String, Object> leadRow(Long orgId, Long assigneeUserId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", LEAD_ID);
        row.put("lead_key", "LC20261001-ABCD1234");
        row.put("customer_name", "李四");
        row.put("customer_phone", "13800000000");
        row.put("customer_wechat", "lisi_wx");
        row.put("customer_address", "上海市静安区××路88号");
        row.put("source", "展会获客");
        row.put("org_id", orgId);
        row.put("assignee_user_id", assigneeUserId);
        row.put("status", "FOLLOWING");
        row.put("version", 2L);
        return row;
    }

    private static MembershipDO membershipOf(Long orgId) {
        MembershipDO membership = new MembershipDO();
        membership.setOrganizationId(orgId);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        return membership;
    }

    private void stubMembership(Long userId, Long orgId) {
        when(membershipService.getPrimaryMembership(userId)).thenReturn(membershipOf(orgId));
    }

    private void stubOrganization(Long orgId, Integer type, Long leaderUserId) {
        OrganizationDO organization = new OrganizationDO();
        organization.setId(orgId);
        organization.setType(type);
        organization.setLeaderUserId(leaderUserId);
        when(organizationService.getOrganization(orgId)).thenReturn(organization);
    }

    private static void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

}
