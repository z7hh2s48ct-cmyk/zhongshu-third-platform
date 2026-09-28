package cn.zszj.module.bpm.firstchain;

import org.junit.jupiter.api.Test;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FirstChainStateMachines} 的单元测试（ZS-BPM-003，D-07 M3/M6 状态机定义）。
 *
 * <p>覆盖：加盟商申请三态（M3：DRAFT→SUBMITTED→APPROVED/REJECTED）与线索五态（M6：
 * DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED/INVALID）的合法迁移表、终态锁定、未知状态 fail-closed。
 *
 * @author ZS-BPM-003
 */
class FirstChainStateMachinesTest {

    // ========== 加盟商申请三态（D-07 M3） ==========

    @Test // 合法主链：DRAFT→SUBMITTED→APPROVED / SUBMITTED→REJECTED
    void application_happyPathTransitions_allowed() {
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.DRAFT.name(), FranchiseeApplicationStatus.SUBMITTED.name()));
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.SUBMITTED.name(), FranchiseeApplicationStatus.APPROVED.name()));
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.SUBMITTED.name(), FranchiseeApplicationStatus.REJECTED.name()));
    }

    @Test // 跨态与回退拒绝：DRAFT 不可直达 APPROVED；终态不可迁出
    void application_skipAndTerminalTransitions_rejected() {
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.DRAFT.name(), FranchiseeApplicationStatus.APPROVED.name()));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.DRAFT.name(), FranchiseeApplicationStatus.REJECTED.name()));
        for (String from : new String[]{FranchiseeApplicationStatus.APPROVED.name(),
                FranchiseeApplicationStatus.REJECTED.name()}) {
            for (String to : new String[]{FranchiseeApplicationStatus.DRAFT.name(),
                    FranchiseeApplicationStatus.SUBMITTED.name(), from}) {
                assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION, from, to),
                        "终态 " + from + " → " + to + " 必须拒绝");
            }
        }
    }

    @Test // isTerminal：APPROVED/REJECTED 终态；DRAFT/SUBMITTED 非终态
    void application_terminalFlags() {
        assertTrue(FirstChainStateMachines.isTerminal(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.APPROVED.name()));
        assertTrue(FirstChainStateMachines.isTerminal(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.REJECTED.name()));
        assertFalse(FirstChainStateMachines.isTerminal(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.DRAFT.name()));
        assertFalse(FirstChainStateMachines.isTerminal(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.SUBMITTED.name()));
    }

    @Test // 未知状态 fail-closed（不抛异常返回 false，由 assertAllowed 统一报错）
    void unknownStatus_failClosed() {
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION, "UNKNOWN", "DRAFT"));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION, "DRAFT", "UNKNOWN"));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.APPLICATION, null, "DRAFT"));
    }

    @Test // assertAllowed：非法迁移抛 STATE_TRANSITION_NOT_ALLOWED；合法迁移不抛
    void assertAllowed_serviceException() {
        assertServiceException(() -> FirstChainStateMachines.assertAllowed(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.APPROVED.name(), FranchiseeApplicationStatus.SUBMITTED.name()),
                FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED);
        FirstChainStateMachines.assertAllowed(FirstChainObjectType.APPLICATION,
                FranchiseeApplicationStatus.DRAFT.name(), FranchiseeApplicationStatus.SUBMITTED.name());
    }

    // ========== 线索五态（D-07 M6） ==========

    @Test // 合法主链：DISTRIBUTED→ASSIGNED→FOLLOWING→CONVERTED / INVALID
    void lead_happyPathTransitions_allowed() {
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.DISTRIBUTED.name(), LeadStatus.ASSIGNED.name()));
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.ASSIGNED.name(), LeadStatus.FOLLOWING.name()));
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.FOLLOWING.name(), LeadStatus.CONVERTED.name()));
        assertTrue(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.FOLLOWING.name(), LeadStatus.INVALID.name()));
    }

    @Test // 跳态拒绝：不可越过分配直接跟进/转商机；ASSIGNED 不可直接 CONVERTED（D-07 M7 前置 ≥1 跟进）
    void lead_skipTransitions_rejected() {
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.DISTRIBUTED.name(), LeadStatus.FOLLOWING.name()));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.DISTRIBUTED.name(), LeadStatus.CONVERTED.name()));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.ASSIGNED.name(), LeadStatus.CONVERTED.name()));
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.ASSIGNED.name(), LeadStatus.INVALID.name()));
    }

    @Test // 线索终态锁定：CONVERTED/INVALID 不可迁出
    void lead_terminalLocked() {
        for (String from : new String[]{LeadStatus.CONVERTED.name(), LeadStatus.INVALID.name()}) {
            for (String to : new String[]{LeadStatus.DISTRIBUTED.name(), LeadStatus.ASSIGNED.name(),
                    LeadStatus.FOLLOWING.name(), LeadStatus.CONVERTED.name(), LeadStatus.INVALID.name()}) {
                assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD, from, to),
                        "线索终态 " + from + " → " + to + " 必须拒绝");
            }
        }
        assertTrue(FirstChainStateMachines.isTerminal(FirstChainObjectType.LEAD, LeadStatus.CONVERTED.name()));
        assertTrue(FirstChainStateMachines.isTerminal(FirstChainObjectType.LEAD, LeadStatus.INVALID.name()));
        assertFalse(FirstChainStateMachines.isTerminal(FirstChainObjectType.LEAD, LeadStatus.FOLLOWING.name()));
    }

    @Test // 对象类型隔离：申请表迁移不适用于线索（fail-closed）
    void objectType_isolation() {
        // SUBMITTED 只存在于申请域；线索域查无此状态 → false
        assertFalse(FirstChainStateMachines.canTransition(FirstChainObjectType.LEAD,
                LeadStatus.FOLLOWING.name(), FranchiseeApplicationStatus.SUBMITTED.name()));
        assertEquals("franchisee_application", FirstChainObjectType.APPLICATION.getKey());
        assertEquals("lead", FirstChainObjectType.LEAD.getKey());
    }

}
