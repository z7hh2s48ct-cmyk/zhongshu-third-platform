package cn.zszj.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.dataobject.permission.CrossOrgVisitGrantDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.mysql.permission.CrossOrgVisitGrantMapper;
import cn.zszj.module.system.enums.permission.CrossOrgVisitDenyReasonEnum;
import cn.zszj.module.system.enums.permission.CrossOrgVisitStatusEnum;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_GRANT_NOT_EXISTS;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_TARGET_INVALID;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_VALID_WINDOW_INVALID;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_VISITOR_NOT_PLATFORM;

/**
 * 跨组织访问授权 Service 实现（ZS-SEC-001.B）。
 *
 * <p>以<b>服务端授权记录/策略</b>（{@link CrossOrgVisitGrantDO}）取代 ZS-SEC-001.A 关闭的旧越权放大链
 * （持 {@code system:tenant:visit} 即 {@code skipPermissionCheck()=true} 整体跳过功能权限）。{@link #authorizeVisit}
 * 为正向矩阵核心：10 步 fail-closed 判定序，任一维度不满足即拒绝，<b>每次判定（获批/拒绝）都落不可改写审计</b>。
 *
 * <p>D-09 一期口径：跨组织只允许<b>显式平台角色</b>（复用 {@link PermissionService#getOrgDataPermission} 的
 * {@code all=true} 判定入口——超管或 PLATFORM 类型组织任职）；平台任职过期/撤销 → {@code all=false} → 拒绝，
 * 与授权记录有效期形成双层兜底。到期自动回收 JOB + 双人审批工作流为后置边界，本实现只交付记录式受控授权。
 *
 * @author ZS-SEC-001.B
 */
@Service
@Validated
@Slf4j
public class CrossOrgVisitServiceImpl implements CrossOrgVisitService {

    /**
     * 审计业务类型：跨组织访问判定
     */
    private static final String BIZ_TYPE_VISIT = "CROSS_ORG_VISIT";
    /**
     * 审计业务类型：跨组织访问授权记录（发放/撤销）
     */
    private static final String BIZ_TYPE_GRANT = "CROSS_ORG_VISIT_GRANT";
    /**
     * 目标级粗粒度门（未指定具体动作）时的审计动作占位
     */
    private static final String ACTION_VISIT = "VISIT";

    @Resource
    private CrossOrgVisitGrantMapper crossOrgVisitGrantMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private TenantService tenantService;
    @Resource
    private OrganizationService organizationService;
    @Resource
    private AuditPort auditPort;

