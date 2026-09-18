package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-002 P2-A RED/GREEN 测试：验证批量撤销按 refresh-token 自然序获取行锁。
 *
 * <p>RED（修复前）：锁获取顺序跟随 access-token 查询结果顺序（不稳定）→ InOrder 断言失败。
 * <p>GREEN（修复后）：锁获取顺序按 refresh-token 自然序排序 → InOrder 断言通过。
 *
 * <p>使用裸 new + mock 绕开 Spring CGLIB 代理，精确控制 mapper 返回顺序。
 */
public class OAuth2TokenServiceImplBatchLockOrderUnitTest {

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
        // ZS-LOGIN-003 codex r1 P1：用户级撤销现在还会失效未消费授权码，裸构造实例需注入 mapper mock
        ReflectionTestUtils.setField(service, "oauth2CodeMapper", mock(OAuth2CodeMapper.class));
        // ZS-LOGIN-003 codex r2 P1：撤销现在先取用户行锁（统一锁序最外层），注入 AdminUserMapper mock
        ReflectionTestUtils.setField(service, "adminUserMapper", mock(AdminUserMapper.class));
        // ZS-LOGIN-005.B codex r0 P2：懒解析 provider 未注入为 null，处理 ACCESS 行时 getIfAvailable() NPE——
        // 注入 getIfAvailable()=null 的 ObjectProvider mock（与容器无 bean 时 Spring 行为一致 = .A 降级语义）
        @SuppressWarnings("unchecked")
        ObjectProvider<ReliableEventPort> nullProvider = mock(ObjectProvider.class);
        ReflectionTestUtils.setField(service, "reliableEventPortProvider", nullProvider);
    }

    /**
     * 核心 RED 测试：access-token 查询返回 [R_zzz, R_aaa] 顺序（ID 序），
     * 修复后锁获取必须按字母序 [R_aaa, R_zzz]。
     */
    @Test
    void batchRevoke_mustAcquireLocksInSortedRefreshTokenOrder() {
        Long userId = 100L;
        Integer userType = UserTypeEnum.ADMIN.getValue();

        // 模拟查询返回：R_zzz 的 access-token 在前（ID 较小），R_aaa 在后
        String rZzz = "zzz-refresh-token";
        String rAaa = "aaa-refresh-token";
        List<OAuth2AccessTokenDO> tokens = new ArrayList<>();
        tokens.add(makeAccessToken("at-1", rZzz, userId, userType));
        tokens.add(makeAccessToken("at-2", rAaa, userId, userType));
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, userType)).thenReturn(tokens);

        // 模拟 FOR UPDATE 返回（锁获取成功）
        when(refreshTokenMapper.selectByRefreshTokenForUpdate(anyString()))
                .thenAnswer(inv -> makeRefreshToken(inv.getArgument(0), userId, userType));
        // 模拟 listAliveAccessTokens 查询
        when(accessTokenMapper.selectListByRefreshToken(rZzz))
                .thenReturn(List.of(makeAccessToken("at-1", rZzz, userId, userType)));
        when(accessTokenMapper.selectListByRefreshToken(rAaa))
                .thenReturn(List.of(makeAccessToken("at-2", rAaa, userId, userType)));

        // 调用
        service.removeAccessToken(userId, userType);

        // 断言：锁获取顺序必须是 R_aaa 先于 R_zzz（字母序）
        InOrder lockOrder = inOrder(refreshTokenMapper);
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(rAaa);
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(rZzz);
    }

    /**
     * 验证去重：同一 refresh-token 下有多条 access-token 时，只锁一次。
     */
    @Test
    void batchRevoke_deduplicatesRefreshTokens() {
        Long userId = 200L;
        Integer userType = UserTypeEnum.ADMIN.getValue();

        String r1 = "bbb-refresh";
        String r2 = "aaa-refresh";
        List<OAuth2AccessTokenDO> tokens = new ArrayList<>();
        tokens.add(makeAccessToken("at-1", r1, userId, userType));
        tokens.add(makeAccessToken("at-2", r1, userId, userType)); // 同一 refresh-token
        tokens.add(makeAccessToken("at-3", r2, userId, userType));
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, userType)).thenReturn(tokens);

        when(refreshTokenMapper.selectByRefreshTokenForUpdate(anyString()))
                .thenAnswer(inv -> makeRefreshToken(inv.getArgument(0), userId, userType));
        when(accessTokenMapper.selectListByRefreshToken(anyString())).thenReturn(List.of());

        service.removeAccessToken(userId, userType);

        // 断言：R_aaa 先于 R_bbb（排序），且每个只锁一次（去重）
        InOrder lockOrder = inOrder(refreshTokenMapper);
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(r2); // "aaa-refresh"
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(r1); // "bbb-refresh"
        // 不应有第三次锁获取
        verify(refreshTokenMapper, never()).selectByRefreshTokenForUpdate(
                org.mockito.ArgumentMatchers.argThat(arg ->
                        !arg.equals(r1) && !arg.equals(r2)));
    }

    /**
     * 空列表时直接返回，不获取任何锁。
     */
    @Test
    void batchRevoke_emptyTokens_noLocks() {
        Long userId = 300L;
        Integer userType = UserTypeEnum.ADMIN.getValue();
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, userType)).thenReturn(List.of());

        service.removeAccessToken(userId, userType);

        verify(refreshTokenMapper, never()).selectByRefreshTokenForUpdate(anyString());
    }

    // ========== 工具 ==========

    private static OAuth2AccessTokenDO makeAccessToken(String accessToken, String refreshToken,
                                                       Long userId, Integer userType) {
        OAuth2AccessTokenDO at = new OAuth2AccessTokenDO();
        at.setAccessToken(accessToken);
        at.setRefreshToken(refreshToken);
        at.setUserId(userId);
        at.setUserType(userType);
        at.setClientId("test-client");
        at.setExpiresTime(LocalDateTime.now().plusHours(1));
        at.setTenantId(0L);
        return at;
    }

    private static OAuth2RefreshTokenDO makeRefreshToken(String refreshToken, Long userId, Integer userType) {
        OAuth2RefreshTokenDO rt = new OAuth2RefreshTokenDO();
        rt.setRefreshToken(refreshToken);
        rt.setUserId(userId);
        rt.setUserType(userType);
        rt.setClientId("test-client");
        rt.setExpiresTime(LocalDateTime.now().plusDays(1));
        rt.setTenantId(0L);
        return rt;
    }
}
