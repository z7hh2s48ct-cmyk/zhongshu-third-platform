package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.security.core.LoginUser;

/**
 * WebSocket 握手一次性短时票据 Service 接口（ZS-LOGIN-001.B）。
 *
 * <p>背景：admin-web IM 与 miniapp IM/客服的 WebSocket 握手曾以 {@code ?token=<刷新令牌>} 作凭据
 * （浏览器 WS 无法自定义 Header），依赖 LOGIN-001 的 {@code refresh-token-as-access-token-enabled}
 * 回退放行——转换出的"访问令牌"继承刷新令牌 TTL（default client 达 30 天）、不落库、不可撤销。
 * 本卡以【一次性短时票据】替代该迁移期兼容并删除 gate：登录态 API 换票 → WS 握手带 {@code ?ticket=}
 * → 握手拦截器原子消费（GETDEL）→ 建立会话归属。
 *
 * @author ZS-LOGIN-001.B
 */
public interface OAuth2WsTicketService {

    /**
     * 为当前 SecurityContext 登录用户签发一次性短时握手票据。
     *
     * @return 票据（仅可通过 WS 握手消费，不放行任何 API）
     */
    String issueTicket();

    /**
     * 原子消费票据（GETDEL：重放/并发握手仅一方成功）。
     *
     * @param ticket 握手票据
     * @return 票据对应的登录用户；无效/过期/重放返回 null
     */
    LoginUser consumeTicket(String ticket);

}
