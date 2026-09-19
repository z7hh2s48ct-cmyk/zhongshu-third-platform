package cn.zszj.module.system.framework.websocket;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.system.service.oauth2.OAuth2WsTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2WsTicketHandshakeInterceptor} 握手拦截器的单元测试类（ZS-LOGIN-001.B）。
 *
 * <p>合同：①带 {@code ?ticket=} 且票据有效 → 放行 + LOGIN_USER 写入 attributes（与 starter
 * {@code WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER} 同键）；②票据无效/过期/重放 → 拒绝握手；
 * ③无 ticket 参数 → 放行透传（交由既有 SecurityContext 链，不介入 attributes）。
 *
 * @author ZS-LOGIN-001.B
 */
public class OAuth2WsTicketHandshakeInterceptorTest {

    /** 与 starter WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER 同键（字面量锚定口径一致） */
    private static final String ATTRIBUTE_LOGIN_USER = "LOGIN_USER";

    private OAuth2WsTicketHandshakeInterceptor interceptor;
    private OAuth2WsTicketService wsTicketService;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;

    /** 握手结果：allowed + attributes 透传 */
    private record HandshakeResult(boolean allowed, Map<String, Object> attributes) {
    }

    @BeforeEach
    public void setUp() {
        wsTicketService = mock(OAuth2WsTicketService.class);
        interceptor = new OAuth2WsTicketHandshakeInterceptor();
        org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "wsTicketService", wsTicketService);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
    }

    private HandshakeResult handshake(String uri) {
        Mockito.when(request.getURI()).thenReturn(URI.create(uri));
        Map<String, Object> attributes = new HashMap<>();
        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, attributes);
        return new HandshakeResult(allowed, attributes);
    }

    @Test
    public void testValidTicket_attributesSetAndAllowed() {
        LoginUser loginUser = new LoginUser().setId(7L).setUserType(2);
        when(wsTicketService.consumeTicket("t-valid")).thenReturn(loginUser);
        HandshakeResult result = handshake("ws://localhost/infra/ws?ticket=t-valid");
        assertTrue(result.allowed());
        assertSame(loginUser, result.attributes().get(ATTRIBUTE_LOGIN_USER));
    }

    @Test
    public void testInvalidTicket_handshakeRejected() {
        when(wsTicketService.consumeTicket("t-bad")).thenReturn(null);
        HandshakeResult result = handshake("ws://localhost/infra/ws?ticket=t-bad");
        assertTrue(!result.allowed());
        assertNull(result.attributes().get(ATTRIBUTE_LOGIN_USER));
    }

    @Test
    public void testBlankTicket_handshakeRejected() {
        when(wsTicketService.consumeTicket("")).thenReturn(null);
        HandshakeResult result = handshake("ws://localhost/infra/ws?ticket=");
        assertTrue(!result.allowed());
    }

    @Test
    public void testNoTicketParam_passthroughAllowed() {
        // 无 ticket 参数：本拦截器不介入（不消费票据、不写 attributes），交由既有 SecurityContext 链
        HandshakeResult result = handshake("ws://localhost/infra/ws");
        assertTrue(result.allowed());
        assertNull(result.attributes().get(ATTRIBUTE_LOGIN_USER));
        verifyNoInteractions(wsTicketService);
    }

    @Test
    public void testOrder_highestPrecedence_beforeLoginUserHandshake() {
        // 保证先于 starter LoginUserHandshakeInterceptor 执行（否则「SecurityContext 无主体即拒绝」先到）
        Order order = OAuth2WsTicketHandshakeInterceptor.class.getAnnotation(Order.class);
        assertNotNull(order, "拦截器必须标注 @Order（保证先于 LoginUserHandshakeInterceptor）");
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());
    }

}
