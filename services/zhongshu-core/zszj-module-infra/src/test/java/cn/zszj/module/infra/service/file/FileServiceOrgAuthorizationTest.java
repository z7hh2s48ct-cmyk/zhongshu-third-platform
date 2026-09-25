package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-001.B：业务组织和对象文件策略测试（org 轴对象授权 + 获批跨组织 visit 收敛，H2）。
 *
 * <p>合同（docs/05 §16.1 line1039「按批准组织/对象授权下载，不把 tenant 简单改名」）：
 * <ol>
 *     <li>上传记录服务端签发的业务组织归属（{@code organizationId} ← {@link LoginUser#getOrgId()}）；</li>
 *     <li>常规上下文：PRIVATE 文件已归属组织时，须在调用者授权组织范围内才可读——<b>同租户异组织拒绝</b>
 *         （org 轴独立于 tenant 轴，非「tenant 改名」）；<b>本人所有权不凌驾组织排除</b>（D-09 FND-AUTH-004，
 *         转岗/离任成员不得凭 owner 访问旧组织文件）；{@code organizationId==null} 历史文件仍由 tenant 轴治理；</li>
 *     <li>获批跨组织 visit 上下文：tenant 轴（home vs target）必然失配，改由 SEC-001.B 授权范围快照按获批组织
 *         收敛对象维（whole-tenant 放行 / 限定组织须命中 / 范围外拒绝），且<b>不走本地 org 门</b>；</li>
 *     <li>PUBLIC 公开素材仍按批准用途匿名可读（org 门不施加于 PUBLIC）；</li>
 *     <li>批量删除混入越权组织文件整批拒绝、零删除（循 FILE-001.A「批量混入拒绝」语义）。</li>
 * </ol>
 *
 * <p>org 门复用框架级 {@link OrgDataPermissionChecker}（ZS-PERM-002.B 对象级 org 轴入口，DRY 不复制 D-09 谓词），
 * 以 {@code @MockitoBean} 隔离其裁决、聚焦本卡「文件域消费 org 轴 + visit 收敛」的接线正确性；visit 维用真实
 * {@link CrossOrgVisitScopeHolder}（thread-local，循 SEC-001.B/PERM-001.B 先例）。
 *
 * @author ZS-FILE-001.B
 */
@Import({FileServiceImpl.class, cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
@DisplayName("FileService - 业务组织/对象文件策略（ZS-FILE-001.B / org 轴 + visit 收敛）")
public class FileServiceOrgAuthorizationTest extends BaseDbUnitTest {

    private static final Long ORG_A = 9001L;
    private static final Long ORG_B = 9002L;
    private static final Long TENANT_HOME = 1L;
    private static final Long TENANT_TARGET = 2L;

    @Resource
    private FileServiceImpl fileService;

    @Resource
    private FileMapper fileMapper;

    @MockitoBean
    private cn.zszj.module.infra.service.file.FileConfigService fileConfigService;

    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    @MockitoBean
    private OrgDataPermissionChecker orgDataPermissionChecker;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(TENANT_HOME);
        // createFile 依赖 master FileClient 上传内容——mock 客户端记录路径即返回 URL
        cn.zszj.module.infra.framework.file.core.client.FileClient masterClient =
                org.mockito.Mockito.mock(cn.zszj.module.infra.framework.file.core.client.FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            when(masterClient.upload(any(), anyString(), anyString())).thenReturn("https://oss.example.com/mock.txt");
        } catch (Exception ignored) { }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    // ========== ① 上传归属：记录服务端签发的业务组织 ==========

    @Test
    @DisplayName("上传（字节）记录当前登录组织归属 organizationId")
    public void createFile_bytes_setsOrganizationIdFromLoginOrgContext() {
        loginThreadLocal(adminUser(101L, TENANT_HOME, ORG_A));

        String url = fileService.createFile("test-content".getBytes(StandardCharsets.UTF_8),
                "test.txt", null, "text/plain");

        FileDO file = fileMapper.selectList(new LambdaQueryWrapperX<FileDO>().eq(FileDO::getUrl, url)).get(0);
        assertEquals(ORG_A, file.getOrganizationId(), "上传须记录服务端签发的业务组织归属（org 轴数据载体）");
    }

    @Test
    @DisplayName("上传（presigned create 记录）同样记录组织归属")
    public void createFileByReqVO_setsOrganizationId() {
        loginThreadLocal(adminUser(101L, TENANT_HOME, ORG_A));
        FileCreateReqVO reqVO = new FileCreateReqVO();
        reqVO.setName("presigned.txt");
        reqVO.setPath("presigned/" + randomString() + ".txt");
        reqVO.setUrl("https://oss.example.com/" + randomString() + ".txt");
        reqVO.setType("text/plain");
        reqVO.setSize(100L);

        Long id = fileService.createFile(reqVO);

        assertEquals(ORG_A, fileMapper.selectById(id).getOrganizationId(), "presigned 记录同样落组织归属");
    }

    @Test
    @DisplayName("无组织上下文（匿名/系统/无默认任职）上传 organizationId 为 null（由 tenant 轴治理）")
    public void createFile_noOrgContext_organizationIdNull() {
        // 不设 thread-local：匿名/系统上下文
        String url = fileService.createFile("test-content".getBytes(StandardCharsets.UTF_8),
                "test.txt", null, "text/plain");

        FileDO file = fileMapper.selectList(new LambdaQueryWrapperX<FileDO>().eq(FileDO::getUrl, url)).get(0);
        assertNull(file.getOrganizationId(), "无组织上下文时 organizationId 为 null，历史/匿名文件由 tenant 轴治理");
    }

    // ========== ② 常规上下文 org 轴门（不把 tenant 简单改名） ==========

    @Test
    @DisplayName("PRIVATE 组织文件在授权组织范围内 → 可读")
    public void validateReadable_privateOrgFile_inOrgScope_allowed() {
        FileDO file = seedFile(TENANT_HOME, "PRIVATE", ORG_A, 101L);
        LoginUser manager = adminUser(103L, TENANT_HOME, ORG_A); // 非 owner，同租户管理员
        stubOrgVisible(ORG_A, true);
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, manager), "授权组织范围内的组织文件可读");
    }

    @Test
    @DisplayName("PRIVATE 组织文件越界（同租户异组织）→ 拒绝（org 轴独立于 tenant 轴）")
    public void validateReadable_privateOrgFile_outOfOrgScope_sameTenant_denied() {
        FileDO file = seedFile(TENANT_HOME, "PRIVATE", ORG_B, 101L); // 同租户 1，异组织 ORG_B
        LoginUser caller = adminUser(105L, TENANT_HOME, ORG_A);      // 同租户、非 owner
        stubOrgVisible(ORG_B, false);

        assertThrows(AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, caller),
                "同技术租户但异业务组织的文件必须拒绝——org 轴不是 tenant 改名");
    }

    @Test
    @DisplayName("本人所有权不凌驾组织排除：owner 但组织越界 → 拒绝（D-09 FND-AUTH-004）")
    public void validateReadable_privateOrgFile_ownerButOutOfOrgScope_denied() {
        FileDO file = seedFile(TENANT_HOME, "PRIVATE", ORG_B, 101L); // owner=101
        LoginUser owner = adminUser(101L, TENANT_HOME, ORG_A);       // 正是 owner，但现任组织 ORG_A
        stubOrgVisible(ORG_B, false);

        assertThrows(AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, owner),
                "转岗/离任成员不得凭 owner 访问旧组织文件（org 归属后由组织范围独占裁决）");
    }

    @Test
    @DisplayName("organizationId==null 历史文件由 tenant 轴治理，org 门不介入 → 同租户 owner 可读")
    public void validateReadable_privateOrgNullFile_sameTenantOwner_allowed_checkerNotConsulted() {
        FileDO file = seedFile(TENANT_HOME, "PRIVATE", null, 101L); // 无组织归属
        LoginUser owner = adminUser(101L, TENANT_HOME, ORG_A);

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, owner), "org==null 历史文件保 FILE-001.A tenant 轴语义");
        verify(orgDataPermissionChecker, never()).isObjectVisible(any(), any());
    }

    // ========== ③ 获批跨组织 visit 上下文收敛 ==========

    @Test
    @DisplayName("visit 上下文：文件在获批组织内 → 放行（tenant 轴 home≠target 失配仍放行）")
    public void validateReadable_visitContext_fileInApprovedOrg_allowed() {
        LoginUser visitor = visitUser(104L, TENANT_HOME, TENANT_TARGET);
        loginThreadLocal(visitor);
        CrossOrgVisitScopeHolder.setScope(visitScope(true, TENANT_TARGET, setOf(ORG_A)));
        FileDO file = seedFile(TENANT_TARGET, "PRIVATE", ORG_A, 500L); // 属 target 租户 2

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, visitor),
                "获批组织范围内的目标租户文件，visit 收敛后放行（本地 tenant 轴会因 home≠target 失配）");
    }

    @Test
    @DisplayName("visit 上下文：文件在获批组织外 → 拒绝")
    public void validateReadable_visitContext_fileOutOfApprovedOrg_denied() {
        LoginUser visitor = visitUser(104L, TENANT_HOME, TENANT_TARGET);
        loginThreadLocal(visitor);
        CrossOrgVisitScopeHolder.setScope(visitScope(true, TENANT_TARGET, setOf(ORG_A)));
        FileDO file = seedFile(TENANT_TARGET, "PRIVATE", ORG_B, 500L); // ORG_B 不在获批 {ORG_A}

        assertThrows(AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, visitor),
                "获批组织范围外的目标文件必须拒绝（visit 不放大到全租户）");
    }

    @Test
    @DisplayName("visit 上下文：whole-tenant 授权（targetOrgIds=null）→ 任意组织放行")
    public void validateReadable_visitContext_wholeTenant_anyOrg_allowed() {
        LoginUser visitor = visitUser(104L, TENANT_HOME, TENANT_TARGET);
        loginThreadLocal(visitor);
        CrossOrgVisitScopeHolder.setScope(visitScope(true, TENANT_TARGET, null)); // whole-tenant
        FileDO file = seedFile(TENANT_TARGET, "PRIVATE", ORG_B, 500L);

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, visitor), "whole-tenant 授权放行目标租户内任意组织文件");
    }

    @Test
    @DisplayName("visit 上下文由获批范围收敛，不走本地 org 门（OrgDataPermissionChecker 不被调用）")
    public void validateReadable_visitContext_localOrgCheckerBypassed() {
        LoginUser visitor = visitUser(104L, TENANT_HOME, TENANT_TARGET);
        loginThreadLocal(visitor);
        CrossOrgVisitScopeHolder.setScope(visitScope(true, TENANT_TARGET, setOf(ORG_A)));
        FileDO file = seedFile(TENANT_TARGET, "PRIVATE", ORG_A, 500L);

        fileService.validateFileReadable(file, visitor);

        verify(orgDataPermissionChecker, never()).isObjectVisible(any(), any());
    }

    // ========== ④ PUBLIC 公开素材 ==========

    @Test
    @DisplayName("PUBLIC 组织文件匿名可读（org 门不施加于公开素材）")
    public void validateReadable_publicOrgFile_anonymousAllowed() {
        FileDO file = seedFile(TENANT_HOME, "PUBLIC", ORG_B, 101L);

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, null), "公开素材按批准用途匿名可读");
        verify(orgDataPermissionChecker, never()).isObjectVisible(any(), any());
    }

    // ========== ⑤ 批量删除混入越权组织 ==========

    @Test
    @DisplayName("批量删除混入越权组织文件 → 整批拒绝、零删除")
    public void deleteFileList_mixedCrossOrgFile_rejectsAllAndDeletesNothing() throws Exception {
        FileDO inScope = seedFile(TENANT_HOME, "PRIVATE", ORG_A, 101L);
        FileDO outScope = seedFile(TENANT_HOME, "PRIVATE", ORG_B, 101L); // 同租户、越权组织
        stubOrgVisible(ORG_A, true);
        stubOrgVisible(ORG_B, false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.deleteFileList(List.of(inScope.getId(), outScope.getId())));
        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode(), "混入越权组织文件整批拒绝（不泄露存在性，循 tenant 混入语义）");
        assertNotNull(fileMapper.selectById(inScope.getId()), "整批拒绝后范围内文件不得被删除");
        assertNotNull(fileMapper.selectById(outScope.getId()), "整批拒绝后越权文件不得被删除");
    }

    @Test
    @DisplayName("单文件删除越权组织 → 拒绝（堵住单删端点绕过批量 org 门）")
    public void deleteFile_singleOutOfOrgScope_denied() throws Exception {
        FileDO outScope = seedFile(TENANT_HOME, "PRIVATE", ORG_B, 101L); // 同租户、越权组织
        loginThreadLocal(adminUser(105L, TENANT_HOME, ORG_A));
        stubOrgVisible(ORG_B, false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.deleteFile(outScope.getId()));
        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode(),
                "单删端点越权组织文件按 FILE_NOT_EXISTS 拒绝（与 deleteFileList 批量门对齐，不泄露存在性）");
        assertNotNull(fileMapper.selectById(outScope.getId()), "越权单删不得移除记录");
    }

    // ========== 造数辅助 ==========

    private FileDO seedFile(Long tenantId, String scope, Long organizationId, Long ownerUserId) {
        FileDO file = FileDO.builder()
                .configId(1L).name(randomString() + ".txt")
                .path("test/" + randomString() + ".txt")
                .url("https://oss.example.com/" + randomString() + ".txt")
                .type("text/plain").size(100L)
                .ownerUserId(ownerUserId).scope(scope)
                .organizationId(organizationId)
                .build();
        file.setTenantId(tenantId);
        fileMapper.insert(file);
        return file;
    }

    private LoginUser adminUser(Long id, Long tenantId, Long orgId) {
        LoginUser user = new LoginUser();
        user.setId(id);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(tenantId);
        if (orgId != null) {
            user.setInfo(new HashMap<>(Map.of(LoginUser.INFO_KEY_ORG_ID, String.valueOf(orgId))));
        }
        return user;
    }

    private LoginUser visitUser(Long id, Long homeTenantId, Long targetTenantId) {
        LoginUser user = adminUser(id, homeTenantId, ORG_A);
        user.setVisitTenantId(targetTenantId);
        return user;
    }

    private void loginThreadLocal(LoginUser user) {
        SecurityFrameworkUtils.setLoginUser(user, new MockHttpServletRequest());
    }

    private CrossOrgVisitDecisionDTO visitScope(boolean authorized, Long targetTenantId, Set<Long> targetOrgIds) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(authorized);
        dto.setReason(authorized ? "AUTHORIZED" : "NO_GRANT");
        dto.setTargetTenantId(targetTenantId);
        dto.setTargetOrgIds(targetOrgIds);
        dto.setAllowedActions(new HashSet<>(List.of("infra:file:query")));
        return dto;
    }

    private void stubOrgVisible(Long orgId, boolean visible) {
        when(orgDataPermissionChecker.isObjectVisible(eq(orgId), any())).thenReturn(visible);
    }

    @SafeVarargs
    private static Set<Long> setOf(Long... values) {
        return new HashSet<>(List.of(values));
    }
}
