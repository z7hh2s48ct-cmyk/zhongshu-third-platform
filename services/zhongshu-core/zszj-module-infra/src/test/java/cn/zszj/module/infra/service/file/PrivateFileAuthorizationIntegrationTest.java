package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-001/004 批次执行入口（docs/03 §B04）：私有文件授权矩阵整合测试（H2）。
 *
 * <p>整合 B04 授权主阵容：PUBLIC 匿名可读；PRIVATE 必须登录且「owner（含同租户）」或
 * 「同租户 infra:file:query」之一；跨技术租户任何身份均拒绝——私有附件不得凭 URL/ID/会话 ID
 * 越过平台授权（正文取流的票据协议看守见 {@code FileDeliveryTicketTest}）。</p>
 */
@Import({FileServiceImpl.class, cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class PrivateFileAuthorizationIntegrationTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;
    @Resource
    private FileMapper fileMapper;

    @MockitoBean
    private FileConfigService fileConfigService;
    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            when(masterClient.getContentRange(anyString(), anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(inv -> objectStore.get(inv.getArgument(0, String.class)));
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== 授权矩阵 ==========

    @Test
    public void publicFile_anonymous_readable() {
        FileDO file = seedFile("PUBLIC", 101L, 1L);
        assertDoesNotThrow(() -> fileService.validateFileReadable(file, null), "公开素材匿名可读");
        assertDoesNotThrow(() -> fileService.validateFileReadable(file, user(999L, 2L)),
                "公开素材对任意（含跨租户）主体可读");
    }

    @Test
    public void privateFile_anonymous_denied() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, null), "私有附件禁止匿名读取");
    }

    @Test
    public void privateFile_ownerSameTenant_readable() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        assertDoesNotThrow(() -> fileService.validateFileReadable(file, user(101L, 1L)), "所有者本人（同租户）可读");
    }

    @Test
    public void privateFile_ownerCrossTenant_denied() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, user(101L, 2L)),
                "owner 匹配必须同技术租户，跨租户 owner 不放大范围");
    }

    @Test
    public void privateFile_nonOwnerSameTenant_withoutPermission_denied() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        when(permissionCommonApi.hasAnyPermissions(102L, "infra:file:query")).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, user(102L, 1L)),
                "同租户非所有者且无 infra:file:query → 拒绝");
    }

    @Test
    public void privateFile_nonOwnerSameTenant_withPermission_readable() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);
        assertDoesNotThrow(() -> fileService.validateFileReadable(file, user(103L, 1L)),
                "同租户持 infra:file:query 管理面可读");
    }

    @Test
    public void privateFile_nonOwnerCrossTenant_withPermission_denied() {
        FileDO file = seedFile("PRIVATE", 101L, 1L);
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, user(103L, 2L)),
                "跨技术租户即使持权限也拒绝（租户谓词优先）");
    }

    // ========== 造数辅助 ==========

    private FileDO seedFile(String scope, Long ownerUserId, Long tenantId) {
        String path = "asset/" + scope + "-" + randomString() + ".bin";
        byte[] content = ("auth-" + scope).getBytes();
        objectStore.put(path, content);
        FileDO file = FileDO.builder()
                .configId(1L).name("asset.bin").path(path)
                .url("https://oss.example.com/" + path)
                .type("application/octet-stream").size((long) content.length)
                .fileHash(DigestUtil.sha256Hex(content))
                .ownerUserId(ownerUserId).scope(scope)
                .build();
        file.setTenantId(tenantId);
        fileMapper.insert(file);
        return fileMapper.selectById(file.getId());
    }

    private static LoginUser user(Long id, Long tenantId) {
        return new LoginUser().setId(id).setTenantId(tenantId);
    }

}
