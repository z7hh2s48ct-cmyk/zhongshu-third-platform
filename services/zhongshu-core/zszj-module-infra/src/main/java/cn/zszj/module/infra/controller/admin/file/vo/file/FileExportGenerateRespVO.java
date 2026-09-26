package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出生成响应 VO（ZS-FILE-004.B）。
 *
 * <p>只回传平台内文件编号与保留期；不返回任何存储 URL（导出件一律 PRIVATE，
 * 交付必须走 ZS-FILE-004.A 票据/会话链）。</p>
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出生成 Response VO（ZS-FILE-004.B）")
@Data
public class FileExportGenerateRespVO {

    @Schema(description = "导出件文件编号（平台内主键，交付链凭此签发票据）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long fileId;

    @Schema(description = "保留期到期时间（生成时刻 + 配置保留天数）", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime retentionExpireTime;

}
