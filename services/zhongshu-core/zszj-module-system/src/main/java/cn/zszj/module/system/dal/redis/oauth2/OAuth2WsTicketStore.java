package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.security.core.LoginUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 握手票据的 Redis 存储载荷（ZS-LOGIN-001.B）。
 *
 * <p>loginUser：WS 会话归属主体快照；accessToken：签发时绑定的当前访问令牌——消费时经
 * {@code OAuth2TokenCommonApi.checkAccessToken} 做 DB 权威复核（r0 P1-2：票据签发后用户
 * 登出/被撤销/TTL 内过期 → 拒绝握手，不脱离会话生命周期）。仅存服务端 Redis（TTL 60s），
 * 不随任何响应/URL 出域。
 *
 * @author ZS-LOGIN-001.B
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2WsTicketStore {

    /** WS 会话归属主体快照 */
    private LoginUser loginUser;

    /** 签发时绑定的当前访问令牌（消费时权威复核；仅存服务端 Redis） */
    private String accessToken;

}
