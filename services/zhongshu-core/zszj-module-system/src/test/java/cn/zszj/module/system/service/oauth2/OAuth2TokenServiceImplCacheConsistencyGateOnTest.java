package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
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
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.A（codex r0 P2）：gate=true 下「已缓存转换凭据」（合成令牌，key=refreshToken 串）
 * 的 DB 权威核验——刷新令牌行被撤销后，残留的合成缓存条目必须被拒且自愈 evict。
 *
 * @author ZS-LOGIN-005.A
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=true")
public class OAuth2TokenServiceImplCacheConsistencyGateOnTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private OAuth2ClientService oauth2ClientService;

    @BeforeEach
    public void beforeEach() {
        when(adminUserService.getUser(anyLong())).thenReturn(randomPojo(AdminUserDO.class));
    }

    @AfterEach
    public void afterEach() {
        oauth2AccessTokenRedisDAO.delete("system_oauth2_access_token:*");
    }

    @Test
    public void testCheckAccessToken_syntheticCachedButRefreshRevoked_rejectsAndEvicts() {
        Long userId = randomLongId();
        String refreshToken = randomString();
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO();
        refresh.setRefreshToken(refreshToken).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(randomString())
                .setScopes(java.util.List.of("read")).setExpiresTime(LocalDateTime.now().plusDays(1));
        refresh.setTenantId(1L);
        oauth2RefreshTokenMapper.insert(refresh);

        // 预热：gate=true 下 getAccessToken 把合成凭据写入缓存（key=refreshToken 串）
        OAuth2AccessTokenDO synthetic = oauth2TokenService.getAccessToken(refreshToken);
        assertNotNull(synthetic, "gate=true 时刷新令牌应转换为合成访问令牌");
        assertNotNull(oauth2AccessTokenRedisDAO.get(refreshToken), "前置：合成凭据已缓存");

        // 权威撤销：删除刷新令牌行（DB 层撤销），缓存残留合成条目
        oauth2RefreshTokenMapper.deleteById(refresh.getId());

        // 鉴权必须拒绝（修复前：缓存命中直接放行合成凭据）且自愈 evict
        assertServiceException(() -> oauth2TokenService.checkAccessToken(refreshToken),
                new ErrorCode(401, "访问令牌不存在"));
        assertNull(oauth2AccessTokenRedisDAO.get(refreshToken),
                "DB 权威校验拒绝后必须自愈 evict 合成凭据缓存条目");
    }
}
