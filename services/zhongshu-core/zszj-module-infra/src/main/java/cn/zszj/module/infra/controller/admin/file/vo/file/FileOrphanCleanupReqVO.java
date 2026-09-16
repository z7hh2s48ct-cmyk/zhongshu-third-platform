package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 孤儿对象清理 Request VO（ZS-FILE-005.B：显式 path 授权，不做通配清理）
 *
 * @author ZS-FILE-005.B
 */
@Schema(description = "管理后台 - 孤儿对象清理 Request VO")
@Data
public class FileOrphanCleanupReqVO {

    @Schema(description = "存储配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "存储配置编号不能为空")
    private Long configId;

    @Schema(description = "待清理对象相对路径列表（须来自预览结果；执行前逐项重核验）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "待清理路径不能为空")
    private List<String> paths;

}
