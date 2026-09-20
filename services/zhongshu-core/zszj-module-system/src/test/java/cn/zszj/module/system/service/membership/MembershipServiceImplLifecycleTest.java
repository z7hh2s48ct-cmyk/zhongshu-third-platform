package cn.zszj.module.system.service.membership;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
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
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;
import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MembershipServiceImpl} 任职生命周期的单元测试类。
 *
 * <p>ZS-IAM-004（D-09 FND-IAM-003/005/006、FND-AUTH-007）：验证「停用/离职/过期及时失权、
 * 恢复不复活旧越界授权、生命周期审计、转岗不重写已发生业务的责任历史」。
 *
 * <p><b>失权触发范围（D1 方案 B，精确）</b>：{@link MembershipContextResolver} 仅依据默认任职（primary）
 * 解析并签名 token 组织上下文，故仅当①被变更任职是 primary，或②变更后账号已无任何 ACTIVE 任职时，
 * 才复用 LOGIN-003 撤销链 {@link OAuth2TokenService#removeAccessToken(Long, Integer)} 撤销全部会话；
 * 停用一条不影响上下文的次级任职（primary 仍在职）不强制全端下线。恢复（→ACTIVE）<b>不</b>复活任何令牌。
 *
 * @author ZS-IAM-004
 */
@Import({MembershipServiceImpl.class, OrganizationServiceImpl.class})
public class MembershipServiceImplLifecycleTest extends BaseDbUnitTest {

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
     * ZS-IAM-004：失权联动复用 LOGIN-003 撤销链，此处 mock 以断言撤销触发/不触发。
     */
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;

    private void insertUser(Long userId) {
        adminUserMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setSex(randomEle(SexEnum.values()).getSex());
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
            o.setStatus(null);
            o.setIsPrimary(null);
            o.setValidFrom(null);
            o.setValidTo(null);
        });
    }

    // ========== ① 停用/离职/过期及时失权（primary 任职 → 撤销全部会话） ==========

    /**
     * 场景 ①：停用默认任职必须撤销该账号全部会话。
     *
     * <p>RED：修复前 {@code changeStatus} 只改状态、写流水，从不触碰令牌 ——
     * 被停用账号的 access/refresh 凭据在自然过期前仍携带旧组织上下文可用。
     */
    @Test
    public void testChangeStatus_suspendPrimary_revokesAllSessions() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);
        assertEquals(1, membershipMapper.selectById(id).getIsPrimary());

        membershipService.changeStatus(id, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职调查");

        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
    }

    /**
     * 场景 ①-2：离职默认任职必须撤销全部会话。
     */
    @Test
    public void testChangeStatus_terminatePrimary_revokesAllSessions() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);

        membershipService.changeStatus(id, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");

        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
    }

    /**
     * 场景 ①-3：过期默认任职必须撤销全部会话。
     */
    @Test
    public void testChangeStatus_expirePrimary_revokesAllSessions() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);

        membershipService.changeStatus(id, MembershipStatusEnum.EXPIRED.getStatus(), userId, "任期届满");

        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
    }

    // ========== ② D1 方案 B 精确性：次级任职停用不越权中断 ==========

    /**
     * 场景 ②：停用一条<b>非默认</b>次级任职（primary 仍在职）不得撤销会话 —— token 上下文只由 primary 决定，
     * 次级任职停用不影响已签发上下文（方案 B 精确失权，避免过度中断）。
     *
     * <p>RED：方案 A 会无条件撤销，本断言防止越权中断。
     */
    @Test
    public void testChangeStatus_suspendSecondaryWhilePrimaryActive_noRevoke() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long primaryId = membershipService.createMembership(newMembership(userId, org1), userId);
        Long secondaryId = membershipService.createMembership(newMembership(userId, org2), userId);
        assertEquals(1, membershipMapper.selectById(primaryId).getIsPrimary());
        assertEquals(0, membershipMapper.selectById(secondaryId).getIsPrimary());

        membershipService.changeStatus(secondaryId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "兼任结束");

        verify(oauth2TokenService, never()).removeAccessToken(anyLong(), anyInt());
    }

    /**
     * 场景 ②-2：变更后账号已无任何 ACTIVE 任职（即使被变更行非 primary）也必须撤销 —— 上下文彻底丧失（方案 B 条件②）。
     */
    @Test
    public void testChangeStatus_lastActiveLost_revokesEvenIfNotPrimary() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long primaryId = membershipService.createMembership(newMembership(userId, org1), userId);
        Long secondaryId = membershipService.createMembership(newMembership(userId, org2), userId);
        // 先离职 primary（触发一次撤销），随后 primary 保留 is_primary=1 但状态 TERMINATED
        membershipService.changeStatus(primaryId, MembershipStatusEnum.TERMINATED.getStatus(), userId, "离职");
        reset(oauth2TokenService);

        // 停用唯一剩余的 ACTIVE 次级任职 → 账号已无任何在职任职 → 撤销
        membershipService.changeStatus(secondaryId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "兼任结束");

        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
    }

    // ========== ③ 恢复不复活旧越界授权（codex r0 P2） ==========

    /**
     * 场景 ③：复职<b>默认任职</b>必须撤销全部会话（强制重新登录）。
     *
     * <p>codex r0 P2：并发登录签发的令牌可能逃逸停用时的撤销；停用期间 {@code checkAccessToken} 每请求
     * fail-closed 复验会拒绝它（MEMBERSHIP_INVALID_STATUS），但复职后同一令牌会被重新接受而无需再次登录，
     * 静默复活旧授权。复职默认任职时撤销全部会话，逃逸令牌一并失效、强制重新登录经上下文重解析，彻底关闭该窗口。
     */
    @Test
    public void testChangeStatus_resumePrimary_revokesToForceCleanReLogin() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);
        assertEquals(1, membershipMapper.selectById(id).getIsPrimary());
        membershipService.changeStatus(id, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职");
        reset(oauth2TokenService);

        membershipService.changeStatus(id, MembershipStatusEnum.ACTIVE.getStatus(), userId, "复职");

        // 复职默认任职：撤销全部会话（含可能逃逸停用撤销的令牌），强制重新登录 + 上下文全新解析
        verify(oauth2TokenService, times(1)).removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));
        assertEquals(MembershipStatusEnum.ACTIVE.getStatus(), membershipMapper.selectById(id).getStatus());
    }

    /**
     * 场景 ③-2：复职一条<b>非默认</b>次级任职（primary 仍在职）不得撤销会话 —— 不影响上下文，避免过度中断。
     */
    @Test
    public void testChangeStatus_resumeSecondaryWhilePrimaryActive_noRevoke() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long primaryId = membershipService.createMembership(newMembership(userId, org1), userId);
        Long secondaryId = membershipService.createMembership(newMembership(userId, org2), userId);
        membershipService.changeStatus(secondaryId, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "兼任暂停");
        reset(oauth2TokenService);

        membershipService.changeStatus(secondaryId, MembershipStatusEnum.ACTIVE.getStatus(), userId, "兼任恢复");

        // primary 仍在职，次级复职不影响已签发上下文 → 不撤销
        verify(oauth2TokenService, never()).removeAccessToken(anyLong(), anyInt());
        assertEquals(1, membershipMapper.selectById(primaryId).getIsPrimary());
    }

    // ========== ④ 撤销失败必须回滚状态变更（宁可显式失败，不留「已停用但仍在线」窗口） ==========

    /**
     * 场景 ④：撤销会话失败时，状态变更必须随 {@code @Transactional} 回滚。
     *
     * <p>RED：修复前 changeStatus 不调用撤销，异常不抛出、状态被改为 SUSPENDED。
     */
    @Test
    public void testChangeStatus_revokeFailure_rollsBackStatus() {
        Long orgId = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, orgId), userId);
        doThrow(new RuntimeException("撤销失败")).when(oauth2TokenService)
                .removeAccessToken(eq(userId), eq(UserTypeEnum.ADMIN.getValue()));

        assertThrows(RuntimeException.class, () ->
                membershipService.changeStatus(id, MembershipStatusEnum.SUSPENDED.getStatus(), userId, "停职"));

        // 状态回滚为在职，不留「已停用但仍在线」窗口
        assertEquals(MembershipStatusEnum.ACTIVE.getStatus(), membershipMapper.selectById(id).getStatus());
    }

    // ========== ⑤ 转岗不重写已发生业务的责任历史（流水只增不改） ==========

    /**
     * 场景 ⑤：转岗仅追加流水、改写当前组织，绝不重写既有历史行 —— 已发生业务的责任归属溯源不被篡改。
     */
    @Test
    public void testTransferMembership_historyAppendOnly_notRewritten() {
        Long org1 = insertOrganization(CommonStatusEnum.ENABLE);
        Long org2 = insertOrganization(CommonStatusEnum.ENABLE);
        Long userId = randomLongId();
        insertUser(userId);
        Long id = membershipService.createMembership(newMembership(userId, org1), userId);
        List<MembershipHistoryDO> before = membershipHistoryMapper.selectListByMembershipId(id);
        assertEquals(1, before.size());
        MembershipHistoryDO createRow = before.get(0);

        membershipService.transferMembership(id, org2, userId, "门店调动");

        List<MembershipHistoryDO> after = membershipHistoryMapper.selectListByMembershipId(id);
        assertEquals(2, after.size()); // 只增
        // 入职起点流水未被改写：仍指向原组织 org1、动作仍为 CREATE
        MembershipHistoryDO stillCreate = after.stream()
                .filter(h -> h.getId().equals(createRow.getId())).findFirst().orElseThrow();
        assertEquals(MembershipActionEnum.CREATE.getAction(), stillCreate.getAction());
        assertEquals(org1, stillCreate.getToOrganizationId());
        // 新追加的转岗流水承载 from→to
        MembershipHistoryDO transfer = after.stream()
                .filter(h -> MembershipActionEnum.TRANSFER.getAction().equals(h.getAction())).findFirst().orElseThrow();
        assertEquals(org1, transfer.getFromOrganizationId());
        assertEquals(org2, transfer.getToOrganizationId());
    }

    // ========== ⑥ 生命周期审计（@LogRecord）留痕 ==========

    /**
     * 场景 ⑥：入职/转岗/状态流转三类生命周期入口都必须携带 {@link LogRecord} 操作审计。
     *
     * <p>RED：修复前 membership 方法零审计注解。
     */
    @Test
    public void testLifecycleMethods_annotatedWithLogRecord() throws NoSuchMethodException {
        assertHasLogRecord("createMembership", MembershipDO.class, Long.class);
        assertHasLogRecord("transferMembership", Long.class, Long.class, Long.class, String.class);
        assertHasLogRecord("changeStatus", Long.class, Integer.class, Long.class, String.class);
    }

    private void assertHasLogRecord(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = MembershipServiceImpl.class.getMethod(methodName, paramTypes);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, () -> methodName + " 缺少 @LogRecord 审计注解");
        assertFalse(logRecord.type().isEmpty(), () -> methodName + " 的 @LogRecord.type 不得为空");
        assertFalse(logRecord.subType().isEmpty(), () -> methodName + " 的 @LogRecord.subType 不得为空");
    }

}
