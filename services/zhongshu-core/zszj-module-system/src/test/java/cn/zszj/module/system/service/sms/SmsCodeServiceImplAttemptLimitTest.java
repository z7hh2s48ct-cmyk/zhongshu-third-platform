package cn.zszj.module.system.service.sms;

import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
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

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_ATTEMPT_LIMIT;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXPIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_USED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004：验证码「尝试次数上限（暴力破解防护）」与「每 IP 发送频控」测试。
 * <p>
 * 缺口复现：
 * <ol>
 *     <li>{@code validateSmsCode0} 原本对「验证码不匹配」的失败尝试<b>完全不计数</b>，
 *     攻击者可以无限次枚举 4~6 位验证码；配合历史上 {@code begin-code == end-code == 9999} 的固定演示码，
 *     等于零成本撞库。本类要求：失败尝试按「手机号 + 场景」计数，超过阈值后在锁定期内直接拒绝，
 *     且<b>连正确的验证码也一并拒绝</b>（否则锁定形同虚设）。</li>
 *     <li>{@code createSmsCode} 里「每个 IP 每天/每小时可发送数量」长期是 TODO，
 *     只有按手机号维度的频控，攻击者用同一 IP 轮换手机号即可无限发送短信（短信轰炸 + 费用损耗）。</li>
 * </ol>
 * <p>
 * 本类使用真实内存 Redis（{@link BaseDbAndRedisUnitTest}）以验证计数、锁定与 TTL 自动解锁的真实行为；
 * 短信通道本身始终是 {@link SmsSendService} 的 Mock，<b>不会触达任何真实短信通道</b>。
 * <p>
 * 注意：{@code /sql/clean.sql} 只清理 DB、不清理 Redis，故每个用例使用各自独立的手机号与 IP，避免计数串扰。
 *
 * @author ZS-LOGIN-004
 */
