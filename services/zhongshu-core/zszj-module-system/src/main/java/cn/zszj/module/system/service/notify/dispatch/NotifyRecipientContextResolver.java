package cn.zszj.module.system.service.notify.dispatch;

/**
 * 收件人上下文解析器（ZS-MSG-001）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「B05 先使用 System 用户，不先建任职关系」。
 * 本接口承载该要求：B05 实现 {@link AdminUserNotifyRecipientContextResolver}（只解析 ADMIN）；
 * 任职路由（按组织/岗位/角色动态解析收件人）归 MSG-001.B（D-09 后追加实现）。
 */
public interface NotifyRecipientContextResolver {

    /**
     * 解析收件人上下文——校验存在/未停用/租户匹配。
     *
     * <p>校验失败返回明确状态（不抛异常），供 {@link NotifyDispatcher} 消费。
     *
     * @param recipient 收件人
     * @return 上下文解析结果（valid=true 或 invalidStatus 非空）
     */
    NotifyRecipientContext resolve(NotifyRecipient recipient);

    /**
     * 是否支持该收件人类型（B05 只支持 ADMIN）。
     */
    boolean supports(NotifyRecipient recipient);

}
