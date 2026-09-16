package cn.zszj.module.infra.framework.outbox.recovery;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_NOT_FOUND;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_NOT_DEAD;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_OPERATOR_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_REASON_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_RETRY_LIMIT_EXCEEDED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_TENANT_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_RECOVERY_WRITE_FAILED;

/**
 * {@link OutboxRecoveryService} 实现（ZS-JOB-004）：JdbcTemplate + {@link AuditPort} 双轨留痕，
 * 循 {@code ConfigChangeRecorder}（ZS-CFG-004）恢复留痕范式；租户 fail-closed、payload 只读。
 *
 * <p><b>铁律（开发计划 §1.2-9）</b>：所有恢复 UPDATE 仅改 {@code status/next_retry_at/claim_*}，
 * <b>永不</b> {@code SET payload/headers/event_type}——历史事实不可掩盖（验收「修改历史被拒」+「不编辑 payload」）。
 *
 * <p><b>护栏校验顺序</b>（fail-closed）：租户 → operator（无权即 {@code ACCESS_DENIED} 独立留痕 + 拒绝）→ reason →
 * 载入事件（租户 scoped，跨租户一律 {@code NOT_FOUND} 杜绝探测）→ 状态守卫（仅 DEAD）→ 无限重试护栏 →
 * 乐观并发 UPDATE（命中 0 行=并发落空 no-op，不抛）→ 台账 INSERT → {@code AuditPort} SUCCESS（随事务，失败即回滚）。
 *
 * <p><b>脱敏</b>：回查 payload 经 {@link LogSanitizeUtils#sanitizeJson}（ZS-SEC-007，敏感键掩码 + 解析失败不回退原文）；
 * {@code last_error} 只呈现受控摘要（{@code errorCategory/errorClass/messageLength}），绝不落异常原文/堆栈。
 */
@Service
@Slf4j
public class OutboxRecoveryServiceImpl implements OutboxRecoveryService {

    private static final String BIZ_TYPE = "outbox_event";

    /**
     * 单段合法 Java 标识符形态（{@code last_error} JSON 摘要 {@code errorClass} 逐段验证的基础）：字母/下划线开头、
     * 由字母数字下划线构成，<b>不含点号</b>（点号作为限定名分段符由 {@link #isQualifiedJavaName} 逐段切分校验）；
     * 含空格、{@code =}、{@code -}、连续点（{@code a..b}）、数字开头段（{@code a.1}）等一律非法（ZS-JOB-004 codex P2）。
     */
    private static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    /**
     * 受控异常类型名后缀白名单：{@code errorClass} 的 simpleName 须以其一结尾方视为来自受控异常类型
     * （派发器 {@code describeThrowable} 恒写入 {@code getClass().getSimpleName()}，符合 {@code Exception/Error/Throwable}
     * 命名约定）；否则即便字符形态合法也降级为 {@code UNPARSEABLE_ERROR}（ZS-JOB-004 codex P2）。
     */
    private static final Set<String> THROWABLE_NAME_SUFFIXES = Set.of("Exception", "Error", "Throwable");

    /**
     * 受控内部常量码白名单（裸 {@code last_error} 直显的唯一门槛）：仅当裸值精确等于集合内常量
     * （派发器 {@code dispatchOnce} 写入的 {@code NO_SINK_SUPPORTS_EVENT_TYPE}）方可直显；
     * 其余任意裸文本（含全大写敏感串如 {@code PASSWORD_REAL_SECRET_123}）一律降级为
     * {@code UNPARSEABLE_ERROR}（ZS-JOB-004 codex P2）。
     */
    private static final Set<String> KNOWN_ERROR_CONSTANTS = Set.of("NO_SINK_SUPPORTS_EVENT_TYPE");

    /** 载入事件（租户 scoped）：只读恢复所需字段。 */
    private static final String LOAD_EVENT_SQL = "SELECT id, event_type, biz_type, biz_id, status, retry_count, "
            + "payload, last_error, tenant_id, create_time FROM outbox_event WHERE id = ? AND tenant_id = ?";

