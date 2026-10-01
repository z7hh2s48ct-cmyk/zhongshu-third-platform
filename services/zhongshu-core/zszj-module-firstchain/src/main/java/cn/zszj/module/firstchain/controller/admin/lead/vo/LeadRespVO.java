package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 首链线索 Response VO（ZS-FC-002，PILOT-REQ-009；D-12 A 类字段经统一裁决输出——"
        + "F2 敏感级低于访问者上限时脱敏展示（尾四位），超限字段不出现，DO 不直接出 Controller 层）")
@Data
public class LeadRespVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "线索编号（业务键，租户内唯一，服务端生成）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "LC20261001-A1B2C3D4")
    private String leadKey;

    @Schema(description = "客户姓名（D-12 F1）", example = "李四")
    private String customerName;

    @Schema(description = "客户手机号（D-12 F2：无授权时脱敏为尾四位）", example = "****0000")
    private String customerPhone;

    @Schema(description = "客户微信号（D-12 F2：无授权时脱敏）", example = "****_wx")
    private String customerWechat;

    @Schema(description = "客户详细地址（D-12 F2：无授权时脱敏）", example = "****************88 号")
    private String customerAddress;

    @Schema(description = "线索来源（D-12 F1）", example = "展会获客")
    private String source;

    @Schema(description = "归属加盟商组织编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    private Long orgId;

    @Schema(description = "被分配员工用户编号（未分配为 null）", example = "2048")
    private Long assigneeUserId;

    @Schema(description = "线索状态（DISTRIBUTED/ASSIGNED/FOLLOWING/CONVERTED/INVALID，D-07 M6 五态）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "FOLLOWING")
    private String status;

    @Schema(description = "乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long version;

    @Schema(description = "创建时间（审计字段）")
    private LocalDateTime createTime;

    @Schema(description = "更新时间（审计字段）")
    private LocalDateTime updateTime;

    @Schema(description = "创建者（审计字段）", example = "1")
    private String creator;

    @Schema(description = "更新者（审计字段）", example = "1")
    private String updater;

}
