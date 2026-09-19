package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.zszj.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2WsTicketRedisDAO;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2WsTicketStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2WsTicketServiceImpl} 的单元测试类（ZS-LOGIN-001.B，r0 处置补充）。
 *
 * <p>覆盖 r0 P1-1/P1-2/P2-8 处置面：①匿名/缺令牌拒绝签发；②密码学随机票据（长度 + 唯一性）；
 * ③签发配额超限拒绝；④消费时绑定令牌权威复核——已撤销（ServiceException）/用户不匹配/查无
 * 令牌 → 拒绝握手；⑤有效 → 放行。
 *
 * @author ZS-LOGIN-001.B
 */
public class OAuth2WsTicketServiceTest {

    private OAuth2WsTicketServiceImpl service;
    private OAuth2WsTicketRedisDAO wsTicketRedisDAO;
    private OAuth2TokenCommonApi oauth2TokenApi;

    @BeforeEach
    public void setUp() {
        wsTicketRedisDAO = mock(OAuth2WsTicketRedisDAO.class);
        oauth2TokenApi = mock(OAuth2TokenCommonApi.class);
        service = new OAuth2WsTicketServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "wsTicketRedisDAO", wsTicketRedisDAO);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "oauth2TokenApi", oauth2TokenApi);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "ttlSeconds", 60);
    }

    @AfterEach
    public void tearDown() {
        // SecurityFrameworkUtils 静态桩由各用例 try-with-resources 自行关闭
    }

    private LoginUser buildLoginUser() {
        return new LoginUser().setId(7L).setUserType(2).setTenantId(1L);
    }

    @Test
    public void testIssueTicket_blankToken_rejected() {
        assertThrows(IllegalStateException.class, () -> service.issueTicket(" "));
    }

    @Test
    public void testIssueTicket_quotaExceeded_rejected() {
        when(wsTicketRedisDAO.tryAcquireIssueQuota(7L)).thenReturn(false);
        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUser).thenReturn(buildLoginUser());
            assertThrows(ServiceException.class, () -> service.issueTicket("at-current"));
        }
    }

    @Test
    public void testIssueTicket_success_bindsAccessToken_cryptoRandom() {
        when(wsTicketRedisDAO.tryAcquireIssueQuota(7L)).thenReturn(true);
        try (MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUser).thenReturn(buildLoginUser());
            String t1 = service.issueTicket("at-current");
            String t2 = service.issueTicket("at-current");
            assertNotNull(t1);
            // r0 P1-1：密码学随机源——32 字节 hex；两次签发不重复
            assertEquals(64, t1.length());
            assertTrue(!t1.equals(t2));
        }
        // 绑定令牌入存储载荷（消费侧权威复核依据）；两次签发均绑定同一令牌
        verify(wsTicketRedisDAO, times(2)).setTicket(anyString(),
                org.mockito.ArgumentMatchers.argThat((OAuth2WsTicketStore s) -> "at-current".equals(s.getAccessToken())),
                any());
    }

    @Test
    public void testConsumeTicket_revokedToken_rejected() {
        // r0 P1-2：取票后登出/撤销 → checkAccessToken 抛 ServiceException → 拒绝握手
        LoginUser loginUser = buildLoginUser();
        when(wsTicketRedisDAO.consumeTicket("t-revoked"))
                .thenReturn(new OAuth2WsTicketStore(loginUser, "at-revoked"));
        when(oauth2TokenApi.checkAccessToken("at-revoked"))
                .thenThrow(new ServiceException(401, "访问令牌不存在"));
        assertNull(service.consumeTicket("t-revoked"));
    }

    @Test
    public void testConsumeTicket_userMismatch_rejected() {
        LoginUser loginUser = buildLoginUser();
        when(wsTicketRedisDAO.consumeTicket("t-mismatch"))
                .thenReturn(new OAuth2WsTicketStore(loginUser, "at-other"));
        OAuth2AccessTokenCheckRespDTO checked = new OAuth2AccessTokenCheckRespDTO();
        checked.setUserId(9999L);
        when(oauth2TokenApi.checkAccessToken("at-other")).thenReturn(checked);
        assertNull(service.consumeTicket("t-mismatch"));
    }

    @Test
    public void testConsumeTicket_valid_allowed() {
        LoginUser loginUser = buildLoginUser();
        when(wsTicketRedisDAO.consumeTicket("t-ok"))
                .thenReturn(new OAuth2WsTicketStore(loginUser, "at-ok"));
        OAuth2AccessTokenCheckRespDTO checked = new OAuth2AccessTokenCheckRespDTO();
        checked.setUserId(7L);
        when(oauth2TokenApi.checkAccessToken("at-ok")).thenReturn(checked);
        LoginUser result = service.consumeTicket("t-ok");
        assertNotNull(result);
        assertEquals(7L, result.getId());
        verify(oauth2TokenApi).checkAccessToken(eq("at-ok"));
    }

}
