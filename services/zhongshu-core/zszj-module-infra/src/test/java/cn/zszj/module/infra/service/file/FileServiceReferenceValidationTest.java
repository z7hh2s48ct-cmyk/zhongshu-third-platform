package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_REFERENCE_INVALID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 业务模块私有附件引用校验测试（ZS-FC-001 PILOT-REQ-001「未授权组织不能读取附件」的写入侧闭环，H2）。
 *
 * <p>背景：业务模块（首链申请）只存 {@code fileId} 引用（接入合同 §1.10），读取侧由 FILE-001.A/B 与
 * FILE-004.A 票据交付把关；但写入侧此前不校验被引用文件，调用方可挂接不存在、他租户/他组织、
 * 公开、导出件或不可读的文件编号。{@link FileServiceImpl#validatePrivateFileReferences} 补齐：
 * <ol>
 *     <li>引用必须<b>存在于当前租户</b>、{@code scope=PRIVATE}（公开文件可匿名读取，违背私有附件语义）、
 *         非 DELETING 中间态、非导出件（导出件有保留期自动清理，被引用即悬空）；</li>
 *     <li>引用方必须对该文件<b>具备读取资格</b>——与 {@link FileServiceImpl#validateFileReadable} 同一裁决
 *         （所有者本人 / 持 infra:file:query 的同租户管理员，且过 org 轴对象门），不另造授权口径；</li>
 *     <li>一律以同一错误码 {@code FILE_REFERENCE_INVALID} 拒绝（不存在/越权不可区分，不泄露他租户/他组织文件存在性），
 *         整批 fail-closed——任一非法则整批拒绝；无登录主体按拒绝处理。</li>
 * </ol>
 *
 * @author ZS-FC-001
 */
@Import({FileServiceImpl.class, cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
@DisplayName("FileService - 私有附件引用校验（ZS-FC-001 写入侧）")
public class FileServiceReferenceValidationTest extends BaseDbUnitTest {

    private static final Long TENANT_HOME = 1L;
    private static final Long TENANT_OTHER = 2L;
    private static final Long ORG_A = 9001L;
    private static final Long ORG_B = 9002L;
    private static final Long OWNER = 101L;

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
        when(orgDataPermissionChecker.isObjectVisible(anyLong(), anyLong())).thenReturn(true);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    // ========== 合法引用 ==========

    @Test
    @DisplayName("自己上传的 PRIVATE 文件（所有者 + 组织范围内）→ 通过")
    public void owner_privateFile_passes() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(List.of(file.getId())));
    }

    @Test
    @DisplayName("非所有者但持 infra:file:query 的同租户管理员、组织范围内 → 通过（与读取授权同一裁决）")
    public void tenantManager_privateFile_passes() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        login(103L, TENANT_HOME, ORG_A);
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);

        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(List.of(file.getId())));
    }

    @Test
    @DisplayName("历史无组织归属（organizationId=null）的 PRIVATE 文件，所有者本人 → 通过（tenant 轴治理）")
    public void legacyNoOrgFile_owner_passes() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", null, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(List.of(file.getId())));
    }

    @Test
    @DisplayName("重复编号与多文件合法 → 通过")
    public void duplicatesAndMultiple_pass() {
        FileDO f1 = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        FileDO f2 = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(
                List.of(f1.getId(), f2.getId(), f1.getId())));
    }

    @Test
    @DisplayName("空集合 / null → 无引用，直接通过（不要求登录上下文）")
    public void emptyOrNull_noop() {
        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(null));
        assertDoesNotThrow(() -> fileService.validatePrivateFileReferences(List.of()));
    }

    // ========== 拒绝路径（统一 FILE_REFERENCE_INVALID） ==========

    @Test
    @DisplayName("文件不存在 → 拒绝")
    public void notExists_rejected() {
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(987654321L));
    }

    @Test
    @DisplayName("PUBLIC 公开文件 → 拒绝（公开文件匿名可读，违背私有附件语义）")
    public void publicFile_rejected() {
        FileDO file = seed(TENANT_HOME, "PUBLIC", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("DELETING 删除中间态文件 → 拒绝（即将被清除，引用即悬空）")
    public void deletingFile_rejected() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        file.setStatus(FileDO.STATUS_DELETING);
        fileMapper.updateById(file);
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("导出件（purpose=export，有保留期自动清理）→ 拒绝")
    public void exportFile_rejected() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        file.setPurpose(FileDO.PURPOSE_EXPORT);
        file.setRetentionExpireTime(LocalDateTime.now().plusDays(1));
        fileMapper.updateById(file);
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("他租户文件 → 拒绝（与不存在同码，不泄露跨租户存在性）")
    public void crossTenantFile_rejected() {
        FileDO file = seed(TENANT_OTHER, "PRIVATE", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("同租户异组织、不在授权组织范围 → 拒绝（org 轴独立于 tenant 轴；所有者本人也不凌驾组织排除）")
    public void otherOrgOutOfScope_rejected() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_B, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);
        when(orgDataPermissionChecker.isObjectVisible(ORG_B, OWNER)).thenReturn(false);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("同租户非所有者且无 infra:file:query → 拒绝（不能引用自己读不了的文件）")
    public void nonOwnerWithoutPermission_rejected() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        login(105L, TENANT_HOME, ORG_A);
        when(permissionCommonApi.hasAnyPermissions(105L, "infra:file:query")).thenReturn(false);

        assertRejected(List.of(file.getId()));
    }

    @Test
    @DisplayName("整批 fail-closed：合法文件夹带一个非法编号 → 整批拒绝")
    public void mixedBatch_rejectedAsWhole() {
        FileDO good = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        FileDO bad = seed(TENANT_HOME, "PUBLIC", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        assertRejected(List.of(good.getId(), bad.getId()));
        assertRejected(List.of(bad.getId(), good.getId()));
    }

    @Test
    @DisplayName("集合含 null 元素 → 拒绝")
    public void nullElement_rejected() {
        FileDO good = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);
        login(OWNER, TENANT_HOME, ORG_A);

        List<Long> ids = new ArrayList<>(Arrays.asList(good.getId(), null));
        assertRejected(ids);
    }

    @Test
    @DisplayName("无登录主体（匿名/系统上下文）→ 拒绝（fail-closed，不得凭空引用私有文件）")
    public void noLoginUser_rejected() {
        FileDO file = seed(TENANT_HOME, "PRIVATE", ORG_A, OWNER);

        assertRejected(List.of(file.getId()));
    }

    // ========== 夹具 ==========

    private void assertRejected(List<Long> ids) {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.validatePrivateFileReferences(ids));
        assertEquals(FILE_REFERENCE_INVALID.getCode(), ex.getCode(),
                "非法引用一律以统一错误码拒绝（不存在/越权不可区分）");
    }

    private FileDO seed(Long tenantId, String scope, Long organizationId, Long ownerUserId) {
        FileDO file = FileDO.builder()
                .configId(1L).name(randomString() + ".pdf")
                .path("test/" + randomString() + ".pdf")
                .url("https://oss.example.com/" + randomString() + ".pdf")
                .type("application/pdf").size(100L)
                .ownerUserId(ownerUserId).scope(scope)
                .organizationId(organizationId)
                .status(FileDO.STATUS_PUBLISHED)
                .build();
        file.setTenantId(tenantId);
        fileMapper.insert(file);
        return file;
    }

    private void login(Long userId, Long tenantId, Long orgId) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(tenantId);
        if (orgId != null) {
            user.setInfo(new HashMap<>(Map.of(LoginUser.INFO_KEY_ORG_ID, String.valueOf(orgId))));
        }
        SecurityFrameworkUtils.setLoginUser(user, new MockHttpServletRequest());
    }

}
