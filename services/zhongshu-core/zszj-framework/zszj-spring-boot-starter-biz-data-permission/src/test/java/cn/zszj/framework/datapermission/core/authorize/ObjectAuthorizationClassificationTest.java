package cn.zszj.framework.datapermission.core.authorize;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ObjectAuthorizationService} 字段等级目录裁决的单元测试（ZS-PERM-003.B，D-12 敏感业务字段目录）。
 *
 * <p>覆盖（D-12 §2/§3/§5）：分级裁剪（员工 F1：F0/F1 清晰、F2 脱敏可见、F3 拒绝输出；负责人 F2：F3 拒绝；
 * 平台 F3：全量清晰）/ 域级默认（未编目字段按域默认定级）/ maskedFields 合同（authorizedFields 非 null 时
 * 恒非 null、⊆ authorizedFields）/ 执行侧同核（脱敏字段读取放行、F3 拒绝）/ visit 请求以授权记录为唯一权威
 * （显式放行字段清晰输出，机制不叠加收紧或放松）/ 注册校验 fail-fast（候选字段未定级、空白编目键、null 编目）。
 *
 * <p>技术夹具为测试内中性对象与 provider，不代表任何商业域；「客户姓名/手机号」等命名仅为对齐 D-12 §4.1
 * 字段形状的测试语义，不构成业务域接入（逐域接入随领域模块落地）。
 *
 * @author ZS-PERM-003.B
 */
class ObjectAuthorizationClassificationTest extends BaseMockitoUnitTest {

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @Mock
    private OrgDataPermissionChecker orgDataPermissionChecker;

    @Mock
    private PermissionCommonApi permissionApi;

    // ========== 技术夹具（中性演示 provider，字段命名仅对齐 D-12 §4.1 形状） ==========

