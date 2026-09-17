package cn.zszj.module.system.framework.outbox;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@link SystemOutboxEventTypes#TOKEN_REVOCATION_COMPENSATION} 事件载荷（ZS-LOGIN-005.B）。
 *
 * <p><b>秘密扩散防护</b>：不携带 token 明文串，仅承载 DB 主键 {@code tokenId} + 类型分派 {@code tokenType}
 * + 过期时间 {@code expiresTime}。{@link OAuth2TokenRevocationCompensationSink#deliver} 内按
 * {@code (tokenType, tokenId)} 从 DB 反查原 token 串（DB 是权威源，本就存明文），Outbox 表零秘密扩散
 * （对齐 ZS-SEC-007 日志脱敏原则与 ZS-CFG-001 秘密门禁）。
 *
 * <p>JSON 序列化经 {@code JsonUtils.toJsonString} 落 {@code outbox_event.payload}（text）。
 *
 * @author ZS-LOGIN-005.B
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenRevocationCompensationPayload {

    /** 令牌 DB 主键（{@code system_oauth2_access_token.id} 或 {@code system_oauth2_refresh_token.id}）。 */
    private Long tokenId;

    /** 令牌类型（{@link TokenType#ACCESS} / {@link TokenType#REFRESH}），Sink 按此分派反查表。 */
    private TokenType tokenType;

    /**
     * 凭据到期时间（写入事件时快照，Sink 用于过期识别）。
     * <p>已过期凭据（{@code expiresTime < now}）Sink 静默 skip：Redis key TTL 到期自清理，无需墓碑。
     */
    private LocalDateTime expiresTime;

    /**
     * 令牌类型枚举。
     */
    public enum TokenType {
        /** 访问令牌（{@code system_oauth2_access_token}）。 */
        ACCESS,
        /** 刷新令牌（{@code system_oauth2_refresh_token}）。 */
        REFRESH
    }

}
