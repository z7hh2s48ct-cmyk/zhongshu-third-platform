package cn.zszj.module.system.framework.audit.core;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_DETAIL_SERIALIZE_FAILED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_FIELD_MISSING;
import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_WRITE_FAILED;

/**
 * {@link AuditPort} 的 JDBC 适配实现（ZS-AUDIT-001）——业务数据源上同步落库、不可改写。
 *
 * <p>适配自供体 infra 审计端口 JDBC 实现（供体快照隔离于 {@code reference/donors/}，命名映射见 docs/06），落地众墅要求：
 * <ul>
 *   <li>忠实 {@link JdbcTemplate}（可移植 H2/PG、显式技术租户控制，规避 MyBatis-Plus 租户插件自动注入与
 *       BaseDO 逻辑删除语义——审计只追加不改写，无 {@code deleted/updater/update_time}）；</li>
 *   <li>{@code tenantId} 可空、<b>不默认 0</b>（供体 {@code tenantId==null?0L} 已去除；D-09 技术租户扩展点，
 *       未提供即写 SQL NULL，不伪造归属）；</li>
 *   <li>主键由 DB 生成（{@link GeneratedKeyHolder} + 指定生成列 {@code id}），替代供体 PG 专用 {@code RETURNING id}，
 *       兼容 H2 单测；</li>
 *   <li>{@code detail} 经 {@link JsonUtils} 序列化落 {@code text}（JSON 串），替代供体 PG 专用 {@code jsonb}，双方言可移植；</li>
 *   <li>{@code create_time} 由 DB {@code DEFAULT CURRENT_TIMESTAMP} 填充（单一时钟源），不由应用侧写入。</li>
 * </ul>
 *
 * <p>事务语义（{@link #record}）：
 * <ul>
 *   <li>{@code SUCCESS}——<b>随调用方事务</b>：经 {@code PROPAGATION_REQUIRED} 的 {@link TransactionTemplate} 写入，
 *       有调用方事务则加入（业务提交则留、业务回滚则不留），无调用方事务则自开事务提交；<b>fail-closed 兜底</b>——
 *       校验 / 序列化 / 写入任一失败即先将所参与事务标记 {@code rollback-only} 再抛出，纵使调用方吞掉异常，
 *       业务也无法「无成功审计而提交」；</li>
 *   <li>{@code DENIED} / {@code FAILURE}——<b>独立事务</b>：经 {@code PROPAGATION_REQUIRES_NEW} 的
 *       {@link TransactionTemplate} 写入，挂起并独立于业务事务提交，业务回滚不丢失拒绝 / 失败留痕；其失败仅回滚
 *       审计子事务并向调用方抛出，不牵连业务事务（拒绝 / 失败留痕独立于业务）。</li>
 * </ul>
 *
 * <p>幂等：{@code idempotencyKey} 先归一化——空白（{@code null} / 空串 / 纯空格）一律视作「无幂等键」落 SQL NULL
 * （唯一索引视 NULL 互异，多条无键事件不互相碰撞）；携非空键的事件先经 pre-check
 * （{@code SELECT id ... WHERE idempotency_key=?}）命中即返回既有事件 ID、不重复插入；并发竞态由
 * {@code uk_audit_event_idempotency} 唯一约束 DB 硬兜底（撞唯一键→写入失败码→调用方事务回滚重试即经 pre-check 命中，
 * 最终不重复入账）。
 *
 * <p>fail-closed：{@code eventType} / {@code actorType} / {@code result} 必填，缺失即抛
 * {@link cn.zszj.framework.common.exception.ServiceException}（{@code AUDIT_EVENT_FIELD_MISSING}）。{@code SUCCESS} 事件的
 * 校验 / 序列化 / 写入失败会先将所参与事务标记 {@code rollback-only} 再抛出——即使调用方吞掉异常，业务事务也无法提交，
 * 杜绝「无主体的模糊审计」与「业务提交却无成功审计」。
 */
@Repository
@Slf4j
public class JdbcAuditPort implements AuditPort {

    /** 审计事件插入语句；{@code id} 由 DB 生成、{@code create_time} 由 DB 默认值填充，均不在列。 */
    private static final String INSERT_SQL = "INSERT INTO audit_event "
            + "(event_type, actor_type, actor_id, action, biz_type, biz_id, biz_version, reason, "
            + "result, detail, tenant_id, trace_id, idempotency_key) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 幂等 pre-check：按幂等键回读既有事件 ID。 */
    private static final String SELECT_ID_BY_IDEMPOTENCY_KEY =
            "SELECT id FROM audit_event WHERE idempotency_key = ?";

    private final JdbcTemplate jdbcTemplate;

    /** {@code SUCCESS} 随调用方事务模板（{@code REQUIRED}，加入既有事务或自开）；fail-closed 失败可标记其 rollback-only。 */
    private final TransactionTemplate requiredTemplate;

    /** {@code DENIED} / {@code FAILURE} 独立事务模板（{@code REQUIRES_NEW}），业务回滚不丢失。 */
    private final TransactionTemplate requiresNewTemplate;

