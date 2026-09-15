package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.inbox.JdbcConsumerInboxPort;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTodoDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.todo.DefaultTodoStatusMapper;
import cn.zszj.module.system.service.notify.todo.NotifyTodoRegisterCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import cn.zszj.module.system.service.notify.todo.NotifyTodoServiceImpl;
import cn.zszj.module.system.service.notify.todo.NotifyTodoStatus;
import cn.zszj.module.system.service.notify.todo.NotifyTodoTransitionCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoTransitionResult;
import cn.zszj.module.system.service.notify.todo.TodoStatusMapper;
import cn.zszj.module.system.service.notify.todo.TodoTransitionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link NotifyTodoService} 单元测试（ZS-MSG-002，H2）——业务待办生命周期与站内信已读解耦。
 *
 * <p>覆盖 25 用例，对齐 docs/05 ZS-MSG-002 四条验收：
 * ①阅读消息不会完成业务任务（双向解耦）；②业务完成/撤回/转派更新正确；
 * ③重复/乱序事件不复活已失效待办（经 JOB-003 Inbox 幂等 + 版本水位 + 应用层终态守卫）；
 * ④首链启用前按 D-07 验证状态映射（DefaultTodoStatusMapper 不硬编码试点业务态）。
 *
 * <p>codex r0 修复补强（F2/F5/F6/F7/F8）：待办行自身 biz_version 单调护栏（业务键缺失 + 注册基线两入口）、
 * 载荷指纹长度前缀编码（分隔符歧义判 PARAM_CONFLICT）、重复事件重放 Inbox 首次结果、sourceType 必填校验、
 * todoKey/transitionType 缺失错误码拆分。F1（并发注册保存点恢复 PG 事务）以 msg002-pg-verify.mjs 做机制级 PG 验证。
 *
 * <p>循 MSG-001 NotifyDispatcherTest / JOB-003 Inbox 先例：H2 + BaseDbUnitTest + @Import 显式装配
 * （含 {@link JdbcConsumerInboxPort} 提供 ConsumerInboxPort）+ 注入 DataSource/PlatformTransactionManager
 * 手动构建 JdbcTemplate/TransactionTemplate（BaseDbUnitTest 不直接暴露二者为 Bean）+
 * TransactionTemplate 显式控制事务（applyTransition 须在业务事务内——Inbox MANDATORY）+ JdbcTemplate 直查断言。
 */
@Import({NotifyTodoServiceImpl.class, NotifyMessageServiceImpl.class, DefaultTodoStatusMapper.class,
        JdbcConsumerInboxPort.class})
public class NotifyTodoServiceTest extends BaseDbUnitTest {

