package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 导出件保留期清理请求 VO（ZS-FILE-004.B：显式 id 授权，有界批次）。
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出件保留期清理 Request VO（ZS-FILE-004.B）")
@Data
public class FileExportRetentionCleanupReqVO {

    @Schema(description = "授权的导出件文件编号列表（来自预览，操作方逐条确认）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件编号列表不能为空")
    private List<Long> fileIds;

}