@Import({SmsCodeServiceImpl.class, SmsCodeSecurityRedisDAO.class})
public class SmsCodeServiceImplAttemptLimitTest extends BaseDbAndRedisUnitTest {

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
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(10);
        when(smsCodeProperties.getBeginCode()).thenReturn(1000);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
        when(smsCodeProperties.getMaxValidateAttempts()).thenReturn(3);
        when(smsCodeProperties.getAttemptLockDuration()).thenReturn(Duration.ofMinutes(10));
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(20);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()).thenReturn(50);
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    // ========== 尝试次数上限 ==========

    @Test
    public void wrongCode_incrementsAttemptCounter() {
        // mock 数据
        String mobile = "15600000001";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);

        // 调用：提交错误的验证码
        assertServiceException(() -> smsCodeService.validateSmsCode(
                newValidateReqDTO(mobile, WRONG_CODE, SCENE)), SMS_CODE_NOT_FOUND);

        // 断言：失败尝试被计数
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE),
                "验证码不匹配的尝试必须被计数，否则暴力枚举无任何成本");
    }

    @Test
    public void exceedAttemptLimit_lockedEvenWithCorrectCode() {
        // mock 数据
        String mobile = "15600000002";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);
        int maxAttempts = 3;

        // 连续错误尝试，每次都按「验证码不存在」拒绝，但计数持续累加
        for (int i = 0; i < maxAttempts; i++) {
            assertServiceException(() -> smsCodeService.validateSmsCode(
                    newValidateReqDTO(mobile, WRONG_CODE, SCENE)), SMS_CODE_NOT_FOUND);
        }
        assertEquals(maxAttempts, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE));

        // 断言：超过阈值后锁定——即使提交<b>正确的</b>验证码也一律拒绝
        assertServiceException(() -> smsCodeService.validateSmsCode(
                        newValidateReqDTO(mobile, CORRECT_CODE, SCENE)),
                SMS_CODE_EXCEED_ATTEMPT_LIMIT, Duration.ofMinutes(10).getSeconds());
        // 断言：锁定期内不再查询 DB，计数也不会被「正确的码」洗白
        assertEquals(maxAttempts, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE),
                "锁定期间的调用不得改变计数，否则攻击者可借此探测锁定状态");
    }

    @Test
    public void exceedAttemptLimit_useSmsCodeAlsoLocked() {
        // mock 数据
        String mobile = "15600000003";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);
        for (int i = 0; i < 3; i++) {
            assertServiceException(() -> smsCodeService.validateSmsCode(
                    newValidateReqDTO(mobile, WRONG_CODE, SCENE)), SMS_CODE_NOT_FOUND);
        }

        // 断言：useSmsCode（登录/改密的真实入口）同样受锁定约束，不能绕过
        assertServiceException(() -> smsCodeService.useSmsCode(
                        newUseReqDTO(mobile, CORRECT_CODE, SCENE, "10.0.0.3")),
                SMS_CODE_EXCEED_ATTEMPT_LIMIT, Duration.ofMinutes(10).getSeconds());
        // 断言：验证码未被消费
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(mobile, CORRECT_CODE, SCENE);
        assertNotNull(smsCodeDO);
        assertEquals(Boolean.FALSE, smsCodeDO.getUsed(), "锁定期间不得消费验证码");
    }

    @Test
    public void successConsume_resetsAttemptCounter() {
        // mock 数据
        String mobile = "15600000004";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);

        // 先失败 2 次（未达阈值 3）
        for (int i = 0; i < 2; i++) {
            assertServiceException(() -> smsCodeService.validateSmsCode(
                    newValidateReqDTO(mobile, WRONG_CODE, SCENE)), SMS_CODE_NOT_FOUND);
        }
        assertEquals(2, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE));

        // 合法用户走通一次完整校验并消费
        smsCodeService.useSmsCode(newUseReqDTO(mobile, CORRECT_CODE, SCENE, "10.0.0.4"));

        // 断言：计数归零，合法用户不会因历史误输被越锁越死
        assertEquals(0, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE),
                "成功消费后必须清零失败计数");
    }

    @Test
    public void expiredCode_notCountedAsAttempt() {
        // mock 数据：一条已过期的验证码
        String mobile = "15600000005";
        SmsCodeDO smsCodeDO = insertSmsCode(mobile, CORRECT_CODE, SCENE);
        smsCodeDO.setCreateTime(LocalDateTime.now().minusMinutes(6));
        smsCodeMapper.updateById(smsCodeDO);

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(
                newValidateReqDTO(mobile, CORRECT_CODE, SCENE)), SMS_CODE_EXPIRED);

        // 断言：过期不是攻击信号（合法用户手慢而已），不得计入失败尝试
        assertEquals(0, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE),
                "验证码过期不应计入暴力破解尝试次数");
    }

    @Test
    public void usedCode_notCountedAsAttempt() {
        // mock 数据：一条已被消费的验证码
        String mobile = "15600000006";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);
        smsCodeService.useSmsCode(newUseReqDTO(mobile, CORRECT_CODE, SCENE, "10.0.0.6"));

        // 调用：重复提交同一条（已使用的）验证码
        assertServiceException(() -> smsCodeService.validateSmsCode(
                newValidateReqDTO(mobile, CORRECT_CODE, SCENE)), SMS_CODE_USED);

        // 断言：码是对的、只是已用过，不计入失败尝试；否则并发消费会把合法用户直接锁死
        assertEquals(0, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, SCENE),
                "重复提交已使用的正确验证码不应计入暴力破解尝试次数");
    }

    @Test
    public void wrongScene_rejectedAndCounted() {
        // mock 数据：只发过「会员登录」场景的验证码
        String mobile = "15600000007";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);

        // 调用：拿登录场景的码去撞「重置密码」场景
        Integer otherScene = SmsSceneEnum.MEMBER_RESET_PASSWORD.getScene();
        assertServiceException(() -> smsCodeService.validateSmsCode(
                newValidateReqDTO(mobile, CORRECT_CODE, otherScene)), SMS_CODE_NOT_FOUND);

        // 断言：错场景等同于「码不匹配」，同样计入失败尝试
        assertEquals(1, smsCodeSecurityRedisDAO.getValidateAttempts(mobile, otherScene),
                "跨场景撞码同样属于暴力尝试，必须按 手机号+场景 计数");
    }

    @Test
    public void attemptLock_hasTtlSoItAutoUnlocks() {
        // mock 数据
        String mobile = "15600000008";
        insertSmsCode(mobile, CORRECT_CODE, SCENE);

        // 触发一次失败尝试
        assertServiceException(() -> smsCodeService.validateSmsCode(
                newValidateReqDTO(mobile, WRONG_CODE, SCENE)), SMS_CODE_NOT_FOUND);

        // 断言：锁定计数键带有 TTL，即锁定期到期后由 Redis 自动解锁，无需人工介入、也不会永久锁死合法用户
        Duration ttl = smsCodeSecurityRedisDAO.getValidateAttemptsTtl(mobile, SCENE);
        assertTrue(!ttl.isZero() && !ttl.isNegative(), "失败计数键必须设置 TTL，实际=" + ttl);
        assertTrue(ttl.compareTo(Duration.ofMinutes(10)) <= 0,
                "TTL 不得超过配置的锁定时长，实际=" + ttl);
    }

    // ========== 每 IP 发送频控 ==========

    @Test
    public void sameIpManyMobiles_exceedIpHourLimit() {
        // 准备参数：每 IP 每小时上限 3 条
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(3);
        String ip = "192.168.10.1";

        // 同一 IP 轮换手机号发送，前 3 条放行
        for (int i = 0; i < 3; i++) {
            smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156100", i), ip));
        }
        assertEquals(3, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));

        // 断言：第 4 条被 IP 维度频控拒绝——这正是「只按手机号频控」时被漏掉的喷洒攻击
        assertServiceException(() -> smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156100", 3), ip)),
                SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);
        // 断言：被拒的请求不写库、不占用配额
        assertEquals(3, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip));
        assertEquals(0, smsCodeMapper.selectCount(SmsCodeDO::getCreateIp, ipMobile("156100", 3)).intValue(),
                "被频控拒绝的请求不得落库");
    }

    @Test
    public void differentIp_doNotShareQuota() {
        // 准备参数：每 IP 每小时上限 2 条
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(2);
        String ipA = "192.168.20.1";
        String ipB = "192.168.20.2";

        // IP A 用满配额
        smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156200", 0), ipA));
        smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156200", 1), ipA));
        assertServiceException(() -> smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156200", 2), ipA)),
                SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);

        // 断言：IP B 不受 IP A 的配额影响（防「误伤合法用户」）
        smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156200", 3), ipB));
        assertEquals(1, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ipB));
        assertEquals(2, smsCodeSecurityRedisDAO.getIpSendCountPerHour(ipA));
    }

    @Test
    public void sameIp_exceedIpDayLimit() {
        // 准备参数：小时上限放开，只让「每天上限」生效
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(100);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()).thenReturn(2);
        String ip = "192.168.30.1";

        smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156300", 0), ip));
        smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156300", 1), ip));
        assertEquals(2, smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip));

        // 断言：天维度同样封顶
        assertServiceException(() -> smsCodeService.sendSmsCode(newSendReqDTO(ipMobile("156300", 2), ip)),
                SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);
    }

    @Test
    public void blankIp_skipsIpThrottleButStillSends() {
        // 准备参数：IP 上限压到 0，用于验证「取不到 IP 时不误伤」
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(0);
        String mobile = "15600000009";

        // 调用：createIp 为空串（例如内部任务、非 HTTP 入口触发）
        smsCodeService.sendSmsCode(newSendReqDTO(mobile, ""));

        // 断言：无 IP 维度可计时跳过 IP 频控，但仍按手机号维度正常发送、正常落库
        verify(smsSendService).sendSingleSms(eq(mobile), isNull(), isNull(),
                eq("user-sms-login"), anyMap());
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(mobile, null, SCENE);
        assertNotNull(smsCodeDO, "IP 为空时仍应正常创建验证码");
    }

    // ========== 工具方法 ==========

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

    private static SmsCodeUseReqDTO newUseReqDTO(String mobile, String code, Integer scene, String usedIp) {
        return new SmsCodeUseReqDTO().setMobile(mobile).setCode(code).setScene(scene).setUsedIp(usedIp);
    }

    private static SmsCodeSendReqDTO newSendReqDTO(String mobile, String createIp) {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setScene(SCENE);
        reqDTO.setCreateIp(createIp);
        return reqDTO;
    }

    /**
     * 生成「同一 IP 下轮换的手机号」，形如 {@code 15610000000}（11 位，符合 mobile 列宽）
     */
    private static String ipMobile(String prefix, int index) {
        return prefix + String.format("%05d", index);
    }

}
