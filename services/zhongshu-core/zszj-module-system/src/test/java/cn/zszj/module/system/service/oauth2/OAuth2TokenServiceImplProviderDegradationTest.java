package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.framework.outbox.SystemOutboxEventTypes;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.B：{@code ObjectProvider<ReliableEventPort>} <b>懒解析降级测试</b>。
 *
 * <p><b>场景</b>：system 模块<b>单独运行</b>（未装配 infra 的 {@code JdbcReliableEventPort} bean），
 * {@code reliableEventPortProvider.getIfAvailable()} 返回 {@code null}，
 * {@code appendRevocationCompensationEvent} 必须<b>静默降级为 .A 语义</b>——
 * 撤销主逻辑仍成功（不抛异常、不阻断业务），outbox_event 表 0 行（无预写），
 * 缓存清空仍由 afterCompletion 的 {@code revokeWithTombstone} 执行（.A 遗留路径）。
 *
 * <p><b>装配策略</b>：本测试类 {@code @Import} <b>不包含</b> {@code JdbcReliableEventPort}
 * 与 {@code OAuth2TokenRevocationCompensationSink}，模拟 system 单独部署的最小上下文。
 * Spring 的 {@code ObjectProvider} 语义保证：容器无匹配 bean 时 {@code getIfAvailable()} 返回 null，
 * 注入本身不抛 {@code NoSuchBeanDefinitionException}。
 *
 * <p><b>骨架与 GREEN 均须通过</b>：骨架下 {@code appendRevocationCompensationEvent} 是 no-op（0 事件）；
 * GREEN 下查 provider 得 null 后静默 return（0 事件）。两者行为一致，本测试锁定"降级不阻断业务"契约。
 *
 * @author ZS-LOGIN-005.B
 */
