package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 预签名直传完成确认 Request VO（ZS-FILE-003）")
@Data
public class FileUploadCredentialCompleteReqVO {

    @Schema(description = "凭证令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "凭证令牌不能为空")
    private String credentialToken;

}
