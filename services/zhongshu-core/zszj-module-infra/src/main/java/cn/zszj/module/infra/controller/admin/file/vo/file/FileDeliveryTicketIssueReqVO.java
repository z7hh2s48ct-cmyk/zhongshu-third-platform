package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - 文件交付票据签发 Request VO（ZS-FILE-004.A）")
@Data
public class FileDeliveryTicketIssueReqVO {

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "文件编号不能为空")
    private Long fileId;

    @Schema(description = "交付用途（download / export）", requiredMode = Schema.RequiredMode.REQUIRED, example = "download")
    @NotBlank(message = "交付用途不能为空")
    @Pattern(regexp = "download|export", message = "交付用途仅支持 download / export")
    private String purpose;

}
