package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OAuth2AccessTokenRedisDAO} 会话代际键 TTL 的单元测试类。
 *
 * <p>ZS-LOGIN-002 P2-B（codex r0）：当 token 剩余不足 1 秒时 {@code secondsUntil} 返回 0，
 * 而原实现把 0 当作"无限期"——配置 1 秒 access-token 生命周期的客户端每次刷新都创建永不过期的
 * access-generation 键；{@code nextSessionGeneration} 在会话最后一秒刷新时同样创建永久键。
 * access-generation 键在登出时刻意不删，故这些条目永久累积（内存泄漏）。
 *
 * <p>修复合同：<b>正的亚秒生命周期用毫秒 TTL 保留（有限过期），已过期条目（<= 0）跳过写入</b>而非写永久键。
 *
 * <p>RED 说明：本测试用<b>毫秒语义</b>调用 DAO。原实现按秒解释——
 * <ul>
 *     <li>{@code nextSessionGeneration(rt, 800)} 原设 800 <b>秒</b> TTL（本测试断言 <= 800 毫秒 → 失败）；</li>
 *     <li>{@code nextSessionGeneration(rt, 0)} 原会 INCR 建键且不设过期（本测试断言返回 0 且无键 → 失败）；</li>
 *     <li>{@code setAccessTokenGeneration(at, g, -1)} 原走 else 分支写<b>永久键</b>（本测试断言跳过、无键 → 失败）。</li>
 * </ul>
 *
 * @author ZS-LOGIN-002 P2-B
 */
@Import(OAuth2AccessTokenRedisDAO.class)
public class OAuth2AccessTokenRedisDaoTtlTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static String sessionGenerationKey(String refreshToken) {
        return String.format(RedisKeyConstants.OAUTH2_REFRESH_SESSION_GENERATION, refreshToken);
    }

    private static String accessGenerationKey(String accessToken) {
        return String.format(RedisKeyConstants.OAUTH2_ACCESS_SESSION_GENERATION, accessToken);
    }

    /**
     * P2-B ①：正的亚秒 TTL（800ms）必须产生<b>有限</b>毫秒级 TTL，而非被当作秒或永久键。
     */
    @Test
    public void nextSessionGeneration_subSecondPositiveTtl_setsFiniteMillisTtl() {
        String refreshToken = randomString();

        long generation = oauth2AccessTokenRedisDAO.nextSessionGeneration(refreshToken, 800L);

        assertEquals(1L, generation, "首次推进代际应返回 1");
        Long ttlMillis = stringRedisTemplate.getExpire(sessionGenerationKey(refreshToken), TimeUnit.MILLISECONDS);
        assertNotNull(ttlMillis, "代际键必须存在");
        assertTrue(ttlMillis > 0 && ttlMillis <= 800L,
                "亚秒正 TTL 必须落成有限毫秒过期（0 < ttl <= 800ms），实际=" + ttlMillis
                        + "；若为 -1 表示永久键泄漏，若远大于 800 表示被误当秒");
        // 值可读回
        assertEquals(1L, oauth2AccessTokenRedisDAO.getSessionGeneration(refreshToken).longValue());
    }

    /**
     * P2-B ②：已过期（ttl <= 0）必须<b>跳过</b>推进，不得创建无 TTL 的永久代际键。
     */
    @Test
    public void nextSessionGeneration_expired_skipsAndCreatesNoKey() {
        String refreshToken = randomString();

        long generation = oauth2AccessTokenRedisDAO.nextSessionGeneration(refreshToken, 0L);

        assertEquals(0L, generation, "已过期应跳过推进并返回 0");
        assertNull(stringRedisTemplate.opsForValue().get(sessionGenerationKey(refreshToken)),
                "已过期不得创建代际键（否则为永不过期的泄漏键）");
    }

    /**
     * P2-B ③：已过期（ttl <= 0）的 access-generation 必须<b>跳过</b>写入，不得写永久键。
     */
    @Test
    public void setAccessTokenGeneration_expired_skipsAndCreatesNoPermanentKey() {
        String accessToken = randomString();

        oauth2AccessTokenRedisDAO.setAccessTokenGeneration(accessToken, 5L, -1L);

        assertNull(oauth2AccessTokenRedisDAO.getAccessTokenGeneration(accessToken),
                "已过期访问令牌不应登记代际（避免永不过期的 access-generation 键累积）");
        Long ttlMillis = stringRedisTemplate.getExpire(accessGenerationKey(accessToken), TimeUnit.MILLISECONDS);
        // 键不存在时 getExpire 返回 -2；永久键返回 -1（正是本缺陷）
        assertTrue(ttlMillis != null && ttlMillis == -2L,
                "已过期时不得存在 access-generation 键；ttl=" + ttlMillis + "（-1=永久键泄漏，-2=不存在，符合预期）");
    }

    /**
     * P2-B ④：正的亚秒 TTL 的 access-generation 必须落成<b>有限</b>毫秒 TTL 并可读回代际值。
     */
    @Test
    public void setAccessTokenGeneration_subSecondPositiveTtl_setsFiniteMillisTtl() {
        String accessToken = randomString();

        oauth2AccessTokenRedisDAO.setAccessTokenGeneration(accessToken, 7L, 900L);

        assertEquals(7L, oauth2AccessTokenRedisDAO.getAccessTokenGeneration(accessToken).longValue());
        Long ttlMillis = stringRedisTemplate.getExpire(accessGenerationKey(accessToken), TimeUnit.MILLISECONDS);
        assertNotNull(ttlMillis);
        assertTrue(ttlMillis > 0 && ttlMillis <= 900L,
                "亚秒正 TTL 必须落成有限毫秒过期（0 < ttl <= 900ms），实际=" + ttlMillis);
    }
}
