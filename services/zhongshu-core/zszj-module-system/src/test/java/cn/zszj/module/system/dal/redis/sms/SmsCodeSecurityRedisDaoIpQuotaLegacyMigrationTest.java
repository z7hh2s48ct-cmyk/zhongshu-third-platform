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

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_SEND_IP_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-004 P2（codex r2，桶键格式变更回归）：在活跃的小时/天窗口内部署时，
 * 旧<b>未标记</b>桶键（无 {@code {...}} hash tag）的计数必须惰性播种进新 hash-tag 桶键，
 * 保留活跃桶计数与剩余过期时间。
 *
 * <p>缺陷：r1 为消除 CROSSSLOT 把每 IP 配额桶键从 {@code sms_code_send_ip_count:<ip>:<h|d>:<bucket>} 改为
 * {@code sms_code_send_ip_count:{<ip>}:<h|d>:<bucket>}。在活跃窗口内部署时，键格式变更抛弃了旧未标记键下的
 * 所有计数——已耗尽配额的 IP 立即获得满额；混合版本实例在滚动部署期各自独立计额。
 *
 * <p>修复：{@code reserveIpSendQuota} 执行两键预留脚本前，对小时/天桶各做一次「旧→新」惰性迁移
 * （旧键存在才播种：seed-if-absent 保幂等 + {@code PTTL/PEXPIRE} 保留剩余过期 + best-effort 删旧键）；
 * 无旧键时仅一次快速 {@code GET(null)}，常态 no-op。
 *
 * <p>RED（对着 r2，即修复前）：播种旧未标记桶到上限后，{@code reserveIpSendQuota} 只读新 hash-tag 键（空）
 * 而<b>放行</b>（{@code isReserved()==true}、hourCount=1）→ 耗尽配额未生效 → 断言失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）播种旧格式桶验证迁移语义。
 *
 * @author ZS-LOGIN-004 P2(r3) 惰性播种
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
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyHour)),
                "迁移后旧未标记桶键应被删除");
        assertEquals(String.valueOf(hourLimit), stringRedisTemplate.opsForValue().get(newHour),
                "被拒请求不得占用配额：新桶计数应保持迁移播种的旧值");
        Long newPttl = stringRedisTemplate.getExpire(newHour, java.util.concurrent.TimeUnit.MILLISECONDS);
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
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyDay)),
                "迁移后旧未标记天桶键应被删除");
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
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyHour)));
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(legacyDay)));
        Long hourPttl = stringRedisTemplate.getExpire(newHour, java.util.concurrent.TimeUnit.MILLISECONDS);
        Long dayPttl = stringRedisTemplate.getExpire(newDay, java.util.concurrent.TimeUnit.MILLISECONDS);
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
}
