package cn.zszj.module.firstchain.framework;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.service.lead.FirstchainLeadAppService;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.notify.landing.NotifyLandingClient;
import cn.zszj.module.system.service.organization.OrganizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.util.Map;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_VISIBLE_DENIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainNotifyLandingProvider} 的单元测试（ZS-FC-003 服务端接线 wave）——MSG-003 硬合同。
 *
 * <p>覆盖：authorize 重新授权硬合同（申请域经真实 H2 JDBC 面验证「提交人本人/PLATFORM 任职放行、
 * 无关人拒绝」；线索域复用三视角可见性，越权异常原样上抛 → 解析判 REVOKED）；resolve 结构化落点
 * （WEB 返回模块/路由/业务 ID；MOBILE 未注册判 CLIENT_UNSUPPORTED）；畸形 leadId fail-closed 拒绝。
 *
 * @author ZS-FC-003
 */
class FirstchainNotifyLandingProviderTest {

    private static final Long TENANT_ID = 1L;

    private static final Long SUBMITTER_ID = 100L;

    private static final Long PLATFORM_USER_ID = 1L;

    private static final Long OTHER_USER_ID = 500L;

    private static final String APP_KEY = "FC20261001-ABCD1234";

    private DataSource dataSource;

    private MembershipService membershipService;

    private OrganizationService organizationService;

    private FirstchainLeadAppService leadAppService;

    private FirstchainNotifyLandingProvider applicationProvider;

    private FirstchainNotifyLandingProvider leadProvider;

