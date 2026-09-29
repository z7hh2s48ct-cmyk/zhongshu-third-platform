package cn.zszj.module.firstchain.enums;

import cn.zszj.framework.common.exception.ErrorCode;

/**
 * firstchain 模块错误码（ZS-FC-002；模块段 1_070，经查与
 * infra 1_001 / system 1_002 / bpm 1_009 / crm 1_020 / erp 1_030 等既有段无冲突）。
 *
 * <p>分段：1_070_001_xxx 线索；1_070_002_xxx 跟进；1_070_003_xxx 商机。
 *
 * @author ZS-FC-002
 */
public interface ErrorCodeConstants {

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