    @Resource
    private NotifyTodoService notifyTodoService;
    @Resource
    private NotifyMessageService notifyMessageService;
    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private TodoStatusMapper todoStatusMapper;
    @Resource
    private DataSource dataSource;
    @Resource
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final Long ADMIN_USER_ID = 100L;
    private static final Long ASSIGNEE_USER_ID = 200L;
    private static final String SOURCE = "BPM";

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        jdbcTemplate.execute("DELETE FROM system_notify_todo");
        jdbcTemplate.execute("DELETE FROM system_notify_message");
        jdbcTemplate.execute("DELETE FROM inbox_event");
        jdbcTemplate.execute("DELETE FROM inbox_object_watermark");
    }

    // ========= 验收① 阅读消息 ≠ 完成业务任务（双向解耦） =========

    @Test
    void test_阅读站内消息_不完成业务待办() {
        Long messageId = insertMessage(ADMIN_USER_ID, UserTypeEnum.ADMIN.getValue());
        Long todoId = register(registerCmd("TODO_" + UUID.randomUUID()).toBuilder().messageId(messageId).build());
        // 阅读站内信（真实消息服务标记已读）
        int read = notifyMessageService.updateNotifyMessageRead(List.of(messageId), ADMIN_USER_ID,
                UserTypeEnum.ADMIN.getValue());
        assertThat(read).isEqualTo(1);
        // 待办仍为 PENDING——阅读消息不完成业务任务
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.PENDING.name());
    }

    @Test
    void test_完成业务待办_不标记消息已读() {
        Long messageId = insertMessage(ADMIN_USER_ID, UserTypeEnum.ADMIN.getValue());
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey).toBuilder().messageId(messageId).build());
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.COMPLETE, todoKey));
        assertThat(r.isChanged()).isTrue();
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.COMPLETED.name());
        // 关联站内信仍未读——完成待办不改消息已读
        NotifyMessageDO message = notifyMessageMapper.selectById(messageId);
        assertThat(message.getReadStatus()).isFalse();
    }

    // ========= 验收② 业务完成/撤回/转派更新正确 =========

    @Test
    void test_业务完成_更新正确() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.COMPLETE, todoKey));
        assertThat(r.isChanged()).isTrue();
        assertThat(r.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_APPLIED);
        assertThat(r.getStatus()).isEqualTo(NotifyTodoStatus.COMPLETED);
        Map<String, Object> row = queryTodo(todoId);
        assertThat(row.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) row.get("todo_version")).longValue()).isEqualTo(1L);
    }

    @Test
    void test_业务撤回_更新正确() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.WITHDRAW, todoKey).toBuilder().reason("发起人撤回").build());
        assertThat(r.isChanged()).isTrue();
        assertThat(r.getStatus()).isEqualTo(NotifyTodoStatus.WITHDRAWN);
        Map<String, Object> row = queryTodo(todoId);
        assertThat(row.get("status")).isEqualTo("WITHDRAWN");
        assertThat(row.get("status_reason")).isEqualTo("发起人撤回");
    }

    @Test
    void test_业务转派_更新assignee与状态() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.REASSIGN, todoKey).toBuilder()
                .assignee(NotifyRecipient.admin(ASSIGNEE_USER_ID)).build());
        assertThat(r.isChanged()).isTrue();
        assertThat(r.getStatus()).isEqualTo(NotifyTodoStatus.REASSIGNED);
        Map<String, Object> row = queryTodo(todoId);
        assertThat(row.get("status")).isEqualTo("REASSIGNED");
        assertThat(row.get("assignee_type")).isEqualTo("ADMIN");
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(ASSIGNEE_USER_ID);
    }

    // ========= 验收③ 重复/乱序事件不复活已失效待办 =========

    @Test
    void test_重复完成事件_幂等不重复副作用() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyTodoTransitionCmd cmd = transitionCmd(eventId, TodoTransitionType.COMPLETE, todoKey);
        NotifyTodoTransitionResult r1 = transition(cmd);
        assertThat(r1.isChanged()).isTrue();
        // 同 eventId 重复投递：命中 Inbox 幂等，不重复副作用、todo_version 不再增
        NotifyTodoTransitionResult r2 = transition(cmd);
        assertThat(r2.isChanged()).isFalse();
        assertThat(r2.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_DUPLICATE_COMPLETED);
        assertThat(((Number) queryTodo(todoId).get("todo_version")).longValue()).isEqualTo(1L);
    }

    @Test
    void test_已失效待办_后到完成事件不复活() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        // 先撤回 → 终态 WITHDRAWN
        transition(transitionCmd("EVT_" + UUID.randomUUID(), TodoTransitionType.WITHDRAW, todoKey));
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.WITHDRAWN.name());
        // 后到的新事件 COMPLETE 作用于终态待办 → 终态守卫，不复活
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.COMPLETE, todoKey));
        assertThat(r.isChanged()).isFalse();
        assertThat(r.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_TERMINAL_GUARDED);
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.WITHDRAWN.name());
    }

    @Test
    void test_乱序旧版本事件_不覆盖新状态() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey).toBuilder().bizType("task").bizId("T-1").build());
        // 新版本 v2 先完成
        NotifyTodoTransitionResult r1 = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.COMPLETE, todoKey).toBuilder().bizType("task").bizId("T-1").bizVersion("2").build());
        assertThat(r1.isChanged()).isTrue();
        // 旧版本 v1 后到（不同 eventId）→ 版本水位拒绝 STALE_VERSION，不覆盖新状态
        NotifyTodoTransitionResult r2 = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.WITHDRAW, todoKey).toBuilder().bizType("task").bizId("T-1").bizVersion("1").build());
        assertThat(r2.isChanged()).isFalse();
        assertThat(r2.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_STALE_VERSION);
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.COMPLETED.name());
    }

    // ========= 验收③ 补强（codex r0 F2/F5/F6）：待办行版本护栏 / 指纹确定性 / 首次结果重放 =========

    @Test
    void test_业务键缺失_旧版本转派不覆盖新处理人() {
        // F2 入口①：bizType/bizId 缺失 → Inbox 对象水位跳过版本比较；待办行自身 biz_version 护栏兜底
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        // v2 转派给 ADMIN(ASSIGNEE_USER_ID)
        NotifyTodoTransitionResult r2 = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.REASSIGN, todoKey).toBuilder()
                .bizVersion("2").assignee(NotifyRecipient.admin(ASSIGNEE_USER_ID)).build());
        assertThat(r2.isChanged()).isTrue();
        assertThat(((Number) queryTodo(todoId).get("assignee_id")).longValue()).isEqualTo(ASSIGNEE_USER_ID);
        // v1 转派给 MEMBER(300) 后到（旧版本，不同 eventId）→ 待办行护栏拒绝，assignee/biz_version/todo_version 不回退
        NotifyTodoTransitionResult r1 = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.REASSIGN, todoKey).toBuilder()
                .bizVersion("1").assignee(NotifyRecipient.member(300L)).build());
        assertThat(r1.isChanged()).isFalse();
        assertThat(r1.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_STALE_VERSION);
        Map<String, Object> row = queryTodo(todoId);
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(ASSIGNEE_USER_ID);
        assertThat(row.get("biz_version")).isEqualTo("2");
        assertThat(((Number) row.get("todo_version")).longValue()).isEqualTo(1L);
    }

    @Test
    void test_注册已存版本_旧版本流转被待办行护栏拒绝() {
        // F2 入口②：注册即存 biz_version=10 + 初始处理人，但 Inbox 水位不 seed 注册基线（水位仅首次流转以该次版本初始化）
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey).toBuilder()
                .bizType("task").bizId("T-9").bizVersion("10")
                .assignee(NotifyRecipient.admin(ADMIN_USER_ID)).build());
        // v9 旧版本 REASSIGN 给不同处理人（旧于注册基线）→ Inbox 水位以 9 首建不会拒；待办行护栏（baseline=10）拒绝。
        // codex r1 ②：补处理人维度——断言 status/assignee/biz_version/todo_version 均不被旧事件覆盖
        NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.REASSIGN, todoKey).toBuilder()
                .bizType("task").bizId("T-9").bizVersion("9")
                .assignee(NotifyRecipient.member(ASSIGNEE_USER_ID)).build());
        assertThat(r.isChanged()).isFalse();
        assertThat(r.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_STALE_VERSION);
        Map<String, Object> row = queryTodo(todoId);
        assertThat(row.get("status")).isEqualTo(NotifyTodoStatus.PENDING.name());
        assertThat(((Number) row.get("assignee_id")).longValue()).isEqualTo(ADMIN_USER_ID);
        assertThat(row.get("assignee_type")).isEqualTo("ADMIN");
        assertThat(row.get("biz_version")).isEqualTo("10");
        assertThat(((Number) row.get("todo_version")).longValue()).isZero();
    }

    @Test
    void test_重复事件_返回首次结果而非当前状态() {
        // F6：E1 转派 → E2 完成 → 重投 E1，应返回 E1 首次结果 REASSIGNED（而非当前 COMPLETED）+ 首次 reason（R3）
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        NotifyTodoTransitionCmd e1 = transitionCmd("EVT_" + UUID.randomUUID(),
                TodoTransitionType.REASSIGN, todoKey).toBuilder()
                .assignee(NotifyRecipient.admin(ASSIGNEE_USER_ID)).reason("转派给区域经理").build();
        NotifyTodoTransitionResult r1 = transition(e1);
        assertThat(r1.isChanged()).isTrue();
        assertThat(r1.getStatus()).isEqualTo(NotifyTodoStatus.REASSIGNED);
        assertThat(r1.getReason()).isEqualTo("转派给区域经理");
        transition(transitionCmd("EVT_" + UUID.randomUUID(), TodoTransitionType.COMPLETE, todoKey));
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.COMPLETED.name());
        // 重投 E1（同 eventId）→ 返回首次结果 REASSIGNED + 首次 reason，非当前 COMPLETED
        NotifyTodoTransitionResult replay = transition(e1);
        assertThat(replay.isChanged()).isFalse();
        assertThat(replay.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_DUPLICATE_COMPLETED);
        assertThat(replay.getStatus()).isEqualTo(NotifyTodoStatus.REASSIGNED);
        assertThat(replay.getTodoId()).isEqualTo(todoId);
        assertThat(replay.getReason()).isEqualTo("转派给区域经理");
    }

    @Test
    void test_载荷指纹长度前缀_分隔符歧义判参数冲突() {
        // F5：todoKey="A|B"+sourceType="C" 与 todoKey="A"+sourceType="B|C" 在 | 拼接下哈希输入相同；
        // 长度前缀编码后指纹不同 → 同 eventId 不同目标判 PARAM_CONFLICT（而非误判 DUPLICATE_COMPLETED）
        String eventId = "EVT_" + UUID.randomUUID();
        register(registerCmd("A|B").toBuilder().sourceType("C").build());
        NotifyTodoTransitionResult r1 = transition(NotifyTodoTransitionCmd.builder()
                .eventId(eventId).transitionType(TodoTransitionType.COMPLETE).todoKey("A|B").sourceType("C").build());
        assertThat(r1.isChanged()).isTrue();
        NotifyTodoTransitionResult r2 = transition(NotifyTodoTransitionCmd.builder()
                .eventId(eventId).transitionType(TodoTransitionType.COMPLETE).todoKey("A").sourceType("B|C").build());
        assertThat(r2.isChanged()).isFalse();
        assertThat(r2.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_PARAM_CONFLICT);
    }

    @Test
    void test_载荷指纹_null与空串版本判参数冲突() {
        // R2：bizVersion=null（COALESCE 保留既有）与 ""（覆盖既有）写库语义不同，指纹须区分 → 同 eventId 判 PARAM_CONFLICT
        String todoKey = "TODO_" + UUID.randomUUID();
        register(registerCmd(todoKey).toBuilder().bizType("task").bizId("T-1").bizVersion("10").build());
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyTodoTransitionResult r1 = transition(transitionCmd(eventId, TodoTransitionType.WITHDRAW, todoKey)
                .toBuilder().bizType("task").bizId("T-1").bizVersion(null).build());
        assertThat(r1.isChanged()).isTrue();
        // 同 eventId、其余字段相同，仅 bizVersion 由 null 改空串 → 指纹不同 → PARAM_CONFLICT（而非误判 DUPLICATE_COMPLETED）
        NotifyTodoTransitionResult r2 = transition(transitionCmd(eventId, TodoTransitionType.WITHDRAW, todoKey)
                .toBuilder().bizType("task").bizId("T-1").bizVersion("").build());
        assertThat(r2.isChanged()).isFalse();
        assertThat(r2.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_PARAM_CONFLICT);
    }

    // ========= 幂等注册 / Inbox 同事务抢位 / 回滚 =========

    @Test
    void test_幂等注册_同key不重复建() {
        String todoKey = "TODO_" + UUID.randomUUID();
        NotifyTodoRegisterCmd cmd = registerCmd(todoKey);
        Long id1 = register(cmd);
        Long id2 = register(cmd);
        assertThat(id1).isEqualTo(id2);
        assertThat(countTodos()).isEqualTo(1);
    }

    @Test
    void test_成功流转_同事务内inbox抢位complete() {
        String todoKey = "TODO_" + UUID.randomUUID();
        register(registerCmd(todoKey));
        String eventId = "EVT_" + UUID.randomUUID();
        transition(transitionCmd(eventId, TodoTransitionType.COMPLETE, todoKey));
        Map<String, Object> inbox = jdbcTemplate.queryForMap(
                "SELECT status FROM inbox_event WHERE consumer = 'notify_todo' AND event_key = ? AND tenant_id = ?",
                eventId, TENANT_ID);
        assertThat(inbox.get("status")).isEqualTo("COMPLETED");
    }

    @Test
    void test_业务事务回滚_待办与inbox抢位一并回滚() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyTodoTransitionCmd cmd = transitionCmd(eventId, TodoTransitionType.COMPLETE, todoKey);
        transactionTemplate.execute(s -> {
            notifyTodoService.applyTransition(cmd);
            s.setRollbackOnly();
            return null;
        });
        // 待办状态未变 + inbox 无残留（Inbox MANDATORY 与业务副作用同事务回滚）
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.PENDING.name());
        Integer inboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM inbox_event WHERE event_key = ?", Integer.class, eventId);
        assertThat(inboxCount).isZero();
    }

    @Test
    void test_待办不存在_流转返回未生效且inbox可重试() {
        String todoKey = "TODO_MISSING_" + UUID.randomUUID();
        String eventId = "EVT_" + UUID.randomUUID();
        NotifyTodoTransitionResult r = transition(transitionCmd(eventId, TodoTransitionType.COMPLETE, todoKey));
        assertThat(r.isChanged()).isFalse();
        assertThat(r.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_NOT_FOUND);
        // inbox 记录标记 FAILED（可经 tryBegin 重领重试，不占终态）
        Map<String, Object> inbox = jdbcTemplate.queryForMap(
                "SELECT status FROM inbox_event WHERE consumer = 'notify_todo' AND event_key = ? AND tenant_id = ?",
                eventId, TENANT_ID);
        assertThat(inbox.get("status")).isEqualTo("FAILED");
    }

    @Test
    void test_跨技术租户_待办隔离() {
        String todoKey = "TODO_" + UUID.randomUUID();
        Long todoId = register(registerCmd(todoKey));
        NotifyRecipient recipient = NotifyRecipient.admin(ADMIN_USER_ID);
        assertThat(notifyTodoService.getUnfinishedCount(recipient)).isEqualTo(1L);
        // 切到租户 B：同 key 待办不可见、流转不生效
        TenantContextHolder.setTenantId(OTHER_TENANT_ID);
        try {
            assertThat(notifyTodoService.getUnfinishedCount(recipient)).isZero();
            NotifyTodoTransitionResult r = transition(transitionCmd("EVT_" + UUID.randomUUID(),
                    TodoTransitionType.COMPLETE, todoKey));
            assertThat(r.isChanged()).isFalse();
            assertThat(r.getOutcome()).isEqualTo(NotifyTodoTransitionResult.OUTCOME_NOT_FOUND);
        } finally {
            TenantContextHolder.setTenantId(TENANT_ID);
        }
        // 租户 A 的待办未被租户 B 影响
        assertThat(statusOf(todoId)).isEqualTo(NotifyTodoStatus.PENDING.name());
    }

    // ========= 必填校验（fail-closed） =========

    @Test
    void test_eventId为空_抛异常() {
        String todoKey = "TODO_" + UUID.randomUUID();
        register(registerCmd(todoKey));
        NotifyTodoTransitionCmd cmd = transitionCmd(null, TodoTransitionType.COMPLETE, todoKey);
        assertThatThrownBy(() -> notifyTodoService.applyTransition(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("eventId");
    }

    @Test
    void test_todoKey为空_注册抛异常() {
        NotifyTodoRegisterCmd cmd = registerCmd(null);
        assertThatThrownBy(() -> notifyTodoService.registerTodo(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("todoKey");
    }

    @Test
    void test_收件人为空_注册抛异常() {
        NotifyTodoRegisterCmd cmd = NotifyTodoRegisterCmd.builder()
                .todoKey("TODO_" + UUID.randomUUID()).sourceType(SOURCE).recipient(null).build();
        assertThatThrownBy(() -> notifyTodoService.registerTodo(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("收件人");
    }

    @Test
    void test_sourceType为空_注册抛字段校验异常() {
        // F7：sourceType 为幂等键组成，空白即拒（不落 DB 约束异常）
        NotifyTodoRegisterCmd cmd = NotifyTodoRegisterCmd.builder()
                .todoKey("TODO_" + UUID.randomUUID()).sourceType("  ")
                .recipient(NotifyRecipient.admin(ADMIN_USER_ID)).build();
        assertThatThrownBy(() -> notifyTodoService.registerTodo(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("sourceType");
    }

    @Test
    void test_sourceType为空_流转抛字段校验异常() {
        // F7：抢位前校验 sourceType（避免 null 误分类 NOT_FOUND 留 FAILED 记录）
        NotifyTodoTransitionCmd cmd = transitionCmd("EVT_" + UUID.randomUUID(), TodoTransitionType.COMPLETE,
                "TODO_" + UUID.randomUUID()).toBuilder().sourceType(null).build();
        assertThatThrownBy(() -> notifyTodoService.applyTransition(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("sourceType");
    }

    @Test
    void test_transitionType为空_抛字段校验异常非eventId() {
        // F8：transitionType 缺失用 FIELD_REQUIRED（不复用 eventId 错误码误导排查）
        NotifyTodoTransitionCmd cmd = transitionCmd("EVT_" + UUID.randomUUID(), null, "TODO_" + UUID.randomUUID());
        assertThatThrownBy(() -> notifyTodoService.applyTransition(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("transitionType");
    }

    @Test
    void test_流转todoKey为空_抛KEY_REQUIRED() {
        // F8：todoKey 缺失用 KEY_REQUIRED（不复用 eventId 错误码）
        NotifyTodoTransitionCmd cmd = transitionCmd("EVT_" + UUID.randomUUID(), TodoTransitionType.COMPLETE, "");
        assertThatThrownBy(() -> notifyTodoService.applyTransition(cmd))
                .isInstanceOf(Exception.class).hasMessageContaining("todoKey");
    }

    // ========= 验收④ D-07 状态映射扩展点：不硬编码试点业务态 =========

    @Test
    void test_DefaultTodoStatusMapper_只识别通用机制态_不硬编码试点业务态() {
        // 通用机制态标识 → 对应流转类型
        assertThat(todoStatusMapper.mapTransition("GENERIC", "COMPLETED")).isEqualTo(TodoTransitionType.COMPLETE);
        assertThat(todoStatusMapper.mapTransition("GENERIC", "WITHDRAWN")).isEqualTo(TodoTransitionType.WITHDRAW);
        assertThat(todoStatusMapper.mapTransition("GENERIC", "REASSIGNED")).isEqualTo(TodoTransitionType.REASSIGN);
        assertThat(todoStatusMapper.mapTransition("GENERIC", "INVALID")).isEqualTo(TodoTransitionType.INVALIDATE);
        // D-07 试点业务态（加盟商申请/线索）不被 Default 识别为业务流转——不固化试点状态（D-07 红线）
        assertThat(todoStatusMapper.mapTransition("GENERIC", "PENDING_APPROVAL")).isNull();
        assertThat(todoStatusMapper.mapTransition("GENERIC", "APPROVED")).isNull();
        assertThat(todoStatusMapper.mapTransition("GENERIC", "CONVERTED")).isNull();
        assertThat(todoStatusMapper.mapTransition("GENERIC", "NEW")).isNull();
    }

    // ========= 夹具 =========

    private NotifyTodoRegisterCmd registerCmd(String todoKey) {
        return NotifyTodoRegisterCmd.builder()
                .todoKey(todoKey).sourceType(SOURCE)
                .title("测试待办").recipient(NotifyRecipient.admin(ADMIN_USER_ID)).build();
    }

    private NotifyTodoTransitionCmd transitionCmd(String eventId, TodoTransitionType type, String todoKey) {
        return NotifyTodoTransitionCmd.builder()
                .eventId(eventId).transitionType(type).todoKey(todoKey).sourceType(SOURCE)
                .actorType(OutboxActorType.SYSTEM).build();
    }

    private Long register(NotifyTodoRegisterCmd cmd) {
        return transactionTemplate.execute(s -> notifyTodoService.registerTodo(cmd));
    }

    private NotifyTodoTransitionResult transition(NotifyTodoTransitionCmd cmd) {
        return transactionTemplate.execute(s -> notifyTodoService.applyTransition(cmd));
    }

    private Long insertMessage(Long userId, Integer userType) {
        NotifyMessageDO message = new NotifyMessageDO().setUserId(userId).setUserType(userType)
                .setTemplateId(1L).setTemplateCode("T").setTemplateType(1).setTemplateNickname("n")
                .setTemplateContent("c").setTemplateParams(Map.of("k", "v")).setReadStatus(false);
        notifyMessageMapper.insert(message);
        return message.getId();
    }

    private Map<String, Object> queryTodo(Long todoId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, status_reason, todo_version, assignee_type, assignee_id, biz_version "
                        + "FROM system_notify_todo WHERE id = ?",
                todoId);
    }

    private String statusOf(Long todoId) {
        return (String) queryTodo(todoId).get("status");
    }

    private int countTodos() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM system_notify_todo", Integer.class);
        return c != null ? c : 0;
    }
}
