package cn.zszj.module.infra.controller.admin.job.vo.outbox;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - Outbox 健康监测快照 Response VO（ZS-JOB-004）。
 *
 * <p>映射 {@code OutboxHealthMetrics}：积压 / DEAD / 最长等待 / 失败率 / 过期租约 + 越阈清单。
 * 阈值为保守占位（待 WP-19 实测回填），{@code breaches} 只反映「监测逻辑越阈」，<b>不</b>代表任何生产容量承诺（验收④）。
 */
@Schema(description = "管理后台 - Outbox 健康监测快照 Response VO（ZS-JOB-004）")
@Data
public class OutboxHealthRespVO {

    @Schema(description = "PENDING 积压计数", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Integer pendingBacklog;

    @Schema(description = "DEAD 计数（需人工恢复）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer deadCount;

    @Schema(description = "最老 PENDING 事件距今等待秒数", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    private Long longestWaitSeconds;

    @Schema(description = "失败率百分比（DEAD /（DISPATCHED+DEAD+SKIPPED）× 100）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "5.0")
    private Double failureRatePercent;

    @Schema(description = "过期租约计数（dispatcher 停摆征兆）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer staleLeaseCount;

    @Schema(description = "越阈清单（机器可读常量码，如 PENDING_BACKLOG_WARN；非空即应告警）")
    private List<String> breaches;

}
