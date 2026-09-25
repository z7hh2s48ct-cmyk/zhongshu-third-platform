package cn.zszj.module.system.controller.admin.permission.vo.crossorgvisit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ZS-CLIENT-002.B：跨组织访问「我的授权目标」响应 VO——获批业务组织导航的唯一数据源。
 *
 * <p>与 {@link cn.zszj.module.system.service.permission.CrossOrgVisitService#authorizeVisit} 的拒绝维度对齐：
 * 仅返回<b>最新记录有效（ACTIVE 且未过期）、目标租户存在且启用、限定组织范围全部有效、目标非登录租户</b>
 * 的授权目标；任一维度不满足即不出现（客户端不做授权推导，只消费服务端结论）。
 *
 * @author ZS-CLIENT-002.B
 */
@Schema(description = "管理后台 - 跨组织访问我的授权目标 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossOrgVisitMyTargetsRespVO {

    @Schema(description = "登录租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long loginTenantId;

    @Schema(description = "登录租户名称（租户记录缺失时为空，不阻断目标列表）", example = "总部")
    private String loginTenantName;

    @Schema(description = "可访问的授权目标列表（空 = 未获批 / 无可用目标）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TargetVO> targets;

    @Schema(description = "跨组织访问授权目标 VO")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetVO {

        @Schema(description = "目标租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
        private Long tenantId;

        @Schema(description = "目标租户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "华东区域")
        private String tenantName;

        @Schema(description = "限定组织范围（null = 目标租户内全部组织）")
        private List<Long> targetOrgIds;

        @Schema(description = "授权有效期至（null = 无固定期限）")
        private LocalDateTime validTo;

    }

}
