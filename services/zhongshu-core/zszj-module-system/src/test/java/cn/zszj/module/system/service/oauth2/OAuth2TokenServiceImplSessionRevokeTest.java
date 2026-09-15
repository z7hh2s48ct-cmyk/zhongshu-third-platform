package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_TOKEN_SESSION_NOT_OWNED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ZS-LOGIN-006：按「不可用于认证的会话 ID」撤销会话（{@code removeAccessTokenById(id, expectedUserId, expectedUserType)}）的单元测试。
 *
 * <p><b>缺陷（RED 依据）</b>：修复前踢出以原始 accessToken 串标识（{@code removeAccessToken(String)}），
 * 且管理端分页 {@code OAuth2AccessTokenRespVO} 直接回显 accessToken/refreshToken——「持有即可认证」的凭据被
 * 暴露给任何具备查看权限的主体（前端表格 / 浏览器缓存 / 访问日志），踢出还要求前端持有原始令牌串。
 *
 * <p><b>合同（GREEN）</b>：
 * <ol>
 *     <li>会话以 DB 主键 ID（不可用于认证）定位撤销，撤销后访问令牌 / 刷新令牌 / Redis 缓存凭据全部清理；</li>
 *     <li>自助撤销（expectedUserId 非空）只能操作本人会话，操作他人抛 {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}；</li>
 *     <li>管理员撤销（expectedUserId 为空）可撤销本租户任意会话；</li>
 *     <li>不存在的会话 ID 幂等返回 {@code null}（不抛异常）；</li>
 *     <li>跨租户 ID 拒绝（幂等 {@code null}，不回显存在性、不删除他人租户会话）。</li>
 * </ol>
 *
 * <p><b>codex r0 补强合同</b>：
 * <ol>
 *     <li>P1：client_credentials 机器主体（userId=0）不得自助撤销——expectedUserId=0 抛
 *         {@code OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER}，杜绝跨客户端撤销他人 userId=0 会话；</li>
 *     <li>P2：归属校验 userId + userType 双维——同编号跨 ADMIN/MEMBER 类型抛 {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}；</li>
 *     <li>P2（竞态）：撤销以已校验 DO 的 refreshToken 为锁锚重读当前存活代际（而非二次按串重查），
 *         即便目标 ID 对应代际已被并发刷新取代，仍撤销同一 refreshToken 下的当前存活代际 + 刷新令牌。</li>
 * </ol>
 *
 * <p>门控取安全默认 {@code refresh-token-as-access-token-enabled=false}（与 LOGIN-001/003 一致）。
 * 撤销主体逻辑委托与 {@code removeAccessToken(String)} 共享的 {@code revokeSession}（行锁 + 代际清理 + 提交后缓存失效），
 * 本类固化「ID 定位 + 归属 + 类型 + 租户 + 幂等 + 机器主体拒绝」的<b>新增</b>合同。
 *
 * @author ZS-LOGIN-006
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplSessionRevokeTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    @BeforeEach
    public void clearTenantBefore() {
        // 保证租户上下文纯净，避免与其它测试类的 ThreadLocal 泄漏相互干扰
        TenantContextHolder.clear();
    }

    @AfterEach
    public void clearTenantAfter() {
        TenantContextHolder.clear();
    }

    /**
     * 管理员撤销：同一租户内按会话 ID 撤销成功，访问令牌 / 刷新令牌 / Redis 缓存凭据均被清理。
     */
    @Test
    public void testRemoveAccessTokenById_adminRevokeSameTenant_success() {
        // mock 数据（租户 1 的一条存活会话）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        accessTokenDO.setTenantId(1L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(accessTokenDO.getRefreshToken());
        refreshTokenDO.setTenantId(1L);
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        // 调用方处于同一租户
        TenantContextHolder.setTenantId(1L);

        // 调用（expectedUserId=null 表示管理员撤销，不校验归属）
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), null, null);

        // 断言：返回被撤销会话，且访问令牌 / 刷新令牌 / Redis 缓存全部清理
        assertNotNull(result);
        assertEquals(accessTokenDO.getId(), result.getId());
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(accessTokenDO.getRefreshToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()),
                "撤销后缓存凭据必须被清理，不得残留可用幽灵令牌");
    }

    /**
     * 自助撤销：本人（expectedUserId 与会话归属一致）撤销自己的会话成功。
     */
    @Test
    public void testRemoveAccessTokenById_selfRevokeOwnSession_success() {
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(100L)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(accessTokenDO.getRefreshToken());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // 调用（本人 100L 撤销自己的会话，userType 同为 ADMIN）
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(
                accessTokenDO.getId(), 100L, UserTypeEnum.ADMIN.getValue());

        // 断言
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(accessTokenDO.getRefreshToken()));
    }

    /**
     * 归属校验：自助撤销他人会话（expectedUserId 不匹配）必须抛 {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}，
     * 且目标会话不被撤销。
     */
    @Test
    public void testRemoveAccessTokenById_selfRevokeOthersSession_throwsNotOwned() {
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(100L)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);

        // 调用（用户 200L 试图撤销归属于 100L 的会话）
        assertServiceException(
                () -> oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), 200L, UserTypeEnum.ADMIN.getValue()),
                OAUTH2_TOKEN_SESSION_NOT_OWNED);
        // 断言：会话仍然存活（越权撤销未生效）
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()),
                "越权撤销他人会话不得生效");
    }

    /**
     * 幂等：不存在的会话 ID 返回 {@code null}，不抛异常。
     */
    @Test
    public void testRemoveAccessTokenById_notFoundIdempotent() {
        assertNull(oauth2TokenService.removeAccessTokenById(randomLongId(), null, null),
                "不存在的会话 ID 必须幂等返回 null");
    }

    /**
     * 跨租户拒绝：调用方租户与会话归属租户不一致时，幂等返回 {@code null}，且不删除他人租户的会话。
     *
     * <p>生产环境 {@code selectById} 已被租户拦截器追加 {@code tenant_id} 过滤（跨租户查不到 → null）；
     * 服务层再显式复核租户上下文作为纵深防御，杜绝拦截器被绕过 / 误配时的越权撤销。H2 单测无拦截器，
     * 本用例正是校验这条显式防线。
     */
    @Test
    public void testRemoveAccessTokenById_crossTenant_rejectedIdempotent() {
        // mock 数据（租户 1 的存活会话）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        accessTokenDO.setTenantId(1L);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 调用方处于租户 999（跨租户）
        TenantContextHolder.setTenantId(999L);

        // 调用
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), null, null);

        // 断言：拒绝（幂等 null），且他人租户会话未被删除
        assertNull(result, "跨租户会话撤销必须被幂等拒绝（不回显存在性）");
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()),
                "跨租户撤销不得删除他人租户的会话");
    }

    /**
     * P1（codex r0）：client_credentials 机器主体（userId=0）不得自助撤销。
     *
     * <p>两个客户端 A / B 都持 userId=0 的机器令牌，会话表中存在两条 userId=0 记录（不同 clientId）。
     * 客户端 A 以 expectedUserId=0 试图撤销 B 的会话：修复前 userId 归属校验 0==0 放行 → 跨客户端越权撤销；
     * 修复后服务层前置拒绝 expectedUserId<=0，抛 {@code OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER}，两条会话均存活。
     */
    @Test
    public void testRemoveAccessTokenById_clientCredentialsSubject_rejected() {
        // 客户端 A、B 各自的 client_credentials 会话（同租户、userId=0、不同 clientId）
        OAuth2AccessTokenDO sessionA = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(0L).setUserType(UserTypeEnum.ADMIN.getValue()).setClientId("client-A")
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        sessionA.setTenantId(1L);
        oauth2AccessTokenMapper.insert(sessionA);
        OAuth2AccessTokenDO sessionB = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(0L).setUserType(UserTypeEnum.ADMIN.getValue()).setClientId("client-B")
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        sessionB.setTenantId(1L);
        oauth2AccessTokenMapper.insert(sessionB);
        TenantContextHolder.setTenantId(1L);

        // 客户端 A 以机器主体身份（expectedUserId=0）试图撤销 B 的会话
        assertServiceException(
                () -> oauth2TokenService.removeAccessTokenById(sessionB.getId(), 0L, UserTypeEnum.ADMIN.getValue()),
                OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER);
        // 断言：两条机器会话均未被撤销（跨客户端越权被拒）
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(sessionA.getAccessToken()));
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(sessionB.getAccessToken()),
                "机器主体（userId=0）不得自助撤销他人客户端会话");
    }

    /**
     * P2（codex r0）：归属校验 userId + userType 双维。同编号跨 ADMIN/MEMBER 类型不得误撤。
     *
     * <p>MEMBER 用户 100 的会话，被 ADMIN 用户 100 以自助撤销（expectedUserType=ADMIN）指向：
     * userId 相同但 userType 不符，抛 {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}，会话存活。
     */
    @Test
    public void testRemoveAccessTokenById_userTypeMismatch_throwsNotOwned() {
        OAuth2AccessTokenDO memberSession = randomPojo(OAuth2AccessTokenDO.class)
                .setUserId(100L).setUserType(UserTypeEnum.MEMBER.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(memberSession);

        // ADMIN 用户 100 试图撤销 MEMBER 用户 100 的会话（同编号、异类型）
        assertServiceException(
                () -> oauth2TokenService.removeAccessTokenById(
                        memberSession.getId(), 100L, UserTypeEnum.ADMIN.getValue()),
                OAUTH2_TOKEN_SESSION_NOT_OWNED);
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(memberSession.getAccessToken()),
                "同编号跨 userType 不得误撤他人类型会话");
    }

    /**
     * P2（codex r0 竞态）：撤销以已校验 DO 的 refreshToken 为锁锚重读当前存活代际，而非二次按串重查。
     *
     * <p>同一 refreshToken 下存在两条存活访问令牌代际（模拟并发刷新已插入新代际）。按其中一条的会话 ID 撤销时，
     * 修复委托 {@code revokeSession} 走「刷新令牌行锁 + 按 refreshToken 重读存活代际」，两条代际与刷新令牌一并撤销——
     * 这正是消除「校验时 DO 存活、委托按串重查得 null 提前返回、新代际幸存」竞态窗口所依赖的机制。
     * 双连接真实交错由 pg-regression（真实 PG READ COMMITTED）覆盖，本用例固化单进程可判定的重读撤销语义。
     */
    @Test
    public void testRemoveAccessTokenById_revokesCurrentGenerationByRefreshToken() {
        String refreshToken = "RT-shared-generation";
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setRefreshToken(refreshToken);
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 旧代际 A、新代际 B 共享同一 refreshToken（并发刷新提交后 B 已插入）
        OAuth2AccessTokenDO genA = randomPojo(OAuth2AccessTokenDO.class)
                .setRefreshToken(refreshToken).setUserId(100L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(genA);
        OAuth2AccessTokenDO genB = randomPojo(OAuth2AccessTokenDO.class)
                .setRefreshToken(refreshToken).setUserId(100L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(genB);

        // 按旧代际 A 的会话 ID 撤销
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(
                genA.getId(), 100L, UserTypeEnum.ADMIN.getValue());

        // 断言：按 refreshToken 重读，A、B 两代际与刷新令牌全部撤销（新代际不幸存）
        assertNotNull(result);
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(genA.getAccessToken()));
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(genB.getAccessToken()),
                "并发刷新提交的新代际必须一并撤销，不得幸存");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken));
    }

}
