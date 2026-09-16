package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 孤儿对象清理 Response VO（ZS-FILE-005.B：逐项记录结果，中段失败不伪报全成功）
 *
 * @author ZS-FILE-005.B
 */
@Schema(description = "管理后台 - 孤儿对象清理 Response VO")
@Data
public class FileOrphanCleanupRespVO {

    @Schema(description = "清理成功的 path（含「对象已不存在」的幂等成功）")
    private List<String> successPaths = new ArrayList<>();

    @Schema(description = "清理失败/跳过的逐项明细")
    private List<Failure> failures = new ArrayList<>();

    @Schema(description = "清理失败明细")
    @Data
    public static class Failure {

        @Schema(description = "对象相对路径", requiredMode = Schema.RequiredMode.REQUIRED)
        private String path;

        @Schema(description = "失败原因（引用命中/存储失败等受控描述）", requiredMode = Schema.RequiredMode.REQUIRED)
        private String errorMessage;

    }

}
