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
import java.util.Collections;

import static cn.zszj.module.system.enums.ErrorCodeConstants.SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P1（codex r0）<b>正向补充</b>：当直连 peer 命中「可信代理白名单」时，配额应采信 XFF 派生的真实客户端地址。
 *
 * <p>本类与 {@code SmsCodeServiceImplQuotaIpTest}（默认不信任 XFF、伪造不得获得新桶）互补，
 * 共同证明 P1 修复是「<b>可信代理感知</b>」而非「一刀切忽略 XFF」：
 * <ul>
 *     <li>peer <b>不在</b>白名单（QuotaIpTest）：忽略 XFF，用 peer address 计配额 → 伪造 XFF 无效；</li>
 *     <li>peer <b>在</b>白名单（本类）：请求确实经过可信反代，采信 XFF 最左侧客户端地址计配额 → 真实客户端各自独立频控。</li>
 * </ul>
 *
 * <p>说明：本类引用 {@code SmsCodeProperties#getTrustedProxies()}（P1 新增），故<b>仅在 GREEN 阶段</b>编译运行，
 * RED 阶段不纳入 worktree。
 *
 * @author ZS-LOGIN-004 P1
 */
@Import({SmsCodeServiceImpl.class, SmsCodeSecurityRedisDAO.class})
public class SmsCodeServiceImplTrustedProxyTest extends BaseDbAndRedisUnitTest {

    private static final Integer SCENE = SmsSceneEnum.MEMBER_LOGIN.getScene();

    private static final String TRUSTED_PROXY = "10.0.0.1";

    private static final String REAL_CLIENT = "198.51.100.7";

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
        // 关键：把直连 peer（反代出口地址）列入可信代理白名单
        when(smsCodeProperties.getTrustedProxies()).thenReturn(Collections.singletonList(TRUSTED_PROXY));
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void trustedProxy_honorsXffForQuotaBucket() {
        // 准备：小时配额=2；所有请求都经同一可信反代（peer 固定），XFF 指向同一真实客户端
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()).thenReturn(2);
        when(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()).thenReturn(1000);

        int rejected = 0;
        for (int i = 0; i < 3; i++) {
            bindRequest(TRUSTED_PROXY, REAL_CLIENT);
            try {
                smsCodeService.sendSmsCode(newSendReqDTO(mobileOf(i), TRUSTED_PROXY));
            } catch (ServiceException ex) {
                if (SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP.getCode().equals(ex.getCode())) {
                    rejected++;
                } else {
                    throw ex;
                }
            }
        }

        // 断言：经可信代理时，配额按 XFF 派生的真实客户端计——第 3 次超限被拒
        assertEquals(1, rejected, "可信代理后同一真实客户端超过小时配额应被拒；实际被拒=" + rejected);
        assertEquals(2, smsCodeSecurityRedisDAO.getIpSendCountPerHour(REAL_CLIENT),
                "配额桶应落在 XFF 派生的真实客户端地址上");
        assertEquals(0, smsCodeSecurityRedisDAO.getIpSendCountPerHour(TRUSTED_PROXY),
                "可信代理自身的 peer address 不应作为配额键（否则所有客户端共享一个桶）");
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
        return "1560400000" + index;
    }

}
