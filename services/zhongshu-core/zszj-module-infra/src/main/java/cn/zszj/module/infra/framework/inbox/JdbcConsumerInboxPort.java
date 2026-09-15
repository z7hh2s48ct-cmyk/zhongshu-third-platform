package cn.zszj.module.infra.framework.inbox;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_COMMAND_FIELD_MISSING;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_TENANT_CONTEXT_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.INBOX_TRANSACTION_REQUIRED;

/**
 * {@link ConsumerInboxPort} 的 JDBC 适配实现（ZS-JOB-003）——业务数据源上与业务副作用同事务的幂等抢位。
 *
 * <p>适配自供体通知 Inbox 模式（供体快照隔离于 {@code reference/donors/}，如 commerce
 * payment_notification_inbox：唯一键抢占 + DuplicateKey 即重复 + 状态推进 + 补偿扫描），落地众墅要求：
 * <ul>
 *   <li><b>处理键唯一约束 DB 硬兜底</b>：(tenant_id, consumer, event_key)——同事件并发、ACK 丢失、重启重放
 *       只产生一次业务副作用；不同租户同业务号互不冲突（键含租户，去除一切默认租户归属）；</li>
 *   <li><b>幂等记录与业务副作用同事务（MANDATORY）</b>：抢位/推进必须加入调用方真实事务，业务回滚则抢位
 *       一并回滚（无代开、无半态残留）；DataSource/TM 构造期同源配对强制（循 ZS-JOB-002 OutboxTransactions
 *       r2 产物同款缺陷预防，包内独立成 {@link InboxTransactions} 保持两机制独立可评审）；</li>
 *   <li><b>参数冲突不当相同成功</b>：同键不同 payload_hash → PARAM_CONFLICT（供体 body_hash 语义推广）；</li>
 *   <li><b>版本乱序护栏</b>：checkVersionStale 时与同对象（consumer+bizType+bizId）已完成记录比较——
 *       旧版本 STALE_VERSION 拒绝；版本为可解析整数才比较（非数值语义由事件类型解释，D-07 登记）；</li>
 *   <li><b>可回查中间态</b>：RESULT_UNKNOWN 状态 + 查询端口（find/listByStatus）；重入结果未知记录返回
 *       DUPLICATE_RESULT_UNKNOWN，须先回查不得盲目重处理；FAILED 重入即重领（retry_count+1）可重试；</li>
 *   <li><b>失败留痕不落原文</b>：fail 只存受控描述（异常类别/长度，循 ZS-JOB-002 last_error 惯例——
 *       自由文本无可靠值级脱敏）；fail-closed：守卫失败毒化本数据源 {@link ConnectionHolder}
 *       （@Transactional 与编程式事务同样生效），调用方吞异常也无法「无幂等记录而提交」。</li>
 * </ul>
 */
@Repository
@Slf4j
public class JdbcConsumerInboxPort implements ConsumerInboxPort {

    /** 受控 reason/描述限长（防撑爆存储，循 JOB-002 last_error 惯例）。 */
    private static final int MAX_ERROR_LENGTH = 512;

    private static final String SELECT_BY_KEY_SQL = "SELECT id, consumer, event_key, payload_hash, status, result, "
            + "retry_count, biz_type, biz_id, biz_version, last_error, tenant_id, trace_id FROM inbox_event "
            + "WHERE tenant_id = ? AND consumer = ? AND event_key = ?";

    /** 同对象已完成版本清单（版本乱序护栏；数量有限——同一消费者对同一对象的已完成事件数，Java 侧解析比较）。 */
    private static final String SELECT_COMPLETED_VERSIONS_SQL = "SELECT biz_version FROM inbox_event "
            + "WHERE tenant_id = ? AND consumer = ? AND biz_type = ? AND biz_id = ? "
            + "AND status = 'COMPLETED' AND biz_version IS NOT NULL";

    private static final String INSERT_SQL = "INSERT INTO inbox_event "
            + "(consumer, event_key, payload_hash, status, biz_type, biz_id, biz_version, tenant_id, trace_id) "
            + "VALUES (?, ?, ?, 'PROCESSING', ?, ?, ?, ?, ?)";

