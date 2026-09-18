package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.RedisKeyConstants;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.framework.outbox.OAuth2TokenRevocationCompensationSink;
import cn.zszj.module.system.framework.outbox.SystemOutboxEventTypes;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-005.B：撤销主逻辑 → Outbox 预写 → Sink 补偿<b>端到端半集成测试</b>。
 *
 * <p><b>半集成模式说明</b>：不引入 {@code OutboxDispatcherService}（依赖 {@code dispatcher_lease} 表，
 * 该表归 JOB-002 装配，未在 system 测试上下文建表），改为「Service 事务提交 → 手工读 outbox_event 表 →
 * 构造 {@link OutboxEventRecord} → 直接调用 {@link OAuth2TokenRevocationCompensationSink#deliver}」，
 * 覆盖 Service→outbox→Sink 完整链路，不重复 JOB-002 的 dispatcher 领取/租约/重试测试。
 *
 * <p><b>验收映射</b>（对齐开发计划 §3 用例 12/13/13b/14）：
 * <ul>
 *   <li>用例 12：{@link #compensation_committedRevocation_sinkReplaysMarkRevokedAndDelete}</li>
 *   <li>用例 13：{@link #compensation_redisFailedInAfterCompletion_sinkStillCompensatesFromOutbox}</li>
 *   <li>用例 13b：{@link #compensation_normalPathIdempotent_multipleSinkInvocationsSafe}</li>
 *   <li>用例 14：{@link #compensation_rolledBackTx_noEventPersisted_nothingToDispatch}（保护性回归）</li>
 * </ul>
 *
 * @author ZS-LOGIN-005.B
 */
@Import({
        OAuth2TokenServiceImpl.class,
        OAuth2AccessTokenRedisDAO.class,
        JdbcReliableEventPort.class,
        OAuth2TokenRevocationCompensationSink.class
})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-enabled=false")
public class LoginCompensationIntegrationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;
    @Resource
    private OAuth2TokenRevocationCompensationSink sink;
    @Resource
    private OAuth2AccessTokenMapper accessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper refreshTokenMapper;
    @Resource
    private OAuth2AccessTokenRedisDAO redisDAO;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
                .setScopes(List.of("read")).setExpiresTime(DateUtils.now().plusDays(1));
        refresh.setTenantId(1L);
        refreshTokenMapper.insert(refresh);
        OAuth2AccessTokenDO access = new OAuth2AccessTokenDO();
        access.setAccessToken(randomString()).setUserId(userId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setClientId(clientId)
                .setRefreshToken(refresh.getRefreshToken())
                .setScopes(List.of("read"))
                .setExpiresTime(DateUtils.now().plusMinutes(30));
        access.setUserInfo(Map.of("nickname", "ut-user"));
        access.setTenantId(1L);
        accessTokenMapper.insert(access);
        return access;
    }

    private void preheatCache(OAuth2AccessTokenDO access) {
        redisDAO.set(access);
        assertNotNull(redisDAO.get(access.getAccessToken()), "前置：缓存预热必须成功");
    }

    /** 从 outbox_event 表按 id 升序读取所有补偿事件并转为 OutboxEventRecord（模拟 dispatcher 领取结果）。 */
    private List<OutboxEventRecord> readCompensationEventsAsRecords() {
        return jdbcTemplate.query(
                "SELECT id, event_type, biz_type, biz_id, biz_version, payload, headers, tenant_id, "
                        + "retry_count, actor_type, actor_id, trace_id, claimed_by, claim_token "
                        + "FROM outbox_event WHERE event_type = ? ORDER BY id ASC",
                (rs, rowNum) -> new OutboxEventRecord(
                        rs.getLong("id"),
                        rs.getString("event_type"),
                        rs.getString("biz_type"),
                        rs.getString("biz_id"),
                        rs.getString("biz_version"),
                        rs.getString("payload"),
                        rs.getString("headers"),
                        (Long) rs.getObject("tenant_id"),
                        rs.getInt("retry_count"),
                        rs.getString("actor_type"),
                        rs.getString("actor_id"),
                        rs.getString("trace_id"),
                        rs.getString("claimed_by"),
                        rs.getString("claim_token")),
                SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION);
    }

    private long countCompensationEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM outbox_event WHERE event_type = ?",
                Long.class, SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION);
        return count == null ? 0L : count;
    }

    private boolean tombstoneExists(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                String.format(RedisKeyConstants.OAUTH2_ACCESS_TOKEN_REVOKE_TOMBSTONE, token)));
    }

    // ========== 用例 12：撤销 → 提交 → Sink 补偿成功 ==========

    /**
     * RED：撤销主逻辑事务提交后，outbox_event 表存在预写事件；手工派发至 Sink，
     * Sink 反查 DB（deleted=TRUE）→ 重放 {@code markRevoked + delete} → 缓存清空 + 墓碑存在。
     * <p>骨架下 outbox_event 表 0 行，无法构造 OutboxEventRecord → 断言失败。
     */
    @Test
    public void compensation_committedRevocation_sinkReplaysMarkRevokedAndDelete() throws Exception {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        preheatCache(access);

        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.removeAccessToken(access.getAccessToken()));

        List<OutboxEventRecord> events = readCompensationEventsAsRecords();
        assertEquals(2, events.size(),
                "撤销提交后 outbox_event 必须有 2 条预写事件（1 ACCESS + 1 REFRESH），骨架下 0 → RED");

        // 手工派发所有事件到 Sink（模拟 dispatcher 领取 → 投递）
        for (OutboxEventRecord event : events) {
            sink.deliver(event);
        }

        // ACCESS 事件补偿后：access token 缓存清空 + 墓碑存在
        assertNull(redisDAO.get(access.getAccessToken()), "Sink 补偿后 access 缓存必须已清");
        assertTrue(tombstoneExists(access.getAccessToken()), "Sink 补偿后 access 墓碑必须存在");
    }

    // ========== 用例 13：Redis 一次失败 → 事务提交 → Sink 从 outbox 补偿 ==========

    /**
     * RED：afterCompletion 里 {@code revokeWithTombstone} 的 {@code markRevoked} 抛异常（Redis 一次失败），
     * 事务已提交（outbox 事件已持久化），.A 语义只 WARN 无重放；本卡补偿链路：Sink 从 outbox 读取事件
     * → 反查 DB deleted=TRUE → 重放成功 → 缓存清空 + 墓碑存在（.A 遗留缺陷被修复）。
     */
    @Test
    public void compensation_redisFailedInAfterCompletion_sinkStillCompensatesFromOutbox() throws Exception {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        preheatCache(access);

        // 用 spy 让 Service 内的 markRevoked 首次调用抛异常（模拟 Redis 一次失败）
        OAuth2AccessTokenRedisDAO realDao = (OAuth2AccessTokenRedisDAO) ReflectionTestUtils
                .getField(oauth2TokenService, "oauth2AccessTokenRedisDAO");
        assertNotNull(realDao, "前置：Service 必须已注入 RedisDAO");
        OAuth2AccessTokenRedisDAO spyDao = Mockito.spy(realDao);
        Mockito.doThrow(new IllegalStateException("Redis 一次失败（模拟）"))
                .doCallRealMethod()  // 后续调用恢复真实行为（Sink 补偿时用真实 DAO）
                .when(spyDao).markRevoked(Mockito.anyString(), Mockito.anyLong());
        ReflectionTestUtils.setField(oauth2TokenService, "oauth2AccessTokenRedisDAO", spyDao);
        try {
            // 事务提交成功（afterCompletion 里 markRevoked 抛异常被 revokeWithTombstone catch 记 WARN，不阻断）
            transactionTemplate.executeWithoutResult(status ->
                    oauth2TokenService.removeAccessToken(access.getAccessToken()));
        } finally {
            ReflectionTestUtils.setField(oauth2TokenService, "oauth2AccessTokenRedisDAO", realDao);
        }

        // Redis 一次失败后：缓存条目仍在（delete 也可能被 skip 或成功，取决于失败点）
        // outbox 事件必须已预写（事务内 append 与 afterCompletion 分离）
        List<OutboxEventRecord> events = readCompensationEventsAsRecords();
        assertEquals(2, events.size(),
                "Redis 一次失败不影响事务内 outbox 预写（appendRevocationCompensationEvent 在事务体内），骨架下 0 → RED");

        // Sink 从 outbox 补偿（用真实 RedisDAO，此时 Redis 已恢复）
        for (OutboxEventRecord event : events) {
            sink.deliver(event);
        }

        assertNull(redisDAO.get(access.getAccessToken()),
                "Sink 补偿后缓存必须已清（.A 遗留缺陷：Redis 一次失败无重放，本卡修复）");
        assertTrue(tombstoneExists(access.getAccessToken()),
                "Sink 补偿后墓碑必须存在（阻塞后续回填复活）");
    }

    // ========== 用例 13b：正常路径幂等 ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过 GREEN 后才有事件可派发）：正常撤销路径下，
     * Sink 被多次调用（模拟 dispatcher at-least-once 重复投递）不会误撤销其它有效凭据、
     * 不产生副作用叠加，最终状态一致。
     */
    @Test
    public void compensation_normalPathIdempotent_multipleSinkInvocationsSafe() throws Exception {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        preheatCache(access);

        transactionTemplate.executeWithoutResult(status ->
                oauth2TokenService.removeAccessToken(access.getAccessToken()));

        List<OutboxEventRecord> events = readCompensationEventsAsRecords();
        assertEquals(2, events.size(), "前置：必须有 2 条预写事件，骨架下 0 → RED");

        // 首次派发
        for (OutboxEventRecord event : events) {
            sink.deliver(event);
        }
        assertNull(redisDAO.get(access.getAccessToken()));
        assertTrue(tombstoneExists(access.getAccessToken()));

        // 二次派发（模拟 dispatcher at-least-once 重试）：必须幂等无副作用叠加
        for (OutboxEventRecord event : events) {
            assertDoesNotThrow(() -> sink.deliver(event), "重复派发必须幂等不抛");
        }
        assertNull(redisDAO.get(access.getAccessToken()), "二次派发后缓存仍空");
        assertTrue(tombstoneExists(access.getAccessToken()), "二次派发后墓碑仍存在");

        // 三次派发（强幂等断言）
        for (OutboxEventRecord event : events) {
            sink.deliver(event);
        }
        assertTrue(tombstoneExists(access.getAccessToken()), "三次派发后墓碑仍存在（SETNX 幂等）");
    }

    // ========== 用例 14：业务事务回滚 → 无事件可派发（保护性回归） ==========

    /**
     * 保护性回归（骨架与 GREEN 均须通过）：业务事务回滚时 outbox 事件也回滚（MANDATORY 传播），
     * 表 0 行 → 无事件可派发 → 无需 Sink 补偿；DB access 行随回滚恢复有效，缓存预热条目仍在
     * （afterCompletion 里 revokeWithTombstone 仍执行——.A 语义：宁可可用性抖动不留撤销态不一致）。
     */
    @Test
    public void compensation_rolledBackTx_noEventPersisted_nothingToDispatch() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        preheatCache(access);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                oauth2TokenService.removeAccessToken(access.getAccessToken());
                throw new RuntimeException("强制回滚");
            });
        } catch (RuntimeException ignored) {
            // 预期回滚
        }

        assertEquals(0L, countCompensationEvents(),
                "业务事务回滚 → outbox 事件一并回滚（MANDATORY 传播硬契约）");
        List<OutboxEventRecord> events = readCompensationEventsAsRecords();
        assertTrue(events.isEmpty(), "无事件可派发");
        assertNotNull(accessTokenMapper.selectByAccessToken(access.getAccessToken()),
                "前置：DB access 行随回滚恢复有效");
        // .A 语义：afterCompletion 里 revokeWithTombstone 仍执行（回滚补偿），缓存被清（miss 回源重建）
        assertNull(redisDAO.get(access.getAccessToken()),
                ".A 语义：回滚补偿仍清缓存，宁可可用性抖动不留撤销态不一致");
    }

}
