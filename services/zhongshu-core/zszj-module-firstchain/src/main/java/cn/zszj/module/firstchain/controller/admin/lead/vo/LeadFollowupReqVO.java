package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 首链线索跟进 Request VO（ZS-FC-002，PILOT-REQ-007；仅被分配员工本人，追加式无修改通道）")
@Data
public class LeadFollowupReqVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "线索编号不能为空")
    private Long leadId;

    @Schema(description = "跟进内容（D-12 F1）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "电话沟通，客户对整装套餐有意向")
    @NotBlank(message = "跟进内容不能为空")
    private String content;

    @Schema(description = "下一步计划", example = "周末上门量房")
    private String nextStep;

    @Schema(description = "跟进时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-01 10:00:00")
    @NotNull(message = "跟进时间不能为空")
    private LocalDateTime followupTime;

}
