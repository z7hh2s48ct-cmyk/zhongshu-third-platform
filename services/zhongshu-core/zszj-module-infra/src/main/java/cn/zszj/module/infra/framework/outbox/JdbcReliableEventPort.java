package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_FIELD_MISSING;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_PAYLOAD_SERIALIZE_FAILED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_TRANSACTION_REQUIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.OUTBOX_EVENT_WRITE_FAILED;

/**
 * {@link ReliableEventPort} 的 JDBC 适配实现（ZS-JOB-002）——业务数据源上与业务写同事务追加 Outbox 事件。
 *
 * <p>适配自供体 JdbcReliableEventPort（供体快照隔离于 {@code reference/donors/}，命名映射见 docs/06），落地众墅要求：
 * <ul>
 *   <li>忠实 {@link JdbcTemplate}（可移植 H2/PG，与业务写同一线程绑定事务——业务回滚则事件一并回滚）；
 *       主键由 DB 生成（{@link GeneratedKeyHolder} + 指定生成列 {@code id}），替代供体 PG 专用 {@code RETURNING id}；
 *       {@code payload/headers} 经 {@link JsonUtils} 序列化落 {@code text}（JSON 串），替代供体 PG 专用
 *       {@code jsonb}，双方言可移植（均循 ZS-AUDIT-001 JdbcAuditPort 先例）；</li>
 *   <li><b>事务上下文强制</b>：append 必须在业务事务内调用，无事务即拒绝且不代开——供体无此约束，
 *       无事务调用会静默自提交，丢失「业务回滚无事件」保证；</li>
 *   <li><b>租户上下文强制</b>：技术租户一律取自 {@link TenantContextHolder} 当前上下文，缺失即拒绝——
 *       供体「缺 tenant 默认写 0」会伪造归属，已去除；消息合同不含租户字段，调用方无法跨租户串用；</li>
 *   <li>fail-closed：{@code eventType}/{@code actorType} 必填；序列化/写入失败先将所参与事务标记
 *       rollback-only 再抛出，纵使调用方吞掉异常，业务也无法「无事件而提交」。</li>
 * </ul>
 */
@Repository
@Slf4j
public class JdbcReliableEventPort implements ReliableEventPort {

    /** 事件插入语句；{@code id} 由 DB 生成、{@code create_time/status/retry_count/next_retry_at} 由 DB 默认值填充，均不在列。 */
    private static final String INSERT_SQL = "INSERT INTO outbox_event "
            + "(event_type, biz_type, biz_id, biz_version, payload, headers, tenant_id, actor_type, actor_id, trace_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * REQUIRED 事务模板：加入调用方业务事务（不新开——事务上下文守卫已在进入前强制存在），
     * 序列化/写入失败可标记其 rollback-only（fail-closed 兜底）。
     */
    private final TransactionTemplate callerTransactionTemplate;

    public JdbcReliableEventPort(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.callerTransactionTemplate = new TransactionTemplate(transactionManager);
        this.callerTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Override
    public long append(OutboxEventMessage message) {
        // ① 事务上下文强制：必须在业务事务内调用（同库同事务，业务回滚则事件一并回滚）；无事务即拒绝，不代开。
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw exception(OUTBOX_EVENT_TRANSACTION_REQUIRED);
        }
        // ② 租户上下文强制：技术租户一律取自当前上下文，缺失即拒绝，不默认写 0（不伪造归属）。
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED);
        }
        validateRequired(message);
        Long eventId = callerTransactionTemplate.execute(status -> {
            try {
                return doAppend(message, tenantId);
            } catch (RuntimeException ex) {
                // fail-closed：序列化/写入失败即标记所参与事务 rollback-only 再抛出，
                // 纵使调用方吞掉异常，业务事务也无法「无事件而提交」。
                status.setRollbackOnly();
                throw ex;
            }
        });
        return eventId != null ? eventId : -1L;
    }

    /** fail-closed 必填校验：缺失即抛，杜绝无类型的模糊事件进入投递。 */
    private void validateRequired(OutboxEventMessage message) {
        if (message == null) {
            throw exception(OUTBOX_EVENT_FIELD_MISSING, "message");
        }
        if (!StringUtils.hasText(message.getEventType())) {
            throw exception(OUTBOX_EVENT_FIELD_MISSING, "eventType");
        }
        if (message.getActorType() == null) {
            throw exception(OUTBOX_EVENT_FIELD_MISSING, "actorType");
        }
    }

    /** 在调用方事务内序列化并插入事件，返回事件 ID（无法取得生成键返回 {@code -1}）。 */
    private Long doAppend(OutboxEventMessage message, Long tenantId) {
        String payloadJson = serializePayload(message.getPayload());
        String headersJson = message.getHeaders() == null ? null : JsonUtils.toJsonString(message.getHeaders());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, message.getEventType());
                ps.setString(2, message.getBizType());
                ps.setString(3, message.getBizId());
                ps.setString(4, message.getBizVersion());
                ps.setString(5, payloadJson);
                if (headersJson == null) {
                    ps.setNull(6, Types.VARCHAR);
                } else {
                    ps.setString(6, headersJson);
                }
                ps.setLong(7, tenantId);
                ps.setString(8, message.getActorType().name());
                ps.setString(9, message.getActorId());
                ps.setString(10, message.getTraceId());
                return ps;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("[doAppend][Outbox 事件写入失败 eventType={} bizType={} bizId={}]",
                    message.getEventType(), message.getBizType(), message.getBizId(), e);
            throw exception(OUTBOX_EVENT_WRITE_FAILED);
        }
        Number generatedKey = keyHolder.getKey();
        return generatedKey != null ? generatedKey.longValue() : -1L;
    }

    /** 载荷序列化为 JSON 串（null 归一化为空对象 {}；失败抛专用码，事件不落库、业务事务回滚）。 */
    private String serializePayload(Map<String, Object> payload) {
        try {
            return JsonUtils.toJsonString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            log.error("[serializePayload][Outbox 载荷序列化失败 errClass({})]", e.getClass().getSimpleName());
            throw exception(OUTBOX_EVENT_PAYLOAD_SERIALIZE_FAILED);
        }
    }

}
