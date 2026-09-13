package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-003：兼容开关开启（{@code refresh-token-as-access-token-enabled=true}，即现网实际配置）下，
 * 用户维度撤销必须清除「已缓存的转换凭据」的单元测试类。
 *
 * <p>背景（ZS-LOGIN-001）：gate=true 时 {@code getAccessToken} 会把「刷新令牌」静默当作「访问令牌」，
 * 并由 {@code convertToAccessToken} 合成一条 {@code accessToken == refreshToken} 的记录写入 Redis，
 * TTL 继承刷新令牌（default client 达 30 天）、<b>不落 system_oauth2_access_token 表</b>。
 *
 * <p><b>缺陷（RED 依据）</b>：修复前 {@code removeAccessToken(userId, userType)} 的刷新凭据集合完全由
 * access-token 记录反推，「无 Access 记录但仍有 Refresh」的孤立刷新凭据会走 early-return —— 于是：
 * <ol>
 *     <li>Redis 里的合成凭据条目<b>无人清理</b>，被禁用 / 被删除 / 已改密的用户仍能凭它通过鉴权达 30 天；</li>
 *     <li>DB 里的刷新令牌行也存活，可随时 {@code refreshAccessToken} 换出全新访问令牌。</li>
 * </ol>
 * 这正是卡片验收项「无 Access 记录但仍有 Refresh 的夹具也不能残留权限」与
 * 「已缓存的转换凭据……被清除，不残留」在现网门控下的真实形态。
 *
 * <p>注：严禁使用 {@code ReflectionTestUtils.setField} 注入门控——{@code OAuth2TokenServiceImpl} 含
 * {@code @Transactional} 方法，注入的是 CGLIB 代理，setField 会写到代理而非目标对象，造成假绿
 * （沿用 {@code OAuth2TokenServiceImplCompatTest} 的既有约定）。
 *
 * @author ZS-LOGIN-003
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=true")
public class OAuth2TokenServiceImplSyntheticCredentialRevokeTest extends BaseDbAndRedisUnitTest {

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

    // ========== ① 孤立刷新凭据 + 已缓存转换凭据 ==========

    /**
     * 场景 ①（卡片明写判据 × 现网门控）：孤立刷新凭据已被当作访问令牌使用并缓存，
     * 用户维度撤销后该凭据必须彻底不可用。
     *
     * <p>RED：修复前撤销是 no-op —— Redis 合成条目与 DB 刷新令牌行都残留，
     * {@code getAccessToken} 命中缓存直接返回，被禁用用户继续持有权限。
     */
    @Test
    public void testRemoveAccessTokenByUser_gateEnabled_orphanRefreshToken_shouldPurgeCachedSyntheticCredential() {
        // 准备：只有刷新令牌，没有任何访问令牌
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId);
        when(adminUserService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String credential = refreshTokenDO.getRefreshToken();
        // 前置：gate=true 下刷新令牌可当访问令牌使用，并被缓存进 Redis（产生「已缓存的转换凭据」）
        assertNotNull(oauth2TokenService.getAccessToken(credential),
                "前置：gate=true 时刷新令牌应可被当作访问令牌使用");
        assertNotNull(oauth2AccessTokenRedisDAO.get(credential),
                "前置：转换凭据应已被缓存进 Redis");
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size(),
                "前置：该夹具不应有 access-token 记录（孤立刷新凭据）");

        // 调用：模拟禁用 / 删除 / 改密触发的用户维度撤销
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：已缓存的转换凭据被清除
        assertNull(oauth2AccessTokenRedisDAO.get(credential),
                "已缓存的转换凭据（合成访问令牌）必须被清除，不得残留至刷新令牌 TTL");
        // 断言：DB 刷新令牌行被撤销
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(credential),
                "孤立刷新凭据必须被撤销");
        // 断言：凭据彻底不可用
        assertNull(oauth2TokenService.getAccessToken(credential), "撤销后该凭据不得再被解析为访问令牌");
        assertServiceException(() -> oauth2TokenService.checkAccessToken(credential),
                new ErrorCode(401, "访问令牌不存在"));
        // 断言：也不能再换出新访问令牌
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(credential, clientId),
                new ErrorCode(400, "无效的刷新令牌"));
    }

    // ========== ② 正常会话 + 同时被当作访问令牌缓存 ==========

    /**
     * 场景 ②：正常会话（refresh + access），且刷新令牌也被当作访问令牌缓存过（WebSocket 握手场景）。
     * 撤销后两个 Redis 条目、两行 DB 记录都必须清除。
     *
     * <p>本例作为<b>回归看守</b>：access-token 存在时既有实现已会 {@code delete(refreshToken)}，
     * 改造不得把它弄坏。
     */
    @Test
    public void testRemoveAccessTokenByUser_gateEnabled_normalSession_shouldPurgeBothCredentials() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId);
        when(adminUserService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        OAuth2AccessTokenDO accessTokenDO = seedAccessToken(refreshToken, userId, clientId);
        // 刷新令牌也被当作访问令牌使用过（gate=true 兼容路径）
        assertNotNull(oauth2TokenService.getAccessToken(refreshToken));

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.getAccessToken()), "访问令牌行应被撤销");
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()), "访问令牌缓存应被清除");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken), "刷新令牌行应被撤销");
        assertNull(oauth2AccessTokenRedisDAO.get(refreshToken), "已缓存的转换凭据应被清除");
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken), "会话代际键应被清理");
    }

    /**
     * 场景 ②-2：gate=true 下，撤销后即便 Redis 被重新预热（再次 getAccessToken），
     * 也不得因 DB 刷新令牌残留而重新合成出可用凭据。
     */
    @Test
    public void testRemoveAccessTokenByUser_gateEnabled_shouldNotRehydrateSyntheticCredential() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId);
        when(adminUserService.getUser(eq(userId))).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String credential = refreshTokenDO.getRefreshToken();
        assertNotNull(oauth2TokenService.getAccessToken(credential), "前置：应已合成并缓存");

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        // 再次尝试预热（模拟撤销后仍有请求携旧凭据到达）
        assertNull(oauth2TokenService.getAccessToken(credential),
                "撤销后不得由残留的 DB 刷新令牌重新合成出可用凭据");
        assertNull(oauth2AccessTokenRedisDAO.get(credential), "不得重新写入缓存");
    }

    // ========== 夹具 ==========

    private void mockClient(String clientId) {
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(300).setRefreshTokenValiditySeconds(3600);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
    }

    private OAuth2RefreshTokenDO seedRefreshToken(String clientId, Long userId) {
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class, o -> o
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().plusDays(1))
                .setTenantId(0L));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        return refreshTokenDO;
    }

    private OAuth2AccessTokenDO seedAccessToken(String refreshToken, Long userId, String clientId) {
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o
                .setRefreshToken(refreshToken)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().plusMinutes(30))
                .setTenantId(0L));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        return accessTokenDO;
    }

}
