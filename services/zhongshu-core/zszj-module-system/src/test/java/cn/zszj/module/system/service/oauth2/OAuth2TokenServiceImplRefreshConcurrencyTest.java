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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenServiceImpl} 刷新路径「并发 / 重放 / 退出竞态」的单元测试类。
 *
 * <p>ZS-LOGIN-002：修复前 {@code refreshAccessToken} 的 {@code selectByRefreshToken} 无行锁、无乐观版本、无条件领取，
 * 存在两个失控面：
 * <ol>
 *     <li><b>并发同一 refreshToken 刷新</b>：N 个线程都读到同一条刷新令牌、都各自插入一条访问令牌，
 *         最终产生 N 个同时有效的访问令牌（失控会话）。</li>
 *     <li><b>刷新与退出竞态</b>：{@code removeAccessToken} 删掉刷新令牌后，晚到的刷新已持有旧的
 *         refreshTokenDO 快照，仍会插入新访问令牌，从而「复活」已被退出的会话（孤儿访问令牌）。</li>
 * </ol>
 *
 * <p>合同（本底座决策，见开发计划 §3.3）：<b>沿用原 refresh token + 会话代际号 + 重放检测</b>——
 * 不轮换 refresh token（不破坏 ZS-LOGIN-001 门控与前端刷新契约），任一时刻同一刷新凭据下
 * <b>恰好 1 个</b>有效访问令牌；每次刷新推进一个可定位的会话代际号（存 Redis，不加 DB 列）；
 * 旧代际访问令牌被撤销，重放时按既有 401 语义拒绝。
 *
 * <p>测试手法说明：为了让竞态<b>确定性复现</b>（而非偶发），在 mock 的
 * {@code validOAuthClientFromCache}（位于「读刷新令牌」与「删旧插新访问令牌」之间）里注入短延时，
 * 放大竞态窗口。该延时在修复后位于行锁临界区内，因此同时验证了「锁确实被持有并串行化」。
 *
 * @author ZS-LOGIN-002
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplRefreshConcurrencyTest extends BaseDbAndRedisUnitTest {

    /**
     * 并发线程数。注意单元测试 Druid 连接池 max-active 默认为 8，此处取 8 恰好用满，
     * 每个线程各持 1 条连接、各开 1 个事务，不会出现连接等待。
     */
    private static final int THREADS = 8;
    /**
     * 竞态窗口放大延时（毫秒）：并发刷新场景用，8 线程串行化后总耗时约 8 × (15 + DB) < H2 锁超时。
     */
    private static final long RACE_WINDOW_MILLIS = 15L;
    /**
     * 竞态窗口放大延时（毫秒）：退出 vs 晚到刷新场景用，取较大值以确保「退出先完成、刷新后插入」确定性复现。
     */
    private static final long LOGOUT_RACE_WINDOW_MILLIS = 200L;
    /**
     * 并发任务的硬超时（秒）：避免死锁 / 连接池耗尽时无限挂起 CI。
     */
    private static final long CONCURRENT_TIMEOUT_SECONDS = 120L;

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

    // ========== ① 并发刷新：不得产生失控会话 ==========

    /**
     * 场景 ①：N 个线程并发用同一 refreshToken 刷新。
     *
     * <p>合同：最终有效访问令牌数 == 1；且会话代际号 == 成功刷新次数（证明串行化、无丢失更新）。
     * <p>RED：修复前无行锁，8 个线程都插入 → 有效访问令牌数 == 8（失控会话）。
     */
    @Test
    public void testRefreshAccessToken_concurrentSameRefreshToken_shouldNotCreateRunawaySessions() {
        // 准备：客户端 + 用户 + 一条未过期刷新令牌（不预置访问令牌，使并发插入确定性叠加）
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, RACE_WINDOW_MILLIS);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        assertEquals(0, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(), "前置：不应有存量访问令牌");

        // 调用：8 线程栅栏同步并发刷新
        ConcurrentOutcome outcome = runConcurrently(buildSameTasks(THREADS,
                () -> oauth2TokenService.refreshAccessToken(refreshToken, clientId)));

        // 断言：至少一个线程成功；有效访问令牌恰好 1 个
        assertTrue(outcome.successCount >= 1, "至少应有一个线程刷新成功，失败原因：" + outcome.firstFailureMessage());
        List<OAuth2AccessTokenDO> aliveTokens = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        assertEquals(1, aliveTokens.size(),
                "同一刷新凭据并发刷新后，有效访问令牌必须恰好 1 个（不得产生失控会话）；实际="
                        + aliveTokens.size() + "，成功线程数=" + outcome.successCount);
        // 断言：会话代际号 == 成功刷新次数（无丢失更新 → 真串行化）
        Long generation = oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken);
        assertNotNull(generation, "刷新后必须留下可定位的会话代际标识");
        assertEquals(outcome.successCount, generation.longValue(), "会话代际号应等于成功刷新次数");
        // 断言：唯一存活的访问令牌来自本轮成功刷新之一，且它就是「最新代际」
        // 注：不用「最后一个 recordSuccess 的结果」作断言依据——提交完成的先后与 recordSuccess 的先后
        // 在极端线程调度下可能错位（偶发假失败）；改用「代际号 == 成功次数」这一确定性更强的等价判据。
        String aliveAccessToken = aliveTokens.get(0).getAccessToken();
        assertTrue(outcome.containsAccessToken(aliveAccessToken),
                "唯一存活的访问令牌必须来自本轮成功的刷新，实际=" + aliveAccessToken);
        assertEquals(outcome.successCount,
                generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(aliveAccessToken)),
                "存活访问令牌必须属于最新代际（代际号 == 成功刷新次数），旧代际一律已撤销");
    }

    // ========== ② 重放 / 受控幂等：连续两次刷新只有一个有效会话 ==========

    /**
     * 场景 ②：同一 refreshToken 连续两次刷新（模拟客户端重试 / 重放）。
     *
     * <p>合同（受控幂等）：第二次刷新成功但<b>取代</b>第一次，不得产生两个独立有效会话；
     * 旧代际访问令牌被撤销，重放时按既有 401 语义拒绝。
     * <p>RED：修复前无代际标识，getSessionGeneration / getAccessTokenGeneration 均为 null。
     */
    @Test
    public void testRefreshAccessToken_twiceSequential_shouldKeepSingleSessionAndRejectOldGeneration() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();

        // 调用：连续刷新两次
        OAuth2AccessTokenDO first = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        OAuth2AccessTokenDO second = oauth2TokenService.refreshAccessToken(refreshToken, clientId);

        // 断言：两次返回不同访问令牌（沿用原 refresh，不轮换刷新令牌）
        assertNotEquals(first.getAccessToken(), second.getAccessToken());
        assertEquals(refreshToken, oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken).getRefreshToken(),
                "决策为沿用原 refresh token，刷新令牌不得被轮换");
        // 断言：不得产生两个独立有效会话
        List<OAuth2AccessTokenDO> aliveTokens = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        assertEquals(1, aliveTokens.size(), "连续刷新后有效访问令牌必须恰好 1 个，实际=" + aliveTokens.size());
        assertEquals(second.getAccessToken(), aliveTokens.get(0).getAccessToken());
        // 断言：旧代际（重放）被拒绝，新代际可用
        assertServiceException(() -> oauth2TokenService.checkAccessToken(first.getAccessToken()),
                new ErrorCode(401, "访问令牌不存在"));
        assertNotNull(oauth2TokenService.getAccessToken(second.getAccessToken()), "新代际访问令牌应可用");
        // 断言：代际号可定位
        assertEquals(1L, generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(first.getAccessToken())),
                "首次刷新的访问令牌应标记为代际 1");
        assertEquals(2L, generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(second.getAccessToken())),
                "第二次刷新的访问令牌应标记为代际 2");
        assertEquals(2L, generationOf(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken)),
                "会话当前代际应为 2");
    }

    // ========== ③ 退出 vs 晚到刷新：不得复活会话 ==========

    /**
     * 场景 ③：线程 A 退出（removeAccessToken）与线程 B 晚到刷新（refreshAccessToken）并发。
     *
     * <p>合同：退出完成后不得残留任何有效访问令牌 / 刷新令牌（会话不得被复活）。
     * <p>RED：修复前退出无锁序，晚到刷新持有旧 refreshTokenDO 快照，退出后仍插入访问令牌 → 孤儿令牌存活。
     * <p>为确定性复现，刷新路径被注入 {@link #LOGOUT_RACE_WINDOW_MILLIS} 延时（位于「读刷新令牌」之后），
     * 退出路径无延时 → 退出必然先完成，刷新必然晚到。
     */
    @Test
    public void testRefreshAccessToken_concurrentWithLogout_shouldNotReviveSession() {
        // 准备：刷新令牌 + 一条存量访问令牌（退出以该访问令牌为凭据）
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, LOGOUT_RACE_WINDOW_MILLIS);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        OAuth2AccessTokenDO seededAccessToken = seedAccessToken(refreshToken, userId);

        // 调用：退出 与 晚到刷新 并发
        List<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(() -> oauth2TokenService.removeAccessToken(seededAccessToken.getAccessToken())); // 退出，无延时，先完成
        tasks.add(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId)); // 晚到刷新，被延时拖后
        ConcurrentOutcome outcome = runConcurrently(tasks);

        // 断言：退出与晚到刷新中至少一个成功（不因并发互相炸掉）
        assertTrue(outcome.successCount >= 1,
                "退出与晚到刷新中至少一个应成功；失败原因：" + outcome.firstFailureMessage());
        // 断言：不得复活会话——最终既无有效访问令牌，也无刷新令牌
        List<OAuth2AccessTokenDO> aliveTokens = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        assertEquals(0, aliveTokens.size(),
                "退出后晚到刷新不得复活会话，应无任何有效访问令牌；实际残留=" + aliveTokens.size());
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken), "退出后刷新令牌应已被删除");
        // 断言：Redis 中也不得残留可用凭据
        for (OAuth2AccessTokenDO aliveToken : aliveTokens) {
            assertNull(oauth2AccessTokenRedisDAO.get(aliveToken.getAccessToken()));
        }
        assertNull(oauth2AccessTokenRedisDAO.get(seededAccessToken.getAccessToken()));
        // 断言：会话代际标识随退出清理
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken), "退出后会话代际标识应被清理");
    }

    /**
     * 场景 ③ 补充（确定性正向）：退出<b>已完成</b>后再刷新，必须被拒绝且不复活。
     */
    @Test
    public void testRefreshAccessToken_afterLogoutCompleted_shouldBeRejected() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        OAuth2AccessTokenDO seededAccessToken = seedAccessToken(refreshToken, userId);
        // 先刷新一次：存量访问令牌被取代，并留下会话代际标识
        OAuth2AccessTokenDO refreshedAccessToken = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        assertNotNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken), "前置：应已有会话代际标识");
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(seededAccessToken.getAccessToken()),
                "前置：被取代的旧代际访问令牌应已被撤销");

        // 调用：退出完成。以「当前有效访问令牌」为凭据——这是现实退出路径（请求头携带的即当前访问令牌）。
        // 已知边界：若改用已被取代的旧令牌串退出，removeAccessToken 按既有契约早退返回 null（见其 Javadoc），
        // 该既有语义不在 ZS-LOGIN-002 范围内变更。
        oauth2TokenService.removeAccessToken(refreshedAccessToken.getAccessToken());

        // 断言：晚到刷新被拒绝（刷新令牌已不存在），且不复活
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refreshToken, clientId),
                new ErrorCode(400, "无效的刷新令牌"));
        assertEquals(0, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(), "不得复活会话");
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken));
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken), "退出后会话代际标识应被清理");
    }

    // ========== ④ 会话代际可定位 ==========

    /**
     * 场景 ④：刷新产生新代际时必须保留可定位标识（便于审计与「保留可定位的会话代际」）。
     *
     * <p>合同：会话代际随刷新单调递增；每个访问令牌都能反查其所属代际（含已被取代的旧代际，供审计定位）；
     * 且代际状态存 Redis，不新增 DB 列。
     * <p>RED：修复前服务层未接入代际登记 → 全部为 null。
     */
    @Test
    public void testRefreshAccessToken_shouldKeepLocatableSessionGeneration() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, 0L);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();
        assertNull(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken), "前置：新会话尚无代际标识");

        // 调用：连续三次刷新
        OAuth2AccessTokenDO gen1 = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        OAuth2AccessTokenDO gen2 = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        OAuth2AccessTokenDO gen3 = oauth2TokenService.refreshAccessToken(refreshToken, clientId);

        // 断言：会话代际单调递增到 3
        assertEquals(3L, generationOf(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken)));
        // 断言：每个代际的访问令牌均可反查定位（旧代际保留，供审计）
        assertEquals(1L, generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(gen1.getAccessToken())));
        assertEquals(2L, generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(gen2.getAccessToken())));
        assertEquals(3L, generationOf(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(gen3.getAccessToken())));
        // 断言：仅最新代际有效，旧代际一律拒绝（重放检测）
        assertEquals(1, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size());
        assertServiceException(() -> oauth2TokenService.checkAccessToken(gen1.getAccessToken()),
                new ErrorCode(401, "访问令牌不存在"));
        assertServiceException(() -> oauth2TokenService.checkAccessToken(gen2.getAccessToken()),
                new ErrorCode(401, "访问令牌不存在"));
        assertNotNull(oauth2TokenService.checkAccessToken(gen3.getAccessToken()));
    }

    // ========== ⑤ 客户端重试风暴：服务端串行化不失控 ==========

    /**
     * 场景 ⑤：模拟客户端重试风暴——多轮、每轮多个并发重试。
     *
     * <p>合同：无论多少轮重试，最终有效访问令牌恒为 1 个；代际号 == 累计成功次数（服务端串行化、不失控）。
     * <p>RED：修复前每轮并发都会叠加多个有效访问令牌。
     */
    @Test
    public void testRefreshAccessToken_clientRetryStorm_shouldStaySerialized() {
        // 准备
        String clientId = randomString();
        Long userId = randomLongId();
        mockClient(clientId, RACE_WINDOW_MILLIS);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));
        OAuth2RefreshTokenDO refreshTokenDO = seedRefreshToken(clientId, userId);
        String refreshToken = refreshTokenDO.getRefreshToken();

        // 调用：3 轮重试，每轮 4 个并发
        int totalSuccess = 0;
        for (int round = 0; round < 3; round++) {
            ConcurrentOutcome outcome = runConcurrently(buildSameTasks(4,
                    () -> oauth2TokenService.refreshAccessToken(refreshToken, clientId)));
            totalSuccess += outcome.successCount;
            // 每轮结束后：有效访问令牌恒为 1
            assertEquals(1, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(),
                    "第 " + (round + 1) + " 轮重试后有效访问令牌必须恰好 1 个");
        }

        // 断言：累计代际 == 累计成功次数；有效会话仍为 1
        assertTrue(totalSuccess > 0, "重试风暴中至少应有一次刷新成功");
        assertEquals((long) totalSuccess, generationOf(oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken)),
                "累计会话代际应等于累计成功刷新次数");
        assertEquals(1, oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken).size(),
                "重试风暴结束后有效访问令牌必须恰好 1 个");
    }

    // ========== 夹具与并发工具 ==========

    /**
     * 构造 threads 个执行同一动作的任务。
     */
    private List<Callable<Object>> buildSameTasks(int threads, Callable<Object> action) {
        List<Callable<Object>> tasks = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            tasks.add(action);
        }
        return tasks;
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
                        Object value = task.call();
                        outcome.recordSuccess(value);
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

    /**
     * mock OAuth2 客户端；在 {@code validOAuthClientFromCache} 中注入延时以放大竞态窗口。
     * <p>该调用点位于「读刷新令牌」之后、「删旧插新访问令牌」之前，因此延时既能确定性复现竞态，
     * 又能在修复后验证行锁临界区确实覆盖了整个读改写过程。
     */
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

    /**
     * 插入一条未过期的刷新令牌（显式设置 tenantId，避免依赖 ThreadLocal 在子线程中丢失）。
     */
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

    /**
     * 插入一条挂在指定刷新令牌下的有效访问令牌，并写入 Redis 缓存。
     */
    private OAuth2AccessTokenDO seedAccessToken(String refreshToken, Long userId) {
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o
                .setRefreshToken(refreshToken)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusMinutes(30))
                .setTenantId(0L));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO); // 注意：set 会清空 DO 的 createTime 等字段
        return accessTokenDO;
    }

    private static long generationOf(Long generation) {
        assertNotNull(generation, "会话代际标识缺失（应为可定位的代际号）");
        return generation;
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

        /**
         * 本轮成功的刷新是否产生过指定访问令牌（用于断言「唯一存活令牌 ∈ 成功集合」）。
         */
        private synchronized boolean containsAccessToken(String accessToken) {
            for (Object result : results) {
                if (result instanceof OAuth2AccessTokenDO
                        && ((OAuth2AccessTokenDO) result).getAccessToken().equals(accessToken)) {
                    return true;
                }
            }
            return false;
        }

        private synchronized String firstFailureMessage() {
            return failures.isEmpty() ? "无" : String.valueOf(failures.get(0));
        }
    }

}
