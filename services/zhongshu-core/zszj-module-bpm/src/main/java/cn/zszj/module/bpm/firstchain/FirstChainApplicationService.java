package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import lombok.Builder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 首链加盟商申请领域服务（ZS-BPM-003，D-07 M3/M4/M10）——领域状态机 + 流程实例绑定 + 幂等写回。
 *
 * @author ZS-BPM-003
 */
public class FirstChainApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    private final AuditPort auditPort;

    private final FirstChainStateTransitionExecutor stateTransitionExecutor;

    private final FirstChainProcessBindingService processBindingService;

    private final FirstChainProcessPort processPort;

    public FirstChainApplicationService(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
                                        AuditPort auditPort, FirstChainStateTransitionExecutor stateTransitionExecutor,
                                        FirstChainProcessBindingService processBindingService,
                                        FirstChainProcessPort processPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.auditPort = auditPort;
        this.stateTransitionExecutor = stateTransitionExecutor;
        this.processBindingService = processBindingService;
        this.processPort = processPort;
    }

    /**
     * 创建加盟商申请（D-07 M3：DRAFT 基线，version=0 起步；同事务 SUCCESS 审计）。
     */
    public Long createApplication(CreateApplicationCmd cmd) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 提交申请（DRAFT→SUBMITTED，M3）并同事务发起审批流程 + 建立绑定（M10=B）。
     */
    public void submitApplication(Long id, long expectedVersion, Long approverUserId, String actorId) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 审批回调（幂等命令门）：流程完成结果 → 领域动作（通过→APPROVED / 拒绝→REJECTED 必填意见）。
     *
     * <p>三重守卫：绑定条件迁移（幂等门，重复同结果=吸收、异结果=并发冲突）→ 领域版本条件更新
     * （旧流程晚到 0 行=弃单留痕不覆盖新版本）→ 终态锁定（状态机 fail-closed）。
     */
    public void onApprovalCompleted(String processInstanceId, boolean approved, String reason, String actorId) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 撤回审批流（M3：领域状态不变仍 SUBMITTED，绑定 BOUND→WITHDRAWN，可 restartApproval 重发）。
     */
    public void withdrawApproval(Long applicationId, long expectedVersion, String actorId) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 重新发起审批（SUBMITTED 且无活跃绑定时可用；撤回后重发，不重复建对象）。
     */
    public void restartApproval(Long applicationId, long expectedVersion, Long approverUserId, String actorId) {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

    /**
     * 创建命令（D-07 M8 申请字段：编号/申请方名称/联系人/电话/资质附件；审批意见在拒绝时落 reject_reason）。
     */
    @Builder
    public record CreateApplicationCmd(String appKey, String applicantName, String contactName,
                                       String contactPhone, String attachmentFileIds, String actorId) {
    }

}
