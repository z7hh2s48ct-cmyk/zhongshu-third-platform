package cn.zszj.module.bpm.harness;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-BPM-001 运行阶段（低权限 zhongshu_app 账号，禁止改引擎结构，异步执行器开启）：
 * 以新引擎进程重启到 bootstrap 阶段留下的同一 PG 库，验证重启恢复、部署/发起/通过/拒绝/
 * 撤回/转办/分页历史、事务原子性、异步积压恢复与运行期零 DDL。
 * 由 scripts/db/run-bpm001-verify.mjs 作为第二阶段调用。
 */
@SpringBootTest(classes = BpmPgHarnessConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BpmPgHarnessRuntimeTest {

    private static final Duration ASYNC_WAIT = Duration.ofSeconds(60);

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ManagementService managementService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private org.flowable.job.service.impl.asyncexecutor.AsyncExecutor asyncExecutor;

    @BeforeAll
    static void requireRuntimePhase() {
        // 运行阶段语义：低权限账号禁止改结构 + 执行器不自动启动（R70 显式启停，保证重启恢复断言确定性）
        assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_SCHEMA_UPDATE)),
                "[bpm-pg-harness] runtime 阶段要求 ZSZJ_BPM_HARNESS_SCHEMA_UPDATE=false");
        assertFalse(Boolean.parseBoolean(BpmPgHarnessConfiguration.requireEnv(BpmPgHarnessConfiguration.ENV_ASYNC_EXECUTOR)),
                "[bpm-pg-harness] runtime 阶段要求 ZSZJ_BPM_HARNESS_ASYNC_EXECUTOR=false（执行器由 R70 显式启停）");
    }

    @Test
    @Order(10)
    void engineRestartsOnExistingSchemaWithoutStructureChange() {
        // 重启直证：新进程以 schema-update=false 启动，bootstrap 阶段的定义/实例/历史全部可见
        assertEquals(2, repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(BpmPgHarness.PROCESS_APPROVAL)
                .processDefinitionTenantId(BpmPgHarness.TENANT_1).count(), "租户1 审批流程应保留 v1+v2 两个版本");
        assertEquals(7, runtimeService.createProcessInstanceQuery().count(), "重启后 7 个运行实例应全部恢复");
        // schema.version 与 bootstrap 写入探针表的记录一致 → 重启未改引擎结构
        String recorded = jdbcTemplate.queryForObject(
                "SELECT note FROM bpm_harness_probe WHERE note LIKE 'bootstrap:schema.version=%' ORDER BY id LIMIT 1",
                String.class);
        String current = jdbcTemplate.queryForObject(
                "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version'", String.class);
        assertEquals(recorded, "bootstrap:schema.version=" + current,
                "重启后引擎 schema 版本应与 bootstrap 记录一致（运行期零 DDL）");
    }

    @Test
    @Order(20)
    void approveCompletesInstanceOnOriginalDefinitionVersion() {
        ProcessInstance instance = requireRunning("bpm001-A1");
        Task task = requireSingleTask(instance.getId());
        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId()).finished().singleResult();
        assertNotNull(historic, "审批通过后实例应进入历史");
        assertEquals(1, historic.getProcessDefinitionVersion(), "旧实例应在原版本 v1 上完成（版本钉住）");
        assertEquals(BpmPgHarness.OUTCOME_APPROVE, historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId()).variableName(BpmPgHarness.VAR_OUTCOME)
                .singleResult().getValue());
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId()).singleResult());
    }

    @Test
    @Order(30)
    void rejectTakesRejectedBranch() {
        ProcessInstance instance = requireRunning("bpm001-A2");
        Task task = requireSingleTask(instance.getId());
        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_REJECT));
        assertNotNull(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId()).finished().singleResult(), "拒绝后实例应进入历史");
        assertEquals(1, historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instance.getId()).activityId("rejectedEnd").count(), "应走 reject 分支结束");
    }

    @Test
    @Order(40)
    void withdrawCancelsInstanceWithReasonInHistory() {
        ProcessInstance instance = requireRunning("bpm001-A3");
        runtimeService.deleteProcessInstance(instance.getId(), BpmPgHarness.WITHDRAW_REASON);
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId()).finished().singleResult();
        assertNotNull(historic, "撤回后实例应进入历史");
        assertEquals(BpmPgHarness.WITHDRAW_REASON, historic.getDeleteReason(), "撤回原因应落历史可回查");
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId()).singleResult());
    }

    @Test
    @Order(50)
    void transferReassignsTaskAndCompletesNormally() {
        ProcessInstance instance = requireRunning("bpm001-A4");
        Task task = requireSingleTask(instance.getId());
        taskService.claim(task.getId(), "tech-user-a");
        assertEquals("tech-user-a", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
        taskService.setAssignee(task.getId(), "tech-user-b");
        assertEquals("tech-user-b", taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee(),
                "转办后受理人应更新");
        taskService.complete(task.getId(), Map.of(BpmPgHarness.VAR_OUTCOME, BpmPgHarness.OUTCOME_APPROVE));
        assertEquals("tech-user-b", historyService.createHistoricTaskInstanceQuery()
                .taskId(task.getId()).singleResult().getAssignee(), "任务历史应记录转办后的受理人");
    }

    @Test
    @Order(60)
    void springTransactionRollbackLeavesNoEngineTrace() {
        long before = runtimeService.createProcessInstanceQuery().count();
        // 控制组：提交事务内的发起持久化
        transactionTemplate.executeWithoutResult(status ->
                runtimeService.startProcessInstanceByKeyAndTenantId(
                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A7", null, BpmPgHarness.TENANT_1));
        assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count());
        // 实验组：回滚事务内的发起不留下任何引擎痕迹（证明引擎走 Spring 事务管理器）
        try {
            transactionTemplate.executeWithoutResult(status -> {
                runtimeService.startProcessInstanceByKeyAndTenantId(
                        BpmPgHarness.PROCESS_APPROVAL, "bpm001-A8", null, BpmPgHarness.TENANT_1);
                throw new IllegalStateException("harness: 模拟业务事务失败");
            });
        } catch (IllegalStateException expected) {
            // 由回滚断言承接
        }
        assertEquals(before + 1, runtimeService.createProcessInstanceQuery().count(),
                "回滚事务内的发起不应持久化");
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey("bpm001-A8").singleResult());
    }

    @Test
    @Order(70)
    void asyncBacklogRecoversAfterRestartAndExecutorStartStopWorks() {
        // 执行器未自启：bootstrap 留下的 E1 积压跨重启持久保留
        String e1 = requireRunning("bpm001-E1").getId();
        assertEquals(1, managementService.createJobQuery().count(), "执行器未启动时积压任务应保留");
        assertEquals(0, probeCount(e1), "执行器未启动时探针不应有回声记录");
        // 显式启动执行器（启停控制直证）：积压被消化且实例完结
        asyncExecutor.start();
        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e1) > 0),
                "执行器启动后异步积压任务未被执行（探针无记录）");
        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(e1).finished().count() == 1), "E1 实例应执行完毕");
        // 实时路径：执行器在线时新发起的 E2 同样被执行
        ProcessInstance e2 = runtimeService.startProcessInstanceByKeyAndTenantId(
                BpmPgHarness.PROCESS_ASYNC_ECHO, "bpm001-E2", null, BpmPgHarness.TENANT_1);
        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> probeCount(e2.getId()) > 0),
                "执行器在线时异步任务未被执行");
        assertTrue(BpmPgHarness.waitUntil(ASYNC_WAIT, () -> historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(e2.getId()).finished().count() == 1));
        assertEquals(0, managementService.createJobQuery().count(), "不应残留待执行任务");
        assertEquals(0, managementService.createDeadLetterJobQuery().count(), "不应有死信任务");
        // 优雅停机：执行器显式关闭后回到非活动态（启停收尾，JVM 退出时不再挂起）
        asyncExecutor.shutdown();
        assertFalse(asyncExecutor.isActive(), "执行器 shutdown 后应回到非活动态");
    }

    @Test
    @Order(80)
    void historyAndRuntimeQueriesSupportPaging() {
        List<HistoricProcessInstance> page = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().asc()
                .listPage(0, 2);
        assertEquals(2, page.size(), "分页历史查询应返回整页");
        assertTrue(historyService.createHistoricProcessInstanceQuery().finished().count() >= 4,
                "通过/拒绝/撤回/转办完成共 4 个实例应进入历史");
        assertEquals(2, historyService.createHistoricTaskInstanceQuery().finished()
                .listPage(0, 2).size(), "任务历史分页应可用");
        assertTrue(runtimeService.createProcessInstanceQuery()
                .listPage(0, 3).size() <= 3, "运行实例分页应可用");
    }

    @Test
    @Order(90)
    void configuredEventListenersFireOnRealEngine() {
        // 与 BpmFlowableConfiguration 相同的 setEventListeners 扩展点在真实 PG 引擎上生效
        assertTrue(BpmPgHarness.EngineEventRecorder.count("PROCESS_STARTED") >= 2,
                "引擎事件监听器应记录 PROCESS_STARTED");
        assertTrue(BpmPgHarness.EngineEventRecorder.count("TASK_COMPLETED") >= 3,
                "引擎事件监听器应记录 TASK_COMPLETED");
    }

    private ProcessInstance requireRunning(String businessKey) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey).singleResult();
        assertNotNull(instance, "实例 " + businessKey + " 应处于运行态（重启恢复）");
        return instance;
    }

    private Task requireSingleTask(String instanceId) {
        Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
        assertNotNull(task, "审批实例应有且仅有一个待办任务");
        return task;
    }

    private int probeCount(String instanceId) {
        List<Integer> counts = jdbcTemplate.queryForList(
                "SELECT count(*) FROM bpm_harness_probe WHERE note = ?", Integer.class, instanceId);
        return counts.isEmpty() ? 0 : counts.get(0);
    }
}
