package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APP_KEY_EXISTS;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_BINDING_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_PROCESS_NOT_BOUND;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_REJECT_REASON_REQUIRED;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_TENANT_REQUIRED;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_VERSION_CONFLICT;

/**
 * 首链加盟商申请领域服务（ZS-BPM-003，D-07 M3/M4/M10）——领域状态机 + 流程实例绑定 + 幂等写回。
 *
 * <p>状态一致性合同（验收①②③）：
 * <ul>
 *     <li>通过/拒绝/撤回与业务状态一致——通过→APPROVED、拒绝→REJECTED（必填意见落 reject_reason + 审计）、
 *     撤回→领域状态不变（仍 SUBMITTED，仅解绑流程实例，可重新发起，不重复建对象）；</li>
 *     <li>重复回调/旧流程晚到/并发审批撤回——绑定条件迁移为幂等门（重复同结果=吸收、异结果/异操作=先到者生效
 *     后来者显式冲突）；领域迁移为版本条件更新（终态/更高版本不可覆盖，晚到结果弃单留痕
 *     {@code FIRST_CHAIN_PROCESS_RESULT_DISCARDED} 可回查）；</li>
 *     <li>指标口径与来源——{@link FirstChainMetricsService} 从权威状态列派生，无独立计数器。</li>
 * </ul>
 *
 * <p>事务边界：全部写路径单事务（领域迁移 + 绑定 + 审计 SUCCESS 同事务，任一失败整体回滚——at-least-once
 * 回调重投安全）；DENIED/弃单审计由 {@code JdbcAuditPort} 独立事务落库（业务回滚不丢失拒绝留痕）。
 * 待办/事件接线（PILOT-REQ-010）随首链业务模块按 BPM-004 接入合同落卡，本卡不引入 bpm→system 主代码依赖。
 *
 * @author ZS-BPM-003
 */
