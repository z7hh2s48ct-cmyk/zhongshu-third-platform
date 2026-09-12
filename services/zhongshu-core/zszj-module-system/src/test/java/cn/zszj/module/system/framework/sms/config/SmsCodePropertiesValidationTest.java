package cn.zszj.module.system.framework.sms.config;

import cn.hutool.core.util.ReflectUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zszj.module.system.dal.dataobject.sms.SmsChannelDO;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zszj.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.zszj.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.service.sms.SmsChannelService;
import cn.zszj.module.system.service.sms.SmsCodeServiceImpl;
import cn.zszj.module.system.service.sms.SmsSendService;
import cn.zszj.module.system.service.sms.SmsSendServiceImpl;
import cn.zszj.module.system.service.sms.SmsTemplateService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.regex.Pattern;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_SEND_CHANNEL_NOT_READY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004：验证码「配置安全性」与「通道就绪性」测试。
 * <p>
 * 覆盖三件历史上互相掩盖的事：
 * <ol>
 *     <li><b>固定演示验证码被带进生产</b>：{@code application.yaml} 把 {@code begin-code} 与 {@code end-code}
 *     同时写死为 9999，而生产模板 {@code script/config/application-prod.yaml} 并不覆盖该段，
 *     于是生产环境实际使用的就是「人人都知道的 9999」。要求：非宽松 profile 下取值域不安全即<b>启动失败</b>，
 *     固定演示码只允许出现在 local / dev / test / unit-test；同时用文件级守卫断言打包模板里不再出现裸 9999。</li>
 *     <li><b>取值域过窄等同可枚举</b>：不仅 {@code begin == end} 不安全，取值域小于
 *     {@link SmsCodePropertiesValidator#MIN_SECURE_CODE_RANGE} 同样不安全。</li>
 *     <li><b>通道未就绪却假报发送成功</b>：{@code SmsSendServiceImpl#sendSingleSms} 在模板/渠道被禁用时
 *     只写日志、不发 MQ，却照样返回 sendLogId，调用方无从知晓「其实没发出去」，用户会一直等一条不会来的短信。
 *     要求：验证码发送前先探通道，不就绪则明确失败，且<b>不写库、不占配额</b>。</li>
 * </ol>
 * <p>
 * 本类<b>不触达任何真实短信通道</b>：{@link SmsSendService} 在 Spring 侧是 Mock，
 * {@link SmsSendServiceImpl} 在纯 Mockito 侧只依赖 Mock 的模板/渠道服务。
 *
 * @author ZS-LOGIN-004
 */
@Import(SmsCodeServiceImpl.class)
public class SmsCodePropertiesValidationTest extends BaseDbUnitTest {

    /**
     * 打包模板中出现「裸 9999 固定演示码」即视为回退。允许带注释，但不允许作为实际取值。
     */
    private static final Pattern FIXED_DEMO_BEGIN_CODE = Pattern.compile("^\\s*begin-code:\\s*9999\\s*(#.*)?$",
            Pattern.MULTILINE);

    private static final Pattern FIXED_DEMO_END_CODE = Pattern.compile("^\\s*end-code:\\s*9999\\s*(#.*)?$",
            Pattern.MULTILINE);

    @Resource
    private SmsCodeServiceImpl smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockitoBean
    private SmsCodeProperties smsCodeProperties;
    @MockitoBean
    private SmsSendService smsSendService;
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
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour())
                .thenReturn(SmsCodeProperties.DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_HOUR);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay())
                .thenReturn(SmsCodeProperties.DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_DAY);
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(true);
    }

    // ========== 启动期配置校验：非宽松 profile 必须拒绝不安全取值域 ==========

    @Test
    public void validate_prodProfile_beginEqualsEnd_startupFails() {
        // 断言：历史上「begin-code = end-code = 9999」的固定演示码，在 prod 下必须让应用启动失败
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SmsCodePropertiesValidator.validate(9999, 9999, new String[]{"prod"}));
        assertTrue(ex.getMessage().contains("begin-code"), "异常信息需指明问题配置项，实际=" + ex.getMessage());
    }

    @Test
    public void validate_prodProfile_rangeTooSmall_startupFails() {
        // 断言：取值域仅 100 个（9900~9999），远小于安全下限，等同可被暴力枚举
        assertThrows(IllegalStateException.class,
                () -> SmsCodePropertiesValidator.validate(9900, 9999, new String[]{"prod"}));
    }

    @Test
    public void validate_prodProfile_endLessThanBegin_startupFails() {
        // 断言：反向区间是明显的配置错误，不得静默放过
        assertThrows(IllegalStateException.class,
                () -> SmsCodePropertiesValidator.validate(9999, 1000, new String[]{"prod"}));
    }

    @Test
    public void validate_noActiveProfile_treatedAsStrict() {
        // 断言：未显式指定 active profile 时按严格处理（fail-safe）——
        // 不能因为漏配 SPRING_PROFILES_ACTIVE 就静默退化成「演示态」
        assertThrows(IllegalStateException.class,
                () -> SmsCodePropertiesValidator.validate(9999, 9999, new String[]{"default"}));
    }

    @Test
    public void validate_prodProfile_secureRange_passes() {
        // 断言：1000~9999（9000 个取值）在生产下是安全的，不得误伤正常部署
        assertDoesNotThrow(() -> SmsCodePropertiesValidator.validate(1000, 9999, new String[]{"prod"}));
    }

    @Test
    public void validate_permissiveProfiles_fixedDemoCodeAllowed() {
        // 断言：本地/开发/测试环境仍允许固定演示码（否则日常联调成本过高），仅告警不阻断
        for (String profile : SmsCodePropertiesValidator.PERMISSIVE_PROFILES) {
            assertDoesNotThrow(() -> SmsCodePropertiesValidator.validate(9999, 9999, new String[]{profile}),
                    "profile=" + profile + " 应允许固定演示码");
        }
    }

    @Test
    public void isInsecureCodeRange_policy() {
        // 未配置（交由 @NotNull 校验兜底）时不在此处判定，避免双重语义
        assertFalse(SmsCodePropertiesValidator.isInsecureCodeRange(null, null));
        assertFalse(SmsCodePropertiesValidator.isInsecureCodeRange(1000, null));
        // begin == end：取值域 1，最典型的固定演示码
        assertTrue(SmsCodePropertiesValidator.isInsecureCodeRange(9999, 9999));
        assertTrue(SmsCodePropertiesValidator.isInsecureCodeRange(0, 0));
        // 反向区间
        assertTrue(SmsCodePropertiesValidator.isInsecureCodeRange(9999, 1000));
        // 取值域过小
        assertTrue(SmsCodePropertiesValidator.isInsecureCodeRange(9900, 9999));
        assertTrue(SmsCodePropertiesValidator.isInsecureCodeRange(1000,
                1000 + SmsCodePropertiesValidator.MIN_SECURE_CODE_RANGE - 2));
        // 恰好达到下限即视为安全（闭区间计数）
        assertFalse(SmsCodePropertiesValidator.isInsecureCodeRange(1000,
                1000 + SmsCodePropertiesValidator.MIN_SECURE_CODE_RANGE - 1));
        assertFalse(SmsCodePropertiesValidator.isInsecureCodeRange(1000, 9999));
    }

    @Test
    public void isPermissiveProfile_policy() {
        assertTrue(SmsCodePropertiesValidator.isPermissiveProfile(new String[]{"local"}));
        assertTrue(SmsCodePropertiesValidator.isPermissiveProfile(new String[]{"unit-test"}));
        assertFalse(SmsCodePropertiesValidator.isPermissiveProfile(new String[]{"prod"}));
        // 混合 profile 时按「最严格」处理：只要有一个非宽松 profile，就不允许固定演示码
        assertFalse(SmsCodePropertiesValidator.isPermissiveProfile(new String[]{"prod", "dev"}));
        // 空 profile 视同未指定 → 严格
        assertFalse(SmsCodePropertiesValidator.isPermissiveProfile(new String[]{}));
        assertFalse(SmsCodePropertiesValidator.isPermissiveProfile(null));
    }

    @Test
    public void resolveProfiles_fallsBackToDefaultProfiles() {
        // 无 active profile
        MockEnvironment environment = new MockEnvironment();
        SmsCodePropertiesValidator validator =
                new SmsCodePropertiesValidator(new SmsCodeProperties(), environment);
        assertArrayEquals(environment.getDefaultProfiles(), validator.resolveProfiles(),
                "未指定 active profile 时应回落 default profile（即 default，属严格分支）");

        // 有 active profile
        MockEnvironment prodEnvironment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        prodEnvironment.setActiveProfiles("prod");
        SmsCodePropertiesValidator prodValidator =
                new SmsCodePropertiesValidator(new SmsCodeProperties(), prodEnvironment);
        assertArrayEquals(new String[]{"prod"}, prodValidator.resolveProfiles());
    }

    // ========== 通道就绪性：不得假报发送成功 ==========

    @Test
    public void isTemplateSendable_templateMissing_returnsFalse() {
        SmsTemplateService templateService = mock(SmsTemplateService.class);
        SmsChannelService channelService = mock(SmsChannelService.class);
        when(templateService.getSmsTemplateByCodeFromCache(eq("user-sms-login"))).thenReturn(null);
        SmsSendServiceImpl sendService = newSmsSendService(templateService, channelService);

        assertFalse(sendService.isTemplateSendable("user-sms-login"), "模板不存在时不得声称可发送");
    }

    @Test
    public void isTemplateSendable_templateDisabled_returnsFalse() {
        SmsTemplateService templateService = mock(SmsTemplateService.class);
        SmsChannelService channelService = mock(SmsChannelService.class);
        when(templateService.getSmsTemplateByCodeFromCache(eq("user-sms-login")))
                .thenReturn(randomPojo(SmsTemplateDO.class, o -> o.setChannelId(1L)
                        .setStatus(CommonStatusEnum.DISABLE.getStatus())));
        SmsSendServiceImpl sendService = newSmsSendService(templateService, channelService);

        assertFalse(sendService.isTemplateSendable("user-sms-login"), "模板被禁用时不得声称可发送");
    }

    @Test
    public void isTemplateSendable_channelMissing_returnsFalse() {
        SmsTemplateService templateService = mock(SmsTemplateService.class);
        SmsChannelService channelService = mock(SmsChannelService.class);
        when(templateService.getSmsTemplateByCodeFromCache(eq("user-sms-login")))
                .thenReturn(randomPojo(SmsTemplateDO.class, o -> o.setChannelId(1L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus())));
        when(channelService.getSmsChannel(eq(1L))).thenReturn(null);
        SmsSendServiceImpl sendService = newSmsSendService(templateService, channelService);

        assertFalse(sendService.isTemplateSendable("user-sms-login"), "渠道不存在时不得声称可发送");
    }

    @Test
    public void isTemplateSendable_channelDisabled_returnsFalse() {
        SmsTemplateService templateService = mock(SmsTemplateService.class);
        SmsChannelService channelService = mock(SmsChannelService.class);
        when(templateService.getSmsTemplateByCodeFromCache(eq("user-sms-login")))
                .thenReturn(randomPojo(SmsTemplateDO.class, o -> o.setChannelId(1L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus())));
        when(channelService.getSmsChannel(eq(1L))).thenReturn(randomPojo(SmsChannelDO.class,
                o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        SmsSendServiceImpl sendService = newSmsSendService(templateService, channelService);

        assertFalse(sendService.isTemplateSendable("user-sms-login"), "渠道被禁用时不得声称可发送");
    }

    @Test
    public void isTemplateSendable_templateAndChannelEnabled_returnsTrue() {
        SmsTemplateService templateService = mock(SmsTemplateService.class);
        SmsChannelService channelService = mock(SmsChannelService.class);
        when(templateService.getSmsTemplateByCodeFromCache(eq("user-sms-login")))
                .thenReturn(randomPojo(SmsTemplateDO.class, o -> o.setChannelId(1L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus())));
        when(channelService.getSmsChannel(eq(1L))).thenReturn(randomPojo(SmsChannelDO.class,
                o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus())));
        SmsSendServiceImpl sendService = newSmsSendService(templateService, channelService);

        // 正向护栏：通道齐备时必须放行，否则合法验证码也发不出去
        assertTrue(sendService.isTemplateSendable("user-sms-login"));
    }

    @Test
    public void sendSmsCode_channelNotReady_failsFastAndWritesNothing() {
        // 准备参数
        when(smsSendService.isTemplateSendable(anyString())).thenReturn(false);
        SmsCodeSendReqDTO reqDTO = newSendReqDTO("15601691300", "127.0.0.1");

        // 调用，并断言明确失败（而不是静默「假装已发送」）
        assertServiceException(() -> smsCodeService.sendSmsCode(reqDTO), SMS_CODE_SEND_CHANNEL_NOT_READY);

        // 断言：不写库、不占用发送配额、不调用发送
        assertEquals(0, smsCodeMapper.selectCount().intValue(), "通道未就绪时不得落库，避免占用配额与产生脏数据");
        verify(smsSendService, never()).sendSingleSms(anyString(), any(), any(), anyString(), anyMap());
    }

    @Test
    public void sendSmsCode_channelReady_sendsAndPersists() {
        // 准备参数
        SmsCodeSendReqDTO reqDTO = newSendReqDTO("15601691300", "127.0.0.1");

        // 调用
        smsCodeService.sendSmsCode(reqDTO);

        // 断言：正向护栏——通道就绪时正常落库并发送
        SmsCodeDO smsCodeDO = smsCodeMapper.selectLastByMobile(reqDTO.getMobile(), null, reqDTO.getScene());
        assertNotNull(smsCodeDO);
        assertEquals(reqDTO.getCreateIp(), smsCodeDO.getCreateIp());
        assertFalse(smsCodeDO.getUsed());
        verify(smsSendService).sendSingleSms(eq(reqDTO.getMobile()), isNull(), isNull(),
                eq("user-sms-login"), anyMap());
    }

    // ========== 打包模板守卫：固定演示码不得回流到生产配置 ==========

    @Test
    public void packagedApplicationYaml_hasNoFixedDemoCode() throws Exception {
        Path path = locate("zszj-server/src/main/resources/application.yaml");
        assumeTrue(path != null, "未定位到 zszj-server 的 application.yaml，跳过打包模板守卫");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertFalse(FIXED_DEMO_BEGIN_CODE.matcher(content).find(),
                "基础模板 application.yaml 不得再把 begin-code 写死为 9999（生产会继承该值）");
        assertFalse(FIXED_DEMO_END_CODE.matcher(content).find(),
                "基础模板 application.yaml 不得再把 end-code 写死为 9999（生产会继承该值）");
        assertTrue(content.contains("begin-code: ${ZSZJ_SMS_CODE_BEGIN:"),
                "begin-code 必须走环境变量占位 + 安全默认随机下界");
        assertTrue(content.contains("end-code: ${ZSZJ_SMS_CODE_END:"),
                "end-code 必须走环境变量占位 + 安全默认随机上界");
    }

    @Test
    public void packagedProdTemplate_keepsSecureCodeRangeAndClosesDemoLoginModes() throws Exception {
        Path path = locate("script/config/application-prod.yaml");
        assumeTrue(path != null, "未定位到 script/config/application-prod.yaml，跳过生产模板守卫");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertFalse(FIXED_DEMO_BEGIN_CODE.matcher(content).find(), "生产模板不得出现固定演示验证码");
        assertFalse(FIXED_DEMO_END_CODE.matcher(content).find(), "生产模板不得出现固定演示验证码");
        assertTrue(content.contains("login-mode:"), "生产模板必须显式声明登录方式门控段");
        assertTrue(content.contains("${ZSZJ_LOGIN_MODE_SMS_ENABLED:false}"),
                "生产模板的短信登录门控必须默认关闭");
        assertTrue(content.contains("${ZSZJ_LOGIN_MODE_REGISTER_ENABLED:false}"),
                "生产模板的自助注册门控必须默认关闭");
    }

    @Test
    public void nonProdProfiles_mayKeepFixedDemoCode() throws Exception {
        Path path = locate("zszj-server/src/main/resources/application-local.yaml");
        assumeTrue(path != null, "未定位到 application-local.yaml，跳过非生产 profile 守卫");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        // 断言：固定演示码被下沉到非生产 profile，日常联调不受影响
        assertTrue(FIXED_DEMO_BEGIN_CODE.matcher(content).find(),
                "本地 profile 应保留 begin-code: 9999 以便联调");
        assertTrue(FIXED_DEMO_END_CODE.matcher(content).find(),
                "本地 profile 应保留 end-code: 9999 以便联调");
    }

    // ========== 工具方法 ==========

    private static SmsSendServiceImpl newSmsSendService(SmsTemplateService templateService,
                                                        SmsChannelService channelService) {
        SmsSendServiceImpl sendService = new SmsSendServiceImpl();
        ReflectUtil.setFieldValue(sendService, "smsTemplateService", templateService);
        ReflectUtil.setFieldValue(sendService, "smsChannelService", channelService);
        return sendService;
    }

    private static SmsCodeSendReqDTO newSendReqDTO(String mobile, String createIp) {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        reqDTO.setCreateIp(createIp);
        return reqDTO;
    }

    /**
     * 以 reactor 内 {@code zszj-module-system} 模块目录（即 surefire 的工作目录）为基准，向上定位仓库内文件。
     * 逐级回退最多 4 层，兼容「在模块目录执行」与「在 reactor 根执行」两种情形。
     */
    private static Path locate(String relativePath) {
        Path base = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i <= 4; i++) {
            Path candidate = base.resolve(relativePath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
            Path parent = base.getParent();
            if (parent == null || parent.equals(base)) {
                return null;
            }
            base = parent;
        }
        return null;
    }

}
