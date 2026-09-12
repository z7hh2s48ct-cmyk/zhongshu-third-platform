package cn.zszj.module.system.service.sms;

import cn.hutool.core.map.MapUtil;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
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

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(SmsCodeServiceImpl.class)
public class SmsCodeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SmsCodeServiceImpl smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockitoBean
    private SmsCodeProperties smsCodeProperties;
    @MockitoBean
    private SmsSendService smsSendService;
    // ZS-LOGIN-004：验证码安全计数器（尝试次数 / 每 IP 频控）。本类只关注验证码自身的生成与校验语义，
    // 故以 Mock 提供；Mock 默认返回 0 计数，等价于「未触达任何上限」，不影响本类既有断言。
    @MockitoBean
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @BeforeEach
    public void setUp() {
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(10);
        when(smsCodeProperties.getBeginCode()).thenReturn(9999);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
        // ZS-LOGIN-004：新增配置项打桩。Mock 默认返回 null，须显式给出，否则限制失效 / NPE
        when(smsCodeProperties.getMaxValidateAttempts()).thenReturn(SmsCodeProperties.DEFAULT_MAX_VALIDATE_ATTEMPTS);
        when(smsCodeProperties.getAttemptLockDuration()).thenReturn(SmsCodeProperties.DEFAULT_ATTEMPT_LOCK_DURATION);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour())
                .thenReturn(SmsCodeProperties.DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_HOUR);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay())
                .thenReturn(SmsCodeProperties.DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_DAY);
        // ZS-LOGIN-004：声明验证码通道就绪。本类不触达真实短信通道（smsSendService 本身即 Mock）
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    @Test
    public void sendSmsCode_success() {
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });

        // 调用
        smsCodeService.sendSmsCode(reqDTO);
        // 断言 code 验证码
        SmsCodeDO smsCodeDO = smsCodeMapper.selectOne(null);
        assertPojoEquals(reqDTO, smsCodeDO);
        assertEquals("9999", smsCodeDO.getCode());
        assertEquals(1, smsCodeDO.getTodayIndex());
        assertFalse(smsCodeDO.getUsed());
        // 断言调用
        verify(smsSendService).sendSingleSms(eq(reqDTO.getMobile()), isNull(), isNull(),
                eq("user-sms-login"), eq(MapUtil.of("code", "9999")));
    }

    @Test
    public void sendSmsCode_tooFast() {
        // mock 数据
        SmsCodeDO smsCodeDO = randomPojo(SmsCodeDO.class,
                o -> o.setMobile("15601691300").setTodayIndex(1));
        smsCodeMapper.insert(smsCodeDO);
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.sendSmsCode(reqDTO),
                SMS_CODE_SEND_TOO_FAST);
    }

    @Test
    public void sendSmsCode_exceedDay() {
        // mock 数据
        SmsCodeDO smsCodeDO = randomPojo(SmsCodeDO.class,
                o -> o.setMobile("15601691300").setTodayIndex(10).setCreateTime(LocalDateTime.now()));
        smsCodeMapper.insert(smsCodeDO);
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMillis(0));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.sendSmsCode(reqDTO),
                SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
    }

    @Test
    public void testUseSmsCode_success() {
        // 准备参数
        SmsCodeUseReqDTO reqDTO = randomPojo(SmsCodeUseReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> {
            o.setMobile(reqDTO.getMobile()).setScene(reqDTO.getScene())
                    .setCode(reqDTO.getCode()).setUsed(false);
        }));

        // 调用
        smsCodeService.useSmsCode(reqDTO);
        // 断言
        SmsCodeDO smsCodeDO = smsCodeMapper.selectOne(null);
        assertTrue(smsCodeDO.getUsed());
        assertNotNull(smsCodeDO.getUsedTime());
        assertEquals(reqDTO.getUsedIp(), smsCodeDO.getUsedIp());
    }

    @Test
    public void validateSmsCode_success() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(false)));

        // 调用
        smsCodeService.validateSmsCode(reqDTO);
    }

    @Test
    public void validateSmsCode_notFound() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_NOT_FOUND);
    }

    @Test
    public void validateSmsCode_expired() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(false)
                .setCreateTime(LocalDateTime.now().minusMinutes(6))));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_EXPIRED);
    }

    @Test
    public void validateSmsCode_used() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(true)
                .setCreateTime(LocalDateTime.now())));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_USED);
    }

}
