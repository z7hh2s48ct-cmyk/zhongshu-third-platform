package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.redis.config.ZszjCacheAutoConfiguration;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.permission.PermissionApiImpl;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.membership.MembershipContextResolver;
import cn.zszj.module.system.service.membership.MembershipServiceImpl;
import cn.zszj.module.system.service.notify.landing.NotifyLandingClient;
import cn.zszj.module.system.service.notify.landing.NotifyLandingDescriptor;
import cn.zszj.module.system.service.notify.landing.NotifyLandingProvider;
import cn.zszj.module.system.service.notify.landing.NotifyLandingRegistry;
import cn.zszj.module.system.service.notify.landing.NotifyLandingService;
import cn.zszj.module.system.service.notify.landing.NotifyLandingServiceImpl;
import cn.zszj.module.system.service.notify.landing.NotifyLandingUnavailable;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.organization.OrganizationServiceImpl;
import cn.zszj.module.system.service.permission.MenuService;
import cn.zszj.module.system.service.permission.OrgDataScopeResolver;
import cn.zszj.module.system.service.permission.PermissionServiceImpl;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-MSG-003.C：业务组织/任职与消息可见范围（org 轴）端到端测试。
 *
 * <p>真实 H2 + 真实任职生命周期流转 + 真实范围解析（{@code PermissionCommonApi} → {@code OrgDataScopeResolver}）
 * + 真实对象门（{@link NotifyMessageOrgAuthorizer} → 产线同构的 {@link OrgDataPermissionChecker}）。锁定主卡
 * docs/05 line710「覆盖消息列表/正文/管理查询的范围」与 line711 验收「组织/权限变化后旧消息不得越界可见」：
 * <ol>
 *   <li><b>写入归属</b>：消息 organizationId = 派发时刻收件人<b>服务端组织上下文</b>（默认任职组织；
 *       无任职/离职降级为空，不阻断派发）；</li>
 *   <li><b>我的读路径</b>：我的分页 / 未读列表 / 未读计数按登录主体 org 范围过滤，任职失权即时收缩
 *       （本人无组织列历史消息仍可见，与检查器 self 兜底同口径）；</li>
 *   <li><b>管理读路径</b>：管理分页仅授权组织内消息；管理单条越界即伪装不存在（防存在性探测）；</li>
 *   <li><b>visit 上下文</b>：获批跨组织访问时读路径与落点收敛到授权 {@code targetOrgIds}
 *       （whole-tenant 授权不受限；无组织列消息在限定授权下隐藏，与
 *       {@link CrossOrgVisitScopeHolder#isObjectAllowed(Long)} 同口径）；</li>
 *   <li><b>落点二次授权</b>：组织归属越界（含离职后的旧组织消息）→ REVOKED，旧消息不得借落点进入详情。</li>
 * </ol>
 *
 * <p>边界：org 轴 SQL 静默过滤（框架级 {@code OrgDataPermissionRule} 注册）与 DataPermissionRuleHandler
 * 在 visit 上下文的收敛不在本卡（随 ZS-CLIENT-002.B 或后续）；跨技术租户行级隔离由生产 tenant 拦截器提供，
 * H2 上下文不含该拦截器（循 MSG-003 既有边界登记）；真实 PG/HTTP 联验随 B08 批次收口。
 *
 * @author ZS-MSG-003.C
 */
@Import({ZszjCacheAutoConfiguration.class, PermissionServiceImpl.class, PermissionApiImpl.class, OrgDataScopeResolver.class,
        MembershipServiceImpl.class, MembershipContextResolver.class, OrganizationServiceImpl.class,
        NotifyMessageServiceImpl.class, NotifyLandingServiceImpl.class, NotifyMessageOrgAuthorizer.class,
        NotifyMessageOrgVisibilityTest.FixtureConfig.class})
@TestPropertySource(properties = "spring.main.allow-circular-references=true") // 与生产 zszj-server 一致（Role↔Permission 循环依赖）
public class NotifyMessageOrgVisibilityTest extends BaseDbAndRedisUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Integer ADMIN_TYPE = UserTypeEnum.ADMIN.getValue();

    @Resource
    private NotifyMessageServiceImpl notifyMessageService;
    @Resource
    private NotifyLandingService notifyLandingService;
    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private MembershipServiceImpl membershipService;
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    /** 经真实门面断言范围夹具生效（失败时区分「夹具错」与「被测逻辑错」，循 PERM-004.B 先例）。 */
    @Resource
    private PermissionCommonApi permissionApi;

    /**
     * MembershipServiceImpl 失权联动依赖 OAuth2TokenService（@Lazy）；本类 mock 之以满足上下文装配
     * （org 轴可见范围不依赖会话撤销，mock 不影响断言）。
     */
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;
    /** PermissionServiceImpl 装配所需的协作者（org 轴 getOrgDataPermission 路径不实际调用它们）。 */
    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService adminUserService;

    @BeforeEach
    public void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDownContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    // ========== A. 写入归属：派发时刻收件人服务端组织上下文 ==========

    @Test // 有默认任职（组织启用）→ 消息落收件人组织归属
    public void testCreateNotifyMessage_withActivePrimaryMembership_recordsRecipientOrg() {
        Long userId = insertUser();
        insertOrg(3000L);
        createPrimaryMembership(userId, 3000L);

        Long messageId = createNotifyMessage(userId);

        assertEquals(3000L, notifyMessageMapper.selectById(messageId).getOrganizationId());
    }

    @Test // 无默认任职（降级空上下文）→ org 归属为空，不阻断消息创建
    public void testCreateNotifyMessage_withoutMembership_recordsNullOrg() {
        Long userId = insertUser();

        Long messageId = createNotifyMessage(userId);

        assertNull(notifyMessageMapper.selectById(messageId).getOrganizationId());
    }

    @Test // 离职（任职非在职）→ 服务端组织上下文拒绝，org 归属降级为空（不阻断派发）
    public void testCreateNotifyMessage_afterTermination_recordsNullOrg() {
        Long userId = insertUser();
        insertOrg(3001L);
        Long membershipId = createPrimaryMembership(userId, 3001L);
        membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        Long messageId = createNotifyMessage(userId);

        assertNull(notifyMessageMapper.selectById(messageId).getOrganizationId());
    }

    @Test // MEMBER 收件人（会员命名空间，与管理员编号空间独立）→ 归属恒空，防同号管理员错配
    public void testCreateNotifyMessage_memberRecipient_recordsNullOrg() {
        // 构造「同号」管理员 + 任职：若归属解析误用会员编号查任职，将错配到该管理员组织
        Long sharedId = insertUser();
        insertOrg(3002L);
        createPrimaryMembership(sharedId, 3002L);

        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class);
        Long messageId = notifyMessageService.createNotifyMessage(sharedId, UserTypeEnum.MEMBER.getValue(),
                template, randomString(), Map.of());

        assertNull(notifyMessageMapper.selectById(messageId).getOrganizationId());
    }

    // ========== B. 我的读路径：org 轴可见范围过滤 ==========

    @Test // 任职组织=A：我的分页/未读列表/未读计数仅含 A 组织消息 + 本人无组织列消息
    public void testMyReadPaths_filterByOrgScope_andKeepOwnNullOrgMessages() {
        Long userId = insertUser();
        insertOrg(3100L);
        insertOrg(3200L);
        createPrimaryMembership(userId, 3100L);
        NotifyMessageDO inScope = insertMessage(userId, 3100L, null);
        insertMessage(userId, 3200L, null); // 他组织（同租户）消息：不在授权范围
        NotifyMessageDO nullOrg = insertMessage(userId, null, null);

        loginAs(userId, ADMIN_TYPE);
        // 夹具自检：范围 = A 组织子树
        assertEquals(Set.of(3100L), permissionApi.getOrgDataPermission(userId).getOrgIds());

        PageResult<NotifyMessageDO> page = notifyMessageService.getMyMyNotifyMessagePage(
                new NotifyMessageMyPageReqVO(), userId, ADMIN_TYPE);
        assertEquals(2, page.getTotal());
        assertEquals(Set.of(inScope.getId(), nullOrg.getId()), ids(page.getList()));

        List<NotifyMessageDO> unread = notifyMessageService.getUnreadNotifyMessageList(userId, ADMIN_TYPE, 10);
        assertEquals(2, unread.size());
        assertEquals(Set.of(inScope.getId(), nullOrg.getId()), ids(unread));

        assertEquals(2L, notifyMessageService.getUnreadNotifyMessageCount(userId, ADMIN_TYPE));
    }

    @Test // 离职后范围即时收敛：旧组织消息不可见，仅剩本人无组织列历史消息（新请求 = 无陈旧缓存）
    public void testMyReadPaths_shrinkImmediatelyAfterTermination() {
        Long userId = insertUser();
        insertOrg(3101L);
        insertOrg(3201L);
        Long membershipId = createPrimaryMembership(userId, 3101L);
        NotifyMessageDO nullOrg = insertMessage(userId, null, null);
        insertMessage(userId, 3101L, null);
        insertMessage(userId, 3201L, null);

        loginAs(userId, ADMIN_TYPE);
        // 变更前：3101 在范围（+本人无组织列消息），3201 不可见
        assertEquals(2, notifyMessageService.getMyMyNotifyMessagePage(
                new NotifyMessageMyPageReqVO(), userId, ADMIN_TYPE).getTotal());

        membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        loginAs(userId, ADMIN_TYPE); // 新请求：全新 LoginUser，范围实时重派生
        OrgDataPermissionRespDTO after = permissionApi.getOrgDataPermission(userId);
        assertTrue(after.getOrgIds().isEmpty());
        PageResult<NotifyMessageDO> page = notifyMessageService.getMyMyNotifyMessagePage(
                new NotifyMessageMyPageReqVO(), userId, ADMIN_TYPE);
        assertEquals(1, page.getTotal());
        assertEquals(Set.of(nullOrg.getId()), ids(page.getList()));
        assertEquals(1, notifyMessageService.getUnreadNotifyMessageList(userId, ADMIN_TYPE, 10).size());
        assertEquals(1L, notifyMessageService.getUnreadNotifyMessageCount(userId, ADMIN_TYPE));
    }

    // ========== C. 管理读路径：列表过滤 + 单条正文门 ==========

    @Test // 管理分页仅授权组织内消息；管理单条越界/他人无组织列消息伪装不存在
    public void testAdminPageAndSingleGet_respectOrgScope() {
        Long operator = insertUser();
        insertOrg(3300L);
        insertOrg(3400L);
        createPrimaryMembership(operator, 3300L);
        Long otherUser = insertUser();
        NotifyMessageDO inScope = insertMessage(otherUser, 3300L, null);
        NotifyMessageDO outOfScope = insertMessage(otherUser, 3400L, null);
        NotifyMessageDO otherNullOrg = insertMessage(otherUser, null, null);
        NotifyMessageDO ownNullOrg = insertMessage(operator, null, null);

        loginAs(operator, ADMIN_TYPE);

        // 管理分页：仅 3300 组织消息（他人无组织列消息不出现在管理视图，fail-closed）
        NotifyMessagePageReqVO reqVO = new NotifyMessagePageReqVO();
        PageResult<NotifyMessageDO> page = notifyMessageService.getNotifyMessagePage(reqVO);
        assertEquals(1, page.getTotal());
        assertEquals(inScope.getId(), page.getList().get(0).getId());

        // 管理单条（正文）：范围内可见；范围外 / 他人无组织列 → 伪装不存在（防存在性探测）
        assertEquals(inScope.getId(), notifyMessageService.getNotifyMessage(inScope.getId()).getId());
        assertNull(notifyMessageService.getNotifyMessage(outOfScope.getId()));
        assertNull(notifyMessageService.getNotifyMessage(otherNullOrg.getId()));
        // 本人无组织列消息：检查器 self 兜底（与单条全口径一致，管理分页不含属有意收敛）
        assertNotNull(notifyMessageService.getNotifyMessage(ownNullOrg.getId()));
    }

    // ========== D. visit 上下文（获批跨组织访问） ==========

    @Test // visit 限定目标组织：读路径收敛到授权组织；无组织列消息一并隐藏（与 isObjectAllowed 同口径）
    public void testVisitScope_limitsReadPathsToTargetOrgs() {
        Long visitor = insertUser();
        NotifyMessageDO target = insertMessage(visitor, 4000L, null);
        insertMessage(visitor, 4100L, null);
        insertMessage(visitor, null, null);

        loginAs(visitor, ADMIN_TYPE);
        CrossOrgVisitScopeHolder.setScope(authorizedVisit(Set.of(4000L)));

        PageResult<NotifyMessageDO> page = notifyMessageService.getMyMyNotifyMessagePage(
                new NotifyMessageMyPageReqVO(), visitor, ADMIN_TYPE);
        assertEquals(1, page.getTotal());
        assertEquals(Set.of(target.getId()), ids(page.getList()));
        assertEquals(1, notifyMessageService.getUnreadNotifyMessageList(visitor, ADMIN_TYPE, 10).size());
        assertEquals(1L, notifyMessageService.getUnreadNotifyMessageCount(visitor, ADMIN_TYPE));
    }

    @Test // visit whole-tenant 授权（targetOrgIds=null）：读路径不受限
    public void testVisitScope_wholeTenantAllowsAll() {
        Long visitor = insertUser();
        insertMessage(visitor, 4200L, null);
        insertMessage(visitor, 4300L, null);
        insertMessage(visitor, null, null);

        loginAs(visitor, ADMIN_TYPE);
        CrossOrgVisitScopeHolder.setScope(authorizedVisit(null));

        assertEquals(3, notifyMessageService.getMyMyNotifyMessagePage(
                new NotifyMessageMyPageReqVO(), visitor, ADMIN_TYPE).getTotal());
    }

    @Test // 落点：visit 目标组织内消息 → org 门放行（正常进入后续注册/授权链，available）
    public void testLanding_visitScopeWithinTargetOrg_available() {
        Long visitor = insertUser();
        NotifyMessageDO message = insertMessage(visitor, 4000L, FixtureConfig.FixtureLandingProvider.TEMPLATE_CODE);

        loginAs(visitor, ADMIN_TYPE);
        CrossOrgVisitScopeHolder.setScope(authorizedVisit(Set.of(4000L)));

        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), visitor, ADMIN_TYPE, NotifyLandingClient.WEB);
        assertTrue(result.getAvailable());
        assertNull(result.getUnavailableCode());
    }

    @Test // 落点：visit 目标组织外消息 → REVOKED（旧消息不得借落点进入详情/附件）
    public void testLanding_visitScopeOutOfTargetOrg_revoked() {
        Long visitor = insertUser();
        NotifyMessageDO message = insertMessage(visitor, 5000L, FixtureConfig.FixtureLandingProvider.TEMPLATE_CODE);

        loginAs(visitor, ADMIN_TYPE);
        CrossOrgVisitScopeHolder.setScope(authorizedVisit(Set.of(4000L)));

        NotifyMessageLandingRespVO result = notifyLandingService.resolveMessageLanding(
                message.getId(), visitor, ADMIN_TYPE, NotifyLandingClient.WEB);
        assertFalse(result.getAvailable());
        assertEquals(NotifyLandingUnavailable.REVOKED.name(), result.getUnavailableCode());
        assertNull(result.getDescriptor());
    }

    @Test // 落点（非 visit）：离职后原组织消息 → REVOKED；本人无组织列消息仍可用
    public void testLanding_afterTermination_revoked_andOwnNullOrgAvailable() {
        Long userId = insertUser();
        insertOrg(3600L);
        Long membershipId = createPrimaryMembership(userId, 3600L);
        String templateCode = FixtureConfig.FixtureLandingProvider.TEMPLATE_CODE;
        NotifyMessageDO oldOrgMessage = insertMessage(userId, 3600L, templateCode);
        NotifyMessageDO nullOrgMessage = insertMessage(userId, null, templateCode);

        // 离职前：在职组织消息可正常进入落点
        loginAs(userId, ADMIN_TYPE);
        assertTrue(notifyLandingService.resolveMessageLanding(
                oldOrgMessage.getId(), userId, ADMIN_TYPE, NotifyLandingClient.WEB).getAvailable());

        membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        loginAs(userId, ADMIN_TYPE); // 新请求：org 范围实时收缩（ORG_SELF）
        NotifyMessageLandingRespVO revoked = notifyLandingService.resolveMessageLanding(
                oldOrgMessage.getId(), userId, ADMIN_TYPE, NotifyLandingClient.WEB);
        assertFalse(revoked.getAvailable());
        assertEquals(NotifyLandingUnavailable.REVOKED.name(), revoked.getUnavailableCode());
        // 无组织列消息：本人兜底，不在 org 门拒绝范围
        assertTrue(notifyLandingService.resolveMessageLanding(
                nullOrgMessage.getId(), userId, ADMIN_TYPE, NotifyLandingClient.WEB).getAvailable());
    }

    // ========== E. 护栏：无登录 / 非 ADMIN 不施加 org 限制（与检查器护栏同口径） ==========

    @Test // 无登录上下文（系统内部/定时任务）：管理分页不受 org 过滤
    public void testAdminPage_withoutLogin_noOrgFiltering() {
        Long otherUser = insertUser();
        insertMessage(otherUser, 3700L, null);
        insertMessage(otherUser, 3800L, null);
        insertMessage(otherUser, null, null);

        assertEquals(3, notifyMessageService.getNotifyMessagePage(new NotifyMessagePageReqVO()).getTotal());
    }

    @Test // 非 ADMIN 主体（MEMBER）：不施加 org 限制（MEMBER 对象授权属会员模块另一轴）
    public void testAdminPage_nonAdminUser_noOrgFiltering() {
        Long member = insertUser();
        Long otherUser = insertUser();
        insertMessage(otherUser, 3900L, null);
        insertMessage(otherUser, 3901L, null);

        loginAs(member, UserTypeEnum.MEMBER.getValue());

        assertEquals(2, notifyMessageService.getNotifyMessagePage(new NotifyMessagePageReqVO()).getTotal());
    }

    @Test // ADMIN 无有效任职（orgIds 空）→ 管理分页 fail-closed 恒假（0 条）且不抛异常
    public void testAdminPage_withoutMembership_failClosedEmpty() {
        Long operator = insertUser();
        Long otherUser = insertUser();
        insertMessage(otherUser, 3500L, null);
        insertMessage(otherUser, null, null);

        loginAs(operator, ADMIN_TYPE);
        // 夹具自检：无任职 → 范围为空（self 兜底仅作用于无组织列对象的单条门）
        assertTrue(permissionApi.getOrgDataPermission(operator).getOrgIds().isEmpty());

        assertEquals(0, notifyMessageService.getNotifyMessagePage(new NotifyMessagePageReqVO()).getTotal());
    }

    // ========== 夹具 ==========

    private Long insertUser() {
        Long userId = randomLongId();
        adminUserMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setSex(randomEle(SexEnum.values()).getSex());
        }));
        return userId;
    }

    private void insertOrg(Long id) {
        organizationMapper.insert(randomPojo(OrganizationDO.class, o -> {
            o.setId(id);
            o.setType(OrganizationTypeEnum.STORE.getType());
            o.setParentId(OrganizationDO.PARENT_ID_ROOT);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setRefDeptId(null);
        }));
    }

    private Long createPrimaryMembership(Long userId, Long orgId) {
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setId(null);
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(null);   // 交给 Service 默认 ACTIVE
            o.setIsPrimary(1);   // 显式默认任职
            o.setValidFrom(null);
            o.setValidTo(null);
        });
        return membershipService.createMembership(membership, userId);
    }

    private Long createNotifyMessage(Long userId) {
        NotifyTemplateDO template = randomPojo(NotifyTemplateDO.class);
        return notifyMessageService.createNotifyMessage(userId, ADMIN_TYPE, template, randomString(), Map.of());
    }

    private NotifyMessageDO insertMessage(Long userId, Long organizationId, String templateCode) {
        NotifyMessageDO message = randomPojo(NotifyMessageDO.class, o -> {
            o.setUserId(userId);
            o.setUserType(ADMIN_TYPE);
            o.setOrganizationId(organizationId);
            o.setReadStatus(false);
            o.setTemplateParams(Map.of("bizId", 1024));
        });
        if (templateCode != null) {
            message.setTemplateCode(templateCode);
        }
        notifyMessageMapper.insert(message);
        return message;
    }

    /** 模拟一次真实请求的登录主体（循 SEC-001.B CrossOrgVisitScopeHolderTest 先例；重复调用 = 新请求）。 */
    private void loginAs(Long userId, Integer userType) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(userType);
        loginUser.setTenantId(TENANT_ID);
        SecurityFrameworkUtils.setLoginUser(loginUser, new MockHttpServletRequest());
    }

    private CrossOrgVisitDecisionDTO authorizedVisit(Set<Long> targetOrgIds) {
        CrossOrgVisitDecisionDTO decision = new CrossOrgVisitDecisionDTO();
        decision.setAuthorized(true);
        decision.setReason("AUTHORIZED");
        decision.setTargetTenantId(TENANT_ID);
        decision.setTargetOrgIds(targetOrgIds);
        decision.setAllowedActions(Set.of("system:notify-message:query"));
        decision.setAllowedFields(Set.of());
        return decision;
    }

    private static Set<Long> ids(List<NotifyMessageDO> messages) {
        return messages.stream().map(NotifyMessageDO::getId).collect(Collectors.toSet());
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class FixtureConfig {

        /** 与产线 ZszjDeptDataPermissionAutoConfiguration 装配同构：真实检查器 + 真实门面。 */
        @Bean
        public OrgDataPermissionChecker orgDataPermissionChecker(PermissionCommonApi permissionApi) {
            return new OrgDataPermissionChecker(permissionApi);
        }

        /** 落点可用夹具：org 门放行后应得到 available 描述（用于区分「org 门拒绝」与「后续链路拒绝」）。 */
        public static class FixtureLandingProvider implements NotifyLandingProvider {

            static final String TEMPLATE_CODE = "zs_msg_org_visibility_fixture";

            @Override
            public String templateCode() {
                return TEMPLATE_CODE;
            }

            @Override
            public String module() {
                return "system";
            }

            @Override
            public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
                // 夹具：业务重读授权通过
            }

            @Override
            public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
                return NotifyLandingDescriptor.builder().module(module()).route("/system/msg-org-fixture").build();
            }

        }

        @Bean
        public NotifyLandingRegistry notifyLandingRegistry() {
            return new NotifyLandingRegistry(List.of(new FixtureLandingProvider()));
        }

    }

}
