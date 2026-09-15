package cn.zszj.module.system.service.notify.dispatch;

import cn.zszj.framework.common.enums.UserTypeEnum;
import lombok.Value;

/**
 * 通知收件人（ZS-MSG-001）。
 *
 * <p>B05 只支持 {@link UserTypeEnum#ADMIN}（System 用户）；MEMBER 待 D-09 身份模型落地后追加。
 * 任职路由（按组织/岗位/角色动态解析收件人）归 MSG-001.B（D-09 后），本卡只保留扩展点。
 */
@Value
public class NotifyRecipient {

    /** 收件人类型（B05 只支持 ADMIN） */
    UserTypeEnum type;

    /** 收件人编号（AdminUserDO.id） */
    Long id;

    public static NotifyRecipient admin(Long id) {
        return new NotifyRecipient(UserTypeEnum.ADMIN, id);
    }

    public static NotifyRecipient member(Long id) {
        return new NotifyRecipient(UserTypeEnum.MEMBER, id);
    }

}
