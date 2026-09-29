package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 首链加盟商申请审批拒绝 Request VO（ZS-FC-001，PILOT-REQ-002；D-07 M4：REJECTED 不建任何主体）")
@Data
public class ApplicationRejectReqVO {

    @Schema(description = "申请编号（业务键，租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "FC20260929-A1B2C3D4")
    @NotBlank(message = "申请编号不能为空")
    private String appKey;

    @Schema(description = "拒绝意见（必填，落 reject_reason + 审计留痕）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "资质材料不全")
    @NotBlank(message = "拒绝必须填写审批意见")
    @Size(max = 1024, message = "审批意见长度不能超过 1024 个字符")
    private String reason;

}