    /** 显式编目 provider：四个字段覆盖 F0～F3 全部等级，无域级默认 */
    static class ClassifiedDemoProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "classified-demo";
        }

        @Override
        public Collection<String> getCandidateActions() {
            return List.of("cd:query");
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("name", "followUp", "phone", "cost");
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            Map<String, FieldLevel> levels = new HashMap<>();
            levels.put("name", FieldLevel.F0);
            levels.put("followUp", FieldLevel.F1);
            levels.put("phone", FieldLevel.F2);
            levels.put("cost", FieldLevel.F3);
            return levels;
        }

    }

    /** 域级默认 provider：仅编目 alpha=F1，未编目的 beta 落域默认 F3 */
    static class DomainDefaultProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "domain-default";
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("alpha", "beta");
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            Map<String, FieldLevel> levels = new HashMap<>();
            levels.put("alpha", FieldLevel.F1);
            return levels;
        }

        @Override
        public FieldLevel getDomainDefaultLevel() {
            return FieldLevel.F3;
        }

    }

    /** 候选字段未定级且无域级默认（注册校验应 fail-fast） */
    static class UnclassifiedFieldProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "unclassified-field";
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("mystery");
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            return Map.of();
        }

    }

    /** 编目键含空白（注册校验应 fail-fast） */
    static class BlankCatalogKeyProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "blank-catalog-key";
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            Map<String, FieldLevel> levels = new HashMap<>();
            levels.put(" ", FieldLevel.F0);
            return levels;
        }

    }

    /** 编目映射为 null（注册校验应 fail-fast） */
    static class NullCatalogProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "null-catalog";
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            return null;
        }

    }

    /** 编目值为 null（注册校验应 fail-fast） */
    static class NullLevelValueProvider implements ClassifiedObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "null-level-value";
        }

        @Override
        public Map<String, FieldLevel> getFieldLevels() {
            Map<String, FieldLevel> levels = new HashMap<>();
            levels.put("name", null);
            return levels;
        }

    }

    // ========== 测试辅助 ==========

    private ObjectAuthorizationService newService(ObjectAuthorizationProvider... providers) {
        return new ObjectAuthorizationService(securityFrameworkService, orgDataPermissionChecker,
                new FieldLevelScopeResolver(permissionApi), List.of(providers));
    }

    private ObjectAuthorizationRequest classifiedRequest() {
        return ObjectAuthorizationRequest.of("classified-demo", 100L, null);
    }

    private LoginUser adminUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1L);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        return loginUser;
    }

    private OrgDataPermissionRespDTO accessDto(boolean all, Set<Long> ledOrgIds) {
        OrgDataPermissionRespDTO dto = new OrgDataPermissionRespDTO();
        dto.setAll(all);
        dto.setSelf(true);
        dto.setOrgIds(new java.util.HashSet<>());
        dto.setLedOrgIds(new java.util.HashSet<>(ledOrgIds));
        return dto;
    }

    /** 员工访问者：非平台、非目标组织负责人 → 解析上限 F1 */
    private void stubEmployee(MockedStatic<SecurityFrameworkUtils> ms) {
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
        ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(false);
        when(permissionApi.getOrgDataPermission(1L)).thenReturn(accessDto(false, Set.of()));
    }

    /** 负责人访问者：ledOrgIds 命中对象所属组织 → 解析上限 F2 */
    private void stubLeader(MockedStatic<SecurityFrameworkUtils> ms) {
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
        ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(false);
        when(permissionApi.getOrgDataPermission(1L)).thenReturn(accessDto(false, Set.of(100L)));
    }

    /** 平台访问者：all=true → 解析上限 F3 */
    private void stubPlatform(MockedStatic<SecurityFrameworkUtils> ms) {
        ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser());
        ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(false);
        when(permissionApi.getOrgDataPermission(1L)).thenReturn(accessDto(true, Set.of()));
    }

    /** visit 授权范围（循 SEC-001.B 先例：authorized + whole-tenant + 指定动作/字段） */
    private cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO visitScopeDto(
            Set<String> allowedFields) {
        cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO dto =
                new cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO();
        dto.setAuthorized(true);
        dto.setTargetTenantId(2L);
        dto.setTargetOrgIds(null);
        dto.setAllowedActions(Set.of("cd:query"));
        dto.setAllowedFields(allowedFields);
        return dto;
    }

    // ========== 注册校验（构造 fail-fast，配置错误不静默） ==========

    @Test // 1. 候选字段未编目且无域级默认 → 构造 fail-fast
    void constructor_candidateFieldWithoutLevel_failFast() {
        assertThrows(IllegalStateException.class,
                () -> newService(new UnclassifiedFieldProvider()));
    }

    @Test // 2. 编目键空白 → 构造 fail-fast（对齐 .A 空白 objectType 口径）
    void constructor_blankCatalogKey_failFast() {
        assertThrows(IllegalStateException.class,
                () -> newService(new BlankCatalogKeyProvider()));
    }

    @Test // 3. 编目映射 null → 构造 fail-fast
    void constructor_nullCatalog_failFast() {
        assertThrows(IllegalStateException.class,
                () -> newService(new NullCatalogProvider()));
    }

    @Test // 4. 编目值为 null → 构造 fail-fast
    void constructor_nullLevelValue_failFast() {
        assertThrows(IllegalStateException.class,
                () -> newService(new NullLevelValueProvider()));
    }

    @Test // 5. 域级默认覆盖未编目候选 → 构造通过
    void constructor_domainDefaultCoversUncataloged_passes() {
        assertDoesNotThrow(() -> newService(new DomainDefaultProvider()));
    }

    // ========== 分级裁剪（非 visit，D-12 §3 默认映射） ==========

    @Test // 6. 员工（F1）：F0/F1 清晰可见；F2 脱敏可见（maskedFields）；F3 拒绝输出
    void authorize_employee_levelTrimming() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(classifiedRequest());
            assertEquals(Set.of("name", "followUp", "phone"), resp.getAuthorizedFields(),
                    "F3 字段不得出现在授权字段（隐藏字段不能经详情/批量旁路）");
            assertEquals(Set.of("phone"), resp.getMaskedFields(),
                    "F2 对低于 F2 的访问者默认脱敏可见（D-12 §5.2 非整字段隐藏）");
        }
    }

    @Test // 7. 负责人（F2）：F2 清晰可见（无脱敏）；F3 拒绝输出
    void authorize_orgLeader_levelTrimming() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubLeader(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(classifiedRequest());
            assertEquals(Set.of("name", "followUp", "phone"), resp.getAuthorizedFields());
            assertNotNull(resp.getMaskedFields());
            assertTrue(resp.getMaskedFields().isEmpty(), "负责人 F2 清晰可见，无脱敏字段");
        }
    }

    @Test // 8. 平台（F3）：全量候选清晰可见
    void authorize_platform_fullVisibility() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPlatform(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(classifiedRequest());
            assertEquals(Set.of("name", "followUp", "phone", "cost"), resp.getAuthorizedFields());
            assertTrue(resp.getMaskedFields().isEmpty());
        }
    }

    @Test // 9. 域级默认：未编目字段落域默认 F3（员工不可见；平台可见）
    void authorize_domainDefault_uncatalogedField() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new DomainDefaultProvider());

            ObjectAuthorizationRespDTO employee = service.authorize(
                    ObjectAuthorizationRequest.of("domain-default", 100L, null));
            assertEquals(Set.of("alpha"), employee.getAuthorizedFields(),
                    "未编目字段按域默认 F3 → 员工不可见");
        }
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPlatform(ms);
            ObjectAuthorizationService service = newService(new DomainDefaultProvider());

            ObjectAuthorizationRespDTO platform = service.authorize(
                    ObjectAuthorizationRequest.of("domain-default", 100L, null));
            assertEquals(Set.of("alpha", "beta"), platform.getAuthorizedFields());
        }
    }

    @Test // 10. maskedFields 合同：⊆ authorizedFields；对象维不可见时不产生任何输出（先对象门后字段维）
    void authorize_maskedFields_subsetOfAuthorized() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(classifiedRequest());
            assertTrue(resp.getAuthorizedFields().containsAll(resp.getMaskedFields()));
        }
        // 对象门：checker 判不可见 → 拒绝（分级裁剪不绕过对象维）
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(false);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());
            assertServiceException(() -> service.authorize(classifiedRequest()), FORBIDDEN);
        }
    }

    @Test // 11. 未分级域零变化（.A 合同保持）：plain provider 非 visit 全候选、maskedFields 恒非 null 空集
    void authorize_plainProvider_zeroChange_maskedEmpty() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new PlainProviderFixture());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("plain-object", 100L, null));
            assertEquals(Set.of("fieldA", "fieldB"), resp.getAuthorizedFields(),
                    "未分级 provider 维持 .A 非 visit 全候选行为");
            assertNotNull(resp.getMaskedFields());
            assertTrue(resp.getMaskedFields().isEmpty());
        }
    }

    @Test // 12. 未启用字段级（无候选）：authorizedFields=null 且 maskedFields=null（两维 null 对齐）
    void authorize_noCandidates_bothNull() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new NoFieldsProviderFixture());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("no-fields", 100L, null));
            assertNull(resp.getAuthorizedFields());
            assertNull(resp.getMaskedFields());
        }
    }

    // ========== 执行侧（输出=执行同核：脱敏字段读取放行、F3 拒绝） ==========

    @Test // 13. 员工请求脱敏 F2 字段 → 放行（脱敏可见是合法读取）；请求 F3 字段 → FORBIDDEN
    void checkFields_employee_maskedPasses_confidentialForbidden() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubEmployee(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            assertDoesNotThrow(() -> service.checkFieldsAllowed(classifiedRequest(), Set.of("phone")),
                    "F2 脱敏字段对员工是合法读取（须以脱敏形态返回）");
            assertServiceException(() -> service.checkFieldsAllowed(classifiedRequest(), Set.of("cost")),
                    FORBIDDEN);
            assertServiceException(() -> service.checkFieldsAllowed(
                    classifiedRequest(), Set.of("name", "cost")), FORBIDDEN,
                    "混入 F3 字段整批拒绝（隐藏字段不能经批量旁路）");
        }
    }

    @Test // 14. 平台请求 F3 字段 → 放行（执行侧与输出同核）
    void checkFields_platform_confidentialPasses() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            when(orgDataPermissionChecker.isObjectVisible(100L, null)).thenReturn(true);
            stubPlatform(ms);
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            assertDoesNotThrow(() -> service.checkFieldsAllowed(
                    classifiedRequest(), Set.of("name", "phone", "cost")));
        }
    }

    // ========== visit 请求（授权记录为唯一权威，机制不叠加收紧或放松） ==========

    @Test // 15. visit：显式放行的 F2/F3 字段清晰输出（授权记录已显式列名，无脱敏叠加）
    void authorize_visit_explicitGrant_clearOutput() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser();
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(true);
            CrossOrgVisitScopeHolder.setScope(visitScopeDto(Set.of("name", "phone", "cost")));
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("classified-demo", 5000L, null));
            assertEquals(Set.of("name", "phone", "cost"), resp.getAuthorizedFields(),
                    "visit 字段维以授权记录为唯一权威（D-12 §3：F2/F3 须经授权记录字段维显式放行）");
            assertNotNull(resp.getMaskedFields());
            assertTrue(resp.getMaskedFields().isEmpty(), "显式放行=清晰输出，无脱敏叠加");
            verifyNoInteractions(permissionApi);
        }
    }

    @Test // 16. visit：授权记录未列出的字段不输出（逐字段收敛不变）
    void authorize_visit_grantLimited_intersect() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser();
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            ms.when(SecurityFrameworkUtils::skipPermissionCheck).thenReturn(true);
            CrossOrgVisitScopeHolder.setScope(visitScopeDto(Set.of("name")));
            ObjectAuthorizationService service = newService(new ClassifiedDemoProvider());

            ObjectAuthorizationRespDTO resp = service.authorize(
                    ObjectAuthorizationRequest.of("classified-demo", 5000L, null));
            assertEquals(Set.of("name"), resp.getAuthorizedFields());
        }
    }

    // ========== 辅助夹具（复用 .A 测试形状） ==========

    /** 未分级 provider（.A 零变化路径） */
    static class PlainProviderFixture implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "plain-object";
        }

        @Override
        public Collection<String> getCandidateFields() {
            return List.of("fieldA", "fieldB");
        }

    }

    /** 不声明候选字段的 provider */
    static class NoFieldsProviderFixture implements ObjectAuthorizationProvider {

        @Override
        public String getObjectType() {
            return "no-fields";
        }

    }

}
