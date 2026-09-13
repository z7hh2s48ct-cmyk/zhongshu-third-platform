package cn.zszj.module.system.dal.redis.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.IpQuotaReservation;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_SEND_IP_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-004 P2（codex r2/r3/r4，桶键格式变更回归）：在活跃的小时/天窗口内部署时，
 * 旧<b>未标记</b>桶键（无 {@code {...}} hash tag）的计数必须惰性播种进新 hash-tag 桶键，
 * 保留活跃桶计数与剩余过期时间；<b>同时旧未标记桶必须保留</b>，随其自身 TTL 自然过期，
 * 使滚动部署期仍在写旧键的遗留实例不会因每次新版本请求都被删除而反复重置配额（可重复绕过）。
 *
 * <p>缺陷（r2 原始）：r1 为消除 CROSSSLOT 把每 IP 配额桶键从 {@code sms_code_send_ip_count:<ip>:<h|d>:<bucket>}
 * 改为 {@code sms_code_send_ip_count:{<ip>}:<h|d>:<bucket>}。在活跃窗口内部署时，键格式变更抛弃了旧未标记键下的
 * 所有计数——已耗尽配额的 IP 立即获得满额；混合版本实例在滚动部署期各自独立计额。
 *
 * <p>修复（r3）：{@code reserveIpSendQuota} 执行两键预留脚本前，对小时/天桶各做一次「旧→新」惰性迁移
 * （旧键存在才播种：seed-if-absent 保幂等 + {@code PTTL/PEXPIRE} 保留剩余过期）。
 *
 * <p>修复（r4，codex r3 揪出）：
 * <ul>
 *   <li>P1：{@code migrateLegacyIpBucket} 末尾不再无条件 {@code DEL} 旧未标记桶——保留旧桶随其自身 TTL 自然过期，
 *       杜绝「新版本请求（即便被拒）每次都重置遗留实例配额」的可重复绕过；</li>
 *   <li>defensive：旧桶剩余 {@code PTTL<=0}（无 TTL / 已过期边界）时视为已过期→不播种，交由后续
 *       {@code SCRIPT_RESERVE_IP_QUOTA} 首次 {@code INCR} 时以完整桶 TTL 新建，杜绝播种出无 TTL 的永久新桶。</li>
 * </ul>
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）播种旧格式桶验证迁移语义。
 *
 * @author ZS-LOGIN-004 P2(r3/r4) 惰性播种 + 滚动部署保留旧桶
 */
