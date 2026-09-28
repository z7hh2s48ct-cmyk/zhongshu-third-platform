package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.util.collection.SetUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.enums.permission.OrgDataScopeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OrgDataScopeResolver} 的单元测试（ZS-PERM-002.B，org 轴范围解析）。
 *
 * <p>覆盖 D-09 一期口径：超管 / PLATFORM 类型组织任职 → {@code ORG_ALL}（显式平台角色，跨组织全量）；
 * 非平台在职任职 → {@code ORG_AND_CHILD}（本人各在职组织 + 组织树后代并集，支持两组织合法互授）；
 * 无有效任职 → {@code ORG_SELF}（仅本人兜底）。「有效任职」判定与 IAM-002 {@code MembershipContextResolver}
 * 同口径：状态 ACTIVE + valid_from 已到 + valid_to 未过 + 组织存在且显式开启，任一不满足即排除。
 * 组织树下钻对脏数据成环具备终止性（visited 去重 + 深度上限）。
 *
 * @author ZS-PERM-002.B
 */
@Import(OrgDataScopeResolver.class)
public class OrgDataScopeResolverTest extends BaseDbUnitTest {

    @Resource
    private OrgDataScopeResolver resolver;
    @Resource
    private MembershipMapper membershipMapper;
    @Resource
    private OrganizationMapper organizationMapper;

    private void insertOrg(Long id, OrganizationTypeEnum type, Long parentId, CommonStatusEnum status) {
        insertOrg(id, type, parentId, status, null);
    }

    private void insertOrg(Long id, OrganizationTypeEnum type, Long parentId, CommonStatusEnum status,
                           Long leaderUserId) {
        organizationMapper.insert(randomPojo(OrganizationDO.class, o -> {
            o.setId(id);
            o.setType(type.getType());
            o.setParentId(parentId);
            o.setStatus(status.getStatus());
            o.setRefDeptId(null);
            o.setLeaderUserId(leaderUserId);
        }));
    }

    private void insertMembership(Long userId, Long orgId, MembershipStatusEnum status,
                                  LocalDateTime validFrom, LocalDateTime validTo) {
        membershipMapper.insert(randomPojo(MembershipDO.class, o -> {
            o.setId(null);
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(status.getStatus());
            o.setValidFrom(validFrom);
            o.setValidTo(validTo);
            o.setIsPrimary(0);
        }));
    }

    @Test // 超级管理员：全部组织，ORG_ALL（不依赖任何任职行）
    public void testResolve_superAdmin_all() {
        OrgDataPermissionRespDTO dto = resolver.resolve(randomLongId(), true);

        assertTrue(dto.getAll());
        assertEquals(OrgDataScopeEnum.ORG_ALL.getScope(), dto.getScopeType());
    }

