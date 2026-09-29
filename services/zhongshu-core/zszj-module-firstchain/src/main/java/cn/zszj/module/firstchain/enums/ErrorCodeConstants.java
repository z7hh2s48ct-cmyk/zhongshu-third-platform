package cn.zszj.module.firstchain.enums;

import cn.zszj.framework.common.exception.ErrorCode;

/**
 * Firstchain 错误码枚举类
 *
 * <p>firstchain 模块段：1-021-000-000（紧邻 crm 1-020 段之后的空闲段登记；
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

}
