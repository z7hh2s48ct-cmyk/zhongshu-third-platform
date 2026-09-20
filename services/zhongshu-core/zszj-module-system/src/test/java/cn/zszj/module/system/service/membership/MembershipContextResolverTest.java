package cn.zszj.module.system.service.membership;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MembershipContextResolver} 的单元测试类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-004）核心：服务端组织上下文解析——仅在职未过期且组织开启的默认任职可进入上下文；
 * 停用/离职/过期/组织停用一律拒绝（fail-closed）；无默认任职降级空上下文；不接受任何客户端指定的组织入参。
 *
 * @author ZS-IAM-002
 */
@Import(MembershipContextResolver.class)
public class MembershipContextResolverTest extends BaseDbUnitTest {

    @Resource
    private MembershipContextResolver resolver;
    @Resource
    private MembershipMapper membershipMapper;
    @Resource
    private OrganizationMapper organizationMapper;

    private Long insertOrganization(CommonStatusEnum status, OrganizationTypeEnum type) {
        OrganizationDO organization = randomPojo(OrganizationDO.class, o -> {
            o.setType(type.getType());
            o.setParentId(OrganizationDO.PARENT_ID_ROOT);
            o.setStatus(status.getStatus());
        });
        organizationMapper.insert(organization);
        return organization.getId();
    }

    private Long insertPrimaryMembership(Long userId, Long orgId, MembershipStatusEnum status, LocalDateTime validTo) {
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(status.getStatus());
            o.setIsPrimary(1);
            o.setValidFrom(LocalDateTime.now().minusDays(1));
            o.setValidTo(validTo);
        });
        membershipMapper.insert(membership);
        return membership.getId();
    }

    @Test
    public void testResolve_active() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        Long membershipId = insertPrimaryMembership(userId, orgId, MembershipStatusEnum.ACTIVE, null);

        OrganizationContext context = resolver.resolve(userId);

        assertFalse(context.isEmpty());
        assertEquals(orgId, context.getOrgId());
        assertEquals(OrganizationTypeEnum.STORE.getType(), context.getOrgType());
        assertEquals(membershipId, context.getMembershipId());
    }

    @Test
    public void testResolve_suspendedRejected() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        insertPrimaryMembership(userId, orgId, MembershipStatusEnum.SUSPENDED, null);

        assertServiceException(() -> resolver.resolve(userId),
                MEMBERSHIP_INVALID_STATUS, MembershipStatusEnum.SUSPENDED.getStatus());
    }

    @Test
    public void testResolve_terminatedRejected() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        insertPrimaryMembership(userId, orgId, MembershipStatusEnum.TERMINATED, null);

        assertServiceException(() -> resolver.resolve(userId),
                MEMBERSHIP_INVALID_STATUS, MembershipStatusEnum.TERMINATED.getStatus());
    }

    @Test
    public void testResolve_expiredRejected() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        // 有效期已过：过期上下文拒绝
        insertPrimaryMembership(userId, orgId, MembershipStatusEnum.ACTIVE, LocalDateTime.now().minusDays(1));

        assertServiceException(() -> resolver.resolve(userId), MEMBERSHIP_EXPIRED);
    }

    @Test
    public void testResolve_futureValidToAccepted() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        insertPrimaryMembership(userId, orgId, MembershipStatusEnum.ACTIVE, LocalDateTime.now().plusDays(30));

        assertFalse(resolver.resolve(userId).isEmpty());
    }

    @Test
    public void testResolve_futureValidFromRejected() {
        // codex r0 P1：生效期未到的任职不得提前进入上下文（valid_from 在未来、无 valid_to）
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
            o.setIsPrimary(1);
            o.setValidFrom(LocalDateTime.now().plusDays(1));
            o.setValidTo(null);
        });
        membershipMapper.insert(membership);

        assertServiceException(() -> resolver.resolve(userId), MEMBERSHIP_NOT_EFFECTIVE);
    }

    @Test
    public void testResolve_organizationDisabledRejected() {
        Long orgId = insertOrganization(CommonStatusEnum.DISABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        insertPrimaryMembership(userId, orgId, MembershipStatusEnum.ACTIVE, null);

        assertServiceException(() -> resolver.resolve(userId), MEMBERSHIP_ORGANIZATION_DISABLED, orgId);
    }

    @Test
    public void testResolve_noPrimaryDowngrade() {
        Long userId = randomLongId();
        // 无默认任职：降级空上下文，不抛异常（历史无部门账号兼容）
        OrganizationContext context = resolver.resolve(userId);
        assertTrue(context.isEmpty());
        assertNull(context.getOrgId());
    }

    @Test
    public void testResolve_nonPrimaryIgnored() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE, OrganizationTypeEnum.STORE);
        Long userId = randomLongId();
        // 仅存在非默认任职 → 上下文降级为空（服务端只认默认任职，杜绝未授权身份切换）
        MembershipDO membership = randomPojo(MembershipDO.class, o -> {
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(MembershipStatusEnum.ACTIVE.getStatus());
            o.setIsPrimary(0);
            o.setValidFrom(LocalDateTime.now().minusDays(1));
            o.setValidTo(null);
        });
        membershipMapper.insert(membership);

        assertTrue(resolver.resolve(userId).isEmpty());
    }

    @Test
    public void testResolve_invalidUserIdDowngrade() {
        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve(0L).isEmpty());
        assertTrue(resolver.resolve(-1L).isEmpty());
    }

}
