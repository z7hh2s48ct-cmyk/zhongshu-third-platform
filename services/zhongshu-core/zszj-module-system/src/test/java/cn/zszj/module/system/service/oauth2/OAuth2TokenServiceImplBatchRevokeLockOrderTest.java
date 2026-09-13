package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-002 P2-A：批量撤销按 refresh-token 稳定锁序防死锁。
 *
 * <p>缺陷：{@code removeAccessToken(userId, userType)} 按 access-token 查询结果顺序逐个
 * {@code SELECT ... FOR UPDATE} 锁定 refresh-token 行。并发刷新改变 access-token 行 ID 顺序后，
 * 两个重叠的批量撤销事务以不同顺序获取锁 → 死锁。
 *
 * <p>修复合同：获取锁前对 refresh-token 标识去重并按自然序排序，消除锁环。
 *
 * @author ZS-LOGIN-002 codex-r0-fix
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplBatchRevokeLockOrderTest extends BaseDbAndRedisUnitTest {

    private static final long TIMEOUT_SECONDS = 60L;

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

    /**
     * 场景：两个并发批量撤销 + 一个并发刷新（改变 access-token 行顺序）。
     *
     * <p>设置：用户有 2 个会话（R_aaa, R_zzz），access-token 插入顺序使查询返回 [R_zzz, R_aaa]。
     * 并发刷新 R_zzz 删除旧 access-token 并插入新行（更高 ID），使后续查询返回 [R_aaa, R_zzz_new]。
     *
     * <p>RED（修复前）：Thread A 按 [R_zzz, R_aaa] 锁序，Thread B 按 [R_aaa, R_zzz] → 死锁。
     * <p>GREEN（修复后）：两者都排序为 [R_aaa, R_zzz] → 无死锁。
     */
    @Test
    public void batchRevoke_concurrentWithRefresh_noDeadlock() throws Exception {
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        mockClient(clientId);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));

        // R_zzz 先插入（access-token ID 较小），R_aaa 后插入（ID 较大）
        String rZzz = "zzz-" + randomString();
        String rAaa = "aaa-" + randomString();
        seedRefreshToken(rZzz, clientId, userId);
        seedAccessToken(rZzz, userId, clientId);
        seedRefreshToken(rAaa, clientId, userId);
        seedAccessToken(rAaa, userId, clientId);

        // 并发：2 个批量撤销 + 1 个刷新
        CountDownLatch refreshDone = new CountDownLatch(1);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> futureA = executor.submit(() -> {
                try {
                    startGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    oauth2TokenService.removeAccessToken(userId, userType);
                } catch (Throwable ex) {
                    errorA.set(ex);
                }
            });
            Future<?> futureC = executor.submit(() -> {
                try {
                    startGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Thread.sleep(5);
                    try {
                        oauth2TokenService.refreshAccessToken(rZzz, clientId);
                    } catch (Exception ignored) {
                    }
                } catch (Throwable ignored) {
                } finally {
                    refreshDone.countDown();
                }
            });
            Future<?> futureB = executor.submit(() -> {
                try {
                    startGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    refreshDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    oauth2TokenService.removeAccessToken(userId, userType);
                } catch (Throwable ex) {
                    errorB.set(ex);
                }
            });
            startGate.countDown();
            futureA.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            futureC.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            futureB.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        if (errorA.get() != null && isDeadlock(errorA.get())) {
            fail("Thread A deadlock (unstable lock order): " + errorA.get().getMessage());
        }
        if (errorB.get() != null && isDeadlock(errorB.get())) {
            fail("Thread B deadlock (unstable lock order): " + errorB.get().getMessage());
        }
        // 最终所有令牌被撤销
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, userType).size(),
                "batch revoke must remove all access tokens");
    }

    /**
     * 场景：多会话（4 个）并发批量撤销，验证锁序稳定无死锁。
     */
    @Test
    public void batchRevoke_fourSessions_concurrent_noDeadlock() throws Exception {
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        mockClient(clientId);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));

        // 4 个会话，refresh-token 字母序与插入序相反
        String[] prefixes = {"ddd", "ccc", "bbb", "aaa"};
        for (String prefix : prefixes) {
            String rt = prefix + "-" + randomString();
            seedRefreshToken(rt, clientId, userId);
            seedAccessToken(rt, userId, clientId);
        }

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch barrier = new CountDownLatch(threads);
        List<Throwable> errors = new ArrayList<>();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        barrier.countDown();
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        oauth2TokenService.removeAccessToken(userId, userType);
                    } catch (Throwable ex) {
                        synchronized (errors) { errors.add(ex); }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        for (Throwable err : errors) {
            if (isDeadlock(err)) {
                fail("Concurrent batch revoke deadlocked: " + err.getMessage());
            }
        }
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, userType).size());
    }

    /**
     * 场景：2 个并发批量撤销（插入序与字母序相反），验证无死锁。
     */
    @Test
    public void batchRevoke_twoConcurrent_noDeadlock() throws Exception {
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        mockClient(clientId);
        when(adminUserService.getUser(userId)).thenReturn(randomPojo(AdminUserDO.class));

        // 插入序：R_zzz 先（ID 小），R_aaa 后（ID 大）→ 查询序 [R_zzz, R_aaa] ≠ 字母序
        String rZzz = "zzz-" + randomString();
        String rAaa = "aaa-" + randomString();
        seedRefreshToken(rZzz, clientId, userId);
        seedAccessToken(rZzz, userId, clientId);
        seedRefreshToken(rAaa, clientId, userId);
        seedAccessToken(rAaa, userId, clientId);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch barrier = new CountDownLatch(threads);
        List<Throwable> errors = new ArrayList<>();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        barrier.countDown();
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        oauth2TokenService.removeAccessToken(userId, userType);
                    } catch (Throwable ex) {
                        synchronized (errors) { errors.add(ex); }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        for (Throwable err : errors) {
            assertTrue(!isDeadlock(err),
                    "Concurrent batch revoke must not deadlock (stable lock order required); got: " + err.getMessage());
        }
        assertEquals(0, oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, userType).size());
    }

    // ========== 工具方法 ==========

    private static boolean isDeadlock(Throwable ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String cls = ex.getClass().getName().toLowerCase();
        return msg.contains("deadlock") || msg.contains("lock timeout")
                || cls.contains("jdbcsqltransactionrollback")
                || msg.contains("concurrent update")
                || (ex.getCause() != null && isDeadlock(ex.getCause()));
    }

    private void mockClient(String clientId) {
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(clientId)
                .setAccessTokenValiditySeconds(300).setRefreshTokenValiditySeconds(3600);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenReturn(clientDO);
    }

    private void seedRefreshToken(String refreshToken, String clientId, Long userId) {
        OAuth2RefreshTokenDO rtDO = randomPojo(OAuth2RefreshTokenDO.class, o -> o
                .setRefreshToken(refreshToken)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().plusDays(1))
                .setTenantId(0L));
        oauth2RefreshTokenMapper.insert(rtDO);
    }

    private void seedAccessToken(String refreshToken, Long userId, String clientId) {
        OAuth2AccessTokenDO atDO = randomPojo(OAuth2AccessTokenDO.class, o -> o
                .setRefreshToken(refreshToken)
                .setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().plusHours(1))
                .setTenantId(0L));
        oauth2AccessTokenMapper.insert(atDO);
        oauth2AccessTokenRedisDAO.set(atDO);
    }
}
