package cn.zszj.module.firstchain.pg;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.bpm.firstchain.FirstChainProcessPort;
import cn.zszj.module.bpm.firstchain.FirstChainStateTransitionExecutor;
import cn.zszj.module.bpm.firstchain.FranchiseeApplicationStatus;
import cn.zszj.module.firstchain.service.FirstchainLeadMetricsService;
import cn.zszj.module.firstchain.service.FirstchainLeadService;
import cn.zszj.module.firstchain.service.opening.FirstchainDefaultRoleRegistry;
import cn.zszj.module.firstchain.service.opening.FirstchainOpeningService;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.notify.dispatch.NotifyDispatcher;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.permission.RoleService;
import cn.zszj.module.system.service.user.AdminUserService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APP_KEY_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_LEAD_TERMINAL;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_DETAIL_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_KEY_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ZS-FC-001/002 真实 PostgreSQL 运行期验证（由 scripts/db/run-firstchain-verify.mjs 注入
 * {@code ZSZJ_FIRSTCHAIN_HARNESS_*} 环境变量调用；缺环境快速失败，不静默跳过）。
 *
 * <p>循 {@code BpmFirstChainPgRuntimeTest} 套件形态，覆盖 FC 批次放行轮登记的线索域完整真实 PG
 * 运行期套件 + 申请域/开通域 PG 运行期语义：
 * <ol>
 *   <li>申请域链路（创建/提交〔StubProcessPort〕/撤回/重发 + app_key uk 撞号显式分类）；</li>
 *   <li>开通同事务幂等（真实 PG：组织/角色落真实 system 表、重复处理吸收不重建、初始密码一次性下发）；</li>
 *   <li>开通失败整体回滚（审批写回/组织桩行/审计随真实 PG 事务一并回滚）；</li>
 *   <li>线索五态全链（分配目标归属校验/领取/跟进/转商机三前置与双向引用/审计落库）；</li>
 *   <li>下发幂等（同键同内容吸收——<b>COALESCE 方言路径直证</b>，R1 IFNULL 缺陷回归锚点；同键异内容显式冲突）；</li>
 *   <li>并发领取恰一方生效（真实双线程条件 UPDATE）；</li>
 *   <li>无效关闭原因枚举守卫与终态锁定；</li>
 *   <li>跨租户隔离反向（他租户不可见不可操作 + SQL 层直证）。</li>
 * </ol>
 *
 * @author ZS-FC-002
 */
