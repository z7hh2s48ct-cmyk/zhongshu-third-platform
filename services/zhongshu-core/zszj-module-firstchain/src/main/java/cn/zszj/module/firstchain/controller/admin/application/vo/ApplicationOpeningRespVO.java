package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 首链审批开通结果 Response VO（ZS-FC-001，M5-A 初始密码下发载体）")
@Data
public class ApplicationOpeningRespVO {

    @Schema(description = "已开通（或既有）的加盟商组织编号（重复处理返回既有结果，M4-A）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "300")
    private Long organizationId;

    @Schema(description = "本次新建负责人的初始密码（一次性下发：仅实际创建账号时返回，重复处理为 null；"
            + "明文禁落审计/日志，请交由加盟商负责人并在首次登录后修改）", example = "aB3dEf7hIj9kLm2N")
    private String initialPassword;

}
