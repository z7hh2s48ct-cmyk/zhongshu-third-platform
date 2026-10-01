package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链线索分配 Request VO（ZS-FC-002，PILOT-REQ-006；负责人分配本组织员工）")
@Data
public class LeadAssignReqVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "线索编号不能为空")
    private Long id;

    @Schema(description = "被分配员工用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "被分配员工不能为空")
    private Long assigneeUserId;

    @Schema(description = "期望版本（乐观锁基线）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

}
