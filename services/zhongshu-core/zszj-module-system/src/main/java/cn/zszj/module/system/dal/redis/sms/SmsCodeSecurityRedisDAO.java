package cn.zszj.module.system.dal.redis.sms;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
 * <p>
 * ZS-LOGIN-004 codex r0 修复（P2-C/P2-D/P2-E）：所有「检查 + 计数 + 设过期」的复合动作一律合并为
 * <b>单个 Lua 脚本原子执行</b>（{@link DefaultRedisScript}），杜绝以下三类竞态：
 * <ul>
 *     <li>P2-E：{@code INCR} 与 {@code EXPIRE} 分离——首个 INCR 成功但进程/通信在 EXPIRE 前中断 → 计数键永无 TTL，
 *         一旦达上限连正确码都被无限期拒绝；</li>
 *     <li>P2-C：校验尝试「读计数-判上限」与「失败自增」分离——并发下多个猜测都读到低于上限而全部抵达 DB；</li>
 *     <li>P2-D：每 IP 发送配额「校验-建码-自增」分离——并发下多个请求都通过校验、全部建码发短信、超发配额。</li>
 * </ul>
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

    // ========== ZS-LOGIN-004：原子 Lua 脚本 ==========

    /**
     * P2-E：自增并在「首次创建」时原子设定过期。
     * <p>KEYS[1]=计数键，ARGV[1]=TTL 毫秒（<=0 表示不设过期）。返回自增后的值。
     * <p>合并 INCR + PEXPIRE 为单条脚本，消除「INCR 成功但 EXPIRE 未执行 → 永久键」的窗口。
     */
    private static final RedisScript<Long> SCRIPT_INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) "
          + "if n == 1 then "
          + "  local ttl = tonumber(ARGV[1]) "
          + "  if ttl > 0 then redis.call('PEXPIRE', KEYS[1], ttl) end "
          + "end "
          + "return n",
            Long.class);

    /**
     * P2-C：原子「判上限 + 预留一次校验容量」。
     * <p>KEYS[1]=失败计数键，ARGV[1]=上限，ARGV[2]=TTL 毫秒。
     * <p>已达上限 → 返回 {@code -1} 且<b>不改动计数</b>（锁定期间的调用不得改变计数，避免被探测）；
     * 否则原子自增（首次自增时设 TTL）并返回新值（>=1），表示「已预留一个校验名额」。
     */
    private static final RedisScript<Long> SCRIPT_RESERVE_VALIDATE_ATTEMPT = new DefaultRedisScript<>(
            "local maxAttempts = tonumber(ARGV[1]) "
          + "local ttl = tonumber(ARGV[2]) "
          + "local current = redis.call('GET', KEYS[1]) "
          + "if not current then current = 0 else current = tonumber(current) end "
          + "if current >= maxAttempts then return -1 end "
          + "local n = redis.call('INCR', KEYS[1]) "
          + "if n == 1 and ttl > 0 then redis.call('PEXPIRE', KEYS[1], ttl) end "
          + "return n",
            Long.class);

    /**
     * P2-C：释放一次已预留但未构成「失败」的校验容量（校验成功 / 验证码过期 / 已使用）。
     * <p>KEYS[1]=失败计数键。原子自减，减到 <=0 时删除键，避免残留 0 值键与错位 TTL。
     */
    private static final RedisScript<Long> SCRIPT_RELEASE_VALIDATE_ATTEMPT = new DefaultRedisScript<>(
            "local n = redis.call('DECR', KEYS[1]) "
          + "if n <= 0 then redis.call('DEL', KEYS[1]) end "
          + "return n",
            Long.class);

    /**
     * P2-D：原子「检查并预留」每 IP 的小时桶 + 天桶两个发送配额。
     * <p>KEYS[1]=小时桶键，KEYS[2]=天桶键；ARGV[1]=小时上限，ARGV[2]=天上限，ARGV[3]=小时 TTL 毫秒，ARGV[4]=天 TTL 毫秒。
     * <p>两桶各自原子自增（首次设 TTL）；任一桶超限则<b>回滚两桶自增</b>并返回 {@code -1}（拒绝），
     * 否则返回预留后的小时桶计数（>=1，放行）。上限 <=0 视为「该维度不限制」。
     * 整段脚本单线程原子执行，故并发下恰有 {@code limit} 个请求能预留成功。
     * <p>返回值刻意用 {@code long}（{@code -1}=拒绝）而非 {@code boolean}：与 {@link #reserveValidateAttempt} 契约一致，
     * 且 Mockito 对未打桩的 {@code long} 方法默认返回 {@code 0}（=放行），使「以 Mock 提供安全计数器、只关注验证码自身语义」
     * 的既有单测无需改动即保持「未触达任何上限」的原意。
     */
    private static final RedisScript<Long> SCRIPT_RESERVE_IP_QUOTA = new DefaultRedisScript<>(
            "local hourLimit = tonumber(ARGV[1]) "
          + "local dayLimit = tonumber(ARGV[2]) "
          + "local hourTtl = tonumber(ARGV[3]) "
          + "local dayTtl = tonumber(ARGV[4]) "
          + "local h = redis.call('INCR', KEYS[1]) "
          + "if h == 1 and hourTtl > 0 then redis.call('PEXPIRE', KEYS[1], hourTtl) end "
          + "local d = redis.call('INCR', KEYS[2]) "
          + "if d == 1 and dayTtl > 0 then redis.call('PEXPIRE', KEYS[2], dayTtl) end "
          + "if hourLimit > 0 and h > hourLimit then "
          + "  redis.call('DECR', KEYS[1]) redis.call('DECR', KEYS[2]) return -1 "
          + "end "
          + "if dayLimit > 0 and d > dayLimit then "
          + "  redis.call('DECR', KEYS[1]) redis.call('DECR', KEYS[2]) return -1 "
          + "end "
          + "return h",
            Long.class);

    /**
     * P2-D：释放此前预留的每 IP 配额（建码或派发失败时按需回滚，被拒的请求不占用配额）。
     * <p>KEYS[1]=小时桶键，KEYS[2]=天桶键。
     */
    private static final RedisScript<Long> SCRIPT_RELEASE_IP_QUOTA = new DefaultRedisScript<>(
            "redis.call('DECR', KEYS[1]) "
          + "redis.call('DECR', KEYS[2]) "
          + "return 1",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ========== 验证码校验失败次数（暴力破解防护） ==========

    /**
     * 累加一次「验证码不匹配」的失败尝试，并返回累加后的次数。
     * <p>
     * ZS-LOGIN-004 P2-E：自增与「首次创建时的过期设定」合并为<b>单个 Lua 脚本</b>原子执行，
     * 消除原实现「{@code INCR} 成功后、{@code EXPIRE} 之前进程停止/通信失败 → 计数键永无 TTL」的窗口。
     * TTL 仍<b>只在首次计数（返回值 == 1）时设定</b>：锁定期自「第一次错误尝试」起算并保持固定长度，
     * 攻击者无法通过持续试错把锁定窗口无限顺延。
     *
     * @param mobile       手机号
     * @param scene        验证码场景
     * @param lockDuration 锁定时长（即计数键的 TTL）；为空或非正数时不设过期
     * @return 累加后的失败次数
     */
    public long increaseValidateAttempts(String mobile, Integer scene, Duration lockDuration) {
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        long ttlMillis = (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative())
                ? 0L : lockDuration.toMillis();
        Long result = stringRedisTemplate.execute(SCRIPT_INCREMENT_WITH_EXPIRE,
                Collections.singletonList(redisKey), String.valueOf(ttlMillis));
        return result == null ? 0L : result;
    }

    /**
     * ZS-LOGIN-004 P2-C：原子「判上限 + 预留一次校验容量」。
     * <p>用于取代「先 {@link #getValidateAttempts} 读计数、判上限，再在失败后 {@link #increaseValidateAttempts} 自增」
     * 这一非原子的 check-then-act——并发下多个猜测都会读到低于上限而全部抵达 DB。
     *
     * @param mobile       手机号
     * @param scene        验证码场景
     * @param maxAttempts  失败尝试上限；<=0 表示不启用限制，直接返回 0（放行且不计数）
     * @param lockDuration 锁定时长（首次预留时设为计数键 TTL）
     * @return {@code -1} 表示已达上限（锁定，计数不变，调用方应直接拒绝）；
     *         {@code 0} 表示未启用限制（放行、不计数）；{@code >=1} 表示已成功预留一个名额（放行，返回预留后的计数）
     */
    public long reserveValidateAttempt(String mobile, Integer scene, int maxAttempts, Duration lockDuration) {
        if (maxAttempts <= 0) {
            return 0L;
        }
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        long ttlMillis = (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative())
                ? 0L : lockDuration.toMillis();
        Long result = stringRedisTemplate.execute(SCRIPT_RESERVE_VALIDATE_ATTEMPT,
                Collections.singletonList(redisKey), String.valueOf(maxAttempts), String.valueOf(ttlMillis));
        return result == null ? 0L : result;
    }

    /**
     * ZS-LOGIN-004 P2-C：释放一次「已预留但未构成失败」的校验容量。
     * <p>在校验成功、验证码过期、验证码已使用等<b>非攻击信号</b>的路径调用，把 {@link #reserveValidateAttempt}
     * 预留的名额退还，保证只有「验证码不匹配 / 错场景」才真正累加失败计数。
     */
    public void releaseValidateAttempt(String mobile, Integer scene) {
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        stringRedisTemplate.execute(SCRIPT_RELEASE_VALIDATE_ATTEMPT, Collections.singletonList(redisKey));
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
     * ZS-LOGIN-004 P2-D：在持久化与派发<b>之前</b>，原子地检查并预留每 IP 的小时桶 + 天桶两个发送配额。
     * <p>取代原「先 {@code validateIpSendLimit} 读两桶计数判上限、建码发短信后再各自 {@code INCR} 且不检查结果」
     * 的非原子流程——并发下多个请求都会通过校验、全部建码派发、超发配额。
     *
     * @param ip        客户端 IP（配额维度键）
     * @param hourLimit 每小时上限；<=0 表示该维度不限制
     * @param dayLimit  每天上限；<=0 表示该维度不限制
     * @return {@code < 0}（即 {@code -1}）表示任一配额超限（已回滚，拒绝）；
     *         {@code >= 0} 表示两个配额均已成功预留（放行，返回值为预留后的小时桶计数）
     */
    public long reserveIpSendQuota(String ip, int hourLimit, int dayLimit) {
        List<String> keys = Arrays.asList(
                formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, currentHourBucket()),
                formatIpSendCountKey(ip, BUCKET_TYPE_DAY, currentDayBucket()));
        Long result = stringRedisTemplate.execute(SCRIPT_RESERVE_IP_QUOTA, keys,
                String.valueOf(hourLimit), String.valueOf(dayLimit),
                String.valueOf(HOUR_BUCKET_TTL.toMillis()), String.valueOf(DAY_BUCKET_TTL.toMillis()));
        return result == null ? 0L : result;
    }

    /**
     * ZS-LOGIN-004 P2-D：释放此前预留的每 IP 配额（建码或派发失败时按需回滚，被拒的请求不占用配额）。
     */
    public void releaseIpSendQuota(String ip) {
        List<String> keys = Arrays.asList(
                formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, currentHourBucket()),
                formatIpSendCountKey(ip, BUCKET_TYPE_DAY, currentDayBucket()));
        stringRedisTemplate.execute(SCRIPT_RELEASE_IP_QUOTA, keys);
    }

    /**
     * 获得该 IP 在当前小时桶内已发送的数量。
     */
    public long getIpSendCountPerHour(String ip) {
        return parseCount(stringRedisTemplate.opsForValue()
                .get(formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, currentHourBucket())));
    }

    /**
     * 累加该 IP 当前小时桶的发送计数，并返回累加后的值。
     * <p>ZS-LOGIN-004 P2-E：自增与首次过期设定合并为单个 Lua 脚本原子执行。
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
     * <p>ZS-LOGIN-004 P2-E：自增与首次过期设定合并为单个 Lua 脚本原子执行。
     */
    public long increaseIpSendCountPerDay(String ip) {
        return increaseIpSendCount(ip, BUCKET_TYPE_DAY, currentDayBucket(), DAY_BUCKET_TTL);
    }

    private long increaseIpSendCount(String ip, String bucketType, String bucket, Duration ttl) {
        String redisKey = formatIpSendCountKey(ip, bucketType, bucket);
        Long result = stringRedisTemplate.execute(SCRIPT_INCREMENT_WITH_EXPIRE,
                Collections.singletonList(redisKey), String.valueOf(ttl.toMillis()));
        return result == null ? 0L : result;
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