    private static final String RECLAIM_FAILED_SQL = "UPDATE inbox_event "
            + "SET status = 'PROCESSING', last_error = NULL "
            + "WHERE id = ? AND status = 'FAILED'";

    private static final String COMPLETE_SQL = "UPDATE inbox_event "
            + "SET status = 'COMPLETED', result = ?, complete_time = ? WHERE id = ? AND status = 'PROCESSING'";

    private static final String FAIL_SQL = "UPDATE inbox_event "
            + "SET status = 'FAILED', last_error = ?, retry_count = retry_count + 1 "
            + "WHERE id = ? AND status = 'PROCESSING'";

    private static final String UNKNOWN_SQL = "UPDATE inbox_event "
            + "SET status = 'RESULT_UNKNOWN', last_error = ? WHERE id = ? AND status = 'PROCESSING'";

    private static final String LIST_BY_STATUS_SQL = "SELECT id, consumer, event_key, payload_hash, status, result, "
            + "retry_count, biz_type, biz_id, biz_version, last_error, tenant_id, trace_id FROM inbox_event "
            + "WHERE consumer = ? AND status = ? ORDER BY id LIMIT ?";

    private final JdbcTemplate jdbcTemplate;

    /** 本端口绑定的业务数据源——MANDATORY 拒绝路径据其定位需毒化的事务连接。 */
    private final DataSource dataSource;

    /** MANDATORY 事务模板（同源配对校验见 {@link InboxTransactions}）。 */
    private final TransactionTemplate callerTransactionTemplate;

    public JdbcConsumerInboxPort(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;
        this.callerTransactionTemplate = InboxTransactions.mandatoryTemplate(dataSource, transactionManager);
    }

    @Override
    public InboxTryBegin tryBegin(InboxCommand command) {
        try {
            // 守卫（租户/必填）置于事务模板内：失败即标记所参与事务 rollback-only，吞异常也无法无记录提交
            return callerTransactionTemplate.execute(status -> {
                try {
                    Long tenantId = requireTenant();
                    validateRequired(command);
                    return doTryBegin(command, tenantId);
                } catch (RuntimeException ex) {
                    status.setRollbackOnly();
                    throw ex;
                }
            });
        } catch (IllegalTransactionStateException e) {
            // MANDATORY 拒绝：线程没有绑定本数据源的真实事务（完全无事务，或仅普通查询级连接绑定）
            return failClosed(exception(INBOX_TRANSACTION_REQUIRED));
        }
    }

