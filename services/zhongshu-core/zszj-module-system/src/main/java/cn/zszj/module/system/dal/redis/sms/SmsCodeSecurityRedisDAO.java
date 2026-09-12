package cn.zszj.module.system.dal.redis.sms;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_SEND_IP_COUNT;
import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_VALIDATE_ATTEMPTS;

/**
 * ZS-LOGIN-004：短信验证码「安全计数器」的 RedisDAO
 * <p>
 * 承载两类与登录安全强相关、但<b>不属于业务数据</b>的计数状态：
 * <ol>
 *     <li>验证码校验失败次数（按 手机号 + 场景），用于暴力破解防护；</li>
 *     <li>每 IP 短信发送次数（按 小时 / 天 滚动桶），用于封堵「同一 IP 轮换手机号」的短信喷洒。</li>
 * </ol>
 * <p>
 * 选型说明：刻意存 Redis 而<b>不新增 DB 列</b>，避免 Flyway 迁移与 ZS-DB-019.B 的 PG 回归耦合；
 * 两类计数都需要 TTL 自然过期（锁定期到期自动解锁、时间桶自然归零），Redis 原生支持而 DB 需额外清理任务。
 * <p>
 * 容错策略（fail-closed）：本类<b>不吞 Redis 异常</b>。理由是本底座的访问令牌本身即以 Redis 为一级缓存，
 * Redis 不可用时登录链路已整体不可用，此时「放行」等于把暴力破解防护与频控同时清零，
 * 故宁可让验证码校验随 Redis 一起失败，也不静默降级为「无限制」。
 *
 * @author ZS-LOGIN-004
 */
@Repository
public class SmsCodeSecurityRedisDAO {

    /**
     * 每 IP 发送计数的小时桶格式
     */
    private static final DateTimeFormatter HOUR_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHH");
    /**
     * 每 IP 发送计数的天桶格式
     */
    private static final DateTimeFormatter DAY_BUCKET = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 时间桶类型：小时
     */
    private static final String BUCKET_TYPE_HOUR = "h";
    /**
     * 时间桶类型：天
     */
    private static final String BUCKET_TYPE_DAY = "d";

    /**
     * 小时桶的 TTL，略大于 1 小时以覆盖桶边界的读写抖动
     */
    private static final Duration HOUR_BUCKET_TTL = Duration.ofHours(2);
    /**
     * 天桶的 TTL，略大于 1 天
     */
    private static final Duration DAY_BUCKET_TTL = Duration.ofDays(2);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ========== 验证码校验失败次数（暴力破解防护） ==========

    /**
     * 累加一次「验证码不匹配」的失败尝试，并返回累加后的次数。
     * <p>
     * TTL <b>只在首次计数（返回值 == 1）时设定</b>：锁定期自「第一次错误尝试」起算并保持固定长度，
     * 攻击者无法通过持续试错把锁定窗口无限顺延，也无法靠等待把计数「刷回」再重新获得完整配额。
     *
     * @param mobile       手机号
     * @param scene        验证码场景
     * @param lockDuration 锁定时长（即计数键的 TTL）；为空或非正数时不设过期
     * @return 累加后的失败次数
     */
    public long increaseValidateAttempts(String mobile, Integer scene, Duration lockDuration) {
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        Long attempts = stringRedisTemplate.opsForValue().increment(redisKey);
        long current = attempts == null ? 0L : attempts;
        if (current == 1L && lockDuration != null && !lockDuration.isZero() && !lockDuration.isNegative()) {
            stringRedisTemplate.expire(redisKey, lockDuration.getSeconds(), TimeUnit.SECONDS);
        }
        return current;
    }

    /**
     * 获得当前失败尝试次数；不存在时返回 0。
     */
    public long getValidateAttempts(String mobile, Integer scene) {
        return parseCount(stringRedisTemplate.opsForValue().get(formatValidateAttemptsKey(mobile, scene)));
    }

    /**
     * 清零失败尝试次数。<b>仅允许在「验证码被成功消费」时调用</b>，
     * 即只有合法用户走通一次完整校验才能解锁；不对外暴露任何「按时间/按请求」的重置入口。
     */
    public void resetValidateAttempts(String mobile, Integer scene) {
        stringRedisTemplate.delete(formatValidateAttemptsKey(mobile, scene));
    }

    /**
     * 获得失败尝试计数键的剩余锁定时长；键不存在或无 TTL 时返回 {@link Duration#ZERO}。
     * <p>暴露此方法是为了让测试可以断言「锁定确实会自动到期」，而非依赖人工介入。
     */
    public Duration getValidateAttemptsTtl(String mobile, Integer scene) {
        Long seconds = stringRedisTemplate.getExpire(formatValidateAttemptsKey(mobile, scene), TimeUnit.SECONDS);
        return seconds == null || seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
    }

    // ========== 每 IP 短信发送频控 ==========

    /**
     * 获得该 IP 在当前小时桶内已发送的数量。
     */
    public long getIpSendCountPerHour(String ip) {
        return parseCount(stringRedisTemplate.opsForValue()
                .get(formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, currentHourBucket())));
    }

    /**
     * 累加该 IP 当前小时桶的发送计数，并返回累加后的值。
     */
    public long increaseIpSendCountPerHour(String ip) {
        return increaseIpSendCount(ip, BUCKET_TYPE_HOUR, currentHourBucket(), HOUR_BUCKET_TTL);
    }

    /**
     * 获得该 IP 在当前天桶内已发送的数量。
     */
    public long getIpSendCountPerDay(String ip) {
        return parseCount(stringRedisTemplate.opsForValue()
                .get(formatIpSendCountKey(ip, BUCKET_TYPE_DAY, currentDayBucket())));
    }

    /**
     * 累加该 IP 当前天桶的发送计数，并返回累加后的值。
     */
    public long increaseIpSendCountPerDay(String ip) {
        return increaseIpSendCount(ip, BUCKET_TYPE_DAY, currentDayBucket(), DAY_BUCKET_TTL);
    }

    private long increaseIpSendCount(String ip, String bucketType, String bucket, Duration ttl) {
        String redisKey = formatIpSendCountKey(ip, bucketType, bucket);
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        long current = count == null ? 0L : count;
        if (current == 1L) {
            stringRedisTemplate.expire(redisKey, ttl.getSeconds(), TimeUnit.SECONDS);
        }
        return current;
    }

    /**
     * 拼装失败尝试计数键。公开给测试用于直接断言键存在性与 TTL。
     */
    public static String formatValidateAttemptsKey(String mobile, Integer scene) {
        return String.format(SMS_CODE_VALIDATE_ATTEMPTS, mobile, scene);
    }

    private static String formatIpSendCountKey(String ip, String bucketType, String bucket) {
        return String.format(SMS_CODE_SEND_IP_COUNT, ip, bucketType, bucket);
    }

    private static String currentHourBucket() {
        return LocalDateTime.now().format(HOUR_BUCKET);
    }

    private static String currentDayBucket() {
        return LocalDateTime.now().format(DAY_BUCKET);
    }

    private static long parseCount(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

}