    /**
     * 载入事件并加行锁（租户 scoped，{@code FOR UPDATE}）：<b>人工恢复专用</b>——取得行锁后再检查状态、读取重试次数、
     * 写台账与审计，持锁至事务提交，杜绝并发「状态往返」交错下过期计数导致的序号碰撞与上限突破（ZS-JOB-004 codex P1）。
     * 只读回查路径（{@link #getRecoveryDetail}）仍用无锁 {@link #LOAD_EVENT_SQL}，避免读放大与无谓锁竞争。
     */
    private static final String LOAD_EVENT_FOR_UPDATE_SQL = LOAD_EVENT_SQL + " FOR UPDATE";

    /** 重试：DEAD→PENDING，重置退避（next_retry_at=now）+ 清租约（乐观并发：WHERE status='DEAD' AND tenant_id=?）。 */
    private static final String RETRY_UPDATE_SQL = "UPDATE outbox_event SET status = 'PENDING', next_retry_at = ?, "
            + "claimed_by = NULL, claim_token = NULL, claim_expires_at = NULL "
            + "WHERE id = ? AND status = 'DEAD' AND tenant_id = ?";

    /** 跳过：DEAD→SKIPPED（人工放弃终态；乐观并发同守卫）。 */
    private static final String SKIP_UPDATE_SQL = "UPDATE outbox_event SET status = 'SKIPPED' "
            + "WHERE id = ? AND status = 'DEAD' AND tenant_id = ?";

    /** 本事件累计人工重试次数（无限重试护栏依据）。 */
    private static final String COUNT_MANUAL_RETRY_SQL = "SELECT COUNT(*) FROM outbox_recovery_log "
            + "WHERE event_id = ? AND action = 'RETRY' AND tenant_id = ?";

    /** 恢复台账 INSERT（不可变事实，只增不改；operator_type NOT NULL 由服务层兜底 SYSTEM）。 */
    private static final String INSERT_RECOVERY_LOG_SQL = "INSERT INTO outbox_recovery_log (event_id, action, "
            + "reason, before_status, after_status, before_retry_count, manual_retry_seq, operator_type, "
            + "operator_id, tenant_id, trace_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 恢复台账历史（回查前后关联，租户 scoped）。 */
    private static final String RECOVERY_HISTORY_SQL = "SELECT id, event_id, action, reason, before_status, "
            + "after_status, before_retry_count, manual_retry_seq, operator_type, operator_id, tenant_id, "
            + "trace_id, create_time FROM outbox_recovery_log WHERE event_id = ? AND tenant_id = ? ORDER BY id";

    /** DEAD 分页计数（租户 scoped）。 */
    private static final String PAGE_DEAD_COUNT_SQL = "SELECT COUNT(*) FROM outbox_event "
            + "WHERE status = 'DEAD' AND tenant_id = ?";

    /** DEAD 分页查询（租户 scoped，稳定 id 排序；LIMIT/OFFSET 双方言兼容）。 */
    private static final String PAGE_DEAD_SQL = "SELECT id, event_type, biz_type, biz_id, status, retry_count, "
            + "payload, last_error, tenant_id, create_time FROM outbox_event WHERE status = 'DEAD' AND tenant_id = ? "
            + "ORDER BY id LIMIT ? OFFSET ?";

    /** 事件行映射（RowMapper 按列标签取值，规避 H2 列名大小写；循 {@code OutboxDispatcherService#mapCandidate}）。 */
    private static final RowMapper<EventRow> EVENT_ROW_MAPPER = (rs, n) -> {
        EventRow row = new EventRow();
        row.id = rs.getLong("id");
        row.eventType = rs.getString("event_type");
        row.bizType = rs.getString("biz_type");
        row.bizId = rs.getString("biz_id");
        row.status = rs.getString("status");
        row.retryCount = rs.getInt("retry_count");
        row.payload = rs.getString("payload");
        row.lastError = rs.getString("last_error");
        row.tenantId = rs.getLong("tenant_id");
        Timestamp createTime = rs.getTimestamp("create_time");
        row.createTime = createTime == null ? null : createTime.toLocalDateTime();
        return row;
    };

    private final JdbcTemplate jdbcTemplate;

