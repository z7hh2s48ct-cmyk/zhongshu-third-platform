package cn.zszj.module.system.service.notify.channel;

/**
 * 渠道发送生命周期状态（ZS-MSG-004）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-004）：「将待发送、已受理、送达、失败与站内消息状态分开」——
 * 本枚举只承载<b>渠道侧</b>投递生命周期，与站内消息已读/未读（{@code system_notify_message.read_status}）、
 * 业务待办处理状态（{@code system_notify_todo.status}）、派发日志状态（{@code system_notify_send_log.status}）
 * 分别建模，互不推断：派发 SUCCESS 仅表示「已进入可靠投递管道」，不推定渠道送达。
 *
 * <p>状态机（对齐 docs/05 ZS-JOB-002/003 可恢复语义）：
 * <pre>
 *   PENDING ──提交受理──▶ ACCEPTED ──送达回执/回查──▶ DELIVERED
 *      │  ▲                  │  └──失败回执/回查──▶ FAILED
 *      │  └──回查未发出/人工重试─┘
 *      ├──明确拒绝──▶ FAILED
 *      └──超时/不确定──▶ UNKNOWN ──先回查──▶ DELIVERED / 重发(PENDING→…) / 仍未知(退避重试)
 * </pre>
 * 任意<b>非终态</b>（PENDING/ACCEPTED/UNKNOWN）收到送达回执均以<b>回执为权威</b>直接推进 DELIVERED（乱序回执不丢
 * 事实）；终态收到回执按一致性吸收（DUPLICATE）或登记矛盾（CONTRADICTION）留人工核实。
 */
public enum NotifyChannelSendStatus {

    /** 待发送：已入台账，尚未提交渠道（或回查确认未发出后待重发） */
    PENDING,

    /** 已受理：渠道已受理提交（携带渠道流水号），等待送达回执；受理 ≠ 送达 */
    ACCEPTED,

    /** 送达：送达回执或渠道回查确认已送达（终态） */
    DELIVERED,

    /** 失败：渠道明确拒绝（提交被拒 / 失败回执）；可人工重试（次数累计可追溯） */
    FAILED,

    /** 结果未知：提交超时或技术异常，无法确定渠道是否已发出——必须先回查再决定重发，禁止盲目重发 */
    UNKNOWN

}
