package cn.zszj.module.infra.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 参数配置恢复至历史值 Request VO（ZS-CFG-004 B04）")
@Data
public class ConfigRestoreReqVO {

    @Schema(description = "参数配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "参数配置编号不能为空")
    private Long id;

    @Schema(description = "恢复目标变更历史编号（其 old_value 即恢复值）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "恢复目标变更历史编号不能为空")
    private Long historyId;

    @Schema(description = "当前乐观锁版本，从详情回传", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "乐观锁版本不能为空")
    private Integer version;

    @Schema(description = "恢复审查依据", requiredMode = Schema.RequiredMode.REQUIRED, example = "审批单-2026-001：回滚误改的注册开关")
    @NotBlank(message = "恢复审查依据不能为空")
    @Size(max = 255, message = "恢复审查依据不能超过 255 个字符")
    private String reason;

}
