package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.SetUtils;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.redis.config.ZszjCacheAutoConfiguration;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.permission.PermissionApiImpl;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipHistoryMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.enums.membership.MembershipActionEnum;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.enums.permission.OrgDataScopeEnum;
import cn.zszj.module.system.service.membership.MembershipServiceImpl;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.organization.OrganizationServiceImpl;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ZS-PERM-004.B：组织任职变更后的 org 轴撤权审计一致性测试（真实 H2 + 真实生命周期流转 + 真实范围解析）。
 *
 * <p>本类填补 {@link OrgDataScopeResolverTest}（静态快照派生）、{@code MembershipServiceImplTest}（生命周期操作侧）、
 * {@code PermissionCacheConsistencyTest}（RBAC 轴缓存一致性，PERM-004.A）三者之间的 GAP：<b>任职生命周期变更
 * × org 轴数据授权的端到端一致性</b>——驱动<b>真实</b> {@link MembershipServiceImpl#changeStatus}/{@code transferMembership}
 * 流转，断言<b>真实</b> {@link OrgDataScopeResolver#resolve} 范围与 {@link OrgDataPermissionChecker} 对象级裁决的即时后果。
 *
 * <p>锁定 §16.1 退出条件「任职失效及时拒绝，恢复不自动恢复越界授权」与主卡验收「合法恢复可重新取权；审计包含
 * 操作者、目标、变更、结果与 trace」。org 轴范围每请求经 {@code getOrgDataPermission → resolve} <b>实时</b>派生
 * （无跨请求缓存），故本套件既验证及时性，也作<b>回归锁</b>——防未来给该路径引入缓存或放宽 {@code isEffective} 口径。
 *
 * <p>范围解析以 {@code superAdmin=false} 驱动（被测主体为普通组织成员）；超管/平台 → ORG_ALL 路径由
 * {@link OrgDataScopeResolverTest} 覆盖，RBAC 角色缓存一致性由 PERM-004.A 覆盖，此处不重复。
 *
 * @author ZS-PERM-004.B
 */
@Import({ZszjCacheAutoConfiguration.class, PermissionServiceImpl.class, PermissionApiImpl.class, OrgDataScopeResolver.class,
        MembershipServiceImpl.class, OrganizationServiceImpl.class})
@TestPropertySource(properties = "spring.main.allow-circular-references=true") // 与生产 zszj-server 一致（Role↔Permission 循环依赖）
public class OrgDataPermissionRevocationConsistencyTest extends BaseDbAndRedisUnitTest {

    /**
     * 经真实门面 {@link PermissionCommonApi}（{@code PermissionApiImpl} → {@link PermissionService#getOrgDataPermission}
     * → {@link OrgDataScopeResolver#resolve}）驱动 org 轴范围断言，与产线 org 轴读路径
     * （{@link OrgDataPermissionChecker} → {@code PermissionCommonApi} → …，见 ZszjDeptDataPermissionAutoConfiguration）<b>同构</b>。
     * 整条链在 {@code @EnableCaching} 代理下无 {@code @Cacheable}，故每请求实时重派生；走<b>真实门面 + 缓存代理</b>
     * 使本套件成为「门面层与服务层均不得引入跨请求缓存」的回归锁（§1.2、D2 维度4、CodeReview P1-1）。
     */
    @Resource
    private PermissionCommonApi permissionApi;
    @Resource
    private MembershipServiceImpl membershipService;
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private MembershipHistoryMapper membershipHistoryMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    /**
     * MembershipServiceImpl 失权联动依赖 OAuth2TokenService（@Lazy）；本类 mock 之以满足上下文装配。
     * org 轴数据授权的及时性<b>不依赖</b>会话撤销（会话撤销是 IAM-004 的独立层），故 mock 不影响本套件断言。
     */
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;

    /**
     * PermissionServiceImpl 装配所需的协作者（org 轴 getOrgDataPermission 路径不实际调用它们，仅为满足
     * {@code @Resource} 上下文装配）：被测主体均为无角色的普通组织成员，{@code isSuperAdminUser} 短路返回 false。
     */
    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService adminUserService;

    // ========== 夹具 ==========

    private void insertUser(Long userId) {
        adminUserMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setSex(randomEle(SexEnum.values()).getSex());
        }));
    }

    private void insertOrg(Long id, OrganizationTypeEnum type, Long parentId, CommonStatusEnum status) {
        organizationMapper.insert(randomPojo(OrganizationDO.class, o -> {
            o.setId(id);
            o.setType(type.getType());
            o.setParentId(parentId);
            o.setStatus(status.getStatus());
            o.setRefDeptId(null);
        }));
    }

    private void disableOrg(Long id) {
        OrganizationDO org = organizationMapper.selectById(id);
        org.setStatus(CommonStatusEnum.DISABLE.getStatus());
        organizationMapper.updateById(org);
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

    private Long createSecondaryMembership(Long userId, Long orgId) {
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setId(null);
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(null);
            o.setIsPrimary(0);   // 次级任职
            o.setValidFrom(null);
            o.setValidTo(null);
        });
        return membershipService.createMembership(membership, userId);
    }

    /** 构造 org 轴检查器，注入<b>真实</b>门面 {@code permissionApi}（与产线 ZszjDeptDataPermissionAutoConfiguration 装配同构）。 */
    private OrgDataPermissionChecker newChecker() {
        return new OrgDataPermissionChecker(permissionApi);
    }

    /**
     * 模拟一次新请求：全新 LoginUser（请求级 CONTEXT_KEY 缓存随之为空 → 检查器必然重新解析）。
     * 边界：仅复现 LoginUser 作用域缓存的失效；本套件同线程执行，ThreadLocal/RequestContextHolder 作用域的
     * org 范围缓存（当前不存在）不在本锁范围（CodeReview P3-3）。
     */
    private LoginUser newRequest(Long userId) {
        return randomPojo(LoginUser.class, o -> o.setId(userId).setUserType(UserTypeEnum.ADMIN.getValue()));
    }

    // ========== 维度1：任职失效及时拒绝 ==========

    @Test // 默认任职离职（TERMINATE）后，org 轴范围即时收敛 ORG_SELF，旧组织对象显式拒绝（FND-AUTH-004）
    public void testTerminatePrimaryMembership_orgScopeShrinksImmediately() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(2001L, OrganizationTypeEnum.DEPARTMENT, 2000L, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2000L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            // 请求1（变更前）：门店子树在授权范围内
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertEquals(SetUtils.asSet(2000L, 2001L), permissionApi.getOrgDataPermission(userId).getOrgIds());
            assertTrue(checker.isObjectVisible(2000L, null));
            assertTrue(checker.isObjectVisible(2001L, null));

            // 离职默认任职
            membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

            // 请求2（变更后，全新 LoginUser）：范围即时收敛，旧组织对象拒绝
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            OrgDataPermissionRespDTO after = permissionApi.getOrgDataPermission(userId);
            assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), after.getScopeType());
            assertTrue(after.getOrgIds().isEmpty());
            assertFalse(checker.isObjectVisible(2000L, null));
            assertServiceException(() -> checker.checkObjectVisible(2001L, null), FORBIDDEN);
        }
    }

    @Test // 停用（SUSPEND）默认任职后 org 轴范围同样即时失权（收敛 ORG_SELF，旧组织对象拒绝）
    public void testSuspendPrimaryMembership_orgScopeShrinksImmediately() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2100L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2100L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertTrue(checker.isObjectVisible(2100L, null));

            membershipService.changeStatus(membershipId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职");

            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), permissionApi.getOrgDataPermission(userId).getScopeType());
            assertFalse(checker.isObjectVisible(2100L, null));
        }
    }

    @Test // 关键：停用「非默认任职」时 IAM-004 不撤会话（primary 仍在职），但 org 轴数据授权必须独立即时收缩
    public void testSuspendSecondaryMembership_orgScopeShrinksWithoutSessionRevocation() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2200L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);   // 默认任职组织
        insertOrg(2300L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);   // 次级任职组织
        createPrimaryMembership(userId, 2200L);
        Long secondaryId = createSecondaryMembership(userId, 2300L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            // 变更前：两组织合法互授（各自子树并集）
            assertTrue(permissionApi.getOrgDataPermission(userId).getOrgIds().containsAll(SetUtils.asSet(2200L, 2300L)));
            assertTrue(checker.isObjectVisible(2300L, null));

            // 停用次级任职（默认任职仍在职 → IAM-004 shouldRevokeSessions 返回 false，不撤会话）
            membershipService.changeStatus(secondaryId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "次级停职");
            // 断言「不撤会话」半句（CodeReview P2-1）：primary 仍在职 → shouldRevokeSessions=false，会话撤销（IAM-004 独立层）不应触发
            verify(oauth2TokenService, never()).removeAccessToken(anyLong(), anyInt());

            // org 轴数据授权独立即时收缩：2300 退出范围、对象拒绝；2200（默认任职）仍在范围
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            OrgDataPermissionRespDTO after = permissionApi.getOrgDataPermission(userId);
            assertTrue(after.getOrgIds().contains(2200L));
            assertFalse(after.getOrgIds().contains(2300L));
            assertTrue(checker.isObjectVisible(2200L, null));
            assertFalse(checker.isObjectVisible(2300L, null));
            assertServiceException(() -> checker.checkObjectVisible(2300L, null), FORBIDDEN);
        }
    }

    // ========== 维度2：转岗及时改授权 ==========

    @Test // 转岗 A→B 后：范围即时排除 A 子树、纳入 B 子树；A 对象拒绝、B 对象放行
    public void testTransferMembership_scopeSwitchesFromOldToNewOrg() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2400L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);   // A
        insertOrg(2500L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);   // B
        Long membershipId = createPrimaryMembership(userId, 2400L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertTrue(checker.isObjectVisible(2400L, null));
            assertFalse(checker.isObjectVisible(2500L, null));

            membershipService.transferMembership(membershipId, 2500L, userId, "门店调动");

            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            OrgDataPermissionRespDTO after = permissionApi.getOrgDataPermission(userId);
            assertFalse(after.getOrgIds().contains(2400L));
            assertTrue(after.getOrgIds().contains(2500L));
            assertFalse(checker.isObjectVisible(2400L, null));   // 旧组织对象即时拒绝
            assertServiceException(() -> checker.checkObjectVisible(2400L, null), FORBIDDEN);
            assertTrue(checker.isObjectVisible(2500L, null));    // 新组织对象放行
        }
    }

    // ========== 维度3：恢复不自动恢复越界授权 ==========

    @Test // 停用期间组织被禁用 → 复职后范围不得恢复该组织（isEnable fail-closed），即「恢复不复活越界授权」
    public void testResumeAfterOrgDisabledDuringSuspension_doesNotRestoreOutOfScope() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2600L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2600L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertTrue(checker.isObjectVisible(2600L, null)); // 变更前在范围

            membershipService.changeStatus(membershipId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职");
            disableOrg(2600L); // 停用期间组织被禁用
            membershipService.changeStatus(membershipId, MembershipStatusEnum.ACTIVE.getStatus(), userId, "复职");

            // 复职后：组织已禁用 → 任职被 isEffective 排除 → 范围 ORG_SELF，不恢复 2600
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            OrgDataPermissionRespDTO after = permissionApi.getOrgDataPermission(userId);
            assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), after.getScopeType());
            assertFalse(after.getOrgIds().contains(2600L));
            assertFalse(checker.isObjectVisible(2600L, null));
        }
    }

    @Test // 合法恢复可重新取权（主卡 line566）：组织仍启用时复职 → 范围按当前有效任职重新纳入
    public void testResumeWithOrgStillEnabled_restoresCurrentAuthorization() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2700L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(2701L, OrganizationTypeEnum.DEPARTMENT, 2700L, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2700L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            membershipService.changeStatus(membershipId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职");
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertFalse(checker.isObjectVisible(2700L, null)); // 停用期间失权

            membershipService.changeStatus(membershipId, MembershipStatusEnum.ACTIVE.getStatus(), userId, "复职");

            // 复职后组织仍启用 → 合法授权重新取得（本组织 + 后代）
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertEquals(SetUtils.asSet(2700L, 2701L), permissionApi.getOrgDataPermission(userId).getOrgIds());
            assertTrue(checker.isObjectVisible(2700L, null));
            assertTrue(checker.isObjectVisible(2701L, null));
        }
    }

    // ========== 维度4：无陈旧缓存（每请求实时派生） ==========

    @Test // 经真实缓存代理：变更前读取（若该路径被误加缓存即填充陈旧值）→ 离职 → 变更后重复读取必即时收缩，无陈旧命中
    public void testRevocation_noStaleCacheAcrossRequests() {
        Long userId = randomLongId();
        insertUser(userId);
        insertOrg(2800L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2800L);

        OrgDataPermissionChecker checker = newChecker();
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            // 变更前：经真实 getOrgDataPermission（@EnableCaching 代理）读取并断言在范围
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
            assertTrue(permissionApi.getOrgDataPermission(userId).getOrgIds().contains(2800L));
            assertTrue(checker.isObjectVisible(2800L, null));

            // 离职默认任职
            membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

            // 变更后：连续三次「独立请求」（每次全新 LoginUser）经同一缓存代理读取——必须一致收缩，无陈旧命中
            for (int i = 0; i < 3; i++) {
                ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(newRequest(userId));
                assertTrue(permissionApi.getOrgDataPermission(userId).getOrgIds().isEmpty());
                assertFalse(checker.isObjectVisible(2800L, null));
            }
        }
    }

    // ========== 维度5：撤权审计完整性（操作者/目标/变更/结果——MembershipHistoryDO 流水；trace-id 由操作日志层承载，本套件不断言） ==========

    @Test // 生命周期各流转写完整历史流水：操作者、目标、变更（动作 + from/to 组织 + from/to 状态）、原因
    public void testLifecycleTransitions_writeCompleteAuditTrail() {
        Long userId = randomLongId();
        Long operatorId = randomLongId();
        insertUser(userId);
        insertOrg(2900L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(2950L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        Long membershipId = createPrimaryMembership(userId, 2900L);

        // 入职流水
        MembershipHistoryDO create = lastHistory(membershipId);
        assertEquals(MembershipActionEnum.CREATE.getAction(), create.getAction());
        assertEquals(userId, create.getUserId());            // 目标
        assertEquals(2900L, create.getToOrganizationId());   // 变更：入职组织
        assertEquals(userId, create.getOperatorId());        // 操作者（createPrimaryMembership 以 userId 为操作者）

        // 转岗流水：from/to 组织 + 操作者 + 原因
        membershipService.transferMembership(membershipId, 2950L, operatorId, "门店调动");
        MembershipHistoryDO transfer = lastHistory(membershipId);
        assertEquals(MembershipActionEnum.TRANSFER.getAction(), transfer.getAction());
        assertEquals(2900L, transfer.getFromOrganizationId());
        assertEquals(2950L, transfer.getToOrganizationId());
        assertEquals(operatorId, transfer.getOperatorId());
        assertEquals("门店调动", transfer.getReason());

        // 停用流水：from/to 状态（结果）+ 操作者 + 原因
        membershipService.changeStatus(membershipId, MembershipStatusEnum.SUSPENDED.getStatus(), operatorId, "停职");
        MembershipHistoryDO suspend = lastHistory(membershipId);
        assertEquals(MembershipActionEnum.SUSPEND.getAction(), suspend.getAction());
        assertEquals(MembershipStatusEnum.ACTIVE.getStatus(), suspend.getFromStatus());
        assertEquals(MembershipStatusEnum.SUSPENDED.getStatus(), suspend.getToStatus());
        assertEquals(operatorId, suspend.getOperatorId());
        assertEquals("停职", suspend.getReason());

        // 复职流水
        membershipService.changeStatus(membershipId, MembershipStatusEnum.ACTIVE.getStatus(), operatorId, "复职");
        MembershipHistoryDO resume = lastHistory(membershipId);
        assertEquals(MembershipActionEnum.RESUME.getAction(), resume.getAction());
        assertEquals(MembershipStatusEnum.SUSPENDED.getStatus(), resume.getFromStatus());
        assertEquals(MembershipStatusEnum.ACTIVE.getStatus(), resume.getToStatus());

        // 离职流水（保留行承载历史归属，只增不改）
        membershipService.changeStatus(membershipId, MembershipStatusEnum.TERMINATED.getStatus(), operatorId, "离职");
        MembershipHistoryDO terminate = lastHistory(membershipId);
        assertEquals(MembershipActionEnum.TERMINATE.getAction(), terminate.getAction());
        assertEquals(MembershipStatusEnum.TERMINATED.getStatus(), terminate.getToStatus());

        // 流水只增：入职→转岗→停用→复职→离职 共 5 行，动作序列精确匹配（承载「只增不改 + 完整流转链」，CodeReview P3-1）
        List<MembershipHistoryDO> all = membershipHistoryMapper.selectListByMembershipId(membershipId);
        assertEquals(5, all.size());
        assertEquals(List.of(MembershipActionEnum.CREATE.getAction(), MembershipActionEnum.TRANSFER.getAction(),
                        MembershipActionEnum.SUSPEND.getAction(), MembershipActionEnum.RESUME.getAction(),
                        MembershipActionEnum.TERMINATE.getAction()),
                all.stream().map(MembershipHistoryDO::getAction).toList());
    }

    private MembershipHistoryDO lastHistory(Long membershipId) {
        List<MembershipHistoryDO> history = membershipHistoryMapper.selectListByMembershipId(membershipId);
        return history.get(history.size() - 1);
    }

}
