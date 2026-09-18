package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
        // ZS-LOGIN-005.B codex r0 P2：懒解析 provider 未注入为 null，处理 ACCESS 行时 getIfAvailable() NPE——
        // 注入 getIfAvailable()=null 的 ObjectProvider mock（与容器无 bean 时 Spring 行为一致 = .A 降级语义）
        @SuppressWarnings("unchecked")
        ObjectProvider<ReliableEventPort> nullProvider = mock(ObjectProvider.class);
        ReflectionTestUtils.setField(service, "reliableEventPortProvider", nullProvider);
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

    @Test
    void batchRevoke_tombstoneFails_deleteAndLaterStepsStillRun() {
        // codex r1 P2：墓碑写入失败后，删缓存与后续步骤（代际键清理）必须继续执行——逐项隔离
        Long userId = 480L;
        String orphan = "fff-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());
        doThrow(new RuntimeException("tombstone down")).when(redisDAO).markRevoked(anyString(), anyLong());

        assertDoesNotThrow(() -> service.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        verify(redisDAO, times(1)).delete(orphan); // 墓碑失败不中断删缓存
        verify(redisDAO, times(1)).deleteSessionGeneration(orphan); // 后续步骤继续
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
