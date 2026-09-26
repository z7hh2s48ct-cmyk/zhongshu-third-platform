package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出件保留期清理预览 Response VO（ZS-FILE-004.B：只读候选清点，循孤儿清理两步人工授权先例）。
 *
 * @author ZS-FILE-004.B
 */
@Schema(description = "管理后台 - 导出件保留期清理预览 Response VO（ZS-FILE-004.B）")
@Data
public class FileExportRetentionPreviewRespVO {

    @Schema(description = "候选条目（purpose='export' ∧ PUBLISHED ∧ 保留期已到，按到期时间升序）")
    private List<FileExportRetentionItemRespVO> items = new ArrayList<>();

    @Schema(description = "是否因达到上限被截断（如实标注，操作方分用途/分页多次清点）", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean truncated;

}
