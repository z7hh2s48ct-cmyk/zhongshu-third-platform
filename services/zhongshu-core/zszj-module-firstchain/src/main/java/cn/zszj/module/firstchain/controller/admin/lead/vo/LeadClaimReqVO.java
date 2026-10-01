package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链线索领取 Request VO（ZS-FC-002，PILOT-REQ-006；领取人=登录用户服务端绑定）")
@Data
public class LeadClaimReqVO {

    @Schema(description = "线索编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "线索编号不能为空")
    private Long id;

    @Schema(description = "期望版本（乐观锁基线）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

}
