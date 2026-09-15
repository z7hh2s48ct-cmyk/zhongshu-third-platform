package cn.zszj.module.system.service.notify;

import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.todo.NotifyTodoEventSink;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.notify.todo.NotifyTodoTransitionCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoTransitionResult;
import cn.zszj.module.system.service.notify.todo.TodoTransitionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotifyTodoEventSink} 单元测试（ZS-MSG-002，codex r0 F3/F4）——错误载荷可见失败 + 转派目标解析。
 *
 * <p>循 {@code NotifySendServiceImplTest} 先例：BaseMockitoUnitTest + @InjectMocks Sink + @Mock NotifyTodoService。
 * 验证：①缺字段 / 未知类型 / REASSIGN 缺目标 → 抛出（不静默确认 DISPATCHED）；②applyTransition 未生效可恢复态
 * （NOT_FOUND）→ 抛出触发重试；③已应用（APPLIED）→ 正常返回（可确认）；④REASSIGN 载荷 assigneeType/assigneeId →
 * 解析为 {@link NotifyRecipient} 传入命令。
 */
class NotifyTodoEventSinkTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NotifyTodoEventSink notifyTodoEventSink;

    @Mock
    private NotifyTodoService notifyTodoService;

    @Test
    void testSupports() {
        assertThat(notifyTodoEventSink.supports("NOTIFY_TODO_TRANSITION")).isTrue();
        assertThat(notifyTodoEventSink.supports("OTHER")).isFalse();
    }

    @Test
    void testDeliver_缺字段_抛出可见失败() {
        // F3：缺 transitionType → 抛出，不调用 applyTransition（不静默确认 DISPATCHED）
        OutboxEventRecord event = event("{\"todoKey\":\"T1\",\"sourceType\":\"BPM\"}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transitionType/todoKey/sourceType");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_未知流转类型_抛出() {
        // F3：未知 transitionType → 抛出
        OutboxEventRecord event = event("{\"transitionType\":\"NOPE\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\"}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知 transitionType");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_REASSIGN缺转派目标_抛出() {
        // F4：REASSIGN 未携带 assigneeType/assigneeId → 抛出（避免只改状态保留旧处理人）
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\"}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REASSIGN 转派目标缺失或非法");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_REASSIGN小数ID_拒绝不静默截断() {
        // R1：assigneeId=200.9 若用 longValue() 会截断为 200（转派给错误用户）——longValueExact 拒绝并抛出
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":200.9}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REASSIGN 转派目标缺失或非法");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_REASSIGN越界ID_拒绝() {
        // R1：超出 Long 范围的大整数 longValueExact 抛 ArithmeticException → 拒绝（不截断为错误 ID）
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":99999999999999999999}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REASSIGN 转派目标缺失或非法");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_REASSIGN解析MEMBER目标_传入cmd() {
        // R1/F4：合法 MEMBER 目标正常无损解析（补 codex 建议的 MEMBER 用例，与 ADMIN 用例对称）
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"MEMBER\",\"assigneeId\":300}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(true)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).build());
        notifyTodoEventSink.deliver(event);
        ArgumentCaptor<NotifyTodoTransitionCmd> captor = ArgumentCaptor.forClass(NotifyTodoTransitionCmd.class);
        verify(notifyTodoService).applyTransition(captor.capture());
        assertThat(captor.getValue().getAssignee()).isEqualTo(NotifyRecipient.member(300L));
    }

    @Test
    void testDeliver_REASSIGN解析转派目标_传入cmd() {
        // F4：assigneeType/assigneeId → NotifyRecipient.admin(200) 传入 applyTransition
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":200}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(true)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).build());
        notifyTodoEventSink.deliver(event);
        ArgumentCaptor<NotifyTodoTransitionCmd> captor = ArgumentCaptor.forClass(NotifyTodoTransitionCmd.class);
        verify(notifyTodoService).applyTransition(captor.capture());
        NotifyTodoTransitionCmd cmd = captor.getValue();
        assertThat(cmd.getAssignee()).isEqualTo(NotifyRecipient.admin(200L));
        assertThat(cmd.getTodoKey()).isEqualTo("T1");
        assertThat(cmd.getTransitionType()).isEqualTo(TodoTransitionType.REASSIGN);
    }

    @Test
    void testDeliver_完成已应用_正常返回() {
        // F3：APPLIED 属可确认终局 → 正常返回（不抛）
        OutboxEventRecord event = event("{\"transitionType\":\"COMPLETE\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\"}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(true)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).build());
        notifyTodoEventSink.deliver(event);
        verify(notifyTodoService).applyTransition(any());
    }

    @Test
    void testDeliver_待办未就位_抛出触发重试() {
        // F3：NOT_FOUND 属可恢复态 → 抛出触发 dispatcher 重试（不静默确认）
        OutboxEventRecord event = event("{\"transitionType\":\"COMPLETE\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\"}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(false)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_NOT_FOUND).build());
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未生效需重试");
    }

    @Test
    void testDeliver_REASSIGN解码精度小数_拒绝不漏判() {
        // R1'：200.00000000000001 经默认 Double 解码会舍入为 200.0（漏拒）——专用 Reader 保留 BigDecimal 原始精度，
        // longValueExact 检出非零小数 → 拒绝（不静默接受 200 转派给错误用户）
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":200.00000000000001}");
        assertThatThrownBy(() -> notifyTodoEventSink.deliver(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REASSIGN 转派目标缺失或非法");
        verify(notifyTodoService, never()).applyTransition(any());
    }

    @Test
    void testDeliver_REASSIGN大整数浮点_无损解析不舍入() {
        // R1'：9007199254740993.0（2^53+1）经 Double 解码会舍入为 9007199254740992（错误 ID）——专用 Reader 保留
        // 原始十进制 → longValueExact 无损得 9007199254740993L
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":9007199254740993.0}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(true)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).build());
        notifyTodoEventSink.deliver(event);
        ArgumentCaptor<NotifyTodoTransitionCmd> captor = ArgumentCaptor.forClass(NotifyTodoTransitionCmd.class);
        verify(notifyTodoService).applyTransition(captor.capture());
        assertThat(captor.getValue().getAssignee()).isEqualTo(NotifyRecipient.admin(9007199254740993L));
    }

    @Test
    void testDeliver_REASSIGN科学计数法_无损解析() {
        // R1'：9.007199254740993e15 科学计数法经 Double 解码同样失真——专用 Reader 保留精度 → 9007199254740993L
        OutboxEventRecord event = event("{\"transitionType\":\"REASSIGN\",\"todoKey\":\"T1\",\"sourceType\":\"BPM\","
                + "\"assigneeType\":\"ADMIN\",\"assigneeId\":9.007199254740993e15}");
        when(notifyTodoService.applyTransition(any()))
                .thenReturn(NotifyTodoTransitionResult.builder()
                        .todoKey("T1").changed(true)
                        .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).build());
        notifyTodoEventSink.deliver(event);
        ArgumentCaptor<NotifyTodoTransitionCmd> captor = ArgumentCaptor.forClass(NotifyTodoTransitionCmd.class);
        verify(notifyTodoService).applyTransition(captor.capture());
        assertThat(captor.getValue().getAssignee()).isEqualTo(NotifyRecipient.admin(9007199254740993L));
    }

    /** 构造 NOTIFY_TODO_TRANSITION 事件记录（payload 可变，其余取默认上下文）。 */
    private static OutboxEventRecord event(String payload) {
        return new OutboxEventRecord(1L, "NOTIFY_TODO_TRANSITION", "task", "T-1", null,
                payload, null, 1L, 0, "SYSTEM", null, "trace-1", "inst@disp", "token-1");
    }
}
