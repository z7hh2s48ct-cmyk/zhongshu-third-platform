package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-003：用户维度撤销（{@code removeAccessToken(userId, userType)}）的锁序 / 去重 / 租户上下文单元测试。
 *
 * <p>沿用 {@code OAuth2TokenServiceImplBatchLockOrderUnitTest} 的手法：裸 {@code new} + mock，
 * 绕开 Spring CGLIB 代理，精确控制 mapper 返回集合与调用顺序。
 *
 * <p><b>RED 依据</b>：修复前刷新凭据集合完全由 access-token 记录反推，且 access-token 为空时首句 early-return，
 * 因此「孤立刷新凭据」既不会被发现、也不会被加锁撤销；同时整个撤销受调用方 ThreadLocal 租户上下文约束，
 * 在无租户上下文（定时任务 / MQ 消费）或跨租户运维调用时会静默收窄甚至 NPE。
 *
 * @author ZS-LOGIN-003
 */
public class OAuth2TokenServiceImplUserRevokeLockOrderUnitTest {

    private static final Integer USER_TYPE = UserTypeEnum.ADMIN.getValue();

    private OAuth2TokenServiceImpl service;
    private OAuth2AccessTokenMapper accessTokenMapper;
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    private OAuth2AccessTokenRedisDAO redisDAO;
    private AdminUserMapper adminUserMapper;

    @BeforeEach
    void setUp() {
        service = new OAuth2TokenServiceImpl();
        accessTokenMapper = mock(OAuth2AccessTokenMapper.class);
        refreshTokenMapper = mock(OAuth2RefreshTokenMapper.class);
        redisDAO = mock(OAuth2AccessTokenRedisDAO.class);
        adminUserMapper = mock(AdminUserMapper.class);
        ReflectionTestUtils.setField(service, "oauth2AccessTokenMapper", accessTokenMapper);
        ReflectionTestUtils.setField(service, "oauth2RefreshTokenMapper", refreshTokenMapper);
        ReflectionTestUtils.setField(service, "oauth2AccessTokenRedisDAO", redisDAO);
        // ZS-LOGIN-003 codex r1 P1：用户级撤销现在还会失效未消费授权码，裸构造实例需注入 mapper mock
        ReflectionTestUtils.setField(service, "oauth2CodeMapper", mock(OAuth2CodeMapper.class));
        // ZS-LOGIN-003 codex r2 P1：撤销现在先取用户行锁（统一锁序最外层），注入 AdminUserMapper mock
        ReflectionTestUtils.setField(service, "adminUserMapper", adminUserMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.setIgnore(false);
        TenantContextHolder.setTenantId(null);
    }

    /**
     * 核心 RED 测试：无 access-token、只有孤立 refresh-token 时，仍必须加锁并撤销。
     *
     * <p>RED（修复前）：access-token 集合为空 → 首句 early-return → 不加锁、不删除、不清缓存。
     * <p>GREEN（修复后）：refresh-token 直查补齐全集 → 行锁 → 删刷新令牌 → 清 Redis 凭据与代际键。
     */
    @Test
    void batchRevoke_orphanRefreshToken_mustStillLockAndRevoke() {
        Long userId = 100L;
        String orphan = "aaa-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        // 断言：先锁刷新令牌行（复用 ZS-LOGIN-002 行锁，不新造锁）
        verify(refreshTokenMapper, times(1)).selectByRefreshTokenForUpdate(orphan);
        // 断言：刷新令牌被撤销
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
        // 断言：Redis 中以 refreshToken 串为 key 的「已缓存转换凭据」被清除
        verify(redisDAO, times(1)).delete(orphan);
        // 断言：会话代际键被清理
        verify(redisDAO, times(1)).deleteSessionGeneration(orphan);
    }

    /**
     * 孤立刷新凭据下若被并发刷新插入了访问令牌，获锁后重读必须把它们一并撤销。
     */
    @Test
    void batchRevoke_orphanRefreshToken_mustRevokeAliveAccessTokensFoundAfterLock() {
        Long userId = 110L;
        String orphan = "bbb-orphan-refresh";
        OAuth2AccessTokenDO alive = makeAccessToken("at-alive", orphan, userId);
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of(alive));

        service.removeAccessToken(userId, USER_TYPE);

        verify(accessTokenMapper, times(1)).deleteById(alive.getId());
        verify(redisDAO, times(1)).delete("at-alive");
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
        verify(redisDAO, times(1)).delete(orphan);
    }

