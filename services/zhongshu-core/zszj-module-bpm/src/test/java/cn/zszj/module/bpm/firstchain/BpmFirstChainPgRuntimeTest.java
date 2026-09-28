package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.bpm.harness.BpmPgHarnessConfiguration;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ZS-BPM-003 真实 PostgreSQL + 真实 Flowable 引擎运行时验证（由 scripts/db/run-bpm003-verify.mjs 注入
 * {@code ZSZJ_BPM_HARNESS_*} 环境变量调用；缺环境快速失败，不静默跳过）。
 *
 * <p>覆盖 docs/05 ZS-BPM-003 验收的引擎侧合同：首链通过/拒绝/撤回/转派四操作与业务状态一致（验收①）、
 * 重复回调/旧流程晚到/并发审批撤回不重复建对象或覆盖新版本（验收②，含真实双线程门竞争）、
 * 审计与弃单留痕落真实 PG（验收③口径来源可回查）。
 *
 * @author ZS-BPM-003
 */
@SpringBootTest(classes = {BpmPgHarnessConfiguration.class, FirstChainBpmConfiguration.class,
        JdbcAuditPort.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BpmFirstChainPgRuntimeTest {

    private static final Long TENANT_ID = 1L;

    private static final Long APPROVER_ID = 200L;

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AuditPort auditPort;
    @Autowired
    private FirstChainApplicationService applicationService;
    @Autowired
    private FirstChainProcessPort processPort;
    @Autowired
    private FirstChainProcessBindingService bindingService;

    @BeforeAll
    void setUpSchemaAndContext() {
        // 结构自备（IF NOT EXISTS 幂等）：引擎 act_* 表由 harness bootstrap（schemaUpdate=true）创建；
        // 领域两表照抄迁移 V20260928.001，audit_event 照抄 system 侧 DDL（JdbcAuditPort 所需列）
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS bpm_first_chain_application (
                    id            bigserial PRIMARY KEY,
                    app_key       varchar(64)  NOT NULL,
                    applicant_name varchar(128) NOT NULL,
                    contact_name  varchar(64),
                    contact_phone varchar(32),
                    attachment_file_ids varchar(512),
                    reject_reason varchar(1024),
                    status        varchar(32)  NOT NULL,
                    version       bigint       NOT NULL DEFAULT 0,
                    tenant_id     bigint       NOT NULL,
                    creator       varchar(64)  DEFAULT '',
                    create_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updater       varchar(64)  DEFAULT '',
                    update_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted       boolean      NOT NULL DEFAULT FALSE
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS bpm_first_chain_process_binding (
                    id                  bigserial PRIMARY KEY,
                    domain_type         varchar(32) NOT NULL,
                    domain_id           bigint      NOT NULL,
                    app_key             varchar(64) NOT NULL,
                    process_instance_id varchar(64) NOT NULL,
                    outcome             varchar(32),
                    status              varchar(24) NOT NULL,
                    tenant_id           bigint      NOT NULL,
                    creator             varchar(64) DEFAULT '',
                    create_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updater             varchar(64) DEFAULT '',
                    update_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_event (
                    id bigserial PRIMARY KEY,
                    event_type varchar(64) NOT NULL,
                    actor_type varchar(32),
                    actor_id varchar(64),
                    action varchar(64),
                    biz_type varchar(64),
                    biz_id varchar(128),
                    biz_version varchar(32),
                    reason varchar(1024),
                    result varchar(16) NOT NULL,
                    tenant_id bigint,
                    trace_id varchar(64),
                    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bfc_app_key "
                + "ON bpm_first_chain_application (tenant_id, app_key) WHERE deleted = FALSE");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bfc_binding_pinst "
                + "ON bpm_first_chain_process_binding (process_instance_id)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bfc_binding_active "
                + "ON bpm_first_chain_process_binding (tenant_id, domain_type, domain_id) WHERE status = 'BOUND'");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bfc_audit_idem "
                + "ON audit_event (event_type, biz_type, biz_id, action, result, coalesce(reason,''))");
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterAll
    void tearDownContext() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(10)
    void submitStartsRealProcessWithActiveBinding() {
        Long id = createApplication("BPM003-S1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = activeProcessInstanceId(id);
        assertNotNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult(), "提交后真实流程实例应运行中");
        TaskSlice task = activeTask(processInstanceId);
        assertEquals(String.valueOf(APPROVER_ID), task.assignee, "单节点审批任务应指派给变量审批人");
    }

    @Test
    @Order(20)
    void transferKeepsDomainStateConsistent() {
        Long id = createApplication("BPM003-T1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = activeProcessInstanceId(id);
        // 转派（流程侧操作）：受理人更新，领域状态与版本不变（验收①「转派与业务状态一致」）
        ((FlowableFirstChainProcessAdapter) processPort).transferApprovalTask(processInstanceId, "approver-2");
        assertEquals("approver-2", activeTask(processInstanceId).assignee);
        Map<String, Object> row = queryApplication(id);
        assertEquals(FranchiseeApplicationStatus.SUBMITTED.name(), row.get("status"));
        assertEquals(1L, ((Number) row.get("version")).longValue());
        // 转派后的审批照常落领域
        ((FlowableFirstChainProcessAdapter) processPort).completeApprovalTask(processInstanceId, true, null);
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-2");
        assertEquals(FranchiseeApplicationStatus.APPROVED.name(), queryApplication(id).get("status"));
    }

    @Test
    @Order(30)
    void approveRejectWithdrawConsistentOnRealEngine() {
        // 通过
        Long approved = createApplication("BPM003-A1");
        applicationService.submitApplication(approved, 0L, APPROVER_ID, "creator-1");
        String approvePinst = activeProcessInstanceId(approved);
        ((FlowableFirstChainProcessAdapter) processPort).completeApprovalTask(approvePinst, true, null);
        applicationService.onApprovalCompleted(approvePinst, true, null, "approver-1");
        assertEquals(FranchiseeApplicationStatus.APPROVED.name(), queryApplication(approved).get("status"));
        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(approvePinst).singleResult(),
                "通过后真实流程实例应完结");
        // 拒绝（必填意见 → 原因落库）
        Long rejected = createApplication("BPM003-R1");
        applicationService.submitApplication(rejected, 0L, APPROVER_ID, "creator-1");
        String rejectPinst = activeProcessInstanceId(rejected);
        ((FlowableFirstChainProcessAdapter) processPort).completeApprovalTask(rejectPinst, false, "资质不全");
        applicationService.onApprovalCompleted(rejectPinst, false, "资质不全", "approver-1");
        Map<String, Object> rejectedRow = queryApplication(rejected);
        assertEquals(FranchiseeApplicationStatus.REJECTED.name(), rejectedRow.get("status"));
        assertEquals("资质不全", rejectedRow.get("reject_reason"));
        // 撤回（领域不变 + 真实实例取消 + 重发）
        Long withdrawn = createApplication("BPM003-W1");
        applicationService.submitApplication(withdrawn, 0L, APPROVER_ID, "creator-1");
        String withdrawPinst = activeProcessInstanceId(withdrawn);
        applicationService.withdrawApproval(withdrawn, 1L, "creator-1");
        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(withdrawPinst).singleResult(),
                "撤回后真实流程实例应取消");
        assertEquals(FranchiseeApplicationStatus.SUBMITTED.name(), queryApplication(withdrawn).get("status"));
        applicationService.restartApproval(withdrawn, 1L, APPROVER_ID, "creator-1");
        assertNotEquals(withdrawPinst, activeProcessInstanceId(withdrawn), "重发应产生新流程实例");
    }

    @Test
    @Order(40)
    void duplicateCallbackAbsorbedOnRealEngine() {
        Long id = createApplication("BPM003-D1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = activeProcessInstanceId(id);
        ((FlowableFirstChainProcessAdapter) processPort).completeApprovalTask(processInstanceId, true, null);
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");
        long versionAfterFirst = version(id);
        // 同一结果重复回调（at-least-once 重投）：幂等吸收
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");
        assertEquals(versionAfterFirst, version(id));
        assertEquals(FranchiseeApplicationStatus.APPROVED.name(), queryApplication(id).get("status"));
    }

    @Test
    @Order(50)
    void lateDifferentOutcomeDiscardedOnRealEngine() {
        Long id = createApplication("BPM003-L1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = activeProcessInstanceId(id);
        ((FlowableFirstChainProcessAdapter) processPort).completeApprovalTask(processInstanceId, true, null);
        applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");
        // 重放窗口：绑定被重放回 BOUND，携带不同结果晚到
        jdbcTemplate.update("UPDATE bpm_first_chain_process_binding SET status = 'BOUND', outcome = NULL "
                + "WHERE process_instance_id = ?", processInstanceId);
        applicationService.onApprovalCompleted(processInstanceId, false, "晚到拒绝", "approver-1");
        assertEquals(FranchiseeApplicationStatus.APPROVED.name(), queryApplication(id).get("status"),
                "旧流程晚到结果不得覆盖领域新版本");
        assertEquals(1, auditCount(id, FirstChainProcessBindingService.EVENT_RESULT_DISCARDED,
                "DISCARD_LATE_RESULT"), "弃单应留痕可回查");
    }

    @Test
    @Order(60)
    void concurrentApproveAndWithdraw_exactlyOneWins() throws Exception {
        Long id = createApplication("BPM003-C1");
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        String processInstanceId = activeProcessInstanceId(id);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        Thread approver = new Thread(() -> {
            await(start);
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                ((FlowableFirstChainProcessAdapter) processPort)
                        .completeApprovalTask(processInstanceId, true, null);
                applicationService.onApprovalCompleted(processInstanceId, true, null, "approver-1");
                successes.incrementAndGet();
            } catch (Exception e) {
                conflicts.incrementAndGet();
            } finally {
                TenantContextHolder.clear();
            }
        });
        Thread withdrawer = new Thread(() -> {
            await(start);
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                applicationService.withdrawApproval(id, 1L, "creator-1");
                successes.incrementAndGet();
            } catch (Exception e) {
                conflicts.incrementAndGet();
            } finally {
                TenantContextHolder.clear();
            }
        });
        approver.start();
        withdrawer.start();
        start.countDown();
        approver.join(30_000);
        withdrawer.join(30_000);
        // 恰好一方生效：领域终态唯一（APPROVED）或保持 SUBMITTED（撤回胜出），且冲突计数恰为一
        assertEquals(1, successes.get(), "并发审批/撤回应恰有一方生效");
        assertEquals(1, conflicts.get(), "败方应显式冲突（不静默）");
        String status = (String) queryApplication(id).get("status");
        assertTrue(FranchiseeApplicationStatus.APPROVED.name().equals(status)
                        || FranchiseeApplicationStatus.SUBMITTED.name().equals(status),
                "领域状态应与胜出方一致，实际=" + status);
    }

    // ========== 测试辅助 ==========

    private record TaskSlice(String id, String assignee) {
    }

    private Long createApplication(String appKey) {
        return applicationService.createApplication(CreateApplicationCmd.builder()
                .appKey(appKey).applicantName("申请方-" + appKey).contactName("联系人")
                .contactPhone("13800000000").attachmentFileIds("[101]").actorId("creator-1").build());
    }

    private String activeProcessInstanceId(Long applicationId) {
        Map<String, Object> binding = bindingService.findActiveByDomain(TENANT_ID,
                FirstChainObjectType.APPLICATION, applicationId);
        assertNotNull(binding, "应有活跃流程绑定");
        return (String) binding.get("process_instance_id");
    }

    private TaskSlice activeTask(String processInstanceId) {
        var task = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult();
        assertNotNull(task, "应有活跃审批任务");
        return new TaskSlice(task.getId(), task.getAssignee());
    }

    private Map<String, Object> queryApplication(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_application WHERE tenant_id = ? AND id = ?", TENANT_ID, id);
    }

    private long version(Long id) {
        return ((Number) queryApplication(id).get("version")).longValue();
    }

    private int auditCount(Long applicationId, String eventType, String action) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = ? AND biz_type = ? AND action = ?",
                Integer.class, eventType, FirstChainObjectType.APPLICATION.getKey(), action);
        return count == null ? 0 : count;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("并发起点等待被中断");
        }
    }

}
