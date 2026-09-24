package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.dataobject.permission.CrossOrgVisitGrantDO;
import cn.zszj.module.system.dal.dataobject.tenant.TenantDO;
import cn.zszj.module.system.dal.mysql.permission.CrossOrgVisitGrantMapper;
import cn.zszj.module.system.enums.permission.CrossOrgVisitDenyReasonEnum;
import cn.zszj.module.system.enums.permission.CrossOrgVisitStatusEnum;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_GRANT_NOT_EXISTS;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_TARGET_INVALID;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_VALID_WINDOW_INVALID;
import static cn.zszj.module.system.enums.ErrorCodeConstants.CROSS_ORG_VISIT_VISITOR_NOT_PLATFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-SEC-001.B：跨组织访问授权判定「正向矩阵」测试（真实 H2 授权记录持久化 + Mock 协作方）。
 *
 * <p>锁定 docs/05 line330-331 与 §16.1 line1014 退出条件：以<b>服务端授权记录/策略</b>取代 ZS-SEC-001.A 关闭的
 * 旧越权放大链——「正常授权有效；过期/撤销/错对象或字段拒绝；须 D-09；即使具备访问入口权限，也不能自动获得
 * 所有目标动作/敏感字段」。被测 {@link CrossOrgVisitServiceImpl#authorizeVisit} 为 10 步 fail-closed 判定序，
 * 本套件按维度逐一坐实每个拒绝理由，并断言<b>每次判定都落不可改写审计</b>（GRANTED/DENIED）。
 *
 * <p>协作方 {@link PermissionService}（D-09 平台角色资格）、{@link TenantService}（目标租户状态）、
 * {@link OrganizationService}（目标组织状态）、{@link AuditPort}（审计）走 Mock——本套件聚焦授权判定引擎语义，
 * org 轴范围派生的实时性由 {@code OrgDataScopeResolverTest}/{@code OrgDataPermissionRevocationConsistencyTest} 覆盖。
 *
 * @author ZS-SEC-001.B
 */
@Import(CrossOrgVisitServiceImpl.class)
public class CrossOrgVisitServiceImplTest extends BaseDbUnitTest {

    private static final Long VISITOR_USER_ID = 104L;
    private static final Long HOME_TENANT_ID = 1L;
    private static final Long TARGET_TENANT_ID = 2L;
    private static final Long ORG_A = 9001L;
    private static final Long ORG_B = 9002L;
    private static final String ACTION_QUERY = "system:user:query";
    private static final String ACTION_CREATE = "system:user:create";
    private static final String FIELD_MOBILE = "contactMobile";
    private static final String FIELD_EMAIL = "contactEmail";

    @Resource
    private CrossOrgVisitServiceImpl crossOrgVisitService;
    @Resource
    private CrossOrgVisitGrantMapper crossOrgVisitGrantMapper;

    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private OrganizationService organizationService;
    @MockitoBean
    private AuditPort auditPort;

    // ========== 正向：正常授权有效 ==========

    @Test
    public void testAuthorize_normalGranted() {
        // 有效授权记录（whole-tenant，动作含 query）+ 平台角色 + 启用目标
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY, ACTION_CREATE)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertTrue(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.AUTHORIZED.name(), decision.getReason());
        assertEquals(TARGET_TENANT_ID, decision.getTargetTenantId());
        assertNull(decision.getTargetOrgIds(), "whole-tenant 授权：目标组织为 null（全部组织）");
        assertTrue(decision.getAllowedActions().contains(ACTION_QUERY));
        assertTrue(decision.getAllowedActions().contains(ACTION_CREATE));
        // 获批写 GRANTED 审计（SUCCESS）
        assertVisitAudit(AuditEventTypes.CROSS_ORG_VISIT_GRANTED, AuditEventMessage.AuditResult.SUCCESS);
    }

    @Test
    public void testAuthorize_scopedOrgGrantReflectsScope() {
        // 限定组织范围的授权：目标组织快照回传，供拦截器写入上下文
        insertGrant(g -> g.targetOrgIds(List.of(ORG_A)).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        givenOrg(ORG_A, CommonStatusEnum.ENABLE);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, ORG_A, null));

        assertTrue(decision.isAuthorized());
        assertEquals(1, decision.getTargetOrgIds().size());
        assertTrue(decision.getTargetOrgIds().contains(ORG_A));
    }

    // ========== 拒绝：无授权记录（取代旧 system:tenant:visit 粗粒度放大） ==========

    @Test
    public void testAuthorize_denyNoGrant() {
        // 不插入任何授权记录：即使概念上持旧 system:tenant:visit，也须拒绝（不再放大）
        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.NO_GRANT.name(), decision.getReason());
        assertVisitAudit(AuditEventTypes.CROSS_ORG_VISIT_DENIED, AuditEventMessage.AuditResult.DENIED);
    }

    // ========== 拒绝：撤销 / 过期（有效期 + 状态维度） ==========

    @Test
    public void testAuthorize_denyRevoked() {
        insertGrant(g -> g.status(CrossOrgVisitStatusEnum.REVOKED.getStatus()).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.GRANT_REVOKED.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyExpired() {
        insertGrant(g -> g.validTo(LocalDateTime.now().minusDays(1)).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.GRANT_EXPIRED.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyNotYetValid() {
        insertGrant(g -> g.validFrom(LocalDateTime.now().plusDays(1)).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.GRANT_EXPIRED.name(), decision.getReason());
    }

    // ========== 拒绝：须 D-09（非显式平台角色） ==========

    @Test
    public void testAuthorize_denyNotPlatformRole() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(false); // 平台任职过期/撤销 → all=false

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.NOT_PLATFORM_ROLE.name(), decision.getReason());
    }

    // ========== 拒绝：停用 / 无效目标 ==========

    @Test
    public void testAuthorize_denyTargetDisabled() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.DISABLE);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.TARGET_INVALID.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyTargetNotFound() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        when(tenantService.getTenant(TARGET_TENANT_ID)).thenReturn(null);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.TARGET_INVALID.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyTargetOrgInvalid() {
        insertGrant(g -> g.targetOrgIds(List.of(ORG_A)).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        givenOrg(ORG_A, CommonStatusEnum.DISABLE); // 目标组织停用

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.TARGET_ORG_INVALID.name(), decision.getReason());
    }

    // ========== 拒绝：不能自动获得所有目标动作 / 对象 / 字段 ==========

    @Test
    public void testAuthorize_denyActionNotAllowed() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY))); // 只授 query
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_CREATE, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.ACTION_NOT_ALLOWED.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyActionWhenGrantHasNoActions() {
        // 授权记录动作为空：fail-closed，任何具体动作都不得自动获得（line331）
        insertGrant(g -> g.allowedActions(List.of()));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.ACTION_NOT_ALLOWED.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyObjectOutOfScope() {
        insertGrant(g -> g.targetOrgIds(List.of(ORG_A)).allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        givenOrg(ORG_A, CommonStatusEnum.ENABLE);

        // 请求对象属于 ORG_B，不在授权组织范围
        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, ORG_B, null));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.OBJECT_OUT_OF_SCOPE.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyFieldNotAllowed() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)).allowedFields(List.of(FIELD_MOBILE)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);

        // 请求字段含未授权的 email
        CrossOrgVisitDecisionDTO decision =
                crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, List.of(FIELD_MOBILE, FIELD_EMAIL)));

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.FIELD_NOT_ALLOWED.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_fieldsWithinScopeGranted() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)).allowedFields(List.of(FIELD_MOBILE, FIELD_EMAIL)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);

        CrossOrgVisitDecisionDTO decision =
                crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, List.of(FIELD_MOBILE)));

        assertTrue(decision.isAuthorized());
        assertTrue(decision.getAllowedFields().contains(FIELD_MOBILE));
    }

    // ========== 拒绝：非法请求（目标即原租户 / 缺原主体） ==========

    @Test
    public void testAuthorize_denyInvalidRequestSameTenant() {
        CrossOrgVisitCheckReqDTO req = req(ACTION_QUERY, null, null);
        req.setTargetTenantId(HOME_TENANT_ID); // 目标 == 原租户，非跨组织

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req);

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.INVALID_REQUEST.name(), decision.getReason());
    }

    @Test
    public void testAuthorize_denyInvalidRequestNullVisitor() {
        CrossOrgVisitCheckReqDTO req = req(ACTION_QUERY, null, null);
        req.setVisitorUserId(null);

        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req);

        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.INVALID_REQUEST.name(), decision.getReason());
    }

    // ========== 发放 / 撤销（人工，非双人审批工作流） ==========

    @Test
    public void testCreateGrant_success() {
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        CrossOrgVisitGrantDO grant = newGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));

        Long id = crossOrgVisitService.createGrant(grant);

        CrossOrgVisitGrantDO persisted = crossOrgVisitGrantMapper.selectById(id);
        assertEquals(CrossOrgVisitStatusEnum.ACTIVE.getStatus(), persisted.getStatus());
        assertEquals(TARGET_TENANT_ID, persisted.getTargetTenantId());
    }

    @Test
    public void testCreateGrant_denyVisitorNotPlatform() {
        givenPlatformRole(false);
        CrossOrgVisitGrantDO grant = newGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));

        assertServiceException(() -> crossOrgVisitService.createGrant(grant),
                CROSS_ORG_VISIT_VISITOR_NOT_PLATFORM, VISITOR_USER_ID);
    }

    @Test
    public void testCreateGrant_denyTargetInvalid() {
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.DISABLE);
        CrossOrgVisitGrantDO grant = newGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));

        assertServiceException(() -> crossOrgVisitService.createGrant(grant),
                CROSS_ORG_VISIT_TARGET_INVALID, TARGET_TENANT_ID);
    }

    @Test
    public void testCreateGrant_denyValidWindowInvalid() {
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        CrossOrgVisitGrantDO grant = newGrant(g -> g
                .validFrom(LocalDateTime.now().plusDays(2))
                .validTo(LocalDateTime.now().plusDays(1))); // valid_to 早于 valid_from

        assertServiceException(() -> crossOrgVisitService.createGrant(grant), CROSS_ORG_VISIT_VALID_WINDOW_INVALID);
    }

    @Test
    public void testRevokeGrant_thenAuthorizeDenied() {
        CrossOrgVisitGrantDO grant = insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.ENABLE);
        // 撤销前可获批
        assertTrue(crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null)).isAuthorized());

        crossOrgVisitService.revokeGrant(grant.getId(), "越权风险，人工撤销");

        assertEquals(CrossOrgVisitStatusEnum.REVOKED.getStatus(),
                crossOrgVisitGrantMapper.selectById(grant.getId()).getStatus());
        // 撤销后拒绝（上下文清理与撤销有效性由集成层夹具覆盖，此处坐实授权记录状态翻转的即时后果）
        CrossOrgVisitDecisionDTO decision = crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, null, null));
        assertFalse(decision.isAuthorized());
        assertEquals(CrossOrgVisitDenyReasonEnum.GRANT_REVOKED.name(), decision.getReason());
    }

    @Test
    public void testRevokeGrant_notExists() {
        assertServiceException(() -> crossOrgVisitService.revokeGrant(999_999L, "不存在"),
                CROSS_ORG_VISIT_GRANT_NOT_EXISTS);
    }

    // ========== 审计：每次判定都落不可改写审计 ==========

    @Test
    public void testAuthorize_auditRecordsSubjectTargetReason() {
        insertGrant(g -> g.allowedActions(List.of(ACTION_QUERY)));
        givenPlatformRole(true);
        givenTargetTenant(CommonStatusEnum.DISABLE); // 触发 TARGET_INVALID 拒绝

        crossOrgVisitService.authorizeVisit(req(ACTION_QUERY, ORG_A, List.of(FIELD_MOBILE)));

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        AuditEventMessage msg = captor.getValue();
        assertEquals(AuditEventTypes.CROSS_ORG_VISIT_DENIED, msg.getEventType());
        assertEquals(AuditEventMessage.AuditResult.DENIED, msg.getResult());
        assertEquals(String.valueOf(VISITOR_USER_ID), msg.getActorId(), "原主体留痕");
        assertEquals(String.valueOf(TARGET_TENANT_ID), msg.getBizId(), "目标留痕");
        assertEquals(ACTION_QUERY, msg.getAction());
        assertEquals(HOME_TENANT_ID, msg.getTenantId());
        assertTrue(msg.getDetail().containsKey("objectOrgId"));
        assertTrue(msg.getDetail().containsKey("requestedFields"));
    }

    // ========== 辅助 ==========

    private void assertVisitAudit(String eventType, AuditEventMessage.AuditResult result) {
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort, times(1)).record(captor.capture());
        assertEquals(eventType, captor.getValue().getEventType());
        assertEquals(result, captor.getValue().getResult());
    }

    private CrossOrgVisitCheckReqDTO req(String action, Long objectOrgId, List<String> requestedFields) {
        CrossOrgVisitCheckReqDTO req = new CrossOrgVisitCheckReqDTO();
        req.setVisitorUserId(VISITOR_USER_ID);
        req.setVisitorTenantId(HOME_TENANT_ID);
        req.setTargetTenantId(TARGET_TENANT_ID);
        req.setAction(action);
        req.setObjectOrgId(objectOrgId);
        req.setRequestedFields(requestedFields);
        return req;
    }

    private CrossOrgVisitGrantDO newGrant(Consumer<CrossOrgVisitGrantDO.CrossOrgVisitGrantDOBuilder> customizer) {
        CrossOrgVisitGrantDO.CrossOrgVisitGrantDOBuilder builder = CrossOrgVisitGrantDO.builder()
                .visitorUserId(VISITOR_USER_ID)
                .visitorTenantId(HOME_TENANT_ID)
                .targetTenantId(TARGET_TENANT_ID)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .status(CrossOrgVisitStatusEnum.ACTIVE.getStatus())
                .reason("测试授权");
        customizer.accept(builder);
        return builder.build();
    }

    private CrossOrgVisitGrantDO insertGrant(Consumer<CrossOrgVisitGrantDO.CrossOrgVisitGrantDOBuilder> customizer) {
        CrossOrgVisitGrantDO grant = newGrant(customizer);
        crossOrgVisitGrantMapper.insert(grant);
        return grant;
    }

    private void givenPlatformRole(boolean isPlatform) {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setAll(isPlatform);
        when(permissionService.getOrgDataPermission(VISITOR_USER_ID)).thenReturn(dto);
    }

    private void givenTargetTenant(CommonStatusEnum status) {
        TenantDO tenant = new TenantDO();
        tenant.setId(TARGET_TENANT_ID);
        tenant.setStatus(status.getStatus());
        when(tenantService.getTenant(TARGET_TENANT_ID)).thenReturn(tenant);
    }

    private void givenOrg(Long orgId, CommonStatusEnum status) {
        OrganizationDO org = new OrganizationDO();
        org.setId(orgId);
        org.setStatus(status.getStatus());
        when(organizationService.getOrganization(orgId)).thenReturn(org);
    }

}
