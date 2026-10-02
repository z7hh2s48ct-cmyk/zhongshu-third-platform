package cn.zszj.module.firstchain.service;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link FirstchainUserOrgChecker}：「用户是否属于组织」的判定合同（真实 server E2E 暴露的缺陷回归锚点）。
 *
 * <p>背景：首版按 {@code user.deptId == orgId} 判定，但开通/建员工链路创建账号时并不设 deptId，归属由
 * {@code system_membership}（D-09 任职模型）承载——真实 server 上负责人把线索分配给本组织合法员工恒被
 * {@code LEAD_ASSIGN_TARGET_NOT_IN_ORG} 拒绝，PILOT-REQ-006 主链不通。H2 单测以 Stub 校验器隔离，未触及本类。
 *
 * <p>合同：目标用户存在且启用，并在目标组织持有<b>有效任职</b>（状态 ACTIVE、在有效期内）；与 deptId 无关；
 * 每次实时判定。
 *
 * @author ZS-FC-002
 */
class FirstchainUserOrgCheckerTest {

    private static final Long TENANT_ID = 1L;

    private static final Long ORG_ID = 700L;

    private static final Long OTHER_ORG_ID = 701L;

    private static final Long USER_ID = 900L;

    private AdminUserApi adminUserApi;

    private MembershipService membershipService;

    private FirstchainUserOrgChecker checker;

    @BeforeEach
    void setUp() {
        adminUserApi = mock(AdminUserApi.class);
        membershipService = mock(MembershipService.class);
        checker = new FirstchainUserOrgChecker(adminUserApi, membershipService);
        stubUser(CommonStatusEnum.ENABLE.getStatus(), null); // 开通链路建的账号没有 deptId
    }

    @Test
    void activeMembershipInOrg_enabledUser_withoutDeptId_isMember() {
        stubMemberships(membership(ORG_ID, MembershipStatusEnum.ACTIVE, null));

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isTrue();
    }

    @Test
    void membershipOnlyInOtherOrg_isNotMember() {
        stubMemberships(membership(OTHER_ORG_ID, MembershipStatusEnum.ACTIVE, null));

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
    }

    @Test
    void noMembership_isNotMember() {
        stubMemberships();

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
    }

    @Test
    void inactiveMembership_isNotMember() {
        for (MembershipStatusEnum status : List.of(MembershipStatusEnum.SUSPENDED,
                MembershipStatusEnum.EXPIRED, MembershipStatusEnum.TERMINATED)) {
            stubMemberships(membership(ORG_ID, status, null));

            assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID))
                    .as("任职状态 %s 不得视为本组织成员", status).isFalse();
        }
    }

    @Test
    void membershipPastValidTo_isNotMember() {
        stubMemberships(membership(ORG_ID, MembershipStatusEnum.ACTIVE, LocalDateTime.now().minusDays(1)));

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
    }

    @Test
    void membershipNotYetValid_isNotMember() {
        MembershipDO future = membership(ORG_ID, MembershipStatusEnum.ACTIVE, null);
        future.setValidFrom(LocalDateTime.now().plusDays(1));
        stubMemberships(future);

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
    }

    @Test
    void disabledUser_isNotMember_evenWithActiveMembership() {
        stubUser(CommonStatusEnum.DISABLE.getStatus(), null);
        stubMemberships(membership(ORG_ID, MembershipStatusEnum.ACTIVE, null));

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
    }

    @Test
    void missingUser_orNullArguments_areNotMember() {
        when(adminUserApi.getUser(USER_ID)).thenReturn(null);
        stubMemberships(membership(ORG_ID, MembershipStatusEnum.ACTIVE, null));

        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, ORG_ID)).isFalse();
        assertThat(checker.isUserInOrg(TENANT_ID, null, ORG_ID)).isFalse();
        assertThat(checker.isUserInOrg(TENANT_ID, USER_ID, null)).isFalse();
    }

    // ========== 夹具 ==========

    private void stubUser(Integer status, Long deptId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(USER_ID);
        user.setStatus(status);
        user.setDeptId(deptId);
        when(adminUserApi.getUser(USER_ID)).thenReturn(user);
    }

    private void stubMemberships(MembershipDO... memberships) {
        when(membershipService.getMembershipListByUserId(USER_ID)).thenReturn(List.of(memberships));
    }

    private static MembershipDO membership(Long orgId, MembershipStatusEnum status, LocalDateTime validTo) {
        MembershipDO membership = new MembershipDO();
        membership.setUserId(USER_ID);
        membership.setOrganizationId(orgId);
        membership.setStatus(status.getStatus());
        membership.setValidFrom(LocalDateTime.now().minusYears(1));
        membership.setValidTo(validTo);
        return membership;
    }

}
