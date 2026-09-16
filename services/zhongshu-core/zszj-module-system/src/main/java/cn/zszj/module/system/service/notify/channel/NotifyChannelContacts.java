package cn.zszj.module.system.service.notify.channel;

import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipientContext;

/**
 * 渠道-联系方式映射（ZS-MSG-004；r1 P3 去重——派发侧与重试侧共用同一份渠道语义，防双份拷贝漂移）。
 *
 * <p>SMS→手机号，EMAIL→邮箱；PUSH 暂无联系方式语义（返回 null，与缺失同归 RECIPIENT_CONTACT_MISSING
 * 明确阻断，待 D-10 门禁后定义设备令牌语义）。
 */
public final class NotifyChannelContacts {

    private NotifyChannelContacts() {
    }

    /** 按渠道从收件人上下文取联系方式；上下文为空返回 null。 */
    public static String contactFor(NotifyChannel channel, NotifyRecipientContext ctx) {
        if (ctx == null) {
            return null;
        }
        switch (channel) {
            case SMS:
                return ctx.getContactMobile();
            case EMAIL:
                return ctx.getContactEmail();
            default:
                return null;
        }
    }

}
