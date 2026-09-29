package cn.zszj.module.firstchain.enums;

/**
 * 线索无效关闭原因（ZS-FC-002，D-07 M7 拍板枚举：无法联系/预算不符/非目标客户/重复线索/其他+说明）。
 *
 * <p>OTHER 时 reasonDetail 必填（服务端强制，{@code LEAD_INVALIDATE_REASON_DETAIL_REQUIRED}）。
 *
 * @author ZS-FC-002
 */
public enum LeadInvalidateReason {

    /** 无法联系 */
    UNREACHABLE,

    /** 预算不符 */
    BUDGET_MISMATCH,

    /** 非目标客户 */
    NOT_TARGET,

    /** 重复线索 */
    DUPLICATE,

    /** 其他（必填说明） */
    OTHER;

    /**
     * 校验枚举字符串合法（fail-closed：未知原因拒绝，不静默降级）。
     */
    public static LeadInvalidateReason of(String name) {
        for (LeadInvalidateReason reason : values()) {
            if (reason.name().equals(name)) {
                return reason;
            }
        }
        return null;
    }

}
