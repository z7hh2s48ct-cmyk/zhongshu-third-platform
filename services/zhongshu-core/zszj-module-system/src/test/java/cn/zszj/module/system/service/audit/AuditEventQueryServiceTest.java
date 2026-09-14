package cn.zszj.module.system.service.audit;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.controller.admin.audit.vo.AuditEventPageReqVO;
import cn.zszj.module.system.dal.dataobject.audit.AuditEventDO;
import cn.zszj.module.system.dal.mysql.audit.AuditEventMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;

import static cn.zszj.module.system.enums.ErrorCodeConstants.AUDIT_EVENT_CLEAN_FAILED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-AUDIT-002：审计读取范围隔离、写时脱敏与受控清理测试。
 *
 * @author ZS-AUDIT-002
 */
@Import({AuditEventQueryService.class, cn.zszj.module.system.framework.audit.core.JdbcAuditPort.class})
public class AuditEventQueryServiceTest extends BaseDbUnitTest {

    @Resource
    private AuditEventQueryService auditEventQueryService;

    @Resource
    private AuditEventMapper auditEventMapper;

    @Resource
    private AuditPort auditPort;

    @Resource
    private DataSource dataSource;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        jdbcTemplate = new JdbcTemplate(dataSource);
        // ZS-AUDIT-001 JdbcAuditPort 依赖 JdbcTemplate（H2 测试环境手动注入）
        cn.zszj.module.system.framework.audit.core.JdbcAuditPort port =
                (cn.zszj.module.system.framework.audit.core.JdbcAuditPort) auditPort;
        org.springframework.test.util.ReflectionTestUtils.setField(port, "jdbcTemplate", jdbcTemplate);
        // SecurityFrameworkUtils.getLoginUser() 走 SecurityContextHolder.getAuthentication().getPrincipal()，
        // 非 web 测试需直接设 SecurityContext 认证
        LoginUser loginUser = new LoginUser()
                .setId(100L).setUserType(cn.zszj.framework.common.enums.UserTypeEnum.ADMIN.getValue()).setTenantId(1L);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(loginUser, null,
                        java.util.Collections.emptyList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        // SecurityFrameworkUtils 内部走 RequestContextHolder.getRequest().getRemoteAddr() 等，
        // 非 web 测试需提供 ServletRequestAttributes 供框架工具类读取
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(
                        new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    private void seedEvent(Long tenantId, String eventType, String detailJson) {
        jdbcTemplate.update(
                "INSERT INTO audit_event (event_type, actor_type, actor_id, result, detail, tenant_id, trace_id)"
                        + " VALUES (?, 'ADMIN', '100', 'SUCCESS', ?, ?, 'trace-seed')",
                eventType, detailJson, tenantId);
    }

    @Test
    public void record_sensitiveDetail_sanitizedInDb() {
        Map<String, Object> detail = Map.of(
                "password", "SuperSecret123",
                "action", "密码重置");
        AuditEventMessage msg = AuditEventMessage.builder()
                .eventType("USER_PASSWORD_RESET")
                .actorType(cn.zszj.framework.common.enums.UserTypeEnum.ADMIN.getValue() != null
                        ? AuditEventMessage.ActorType.ADMIN : AuditEventMessage.ActorType.ADMIN)
                .actorId("100")
                .result(AuditEventMessage.AuditResult.SUCCESS)
                .detail(detail)
                .tenantId(1L)
                .build();
        auditPort.record(msg);

        String raw = jdbcTemplate.queryForObject(
                "SELECT detail FROM audit_event WHERE event_type = 'USER_PASSWORD_RESET' AND tenant_id = 1",
                String.class);
        assertNotNull(raw);
        assertFalse(raw.contains("SuperSecret123"), "凭据明文不得出现在审计明细");
        assertTrue(raw.length() > 0);
    }

    @Test
    public void getAuditEventPage_tenantIsolation_onlyOwnTenant() {
        seedEvent(1L, "TENANT_1_EVENT", "{}");
        seedEvent(2L, "TENANT_2_EVENT", "{}");

        TenantContextHolder.setTenantId(1L);
        AuditEventPageReqVO reqVO = new AuditEventPageReqVO();
        PageResult<AuditEventDO> page = auditEventQueryService.getAuditEventPage(reqVO);

        assertEquals(1, page.getList().size(), "租户 1 只能看到本租户审计");

        TenantContextHolder.setTenantId(2L);
        PageResult<AuditEventDO> page2 = auditEventQueryService.getAuditEventPage(reqVO);
        assertEquals(1, page2.getList().size());
        assertEquals("TENANT_2_EVENT", page2.getList().get(0).getEventType());
    }

    @Test
    public void getAuditEventPage_systemTenant_crossScopeAllowed() {
        seedEvent(1L, "T1_EVENT", "{}");
        seedEvent(2L, "T2_EVENT", "{}");

        TenantContextHolder.setTenantId(0L);
        AuditEventPageReqVO reqVO = new AuditEventPageReqVO();
        PageResult<AuditEventDO> page = auditEventQueryService.getAuditEventPage(reqVO);

        assertEquals(2, page.getList().size(), "系统租户可跨范围追查");
    }

    @Test
    public void cleanExpiredEvents_deletesOnlyExpired_andWritesAuditRecord() {
        seedEvent(1L, "OLD_EVENT", "{}");
        jdbcTemplate.update("UPDATE audit_event SET create_time = ? WHERE event_type = 'OLD_EVENT'",
                java.time.LocalDateTime.now().minusDays(400));
        seedEvent(1L, "RECENT_EVENT", "{}");

        int deleted = auditEventQueryService.cleanExpiredEvents(100L, 365);

        assertEquals(1, deleted, "只清理过期事件");
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE event_type = 'AUDIT_CLEANED'", Integer.class);
        assertEquals(1, cnt, "清理动作必须写操作记录");
    }

    @Test
    public void cleanExpiredEvents_invalidRetention_rejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> auditEventQueryService.cleanExpiredEvents(100L, 0));
        assertEquals(AUDIT_EVENT_CLEAN_FAILED.getCode(), ex.getCode());
    }
}
