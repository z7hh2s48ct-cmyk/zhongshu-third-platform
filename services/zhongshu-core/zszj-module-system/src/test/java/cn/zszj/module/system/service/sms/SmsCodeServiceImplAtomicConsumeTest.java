package cn.zszj.module.system.service.sms;

import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zszj.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.framework.sms.config.SmsCodeProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_USED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004：短信验证码「一次性消费」的并发原子性测试。
 * <p>
 * 缺口复现：{@code SmsCodeServiceImpl#useSmsCode} 原实现是典型的 <b>check-then-act</b>——
 * 先 {@code validateSmsCode0} 里 SELECT 出 {@code used = false}，再无条件 {@code updateById} 写回 {@code used = true}。
 * 两个动作之间没有任何互斥，N 个线程并发消费同一条验证码时全部都能通过 SELECT 校验、全部都会写回成功，
 * 「一次性」被击穿（一条验证码可被多个请求各自换取一次登录/改密，等同会话失控）。
 * <p>
 * 竞态对齐手法：本测试<b>不靠 sleep 碰运气</b>。{@code validateSmsCode0} 在 SELECT 之后、判定 {@code used} 之前
 * 会调用 {@code smsCodeProperties.getExpireTimes()}，故用 {@link CountDownLatch} 桩住该方法，
 * 把「已读到 used=false」与「即将写回 used=true」之间的窗口精确对齐到同一时刻，
 * RED / GREEN 两阶段都走同一条代码路径，因而复现是确定性的。
 * <p>
 * 因 {@code useSmsCode} 无 {@code @Transactional}，每次 Mapper 调用独立借还连接，栅栏等待期间不占用 DB 连接，
 * 8 线程不会耗尽 Druid 连接池。
 *
 * @author ZS-LOGIN-004
 */
@Import(SmsCodeServiceImpl.class)
public class SmsCodeServiceImplAtomicConsumeTest extends BaseDbUnitTest {

    private static final int THREADS = 8;

    private static final String MOBILE = "15601691300";

    private static final Integer SCENE = SmsSceneEnum.MEMBER_LOGIN.getScene();

    private static final String CODE = "1234";

    @Resource
    private SmsCodeServiceImpl smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockitoBean
    private SmsCodeProperties smsCodeProperties;
    @MockitoBean
    private SmsSendService smsSendService;
    // ZS-LOGIN-004：本类只验证「消费原子性」，安全计数器以 Mock 提供（计数恒为 0，即不触发任何上限）
    @MockitoBean
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @BeforeEach
    public void setUp() {
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(10);
        when(smsCodeProperties.getBeginCode()).thenReturn(1000);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
        when(smsCodeProperties.getMaxValidateAttempts()).thenReturn(SmsCodeProperties.DEFAULT_MAX_VALIDATE_ATTEMPTS);
        when(smsCodeProperties.getAttemptLockDuration()).thenReturn(SmsCodeProperties.DEFAULT_ATTEMPT_LOCK_DURATION);
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    @Test
    public void concurrentUseSameCode_onlyOneSucceeds() throws Exception {
        // mock 数据：一条尚未使用的验证码
        insertSmsCode(MOBILE, CODE, SCENE, "127.0.0.1");
        // 竞态栅栏：让 8 个线程都在「读到 used=false」之后同时冲向写回
        stubExpireTimesAsBarrier(THREADS);

        // 并发调用
        Throwable[] results = runConcurrently(THREADS,
                i -> smsCodeService.useSmsCode(newUseReqDTO(MOBILE, CODE, SCENE, "10.0.0." + i)));

        // 断言：有且仅有一个成功，其余全部按「验证码已使用」拒绝
        long success = Arrays.stream(results).filter(Objects::isNull).count();
        long usedRejected = Arrays.stream(results).filter(e -> isServiceException(e, SMS_CODE_USED)).count();
        assertEquals(1, success,
                "并发消费同一条验证码，成功数必须恰为 1；实际=" + success + "，全部结果=" + describe(results));
        assertEquals(THREADS - 1, usedRejected,
                "其余调用必须全部因「验证码已使用」被拒；实际=" + usedRejected + "，全部结果=" + describe(results));
        assertEquals(THREADS, success + usedRejected, "不允许出现其它类型的失败：" + describe(results));

        // 断言：DB 中该验证码只留下唯一胜出者的消费痕迹
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(MOBILE, CODE, SCENE);
        assertNotNull(smsCodeDO);
        assertTrue(smsCodeDO.getUsed());
        assertNotNull(smsCodeDO.getUsedTime());
        assertTrue(smsCodeDO.getUsedIp().startsWith("10.0.0."),
                "usedIp 应记录唯一胜出者的 IP，实际=" + smsCodeDO.getUsedIp());
    }

    @Test
    public void useSameCodeTwice_secondRejected() {
        // mock 数据
        insertSmsCode(MOBILE, CODE, SCENE, "127.0.0.1");

        // 第一次消费成功
        smsCodeService.useSmsCode(newUseReqDTO(MOBILE, CODE, SCENE, "10.0.0.1"));
        // 第二次必须被拒
        assertServiceException(() -> smsCodeService.useSmsCode(newUseReqDTO(MOBILE, CODE, SCENE, "10.0.0.2")),
                SMS_CODE_USED);

        // 断言：消费痕迹仍是第一次的，未被第二次覆盖
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(MOBILE, CODE, SCENE);
        assertTrue(smsCodeDO.getUsed());
        assertEquals("10.0.0.1", smsCodeDO.getUsedIp());
    }

    @Test
    public void concurrentUseDifferentCodes_allSucceed() throws Exception {
        // mock 数据：8 个互不相干的手机号，各一条验证码
        for (int i = 0; i < THREADS; i++) {
            insertSmsCode(mobileOf(i), codeOf(i), SCENE, "127.0.0.1");
        }
        stubExpireTimesAsBarrier(THREADS);

        // 并发调用
        Throwable[] results = runConcurrently(THREADS,
                i -> smsCodeService.useSmsCode(newUseReqDTO(mobileOf(i), codeOf(i), SCENE, "10.0.0." + i)));

        // 断言：互不干扰、全部成功。这是「防过度加锁误伤合法用户」的反向护栏
        long success = Arrays.stream(results).filter(Objects::isNull).count();
        assertEquals(THREADS, success, "不同手机号/验证码的并发消费不应互相阻塞：" + describe(results));
        for (int i = 0; i < THREADS; i++) {
            SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(mobileOf(i), codeOf(i), SCENE);
            assertNotNull(smsCodeDO);
            assertTrue(smsCodeDO.getUsed(), "第 " + i + " 条验证码应被成功消费");
        }
    }

    @Test
    public void useWrongCode_rejectedAndRealCodeStaysUsable() {
        // mock 数据
        insertSmsCode(MOBILE, CODE, SCENE, "127.0.0.1");

        // 调用，并断言异常
        assertServiceException(
                () -> smsCodeService.useSmsCode(newUseReqDTO(MOBILE, "9999", SCENE, "10.0.0.9")),
                SMS_CODE_NOT_FOUND);

        // 断言：错误尝试不得把正确的那一条消费掉
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(MOBILE, CODE, SCENE);
        assertNotNull(smsCodeDO);
        assertFalse(smsCodeDO.getUsed(), "验证码不匹配的尝试不得消费掉真实验证码");
    }

    @Test
    public void clientRetryStorm_exactlyOneConsume() {
        // mock 数据
        insertSmsCode(MOBILE, CODE, SCENE, "127.0.0.1");

        // 模拟客户端网络抖动后的串行重试风暴
        int success = 0;
        List<Integer> rejectedCodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            try {
                smsCodeService.useSmsCode(newUseReqDTO(MOBILE, CODE, SCENE, "10.0.0." + i));
                success++;
            } catch (ServiceException ex) {
                rejectedCodes.add(ex.getCode());
            }
        }

        // 断言：重试不产生「多次消费」，且失败原因统一为「已使用」（便于前端一致处理）
        assertEquals(1, success, "客户端重试风暴下，同一条验证码只能被消费一次");
        assertEquals(4, rejectedCodes.size());
        assertTrue(rejectedCodes.stream().allMatch(SMS_CODE_USED.getCode()::equals),
                "重试必须统一按「验证码已使用」拒绝，实际=" + rejectedCodes);
    }

    // ========== 工具方法 ==========

    /**
     * 把 {@code getExpireTimes()} 变成并发栅栏：第 N 个线程到达后所有线程同时放行，
     * 从而把 SELECT 与写回之间的竞态窗口对齐，确定性复现「多消费者同时通过校验」。
     */
    private void stubExpireTimesAsBarrier(int threads) {
        CountDownLatch barrier = new CountDownLatch(threads);
        when(smsCodeProperties.getExpireTimes()).thenAnswer(invocation -> {
            barrier.countDown();
            if (!barrier.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发栅栏超时：未能对齐 " + threads + " 个消费线程");
            }
            return Duration.ofMinutes(5);
        });
    }

    private Throwable[] runConcurrently(int threads, IntConsumer action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            Throwable[] results = new Throwable[threads];
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    try {
                        startGate.await();
                        action.accept(index);
                    } catch (Throwable ex) {
                        results[index] = ex;
                    }
                }));
            }
            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(120, TimeUnit.SECONDS);
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private SmsCodeDO insertSmsCode(String mobile, String code, Integer scene, String createIp) {
        SmsCodeDO smsCodeDO = new SmsCodeDO();
        smsCodeDO.setMobile(mobile);
        smsCodeDO.setCode(code);
        smsCodeDO.setScene(scene);
        smsCodeDO.setCreateIp(createIp);
        smsCodeDO.setTodayIndex(1);
        smsCodeDO.setUsed(false);
        smsCodeDO.setCreateTime(LocalDateTime.now());
        smsCodeMapper.insert(smsCodeDO);
        return smsCodeDO;
    }

    private static SmsCodeUseReqDTO newUseReqDTO(String mobile, String code, Integer scene, String usedIp) {
        return new SmsCodeUseReqDTO().setMobile(mobile).setCode(code).setScene(scene).setUsedIp(usedIp);
    }

    private static boolean isServiceException(Throwable ex, ErrorCode errorCode) {
        return ex instanceof ServiceException && errorCode.getCode().equals(((ServiceException) ex).getCode());
    }

    private static String mobileOf(int index) {
        return "1560169130" + index;
    }

    private static String codeOf(int index) {
        return String.valueOf(1000 + index);
    }

    private static String describe(Throwable[] results) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(i).append('=');
            if (results[i] == null) {
                builder.append("SUCCESS");
            } else if (results[i] instanceof ServiceException) {
                builder.append("ServiceException(").append(((ServiceException) results[i]).getCode()).append(')');
            } else {
                builder.append(results[i].getClass().getSimpleName()).append(':').append(results[i].getMessage());
            }
        }
        return builder.append(']').toString();
    }

}
