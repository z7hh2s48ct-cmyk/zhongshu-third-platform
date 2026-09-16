package cn.zszj.module.system.service.notify.landing;

/**
 * 落点不可用的原因码（ZS-MSG-003）。
 *
 * <p>与安全拒绝（{@link cn.zszj.module.system.enums.ErrorCodeConstants} 的
 * 403 语义错误码）不同：不可用是「消息本身属于当前用户」前提下，落点无法前往的
 * <strong>业务可呈现状态</strong>，前端按码展示明确提示（未知/关闭模块/失权/端不支持）。
 */
public enum NotifyLandingUnavailable {

    /** 模板编码未注册落点（未知消息类型） */
    NOT_REGISTERED,

    /** 落点归属模块未启用（ModuleCatalog 运行白名单外，如 bpm 关闭） */
    MODULE_DISABLED,

    /** 业务重新授权未通过：旧消息在业务撤权/删除后不得进入详情、下载附件 */
    REVOKED,

    /** 该端未注册落点（如仅声明了 Web 路由，移动端查询） */
    CLIENT_UNSUPPORTED;

}
