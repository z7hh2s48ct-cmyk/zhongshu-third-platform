package cn.zszj.module.system.service.notify.todo;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.inbox.ConsumerInboxPort;
import cn.zszj.module.infra.framework.inbox.InboxCommand;
import cn.zszj.module.infra.framework.inbox.InboxRecord;
import cn.zszj.module.infra.framework.inbox.InboxTryBegin;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTodoDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyTodoMapper;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_EVENT_ID_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_FIELD_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_KEY_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_NOT_FOUND;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_RECIPIENT_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_TENANT_REQUIRED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_TODO_WRITE_FAILED;

/**
 * {@link NotifyTodoService} 实现（ZS-MSG-002）——业务待办生命周期，独立于站内信已读语义。
 *
 * <p>关键机制（对齐 docs/05 §11 验收 + 计划 §2.3）：
 * <ul>
 *   <li><b>幂等注册</b>：唯一键 (tenant, sourceType, todoKey) 预检 + 并发撞键复判，重复注册返回既有；</li>
 *   <li><b>可靠事件更新</b>：{@link #applyTransition} 在业务事务内经 {@link ConsumerInboxPort#tryBegin}
 *       抢位（consumer={@code notify_todo}，eventKey=eventId）——重复事件 DUPLICATE_COMPLETED 返回首次结果不重复
 *       副作用；乱序旧版本经对象版本水位 STALE_VERSION 拒绝（{@code checkVersionStale} 仅对可解析整数版本生效）；</li>
 *   <li><b>应用层终态守卫</b>（非 DB 状态流转约束，循 D-07「不把流转条件写成不可逆约束」）：终态待办
 *       （COMPLETED/WITHDRAWN/INVALID）不被后到事件复活，返回 TERMINAL_GUARDED、changed=false；</li>
 *   <li><b>已读独立</b>：本实现全程不触碰 {@code NotifyMessageDO} 的 readStatus/readTime；</li>
 *   <li><b>fail-closed</b>：租户缺失即拒绝（不默认 0）；DB 写失败抛异常触发事务回滚，Inbox 抢位随业务事务
 *       一并回滚（ZS-JOB-003 MANDATORY 保证）；待办不存在标记 Inbox FAILED（可重试，不占终态）。</li>
 * </ul>
 *
 * <p>不复制第二套审批引擎：待办是 BPM Flowable 任务权威状态的投影，本实现只应用外部事件驱动的通用状态流转。
 */
@Service
@Slf4j
public class NotifyTodoServiceImpl implements NotifyTodoService {

    /** Inbox 消费者标识（处理键 = 租户 + consumer + eventId）。 */
    static final String CONSUMER = "notify_todo";

    @Resource
    private NotifyTodoMapper notifyTodoMapper;

    @Resource
    private ConsumerInboxPort inboxPort;

    @Resource
    private DataSource dataSource;

