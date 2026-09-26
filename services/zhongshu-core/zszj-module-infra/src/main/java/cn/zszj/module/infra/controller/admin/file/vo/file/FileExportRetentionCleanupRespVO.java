package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出件保留期清理 Response VO（ZS-FILE-004.B：逐项记录结果，中段失败不伪报全成功）。
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出件保留期清理 Response VO（ZS-FILE-004.B）")
@Data
public class FileExportRetentionCleanupRespVO {

    @Schema(description = "清理成功的文件编号")
    private List<Long> successIds = new ArrayList<>();

    @Schema(description = "清理失败/跳过的逐项明细（含原因，便于对账）")
    private List<FileExportRetentionItemRespVO> failures = new ArrayList<>();

}
