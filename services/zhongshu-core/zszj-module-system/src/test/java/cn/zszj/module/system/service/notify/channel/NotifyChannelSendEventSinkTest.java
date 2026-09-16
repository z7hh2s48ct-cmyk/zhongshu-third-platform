package cn.zszj.module.system.service.notify.channel;

import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotifyChannelSendEventSink} 单元测试（ZS-MSG-004）——载荷解析与可见失败。
 *
 * <p>循 {@code NotifyTodoEventSinkTest} 先例：BaseMockitoUnitTest + @InjectMocks。验证：
 * supports 声明 / 正常载荷解析 sendId 透传 / 缺 sendId 抛出（不静默确认 DISPATCHED）/ sendId 非法抛出 /
 * 服务可重试异常原样上抛（交 Outbox 退避重投）。
 */
class NotifyChannelSendEventSinkTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NotifyChannelSendEventSink notifyChannelSendEventSink;

    @Mock
    private NotifyChannelSendService channelSendService;

    @Test
    void testSupports() {
        assertThat(notifyChannelSendEventSink.supports("NOTIFY_CHANNEL_SEND")).isTrue();
        assertThat(notifyChannelSendEventSink.supports("NOTIFY_DISPATCHED")).isFalse();
        assertThat(notifyChannelSendEventSink.supports("OTHER")).isFalse();
    }

    @Test
    void testDeliver_正常载荷_透传sendId() {
        OutboxEventRecord event = event("{\"sendId\":123,\"channel\":\"SMS\"}");
        notifyChannelSendEventSink.deliver(event);
        verify(channelSendService).processOutboxDelivery(123L, 1L);
    }

    @Test
    void testDeliver_缺sendId_抛出可见失败() {
        OutboxEventRecord event = event("{\"channel\":\"SMS\"}");
        assertThatThrownBy(() -> notifyChannelSendEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sendId");
        verify(channelSendService, never()).processOutboxDelivery(anyLong(), anyLong());
    }

    @Test
    void testDeliver_sendId非法_抛出() {
        OutboxEventRecord event = event("{\"sendId\":\"abc\"}");
        assertThatThrownBy(() -> notifyChannelSendEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sendId 非法");
        verify(channelSendService, never()).processOutboxDelivery(anyLong(), anyLong());
    }

    @Test
    void testDeliver_服务抛可重试异常_原样上抛交退避() {
        OutboxEventRecord event = event("{\"sendId\":456}");
        doThrow(new NotifyChannelSendRetryableException("提交结果未知须先回查 sendId=456"))
                .when(channelSendService).processOutboxDelivery(456L, 1L);
        assertThatThrownBy(() -> notifyChannelSendEventSink.deliver(event))
                .isInstanceOf(NotifyChannelSendRetryableException.class);
    }

    private OutboxEventRecord event(String payload) {
        // OutboxEventRecord 为包外可见构造：id/eventType/bizType/bizId/bizVersion/payload/headers/
        // tenantId/retryCount/actorType/actorId/traceId/claimedBy/claimToken
        return new OutboxEventRecord(1L, "NOTIFY_CHANNEL_SEND", "notify_channel_send", "1", null,
                payload, null, 1L, 0, "SYSTEM", null, null, "inst", "token");
    }

}
