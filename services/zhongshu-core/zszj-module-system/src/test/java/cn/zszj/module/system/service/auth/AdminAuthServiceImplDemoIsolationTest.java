package cn.zszj.module.system.service.auth;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.api.sms.SmsCodeApi;
import cn.zszj.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import cn.zszj.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.enums.logger.LoginLogTypeEnum;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.service.logger.LoginLogService;
import cn.zszj.module.system.service.member.MemberService;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.social.SocialUserService;
import cn.zszj.module.system.service.user.AdminUserService;
import com.anji.captcha.service.CaptchaService;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.system.enums.ErrorCodeConstants.AUTH_LOGIN_MODE_DISABLED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004：管理后台登录方式的「演示隔离门控」测试。
 * <p>
 * 缺口复现：{@code AdminAuthServiceImpl} 的 {@code register} / {@code smsLogin} / {@code socialLogin} /
 * {@code resetPassword} / {@code sendSmsCode} 全部<b>无任何开关</b>，只要 HTTP 路由暴露就能直接调用。
 * 一期只获准「账号密码」这一种技术登录方式，其余入口本应在非演示环境下关闭；更糟的是它们与
 * {@code application.yaml} 里 {@code begin-code == end-code == 9999} 的固定演示验证码叠加后，
 * 等于「任何人用 9999 即可短信登录 / 重置任意账号密码」。
 * <p>
 * 本类<b>刻意不加 {@code @TestPropertySource}</b>，即完全采用生产默认值（四个门控全为 {@code false}），
 * 断言这些入口在被直调时一律拒绝，且不产生任何副作用（不消费验证码、不创建令牌、不写用户）。
 * <p>
 * 门控落在 <b>Service 层</b>而非 Controller，因此本类「直接调用 Service」正是对
 * 「能否绕过 Controller 层限制」这一评审关注点的正面回答：绕过 HTTP 也一样被拒。
 * <p>
 * 正向护栏：{@code login}（账密）、{@code refreshToken}、{@code logout} 属于一期获准能力与既有会话维护，
 * <b>不得</b>被门控误伤。
 *
 * @author ZS-LOGIN-004
 */
@Import(AdminAuthServiceImpl.class)
public class AdminAuthServiceImplDemoIsolationTest extends BaseDbUnitTest {

    @Resource
    private AdminAuthServiceImpl authService;

    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private CaptchaService captchaService;
    @MockitoBean
    private LoginLogService loginLogService;
    @MockitoBean
    private SocialUserService socialUserService;
    @MockitoBean
    private SmsCodeApi smsCodeApi;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private Validator validator;

    @BeforeEach
    public void setUp() {
        authService.setCaptchaEnable(true);
    }

    // ========== 未获准入口：直调即拒 ==========