    /** 与 MyBatis insert 同一事务连接（JdbcTemplate 经 DataSourceUtils 绑定）：并发撞键后建/回滚/释放保存点恢复 PG 事务可用性。 */
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    private void initJdbcTemplate() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long registerTodo(NotifyTodoRegisterCmd cmd) {
        Long tenantId = requireTenant();
        if (cmd == null || !StringUtils.hasText(cmd.getTodoKey())) {
            throw exception(NOTIFY_TODO_KEY_REQUIRED);
        }
        if (!StringUtils.hasText(cmd.getSourceType())) {
            // F7：sourceType 为幂等键组成，空白即拒绝（避免落 DB 约束异常或后续流转错分 NOT_FOUND）
            throw exception(NOTIFY_TODO_FIELD_REQUIRED, "sourceType");
        }
        if (cmd.getRecipient() == null || cmd.getRecipient().getId() == null
                || cmd.getRecipient().getType() == null) {
            throw exception(NOTIFY_TODO_RECIPIENT_REQUIRED);
        }
        NotifyTodoDO existing = notifyTodoMapper.selectByUniqueKey(tenantId, cmd.getSourceType(), cmd.getTodoKey());
        if (existing != null) {
            return existing.getId();
        }
        NotifyTodoDO todo = NotifyTodoDO.builder()
                .todoKey(cmd.getTodoKey()).sourceType(cmd.getSourceType())
                .bizType(cmd.getBizType()).bizId(cmd.getBizId()).bizVersion(cmd.getBizVersion())
                .title(cmd.getTitle()).messageId(cmd.getMessageId())
                .recipientType(cmd.getRecipient().getType().name()).recipientId(cmd.getRecipient().getId())
                .assigneeType(cmd.getAssignee() != null && cmd.getAssignee().getType() != null
                        ? cmd.getAssignee().getType().name() : null)
                .assigneeId(cmd.getAssignee() != null ? cmd.getAssignee().getId() : null)
                .status(NotifyTodoStatus.PENDING.name()).todoVersion(0L).tenantId(tenantId)
                .build();
        // F1：插入前建同连接保存点——并发撞唯一键后 PG 事务因 23505 中止（25P02），须回滚到保存点恢复
        // 事务可用性，复判查询才能返回既有 ID（循 JdbcConsumerInboxPort#claimNew 先例）。无事务（autocommit）
        // 时语句级回滚已足够，不建保存点。
        Savepoint savepoint = TransactionSynchronizationManager.isActualTransactionActive()
                ? createRegisterSavepoint() : null;
        try {
            notifyTodoMapper.insert(todo);
            // R4：插入成功后释放保存点——PG RELEASE SAVEPOINT 销毁之，避免外层事务批量注册时保存点资源累积
            if (savepoint != null) {
                releaseRegisterSavepoint(savepoint);
            }
            return todo.getId();
        } catch (DuplicateKeyException e) {
            if (savepoint != null) {
                // 撞键：回滚到保存点恢复事务可用性（25P02→可用），再释放（PG ROLLBACK TO 保留目标保存点须另行 RELEASE）；
                // 释放失败则异常向外传播触发外层回滚 + 幂等重试（注册幂等，不掩盖已处理结果）
                rollbackRegisterSavepoint(savepoint);
                releaseRegisterSavepoint(savepoint);
            }
            // 并发撞唯一键：复判返回既有（幂等）
            NotifyTodoDO concurrent = notifyTodoMapper.selectByUniqueKey(tenantId, cmd.getSourceType(), cmd.getTodoKey());
            if (concurrent == null) {
                throw exception(NOTIFY_TODO_WRITE_FAILED);
            }
            return concurrent.getId();
        }
    }

    @Override
    public NotifyTodoTransitionResult applyTransition(NotifyTodoTransitionCmd cmd) {
        Long tenantId = requireTenant();
        if (cmd == null || !StringUtils.hasText(cmd.getEventId())) {
            throw exception(NOTIFY_TODO_EVENT_ID_REQUIRED);
        }
        // F8：拆分错误码——todoKey/transitionType 缺失不再复用 eventId 错误码（避免误导排查）
        if (!StringUtils.hasText(cmd.getTodoKey())) {
            throw exception(NOTIFY_TODO_KEY_REQUIRED);
        }
        if (cmd.getTransitionType() == null) {
            throw exception(NOTIFY_TODO_FIELD_REQUIRED, "transitionType");
        }
        // F7：sourceType 为待办定位键组成，抢位前校验（避免 null 误分类 NOT_FOUND 留 FAILED 记录）
        if (!StringUtils.hasText(cmd.getSourceType())) {
            throw exception(NOTIFY_TODO_FIELD_REQUIRED, "sourceType");
        }
        InboxCommand inboxCmd = InboxCommand.builder()
                .consumer(CONSUMER).eventKey(cmd.getEventId())
                .payloadHash(payloadHash(cmd))
                .bizType(cmd.getBizType()).bizId(cmd.getBizId()).bizVersion(cmd.getBizVersion())
                .checkVersionStale(isParsableVersion(cmd.getBizVersion()))
                .traceId(cmd.getTraceId())
                .build();
        InboxTryBegin begin = inboxPort.tryBegin(inboxCmd);
        switch (begin.getOutcome()) {
            case CLAIMED:
            case RETRIED_CLAIMED:
                return applyClaimed(begin.getRecord(), cmd, tenantId);
            case DUPLICATE_COMPLETED:
                // F6：重放 Inbox 已保存的首次结果（含 todoId/首次状态），而非以当前状态重构丢失原结果
                return replayCompletedResult(cmd, begin.getRecord(), tenantId);
            case DUPLICATE_IN_FLIGHT:
                return notChanged(cmd, NotifyTodoTransitionResult.OUTCOME_DUPLICATE_IN_FLIGHT,
                        "同键处理中（并发抢占）", currentStatus(cmd, tenantId));
            case DUPLICATE_RESULT_UNKNOWN:
                return notChanged(cmd, NotifyTodoTransitionResult.OUTCOME_DUPLICATE_RESULT_UNKNOWN,
                        "先前结果未知，须先回查", currentStatus(cmd, tenantId));
            case PARAM_CONFLICT:
                return notChanged(cmd, NotifyTodoTransitionResult.OUTCOME_PARAM_CONFLICT,
                        "同键不同载荷指纹（参数冲突）", currentStatus(cmd, tenantId));
            case STALE_VERSION:
                return notChanged(cmd, NotifyTodoTransitionResult.OUTCOME_STALE_VERSION,
                        "旧版本事件被版本水位拒绝，不复活已失效待办", currentStatus(cmd, tenantId));
            default:
                throw new IllegalStateException("未知 Inbox Outcome: " + begin.getOutcome());
        }
    }

