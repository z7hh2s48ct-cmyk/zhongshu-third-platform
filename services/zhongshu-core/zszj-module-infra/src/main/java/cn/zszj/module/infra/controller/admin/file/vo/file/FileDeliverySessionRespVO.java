package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 下载会话建立 Response VO（ZS-FILE-004.A）")
@Data
public class FileDeliverySessionRespVO {

    @Schema(description = "下载会话 ID（绑定主体/登录会话/资产版本/用途，取流时须携带）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deliverySessionId;

    @Schema(description = "资产总大小（字节，同一不可变版本）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    private Long totalSize;

}
