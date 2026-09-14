package cn.zszj.module.system.controller.admin.audit;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.controller.admin.audit.vo.AuditEventPageReqVO;
import cn.zszj.module.system.controller.admin.audit.vo.AuditEventRespVO;
import cn.zszj.module.system.dal.dataobject.audit.AuditEventDO;
import cn.zszj.module.system.service.audit.AuditEventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计事件管理（ZS-AUDIT-002）——只读查询 + 受控清理。
 *
 * <p>审计历史只追加、不可改写：本 Controller 不提供任何单条改写端点；
 * 清理仅按保留期批量执行并经 AuditPort 写操作记录。非系统租户强制本租户范围（service 层控制）。
 */
@Tag(name = "管理后台 - 审计事件（ZS-AUDIT-002）")
@RestController
@RequestMapping("/system/audit-event")
public class AuditEventController {

    /** 保留期下限（天）：防止误配置过短导致审计历史丢失 */
    private static final int MIN_RETENTION_DAYS = 90;

    @Resource
    private AuditEventQueryService auditEventQueryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询审计事件（租户范围隔离）")
    @PreAuthorize("@ss.hasPermission('system:audit:query')")
    public CommonResult<PageResult<AuditEventRespVO>> getAuditEventPage(@Valid AuditEventPageReqVO pageVO) {
        PageResult<AuditEventDO> page = auditEventQueryService.getAuditEventPage(pageVO);
        List<AuditEventRespVO> list = page.getList().stream()
                .map(AuditEventRespVO::from).collect(Collectors.toList());
        return CommonResult.success(new PageResult<>(list, page.getTotal()));
    }

    @PostMapping("/clean")
    @Operation(summary = "受控清理过期审计事件", description = "按保留期批量清理并经 AuditPort 写操作记录")
    @PreAuthorize("@ss.hasPermission('system:audit:clean')")
    public CommonResult<Integer> cleanAuditEvents(
            @Parameter(description = "保留期天数（下限 " + MIN_RETENTION_DAYS + "）", required = true)
            @RequestParam("retentionDays")
            @Min(value = MIN_RETENTION_DAYS, message = "保留期不得少于下限")
            int retentionDays) {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(auditEventQueryService.cleanExpiredEvents(operatorId, retentionDays));
    }

}
