package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 鉴权取流分块 Response VO（ZS-FILE-004.A）")
@Data
public class FileDeliveryChunkRespVO {

    @Schema(description = "分块内容（Range 语义 [start, endInclusive]，越界收敛到内容末尾）", requiredMode = Schema.RequiredMode.REQUIRED)
    private byte[] content;

    @Schema(description = "资产总大小（字节，同一不可变版本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Long totalSize;

    @Schema(description = "是否最后一块", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private boolean last;

}
