package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 首链审批流程实例绑定服务（ZS-BPM-003）——幂等命令门。
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

    public FirstChainProcessBindingService(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

}
