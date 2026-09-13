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
 * ZS-LOGIN-004 P1（codex r2/r3/r4，格式迁移回归）：升级/滚动部署时既有<b>旧 String 计数器</b>必须惰性迁移为 Hash，
 * 保留计数与剩余 TTL，杜绝新实现 Hash 命令对旧键抛 {@code WRONGTYPE}；
 * <b>迁移遇 PTTL<=0（过期边界 0 / 异常持久 -1）时必须视为已过期丢弃旧锁</b>，
 * 绝不能迁移出无 TTL 的永久 Hash（永久拒绝，含正确码）。
 *
 * <p>缺陷（r2 原始）：r1 把校验尝试计数从 String（{@code INCR}）改为 Hash（字段 {@code c} 计数 +
 * {@code r:<token>} 预留标记）。部署到含既有计数器的环境时，未改名的键
 * {@code sms_code_validate_attempts:<mobile>:<scene>} 仍是旧实现写入的 <b>string</b>，
 * 新代码的 {@code HGET/HINCRBY/HDEL} 对其抛 {@code WRONGTYPE} → 连正确码校验都失败，
 * 直到旧计数器自然过期（默认最长约 10 分钟）。
 *
 * <p>修复（r3）：所有触及该键的 Lua 脚本前置「TYPE 分支」惰性迁移——旧 string 键存在时把值搬进 Hash 字段
 * {@code c} 并以 {@code PTTL/PEXPIRE} 保留剩余 TTL，随后脚本照常作用于迁移后的 Hash。
 *
 * <p>修复（r4，codex r3 揪出 P2）：迁移片段按 {@code PTTL} 分支——
 * <ul>
 *   <li>{@code PTTL > 0}：搬迁旧值到 Hash 字段 {@code c}，PEXPIRE 保留剩余 TTL（同 r3 语义）；</li>
 *   <li>{@code PTTL <= 0}（过期边界 0 / 异常持久 -1）：<b>仅 DEL 丢弃旧锁</b>，视为已过期——
 *       后续脚本 {@code HGET} 得 nil → 计数按 0 起算，{@code HINCRBY} 首次以完整 lockDuration TTL 新建，
 *       绝不产生无 TTL 的永久 Hash。</li>
 * </ul>
 *
 * <p>RED（对着 r2，即修复前）：预存 string 计数器后任何 {@code getValidateAttempts/reserveValidateAttempt/
 * increaseValidateAttempts/releaseValidateAttempt} 都抛 {@code WRONGTYPE}（Spring 翻译为
 * {@link org.springframework.dao.InvalidDataAccessApiUsageException}）→ 本类各测试直接以异常失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）播种旧格式数据验证迁移语义。
 *
 * @author ZS-LOGIN-004 P1(r3/r4) 惰性迁移 + PTTL<=0 视为过期
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

    /**
     * ZS-LOGIN-004 r4(P2)：迁移发生在过期边界（Redis 可能返回 {@code PTTL == 0} 而旧键仍存在），
     * 或旧键因异常持久（{@code PTTL == -1}）——两种情况都必须视为「已过期」丢弃旧锁，
     * 绝不能迁移出无 TTL 的永久 Hash（永久拒绝，含正确码；且成功消费的 {@code resetValidateAttempts} 也无法运行）。
     *
     * <p>缺陷（r3）：迁移片段无脑 {@code DEL + HSET c=legacy}，仅当 {@code pttl>0} 才 PEXPIRE。
     * PTTL<=0 时旧锁被搬进 Hash 却<b>未设 TTL</b>——永久驻留；若其计数已达上限，
     * 之后每次校验都被无限期拒绝（含正确码），成功消费的 reset 也无法运行。
     *
     * <p>修复（r4）：迁移片段按 {@code PTTL} 分支——{@code PTTL>0} 才搬迁并保留剩余 TTL；
     * {@code PTTL<=0} 仅 {@code DEL} 丢弃旧锁，后续脚本 {@code HGET} 得 nil → 按 0 起算，
     * {@code HINCRBY} 首次以完整 lockDuration TTL 新建。
     *
     * <p>RED（对着 r3，即修复前）：DEL + HSET c="5" 无 PEXPIRE → 永久 Hash 计数=5 → reserve 返回 rejected
     * （{@code isReservedSlot()==false}）且 TTL 为 -1（{@link Duration#ZERO}）→ 两条断言均失败。
     */
    @Test
    public void reserveValidateAttempt_legacyCounterWithoutTtl_treatedAsExpired_noPermanentLock() {
        String mobile = "15691000106";
        Integer scene = 1;
        String key = SmsCodeSecurityRedisDAO.formatValidateAttemptsKey(mobile, scene);
        // 故意不设 expire，使旧 string 计数器 PTTL=-1（模拟异常持久键 / 过期边界遗留）；
        // 计数「5」恰达上限，若被误搬进无 TTL 的 Hash 就会造成永久锁死（含正确码）。
        stringRedisTemplate.opsForValue().set(key, "5");

        ValidateAttemptReservation reservation =
                smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);

        assertTrue(reservation.isReservedSlot(),
                "r4(P2)：旧锁剩余 PTTL<=0 必须视为已过期→丢弃→按 0 起算开新窗口，"
                        + "绝不能因搬运旧计数=5 而永久拒绝（含正确码）");
        Duration ttl = smsCodeSecurityRedisDAO.getValidateAttemptsTtl(mobile, scene);
        assertTrue(ttl.toMillis() > 0,
                "r4(P2)：迁移后计数 Hash 必须有正 TTL（自然过期通道），绝不能产生无 TTL 的永久锁，"
                        + "实际=" + ttl.toMillis() + "ms");
    }
}