@SpringBootTest(classes = {FirstchainPgHarnessConfiguration.class, FirstChainApplicationService.class,
        FirstChainProcessBindingService.class, FirstChainStateTransitionExecutor.class, JdbcAuditPort.class,
        FirstchainOpeningService.class, FirstchainDefaultRoleRegistry.class, FirstchainNotifyWiringService.class,
        FirstchainLeadService.class, FirstchainLeadMetricsService.class,
        FirstchainPgRuntimeTest.StubProcessPort.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirstchainPgRuntimeTest {

    private static final Long TENANT_ID = 1L;

    private static final Long APPROVER_ID = 200L;

    private static final Long ORG_ID = 100L;

    private static final Long EMPLOYEE_ID = 300L;

    private static final Long LEADER_USER_ID = 900L;

    private static final Long OPENED_ORG_ID = 700L;

    private static final Long ROLE_LEADER_ID = 501L;

    private static final Long ROLE_MEMBER_ID = 502L;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FirstChainApplicationService applicationService;
    @Autowired
    private FirstChainProcessBindingService bindingService;
    @Autowired
    private FirstchainOpeningService openingService;
    @Autowired
    private FirstchainLeadService leadService;
    @Autowired
    private FirstChainProcessPort processPort;

    @MockitoBean
    private OrganizationService organizationService;
    @MockitoBean
    private MembershipService membershipService;
    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private NotifyTodoService notifyTodoService;
    @MockitoBean
    private NotifyDispatcher notifyDispatcher;
    @MockitoBean
    private ReliableEventPort reliableEventPort;

    @BeforeAll
    void setUpSchemaAndContext() {
        // 结构自备（IF NOT EXISTS 幂等）：system_* 全表由编排器应用全部 Flyway 迁移就位；
        // 首链三表照抄迁移 V20260928.001/V20260929.001，audit_event 照抄 bpm003 套件同款
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
                CREATE TABLE IF NOT EXISTS bpm_first_chain_lead (
                    id                bigserial PRIMARY KEY,
                    lead_key          varchar(64)  NOT NULL,
                    customer_name     varchar(128),
                    customer_phone    varchar(32),
                    customer_wechat   varchar(64),
                    customer_address  varchar(256),
                    source            varchar(64),
                    org_id            bigint       NOT NULL,
                    assignee_user_id  bigint,
                    status            varchar(32)  NOT NULL,
                    version           bigint       NOT NULL DEFAULT 0,
                    tenant_id         bigint       NOT NULL,
                    creator           varchar(64)  DEFAULT '',
                    create_time       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updater           varchar(64)  DEFAULT '',
                    update_time       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted           boolean      NOT NULL DEFAULT FALSE
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS bpm_first_chain_followup (
                    id               bigserial PRIMARY KEY,
                    lead_id          bigint       NOT NULL,
                    content          varchar(2048) NOT NULL,
                    next_step        varchar(512),
                    followup_user_id bigint       NOT NULL,
                    followup_time    timestamp    NOT NULL,
                    tenant_id        bigint       NOT NULL,
                    creator          varchar(64)  DEFAULT '',
                    create_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updater          varchar(64)  DEFAULT '',
                    update_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted          boolean      NOT NULL DEFAULT FALSE
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS bpm_first_chain_opportunity (
                    id            bigserial PRIMARY KEY,
                    opp_key       varchar(64)  NOT NULL,
                    lead_id       bigint       NOT NULL,
                    customer_name varchar(128),
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
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_app_key "
                + "ON bpm_first_chain_application (tenant_id, app_key) WHERE deleted = FALSE");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_binding_pinst "
                + "ON bpm_first_chain_process_binding (process_instance_id)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_binding_active "
                + "ON bpm_first_chain_process_binding (tenant_id, domain_type, domain_id) WHERE status = 'BOUND'");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_audit_idem "
                + "ON audit_event (event_type, biz_type, biz_id, action, result, coalesce(reason,''))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_lead_key "
                + "ON bpm_first_chain_lead (tenant_id, lead_key) WHERE deleted = FALSE");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_opp_key "
                + "ON bpm_first_chain_opportunity (tenant_id, opp_key) WHERE deleted = FALSE");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_fcpg_opp_lead "
                + "ON bpm_first_chain_opportunity (lead_id) WHERE deleted = FALSE");
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterAll
    void tearDownContext() {
        TenantContextHolder.clear();
    }

    // ========== ① 申请域链路（真实 PG + Stub 流程端口） ==========

    @Test
    @Order(10)
    void applicationChainOnRealPg() {
        Long id = applicationService.createApplication(CreateApplicationCmd.builder()
                .appKey("FCPG-A1").applicantName("申请方-FCPG-A1").contactName("联系人")
                .contactPhone("13800000000").attachmentFileIds("[101]").actorId("creator-1").build());
        assertEquals(0L, versionOfApplication(id));
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        assertEquals(FranchiseeApplicationStatus.SUBMITTED.name(), applicationRow(id).get("status"));
        assertEquals(1L, versionOfApplication(id));
        assertNotNull(activeProcessInstanceId(id), "提交后应有活跃流程绑定（Stub 端口签发实例）");
        // app_key uk 撞号显式分类（真实 PG 部分唯一索引）
        assertServiceException(() -> applicationService.createApplication(CreateApplicationCmd.builder()
                        .appKey("FCPG-A1").applicantName("撞号方").actorId("creator-1").build()),
                FIRST_CHAIN_APP_KEY_EXISTS, "FCPG-A1");
        // 撤回（解绑不推版本）+ 重发（领域不变、换绑新实例）
        applicationService.withdrawApproval(id, 1L, "creator-1");
        String firstPinst = activeProcessInstanceIdAfterWithdraw(id);
        applicationService.restartApproval(id, 1L, APPROVER_ID, "creator-1");
        assertNotEquals(firstPinst, activeProcessInstanceId(id), "重发应产生新流程实例绑定");
    }

    // ========== ②③ 开通同事务幂等 / 失败回滚（真实 PG + system 表直写桩） ==========

    @Test
    @Order(20)
    void openingIdempotentOnRealPg() {
        Long id = createSubmittedApplication("FCPG-O1");
        stubOpeningSuccess();
        stubPlatformOperator();

        FirstchainOpeningService.OpeningResult first = openingService.approveAndOpen(
                approveCmd("FCPG-O1", "资质齐备"));
        // 开通主体落真实 PG：组织行 + 模板角色行；初始密码一次性下发
        assertEquals(OPENED_ORG_ID, first.organizationId());
        assertNotNull(first.initialPassword());
        assertEquals(16, first.initialPassword().length());
        Integer orgRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_organization WHERE tenant_id = ? AND code = ? AND deleted = 0",
                Integer.class, TENANT_ID, "FCPG-O1");
        assertEquals(1, orgRows, "开通应恰好产生一个组织行（真实 system_organization）");
        Integer roleRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_role WHERE tenant_id = ? AND code IN "
                        + "('firstchain:franchisee:leader', 'firstchain:franchisee:member') AND deleted = 0",
                Integer.class, TENANT_ID);
        assertEquals(2, roleRows, "默认模板角色应落真实 system_role");
        assertEquals(1, auditCount("OBJECT_CREATED", "OPEN", "FCPG-O1", "SUCCESS"), "开通审计随事务落真实 PG");
        // 重复处理（M4-A）：返回既有组织、不重发密码、不重建任何主体
        FirstchainOpeningService.OpeningResult second = openingService.approveAndOpen(approveCmd("FCPG-O1", null));
        assertEquals(first.organizationId(), second.organizationId());
        assertNull(second.initialPassword());
        assertEquals(1, orgRows("FCPG-O1"), "重复处理不得产生第二个组织");
        org.mockito.Mockito.verify(adminUserService, org.mockito.Mockito.times(1)).createUser(any());
        assertEquals(1, auditCount("OBJECT_CREATED", "OPEN", "FCPG-O1", "SUCCESS"), "重复处理不重复留痕");
    }

    @Test
    @Order(30)
    void openingFailureRollsBackWholeChainOnRealPg() {
        Long id = createSubmittedApplication("FCPG-F1");
        stubOpeningSuccess();
        stubPlatformOperator();
        // 任职创建失败（开通中段）→ 同事务整体回滚（真实 PG：审批写回/组织桩行/审计一并回滚）
        org.mockito.Mockito.doThrow(new RuntimeException("任职创建失败")).when(membershipService)
                .createMembership(any(), any());

        try {
            openingService.approveAndOpen(approveCmd("FCPG-F1", null));
            fail("开通失败应向上抛出");
        } catch (RuntimeException expected) {
            // fail-closed
        }
        assertEquals(FranchiseeApplicationStatus.SUBMITTED.name(), applicationRow(id).get("status"),
                "审批写回应随事务回滚");
        assertEquals(0, orgRows("FCPG-F1"), "组织桩行应随真实 PG 事务回滚清除");
        assertEquals(0, auditCount("OBJECT_CREATED", "OPEN", "FCPG-F1", "SUCCESS"), "开通审计不应残留");
    }

    // ========== ④ 线索五态全链（真实 PG） ==========

    @Test
    @Order(40)
    void leadFiveStateChainOnRealPg() {
        Long leadId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-L1", "李四", "13800000000", "lisi_wx", "上海市", "展会获客", ORG_ID, "1"));
        assertEquals("DISTRIBUTED", leadRow(leadId).get("status"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "1");
        assertEquals("ASSIGNED", leadRow(leadId).get("status"));
        assertEquals(EMPLOYEE_ID, ((Number) leadRow(leadId).get("assignee_user_id")).longValue());
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "1");
        assertEquals("FOLLOWING", leadRow(leadId).get("status"));
        Long followupId = leadService.followup(leadId, new FirstchainLeadService.FollowupCmd(
                "电话沟通", "上门量房", EMPLOYEE_ID, java.time.LocalDateTime.now(), "1"));
        assertNotNull(followupId, "跟进记录应落真实 PG");
        // 转商机三前置满足 → CONVERTED + 双向引用（商机 lead_id 回链）
        Long opportunityId = leadService.convertToOpportunity(leadId, "FCPG-OPP-1", 2L, "1");
        assertEquals("CONVERTED", leadRow(leadId).get("status"));
        Integer oppRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_opportunity WHERE tenant_id = ? AND id = ? AND lead_id = ?",
                Integer.class, TENANT_ID, opportunityId, leadId);
        assertEquals(1, oppRows, "商机应落库并双向引用线索");
        // 转商机后终态锁定 + 跟进拒绝
        assertServiceException(() -> leadService.followup(leadId, new FirstchainLeadService.FollowupCmd(
                "迟到跟进", null, EMPLOYEE_ID, java.time.LocalDateTime.now(), "1")),
                FOLLOWUP_LEAD_TERMINAL, "CONVERTED");
        // 全链审计落真实 PG（DISTRIBUTE 以业务键留痕；CONVERT 以行 ID 留痕）
        String leadKey = (String) leadRow(leadId).get("lead_key");
        assertTrue(auditCount("OBJECT_CREATED", "DISTRIBUTE", leadKey, "SUCCESS") >= 1);
        assertTrue(auditCount("OBJECT_UPDATED", "CONVERT", String.valueOf(leadId), "SUCCESS") >= 1);
    }

    // ========== ⑤ 下发幂等（COALESCE 方言路径直证——R1 IFNULL 缺陷回归锚点） ==========

    @Test
    @Order(50)
    void distributeIdempotencyOnRealPg() {
        Long firstId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-DUP", "王五", "13900000000", null, null, "门店自然流", ORG_ID, "1"));
        // 同键同内容重复下发：幂等吸收（幂等比对走 COALESCE——真实 PG 无 IFNULL，本用例即 R1 方言回归锚点）
        Long secondId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-DUP", "王五", "13900000000", null, null, "门店自然流", ORG_ID, "1"));
        assertEquals(firstId, secondId, "重复下发应吸收返回既有线索");
        // 同键异内容（手机号不同）：显式冲突不静默覆盖
        assertServiceException(() -> leadService.distribute(new FirstchainLeadService.DistributeCmd(
                        "FCPG-DUP", "王五", "13711112222", null, null, "门店自然流", ORG_ID, "1")),
                LEAD_KEY_CONFLICT, "FCPG-DUP");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_lead WHERE tenant_id = ? AND lead_key = ? AND deleted = FALSE",
                Integer.class, TENANT_ID, "FCPG-DUP");
        assertEquals(1, rows, "重复下发只留一行");
    }

    // ========== ⑥ 并发领取恰一方生效（真实双线程条件 UPDATE） ==========

    @Test
    @Order(60)
    void concurrentClaimExactlyOneWinsOnRealPg() throws Exception {
        Long leadId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-C1", "赵六", "13600000000", null, null, "展会获客", ORG_ID, "1"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "1");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        Runnable claimTask = () -> {
            await(start);
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                leadService.claim(leadId, EMPLOYEE_ID, 1L, "1");
                successes.incrementAndGet();
            } catch (Exception e) {
                conflicts.incrementAndGet();
            } finally {
                TenantContextHolder.clear();
            }
        };
        Thread first = new Thread(claimTask);
        Thread second = new Thread(claimTask);
        first.start();
        second.start();
        start.countDown();
        first.join(30_000);
        second.join(30_000);
        assertEquals(1, successes.get(), "并发领取应恰有一方生效（PILOT-REQ-006 领取并发唯一）");
        assertEquals(1, conflicts.get(), "败方应显式冲突（版本/状态二分类，不静默）");
        assertEquals("FOLLOWING", leadRow(leadId).get("status"));
        assertEquals(2L, ((Number) leadRow(leadId).get("version")).longValue(), "版本应恰好推进一次");
    }

    // ========== ⑦ 无效关闭原因守卫与终态锁定（真实 PG） ==========

    @Test
    @Order(70)
    void invalidateReasonGuardsAndTerminalLockOnRealPg() {
        Long leadId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-I1", "钱七", "13500000000", null, null, "展会获客", ORG_ID, "1"));
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "1");
        leadService.claim(leadId, EMPLOYEE_ID, 1L, "1");
        // 原因枚举必填 / OTHER 说明必填
        assertServiceException(() -> leadService.invalidate(leadId, null, null, 2L, "1"),
                LEAD_INVALIDATE_REASON_REQUIRED);
        assertServiceException(() -> leadService.invalidate(leadId, "OTHER", null, 2L, "1"),
                LEAD_INVALIDATE_REASON_DETAIL_REQUIRED);
        // 合法无效关闭 → INVALID 终态
        leadService.invalidate(leadId, "UNREACHABLE", null, 2L, "1");
        assertEquals("INVALID", leadRow(leadId).get("status"));
        // 终态锁定：再跟进/再分配均拒绝（跟进=模块终态分类；领取=执行器单一执行面的 bpm 段状态冲突）
        assertServiceException(() -> leadService.followup(leadId, new FirstchainLeadService.FollowupCmd(
                "终态跟进", null, EMPLOYEE_ID, java.time.LocalDateTime.now(), "1")),
                FOLLOWUP_LEAD_TERMINAL, "INVALID");
        assertServiceException(() -> leadService.claim(leadId, EMPLOYEE_ID, 3L, "1"),
                cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_CONFLICT, "INVALID");
    }

    // ========== ⑧ 跨租户隔离反向（真实 PG SQL 层直证） ==========

    @Test
    @Order(80)
    void crossTenantLeadIsolationOnRealPg() {
        Long leadId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                "FCPG-X1", "孙八", "13400000000", null, null, "展会获客", ORG_ID, "1"));
        TenantContextHolder.setTenantId(2L);
        try {
            // 他租户上下文：领取/跟进均按 NOT_EXISTS 分类（不泄露存在性）
            assertServiceException(() -> leadService.claim(leadId, EMPLOYEE_ID, 0L, "intruder"), LEAD_NOT_EXISTS);
            assertServiceException(() -> leadService.getLead(leadId), LEAD_NOT_EXISTS);
            Integer rows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM bpm_first_chain_lead WHERE tenant_id = 2 AND id = ?", Integer.class, leadId);
            assertEquals(0, rows, "他租户上下文不得见该线索行");
        } finally {
            TenantContextHolder.setTenantId(TENANT_ID);
        }
        // 归属租户操作不受影响
        leadService.assign(leadId, EMPLOYEE_ID, 0L, "1");
        assertEquals("ASSIGNED", leadRow(leadId).get("status"));
    }

    // ========== 测试辅助 ==========

    /** 流程端口桩（循 FirstchainOpeningServiceTest 同款；真实引擎归 bpm003 套件直证） */
    public static class StubProcessPort implements FirstChainProcessPort {

        private final AtomicLong sequence = new AtomicLong();

        @Override
        public String startApprovalProcess(Long tenantId, String appKey, Long approverUserId) {
            return "PINST-" + sequence.incrementAndGet();
        }

        @Override
        public boolean withdrawProcess(String processInstanceId) {
            return true;
        }

    }

    private Long createSubmittedApplication(String appKey) {
        Long id = applicationService.createApplication(CreateApplicationCmd.builder()
                .appKey(appKey).applicantName("申请方-" + appKey).contactName("联系人")
                .contactPhone("13800000000").attachmentFileIds("[101]").actorId("creator-1").build());
        applicationService.submitApplication(id, 0L, APPROVER_ID, "creator-1");
        return id;
    }

    /** 开通成功桩：组织/角色落真实 system 表（真实 PG 语义：uk 兜底、幂等兜底查询可命中）；账号/任职捕获。 */
    private void stubOpeningSuccess() {
        AtomicLong roleIdSeq = new AtomicLong(ROLE_LEADER_ID - 1);
        when(roleService.createRole(any(), any())).thenAnswer(invocation -> {
            cn.zszj.module.system.controller.admin.permission.vo.role.RoleSaveReqVO saveReqVO =
                    invocation.getArgument(0);
            long roleId = roleIdSeq.incrementAndGet();
            // 真实 system_role 的 type/data_scope/status 均 NOT NULL（基线形态）：type=2 自定义（与
            // RoleService createRole(type=null→CUSTOM) 生产语义同值域），其余走列默认
            jdbcTemplate.update(
                    "INSERT INTO system_role (id, name, code, sort, status, type, remark, tenant_id, deleted) "
                            + "VALUES (?, ?, ?, ?, 0, 2, ?, ?, 0)",
                    roleId, saveReqVO.getName(), saveReqVO.getCode(), saveReqVO.getSort(),
                    saveReqVO.getRemark(), TENANT_ID);
            return roleId;
        });
        when(organizationService.createOrganization(any())).thenAnswer(invocation -> {
            cn.zszj.module.system.dal.dataobject.organization.OrganizationDO organization = invocation.getArgument(0);
            jdbcTemplate.update(
                    "INSERT INTO system_organization (id, name, code, type, parent_id, leader_user_id, status, "
                            + "tenant_id, deleted) VALUES (?, ?, ?, ?, ?, ?, 0, ?, 0)",
                    OPENED_ORG_ID, organization.getName(), organization.getCode(), organization.getType(),
                    organization.getParentId(), organization.getLeaderUserId(), TENANT_ID);
            return OPENED_ORG_ID;
        });
        when(adminUserService.createUser(any())).thenReturn(LEADER_USER_ID);
    }

    /** 审批操作人资格桩：APPROVER 的默认任职挂 PLATFORM 组织（ACTIVE，M2 资格二次校验入参）。 */
    private void stubPlatformOperator() {
        cn.zszj.module.system.dal.dataobject.membership.MembershipDO membership =
                new cn.zszj.module.system.dal.dataobject.membership.MembershipDO();
        membership.setUserId(APPROVER_ID);
        membership.setOrganizationId(10L);
        membership.setStatus(cn.zszj.module.system.enums.membership.MembershipStatusEnum.ACTIVE.getStatus());
        when(membershipService.getPrimaryMembership(APPROVER_ID)).thenReturn(membership);
        cn.zszj.module.system.dal.dataobject.organization.OrganizationDO platformOrg =
                new cn.zszj.module.system.dal.dataobject.organization.OrganizationDO();
        platformOrg.setId(10L);
        platformOrg.setType(cn.zszj.module.system.enums.organization.OrganizationTypeEnum.PLATFORM.getType());
        when(organizationService.getOrganization(10L)).thenReturn(platformOrg);
    }

    private static FirstchainOpeningService.ApproveCmd approveCmd(String appKey, String reason) {
        return FirstchainOpeningService.ApproveCmd.builder()
                .appKey(appKey).reason(reason).actorId("approver-1").operatorUserId(APPROVER_ID).build();
    }

    private Map<String, Object> applicationRow(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_application WHERE tenant_id = ? AND id = ?", TENANT_ID, id);
    }

    private long versionOfApplication(Long id) {
        return ((Number) applicationRow(id).get("version")).longValue();
    }

    private String activeProcessInstanceId(Long applicationId) {
        Map<String, Object> binding = bindingService.findActiveByDomain(TENANT_ID,
                FirstChainObjectType.APPLICATION, applicationId);
        assertNotNull(binding, "应有活跃流程绑定");
        return (String) binding.get("process_instance_id");
    }

    private String activeProcessInstanceIdAfterWithdraw(Long applicationId) {
        Map<String, Object> binding = bindingService.findLatestByDomain(TENANT_ID,
                FirstChainObjectType.APPLICATION, applicationId);
        assertNotNull(binding, "撤回后应保留最新绑定（WITHDRAWN）");
        return (String) binding.get("process_instance_id");
    }

    private Map<String, Object> leadRow(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM bpm_first_chain_lead WHERE tenant_id = ? AND id = ?", TENANT_ID, id);
    }

    private int orgRows(String code) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_organization WHERE tenant_id = ? AND code = ? AND deleted = 0",
                Integer.class, TENANT_ID, code);
        return count == null ? 0 : count;
    }

    private int auditCount(String eventType, String action, String bizId, String result) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = ? AND action = ? AND biz_id = ? AND result = ?",
                Integer.class, eventType, action, bizId, result);
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
