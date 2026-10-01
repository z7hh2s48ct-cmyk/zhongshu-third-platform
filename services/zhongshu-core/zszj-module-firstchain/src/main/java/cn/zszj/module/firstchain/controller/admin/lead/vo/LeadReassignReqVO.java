package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链线索改派 Request VO（ZS-FC-002，D-07 M6；负责人改派，状态不变推版本）")
@Data
public class LeadReassignReqVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "线索编号不能为空")
    private Long id;

    @Schema(description = "新被分配员工用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2050")
    @NotNull(message = "改派目标员工不能为空")
    private Long newAssigneeUserId;

    @Schema(description = "期望版本（乐观锁基线）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

}
