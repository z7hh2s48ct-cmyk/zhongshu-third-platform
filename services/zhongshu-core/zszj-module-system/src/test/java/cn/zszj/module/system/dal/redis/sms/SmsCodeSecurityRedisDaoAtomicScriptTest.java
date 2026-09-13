package cn.zszj.module.system.dal.redis.sms;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P2-E（codex r0）：失败尝试计数器的「创建/自增」与「初始过期」必须合并为<b>单个原子 Redis 操作</b>。
 *
 * <p>缺陷：原实现是 {@code INCR} 之后「若返回值 == 1 再 {@code EXPIRE}」两条独立命令。若首个 INCR 成功但进程停止
 * 或 Redis 通信在 EXPIRE 前失败，计数键将<b>永无 TTL</b>；后续自增只在 count==1 时设过期，永不修复它。一旦达上限，
 * 连正确码也被拒、成功消费无法清计数器 → 用户被无限期锁死。
 *
 * <p>本测试用裸 {@code new DAO() + mock(StringRedisTemplate)}（绕开 Spring 代理），断言修复后：
 * <ul>
 *     <li>{@code increaseValidateAttempts} 走<b>单次</b> {@code execute(RedisScript, keys, args)}（Lua 原子脚本）；</li>
 *     <li><b>不再</b>使用分离的 {@code opsForValue().increment(...)} + {@code expire(...)} 两条命令。</li>
 * </ul>
 *
 * <p>RED：原实现从不调用 {@code execute(script)}，而是 {@code increment} + {@code expire} → 两条 verify 均失败。
 *
 * @author ZS-LOGIN-004 P2-E
 */
public class SmsCodeSecurityRedisDaoAtomicScriptTest {

    @SuppressWarnings("unchecked")
    @Test
    public void increaseValidateAttempts_mergesIncrAndExpireIntoSingleAtomicScript() {
        // 准备：裸 DAO + mock 模板（同时桩住「原子脚本」与「分离命令」两条路径，使 RED/GREEN 都能运行）
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        SmsCodeSecurityRedisDAO dao = new SmsCodeSecurityRedisDAO();
        ReflectionTestUtils.setField(dao, "stringRedisTemplate", template);

        // 调用
        long attempts = dao.increaseValidateAttempts("15600000001", 1, Duration.ofMinutes(10));

        // 断言：返回值正确
        assertEquals(1L, attempts);
        // 断言（P2-E 核心）：自增 + 初始过期合并为「单次原子脚本执行」
        verify(template, times(1)).execute(any(RedisScript.class), anyList(), any());
        // 断言：不再使用会产生「INCR 成功但 EXPIRE 缺失」窗口的分离命令
        verify(valueOps, never()).increment(anyString());
        verify(template, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void increaseIpSendCount_mergesIncrAndExpireIntoSingleAtomicScript() {
        // 准备
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        SmsCodeSecurityRedisDAO dao = new SmsCodeSecurityRedisDAO();
        ReflectionTestUtils.setField(dao, "stringRedisTemplate", template);

        // 调用
        long count = dao.increaseIpSendCountPerHour("203.0.113.7");

        // 断言
        assertEquals(1L, count);
        verify(template, times(1)).execute(any(RedisScript.class), anyList(), any());
        verify(valueOps, never()).increment(anyString());
        verify(template, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
}
