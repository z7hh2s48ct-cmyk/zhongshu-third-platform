package cn.zszj.module.system.dal.redis.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P1（codex r1）：每 IP 发送配额的「小时桶键」与「天桶键」必须落在<b>同一个 Redis Cluster 槽</b>。
 *
 * <p>缺陷：{@code reserveIpSendQuota} / {@code releaseIpSendQuota} 的 Lua 脚本一次操作两个键
 * （{@code SMS_CODE_SEND_IP_COUNT} 的小时桶 + 天桶）。原键格式 {@code sms_code_send_ip_count:<ip>:<h|d>:<bucket>}
 * 不含 hash tag，Cluster 对<b>整键</b>算槽 → 小时桶与天桶哈希到不同槽 → 两键脚本抛 {@code CROSSSLOT}，
 * {@code sendSmsCode} 在建码/派发前直接失败（原单键实现是 Cluster 安全的，r0 的双键原子预留引入了此回归）。
 *
 * <p>修复：把 IP 包进 {@code {...}} hash tag（{@code sms_code_send_ip_count:{<ip>}:<h|d>:<bucket>}）——
 * Cluster 只对 {@code {...}} 内容算槽，同一 IP 的两桶键因此同槽，脚本恢复 Cluster 安全。
 *
 * <p>本地内嵌单节点 Redis 无法真复现 {@code CROSSSLOT}，故本类做<b>结构性</b>断言：
 * <ul>
 *     <li>真实 Redis 落键后扫描，断言小时/天两桶键的 hash tag 内容一致且等于 IP；</li>
 *     <li>用 CRC16-CCITT（Redis Cluster 槽算法）断言两键 {@code slot} 相同；</li>
 *     <li>用 Mock 模板捕获 {@code reserveIpSendQuota} 实际传给脚本的 KEYS，断言其同槽。</li>
 * </ul>
 *
 * <p>RED：原键无 {@code {...}} → hashTag 退回整键 → 两桶 tag 不等、slot 不等 → 断言失败。
 *
 * @author ZS-LOGIN-004 P1(r1)
 */
@Import(SmsCodeSecurityRedisDAO.class)
public class SmsCodeSecurityRedisDaoClusterSlotTest extends BaseDbAndRedisUnitTest {

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void ipQuotaHourAndDayKeys_shareSameClusterSlot() {
        // 准备：真实落一个小时桶键 + 一个天桶键（同一 IP）
        String ip = "203.0.113.77";
        smsCodeSecurityRedisDAO.increaseIpSendCountPerHour(ip);
        smsCodeSecurityRedisDAO.increaseIpSendCountPerDay(ip);

        // 扫描实际落库的键
        Set<String> keys = stringRedisTemplate.keys("sms_code_send_ip_count:*");
        assertNotNull(keys);
        String hourKey = keys.stream().filter(k -> k.contains(ip) && k.contains(":h:")).findFirst().orElse(null);
        String dayKey = keys.stream().filter(k -> k.contains(ip) && k.contains(":d:")).findFirst().orElse(null);
        assertNotNull(hourKey, "应存在该 IP 的小时桶键，实际全部=" + keys);
        assertNotNull(dayKey, "应存在该 IP 的天桶键，实际全部=" + keys);

        // 断言：两键都带 per-IP hash tag，且 tag 内容一致（== IP）→ Cluster 只对 tag 算槽 → 同槽
        String hourTag = hashTag(hourKey);
        String dayTag = hashTag(dayKey);
        assertEquals(ip, hourTag, "小时桶键必须把 IP 包进 {...} hash tag，实际键=" + hourKey);
        assertEquals(hourTag, dayTag, "小时桶与天桶必须共享同一 per-IP hash tag，hourKey=" + hourKey + ", dayKey=" + dayKey);
        assertEquals(crc16Slot(hourKey), crc16Slot(dayKey),
                "两桶键必须哈希到同一 Redis Cluster 槽，否则两键脚本抛 CROSSSLOT；hourKey=" + hourKey + ", dayKey=" + dayKey);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void reserveIpQuotaScript_passesTwoKeysInSameSlot() {
        // 准备：Mock 模板，捕获 reserveIpSendQuota 传给 Lua 脚本的 KEYS
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        List<List<String>> captured = new ArrayList<>();
        // reserve IP 配额脚本固定传 4 个 ARGV（小时上限/天上限/小时TTL/天TTL），故用 4 个 vararg 匹配器精确拦截
        when(template.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenAnswer(invocation -> {
            captured.add((List<String>) invocation.getArgument(1));
            return 1L;
        });
        SmsCodeSecurityRedisDAO dao = new SmsCodeSecurityRedisDAO();
        ReflectionTestUtils.setField(dao, "stringRedisTemplate", template);

        // 调用（忽略返回值类型，仅关注传入脚本的键）
        dao.reserveIpSendQuota("203.0.113.88", 10, 100);

        // 断言：脚本恰收到两个键（小时桶 + 天桶），且二者同槽
        assertEquals(1, captured.size(), "reserveIpSendQuota 应恰好执行一次两键脚本");
        List<String> keys = captured.get(0);
        assertEquals(2, keys.size(), "配额预留脚本必须操作小时桶 + 天桶两个键");
        assertEquals(hashTag(keys.get(0)), hashTag(keys.get(1)),
                "两键必须共享 hash tag，keys=" + keys);
        assertEquals(crc16Slot(keys.get(0)), crc16Slot(keys.get(1)),
                "两键必须落在同一 Cluster 槽以避免 CROSSSLOT，keys=" + keys);
    }

    // ========== Redis Cluster 槽计算工具 ==========

    /**
     * Redis Cluster hash tag：首个 {@code '{'} 与其后首个 {@code '}'} 之间的内容；无有效 tag（或空 tag）则退回整键。
     */
    static String hashTag(String key) {
        int start = key.indexOf('{');
        if (start >= 0) {
            int end = key.indexOf('}', start + 1);
            if (end > start + 1) {
                return key.substring(start + 1, end);
            }
        }
        return key;
    }

    static int crc16Slot(String key) {
        return crc16(hashTag(key).getBytes(StandardCharsets.UTF_8)) % 16384;
    }

    /**
     * CRC16-CCITT（XMODEM，poly 0x1021，init 0）——Redis Cluster 计算槽号所用的校验算法。
     */
    static int crc16(byte[] bytes) {
        int crc = 0;
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return crc;
    }
}
