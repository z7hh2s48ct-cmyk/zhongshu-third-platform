package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出件保留期条目 VO（ZS-FILE-004.B：预览候选与清理失败逐项明细共用）。
 *
 * <p>预览场景只填 fileId/name/purpose/retentionExpireTime（供人工核对授权）；
 * 清理失败场景额外填 errorMessage（受控描述，不伪报成功）。</p>
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出件保留期条目 VO（ZS-FILE-004.B）")
@Data
public class FileExportRetentionItemRespVO {

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long fileId;

    @Schema(description = "原文件名", example = "export-2026.bin")
    private String name;

    @Schema(description = "用途（导出件固定 export）", example = "export")
    private String purpose;

    @Schema(description = "保留期到期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime retentionExpireTime;

    @Schema(description = "清理失败/跳过原因（受控描述；预览场景为空）")
    private String errorMessage;

}
