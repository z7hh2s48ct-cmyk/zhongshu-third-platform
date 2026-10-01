package cn.zszj.module.firstchain.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - 首链员工账号创建 Request VO（ZS-FC-001，D-07 M5-A：负责人直接创建账号）")
@Data
public class EmployeeCreateReqVO {

    @Schema(description = "登录用户名（全平台唯一，统一 trim+小写存储循 D-09 M1/M2；4~30 位字母数字）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan001")
    @NotBlank(message = "登录用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户名须为 4~30 位字母数字")
    private String username;

    @Schema(description = "员工姓名（昵称展示）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "员工姓名不能为空")
    private String nickname;

}
