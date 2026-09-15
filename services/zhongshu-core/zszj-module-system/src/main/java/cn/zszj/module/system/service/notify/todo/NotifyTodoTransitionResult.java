package cn.zszj.module.system.service.notify.todo;

import lombok.Builder;
import lombok.Value;

/**
 * 业务待办流转结果（ZS-MSG-002）。
 *
 * <p>{@code changed=false} 表示本次未生效（重复事件 / 乱序旧版本 / 终态守卫 / 待办不存在 / 并发处理中）；
 * {@code outcome} 承载 ZS-JOB-003 Inbox Outcome 映射或本服务的应用层结果（TERMINAL_GUARDED / NOT_FOUND）。
 * 结果而非异常，故 PENDING/COMPLETED/DUPLICATE/STALE_VERSION/TERMINAL_GUARDED 等不分配错误码。
 */
@Value
@Builder
public class NotifyTodoTransitionResult {

    /** 本次流转已应用（状态发生变更） */
    public static final String OUTCOME_APPLIED = "APPLIED";
    /** 重复事件：同 eventId 已完成，返回首次结果不重复副作用 */
    public static final String OUTCOME_DUPLICATE_COMPLETED = "DUPLICATE_COMPLETED";
    /** 同键处理中（并发抢占） */
    public static final String OUTCOME_DUPLICATE_IN_FLIGHT = "DUPLICATE_IN_FLIGHT";
    /** 先前结果未知，须先回查 */
    public static final String OUTCOME_DUPLICATE_RESULT_UNKNOWN = "DUPLICATE_RESULT_UNKNOWN";
    /** 同键不同载荷指纹（参数冲突） */
    public static final String OUTCOME_PARAM_CONFLICT = "PARAM_CONFLICT";
    /** 乱序旧版本事件被版本水位拒绝 */
    public static final String OUTCOME_STALE_VERSION = "STALE_VERSION";
    /** 终态守卫：待办已终态，后到事件不复活 */
    public static final String OUTCOME_TERMINAL_GUARDED = "TERMINAL_GUARDED";
    /** 待办不存在（Inbox 记录标记 FAILED 可重试） */
    public static final String OUTCOME_NOT_FOUND = "NOT_FOUND";

    /** 待办 ID（未定位到待办时可空） */
    Long todoId;

    /** 稳定业务任务 ID */
    String todoKey;

    /** 流转后（或当前）通用状态；未定位到待办时可空 */
    NotifyTodoStatus status;

    /** 本次是否生效（状态是否变更） */
    boolean changed;

    /** 结果标识（见 OUTCOME_* 常量） */
    String outcome;

    /** 结果原因（脱敏，可空） */
    String reason;

}
