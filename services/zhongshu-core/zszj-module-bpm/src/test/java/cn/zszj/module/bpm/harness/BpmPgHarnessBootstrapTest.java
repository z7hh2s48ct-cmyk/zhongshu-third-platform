package cn.zszj.module.bpm.harness;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-BPM-001 引导阶段（owner 账号，引擎自管建表，异步执行器挂起）：
 * 在空 PG 上建引擎表、部署中性夹具、发起运行实例并留下异步积压，
 * 供 runtime 阶段以低权限 app 账号重启后接续验证。
 * 由 scripts/db/run-bpm001-verify.mjs 注入环境变量并作为第一阶段调用。
 */
@SpringBootTest(classes = BpmPgHarnessConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BpmPgHarnessBootstrapTest {

    private static final String RES_APPROVAL = "cn/zszj/module/bpm/harness/neutral-approval.bpmn20.xml";
    private static final String RES_APPROVAL_V2 = "cn/zszj/module/bpm/harness/neutral-approval-v2.bpmn20.xml";
    private static final String RES_ASYNC = "cn/zszj/module/bpm/harness/neutral-async-echo.bpmn20.xml";

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ManagementService managementService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void requireBootstrapPhase() {
        // 引导阶段语义：允许建表（owner）+ 执行器挂起（留积压）；错阶段即快速失败
        assertTrue(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
                "[bpm-pg-harness] bootstrap 阶段要求 ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=true");
        assertTrue(!Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
                "[bpm-pg-harness] bootstrap 阶段要求 ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false");
        BpmPgHarness.EngineEventRecorder.reset();
    }

    @Test
    @Order(10)
    void engineCreatesSchemaOnEmptyPostgres() {
        // PG 会把未加引号的标识符折叠为小写：Flowable 在 PG 上建的引擎表为小写 act_*
        Integer actTables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND lower(table_name) LIKE 'act\\_%'",
                Integer.class);
        assertTrue(actTables != null && actTables >= 35,
                "[bpm-pg-harness] Flowable 引擎表未在 PG 建齐，实际 ACT_ 表数量=" + actTables);
        String schemaVersion = jdbcTemplate.queryForObject(
                "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
        assertNotNull(schemaVersion, "引擎 schema.version 缺失");
        // 以 owner 身份把版本写入探针表，供 runtime 阶段校验「重启不改结构」
        jdbcTemplate.update("INSERT INTO bpm_harness_probe(note) VALUES (?)",
                "bootstrap:schema.version=" + schemaVersion);
        System.out.println("[bpm-pg-harness] bootstrap schema.version=" + schemaVersion
                + " actTables=" + actTables);
    }

    @Test
    @Order(20)
    void deployNeutralFixturesWithDuplicateFilteringIdempotency() {
        deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
        deploy(RES_ASYNC, BpmPgHarness.TENANT_1);
        deploy(RES_APPROVAL, BpmPgHarness.TENANT_2);
        long deployments = repositoryService.createDeploymentQuery().count();
        // 同名同资源重复部署（重复过滤）不产生新部署——重复部署幂等
        deploy(RES_APPROVAL, BpmPgHarness.TENANT_1);
        assertEquals(deployments, repositoryService.createDeploymentQuery().count(),
                "重复部署不应新增 deployment");
        assertEquals(1, repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
                .processDefinitionTenantId(BpmPgHarness.TENANT_1).count());
    }

    @Test
    @Order(30)
    void startInstancesForRuntimePhase() {
        // A1~A4 技术租户1（v1）；A5 技术租户2；审批实例共 6 个在 runtime 阶段逐项处置
        startApproval("bpm001-A1");
        startApproval("bpm001-A2");
        startApproval("bpm001-A3");
        startApproval("bpm001-A4");
        startApproval("bpm001-A5", BpmPgHarness.TENANT_2);
        // E1 异步实例：执行器挂起 → 任务留积压
        runtimeService.startProcessInstanceByKeyAndTenantId(
                BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E1", null, BpmPgHarness.TENANT_1);
        assertEquals(6, runtimeService.createProcessInstanceQuery().count(),
                "应发起 6 个运行实例（5 审批 + 1 异步）");
        assertEquals(1, managementService.createJobQuery().count(),
                "异步执行器挂起时应留下 1 个积压任务");
        assertEquals(0, countProbeRowsForInstance(instanceIdByBusinessKey("bpm001-E1")),
                "执行器挂起时探针表不应有异步回声记录");
    }

    @Test
    @Order(40)
    void engineTenantIdIsLabelNotAcl() {
        // 引擎 tenantId 是技术标签：按租户过滤可见各自集合，但不过滤的查询跨租户可见
        // （业务组织隔离归 B08/D-07，本断言钉住引擎真实语义防止误依赖）
        assertEquals(5, runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(BpmPgHarness.TENANT_1).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(BpmPgHarness.TENANT_2).count());
        assertEquals(6, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    @Order(50)
    void changedContentDeploysNewVersionAndOldInstancesStayPinned() {
        deploy(RES_APPROVAL_V2, BpmPgHarness.TENANT_1);
        // 新发起的实例落在最新版本 v2
        ProcessInstance onV2 = startApproval("bpm001-A6");
        assertEquals(2, repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
                .processDefinitionTenantId(BpmPgHarness.TENANT_1)
                .orderByProcessDefinitionVersion().desc().list().get(0).getVersion());
        // 已运行实例钉在原版本 v1
        ProcessInstance onV1 = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey("bpm001-A1").singleResult();
        assertNotEquals(onV2.getProcessDefinitionVersion(), onV1.getProcessDefinitionVersion());
        assertEquals(1, onV1.getProcessDefinitionVersion());
        assertEquals(2, onV2.getProcessDefinitionVersion());
    }

    private ProcessInstance startApproval(String businessKey) {
        return startApproval(businessKey, BpmPgHarness.TENANT_1);
    }

    private ProcessInstance startApproval(String businessKey, String tenantId) {
        return runtimeService.startProcessInstanceByKeyAndTenantId(
                BpmPgHarness.PROCESS_APPROVAL, businessKey, null, tenantId);
    }

    private void deploy(String resource, String tenantId) {
        repositoryService.createDeployment()
                .addClasspathResource(resource)
                .name("bpmPgHarness@" + resource.substring(resource.lastIndexOf('/') + 1) + "@" + tenantId)
                .tenantId(tenantId)
                .enableDuplicateFiltering()
                .deploy();
    }

    private String instanceIdByBusinessKey(String businessKey) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey).singleResult();
        return instance == null ? null : instance.getId();
    }

    private int countProbeRowsForInstance(String instanceId) {
        if (instanceId == null) {
            return -1;
        }
        List<Integer> counts = jdbcTemplate.queryForList(
                "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
        return counts.isEmpty() ? -1 : counts.get(0);
    }
}
