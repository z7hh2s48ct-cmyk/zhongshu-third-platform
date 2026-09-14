package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 预签名直传凭证创建 Response VO（ZS-FILE-003）")
@Data
@Accessors(chain = true)
public class FileUploadCredentialCreateRespVO {

    @Schema(description = "凭证令牌（完成确认唯一凭据）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String credentialToken;

    @Schema(description = "预签名上传地址（仅写临时区）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uploadUrl;

    @Schema(description = "临时对象键（服务端生成）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tempPath;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;

}