    /**
     * 抢到处理键后应用流转：定位待办 → 终态守卫 → 定向乐观锁更新 → Inbox complete；
     * 待办不存在则 Inbox fail（FAILED 可重试）并返回未生效。
     */
    private NotifyTodoTransitionResult applyClaimed(InboxRecord record, NotifyTodoTransitionCmd cmd, Long tenantId) {
        long inboxId = record.getInboxId();
        NotifyTodoDO todo = notifyTodoMapper.selectByUniqueKey(tenantId, cmd.getSourceType(), cmd.getTodoKey());
        if (todo == null) {
            // 待办不存在：标记 Inbox FAILED（可经 tryBegin 重领重试，不占终态），返回未生效不抛
            inboxPort.fail(inboxId, exception(NOTIFY_TODO_NOT_FOUND));
            return NotifyTodoTransitionResult.builder()
                    .todoKey(cmd.getTodoKey()).changed(false)
                    .outcome(NotifyTodoTransitionResult.OUTCOME_NOT_FOUND)
                    .reason("业务待办不存在").build();
        }
        NotifyTodoStatus current = NotifyTodoStatus.valueOf(todo.getStatus());
        if (current.isTerminal()) {
            // 应用层终态守卫（非 DB 约束）：终态待办不被后到事件复活
            String reason = "待办已处于终态，后到事件不复活";
            inboxPort.complete(inboxId, resultJson(todo.getId(), cmd.getTodoKey(), current.name(), false,
                    NotifyTodoTransitionResult.OUTCOME_TERMINAL_GUARDED, reason));
            return NotifyTodoTransitionResult.builder()
                    .todoId(todo.getId()).todoKey(cmd.getTodoKey()).status(current).changed(false)
                    .outcome(NotifyTodoTransitionResult.OUTCOME_TERMINAL_GUARDED)
                    .reason(reason).build();
        }
        // F2：待办行自身版本单调护栏——闭合 Inbox 对象水位两处缺口：①业务键（bizType/bizId）缺失时
        // staleVersion 跳过比较；②注册已存 biz_version 但水位未 seed 基线。以「目标待办当前 biz_version」为
        // 稳定比较对象：incoming 与 baseline 均可解析且 incoming < baseline → 拒绝，旧版本事件不覆盖较新待办
        // （assignee/biz_version/todo_version 不回退）。
        Long incomingVersion = parseVersion(cmd.getBizVersion());
        Long baselineVersion = parseVersion(todo.getBizVersion());
        if (incomingVersion != null && baselineVersion != null && incomingVersion < baselineVersion) {
            String reason = "旧版本事件不覆盖较新待办";
            inboxPort.complete(inboxId, resultJson(todo.getId(), cmd.getTodoKey(), current.name(), false,
                    NotifyTodoTransitionResult.OUTCOME_STALE_VERSION, reason));
            return NotifyTodoTransitionResult.builder()
                    .todoId(todo.getId()).todoKey(cmd.getTodoKey()).status(current).changed(false)
                    .outcome(NotifyTodoTransitionResult.OUTCOME_STALE_VERSION)
                    .reason(reason).build();
        }
        NotifyTodoStatus target = cmd.getTransitionType().targetStatus();
        String assigneeType = todo.getAssigneeType();
        Long assigneeId = todo.getAssigneeId();
        if (cmd.getTransitionType() == TodoTransitionType.REASSIGN && cmd.getAssignee() != null
                && cmd.getAssignee().getType() != null) {
            assigneeType = cmd.getAssignee().getType().name();
            assigneeId = cmd.getAssignee().getId();
        }
        int rows = notifyTodoMapper.updateTransition(todo.getId(), tenantId, target.name(), cmd.getReason(),
                assigneeType, assigneeId, cmd.getBizVersion(), todo.getTodoVersion());
        if (rows != 1) {
            // 并发已改变 todo_version：抛异常触发事务回滚（含 Inbox 抢位），可重试
            throw exception(NOTIFY_TODO_WRITE_FAILED);
        }
        inboxPort.complete(inboxId, resultJson(todo.getId(), cmd.getTodoKey(), target.name(), true,
                NotifyTodoTransitionResult.OUTCOME_APPLIED, cmd.getReason()));
        return NotifyTodoTransitionResult.builder()
                .todoId(todo.getId()).todoKey(cmd.getTodoKey()).status(target).changed(true)
                .outcome(NotifyTodoTransitionResult.OUTCOME_APPLIED).reason(cmd.getReason()).build();
    }

