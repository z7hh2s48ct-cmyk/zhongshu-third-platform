package cn.zszj.module.system.service.oauth2;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-003：用户维度撤销（{@code removeAccessToken(userId, userType)}）的「孤立刷新凭据」单元测试类。
 *
 * <p>门控取<b>安全默认</b> {@code refresh-token-as-access-token-enabled=false}（与 LOGIN-001 一致）；
 * gate=true 的「已缓存转换凭据」路径由 {@code OAuth2TokenServiceImplSyntheticCredentialRevokeTest} 看守。
 *
 * <p><b>缺陷（RED 依据）</b>：修复前 {@code removeAccessToken(userId, userType)} 的刷新凭据集合
 * 完全<b>由 access-token 记录反推</b>，且在 access-token 为空时首句即 early-return。于是：
 * <ol>
 *     <li>「无 Access 记录但仍有 Refresh」的孤立刷新凭据<b>永远不会被撤销</b>——
 *         它仍可用于 {@code refreshAccessToken} 换出全新访问令牌（会话复活），
 *         在 gate=true 下更可直接当作访问令牌通过鉴权；</li>
 *     <li>LOGIN-001 兼容路径写入 Redis 的「合成访问令牌」条目（key = refreshToken 串）
 *         对孤立刷新凭据同样无人清理，会一直存活到刷新令牌 TTL（default client 达 30 天）。</li>
 * </ol>
 *
 * <p><b>合同（GREEN）</b>：用户维度撤销必须取「access-token 反推 ∪ refresh-token 直查」的<b>全集</b>，
 * 沿用 ZS-LOGIN-002 的<b>去重 + 自然序排序 + 行锁 + 获锁后重读</b>，逐个撤销访问令牌、刷新令牌、
 * Redis 缓存凭据（含合成条目）与会话代际键。
 *
 * <p><b>范围边界</b>：仅技术账号闭环（B03），不涉及任职撤销（B07 / D-09 后）。
 *
 * @author ZS-LOGIN-003
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplOrphanRevokeTest extends BaseDbAndRedisUnitTest {

    /**
     * 并发任务的硬超时（秒）：避免死锁 / 连接池耗尽时无限挂起 CI。
     */
    private static final long CONCURRENT_TIMEOUT_SECONDS = 120L;
    /**
     * 竞态窗口放大延时（毫秒）：注入在「读刷新令牌（已持行锁）」之后、「删旧插新访问令牌」之前。
     */
    private static final long RACE_WINDOW_MILLIS = 200L;

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;
    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;

    @BeforeEach
    public void before() {
        serviceLogger = (Logger) LoggerFactory.getLogger(OAuth2TokenServiceImpl.class);
        serviceLogger.setLevel(Level.INFO);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    public void after() {
        if (serviceLogger != null && logAppender != null) {
            serviceLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    // ========== ① 孤立刷新凭据：不得残留权限 ==========

    /**
     * 场景 ①（卡片明写判据）：无 Access 记录但仍有 Refresh 的夹具，撤销后不得残留任何权限。
     *
     * <p>RED：修复前 access-token 集合为空 → 首句 early-return → 刷新令牌行、Redis 合成条目、
     * 会话代际键<b>全部残留</b>。
     */
    @Test
    public void testRemoveAccessTokenByUser_orphanRefreshToken_shouldLeaveNoResidualPrivilege() {
        // 准备：只有刷新令牌，没有任何访问令牌（孤立刷新凭据）
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        // 额外污染：LOGIN-001 兼容路径会把「合成访问令牌」写进 Redis（key = refreshToken 串）
        cacheSyntheticAccessToken(refreshToken, userId, clientId);
        // 额外污染：留下会话代际键
        oauth2AccessTokenRedisDAO.nextSessionGeneration(refreshToken, 60_000L);
        // 前置断言
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size(),
                "前置：该夹具不应有 access-token 记录");
        assertNotNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken), "前置：刷新令牌应存活");
        assertNotNull(oauth2AccessTokenRedisDAO.get(refreshToken), "前置：Redis 应存在已缓存的转换凭据");

        // 调用：模拟禁用 / 删除 / 改密触发的用户维度撤销
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：刷新令牌行必须被撤销
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken),
                "孤立刷新凭据必须被撤销，否则可继续换出新访问令牌（残留权限）");
        // 断言：已缓存的转换凭据必须被清除
        assertNull(oauth2AccessTokenRedisDAO.get(refreshToken),
                "Redis 中「刷新令牌当访问令牌」的已缓存转换凭据必须被清除");
        // 断言：会话代际键必须随会话终结清理
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken),
                "会话终结后会话代际键必须被清理");
        // 断言：不得残留任何访问令牌
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size());
    }

    /**
     * 场景 ①-2：孤立刷新凭据被撤销后，再用它刷新必须被拒绝（会话不得复活）。
     *
     * <p>RED：修复前撤销是 no-op，刷新会成功换出全新访问令牌 → 被禁用 / 被删除的用户重新获得权限。
     */
    @Test
    public void testRemoveAccessTokenByUser_orphanRefreshToken_shouldRejectSubsequentRefresh() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：刷新被拒绝，且不产生任何访问令牌
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(400, "无效的刷新令牌"));
        assertEquals(0, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(),
                "撤销后不得由孤立刷新凭据换出新访问令牌（会话复活）");
    }

    /**
     * 场景 ①-3：孤立刷新凭据在撤销后也不得通过 {@code checkAccessToken}（即使它曾被当作访问令牌缓存）。
     */
    @Test
    public void testRemoveAccessTokenByUser_orphanRefreshToken_shouldFailCheckAccessToken() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        cacheSyntheticAccessToken(refreshToken, userId, clientId);

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：合成凭据不可用
        assertNull(oauth2TokenService.getAccessToken(refreshToken), "撤销后合成访问令牌不得可用");
        assertServiceException(() -> oauth2TokenService.checkAccessToken(refreshToken),
                new ErrorCode(401, "访问令牌不存在"));
    }

    // ========== ② 混合会话：正常会话与孤立刷新凭据都要撤销 ==========

    /**
     * 场景 ②：用户同时拥有「有 access-token 的正常会话」与「孤立刷新凭据」，撤销必须覆盖全集。
     *
     * <p>RED：修复前只撤销由 access-token 反推出的那一个会话，孤立刷新凭据存活。
     */
    @Test
    public void testRemoveAccessTokenByUser_mixedNormalAndOrphan_shouldRevokeAll() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        // 会话 A：正常（refresh + access）
        OAuth2RefreshTokenDO normalRefresh = seedRefreshToken(clientId, userId);
        OAuth2AccessTokenDO normalAccess = seedAccessToken(normalRefresh.getRefreshToken(), userId, clientId);
        // 会话 B：孤立（只有 refresh）
        OAuth2RefreshTokenDO orphanRefresh = seedRefreshToken(clientId, userId);

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：两个会话都被撤销
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(normalAccess.getAccessToken()), "正常会话的访问令牌应被撤销");
        assertNull(oauth2AccessTokenRedisDAO.get(normalAccess.getAccessToken()), "正常会话的 Redis 缓存应被清除");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(normalRefresh.getRefreshToken()), "正常会话的刷新令牌应被撤销");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(orphanRefresh.getRefreshToken()),
                "孤立刷新凭据必须一并撤销");
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size());
        // 断言：两个会话的代际键都被清理
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(normalRefresh.getRefreshToken()));
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(orphanRefresh.getRefreshToken()));
    }

    /**
     * 场景 ②-2：撤销必须只影响目标用户，不得波及同租户下的其他用户会话。
     */
    @Test
    public void testRemoveAccessTokenByUser_shouldNotAffectOtherUsers() {
        // 准备
        String clientId = randomString();
        Long targetUserId = randomLongId();
        Long otherUserId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(org.mockito.ArgumentMatchers.anyLong())).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO targetRefresh = seedRefreshToken(clientId, targetUserId);
        seedAccessToken(targetRefresh.getRefreshToken(), targetUserId, clientId);
        OAuth2RefreshTokenDO otherRefresh = seedRefreshToken(clientId, otherUserId);
        OAuth2AccessTokenDO otherAccess = seedAccessToken(otherRefresh.getRefreshToken(), otherUserId, clientId);

        // 调用
        oauth2TokenService.removeAccessToken(targetUserId, UserTypeEnum.ADMIN.getValue());

        // 断言：目标用户被清空，其他用户不受影响
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(targetRefresh.getRefreshToken()));
        assertNotNull(oauth2RefreshTokenMapper.selectByRefreshToken(otherRefresh.getRefreshToken()),
                "其他用户的刷新令牌不得被误撤销");
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(otherAccess.getAccessToken()),
                "其他用户的访问令牌不得被误撤销");
    }

    /**
     * 场景 ②-3：既无 access-token 也无 refresh-token 时，撤销是无害空操作（不得抛异常、不得加锁）。
     */
    @Test
    public void testRemoveAccessTokenByUser_noSessionAtAll_shouldBeNoop() {
        Long userId = randomLongId();

        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size());
    }

    // ========== ③ 并发 / 竞态：撤销与刷新并发不得复活会话 ==========

    /**
     * 场景 ③：孤立刷新凭据上的「用户维度撤销」与「晚到刷新」并发，会话不得复活。
     *
     * <p>刻意复用 ZS-LOGIN-002 已有的<b>行锁 + 会话代际</b>机制，不新造锁。
     *
     * <p>RED：修复前撤销首句 early-return（access-token 为空）且<b>不获取任何行锁</b>，
     * 晚到刷新在 200ms 延时后提交并插入访问令牌 → 会话复活（残留 1 个有效访问令牌 + 刷新令牌存活）。
     * <p>GREEN：撤销取刷新凭据全集并逐个行锁，获锁后重读 access-token，把并发刷新已提交的新代际一并撤销。
     */
    @Test
    public void testRemoveAccessTokenByUser_orphanRefreshToken_concurrentWithRefresh_shouldNotRevive() {
        // 准备：孤立刷新凭据
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, RACE_WINDOW_MILLIS);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();

        // 调用：撤销 与 晚到刷新 并发
        List<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(() -> {
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            return null;
        });
        tasks.add(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId));
        ConcurrentOutcome outcome = runConcurrently(tasks);

        // 断言：不得复活会话
        assertEquals(0, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(),
                "撤销与晚到刷新并发后不得残留有效访问令牌（会话不得复活）");
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()).size(),
                "撤销后该用户不得残留任何访问令牌");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken),
                "撤销后刷新令牌必须已被删除，晚到刷新不得复活会话");
        // 断言：并发过程中未出现死锁 / 超时（沿用 LOGIN-002 的稳定锁序）
        assertTrue(outcome.failures.stream().noneMatch(OAuth2TokenServiceImplOrphanRevokeTest::isDeadlock),
                "撤销与刷新并发不得死锁；实际失败：" + outcome.firstFailureMessage());
    }

    /**
     * 场景 ③-2：两个孤立刷新凭据（字母序与插入序相反）+ 并发撤销，必须无死锁（稳定锁序覆盖孤立凭据）。
     */
    @Test
    public void testRemoveAccessTokenByUser_twoOrphans_concurrentRevoke_noDeadlock() {
        // 准备：插入序 zzz → aaa，字母序 aaa → zzz
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        String rZzz = "zzz-" + randomString();
        String rAaa = "aaa-" + randomString();
        seedRefreshToken(rZzz, clientId, userId);
        seedRefreshToken(rAaa, clientId, userId);

        // 调用：两个并发撤销
        List<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(() -> {
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            return null;
        });
        tasks.add(() -> {
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            return null;
        });
        ConcurrentOutcome outcome = runConcurrently(tasks);

        // 断言
        assertTrue(outcome.failures.stream().noneMatch(OAuth2TokenServiceImplOrphanRevokeTest::isDeadlock),
                "并发撤销孤立刷新凭据不得死锁（须沿用 LOGIN-002 的去重 + 自然序锁序）；实际失败："
                        + outcome.firstFailureMessage());
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(rZzz), "孤立刷新凭据 zzz 应被撤销");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(rAaa), "孤立刷新凭据 aaa 应被撤销");
    }

    // ========== ④ 事件与审计可追踪 ==========

    /**
     * 场景 ④：用户维度撤销必须留下结构化审计日志（用户编号 + 撤销会话数 + 孤立凭据数），
     * 与 {@code AdminUserServiceImpl} 侧的操作日志经 trace-id 关联，满足「事件及审计可追踪」。
     *
     * <p>RED：修复前 {@code removeAccessToken(userId, userType)} <b>没有任何日志</b>，
     * 大批会话被静默撤销，运维与安全审计无法定位。
     */
    @Test
    public void testRemoveAccessTokenByUser_shouldEmitAuditLog() {
        // 准备：1 个正常会话 + 1 个孤立刷新凭据
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO normalRefresh = seedRefreshToken(clientId, userId);
        seedAccessToken(normalRefresh.getRefreshToken(), userId, clientId);
        seedRefreshToken(clientId, userId);

        // 调用
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        // 断言：审计日志包含 ZS-LOGIN-003 标记、用户编号与撤销会话数
        String auditLog = findAuditLog(userId);
        assertNotNull(auditLog, "用户维度撤销必须留下审计日志；实际日志：" + formattedLogs());
        assertTrue(auditLog.contains("ZS-LOGIN-003"), "审计日志必须带任务标记以便检索，实际：" + auditLog);
        assertTrue(auditLog.contains("2"), "审计日志必须给出撤销会话数（本例 2 个），实际：" + auditLog);
    }

    // ========== 夹具与并发工具 ==========

    private void mockClient(String clientId, long delayMillis) {
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(300).setRefreshTokenValiditySeconds(3600);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenAnswer(invocation -> {
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
            return clientDO;
        });
    }

    // ========== ⑤ codex r1 P1：撤销墓碑阻塞旧快照回填（缓存复活竞态） ==========

    /**
     * 场景 ⑤：撤销后，并发鉴权路径遗留的「旧 DB 快照」不得把已撤销凭据回填复活。
     *
     * <p>竞态：{@code getAccessToken} 缓存未命中时从 DB 回源；若「读 DB 旧快照 → 撤销提交并删缓存 →
     * 旧快照回填」交错，旧凭据在 Redis 复活且后续命中不再校验用户状态。修复后撤销先落墓碑，
     * {@code set()} 以「查墓碑 + 写缓存」Lua 门闩拒绝回填。
     */
    @Test
    public void testRemoveAccessTokenByUser_tombstoneBlocksStaleSnapshotBackfill() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(org.mockito.ArgumentMatchers.anyLong())).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refresh = seedRefreshToken(clientId, userId);
        OAuth2AccessTokenDO access = seedAccessToken(refresh.getRefreshToken(), userId, clientId);

        // 撤销（写墓碑 + 删缓存 + 删 DB 行）
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()), "撤销后缓存应被清除");

        // 模拟并发鉴权遗留的旧快照回填（该快照在撤销提交前读取，仍带未过期 expiresTime）
        oauth2AccessTokenRedisDAO.set(access);

        // 断言：墓碑拒绝回填，旧凭据不得复活
        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "撤销墓碑存在时，旧快照回填必须被拒绝（不得复活已撤销会话）");
    }

    /**
     * 场景 ⑤-2：未撤销的存活凭据不受墓碑影响——缓存回填正常工作（门闩不误伤）。
     */
    @Test
    public void testAccessTokenCache_setWithoutTombstone_shouldSucceed() {
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(org.mockito.ArgumentMatchers.anyLong())).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refresh = seedRefreshToken(clientId, userId);
        OAuth2AccessTokenDO access = seedAccessToken(refresh.getRefreshToken(), userId, clientId);

        oauth2AccessTokenRedisDAO.delete(access.getAccessToken()); // 先清缓存
        oauth2AccessTokenRedisDAO.set(access);                     // 无墓碑时回填

        assertNotNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "无撤销墓碑时缓存回填必须正常工作");
    }

    /**
     * 场景 ⑤-3（codex r2 P1）：仅有未消费授权码、无任何 Access/Refresh 会话的用户被撤销时，
     * code 必须一并失效——否则有效期（5 分钟）内仍可经 grantAuthorizationCodeForAccessToken
     * 兑换出新会话（撤销后复活）。
     */
    @Test
    public void testRemoveAccessTokenByUser_noSessionButUnconsumedCode_codeMustBeRevoked() {
        Long userId = randomLongId();
        OAuth2CodeDO code = new OAuth2CodeDO().setCode(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(randomString())
                .setScopes(List.of("read")).setRedirectUri(randomString()).setState("")
                .setExpiresTime(LocalDateTime.now().plusMinutes(5));
        oauth2CodeMapper.insert(code);
        assertNotNull(oauth2CodeMapper.selectByCode(code.getCode()), "前置：授权码已存在");

        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());

        assertNull(oauth2CodeMapper.selectByCode(code.getCode()), "未消费授权码必须随用户级撤销一并失效");
    }

    private OAuth2RefreshTokenDO seedRefreshToken(String clientId, Long userId) {
        return seedRefreshToken(randomString(), clientId, userId);
    }

    /**
     * 插入一条未过期的刷新令牌（显式设置 tenantId，避免依赖 ThreadLocal 在子线程中丢失）。
     */
    private OAuth2RefreshTokenDO seedRefreshToken(String refreshToken, String clientId, Long userId) {
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.class, o -> o
                .setRefreshToken(refreshToken)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().plusDays(1))
                .setTenantId(0L));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        return refreshTokenDO;
    }

    /**
     * 插入一条挂在指定刷新令牌下的有效访问令牌，并写入 Redis 缓存。
     */
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

    /**
     * 模拟 ZS-LOGIN-001 兼容路径写入 Redis 的「合成访问令牌」：accessToken == refreshToken，
     * 不落 system_oauth2_access_token 表，仅随 Redis TTL 过期。
     */
    private void cacheSyntheticAccessToken(String refreshToken, Long userId, String clientId) {
        OAuth2AccessTokenDO synthetic = new OAuth2AccessTokenDO();
        synthetic.setAccessToken(refreshToken);
        synthetic.setRefreshToken(refreshToken);
        synthetic.setUserId(userId);
        synthetic.setUserType(UserTypeEnum.ADMIN.getValue());
        synthetic.setClientId(clientId);
        synthetic.setExpiresTime(LocalDateTime.now().plusDays(1));
        synthetic.setTenantId(0L);
        oauth2AccessTokenRedisDAO.set(synthetic);
    }

    private String findAuditLog(Long userId) {
        for (ILoggingEvent event : logAppender.list) {
            String message = event.getFormattedMessage();
            if (message.contains("removeAccessToken") && message.contains(String.valueOf(userId))) {
                return message;
            }
        }
        return null;
    }

    private List<String> formattedLogs() {
        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : logAppender.list) {
            messages.add(event.getFormattedMessage());
        }
        return messages;
    }

    /**
     * 栅栏同步并发执行所有任务，返回成功数 / 结果 / 失败原因。硬超时保护，避免死锁挂起 CI。
     */
    private ConcurrentOutcome runConcurrently(List<Callable<Object>> tasks) {
        int threads = tasks.size();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ConcurrentOutcome outcome = new ConcurrentOutcome();
        try {
            for (Callable<Object> task : tasks) {
                executor.submit(() -> {
                    try {
                        barrier.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS); // 同时起跑，最大化竞态
                        outcome.recordSuccess(task.call());
                    } catch (Throwable ex) {
                        outcome.recordFailure(ex);
                    }
                });
            }
        } finally {
            executor.shutdown();
        }
        try {
            assertTrue(executor.awaitTermination(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "并发任务未在 " + CONCURRENT_TIMEOUT_SECONDS + "s 内结束（疑似死锁或连接池耗尽）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试被中断", e);
        }
        return outcome;
    }

    private static boolean isDeadlock(Throwable ex) {
        if (ex == null) {
            return false;
        }
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String cls = ex.getClass().getName().toLowerCase();
        return msg.contains("deadlock") || msg.contains("lock timeout")
                || cls.contains("jdbcsqltransactionrollback")
                || msg.contains("concurrent update")
                || isDeadlock(ex.getCause());
    }

    /**
     * 并发执行结果收集器。
     */
    private static class ConcurrentOutcome {

        private final List<Object> results = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();
        private int successCount;

        private synchronized void recordSuccess(Object value) {
            results.add(value);
            successCount++;
        }

        private synchronized void recordFailure(Throwable ex) {
            failures.add(ex);
        }

        private synchronized String firstFailureMessage() {
            return failures.isEmpty() ? "无" : String.valueOf(failures.get(0));
        }
    }

}
