package cn.zszj.module.system.dal.redis.oauth2;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.OAUTH2_ACCESS_SESSION_GENERATION;
import static cn.zszj.module.system.dal.redis.RedisKeyConstants.OAUTH2_ACCESS_TOKEN;
import static cn.zszj.module.system.dal.redis.RedisKeyConstants.OAUTH2_REFRESH_SESSION_GENERATION;

/**
 * {@link OAuth2AccessTokenDO} 的 RedisDAO
 *
 * @author 芋道源码
 */
@Repository
public class OAuth2AccessTokenRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public OAuth2AccessTokenDO get(String accessToken) {
        String redisKey = formatKey(accessToken);
        return JsonUtils.parseObject(stringRedisTemplate.opsForValue().get(redisKey), OAuth2AccessTokenDO.class);
    }

    public void set(OAuth2AccessTokenDO accessTokenDO) {
        String redisKey = formatKey(accessTokenDO.getAccessToken());
        // 清理多余字段，避免缓存
        accessTokenDO.setUpdater(null).setUpdateTime(null).setCreateTime(null).setCreator(null).setDeleted(null);
        long time = LocalDateTimeUtil.between(LocalDateTime.now(), accessTokenDO.getExpiresTime(), ChronoUnit.SECONDS);
        if (time > 0) {
            stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(accessTokenDO), time, TimeUnit.SECONDS);
        }
    }

    public void delete(String accessToken) {
        String redisKey = formatKey(accessToken);
        stringRedisTemplate.delete(redisKey);
    }

    public void deleteList(Collection<String> accessTokens) {
        List<String> redisKeys = CollectionUtils.convertList(accessTokens, OAuth2AccessTokenRedisDAO::formatKey);
        stringRedisTemplate.delete(redisKeys);
    }

    // ========== ZS-LOGIN-002：会话代际（session generation）登记 ==========
    //
    // 设计取舍（见开发计划 §3.3 与「DB 字段变更警示」）：本底座刷新采用「沿用原 refresh token + 会话代际号 + 重放检测」，
    // 代际/重放状态<b>只存 Redis</b>，不新增 DB 列，以免与 ZS-DB-019.B 的 PG 回归及 Flyway 迁移产生耦合。
    // 代际登记是「审计可定位」与「重放可辨识」的辅助层；真正的重放拒绝由刷新时删除旧访问令牌（DB + Redis）保证，
    // 因此下列方法均为 best-effort：调用方须自行容错，Redis 抖动不得阻断合法刷新（一致性恢复归 ZS-LOGIN-005.A）。

    /**
     * 原子推进会话代际号，并返回新代际。
     *
     * <p>ZS-LOGIN-002 P2-B（codex r0 修复）：参数改为毫秒精度 TTL。
     * 对正的亚秒剩余时间仍设有限 TTL（毫秒级），对已过期（<= 0）跳过推进，
     * 避免创建永不过期的代际键（原实现 ttlSeconds==0 时不设 EXPIRE → 永久键泄漏）。
     *
     * @param refreshToken 刷新令牌（会话标识；沿用不轮换，故可作会话主键）
     * @param ttlMillis    代际键的存活毫秒数，一般取刷新令牌剩余有效期；<= 0 时跳过推进（已过期）
     * @return 新代际号（从 1 开始单调递增）；若跳过则返回 0
     */
    public long nextSessionGeneration(String refreshToken, long ttlMillis) {
        if (ttlMillis <= 0) {
            // ZS-LOGIN-002 P2-B：已过期或无剩余时间，不推进代际（避免创建永久键）
            return 0L;
        }
        String redisKey = formatSessionGenerationKey(refreshToken);
        Long generation = stringRedisTemplate.opsForValue().increment(redisKey);
        if (generation != null) {
            stringRedisTemplate.expire(redisKey, ttlMillis, TimeUnit.MILLISECONDS);
        }
        return generation == null ? 0L : generation;
    }

    /**
     * 获得会话当前代际号；不存在时返回 {@code null}（例如会话已退出、或代际键已过期）。
     */
    public Long getSessionGeneration(String refreshToken) {
        return parseGeneration(stringRedisTemplate.opsForValue().get(formatSessionGenerationKey(refreshToken)));
    }

    /**
     * 登记访问令牌所属的会话代际，作为「可定位的会话代际」审计标识。
     *
     * <p>ZS-LOGIN-002 P2-B（codex r0 修复）：参数改为毫秒精度 TTL。
     * 对正的亚秒剩余时间使用毫秒 TTL 保留（而非误当"无限期"写永久键），
     * 对已过期条目（<= 0）跳过写入。access-generation 键在登出时刻意不删，
     * 故必须确保每个键都有有限 TTL，否则会永久累积。
     *
     * @param accessToken 访问令牌
     * @param generation  所属代际号
     * @param ttlMillis   存活毫秒数，一般取访问令牌剩余有效期；<= 0 时跳过写入（已过期）
     */
    public void setAccessTokenGeneration(String accessToken, long generation, long ttlMillis) {
        if (ttlMillis <= 0) {
            // ZS-LOGIN-002 P2-B：已过期，跳过写入，避免创建永不过期的 access-generation 键
            return;
        }
        String redisKey = formatAccessGenerationKey(accessToken);
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(generation), ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 反查访问令牌所属的会话代际；不存在时返回 {@code null}。
     * <p>注意：旧代际访问令牌被刷新取代后，本标识<b>仍保留至自然过期</b>，以便审计定位「某令牌属于第几代会话」。
     */
    public Long getAccessTokenGeneration(String accessToken) {
        return parseGeneration(stringRedisTemplate.opsForValue().get(formatAccessGenerationKey(accessToken)));
    }

    /**
     * 清理会话代际键（会话退出时调用）。访问令牌侧的代际标识保留至自然过期，供审计定位。
     */
    public void deleteSessionGeneration(String refreshToken) {
        stringRedisTemplate.delete(formatSessionGenerationKey(refreshToken));
    }

    private static Long parseGeneration(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatKey(String accessToken) {
        return String.format(OAUTH2_ACCESS_TOKEN, accessToken);
    }

    private static String formatSessionGenerationKey(String refreshToken) {
        return String.format(OAUTH2_REFRESH_SESSION_GENERATION, refreshToken);
    }

    private static String formatAccessGenerationKey(String accessToken) {
        return String.format(OAUTH2_ACCESS_SESSION_GENERATION, accessToken);
    }

}