@Slf4j
public class FirstChainApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    private final AuditPort auditPort;

    private final FirstChainStateTransitionExecutor stateTransitionExecutor;

    private final FirstChainProcessBindingService processBindingService;

    private final FirstChainProcessPort processPort;

    public FirstChainApplicationService(DataSource dataSource, PlatformTransactionManager transactionManager,
                                        AuditPort auditPort, FirstChainStateTransitionExecutor stateTransitionExecutor,
                                        FirstChainProcessBindingService processBindingService,
                                        FirstChainProcessPort processPort) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditPort = auditPort;
        this.stateTransitionExecutor = stateTransitionExecutor;
        this.processBindingService = processBindingService;
        this.processPort = processPort;
    }

    /**
     * 创建加盟商申请（D-07 M3：DRAFT 基线，version=0 起步；app_key 租户内唯一；同事务 SUCCESS 审计）。
     *
     * @return 申请行 ID
     */
    public Long createApplication(CreateApplicationCmd cmd) {
        Long tenantId = requireTenantId();
        return transactionTemplate.execute(status -> {
            Long exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM bpm_first_chain_application WHERE tenant_id = ? AND app_key = ? "
                            + "AND deleted = FALSE", Long.class, tenantId, cmd.appKey());
            if (exists != null && exists > 0) {
                throw exception(FIRST_CHAIN_APP_KEY_EXISTS, cmd.appKey());
            }
            jdbcTemplate.update(
                    "INSERT INTO bpm_first_chain_application "
                            + "(app_key, applicant_name, contact_name, contact_phone, attachment_file_ids, "
                            + "status, version, tenant_id, creator, updater) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?)",
                    cmd.appKey(), cmd.applicantName(), cmd.contactName(), cmd.contactPhone(),
                    cmd.attachmentFileIds(), FranchiseeApplicationStatus.DRAFT.name(), tenantId,
                    cmd.actorId(), cmd.actorId());
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM bpm_first_chain_application WHERE tenant_id = ? AND app_key = ? AND deleted = FALSE",
                    Long.class, tenantId, cmd.appKey());
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_CREATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(cmd.actorId())
                    .action("CREATE")
                    .bizType(FirstChainObjectType.APPLICATION.getKey())
                    .bizId(cmd.appKey())
                    .bizVersion("0")
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            return id;
        });
    }

    /**
     * 提交申请（DRAFT→SUBMITTED，M3）并同事务发起审批流程 + 建立绑定（M10=B 单节点审批流）。
     *
     * <p>流程发起失败（引擎异常）→ 整体回滚：领域回 DRAFT、无绑定残留（可重试提交）。
     */
    public void submitApplication(Long id, long expectedVersion, Long approverUserId, String actorId) {
        Long tenantId = requireTenantId();
        transactionTemplate.executeWithoutResult(status -> {
            Map<String, Object> row = requireApplication(tenantId, id);
            long actualVersion = ((Number) row.get("version")).longValue();
            if (actualVersion != expectedVersion) {
                // 分类次序：调用方视图过期优先报版本冲突（可回查），再谈状态机语义
                throw exception(FIRST_CHAIN_VERSION_CONFLICT, expectedVersion);
            }
            String currentStatus = (String) row.get("status");
            FirstChainStateMachines.assertAllowed(FirstChainObjectType.APPLICATION, currentStatus,
                    FranchiseeApplicationStatus.SUBMITTED.name());
            String appKey = (String) row.get("app_key");
            long newVersion = stateTransitionExecutor.transition(tenantId, FirstChainObjectType.APPLICATION,
                    id, currentStatus, FranchiseeApplicationStatus.SUBMITTED.name(), expectedVersion, actorId);
            String processInstanceId = processPort.startApprovalProcess(tenantId, appKey, approverUserId);
            processBindingService.insertBound(tenantId, FirstChainObjectType.APPLICATION, id, appKey,
                    processInstanceId, actorId);
            auditTransition(tenantId, appKey, "SUBMIT", newVersion, null, actorId);
        });
    }

    /**
     * 审批回调（幂等命令门）：流程完成结果 → 领域动作（通过→APPROVED / 拒绝→REJECTED 必填意见）。
     *
     * <p>守卫次序：①拒绝必填意见（未过门先校验，整事务回滚绑定不动）；②绑定条件迁移 BOUND→COMPLETED
     * （0 行=重复同结果吸收 / 异结果或撤回竞争显式冲突）；③领域版本条件更新（非 SUBMITTED=旧流程晚到，
     * 弃单留痕不覆盖新版本）。租户上下文以绑定行租户为准（回调可能无租户上下文）。
     */
    public void onApprovalCompleted(String processInstanceId, boolean approved, String reason, String actorId) {
        if (!approved && (reason == null || reason.isBlank())) {
            throw exception(FIRST_CHAIN_REJECT_REASON_REQUIRED);
        }
        Map<String, Object> binding = processBindingService.findByProcessInstanceId(processInstanceId);
        if (binding == null) {
            throw exception(FIRST_CHAIN_PROCESS_NOT_BOUND);
        }
        Long tenantId = ((Number) binding.get("tenant_id")).longValue();
        Long domainId = ((Number) binding.get("domain_id")).longValue();
        String outcome = approved
                ? FirstChainProcessBindingService.OUTCOME_APPROVED
                : FirstChainProcessBindingService.OUTCOME_REJECTED;
        TenantUtils.execute(tenantId, () -> transactionTemplate.executeWithoutResult(status -> {
            // ① 幂等门：条件迁移 BOUND→COMPLETED（记流程侧事实结果）
            boolean gateWon = processBindingService.transitionGate(tenantId, processInstanceId,
                    FirstChainProcessBindingService.STATUS_COMPLETED, outcome);
            if (!gateWon) {
                // 重复同结果=吸收返回；异结果/撤回竞争=显式冲突（先到者生效）
                processBindingService.resolveGateLost(tenantId, processInstanceId,
                        FirstChainProcessBindingService.STATUS_COMPLETED, outcome);
                return;
            }
            // ② 领域迁移：仅 SUBMITTED 可被审批结果推进；非 SUBMITTED=旧流程晚到 → 弃单留痕不覆盖
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, status, version, app_key FROM bpm_first_chain_application "
                            + "WHERE tenant_id = ? AND id = ? AND deleted = FALSE", tenantId, domainId);
            String action = approved ? "APPROVE" : "REJECT";
            String targetStatus = approved
                    ? FranchiseeApplicationStatus.APPROVED.name()
                    : FranchiseeApplicationStatus.REJECTED.name();
            if (rows.isEmpty() || !FranchiseeApplicationStatus.SUBMITTED.name().equals(rows.get(0).get("status"))) {
                String actualStatus = rows.isEmpty() ? "ABSENT" : String.valueOf(rows.get(0).get("status"));
                log.warn("[onApprovalCompleted][首链流程晚到结果弃单：application({}) 当前状态 {}，流程结果 {} 不覆盖]",
                        domainId, actualStatus, outcome);
                String discardAppKey = rows.isEmpty()
                        ? (String) binding.get("app_key")
                        : (String) rows.get(0).get("app_key");
                auditPort.record(AuditEventMessage.builder()
                        .eventType(FirstChainProcessBindingService.EVENT_RESULT_DISCARDED)
                        .actorType(ActorType.ADMIN)
                        .actorId(actorId)
                        .action("DISCARD_LATE_RESULT")
                        .bizType(FirstChainObjectType.APPLICATION.getKey())
                        .bizId(discardAppKey)
                        .reason("processOutcome=" + outcome + "; domainStatus=" + actualStatus)
                        .result(AuditResult.SUCCESS)
                        .tenantId(tenantId)
                        .build());
                return;
            }
            long currentVersion = ((Number) rows.get(0).get("version")).longValue();
            long newVersion = stateTransitionExecutor.transition(tenantId, FirstChainObjectType.APPLICATION,
                    domainId, FranchiseeApplicationStatus.SUBMITTED.name(), targetStatus, currentVersion, actorId);
            if (!approved) {
                jdbcTemplate.update("UPDATE bpm_first_chain_application SET reject_reason = ? "
                        + "WHERE tenant_id = ? AND id = ?", reason, tenantId, domainId);
            }
            auditTransition(tenantId, (String) rows.get(0).get("app_key"), action, newVersion,
                    reason, actorId);
        }));
    }

    /**
     * 撤回审批流（M3：领域状态不变仍 SUBMITTED，绑定 BOUND→WITHDRAWN；可 restartApproval 重发）。
     *
     * <p>语义：最新绑定 BOUND=正常撤回（流程侧取消 + 绑定门 + 审计）；WITHDRAWN=重复撤回幂等吸收；
     * COMPLETED=审批已完成后的撤回（并发审批/撤回竞争，先到者生效）显式冲突；无任何绑定=未提交过。
     */
    public void withdrawApproval(Long applicationId, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        transactionTemplate.executeWithoutResult(status -> {
            Map<String, Object> row = requireApplication(tenantId, applicationId);
            long actualVersion = ((Number) row.get("version")).longValue();
            if (actualVersion != expectedVersion) {
                throw exception(FIRST_CHAIN_VERSION_CONFLICT, expectedVersion);
            }
            Map<String, Object> binding = processBindingService.findLatestByDomain(tenantId,
                    FirstChainObjectType.APPLICATION, applicationId);
            if (binding == null) {
                throw exception(FIRST_CHAIN_PROCESS_NOT_BOUND);
            }
            String bindingStatus = (String) binding.get("status");
            String processInstanceId = (String) binding.get("process_instance_id");
            switch (bindingStatus) {
                case FirstChainProcessBindingService.STATUS_BOUND -> {
                    processPort.withdrawProcess(processInstanceId);
                    boolean gateWon = processBindingService.transitionGate(tenantId, processInstanceId,
                            FirstChainProcessBindingService.STATUS_WITHDRAWN, null);
                    if (!gateWon) {
                        processBindingService.resolveGateLost(tenantId, processInstanceId,
                                FirstChainProcessBindingService.STATUS_WITHDRAWN, null);
                        return;
                    }
                    auditTransition(tenantId, (String) row.get("app_key"), "WITHDRAW",
                            actualVersion, null, actorId);
                }
                case FirstChainProcessBindingService.STATUS_WITHDRAWN -> {
                    // 重复撤回：幂等吸收
                }
                default -> throw exception(FIRST_CHAIN_PROCESS_BINDING_CONFLICT);
            }
        });
    }

    /**
     * 重新发起审批（SUBMITTED 且无活跃绑定；撤回后重发，绑定新建不重复建对象）。
     */
    public void restartApproval(Long applicationId, long expectedVersion, Long approverUserId, String actorId) {
        Long tenantId = requireTenantId();
        transactionTemplate.executeWithoutResult(status -> {
            Map<String, Object> row = requireApplication(tenantId, applicationId);
            long actualVersion = ((Number) row.get("version")).longValue();
            if (actualVersion != expectedVersion) {
                throw exception(FIRST_CHAIN_VERSION_CONFLICT, expectedVersion);
            }
            if (!FranchiseeApplicationStatus.SUBMITTED.name().equals(row.get("status"))) {
                throw exception(FIRST_CHAIN_APPLICATION_NOT_EXISTS);
            }
            processBindingService.assertNoActiveBinding(tenantId, FirstChainObjectType.APPLICATION, applicationId);
            String processInstanceId = processPort.startApprovalProcess(tenantId,
                    (String) row.get("app_key"), approverUserId);
            processBindingService.insertBound(tenantId, FirstChainObjectType.APPLICATION, applicationId,
                    (String) row.get("app_key"), processInstanceId, actorId);
        });
    }

    // ========== 内部 ==========

    private Map<String, Object> requireApplication(Long tenantId, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, app_key, status, version FROM bpm_first_chain_application "
                        + "WHERE tenant_id = ? AND id = ? AND deleted = FALSE", tenantId, id);
        if (rows.isEmpty()) {
            throw exception(FIRST_CHAIN_APPLICATION_NOT_EXISTS);
        }
        return rows.get(0);
    }

    private void auditTransition(Long tenantId, String appKey, String action, long version,
                                 String reason, String actorId) {
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_UPDATED)
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action(action)
                .bizType(FirstChainObjectType.APPLICATION.getKey())
                .bizId(appKey)
                .bizVersion(String.valueOf(version))
                .reason(reason)
                .result(AuditResult.SUCCESS)
                .tenantId(tenantId)
                .build());
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(FIRST_CHAIN_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 创建命令（D-07 M8 申请字段：编号/申请方名称/联系人/电话/资质附件；审批意见在拒绝时落 reject_reason）。
     */
    @lombok.Builder
    public record CreateApplicationCmd(String appKey, String applicantName, String contactName,
                                       String contactPhone, String attachmentFileIds, String actorId) {
    }

}
