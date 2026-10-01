package cn.zszj.module.firstchain.framework;

/**
 * 首链通知合同（ZS-FC-003 服务端接线 wave；MSG-001 统一派发 + MSG-002 待办 + MSG-003 落点的编码事实源）。
 *
 * <p>模板种子见迁移 {@code V20261001.002__firstchain_notify_templates.sql}（七模板覆盖 PILOT-REQ-010
 * 六节点：审批〔提交提醒 + 通过/拒绝〕、开通〔随通过模板语义〕、下发、分配、领取、结束）——
 * 模板编码/必填参数与本类不得漂移（漂移即 TEMPLATE_NOT_FOUND / TEMPLATE_PARAM_MISSING 可见失败，
 * 通知失败不静默）。落点注册见 {@link FirstchainNotifyLandingProvider}（每模板一条目）。
 *
 * @author ZS-FC-003
 */
public final class FirstchainNotifyTemplates {

    // ========== 模板编码（与 V20261001.002 种子一一对应） ==========

    /** 审批节点①：申请提交提醒（致审批人；与 MSG-002 审批待办同事务落位） */
    public static final String APPLICATION_SUBMITTED = "firstchain_application_submitted";

    /** 审批节点②+开通节点：审批通过并开通（致提交人与新任负责人） */
    public static final String APPLICATION_APPROVED = "firstchain_application_approved";

    /** 审批节点②（拒绝支路）：审批拒绝（致提交人，携审批意见） */
    public static final String APPLICATION_REJECTED = "firstchain_application_rejected";

    /** 下发节点：新线索下发（致归属组织负责人） */
    public static final String LEAD_DISTRIBUTED = "firstchain_lead_distributed";

    /** 分配节点：线索分配（致被分配员工） */
    public static final String LEAD_ASSIGNED = "firstchain_lead_assigned";

    /** 领取节点：线索领取（致归属组织负责人） */
    public static final String LEAD_CLAIMED = "firstchain_lead_claimed";

    /** 结束节点：线索结束（致归属组织负责人；转商机/无效关闭共用模板，参数区分类型） */
    public static final String LEAD_CLOSED = "firstchain_lead_closed";

    // ========== 待办合同（MSG-002） ==========

    /** 待办业务来源（机制级分类：首链申请审批走 BPM 审批面） */
    public static final String TODO_SOURCE_TYPE = "BPM";

    // ========== 业务类型（bizType，与 FirstChainObjectType 键一致） ==========

    /** 申请域 bizType（与 {@code FirstChainObjectType.APPLICATION.getKey()} 同值常量，避免反向依赖 bpm 包） */
    public static final String BIZ_TYPE_APPLICATION = "franchisee_application";

    /** 线索域 bizType（与 {@code FirstChainObjectType.LEAD.getKey()} 同值常量） */
    public static final String BIZ_TYPE_LEAD = "lead";

    /** 落点归属模块（ModuleCatalog 命名空间） */
    public static final String MODULE = "firstchain";

    private FirstchainNotifyTemplates() {
    }

}
