package cn.zszj.module.system.service.notify.todo;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * {@link TodoStatusMapper} 默认实现（ZS-MSG-002）——只识别<b>通用机制态标识</b>，不硬编码任何 D-07 试点业务态。
 *
 * <p>D-07 红线（docs/02 §6.5）：禁止把建议状态写成不可逆约束。本实现对通用机制态标识
 * （COMPLETED/WITHDRAWN/REASSIGNED/INVALID）做恒等映射；对未知源态（含 D-07 试点业务态如
 * PENDING_APPROVAL/APPROVED/CONVERTED/NEW）一律返回 {@code null}，由调用方决定处置——
 * <b>不静默映射</b>为业务流转，也就不会在首链启用前固化任何试点状态语义。
 */
@Component
public class DefaultTodoStatusMapper implements TodoStatusMapper {

    @Override
    public boolean supports(String sourceType) {
        // 通用兜底：任何来源均可尝试恒等映射（识别不了则返回 null）
        return true;
    }

    @Override
    public TodoTransitionType mapTransition(String sourceType, String sourceTaskState) {
        if (!StringUtils.hasText(sourceTaskState)) {
            return null;
        }
        switch (sourceTaskState.trim().toUpperCase(Locale.ROOT)) {
            case "COMPLETED":
                return TodoTransitionType.COMPLETE;
            case "WITHDRAWN":
                return TodoTransitionType.WITHDRAW;
            case "REASSIGNED":
                return TodoTransitionType.REASSIGN;
            case "INVALID":
                return TodoTransitionType.INVALIDATE;
            default:
                // D-07 试点业务态不在此识别——返回 null 交调用方决定（不固化未确认状态）
                return null;
        }
    }

}
