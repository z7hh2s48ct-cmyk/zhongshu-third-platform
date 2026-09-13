import io

# ===== A) CacheConsistencyTest: in-tx assertions =====
p = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImplCacheConsistencyTest.java"
s = io.open(p, encoding='utf-8').read()

old1 = '''        try {
            transactionTemplate.executeWithoutResult(status -> {
                OAuth2AccessTokenDO created = oauth2TokenService.createAccessToken(userId,
                        UserTypeEnum.ADMIN.getValue(), clientId, java.util.List.of("read"));
                tokenRef.set(created.getAccessToken());
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }'''
new1 = '''        try {
            transactionTemplate.executeWithoutResult(status -> {
                OAuth2AccessTokenDO created = oauth2TokenService.createAccessToken(userId,
                        UserTypeEnum.ADMIN.getValue(), clientId, java.util.List.of("read"));
                tokenRef.set(created.getAccessToken());
                // ZS-LOGIN-005.A codex r0 P2：发布必须推迟到提交后——事务内不得已出现在缓存
                assertNull(oauth2AccessTokenRedisDAO.get(tokenRef.get()),
                        "事务提交前新令牌缓存不得提前发布");
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }'''
assert old1 in s, "old1"
s = s.replace(old1, new1)

old2 = '''        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "提交后撤销的缓存失效必须已执行");'''
new2 = '''        transactionTemplate.executeWithoutResult(status -> {
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            // ZS-LOGIN-005.A codex r0 P2：失效必须推迟到提交后——事务内缓存条目仍在（不得抢跑）
            assertNotNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                    "事务提交前撤销的缓存失效不得抢跑执行");
        });

        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "提交后撤销的缓存失效必须已执行");'''
assert old2 in s, "old2"
s = s.replace(old2, new2)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('consistency test ok')

# ===== B) gate=true synthetic authority test class =====
gate_test = '''package cn.zszj.module.system.service.oauth2;

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
        refresh.setUserInfo(java.util.Map.of("nickname", "gate-user"));
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
'''
io.open(r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImplCacheConsistencyGateOnTest.java", 'w', encoding='utf-8', newline='\n').write(gate_test)
print('gate test ok')

# ===== C) mock-based authority/failure unit tests =====
mock_test = '''package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.A：鉴权权威核验与故障注入的纯 mock 单元测试。
 *
 * <p>覆盖 codex r0 P2 要求的故障路径：①DB 权威核验异常 → 失败关闭（401）；②Redis 删除/墓碑失败 →
 * 撤销仍以 DB 为权威成功（不阻断、逐项隔离继续执行剩余步骤）。
 *
 * @author ZS-LOGIN-005.A
 */
public class OAuth2TokenServiceImplAuthorityUnitTest {

    private OAuth2TokenServiceImpl service;
    private OAuth2AccessTokenMapper accessTokenMapper;
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    private OAuth2AccessTokenRedisDAO redisDAO;

    @BeforeEach
    void setUp() {
        service = new OAuth2TokenServiceImpl();
        accessTokenMapper = mock(OAuth2AccessTokenMapper.class);
        refreshTokenMapper = mock(OAuth2RefreshTokenMapper.class);
        redisDAO = mock(OAuth2AccessTokenRedisDAO.class);
        ReflectionTestUtils.setField(service, "oauth2AccessTokenMapper", accessTokenMapper);
        ReflectionTestUtils.setField(service, "oauth2RefreshTokenMapper", refreshTokenMapper);
        ReflectionTestUtils.setField(service, "oauth2AccessTokenRedisDAO", redisDAO);
        ReflectionTestUtils.setField(service, "oauth2CodeMapper", mock(OAuth2CodeMapper.class));
        ReflectionTestUtils.setField(service, "adminUserMapper", mock(cn.zszj.module.system.dal.mysql.user.AdminUserMapper.class));
        ReflectionTestUtils.setField(service, "refreshTokenAsAccessTokenEnabled", false);
    }

    private OAuth2AccessTokenDO cachedToken() {
        OAuth2AccessTokenDO at = new OAuth2AccessTokenDO();
        at.setId(1L);
        at.setAccessToken("at-authority");
        at.setRefreshToken("rt-1");
        at.setUserId(100L);
        at.setUserType(UserTypeEnum.ADMIN.getValue());
        at.setExpiresTime(LocalDateTime.now().plusMinutes(30));
        return at;
    }

    @Test
    void checkAccessToken_dbAuthorityQueryThrows_failClosed() {
        when(redisDAO.get("at-authority")).thenReturn(cachedToken());
        when(accessTokenMapper.selectAuthorityCountByAccessToken("at-authority"))
                .thenThrow(new RuntimeException("pg jitter"));

        // 失败关闭：DB 核验异常宁可拒绝，不放行可能已撤销的凭据
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.checkAccessToken("at-authority"));
        assertEquals(401, ex.getCode());
    }

    @Test
    void checkAccessToken_dbAuthorityCountZero_rejectsAndSelfHeals() {
        when(redisDAO.get("at-authority")).thenReturn(cachedToken());
        when(accessTokenMapper.selectAuthorityCountByAccessToken("at-authority")).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.checkAccessToken("at-authority"));
        assertEquals(401, ex.getCode());
        // 自愈：落墓碑 + evict
        verify(redisDAO, times(1)).delete("at-authority");
    }

    @Test
    void checkAccessToken_dbAuthorityAlive_passes() {
        OAuth2AccessTokenDO cached = cachedToken();
        when(redisDAO.get("at-authority")).thenReturn(cached);
        when(accessTokenMapper.selectAuthorityCountByAccessToken("at-authority")).thenReturn(1);

        assertDoesNotThrow(() -> service.checkAccessToken("at-authority"));
    }

    @Test
    void batchRevoke_redisFailure_dbStillAuthoritativeAndStepsIsolated() {
        Long userId = 470L;
        String orphan = "eee-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());
        // Redis 全线故障
        doThrow(new RuntimeException("redis down")).when(redisDAO).delete(anyString());
        doThrow(new RuntimeException("redis down")).when(redisDAO).markRevoked(anyString(), anyLong());

        // 撤销不得因 Redis 失败而失败——DB 权威删除照常完成
        assertDoesNotThrow(() -> service.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
    }

    private static cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO makeRefreshToken(
            String refreshToken, Long userId) {
        cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO rt =
                new cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO();
        rt.setId(refreshToken.hashCode() & 0x7fffffffL);
        rt.setRefreshToken(refreshToken);
        rt.setUserId(userId);
        rt.setUserType(UserTypeEnum.ADMIN.getValue());
        rt.setExpiresTime(LocalDateTime.now().plusDays(1));
        return rt;
    }
}
'''
io.open(r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImplAuthorityUnitTest.java", 'w', encoding='utf-8', newline='\n').write(mock_test)
print('mock test ok')
