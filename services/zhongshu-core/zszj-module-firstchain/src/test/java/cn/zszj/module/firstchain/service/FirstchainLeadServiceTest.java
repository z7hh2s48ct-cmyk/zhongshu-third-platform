package cn.zszj.module.firstchain.service;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainStateTransitionExecutor;
import cn.zszj.module.firstchain.service.FirstchainLeadService.DistributeCmd;
import cn.zszj.module.firstchain.service.FirstchainLeadService.FollowupCmd;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_TRANSITION_NOT_ALLOWED;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_VERSION_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_LEAD_TERMINAL;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_NOT_ALLOWED_FOR_ACTOR;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ASSIGN_TARGET_NOT_IN_ORG;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CLAIM_NOT_ASSIGNEE;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_REQUIRE_CUSTOMER_NAME;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_REQUIRE_FOLLOWUP;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_DETAIL_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_KEY_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_REASSIGN_TARGET_NOT_IN_ORG;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_STATE_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_TENANT_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_VERSION_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.OPPORTUNITY_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FirstchainLeadService} 的单元测试（ZS-FC-002，H2）——首链线索闭环。
 *
 * <p>覆盖 docs/05 ZS-FC-002 验收：五态流转与分配/领取/改派（PILOT-REQ-005/006）、跟进记录追加与
 * 本人守卫（PILOT-REQ-007）、转商机与无效关闭条件（PILOT-REQ-008）、租户隔离与对象 ID 越权
 * （PILOT-REQ-009 服务端范围）、并发/重复幂等与版本乐观锁（M9）。流程无关（线索域不经审批流，
 * 与 BPM-003 的 PG 真实引擎联验边界分离）。
 *
 * <p>循 ZS-BPM-003 {@code FirstChainApplicationServiceTest} 基建先例：H2 + BaseDbUnitTest +
 * @Import 显式装配（JdbcAuditPort + UserOrgChecker 桩）+ JdbcTemplate 直查断言。
 *
 * @author ZS-FC-002
 */
@Import({FirstchainLeadService.class, FirstchainLeadMetricsService.class, FirstChainStateTransitionExecutor.class,
        JdbcAuditPort.class, FirstchainLeadServiceTest.FixedOrgChecker.class})
class FirstchainLeadServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    private static final Long OTHER_TENANT_ID = 2L;

    private static final Long ORG_ID = 100L;

    private static final Long EMPLOYEE_ID = 300L;

    @Resource
    private FirstchainLeadService leadService;

    @Resource
    private FirstchainLeadMetricsService metricsService;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /** 归属校验桩：300/301 属于组织 100，其余全拒（不查 system，H2 单测自持）。 */
    public static class FixedOrgChecker implements FirstchainLeadService.UserOrgChecker {

        @Override
        public boolean isUserInOrg(Long tenantId, Long userId, Long orgId) {
            return TENANT_ID.equals(tenantId) && ORG_ID.equals(orgId)
                    && (EMPLOYEE_ID.equals(userId) || Long.valueOf(301L).equals(userId));
        }

    }

    // ========== PILOT-REQ-005：下发与幂等 ==========

    @Test
    public void testDistribute_success() {
        Long id = leadService.distribute(distributeCmd("LD-001"));
        assertThat(id).isNotNull();
        Map<String, Object> lead = jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_lead WHERE id = ?", id);
        assertThat(lead.get("status")).isEqualTo("DISTRIBUTED");
        assertThat(((Number) lead.get("version")).longValue()).isEqualTo(0L);
        assertThat(((Number) lead.get("org_id")).longValue()).isEqualTo(ORG_ID);
        // 同事务 SUCCESS 审计
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE biz_type = 'lead' AND action = 'DISTRIBUTE' AND result = 'SUCCESS'",
                Integer.class);
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    public void testDuplicateDistribute_idempotent() {
        Long first = leadService.distribute(distributeCmd("LD-002"));
        Long second = leadService.distribute(distributeCmd("LD-002")); // 同键同内容 → 吸收
        assertThat(second).isEqualTo(first);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_lead WHERE lead_key = 'LD-002'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testDistribute_sameKeyDifferentContent_conflict() {
        leadService.distribute(distributeCmd("LD-003"));
        assertServiceException(() -> leadService.distribute(new DistributeCmd(
                        "LD-003", "其他客户", "13900000000", null, null, "PLATFORM", ORG_ID, "actor-900")),
                LEAD_KEY_CONFLICT, "LD-003");
    }

    @Test
    public void testDistribute_orgIdMissing_rejected() {
        assertServiceException(() -> leadService.distribute(new DistributeCmd(
                        "LD-004", "客户", "13900000000", null, null, "PLATFORM", null, "actor-900")),
                LEAD_TENANT_REQUIRED);
    }

    // ========== PILOT-REQ-006：分配/领取/改派 ==========

    @Test
    public void testAssign_claim_flow() {
        Long leadId = leadService.distribute(distributeCmd("LD-010"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        Map<String, Object> afterAssign = jdbcTemplate.queryForMap(
                "SELECT status, assignee_user_id, version FROM bpm_first_chain_lead WHERE id = ?", leadId);
        assertThat(afterAssign.get("status")).isEqualTo("ASSIGNED");
        assertThat(((Number) afterAssign.get("assignee_user_id")).longValue()).isEqualTo(EMPLOYEE_ID);
        assertThat(((Number) afterAssign.get("version")).longValue()).isEqualTo(1L);
        // 员工本人领取成功
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bpm_first_chain_lead WHERE id = ?", String.class, leadId))
                .isEqualTo("FOLLOWING");
    }

    @Test
    public void testAssign_targetNotInOrg_rejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-011"));
        assertServiceException(() -> leadService.assign(leadId, 999L, 0L, "actor-900"),
                LEAD_ASSIGN_TARGET_NOT_IN_ORG);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bpm_first_chain_lead WHERE id = ?", String.class, leadId))
                .isEqualTo("DISTRIBUTED"); // 无副作用
    }

    @Test
    public void testClaim_notAssignee_rejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-012"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        assertServiceException(() -> leadService.claim(leadId, 301L, 1L, "actor-900"),
                LEAD_CLAIM_NOT_ASSIGNEE);
    }

    @Test
    public void testClaim_afterFollowing_stateConflict() {
        Long leadId = leadService.distribute(distributeCmd("LD-013"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        // 已 FOLLOWING 后重复领取（并发领取唯一性的单测形态）：版本已进 2，旧期望版本 0 行读回 → 执行器版本冲突分类
        assertServiceException(() -> leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900"),
                FIRST_CHAIN_VERSION_CONFLICT, 1L);
    }

    @Test
    public void testReassign_keepsStatusAndBumpsVersion() {
        Long leadId = leadService.distribute(distributeCmd("LD-014"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.reassign(leadId, 301L, 1L, "actor-900");
        Map<String, Object> lead = jdbcTemplate.queryForMap(
                "SELECT status, assignee_user_id, version FROM bpm_first_chain_lead WHERE id = ?", leadId);
        assertThat(lead.get("status")).isEqualTo("ASSIGNED"); // 改派不改状态
        assertThat(((Number) lead.get("assignee_user_id")).longValue()).isEqualTo(301L);
        assertThat(((Number) lead.get("version")).longValue()).isEqualTo(2L);
        // 改派目标不在本组织拒绝
        assertServiceException(() -> leadService.reassign(leadId, 999L, 2L, "actor-900"),
                LEAD_REASSIGN_TARGET_NOT_IN_ORG);
    }

    @Test
    public void testReassign_staleVersion_conflict() {
        Long leadId = leadService.distribute(distributeCmd("LD-015"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        assertServiceException(() -> leadService.reassign(leadId, 301L, 0L, "actor-900"), // 期望版本已过期
                LEAD_VERSION_CONFLICT, 0L);
    }

    // ========== PILOT-REQ-007：跟进记录 ==========

    @Test
    public void testFollowup_byAssignee_success() {
        Long leadId = leadService.distribute(distributeCmd("LD-020"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        Long followupId = leadService.followup(leadId, new FollowupCmd(
                "电话沟通顺利", "下周上门", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        assertThat(followupId).isNotNull();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_followup WHERE lead_id = ?", Integer.class, leadId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testFollowup_byNonAssignee_rejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-021"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        assertServiceException(() -> leadService.followup(leadId, new FollowupCmd(
                        "越权跟进", null, 301L, LocalDateTime.now(), "actor-900")),
                FOLLOWUP_NOT_ALLOWED_FOR_ACTOR);
    }

    @Test
    public void testFollowup_afterTerminal_rejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-022"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        leadService.followup(leadId, new FollowupCmd("跟进", "下一步", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        leadService.invalidate(leadId, "UNREACHABLE", null, 2L, "actor-900");
        assertServiceException(() -> leadService.followup(leadId, new FollowupCmd(
                        "终态后跟进", null, EMPLOYEE_ID, LocalDateTime.now(), "actor-900")),
                FOLLOWUP_LEAD_TERMINAL, "INVALID");
    }

    // ========== PILOT-REQ-008：转商机与无效关闭 ==========

    @Test
    public void testConvert_success() {
        Long leadId = leadService.distribute(distributeCmd("LD-030"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        leadService.followup(leadId, new FollowupCmd("意向明确", "报价跟进", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        Long opportunityId = leadService.convertToOpportunity(leadId, "OPP-030", 2L, "actor-900");
        assertThat(opportunityId).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bpm_first_chain_lead WHERE id = ?", String.class, leadId))
                .isEqualTo("CONVERTED"); // 终态锁定
        Map<String, Object> opportunity = jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_opportunity WHERE id = ?", opportunityId);
        assertThat(((Number) opportunity.get("lead_id")).longValue()).isEqualTo(leadId); // 双向引用
        assertThat(((Number) jdbcTemplate.queryForObject(
                "SELECT version FROM bpm_first_chain_lead WHERE id = ?", Integer.class, leadId)).longValue())
                .isEqualTo(3L);
    }

    @Test
    public void testConvert_withoutFollowup_rejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-031"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        assertServiceException(() -> leadService.convertToOpportunity(leadId, "OPP-031", 2L, "actor-900"),
                LEAD_CONVERT_REQUIRE_FOLLOWUP);
    }

    @Test
    public void testConvert_withoutCustomerName_rejected() {
        Long leadId = leadService.distribute(new DistributeCmd(
                "LD-032", null, "13900000000", null, null, "PLATFORM", ORG_ID, "actor-900"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        leadService.followup(leadId, new FollowupCmd("跟进", "下一步", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        assertServiceException(() -> leadService.convertToOpportunity(leadId, "OPP-032", 2L, "actor-900"),
                LEAD_CONVERT_REQUIRE_CUSTOMER_NAME);
    }

    @Test
    public void testConvertTwice_secondRejected() {
        Long leadId = leadService.distribute(distributeCmd("LD-033"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        leadService.followup(leadId, new FollowupCmd("跟进", "下一步", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        leadService.convertToOpportunity(leadId, "OPP-033", 2L, "actor-900");
        // 重复转化（晚到方基于最新版本）：fromStatus=ASSIGNED？——执行器按 FOLLOWING 守卫，实际已 CONVERTED → 状态冲突分类（并发下 uk(lead_id) 兜底）
        assertServiceException(() -> leadService.convertToOpportunity(leadId, "OPP-033B", 3L, "actor-900"),
                FIRST_CHAIN_STATE_CONFLICT, "CONVERTED");
        Integer oppCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_opportunity WHERE lead_id = ?", Integer.class, leadId);
        assertThat(oppCount).isEqualTo(1); // 单商机
    }

    @Test
    public void testInvalidate_successAndReasonGuards() {
        Long leadId = leadService.distribute(distributeCmd("LD-040"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");
        leadService.followup(leadId, new FollowupCmd("跟进", "下一步", EMPLOYEE_ID, LocalDateTime.now(), "actor-900"));
        // 无原因拒绝
        assertServiceException(() -> leadService.invalidate(leadId, null, null, 2L, "actor-900"),
                LEAD_INVALIDATE_REASON_REQUIRED);
        // OTHER 无说明拒绝
        assertServiceException(() -> leadService.invalidate(leadId, "OTHER", null, 2L, "actor-900"),
                LEAD_INVALIDATE_REASON_DETAIL_REQUIRED);
        // 合法关闭
        leadService.invalidate(leadId, "BUDGET_MISMATCH", null, 2L, "actor-900");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bpm_first_chain_lead WHERE id = ?", String.class, leadId))
                .isEqualTo("INVALID");
        // 终态锁定：INVALID 行不再满足领取的 ASSIGNED 前置 → 执行器读回状态冲突（fail-closed）
        assertServiceException(() -> leadService.claim(leadId, EMPLOYEE_ID, 3L, "actor-900"),
                FIRST_CHAIN_STATE_CONFLICT, "INVALID");
    }

    // ========== PILOT-REQ-009：租户隔离与越权 ==========

    @Test
    public void testCrossTenant_notExists() {
        Long leadId = leadService.distribute(distributeCmd("LD-050"));
        TenantContextHolder.setTenantId(OTHER_TENANT_ID); // 另一租户上下文
        assertServiceException(() -> leadService.getLead(leadId), LEAD_NOT_EXISTS);
        assertServiceException(() -> leadService.claim(leadId, EMPLOYEE_ID, 0L, "actor-900"), LEAD_NOT_EXISTS);
    }

    @Test
    public void testNoTenantContext_rejected() {
        TenantContextHolder.clear();
        assertServiceException(() -> leadService.distribute(distributeCmd("LD-051")), LEAD_TENANT_REQUIRED);
    }

    // ========== 指标（三视角同源） ==========

    @Test
    public void testMetrics_fromAuthoritativeStatusColumn() {
        leadService.distribute(distributeCmd("LD-060"));
        Long leadId = leadService.distribute(distributeCmd("LD-061"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "actor-900");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "actor-900");

        // 平台全域口径
        Map<String, Long> all = metricsService.countByStatus(TENANT_ID);
        assertThat(all.get("DISTRIBUTED")).isEqualTo(1L);
        assertThat(all.get("FOLLOWING")).isEqualTo(1L);
        // 负责人视角（本组织口径）
        Map<String, Long> byOrg = metricsService.countByStatusAndOrg(TENANT_ID, ORG_ID);
        assertThat(byOrg.get("DISTRIBUTED")).isEqualTo(1L);
        assertThat(byOrg.get("FOLLOWING")).isEqualTo(1L);
        // 员工视角（本人口径）：未分配的不进本人范围
        Map<String, Long> byAssignee = metricsService.countByStatusAndAssignee(TENANT_ID, EMPLOYEE_ID);
        assertThat(byAssignee.get("FOLLOWING")).isEqualTo(1L);
        assertThat(byAssignee.containsKey("DISTRIBUTED")).isFalse();
    }

    private DistributeCmd distributeCmd(String leadKey) {
        return new DistributeCmd(leadKey, "客户甲", "13900001111", "wx_001", "上海市", "PLATFORM", ORG_ID, "actor-900");
    }

}
