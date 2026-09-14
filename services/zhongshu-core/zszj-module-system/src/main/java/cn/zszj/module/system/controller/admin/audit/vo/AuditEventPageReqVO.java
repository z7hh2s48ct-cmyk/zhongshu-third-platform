package cn.zszj.module.system.controller.admin.audit.vo;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审计事件分页 Request VO（ZS-AUDIT-002）")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditEventPageReqVO extends PageParam {

    @Schema(description = "事件类型", example = "OBJECT_UPDATED")
    private String eventType;

    @Schema(description = "主体标识", example = "1024")
    private String actorId;

    @Schema(description = "业务对象类型", example = "file")
    private String bizType;

    @Schema(description = "业务对象编号", example = "1024")
    private String bizId;

    @Schema(description = "链路标识", example = "a1b2c3d4e5f6")
    private String traceId;

    @Schema(description = "结果", example = "SUCCESS")
    private String result;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createTime;

}