    /** 抢位核心：先查既有（含跨键对象版本护栏），再插入/重领；并发首抢撞唯一键时按并发抢占复判。 */
    private InboxTryBegin doTryBegin(InboxCommand command, Long tenantId) {
        InboxRecord existing = findByKey(tenantId, command.getConsumer(), command.getEventKey());
        if (existing == null) {
            if (staleVersion(command, tenantId)) {
                return new InboxTryBegin(InboxTryBegin.Outcome.STALE_VERSION, null);
            }
            try {
                return insertClaimed(command, tenantId);
            } catch (DuplicateKeyException e) {
                // 并发首抢撞 uk_inbox_event_key：复判为并发抢占现场（DB 硬兜底）
                InboxRecord concurrent = findByKey(tenantId, command.getConsumer(), command.getEventKey());
                if (concurrent == null) {
                    throw e;
                }
                existing = concurrent;
            }
        }
        if (!StringUtils.hasText(existing.getPayloadHash())
                || !existing.getPayloadHash().equals(command.getPayloadHash())) {
            return new InboxTryBegin(InboxTryBegin.Outcome.PARAM_CONFLICT, existing);
        }
        if (staleVersion(command, tenantId)) {
            return new InboxTryBegin(InboxTryBegin.Outcome.STALE_VERSION, existing);
        }
        switch (existing.getStatus()) {
            case "PROCESSING":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_IN_FLIGHT, existing);
            case "COMPLETED":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_COMPLETED, existing);
            case "RESULT_UNKNOWN":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_RESULT_UNKNOWN, existing);
            case "FAILED":
                // 失败重入即重领（retry_count 已由 fail 推进为「已尝试次数」，重领不重复递增；
                // 条件更新防并发抢领竞争：已被他线程领走则按并发抢占复判）
                int reclaimed = jdbcTemplate.update(RECLAIM_FAILED_SQL, existing.getInboxId());
                if (reclaimed == 1) {
                    return new InboxTryBegin(InboxTryBegin.Outcome.RETRIED_CLAIMED,
                            withStatusAndRetry(existing, "PROCESSING", existing.getRetryCount()));
                }
                // 领取竞争失败：此刻记录必然已回 PROCESSING（他线程持有），按并发抢占复判
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_IN_FLIGHT,
                        findByKey(tenantId, command.getConsumer(), command.getEventKey()));
            default:
                throw new IllegalStateException("Inbox 未知状态: " + existing.getStatus());
        }
    }

    /** 版本乱序护栏：同对象已完成记录中存在数值更大的版本即判旧版本（可解析整数才比较，D-07 登记语义边界）。 */
    private boolean staleVersion(InboxCommand command, Long tenantId) {
        if (!command.isCheckVersionStale() || !StringUtils.hasText(command.getBizType())
                || !StringUtils.hasText(command.getBizId())
                || !StringUtils.hasText(command.getBizVersion())) {
            return false;
        }
        Long incoming = parseVersion(command.getBizVersion());
        if (incoming == null) {
            return false;
        }
        List<Long> completed = jdbcTemplate.queryForList(SELECT_COMPLETED_VERSIONS_SQL, String.class,
                tenantId, command.getConsumer(), command.getBizType(), command.getBizId()).stream()
                .map(JdbcConsumerInboxPort::parseVersion)
                .filter(v -> v != null)
                .toList();
        return completed.stream().anyMatch(v -> v > incoming);
    }

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

    private InboxTryBegin insertClaimed(InboxCommand command, Long tenantId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
            ps.setString(1, command.getConsumer());
            ps.setString(2, command.getEventKey());
            ps.setString(3, command.getPayloadHash());
            ps.setString(4, command.getBizType());
            ps.setString(5, command.getBizId());
            ps.setString(6, command.getBizVersion());
            ps.setLong(7, tenantId);
            ps.setString(8, command.getTraceId());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        long inboxId = key != null ? key.longValue() : -1L;
        InboxRecord record = new InboxRecord(inboxId, command.getConsumer(), command.getEventKey(),
                command.getPayloadHash(), "PROCESSING", null, 0, command.getBizType(), command.getBizId(),
                command.getBizVersion(), null, tenantId, command.getTraceId());
        return new InboxTryBegin(InboxTryBegin.Outcome.CLAIMED, record);
    }

    @Override
    public boolean complete(long inboxId, String resultJson) {
        return inCallerTransaction(() ->
                jdbcTemplate.update(COMPLETE_SQL, resultJson, new Timestamp(System.currentTimeMillis()), inboxId) == 1);
    }

    @Override
    public boolean fail(long inboxId, Throwable error) {
        return inCallerTransaction(() ->
                jdbcTemplate.update(FAIL_SQL, describeThrowable(error), inboxId) == 1);
    }

    @Override
    public boolean markResultUnknown(long inboxId, String reason) {
        String controlled = reason == null ? null
                : reason.length() > MAX_ERROR_LENGTH ? reason.substring(0, MAX_ERROR_LENGTH) : reason;
        return inCallerTransaction(() ->
                jdbcTemplate.update(UNKNOWN_SQL, controlled, inboxId) == 1);
    }

    /** 推进操作同样必须处于调用方业务事务内（与抢位同合同，防终态在无事务下静默自提交）。 */
    private boolean inCallerTransaction(java.util.function.Supplier<Boolean> action) {
        try {
            return callerTransactionTemplate.execute(status -> action.get());
        } catch (IllegalTransactionStateException e) {
            return failClosed(exception(INBOX_TRANSACTION_REQUIRED));
        }
    }

    @Override
    public Optional<InboxRecord> find(String consumer, String eventKey) {
        // 回查入口：有租户上下文按租户过滤，无则跨租户回查（运维/系统租户台账场景）
        Long tenantId = TenantContextHolder.getTenantId();
        List<InboxRecord> rows = tenantId != null
                ? jdbcTemplate.query(SELECT_BY_KEY_SQL, this::mapRow, tenantId, consumer, eventKey)
                : jdbcTemplate.query("SELECT id, consumer, event_key, payload_hash, status, result, retry_count, "
                        + "biz_type, biz_id, biz_version, last_error, tenant_id, trace_id FROM inbox_event "
                        + "WHERE consumer = ? AND event_key = ?", this::mapRow, consumer, eventKey);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<InboxRecord> listByStatus(String consumer, String status, int limit) {
        return new ArrayList<>(jdbcTemplate.query(LIST_BY_STATUS_SQL, this::mapRow, consumer, status, limit));
    }

    private InboxRecord findByKey(Long tenantId, String consumer, String eventKey) {
        List<InboxRecord> rows = jdbcTemplate.query(SELECT_BY_KEY_SQL, this::mapRow, tenantId, consumer, eventKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private InboxRecord mapRow(ResultSet rs, int i) throws SQLException {
        return new InboxRecord(rs.getLong("id"), rs.getString("consumer"), rs.getString("event_key"),
                rs.getString("payload_hash"), rs.getString("status"), rs.getString("result"),
                rs.getInt("retry_count"), rs.getString("biz_type"), rs.getString("biz_id"),
                rs.getString("biz_version"), rs.getString("last_error"), rs.getLong("tenant_id"),
                rs.getString("trace_id"));
    }

    private InboxRecord withStatusAndRetry(InboxRecord source, String status, int retryCount) {
        return new InboxRecord(source.getInboxId(), source.getConsumer(), source.getEventKey(),
                source.getPayloadHash(), status, source.getResult(), retryCount, source.getBizType(),
                source.getBizId(), source.getBizVersion(), source.getLastError(), source.getTenantId(),
                source.getTraceId());
    }

    /** fail-closed：毒化本数据源绑定的真实事务连接（@Transactional 与编程式同样生效）后抛出。 */
    private <T> T failClosed(ServiceException ex) {
        Object resource = TransactionSynchronizationManager.getResource(dataSource);
        if (resource instanceof ConnectionHolder holder) {
            holder.setRollbackOnly();
        }
        throw ex;
    }

    /** 必填校验（fail-closed）：consumer/eventKey/payloadHash 缺失即抛，杜绝无键模糊抢位。 */
    private void validateRequired(InboxCommand command) {
        if (command == null) {
            throw exception(INBOX_COMMAND_FIELD_MISSING, "command");
        }
        if (!StringUtils.hasText(command.getConsumer())) {
            throw exception(INBOX_COMMAND_FIELD_MISSING, "consumer");
        }
        if (!StringUtils.hasText(command.getEventKey())) {
            throw exception(INBOX_COMMAND_FIELD_MISSING, "eventKey");
        }
        if (!StringUtils.hasText(command.getPayloadHash())) {
            throw exception(INBOX_COMMAND_FIELD_MISSING, "payloadHash");
        }
    }

    /** 技术租户强制：缺失即拒绝，不默认写 0（循 ZS-AUDIT-001/JOB-002 惯例）。 */
    private Long requireTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(INBOX_TENANT_CONTEXT_REQUIRED);
        }
        return tenantId;
    }

    /** 失败留痕受控描述（循 ZS-JOB-002 describeThrowable 惯例：不落异常原文）。 */
    private String describeThrowable(Throwable e) {
        String message = e == null ? null : e.getMessage();
        try {
            return JsonUtils.toJsonString(Map.of(
                    "errorClass", e == null ? "unknown" : e.getClass().getSimpleName(),
                    "messageLength", message == null ? 0 : message.length()));
        } catch (Exception ex) {
            return "{\"errorClass\":\"(未知)\",\"messageLength\":0}";
        }
    }

}
