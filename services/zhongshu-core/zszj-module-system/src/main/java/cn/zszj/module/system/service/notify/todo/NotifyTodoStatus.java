package cn.zszj.module.system.service.notify.todo;

/**
 * 业务待办通用处理状态（ZS-MSG-002，机制级）。
 *
 * <p>D-07 红线：本枚举为<b>通用机制状态</b>（循 ZS-JOB-003 {@code inbox_event} status CHECK 机制先例），
 * <b>不含</b> D-07 试点业务态（加盟商申请 DRAFT/PENDING_APPROVAL/APPROVED、线索 NEW/ASSIGNED/CONVERTED 等）；
 * 业务态 → 通用待办态的映射经 {@link TodoStatusMapper} 扩展点，D-07 确认后由专门 Mapper 注册。
 *
 * <p>终态（{@link #isTerminal()}）COMPLETED/WITHDRAWN/INVALID 不可被后到事件复活——终态守卫为应用层机制
 * （非 DB 状态流转约束），循 D-07「不把流转条件写成不可逆数据库约束」。REASSIGNED 为非终态（换人后可继续推进）。
 */
public enum NotifyTodoStatus {

    /** 待处理（注册初始态） */
    PENDING,

    /** 已完成（终态） */
    COMPLETED,

    /** 已撤回（终态） */
    WITHDRAWN,

    /** 已转派（非终态，换人后可继续被 COMPLETE/WITHDRAW 推进） */
    REASSIGNED,

    /** 已失效（终态） */
    INVALID;

    /** 是否终态——终态待办不被后到事件复活（应用层终态守卫依据）。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == WITHDRAWN || this == INVALID;
    }

}
