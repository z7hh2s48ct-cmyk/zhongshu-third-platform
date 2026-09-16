package cn.zszj.module.system.service.notify.channel;

import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 渠道发送器注册器（ZS-MSG-004）——收集容器内全部 {@link NotifyChannelSender} 实现，按渠道查找。
 *
 * <p><b>B05 现状：注册器为空</b>——真实短信/邮件/推送发送器受 D-10 门禁（「本轮先核查站内闭环，
 * 不调用真实短信、邮件或微信」），生产容器无任何实现；此时派发侧将 SMS/EMAIL/PUSH 记
 * CHANNEL_NOT_CONFIGURED 明确阻断（可查询、不静默丢弃），符合「未配置渠道明确阻断」要求。
 * Mock 实现只存在于测试装配，不进入生产上下文（Mock 不作为真实渠道验收）。
 */
@Component
@Slf4j
public class NotifyChannelSenderRegistry {

    private final Map<NotifyChannel, NotifyChannelSender> sendersByChannel;

    public NotifyChannelSenderRegistry(@Autowired(required = false) List<NotifyChannelSender> senders) {
        Map<NotifyChannel, NotifyChannelSender> map = new EnumMap<>(NotifyChannel.class);
        for (NotifyChannelSender sender : senders == null ? Collections.<NotifyChannelSender>emptyList() : senders) {
            NotifyChannelSender previous = map.put(sender.channel(), sender);
            if (previous != null) {
                // 同渠道多实现是装配错误（歧义），启动即失败（fail-fast）而非静默任选
                throw new IllegalStateException("渠道 " + sender.channel() + " 存在多个 NotifyChannelSender 实现: "
                        + previous.getClass().getName() + " / " + sender.getClass().getName());
            }
        }
        this.sendersByChannel = Collections.unmodifiableMap(map);
        log.info("[NotifyChannelSenderRegistry][已注册渠道发送器: {}]", map.keySet());
    }

    /** 按渠道查找发送器；无实现返回 empty（= 渠道未配置，派发侧记 CHANNEL_NOT_CONFIGURED）。 */
    public Optional<NotifyChannelSender> find(NotifyChannel channel) {
        return Optional.ofNullable(sendersByChannel.get(channel));
    }

}
