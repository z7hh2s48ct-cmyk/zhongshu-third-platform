package cn.zszj.module.firstchain.enums;

import cn.zszj.framework.common.exception.ErrorCode;

/**
 * Firstchain 错误码枚举类
 *
 * <p>firstchain 模块段：<b>1-021-000-000（ZS-FC-001 申请开通域）与 1-070-xxx-xxx（ZS-FC-002 线索域）</b>（均为主树登记的空闲段；
 * 各模块已占用段见 {@code ServiceErrorCodeRange} 与各模块 {@code ErrorCodeConstants} 类注释，
 * 本段在本卡登记前全库无占用——申请域写侧/审批/开通分类专用，禁止跨段占用）。
 *
 * <p>对应 ZS-FC-001（PILOT-REQ-001~004 服务端；D-07 M2/M3/M4）。bpm 域层
 * （cn.zszj.module.bpm.firstchain，1-009-020-x）的分类错误码在领域写回路径原样透传，
 * 不在本段重复定义——两段职责：本段管「firstchain 模块 REST 面与开通服务」自检分类，
 * bpm 段管「领域状态机与幂等写回」分类。
 *
 * @author ZS-FC-001
 */
public interface ErrorCodeConstants {

    // ========== 首链申请域（ZS-FC-001，PILOT-REQ-001~004）1-021-000-000 ==========

    ErrorCode FIRSTCHAIN_TENANT_REQUIRED = new ErrorCode(1_021_000_000, "无租户上下文：首链申请域写侧入口拒绝");

    ErrorCode FIRSTCHAIN_APPLICATION_NOT_EXISTS = new ErrorCode(1_021_000_001, "加盟商申请不存在或不可见");

    ErrorCode FIRSTCHAIN_APPLICATION_STATUS_CONFLICT = new ErrorCode(1_021_000_002,
            "申请状态冲突：当前状态 {}，不允许该审批操作");

    ErrorCode FIRSTCHAIN_APPLICATION_VERSION_CONFLICT = new ErrorCode(1_021_000_003,
            "申请版本冲突：期望版本 {} 与当前不符");

    ErrorCode FIRSTCHAIN_REJECT_REASON_REQUIRED = new ErrorCode(1_021_000_004, "拒绝必须填写审批意见");

    ErrorCode FIRSTCHAIN_OPENING_DUPLICATE = new ErrorCode(1_021_000_005,
            "重复开通请求：申请【{}】已开通（重复处理返回既有结果，本码登记供 PILOT-REQ-003 幂等验收对照与 M4-B 异步解耦时复用）");

    ErrorCode FIRSTCHAIN_OPENING_ORG_CONFLICT = new ErrorCode(1_021_000_006,
            "开通冲突：申请编号【{}】对应的组织已被并发开通占用（先到者生效，DB 唯一约束兜底）");

    ErrorCode FIRSTCHAIN_APPROVE_QUALIFICATION_DENIED = new ErrorCode(1_021_000_007,
            "审批资格不足：操作人无 PLATFORM 有效任职（M2：审批人=任意 PLATFORM 有效任职）");


    // ========== 1_070_001_xxx 线索 ==========
    ErrorCode LEAD_NOT_EXISTS = new ErrorCode(1_070_001_000, "线索不存在");
    ErrorCode LEAD_KEY_EXISTS = new ErrorCode(1_070_001_001, "线索编号【{}】已存在");
    ErrorCode LEAD_KEY_CONFLICT = new ErrorCode(1_070_001_002, "线索编号【{}】已存在但内容与下发请求不一致");
    ErrorCode LEAD_STATE_TRANSITION_NOT_ALLOWED = new ErrorCode(1_070_001_003, "线索状态【{}】不允许变更为【{}】");
    ErrorCode LEAD_VERSION_CONFLICT = new ErrorCode(1_070_001_004, "线索已被他人修改（期望版本 {}），请刷新后重试");
    ErrorCode LEAD_STATE_CONFLICT = new ErrorCode(1_070_001_005, "线索当前状态为【{}】，与操作期望不一致");
    ErrorCode LEAD_TENANT_REQUIRED = new ErrorCode(1_070_001_006, "线索操作缺少租户上下文");
    ErrorCode LEAD_ASSIGN_TARGET_NOT_IN_ORG = new ErrorCode(1_070_001_007, "分配目标员工不属于该线索归属组织");
    ErrorCode LEAD_CLAIM_NOT_ASSIGNEE = new ErrorCode(1_070_001_008, "员工只能领取分配给自己的线索");
    ErrorCode LEAD_REASSIGN_TARGET_NOT_IN_ORG = new ErrorCode(1_070_001_009, "改派目标员工不属于该线索归属组织");

    ErrorCode LEAD_ACTOR_QUALIFICATION_DENIED = new ErrorCode(1_070_001_010,
            "线索操作人无有效组织任职（须平台运营或加盟商成员，M2 三角色对象级资格）");

    ErrorCode LEAD_ACTOR_NOT_LEADER = new ErrorCode(1_070_001_011, "仅线索归属组织的负责人可执行该操作（D-07 M6）");

    ErrorCode LEAD_CONVERT_NOT_ASSIGNEE = new ErrorCode(1_070_001_012, "仅线索被分配员工可发起转商机（D-07 M7）");

    ErrorCode LEAD_VISIBLE_DENIED = new ErrorCode(1_070_001_013,
            "无权查看该线索（PILOT-REQ-009：员工看本人、负责人看本组织、平台人员看授权范围）");

    ErrorCode LEAD_DISTRIBUTE_NOT_PLATFORM = new ErrorCode(1_070_001_014,
            "仅平台运营可下发线索（PILOT-REQ-005：归属由服务端写入）");

    // ========== 1_070_002_xxx 跟进 ==========
    ErrorCode FOLLOWUP_NOT_ALLOWED_FOR_ACTOR = new ErrorCode(1_070_002_000, "仅分配员工本人可提交跟进记录");
    ErrorCode FOLLOWUP_LEAD_TERMINAL = new ErrorCode(1_070_002_001, "线索已结束（{}），不能再提交跟进记录");
    ErrorCode FOLLOWUP_TIME_REQUIRED = new ErrorCode(1_070_002_002, "跟进时间不能为空");

    // ========== 1_070_003_xxx 商机 ==========
    ErrorCode LEAD_CONVERT_REQUIRE_FOLLOWUP = new ErrorCode(1_070_003_000, "转商机前须至少提交一条跟进记录");
    ErrorCode LEAD_CONVERT_REQUIRE_CUSTOMER_NAME = new ErrorCode(1_070_003_001, "转商机前须先补全客户姓名");
    ErrorCode LEAD_INVALIDATE_REASON_REQUIRED = new ErrorCode(1_070_003_002, "无效关闭必须填写原因");
    ErrorCode LEAD_INVALIDATE_REASON_DETAIL_REQUIRED = new ErrorCode(1_070_003_003, "关闭原因为「其他」时必须补充说明");
    ErrorCode OPPORTUNITY_KEY_EXISTS = new ErrorCode(1_070_003_004, "商机编号【{}】已存在");
    ErrorCode OPPORTUNITY_ALREADY_EXISTS = new ErrorCode(1_070_003_005, "该线索已转出商机，不能重复转化");
}
