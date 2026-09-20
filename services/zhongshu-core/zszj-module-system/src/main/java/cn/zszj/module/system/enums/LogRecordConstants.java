package cn.zszj.module.system.enums;

/**
 * System 操作日志枚举
 * 目的：统一管理，也减少 Service 里各种“复杂”字符串
 *
 * @author 芋道源码
 */
public interface LogRecordConstants {

    // ======================= SYSTEM_USER 用户 =======================

    String SYSTEM_USER_TYPE = "SYSTEM 用户";
    String SYSTEM_USER_CREATE_SUB_TYPE = "创建用户";
    String SYSTEM_USER_CREATE_SUCCESS = "创建了用户【{{#user.nickname}}】";
    String SYSTEM_USER_UPDATE_SUB_TYPE = "更新用户";
    String SYSTEM_USER_UPDATE_SUCCESS = "更新了用户【{{#user.nickname}}】: {_DIFF{#updateReqVO}}";
    String SYSTEM_USER_DELETE_SUB_TYPE = "删除用户";
    String SYSTEM_USER_DELETE_SUCCESS = "删除了用户【{{#user.nickname}}】";
    String SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE = "重置用户密码";
    String SYSTEM_USER_UPDATE_PASSWORD_SUCCESS = "将用户【{{#user.nickname}}】的密码从【{{#user.password}}】重置为【{{#newPassword}}】，其全部登录会话已失效";
    // ZS-LOGIN-003：以下两类生命周期入口同样会触发「全部登录会话失效」，此前无操作日志留痕，故补齐
    String SYSTEM_USER_UPDATE_STATUS_SUB_TYPE = "更新用户状态";
    String SYSTEM_USER_UPDATE_STATUS_SUCCESS = "将用户【{{#user.nickname}}】的状态更新为【{{#status}}】；若为禁用则其全部登录会话已失效";
    String SYSTEM_USER_UPDATE_SELF_PASSWORD_SUB_TYPE = "修改自身密码";
    String SYSTEM_USER_UPDATE_SELF_PASSWORD_SUCCESS = "用户【{{#user.nickname}}】修改了自己的密码，其全部登录会话已失效";

    // ======================= SYSTEM_MEMBERSHIP 任职（ZS-IAM-004） =======================
    // 生命周期审计与 system_membership_history 流水互补：流水承载结构化历史归属（只增不改、业务责任溯源），
    // @LogRecord 承载操作日志（操作者/时间/trace-id，与 LOGIN-003 会话撤销日志同 trace 关联）。

    String SYSTEM_MEMBERSHIP_TYPE = "SYSTEM 任职";
    String SYSTEM_MEMBERSHIP_CREATE_SUB_TYPE = "任职入职";
    String SYSTEM_MEMBERSHIP_CREATE_SUCCESS = "为账号【{{#membership.userId}}】在组织【{{#membership.organizationId}}】创建任职";
    String SYSTEM_MEMBERSHIP_TRANSFER_SUB_TYPE = "任职转岗";
    String SYSTEM_MEMBERSHIP_TRANSFER_SUCCESS = "将任职【{{#membershipId}}】转岗至组织【{{#toOrganizationId}}】，原因【{{#reason}}】；已发生业务的责任历史不被改写";
    String SYSTEM_MEMBERSHIP_CHANGE_STATUS_SUB_TYPE = "任职状态流转";
    String SYSTEM_MEMBERSHIP_CHANGE_STATUS_SUCCESS = "将任职【{{#membershipId}}】状态变更为【{{#toStatus}}】，原因【{{#reason}}】；若为停用/离职/过期且致默认任职上下文丧失，则其全部登录会话已失效";

    // ======================= SYSTEM_ROLE 角色 =======================

    String SYSTEM_ROLE_TYPE = "SYSTEM 角色";
    String SYSTEM_ROLE_CREATE_SUB_TYPE = "创建角色";
    String SYSTEM_ROLE_CREATE_SUCCESS = "创建了角色【{{#role.name}}】";
    String SYSTEM_ROLE_UPDATE_SUB_TYPE = "更新角色";
    String SYSTEM_ROLE_UPDATE_SUCCESS = "更新了角色【{{#role.name}}】: {_DIFF{#updateReqVO}}";
    String SYSTEM_ROLE_DELETE_SUB_TYPE = "删除角色";
    String SYSTEM_ROLE_DELETE_SUCCESS = "删除了角色【{{#role.name}}】";

}
