package cn.zszj.framework.datapermission.core.authorize;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.security.core.service.SecurityFrameworkServiceImpl;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ObjectAuthorizationService} 的单元测试（ZS-PERM-003.A）。
 *
 * <p>覆盖：扩展点路由、空白 objectType 与重复注册 fail-fast / 对象维（非 visit 走 org 轴 checker；visit 请求经
 * {@link CrossOrgVisitScopeHolder} 收敛，无 scope fail-closed 且不落 home checker）/ 动作维（RBAC 交集、
 * 全拒=空集、与 D6 收敛共用真实现）/ 状态维钩子 / 字段维（未声明候选=未启用 null、非 visit 零变化、
 * visit 交集且 allowedFields 缺失 fail-closed、空白候选过滤）/ 执行侧共用（输出=执行同核、伪造动作不生效、
 * 撤权与状态变更后重新校验、未注册 fail-closed、隐藏字段拒绝、空字段请求=不校验字段维、非 visit 候选内通过）。
 *
 * <p>技术夹具为测试内中性对象与 provider，不代表任何商业域（字段等级目录 F0～F3 归 ZS-PERM-003.B，
 * 本卡不定义任何商业字段名/等级）。
 *
 * @author ZS-PERM-003.A
 */
class ObjectAuthorizationServiceTest extends BaseMockitoUnitTest {

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @Mock
    private OrgDataPermissionChecker orgDataPermissionChecker;

    @Mock
    private PermissionCommonApi permissionApi;

    // ========== 技术夹具（中性演示对象 / provider，不代表任何商业域） ==========

    /** 演示业务对象（仅含状态字段，供状态维钩子） */
    static class DemoObject {

        private String status;

        DemoObject(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        void setStatus(String status) {
            this.status = status;
        }

    }

    /** 演示 provider：动作 demo:query/demo:update/demo:delete；字段 fieldA/fieldB/fieldC；归档对象剔除 demo:update */
    static class DemoProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "demo-object";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("demo:query", "demo:update", "demo:delete");
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("fieldA", "fieldB", "fieldC");
        }

