package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APP_KEY_EXISTS;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_BINDING_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_NOT_BOUND;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_REJECT_REASON_REQUIRED;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_TENANT_REQUIRED;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_VERSION_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FirstChainApplicationService} 的单元测试（ZS-BPM-003，H2）——首链领域状态与幂等写回。
 *
 * <p>覆盖 docs/05 ZS-BPM-003 验收：通过/拒绝/撤回与业务状态一致；**重复回调、旧流程晚到、并发审批/撤回
 * 不重复建对象或覆盖新版本**；失败可回查（错误码分类 + 审计留痕）；指标从领域事实派生。流程引擎经
 * {@link FirstChainProcessPort} 桩隔离（真实 Flowable+PG 联验归 run-bpm003-verify.mjs 套件）。
 *
 * <p>循 ZS-BPM-004 合同样例测试先例：H2 + BaseDbUnitTest + @Import 显式装配（JdbcAuditPort）+
 * JdbcTemplate 直查断言。
 *
 * @author ZS-BPM-003
 */
@Import({FirstChainApplicationService.class, FirstChainProcessBindingService.class,
        FirstChainStateTransitionExecutor.class, FirstChainMetricsService.class, JdbcAuditPort.class,
        FirstChainApplicationServiceTest.StubProcessPort.class})
class FirstChainApplicationServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    private static final Long OTHER_TENANT_ID = 2L;

    private static final Long APPROVER_ID = 200L;

    @Resource
    private FirstChainApplicationService applicationService;

    @Resource
    private FirstChainMetricsService metricsService;

    @Resource
    private FirstChainProcessPort processPort;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        ((StubProcessPort) processPort).reset();
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ========== ① 创建（M3 DRAFT 基线） ==========

    @Test
    void create_draftBaselineWithAudit() {
        Long id = createApplication("APP-001");
        assertThat(id).isNotNull();
        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.DRAFT.name());
        assertThat(((Number) row.get("version")).longValue()).isZero();
        assertThat(((Number) row.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        assertThat(queryAuditCount("OBJECT_CREATED", "franchisee_application", id)).isEqualTo(1);
    }

    @Test
    void create_duplicateAppKey_sameTenant_rejected() {
        createApplication("APP-DUP");
        assertServiceException(() -> createApplication("APP-DUP"), FIRST_CHAIN_APP_KEY_EXISTS);
    }

    @Test
    void create_withoutTenant_failClosed() {
        TenantContextHolder.clear();
        assertServiceException(() -> createApplication("APP-NO-TENANT"), FIRST_CHAIN_TENANT_REQUIRED);
    }

    // ========== ② 提交（DRAFT→SUBMITTED + 流程绑定，M3/M10） ==========

    @Test
    void submit_draftToSubmitted_withBindingAndProcess() {
        Long id = createApplication("APP-S1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(1L);
        // 流程绑定：BOUND + 流程已发起（桩记录一次）
        Map<String, Object> binding = queryActiveBinding(id);
        assertThat(binding.get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_BOUND);
        assertThat(binding.get("process_instance_id")).isEqualTo("PINST-1");
        assertThat(((StubProcessPort) processPort).startCalls).hasSize(1);
        assertThat(((StubProcessPort) processPort).startCalls.get(0).approverUserId).isEqualTo(APPROVER_ID);
        assertThat(queryAuditCount("OBJECT_UPDATED", "franchisee_application", id)).isEqualTo(1);
    }

    @Test
    void submit_staleVersion_versionConflict() {
        Long id = createApplication("APP-S2");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        // 携带旧版本重复提交 → 版本冲突（可回查分类）
        assertServiceException(() -> applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1"),
                FIRST_CHAIN_VERSION_CONFLICT);
    }

    @Test
    void submit_alreadySubmitted_stateConflict() {
        Long id = createApplication("APP-S3");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        // 携带当前版本对 SUBMITTED 再提交 → 状态冲突（状态机 DRAFT→SUBMITTED 不复现）
        assertServiceException(() -> applicationService.submitApplication(id, 1L, APPROVER_ID, "creator-1"),
                FIRST_CHAIN_STATE_CONFLICT);
    }

    @Test
    void submit_crossTenant_notExists() {
        Long id = createApplication("APP-S4");
        TenantContextHolder.setTenantId(OTHER_TENANT_ID);
        assertServiceException(() -> applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1"),
                FIRST_CHAIN_APPLICATION_NOT_EXISTS);
    }

    // ========== ③ 审批回调（幂等命令门：重复/晚到/并发，验收②） ==========

    @Test
    void approval_completed_transitionsToApproved() {
        Long id = createApplication("APP-A1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");

        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.APPROVED.name());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(2L);
        Map<String, Object> binding = queryActiveBinding(id);
        assertThat(binding.get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_COMPLETED);
        assertThat(binding.get("outcome")).isEqualTo(FirstChainProcessBindingService.OUTCOME_APPROVED);
    }

    @Test
    void approval_rejectedWithoutReason_rejected() {
        Long id = createApplication("APP-A2");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");

        assertServiceException(() -> applicationService.onApprovalCompleted(processInstanceId, false, null,
                "approver-1"), FIRST_CHAIN_REJECT_REASON_REQUIRED);
        // 拒绝未生效：领域仍 SUBMITTED、绑定仍 BOUND（事务整体回滚，可重试补意见）
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(queryActiveBinding(id).get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_BOUND);
    }

    @Test
    void approval_rejectedWithReason_persistedAndAudited() {
        Long id = createApplication("APP-A3");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");

        applicationService.onApprovalCompleted(processInstanceId, false, "资质材料不全", "approver-1");

        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.REJECTED.name());
        assertThat(row.get("reject_reason")).isEqualTo("资质材料不全");
        // 原因进入审计（M3 必填意见留痕）
        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "SELECT * FROM audit_event WHERE biz_id = ? AND action = 'REJECT' AND result = 'SUCCESS'",
                String.valueOf(id));
        assertThat(audit.get("reason")).isEqualTo("资质材料不全");
    }

    @Test
    void approval_duplicateCallback_idempotentNoSecondTransition() {
        Long id = createApplication("APP-A4");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        // 同一结果重复回调（at-least-once 重投）：幂等吸收，不产生第二次流转与重复成功审计
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        assertThat(((Number) queryApplication(id).get("version")).longValue()).isEqualTo(2L);
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.APPROVED.name());
        assertThat(queryAuditCount("OBJECT_UPDATED", "franchisee_application", id)).isEqualTo(2L); // SUBMIT + APPROVE 各一次
    }

    @Test
    void approval_concurrentWithdrawAfterCompleted_bindingConflict() {
        Long id = createApplication("APP-A5");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        // 审批完成后撤回到达（并发审批/撤回竞争）：绑定门 0 行即冲突，领域终态不被覆盖
        assertServiceException(() -> applicationService.withdrawApproval(id, 2L, "creator-1"),
                FIRST_CHAIN_PROCESS_BINDING_CONFLICT);
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.APPROVED.name());
    }

    @Test
    void approval_lateDifferentOutcome_discardedNotOverwrite() {
        Long id = createApplication("APP-A6");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        // 旧流程晚到（模拟 at-least-once 重放窗口：绑定被重放回 BOUND，携带不同结果到达）
        jdbcTemplate.update("UPDATE bpm_first_chain_process_binding SET status = 'BOUND', outcome = NULL "
                + "WHERE process_instance_id = ?", processInstanceId);
        applicationService.onApprovalCompleted(processInstanceId, false, "晚到的拒绝", "approver-1");

        // 领域权威胜出：不覆盖新版本、不异常；弃单留痕（可回查）
        assertThat(queryApplication(id).get("status")).isEqualTo(FranchiseeApplicationStatus.APPROVED.name());
        assertThat(((Number) queryApplication(id).get("version")).longValue()).isEqualTo(2L);
        assertThat(queryAuditCount(FirstChainProcessBindingService.EVENT_RESULT_DISCARDED,
                "franchisee_application", id)).isEqualTo(1);
    }

    @Test
    void approval_unknownProcess_notBound() {
        assertServiceException(() -> applicationService.onApprovalCompleted("PINST-UNKNOWN", true, null,
                "approver-1"), FIRST_CHAIN_PROCESS_NOT_BOUND);
    }

    // ========== ④ 撤回（M3：撤回不改领域状态，仅解绑审批流可重发） ==========

    @Test
    void withdraw_releasesBinding_domainStaysSubmitted() {
        Long id = createApplication("APP-W1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");

        applicationService.withdrawApproval(id, 1L, "creator-1");

        Map<String, Object> row = queryApplication(id);
        assertThat(row.get("status")).isEqualTo(FranchiseeApplicationStatus.SUBMITTED.name());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(1L); // 撤回不改领域状态
        Map<String, Object> binding = queryActiveBinding(id);
        assertThat(binding.get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_WITHDRAWN);
        assertThat(((StubProcessPort) processPort).withdrawCalls).containsExactly("PINST-1");
        // 撤回后可重新发起审批（新绑定 + 新流程，旧绑定留痕不动）
        applicationService.restartApproval(id, 1L, APPROVER_ID, "creator-1");
        Map<String, Object> newBinding = queryActiveBinding(id);
        assertThat(newBinding.get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_BOUND);
        assertThat(newBinding.get("process_instance_id")).isEqualTo("PINST-2");
        assertThat(((StubProcessPort) processPort).startCalls).hasSize(2);
    }

    @Test
    void withdraw_withoutActiveBinding_notBound() {
        Long id = createApplication("APP-W2");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = (String) queryActiveBinding(id).get("process_instance_id");
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");

        assertServiceException(() -> applicationService.withdrawApproval(id, 2L, "creator-1"),
                FIRST_CHAIN_PROCESS_NOT_BOUND);
    }

    @Test
    void withdraw_duplicateCallback_idempotent() {
        Long id = createApplication("APP-W3");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        applicationService.withdrawApproval(id, 1L, "creator-1");

        // 同一撤回重复到达：幂等吸收不冲突
        applicationService.withdrawApproval(id, 1L, "creator-1");
        assertThat(queryActiveBinding(id).get("status")).isEqualTo(FirstChainProcessBindingService.STATUS_WITHDRAWN);
    }

    // ========== ⑤ 指标派生（验收③：口径来自领域事实） ==========

    @Test
    void metrics_derivedFromAuthoritativeState() {
        Long submitted = createApplication("APP-M1");
        applicationService.submitApplication(submitted, 0L, APPROVER_ID, "creator-1");
        Long approved = createApplication("APP-M2");
        applicationService.submitApplication(approved, 0L, APPROVER_ID, "creator-1");
        applicationService.onApprovalCompleted((String) queryActiveBinding(approved).get("process_instance_id"),
                true, null, "approver-1");
        createApplication("APP-M3"); // 停留 DRAFT

        Map<String, Long> counts = metricsService.applicationStateCounts();
        assertThat(counts.get(FranchiseeApplicationStatus.DRAFT.name())).isEqualTo(1L);
        assertThat(counts.get(FranchiseeApplicationStatus.SUBMITTED.name())).isEqualTo(1L);
        assertThat(counts.get(FranchiseeApplicationStatus.APPROVED.name())).isEqualTo(1L);
        assertThat(counts.get(FranchiseeApplicationStatus.REJECTED.name())).isNull();
    }

    // ========== 测试辅助 ==========

    private Long createApplication(String appKey) {
        return applicationService.createApplication(CreateApplicationCmd.builder()
                .appKey(appKey)
                .applicantName("申请方-" + appKey)
                .contactName("联系人")
                .contactPhone("13800000000")
                .attachmentFileIds("[101]")
                .actorId("creator-1")
                .build());
    }

    private Map<String, Object> queryApplication(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_application WHERE id = ? AND tenant_id = ?", id, TENANT_ID);
    }

    private Map<String, Object> queryActiveBinding(Long applicationId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_process_binding WHERE domain_type = ? AND domain_id = ?",
                FirstChainObjectType.APPLICATION.getKey(), applicationId);
    }

    private long queryAuditCount(String eventType, String bizType, Long bizId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = ? AND biz_type = ? AND biz_id = ? AND result = 'SUCCESS'",
                Long.class, eventType, bizType, String.valueOf(bizId));
        return count == null ? 0 : count;
    }

    /** 流程端口桩：记录 start/withdraw 调用，流程实例号按序生成（引擎隔离，真实 Flowable 归 PG 套件）。 */
    public static class StubProcessPort implements FirstChainProcessPort {

        final List<StartRecord> startCalls = new java.util.ArrayList<>();
        final List<String> withdrawCalls = new java.util.ArrayList<>();
        private int sequence = 0;

        record StartRecord(Long tenantId, String appKey, Long approverUserId) {
        }

        void reset() {
            startCalls.clear();
            withdrawCalls.clear();
            sequence = 0;
        }

        @Override
        public String startApprovalProcess(Long tenantId, String appKey, Long approverUserId) {
            sequence++;
            startCalls.add(new StartRecord(tenantId, appKey, approverUserId));
            return "PINST-" + sequence;
        }

        @Override
        public void withdrawProcess(String processInstanceId) {
            withdrawCalls.add(processInstanceId);
        }

    }

}
