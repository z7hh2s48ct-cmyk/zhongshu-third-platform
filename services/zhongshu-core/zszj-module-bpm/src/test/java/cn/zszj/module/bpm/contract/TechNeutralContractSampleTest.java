package cn.zszj.module.bpm.contract;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.contract.TechNeutralContractSample.TechNeutralContractCreatedEvent;
import cn.zszj.module.infra.framework.inbox.JdbcConsumerInboxPort;
import cn.zszj.module.infra.framework.outbox.JdbcReliableEventPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.framework.audit.core.JdbcAuditPort;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.todo.NotifyTodoEventSink;
import cn.zszj.module.system.service.notify.todo.NotifyTodoRegisterCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.notify.todo.NotifyTodoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_TENANT_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TechNeutralContractSample} 单元测试（ZS-BPM-004，H2）——「新业务模块接入合同」中性样例端到端验证。
 *
 * <p>覆盖 docs/05 ZS-BPM-004 验收：中性合同样例从提交到审计 / 事件 / 待办可验证；绕过版本、伪造对象归属、
 * 重复来源消息失败或幂等。用例对齐：
 * <ol>
 *   <li>提交链路：对象（归属 + 初始版本）+ 审计（SUCCESS 随事务）+ 待办（幂等注册）+ 同事务内部事件；</li>
 *   <li>完成：版本乐观锁推进 + 审计 + 待办流转事件预写（Outbox 同事务，MANDATORY）；</li>
 *   <li>派发：事件确认 DISPATCHED + 待办 COMPLETED + Inbox 消费留痕（租户上下文 + 编程式事务包裹投递）；</li>
 *   <li>绕过版本：旧版本更新被拒 + DENIED 拒绝留痕独立事务保留 + 无任何副作用；</li>
 *   <li>乱序旧版本事件：到达晚于高版本 → Inbox 版本水位拒绝（不复活、不回退水位）；</li>
 *   <li>伪造对象归属：无租户上下文写侧入口拒绝（样例 + registerTodo 双层）；</li>
 *   <li>无租户上下文 Outbox 追加拒绝（事务内租户强制）；</li>
 *   <li>跨租户操作：对象按租户过滤不可见不可改（0 行即拒 + 拒绝留痕）；</li>
 *   <li>重复注册：同 (tenant, sourceType, todoKey) 幂等返回既有；</li>
 *   <li>重复事件：同 eventId 重投被 Inbox 幂等吸收（DUPLICATE_COMPLETED），不产生重复副作用。</li>
 * </ol>
 *
 * <p>循 ZS-MSG-002 NotifyTodoServiceTest / ZS-JOB-002 OutboxDispatcherServiceTest 先例：H2 + BaseDbUnitTest +
 * @Import 显式装配（跨模块设施：JdbcAuditPort / JdbcReliableEventPort / NotifyTodoServiceImpl /
 * JdbcConsumerInboxPort / NotifyTodoEventSink）+ 注入 DataSource/PlatformTransactionManager 手动构建
 * JdbcTemplate/TransactionTemplate + JdbcTemplate 直查断言。
 *
 * <p>本测试同时验证「设施已按合同工作」（如无租户拒绝）与「样例演示了正确接线」（无法经样例断言时直证设施，
 * 循 JOB-003 先例）；真实系统接入的合同回填与 UAT 不因本测试通过而豁免（D-07）。
 */
@Import({TechNeutralContractSample.class, TechNeutralContractInnerEventListener.class,
        JdbcAuditPort.class, JdbcReliableEventPort.class, NotifyTodoEventSink.class,
        NotifyTodoServiceImpl.class, JdbcConsumerInboxPort.class})
