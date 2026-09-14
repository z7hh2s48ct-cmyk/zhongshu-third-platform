package cn.zszj.module.infra.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 参数配置变更历史 Response VO（ZS-CFG-004 B04）")
@Data
public class ConfigChangeHistoryRespVO {

    @Schema(description = "历史编号", example = "2048")
    private Long id;

    @Schema(description = "参数配置编号", example = "1024")
    private Long configId;

    @Schema(description = "变更时的参数键名", example = "url.druid")
    private String configKey;

    @Schema(description = "变更类型", example = "UPDATE")
    private String changeType;

    @Schema(description = "变更前值（秘密/敏感参数为掩码 ******）", example = "http://old")
    private String oldValue;

    @Schema(description = "变更后值（秘密/敏感参数为掩码 ******）", example = "http://new")
    private String newValue;

    @Schema(description = "变更前乐观锁版本", example = "3")
    private Integer oldVersion;

    @Schema(description = "变更后乐观锁版本", example = "4")
    private Integer newVersion;

    @Schema(description = "操作者（管理员用户 ID）", example = "1")
    private Long operatorId;

    @Schema(description = "审查依据（恢复时必填）", example = "审批单-2026-001")
    private String reason;

    @Schema(description = "变更时间")
    private LocalDateTime createTime;

}
