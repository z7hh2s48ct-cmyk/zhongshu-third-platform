package cn.zszj.module.firstchain.controller.admin.application.vo;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 首链加盟商申请分页 Request VO（ZS-FC-001，PILOT-REQ-001 查询侧）")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationPageReqVO extends PageParam {

    @Schema(description = "申请编号，精确匹配", example = "FC20260929-A1B2C3D4")
    private String appKey;

    @Schema(description = "申请方名称，模糊匹配", example = "众墅家装")
    private String applicantName;

    @Schema(description = "联系人，模糊匹配", example = "张三")
    private String contactName;

    @Schema(description = "申请状态（DRAFT/SUBMITTED/APPROVED/REJECTED，M3 三态）", example = "SUBMITTED")
    private String status;

}
