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
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.sql.Types;
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
 *       只产生一次业务副作用；不同租户同业务号互不冲突（键含租户，去除一切默认租户归属）；并发首抢撞键经
 *       <b>SAVEPOINT 回滚后复判</b>（codex r0 P1：PG 撞 23505 后事务即中止，不回滚到保存点则后续复判查询
 *       报 25P02）；</li>
 *   <li><b>幂等记录与业务副作用同事务（MANDATORY）</b>：抢位/推进/确认必须加入调用方真实事务，业务回滚则
 *       抢位一并回滚；DataSource/TM 构造期同源配对强制（循 ZS-JOB-002 r2 产物同款缺陷预防，包内独立成
 *       {@link InboxTransactions} 保持两机制独立可评审）；</li>
 *   <li><b>租户全量强制</b>（codex r0 P1）：抢位/推进/确认/回查统一按当前租户过滤（缺失即拒绝），
 *       跨租户运维另设显式接口（登记待办）；</li>
 *   <li><b>参数冲突不当相同成功</b>：同键不同 payload_hash → PARAM_CONFLICT（供体 body_hash 语义推广）；
 *       分类顺序：既有记录先按状态复判（COMPLETED 可重放 / UNKNOWN 须回查 / IN_FLIGHT 并发），
 *       版本护栏仅作用于将执行副作用的首抢/重领路径（codex r0 P2：STALE 不得吞掉可重放结果与回查合同）；</li>
 *   <li><b>版本乱序护栏（对象级串行化）</b>：checkVersionStale 时经独立水位行
 *       {@code inbox_object_watermark}（唯一键 INSERT-or-LOCK，行锁持至业务事务提交，占坑语义含已提交的
 *       失败占位）——新旧版本事件在此互斥，旧版本 STALE_VERSION 拒绝；仅对可解析整数版本比较
 *       （非数值语义由事件类型解释，D-07 登记）；</li>
 *   <li><b>可回查中间态且有确认出口</b>：RESULT_UNKNOWN 经 {@link #resolveAfterVerification}
 *       按回查依据推进（codex r0 P1：无出口则永久悬挂）；FAILED 重入即重领（retry_count 语义=
 *       已记录失败次数，由 fail 递增、重领不重复递增）；失败留痕不落原文（循 ZS-JOB-002 惯例）；
 *       fail-closed：守卫失败毒化本数据源 {@link ConnectionHolder}（@Transactional 与编程式事务同样生效），
 *       调用方吞异常也无法「无幂等记录而提交」。</li>
 * </ul>
 */
@Repository
@Slf4j
public class JdbcConsumerInboxPort implements ConsumerInboxPort {

    /** 受控 reason/描述限长（防撑爆存储，循 JOB-002 last_error 惯例）。 */
    private static final int MAX_ERROR_LENGTH = 512;

    private static final String RECORD_COLUMNS = "id, consumer, event_key, payload_hash, status, result, "
            + "retry_count, biz_type, biz_id, biz_version, last_error, tenant_id, trace_id";

    private static final String SELECT_BY_KEY_SQL = "SELECT " + RECORD_COLUMNS + " FROM inbox_event "
            + "WHERE tenant_id = ? AND consumer = ? AND event_key = ?";

    /** 对象版本水位行（codex r1：稳定串行化点）——唯一键 INSERT-or-LOCK，行锁持至业务事务提交。 */
    private static final String WATERMARK_INSERT_SQL = "INSERT INTO inbox_object_watermark "
            + "(tenant_id, consumer, biz_type, biz_id, version_watermark) VALUES (?, ?, ?, ?, 0)";

    private static final String WATERMARK_SELECT_FOR_UPDATE_SQL = "SELECT version_watermark "
            + "FROM inbox_object_watermark WHERE tenant_id = ? AND consumer = ? AND biz_type = ? AND biz_id = ? "
            + "FOR UPDATE";

    private static final String WATERMARK_RAISE_SQL = "UPDATE inbox_object_watermark "
            + "SET version_watermark = ?, update_time = ? "
            + "WHERE tenant_id = ? AND consumer = ? AND biz_type = ? AND biz_id = ?";

    private static final String INSERT_SQL = "INSERT INTO inbox_event "
            + "(consumer, event_key, payload_hash, status, biz_type, biz_id, biz_version, tenant_id, trace_id) "
            + "VALUES (?, ?, ?, 'PROCESSING', ?, ?, ?, ?, ?)";

    /** 处理记录行锁定重读（codex r3：FAILED 分支不得依据过期快照判定——先锁行取最新状态再过护栏）。 */
    private static final String SELECT_BY_ID_FOR_UPDATE_SQL = "SELECT " + RECORD_COLUMNS + " FROM inbox_event "
            + "WHERE id = ? AND tenant_id = ? FOR UPDATE";

    private static final String RECLAIM_FAILED_SQL = "UPDATE inbox_event "
            + "SET status = 'PROCESSING', last_error = NULL "
            + "WHERE id = ? AND tenant_id = ? AND status = 'FAILED'";

    private static final String COMPLETE_SQL = "UPDATE inbox_event "
            + "SET status = 'COMPLETED', result = ?, complete_time = ? "
            + "WHERE id = ? AND tenant_id = ? AND status = 'PROCESSING'";

    private static final String FAIL_SQL = "UPDATE inbox_event "
            + "SET status = 'FAILED', last_error = ?, retry_count = retry_count + 1 "
            + "WHERE id = ? AND tenant_id = ? AND status = 'PROCESSING'";

    private static final String UNKNOWN_SQL = "UPDATE inbox_event "
            + "SET status = 'RESULT_UNKNOWN', last_error = ? "
            + "WHERE id = ? AND tenant_id = ? AND status = 'PROCESSING'";

    private static final String RESOLVE_COMPLETED_SQL = "UPDATE inbox_event "
            + "SET status = 'COMPLETED', result = ?, last_error = ?, complete_time = ? "
            + "WHERE id = ? AND tenant_id = ? AND status = 'RESULT_UNKNOWN'";

    private static final String RESOLVE_FAILED_SQL = "UPDATE inbox_event "
            + "SET status = 'FAILED', last_error = ?, retry_count = retry_count + 1 "
            + "WHERE id = ? AND tenant_id = ? AND status = 'RESULT_UNKNOWN'";

    private static final String LIST_BY_STATUS_SQL = "SELECT " + RECORD_COLUMNS + " FROM inbox_event "
            + "WHERE tenant_id = ? AND consumer = ? AND status = ? ORDER BY id LIMIT ?";

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

    /**
     * 抢位核心：既有记录先按状态复判（可重放/回查/并发），版本护栏仅作用于将执行副作用的首抢/重领；
     * 并发首抢撞唯一键经 SAVEPOINT 回滚后复判（PG 撞 23505 后事务中止，不回滚保存点则后续查询 25P02）。
     */
    private InboxTryBegin doTryBegin(InboxCommand command, Long tenantId) {
        InboxRecord existing = findByKey(tenantId, command.getConsumer(), command.getEventKey());
        if (existing == null) {
            return claimNew(command, tenantId);
        }
        return handleExisting(command, tenantId, existing);
    }

    /** 既有记录完整判定：指纹检查 → 状态复判（可重放/回查/并发）；FAILED 重领前锁行重读过版本护栏。 */
    private InboxTryBegin handleExisting(InboxCommand command, Long tenantId, InboxRecord existing) {
        if (!StringUtils.hasText(existing.getPayloadHash())
                || !existing.getPayloadHash().equals(command.getPayloadHash())) {
            return new InboxTryBegin(InboxTryBegin.Outcome.PARAM_CONFLICT, existing);
        }
        switch (existing.getStatus()) {
            case "PROCESSING":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_IN_FLIGHT, existing);
            case "COMPLETED":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_COMPLETED, existing);
            case "RESULT_UNKNOWN":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_RESULT_UNKNOWN, existing);
            case "FAILED":
                // 失败重入即重领。先锁定处理记录行并重读当前状态（codex r3 发现1：过期 FAILED 快照不得直接
                // 判定——并发他事务可能已推进为 COMPLETED/RESULT_UNKNOWN，锁序统一为「记录行 → 水位行」）；
                // 锁重读后的正常状态直接分类、不消耗任何递归预算（codex r4 F1：depth 预算只属于竞争循环，
                // 分类本身有限收敛）；retry_count 为已记录失败次数（fail 递增、重领不递增）
                List<InboxRecord> locked = jdbcTemplate.query(SELECT_BY_ID_FOR_UPDATE_SQL, this::mapRow,
                        existing.getInboxId(), tenantId);
                InboxRecord current = locked.isEmpty() ? existing : locked.get(0);
                if (!"FAILED".equals(current.getStatus())) {
                    return classify(current);
                }
                if (staleVersion(command, tenantId)) {
                    return new InboxTryBegin(InboxTryBegin.Outcome.STALE_VERSION, current);
                }
                // 持有记录行锁后重领条件更新必然命中；一旦未命中（锁语义被破坏）如实报错
                int reclaimed = jdbcTemplate.update(RECLAIM_FAILED_SQL, current.getInboxId(), tenantId);
                if (reclaimed != 1) {
                    throw new IllegalStateException("Inbox 持行锁重领失败: " + current.getInboxId());
                }
                // 重读返回（codex r3 发现2：retry_count/last_error 与持久化状态一致，不用过期快照构造）
                return new InboxTryBegin(InboxTryBegin.Outcome.RETRIED_CLAIMED,
                        findByKey(tenantId, current.getConsumer(), current.getEventKey()));
            default:
                throw new IllegalStateException("Inbox 未知状态: " + existing.getStatus());
        }
    }

    /**
     * 新登记（codex r2 顺序修正）：同一保存点内「先占处理键、后过版本护栏」——护栏拒绝/并发撞键一律
     * 回滚保存点，<b>占位行与水位抬升一并撤销</b>（被拒请求不得抬高水位，否则合法后续版本被误拒）；
     * 撞键后复判走完整判定入口（指纹检查优先——同键不同指纹并发首抢同样 PARAM_CONFLICT）。
     */
    private InboxTryBegin claimNew(InboxCommand command, Long tenantId) {
        Savepoint savepoint = createClaimSavepoint();
        try {
            InboxTryBegin claimed = insertClaimed(command, tenantId);
            if (staleVersion(command, tenantId)) {
                rollbackClaimSavepoint(savepoint);
                return new InboxTryBegin(InboxTryBegin.Outcome.STALE_VERSION, null);
            }
            return claimed;
        } catch (DuplicateKeyException e) {
            // 并发首抢撞 uk_inbox_event_key：PG 事务已因 23505 中止，须回滚到插入前保存点才能继续复判查询
            rollbackClaimSavepoint(savepoint);
            InboxRecord concurrent = findByKey(tenantId, command.getConsumer(), command.getEventKey());
            if (concurrent == null) {
                throw e;
            }
            return handleExisting(command, tenantId, concurrent);
        }
    }

    /** 既有记录按实际状态统一分类（FAILED 重领竞争后复判共用，codex r0 P2：不固定报并发抢占）。 */
    private InboxTryBegin classify(InboxRecord record) {
        if (record == null) {
            throw new IllegalStateException("Inbox 并发复判时记录消失");
        }
        switch (record.getStatus()) {
            case "PROCESSING":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_IN_FLIGHT, record);
            case "COMPLETED":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_COMPLETED, record);
            case "RESULT_UNKNOWN":
                return new InboxTryBegin(InboxTryBegin.Outcome.DUPLICATE_RESULT_UNKNOWN, record);
            default:
                throw new IllegalStateException("Inbox 未知状态: " + record.getStatus());
        }
    }

    /** 抢位保存点（与 INSERT 同一事务连接——经 DataSourceUtils 绑定；撞键后回滚到它恢复事务可用性）。 */
    private Savepoint createClaimSavepoint() {
        return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Savepoint>)
                con -> con.setSavepoint());
    }

    private void rollbackClaimSavepoint(Savepoint savepoint) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) con -> {
            con.rollback(savepoint);
            return null;
        });
    }

    /**
     * 版本乱序护栏（codex r1：稳定水位行串行化）——以 (tenant, consumer, biz_type, biz_id) 唯一水位行为
     * 对象级互斥点：INSERT-or-撞键（撞键经保存点回滚）后 SELECT ... FOR UPDATE 持有该行直至业务事务提交，
     * 同一行等待者持锁后读到最新已提交水位（同行等待无快照旧值问题），首次处理亦有稳定互斥点；
     * incoming &lt; 水位 → STALE；通过则同事务抬水位（水位与副作用同生共死，业务回滚即回落，不虚高）。
     * 版本为可解析整数才比较；非数值语义由事件类型解释（D-07 登记）。
     */
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
        Savepoint savepoint = createClaimSavepoint();
        try {
            jdbcTemplate.update(WATERMARK_INSERT_SQL, tenantId, command.getConsumer(),
                    command.getBizType(), command.getBizId());
        } catch (DuplicateKeyException e) {
            // 水位行已存在：回滚保存点恢复事务可用性（PG 撞 23505 后事务中止），转入持锁读取
            rollbackClaimSavepoint(savepoint);
        }
        Long watermark = jdbcTemplate.queryForObject(WATERMARK_SELECT_FOR_UPDATE_SQL, Long.class,
                tenantId, command.getConsumer(), command.getBizType(), command.getBizId());
        if (watermark == null) {
            // 撞键的竞争者已整体回滚（水位行随之消失）：重建水位行并锁定（有界一次）
            jdbcTemplate.update(WATERMARK_INSERT_SQL, tenantId, command.getConsumer(),
                    command.getBizType(), command.getBizId());
            watermark = jdbcTemplate.queryForObject(WATERMARK_SELECT_FOR_UPDATE_SQL, Long.class,
                    tenantId, command.getConsumer(), command.getBizType(), command.getBizId());
        }
        boolean stale = watermark != null && incoming < watermark;
        if (!stale) {
            // 通过护栏：同事务抬水位（持行锁中），副作用提交则水位生效、回滚则一并回落
            jdbcTemplate.update(WATERMARK_RAISE_SQL, incoming, new Timestamp(System.currentTimeMillis()),
                    tenantId, command.getConsumer(), command.getBizType(), command.getBizId());
        }
        return stale;
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
        return inCallerTransaction(tenantId ->
                jdbcTemplate.update(COMPLETE_SQL, resultJson, new Timestamp(System.currentTimeMillis()),
                        inboxId, tenantId) == 1);
    }

    @Override
    public boolean fail(long inboxId, Throwable error) {
        return inCallerTransaction(tenantId ->
                jdbcTemplate.update(FAIL_SQL, describeThrowable(error), inboxId, tenantId) == 1);
    }

    @Override
    public boolean markResultUnknown(long inboxId, String reason) {
        String controlled = cap(reason);
        return inCallerTransaction(tenantId ->
                jdbcTemplate.update(UNKNOWN_SQL, controlled, inboxId, tenantId) == 1);
    }

    @Override
    public boolean resolveAfterVerification(long inboxId, boolean executed, String resultJson, String evidence) {
        String controlledEvidence = cap(evidence);
        return inCallerTransaction(tenantId -> executed
                ? jdbcTemplate.update(RESOLVE_COMPLETED_SQL, resultJson, controlledEvidence,
                        new Timestamp(System.currentTimeMillis()), inboxId, tenantId) == 1
                : jdbcTemplate.update(RESOLVE_FAILED_SQL, controlledEvidence, inboxId, tenantId) == 1);
    }

    /** 推进/确认操作同样必须处于调用方业务事务内且按当前租户过滤（与抢位同合同）。 */
    private boolean inCallerTransaction(java.util.function.Function<Long, Boolean> action) {
        try {
            return callerTransactionTemplate.execute(status -> action.apply(requireTenant()));
        } catch (IllegalTransactionStateException e) {
            return failClosed(exception(INBOX_TRANSACTION_REQUIRED));
        }
    }

    @Override
    public Optional<InboxRecord> find(String consumer, String eventKey) {
        Long tenantId = requireTenantForQuery();
        List<InboxRecord> rows = jdbcTemplate.query(SELECT_BY_KEY_SQL, this::mapRow, tenantId, consumer, eventKey);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<InboxRecord> listByStatus(String consumer, String status, int limit) {
        Long tenantId = requireTenantForQuery();
        return List.copyOf(jdbcTemplate.query(LIST_BY_STATUS_SQL, this::mapRow, tenantId, consumer, status, limit));
    }

    /** 回查同样强制租户上下文（codex r0 P1：跨租户运维另设显式接口，登记待办）。 */
    private Long requireTenantForQuery() {
        return requireTenant();
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

    private static String cap(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
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
