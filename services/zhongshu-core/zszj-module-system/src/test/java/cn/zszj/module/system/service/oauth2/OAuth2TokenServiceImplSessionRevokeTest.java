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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ZS-LOGIN-006：按「不可用于认证的会话 ID」撤销会话（{@code removeAccessTokenById(id, expectedUserId)}）的单元测试。
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
 * <p>门控取安全默认 {@code refresh-token-as-access-token-enabled=false}（与 LOGIN-001/003 一致）。
 * 撤销主体逻辑委托既经受测的 {@code removeAccessToken(String)}（行锁 + 代际清理 + 提交后缓存失效），
 * 本类只固化「ID 定位 + 归属 + 租户 + 幂等」的<b>新增</b>合同。
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
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), null);

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

        // 调用（本人 100L 撤销自己的会话）
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), 100L);

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
                () -> oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), 200L),
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
        assertNull(oauth2TokenService.removeAccessTokenById(randomLongId(), null),
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
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessTokenById(accessTokenDO.getId(), null);

        // 断言：拒绝（幂等 null），且他人租户会话未被删除
        assertNull(result, "跨租户会话撤销必须被幂等拒绝（不回显存在性）");
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()),
                "跨租户撤销不得删除他人租户的会话");
    }

}
