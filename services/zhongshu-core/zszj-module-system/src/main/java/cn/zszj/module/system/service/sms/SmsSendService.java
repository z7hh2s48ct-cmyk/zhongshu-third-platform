package cn.zszj.module.system.service.sms;

import cn.zszj.module.system.mq.message.sms.SmsSendMessage;

import java.util.List;
import java.util.Map;

/**
 * 短信发送 Service 接口
 *
 * @author 芋道源码
 */
public interface SmsSendService {

    /**
     * 发送单条短信给管理后台的用户
     *
     * 在 mobile 为空时，使用 userId 加载对应管理员的手机号
     *
     * @param mobile 手机号
     * @param userId 用户编号
     * @param templateCode 短信模板编号
     * @param templateParams 短信模板参数
     * @return 发送日志编号
     */
    Long sendSingleSmsToAdmin(String mobile, Long userId,
                              String templateCode, Map<String, Object> templateParams);

    /**
     * 发送单条短信给用户 APP 的用户
     *
     * 在 mobile 为空时，使用 userId 加载对应会员的手机号
     *
     * @param mobile 手机号
     * @param userId 用户编号
     * @param templateCode 短信模板编号
     * @param templateParams 短信模板参数
     * @return 发送日志编号
     */
    Long sendSingleSmsToMember(String mobile, Long userId,
                               String templateCode, Map<String, Object> templateParams);

    /**
     * 发送单条短信给用户
     *
     * @param mobile 手机号
     * @param userId 用户编号
     * @param userType 用户类型
     * @param templateCode 短信模板编号
     * @param templateParams 短信模板参数
     * @return 发送日志编号
     */
    Long sendSingleSms(String mobile, Long userId, Integer userType,
                       String templateCode, Map<String, Object> templateParams);

    /**
     * ZS-LOGIN-004：判断指定短信模板当前是否「真的可发送」。
     * <p>
     * 仅当模板存在且启用、且其所属渠道存在且启用时返回 {@code true}。
     * <p>
     * 用途：验证码发送前的前置门控。通道未就绪时必须<b>明确失败</b>——不写库、不占用发送配额、更不静默返回成功，
     * 避免「假报已发送」让用户苦等一条永远不会到达的短信，同时避免攻击者借未就绪通道刷库。
     *
     * @param templateCode 短信模板编号
     * @return 是否可发送
     */
    boolean isTemplateSendable(String templateCode);

    default void sendBatchSms(List<String> mobiles, List<Long> userIds, Integer userType,
                              String templateCode, Map<String, Object> templateParams) {
        throw new UnsupportedOperationException("暂时不支持该操作，感兴趣可以实现该功能哟！");
    }

    /**
     * 执行真正的短信发送
     * 注意，该方法仅仅提供给 MQ Consumer 使用
     *
     * @param message 短信
     */
    void doSendSms(SmsSendMessage message);

    /**
     * 接收短信的接收结果
     *
     * @param channelCode 渠道编码
     * @param text 结果内容
     * @throws Throwable 处理失败时，抛出异常
     */
    void receiveSmsStatus(String channelCode, String text) throws Throwable;

}