    @Test // 显式平台角色：PLATFORM 类型组织在职任职 → ORG_ALL（D-09 跨组织只允许显式平台角色）
    public void testResolve_platformMembership_all() {
        Long userId = randomLongId();
        insertOrg(1000L, OrganizationTypeEnum.PLATFORM, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 1000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertTrue(dto.getAll());
        assertEquals(OrgDataScopeEnum.ORG_ALL.getScope(), dto.getScopeType());
    }

    @Test // 负责人：门店在职任职 → 本组织 + 组织树后代（parent→child→grandchild）并集，ORG_AND_CHILD
    public void testResolve_storeSubtree_andChild() {
        Long userId = randomLongId();
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(2001L, OrganizationTypeEnum.DEPARTMENT, 2000L, CommonStatusEnum.ENABLE);
        insertOrg(2002L, OrganizationTypeEnum.DEPARTMENT, 2001L, CommonStatusEnum.ENABLE);
        insertMembership(userId, 2000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertFalse(dto.getAll());
        assertTrue(dto.getSelf());
        assertEquals(OrgDataScopeEnum.ORG_AND_CHILD.getScope(), dto.getScopeType());
        assertEquals(SetUtils.asSet(2000L, 2001L, 2002L), dto.getOrgIds());
    }

    @Test // 两组织合法互授：多任职（BRAND + STORE）→ 各自子树并集，ORG_AND_CHILD
    public void testResolve_multiMembership_union() {
        Long userId = randomLongId();
        insertOrg(3000L, OrganizationTypeEnum.BRAND, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(3001L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 3000L, MembershipStatusEnum.ACTIVE, null, null);
        insertMembership(userId, 3001L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertFalse(dto.getAll());
        assertEquals(OrgDataScopeEnum.ORG_AND_CHILD.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().containsAll(SetUtils.asSet(3000L, 3001L)));
    }

    @Test // 员工无组织上下文：无任何任职 → 仅本人兜底，ORG_SELF，orgIds 空
    public void testResolve_noMembership_self() {
        OrgDataPermissionRespDTO dto = resolver.resolve(randomLongId(), false);

        assertFalse(dto.getAll());
        assertTrue(dto.getSelf());
        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().isEmpty());
    }

    @Test // 非在职任职（TERMINATED）不进入范围 → 收敛到 ORG_SELF
    public void testResolve_inactiveMembership_self() {
        Long userId = randomLongId();
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 2000L, MembershipStatusEnum.TERMINATED, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().isEmpty());
    }

    @Test // 已过有效期（valid_to 早于 now）的 ACTIVE 任职排除 → ORG_SELF（与上下文解析器过期拒绝同口径）
    public void testResolve_expiredMembership_self() {
        Long userId = randomLongId();
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 2000L, MembershipStatusEnum.ACTIVE, null, LocalDateTime.now().minusDays(1));

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().isEmpty());
    }

