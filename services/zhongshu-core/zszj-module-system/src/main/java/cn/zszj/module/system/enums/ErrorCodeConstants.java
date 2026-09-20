package cn.zszj.module.system.enums;

import cn.zszj.framework.common.exception.ErrorCode;

/**
 * System 错误码枚举类
 *
 * system 系统，使用 1-002-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== AUTH 模块 1-002-000-000 ==========
    ErrorCode AUTH_LOGIN_BAD_CREDENTIALS = new ErrorCode(1_002_000_000, "登录失败，账号密码不正确");
    ErrorCode AUTH_LOGIN_USER_DISABLED = new ErrorCode(1_002_000_001, "登录失败，账号被禁用");
    ErrorCode AUTH_LOGIN_CAPTCHA_CODE_ERROR = new ErrorCode(1_002_000_004, "验证码不正确，原因：{}");
    ErrorCode AUTH_THIRD_LOGIN_NOT_BIND = new ErrorCode(1_002_000_005, "未绑定账号，需要进行绑定");
    ErrorCode AUTH_MOBILE_NOT_EXISTS = new ErrorCode(1_002_000_007, "手机号不存在");
    ErrorCode AUTH_REGISTER_CAPTCHA_CODE_ERROR = new ErrorCode(1_002_000_008, "验证码不正确，原因：{}");
    /**
     * ZS-LOGIN-004：登录方式门控。一期只获准「账号密码」这一种技术登录方式，短信登录 / 社交登录 / 自助注册 /
     * 重置密码默认关闭，须经 {@code zszj.security.login-mode.*} 显式开启（生产模板默认 false）。
     * 门控落在 Service 层而非 Controller，故绕过 HTTP 直调 Service 同样被拒。
     */
    ErrorCode AUTH_LOGIN_MODE_DISABLED = new ErrorCode(1_002_000_009, "登录方式({})未开启，请使用账号密码登录");

    // ========== 菜单模块 1-002-001-000 ==========
    ErrorCode MENU_NAME_DUPLICATE = new ErrorCode(1_002_001_000, "已经存在该名字的菜单");
    ErrorCode MENU_PARENT_NOT_EXISTS = new ErrorCode(1_002_001_001, "父菜单不存在");
    ErrorCode MENU_PARENT_ERROR = new ErrorCode(1_002_001_002, "不能设置自己为父菜单");
    ErrorCode MENU_NOT_EXISTS = new ErrorCode(1_002_001_003, "菜单不存在");
    ErrorCode MENU_EXISTS_CHILDREN = new ErrorCode(1_002_001_004, "存在子菜单，无法删除");
    ErrorCode MENU_PARENT_NOT_DIR_OR_MENU = new ErrorCode(1_002_001_005, "父菜单的类型必须是目录或者菜单");
    ErrorCode MENU_COMPONENT_NAME_DUPLICATE = new ErrorCode(1_002_001_006, "已经存在该组件名的菜单");

    // ========== 角色模块 1-002-002-000 ==========
    ErrorCode ROLE_NOT_EXISTS = new ErrorCode(1_002_002_000, "角色不存在");
    ErrorCode ROLE_NAME_DUPLICATE = new ErrorCode(1_002_002_001, "已经存在名为【{}】的角色");
    ErrorCode ROLE_CODE_DUPLICATE = new ErrorCode(1_002_002_002, "已经存在标识为【{}】的角色");
    ErrorCode ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE = new ErrorCode(1_002_002_003, "不能操作类型为系统内置的角色");
    ErrorCode ROLE_IS_DISABLE = new ErrorCode(1_002_002_004, "名字为【{}】的角色已被禁用");
    ErrorCode ROLE_ADMIN_CODE_ERROR = new ErrorCode(1_002_002_005, "标识【{}】不能使用");

    // ========== 用户模块 1-002-003-000 ==========
    ErrorCode USER_USERNAME_EXISTS = new ErrorCode(1_002_003_000, "用户账号已经存在");
    ErrorCode USER_MOBILE_EXISTS = new ErrorCode(1_002_003_001, "手机号已经存在");
    ErrorCode USER_EMAIL_EXISTS = new ErrorCode(1_002_003_002, "邮箱已经存在");
    ErrorCode USER_NOT_EXISTS = new ErrorCode(1_002_003_003, "用户不存在");
    ErrorCode USER_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_002_003_004, "导入用户数据不能为空！");
    ErrorCode USER_PASSWORD_FAILED = new ErrorCode(1_002_003_005, "用户密码校验失败");
    ErrorCode USER_IS_DISABLE = new ErrorCode(1_002_003_006, "名字为【{}】的用户已被禁用");
    ErrorCode USER_IS_DEPT_LEADER = new ErrorCode(1_002_003_007, "用户是部门负责人，请先变更负责人再删除");
    ErrorCode USER_COUNT_MAX = new ErrorCode(1_002_003_008, "创建用户失败，原因：超过租户最大租户配额({})！");
    ErrorCode USER_IMPORT_INIT_PASSWORD = new ErrorCode(1_002_003_009, "初始密码不能为空");
    ErrorCode USER_MOBILE_NOT_EXISTS = new ErrorCode(1_002_003_010, "该手机号尚未注册");
    ErrorCode USER_REGISTER_DISABLED = new ErrorCode(1_002_003_011, "注册功能已关闭");

    // ========== 部门模块 1-002-004-000 ==========
    ErrorCode DEPT_NAME_DUPLICATE = new ErrorCode(1_002_004_000, "已经存在该名字的部门");
    ErrorCode DEPT_PARENT_NOT_EXITS = new ErrorCode(1_002_004_001,"父级部门不存在");
    ErrorCode DEPT_NOT_FOUND = new ErrorCode(1_002_004_002, "当前部门不存在");
    ErrorCode DEPT_EXITS_CHILDREN = new ErrorCode(1_002_004_003, "存在子部门，无法删除");
    ErrorCode DEPT_PARENT_ERROR = new ErrorCode(1_002_004_004, "不能设置自己为父部门");
    ErrorCode DEPT_EXITS_USERS = new ErrorCode(1_002_004_005, "存在成员，无法删除");
    ErrorCode DEPT_NOT_ENABLE = new ErrorCode(1_002_004_006, "部门({})不处于开启状态，不允许选择");
    ErrorCode DEPT_PARENT_IS_CHILD = new ErrorCode(1_002_004_007, "不能设置自己的子部门为父部门");

    // ========== 岗位模块 1-002-005-000 ==========
    ErrorCode POST_NOT_FOUND = new ErrorCode(1_002_005_000, "当前岗位不存在");
    ErrorCode POST_NOT_ENABLE = new ErrorCode(1_002_005_001, "岗位({}) 不处于开启状态，不允许选择");
    ErrorCode POST_NAME_DUPLICATE = new ErrorCode(1_002_005_002, "已经存在该名字的岗位");
    ErrorCode POST_CODE_DUPLICATE = new ErrorCode(1_002_005_003, "已经存在该标识的岗位");
    ErrorCode POST_EXITS_USERS = new ErrorCode(1_002_005_004, "存在用户引用，无法删除");

    // ========== 字典类型 1-002-006-000 ==========
    ErrorCode DICT_TYPE_NOT_EXISTS = new ErrorCode(1_002_006_001, "当前字典类型不存在");
    ErrorCode DICT_TYPE_NOT_ENABLE = new ErrorCode(1_002_006_002, "字典类型不处于开启状态，不允许选择");
    ErrorCode DICT_TYPE_NAME_DUPLICATE = new ErrorCode(1_002_006_003, "已经存在该名字的字典类型");
    ErrorCode DICT_TYPE_TYPE_DUPLICATE = new ErrorCode(1_002_006_004, "已经存在该类型的字典类型");
    ErrorCode DICT_TYPE_HAS_CHILDREN = new ErrorCode(1_002_006_005, "无法删除，该字典类型还有字典数据");
    ErrorCode DICT_TYPE_HAS_CHILDREN_ON_TYPE_CHANGE = new ErrorCode(1_002_006_006, "无法修改字典类型编码，该类型下还有字典数据");

    // ========== 字典数据 1-002-007-000 ==========
    ErrorCode DICT_DATA_NOT_EXISTS = new ErrorCode(1_002_007_001, "当前字典数据不存在");
    ErrorCode DICT_DATA_NOT_ENABLE = new ErrorCode(1_002_007_002, "字典数据({})不处于开启状态，不允许选择");
    ErrorCode DICT_DATA_VALUE_DUPLICATE = new ErrorCode(1_002_007_003, "已经存在该值的字典数据");

    // ========== 通知公告 1-002-008-000 ==========
    ErrorCode NOTICE_NOT_FOUND = new ErrorCode(1_002_008_001, "当前通知公告不存在");

    // ========== 权限分配校验 1-002-009-000 ==========
    ErrorCode PERMISSION_ASSIGN_USER_OTHER_TENANT = new ErrorCode(1_002_009_000, "被授权用户({})不属于当前租户，拒绝分配");
    ErrorCode PERMISSION_ASSIGN_ROLE_OTHER_TENANT = new ErrorCode(1_002_009_001, "被授权角色({})不属于当前租户，拒绝分配");
    ErrorCode PERMISSION_ASSIGN_DEPT_OTHER_TENANT = new ErrorCode(1_002_009_002, "数据权限部门({})不属于当前租户，拒绝分配");
    ErrorCode PERMISSION_GRANT_EXCEED_CEILING = new ErrorCode(1_002_009_003, "超出可授予权限上限，非超级管理员不能授予超级管理员等特权角色");
    ErrorCode PERMISSION_SELF_ELEVATION = new ErrorCode(1_002_009_004, "禁止为当前登录用户自身新增角色，避免自我提权");

    // ========== 业务审计 1-002-010-000 ==========
    /**
     * ZS-AUDIT-001：统一业务审计 fail-closed——eventType/actorType/result 必填，缺失即拒绝写入并中止当前事务，
     * 杜绝「无主体的模糊审计」；detail 序列化失败、DB 写入失败各有其码，供调用方区分处置。
     */
    ErrorCode AUDIT_EVENT_FIELD_MISSING = new ErrorCode(1_002_010_000, "审计事件缺少必填字段({})，拒绝写入");
    ErrorCode AUDIT_EVENT_DETAIL_SERIALIZE_FAILED = new ErrorCode(1_002_010_001, "审计事件明细序列化失败");
    ErrorCode AUDIT_EVENT_WRITE_FAILED = new ErrorCode(1_002_010_002, "审计事件写入失败");
    ErrorCode AUDIT_EVENT_CLEAN_FAILED = new ErrorCode(1_002_010_003, "审计事件清理失败：{}");

    // ========== 短信渠道 1-002-011-000 ==========
    ErrorCode SMS_CHANNEL_NOT_EXISTS = new ErrorCode(1_002_011_000, "短信渠道不存在");
    ErrorCode SMS_CHANNEL_DISABLE = new ErrorCode(1_002_011_001, "短信渠道不处于开启状态，不允许选择");
    ErrorCode SMS_CHANNEL_HAS_CHILDREN = new ErrorCode(1_002_011_002, "无法删除，该短信渠道还有短信模板");

    // ========== 短信模板 1-002-012-000 ==========
    ErrorCode SMS_TEMPLATE_NOT_EXISTS = new ErrorCode(1_002_012_000, "短信模板不存在");
    ErrorCode SMS_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_002_012_001, "已经存在编码为【{}】的短信模板");
    ErrorCode SMS_TEMPLATE_API_ERROR = new ErrorCode(1_002_012_002, "短信 API 模板调用失败，原因是：{}");
    ErrorCode SMS_TEMPLATE_API_AUDIT_CHECKING = new ErrorCode(1_002_012_003, "短信 API 模版无法使用，原因：审批中");
    ErrorCode SMS_TEMPLATE_API_AUDIT_FAIL = new ErrorCode(1_002_012_004, "短信 API 模版无法使用，原因：审批不通过，{}");
    ErrorCode SMS_TEMPLATE_API_NOT_FOUND = new ErrorCode(1_002_012_005, "短信 API 模版无法使用，原因：模版不存在");

    // ========== 短信发送 1-002-013-000 ==========
    ErrorCode SMS_SEND_MOBILE_NOT_EXISTS = new ErrorCode(1_002_013_000, "手机号不存在");
    ErrorCode SMS_SEND_MOBILE_TEMPLATE_PARAM_MISS = new ErrorCode(1_002_013_001, "模板参数({})缺失");
    ErrorCode SMS_SEND_TEMPLATE_NOT_EXISTS = new ErrorCode(1_002_013_002, "短信模板不存在");

    // ========== 短信验证码 1-002-014-000 ==========
    ErrorCode SMS_CODE_NOT_FOUND = new ErrorCode(1_002_014_000, "验证码不存在");
    ErrorCode SMS_CODE_EXPIRED = new ErrorCode(1_002_014_001, "验证码已过期");
    ErrorCode SMS_CODE_USED = new ErrorCode(1_002_014_002, "验证码已使用");
    ErrorCode SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY = new ErrorCode(1_002_014_004, "超过每日短信发送数量");
    ErrorCode SMS_CODE_SEND_TOO_FAST = new ErrorCode(1_002_014_005, "短信发送过于频繁");
    // ZS-LOGIN-004：验证码暴力破解防护（尝试次数上限）+ 每 IP 频控 + 通道未就绪不得假报发送成功
    ErrorCode SMS_CODE_EXCEED_ATTEMPT_LIMIT = new ErrorCode(1_002_014_006, "验证码错误尝试次数过多，请在 {} 秒后重新获取");
    ErrorCode SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP = new ErrorCode(1_002_014_007, "超过该 IP 的短信发送数量上限");
    ErrorCode SMS_CODE_SEND_CHANNEL_NOT_READY = new ErrorCode(1_002_014_008, "短信通道未就绪（模板或渠道不存在/已禁用），验证码未发送");

    // ========== 租户信息 1-002-015-000 ==========
    ErrorCode TENANT_NOT_EXISTS = new ErrorCode(1_002_015_000, "租户不存在");
    ErrorCode TENANT_DISABLE = new ErrorCode(1_002_015_001, "名字为【{}】的租户已被禁用");
    ErrorCode TENANT_EXPIRE = new ErrorCode(1_002_015_002, "名字为【{}】的租户已过期");
    ErrorCode TENANT_CAN_NOT_UPDATE_SYSTEM = new ErrorCode(1_002_015_003, "系统租户不能进行修改、删除等操作！");
    ErrorCode TENANT_NAME_DUPLICATE = new ErrorCode(1_002_015_004, "名字为【{}】的租户已存在");
    ErrorCode TENANT_WEBSITE_DUPLICATE = new ErrorCode(1_002_015_005, "域名为【{}】的租户已存在");

    // ========== 租户套餐 1-002-016-000 ==========
    ErrorCode TENANT_PACKAGE_NOT_EXISTS = new ErrorCode(1_002_016_000, "租户套餐不存在");
    ErrorCode TENANT_PACKAGE_USED = new ErrorCode(1_002_016_001, "租户正在使用该套餐，请给租户重新设置套餐后再尝试删除");
    ErrorCode TENANT_PACKAGE_DISABLE = new ErrorCode(1_002_016_002, "名字为【{}】的租户套餐已被禁用");
    ErrorCode TENANT_PACKAGE_NAME_DUPLICATE = new ErrorCode(1_002_016_003, "已经存在该名字的租户套餐");
    ErrorCode TENANT_PACKAGE_MENU_MODULE_DISABLED = new ErrorCode(1_002_016_004, "菜单【{}】属于未启用模块（{}），套餐不能启用关闭模块的功能");
    // ZS-CFG-003.B：套餐/角色权限交集——授权入口的服务端重检
    ErrorCode TENANT_PACKAGE_MENU_EXCEED = new ErrorCode(1_002_016_005, "菜单【{}】超出租户套餐【{}】的许可范围，不能授予角色");

    // ========== 社交用户 1-002-018-000 ==========
    ErrorCode SOCIAL_USER_AUTH_FAILURE = new ErrorCode(1_002_018_000, "社交授权失败，原因是：{}");
    ErrorCode SOCIAL_USER_NOT_FOUND = new ErrorCode(1_002_018_001, "社交授权失败，找不到对应的用户");

    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR = new ErrorCode(1_002_018_200, "获得手机号失败");
    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_QRCODE_ERROR = new ErrorCode(1_002_018_201, "获得小程序码失败");
    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_SUBSCRIBE_TEMPLATE_ERROR = new ErrorCode(1_002_018_202, "获得小程序订阅消息模版失败");
    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_SUBSCRIBE_MESSAGE_ERROR = new ErrorCode(1_002_018_203, "发送小程序订阅消息失败");
    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_ORDER_UPLOAD_SHIPPING_INFO_ERROR = new ErrorCode(1_002_018_204, "上传微信小程序发货信息失败");
    ErrorCode SOCIAL_CLIENT_WEIXIN_MINI_APP_ORDER_NOTIFY_CONFIRM_RECEIVE_ERROR = new ErrorCode(1_002_018_205, "上传微信小程序订单收货信息失败");
    ErrorCode SOCIAL_CLIENT_NOT_EXISTS = new ErrorCode(1_002_018_210, "社交客户端不存在");
    ErrorCode SOCIAL_CLIENT_UNIQUE = new ErrorCode(1_002_018_211, "社交客户端已存在配置");

    // ========== OAuth2 客户端 1-002-020-000 =========
    ErrorCode OAUTH2_CLIENT_NOT_EXISTS = new ErrorCode(1_002_020_000, "OAuth2 客户端不存在");
    ErrorCode OAUTH2_CLIENT_EXISTS = new ErrorCode(1_002_020_001, "OAuth2 客户端编号已存在");
    ErrorCode OAUTH2_CLIENT_DISABLE = new ErrorCode(1_002_020_002, "OAuth2 客户端已禁用");
    ErrorCode OAUTH2_CLIENT_AUTHORIZED_GRANT_TYPE_NOT_EXISTS = new ErrorCode(1_002_020_003, "不支持该授权类型");
    ErrorCode OAUTH2_CLIENT_SCOPE_OVER = new ErrorCode(1_002_020_004, "授权范围过大");
    ErrorCode OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH = new ErrorCode(1_002_020_005, "无效 redirect_uri: {}");
    ErrorCode OAUTH2_CLIENT_CLIENT_SECRET_ERROR = new ErrorCode(1_002_020_006, "无效 client_secret: {}");

    // ========== OAuth2 授权 1-002-021-000 =========
    ErrorCode OAUTH2_GRANT_CLIENT_ID_MISMATCH = new ErrorCode(1_002_021_000, "client_id 不匹配");
    ErrorCode OAUTH2_GRANT_REDIRECT_URI_MISMATCH = new ErrorCode(1_002_021_001, "redirect_uri 不匹配");
    ErrorCode OAUTH2_GRANT_STATE_MISMATCH = new ErrorCode(1_002_021_002, "state 不匹配");

    // ========== OAuth2 授权 1-002-022-000 =========
    ErrorCode OAUTH2_CODE_NOT_EXISTS = new ErrorCode(1_002_022_000, "code 不存在");
    ErrorCode OAUTH2_CODE_EXPIRE = new ErrorCode(1_002_022_001, "code 已过期");

    // ========== 邮箱账号 1-002-023-000 ==========
    ErrorCode MAIL_ACCOUNT_NOT_EXISTS = new ErrorCode(1_002_023_000, "邮箱账号不存在");
    ErrorCode MAIL_ACCOUNT_RELATE_TEMPLATE_EXISTS = new ErrorCode(1_002_023_001, "无法删除，该邮箱账号还有邮件模板");

    // ========== 邮件模版 1-002-024-000 ==========
    ErrorCode MAIL_TEMPLATE_NOT_EXISTS = new ErrorCode(1_002_024_000, "邮件模版不存在");
    ErrorCode MAIL_TEMPLATE_CODE_EXISTS = new ErrorCode(1_002_024_001, "邮件模版 code({}) 已存在");

    // ========== 邮件发送 1-002-025-000 ==========
    ErrorCode MAIL_SEND_TEMPLATE_PARAM_MISS = new ErrorCode(1_002_025_000, "模板参数({})缺失");
    ErrorCode MAIL_SEND_MAIL_NOT_EXISTS = new ErrorCode(1_002_025_001, "邮箱不存在");

    // ========== 站内信模版 1-002-026-000 ==========
    ErrorCode NOTIFY_TEMPLATE_NOT_EXISTS = new ErrorCode(1_002_026_000, "站内信模版不存在");
    ErrorCode NOTIFY_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_002_026_001, "已经存在编码为【{}】的站内信模板");

    // ========== 站内信模版 1-002-027-000 ==========

    // ========== 站内信发送 1-002-028-000 ==========
    ErrorCode NOTIFY_SEND_TEMPLATE_PARAM_MISS = new ErrorCode(1_002_028_000, "模板参数({})缺失");


    // ========== 通知派发 1-002-028-001（ZS-MSG-001） ==========
    ErrorCode NOTIFY_DISPATCH_EVENT_ID_REQUIRED = new ErrorCode(1_002_028_001, "通知派发缺少 eventId（幂等键），拒绝写入");
    ErrorCode NOTIFY_DISPATCH_RECIPIENTS_REQUIRED = new ErrorCode(1_002_028_002, "通知派发缺少收件人，拒绝写入");
    ErrorCode NOTIFY_DISPATCH_WRITE_FAILED = new ErrorCode(1_002_028_003, "通知发送日志写入失败");
    ErrorCode NOTIFY_DISPATCH_TENANT_REQUIRED = new ErrorCode(1_002_028_004, "通知派发缺少租户上下文，拒绝写入");
    ErrorCode NOTIFY_DISPATCH_ACTOR_TYPE_REQUIRED = new ErrorCode(1_002_028_005, "通知派发缺少 actorType（事件主体类型），拒绝写入");

    // ========== OAuth2 令牌会话管理 1-002-029-000 ==========
    ErrorCode OAUTH2_TOKEN_SESSION_NOT_OWNED = new ErrorCode(1_002_029_000, "无法操作他人的登录会话");
    // ZS-LOGIN-006 codex r0 P1：自助会话管理要求真实用户登录态；client_credentials 机器令牌（userId=0）
    // 没有「本人会话」语义，拒绝其列出 / 撤销同为 userId=0 的其它客户端会话（跨客户端越权）
    ErrorCode OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER = new ErrorCode(1_002_029_001, "自助会话管理要求真实用户登录态");


    // ========== 业务待办 1-002-030-000（ZS-MSG-002） ==========
    ErrorCode NOTIFY_TODO_KEY_REQUIRED = new ErrorCode(1_002_030_001, "业务待办缺少稳定任务 ID（todoKey），拒绝写入");
    ErrorCode NOTIFY_TODO_RECIPIENT_REQUIRED = new ErrorCode(1_002_030_002, "业务待办缺少收件人，拒绝写入");
    ErrorCode NOTIFY_TODO_EVENT_ID_REQUIRED = new ErrorCode(1_002_030_003, "业务待办流转缺少 eventId（幂等键），拒绝处理");
    ErrorCode NOTIFY_TODO_NOT_FOUND = new ErrorCode(1_002_030_004, "业务待办不存在");
    ErrorCode NOTIFY_TODO_WRITE_FAILED = new ErrorCode(1_002_030_005, "业务待办写入失败");
    ErrorCode NOTIFY_TODO_TENANT_REQUIRED = new ErrorCode(1_002_030_006, "业务待办缺少租户上下文，拒绝写入");
    ErrorCode NOTIFY_TODO_FIELD_REQUIRED = new ErrorCode(1_002_030_007, "业务待办缺少必填字段：{}");

    // ========== 消息落点 1-002-031-000（ZS-MSG-003） ==========
    // r0-P3/r1-P3/r2-P3：NOT_FOUND 与 ACCESS_DENIED 统一对外文案，但错误码本身仍可区分
    // （异常处理器原样输出 code），消息 ID 存在性探测的残余风险经评审接受；双码保留用于
    // 内部日志区分与本卡验收证据（他人消息显式拒绝），不得据此宣称探测面已闭合
    ErrorCode NOTIFY_LANDING_MESSAGE_NOT_FOUND = new ErrorCode(1_002_031_000, "站内信不存在或不可访问");
    ErrorCode NOTIFY_LANDING_ACCESS_DENIED = new ErrorCode(1_002_031_001, "站内信不存在或不可访问");
    ErrorCode NOTIFY_LANDING_TENANT_REQUIRED = new ErrorCode(1_002_031_002, "缺少租户上下文，拒绝解析消息落点");

    // ========== 渠道发送生命周期 1-002-032-000（ZS-MSG-004；原拟 1-002-031 与 MSG-003 落点段撞段，改 032） ==========
    ErrorCode NOTIFY_CHANNEL_SEND_NOT_FOUND = new ErrorCode(1_002_032_001, "渠道发送台账记录不存在");
    ErrorCode NOTIFY_CHANNEL_SEND_TENANT_REQUIRED = new ErrorCode(1_002_032_002, "渠道发送处理缺少租户上下文，拒绝执行");
    ErrorCode NOTIFY_CHANNEL_SEND_MANUAL_RETRY_INVALID = new ErrorCode(1_002_032_003, "当前状态({})不允许人工重试（受理/送达状态由回执与回查推进）");
    ErrorCode NOTIFY_CHANNEL_SEND_CONTACT_STILL_MISSING = new ErrorCode(1_002_032_004, "收件人仍缺少该渠道联系方式，人工重试拒绝；请先补齐联系方式后重试");

    // ========== 组织 1-002-033-000（ZS-IAM-002：D-09 FND-IAM-002 组织类型与层级） ==========
    ErrorCode ORGANIZATION_NOT_EXISTS = new ErrorCode(1_002_033_000, "组织不存在");
    ErrorCode ORGANIZATION_CODE_DUPLICATE = new ErrorCode(1_002_033_001, "已经存在编码为【{}】的组织");
    ErrorCode ORGANIZATION_PARENT_NOT_EXISTS = new ErrorCode(1_002_033_002, "父级组织不存在");
    ErrorCode ORGANIZATION_PARENT_ERROR = new ErrorCode(1_002_033_003, "不能设置自己或自己的子组织为父组织");
    ErrorCode ORGANIZATION_HAS_CHILDREN = new ErrorCode(1_002_033_004, "存在子组织，无法删除");
    ErrorCode ORGANIZATION_NOT_ENABLE = new ErrorCode(1_002_033_005, "组织({})不处于开启状态，不允许选择");
    ErrorCode ORGANIZATION_HAS_MEMBERSHIPS = new ErrorCode(1_002_033_006, "存在任职成员，无法删除");
    ErrorCode ORGANIZATION_TYPE_INVALID = new ErrorCode(1_002_033_007, "组织类型({})不合法");

    // ========== 任职 1-002-034-000（ZS-IAM-002：D-09 FND-IAM-001/003/004 任职与服务端上下文） ==========
    ErrorCode MEMBERSHIP_NOT_EXISTS = new ErrorCode(1_002_034_000, "任职不存在");
    ErrorCode MEMBERSHIP_ALREADY_EXISTS = new ErrorCode(1_002_034_001, "该账号在此组织已存在任职");
    ErrorCode MEMBERSHIP_INVALID_STATUS = new ErrorCode(1_002_034_002, "任职状态({})非在职，拒绝进入组织上下文");
    ErrorCode MEMBERSHIP_EXPIRED = new ErrorCode(1_002_034_003, "任职已过有效期，拒绝进入组织上下文");
    ErrorCode MEMBERSHIP_ORGANIZATION_MISMATCH = new ErrorCode(1_002_034_004, "任职组织({})与请求组织不一致，拒绝越权");
    ErrorCode MEMBERSHIP_PRIMARY_REQUIRED = new ErrorCode(1_002_034_005, "账号缺少默认任职，无法解析组织上下文");
    ErrorCode MEMBERSHIP_CONTEXT_FORGED = new ErrorCode(1_002_034_006, "组织上下文须由服务端签发，拒绝客户端指定的身份");
    ErrorCode MEMBERSHIP_ORGANIZATION_DISABLED = new ErrorCode(1_002_034_007, "任职所属组织({})不存在或已停用，拒绝进入组织上下文");
    ErrorCode MEMBERSHIP_NOT_EFFECTIVE = new ErrorCode(1_002_034_008, "任职尚未到生效期，拒绝进入组织上下文");
}
