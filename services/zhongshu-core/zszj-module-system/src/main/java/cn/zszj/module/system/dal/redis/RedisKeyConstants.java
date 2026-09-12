package cn.zszj.module.system.dal.redis;

import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

/**
 * System Redis Key 枚举类
 *
 * @author 芋道源码
 */
public interface RedisKeyConstants {

    /**
     * 指定部门的所有子部门编号数组的缓存
     * <p>
     * KEY 格式：dept_children_ids:{id}
     * VALUE 数据类型：String 子部门编号集合
     */
    String DEPT_CHILDREN_ID_LIST = "dept_children_ids";

    /**
     * 角色的缓存
     * <p>
     * KEY 格式：role:{id}
     * VALUE 数据类型：String 角色信息
     */
    String ROLE = "role";

    /**
     * 用户拥有的角色编号的缓存
     * <p>
     * KEY 格式：user_role_ids:{userId}
     * VALUE 数据类型：String 角色编号集合
     */
    String USER_ROLE_ID_LIST = "user_role_ids";

    /**
     * 拥有指定菜单的角色编号数组的缓存
     * <p>
     * KEY 格式：user_role_ids:{menuId}
     * VALUE 数据类型：String 角色编号集合
     */
    String MENU_ROLE_ID_LIST = "menu_role_ids";

    /**
     * 拥有权限对应的菜单编号数组的缓存
     * <p>
     * KEY 格式：permission_menu_ids:{permission}
     * VALUE 数据类型：String 菜单编号数组
     */
    String PERMISSION_MENU_ID_LIST = "permission_menu_ids";

    /**
     * OAuth2 客户端的缓存
     * <p>
     * KEY 格式：oauth_client:{id}
     * VALUE 数据类型：String 客户端信息
     */
    String OAUTH_CLIENT = "oauth_client";

    /**
     * 访问令牌的缓存
     * <p>
     * KEY 格式：oauth2_access_token:{token}
     * VALUE 数据类型：String 访问令牌信息 {@link OAuth2AccessTokenDO}
     * <p>
     * 由于动态过期时间，使用 RedisTemplate 操作
     */
    String OAUTH2_ACCESS_TOKEN = "oauth2_access_token:%s";

    /**
     * ZS-LOGIN-002：会话代际号（以刷新令牌为会话标识；本底座刷新沿用原 refresh token 不轮换）
     * <p>
     * KEY 格式：oauth2_refresh_session_generation:{refreshToken}
     * VALUE 数据类型：String 代际号（Long，从 1 开始随每次成功刷新单调递增）
     * <p>
     * 用途：① 为每次刷新提供可定位的会话代际标识，便于审计；② 作为并发串行化的「无丢失更新」证据
     * （代际号 == 成功刷新次数）。刻意存 Redis 而非新增 DB 列，避免 Flyway 迁移与 ZS-DB-019.B 的 PG 回归耦合。
     */
    String OAUTH2_REFRESH_SESSION_GENERATION = "oauth2_refresh_session_generation:%s";

    /**
     * ZS-LOGIN-002：访问令牌所属的会话代际号
     * <p>
     * KEY 格式：oauth2_access_session_generation:{accessToken}
     * VALUE 数据类型：String 代际号（Long）
     * <p>
     * 用途：把某个访问令牌反查定位到「第几代会话」，用于重放辨识与审计。旧代际令牌被刷新取代后，
     * 本标识仍保留至自然过期（TTL 取访问令牌有效期）。
     */
    String OAUTH2_ACCESS_SESSION_GENERATION = "oauth2_access_session_generation:%s";

    /**
     * 站内信模版的缓存
     * <p>
     * KEY 格式：notify_template:{code}
     * VALUE 数据格式：String 模版信息
     */
    String NOTIFY_TEMPLATE = "notify_template";

    /**
     * 邮件账号的缓存
     * <p>
     * KEY 格式：mail_account:{id}
     * VALUE 数据格式：String 账号信息
     */
    String MAIL_ACCOUNT = "mail_account";

    /**
     * 邮件模版的缓存
     * <p>
     * KEY 格式：mail_template:{id}
     * VALUE 数据格式：String 模版信息
     */
    String MAIL_TEMPLATE = "mail_template";

    /**
     * 短信模版的缓存
     * <p>
     * KEY 格式：sms_template:{id}
     * VALUE 数据格式：String 模版信息
     */
    String SMS_TEMPLATE = "sms_template";

    /**
     * 小程序订阅模版的缓存
     *
     * KEY 格式：wxa_subscribe_template:{userType}
     * VALUE 数据格式 String, 模版信息
     */
    String WXA_SUBSCRIBE_TEMPLATE = "wxa_subscribe_template";

}
