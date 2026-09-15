package cn.zszj.module.system.service.notify.todo;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * 业务待办流转事件消费接线（ZS-MSG-002）——ZS-JOB-002 {@link OutboxEventSink} 的中性适配层。
 *
 * <p>{@code supports("NOTIFY_TODO_TRANSITION")} → {@code deliver} 解析事件载荷为 {@link NotifyTodoTransitionCmd}
 * 并调用 {@link NotifyTodoService#applyTransition}（Inbox 幂等消费，at-least-once 重投由 eventId 幂等键吸收）。
 *
 * <p><b>错误处置（codex r0 F3/F4）</b>：错误载荷（缺 transitionType/todoKey/sourceType、未知 transitionType、
 * REASSIGN 缺转派目标）与「未生效需重试」结果（NOT_FOUND / 并发处理中 / 结果未知 / 参数冲突）一律<b>抛出</b>
 * 进入 dispatcher 可见失败/重试路径，不静默 return（避免被确认 DISPATCHED 掩盖）；已应用 / 重复完成 / 终态守卫 /
 * 旧版本拒绝属可确认终局，正常返回。REASSIGN 转派目标经 {@code assigneeType/assigneeId} 解析为 {@link NotifyRecipient}。
 *
 * <p><b>B05 状态：中性接线，无生产者时休眠</b>——主库当前无 {@code NOTIFY_TODO_TRANSITION} 生产者；
 * D-07 首链 / BPM 任务事件（completeTask/withdrawTask/transferTask）接入后激活。本卡以 {@code NotifyTodoServiceTest}
 * 直接驱动 {@code applyTransition}（TransactionTemplate 包裹，满足 Inbox MANDATORY）证明幂等/版本/终态守卫机制。
 *
 * <p><b>事务语义（D-07 接线时核实）</b>：{@link #deliver} 依赖 {@code OutboxDispatcherService} 在投递路径为
 * applyTransition 提供业务事务上下文（ZS-JOB-003 Inbox MANDATORY 要求）；若 dispatcher 未开事务，须在接线时以
 * 编程式事务包裹（登记为 D-07 首链接线任务，见计划 §2.5 / §6）。Sink 只做投递动作，不回写业务事实。
 */
@Component
@Slf4j
public class NotifyTodoEventSink implements OutboxEventSink {

    /** 本 Sink 声明消费的事件类型。 */
    public static final String EVENT_TYPE = "NOTIFY_TODO_TRANSITION";

    /**
     * R1'：载荷解码专用 {@link ObjectReader}——启用 {@link DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS}
     * 保留 JSON 数值原始十进制精度。{@code JsonUtils.parseMap} 用仓库默认 mapper 将浮点解码为 Double，会在此丢失
     * 精度（{@code 200.00000000000001→200.0}、{@code 9007199254740993.0→9007199254740992.0}），使 {@link #parseAssignee}
     * 的 {@code longValueExact()} 校验已舍入值而非原始数值、漏拒错误 ID。派生自 {@code JsonUtils.getObjectMapper()}
     * 复用仓库配置，仅本 Reader 启用该特性（ObjectReader 不可变、线程安全），<b>不修改全局 ObjectMapper</b>。
     */
    private static final ObjectReader PAYLOAD_READER = JsonUtils.getObjectMapper()
            .reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .forType(new TypeReference<Map<String, Object>>() {});

    @Resource
    private NotifyTodoService notifyTodoService;

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void deliver(OutboxEventRecord event) {
        Map<String, Object> payload = parsePayload(event.getPayload());
        String transition = str(payload.get("transitionType"));
        String todoKey = str(payload.get("todoKey"));
        String sourceType = str(payload.get("sourceType"));
        // F3：错误载荷不得静默视为投递成功（否则 dispatcher 确认 DISPATCHED 掩盖问题）——抛出进入可见失败/重试
        if (!StringUtils.hasText(transition) || !StringUtils.hasText(todoKey) || !StringUtils.hasText(sourceType)) {
            throw new IllegalArgumentException("NOTIFY_TODO_TRANSITION 载荷缺少 transitionType/todoKey/sourceType, eventId="
                    + event.getEventId());
        }
        TodoTransitionType transitionType;
        try {
            transitionType = TodoTransitionType.valueOf(transition.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("NOTIFY_TODO_TRANSITION 未知 transitionType=" + transition
                    + ", eventId=" + event.getEventId(), e);
        }
        // F4：解析转派目标（REASSIGN 必须携带完整 assigneeType/assigneeId，否则拒绝——避免只改状态保留旧处理人）
        NotifyRecipient assignee = parseAssignee(payload);
        if (transitionType == TodoTransitionType.REASSIGN && assignee == null) {
            throw new IllegalArgumentException("NOTIFY_TODO_TRANSITION REASSIGN 转派目标缺失或非法(assigneeType/assigneeId 须完整且 ID 可无损转 Long), eventId="
                    + event.getEventId());
        }
        NotifyTodoTransitionCmd cmd = NotifyTodoTransitionCmd.builder()
                // eventId 取 Outbox 事件 ID（稳定幂等键，at-least-once 重投由 Inbox 吸收）
                .eventId(String.valueOf(event.getEventId()))
                .transitionType(transitionType)
                .todoKey(todoKey)
                .sourceType(sourceType)
                .bizType(event.getBizType()).bizId(event.getBizId()).bizVersion(event.getBizVersion())
                .reason(str(payload.get("reason")))
                .assignee(assignee)
                .traceId(event.getTraceId())
                .build();
        NotifyTodoTransitionResult result = notifyTodoService.applyTransition(cmd);
        // F3：区分可确认 / 需重试——未生效且属可恢复态（待办未就位 / 并发处理中 / 结果未知 / 参数冲突）抛出触发
        // 重试或人工处置；已应用 / 重复完成 / 终态守卫 / 旧版本拒绝属可确认终局，正常返回由 dispatcher 确认 DISPATCHED。
        String outcome = result.getOutcome();
        if (NotifyTodoTransitionResult.OUTCOME_NOT_FOUND.equals(outcome)
                || NotifyTodoTransitionResult.OUTCOME_DUPLICATE_IN_FLIGHT.equals(outcome)
                || NotifyTodoTransitionResult.OUTCOME_DUPLICATE_RESULT_UNKNOWN.equals(outcome)
                || NotifyTodoTransitionResult.OUTCOME_PARAM_CONFLICT.equals(outcome)) {
            throw new IllegalStateException("NOTIFY_TODO_TRANSITION 未生效需重试/人工处置, outcome=" + outcome
                    + ", eventId=" + event.getEventId());
        }
    }

    private static Map<String, Object> parsePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            // R1'：专用 Reader（USE_BIG_DECIMAL_FOR_FLOATS）保留数值原始精度，避免默认 Double 解码丢精度
            Map<String, Object> parsed = PAYLOAD_READER.readValue(payload);
            return parsed == null ? Map.of() : parsed;
        } catch (IOException e) {
            // 载荷非法 JSON：返回空 Map，交由 deliver 缺字段校验抛出可见失败（循 JsonUtils.parseMap 解析失败返 null 语义）
            return Map.of();
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * F4/R1/R1'：解析转派目标——assigneeType(ADMIN/MEMBER) + assigneeId → NotifyRecipient；不完整或 ID 非整数/越界
     * （不可无损转 Long）返回 null。载荷经 {@link #PAYLOAD_READER}（USE_BIG_DECIMAL_FOR_FLOATS）解码保留原始十进制
     * 精度，故 {@code longValueExact()} 校验原始 JSON 数值而非已舍入 Double——小数（200.9 / 200.00000000000001）拒绝、
     * 大整数浮点（9007199254740993.0）无损解析为 9007199254740993L 而非舍入。
     */
    private static NotifyRecipient parseAssignee(Map<String, Object> payload) {
        String type = str(payload.get("assigneeType"));
        Object idValue = payload.get("assigneeId");
        if (!StringUtils.hasText(type) || idValue == null) {
            return null;
        }
        Long id;
        if (idValue instanceof Number) {
            // R1/R1'：无损转 Long——载荷已由 PAYLOAD_READER 保留原始十进制精度，小数（200.9 / 200.00000000000001）
            // 或越界大整数经 longValueExact 抛 ArithmeticException、NaN/Infinity 抛 NumberFormatException，均拒绝
            // （返回 null → REASSIGN 抛出），不静默截断/舍入转派给错误用户
            try {
                id = new java.math.BigDecimal(idValue.toString()).longValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                return null;
            }
        } else {
            try {
                id = Long.parseLong(String.valueOf(idValue).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        UserTypeEnum userType;
        try {
            userType = UserTypeEnum.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return userType == UserTypeEnum.MEMBER ? NotifyRecipient.member(id) : NotifyRecipient.admin(id);
    }

}