public class TechNeutralContractSampleTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    private static final Long OTHER_TENANT_ID = 2L;

    private static final Long RECIPIENT_ID = 100L;

    @Resource
    private TechNeutralContractSample sample;

    @Resource
    private TechNeutralContractInnerEventListener innerEventListener;

    @Resource
    private ReliableEventPort reliableEventPort;

    @Resource
    private NotifyTodoService notifyTodoService;

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        innerEventListener.reset();
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ========= 验收① 提交链路：对象 + 审计 + 待办 + 同事务内部事件 =========

    @Test
    void test_提交链路_对象审计待办与内部事件就位() {
        String contractKey = newKey();
        Long recordId = submit(contractKey);
        assertThat(recordId).isNotNull();
        // 对象：归属挂租户（不默认 0）、初始版本 0、技术中性初始态
        Map<String, Object> record = queryRecord(contractKey);
        assertThat(record.get("status")).isEqualTo(TechNeutralContractSample.STATUS_PENDING);
        assertThat(((Number) record.get("version")).longValue()).isZero();
        assertThat(((Number) record.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        // 审计：SUCCESS 随业务事务、携对象版本基线
        Map<String, Object> audit = queryAudit("OBJECT_CREATED", contractKey);
        assertThat(audit.get("result")).isEqualTo("SUCCESS");
        assertThat(audit.get("biz_type")).isEqualTo(TechNeutralContractSample.OBJECT_TYPE);
        assertThat(audit.get("biz_version")).isEqualTo("0");
        assertThat(audit.get("actor_type")).isEqualTo("ADMIN");
        assertThat(((Number) audit.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        // 待办：幂等注册 PENDING、来源标识与业务归属齐备
        Map<String, Object> todo = queryTodo(contractKey);
        assertThat(todo.get("status")).isEqualTo("PENDING");
        assertThat(todo.get("source_type")).isEqualTo(TechNeutralContractSample.TODO_SOURCE_TYPE);
        assertThat(todo.get("biz_type")).isEqualTo(TechNeutralContractSample.OBJECT_TYPE);
        assertThat(((Number) todo.get("todo_version")).longValue()).isZero();
        // 同事务内部事件：进程内同步收到，且监听发生在发布方事务内（与跨进程 Outbox 事件分开）
        assertThat(innerEventListener.received()).hasSize(1);
        TechNeutralContractCreatedEvent event = innerEventListener.received().get(0);
        assertThat(event.getContractKey()).isEqualTo(contractKey);
        assertThat(event.getVersion()).isZero();
        assertThat(innerEventListener.lastReceivedInsideTransaction()).isTrue();
    }

    // ========= 验收① 完成：乐观锁推进 + 事件预写 =========

    @Test
    void test_完成_乐观锁推进与流转事件预写() {
        String contractKey = newKey();
        submit(contractKey);
        sample.complete(contractKey, 0L, "样例完成");
        // 对象：版本推进 + 技术中性完成态
        Map<String, Object> record = queryRecord(contractKey);
        assertThat(record.get("status")).isEqualTo(TechNeutralContractSample.STATUS_DONE);
        assertThat(((Number) record.get("version")).longValue()).isEqualTo(1L);
        // 审计：更新成功携新版本
        Map<String, Object> audit = queryAudit("OBJECT_UPDATED", contractKey);
        assertThat(audit.get("result")).isEqualTo("SUCCESS");
        assertThat(audit.get("biz_version")).isEqualTo("1");
        // Outbox 事件预写：PENDING、同事务、载荷字段齐备（Sink 解码合同）
        Map<String, Object> outbox = queryOutbox(contractKey);
        assertThat(outbox.get("event_type")).isEqualTo(NotifyTodoEventSink.EVENT_TYPE);
        assertThat(outbox.get("status")).isEqualTo("PENDING");
        assertThat(outbox.get("biz_version")).isEqualTo("1");
        assertThat(((Number) outbox.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        Map<String, Object> payload = JsonUtils.parseMap(String.valueOf(outbox.get("payload")));
        assertThat(payload.get("transitionType")).isEqualTo("COMPLETE");
        assertThat(payload.get("todoKey")).isEqualTo(contractKey);
        assertThat(payload.get("sourceType")).isEqualTo(TechNeutralContractSample.TODO_SOURCE_TYPE);
        // 待办尚未推进（等消费侧投递）
        assertThat(queryTodo(contractKey).get("status")).isEqualTo("PENDING");
    }

    // ========= 验收① 派发：事件确认 + 待办完成 + 消费留痕 =========

    @Test
    void test_派发_事件确认待办完成与消费留痕() {
        String contractKey = newKey();
        submit(contractKey);
        sample.complete(contractKey, 0L, "样例完成");
        int delivered = sample.deliverPendingTodoEvents(10);
        assertThat(delivered).isEqualTo(1);
        // 事件确认：DISPATCHED + 确认时间
        Map<String, Object> outbox = queryOutbox(contractKey);
        assertThat(outbox.get("status")).isEqualTo("DISPATCHED");
        assertThat(outbox.get("dispatched_at")).isNotNull();
        // 待办：COMPLETED、todo_version 乐观锁推进、biz_version 覆盖为新版本
        Map<String, Object> todo = queryTodo(contractKey);
        assertThat(todo.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) todo.get("todo_version")).longValue()).isEqualTo(1L);
        assertThat(todo.get("biz_version")).isEqualTo("1");
        // Inbox 消费幂等留痕（eventId 为幂等键）
        Long eventId = ((Number) outbox.get("id")).longValue();
        Map<String, Object> inbox = jdbcTemplate.queryForMap(
                "SELECT * FROM inbox_event WHERE consumer = 'notify_todo' AND event_key = ?", String.valueOf(eventId));
        assertThat(inbox.get("status")).isEqualTo("COMPLETED");
        // 版本水位抬升到已处理版本（乱序护栏基线）
        Long watermark = jdbcTemplate.queryForObject(
                "SELECT version_watermark FROM inbox_object_watermark WHERE biz_id = ?", Long.class, contractKey);
        assertThat(watermark).isEqualTo(1L);
    }

    // ========= 验收② 绕过版本：拒绝 + 独立留痕 + 无副作用 =========

    @Test
    void test_绕过版本_旧版本更新被拒并独立留痕() {
        String contractKey = newKey();
        submit(contractKey);
        // 携带过期期望版本（当前为 0，以 99 绕过）——拒绝且事务回滚
        assertThatThrownBy(() -> sample.complete(contractKey, 99L, "过期版本尝试"))
                .isInstanceOf(IllegalStateException.class);
        // 对象未被改动
        Map<String, Object> record = queryRecord(contractKey);
        assertThat(record.get("status")).isEqualTo(TechNeutralContractSample.STATUS_PENDING);
        assertThat(((Number) record.get("version")).longValue()).isZero();
        // 拒绝留痕独立事务保留（业务事务已回滚，DENIED 记录仍在；记录被拒的期望版本）
        Map<String, Object> denied = queryAudit("ACCESS_DENIED", contractKey);
        assertThat(denied.get("result")).isEqualTo("DENIED");
        assertThat(denied.get("biz_version")).isEqualTo("99");
        // 无事件预写、待办未推进、无更新成功审计（唯一 SUCCESS 为 submit 阶段已提交的 OBJECT_CREATED）
        assertThat(rowCount("outbox_event")).isZero();
        assertThat(queryTodo(contractKey).get("status")).isEqualTo("PENDING");
        assertThat(rowCountWhere("audit_event", "event_type = 'OBJECT_UPDATED'")).isZero();
    }

    // ========= 验收② 乱序旧版本事件：水位拒绝不复活 =========

    @Test
    void test_乱序旧版本事件_水位拒绝不复活() {
        String contractKey = newKey();
        submit(contractKey);
        sample.complete(contractKey, 0L, "样例完成");
        assertThat(sample.deliverPendingTodoEvents(10)).isEqualTo(1);
        // 乱序晚到：旧版本（0）事件在已处理版本（1）之后到达
        insertStaleEvent(contractKey);
        assertThat(sample.deliverPendingTodoEvents(10)).isEqualTo(1);
        // 待办不被旧版本复活 / 回退（版本水位拒绝属可确认终局，事件仍被确认）
        Map<String, Object> todo = queryTodo(contractKey);
        assertThat(todo.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) todo.get("todo_version")).longValue()).isEqualTo(1L);
        assertThat(todo.get("biz_version")).isEqualTo("1");
        assertThat(rowCountWhere("outbox_event", "status = 'DISPATCHED'")).isEqualTo(2);
        // 水位未被旧版本拉低
        Long watermark = jdbcTemplate.queryForObject(
                "SELECT version_watermark FROM inbox_object_watermark WHERE biz_id = ?", Long.class, contractKey);
        assertThat(watermark).isEqualTo(1L);
        // 旧版本事件未创建消费记录（护栏在抢位阶段拒绝，占位随保存点回滚）
        assertThat(rowCount("inbox_event")).isEqualTo(1);
    }

    // ========= 验收② 伪造对象归属：无租户 / 跨租户拒绝 =========

    @Test
    void test_无租户上下文_写侧入口拒绝() {
        TenantContextHolder.clear();
        // 样例写侧入口：归属合同第一道防线（不默认 0、不伪造归属）
        assertThatThrownBy(() -> sample.submit(newKey(), "无租户尝试", NotifyRecipient.admin(RECIPIENT_ID)))
                .isInstanceOf(IllegalStateException.class);
        // 设施层直证：registerTodo 租户强制（缺失即拒绝）
        assertServiceException(() -> notifyTodoService.registerTodo(NotifyTodoRegisterCmd.builder()
                .todoKey(newKey()).sourceType(TechNeutralContractSample.TODO_SOURCE_TYPE)
                .recipient(NotifyRecipient.admin(RECIPIENT_ID)).build()), NOTIFY_TODO_TENANT_REQUIRED);
        assertThat(rowCount("tech_neutral_contract_record")).isZero();
    }

    @Test
    void test_无租户上下文_Outbox追加拒绝() {
        TenantContextHolder.clear();
        // 设施层直证：Outbox 追加的租户强制（缺失即拒绝，不默认写 0）——须在事务内触发租户校验
        assertServiceException(() -> transactionTemplate.executeWithoutResult(status ->
                reliableEventPort.append(OutboxEventMessage.builder()
                        .eventType(NotifyTodoEventSink.EVENT_TYPE)
                        .bizType(TechNeutralContractSample.OBJECT_TYPE)
                        .bizId("CT_NO_TENANT")
                        .bizVersion("0")
                        .payload(Map.of("transitionType", "COMPLETE",
                                "todoKey", "CT_NO_TENANT",
                                "sourceType", TechNeutralContractSample.TODO_SOURCE_TYPE))
                        .actorType(OutboxActorType.SYSTEM)
                        .actorId(TechNeutralContractSample.SAMPLE_ACTOR_ID)
                        .build())), OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED);
        assertThat(rowCount("outbox_event")).isZero();
    }

    @Test
    void test_跨租户_对象不可见不可改() {
        String contractKey = newKey();
        submit(contractKey);
        // 伪造归属视角：以另一租户上下文操作本租户对象——按租户过滤 0 行即拒
        TenantContextHolder.setTenantId(OTHER_TENANT_ID);
        assertThatThrownBy(() -> sample.complete(contractKey, 0L, "跨租户尝试"))
                .isInstanceOf(IllegalStateException.class);
        // 对象仍属原租户且未被改动
        Map<String, Object> record = queryRecord(contractKey);
        assertThat(((Number) record.get("tenant_id")).longValue()).isEqualTo(TENANT_ID);
        assertThat(record.get("status")).isEqualTo(TechNeutralContractSample.STATUS_PENDING);
        assertThat(((Number) record.get("version")).longValue()).isZero();
        // 拒绝留痕（独立事务，记请求方租户上下文）
        Map<String, Object> denied = jdbcTemplate.queryForMap(
                "SELECT * FROM audit_event WHERE event_type = 'ACCESS_DENIED' AND tenant_id = ?", OTHER_TENANT_ID);
        assertThat(denied.get("result")).isEqualTo("DENIED");
        assertThat(rowCount("outbox_event")).isZero();
    }

    // ========= 验收② 重复来源消息：注册幂等 + 事件幂等 =========

    @Test
    void test_重复注册_同键幂等返回既有() {
        String contractKey = newKey();
        submit(contractKey);
        Long todoId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_notify_todo WHERE todo_key = ?", Long.class, contractKey);
        // 同 (租户, sourceType, todoKey) 重复注册：返回既有 ID，不产生第二行
        Long again = notifyTodoService.registerTodo(NotifyTodoRegisterCmd.builder()
                .todoKey(contractKey).sourceType(TechNeutralContractSample.TODO_SOURCE_TYPE)
                .recipient(NotifyRecipient.admin(RECIPIENT_ID))
                .build());
        assertThat(again).isEqualTo(todoId);
        assertThat(rowCount("system_notify_todo")).isEqualTo(1);
    }

    @Test
    void test_重复事件_同事件不重复副作用() {
        String contractKey = newKey();
        submit(contractKey);
        sample.complete(contractKey, 0L, "样例完成");
        assertThat(sample.deliverPendingTodoEvents(10)).isEqualTo(1);
        // 模拟 at-least-once 重投：确认丢失后事件回到 PENDING，同 eventId 再次投递
        Long eventId = jdbcTemplate.queryForObject(
                "SELECT id FROM outbox_event WHERE biz_id = ?", Long.class, contractKey);
        jdbcTemplate.update("UPDATE outbox_event SET status = 'PENDING', dispatched_at = NULL WHERE id = ?", eventId);
        assertThat(sample.deliverPendingTodoEvents(10)).isEqualTo(1);
        // Inbox 幂等吸收：待办终态与版本不变（无重复副作用），事件仍被确认
        Map<String, Object> todo = queryTodo(contractKey);
        assertThat(todo.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) todo.get("todo_version")).longValue()).isEqualTo(1L);
        assertThat(queryOutbox(contractKey).get("status")).isEqualTo("DISPATCHED");
        assertThat(rowCount("inbox_event")).isEqualTo(1);
    }

    // ========= 内部工具 =========

    private String newKey() {
        return "CT_" + UUID.randomUUID();
    }

    private Long submit(String contractKey) {
        return sample.submit(contractKey, "中性合同样例任务", NotifyRecipient.admin(RECIPIENT_ID));
    }

    /** 伪乱序旧版本事件：模拟事件乱序到达（新 eventId、旧 biz_version），完整载荷以通过 Sink 解码。 */
    private void insertStaleEvent(String contractKey) {
        jdbcTemplate.update("INSERT INTO outbox_event "
                        + "(event_type, biz_type, biz_id, biz_version, payload, tenant_id, actor_type, actor_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'SYSTEM', ?)",
                NotifyTodoEventSink.EVENT_TYPE, TechNeutralContractSample.OBJECT_TYPE, contractKey, "0",
                JsonUtils.toJsonString(Map.of(
                        "transitionType", "COMPLETE",
                        "todoKey", contractKey,
                        "sourceType", TechNeutralContractSample.TODO_SOURCE_TYPE,
                        "reason", "乱序晚到的旧版本事件")),
                TENANT_ID, "stale-event-probe");
    }

    private Map<String, Object> queryRecord(String contractKey) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM tech_neutral_contract_record WHERE tenant_id = ? AND contract_key = ?",
                TENANT_ID, contractKey);
    }

    private Map<String, Object> queryTodo(String contractKey) {
        return jdbcTemplate.queryForMap("SELECT * FROM system_notify_todo WHERE todo_key = ?", contractKey);
    }

    private Map<String, Object> queryAudit(String eventType, String bizId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM audit_event WHERE event_type = ? AND biz_id = ?", eventType, bizId);
    }

    private Map<String, Object> queryOutbox(String bizId) {
        return jdbcTemplate.queryForMap("SELECT * FROM outbox_event WHERE biz_id = ?", bizId);
    }

    private int rowCount(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private int rowCountWhere(String table, String where) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table + " WHERE " + where,
                Integer.class);
        return count == null ? 0 : count;
    }

}
