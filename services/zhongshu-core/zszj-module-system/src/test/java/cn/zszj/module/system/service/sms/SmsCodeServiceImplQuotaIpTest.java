package cn.zszj.module.system.service.sms;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.framework.sms.config.SmsCodeProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P1（codex r0）：短信「每 IP 发送配额」必须来自<b>不可被调用者伪造</b>的地址。
 *
 * <p>缺陷：{@code AdminAuthServiceImpl.sendSmsCode()} 经 {@code ServletUtils.getClientIP()}（Hutool 实现，
 * <b>先信 X-Forwarded-For 再看 socket 地址</b>）取 createIp 作为配额维度键。当短信端点启用且转发头未被可信代理净化时，
 * 调用者只需每次改 {@code X-Forwarded-For}，就能让每个伪造地址都拿到全新的小时/天配额桶，从而无限绕过每 IP 频控。
 *
 * <p>修复合同（局部、secure by default）：配额 IP 改为「可信代理感知」解析——<b>默认不信任 XFF</b>、
 * 直连回退 peer address（{@code request.getRemoteAddr()}）；仅当 peer 命中可信代理白名单时才采信 XFF。
 * 本测试覆盖<b>默认（白名单为空）</b>路径：伪造 XFF 不得获得独立配额桶。
 *
 * <p>RED：原实现直接以（被 XFF 污染的）createIp 作为配额键，且完全不看请求上下文——
 * 3 次「同 peer、不同伪造 XFF」的发送会各自命中全新桶 → 全部放行、peer 桶计数为 0。
 *
 * <p>说明：本类<b>不引用</b> {@code SmsCodeProperties#getTrustedProxies()}，故 RED 阶段可对着原代码编译运行；
 * 可信代理命中时采信 XFF 的正向路径见 {@code SmsCodeServiceImplTrustedProxyTest}。
 *
 * @author ZS-LOGIN-004 P1
 */
@Import({SmsCodeServiceImpl.class, SmsCodeSecurityRedisDAO.class})
public class SmsCodeServiceImplQuotaIpTest extends BaseDbAndRedisUnitTest {

    private static final Integer SCENE = SmsSceneEnum.MEMBER_LOGIN.getScene();

    @Resource
    private SmsCodeServiceImpl smsCodeService;

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

    @AfterEach
    public void tearDown() {
        // 清理线程绑定的请求上下文，避免污染其它用例
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void spoofedXff_doesNotGrantFreshQuotaBucket_whenNoTrustedProxy() {
        // 准备：小时配额压到 2；攻击者真实直连地址固定，但每次伪造不同 XFF
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(2);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()).thenReturn(1000);
        String realPeer = "203.0.113.9";

        // 调用：3 次发送，peer 相同、XFF（及被上游污染的 createIp）各不相同
        int rejected = 0;
        for (int i = 0; i < 3; i++) {
            String spoofed = "10.9.9." + i;
            bindRequest(realPeer, spoofed);
            try {
                smsCodeService.sendSmsCode(newSendReqDTO(mobileOf(i), spoofed));
            } catch (ServiceException ex) {
                if (SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP.getCode().equals(ex.getCode())) {
                    rejected++;
                } else {
                    throw ex;
                }
            }
        }

        // 断言：默认不信任 XFF → 配额按不可伪造的 peer 计，第 3 次必须被拒
        assertEquals(1, rejected,
                "同一 peer 伪造不同 XFF 不得超过小时配额（默认不信任 X-Forwarded-For）；实际被拒=" + rejected);
        assertEquals(2, smsCodeSecurityRedisDAO.getIpSendCountPerHour(realPeer),
                "配额桶必须落在不可伪造的 peer address 上");
        for (int i = 0; i < 3; i++) {
            assertEquals(0, smsCodeSecurityRedisDAO.getIpSendCountPerHour("10.9.9." + i),
                    "伪造的 XFF 地址不得获得任何独立配额桶（否则频控被绕过）");
        }
    }

    // ========== 工具方法 ==========

    private static void bindRequest(String remoteAddr, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static SmsCodeSendReqDTO newSendReqDTO(String mobile, String createIp) {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setScene(SCENE);
        reqDTO.setCreateIp(createIp);
        return reqDTO;
    }

    private static String mobileOf(int index) {
        return "1567000000" + index;
    }
}
