package cn.zszj.module.bpm.firstchain;

import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED;

/**
 * 首链领域状态机（ZS-BPM-003，D-07 M3/M6 已批准迁移表）。
 *
 * <p>迁移表为<b>唯一权威定义</b>（docs/09 M3/M6）：申请三态 DRAFT→SUBMITTED→APPROVED/REJECTED；
 * 线索五态 DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED/INVALID。fail-closed：未知对象类型/未知状态/
 * from=to 一律不可迁移（返回 false 不抛错，由 {@link #assertAllowed} 统一显式报错——附 from/to 便于回查）。
 * 终态锁定：APPROVED/REJECTED（申请）与 CONVERTED/INVALID（线索）不可迁出。
 *
 * @author ZS-BPM-003
 */
public final class FirstChainStateMachines {

    /**
     * 迁移表：objectType.key → from → 允许的 to 集合（D-07 M3/M6，禁止 from=to 自环——状态推进必须经版本条件更新）
     */
    private static final Map<String, Map<String, Set<String>>> TRANSITIONS = Map.of(
            FirstChainObjectType.APPLICATION.getKey(), Map.of(
                    FranchiseeApplicationStatus.DRAFT.name(), Set.of(FranchiseeApplicationStatus.SUBMITTED.name()),
                    FranchiseeApplicationStatus.SUBMITTED.name(), Set.of(
                            FranchiseeApplicationStatus.APPROVED.name(), FranchiseeApplicationStatus.REJECTED.name())),
            FirstChainObjectType.LEAD.getKey(), Map.of(
                    LeadStatus.DISTRIBUTED.name(), Set.of(LeadStatus.ASSIGNED.name()),
                    LeadStatus.ASSIGNED.name(), Set.of(LeadStatus.FOLLOWING.name()),
                    LeadStatus.FOLLOWING.name(), Set.of(LeadStatus.CONVERTED.name(), LeadStatus.INVALID.name())));

    /**
     * 终态集：objectType.key → 不可迁出的状态集合
     */
    private static final Map<String, Set<String>> TERMINALS = Map.of(
            FirstChainObjectType.APPLICATION.getKey(), Set.of(
                    FranchiseeApplicationStatus.APPROVED.name(), FranchiseeApplicationStatus.REJECTED.name()),
            FirstChainObjectType.LEAD.getKey(), Set.of(LeadStatus.CONVERTED.name(), LeadStatus.INVALID.name()));

    private FirstChainStateMachines() {
    }

    /**
     * 判断 from → to 是否为该对象类型的合法迁移（未知对象/未知状态/from=to 恒 false，fail-closed）。
     */
    public static boolean canTransition(FirstChainObjectType objectType, String from, String to) {
        if (objectType == null || from == null || to == null || from.equals(to)) {
            return false;
        }
        Set<String> allowed = TRANSITIONS.getOrDefault(objectType.getKey(), Map.of()).get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 判断对象当前状态是否终态（未知对象/未知状态返回 false——终态判定不放大可见性）。
     */
    public static boolean isTerminal(FirstChainObjectType objectType, String status) {
        if (objectType == null || status == null) {
            return false;
        }
        Set<String> terminals = TERMINALS.get(objectType.getKey());
        return terminals != null && terminals.contains(status);
    }

    /**
     * 断言迁移合法，否则抛 {@code FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED}（显式拒绝，附 from/to 便于回查）。
     */
    public static void assertAllowed(FirstChainObjectType objectType, String from, String to) {
        if (!canTransition(objectType, from, to)) {
            throw exception(FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED, from, to);
        }
    }

}
