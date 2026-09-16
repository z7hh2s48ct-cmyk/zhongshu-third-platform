package cn.zszj.module.system.service.notify.channel;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 渠道发送投递事件消费接线（ZS-MSG-004）——ZS-JOB-002 {@link OutboxEventSink} 的中性适配层。
 *
 * <p>{@code supports("NOTIFY_CHANNEL_SEND")} → {@code deliver} 解析载荷 {@code sendId} 并调用
 * {@link NotifyChannelSendService#processOutboxDelivery}（PENDING 提交 / UNKNOWN 先回查再重发 /
 * 终态与受理态幂等吸收）。
 *
 * <p><b>错误处置（循 ZS-MSG-002 Sink F3 先例）</b>：载荷缺 sendId / sendId 非法 → 抛出进入 dispatcher
 * 可见失败路径（不静默 return 被确认 DISPATCHED 掩盖）；提交结果未知/回查仍未知 → 服务抛
 * {@link NotifyChannelSendRetryableException} 原样上抛，由 OutboxDispatcherService 推进退避重投
 * （连续 5 次转 DEAD 人工处置，台账归 ZS-JOB-004）。
 *
 * <p><b>事务语义</b>：dispatcher 投递路径不保证事务上下文，服务内部以编程式短事务包裹全部落库推进
 * （外部渠道调用严格在事务外），不依赖调用方事务——区别于 MSG-002 Sink 依赖 dispatcher 事务的登记项。
 *
 * <p><b>B05 状态：中性接线，无生产者时休眠</b>——生产容器无 {@code NotifyChannelSender} 实现（真实渠道受
 * D-10 门禁），派发侧不会产生 {@code NOTIFY_CHANNEL_SEND} 事件，本 Sink 不被触发；机制由
 * {@code NotifyChannelSendServiceTest} 直接驱动锁定语义。
 */
@Component
@Slf4j
public class NotifyChannelSendEventSink implements OutboxEventSink {

    @Resource
    private NotifyChannelSendService channelSendService;

    @Override
    public boolean supports(String eventType) {
        return NotifyChannelSendService.OUTBOX_EVENT_TYPE.equals(eventType);
    }

    @Override
    public void deliver(OutboxEventRecord event) {
        long sendId = parseSendId(event);
        // r0 P1：事件身份透传——服务按 (租户, outboxEventId) 定位台账，陈旧/换绑事件被幂等吸收
        channelSendService.processOutboxDelivery(sendId, event.getEventId());
    }

    /** 解析投递句柄 sendId（缺失/非法 → 抛出可见失败，不静默确认 DISPATCHED——循 MSG-002 F3 先例）。 */
    private long parseSendId(OutboxEventRecord event) {
        Map<String, Object> payload = JsonUtils.parseMap(event.getPayload());
        String sendId = payload == null ? null : str(payload.get("sendId"));
        if (!StringUtils.hasText(sendId)) {
            throw new IllegalArgumentException("NOTIFY_CHANNEL_SEND 载荷缺少 sendId, outboxEventId=" + event.getEventId());
        }
        try {
            return Long.parseLong(sendId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("NOTIFY_CHANNEL_SEND 载荷 sendId 非法: " + sendId
                    + ", outboxEventId=" + event.getEventId(), e);
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
