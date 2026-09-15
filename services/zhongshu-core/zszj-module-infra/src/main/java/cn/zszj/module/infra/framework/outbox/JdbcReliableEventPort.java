package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
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
 *   <li><b>事务参与强制（MANDATORY）</b>：append 事务模板以 {@code PROPAGATION_MANDATORY} 参与调用方事务——
 *       由事务管理器在 TM 层校验「本数据源存在真实事务」，无即拒绝、<b>不另开事务</b>。线程「有事务」或
 *       「绑定过本数据源连接」都不足以证明参与：他数据源事务下普通查询也会绑定本数据源的同步资源
 *       （transactionActive=false），REQUIRED 在该情形会静默另开事务自提交，丢失「业务回滚无事件」保证；</li>
 *   <li><b>租户上下文强制</b>：技术租户一律取自 {@link TenantContextHolder} 当前上下文，缺失即拒绝——
 *       供体「缺 tenant 默认写 0」会伪造归属，已去除；消息合同不含租户字段，调用方无法跨租户串用；</li>
 *   <li><b>fail-closed 全覆盖</b>：{@code eventType}/{@code actorType} 必填；守卫/序列化/写入失败统一将
 *       所参与事务标记 rollback-only——模板内经事务状态标记；MANDATORY 拒绝路径经本数据源绑定的
 *       {@link ConnectionHolder} 直接毒化（对 @Transactional 与编程式事务同样生效，提交时升级
 *       UnexpectedRollbackException），纵使调用方吞掉异常，业务也无法「无事件而提交」；
 *       唯一残余边界：他数据源事务错配且调用方吞异常（他数据源事务无法跨 TM 标记），append 以
 *       MANDATORY 拒绝留痕，见 docs/05 开发记录；</li>
 *   <li>{@code next_retry_at} 由应用侧时钟写入（与派发器领取/租约/退避同一时钟基准）；写入失败日志只记
 *       异常类别不落异常链（DB 异常文本可能携带 SQL 与绑定值）。</li>
 * </ul>
 */
@Repository
@Slf4j
public class JdbcReliableEventPort implements ReliableEventPort {

    /** 事件插入语句；{@code id/create_time} 由 DB 生成，{@code status/retry_count} 由 DB 默认值填充。 */
    private static final String INSERT_SQL = "INSERT INTO outbox_event "
            + "(event_type, biz_type, biz_id, biz_version, payload, headers, tenant_id, actor_type, actor_id, trace_id, next_retry_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /** 本端口绑定的业务数据源——MANDATORY 拒绝路径据其定位需毒化的事务连接。 */
    private final DataSource dataSource;

    /**
     * MANDATORY 事务模板：必须加入调用方在本数据源上的真实事务，无即抛
     * {@link IllegalTransactionStateException}（不另开事务——REQUIRED 在他数据源事务/仅普通查询绑定连接的
     * 情形下会静默自提交，丢失「业务回滚无事件」保证）。
     */
    private final TransactionTemplate callerTransactionTemplate;

    public JdbcReliableEventPort(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;
        this.callerTransactionTemplate = OutboxTransactions.mandatoryTemplate(dataSource, transactionManager);
    }

    @Override
    public long append(OutboxEventMessage message) {
        Long eventId;
        try {
            eventId = callerTransactionTemplate.execute(status -> {
                try {
                    // 租户上下文强制：技术租户一律取自当前上下文，缺失即拒绝，不默认写 0（不伪造归属）。
                    Long tenantId = TenantContextHolder.getTenantId();
                    if (tenantId == null) {
                        throw exception(OUTBOX_EVENT_TENANT_CONTEXT_REQUIRED);
                    }
                    validateRequired(message);
                    return doAppend(message, tenantId);
                } catch (RuntimeException ex) {
                    // fail-closed：租户/必填/序列化/写入任一失败即标记所参与事务 rollback-only 再抛出，
                    // 纵使调用方吞掉异常，业务事务也无法「无事件而提交」。
                    status.setRollbackOnly();
                    throw ex;
                }
            });
        } catch (IllegalTransactionStateException e) {
            // MANDATORY 拒绝：线程没有绑定本数据源的真实事务（完全无事务，或仅普通查询级连接绑定）
            return failClosed(exception(OUTBOX_EVENT_TRANSACTION_REQUIRED));
        }
        return eventId != null ? eventId : -1L;
    }

    /**
     * 模板外守卫失败：毒化本数据源绑定的真实事务连接（{@link ConnectionHolder#setRollbackOnly}，
     * 提交时升级 {@code UnexpectedRollbackException}，对 @Transactional 与编程式事务同样生效）后抛出；
     * 无绑定的真实事务（MANDATORY 拒绝的根因）则无从标记，仅拒绝。
     */
    private long failClosed(ServiceException ex) {
        Object resource = TransactionSynchronizationManager.getResource(dataSource);
        if (resource instanceof ConnectionHolder holder) {
            holder.setRollbackOnly();
        }
        throw ex;
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
                ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                return ps;
            }, keyHolder);
        } catch (DataAccessException e) {
            // 只记异常类别不落异常链：DB 异常文本可能携带 SQL 与绑定值（循 ZS-SEC-007/AUDIT-002 脱敏方向）
            log.error("[doAppend][Outbox 事件写入失败 eventType={} bizType={} bizId={} errClass={}]",
                    message.getEventType(), message.getBizType(), message.getBizId(), e.getClass().getName());
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
