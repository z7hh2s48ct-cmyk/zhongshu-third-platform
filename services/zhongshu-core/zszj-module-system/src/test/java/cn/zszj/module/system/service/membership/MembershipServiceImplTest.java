package cn.zszj.module.system.service.membership;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipHistoryDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipHistoryMapper;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.enums.membership.MembershipActionEnum;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.organization.OrganizationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MembershipServiceImpl} 的单元测试类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-001、003）：任职创建/默认任职/去重/转岗/状态流转 + 历史流水留痕。
 *
 * @author ZS-IAM-002
 */
@Import({MembershipServiceImpl.class, OrganizationServiceImpl.class})
public class MembershipServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MembershipServiceImpl membershipService;
    @Resource
    private MembershipMapper membershipMapper;
    @Resource
    private MembershipHistoryMapper membershipHistoryMapper;
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    /**
     * ZS-IAM-004：MembershipServiceImpl 新增失权联动依赖 OAuth2TokenService（@Lazy），
     * 本类 mock 之以满足 Spring 上下文装配（否则离任默认职的 changeStatus 会因无此 bean 报错）；
     * 失权行为的定向断言由 MembershipServiceImplLifecycleTest 承担。
     */
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;

    /**
     * 插入一个指定编号的账号（codex r0 P2：createMembership 现校验账号存在与租户归属）。
     */
    private void insertUser(Long userId) {
        adminUserMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 落在 tinyint 范围内
        }));
    }

    private Long insertOrganization(CommonStatusEnum status) {
        OrganizationDO organization = randomPojo(OrganizationDO.class, o -> {
            o.setType(OrganizationTypeEnum.DEPARTMENT.getType());
            o.setParentId(OrganizationDO.PARENT_ID_ROOT);
            o.setStatus(status.getStatus());
        });
        organizationMapper.insert(organization);
        return organization.getId();
    }

    private MembershipDO newMembership(Long userId, Long orgId) {
        return randomPojo(MembershipDO.class, o -> {
            o.setId(null);
            o.setUserId(userId);
            o.setOrganizationId(orgId);
            o.setStatus(null); // 交给 Service 默认 ACTIVE
            o.setIsPrimary(null);
            o.setValidFrom(null);
            o.setValidTo(null);
        });
    }

    @Test
    public void testCreateMembership_firstAutoPrimary() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);

        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);

        MembershipDO db = membershipMapper.selectById(id);
        assertEquals(MembershipStatusEnum.ACTIVE.getStatus(), db.getStatus());
        assertEquals(1, db.getIsPrimary());
        assertNotNull(db.getValidFrom());
        // 历史流水：入职
        List<MembershipHistoryDO> history = membershipHistoryMapper.selectListByMembershipId(id);
        assertEquals(1, history.size());
        assertEquals(MembershipActionEnum.CREATE.getAction(), history.get(0).getAction());
    }

    @Test
    public void testCreateMembership_secondNotPrimary() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        membershipService.createMembership(newMembership(userId, org1), userId);

        Long secondId = membershipService.createMembership(newMembership(userId, org2), userId);

        assertEquals(0, membershipMapper.selectById(secondId).getIsPrimary());
    }

    @Test
    public void testCreateMembership_duplicateUserOrg() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        membershipService.createMembership(newMembership(userId, orgId), userId);

        assertServiceException(() -> membershipService.createMembership(newMembership(userId, orgId), userId),
                MEMBERSHIP_ALREADY_EXISTS);
    }

    @Test
    public void testCreateMembership_organizationDisabled() {
        Long orgId = insertOrganization(CommonStatusEnum.DISABLE);
        Long userId = randomLongId();
        insertUser(userId);

        assertServiceException(() -> membershipService.createMembership(newMembership(userId, orgId), userId),
                ORGANIZATION_NOT_ENABLE, organizationMapper.selectById(orgId).getName());
    }

    @Test
    public void testTransferMembership() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, org1), userId);

        membershipService.transferMembership(id, org2, userId, "门店调动");

        assertEquals(org2, membershipMapper.selectById(id).getOrganizationId());
        List<MembershipHistoryDO> history = membershipHistoryMapper.selectListByMembershipId(id);
        MembershipHistoryDO transfer = history.get(history.size() - 1);
        assertEquals(MembershipActionEnum.TRANSFER.getAction(), transfer.getAction());
        assertEquals(org1, transfer.getFromOrganizationId());
        assertEquals(org2, transfer.getToOrganizationId());
        assertEquals("门店调动", transfer.getReason());
    }

    @Test
    public void testChangeStatus_terminateKeepsRow() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);

        membershipService.changeStatus(id, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        // 保留行承载历史归属，不物理删除
        MembershipDO db = membershipMapper.selectById(id);
        assertNotNull(db);
        assertEquals(MembershipStatusEnum.TERMINATED.getStatus(), db.getStatus());
        List<MembershipHistoryDO> history = membershipHistoryMapper.selectListByMembershipId(id);
        assertEquals(MembershipActionEnum.TERMINATE.getAction(), history.get(history.size() - 1).getAction());
    }

    @Test
    public void testChangeStatus_notExists() {
        assertServiceException(() -> membershipService.changeStatus(randomLongId(),
                MembershipStatusEnum.SUSPENDED.getStatus(), 1L, null), MEMBERSHIP_NOT_EXISTS);
    }

    @Test
    public void testGetPrimaryMembership() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long primaryId = membershipService.createMembership(newMembership(userId, org1), userId);
        membershipService.createMembership(newMembership(userId, org2), userId);

        assertEquals(primaryId, membershipService.getPrimaryMembership(userId).getId());
        assertEquals(2, membershipService.getMembershipListByUserId(userId).size());
    }

    @Test
    public void testCreateMembership_replaceRetiredPrimary() {
        // codex r0 P2：org A 主职离任后，在 org B 显式承接默认任职——旧行保留（仅摘除 is_primary）、新行成为唯一 primary
        Long orgA = insertOrganization(CommonStatusEnum.ENABLE);
        Long orgB = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long primaryA = membershipService.createMembership(newMembership(userId, orgA), userId);
        // 离任 org A 主职（保留行承载历史归属，is_primary 仍为 1）
        membershipService.changeStatus(primaryA, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        // 在 org B 显式承接默认任职
        MembershipDO b = newMembership(userId, orgB);
        b.setIsPrimary(1);
        Long primaryB = membershipService.createMembership(b, userId);

        // 旧行保留但已摘除 primary 标志；新行成为默认任职
        assertEquals(0, membershipMapper.selectById(primaryA).getIsPrimary());
        assertEquals(1, membershipMapper.selectById(primaryB).getIsPrimary());
        assertEquals(primaryB, membershipService.getPrimaryMembership(userId).getId());
    }

    @Test
    public void testCreateMembership_userNotExists() {
        // codex r0 P2：为不存在（或跨租户）账号创建任职被拒
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId(); // 未插入对应账号

        assertServiceException(() -> membershipService.createMembership(newMembership(userId, orgId), userId),
                USER_NOT_EXISTS);
    }

}
