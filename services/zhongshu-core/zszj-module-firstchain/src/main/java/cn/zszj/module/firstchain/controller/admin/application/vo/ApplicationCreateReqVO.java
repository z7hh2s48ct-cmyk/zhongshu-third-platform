package cn.zszj.module.firstchain.controller.admin.application.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 首链加盟商申请创建 Request VO（ZS-FC-001，PILOT-REQ-001；D-07 M3/M8）")
@Data
public class ApplicationCreateReqVO {

    @Schema(description = "申请方名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "众墅家装联盟（华东）")
    @NotBlank(message = "申请方名称不能为空")
    @Size(max = 128, message = "申请方名称长度不能超过 128 个字符")
    private String applicantName;

    @Schema(description = "联系人", example = "张三")
    @Size(max = 64, message = "联系人长度不能超过 64 个字符")
    private String contactName;

    @Schema(description = "联系人电话", example = "13800000000")
    @Size(max = 32, message = "联系人电话长度不能超过 32 个字符")
    private String contactPhone;

    @Schema(description = "资质附件文件编号数组（FILE 私有，只存引用；票据交付走 FileDeliveryService 既有设施）。"
            + "写入前服务端校验：文件存在于当前租户、私有、非导出件、且提交人对其具备读取资格，任一不满足整单拒绝；重复编号自动去重",
            example = "[101, 102]")
    @Size(max = 64, message = "资质附件数量不能超过 64 个")
    private List<@NotNull(message = "资质附件文件编号不能为空") @Positive(message = "资质附件文件编号必须为正数") Long> attachmentFileIds;

}
