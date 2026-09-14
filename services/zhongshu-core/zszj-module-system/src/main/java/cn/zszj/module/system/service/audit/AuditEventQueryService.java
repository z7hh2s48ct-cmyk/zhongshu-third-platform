package cn.zszj.module.system.service.audit;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.controller.admin.audit.vo.AuditEventPageReqVO;
import cn.zszj.module.system.dal.dataobject.audit.AuditEventDO;
import cn.zszj.module.system.dal.mysql.audit.AuditEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_CLEAN_FAILED;

/**
 * 审计事件查询与受控清理（ZS-AUDIT-002）。
 *
 * <ul>
 *   <li><b>查询范围</b>——非系统租户强制 tenant_id = 当前租户（普通账号不能查看其他范围审计）；
 *       系统租户（tenant_id = 0）可跨范围按 trace/对象追查；无 update/delete 业务方法，
 *       审计历史只追加不可改写；</li>
 *   <li><b>受控清理</b>——按保留期清理过期事件，清理动作本身经 AuditPort 写操作记录
 *       （含操作者/保留期/条数），满足“清理有范围、审批依据与操作记录”。</li>
 * </ul>
 *
 * @author ZS-AUDIT-002
 */
@Service
@Validated
@Slf4j
public class AuditEventQueryService {

    /** 系统租户编号（可跨范围追查） */
    private static final Long SYSTEM_TENANT_ID = 0L;

    @Resource
    private AuditEventMapper auditEventMapper;

    @Resource
    private AuditPort auditPort;

    /**
     * 分页查询审计事件——租户范围强制隔离（非系统租户只见本租户）。
     */
    public PageResult<AuditEventDO> getAuditEventPage(AuditEventPageReqVO pageReqVO) {
        PageResult<AuditEventDO> page = doGetAuditEventPage(pageReqVO);
        // codex r0 P1：读取时兜底脱敏——覆盖升级前已落库的未净化历史记录（写时脱敏仅覆盖新增数据）
        page.getList().forEach(event -> {
            if (event.getDetail() != null && event.getDetail().contains("\"")) {
                event.setDetail(LogSanitizeUtils.sanitizeJson(event.getDetail()));
            }
        });
        return page;
    }

    private PageResult<AuditEventDO> doGetAuditEventPage(AuditEventPageReqVO pageReqVO) {
        return auditEventMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<AuditEventDO>()                .eqIfPresent(AuditEventDO::getEventType, pageReqVO.getEventType())
                .eqIfPresent(AuditEventDO::getActorId, pageReqVO.getActorId())
                .eqIfPresent(AuditEventDO::getBizType, pageReqVO.getBizType())
                .eqIfPresent(AuditEventDO::getBizId, pageReqVO.getBizId())
                .eqIfPresent(AuditEventDO::getTraceId, pageReqVO.getTraceId())
                .eqIfPresent(AuditEventDO::getResult, pageReqVO.getResult())
                .eqIfPresent(AuditEventDO::getTenantId, resolveTenantScope())
                .betweenIfPresent(AuditEventDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AuditEventDO::getId));
    }

    /**
     * 受控清理：删除 create_time 早于截止时间的过期审计事件。
     * 清理动作本身经 AuditPort 写操作记录（SUCCESS 随事务提交）。
     *
     * @param operatorUserId 操作者用户编号（写入审计记录 actor_id）
     * @param retentionDays  保留期天数
     * @return 清理条数
     */
    @Transactional(rollbackFor = Exception.class) // codex r0 P1：DELETE 与 AUDIT_CLEANED 同事务
    public int cleanExpiredEvents(Long operatorUserId, int retentionDays) {
        if (retentionDays <= 0) {
            throw exception(AUDIT_EVENT_CLEAN_FAILED, "保留期必须为正数天");
        }
        // codex r0 P1：清理仅限系统租户（防止普通租户物理删除全表历史）
        Long currentTenant = TenantContextHolder.getTenantId();
        if (!Objects.equals(currentTenant, SYSTEM_TENANT_ID)) {
            throw exception(AUDIT_EVENT_CLEAN_FAILED, "仅系统租户可执行审计清理");
        }
        LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays);
        int deleted = auditEventMapper.deleteExpiredBefore(deadline);
        log.info("[cleanExpiredEvents][ZS-AUDIT-002 审计事件受控清理：操作者({}) 保留期({}天) 截止({}) 清理({})条]",
                operatorUserId, retentionDays, deadline, deleted);
        if (deleted > 0) {
            // 清理动作本身写审计操作记录（审批依据=保留期合同；操作者与范围记录于事件字段）
            AuditEventMessage message = AuditEventMessage.builder()
                    .eventType("AUDIT_CLEANED")
                    .actorType(AuditEventMessage.ActorType.WORKER)
                    .actorId(String.valueOf(operatorUserId))
                    .action("清理过期审计事件")
                    .reason("保留期 " + retentionDays + " 天")
                    .result(AuditEventMessage.AuditResult.SUCCESS)
                    .detail(java.util.Map.of("deleted", deleted, "deadline", deadline.toString()))
                    .idempotencyKey("audit-clean-" + operatorUserId)
                    .build();
            auditPort.record(message);
        }
        return deleted;
    }

    /**
     * 租户范围解析：非系统租户强制限定当前租户；系统租户返回 null（不限定，可跨范围追查）。
     */
    private Long resolveTenantScope() {
        // codex r0 P2：缺失租户上下文必须拒绝查询（null scope 会被 eqIfPresent 跳过导致全表暴露）
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(AUDIT_EVENT_CLEAN_FAILED, "租户上下文缺失，拒绝审计查询");
        }
        if (Objects.equals(tenantId, SYSTEM_TENANT_ID)) {
            return null; // 系统租户可跨范围追查
        }
        return tenantId;
    }

}
