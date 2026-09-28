package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_BINDING_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_NOT_BOUND;

/**
 * 首链审批流程实例绑定服务（ZS-BPM-003）——幂等命令门。
 *
 * <p>绑定行语义：一个领域对象同一时刻至多一个 {@code BOUND} 活跃绑定（PG 部分唯一索引兜底 + 本服务
 * 事务内守卫）；{@code process_instance_id} 全局唯一（uk）。流程完成/撤回经<b>条件迁移</b>
 * {@code BOUND → COMPLETED/WITHDRAWN（记 outcome）}吸收 at-least-once 重投：
 * <ul>
 *     <li>0 行且绑定已 COMPLETED 同结果 → 幂等吸收（重复回调）；</li>
 *     <li>0 行且绑定已 COMPLETED 异结果 → 并发审批/撤回竞争（先到者生效，后来者显式冲突）；</li>
 *     <li>0 行且绑定已 WITHDRAWN 同为撤回 → 幂等吸收；异操作 → 竞争冲突。</li>
 * </ul>
 *
 * @author ZS-BPM-003
 */
public class FirstChainProcessBindingService {

    /** 绑定状态：审批中（活跃绑定，每对象至多一个） */
    public static final String STATUS_BOUND = "BOUND";

    /** 绑定状态：流程已完成（outcome 记录流程侧事实结果） */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** 绑定状态：流程已撤回（领域状态不变，可重新发起审批） */
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    /** 流程结果：通过 */
    public static final String OUTCOME_APPROVED = "APPROVED";

    /** 流程结果：拒绝 */
    public static final String OUTCOME_REJECTED = "REJECTED";

    /** 弃单审计事件类型：旧流程晚到结果不覆盖新版本（验收②，可回查） */
    public static final String EVENT_RESULT_DISCARDED = AuditEventTypes.FIRST_CHAIN_PROCESS_RESULT_DISCARDED;

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    public FirstChainProcessBindingService(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 按 processInstanceId 查绑定（全局唯一键，不过租户过滤——流程回调可能缺租户上下文；
     * 找到后由调用方以绑定行租户 {@code TenantUtils.execute} 进入领域事务）。
     *
     * @return 绑定行（id/tenant_id/domain_type/domain_id/app_key/process_instance_id/status/outcome）；无绑定返回 null
     */
    public Map<String, Object> findByProcessInstanceId(String processInstanceId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tenant_id, domain_type, domain_id, app_key, process_instance_id, status, outcome "
                        + "FROM bpm_first_chain_process_binding WHERE process_instance_id = ?",
                processInstanceId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 查对象的活跃（BOUND）绑定；无活跃绑定返回 null。
     */
    public Map<String, Object> findActiveByDomain(Long tenantId, FirstChainObjectType objectType, Long domainId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tenant_id, domain_type, domain_id, app_key, process_instance_id, status, outcome "
                        + "FROM bpm_first_chain_process_binding WHERE tenant_id = ? AND domain_type = ? "
                        + "AND domain_id = ? AND status = ?",
                tenantId, objectType.getKey(), domainId, STATUS_BOUND);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 查对象最新一条绑定（任意状态，撤回/重复语义判定用）：撤回不改领域状态，判重依据是绑定自身生命周期。
     */
    public Map<String, Object> findLatestByDomain(Long tenantId, FirstChainObjectType objectType, Long domainId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tenant_id, domain_type, domain_id, app_key, process_instance_id, status, outcome "
                        + "FROM bpm_first_chain_process_binding WHERE tenant_id = ? AND domain_type = ? "
                        + "AND domain_id = ? ORDER BY id DESC LIMIT 1",
                tenantId, objectType.getKey(), domainId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 建立活跃绑定（调用方须已确认无活跃绑定——事务内由 {@link #assertNoActiveBinding} 守卫）。
     */
    public void insertBound(Long tenantId, FirstChainObjectType objectType, Long domainId,
                            String appKey, String processInstanceId, String actorId) {
        jdbcTemplate.update(
                "INSERT INTO bpm_first_chain_process_binding "
                        + "(domain_type, domain_id, app_key, process_instance_id, status, tenant_id, creator, updater) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                objectType.getKey(), domainId, appKey, processInstanceId, STATUS_BOUND, tenantId, actorId, actorId);
    }

    /**
     * 幂等门：条件迁移 BOUND → 目标状态（COMPLETED/WITHDRAWN 记 outcome）。
     *
     * @return {@code true}=门由本次调用赢得（调用方继续领域迁移）；{@code false}=已被他人终结（重复/竞争，
     *         由 {@link #resolveGateLost} 分类）
     */
    public boolean transitionGate(Long tenantId, String processInstanceId, String targetStatus, String outcome,
                                  String updater) {
        int updated = jdbcTemplate.update(
                "UPDATE bpm_first_chain_process_binding SET status = ?, outcome = ?, updater = ?, "
                        + "update_time = CURRENT_TIMESTAMP WHERE process_instance_id = ? AND tenant_id = ? "
                        + "AND status = ?",
                targetStatus, outcome, updater, processInstanceId, tenantId, STATUS_BOUND);
        return updated == 1;
    }

    /**
     * 门未赢得时的分类：同状态同结果（COMPLETED+同 outcome / WITHDRAWN+撤回）→ 幂等吸收返回；
     * 其余（异结果/异操作）→ 绑定并发冲突显式抛出。
     */
    public void resolveGateLost(Long tenantId, String processInstanceId, String targetStatus, String outcome) {
        Map<String, Object> binding = findByProcessInstanceId(processInstanceId);
        String status = binding == null ? null : (String) binding.get("status");
        String boundOutcome = binding == null ? null : (String) binding.get("outcome");
        boolean duplicate = STATUS_WITHDRAWN.equals(targetStatus)
                ? STATUS_WITHDRAWN.equals(status)
                : STATUS_COMPLETED.equals(status) && outcome != null && outcome.equals(boundOutcome);
        if (duplicate) {
            return; // 幂等吸收：重复回调/重复撤回，不抛错不重复留痕
        }
        throw exception(FIRST_CHAIN_PROCESS_BINDING_CONFLICT);
    }

    /**
     * 活跃绑定存在性断言：需要「无活跃绑定」的入口（重新发起审批）在存在活跃绑定时显式冲突。
     */
    public void assertNoActiveBinding(Long tenantId, FirstChainObjectType objectType, Long domainId) {
        if (findActiveByDomain(tenantId, objectType, domainId) != null) {
            throw exception(FIRST_CHAIN_PROCESS_BINDING_CONFLICT);
        }
    }

    /**
     * 活跃绑定缺失断言：撤回/审批入口要求活跃绑定存在，否则显式 {@code FIRST_CHAIN_PROCESS_NOT_BOUND}。
     */
    public void assertActiveBinding(Long tenantId, FirstChainObjectType objectType, Long domainId) {
        if (findActiveByDomain(tenantId, objectType, domainId) == null) {
            throw exception(FIRST_CHAIN_PROCESS_NOT_BOUND);
        }
    }

}
