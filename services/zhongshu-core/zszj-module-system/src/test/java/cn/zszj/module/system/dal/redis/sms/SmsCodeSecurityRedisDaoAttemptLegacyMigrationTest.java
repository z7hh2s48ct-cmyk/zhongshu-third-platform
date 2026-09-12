package cn.zszj.module.system.dal.redis.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.ValidateAttemptReservation;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-004 P1（codex r2，格式迁移回归）：升级/滚动部署时既有<b>旧 String 计数器</b>必须惰性迁移为 Hash，
 * 保留计数与剩余 TTL，杜绝新实现 Hash 命令对旧键抛 {@code WRONGTYPE}。
 *
 * <p>缺陷：r1 把校验尝试计数从 String（{@code INCR}）改为 Hash（字段 {@code c} 计数 + {@code r:<token>} 预留标记）。
 * 部署到含既有计数器的环境时，未改名的键 {@code sms_code_validate_attempts:<mobile>:<scene>} 仍是旧实现写入的
 * <b>string</b>，新代码的 {@code HGET/HINCRBY/HDEL} 对其抛 {@code WRONGTYPE} → 连正确码校验都失败，
 * 直到旧计数器自然过期（默认最长约 10 分钟）。
 *
 * <p>修复：所有触及该键的 Lua 脚本前置「TYPE 分支」惰性迁移——旧 string 键存在时把值搬进 Hash 字段 {@code c}
 * 并以 {@code PTTL/PEXPIRE} 保留剩余 TTL，随后脚本照常作用于迁移后的 Hash。
 *
 * <p>RED（对着 r2，即修复前）：预存 string 计数器后任何 {@code getValidateAttempts/reserveValidateAttempt/
 * increaseValidateAttempts/releaseValidateAttempt} 都抛 {@code WRONGTYPE}（Spring 翻译为
 * {@link org.springframework.dao.InvalidDataAccessApiUsageException}）→ 本类各测试直接以异常失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）播种旧格式数据验证迁移语义。
 *
 * @author ZS-LOGIN-004 P1(r3) 惰性迁移
 */
@Import(SmsCodeSecurityRedisDAO.class)
public class SmsCodeSecurityRedisDaoAttemptLegacyMigrationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Duration LOCK = Duration.ofMinutes(10);
    private static final Duration LEGACY_TTL = Duration.ofMinutes(5);

    /**
     * 播种一个「旧实现写入的」string 计数器（r1 之前的格式），并附带剩余 TTL。
     */
    private String seedLegacyStringCounter(String mobile, Integer scene, String count) {
        String key = SmsCodeSecurityRedisDAO.formatValidateAttemptsKey(mobile, scene);
        stringRedisTemplate.opsForValue().set(key, count);
        stringRedisTemplate.expire(key, LEGACY_TTL);
        return key;
    }

    @Test
    public void getValidateAttempts_migratesLegacyStringCounter_preservingCountAndTtl() {
        String mobile = "15691000101";
        Integer scene = 1;
        String key = seedLegacyStringCounter(mobile, scene, "3");

        // RED：旧 string 键上执行 HGET → WRONGTYPE；GREEN：惰性迁移后返回保留的计数
        long attempts = smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene);

        assertEquals(3L, attempts, "迁移必须保留旧 string 计数器的计数值");
        assertEquals(DataType.HASH, stringRedisTemplate.type(key),
                "迁移后旧 string 键必须变为 Hash 类型");
        assertEquals("3", stringRedisTemplate.opsForHash().get(key, "c"),
                "旧计数必须搬进 Hash 的计数字段 c");
        Duration ttl = smsCodeSecurityRedisDAO.getValidateAttemptsTtl(mobile, scene);
        assertTrue(ttl.toMillis() > 0 && ttl.toMillis() <= LEGACY_TTL.toMillis(),
                "迁移必须保留剩余 TTL，实际=" + ttl.toMillis() + "ms");
    }

    @Test
    public void reserveValidateAttempt_migratesLegacyCounter_andContinuesCountingFromIt() {
        String mobile = "15691000102";
        Integer scene = 1;
        String key = seedLegacyStringCounter(mobile, scene, "3");

        // RED：HGET/HINCRBY 对旧 string 键抛 WRONGTYPE；GREEN：迁移后从 3 继续预留 → 4
        ValidateAttemptReservation reservation =
                smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);

        assertTrue(reservation.isReservedSlot(), "旧计数 3 < 上限 5，迁移后应能继续预留");
        assertEquals(4L, reservation.getCount(), "预留后的计数必须建立在保留的旧计数之上（3+1）");
        assertEquals(4L, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));
        Duration ttl = smsCodeSecurityRedisDAO.getValidateAttemptsTtl(mobile, scene);
        assertTrue(ttl.toMillis() > 0 && ttl.toMillis() <= LEGACY_TTL.toMillis(),
                "迁移必须保留旧计数器的剩余 TTL，实际=" + ttl.toMillis() + "ms");
        assertEquals("1", stringRedisTemplate.opsForHash().get(key, "r:" + reservation.getToken()),
                "预留标记必须与迁移后的计数同处一个 Hash");
    }

    @Test
    public void reserveValidateAttempt_legacyCounterAtLimit_rejectsWithoutTouchingCount() {
        String mobile = "15691000103";
        Integer scene = 1;
        String key = seedLegacyStringCounter(mobile, scene, "5");

        // RED：WRONGTYPE（连「已达上限的拒绝」都变成异常）；GREEN：迁移后按保留计数判上限 → 拒绝
        ValidateAttemptReservation reservation =
                smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);

        assertTrue(reservation.isRejected(), "旧计数已达上限，迁移后必须继续拒绝（锁定期延续）");
        assertEquals(5L, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "拒绝路径不得改动计数");
        assertEquals(DataType.HASH, stringRedisTemplate.type(key));
    }

    @Test
    public void increaseValidateAttempts_migratesLegacyCounter_andPreservesOriginalTtl() {
        String mobile = "15691000104";
        Integer scene = 2;
        seedLegacyStringCounter(mobile, scene, "2");

        // RED：HINCRBY 抛 WRONGTYPE；GREEN：迁移后自增 → 3，且 TTL 沿用旧计数器的剩余值（不因自增重置）
        long attempts = smsCodeSecurityRedisDAO.increaseValidateAttempts(mobile, scene, LOCK);

        assertEquals(3L, attempts, "自增必须建立在保留的旧计数之上（2+1）");
        Duration ttl = smsCodeSecurityRedisDAO.getValidateAttemptsTtl(mobile, scene);
        assertTrue(ttl.toMillis() > 0 && ttl.toMillis() <= LEGACY_TTL.toMillis(),
                "锁定期仍须自旧计数器的第一次错误尝试起算，不得被迁移/自增重置为完整锁定时长，实际=" + ttl.toMillis() + "ms");
    }

    @Test
    public void releaseValidateAttempt_onMigratedLegacyCounter_doesNotThrow() {
        String mobile = "15691000105";
        Integer scene = 1;
        String key = seedLegacyStringCounter(mobile, scene, "3");

        // 迁移后的计数器上正常预留一次，再释放：全程不得出现 WRONGTYPE
        ValidateAttemptReservation reservation =
                smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);
        assertEquals(4L, reservation.getCount());
        assertDoesNotThrow(() -> smsCodeSecurityRedisDAO.releaseValidateAttempt(reservation));
        assertEquals(3L, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "释放应把计数扣回迁移时保留的旧值");
        assertEquals(DataType.HASH, stringRedisTemplate.type(key));
    }
}