    private final AuditPort auditPort;

    private final OutboxHealthProperties properties;

    public OutboxRecoveryServiceImpl(DataSource dataSource, AuditPort auditPort, OutboxHealthProperties properties) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.auditPort = auditPort;
        this.properties = properties;
    }

    @Override
    public PageResult<OutboxEventRecoveryDetail> pageDeadEvents(int pageNo, int pageSize) {
        Long tenantId = requireTenantId();
        int no = Math.max(pageNo, 1);
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        Long total = jdbcTemplate.queryForObject(PAGE_DEAD_COUNT_SQL, Long.class, tenantId);
        if (total == null || total == 0L) {
            return PageResult.empty(0L);
        }
        List<OutboxEventRecoveryDetail> list = jdbcTemplate
                .query(PAGE_DEAD_SQL, EVENT_ROW_MAPPER, tenantId, size, (no - 1) * size)
                .stream().map(event -> toDetail(event, tenantId)).collect(Collectors.toList());
        return new PageResult<>(list, total);
    }

    @Override
    public OutboxEventRecoveryDetail getRecoveryDetail(long eventId) {
        Long tenantId = requireTenantId();
        EventRow event = loadEvent(eventId, tenantId);
        if (event == null) {
            throw exception(OUTBOX_EVENT_NOT_FOUND);
        }
        return toDetail(event, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboxRecoveryResult retryDeadEvent(OutboxRecoveryCmd cmd) {
        return recover(cmd, OutboxRecoveryAction.RETRY);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboxRecoveryResult skipDeadEvent(OutboxRecoveryCmd cmd) {
        return recover(cmd, OutboxRecoveryAction.SKIP);
    }

    /**
     * 恢复主流程（retry/skip 共用；护栏顺序见类注释）。非法恢复一律抛 {@code ServiceException}；
     * 唯一「未生效不抛」是乐观并发落空（UPDATE 命中 0 行，{@code changed=false}/{@code outcome=CONCURRENT}）。
     */
    private OutboxRecoveryResult recover(OutboxRecoveryCmd cmd, OutboxRecoveryAction action) {
        // 1. 租户 fail-closed（缺失即拒，不默认 0）
        Long tenantId = requireTenantId();
        // 2. operator 必填——无权重放纵深防御（controller @PreAuthorize('infra:job:recover') 为第一道闸）
        if (StrUtil.isBlank(cmd.getOperatorId())) {
            recordDenied(cmd, action, tenantId);
            throw exception(OUTBOX_RECOVERY_OPERATOR_REQUIRED);
        }
        // 3. reason 必填（拒绝无据操作）
        if (StrUtil.isBlank(cmd.getReason())) {
            throw exception(OUTBOX_RECOVERY_REASON_REQUIRED);
        }
        // 4. 载入事件并加行锁（租户 scoped，FOR UPDATE；不存在/跨租户一律 NOT_FOUND，杜绝跨租户探测）
        //    持锁至本事务提交：状态守卫、重试计数、台账与审计在锁内串行化，杜绝并发状态往返下的序号碰撞/上限突破
        EventRow event = loadEventForUpdate(cmd.getEventId(), tenantId);
        if (event == null) {
            throw exception(OUTBOX_EVENT_NOT_FOUND);
        }
        // 5. 状态守卫：仅 DEAD 可人工恢复（携带当前状态供运维定位）
        if (!"DEAD".equals(event.status)) {
            throw exception(OUTBOX_RECOVERY_NOT_DEAD, event.status);
        }
        // 6. 无限重试护栏：RETRY 递增序号并校验上限；SKIP 沿用当前序号（放弃为逃生通道，不设限）
        int priorRetries = countManualRetry(cmd.getEventId(), tenantId);
        int manualRetrySeq = action == OutboxRecoveryAction.RETRY ? priorRetries + 1 : Math.max(priorRetries, 1);
        if (action == OutboxRecoveryAction.RETRY && manualRetrySeq > properties.getMaxManualRetry()) {
            throw exception(OUTBOX_RECOVERY_RETRY_LIMIT_EXCEEDED, properties.getMaxManualRetry());
        }
        // 7. 乐观并发 UPDATE（payload/headers/event_type 绝不出现在 SET，铁律 9）
        String afterStatus = action == OutboxRecoveryAction.RETRY ? "PENDING" : "SKIPPED";
        int updated = action == OutboxRecoveryAction.RETRY
                ? jdbcTemplate.update(RETRY_UPDATE_SQL, new Timestamp(System.currentTimeMillis()),
                        cmd.getEventId(), tenantId)
                : jdbcTemplate.update(SKIP_UPDATE_SQL, cmd.getEventId(), tenantId);
        if (updated == 0) {
            log.warn("[recover][事件 {} 恢复落空（载入后被他实例并发处置），action={}]", cmd.getEventId(), action);
            return OutboxRecoveryResult.builder().eventId(cmd.getEventId()).action(action)
                    .beforeStatus(event.status).afterStatus(event.status).manualRetrySeq(manualRetrySeq)
                    .changed(false).outcome("CONCURRENT").build();
        }
        // 8. 写恢复台账（前后关联；operator_type NOT NULL 兜底 SYSTEM）
        int logRows = jdbcTemplate.update(INSERT_RECOVERY_LOG_SQL, cmd.getEventId(), action.name(),
                cmd.getReason(), event.status, afterStatus, event.retryCount, manualRetrySeq,
                StrUtil.blankToDefault(cmd.getOperatorType(), AuditEventMessage.ActorType.SYSTEM.name()),
                cmd.getOperatorId(), tenantId, cmd.getTraceId());
        if (logRows != 1) {
            throw exception(OUTBOX_RECOVERY_WRITE_FAILED);
        }
        // 9. 审计 SUCCESS（随业务事务；失败即抛出触发回滚，恢复不得无审计——fail-closed）
        recordSuccess(cmd, action, event, afterStatus, manualRetrySeq, tenantId);
        // 10. 结果（保留前后关联 + 序号）
        return OutboxRecoveryResult.builder().eventId(cmd.getEventId()).action(action)
                .beforeStatus(event.status).afterStatus(afterStatus).manualRetrySeq(manualRetrySeq)
                .changed(true).outcome(action == OutboxRecoveryAction.RETRY ? "RETRIED" : "SKIPPED").build();
    }

    /** SUCCESS 审计（随事务）：detail 只装脱敏摘要（前后状态/序号/errorCategory），不含异常原文与 payload。 */
    private void recordSuccess(OutboxRecoveryCmd cmd, OutboxRecoveryAction action, EventRow event,
                               String afterStatus, int manualRetrySeq, Long tenantId) {
        String eventType = action == OutboxRecoveryAction.RETRY
                ? AuditEventTypes.OUTBOX_EVENT_RETRIED : AuditEventTypes.OUTBOX_EVENT_SKIPPED;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("beforeStatus", event.status);
        detail.put("afterStatus", afterStatus);
        detail.put("beforeRetryCount", event.retryCount);
        detail.put("manualRetrySeq", manualRetrySeq);
        detail.put("errorCategory", parseErrorSummary(event.lastError).errorCategory);
        auditPort.record(AuditEventMessage.builder()
                .eventType(eventType)
                .actorType(mapActorType(cmd.getOperatorType()))
                .actorId(cmd.getOperatorId())
                .action(action.name())
                .bizType(BIZ_TYPE)
                .bizId(String.valueOf(cmd.getEventId()))
                .reason(cmd.getReason())
                .result(AuditEventMessage.AuditResult.SUCCESS)
                .detail(detail)
                .tenantId(tenantId)
                .traceId(cmd.getTraceId())
                .idempotencyKey(cmd.getEventId() + ":" + action.name() + ":" + manualRetrySeq)
                .build());
    }

    /** DENIED 审计（独立事务由 JdbcAuditPort 内部 REQUIRES_NEW 承担）：吞审计自身失败并告警，业务拒绝原样抛出。 */
    private void recordDenied(OutboxRecoveryCmd cmd, OutboxRecoveryAction action, Long tenantId) {
        try {
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.ACCESS_DENIED)
                    .actorType(mapActorType(cmd.getOperatorType()))
                    .actorId(cmd.getOperatorId())
                    .action(action.name())
                    .bizType(BIZ_TYPE)
                    .bizId(String.valueOf(cmd.getEventId()))
                    .reason("人工恢复缺少操作者上下文，拒绝匿名重放")
                    .result(AuditEventMessage.AuditResult.DENIED)
                    .tenantId(tenantId)
                    .traceId(cmd.getTraceId())
                    .build());
        } catch (Exception e) {
            log.warn("[recordDenied][恢复拒绝审计留痕失败，业务拒绝仍原样返回 eventId={}]", cmd.getEventId(), e);
        }
    }

    private AuditEventMessage.ActorType mapActorType(String operatorType) {
        if (StrUtil.isBlank(operatorType)) {
            return AuditEventMessage.ActorType.SYSTEM;
        }
        try {
            return AuditEventMessage.ActorType.valueOf(operatorType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AuditEventMessage.ActorType.SYSTEM;
        }
    }

    /** 回查详情装配：payload 掩码 + last_error 受控摘要 + 恢复台账历史（前后关联）。 */
    private OutboxEventRecoveryDetail toDetail(EventRow event, Long tenantId) {
        ErrorSummary summary = parseErrorSummary(event.lastError);
        return OutboxEventRecoveryDetail.builder()
                .eventId(event.id)
                .eventType(event.eventType)
                .bizType(event.bizType)
                .bizId(event.bizId)
                .status(event.status)
                .retryCount(event.retryCount)
                .manualRetrySeq(countManualRetry(event.id, tenantId))
                .errorCategory(summary.errorCategory)
                .errorClass(summary.errorClass)
                .messageLength(summary.messageLength)
                .maskedPayload(LogSanitizeUtils.sanitizeJson(event.payload))
                .recoveryHistory(loadHistory(event.id, tenantId))
                .tenantId(event.tenantId)
                .createTime(event.createTime)
                .build();
    }

    private List<OutboxRecoveryLogRecord> loadHistory(long eventId, Long tenantId) {
        return jdbcTemplate.query(RECOVERY_HISTORY_SQL, (rs, n) -> {
            Timestamp createTime = rs.getTimestamp("create_time");
            return OutboxRecoveryLogRecord.builder()
                    .id(rs.getLong("id"))
                    .eventId(rs.getLong("event_id"))
                    .action(rs.getString("action"))
                    .reason(rs.getString("reason"))
                    .beforeStatus(rs.getString("before_status"))
                    .afterStatus(rs.getString("after_status"))
                    .beforeRetryCount(rs.getInt("before_retry_count"))
                    .manualRetrySeq(rs.getInt("manual_retry_seq"))
                    .operatorType(rs.getString("operator_type"))
                    .operatorId(rs.getString("operator_id"))
                    .tenantId(rs.getLong("tenant_id"))
                    .traceId(rs.getString("trace_id"))
                    .createTime(createTime == null ? null : createTime.toLocalDateTime())
                    .build();
        }, eventId, tenantId);
    }

    /**
     * 解析 {@code last_error} 为受控摘要（不落原文）：JSON（{@code describeThrowable} 产物）取 errorClass/messageLength，
     * errorClass 须先为 {@code String} 再经 {@link #isControlledExceptionName}（类名形态 + {@code Exception/Error/Throwable}
     * 后缀）双重校验方可直显，否则整体降级；裸值须精确命中 {@link #KNOWN_ERROR_CONSTANTS} 常量白名单方可直显，否则降级为
     * {@code UNPARSEABLE_ERROR}；解析失败同样降级——任何路径都绝不回显 {@code last_error} 原文（ZS-JOB-004 codex P2）。
     */
    private ErrorSummary parseErrorSummary(String lastError) {
        ErrorSummary summary = new ErrorSummary();
        if (StrUtil.isBlank(lastError)) {
            return summary;
        }
        String trimmed = lastError.trim();
        if (trimmed.startsWith("{")) {
            try {
                Map<String, Object> parsed = JsonUtils.parseMap(trimmed);
                if (parsed != null) {
                    Object errorClass = parsed.get("errorClass");
                    Object messageLength = parsed.get("messageLength");
                    // messageLength 为受控数值，先解析保留（即便类名非法降级，长度仍可回显）
                    summary.messageLength = messageLength instanceof Number ? ((Number) messageLength).intValue() : 0;
                    // 受控异常类型校验：先要求 errorClass 为 String，再验类名形态 + Throwable 后缀，
                    // 否则整体降级（绝不把 errorClass 内的敏感明文当类名回显）
                    if (isControlledExceptionName(errorClass)) {
                        summary.errorClass = (String) errorClass;
                        summary.errorCategory = (String) errorClass;
                    } else {
                        summary.errorClass = null;
                        summary.errorCategory = "UNPARSEABLE_ERROR";
                    }
                    return summary;
                }
            } catch (Exception e) {
                log.warn("[parseErrorSummary][last_error 解析失败，降级为受控类别，绝不回显原文]");
            }
            summary.errorCategory = "UNPARSEABLE_ERROR";
            return summary;
        }
        // 裸值：仅精确命中受控内部常量白名单方可直显；其余一律降级，杜绝敏感明文回显
        summary.errorCategory = KNOWN_ERROR_CONSTANTS.contains(trimmed) ? trimmed : "UNPARSEABLE_ERROR";
        return summary;
    }

    /**
     * 受控异常类型名判定（{@code errorClass} 直显门槛）：须为 {@code String}、为合法 Java 限定名
     * （{@link #isQualifiedJavaName} 逐段验证，拦截连续点/数字开头段等畸形名），且 simpleName（去包名）以
     * {@link #THROWABLE_NAME_SUFFIXES} 之一结尾——三者共同确保值来自受控异常类型而非伪装成类名的敏感明文（ZS-JOB-004 codex P2）。
     */
    private static boolean isControlledExceptionName(Object errorClass) {
        if (!(errorClass instanceof String)) {
            return false;
        }
        String name = (String) errorClass;
        if (!isQualifiedJavaName(name)) {
            return false;
        }
        String simpleName = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
        return THROWABLE_NAME_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }

    /**
     * 合法 Java 限定名逐段验证：以点号切分（{@code -1} 保留尾部空段），每段须匹配 {@link #JAVA_IDENTIFIER_PATTERN}
     * 单段标识符形态——拦截连续点（{@code a..b} 产生空段）、数字开头段（{@code a.1}）、首尾点等畸形限定名，
     * 杜绝 {@code a..bException} 之流借 Throwable 后缀绕过（ZS-JOB-004 codex r2 P2-1）。
     */
    private static boolean isQualifiedJavaName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (String segment : name.split("\\.", -1)) {
            if (!JAVA_IDENTIFIER_PATTERN.matcher(segment).matches()) {
                return false;
            }
        }
        return true;
    }

    private EventRow loadEvent(long eventId, Long tenantId) {
        List<EventRow> rows = jdbcTemplate.query(LOAD_EVENT_SQL, EVENT_ROW_MAPPER, eventId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 载入事件并加行锁（人工恢复专用，持锁至事务提交；只读回查不走此路径）。 */
    private EventRow loadEventForUpdate(long eventId, Long tenantId) {
        List<EventRow> rows = jdbcTemplate.query(LOAD_EVENT_FOR_UPDATE_SQL, EVENT_ROW_MAPPER, eventId, tenantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int countManualRetry(long eventId, Long tenantId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_MANUAL_RETRY_SQL, Integer.class, eventId, tenantId);
        return count == null ? 0 : count;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(OUTBOX_RECOVERY_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /** {@code outbox_event} 只读投影（JdbcTemplate 映射，非 MyBatis DO；循 {@code OutboxEventRecord} 范式）。 */
    private static final class EventRow {
        private long id;
        private String eventType;
        private String bizType;
        private String bizId;
        private String status;
        private int retryCount;
        private String payload;
        private String lastError;
        private Long tenantId;
        private LocalDateTime createTime;
    }

    /** {@code last_error} 受控摘要（无异常原文）。 */
    private static final class ErrorSummary {
        private String errorCategory;
        private String errorClass;
        private int messageLength;
    }

}
