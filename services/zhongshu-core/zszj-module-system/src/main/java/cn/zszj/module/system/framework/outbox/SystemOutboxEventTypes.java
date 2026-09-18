package cn.zszj.module.system.framework.outbox;

/**
 * system 模块 Outbox 事件类型常量（ZS-LOGIN-005.B）。
 *
 * <p>与 {@code AuditEventTypes}（审计事件）语义分层：本类登记<b>领域补偿事件</b>——
 * 由业务事务预写、经 {@code OutboxDispatcherService} 派发至 {@code OutboxEventSink} 幂等重放，
 * 用于跨"业务提交 - 缓存失效"边界的可靠补偿。
 *
 * <p>合同（循 {@code OutboxEventMessage}）：
 * <ul>
 *   <li>{@code event_type} 反向域名 + 域 + 对象 + 动作，Sink 按 {@code supports(eventType)} 分派；</li>
 *   <li>{@code biz_type} 用于 JOB-004 恢复台账按业务类型筛选；</li>
 *   <li>{@code biz_id} 承载业务对象主键字符串化，供幂等去重与人工排查定位。</li>
 * </ul>
 *
 * @author ZS-LOGIN-005.B
 */
public final class SystemOutboxEventTypes {

    private SystemOutboxEventTypes() {
    }

    /**
     * OAuth2 令牌撤销补偿事件：撤销主逻辑内预写，dispatcher 派发至
     * {@link OAuth2TokenRevocationCompensationSink} 幂等重放 {@code markRevoked + delete}。
     *
     * <p>覆盖 {@code .A} 遗留缺陷：Redis 缓存失效失败仅 WARN 无重放；本事件提供可持久恢复的补偿链路。
     */
    public static final String TOKEN_REVOCATION_COMPENSATION =
            "zszj.system.oauth2.token.revocation-compensation";

    /** biz_type：访问令牌（{@code system_oauth2_access_token}）。 */
    public static final String BIZ_TYPE_OAUTH2_ACCESS_TOKEN = "oauth2_access_token";

    /** biz_type：刷新令牌（{@code system_oauth2_refresh_token}）。 */
    public static final String BIZ_TYPE_OAUTH2_REFRESH_TOKEN = "oauth2_refresh_token";

}