        @Override
        public boolean isActionAllowedInStatus(String action, Object object) {
            // 状态维：已归档对象剔除更新动作；其余（含 object=null）不施加状态约束
            if (object instanceof DemoObject demo && "ARCHIVED".equals(demo.getStatus())) {
                return !"demo:update".equals(action);
            }
            return true;
        }

    }

    /** 不声明候选字段的 provider（未启用字段级输出） */
    static class NoFieldsProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "no-fields";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("nf:query");
        }

    }

    /** 默认钩子 provider（路由隔离演示） */
    static class PlainProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "plain-object";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("plain:query");
        }

    }

    /** 候选字段含 null/空白（CodeReview P3-1：字段维过滤与动作维 blank 口径对齐） */
    static class BlankFieldsProvider implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "blank-fields";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("bf:query");
        }

        @Override
        public Collection<String> getCandidateFields() {
            return Arrays.asList("fieldA", null, " ", "fieldB");
        }

    }

    /** 可配 objectType 的 provider（CodeReview P3-2：空白 objectType 构造 fail-fast） */
    static class BlankTypeProvider implements ObjectAuthorizationProvider {

        private final String objectType;

        BlankTypeProvider(String objectType) {
            this.objectType = objectType;
        }

        @Override
        public String getObjectType() {
            return objectType;
        }

    }

    // ========== 测试辅助 ==========

    private ObjectAuthorizationService newService(ObjectAuthorizationProvider... providers) {
        return new ObjectAuthorizationService(securityFrameworkService, orgDataPermissionChecker, List.of(providers));
    }

    private ObjectAuthorizationRequest demoRequest() {
        return ObjectAuthorizationRequest.of("demo-object", 100L, null);
    }

    private ObjectAuthorizationRequest demoRequest(DemoObject object) {
        return ObjectAuthorizationRequest.of("demo-object", 100L, null, object);
    }

    /** demo/no-fields/plain/blank-fields 全量候选动作参数域（逐候选查询的完整覆盖域） */
    private static final List<String> ALL_CANDIDATE_ACTIONS = List.of(
            "demo:query", "demo:update", "demo:delete", "nf:query", "plain:query", "bf:query");

    /**
     * 声明授权表：覆盖全量候选动作参数域（授权动作 strict 放行，其余显式拒绝）。
     *
     * <p>实现逐候选查询 {@code hasAnyPermissions}，若仅 stub 部分参数，STRICT_STUBS 会以
     * {@code PotentialStubbingProblem}（参数不匹配）中断；本表将未授权动作显式声明为 lenient
     * 拒绝，保证查询参数域无空洞且不引入 unused 误报。
     */
    private void stubPermissions(String... allowedActions) {
        Set<String> allowed = Set.of(allowedActions);
        for (String action : ALL_CANDIDATE_ACTIONS) {
            if (allowed.contains(action)) {
                when(securityFrameworkService.hasAnyPermissions(action)).thenReturn(true);
            } else {
                lenient().when(securityFrameworkService.hasAnyPermissions(action)).thenReturn(false);
            }
        }
    }

    /** 模拟一次跨租户访问请求（循 SEC-001.B 先例：有登录用户 + skipPermissionCheck=true） */
    private void stubVisit(MockedStatic<SecurityFrameworkUtils> ms) {
        LoginUser loginUser = randomPojo(LoginUser.class, o -> o.setId(1L));
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
        ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(true);
    }

    private CrossOrgVisitDecisionDTO visitScope(Set<Long> targetOrgIds, Set<String> allowedActions,
                                                Set<String> allowedFields) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(true);
        dto.setTargetTenantId(2L);
        dto.setTargetOrgIds(targetOrgIds);
        dto.setAllowedActions(allowedActions);
        dto.setAllowedFields(allowedFields);
        return dto;
    }

    // ========== 扩展点（路由 / 唯一性 / 未接入零变化） ==========

    @Test // 1. 未注册对象类型：返回 null（未接入=零变化），不抛异常
    void authorize_unknownObjectType_returnsNull() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            ObjectAuthorizationService service = newService(new DemoProvider());
            assertNull(service.authorize(ObjectAuthorizationRequest.of("unknown-type", 100L, null)));
        }
    }

    @Test // 2. 重复注册同一对象类型：构造 fail-fast
    void constructor_duplicateObjectType_failFast() {
        assertThrows(IllegalStateException.class, () -> newService(new DemoProvider(), new DemoProvider()));
    }

    @Test // 3. 多个 provider 按对象类型路由，互不串扰
    void authorize_routesByObjectType() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query", "plain:query");
            ObjectAuthorizationService service = newService(new DemoProvider(), new PlainProvider());

            ObjectAuthorizationRespDTO demo = service.authorize(demoRequest());
            ObjectAuthorizationRespDTO plain = service.authorize(
                    ObjectAuthorizationRequest.of("plain-object", 100L, null));
            assertEquals(Set.of("demo:query"), demo.getAllowedActions());
            assertEquals(Set.of("plain:query"), plain.getAllowedActions());
            assertNull(plain.getAuthorizedFields()); // plain 未声明候选字段 → 未启用字段级
        }
    }

    // ========== 对象维（非 visit：org 轴 checker） ==========

    @Test // 4. 对象门（非 visit）：checker 判不可见 → 拒绝
    void authorize_objectNotVisibleByChecker_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(200L, 999L)).thenReturn(false);
            ObjectAuthorizationService service = newService(new DemoProvider());
            assertServiceException(() -> service.authorize(
                    ObjectAuthorizationRequest.of("demo-object", 200L, 999L)), FORBIDDEN);
        }
    }

    @Test // 5. 对象门（非 visit）：checker 判可见 → 正常输出（动作全授权）
    void authorize_objectVisibleByChecker_passes() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query", "demo:update", "demo:delete");
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertEquals(Set.of("demo:query", "demo:update", "demo:delete"), resp.getAllowedActions());
        }
    }

    // ========== 对象维（visit 请求：D7 收敛，无 scope fail-closed） ==========

    @Test // 6. visit 请求：对象不在授权 targetOrgIds → 拒绝
    void authorize_visitRequest_objectOutOfScope_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(Set.of(4000L), Set.of("demo:query"), null));
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertServiceException(() -> service.authorize(
                    ObjectAuthorizationRequest.of("demo-object", 5000L, null)), FORBIDDEN);
        }
    }

    @Test // 7. visit 请求：whole-tenant（targetOrgIds=null）→ 对象维放行
    void authorize_visitRequest_wholeTenant_passes() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:query"), null));
            stubPermissions("demo:query");
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("demo-object", 5000L, null));
            assertEquals(Set.of("demo:query"), resp.getAllowedActions());
        }
    }

    @Test // 8. visit 请求但无 scope（异常态）→ fail-closed 拒绝，且不落 home 组织 checker
    void authorize_visitRequest_noScope_failClosed() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms); // 有登录用户、跨租户访问信号，但未 setScope
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertServiceException(() -> service.authorize(demoRequest()), FORBIDDEN);
            verify(orgDataPermissionChecker, never()).isObjectVisible(any(), any());
        }
    }

    // ========== 动作维（RBAC 交集 / 空集语义 / D6 收敛共用 / 状态钩子） ==========

    @Test // 9. 动作维：候选 ∩ RBAC（未授权动作被裁剪）
    void authorize_actions_intersectRbac() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query", "demo:update"); // demo:delete 未授权
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertEquals(Set.of("demo:query", "demo:update"), resp.getAllowedActions());
        }
    }

    @Test // 10. 动作维：全部无权限 → 空集（非 null）
    void authorize_actions_allDenied_emptySetNotNull() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertNotNull(resp.getAllowedActions());
            assertTrue(resp.getAllowedActions().isEmpty());
        }
    }

    @Test // 11. visit 请求动作维：与 D6 收敛共用真实现（候选 ∩ visit.allowedActions，且不走 RBAC）
    void authorize_actions_visitRequest_convergedByVisitScope() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:query"), null));
            // 真 SecurityFrameworkServiceImpl：visit 请求下 hasAnyPermissions → CrossOrgVisitScopeHolder 收敛
            ObjectAuthorizationService service = new ObjectAuthorizationService(
                    new SecurityFrameworkServiceImpl(permissionApi), orgDataPermissionChecker,
                    List.of(new DemoProvider()));

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertEquals(Set.of("demo:query"), resp.getAllowedActions());
            verifyNoInteractions(permissionApi); // visit 收敛不落 RBAC
        }
    }

    @Test // 12. 状态维钩子：已归档对象剔除 demo:update；非归档不限制
    void authorize_actions_statusHook_applied() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query", "demo:update", "demo:delete");
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO archived = service.authorize(demoRequest(new DemoObject("ARCHIVED")));
            assertEquals(Set.of("demo:query", "demo:delete"), archived.getAllowedActions());
            ObjectAuthorizationRespDTO active = service.authorize(demoRequest(new DemoObject("ACTIVE")));
            assertEquals(Set.of("demo:query", "demo:update", "demo:delete"), active.getAllowedActions());
        }
    }

    // ========== 字段维（未启用 null / 非 visit 零变化 / visit 交集 fail-closed） ==========

    @Test // 13. 字段维：未声明候选字段 → authorizedFields=null（未启用字段级输出）
    void authorize_fields_candidatesEmpty_returnsNull() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("nf:query");
            ObjectAuthorizationService service = newService(new NoFieldsProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("no-fields", 100L, null));
            assertNull(resp.getAuthorizedFields());
        }
    }

    @Test // 14. 字段维：非 visit → 全候选（.A 零变化，字段等级目录归 ZS-PERM-003.B）
    void authorize_fields_nonVisit_allCandidates() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertEquals(Set.of("fieldA", "fieldB", "fieldC"), resp.getAuthorizedFields());
        }
    }

    @Test // 15. 字段维：visit 请求逐字段 ∩ visit.allowedFields；allowedFields 缺失 → fail-closed 空集
    void authorize_fields_visitRequest_intersectAllowedFields() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:query"), Set.of("fieldA")));
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            assertEquals(Set.of("fieldA"), resp.getAuthorizedFields());

            // 对偶：visit.allowedFields 缺失（null）→ 逐字段 fail-closed → 空集
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:query"), null));
            ObjectAuthorizationRespDTO resp2 = service.authorize(demoRequest());
            assertNotNull(resp2.getAuthorizedFields());
            assertTrue(resp2.getAuthorizedFields().isEmpty());
        }
    }

    // ========== 执行侧（输出=执行同核 / 伪造不生效 / 变更后重新校验 / 隐藏字段拒绝） ==========

    @Test // 16. 执行侧：输出内动作 → 通过
    void checkAction_allowed_passes() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query");
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertDoesNotThrow(() -> service.checkActionAllowed(demoRequest(), "demo:query"));
        }
    }

    @Test // 17. 执行侧：伪造动作（不在输出集合）→ 拒绝；blank 动作 → 拒绝
    void checkAction_forgedAction_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query");
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertServiceException(() -> service.checkActionAllowed(demoRequest(), "demo:delete"), FORBIDDEN);
            assertServiceException(() -> service.checkActionAllowed(demoRequest(), " "), FORBIDDEN);
        }
    }

    @Test // 18. 执行侧：撤权后立即拒绝（每次调用实时重新裁决）
    void checkAction_afterPermissionChange_revalidates() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:update");
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertDoesNotThrow(() -> service.checkActionAllowed(demoRequest(), "demo:update"));
            // 撤权（同一上下文内权限变更）
            when(securityFrameworkService.hasAnyPermissions("demo:update")).thenReturn(false);
            assertServiceException(() -> service.checkActionAllowed(demoRequest(), "demo:update"), FORBIDDEN);
        }
    }

    @Test // 19. 执行侧：对象状态变更后重新校验
    void checkAction_afterStatusChange_revalidates() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:update");
            ObjectAuthorizationService service = newService(new DemoProvider());

            DemoObject demo = new DemoObject("ACTIVE");
            ObjectAuthorizationRequest request = demoRequest(demo);
            assertDoesNotThrow(() -> service.checkActionAllowed(request, "demo:update"));
            // 状态流转：ACTIVE → ARCHIVED
            demo.setStatus("ARCHIVED");
            assertServiceException(() -> service.checkActionAllowed(request, "demo:update"), FORBIDDEN);
        }
    }

    @Test // 20. 执行侧字段维：隐藏字段拒绝 / 允许字段通过 / 空请求=不校验字段维
    void checkFields_hiddenField_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            stubVisit(ms);
            CrossOrgVisitScopeHolder.setScope(visitScope(null, Set.of("demo:query"), Set.of("fieldA")));
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertServiceException(() -> service.checkFieldsAllowed(demoRequest(), Set.of("fieldB")), FORBIDDEN);
            assertDoesNotThrow(() -> service.checkFieldsAllowed(demoRequest(), Set.of("fieldA")));
            assertDoesNotThrow(() -> service.checkFieldsAllowed(demoRequest(), Collections.emptySet()));
        }
    }

    @Test // 21. 执行侧字段维：未启用字段级的域 + 非空字段请求 → fail-closed 拒绝（空请求仍通过）
    void checkFields_notEnabled_forbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            ObjectAuthorizationService service = newService(new NoFieldsProvider());
            ObjectAuthorizationRequest request = ObjectAuthorizationRequest.of("no-fields", 100L, null);

            assertDoesNotThrow(() -> service.checkFieldsAllowed(request, Collections.emptySet()));
            assertServiceException(() -> service.checkFieldsAllowed(request, Set.of("fieldA")), FORBIDDEN);
        }
    }

    @Test // 22. 执行侧：未注册对象类型 → fail-closed 拒绝
    void checkAction_unknownObjectType_forbidden() {
        ObjectAuthorizationService service = newService(new DemoProvider());
        assertServiceException(() -> service.checkActionAllowed(
                ObjectAuthorizationRequest.of("unknown-type", 100L, null), "any:action"), FORBIDDEN);
    }

    @Test // 23. 输出=执行共用同一裁决核心（正向逐项通过 + 被裁动作拒绝）
    void outputEqualsExecution_sharedCore() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions("demo:query"); // update/delete 未授权
            ObjectAuthorizationService service = newService(new DemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(demoRequest());
            // 输出内动作逐项执行通过
            for (String action : resp.getAllowedActions()) {
                assertDoesNotThrow(() -> service.checkActionAllowed(demoRequest(), action));
            }
            // 候选内但被裁的动作执行拒绝（与输出裁剪一致）
            assertServiceException(() -> service.checkActionAllowed(demoRequest(), "demo:update"), FORBIDDEN);
        }
    }

    // ========== CodeReview P3 补强（blank 口径对齐 / 构造校验 / 执行侧分支补全） ==========

    @Test // 24. 字段维：候选字段含 null/空白 → 过滤（对齐动作维 blank 口径，P3-1）
    void authorize_fields_blankCandidates_filtered() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPermissions();
            ObjectAuthorizationService service = newService(new BlankFieldsProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("blank-fields", 100L, null));
            assertEquals(Set.of("fieldA", "fieldB"), resp.getAuthorizedFields());
        }
    }

    @Test // 25. 空白/空 objectType：构造 fail-fast（null 键不得静默注册，P3-2）
    void constructor_blankObjectType_failFast() {
        assertThrows(IllegalStateException.class, () -> newService(new BlankTypeProvider(null)));
        assertThrows(IllegalStateException.class, () -> newService(new BlankTypeProvider(" ")));
    }

    @Test // 26. 执行侧字段维：未注册对象类型 → fail-closed 拒绝（P3-3）
    void checkFields_unknownObjectType_forbidden() {
        ObjectAuthorizationService service = newService(new DemoProvider());

        assertServiceException(() -> service.checkFieldsAllowed(
                ObjectAuthorizationRequest.of("unknown-type", 100L, null), Set.of("fieldA")), FORBIDDEN);
    }

    @Test // 27. 执行侧字段维（非 visit）：候选内字段通过 / 候选外字段拒绝（P3-3）
    void checkFields_nonVisit_candidatesPass() {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            ObjectAuthorizationService service = newService(new DemoProvider());

            assertDoesNotThrow(() -> service.checkFieldsAllowed(demoRequest(), Set.of("fieldA", "fieldC")));
            assertServiceException(() -> service.checkFieldsAllowed(demoRequest(), Set.of("fieldD")), FORBIDDEN);
        }
    }

}
