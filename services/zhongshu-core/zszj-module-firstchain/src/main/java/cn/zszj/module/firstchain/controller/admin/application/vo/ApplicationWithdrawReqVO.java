package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链加盟商申请撤回审批 Request VO（ZS-FC-001；D-07 M3：领域状态不变仅解绑流程）")
@Data
public class ApplicationWithdrawReqVO {

    @Schema(description = "申请编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "申请编号不能为空")
    private Long id;

    @Schema(description = "期望版本（编辑时基线，乐观锁；接入合同 §1.4）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

}
