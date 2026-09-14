package cn.zszj.module.system.controller.admin.audit.vo;

import cn.zszj.module.system.dal.dataobject.audit.AuditEventDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审计事件 Response VO（ZS-AUDIT-002）")
@Data
public class AuditEventRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "事件类型", example = "OBJECT_UPDATED")
    private String eventType;

    @Schema(description = "主体类型", example = "ADMIN")
    private String actorType;

    @Schema(description = "主体标识", example = "1024")
    private String actorId;

    @Schema(description = "动作", example = "update")
    private String action;

    @Schema(description = "业务对象类型", example = "file")
    private String bizType;

    @Schema(description = "业务对象编号", example = "1024")
    private String bizId;

    @Schema(description = "业务对象版本", example = "v3")
    private String bizVersion;

    @Schema(description = "原因", example = "权限回收")
    private String reason;

    @Schema(description = "结果", example = "SUCCESS")
    private String result;

    @Schema(description = "明细（已脱敏 JSON）")
    private String detail;

    @Schema(description = "技术租户", example = "1")
    private Long tenantId;

    @Schema(description = "链路标识", example = "a1b2c3d4e5f6")
    private String traceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static AuditEventRespVO from(AuditEventDO bean) {
        AuditEventRespVO vo = new AuditEventRespVO();
        vo.setId(bean.getId());
        vo.setEventType(bean.getEventType());
        vo.setActorType(bean.getActorType());
        vo.setActorId(bean.getActorId());
        vo.setAction(bean.getAction());
        vo.setBizType(bean.getBizType());
        vo.setBizId(bean.getBizId());
        vo.setBizVersion(bean.getBizVersion());
        vo.setReason(bean.getReason());
        vo.setResult(bean.getResult());
        vo.setDetail(bean.getDetail());
        vo.setTenantId(bean.getTenantId());
        vo.setTraceId(bean.getTraceId());
        vo.setCreateTime(bean.getCreateTime());
        return vo;
    }

}
