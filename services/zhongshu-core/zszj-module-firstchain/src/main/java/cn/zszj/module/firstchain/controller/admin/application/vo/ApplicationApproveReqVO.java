package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 首链加盟商申请审批通过 Request VO（ZS-FC-001，PILOT-REQ-002/003；D-07 M4 同事务幂等开通）")
@Data
public class ApplicationApproveReqVO {

    @Schema(description = "申请编号（业务键，租户内唯一；开通幂等键）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "FC20260929-A1B2C3D4")
    @NotBlank(message = "申请编号不能为空")
    private String appKey;

    @Schema(description = "审批意见（PILOT-REQ-002：通过/拒绝均记录意见；通过时可空）", example = "资质齐备，同意开通")
    @Size(max = 1024, message = "审批意见长度不能超过 1024 个字符")
    private String reason;

}