@Import({
        OAuth2TokenServiceImpl.class,
        OAuth2AccessTokenRedisDAO.class
        // 故意不 Import JdbcReliableEventPort / OAuth2TokenRevocationCompensationSink
        // 模拟 system 单独运行（无 infra outbox 装配）
})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplProviderDegradationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO redisDAO;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private OAuth2ClientService oauth2ClientService;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void beforeEach() {
        when(adminUserService.getUser(anyLong())).thenReturn(randomPojo(AdminUserDO.class));
        TenantContextHolder.setTenantId(1L);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== 种子与工具 ==========

    private void mockClient(String clientId) {
        OAuth2ClientDO client = randomPojo(OAuth2ClientDO.class);
        client.setClientId(clientId).setAccessTokenValiditySeconds(1800).setRefreshTokenValiditySeconds(86400);
        when(oauth2ClientService.validOAuthClientFromCache(clientId)).thenReturn(client);
    }

    private OAuth2AccessTokenDO seedSession(String clientId, Long userId) {
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO();
        refresh.setRefreshToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setScopes(List.of("read")).setExpiresTime(LocalDateTime.now().plusDays(1));
        refresh.setTenantId(1L);
        refreshTokenMapper.insert(refresh);
        OAuth2AccessTokenDO access = new OAuth2AccessTokenDO();
        access.setAccessToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setRefreshToken(refresh.getRefreshToken())
                .setScopes(List.of("read"))
                .setExpiresTime(LocalDateTime.now().plusMinutes(30));
        access.setUserInfo(Map.of("nickname", "ut-user"));
        access.setTenantId(1L);
        accessTokenMapper.insert(access);
        return access;
    }

    private long countCompensationEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM outbox_event WHERE event_type = ?",
                Long.class, SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION);
        return count == null ? 0L : count;
    }

    // ========== 用例 1：removeAccessToken 降级 ==========

    /**
     * 撤销主逻辑在 provider 为 null 时仍成功（不抛异常、不阻断业务），
     * outbox_event 表 0 行（降级为 .A 语义），缓存清空由 afterCompletion 的 revokeWithTombstone 执行。
     */
    @Test
    public void removeAccessToken_withoutReliableEventPort_succeedsAndDoesNotBlock() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        redisDAO.set(access);
        assertNotNull(redisDAO.get(access.getAccessToken()), "前置：缓存预热必须成功");

        // 撤销主逻辑必须不抛异常（provider 为 null 时静默降级）
        assertDoesNotThrow(() -> transactionTemplate.executeWithoutResult(status ->
                        oauth2TokenService.removeAccessToken(access.getAccessToken())),
                "provider 为 null 时撤销主逻辑必须静默降级，不阻断业务");

        // DB 逻辑删除生效
        assertNull(accessTokenMapper.selectByAccessToken(access.getAccessToken()),
                "DB access 行必须已逻辑删除");
        // outbox 0 行（降级为 .A 语义，无预写）
        assertEquals(0L, countCompensationEvents(),
                "provider 为 null 时 outbox_event 必须 0 行（静默降级为 .A 语义）");
        // 缓存清空仍由 afterCompletion 的 revokeWithTombstone 执行（.A 遗留路径）
        assertNull(redisDAO.get(access.getAccessToken()),
                ".A 语义：afterCompletion 里 revokeWithTombstone 仍清缓存");
    }

    // ========== 用例 2：refreshAccessToken 降级 ==========

    /**
     * 刷新主逻辑在 provider 为 null 时仍成功，旧代际被淘汰（DB 逻辑删除 + 缓存清空），
     * outbox_event 表 0 行（降级为 .A 语义）。
     */
    @Test
    public void refreshAccessToken_withoutReliableEventPort_succeedsAndDoesNotBlock() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO oldAccess = seedSession(clientId, userId);
        redisDAO.set(oldAccess);
        assertNotNull(redisDAO.get(oldAccess.getAccessToken()), "前置：旧代际缓存预热必须成功");

        OAuth2AccessTokenDO newAccess = assertDoesNotThrow(() ->
                        transactionTemplate.execute(status ->
                                oauth2TokenService.refreshAccessToken(oldAccess.getRefreshToken(), clientId)),
                "provider 为 null 时刷新主逻辑必须静默降级，不阻断业务");

        assertNotNull(newAccess, "刷新必须返回新 access token");
        // 旧代际 DB 逻辑删除
        assertNull(accessTokenMapper.selectByAccessToken(oldAccess.getAccessToken()),
                "旧代际 DB access 行必须已逻辑删除");
        // outbox 0 行（降级为 .A 语义）
        assertEquals(0L, countCompensationEvents(),
                "provider 为 null 时 refreshAccessToken 必须 0 事件（静默降级）");
        // 旧代际缓存清空（.A 语义）
        assertNull(redisDAO.get(oldAccess.getAccessToken()),
                ".A 语义：afterCompletion 里 revokeWithTombstone 仍清旧代际缓存");
    }

    // ========== 用例 3：removeAccessToken(userId, userType) 用户级批量撤销降级 ==========

    /**
     * 用户级批量撤销主逻辑在 provider 为 null 时仍成功，该用户所有会话被淘汰
     * （DB 逻辑删除 + 缓存清空），outbox_event 表 0 行（降级为 .A 语义）。
     *
     * <p>注：{@code revokeSession} 是 Impl 内部方法（非接口公开），故用
     * {@code removeAccessToken(Long userId, Integer userType)} 覆盖"多会话撤销"降级路径。
     */
    @Test
    public void removeAccessTokenByUser_withoutReliableEventPort_succeedsAndDoesNotBlock() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access1 = seedSession(clientId, userId);
        OAuth2AccessTokenDO access2 = seedSession(clientId, userId);
        redisDAO.set(access1);
        redisDAO.set(access2);
        assertNotNull(redisDAO.get(access1.getAccessToken()), "前置：会话 1 缓存预热必须成功");
        assertNotNull(redisDAO.get(access2.getAccessToken()), "前置：会话 2 缓存预热必须成功");

        assertDoesNotThrow(() -> transactionTemplate.executeWithoutResult(status ->
                        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue())),
                "provider 为 null 时用户级批量撤销必须静默降级，不阻断业务");

        assertNull(accessTokenMapper.selectByAccessToken(access1.getAccessToken()),
                "会话 1 DB access 行必须已逻辑删除");
        assertNull(accessTokenMapper.selectByAccessToken(access2.getAccessToken()),
                "会话 2 DB access 行必须已逻辑删除");
        assertEquals(0L, countCompensationEvents(),
                "provider 为 null 时用户级批量撤销必须 0 事件（静默降级）");
        assertNull(redisDAO.get(access1.getAccessToken()),
                ".A 语义：afterCompletion 里 revokeWithTombstone 仍清会话 1 缓存");
        assertNull(redisDAO.get(access2.getAccessToken()),
                ".A 语义：afterCompletion 里 revokeWithTombstone 仍清会话 2 缓存");
    }

}
