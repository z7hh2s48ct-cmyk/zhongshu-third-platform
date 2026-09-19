package cn.zszj.module.system.framework.websocket;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.system.service.oauth2.OAuth2WsTicketService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手一次性短时票据拦截器（ZS-LOGIN-001.B）。
 *
 * <p><b>合同</b>：
 * <ol>
 *   <li>握手 URL 携带 {@code ?ticket=<票据>} → 原子消费（GETDEL）→ 写入 attributes 的
 *       {@code LOGIN_USER} 键（与 starter {@code WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER}
 *       同键；system 模块未依赖 websocket starter，故此处以字面量锚定并注释同步）→
 *       后置的 starter {@code LoginUserHandshakeInterceptor} 按透传合同放行；</li>
 *   <li>票据无效/过期/重放（消费返回 null）→ {@code return false} 拒绝握手；</li>
 *   <li><b>无 ticket 参数 → 直接放行</b>：交由既有 SecurityContext 链判定（gate
 *       {@code refresh-token-as-access-token-enabled} 已删除，旧 {@code ?token=<刷新令牌>}
 *       在 gate=false 下无法建立 SecurityContext，自然被后置拦截器拒绝——行为兜底不变）。</li>
 * </ol>
 *
 * <p>{@link Order} 最高优先级：先于 starter 的 {@code LoginUserHandshakeInterceptor} 执行，
 * 保证票据路径在「SecurityContext 无主体即拒绝」的检查之前完成 attributes 写入。
 *
 * @author ZS-LOGIN-001.B
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class OAuth2WsTicketHandshakeInterceptor implements HandshakeInterceptor {

    /** 与 starter WebSocketFrameworkUtils.ATTRIBUTE_LOGIN_USER 同键（system 未依赖该 starter，字面量锚定） */
    private static final String ATTRIBUTE_LOGIN_USER = "LOGIN_USER";

    @Resource
    private OAuth2WsTicketService wsTicketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
        if (ticket == null) {
            return true; // 无票据参数：走既有 SecurityContext 链（本拦截器不介入）
        }
        LoginUser loginUser = wsTicketService.consumeTicket(ticket);
        if (loginUser == null) {
            // r0 P3：不记录 URI（query 含票据本体）；此处票据已消费或不存在，通常已失效
            log.warn("[beforeHandshake][WS 握手票据无效或已被消费（一次性语义），拒绝握手]");
            return false;
        }
        attributes.put(ATTRIBUTE_LOGIN_USER, loginUser);
        log.info("[beforeHandshake][WS 握手票据消费成功 userId={} userType={}]", loginUser.getId(), loginUser.getUserType());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // do nothing
    }

}
