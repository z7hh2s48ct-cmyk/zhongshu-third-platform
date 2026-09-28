package cn.zszj.module.bpm.firstchain;

/**
 * 首链领域状态机（ZS-BPM-003，D-07 M3/M6 已批准迁移表）。
 *
 * <p>fail-closed：未知对象类型/未知状态一律不可迁移（不抛错返回 false，由
 * {@link #assertAllowed} 统一以 {@code FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED} 报错）。
 *
 * @author ZS-BPM-003
 */
public final class FirstChainStateMachines {

    private FirstChainStateMachines() {
    }

    /**
     * 判断 from → to 是否为该对象类型的合法迁移（含 from=to 恒 false——状态推进必须经版本条件更新）。
     */
    public static boolean canTransition(FirstChainObjectType objectType, String from, String to) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 判断对象当前状态是否终态（终态锁定：终态不可迁出）。
     */
    public static boolean isTerminal(FirstChainObjectType objectType, String status) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 断言迁移合法，否则抛 {@code FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED}（显式拒绝，附 from/to）。
     */
    public static void assertAllowed(FirstChainObjectType objectType, String from, String to) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

}