    @Test // 未到生效期（valid_from 晚于 now）的 ACTIVE 任职排除 → ORG_SELF
    public void testResolve_notEffectiveMembership_self() {
        Long userId = randomLongId();
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 2000L, MembershipStatusEnum.ACTIVE, LocalDateTime.now().plusDays(1), null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().isEmpty());
    }

    @Test // 组织未显式开启（DISABLE）→ 该任职排除 → ORG_SELF（fail-closed，与上下文解析器一致）
    public void testResolve_disabledOrg_self() {
        Long userId = randomLongId();
        insertOrg(4000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.DISABLE);
        insertMembership(userId, 4000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().isEmpty());
    }

    @Test // 混合：一条 ACTIVE（含子组织）+ 一条 SUSPENDED → 仅 ACTIVE 组织子树进入范围
    public void testResolve_mixedActiveAndInactive_onlyActiveSubtree() {
        Long userId = randomLongId();
        insertOrg(2000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(2001L, OrganizationTypeEnum.DEPARTMENT, 2000L, CommonStatusEnum.ENABLE);
        insertOrg(3001L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertMembership(userId, 2000L, MembershipStatusEnum.ACTIVE, null, null);
        insertMembership(userId, 3001L, MembershipStatusEnum.SUSPENDED, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(SetUtils.asSet(2000L, 2001L), dto.getOrgIds()); // 3001 因停用被排除
    }

    @Test // P2 回归：启用根组织的禁用子组织（及其启用孙组织）不纳入范围——后代下钻同样 fail-closed 校验启用态
    public void testResolve_disabledDescendant_excluded() {
        Long userId = randomLongId();
        insertOrg(6000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(6001L, OrganizationTypeEnum.DEPARTMENT, 6000L, CommonStatusEnum.DISABLE);
        insertOrg(6002L, OrganizationTypeEnum.DEPARTMENT, 6001L, CommonStatusEnum.ENABLE);
        insertMembership(userId, 6000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_AND_CHILD.getScope(), dto.getScopeType());
        assertEquals(SetUtils.asSet(6000L), dto.getOrgIds()); // 6001 禁用被排除、其子树 6002 一并 fail-closed 排除
    }

    @Test // 启用子纳入、禁用孙排除：根启用 + 子启用 + 孙禁用 → 收集根+子
    public void testResolve_enabledChildDisabledGrandchild() {
        Long userId = randomLongId();
        insertOrg(7000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE);
        insertOrg(7001L, OrganizationTypeEnum.DEPARTMENT, 7000L, CommonStatusEnum.ENABLE);
        insertOrg(7002L, OrganizationTypeEnum.DEPARTMENT, 7001L, CommonStatusEnum.DISABLE);
        insertMembership(userId, 7000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(SetUtils.asSet(7000L, 7001L), dto.getOrgIds()); // 7002 禁用被排除
    }

    @Test // 脏数据成环（A↔B）下钻具备终止性，不死循环，收集到环内组织
    public void testResolve_orgCycle_terminates() {
        Long userId = randomLongId();
        insertOrg(5000L, OrganizationTypeEnum.STORE, 5001L, CommonStatusEnum.ENABLE);
        insertOrg(5001L, OrganizationTypeEnum.DEPARTMENT, 5000L, CommonStatusEnum.ENABLE);
        insertMembership(userId, 5000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(OrgDataScopeEnum.ORG_AND_CHILD.getScope(), dto.getScopeType());
        assertTrue(dto.getOrgIds().containsAll(SetUtils.asSet(5000L, 5001L)));
    }

    // ========== 负责人集合 ledOrgIds（ZS-PERM-003.B，D-12 等级×角色映射的判定输入） ==========

    @Test // 组织负责人：ledOrgIds 收录其负责的组织（仅作为字段等级 F2 判定输入，不影响 orgIds 范围）
    public void testResolve_orgLeader_ledOrgIdsFilled() {
        Long userId = randomLongId();
        insertOrg(8000L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE,
                userId);
        insertMembership(userId, 8000L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(SetUtils.asSet(8000L), dto.getLedOrgIds());
        assertEquals(OrgDataScopeEnum.ORG_AND_CHILD.getScope(), dto.getScopeType());
    }

    @Test // 普通成员（非任何组织负责人）：ledOrgIds 为空集
    public void testResolve_memberNotLeader_ledOrgIdsEmpty() {
        Long userId = randomLongId();
        Long otherUser = randomLongId();
        insertOrg(8100L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE,
                otherUser);
        insertMembership(userId, 8100L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertTrue(dto.getLedOrgIds().isEmpty());
    }

    @Test // 禁用组织的负责人不收录（fail-closed：无效任职排除口径一致，负责人身份不因管理动作越级）
    public void testResolve_leaderOfDisabledOrg_ledOrgIdsEmpty() {
        Long userId = randomLongId();
        insertOrg(8200L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.DISABLE,
                userId);
        insertMembership(userId, 8200L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertTrue(dto.getLedOrgIds().isEmpty());
        assertEquals(OrgDataScopeEnum.ORG_SELF.getScope(), dto.getScopeType());
    }

    @Test // 混合：负责 A 组织 + 受雇于 B 组织 → ledOrgIds 仅含 A（负责人身份逐组织判定）
    public void testResolve_mixedLeaderAndMember_ledOnlyLeaderOrg() {
        Long userId = randomLongId();
        insertOrg(8300L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE,
                userId);
        insertOrg(8301L, OrganizationTypeEnum.BRAND, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE,
                randomLongId());
        insertMembership(userId, 8300L, MembershipStatusEnum.ACTIVE, null, null);
        insertMembership(userId, 8301L, MembershipStatusEnum.ACTIVE, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertEquals(SetUtils.asSet(8300L), dto.getLedOrgIds());
    }

    @Test // 非在职/过期任职所在组织的负责人身份不收录（与有效任职判定同口径）
    public void testResolve_leaderWithTerminatedMembership_ledOrgIdsEmpty() {
        Long userId = randomLongId();
        insertOrg(8400L, OrganizationTypeEnum.STORE, OrganizationDO.PARENT_ID_ROOT, CommonStatusEnum.ENABLE,
                userId);
        insertMembership(userId, 8400L, MembershipStatusEnum.TERMINATED, null, null);

        OrgDataPermissionRespDTO dto = resolver.resolve(userId, false);

        assertTrue(dto.getLedOrgIds().isEmpty());
    }

}
