package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.B：{@link OAuth2TokenServiceImpl} 撤销主逻辑「事务内预写 Outbox 补偿事件」测试。
 *
 * <p><b>RED 依据</b>：{@code appendRevocationCompensationEvent} 当前为 RED-2 骨架（仅 log.debug 后 return），
 * 用例 1/2/3 断言 {@code outbox_event} 表新增行数 > 0 会失败；GREEN-2 阶段骨架填充
 * {@code ObjectProvider<ReliableEventPort>} 懒解析 + {@code append} 后全部转绿。
 *
 * <p><b>保护性回归</b>（用例 4/5）：骨架与 GREEN 实现均应通过——锁定两条硬契约：
 * <ul>
 *   <li>用例 4：业务事务回滚 → outbox 事件也回滚（MANDATORY 传播语义，禁止 REQUIRES_NEW 独立事务）；</li>
 *   <li>用例 5：{@code checkAccessToken} 读路径不预写事件（锁定「每撤销 IO+1」边界，读路径 IO 不放大）。</li>
 * </ul>
 *
 * <p><b>装配</b>：@Import {@link JdbcReliableEventPort} 让 {@code ObjectProvider<ReliableEventPort>} 在测试
 * 上下文能解析到实现——system 单独运行时（无 infra 装配）ObjectProvider 返回 null，Service 静默降级为
 * {@code .A} 语义（该降级路径由 {@code OAuth2TokenServiceImplProviderDegradationTest} 覆盖）。
 *
 * @author ZS-LOGIN-005.B
 */
