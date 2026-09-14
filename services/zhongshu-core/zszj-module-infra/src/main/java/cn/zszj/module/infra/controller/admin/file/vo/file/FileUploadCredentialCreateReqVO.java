package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - 预签名直传凭证创建 Request VO（ZS-FILE-003）")
@Data
public class FileUploadCredentialCreateReqVO {

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "report.pdf")
    @NotBlank(message = "文件名称不能为空")
    private String name;

    @Schema(description = "上传用途", requiredMode = Schema.RequiredMode.REQUIRED, example = "attachment")
    @NotBlank(message = "上传用途不能为空")
    private String purpose;

    @Schema(description = "声明大小（字节）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "声明大小不能为空")
    @Positive(message = "声明大小必须为正数")
    private Long size;

    @Schema(description = "声明内容类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "application/pdf")
    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    @Schema(description = "可见范围", example = "PRIVATE")
    private String scope;

}
