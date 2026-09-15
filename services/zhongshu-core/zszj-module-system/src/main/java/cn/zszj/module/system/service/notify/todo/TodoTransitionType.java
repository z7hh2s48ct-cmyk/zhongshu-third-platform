package cn.zszj.module.system.service.notify.todo;

/**
 * 业务待办流转类型（ZS-MSG-002，机制级）——事件驱动的待办生命周期流转。
 *
 * <p>由业务来源事件（BPM 任务完成/撤回/转派等）经 {@link TodoStatusMapper} 映射得到，或由调用方直接指定；
 * 每种流转对应一个通用目标状态（{@link #targetStatus()}）。D-07 红线：不含任何试点业务态语义。
 */
public enum TodoTransitionType {

    /** 业务完成 → COMPLETED */
    COMPLETE,

    /** 业务撤回 → WITHDRAWN */
    WITHDRAW,

    /** 业务转派 → REASSIGNED（并更新 assignee） */
    REASSIGN,

    /** 业务失效 → INVALID */
    INVALIDATE;

    /** 本流转应用后的通用目标状态。 */
    public NotifyTodoStatus targetStatus() {
        switch (this) {
            case COMPLETE:
                return NotifyTodoStatus.COMPLETED;
            case WITHDRAW:
                return NotifyTodoStatus.WITHDRAWN;
            case REASSIGN:
                return NotifyTodoStatus.REASSIGNED;
            case INVALIDATE:
                return NotifyTodoStatus.INVALID;
            default:
                throw new IllegalStateException("未知待办流转类型: " + this);
        }
    }

}
