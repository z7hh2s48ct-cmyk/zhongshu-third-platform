package cn.zszj.module.system.controller.admin.oauth2.vo.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理后台 - 访问令牌（会话）Response VO
 *
 * <p>ZS-LOGIN-006：本响应体用于「会话管理」，<b>不得</b>回显可用于认证的秘密（accessToken / refreshToken）。
 * 一旦经管理端点回显，即等同把「持有即可认证」的凭据暴露给任何具备查看权限的主体（前端表格、
 * 浏览器缓存、访问日志）。会话以不可用于认证的 {@code id}（DB 主键）标识，踢出也凭此 ID，无需持有令牌串。
 * 结构合同由 {@code OAuth2AccessTokenRespVoSecretTest} 反射固化，任何回归（重新加回令牌字段）都会失败并要求评审。
 */
@Schema(description = "管理后台 - 访问令牌（会话）Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AccessTokenRespVO {

    @Schema(description = "会话编号（不可用于认证的管理标识）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "666")
    private Long userId;

    @Schema(description = "用户类型，参见 UserTypeEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer userType;

    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String clientId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;

}
