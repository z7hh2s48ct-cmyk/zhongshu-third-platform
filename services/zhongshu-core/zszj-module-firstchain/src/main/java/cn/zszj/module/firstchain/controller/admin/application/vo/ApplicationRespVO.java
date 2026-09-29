package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 首链加盟商申请 Response VO（ZS-FC-001；含状态/版本/编号与审计字段，DO 不直接出 Controller 层）")
@Data
public class ApplicationRespVO {

    @Schema(description = "申请编号（数据库主键 ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "申请编号（业务键，租户内唯一；开通幂等键）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "FC20260929-A1B2C3D4")
    private String appKey;

    @Schema(description = "申请方名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "众墅家装联盟（华东）")
    private String applicantName;

    @Schema(description = "联系人", example = "张三")
    private String contactName;

    @Schema(description = "联系人电话", example = "13800000000")
    private String contactPhone;

    @Schema(description = "资质附件文件编号数组（FILE 私有，票据交付走既有设施）", example = "[101, 102]")
    private List<Long> attachmentFileIds;

    @Schema(description = "拒绝意见（REJECTED 时非空）", example = "资质材料不全")
    private String rejectReason;

    @Schema(description = "申请状态（DRAFT/SUBMITTED/APPROVED/REJECTED，M3 三态）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SUBMITTED")
    private String status;

    @Schema(description = "乐观锁版本（提交携带编辑时基线，接入合同 §1.4）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long version;

    @Schema(description = "创建时间（审计字段）")
    private LocalDateTime createTime;

    @Schema(description = "更新时间（审计字段）")
    private LocalDateTime updateTime;

    @Schema(description = "创建者（审计字段）", example = "1")
    private String creator;

    @Schema(description = "更新者（审计字段）", example = "1")
    private String updater;

}