    @Override
    public CrossOrgVisitDecisionDTO authorizeVisit(CrossOrgVisitCheckReqDTO req) {
        // 1. 参数校验：原主体与目标租户必填；目标即原租户不是跨组织访问（无意义/可疑请求）
        if (req == null || req.getVisitorUserId() == null || req.getTargetTenantId() == null
                || Objects.equals(req.getTargetTenantId(), req.getVisitorTenantId())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.INVALID_REQUEST);
        }
        // 2. 取 (原主体, 目标租户) 的最新授权记录；无记录即拒——取代旧 system:tenant:visit 粗粒度放大
        CrossOrgVisitGrantDO grant = crossOrgVisitGrantMapper.selectLatestByVisitorAndTarget(
                req.getVisitorUserId(), req.getTargetTenantId());
        if (grant == null) {
            return deny(req, CrossOrgVisitDenyReasonEnum.NO_GRANT);
        }
        // 3. 状态：已撤销 → 撤销拒绝（精确回报，不退化为「无记录」）
        if (!CrossOrgVisitStatusEnum.isActive(grant.getStatus())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.GRANT_REVOKED);
        }
        // 4. 有效期：now 不在 [valid_from, valid_to] → 过期拒绝（valid_to null 视为无固定期限）
        LocalDateTime now = LocalDateTime.now();
        if ((grant.getValidFrom() != null && now.isBefore(grant.getValidFrom()))
                || (grant.getValidTo() != null && now.isAfter(grant.getValidTo()))) {
            return deny(req, CrossOrgVisitDenyReasonEnum.GRANT_EXPIRED);
        }
        // 5. D-09 平台角色资格：跨组织只允许显式平台角色（getOrgDataPermission.all=true = 超管/PLATFORM 任职）
        OrgDataPermissionRespDTO orgPermission = permissionService.getOrgDataPermission(req.getVisitorUserId());
        if (orgPermission == null || !Boolean.TRUE.equals(orgPermission.getAll())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.NOT_PLATFORM_ROLE);
        }
        // 6. 目标状态：目标租户不存在/停用 → 拒绝
        TenantDO targetTenant = tenantService.getTenant(req.getTargetTenantId());
        if (targetTenant == null || !CommonStatusEnum.isEnable(targetTenant.getStatus())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.TARGET_INVALID);
        }
        // 6b. 目标组织有效性：target_org_ids 非空时，在目标租户上下文逐一校验组织存在且启用
        if (CollUtil.isNotEmpty(grant.getTargetOrgIds())) {
            for (Long orgId : grant.getTargetOrgIds()) {
                OrganizationDO org = TenantUtils.execute(req.getTargetTenantId(),
                        () -> organizationService.getOrganization(orgId));
                if (org == null || !CommonStatusEnum.isEnable(org.getStatus())) {
                    return deny(req, CrossOrgVisitDenyReasonEnum.TARGET_ORG_INVALID);
                }
            }
        }
        // 7. 动作维度：req.action 非空时，必须 ∈ allowed_actions（不能自动获得所有目标动作）
        Set<String> allowedActions = toSet(grant.getAllowedActions());
        if (StrUtil.isNotBlank(req.getAction()) && !allowedActions.contains(req.getAction())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.ACTION_NOT_ALLOWED);
        }
        // 8. 对象维度：req.objectOrgId 非空且限定组织范围时，必须 ∈ target_org_ids（错对象拒绝；null=全租户组织放行）
        if (req.getObjectOrgId() != null && CollUtil.isNotEmpty(grant.getTargetOrgIds())
                && !grant.getTargetOrgIds().contains(req.getObjectOrgId())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.OBJECT_OUT_OF_SCOPE);
        }
        // 9. 字段维度：req.requestedFields 非空时，必须 ⊆ allowed_fields（错字段拒绝）
        if (CollUtil.isNotEmpty(req.getRequestedFields())
                && !toSet(grant.getAllowedFields()).containsAll(req.getRequestedFields())) {
            return deny(req, CrossOrgVisitDenyReasonEnum.FIELD_NOT_ALLOWED);
        }
        // 10. 全部通过 → 获批，返回受控范围快照（供拦截器写入 LoginUser 上下文、D6 功能权限收敛消费）
        return authorized(req, grant, allowedActions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGrant(CrossOrgVisitGrantDO grant) {
        // 发放前防御性校验：被授权访问者须为显式平台角色（与判定期 step5 双层兜底）
        OrgDataPermissionRespDTO orgPermission = permissionService.getOrgDataPermission(grant.getVisitorUserId());
        if (orgPermission == null || !Boolean.TRUE.equals(orgPermission.getAll())) {
            throw exception(CROSS_ORG_VISIT_VISITOR_NOT_PLATFORM, grant.getVisitorUserId());
        }
        // 目标租户须存在且启用
        TenantDO targetTenant = tenantService.getTenant(grant.getTargetTenantId());
        if (targetTenant == null || !CommonStatusEnum.isEnable(targetTenant.getStatus())) {
            throw exception(CROSS_ORG_VISIT_TARGET_INVALID, grant.getTargetTenantId());
        }
        // 有效期窗口合法
        if (grant.getValidFrom() != null && grant.getValidTo() != null
                && grant.getValidTo().isBefore(grant.getValidFrom())) {
            throw exception(CROSS_ORG_VISIT_VALID_WINDOW_INVALID);
        }
        grant.setStatus(CrossOrgVisitStatusEnum.ACTIVE.getStatus());
        crossOrgVisitGrantMapper.insert(grant);
        // 发放动作写审计（对象生命周期，SUCCESS 随本事务提交）
        Map<String, Object> detail = new HashMap<>();
        detail.put("visitorUserId", grant.getVisitorUserId());
        detail.put("visitorTenantId", grant.getVisitorTenantId());
        detail.put("targetTenantId", grant.getTargetTenantId());
        detail.put("targetOrgIds", grant.getTargetOrgIds());
        detail.put("allowedActions", grant.getAllowedActions());
        detail.put("validFrom", Objects.toString(grant.getValidFrom(), null));
        detail.put("validTo", Objects.toString(grant.getValidTo(), null));
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_CREATED)
                .actorType(AuditEventMessage.ActorType.ADMIN)
                .actorId(currentActorId())
                .action("GRANT")
                .bizType(BIZ_TYPE_GRANT)
                .bizId(String.valueOf(grant.getId()))
                .reason(grant.getReason())
                .result(AuditEventMessage.AuditResult.SUCCESS)
                .detail(detail)
                .tenantId(grant.getVisitorTenantId())
                .traceId(TracerUtils.getTraceId())
                .build());
        return grant.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeGrant(Long id, String reason) {
        CrossOrgVisitGrantDO grant = crossOrgVisitGrantMapper.selectById(id);
        if (grant == null) {
            throw exception(CROSS_ORG_VISIT_GRANT_NOT_EXISTS);
        }
        // 逻辑状态翻转 ACTIVE→REVOKED，保留历史行（不改写审计态）
        CrossOrgVisitGrantDO update = new CrossOrgVisitGrantDO();
        update.setId(id);
        update.setStatus(CrossOrgVisitStatusEnum.REVOKED.getStatus());
        update.setReason(reason);
        crossOrgVisitGrantMapper.updateById(update);
        // 撤销动作写审计
        Map<String, Object> detail = new HashMap<>();
        detail.put("visitorUserId", grant.getVisitorUserId());
        detail.put("targetTenantId", grant.getTargetTenantId());
        auditPort.record(AuditEventMessage.builder()
                .eventType(AuditEventTypes.OBJECT_UPDATED)
                .actorType(AuditEventMessage.ActorType.ADMIN)
                .actorId(currentActorId())
                .action("REVOKE")
                .bizType(BIZ_TYPE_GRANT)
                .bizId(String.valueOf(id))
                .reason(reason)
                .result(AuditEventMessage.AuditResult.SUCCESS)
                .detail(detail)
                .tenantId(grant.getVisitorTenantId())
                .traceId(TracerUtils.getTraceId())
                .build());
    }

    // ========== 判定收尾：审计 + 结论构造 ==========

    private CrossOrgVisitDecisionDTO deny(CrossOrgVisitCheckReqDTO req, CrossOrgVisitDenyReasonEnum reason) {
        recordVisitAudit(req, reason, false);
        return CrossOrgVisitDecisionDTO.denied(reason.name());
    }

    private CrossOrgVisitDecisionDTO authorized(CrossOrgVisitCheckReqDTO req, CrossOrgVisitGrantDO grant,
                                                Set<String> allowedActions) {
        recordVisitAudit(req, CrossOrgVisitDenyReasonEnum.AUTHORIZED, true);
        CrossOrgVisitDecisionDTO decision = new CrossOrgVisitDecisionDTO();
        decision.setAuthorized(true);
        decision.setReason(CrossOrgVisitDenyReasonEnum.AUTHORIZED.name());
        decision.setTargetTenantId(req.getTargetTenantId());
        // target_org_ids null = 目标租户内全部组织（whole-tenant 授权）
        decision.setTargetOrgIds(grant.getTargetOrgIds() == null ? null : new HashSet<>(grant.getTargetOrgIds()));
        decision.setAllowedActions(allowedActions);
        decision.setAllowedFields(toSet(grant.getAllowedFields()));
        return decision;
    }

    /**
     * 跨组织访问判定审计（GRANTED 随调用方事务 / DENIED 独立事务，由 {@link AuditPort} 内部区分）。
     * detail 只含编号/权限串/字段名，无敏感明文。req 为 null 或缺原主体时无法归属主体，跳过审计（记 warn）。
     */
    private void recordVisitAudit(CrossOrgVisitCheckReqDTO req, CrossOrgVisitDenyReasonEnum reason, boolean granted) {
        if (req == null || req.getVisitorUserId() == null) {
            log.warn("[recordVisitAudit][跨组织访问判定无法归属原主体，跳过审计：reason({})]", reason.name());
            return;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("visitorTenantId", req.getVisitorTenantId());
        detail.put("targetTenantId", req.getTargetTenantId());
        if (req.getObjectOrgId() != null) {
            detail.put("objectOrgId", req.getObjectOrgId());
        }
        if (CollUtil.isNotEmpty(req.getRequestedFields())) {
            detail.put("requestedFields", req.getRequestedFields());
        }
        auditPort.record(AuditEventMessage.builder()
                .eventType(granted ? AuditEventTypes.CROSS_ORG_VISIT_GRANTED : AuditEventTypes.CROSS_ORG_VISIT_DENIED)
                .actorType(AuditEventMessage.ActorType.ADMIN)
                .actorId(String.valueOf(req.getVisitorUserId()))
                .action(StrUtil.isNotBlank(req.getAction()) ? req.getAction() : ACTION_VISIT)
                .bizType(BIZ_TYPE_VISIT)
                .bizId(String.valueOf(req.getTargetTenantId()))
                .reason(reason.getMessage())
                .result(granted ? AuditEventMessage.AuditResult.SUCCESS : AuditEventMessage.AuditResult.DENIED)
                .detail(detail)
                .tenantId(req.getVisitorTenantId())
                .traceId(TracerUtils.getTraceId())
                .build());
    }

    private static Set<String> toSet(List<String> list) {
        return list == null ? new HashSet<>() : new HashSet<>(list);
    }

    private static String currentActorId() {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        return operatorId == null ? null : String.valueOf(operatorId);
    }

}
