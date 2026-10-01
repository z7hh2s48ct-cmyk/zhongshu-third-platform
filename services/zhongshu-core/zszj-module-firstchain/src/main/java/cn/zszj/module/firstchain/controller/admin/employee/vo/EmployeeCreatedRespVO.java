package cn.zszj.module.firstchain.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 首链员工账号创建结果 Response VO（ZS-FC-001，M5-A 初始密码下发载体）")
@Data
public class EmployeeCreatedRespVO {

    @Schema(description = "新建员工用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2050")
    private Long userId;

    @Schema(description = "登录用户名（统一 trim+小写存储）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "zhangsan001")
    private String username;

    @Schema(description = "初始密码（一次性下发：明文禁落审计/日志，请交由员工本人并在首次登录后修改；"
            + "强制首改后置登记）", requiredMode = Schema.RequiredMode.REQUIRED, example = "aB3dEf7hIj9kLm2N")
    private String initialPassword;

}
