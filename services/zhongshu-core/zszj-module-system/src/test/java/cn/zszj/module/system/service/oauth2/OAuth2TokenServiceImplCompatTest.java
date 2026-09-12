package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenServiceImpl} 兼容开关（gate=true）分支的单元测试类。
 *
 * <p>ZS-LOGIN-001：选项 C 下现网实际配置为 {@code refresh-token-as-access-token-enabled=true}，
 * 该分支为迁移期兼容路径，必须有测试看守。本类通过 {@link TestPropertySource} 由 Environment 解析注入门控，
 * <b>严禁</b>使用 {@code ReflectionTestUtils.setField}——{@code OAuth2TokenServiceImpl} 含 {@code @Transactional} 方法，
 * 注入的是 CGLIB 代理，setField 会写到代理而非目标对象，造成"改了没生效"的假绿。
 *
 * @author ZS-LOGIN-001
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=true")
public class OAuth2TokenServiceImplCompatTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    public void testGetAccessToken_unexpiredRefreshToken_compatEnabled_shouldConvert() {
        // mock 数据（用户）：ADMIN 类型转换时 buildUserInfo 会调用 adminUserService.getUser，必须打桩避免 NPE
        Long userId = randomLongId();
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(userId)).thenReturn(user);
        // 构造一个未过期的刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setUserId(userId).setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString()).setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();
        assertNull(oauth2AccessTokenRedisDAO.get(accessToken)); // 前置：排除缓存命中

        // 调用：兼容开关开启时，未过期刷新令牌应被转换为访问令牌
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessToken);
        assertNotNull(result, "兼容开关开启时，未过期刷新令牌应被转换为访问令牌");
        assertEquals(accessToken, result.getAccessToken());       // convertToAccessToken 用 refreshToken 串作 accessToken
        assertEquals(refreshTokenDO.getUserId(), result.getUserId());
        assertEquals(refreshTokenDO.getUserType(), result.getUserType());
        assertEquals(refreshTokenDO.getClientId(), result.getClientId());
    }

    @Test
    public void testGetAccessToken_expiredRefreshToken_compatEnabled_shouldReturnNull() {
        // 构造一个已过期的刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class)
                .setUserId(randomLongId()).setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString()).setExpiresTime(LocalDateTime.now().minusDays(1)); // 已过期
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();

        // 调用，并断言：兼容开关开启时，已过期刷新令牌仍不得转换（DateUtils.isExpired 校验须保留）
        assertNull(oauth2TokenService.getAccessToken(accessToken),
                "兼容开关开启时，已过期刷新令牌仍不得转换（DateUtils.isExpired 校验须保留）");
    }

}