    @BeforeEach
    void setUp() throws Exception {
        // 真实 H2 JDBC 面（申请域授权查询循生产 SQL 形状，不自造桩语义）
        org.springframework.jdbc.datasource.SimpleDriverDataSource h2DataSource =
                new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                        new org.h2.Driver(), "jdbc:h2:mem:firstchain_landing;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa", "");
        dataSource = h2DataSource;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(h2DataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS bpm_first_chain_application");
        jdbcTemplate.execute("CREATE TABLE bpm_first_chain_application (id bigint primary key, app_key varchar(64), "
                + "applicant_name varchar(128), contact_name varchar(64), status varchar(32), version bigint, "
                + "creator varchar(64), tenant_id bigint, deleted boolean default false)");
        jdbcTemplate.update("INSERT INTO bpm_first_chain_application (id, app_key, applicant_name, contact_name, "
                + "status, version, creator, tenant_id) VALUES (1024, ?, '众墅家装联盟（华东）', '张三', 'APPROVED', 2, ?, ?)",
                APP_KEY, String.valueOf(SUBMITTER_ID), TENANT_ID);

        membershipService = mock(MembershipService.class);
        organizationService = mock(OrganizationService.class);
        leadAppService = mock(FirstchainLeadAppService.class);
        applicationProvider = new FirstchainNotifyLandingProvider(
                FirstchainNotifyTemplates.APPLICATION_APPROVED,
                FirstchainNotifyLandingProvider.Kind.APPLICATION, dataSource, membershipService,
                organizationService, leadAppService);
        leadProvider = new FirstchainNotifyLandingProvider(
                FirstchainNotifyTemplates.LEAD_ASSIGNED,
                FirstchainNotifyLandingProvider.Kind.LEAD, dataSource, membershipService,
                organizationService, leadAppService);
        TenantContextHolder.setTenantId(TENANT_ID);
        loginAs(SUBMITTER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    // ========== authorize：申请域（提交人 / 平台任职 / 无关人） ==========

    @Test
    void authorizeApplication_bySubmitter_allowed() {
        assertThatCode(() -> applicationProvider.authorize(message(), Map.of("appKey", APP_KEY)))
                .doesNotThrowAnyException();
    }

    @Test
    void authorizeApplication_byPlatformMembership_allowed() {
        loginAs(PLATFORM_USER_ID);
        stubPlatformMembership(PLATFORM_USER_ID);

        assertThatCode(() -> applicationProvider.authorize(message(), Map.of("appKey", APP_KEY)))
                .doesNotThrowAnyException();
    }

    @Test
    void authorizeApplication_byUnrelatedUser_denied() {
        loginAs(OTHER_USER_ID);

        assertServiceException(() -> applicationProvider.authorize(message(), Map.of("appKey", APP_KEY)),
                FIRSTCHAIN_APPLICATION_NOT_EXISTS);
    }

    @Test
    void authorizeApplication_crossTenantOrMissing_denied() {
        assertServiceException(() -> applicationProvider.authorize(message(), Map.of("appKey", "FC99999999-ZZ")),
                FIRSTCHAIN_APPLICATION_NOT_EXISTS);
    }

    // ========== authorize：线索域（复用三视角可见性） ==========

    @Test
    void authorizeLead_delegatesToLeadVisibility() {
        Map<String, Object> params = Map.of("leadId", "2048");

        // 三视角可见性复用：越权异常原样上抛（解析层判 REVOKED）；放行路径随后恢复（连续打桩）
        when(leadAppService.getLead(2048L))
                .thenThrow(cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception(
                        LEAD_VISIBLE_DENIED))
                .thenReturn(null);
        assertServiceException(() -> leadProvider.authorize(message(), params), LEAD_VISIBLE_DENIED);

        // 可见：委托调用即放行
        leadProvider.authorize(message(), params);
        verify(leadAppService, org.mockito.Mockito.times(2)).getLead(2048L);
    }

    @Test
    void authorizeLead_malformedLeadId_failClosed() {
        assertServiceException(() -> leadProvider.authorize(message(), Map.of("leadId", "not-a-number")),
                LEAD_NOT_EXISTS);
        assertServiceException(() -> leadProvider.authorize(message(), Map.of()), LEAD_NOT_EXISTS);
    }

    // ========== resolve：结构化落点 ==========

    @Test
    void resolve_webReturnsStructuralDescriptor_mobilePerKind() {
        var web = leadProvider.resolve(NotifyLandingClient.WEB, Map.of("leadId", "2048"));
        assertThat(web).isNotNull();
        assertThat(web.getModule()).isEqualTo("firstchain");
        assertThat(web.getRoute()).isEqualTo("/firstchain/lead");
        assertThat(web.getParams()).containsEntry("id", "2048");

        // LEAD 域 MOBILE 落点（FC-003 前端 wave）：UniApp 分包详情页，?id= 接参
        var mobile = leadProvider.resolve(NotifyLandingClient.MOBILE, Map.of("leadId", "2048"));
        assertThat(mobile).isNotNull();
        assertThat(mobile.getRoute()).isEqualTo("/pages-firstchain/lead/detail/index");
        assertThat(mobile.getParams()).containsEntry("id", "2048");

        // APPLICATION 域无移动工作台 → CLIENT_UNSUPPORTED（五道防线既有语义）
        assertThat(applicationProvider.resolve(NotifyLandingClient.MOBILE, Map.of("appKey", APP_KEY)))
                .isNull();

        var applicationWeb = applicationProvider.resolve(NotifyLandingClient.WEB, Map.of("appKey", APP_KEY));
        assertThat(applicationWeb.getRoute()).isEqualTo("/firstchain/application");
        assertThat(applicationWeb.getParams()).containsEntry("appKey", APP_KEY);
    }

    // ========== 注册合同 ==========

    @Test
    void templateCodesAndModule_matchSeedContract() {
        assertThat(applicationProvider.templateCode()).isEqualTo("firstchain_application_approved");
        assertThat(leadProvider.templateCode()).isEqualTo("firstchain_lead_assigned");
        assertThat(applicationProvider.module()).isEqualTo(FirstchainNotifyTemplates.MODULE);
        assertThat(FirstchainNotifyTemplates.MODULE).isEqualTo("firstchain");
    }

    // ========== 夹具 ==========

    private static cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO message() {
        return new cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO();
    }

    private void stubPlatformMembership(Long userId) {
        MembershipDO membership = new MembershipDO();
        membership.setUserId(userId);
        membership.setOrganizationId(10L);
        membership.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(userId)).thenReturn(membership);
        OrganizationDO platformOrg = new OrganizationDO();
        platformOrg.setId(10L);
        platformOrg.setType(OrganizationTypeEnum.PLATFORM.getType());
        when(organizationService.getOrganization(10L)).thenReturn(platformOrg);
    }

    private static void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

}
