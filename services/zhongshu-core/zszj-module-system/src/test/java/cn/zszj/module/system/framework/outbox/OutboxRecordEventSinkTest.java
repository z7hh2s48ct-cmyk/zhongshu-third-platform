package cn.zszj.module.system.framework.outbox;

import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * {@link OutboxRecordEventSink} 的单元测试（ZS-FC-003 风险排查补闭）——记录型事件确认语义。
 *
 * <p>验证：两类记录型事件（NOTIFY_DISPATCHED/AUDIT_CLEANED）被支持且确认无副作用；
 * 有独立消费语义的事件类型（补偿类/渠道类/待办类）不被抢占。
 *
 * @author ZS-FC-003
 */
class OutboxRecordEventSinkTest {

    private final OutboxRecordEventSink sink = new OutboxRecordEventSink();

    @Test
    void supports_recordTypesOnly() {
        assertThat(sink.supports("NOTIFY_DISPATCHED")).isTrue();
        assertThat(sink.supports("AUDIT_CLEANED")).isTrue();
        // 有独立消费语义的事件不得被记录 Sink 抢占（各归其位：补偿/渠道/待办均有专属 Sink）
        assertThat(sink.supports("TOKEN_REVOCATION_COMPENSATION")).isFalse();
        assertThat(sink.supports("CACHE_EVICTION_COMPENSATION")).isFalse();
        assertThat(sink.supports("NOTIFY_CHANNEL_SEND")).isFalse();
        assertThat(sink.supports("NOTIFY_TODO_TRANSITION")).isFalse();
        assertThat(sink.supports("UNKNOWN_EVENT")).isFalse();
    }

    @Test
    void deliver_completesWithoutSideEffects() {
        OutboxEventRecord record = new OutboxEventRecord(
                1L, "NOTIFY_DISPATCHED", "system_notify_message", "1024", "1", "{}", null,
                1L, 0, "ADMIN", "1", null, "inst@outbox-dispatch", "claim-token");
        assertDoesNotThrow(() -> sink.deliver(record));
        // 记录型确认幂等：重复确认无异常（at-least-once 重投安全）
        assertDoesNotThrow(() -> sink.deliver(record));
    }

}
