package cn.zszj.framework.idempotent.core.redis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 幂等 Redis DAO
 *
 * ZS-SEC-011.A：value 存储参数摘要（digest），用于同键异参冲突检测
 *
 * @author 芋道源码
 */
@AllArgsConstructor
public class IdempotentRedisDAO {

    /**
     * 幂等操作
     *
     * KEY 格式：idempotent:%s // 参数为 uuid
     * VALUE 格式：String（参数摘要 MD5）
     * 过期时间：不固定
     */
    private static final String IDEMPOTENT = "idempotent:%s";

    private final StringRedisTemplate redisTemplate;

    /**
     * 设置幂等键（携带参数摘要），如果不存在则设置成功
     *
     * @param key      幂等键
     * @param digest   参数摘要（MD5）
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return 是否设置成功（true = 首次请求，false = 重复请求）
     */
    public Boolean setIfAbsent(String key, String digest, long timeout, TimeUnit timeUnit) {
        String redisKey = formatKey(key);
        return redisTemplate.opsForValue().setIfAbsent(redisKey, digest, timeout, timeUnit);
    }

    /**
     * 获取已存储的参数摘要
     *
     * @param key 幂等键
     * @return 参数摘要，不存在返回 null
     */
    public String getDigest(String key) {
        return redisTemplate.opsForValue().get(formatKey(key));
    }

    public void delete(String key) {
        String redisKey = formatKey(key);
        redisTemplate.delete(redisKey);
    }

    private static String formatKey(String key) {
        return String.format(IDEMPOTENT, key);
    }

}