@Import({
        OAuth2TokenServiceImpl.class,
        OAuth2AccessTokenRedisDAO.class,
        JdbcReliableEventPort.class
})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class OAuth2TokenServiceImplOutboxPreWriteTest extends BaseDbAndRedisUnitTest {

    private static final String COMPENSATION_EVENT_TYPE =
            SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION;

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper refreshTokenMapper;
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

    // ========== 种子与查询工具 ==========

    private void mockClient(String clientId) {
        OAuth2ClientDO client = randomPojo(OAuth2ClientDO.class);
        client.setClientId(clientId).setAccessTokenValiditySeconds(1800).setRefreshTokenValiditySeconds(86400);
        when(oauth2ClientService.validOAuthClientFromCache(clientId)).thenReturn(client);
    }

    private OAuth2RefreshTokenDO seedRefresh(String clientId, Long userId, LocalDateTime expiresTime) {
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO();
        refresh.setRefreshToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setScopes(List.of("read")).setExpiresTime(expiresTime);
        refresh.setTenantId(1L);
        refreshTokenMapper.insert(refresh);
        return refresh;
    }

    private OAuth2AccessTokenDO seedAccess(String clientId, Long userId, String refreshToken,
                                           LocalDateTime expiresTime) {
        OAuth2AccessTokenDO access = new OAuth2AccessTokenDO();
        access.setAccessToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setRefreshToken(refreshToken).setScopes(List.of("read"))
                .setExpiresTime(expiresTime);
        access.setUserInfo(Map.of("nickname", "ut-user"));
        access.setTenantId(1L);
        accessTokenMapper.insert(access);
        return access;
    }

    /** 查询已预写的补偿事件行（按 id 升序，便于稳定断言）。 */
    private List<Map<String, Object>> queryCompensationEvents() {
        return jdbcTemplate.queryForList(
                "SELECT id, event_type, biz_type, biz_id, payload, tenant_id, actor_type "
                        + "FROM outbox_event WHERE event_type = ? ORDER BY id ASC",
                COMPENSATION_EVENT_TYPE);
    }

    private long countCompensationEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM outbox_event WHERE event_type = ?",
                Long.class, COMPENSATION_EVENT_TYPE);
        return count == null ? 0L : count;
    }

    // ========== 用例 1：refreshAccessToken 淘汰旧代际 → 每个旧 access 预写一条事件 ==========

    /**
     * RED：{@code refreshAccessToken} 淘汰 2 个旧代际访问令牌时，事务内应预写 2 条 ACCESS 类型补偿事件；
     * 骨架下 {@code appendRevocationCompensationEvent} 只 log.debug，outbox_event 表 0 行 → 断言失败。
     */
    @Test
    public void refreshAccessToken_committedTx_preWritesAccessEventsForEachEvictedToken() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2RefreshTokenDO refresh = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        seedAccess(clientId, userId, refresh.getRefreshToken(), LocalDateTime.now().plusMinutes(30));
        seedAccess(clientId, userId, refresh.getRefreshToken(), LocalDateTime.now().plusMinutes(30));
        assertEquals(0L, countCompensationEvents(), "前置：无历史事件");

        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.refreshAccessToken(refresh.getRefreshToken(), clientId));

        List<Map<String, Object>> events = queryCompensationEvents();
        assertEquals(2, events.size(),
                "refreshAccessToken 淘汰 2 个旧代际 access 应预写 2 条补偿事件（骨架下 0 → RED）");
        for (Map<String, Object> event : events) {
            assertEquals(SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_ACCESS_TOKEN, event.get("biz_type"),
                    "预写事件 biz_type 必须为 oauth2_access_token");
            assertEquals(1L, ((Number) event.get("tenant_id")).longValue(),
                    "预写事件 tenant_id 必须来自 TenantContextHolder");
            assertNotNull(event.get("payload"), "预写事件 payload 必须非空（承载 tokenId/tokenType/expiresTime）");
            String payload = String.valueOf(event.get("payload"));
            assertTrue(payload.contains("\"tokenType\":\"ACCESS\""),
                    "预写事件 payload 必须包含 tokenType=ACCESS，实际：" + payload);
        }
    }

    // ========== 用例 2：removeAccessToken → 预写 1 ACCESS + 1 REFRESH ==========

    /**
     * RED：{@code removeAccessToken} 撤销会话（1 个 access + 关联 refresh）时应预写 2 条事件
     * （1 ACCESS + 1 REFRESH）；骨架下 0 行 → RED。
     */
    @Test
    public void removeAccessToken_committedTx_preWritesAccessAndRefreshEvents() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2RefreshTokenDO refresh = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        OAuth2AccessTokenDO access = seedAccess(clientId, userId, refresh.getRefreshToken(),
                LocalDateTime.now().plusMinutes(30));
        assertEquals(0L, countCompensationEvents(), "前置：无历史事件");

        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.removeAccessToken(access.getAccessToken()));

        List<Map<String, Object>> events = queryCompensationEvents();
        assertEquals(2, events.size(),
                "removeAccessToken 应预写 2 条事件（1 ACCESS + 1 REFRESH），骨架下 0 → RED");
        long accessCount = events.stream()
                .filter(e -> SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_ACCESS_TOKEN.equals(e.get("biz_type")))
                .count();
        long refreshCount = events.stream()
                .filter(e -> SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_REFRESH_TOKEN.equals(e.get("biz_type")))
                .count();
        assertEquals(1L, accessCount, "必须恰有 1 条 ACCESS 类型事件");
        assertEquals(1L, refreshCount, "必须恰有 1 条 REFRESH 类型事件（合成凭据 key 命名空间与 ACCESS 共用）");
    }

    // ========== 用例 3：removeAccessToken(userId, userType) 用户级批量撤销 → 多会话预写多条事件 ==========

    /**
     * RED：{@code removeAccessToken(Long, Integer)} 撤销用户 2 个会话（各含 1 access + 1 refresh）时，
     * 应预写 4 条事件（2 ACCESS + 2 REFRESH）；骨架下 0 → RED。
     */
    @Test
    public void removeAccessTokenByUser_committedTx_preWritesEventsForAllSessions() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2RefreshTokenDO refresh1 = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        seedAccess(clientId, userId, refresh1.getRefreshToken(), LocalDateTime.now().plusMinutes(30));
        OAuth2RefreshTokenDO refresh2 = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        seedAccess(clientId, userId, refresh2.getRefreshToken(), LocalDateTime.now().plusMinutes(30));
        assertEquals(0L, countCompensationEvents(), "前置：无历史事件");

        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        List<Map<String, Object>> events = queryCompensationEvents();
        assertEquals(4, events.size(),
                "removeAccessToken(userId, userType) 2 会话应预写 4 条事件（2 ACCESS + 2 REFRESH），骨架下 0 → RED");
        long accessCount = events.stream()
                .filter(e -> SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_ACCESS_TOKEN.equals(e.get("biz_type")))
                .count();
        long refreshCount = events.stream()
                .filter(e -> SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_REFRESH_TOKEN.equals(e.get("biz_type")))
                .count();
        assertEquals(2L, accessCount, "必须恰有 2 条 ACCESS 类型事件（每会话 1 条）");
        assertEquals(2L, refreshCount, "必须恰有 2 条 REFRESH 类型事件（每会话 1 条）");
    }

    // ========== 用例 4：业务事务回滚 → outbox 事件也回滚（保护性回归） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：业务事务回滚时 outbox 事件也必须回滚——
     * MANDATORY 传播语义硬契约，禁止 GREEN 实现改用 REQUIRES_NEW 独立事务写事件。
     *
     * <p>骨架下：0 事件（未 append） → 断言 0 通过。
     * <p>GREEN 下：事务内 append 2 事件，事务回滚 → DB 侧回滚 → 断言 0 通过。
     */
    @Test
    public void removeAccessToken_rolledBackTx_noEventPersisted() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2RefreshTokenDO refresh = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        OAuth2AccessTokenDO access = seedAccess(clientId, userId, refresh.getRefreshToken(),
                LocalDateTime.now().plusMinutes(30));

        try {
            transactionTemplate.executeWithoutResult(status -> {
                oauth2TokenService.removeAccessToken(access.getAccessToken());
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }

        assertEquals(0L, countCompensationEvents(),
                "业务事务回滚时 outbox 事件必须一并回滚（MANDATORY 传播硬契约，禁止 REQUIRES_NEW 独立事务）");
        assertNotNull(accessTokenMapper.selectByAccessToken(access.getAccessToken()),
                "前置：DB access 行随回滚恢复");
    }

    // ========== 用例 5：checkAccessToken 读路径不预写（保护性回归） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：{@code checkAccessToken} 是<b>读路径</b>，
     * 不得预写补偿事件——锁定「每撤销 IO+1」边界（计划 §5-3），读路径 IO 不放大。
     *
     * <p>即使 checkAccessToken 内部命中「Redis 有值 + DB 无行」自愈路径（.A 逻辑），
     * 也不应触发补偿事件——自愈 evict 已在读路径内闭环，无需异步补偿。
     */
    @Test
    public void checkAccessToken_readPath_doesNotPreWriteEvent() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2RefreshTokenDO refresh = seedRefresh(clientId, userId, LocalDateTime.now().plusDays(1));
        OAuth2AccessTokenDO access = seedAccess(clientId, userId, refresh.getRefreshToken(),
                LocalDateTime.now().plusMinutes(30));
        assertEquals(0L, countCompensationEvents(), "前置：无历史事件");

        // 读路径调用（不撤销，DB 行仍在，checkAccessToken 通过）
        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.checkAccessToken(access.getAccessToken()));

        assertEquals(0L, countCompensationEvents(),
                "checkAccessToken 读路径不得预写补偿事件（锁定每撤销 IO+1 边界，读路径 IO 不放大）");
    }

}
