package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.RedisKeyConstants;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.framework.outbox.TokenRevocationCompensationPayload.TokenType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-005.B：{@link OAuth2TokenRevocationCompensationSink} 单元测试（真实 H2 + 内嵌 Redis）。
 *
 * <p><b>RED 依据</b>：Sink.deliver 当前抛 {@link UnsupportedOperationException}（骨架状态），
 * 全部 7 个用例应失败；GREEN 阶段落地 6 步逻辑（反序列化 → 过期识别 → DB 反查 → 业务回滚保护 →
 * 物理清理容错 → 重放撤销）后全部转绿。
 *
 * <p><b>验收映射</b>（对齐开发计划 §3 表格用例 6~11 + 8b）：
 * <ul>
 *   <li>用例 6：{@link #deliver_idempotent_multipleInvocationsSameResult} — dispatcher at-least-once 幂等；</li>
 *   <li>用例 7：{@link #deliver_expiredToken_silentSkip} — 过期凭据静默 skip（Redis TTL 自清理）；</li>
 *   <li>用例 8：{@link #deliver_rowLogicallyDeleted_readsViaIncludeDeleted} — 逻辑删除后仍能反查 token 明文；</li>
 *   <li>用例 8b：{@link #deliver_rowStillActive_silentSkipNoRevoke} — <b>业务回滚保护主证据</b>；</li>
 *   <li>用例 9：{@link #deliver_rowPhysicallyPurged_silentReturn} — 物理清理容错；</li>
 *   <li>用例 10：{@link #deliver_refreshTokenType_readsFromRefreshTable} — REFRESH 分派；</li>
 *   <li>用例 11：{@link #deliver_redisFailsAgain_throwsForDispatcherRetry} — Redis 二次失败触发 dispatcher 重试。</li>
 * </ul>
 *
 * @author ZS-LOGIN-005.B
 */
@Import({OAuth2TokenRevocationCompensationSink.class, OAuth2AccessTokenRedisDAO.class})
public class OAuth2TokenRevocationCompensationSinkTest extends BaseDbAndRedisUnitTest {

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

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== 种子与构造工具 ==========

    private OAuth2AccessTokenDO seedAccessRow(LocalDateTime expiresTime) {
        OAuth2AccessTokenDO access = new OAuth2AccessTokenDO();
        access.setAccessToken(randomString())
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setRefreshToken(randomString())
                .setScopes(List.of("read"))
                .setExpiresTime(expiresTime);
        access.setUserInfo(Map.of("nickname", "ut-user"));
        access.setTenantId(1L);
        accessTokenMapper.insert(access);
        return access;
    }

    private OAuth2RefreshTokenDO seedRefreshRow(LocalDateTime expiresTime) {
        OAuth2RefreshTokenDO refresh = new OAuth2RefreshTokenDO();
        refresh.setRefreshToken(randomString())
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setScopes(List.of("read"))
                .setExpiresTime(expiresTime);
        refresh.setTenantId(1L);
        refreshTokenMapper.insert(refresh);
        return refresh;
    }

    private OutboxEventRecord buildEvent(Long tokenId, TokenType tokenType, LocalDateTime expiresTime) {
        TokenRevocationCompensationPayload payload = TokenRevocationCompensationPayload.builder()
                .tokenId(tokenId)
                .tokenType(tokenType)
                .expiresTime(expiresTime)
                .build();
        String bizType = tokenType == TokenType.ACCESS
                ? SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_ACCESS_TOKEN
                : SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_REFRESH_TOKEN;
        return new OutboxEventRecord(
                1L,
                SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION,
                bizType,
                String.valueOf(tokenId),
                null,
                JsonUtils.toJsonString(payload),
                null,
                1L,
                0,
                "SYSTEM",
                null,
                null,
                "ut-instance@ut-dispatcher",
                "ut-claim-token");
    }

    /** 预热缓存：将 DO 写入 Redis（走 SET_IF_NOT_REVOKED 门闩，前置无墓碑必然成功）。 */
    private void preheatCache(OAuth2AccessTokenDO access) {
        redisDAO.set(access);
        assertNotNull(redisDAO.get(access.getAccessToken()), "前置：缓存预热必须成功");
    }

    private void preheatCacheForRefresh(OAuth2RefreshTokenDO refresh) {
        // REFRESH 合成凭据 key 与 access 复用同一命名空间（.A 契约）
        OAuth2AccessTokenDO shim = new OAuth2AccessTokenDO();
        shim.setAccessToken(refresh.getRefreshToken())
                .setUserId(refresh.getUserId())
                .setUserType(refresh.getUserType())
                .setClientId(refresh.getClientId())
                .setRefreshToken(refresh.getRefreshToken())
                .setScopes(refresh.getScopes())
                .setExpiresTime(refresh.getExpiresTime());
        shim.setUserInfo(Map.of("nickname", "ut-user"));
        shim.setTenantId(1L);
        redisDAO.set(shim);
        assertNotNull(redisDAO.get(refresh.getRefreshToken()), "前置：REFRESH 缓存预热必须成功");
    }

    private boolean tombstoneExists(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                String.format(RedisKeyConstants.OAUTH2_ACCESS_TOKEN_REVOKE_TOMBSTONE, token)));
    }

    // ========== 用例 6：dispatcher at-least-once 幂等 ==========

    /**
     * RED：dispatcher 语义为 at-least-once，Sink 必须幂等——同一 event 多次 deliver 结果一致
     * （墓碑存在 + 缓存条目已清），无副作用叠加、无异常抛出。
     */
    @Test
    public void deliver_idempotent_multipleInvocationsSameResult() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        preheatCache(access);
        // 撤销已生效：mapper.deleteById 触发 MyBatis Plus 逻辑删除（deleted=TRUE）
        accessTokenMapper.deleteById(access.getId());
        OutboxEventRecord event = buildEvent(access.getId(), TokenType.ACCESS, access.getExpiresTime());

        sink.deliver(event);
        // 首次投递后：墓碑存在、缓存清空
        assertTrue(tombstoneExists(access.getAccessToken()), "首次 deliver 后墓碑必须存在");
        assertNull(redisDAO.get(access.getAccessToken()), "首次 deliver 后缓存必须已清");

        // 二次投递（模拟 dispatcher 重试）：结果必须一致，且不抛异常
        assertDoesNotThrow(() -> sink.deliver(event), "重复 deliver 必须幂等不抛");
        assertTrue(tombstoneExists(access.getAccessToken()), "重复 deliver 后墓碑仍存在");
        assertNull(redisDAO.get(access.getAccessToken()), "重复 deliver 后缓存仍空");

        // 三次投递（幂等强断言）
        sink.deliver(event);
        assertTrue(tombstoneExists(access.getAccessToken()), "三次 deliver 后墓碑仍存在");
    }

    // ========== 用例 7：过期凭据静默 skip ==========

    /**
     * RED：过期凭据（{@code expiresTime < now}，以事件载荷快照为准）Redis key TTL 到期自清理，
     * 无需墓碑——Sink 必须静默返回、不查 DB、不写 Redis、不抛异常。
     *
     * <p>codex r0 P2-7 加固：令牌行与缓存条目均活跃、<b>仅事件载荷 expiresTime 已过</b>（事件
     * 延迟投递的现实中存在形态）——过期分支必须先行静默返回（不查 DB、不动缓存）。若该分支被
     * 删除，Sink 会反查 DB（deleted=TRUE → 业务回滚保护放行）→ 对活跃缓存写墓碑 + {@code delete}
     * 删缓存条目 → 下方「无墓碑 + 缓存原样」双断言即转红（原版仅断言无墓碑，TTL≤0 守卫掩盖了
     * 分支缺失）。
     */
    @Test
    public void deliver_expiredToken_silentSkip() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        preheatCache(access);
        accessTokenMapper.deleteById(access.getId());
        OutboxEventRecord event = buildEvent(access.getId(), TokenType.ACCESS, LocalDateTime.now().minusMinutes(5));

        assertDoesNotThrow(() -> sink.deliver(event), "过期凭据 deliver 必须静默返回");
        assertFalse(tombstoneExists(access.getAccessToken()),
                "过期凭据不得写墓碑（Redis TTL 已自清理，墓碑无意义且浪费 key 空间）");
        assertNotNull(redisDAO.get(access.getAccessToken()),
                "过期事件路径不得触碰活跃缓存条目（过期分支必须先于 DB 反查/缓存删除静默返回）");
    }

    // ========== 用例 8：逻辑删除后穿透反查 ==========

    /**
     * RED：撤销已生效（{@code deleted=TRUE}）——Sink 必须<b>穿透</b> MyBatis Plus 逻辑删除过滤，
     * 经 {@code selectByIdIncludeDeleted} 反查到 token 明文，重放 {@code markRevoked + delete}。
     * <p>若 Sink 走标准 {@code selectById} 会因逻辑删除过滤读到 {@code null}，误判为「物理清理」而 skip，
     * 缓存遗留幽灵条目——本用例通过预热缓存 + 断言清空来锁定该缺陷。
     */
    @Test
    public void deliver_rowLogicallyDeleted_readsViaIncludeDeleted() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        preheatCache(access);
        accessTokenMapper.deleteById(access.getId());
        // 前置：普通 select 因逻辑删除过滤读到 null；专用 selectByIdIncludeDeleted 必须能读到
        assertNull(accessTokenMapper.selectById(access.getId()), "前置：普通 selectById 被逻辑删除过滤");
        TokenRowSnapshot snapshot = accessTokenMapper.selectByIdIncludeDeleted(access.getId());
        assertNotNull(snapshot, "前置：selectByIdIncludeDeleted 必须穿透");
        assertEquals(access.getAccessToken(), snapshot.getToken(), "前置：反查 token 明文一致");
        assertEquals(Boolean.TRUE, snapshot.getDeleted(), "前置：deleted 标志必须为 TRUE");

        OutboxEventRecord event = buildEvent(access.getId(), TokenType.ACCESS, access.getExpiresTime());
        sink.deliver(event);

        assertNull(redisDAO.get(access.getAccessToken()),
                "deleted=TRUE 时 Sink 必须重放 delete 清理缓存条目");
        assertTrue(tombstoneExists(access.getAccessToken()),
                "deleted=TRUE 时 Sink 必须重放 markRevoked 写墓碑（阻塞后续回填复活）");
    }

    // ========== 用例 8b：业务回滚保护主证据 ==========

    /**
     * RED（业务回滚保护）：预写 outbox 事件在业务事务体内发生，业务事务回滚时事件也回滚——
     * 但若事件已提交（业务事务成功后 dispatcher 领取前，业务方通过其它路径撤销了 deleted 标记的
     * 恢复场景，或竞态双写下 deleted 尚未刷入），Sink 反查发现 {@code deleted=FALSE} 且未过期，
     * 说明<b>凭据仍活跃</b>，必须静默 skip、绝不重放 {@code markRevoked + delete}——
     * 否则会误撤销有效凭据，用户被踢下线。
     *
     * <p>本用例是决策 5（业务事务内预写 + Sink 反查保护）的<b>核心防线证据</b>。
     */
    @Test
    public void deliver_rowStillActive_silentSkipNoRevoke() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        preheatCache(access);
        // 关键：不 deleteById，保持 deleted=FALSE（模拟业务回滚 / 事件误发场景）
        TokenRowSnapshot snapshot = accessTokenMapper.selectByIdIncludeDeleted(access.getId());
        assertNotNull(snapshot, "前置：行必须存在");
        assertEquals(Boolean.FALSE, snapshot.getDeleted(), "前置：deleted 必须为 FALSE（凭据活跃）");

        OutboxEventRecord event = buildEvent(access.getId(), TokenType.ACCESS, access.getExpiresTime());
        assertDoesNotThrow(() -> sink.deliver(event), "deleted=FALSE 时 Sink 必须静默 skip 不抛");

        assertNotNull(redisDAO.get(access.getAccessToken()),
                "业务回滚保护：deleted=FALSE 时不得清缓存（凭据仍活跃，用户不应被踢下线）");
        assertFalse(tombstoneExists(access.getAccessToken()),
                "业务回滚保护：deleted=FALSE 时不得写墓碑（否则会阻塞该凭据后续所有缓存回填）");
    }

    // ========== 用例 9：物理清理容错 ==========

    /**
     * RED：{@code selectByIdIncludeDeleted} 返回 {@code null}（行已被清理任务物理删除）——
     * 无补偿目标，视为已完成，Sink 必须静默返回不抛异常（否则 dispatcher 会永久重试死循环）。
     */
    @Test
    public void deliver_rowPhysicallyPurged_silentReturn() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        Long tokenId = access.getId();
        String tokenStr = access.getAccessToken();
        // 物理清理：JdbcTemplate 直接 DELETE（绕开 MyBatis Plus 逻辑删除）
        jdbcTemplate.update("DELETE FROM system_oauth2_access_token WHERE id = ?", tokenId);
        assertNull(accessTokenMapper.selectByIdIncludeDeleted(tokenId), "前置：行已物理清理");

        OutboxEventRecord event = buildEvent(tokenId, TokenType.ACCESS, access.getExpiresTime());
        assertDoesNotThrow(() -> sink.deliver(event), "物理清理后 deliver 必须静默返回");
        assertFalse(tombstoneExists(tokenStr), "物理清理后无 token 明文可墓碑，不得写入");
    }

    // ========== 用例 10：REFRESH 类型分派 ==========

    /**
     * RED：{@code tokenType=REFRESH} 时 Sink 必须分派到 {@link OAuth2RefreshTokenMapper}
     * 反查（而非 access 表），并复用同一 Redis key 命名空间写墓碑（.A 契约：refresh 作为合成
     * 凭据 key 时使用相同的 {@code oauth2_access_token:*} / {@code oauth2_access_token_revoke_tomb:*}）。
     */
    @Test
    public void deliver_refreshTokenType_readsFromRefreshTable() throws Exception {
        OAuth2RefreshTokenDO refresh = seedRefreshRow(LocalDateTime.now().plusDays(1));
        preheatCacheForRefresh(refresh);
        refreshTokenMapper.deleteById(refresh.getId());
        TokenRowSnapshot snapshot = refreshTokenMapper.selectByIdIncludeDeleted(refresh.getId());
        assertNotNull(snapshot, "前置：refresh 行穿透反查必须成功");
        assertEquals(Boolean.TRUE, snapshot.getDeleted(), "前置：refresh deleted=TRUE");

        OutboxEventRecord event = buildEvent(refresh.getId(), TokenType.REFRESH, refresh.getExpiresTime());
        sink.deliver(event);

        assertNull(redisDAO.get(refresh.getRefreshToken()),
                "REFRESH 类型 deliver 后合成凭据缓存必须已清");
        assertTrue(tombstoneExists(refresh.getRefreshToken()),
                "REFRESH 类型 deliver 后墓碑必须写入（同一命名空间）");
    }

    // ========== 用例 11：Redis 二次失败触发 dispatcher 重试 ==========

    /**
     * RED：Redis 二次失败（{@code markRevoked} 抛异常）时 Sink 必须<b>抛出</b>——
     * 静默吞异常会让 dispatcher 认为投递成功、事件被 complete，补偿链路断裂；
     * 抛出后 dispatcher 会按退避策略重试，最终进入 DEAD 台账（ZS-JOB-004）由人工介入。
     *
     * <p>本用例通过 {@link ReflectionTestUtils} 临时替换 Sink 内部 {@code OAuth2AccessTokenRedisDAO}
     * 字段为 mock，模拟 Redis 二次失败；finally 恢复真实 bean 避免污染后续测试。
     */
    @Test
    public void deliver_redisFailsAgain_throwsForDispatcherRetry() throws Exception {
        OAuth2AccessTokenDO access = seedAccessRow(LocalDateTime.now().plusMinutes(30));
        accessTokenMapper.deleteById(access.getId());
        OutboxEventRecord event = buildEvent(access.getId(), TokenType.ACCESS, access.getExpiresTime());

        OAuth2AccessTokenRedisDAO broken = Mockito.mock(OAuth2AccessTokenRedisDAO.class);
        Mockito.doThrow(new IllegalStateException("Redis 又失败"))
                .when(broken).markRevoked(Mockito.anyString(), Mockito.anyLong());
        OAuth2AccessTokenRedisDAO real = (OAuth2AccessTokenRedisDAO) ReflectionTestUtils
                .getField(sink, "oauth2AccessTokenRedisDAO");
        assertNotNull(real, "前置：Sink 必须已注入真实 RedisDAO");
        ReflectionTestUtils.setField(sink, "oauth2AccessTokenRedisDAO", broken);
        try {
            Exception thrown = assertThrows(Exception.class, () -> sink.deliver(event),
                    "Redis 二次失败时 Sink 必须抛出以触发 dispatcher 重试");
            assertTrue(thrown instanceof IllegalStateException
                            || thrown.getCause() instanceof IllegalStateException,
                    "抛出异常必须保留 Redis 失败根因（供 dispatcher 记录到 outbox_event.last_error）");
        } finally {
            ReflectionTestUtils.setField(sink, "oauth2AccessTokenRedisDAO", real);
        }
    }

}
