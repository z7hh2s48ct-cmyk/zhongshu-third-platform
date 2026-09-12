package cn.zszj.module.system.service.sms;

import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
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

import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P2-D（codex r0）：每 IP 发送配额必须在<b>持久化与派发之前</b>原子地检查并预留。
 *
 * <p>缺陷：原 {@code sendSmsCode} 先 {@code validateIpSendLimit} 读小时/天两桶计数判上限，
 * 待建码发短信<b>之后</b>才各自 {@code INCR}（且两个自增结果都不检查）。来自同一 IP 对不同 mobile 的并发发送，
 * 都会在自增执行前通过校验，于是每个请求都建码并派发短信——即使自增已超上限，配额被击穿（短信轰炸 + 费用损耗）。
 *
 * <p>竞态对齐手法（确定性，不靠 sleep）：把 {@code getSendMaximumQuantityPerIpPerHour()} 桩成 {@link CountDownLatch}
 * 栅栏——该方法在原实现（{@code validateIpSendLimit} 首行）与修复实现（{@code tryReserveIpSendQuota} 入参求值）中
 * 都<b>先于</b>配额决策被调用一次，故 N 个线程都会在此对齐后同时冲向配额逻辑：
 * <ul>
 *     <li>原实现：N 个线程全部读到 hourCount=0 &lt; 上限 → 全部建码派发 → 全部自增 → 桶计数=N（超发）；</li>
 *     <li>修复实现：原子「检查并预留两桶」脚本经 Redis 单线程串行执行，恰有 {@code 上限} 个请求预留成功、其余回滚并拒绝。</li>
 * </ul>
 *
 * <p>RED：原实现下 success=N、rejected=0、桶计数=N → 断言全部失败。
 *
 * <p>本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）验证原子预留；短信通道为 Mock，不触达真实通道。
 * 各线程使用不同手机号，避免同手机号维度的发送频率/当日序号干扰本用例聚焦的「每 IP 配额」竞态。
 *
 * @author ZS-LOGIN-004 P2-D
 */
@Import({SmsCodeServiceImpl.class, SmsCodeSecurityRedisDAO.class})
public class SmsCodeServiceImplIpQuotaConcurrencyTest extends BaseDbAndRedisUnitTest {

    private static final int THREADS = 12;

    private static final int HOUR_LIMIT = 3;

    private static final int DAY_LIMIT = 100;

    private static final String IP = "203.0.113.33";

    private static final Integer SCENE = SmsSceneEnum.MEMBER_LOGIN.getScene();

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
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(1000);
        when(smsCodeProperties.getBeginCode()).thenReturn(1000);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    @Test
    public void concurrentSendsSameIp_onlyUpToQuotaReservedAndSent() throws Exception {
        // 准备：小时配额=3（约束维度），天配额=100（放开）
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(HOUR_LIMIT);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()).thenReturn(DAY_LIMIT);
        // 竞态栅栏：让 N 个线程都在「读小时上限配置」处对齐，随后同时冲向配额决策
        stubHourLimitAsBarrier(THREADS);

        // 并发调用：同一 IP、不同手机号
        Throwable[] results = runConcurrently(THREADS,
                i -> smsCodeService.sendSmsCode(newSendReqDTO(mobileOf(i), IP)));

        // 断言：恰有 HOUR_LIMIT 个请求成功建码派发，其余被「每 IP 超限」原子拒绝
        long success = Arrays.stream(results).filter(Objects::isNull).count();
        long rejected = Arrays.stream(results)
                .filter(e -> isServiceException(e, SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP)).count();
        assertEquals(HOUR_LIMIT, success,
                "并发发送中成功数不得超过小时配额；实际 success=" + success + "，全部=" + describe(results));
        assertEquals(THREADS - HOUR_LIMIT, rejected,
                "超出配额的请求必须被原子预留门拒绝；实际 rejected=" + rejected + "，全部=" + describe(results));
        assertEquals(THREADS, success + rejected, "不允许出现其它类型的失败：" + describe(results));

        // 断言：小时桶计数恰为上限（未因并发而超发），且落库条数与成功数一致（被拒请求不写库）
        assertEquals(HOUR_LIMIT, smsCodeSecurityRedisDAO.getIpSendCountPerHour(IP),
                "小时桶计数必须恰为上限，不得因并发而被突破");
        assertEquals(HOUR_LIMIT, smsCodeMapper.selectCount(SmsCodeDO::getCreateIp, IP).intValue(),
                "被配额拒绝的请求不得落库");
    }

    // ========== 工具方法 ==========

    private void stubHourLimitAsBarrier(int threads) {
        CountDownLatch barrier = new CountDownLatch(threads);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenAnswer(invocation -> {
            barrier.countDown();
            if (!barrier.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发栅栏超时：未能对齐 " + threads + " 个发送线程");
            }
            return HOUR_LIMIT;
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

    private static SmsCodeSendReqDTO newSendReqDTO(String mobile, String createIp) {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setScene(SCENE);
        reqDTO.setCreateIp(createIp);
        return reqDTO;
    }

    private static String mobileOf(int index) {
        return "156030" + String.format("%05d", index);
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
