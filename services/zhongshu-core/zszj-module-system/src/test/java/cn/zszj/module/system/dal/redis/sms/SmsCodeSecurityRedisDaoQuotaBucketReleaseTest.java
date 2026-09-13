package cn.zszj.module.system.dal.redis.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.IpQuotaReservation;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-004 P2（codex r1，桶边界）：释放每 IP 配额时必须减<b>预留时的确切桶键</b>，而非释放时重算的当前桶键。
 *
 * <p>缺陷：r0 的 {@code releaseIpSendQuota(ip)} 在释放时重算 {@code currentHourBucket()/currentDayBucket()}。
 * 若发送在小时边界前预留、边界后失败，释放会减<b>新小时</b>桶而非预留时的桶；午夜同理影响天桶。
 * 更糟的是对新桶（此时不存在）无条件 {@code DECR} 会造出<b>无 TTL 的负计数</b> → 变相放大配额、允许超发。
 *
 * <p>修复：{@code reserveIpSendQuota} 返回 {@link IpQuotaReservation}，携带本次预留的确切桶键；
 * {@code releaseIpSendQuota(reservation)} 只减这些键，且 Lua 内判「键存在且 &gt;0」才 DECR（减到 0 删键）。
 *
 * <p>RED（对着 r0）：释放已消失的预留桶会造出值为 {@code -1} 的键（{@code actual=-1}）→ 断言失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）验证桶键的增减与删除语义。
 *
 * @author ZS-LOGIN-004 P2(r1) 桶边界
 */
@Import(SmsCodeSecurityRedisDAO.class)
public class SmsCodeSecurityRedisDaoQuotaBucketReleaseTest extends BaseDbAndRedisUnitTest {

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void releaseDecrementsReservedBucketKeys_andClearsThemAtZero() {
        // 准备：预留一次配额（小时桶 + 天桶各 +1）
        String ip = "203.0.113.60";
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 10, 100);
        assertTrue(reservation.isReserved());
        assertEquals(1, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));
        assertEquals(1, smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip));

        // 调用：释放本次预留
        smsCodeSecurityRedisDAO.releaseIpSendQuota(reservation);

        // 断言：预留时的两个确切桶键都被减回 0 并删除，不残留 0 值键
        assertEquals(0, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));
        assertEquals(0, smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip));
        for (String key : reservation.getKeys()) {
            assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(key)),
                    "释放后预留桶键应被清除（减到 0 即删），实际仍存在: " + key);
        }
    }

    @Test
    public void releaseAfterReservedBucketGone_doesNotCreateNegativeOrNoTtlKey() {
        // 准备：预留一次配额，随后模拟「预留的小时桶已过期 / 已滚动到下一桶」——预留桶键消失
        String ip = "203.0.113.61";
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 10, 100);
        List<String> keys = reservation.getKeys();
        String reservedHourKey = keys.get(0);
        String reservedDayKey = keys.get(1);
        stringRedisTemplate.delete(reservedHourKey);

        // 调用：失败驱动的释放
        smsCodeSecurityRedisDAO.releaseIpSendQuota(reservation);

        // 断言：对已消失的预留桶键不得 DECR（否则造出无 TTL 的负计数 → 变相放大配额）
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(reservedHourKey)),
                "对已过期/不存在的预留桶键 DECR 会造出无 TTL 负计数，必须避免；键=" + reservedHourKey);
        // 断言：仍存在的天桶被正常回滚到 0 并删除
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(reservedDayKey)),
                "仍存在的天桶应被回滚到 0 并删除；键=" + reservedDayKey);
    }

    @Test
    public void releaseOnlyTouchesReservedKeys_notAnUnrelatedNewerBucket() {
        // 准备：预留一次配额；另造一个「更新的桶键」（模拟释放时若重算当前桶会命中的键），预置计数 5
        String ip = "203.0.113.62";
        IpQuotaReservation reservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(ip, 10, 100);
        String reservedHourKey = reservation.getKeys().get(0);
        String newerBucketKey = "sms_code_send_ip_count:{" + ip + "}:h:9999999999";
        stringRedisTemplate.opsForValue().set(newerBucketKey, "5");

        // 调用
        smsCodeSecurityRedisDAO.releaseIpSendQuota(reservation);

        // 断言：只回滚了预留时的桶键，绝不触碰其它（更新的）桶键
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(reservedHourKey)),
                "预留桶键应被回滚清除");
        assertEquals("5", stringRedisTemplate.opsForValue().get(newerBucketKey),
                "释放只应作用于预留时的确切桶键，不得误减其它/更新的桶");
    }
}
