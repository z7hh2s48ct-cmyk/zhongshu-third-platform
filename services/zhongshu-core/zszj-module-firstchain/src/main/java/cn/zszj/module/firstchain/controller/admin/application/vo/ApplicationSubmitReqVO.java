package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链加盟商申请提交 Request VO（ZS-FC-001，PILOT-REQ-001；D-07 M3 DRAFT→SUBMITTED）")
@Data
public class ApplicationSubmitReqVO {

    @Schema(description = "申请编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "申请编号不能为空")
    private Long id;

    @Schema(description = "期望版本（编辑时基线，乐观锁；接入合同 §1.4）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "期望版本不能为空")
    private Long expectedVersion;

    @Schema(description = "审批人用户编号（M2：审批人=任意 PLATFORM 有效任职；缺省为提交人——M2 双人分离后置登记）",
            example = "200")
    private Long approverUserId;

}
