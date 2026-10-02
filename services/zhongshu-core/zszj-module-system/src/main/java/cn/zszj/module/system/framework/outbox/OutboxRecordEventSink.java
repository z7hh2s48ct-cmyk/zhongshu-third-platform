package cn.zszj.module.system.framework.outbox;

import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 记录型 Outbox 事件确认 Sink（ZS-FC-003 放行轮风险排查补闭）。
 *
 * <p>覆盖两类「无消费语义、仅持久化留痕」的事件：
 * <ul>
 *   <li>{@code NOTIFY_DISPATCHED}（ZS-MSG-001）：站内信在业务事务内已同步落 {@code system_notify_message}，
 *       本事件为该动作的可回查记录（不承载二次副作用）；</li>
 *   <li>{@code AUDIT_CLEANED}（ZS-AUDIT-002）：审计保留期清理动作的留痕（清理本身已完成，同事务追加）。</li>
 * </ul>
 *
 * <p><b>为什么必须显式确认</b>：JOB-002 合同规定「无 Sink 声明支持的事件按失败退避进入可见失败
 * （重试至 DEAD），不丢弃」——该语义面向「有消费意图但消费失败」的事件；记录型事件若不声明 Sink，
 * 会在 {@code OutboxDispatchScheduler}（FC-003 起默认启用）的投递轮中逐个退避至 DEAD，
 * 污染人工处置台账并触发 DEAD_EVENTS_PRESENT 严重告警（逐 60s ERROR）。本 Sink 将其确认为
 * DISPATCHED 并留 info 日志，未来若赋予消费语义（如移动端推送回执），以新 Sink 替换本条目即可。
 *
 * @author ZS-FC-003
 */
@Slf4j
@Component
public class OutboxRecordEventSink implements OutboxEventSink {

    /** 记录型事件类型集合（确认即完成，无二次副作用） */
    static final String TYPE_NOTIFY_DISPATCHED = "NOTIFY_DISPATCHED";

    static final String TYPE_AUDIT_CLEANED = "AUDIT_CLEANED";

    @Override
    public boolean supports(String eventType) {
        return TYPE_NOTIFY_DISPATCHED.equals(eventType) || TYPE_AUDIT_CLEANED.equals(eventType);
    }

    @Override
    public void deliver(OutboxEventRecord event) {
        // 记录型事件：无二次副作用，确认即 DISPATCHED；留 info 级可回查日志（不含 payload 原文，循脱敏红线）
        log.info("[deliver][记录型事件确认：eventType={} bizType={} bizId={} eventId={}]",
                event.getEventType(), event.getBizType(), event.getBizId(), event.getEventId());
    }

}
