package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.A：令牌 DB 权威校验与「提交后缓存失效」一致性测试（真实 H2 + 内嵌 Redis）。
 *
 * <p><b>RED 依据</b>：修复前 ①checkAccessToken 盲信 Redis 命中——撤销路径缓存失效失败/回填竞态残留的
 * 幽灵凭据继续通过鉴权；②Redis 写入在 DB 事务内立即执行——事务回滚后缓存残留「DB 无行、Redis 有值」
 * 的幽灵新令牌（缓存成功而事务回滚）。
 *
 * <p><b>GREEN 合同</b>：①缓存命中后回源 DB 权威校验（普通令牌查 access 表、gate 合成令牌查 refresh 表），
 * DB 无行即已撤销——自愈 evict + 401，DB 查询异常失败关闭；②缓存失效动作注册到事务提交后执行，
 * 回滚时执行失效补偿；新建令牌的缓存发布仅在提交后发生，回滚不残留。
 *
 * @author ZS-LOGIN-005.A
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplCacheConsistencyTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;
    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private OAuth2ClientService oauth2ClientService;

    @BeforeEach
    public void beforeEach() {
        when(adminUserService.getUser(anyLong())).thenReturn(randomPojo(AdminUserDO.class));
        TenantContextHolder.setTenantId(1L); // createOAuth2AccessToken 回填 tenant_id（H2 列 NOT NULL）
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    private void mockClient(String clientId) {
        OAuth2ClientDO client = randomPojo(OAuth2ClientDO.class);
        client.setClientId(clientId).setAccessTokenValiditySeconds(1800).setRefreshTokenValiditySeconds(86400);
        when(oauth2ClientService.validOAuthClientFromCache(clientId)).thenReturn(client);
    }


    private OAuth2AccessTokenDO seedSession(String clientId, Long userId) {
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO();
        refresh.setRefreshToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setScopes(java.util.List.of("read")).setExpiresTime(LocalDateTime.now().plusDays(1));
        refresh.setTenantId(1L);
        oauth2RefreshTokenMapper.insert(refresh);
        OAuth2AccessTokenDO access = new OAuth2AccessTokenDO();
        access.setAccessToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setRefreshToken(refresh.getRefreshToken())
                .setScopes(java.util.List.of("read"))
                .setExpiresTime(LocalDateTime.now().plusMinutes(30));
        access.setUserInfo(java.util.Map.of("nickname", "test-user"));
        access.setTenantId(1L);
        oauth2AccessTokenMapper.insert(access);
        return access;
    }

    // ========== ① DB 权威校验：幽灵缓存条目被拒且自愈 evict ==========

    /**
     * RED：撤销路径（含缓存失效失败的残留）留下的「Redis 有值、DB 无行」幽灵凭据，
     * checkAccessToken 必须拒绝且自愈 evict 缓存——权威以 DB 为准，Redis 仅作加速。
     */
    @Test
    public void testCheckAccessToken_cachedButRevokedInDb_rejectsAndEvicts() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);

        // 预热缓存（模拟撤销前缓存条目已存在）
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()));

        // DB 权威撤销：直接删除 DB 行（模拟撤销事务已提交，但 Redis 失效失败/残留）
        oauth2AccessTokenMapper.deleteById(access.getId());
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(access.getAccessToken()), "前置：DB 行已删除");

        // 缓存仍有幽灵条目，checkAccessToken 必须拒绝（修复前：缓存命中直接放行）
        assertServiceException(() -> oauth2TokenService.checkAccessToken(access.getAccessToken()),
                new ErrorCode(401, "访问令牌不存在"));
        // 自愈 evict：幽灵缓存条目被清理
        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "DB 权威校验拒绝后必须自愈 evict 幽灵缓存条目");
    }

    // ========== ② 提交后缓存发布：事务回滚不残留幽灵新令牌 ==========

    /**
     * RED：修复前 createOAuth2AccessToken 在事务内立即写 Redis——事务回滚后 DB 无行、Redis 有值的
     * 幽灵新令牌残留（缓存成功而事务回滚）。
     */
    @Test
    public void testCreateAccessToken_rolledBackTx_noGhostCacheEntry() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        AtomicReference<String> tokenRef = new AtomicReference<>();

        try {
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
        }

        assertNotNull(tokenRef.get(), "前置：事务内已创建令牌串");
        assertNull(oauth2AccessTokenRedisDAO.get(tokenRef.get()),
                "事务回滚后新令牌不得残留在缓存（缓存发布必须发生在提交后）");
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(tokenRef.get()), "前置：DB 行已随回滚消失");
    }

    /**
     * 护栏：提交路径的缓存发布不被推迟语义误伤——提交后缓存条目必须存在（miss 回源仍然可用）。
     */
    @Test
    public void testCreateAccessToken_committedTx_cachePublished() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        AtomicReference<String> tokenRef = new AtomicReference<>();

        transactionTemplate.executeWithoutResult(status -> {
            OAuth2AccessTokenDO created = oauth2TokenService.createAccessToken(userId,
                    UserTypeEnum.ADMIN.getValue(), clientId, java.util.List.of("read"));
            tokenRef.set(created.getAccessToken());
        });

        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(tokenRef.get()));
        assertNotNull(oauth2AccessTokenRedisDAO.get(tokenRef.get()),
                "事务提交后新令牌的缓存条目必须发布");
    }

    // ========== ③ 提交后缓存失效：撤销在提交后清理缓存 ==========

    /**
     * 撤销的缓存失效动作发生在事务提交后：提交成功 → 缓存条目被清（含墓碑）。
     */
    @Test
    public void testRemoveAccessTokenByUser_afterCommit_cacheInvalidated() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        transactionTemplate.executeWithoutResult(status -> {
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            // ZS-LOGIN-005.A codex r0 P2：失效必须推迟到提交后——事务内缓存条目仍在（不得抢跑）
            assertNotNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                    "事务提交前撤销的缓存失效不得抢跑执行");
        });

        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "提交后撤销的缓存失效必须已执行");
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(access.getAccessToken()));
    }

    /**
     * 回滚补偿：撤销事务回滚（DB 凭据恢复有效），缓存失效动作仍执行——缓存 miss 回源重建，
     * 宁可可用性抖动不留撤销态不一致。
     */
    @Test
    public void testRemoveAccessTokenByUser_rolledBackTx_cacheStillInvalidated() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }

        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(access.getAccessToken()),
                "前置：DB 行随回滚恢复");
        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "回滚补偿仍须清缓存（miss 回源重建，避免撤销态不一致）");
    }

    // ========== ④ codex r1：前序同步回调异常跳过 afterCommit 后，事务结束兜底仍完成失效 ==========

    /**
     * 前序同步回调在 afterCommit 抛异常会跳过同事务内后续 afterCommit 回调；
     * afterCompletion 兜底必须仍完成缓存失效（修复前若只挂 afterCommit 则失效被跳过）。
     */
    @Test
    public void testRemoveAccessTokenByUser_priorCallbackThrows_fallbackStillInvalidates() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                // 故意先注册一个 afterCommit 抛异常的同步回调（先于 service 内部注册的回调执行）
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                throw new RuntimeException("前序回调异常");
                            }
                        });
                oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
            });
        } catch (RuntimeException ignored) {
            // Spring 语义：afterCommit 回调异常在事务已提交后向外传播；事务仍已提交，afterCompletion 仍会执行
        }

        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "前序 afterCommit 异常跳过后，事务结束兜底必须仍完成缓存失效");
    }

    // ========== ⑤ codex r1：flushCache 权威核验绕开 MyBatis SESSION 一级缓存旧快照 ==========

    /**
     * 长事务内（同 - SqlSession 复用）：T1 首次权威核验通过后，T2 在另一连接撤销并提交，
     * T1 再次核验必须拒绝——普通 select 会命中一级缓存旧快照放行，flushCache=TRUE count 必须回源。
     */
    @Test
    public void testCheckAccessToken_withinOpenTx_recheckAfterExternalRevoke_rejects() throws Exception {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        java.util.concurrent.CountDownLatch t1Checked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch t2Committed = new java.util.concurrent.CountDownLatch(1);
        AtomicReference<Throwable> t2Error = new AtomicReference<>();

        Thread t2 = new Thread(() -> {
            try {
                assertTrue(t1Checked.await(10, java.util.concurrent.TimeUnit.SECONDS), "等待 T1 首次核验超时");
                // codex r2 P2：T2 只撤 DB 行（mapper 直改自动提交），不得经 service 撤销——否则缓存也被删，
                // T1 重核验在缓存未命中处早退 401，权威 count 根本不会执行，flushCache 无法被验证
                oauth2AccessTokenMapper.deleteById(access.getId());
            } catch (Throwable ex) {
                t2Error.set(ex);
            } finally {
                t2Committed.countDown();
            }
        });
        t2.start();

        transactionTemplate.executeWithoutResult(status -> {
            try {
                // T1 首次核验：行存在，通过
                oauth2TokenService.checkAccessToken(access.getAccessToken());
            } catch (Exception ex) {
                throw new RuntimeException("T1 首次核验不应失败", ex);
            }
            t1Checked.countDown();
            try {
                assertTrue(t2Committed.await(10, java.util.concurrent.TimeUnit.SECONDS), "等待 T2 撤销提交超时");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
            // 前置：T2 只撤了 DB 行，缓存条目仍在——重核验必然走「缓存命中 → 权威 count」路径
            assertNotNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                    "前置：T2 仅撤销 DB 行，缓存条目必须仍在");
            // T1 同事务内再次核验：T2 已在另一连接撤销并提交——flushCache=TRUE 必须回源拒绝
            assertServiceException(() -> oauth2TokenService.checkAccessToken(access.getAccessToken()),
                    new ErrorCode(401, "访问令牌不存在"));
            // 自愈：权威核验拒绝后缓存条目被 evict
            assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                    "权威核验拒绝后必须自愈 evict 缓存条目");
        });

        assertNull(t2Error.get(), "T2 撤销不得失败: " + t2Error.get());
    }
}
