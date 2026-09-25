package cn.zszj.framework.redis.core;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * ZS-PERM-004.C：缓存版本护栏——防「驱逐失败后晚到回填把旧值写回」。
 *
 * <p>机制：版本键 {@code [prefix:]__cachever__:{cacheName}} 以独立前缀存储（不落在
 * {@code {cacheName}:} 命名空间下，{@code RedisCache.clear()} 的 SCAN 模式不得误删版本键）；
 * get miss 时快照版本 → 驱逐先 bump 版本 → put 写回前对比版本：不一致即丢弃（晚到旧值）。
 *
 * <p>命令走原始字节 {@code INCR} / {@code GET}（不经 value 反序列化，规避 JSON 编码歧义）；
 * 键不存在视为 0；版本值损坏（人工误写等）同样视为 0——bump 的 INCR 会以数值语义覆盖纠偏。
 *
 * @author ZS-PERM-004.C
 */
public class CacheVersionGuard {

    /** 版本键固定中缀（独立于业务缓存键命名空间）。 */
    static final String VERSION_KEY_INFIX = "__cachever__:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final String normalizedKeyPrefix;

    public CacheVersionGuard(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        // 归一化规则与 redisCacheConfiguration.computePrefixWith 一致（无冒号则补冒号）
        if (keyPrefix == null || keyPrefix.isEmpty()) {
            this.normalizedKeyPrefix = "";
        } else {
            this.normalizedKeyPrefix = keyPrefix.endsWith(":") ? keyPrefix : keyPrefix + ":";
        }
    }

    /**
     * 读取缓存当前版本（键不存在 → 0）。
     *
     * <p>raw GET（原始字节）——不经 value 反序列化，规避 JSON 编码歧义。连接异常向上传播，
     * 由调用方决定 fail-safe 策略（快照侧存哨兵 / put 侧丢弃写回）。
     */
    public long currentVersion(String cacheName) {
        byte[] raw = redisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.get(versionKey(cacheName).getBytes(StandardCharsets.UTF_8)));
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(new String(raw, StandardCharsets.UTF_8).trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /**
     * 推进缓存版本（迁移代际——在途旧值写回即刻失效）。
     *
     * <p>raw INCR——键不存在时由 Redis 原子创建为 1；重复 bump 幂等无害（版本只增不减）。
     */
    public void bumpVersion(String cacheName) {
        redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.incr(versionKey(cacheName).getBytes(StandardCharsets.UTF_8)));
    }

    /** 版本键构造：{@code [prefix:]__cachever__:{cacheName}}。 */
    String versionKey(String cacheName) {
        return normalizedKeyPrefix + VERSION_KEY_INFIX + cacheName;
    }

}