    @Test
    public void register_disabledByDefault_rejected() {
        // 准备参数
        AuthRegisterReqVO reqVO = randomPojo(AuthRegisterReqVO.class);

        // 调用，并断言异常
        assertServiceException(() -> authService.register(reqVO), AUTH_LOGIN_MODE_DISABLED, "自助注册");

        // 断言：无任何副作用
        verify(userService, never()).registerUser(any());
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void smsLogin_disabledByDefault_rejected() {
        // 准备参数
        AuthSmsLoginReqVO reqVO = new AuthSmsLoginReqVO("15601691300", "9999");

        // 调用，并断言异常
        assertServiceException(() -> authService.smsLogin(reqVO), AUTH_LOGIN_MODE_DISABLED, "短信登录");

        // 断言：连验证码都不去消费，更不会发令牌
        verify(smsCodeApi, never()).useSmsCode(any());
        verify(userService, never()).getUserByMobile(anyString());
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void socialLogin_disabledByDefault_rejected() {
        // 准备参数
        AuthSocialLoginReqVO reqVO = randomPojo(AuthSocialLoginReqVO.class);

        // 调用，并断言异常
        assertServiceException(() -> authService.socialLogin(reqVO), AUTH_LOGIN_MODE_DISABLED, "社交登录");

        // 断言：不去换取社交用户，也不发令牌
        verify(socialUserService, never()).getSocialUserByCode(anyInt(), anyInt(), anyString(), anyString());
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void resetPassword_disabledByDefault_rejected() {
        // 准备参数
        AuthResetPasswordReqVO reqVO = randomPojo(AuthResetPasswordReqVO.class);

        // 调用，并断言异常
        assertServiceException(() -> authService.resetPassword(reqVO), AUTH_LOGIN_MODE_DISABLED, "重置密码");

        // 断言：不查用户、不消费验证码、不改密码
        verify(userService, never()).getUserByMobile(anyString());
        verify(smsCodeApi, never()).useSmsCode(any());
        verify(userService, never()).updateUserPassword(anyLong(), anyString());
    }

    @Test
    public void sendSmsCode_loginScene_rejected() {
        // 准备参数：短信登录场景的验证码发送
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO("15601691300", SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene());

        // 调用，并断言异常
        assertServiceException(() -> authService.sendSmsCode(reqVO), AUTH_LOGIN_MODE_DISABLED, "短信登录");

        // 断言：不查用户、不发送短信（关闭的入口不得成为「免费短信出口」）
        verify(userService, never()).getUserByMobile(anyString());
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    @Test
    public void sendSmsCode_resetPasswordScene_rejectedBeforeCaptcha() {
        // 准备参数：重置密码场景。该场景原本会先校验图形验证码，门控必须排在它之前，
        // 否则未获准入口仍会消耗图形验证码校验资源、并把「入口已关闭」误报成「验证码错误」
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO("15601691300",
                SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene());

        // 调用，并断言异常
        assertServiceException(() -> authService.sendSmsCode(reqVO), AUTH_LOGIN_MODE_DISABLED, "重置密码");

        // 断言：门控先于图形验证码生效
        verify(captchaService, never()).verification(any());
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    @Test
    public void sendSmsCode_registerScene_rejected() {
        // 准备参数：自助注册场景
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO("15601691300",
                SmsSceneEnum.ADMIN_MEMBER_REGISTER.getScene());

        // 调用，并断言异常
        assertServiceException(() -> authService.sendSmsCode(reqVO), AUTH_LOGIN_MODE_DISABLED, "自助注册");

        // 断言
        verify(smsCodeApi, never()).sendSmsCode(any());
    }

    // ========== 正向护栏：获准能力不得被误伤 ==========

    @Test
    public void login_passwordLogin_notGated() {
        // 准备参数：账密登录是一期唯一获准的技术登录方式
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class, o ->
                o.setUsername("test_username").setPassword("test_password").setSocialType(null));
        authService.setCaptchaEnable(false);
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setUsername("test_username")
                .setPassword("test_password").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserByUsername(eq("test_username"))).thenReturn(user);
        when(userService.isPasswordMatch(eq("test_password"), eq(user.getPassword()))).thenReturn(true);
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并断言成功
        AuthLoginRespVO loginRespVO = authService.login(reqVO);
        assertNotNull(loginRespVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
    }

    @Test
    public void refreshToken_notGated() {
        // 准备参数
        String refreshToken = randomString();
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.refreshAccessToken(eq(refreshToken), eq("default"))).thenReturn(accessTokenDO);

        // 调用，并断言成功：门控只关「登录入口」，不得让既有会话无法续期
        AuthLoginRespVO loginRespVO = authService.refreshToken(refreshToken);
        assertPojoEquals(accessTokenDO, loginRespVO);
    }

    @Test
    public void logout_notGated() {
        // 准备参数
        String token = randomString();
        when(oauth2TokenService.removeAccessToken(eq(token))).thenReturn(null);

        // 调用，并断言不抛异常：门控绝不能阻止用户退出登录
        authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        verify(oauth2TokenService).removeAccessToken(eq(token));
    }

}
