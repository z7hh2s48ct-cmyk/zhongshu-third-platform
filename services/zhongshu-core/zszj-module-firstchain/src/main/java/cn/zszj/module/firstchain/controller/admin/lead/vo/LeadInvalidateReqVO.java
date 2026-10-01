package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链线索无效关闭 Request VO（ZS-FC-002，PILOT-REQ-008；原因枚举必填，OTHER 说明必填）")
@Data
public class LeadInvalidateReqVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "线索编号不能为空")
    private Long leadId;

    @Schema(description = "关闭原因（UNREACHABLE 无法联系/BUDGET_MISMATCH 预算不符/NOT_TARGET 非目标客户/DUPLICATE 重复线索/OTHER 其他）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "UNREACHABLE")
    @NotBlank(message = "关闭原因不能为空")
    private String reasonName;

    @Schema(description = "关闭说明（原因为 OTHER 时必填）", example = "一周内多次联系未接通")
    private String reasonDetail;

    @Schema(description = "期望版本（乐观锁基线）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

}
