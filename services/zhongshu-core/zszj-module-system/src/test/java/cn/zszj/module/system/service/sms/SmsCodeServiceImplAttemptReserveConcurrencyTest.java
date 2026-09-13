package cn.zszj.module.system.service.sms;

import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_ATTEMPT_LIMIT;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P2-C（codex r0）：验证码「校验尝试次数上限」必须<b>原子地</b>强制执行。
 *
 * <p>缺陷：原 {@code validateSmsCode0} 是 check-then-act——先 {@code getValidateAttempts} 读计数判上限，
 * 再在「验证码不匹配」时 {@code increaseValidateAttempts} 自增（且自增结果被忽略）。并发请求同一 mobile+scene 时，
 * 所有请求都会在任何失败自增落地前读到低于上限的计数，于是<b>全部</b>抵达数据库，而非只放行到上限为止。
 * 例如上限 5、已失败 4 次时，多个后续猜测仍能命中 DB。
 *
 * <p>竞态对齐手法（确定性，不靠 sleep）：把 {@code getMaxValidateAttempts()} 桩成 {@link CountDownLatch} 栅栏——
 * 该方法在原实现（{@code validateAttemptLimit} 首行）与修复实现（{@code validateSmsCode0} 首行）中都<b>先于</b>
 * 配额判定被调用一次，故 N 个线程都会在此对齐后同时冲向计数逻辑：
 * <ul>
 *     <li>原实现：N 个线程全部读到 attempts=0 &lt; 上限 → 全部 selectLastByMobile → 全部自增 → 计数=N（超限）；</li>
 *     <li>修复实现：原子「判上限 + 预留」脚本经 Redis 单线程串行执行，恰有 {@code 上限} 个线程预留成功、其余被拒。</li>
 * </ul>
 *
 * <p>RED：原实现下 notFound 数 = N（远超上限）、超限拒绝数 = 0、最终计数 = N → 三条断言全部失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）以验证原子预留的真实语义；短信通道为 Mock。
 *
 * @author ZS-LOGIN-004 P2-C
 */
@Import({SmsCodeServiceImpl.class, SmsCodeSecurityRedisDAO.class})
public class SmsCodeServiceImplAttemptReserveConcurrencyTest extends BaseDbAndRedisUnitTest {

    private static final int THREADS = 16;

    private static final int MAX_ATTEMPTS = 5;

    private static final String MOBILE = "15602000001";

    private static final Integer SCENE = SmsSceneEnum.MEMBER_LOGIN.getScene();

    private static final String CORRECT_CODE = "1234";

    private static final String WRONG_CODE = "9876";

    @Resource
    private SmsCodeServiceImpl smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @MockitoBean
    private SmsCodeProperties smsCodeProperties;
    @MockitoBean
    private SmsSendService smsSendService;

    @BeforeEach
    public void setUp() {
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeProperties.getAttemptLockDuration()).thenReturn(Duration.ofMinutes(10));
        when(smsCodeProperties.getMaxValidateAttempts()).thenReturn(MAX_ATTEMPTS);
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    @Test
    public void concurrentWrongGuesses_onlyUpToLimitReachDatabase() throws Exception {
        // mock 数据：一条真实验证码（并发提交的均为错误码，故都会走「码不匹配」路径）
        insertSmsCode(MOBILE, CORRECT_CODE, SCENE);
        // 竞态栅栏：让 N 个线程都在「读上限配置」处对齐，随后同时冲向计数逻辑
        stubMaxAttemptsAsBarrier(THREADS);

        // 并发调用：全部提交错误验证码
        Throwable[] results = runConcurrently(THREADS,
                i -> smsCodeService.validateSmsCode(newValidateReqDTO(MOBILE, WRONG_CODE, SCENE)));

        // 断言：恰有 MAX_ATTEMPTS 个猜测抵达 DB 并被计数为「码不匹配」，其余在原子门处被「超限」拒绝
        long notFound = Arrays.stream(results).filter(e -> isServiceException(e, SMS_CODE_NOT_FOUND)).count();
        long exceeded = Arrays.stream(results).filter(e -> isServiceException(e, SMS_CODE_EXCEED_ATTEMPT_LIMIT)).count();
        assertEquals(MAX_ATTEMPTS, notFound,
                "并发猜测中抵达数据库的数量不得超过上限；实际 notFound=" + notFound + "，全部=" + describe(results));
        assertEquals(THREADS - MAX_ATTEMPTS, exceeded,
                "超出上限的猜测必须被原子门拒绝；实际 exceeded=" + exceeded + "，全部=" + describe(results));
        assertEquals(THREADS, notFound + exceeded, "不允许出现其它类型的失败：" + describe(results));

        // 断言：最终失败计数恰为上限（未因并发而超发计数）
        assertEquals(MAX_ATTEMPTS, smsCodeSecurityRedisDAO.getValidateAttempts(MOBILE, SCENE),
                "失败尝试计数必须恰为上限，不得因并发而被突破");
    }

    // ========== 工具方法 ==========

    /**
     * 把 {@code getMaxValidateAttempts()} 变成并发栅栏：第 N 个线程到达后所有线程同时放行，
     * 从而把「读上限」与「后续计数/预留」之间的竞态窗口对齐，确定性复现「多个猜测同时通过上限检查」。
     */
    private void stubMaxAttemptsAsBarrier(int threads) {
        CountDownLatch barrier = new CountDownLatch(threads);
        when(smsCodeProperties.getMaxValidateAttempts()).thenAnswer(invocation -> {
            barrier.countDown();
            if (!barrier.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发栅栏超时：未能对齐 " + threads + " 个校验线程");
            }
            return MAX_ATTEMPTS;
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

    private SmsCodeDO insertSmsCode(String mobile, String code, Integer scene) {
        SmsCodeDO smsCodeDO = new SmsCodeDO();
        smsCodeDO.setMobile(mobile);
        smsCodeDO.setCode(code);
        smsCodeDO.setScene(scene);
        smsCodeDO.setCreateIp("127.0.0.1");
        smsCodeDO.setTodayIndex(1);
        smsCodeDO.setUsed(false);
        smsCodeDO.setCreateTime(LocalDateTime.now());
        smsCodeMapper.insert(smsCodeDO);
        return smsCodeDO;
    }

    private static SmsCodeValidateReqDTO newValidateReqDTO(String mobile, String code, Integer scene) {
        SmsCodeValidateReqDTO reqDTO = new SmsCodeValidateReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setCode(code);
        reqDTO.setScene(scene);
        return reqDTO;
    }

    private static boolean isServiceException(Throwable ex, ErrorCode errorCode) {
        return ex instanceof ServiceException && errorCode.getCode().equals(((ServiceException) ex).getCode());
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
                builder.append("SE(").append(((ServiceException) results[i]).getCode()).append(')');
            } else {
                builder.append(results[i].getClass().getSimpleName()).append(':').append(results[i].getMessage());
            }
        }
        return builder.append(']').toString();
    }

}
