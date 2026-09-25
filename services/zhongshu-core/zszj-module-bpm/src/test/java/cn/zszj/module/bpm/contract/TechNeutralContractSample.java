package cn.zszj.module.bpm.contract;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.todo.NotifyTodoEventSink;
import cn.zszj.module.system.service.notify.todo.NotifyTodoRegisterCmd;
import cn.zszj.module.system.service.notify.todo.NotifyTodoService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中性合同样例（ZS-BPM-004）——「新业务模块接入合同」的可执行参照实现，<b>非业务代码</b>。
 *
 * <p>本样例演示一条技术中性的对象生命周期如何按 B03～B05 已交付的技术合同接入底座，完整链路：
 * <ol>
 *   <li>{@link #submit}：对象插入（归属挂租户、version=0 起步）+ 审计（SUCCESS 随业务事务）+ 待办登记
 *       （幂等键 (租户, sourceType, todoKey)）+ 同事务内部事件（进程内同步，与跨进程 Outbox 事件分开）；</li>
 *   <li>{@link #complete}：版本乐观锁推进（携带 expectedVersion，0 行即拒绝且拒绝留痕走独立事务，不静默覆盖）
 *       + 审计 + 待办流转事件预写（Outbox 同事务追加，MANDATORY）；</li>
 *   <li>{@link #deliverPendingTodoEvents}：消费侧接线演示——把事件投递给声明支持的 Sink，投递须在
 *       「事件自身租户上下文 + 编程式事务包裹」内执行（Inbox 端口要求调用方事务）。生产调度器为
 *       {@code OutboxDispatcherService}（租约领取 + 凭证栅栏 + 退避/DEAD），其投递路径的事务包裹要求
 *       已由 {@code NotifyTodoEventSink} 登记为 D-07 首链接线核实项。</li>
 * </ol>
 *
 * <p><b>接入合同要点</b>（完整清单见 {@code services/zhongshu-core/docs/新业务模块接入合同.md}）：
 * 模块注册、API/DTO、对象归属、版本并发、引用历史、动作/字段权限、审计、Outbox、待办、私有文件、流程适配
 * 与跨系统声明；本样例覆盖其中写侧（归属/版本/审计/Outbox/待办）与消费侧（租户上下文/事务包裹/幂等）的
 * 可执行部分。
 *
 * <p><b>D-07 中性红线</b>：本样例全部字段与载荷只含技术语义（{@code PENDING}/{@code DONE}/version/key），
 * <b>不含</b>任何试点业务态（加盟商 / 线索 / 审批业务态等）；真实系统接入时逐系统替换本样例的常量与载荷，
 * 逐系统回填接入合同与 UAT，不把样例通过算作全部业务系统可用。
 */
@Slf4j
public class TechNeutralContractSample {

    /** 对象类型（技术中性名，演示审计 / Outbox / 待办的 bizType 归属声明）。 */
    public static final String OBJECT_TYPE = "tech_neutral_contract";

    /** 对象初始状态（技术语义兜底态，无业务含义）。 */
    public static final String STATUS_PENDING = "PENDING";

    /** 对象完成状态（技术语义终态，无业务含义）。 */
    public static final String STATUS_DONE = "DONE";

    /** 待办业务来源标识（与 todoKey 共同构成注册幂等键的组成，演示「来源 ID 映射」合同）。 */
    public static final String TODO_SOURCE_TYPE = "TECH_NEUTRAL_CONTRACT";

    /** 样例主体标识（演示 actor 留痕；真实系统接入时替换为真实主体）。 */
    public static final String SAMPLE_ACTOR_ID = "tech-neutral-contract-sample";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AuditPort auditPort;
    private final ReliableEventPort reliableEventPort;
    private final NotifyTodoService notifyTodoService;
    private final List<OutboxEventSink> outboxEventSinks;
    private final ApplicationEventPublisher eventPublisher;

    public TechNeutralContractSample(DataSource dataSource, PlatformTransactionManager transactionManager,
                                     AuditPort auditPort, ReliableEventPort reliableEventPort,
                                     NotifyTodoService notifyTodoService, List<OutboxEventSink> outboxEventSinks,
                                     ApplicationEventPublisher eventPublisher) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditPort = auditPort;
        this.reliableEventPort = reliableEventPort;
        this.notifyTodoService = notifyTodoService;
        this.outboxEventSinks = outboxEventSinks == null ? List.of() : outboxEventSinks;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 提交：对象插入 + 审计（SUCCESS）+ 待办登记 + 同事务内部事件，四者同一事务（要么全有、要么全无）。
     *
     * <p>对象归属合同：{@code tenant_id} 取自 {@link TenantContextHolder} 当前上下文，缺失即拒绝——不默认 0、
     * 不伪造归属（下游 JdbcAuditPort / JdbcReliableEventPort / NotifyTodoServiceImpl 各自独立强制同一合同）。
     *
     * @param contractKey 对象稳定业务键（与租户共同唯一，演示「来源 ID 映射」）
     * @param title       待办标题（技术中性文本）
     * @param recipient   待办收件人
     * @return 对象行 ID
     */
    public Long submit(String contractKey, String title, NotifyRecipient recipient) {
        Long tenantId = requireTenantId();
        return transactionTemplate.execute(status -> {
            // 1) 对象：归属挂租户（不默认 0、不伪造归属）+ version=0 起步
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO tech_neutral_contract_record "
                                + "(contract_key, status, version, tenant_id, creator, updater) VALUES (?, ?, ?, ?, ?, ?)",
                        new String[]{"id"});
                ps.setString(1, contractKey);
                ps.setString(2, STATUS_PENDING);
                ps.setLong(3, 0L);
                ps.setLong(4, tenantId);
                ps.setString(5, SAMPLE_ACTOR_ID);
                ps.setString(6, SAMPLE_ACTOR_ID);
                return ps;
            }, keyHolder);
            Long recordId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
            // 2) 审计：SUCCESS 随本事务（业务回滚则审计不留）；携对象版本基线
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_CREATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(SAMPLE_ACTOR_ID)
                    .action("CREATE")
                    .bizType(OBJECT_TYPE)
                    .bizId(contractKey)
                    .bizVersion("0")
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            // 3) 待办：幂等注册（(租户, sourceType, todoKey) 幂等键），来源标识与业务归属齐备
            notifyTodoService.registerTodo(NotifyTodoRegisterCmd.builder()
                    .todoKey(contractKey)
                    .sourceType(TODO_SOURCE_TYPE)
                    .bizType(OBJECT_TYPE)
                    .bizId(contractKey)
                    .bizVersion("0")
                    .title(title)
                    .recipient(recipient)
                    .build());
            // 4) 同事务内部事件：进程内同步分发（监听发生在发布方事务内；跨进程通知一律走 Outbox）
            eventPublisher.publishEvent(new TechNeutralContractCreatedEvent(contractKey, 0L));
            return recordId;
        });
    }

    /**
     * 完成：版本乐观锁推进 + 审计 + 待办流转事件预写（三者同一事务）；携带 {@code expectedVersion} 防止绕过版本。
     *
     * <p>版本并发合同：更新必须携带编辑时（或前置流程读取时）的版本号——0 行即冲突，拒绝留痕（DENIED 走
     * JdbcAuditPort 独立事务，业务回滚不丢失）并抛出，<b>不静默覆盖他人更新</b>；同时以状态条件做终态守卫，
     * 携带当前版本的重复完成同样 0 行即拒（不产生第二次成功审计与重复流转事件）；成功路径同事务追加
     * {@code NOTIFY_TODO_TRANSITION} 事件（Outbox MANDATORY，业务回滚则事件一并回滚）。
     *
     * @param contractKey     对象稳定业务键
     * @param expectedVersion 期望的当前版本（乐观锁基线，不符即拒）
     * @param reason          流转原因（脱敏文本，可空）
     */
    public void complete(String contractKey, long expectedVersion, String reason) {
        Long tenantId = requireTenantId();
        transactionTemplate.executeWithoutResult(status -> {
            // 版本乐观锁 + 终态守卫：必须携带期望版本，且仅允许从初始态推进——0 行即拒
            //（版本不符 / 已终态重复完成 / 跨租户不可见 / 不存在），不静默覆盖、不重复完成
            int updated = jdbcTemplate.update(
                    "UPDATE tech_neutral_contract_record SET status = ?, version = version + 1, updater = ?, "
                            + "update_time = CURRENT_TIMESTAMP WHERE tenant_id = ? AND contract_key = ? AND version = ? "
                            + "AND status = ? AND deleted = FALSE",
                    STATUS_DONE, SAMPLE_ACTOR_ID, tenantId, contractKey, expectedVersion, STATUS_PENDING);
            if (updated != 1) {
                // 拒绝留痕：DENIED 走独立事务（业务回滚不丢失），记录被拒的期望版本
                auditPort.record(AuditEventMessage.builder()
                        .eventType(AuditEventTypes.ACCESS_DENIED)
                        .actorType(ActorType.ADMIN)
                        .actorId(SAMPLE_ACTOR_ID)
                        .action("COMPLETE")
                        .bizType(OBJECT_TYPE)
                        .bizId(contractKey)
                        .bizVersion(String.valueOf(expectedVersion))
                        .reason(reason)
                        .result(AuditResult.DENIED)
                        .tenantId(tenantId)
                        .build());
                throw new IllegalStateException("契约对象版本不匹配或不可见，拒绝完成：contractKey=" + contractKey
                        + ", expectedVersion=" + expectedVersion);
            }
            // 审计：更新成功携新版本（随本事务）
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(SAMPLE_ACTOR_ID)
                    .action("COMPLETE")
                    .bizType(OBJECT_TYPE)
                    .bizId(contractKey)
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            // 待办流转事件预写：Outbox 同事务追加（MANDATORY——无真实事务即拒），业务回滚则事件一并回滚
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("transitionType", "COMPLETE");
            payload.put("todoKey", contractKey);
            payload.put("sourceType", TODO_SOURCE_TYPE);
            if (StringUtils.hasText(reason)) {
                payload.put("reason", reason);
            }
            reliableEventPort.append(OutboxEventMessage.builder()
                    .eventType(NotifyTodoEventSink.EVENT_TYPE)
                    .bizType(OBJECT_TYPE)
                    .bizId(contractKey)
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .payload(payload)
                    .actorType(OutboxActorType.SYSTEM)
                    .actorId(SAMPLE_ACTOR_ID)
                    .build());
        });
    }

    /**
     * 派发待办流转事件：读取当前租户的 PENDING 事件，逐个投递给声明支持的 Sink 并确认。
     *
     * <p>消费侧接线合同（本方法演示最小正确实现）：
     * <ol>
     *   <li>领取范围按当前租户限定（不跨租户扫描）；</li>
     *   <li>投递在「事件自身租户上下文」内执行（{@code TenantUtils.execute}，杜绝跨租户串用）；</li>
     *   <li>投递以「编程式事务包裹」执行——消费端口（Inbox）要求调用方事务，业务回滚则幂等记录一并回滚；</li>
     *   <li>投递失败不静默确认：事件保持 PENDING 可重试（生产由 dispatcher 退避 / DEAD 接管，归 ZS-JOB-004）。</li>
     * </ol>
     *
     * <p>生产环境的领取 / 确认由 {@code OutboxDispatcherService} 以租约 + claim_token 凭证栅栏完成
     * （at-least-once，Sink 幂等吸收重投）；本样例为保持自包含，以直读 PENDING + 标记 DISPATCHED 演示等价时序。
     *
     * @param maxEvents 单批最大事件数
     * @return 实际确认的事件数
     */
    public int deliverPendingTodoEvents(int maxEvents) {
        Long tenantId = requireTenantId();
        // 领取范围按当前租户限定，不跨租户扫描；生产由 dispatcher 以租约 + claim_token 完成领取
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM outbox_event WHERE tenant_id = ? AND event_type = ? AND status = 'PENDING' ORDER BY id",
                tenantId, NotifyTodoEventSink.EVENT_TYPE);
        int delivered = 0;
        for (Map<String, Object> row : rows) {
            if (delivered >= maxEvents) {
                break;
            }
            deliverOne(row);
            delivered++;
        }
        return delivered;
    }

    /** 投递单条事件：事件自身租户上下文 + 编程式事务包裹（Inbox MANDATORY 要求），投递与确认同事务原子。 */
    private void deliverOne(Map<String, Object> row) {
        OutboxEventRecord event = toRecord(row);
        OutboxEventSink sink = outboxEventSinks.stream()
                .filter(candidate -> candidate.supports(event.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("无 Sink 声明消费事件类型：" + event.getEventType()));
        TenantUtils.execute(event.getTenantId(), () -> transactionTemplate.executeWithoutResult(status -> {
            try {
                sink.deliver(event);
            } catch (Exception e) {
                // 投递失败不静默确认：本事务回滚，事件保持 PENDING 可重试（生产由 dispatcher 退避 / DEAD 接管）
                throw new IllegalStateException("投递失败，事件保持 PENDING 待重试：eventId=" + event.getEventId(), e);
            }
            jdbcTemplate.update(
                    "UPDATE outbox_event SET status = 'DISPATCHED', dispatched_at = CURRENT_TIMESTAMP WHERE id = ?",
                    event.getEventId());
        }));
    }

    /** 写侧入口租户强制：归属合同第一道防线（缺失即拒——不默认 0、不伪造归属）。 */
    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("无租户上下文：写侧入口拒绝（归属合同要求显式租户，不默认 0）");
        }
        return tenantId;
    }

    /** Outbox 行 → Sink 投递记录（与 dispatcher 领取转换同构；样例以直读 PENDING 演示等价时序）。 */
    private static OutboxEventRecord toRecord(Map<String, Object> row) {
        return new OutboxEventRecord(
                ((Number) row.get("id")).longValue(),
                (String) row.get("event_type"),
                (String) row.get("biz_type"),
                (String) row.get("biz_id"),
                (String) row.get("biz_version"),
                (String) row.get("payload"),
                (String) row.get("headers"),
                row.get("tenant_id") == null ? null : ((Number) row.get("tenant_id")).longValue(),
                row.get("retry_count") == null ? 0 : ((Number) row.get("retry_count")).intValue(),
                (String) row.get("actor_type"),
                (String) row.get("actor_id"),
                (String) row.get("trace_id"),
                (String) row.get("claimed_by"),
                (String) row.get("claim_token"));
    }

    /**
     * 同事务内部事件（进程内同步发布；与 Outbox 跨进程事件的边界：内部事件不落库、不跨进程、随事务回滚而
     * 不发生副作用，仅适合同事务内的进程内扩展；跨系统 / 跨进程通知一律走 Outbox）。
     */
    @Value
    public static class TechNeutralContractCreatedEvent {

        /** 对象稳定业务键 */
        String contractKey;

        /** 创建时版本（恒为 0，演示事件携带版本基线） */
        long version;

    }

}
