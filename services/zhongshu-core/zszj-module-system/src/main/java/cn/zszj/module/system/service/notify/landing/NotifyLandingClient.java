package cn.zszj.module.system.service.notify.landing;

/**
 * 消息落点的「端」类型（ZS-MSG-003）。
 *
 * <p>同一条站内信在不同端可能有不同落点（Web 路由 vs 移动页面路径），由落点注册条目
 * 按「模板编码 × 端」分别声明；解析时按调用方传入的端取对应描述，取不到即该端未注册落点。
 */
public enum NotifyLandingClient {

    /** Web 管理端（zhongshu-admin-web，路由为 Vue Router 路径） */
    WEB,

    /** 移动端（zhongshu-miniapp，路由为 uni-app 页面路径） */
    MOBILE

}
