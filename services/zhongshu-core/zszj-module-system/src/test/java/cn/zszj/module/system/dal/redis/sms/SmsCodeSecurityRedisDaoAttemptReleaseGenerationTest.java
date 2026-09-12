package cn.zszj.module.system.dal.redis.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.ValidateAttemptReservation;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-LOGIN-004 P2（codex r1，代际绑定）：把「校验尝试名额的释放」与「预留时的代际」绑定，
 * 使 stale 请求无法从<b>重建后</b>的计数器扣减。
 *
 * <p>缺陷：r0 的 {@code releaseValidateAttempt(mobile, scene)} 无条件 {@code DECR} 计数器。同一 mobile+scene
 * 两合法请求在途时，一个消费码并 {@code resetValidateAttempts}（删计数器）可能早于另一个释放预留；若期间一次
 * 新错误猜测让<b>重建后</b>计数器自增，旧请求的无条件 {@code DECR} 会误删这次新失败、甚至彻底删计数器；过期后重建同理。
 *
 * <p>修复：计数状态改用单个 Redis Hash（字段 {@code c}=计数、{@code r:<token>}=预留标记）。{@code reserveValidateAttempt}
 * 生成唯一 token 并写入 Hash，返回的 {@link ValidateAttemptReservation} 携带该 token；{@code releaseValidateAttempt}
 * 仅当 {@code HDEL r:<token>} <b>命中</b>（本次预留代际仍有效）才 {@code HINCRBY c -1}；未命中（已被 reset/TTL 清除）
 * 则静默跳过。因标记与计数同处单键 Hash，脚本仍为单键 → Redis Cluster 安全。
 *
 * <p>RED（对着 r0）：reset 重建后计数器为 1，stale 释放无条件 DECR 使其变 0 → 断言 {@code expected <1> but was <0>} 失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）验证代际绑定语义。
 *
 * @author ZS-LOGIN-004 P2(r1) 代际绑定
 */
@Import(SmsCodeSecurityRedisDAO.class)
public class SmsCodeSecurityRedisDaoAttemptReleaseGenerationTest extends BaseDbAndRedisUnitTest {

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    private static final Duration LOCK = Duration.ofMinutes(10);

    @Test
    public void staleReleaseAfterReset_doesNotDecrementRebuiltCounter() {
        String mobile = "15690000401";
        Integer scene = 1;

        // 请求 A 预留一个名额（计数器 c=1，并写入 token A）
        ValidateAttemptReservation reservationA =
                smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);
        assertTrue(reservationA.isReservedSlot());
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));

        // 另一在途请求消费成功 → reset 删除整个计数 Hash（token A 一并清除）
        smsCodeSecurityRedisDAO.resetValidateAttempts(mobile, scene);
        assertEquals(0, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));

        // reset 之后一次新的错误猜测重建计数器 c=1（新一代际）
        smsCodeSecurityRedisDAO.increaseValidateAttempts(mobile, scene, LOCK);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));

        // 请求 A 的 stale 释放：token 已随 reset 清除 → HDEL 未命中 → 不得扣减重建后的新计数器
        smsCodeSecurityRedisDAO.releaseValidateAttempt(reservationA);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "stale 释放不得扣减重建后的计数器");
    }

    @Test
    public void twoInFlightReservations_resetThenStaleReleases_doNotTouchRebuiltCounter() {
        String mobile = "15690000402";
        Integer scene = 2;

        // 两个合法请求在途，各预留一个名额（c=2，两个 token）
        ValidateAttemptReservation a = smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);
        ValidateAttemptReservation b = smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);
        assertTrue(a.isReservedSlot());
        assertTrue(b.isReservedSlot());
        assertEquals(2, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));

        // 其一消费码 → reset 清空计数器（两个 token 一并失效）
        smsCodeSecurityRedisDAO.resetValidateAttempts(mobile, scene);

        // reset 之后一次新错误猜测重建计数器 c=1
        smsCodeSecurityRedisDAO.increaseValidateAttempts(mobile, scene, LOCK);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));

        // 两个 stale 释放均不得扣减新计数器
        smsCodeSecurityRedisDAO.releaseValidateAttempt(a);
        smsCodeSecurityRedisDAO.releaseValidateAttempt(b);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "两个 stale 释放均不得扣减重建后的新计数器");
    }

    @Test
    public void validReservationRelease_decrementsOwnCounter_andIsIdempotent() {
        String mobile = "15690000403";
        Integer scene = 3;

        // 有效代际的释放：token 命中 → 扣减，减到 0 删除键
        ValidateAttemptReservation r = smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene, 5, LOCK);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene));
        smsCodeSecurityRedisDAO.releaseValidateAttempt(r);
        assertEquals(0, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "有效预留的释放应扣减自己的计数器");

        // 幂等：重复释放同一 token 不再扣减（token 已被 HDEL）
        smsCodeSecurityRedisDAO.increaseValidateAttempts(mobile, scene, LOCK);
        smsCodeSecurityRedisDAO.releaseValidateAttempt(r);
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene),
                "重复释放同一预留不得二次扣减");
    }
}
