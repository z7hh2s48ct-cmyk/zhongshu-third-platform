package cn.zszj.module.system.framework.outbox;

import lombok.Value;

import java.time.LocalDateTime;

/**
 * {@link OAuth2TokenRevocationCompensationSink} 反查 DB 得到的令牌行快照（ZS-LOGIN-005.B）。
 *
 * <p>由 {@code selectByIdIncludeDeleted} 专用查询返回——绕过 MyBatis Plus 逻辑删除过滤，
 * 同时携带 {@code deleted} 标志供 Sink 判定重放策略：
 * <ul>
 *   <li>{@code deleted == TRUE} → 撤销已生效，重放 {@code markRevoked + delete}（幂等）；</li>
 *   <li>{@code deleted == FALSE} 且 {@code expiresTime > now} → 业务事务已回滚（token 仍活跃），
 *       <b>静默 skip 不重放</b>（防误撤销有效凭据）；</li>
 *   <li>{@code null} 快照 → 行已物理清理，Sink 静默 skip。</li>
 * </ul>
 *
 * <p>{@code deleted} 类型对齐 {@code BaseDO#deleted}（{@link Boolean}），H2 {@code bit} / PG {@code int2}
 * 由 MyBatis 布尔类型处理器统一映射，方言可移植。
 *
 * @author ZS-LOGIN-005.B
 */
@Value
public class TokenRowSnapshot {

    /** 原令牌串（access_token 或 refresh_token 列的明文值）。 */
    String token;

    /** 逻辑删除标志：{@code FALSE}=活跃、{@code TRUE}=已删除（撤销已生效）。 */
    Boolean deleted;

    /** 到期时间（DB 侧权威值，非事件载荷快照）。 */
    LocalDateTime expiresTime;

}
