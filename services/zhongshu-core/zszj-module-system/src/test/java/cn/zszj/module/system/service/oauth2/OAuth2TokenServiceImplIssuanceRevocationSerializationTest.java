package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.enums.ErrorCodeConstants;
import cn.zszj.module.system.enums.common.SexEnum;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.hutool.core.util.RandomUtil.randomEle;
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
 * ZS-LOGIN-003.B：登录签发与用户级撤销串行化根因修复（ZS-IAM-004 codex r1 P2 移交的幻影写逃逸窗口）。
 *
 * <p><b>RED 依据（修复前的失控面）</b>：唯一签发咽喉点 {@link OAuth2TokenServiceImpl#createAccessToken}
 * 虽有 {@code @Transactional}，却<b>既不取共享用户行锁</b>（{@code selectByIdForUpdate(userId)}）<b>也不在锁内重读账号状态</b>；
 * 而用户级撤销 {@code removeAccessToken(userId, userType)}（{@code doRemoveAccessTokenByUser}）与授权码兑换
 * {@code grantAuthorizationCodeForAccessToken} 均以「用户行锁」为最外层锁。于是签发与撤销<b>不互斥</b>：
 * 快速 suspend（禁用 + 撤销）先提交后，晚到的登录签发仍会为<b>已禁用</b>账号建出一条撤销扫描从未看见的
 * 幽灵凭据（幻影写逃逸），凭内嵌上下文重新匹配而被接受。
 *
 * <p><b>GREEN 合同</b>：签发路径纳入<b>共享用户行锁协议</b>——{@code createAccessToken} 起始（事务内、锁序最外层）
 * 对 ADMIN 且 {@code userId > 0} 的真实账号取 {@code selectByIdForUpdate(userId)}，锁内<b>重读</b>账号状态；
 * 账号不存在 / 已禁用即抛 {@code USER_NOT_EXISTS}，不签发任何凭据。锁序与撤销、兑换一致（用户行锁 → …→ 令牌），
 * 使签发与用户级撤销串行化：撤销先提交则签发被拒，签发先获锁则撤销在获锁后重读并删除新令牌——两种交错都不产可用幽灵凭据。
 * client_credentials 机器令牌（{@code userId = 0}）与 MEMBER 不在此闭环（沿既有边界）。
 *
 * <p>本套件以<b>真实 token 服务</b>（真实 H2 行锁 + 真实 {@code createAccessToken}/{@code removeAccessToken}，非仅 mock）
 * 驱动交错，满足 §16.1「用真实 token 服务（非仅 mock）测该交错」。
 *
 * @author ZS-LOGIN-003.B
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplIssuanceRevocationSerializationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    /**
     * {@code buildUserInfo} 经 {@code adminUserService.getUser} 组装昵称/部门（与行锁重读的权威状态校验解耦）；
     * 本套件 mock 之，账号状态以真实 {@code system_users} 行为准（{@code selectByIdForUpdate} 直查 DB）。
     */
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

    /** 插入一条真实 ADMIN 账号行（租户 1L，与上下文一致），供 {@code selectByIdForUpdate} 行锁 + 状态重读。 */
    private void insertUser(Long userId, CommonStatusEnum status) {
        adminUserMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setId(userId);
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 落在 tinyint 范围内
            o.setStatus(status.getStatus());
            o.setTenantId(1L);
        }));
    }

    private void updateUserStatus(Long userId, CommonStatusEnum status) {
        AdminUserDO user = adminUserMapper.selectById(userId);
        user.setStatus(status.getStatus());
        adminUserMapper.updateById(user);
    }

    private List<OAuth2AccessTokenDO> aliveTokensOf(Long userId) {
        List<OAuth2AccessTokenDO> tokens =
                oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue());
        return tokens == null ? List.of() : tokens;
    }

    // ========== ① 确定性 RED 锚：撤销（禁用）先提交，晚到签发必须被拒且不产幽灵令牌 ==========

    /**
     * RED：模拟快速 suspend 已提交（账号禁用 + 用户级撤销完成）后，晚到的登录签发到达。
     * 修复前 {@code createAccessToken} 不取用户行锁、不重读状态 → 为已禁用账号建出撤销扫描从未看见的幽灵凭据；
     * 修复后签发在共享用户行锁内重读到 DISABLE → 抛 {@code USER_NOT_EXISTS}，不落任何访问令牌。
     */
    @Test
    public void testCreateAccessToken_suspendCommittedBeforeIssuance_rejectsAndNoGhostToken() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        insertUser(userId, CommonStatusEnum.ENABLE);

        // 撤销方先提交：禁用账号 + 用户级撤销（此时无会话，仅确立「已禁用」权威状态）
        transactionTemplate.executeWithoutResult(status -> {
            updateUserStatus(userId, CommonStatusEnum.DISABLE);
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        });

        // 晚到的登录签发：修复后必须在共享用户行锁内重读到 DISABLE 而被拒
        assertServiceException(() -> transactionTemplate.executeWithoutResult(status ->
                        oauth2TokenService.createAccessToken(userId, UserTypeEnum.ADMIN.getValue(), clientId, List.of("read"))),
                ErrorCodeConstants.USER_NOT_EXISTS);

        // 不产任何可用幽灵凭据
        assertTrue(aliveTokensOf(userId).isEmpty(),
                "已禁用账号的晚到签发不得落下任何访问令牌（幻影写逃逸窗口必须闭合）");
    }

    /**
     * 边界锁：账号行不存在（已被并发删除）时签发同样被拒——共享用户行锁重读对 null 亦失败关闭。
     */
    @Test
    public void testCreateAccessToken_userRowAbsent_rejects() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId(); // 未插入账号行

        assertServiceException(() -> transactionTemplate.executeWithoutResult(status ->
                        oauth2TokenService.createAccessToken(userId, UserTypeEnum.ADMIN.getValue(), clientId, List.of("read"))),
                ErrorCodeConstants.USER_NOT_EXISTS);
        assertTrue(aliveTokensOf(userId).isEmpty(), "账号不存在时不得签发任何访问令牌");
    }

    /**
     * 护栏：启用账号的正常签发不受行锁协议误伤——令牌照常创建、缓存照常发布（提交后）。
     */
    @Test
    public void testCreateAccessToken_enabledUser_stillIssues() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        insertUser(userId, CommonStatusEnum.ENABLE);

        AtomicReference<String> tokenRef = new AtomicReference<>();
        transactionTemplate.executeWithoutResult(status -> {
            OAuth2AccessTokenDO created = oauth2TokenService.createAccessToken(userId,
                    UserTypeEnum.ADMIN.getValue(), clientId, List.of("read"));
            tokenRef.set(created.getAccessToken());
        });

        assertNotNull(tokenRef.get(), "启用账号签发必须成功");
        assertNotNull(oauth2AccessTokenMapper.selectByAccessToken(tokenRef.get()), "访问令牌必须落库");
        assertNotNull(oauth2AccessTokenRedisDAO.get(tokenRef.get()), "提交后缓存条目必须发布");
    }

    // ========== ② 真实双线程并发交错：suspend 与登录竞争，任何交错都不得留可用幽灵凭据 ==========

    /**
     * 以真实 token 服务驱动「登录签发」与「禁用 + 用户级撤销」的并发竞争（真实 H2 行锁）。
     * 修复后二者在共享用户行锁上串行化，无论谁先获锁，最终不变量恒成立：
     * <b>账号已禁用时，登录签发要么被拒（无令牌），要么其令牌已被撤销删除——不存在可用幽灵凭据</b>。
     * 修复前签发不取锁、不重读状态，撤销扫描可能看不见晚提交的令牌 → 幽灵凭据存活 → 不变量被破坏。
     */
    @Test
    public void testConcurrentSuspendAndLogin_noUsableGhostCredential() throws Exception {
        for (int iter = 0; iter < 20; iter++) {
            String clientId = randomString();
            mockClient(clientId);
            Long userId = randomLongId();
            insertUser(userId, CommonStatusEnum.ENABLE);

            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AtomicReference<String> issuedToken = new AtomicReference<>();
            AtomicReference<Throwable> loginError = new AtomicReference<>();
            AtomicReference<Throwable> suspendError = new AtomicReference<>();

            Thread login = new Thread(() -> {
                try {
                    assertTrue(startGate.await(10, TimeUnit.SECONDS), "登录线程等待发令超时");
                    transactionTemplate.executeWithoutResult(status -> {
                        OAuth2AccessTokenDO created = oauth2TokenService.createAccessToken(userId,
                                UserTypeEnum.ADMIN.getValue(), clientId, List.of("read"));
                        issuedToken.set(created.getAccessToken());
                    });
                } catch (Throwable ex) {
                    loginError.set(ex); // 修复后：账号被禁用时抛 USER_NOT_EXISTS 属预期结果之一
                } finally {
                    done.countDown();
                }
            });

            Thread suspend = new Thread(() -> {
                try {
                    assertTrue(startGate.await(10, TimeUnit.SECONDS), "撤销线程等待发令超时");
                    transactionTemplate.executeWithoutResult(status -> {
                        updateUserStatus(userId, CommonStatusEnum.DISABLE);
                        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
                    });
                } catch (Throwable ex) {
                    suspendError.set(ex);
                } finally {
                    done.countDown();
                }
            });

            login.start();
            suspend.start();
            startGate.countDown(); // 同时放行，制造真实竞争
            assertTrue(done.await(20, TimeUnit.SECONDS), "并发线程未在超时内完成（疑似死锁）");

            assertNull(suspendError.get(), "撤销线程不得失败: " + suspendError.get());

            // 账号最终已禁用（suspend 事务无论先后都会把状态置为 DISABLE 并提交）
            AdminUserDO finalUser = adminUserMapper.selectById(userId);
            assertNotNull(finalUser, "前置：账号行存在");
            assertTrue(CommonStatusEnum.isDisable(finalUser.getStatus()), "前置：竞争结束后账号应处于禁用态");

            // 核心不变量：已禁用账号不得持有可用幽灵凭据
            String token = issuedToken.get();
            if (token != null) {
                // 登录曾返回令牌（签发先获锁）→ 撤销在获锁后重读必须已将其删除
                assertNull(oauth2AccessTokenMapper.selectByAccessToken(token),
                        "第 " + iter + " 轮：签发先获锁时，随后撤销必须删除该令牌，不得残留幽灵凭据");
                assertServiceException(() -> oauth2TokenService.checkAccessToken(token),
                        new ErrorCode(401, "访问令牌不存在"));
            } else {
                // 登录未返回令牌（撤销先获锁）→ 必须是因账号禁用被拒，而非其它意外异常
                assertNotNull(loginError.get(), "第 " + iter + " 轮：未签发令牌时登录应以异常收场");
            }
        }
    }
}
