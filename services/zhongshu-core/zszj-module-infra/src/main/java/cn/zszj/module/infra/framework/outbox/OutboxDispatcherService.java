package cn.zszj.module.infra.framework.outbox;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox dispatcher：租约领取 + at-least-once 投递（ZS-JOB-002，适配自供体 OutboxDispatcherService）。
 *
 * <p>合同（docs/05 ZS-JOB-002 调整条款落地）：
 * <ul>
 *   <li>领取使用 {@code FOR UPDATE SKIP LOCKED} + 事件级租约（claim_expires_at），多实例不重复领取，
 *       崩溃实例的租约到期后事件可被其他实例重领；时间的写入与比较全部由应用侧参数化（同 JdbcReliableEventPort
 *       时钟基准，双方言可移植）；</li>
 *   <li><b>每次领取唯一凭证（claim_token，每事件一份）</b>：complete/fail 必须携带当前凭证——供体仅以 instanceId 做
 *       claimed_by 匹配，同实例租约过期重领后旧执行者仍能确认新领取；凭证为每事件每次领取新生成的 UUID，
 *       过期旧执行者（含同实例旧线程）无法覆盖新领取状态（栅栏）；</li>
 *   <li>领取以单个短事务原子完成：SELECT ... FOR UPDATE SKIP LOCKED 锁行 + 同事务内回填领取字段。
 *       等价于供体的 {@code UPDATE ... RETURNING} 单语句，拆两步系双方言可移植要求（H2 不支持
 *       UPDATE..RETURNING），事务内行锁保证两步间无他实例插入竞争；</li>
 *   <li>Sink 投递在领取/确认短事务之外：<b>在事件自身租户上下文内执行</b>（{@link TenantUtils#execute}，
 *       投递前后恢复调用线程上下文，杜绝跨租户串用）；成功标记 DISPATCHED，失败退避重试，超过 5 次进入
 *       DEAD 人工处置（重试/DEAD 台账与告警归 ZS-JOB-004）；</li>
 *   <li>Sink 必须幂等（at-least-once：租约过期重领、投递成功但确认丢失都会重投）；无 Sink 声明支持的事件
 *       按失败退避进入可见失败（重试至 DEAD），不丢弃；逐事件异常隔离——任一事件的 Sink 选择/投递异常
 *       不中断本批其余事件；</li>
 *   <li><b>派发事务与业务事务分离</b>：{@link #dispatchOnce} 必须在无事务上下文中调用——领取/确认各自独立
 *       短事务；若被误包进业务事务，租约与确认会延迟提交并放大重复投递，故 fail-fast 拒绝；
 *       {@code claim/complete/fail} 为包内可见（public 入口仅 {@code dispatchOnce/heartbeat}），
 *       防止外部调用绕过无事务守卫；</li>
 *   <li>失败留痕（{@code last_error} 与 WARN 日志）只存受控描述（异常类别/长度/我方常量码），不落
 *       异常原文——自由文本无可靠值级脱敏，见 {@link #describeThrowable}；</li>
 * </ul>
 */
@Slf4j
@Service
public class OutboxDispatcherService {

    /** 连续失败上限，达到即转 DEAD 人工处置（台账/恢复控制台归 ZS-JOB-004）。 */
    private static final int MAX_RETRY_BEFORE_DEAD = 5;

    /** 领取候选查询：PENDING + 已到期 + 租约空闲（无租约或已过期），SKIP LOCKED 跳过他实例持锁行，稳定 id 排序。 */
    private static final String CLAIM_SELECT_SQL = "SELECT id, event_type, biz_type, biz_id, biz_version, "
            + "payload, headers, tenant_id, retry_count, actor_type, actor_id, trace_id FROM outbox_event "
            + "WHERE status = 'PENDING' AND next_retry_at <= ? "
            + "AND (claim_expires_at IS NULL OR claim_expires_at < ?) "
            + "ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED";

    /** 领取标记回填：领取者 + 本次领取唯一凭证 + 租约到期（每事件一份凭证，与候选查询同事务，行锁保证候选不被抢占）。 */
    private static final String CLAIM_MARK_SQL = "UPDATE outbox_event "
            + "SET claimed_by = ?, claim_token = ?, claim_expires_at = ? WHERE id = ?";

    /** 确认语句：仅当前凭证可确认（栅栏——过期旧执行者的旧凭证无法覆盖新领取）。 */
    private static final String COMPLETE_SQL = "UPDATE outbox_event "
            + "SET status = 'DISPATCHED', dispatched_at = ? "
            + "WHERE id = ? AND status = 'PENDING' AND claim_token = ?";

    /** 失败语句：推进重试计数与退避点、达上限转 DEAD，并释放租约（仅当前凭证可操作）。 */
    private static final String FAIL_SQL = "UPDATE outbox_event "
            + "SET retry_count = retry_count + 1, last_error = ?, "
            + "next_retry_at = ?, "
            + "status = CASE WHEN retry_count + 1 >= ? THEN 'DEAD' ELSE status END, "
            + "claimed_by = NULL, claim_token = NULL, claim_expires_at = NULL "
            + "WHERE id = ? AND status = 'PENDING' AND claim_token = ?";

    private static final String HEARTBEAT_UPDATE_SQL = "UPDATE dispatcher_lease "
            + "SET lease_expires_at = ?, heartbeat_at = ? WHERE dispatcher_name = ? AND instance_id = ?";

    private static final String HEARTBEAT_INSERT_SQL = "INSERT INTO dispatcher_lease "
            + "(dispatcher_name, instance_id, lease_expires_at, heartbeat_at) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 领取事务模板（REQUIRED）：有调用方事务则加入（行锁随其提交/回滚），无则自开短事务——
     * {@link #dispatchOnce} 已强制无外部事务，领取由此成为独立短事务。
     */
    private final TransactionTemplate claimTemplate;

    private final List<OutboxEventSink> sinks;

    public OutboxDispatcherService(DataSource dataSource, PlatformTransactionManager transactionManager,
                                   List<OutboxEventSink> sinks) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.claimTemplate = OutboxTransactions.requiredTemplate(dataSource, transactionManager);
        this.sinks = sinks == null ? List.of() : sinks;
    }

    /** 登记并心跳 dispatcher 实例租约（可观测；事件级抢占由 outbox_event.claim_* 承担）。 */
    public void heartbeat(String dispatcherName, String instanceId, long leaseSeconds) {
        Timestamp now = currentTimestamp();
        Timestamp expires = plusSeconds(now, leaseSeconds);
        int updated = jdbcTemplate.update(HEARTBEAT_UPDATE_SQL, expires, now, dispatcherName, instanceId);
        if (updated == 0) {
            try {
                jdbcTemplate.update(HEARTBEAT_INSERT_SQL, dispatcherName, instanceId, expires, now);
            } catch (DuplicateKeyException e) {
                // 并发首次心跳撞唯一约束：退化为更新，本轮心跳不丢失
                jdbcTemplate.update(HEARTBEAT_UPDATE_SQL, expires, now, dispatcherName, instanceId);
            }
        }
    }

    /**
     * 领取一批到期事件：FOR UPDATE SKIP LOCKED + 过期租约回收 + 每事件签发唯一领取凭证。
     *
     * <p>候选锁定与领取标记在单个事务内原子完成（见类注释）；返回记录携带完整事件合同上下文与各自领取凭证。
     * 包内可见：public 投递入口是 {@link #dispatchOnce}（其无事务守卫不可绕过）。
     */
    List<OutboxEventRecord> claim(String dispatcherName, String instanceId,
                                  long leaseSeconds, int maxEvents) {
        String claimedBy = instanceId + "@" + dispatcherName;
        return claimTemplate.execute(status -> {
            Timestamp now = currentTimestamp();
            Timestamp claimExpiresAt = plusSeconds(now, leaseSeconds);
            List<OutboxEventRecord> candidates = new ArrayList<>();
            jdbcTemplate.query(CLAIM_SELECT_SQL, rs -> {
                String claimToken = UUID.randomUUID().toString();
                OutboxEventRecord record = mapCandidate(rs, claimedBy, claimToken);
                jdbcTemplate.update(CLAIM_MARK_SQL, claimedBy, claimToken, claimExpiresAt, record.getEventId());
                candidates.add(record);
            }, now, now, maxEvents);
            return List.copyOf(candidates);
        });
    }

    /** 投递成功确认：仅当前凭证可确认，返回 false 表示凭证已过期（事件已被重领，本轮确认丢失，待新执行者重投）。 */
    boolean complete(long eventId, String claimToken) {
        return jdbcTemplate.update(COMPLETE_SQL, currentTimestamp(), eventId, claimToken) == 1;
    }

    /** 投递失败登记：推进退避（达上限转 DEAD）并释放租约；返回 false 表示凭证已过期（不得推进新领取）。 */
    boolean fail(long eventId, String claimToken, String error, long backoffSeconds) {
        Timestamp now = currentTimestamp();
        return jdbcTemplate.update(FAIL_SQL, error, plusSeconds(now, backoffSeconds),
                MAX_RETRY_BEFORE_DEAD, eventId, claimToken) == 1;
    }

    /**
     * 执行一轮投递：领取 + 分发给第一个声明支持的 Sink。
     *
     * <p>必须在无事务上下文中调用（领取/确认各自独立短事务）——误包进业务事务会延迟租约与确认提交、
     * 放大重复投递（仍属 at-least-once，但不可取），故 fail-fast 拒绝。
     *
     * @return 本轮领取的事件数
     */
    public int dispatchOnce(String dispatcherName, String instanceId, long leaseSeconds,
                            int maxEvents, long backoffSeconds) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("dispatchOnce 必须在无事务上下文中调用：派发事务与业务事务分离，"
                    + "领取/确认须为独立短事务");
        }
        String claimedBy = instanceId + "@" + dispatcherName;
        List<OutboxEventRecord> events = claim(dispatcherName, instanceId, leaseSeconds, maxEvents);
        for (OutboxEventRecord event : events) {
            // 逐事件异常隔离：Sink 选择/投递的任何异常只推进该事件的失败退避，不中断本批其余事件
            try {
                OutboxEventSink sink = sinks.stream()
                        .filter(s -> s.supports(event.getEventType())).findFirst().orElse(null);
                if (sink == null) {
                    log.warn("[dispatchOnce][事件 {}({}) 无 Sink 声明支持，按失败退避进入可见失败]",
                            event.getEventId(), event.getEventType());
                    fail(event.getEventId(), event.getClaimToken(), "NO_SINK_SUPPORTS_EVENT_TYPE", backoffSeconds);
                    continue;
                }
                // 投递在事件自身租户上下文内执行（投递前后恢复调用线程上下文，杜绝跨租户串用）
                deliverInTenantContext(event, sink);
                if (!complete(event.getEventId(), event.getClaimToken())) {
                    // 确认丢失：领取期间租约过期已被重领（新凭证生效）。本轮投递已发生，at-least-once 合同
                    // 要求 Sink 幂等去重；旧领取不能覆盖新状态
                    log.warn("[dispatchOnce][事件 {} 投递成功但确认丢失（凭证过期已重领），依赖 Sink 幂等去重]",
                            event.getEventId());
                }
            } catch (Exception e) {
                log.warn("[dispatchOnce][事件 {} 投递失败: {}]", event.getEventId(), describeThrowable(e));
                fail(event.getEventId(), event.getClaimToken(), describeThrowable(e), backoffSeconds);
            }
        }
        return events.size();
    }

    /** 在事件租户上下文内投递（{@link TenantUtils#execute} finally 恢复调用线程原上下文）；checked 异常经桥接抛出。 */
    private void deliverInTenantContext(OutboxEventRecord event, OutboxEventSink sink) throws Exception {
        Exception[] holder = new Exception[1];
        TenantUtils.execute(event.getTenantId(), () -> {
            try {
                sink.deliver(event);
            } catch (Exception e) {
                holder[0] = e;
            }
        });
        if (holder[0] != null) {
            throw holder[0];
        }
    }

    /** 候选行 → 领取记录（完整事件合同 + 本次领取签发的 claimedBy/claimToken，经同事务标记回填落库）。 */
    private OutboxEventRecord mapCandidate(ResultSet rs, String claimedBy, String claimToken) throws SQLException {
        return new OutboxEventRecord(
                rs.getLong("id"), rs.getString("event_type"), rs.getString("biz_type"), rs.getString("biz_id"),
                rs.getString("biz_version"), rs.getString("payload"), rs.getString("headers"),
                rs.getLong("tenant_id"), rs.getInt("retry_count"),
                rs.getString("actor_type"), rs.getString("actor_id"), rs.getString("trace_id"),
                claimedBy, claimToken);
    }

    /**
     * 失败留痕（fail-safe，codex r1）：异常消息是自由文本，可能携带凭据/签名 URL/响应正文，
     * 键级脱敏（{@code LogSanitizeUtils} 按敏感键掩码）对其无效——故 {@code last_error} 与 WARN 日志
     * <b>不落异常原文</b>，只存受控描述（异常类别 + 消息长度；我方常量码如 NO_SINK_SUPPORTS_EVENT_TYPE
     * 直接保留）。诊断原文由 Sink 自行日志（归 ZS-SEC-007 管辖），恢复台账的可控摘要升级归 ZS-JOB-004。
     */
    private String describeThrowable(Throwable e) {
        String message = e.getMessage();
        try {
            return JsonUtils.toJsonString(Map.of(
                    "errorClass", e.getClass().getSimpleName(),
                    "messageLength", message == null ? 0 : message.length()));
        } catch (Exception ex) {
            return "{\"errorClass\":\"(未知)\",\"messageLength\":0}";
        }
    }

    private static Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    private static Timestamp plusSeconds(Timestamp base, long seconds) {
        return new Timestamp(base.getTime() + seconds * 1000);
    }

}
