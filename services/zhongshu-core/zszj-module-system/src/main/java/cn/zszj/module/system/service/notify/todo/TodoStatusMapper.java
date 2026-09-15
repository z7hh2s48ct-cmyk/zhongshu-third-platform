package cn.zszj.module.system.service.notify.todo;

/**
 * D-07 业务态 → 通用待办流转映射扩展点（ZS-MSG-002）。
 *
 * <p>D-07 红线（docs/02 §6.5）：D-07「方向已采纳、细节待阶段 2 前确认」，禁止固化线索/申请等试点业务状态
 * 与流转条件为不可逆约束。故本卡只提供<b>扩展点</b>：业务来源的任务态字符串 → 通用 {@link TodoTransitionType}。
 * 试点业务态（加盟商申请 DRAFT/PENDING_APPROVAL/APPROVED、线索 NEW/ASSIGNED/CONVERTED 等）的映射待 D-07
 * 确认后由专门 Mapper 注册；{@link DefaultTodoStatusMapper} 只识别通用机制态标识，不硬编码任何试点业务态。
 */
public interface TodoStatusMapper {

    /**
     * 是否支持该业务来源（如 BPM / FILE / GENERIC）。
     * D-07 首链启用前只有 {@link DefaultTodoStatusMapper} 提供通用兜底映射。
     */
    boolean supports(String sourceType);

    /**
     * 把源业务任务态字符串映射为通用待办流转类型。
     *
     * @return 对应流转类型；无法识别（含 D-07 试点业务态）返回 {@code null}，由调用方决定处置
     *         （不静默映射为业务态，避免固化未确认的试点状态）。
     */
    TodoTransitionType mapTransition(String sourceType, String sourceTaskState);

}
