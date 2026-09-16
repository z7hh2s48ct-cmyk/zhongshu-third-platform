package cn.zszj.module.infra.controller.admin.job.vo.outbox;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理后台 - Outbox 事件人工恢复（重试 / 跳过）Request VO（ZS-JOB-004）。
 *
 * <p><b>只接受 eventId + reason</b>——结构性不提供 payload/headers/event_type 编辑入口（历史事实不可掩盖，
 * 验收「修改历史被拒」）；操作者身份由 controller 从安全上下文注入 {@code OutboxRecoveryCmd}，<b>不</b>由请求体携带
 * （杜绝客户端伪造操作者，验收「无权重放被拒」的入口层保障，服务层再纵深校验）。
 */
@Schema(description = "管理后台 - Outbox 事件人工恢复（重试/跳过）Request VO（ZS-JOB-004）")
@Data
public class OutboxRecoveryReqVO {

    @Schema(description = "Outbox 事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "Outbox 事件编号不能为空")
    private Long eventId;

    @Schema(description = "人工恢复理由（必填，落台账 + 审计，不得含敏感明文）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "根因已修复：下游服务恢复，授权重试")
    @NotBlank(message = "人工恢复理由不能为空")
    @Size(max = 512, message = "人工恢复理由不能超过 512 个字符")
    private String reason;

}
