package cn.zszj.module.firstchain.controller.admin.lead.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 首链线索下发 Request VO（ZS-FC-002，PILOT-REQ-005；平台运营，归属组织服务端写入）")
@Data
public class LeadDistributeReqVO {

    @Schema(description = "客户姓名（D-12 F1）", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    @Schema(description = "客户手机号（D-12 F2，员工默认脱敏）", example = "13800000000")
    private String customerPhone;

    @Schema(description = "客户微信号（D-12 F2，员工默认脱敏）", example = "lisi_wx")
    private String customerWechat;

    @Schema(description = "客户详细地址（D-12 F2，员工默认脱敏）", example = "上海市静安区××路 88 号")
    private String customerAddress;

    @Schema(description = "线索来源（D-12 F1）", example = "展会获客")
    private String source;

    @Schema(description = "归属加盟商组织编号（服务端写入 org_id，调用方不得声明归属对象）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "300")
    @NotNull(message = "归属组织不能为空")
    private Long orgId;

}