    public JdbcAuditPort(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.requiredTemplate = new TransactionTemplate(transactionManager);
        this.requiredTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public long record(AuditEventMessage message) {
        if (message != null && message.getResult() == AuditEventMessage.AuditResult.SUCCESS) {
            // SUCCESS 随调用方事务（REQUIRED 加入既有事务或自开）：校验 / 序列化 / 写入任一失败即标记所参与事务
            // rollback-only 再抛出，纵使调用方吞掉异常，业务也无法「无成功审计而提交」（兜底 fail-closed 合同）。
            Long eventId = requiredTemplate.execute(status -> {
                try {
                    validateRequired(message);
                    return doRecord(message);
                } catch (RuntimeException ex) {
                    status.setRollbackOnly();
                    throw ex;
                }
            });
            return eventId != null ? eventId : -1L;
        }
        // DENIED / FAILURE / result 缺失：独立事务（REQUIRES_NEW），业务回滚不丢失拒绝 / 失败留痕；
        // 其失败仅回滚审计子事务并向调用方抛出，不牵连业务事务。
        Long eventId = requiresNewTemplate.execute(status -> {
            validateRequired(message);
            return doRecord(message);
        });
        return eventId != null ? eventId : -1L;
    }

    /** fail-closed 必填校验：缺失即抛，中止当前事务，杜绝无主体的模糊审计。 */
    private void validateRequired(AuditEventMessage message) {
        if (message == null) {
            throw exception(AUDIT_EVENT_FIELD_MISSING, "message");
        }
        if (!StringUtils.hasText(message.getEventType())) {
            throw exception(AUDIT_EVENT_FIELD_MISSING, "eventType");
        }
        if (message.getActorType() == null) {
            throw exception(AUDIT_EVENT_FIELD_MISSING, "actorType");
        }
        if (message.getResult() == null) {
            throw exception(AUDIT_EVENT_FIELD_MISSING, "result");
        }
    }

    /**
     * 幂等 pre-check + 插入，返回事件 ID（幂等命中返回既有 ID；无法取得生成键返回 {@code -1}）。
     *
     * <p>须在目标事务上下文内调用：{@code SUCCESS} 在 {@code REQUIRED} 事务模板内调用（加入调用方事务或自开），
     * {@code DENIED} / {@code FAILURE} 在 {@code REQUIRES_NEW} 事务内调用。
     */
    private long doRecord(AuditEventMessage message) {
        // 归一化：空白幂等键（null / 空串 / 纯空格）一律视作「无键」落 SQL NULL，避免多条无键事件在唯一索引上互相碰撞。
        String idempotencyKey = StringUtils.hasText(message.getIdempotencyKey()) ? message.getIdempotencyKey() : null;
        if (idempotencyKey != null) {
            Long existingId = findIdByIdempotencyKey(idempotencyKey);
            if (existingId != null) {
                return existingId;
            }
        }
        String detailJson = serializeDetail(message.getDetail());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, message.getEventType());
                ps.setString(2, message.getActorType().name());
                ps.setString(3, message.getActorId());
                ps.setString(4, message.getAction());
                ps.setString(5, message.getBizType());
                ps.setString(6, message.getBizId());
                ps.setString(7, message.getBizVersion());
                ps.setString(8, message.getReason());
                ps.setString(9, message.getResult().name());
                ps.setString(10, detailJson);
                if (message.getTenantId() == null) {
                    ps.setNull(11, Types.BIGINT);
                } else {
                    ps.setLong(11, message.getTenantId());
                }
                ps.setString(12, message.getTraceId());
                if (idempotencyKey == null) {
                    ps.setNull(13, Types.VARCHAR);
                } else {
                    ps.setString(13, idempotencyKey);
                }
                return ps;
            }, keyHolder);
        } catch (DataAccessException e) {
            // 含并发幂等竞态撞 uk_audit_event_idempotency（DB 硬兜底）：调用方事务回滚重试即经 pre-check 命中，不重复入账
            log.error("[doRecord] 审计事件写入失败 eventType={} bizType={} bizId={} idempotencyKey={}",
                    message.getEventType(), message.getBizType(), message.getBizId(), idempotencyKey, e);
            throw exception(AUDIT_EVENT_WRITE_FAILED);
        }
        Number generatedKey = keyHolder.getKey();
        return generatedKey != null ? generatedKey.longValue() : -1L;
    }

    /** 按幂等键回读既有事件 ID（不存在返回 {@code null}）。 */
    private Long findIdByIdempotencyKey(String idempotencyKey) {
        List<Long> ids = jdbcTemplate.queryForList(SELECT_ID_BY_IDEMPOTENCY_KEY, Long.class, idempotencyKey);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** 审计明细序列化为 JSON 串（空明细落 SQL NULL；序列化失败抛专用码，不静默吞错）。 */
    private String serializeDetail(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            // ZS-AUDIT-002：写时脱敏——审计历史不可改写，敏感字段（凭据/密码等）必须在落库前净化，
            // 复用 ZS-SEC-007 LogSanitizeUtils 同一规则集（键归一模糊匹配 + 递归掩码 + 失败只记摘要）
            return LogSanitizeUtils.sanitizeJson(JsonUtils.toJsonString(detail));
        } catch (Exception e) {
            log.error("[serializeDetail] 审计明细序列化失败 detail={}", detail, e);
            throw exception(AUDIT_EVENT_DETAIL_SERIALIZE_FAILED);
        }
    }

}
