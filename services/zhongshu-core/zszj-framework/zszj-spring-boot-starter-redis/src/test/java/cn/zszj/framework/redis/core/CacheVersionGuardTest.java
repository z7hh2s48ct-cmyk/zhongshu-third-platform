package cn.zszj.framework.redis.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-PERM-004.C：{@link CacheVersionGuard} 版本键合同单测（mock RedisTemplate，不依赖真实 Redis）。
 *
 * <p><b>RED 依据</b>：骨架阶段 {@code currentVersion} 恒返回 0、{@code bumpVersion} 空实现——
 * 用例 2（INCR 命令与键格式）与用例 3（前缀归一化键格式）失败；GREEN 落地 raw INCR/GET 后转绿。
 *
 * <p><b>契约</b>（开发计划 §2 D4）：
 * <ul>
 *   <li>版本存独立前缀键 {@code [prefix:]__cachever__:{cacheName}}——不放在 {@code {cacheName}:} 前缀下，
 *       RedisCache.clear() 的 SCAN 模式（{@code {cacheName}:*}）不得误删版本键；</li>
 *   <li>命令走原始字节 {@code INCR} / {@code GET}（不经 value 反序列化，规避 JSON 歧义）；GET null → 0；</li>
 *   <li>keyPrefix 归一化规则与 {@code redisCacheConfiguration.computePrefixWith} 一致（无冒号则补冒号）。</li>
 * </ul>
 *
 * @author ZS-PERM-004.C
 */
public class CacheVersionGuardTest {

    private RedisTemplate<String, Object> redisTemplate;
    private RedisConnection connection;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        connection = mock(RedisConnection.class);
        // execute(RedisCallback) 桥接：直接以 mock 连接执行回调，验证 raw 命令语义
        when(redisTemplate.execute(ArgumentMatchers.<RedisCallback<Object>>any())).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        });
    }

    // ========== 用例 1：GET null 视为 0 ==========

    /**
     * RED：键不存在（GET 返回 null）→ 版本 0；非 null（"3"）→ 解析为 3。
     */
    @Test
    public void currentVersion_nullMeansZero() {
        when(connection.get(any(byte[].class))).thenReturn(null, "3".getBytes(StandardCharsets.UTF_8));

        CacheVersionGuard guard = new CacheVersionGuard(redisTemplate, "");

        assertEquals(0L, guard.currentVersion("role"), "键不存在时版本必须视为 0");
        assertEquals(3L, guard.currentVersion("role"), "已有版本必须按 UTF-8 数字解析");
    }

    // ========== 用例 2：bump 走 raw INCR 且键带独立前缀 ==========

    /**
     * RED：bumpVersion 必须对 {@code __cachever__:role} 执行 raw INCR；骨架空实现 → 0 次调用 → 失败。
     */
    @Test
    public void bumpVersion_incrWithIsolatedPrefixKey() {
        when(connection.incr(any(byte[].class))).thenReturn(1L);

        new CacheVersionGuard(redisTemplate, "").bumpVersion("role");

        verify(connection).incr("__cachever__:role".getBytes(StandardCharsets.UTF_8));
    }

    // ========== 用例 3：keyPrefix 归一化（与 computePrefixWith 同规则） ==========

    /**
     * RED：前缀无冒号（"zszj"）→ 归一化为 "zszj:"；已带冒号（"zszj:"）→ 原样——
     * 归一化规则与 {@code redisCacheConfiguration.computePrefixWith} 一致。
     */
    @Test
    public void versionKeyFormat_appliesNormalizedKeyPrefix() {
        when(connection.incr(any(byte[].class))).thenReturn(1L);

        new CacheVersionGuard(redisTemplate, "zszj").bumpVersion("role");
        new CacheVersionGuard(redisTemplate, "zszj:").bumpVersion("role");

        verify(connection, times(2)).incr("zszj:__cachever__:role".getBytes(StandardCharsets.UTF_8));
    }

}
