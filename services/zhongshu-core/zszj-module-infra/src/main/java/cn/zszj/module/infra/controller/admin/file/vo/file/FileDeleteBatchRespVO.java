package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 文件批量删除逐项结果 Response VO（ZS-FILE-005.A）")
@Data
public class FileDeleteBatchRespVO {

    @Schema(description = "删除成功的文件编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> successIds = new ArrayList<>();

    @Schema(description = "删除失败的逐项明细（中段失败不伪报全成功）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Failure> failures = new ArrayList<>();

    @Schema(description = "删除失败明细")
    @Data
    public static class Failure {

        @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long id;

        @Schema(description = "失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
        private String errorMessage;

    }

}