    @Override
    public NotifyTodoDO getTodo(Long id) {
        return notifyTodoMapper.selectById(id);
    }

    @Override
    public List<NotifyTodoDO> listByRecipient(NotifyRecipient recipient, NotifyTodoStatus status, int limit) {
        Long tenantId = requireTenant();
        requireRecipient(recipient);
        return notifyTodoMapper.selectByRecipientAndStatus(tenantId, recipient.getType().name(), recipient.getId(),
                status == null ? null : status.name(), limit);
    }

    @Override
    public long getUnfinishedCount(NotifyRecipient recipient) {
        Long tenantId = requireTenant();
        requireRecipient(recipient);
        return notifyTodoMapper.countUnfinished(tenantId, recipient.getType().name(), recipient.getId());
    }

    // ========= 内部工具 =========

    private NotifyTodoStatus currentStatus(NotifyTodoTransitionCmd cmd, Long tenantId) {
        NotifyTodoDO todo = notifyTodoMapper.selectByUniqueKey(tenantId, cmd.getSourceType(), cmd.getTodoKey());
        return todo == null ? null : NotifyTodoStatus.valueOf(todo.getStatus());
    }

    private NotifyTodoTransitionResult notChanged(NotifyTodoTransitionCmd cmd, String outcome, String reason,
                                                  NotifyTodoStatus status) {
        return NotifyTodoTransitionResult.builder()
                .todoKey(cmd.getTodoKey()).status(status).changed(false).outcome(outcome).reason(reason).build();
    }

    private void requireRecipient(NotifyRecipient recipient) {
        if (recipient == null || recipient.getId() == null || recipient.getType() == null) {
            throw exception(NOTIFY_TODO_RECIPIENT_REQUIRED);
        }
    }

