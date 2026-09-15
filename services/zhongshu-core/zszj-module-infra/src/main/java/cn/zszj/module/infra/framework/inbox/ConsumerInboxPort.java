package cn.zszj.module.infra.framework.inbox;

import java.util.List;
import java.util.Optional;

/**
 * 消费者业务幂等端口（ZS-JOB-003，机制层；领域规则按 D-07 阶段 2 细化）。
 *
 * <p>合同（docs/05 ZS-JOB-003 调整条款）：
 * <ol>
 *   <li><b>幂等记录与业务副作用同事务</b>：tryBegin/complete/fail/markResultUnknown 必须在调用方业务事务内
 *       调用（MANDATORY，无真实事务即拒绝）——业务回滚则抢位一并回滚；同事件并发、ACK 丢失、重启重放由
 *       处理键唯一约束（租户+消费者+事件键）DB 硬兜底，只产生一次业务副作用；</li>
 *   <li><b>与 Web 请求防重的边界</b>：本端口是持久化可重放业务幂等（COMPLETED 携首次可重放结果），
 *       不替代 ZS-SEC-011.A 的短窗口 Web 防重；联验归 ZS-SEC-011.B；</li>
 *   <li><b>乱序/回查</b>：同对象旧版本事件经版本护栏拒绝（STALE_VERSION）；外部结果不确定时以
 *       markResultUnknown 留可查中间态，经查询端口回查后人工/补偿处理，不得盲目重处理；</li>
 *   <li>技术租户一律取自当前上下文（缺失即拒绝），不同租户同业务号互不冲突（唯一键含租户）。</li>
 * </ol>
 */
public interface ConsumerInboxPort {

    /**
     * 在当前事务内抢位：新登记 PROCESSING（CLAIMED）、可重放既有结果、并发抢占识别、参数冲突与
     * 版本护栏判定（见 {@link InboxTryBegin} 各 Outcome 合同）。FAILED 记录重入即重领
     * （retry_count 为已记录失败次数，由 fail 递增，重领不重复递增）。
     */
    InboxTryBegin tryBegin(InboxCommand command);

    /**
     * 处理成功：标记 COMPLETED 并写入可重放业务结果（JSON 串，重复消费时原样返回）；
     * 仅 PROCESSING 可推进，返回 false 表示状态已被并发改变。须在当前租户上下文内调用（跨租户拒绝）。
     */
    boolean complete(long inboxId, String resultJson);

    /**
     * 处理失败：标记 FAILED 并留受控失败描述（异常类别/长度，不落原文——自由文本无可靠值级脱敏，
     * 循 ZS-JOB-002 last_error 惯例）；失败使 retry_count（已记录失败次数）+1，可经 tryBegin 重领重试。
     * 须在当前租户上下文内调用（跨租户拒绝）。
     */
    boolean fail(long inboxId, Throwable error);

    /**
     * 回查确认出口（codex r0 P1：RESULT_UNKNOWN 唯一合法出口）——外部结果经回查核实后推进：
     * executed=true → COMPLETED（result 为回查核实到的业务结果）；executed=false → FAILED（可重试）。
     * evidence 为回查依据（受控限长），仅 RESULT_UNKNOWN 可推进，返回 false 表示状态已被并发改变。
     */
    boolean resolveAfterVerification(long inboxId, boolean executed, String resultJson, String evidence);

    /**
     * 结果未知（外部调用后不确定）：标记 RESULT_UNKNOWN 可查中间态——必须先回查再决定处理，不得盲目重试；
     * 仅 PROCESSING 可推进，返回 false 表示状态已被并发改变。
     */
    boolean markResultUnknown(long inboxId, String reason);

    /** 按处理键查单条记录（回查入口）。须在当前租户上下文内调用（只查当前租户）；读取不受事务约束。 */
    Optional<InboxRecord> find(String consumer, String eventKey);

    /** 按消费者+状态列回查清单（如 RESULT_UNKNOWN/FAILED 台账，id 升序有界）。须在当前租户上下文内调用。 */
    List<InboxRecord> listByStatus(String consumer, String status, int limit);

}
