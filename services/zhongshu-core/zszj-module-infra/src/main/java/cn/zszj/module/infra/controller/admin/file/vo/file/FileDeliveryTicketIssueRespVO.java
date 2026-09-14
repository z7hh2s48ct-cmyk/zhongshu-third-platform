package cn.zszj.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 文件交付票据签发 Response VO（ZS-FILE-004.A）")
@Data
public class FileDeliveryTicketIssueRespVO {

    @Schema(description = "一次性票据 token（仅此一次返回明文，服务端只存散列）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ticketToken;

    @Schema(description = "票据过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;

}