    /** 技术租户强制：缺失即拒绝，不默认写 0（循 MSG-001/JOB-002/JOB-003 惯例）。 */
    private Long requireTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(NOTIFY_TODO_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 载荷指纹（SHA-256，同键不同指纹=参数冲突）：覆盖影响业务结果的关键字段。
     * F5：以「长度前缀 len:value;」编码消除分隔符歧义——避免 todoKey="A|B"+sourceType="C" 与
     * todoKey="A"+sourceType="B|C" 产生相同哈希输入而被误判重复（应判 PARAM_CONFLICT）。
     */
    private static String payloadHash(NotifyTodoTransitionCmd cmd) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, cmd.getTransitionType() == null ? null : cmd.getTransitionType().name());
        appendField(sb, cmd.getTodoKey());
        appendField(sb, cmd.getSourceType());
        appendField(sb, cmd.getBizType());
        appendField(sb, cmd.getBizId());
        appendField(sb, cmd.getBizVersion());
        appendField(sb, cmd.getReason());
        appendField(sb, cmd.getAssignee() == null || cmd.getAssignee().getType() == null ? null
                : cmd.getAssignee().getType().name() + ":" + cmd.getAssignee().getId());
        return sha256Hex(sb.toString());
    }

    /**
     * 长度前缀字段编码（消除拼接分隔符歧义）：非 null 编 {@code <charLen>:<value>;}，null 编哨兵 {@code -1:;}。
     * R2：null 与空串写库语义不同（如 bizVersion：null 经 COALESCE 保留既有、空串覆盖既有），故须区分编码，
     * 否则同 eventId 仅 null↔"" 之差会得相同指纹、漏判 PARAM_CONFLICT（-1 不可能是真实字符串长度，无歧义）。
     */
    private static void appendField(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("-1:;");
            return;
        }
        sb.append(value.length()).append(':').append(value).append(';');
    }

    private static boolean isParsableVersion(String version) {
        return parseVersion(version) != null;
    }

    /** 解析整数版本：可解析返回 Long，否则 null（循 JdbcConsumerInboxPort#parseVersion）。 */
    private static Long parseVersion(String version) {
        if (!StringUtils.hasText(version)) {
            return null;
        }
        try {
            return Long.parseLong(version.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * F6：重放 Inbox 首次结果（{@code record.result} 的 resultJson）——还原 todoId/首次状态，
     * changed=false（本次未新增副作用），区分「本次未生效」与「原处理结果」；结果缺失时退回当前状态查询。
     */
    private NotifyTodoTransitionResult replayCompletedResult(NotifyTodoTransitionCmd cmd, InboxRecord record,
                                                             Long tenantId) {
        Long todoId = null;
        NotifyTodoStatus status = null;
        String reason = null;
        String savedResult = record == null ? null : record.getResult();
        if (StringUtils.hasText(savedResult)) {
            Map<String, Object> parsed = JsonUtils.parseMap(savedResult);
            if (parsed != null) {
                todoId = toLong(parsed.get("todoId"));
                String statusStr = parsed.get("status") == null ? null : String.valueOf(parsed.get("status"));
                if (StringUtils.hasText(statusStr)) {
                    try {
                        status = NotifyTodoStatus.valueOf(statusStr);
                    } catch (IllegalArgumentException ignore) {
                        // 落库状态非枚举值：退回当前状态查询
                    }
                }
                // R3：重放首次 reason（resultJson 已保存），避免丢失原始业务/守卫原因
                String reasonStr = parsed.get("reason") == null ? null : String.valueOf(parsed.get("reason"));
                if (StringUtils.hasText(reasonStr)) {
                    reason = reasonStr;
                }
            }
        }
        if (status == null) {
            status = currentStatus(cmd, tenantId);
        }
        if (reason == null) {
            reason = "重复事件，返回首次处理结果不重复副作用";
        }
        return NotifyTodoTransitionResult.builder()
                .todoId(todoId).todoKey(cmd.getTodoKey()).status(status).changed(false)
                .outcome(NotifyTodoTransitionResult.OUTCOME_DUPLICATE_COMPLETED)
                .reason(reason).build();
    }

    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** F1：注册插入前保存点（与 MyBatis insert 同一事务连接——经 DataSourceUtils 绑定）。 */
    private Savepoint createRegisterSavepoint() {
        return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Savepoint>)
                con -> con.setSavepoint());
    }

    private void rollbackRegisterSavepoint(Savepoint savepoint) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) con -> {
            con.rollback(savepoint);
            return null;
        });
    }

    /** R4：释放注册保存点（PG RELEASE SAVEPOINT 销毁之）——成功或撞键回滚恢复后调用，避免保存点资源累积。 */
    private void releaseRegisterSavepoint(Savepoint savepoint) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) con -> {
            con.releaseSavepoint(savepoint);
            return null;
        });
    }

    private static String resultJson(Long todoId, String todoKey, String status, boolean changed, String outcome,
                                     String reason) {
        return JsonUtils.toJsonString(Map.of(
                "todoId", todoId == null ? "" : todoId,
                "todoKey", emptyIfNull(todoKey),
                "status", emptyIfNull(status),
                "changed", changed,
                "outcome", emptyIfNull(outcome),
                "reason", emptyIfNull(reason)));
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

}
