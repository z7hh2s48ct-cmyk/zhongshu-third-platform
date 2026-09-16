package cn.zszj.module.infra.controller.admin.job;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.infra.controller.admin.job.vo.outbox.OutboxEventPageReqVO;
import cn.zszj.module.infra.controller.admin.job.vo.outbox.OutboxHealthRespVO;
import cn.zszj.module.infra.controller.admin.job.vo.outbox.OutboxRecoveryReqVO;
import cn.zszj.module.infra.framework.outbox.health.OutboxHealthMonitor;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxEventRecoveryDetail;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryCmd;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryResult;
import cn.zszj.module.infra.framework.outbox.recovery.OutboxRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - Outbox 事件恢复台账与健康监测（ZS-JOB-004）。
 *
 * <p>WP-14/WP-19「统一恢复控制台与告警」的后端入口：读（page/detail/health）复用 {@code infra:job:query}，
 * 写（retry/skip）用专属 {@code infra:job:recover}（可独立授予「有权恢复人员」，验收「无权重放被拒」的第一道闸；
 * 服务层再纵深校验 operator 上下文 + 越权 {@code ACCESS_DENIED} 独立事务留痕）。
 *
 * <p><b>操作者身份不由请求体携带</b>：{@code retry/skip} 从安全上下文（{@link SecurityFrameworkUtils#getLoginUserId()}）
 * 与链路（{@link TracerUtils#getTraceId()}）注入 {@link OutboxRecoveryCmd}，杜绝客户端伪造操作者（循 {@code ConfigChangeRecorder}
 * 恢复留痕范式）。请求体仅接受 {@code eventId + reason}——结构性不提供 payload/headers/event_type 编辑入口（验收「修改历史被拒」）。
 */
@Tag(name = "管理后台 - Outbox 事件恢复台账与健康监测")
@RestController
@RequestMapping("/infra/outbox-event")
@Validated
public class OutboxEventController {

    @Resource
    private OutboxRecoveryService recoveryService;

    @Resource
    private OutboxHealthMonitor healthMonitor;

    @GetMapping("/page")
    @Operation(summary = "获得 Outbox DEAD 事件恢复台账分页（当前租户，逐条脱敏）")
    @PreAuthorize("@ss.hasPermission('infra:job:query')")
    public CommonResult<PageResult<OutboxEventRecoveryDetail>> getOutboxEventPage(@Valid OutboxEventPageReqVO pageVO) {
        return success(recoveryService.pageDeadEvents(pageVO.getPageNo(), pageVO.getPageSize()));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "回查 Outbox 事件恢复详情（payload 掩码 + 受控异常摘要 + 恢复台账历史）")
    @Parameter(name = "eventId", description = "Outbox 事件编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('infra:job:query')")
    public CommonResult<OutboxEventRecoveryDetail> getOutboxEventRecoveryDetail(@RequestParam("eventId") Long eventId) {
        return success(recoveryService.getRecoveryDetail(eventId));
    }

    @PutMapping("/retry")
    @Operation(summary = "授权人工重试 DEAD 事件（DEAD→PENDING，理由必填，落台账 + 审计）")
    @PreAuthorize("@ss.hasPermission('infra:job:recover')")
    public CommonResult<OutboxRecoveryResult> retryOutboxEvent(@Valid @RequestBody OutboxRecoveryReqVO reqVO) {
        return success(recoveryService.retryDeadEvent(buildCmd(reqVO)));
    }

    @PutMapping("/skip")
    @Operation(summary = "授权人工跳过 DEAD 事件（DEAD→SKIPPED 放弃终态，理由必填，落台账 + 审计）")
    @PreAuthorize("@ss.hasPermission('infra:job:recover')")
    public CommonResult<OutboxRecoveryResult> skipOutboxEvent(@Valid @RequestBody OutboxRecoveryReqVO reqVO) {
        return success(recoveryService.skipDeadEvent(buildCmd(reqVO)));
    }

    @GetMapping("/health")
    @Operation(summary = "获得 Outbox 健康监测快照（积压 / DEAD / 最长等待 / 失败率 / 过期租约 + 越阈清单）")
    @PreAuthorize("@ss.hasPermission('infra:job:query')")
    public CommonResult<OutboxHealthRespVO> getOutboxHealth() {
        return success(BeanUtils.toBean(healthMonitor.snapshot(), OutboxHealthRespVO.class));
    }

    /**
     * 从安全上下文与链路构建恢复命令：operator 身份服务端注入（非请求体），杜绝伪造。
     *
     * <p>{@code operatorId} 缺失（未认证）不在 controller 拦截，交由服务层记 {@code ACCESS_DENIED} 独立事务留痕后拒绝
     * （纵深防御，循 {@code ConfigChangeRecorder} DENIED 范式）；{@code operatorType} 固定 {@code ADMIN}（后台恢复控制台）。
     */
    private OutboxRecoveryCmd buildCmd(OutboxRecoveryReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String traceId = TracerUtils.getTraceId();
        return OutboxRecoveryCmd.builder()
                .eventId(reqVO.getEventId())
                .reason(reqVO.getReason())
                .operatorType(AuditEventMessage.ActorType.ADMIN.name())
                .operatorId(loginUserId != null ? String.valueOf(loginUserId) : null)
                .traceId(traceId == null || traceId.isEmpty() ? null : traceId)
                .build();
    }

}
