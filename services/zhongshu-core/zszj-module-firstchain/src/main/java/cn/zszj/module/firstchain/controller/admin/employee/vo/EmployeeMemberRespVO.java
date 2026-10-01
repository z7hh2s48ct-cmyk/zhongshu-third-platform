package cn.zszj.module.firstchain.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 首链组织在职成员 Response VO（ZS-FC-003 员工选择器数据源）")
@Data
public class EmployeeMemberRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2050")
    private Long userId;

    @Schema(description = "登录用户名（trim+小写存储）", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan001")
    private String username;

    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String nickname;

}
