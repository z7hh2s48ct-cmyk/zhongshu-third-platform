package cn.zszj.module.infra.controller.admin.config.vo;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 参数配置变更历史分页 Request VO（ZS-CFG-004 B04）")
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigChangeHistoryPageReqVO extends PageParam {

    @Schema(description = "参数配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "参数配置编号不能为空")
    private Long configId;

}