@Import(SmsCodeSecurityRedisDAO.class)
public class SmsCodeSecurityRedisDaoIpQuotaLegacyMigrationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 与 DAO 内部一致的当前小时/天桶（yyyyMMddHH / yyyyMMdd） */
    private static String currentHourBucket() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
    }

    private static String currentDayBucket() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /** r1 之前的旧未标记键格式（IP 不带 {...} hash tag） */
    private static String legacyKey(String ip, String bucketType, String bucket) {
        return String.format(SMS_CODE_SEND_IP_COUNT, ip, bucketType, bucket);
    }

    /** r1 之后的新 hash-tag 键格式 */
    private static String newKey(String ip, String bucketType, String bucket) {
        return String.format(SMS_CODE_SEND_IP_COUNT, "{" + ip + "}", bucketType, bucket);
    }

    @Test
    public void reserveWithExhaustedLegacyHourBucket_isRejected_andMigratesCountAndTtl() {
        String ip = "203.0.113.101";
        int hourLimit = 5;
        String legacyHour = legacyKey(ip, "h", currentHourBucket());
        String newHour = newKey(ip, "h", currentHourBucket());
        // 播种旧未标记小时桶至上限，剩余 TTL 30 分钟（模拟活跃窗口内的滚动部署）
        stringRedisTemplate.opsForValue().set(legacyHour, String.valueOf(hourLimit));
        stringRedisTemplate.expire(legacyHour, Duration.ofMinutes(30));

        // RED：只读新 hash-tag 键（空）→ 放行且 hourCount=1；GREEN：迁移后 INCR 到 limit+1 → 拒绝
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, hourLimit, 100);

        assertTrue(reservation.isRejected(),
                "旧未标记桶已耗尽配额，迁移后必须继续拒绝（不得因键格式变更重新获得满额）");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyHour)),
                "滚动部署期旧未标记桶必须保留，随其自身 TTL 自然过期，不得删除（删除会反复重置遗留实例配额）");
        assertEquals(String.valueOf(hourLimit), stringRedisTemplate.opsForValue().get(newHour),
                "被拒请求不得占用配额：新桶计数应保持迁移播种的旧值");
        Long newPttl = stringRedisTemplate.getExpire(newHour, TimeUnit.MILLISECONDS);
        assertTrue(newPttl != null && newPttl > 0 && newPttl <= Duration.ofMinutes(30).toMillis(),
                "迁移必须保留旧桶的剩余过期时间，实际=" + newPttl + "ms");
    }

    @Test
    public void reserveWithExhaustedLegacyDayBucket_isRejected_andMigratesCount() {
        String ip = "203.0.113.102";
        int dayLimit = 8;
        String legacyDay = legacyKey(ip, "d", currentDayBucket());
        String newDay = newKey(ip, "d", currentDayBucket());
        stringRedisTemplate.opsForValue().set(legacyDay, String.valueOf(dayLimit));
        stringRedisTemplate.expire(legacyDay, Duration.ofHours(6));

        // RED：天桶旧计数被抛弃 → 放行；GREEN：迁移后天桶 INCR 到 limit+1 → 拒绝并回滚
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 5, dayLimit);

        assertTrue(reservation.isRejected(),
                "旧未标记天桶已耗尽配额，迁移后必须继续拒绝");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyDay)),
                "滚动部署期旧未标记桶必须保留，随其自身 TTL 自然过期，不得删除（删除会反复重置遗留实例配额）");
        assertEquals(String.valueOf(dayLimit), stringRedisTemplate.opsForValue().get(newDay),
                "被拒请求不得占用配额：天桶计数应保持迁移播种的旧值");
    }

    @Test
    public void reserveWithPartialLegacyBuckets_preservesCountsAndTtls_andContinuesCounting() {
        String ip = "203.0.113.103";
        String legacyHour = legacyKey(ip, "h", currentHourBucket());
        String legacyDay = legacyKey(ip, "d", currentDayBucket());
        String newHour = newKey(ip, "h", currentHourBucket());
        String newDay = newKey(ip, "d", currentDayBucket());
        stringRedisTemplate.opsForValue().set(legacyHour, "2");
        stringRedisTemplate.expire(legacyHour, Duration.ofMinutes(30));
        stringRedisTemplate.opsForValue().set(legacyDay, "7");
        stringRedisTemplate.expire(legacyDay, Duration.ofHours(12));

        // GREEN：迁移后在保留计数上继续预留 → hourCount = 2+1 = 3
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 5, 100);

        assertTrue(reservation.isReserved());
        assertEquals(3L, reservation.getHourCount(), "预留计数必须建立在保留的旧小时桶计数之上（2+1）");
        assertEquals("3", stringRedisTemplate.opsForValue().get(newHour));
        assertEquals("8", stringRedisTemplate.opsForValue().get(newDay),
                "天桶计数必须建立在保留的旧计数之上（7+1）");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyHour)),
                "滚动部署期旧未标记桶必须保留，随其自身 TTL 自然过期，不得删除（删除会反复重置遗留实例配额）");
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyDay)),
                "滚动部署期旧未标记桶必须保留，随其自身 TTL 自然过期，不得删除（删除会反复重置遗留实例配额）");
        Long hourPttl = stringRedisTemplate.getExpire(newHour, TimeUnit.MILLISECONDS);
        Long dayPttl = stringRedisTemplate.getExpire(newDay, TimeUnit.MILLISECONDS);
        assertTrue(hourPttl != null && hourPttl > 0 && hourPttl <= Duration.ofMinutes(30).toMillis(),
                "小时桶剩余 TTL 必须被保留，实际=" + hourPttl + "ms");
        assertTrue(dayPttl != null && dayPttl > 0 && dayPttl <= Duration.ofHours(12).toMillis(),
                "天桶剩余 TTL 必须被保留，实际=" + dayPttl + "ms");
    }

    @Test
    public void reserveWithoutLegacyKeys_isNoop_andWorksNormally() {
        String ip = "203.0.113.104";

        // 常态（无旧格式数据）：迁移必须 no-op，不得引入行为变化
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 5, 100);

        assertTrue(reservation.isReserved());
        assertEquals(1L, reservation.getHourCount());
        assertEquals(1L, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));
        assertEquals(1L, smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip));
        // 释放语义不受影响
        smsCodeSecurityRedisDAO.releaseIpSendQuota(reservation);
        assertEquals(0L, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));
        assertEquals(0L, smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip));
    }

    /**
     * ZS-LOGIN-004 r4(P1)：滚动部署期旧未标记桶必须保留——不得因新版本请求（即便被拒）反复重置遗留实例配额。
     *
     * <p>缺陷（r3）：{@code migrateLegacyIpBucket} 末尾无条件 {@code stringRedisTemplate.delete(legacyKey)}——
     * 每次新版本请求都会删除旧桶。遗留实例随后对该键 {@code INCR} 会从 1 重来，
     * 使已耗尽配额的 IP 通过「新版本被拒请求 → delete → 遗留实例再 INCR」循环反复重置配额，可重复绕过。
     *
     * <p>修复（r4）：seed-only 保留旧桶随其自身 TTL 自然过期，不再 delete。
     *
     * <p>RED（对着 r3，即修复前）：{@code reserveIpSendQuota} 后旧桶被删；{@code increment(legacyHour)} 返回 1
     * （而非在旧值 5 基础上延续到 6）；且 {@code hasKey(legacyHour)} 为 false——两条断言均失败。
     */
    @Test
    public void reserveWithExhaustedLegacyBucket_preservesLegacyBucketForRollingDeploy_noRepeatableBypass() {
        String ip = "203.0.113.105";
        int hourLimit = 5;
        String legacyHour = legacyKey(ip, "h", currentHourBucket());
        // 播种旧未标记小时桶至上限，剩余 TTL 30 分钟（模拟活跃窗口内的滚动部署）
        stringRedisTemplate.opsForValue().set(legacyHour, String.valueOf(hourLimit));
        stringRedisTemplate.expire(legacyHour, Duration.ofMinutes(30));

        // 新版本请求：配额已耗尽 → 拒绝
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, hourLimit, 100);
        assertTrue(reservation.isRejected(),
                "旧未标记桶已耗尽配额，新版本请求必须被拒绝");

        // r4(P1)：旧未标记桶必须保留——遗留实例仍可继续在其上计数
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyHour)),
                "r4(P1)：滚动部署期旧未标记桶必须保留，不得因新版本请求（即便被拒）而删除；"
                        + "否则遗留实例的下一次 INCR 会从 1 重来，反复重置配额（可重复绕过）");

        // 模拟遗留实例继续 INCR 旧桶：必须在旧值 5 之上延续到 6（若被删则从 1 重来 → 绕过）
        Long continued = stringRedisTemplate.opsForValue().increment(legacyHour);
        assertEquals(6L, continued == null ? -1L : continued,
                "r4(P1)：遗留实例对旧未标记桶的下一次 INCR 必须在旧值之上延续（5→6），"
                        + "而非因新版本请求删除后从 1 重来——否则已耗尽配额的 IP 可通过"
                        + "「新版本被拒请求 → delete → 遗留 INCR」循环反复绕过配额");
    }

    /**
     * ZS-LOGIN-004 r4(defensive)：旧未标记桶无剩余 TTL（PTTL<=0，异常持久 / 过期边界）时视为已过期→不播种，
     * 交由后续 {@code SCRIPT_RESERVE_IP_QUOTA} 首次 INCR 时以完整桶 TTL 新建，杜绝播种出无 TTL 的永久新桶。
     *
     * <p>缺陷（r3）：{@code migrateLegacyIpBucket} 无条件 seed，且 {@code SCRIPT_SEED_IF_ABSENT} 仅在
     * {@code ARGV[2]>0} 时 PEXPIRE。若旧桶 PTTL=-1（无 TTL），seed 出的新桶也无 TTL——
     * 一旦新桶计数达上限则该 IP 被<b>永久</b>限流（自然过期通道被切断）。
     *
     * <p>RED（对着 r3，即修复前）：seed 出 PTTL=-1 的新桶 → {@code getExpire(newHour) <= 0} → 断言失败。
     */
    @Test
    public void reserveWithLegacyBucketWithoutTtl_doesNotCreatePermanentNewBucket() {
        String ip = "203.0.113.106";
        String legacyHour = legacyKey(ip, "h", currentHourBucket());
        String newHour = newKey(ip, "h", currentHourBucket());
        // 故意不设 expire，使旧桶 PTTL=-1（模拟异常持久键 / 过期边界）
        stringRedisTemplate.opsForValue().set(legacyHour, "3");

        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 5, 100);

        assertTrue(reservation.isReserved(),
                "旧桶无有效剩余寿命时视为已过期，新桶必须首次 INCR 新建并放行");
        Long newPttl = stringRedisTemplate.getExpire(newHour, TimeUnit.MILLISECONDS);
        assertTrue(newPttl != null && newPttl > 0,
                "r4(defensive)：新 hash-tag 桶必须有正 TTL（自然过期通道），"
                        + "绝不能因旧桶 PTTL<=0 而播种出永久桶，实际=" + newPttl + "ms");
    }
}