    /**
     * 混合来源（access-token 反推 + refresh-token 直查）时，锁获取顺序必须仍是 refresh-token 自然序。
     *
     * <p>RED（修复前）：只会锁 access-token 反推出的 R_zzz，孤立凭据 R_aaa 完全不加锁。
     * <p>GREEN（修复后）：并集去重排序 → [R_aaa, R_zzz]。
     */
    @Test
    void batchRevoke_mixedSources_mustAcquireLocksInSortedRefreshTokenOrder() {
        Long userId = 200L;
        String rZzz = "zzz-refresh";
        String rAaa = "aaa-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeAccessToken("at-1", rZzz, userId)));
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeRefreshToken(rZzz, userId), makeRefreshToken(rAaa, userId)));
        when(accessTokenMapper.selectListByRefreshToken(anyString())).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        InOrder lockOrder = inOrder(refreshTokenMapper);
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(rAaa);
        lockOrder.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(rZzz);
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(rAaa);
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(rZzz);
    }

    /**
     * 两个来源命中同一 refresh-token 时必须去重，只加一次锁、只删一次。
     */
    @Test
    void batchRevoke_deduplicatesAcrossBothSources() {
        Long userId = 300L;
        String shared = "mmm-shared-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeAccessToken("at-1", shared, userId), makeAccessToken("at-2", shared, userId)));
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeRefreshToken(shared, userId)));
        when(accessTokenMapper.selectListByRefreshToken(shared)).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        verify(refreshTokenMapper, times(1)).selectByRefreshTokenForUpdate(shared);
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(shared);
        verify(redisDAO, times(1)).deleteSessionGeneration(shared);
    }

    /**
     * 两个来源都为空时，不得加任何锁（无害空操作）。
     */
    @Test
    void batchRevoke_noSessionAtAll_mustNotAcquireAnyLock() {
        Long userId = 400L;
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        verify(refreshTokenMapper, never()).selectByRefreshTokenForUpdate(anyString());
        verify(refreshTokenMapper, never()).deleteByRefreshToken(anyString());
    }

    /**
     * ZS-LOGIN-003 codex r2 P1：统一锁序——用户行锁必须先于任何刷新令牌行锁获取，
     * 与兑换路径（grantAuthorizationCodeForAccessToken 的 selectByIdForUpdate）构成同一把外层锁，
     * 杜绝「兑换读到启用态 → 撤销提交 → 兑换仍建新会话」交错。
     */
    @Test
    void batchRevoke_mustAcquireUserRowLockBeforeAnyRefreshTokenLock() {
        Long userId = 450L;
        String orphan = "ddd-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        InOrder outerMostFirst = inOrder(adminUserMapper, refreshTokenMapper);
        outerMostFirst.verify(adminUserMapper).selectByIdForUpdate(userId);
        outerMostFirst.verify(refreshTokenMapper).selectByRefreshTokenForUpdate(orphan);
    }

    /**
     * ZS-LOGIN-003 codex r2 P1：用户取得 code 后退出登录再被改密/禁用时无任何会话，
     * 未消费授权码仍必须被失效（不得因空会话提前返回而残留可兑换的旧 code）。
     */
    @Test
    void batchRevoke_noSessionButUnconsumedCode_mustStillRevokeCodes() {
        Long userId = 460L;
        OAuth2CodeMapper codeMapper = (OAuth2CodeMapper) ReflectionTestUtils.getField(service, "oauth2CodeMapper");
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(codeMapper.deleteByUserIdAndUserType(userId, USER_TYPE)).thenReturn(2);

        service.removeAccessToken(userId, USER_TYPE);

        verify(codeMapper, times(1)).deleteByUserIdAndUserType(userId, USER_TYPE);
    }

    /**
     * 撤销是安全操作，必须在「忽略租户」作用域内执行，不得被调用方 ThreadLocal 租户上下文静默收窄。
     *
     * <p>RED（修复前）：撤销随调用方租户过滤 —— 在定时任务 / MQ 消费等无租户上下文下
     * {@code TenantDatabaseInterceptor#getTenantId()} 会因 {@code getRequiredTenantId()} 抛 NPE，
     * 在跨租户运维调用下则查不到目标用户的凭据 → 撤销静默 no-op，被禁用用户的会话继续存活。
     * <p>GREEN（修复后）：全部 mapper 调用都发生在 {@code TenantContextHolder.isIgnore() == true} 作用域内，
     * 且退出后恢复原值（不污染调用方上下文）。
     */
    @Test
    void batchRevoke_mustRunUnderTenantIgnoreAndRestoreContext() {
        Long userId = 500L;
        String orphan = "ccc-orphan-refresh";
        // 调用方带着一个「错误」的租户上下文（模拟跨租户运维 / 上下文污染）
        TenantContextHolder.setTenantId(999L);
        TenantContextHolder.setIgnore(false);

        AtomicBoolean ignoreOnRefreshQuery = new AtomicBoolean(false);
        AtomicBoolean ignoreOnLock = new AtomicBoolean(false);
        AtomicBoolean ignoreOnDelete = new AtomicBoolean(false);
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, USER_TYPE)).thenAnswer(invocation -> {
            ignoreOnRefreshQuery.set(TenantContextHolder.isIgnore());
            return List.of(makeRefreshToken(orphan, userId));
        });
        when(refreshTokenMapper.selectByRefreshTokenForUpdate(anyString())).thenAnswer(invocation -> {
            ignoreOnLock.set(TenantContextHolder.isIgnore());
            return makeRefreshToken(invocation.getArgument(0), userId);
        });
        when(refreshTokenMapper.deleteByRefreshToken(anyString())).thenAnswer(invocation -> {
            ignoreOnDelete.set(TenantContextHolder.isIgnore());
            return 1;
        });
        when(accessTokenMapper.selectListByRefreshToken(anyString())).thenReturn(List.of());

        service.removeAccessToken(userId, USER_TYPE);

        assertTrue(ignoreOnRefreshQuery.get(), "刷新令牌直查必须在忽略租户作用域内执行");
        assertTrue(ignoreOnLock.get(), "行锁必须在忽略租户作用域内获取");
        assertTrue(ignoreOnDelete.get(), "刷新令牌删除必须在忽略租户作用域内执行");
        // 断言：退出后恢复调用方上下文，不得污染
        assertFalse(TenantContextHolder.isIgnore(), "撤销结束后必须恢复调用方的租户忽略标记");
        assertTrue(Long.valueOf(999L).equals(TenantContextHolder.getTenantId()),
                "撤销结束后必须恢复调用方的租户编号");
    }

    // ========== 工具 ==========

    private static OAuth2AccessTokenDO makeAccessToken(String accessToken, String refreshToken, Long userId) {
        OAuth2AccessTokenDO at = new OAuth2AccessTokenDO();
        at.setId(accessToken.hashCode() & 0x7fffffffL);
        at.setAccessToken(accessToken);
        at.setRefreshToken(refreshToken);
        at.setUserId(userId);
        at.setUserType(USER_TYPE);
        at.setClientId("test-client");
        at.setExpiresTime(LocalDateTime.now().plusHours(1));
        at.setTenantId(0L);
        return at;
    }

    private static OAuth2RefreshTokenDO makeRefreshToken(String refreshToken, Long userId) {
        OAuth2RefreshTokenDO rt = new OAuth2RefreshTokenDO();
        rt.setId(refreshToken.hashCode() & 0x7fffffffL);
        rt.setRefreshToken(refreshToken);
        rt.setUserId(userId);
        rt.setUserType(USER_TYPE);
        rt.setClientId("test-client");
        rt.setExpiresTime(LocalDateTime.now().plusDays(1));
        rt.setTenantId(0L);
        return rt;
    }
}
