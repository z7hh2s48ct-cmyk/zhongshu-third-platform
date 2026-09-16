package cn.zszj.module.system.controller.admin.notify.vo.message;

import cn.zszj.module.system.service.notify.landing.NotifyLandingDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消息落点解析结果 Resp VO（ZS-MSG-003）
 *
 * <p>安全失败的体现方式见 {@code NotifyLandingService#resolveMessageLanding}；
 * 本 VO 只承载「消息属于当前用户」前提下的可用性结论——available=false 时
 * unavailableCode/reason 给出明确不可用原因（未知落点/关闭模块/业务失权/端不支持），
 * 供前端原样提示，不猜测、不降级跳转。
 */
@Schema(description = "消息落点解析结果 Resp VO")
@Data
public class NotifyMessageLandingRespVO {

    @Schema(description = "落点是否可用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean available;

    @Schema(description = "不可用原因码", example = "MODULE_DISABLED")
    private String unavailableCode;

    @Schema(description = "不可用的可呈现原因", example = "该消息所属模块未启用，落点不可用")
    private String reason;

    @Schema(description = "落点描述（仅 available=true 返回，不含消息正文）")
    private NotifyLandingDescriptor descriptor;

    public static NotifyMessageLandingRespVO available(NotifyLandingDescriptor descriptor) {
        NotifyMessageLandingRespVO vo = new NotifyMessageLandingRespVO();
        vo.setAvailable(true);
        vo.setDescriptor(descriptor);
        return vo;
    }

    public static NotifyMessageLandingRespVO unavailable(Enum<?> code, String reason) {
        NotifyMessageLandingRespVO vo = new NotifyMessageLandingRespVO();
        vo.setAvailable(false);
        vo.setUnavailableCode(code.name());
        vo.setReason(reason);
        return vo;
    }

}
