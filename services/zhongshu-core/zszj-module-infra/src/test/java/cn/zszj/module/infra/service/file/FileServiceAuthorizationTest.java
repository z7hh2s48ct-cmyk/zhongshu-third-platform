package cn.zszj.module.infra.service.file;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.infra.enums.file.FileScopeEnum;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;


import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SCOPE_INVALID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-FILE-001.A：文件归属与统一读取授权测试（技术账号/tenant，H2）。
 *
 * <p>合同：①上传默认 PRIVATE 且记录 tenant_id + owner；②PUBLIC 匿名可读、PRIVATE 匿名拒绝；
 * ③PRIVATE 跨租户登录拒绝、同租户登录放行；④批量删除混入越权/不存在 id 整批拒绝且零删除；
 * ⑤update-scope 显式调整（合法/非法）。
 *
 * @author ZS-FILE-001.A
 */
@Import(FileServiceImpl.class)
public class FileServiceAuthorizationTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;

    @Resource
    private FileMapper fileMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.zszj.module.infra.service.file.FileConfigService fileConfigService;


    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
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
    }

    // ========== ① 写路径：默认 PRIVATE + 租户 + 归属 ==========

    @Test
    public void createFile_defaultsPrivateWithTenantAndOwner() {
        String url = fileService.createFile("test-content".getBytes(java.nio.charset.StandardCharsets.UTF_8), "test.txt", null, "text/plain");

        FileDO file = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, url)).get(0);
        assertEquals("PRIVATE", file.getScope(),
                "新上传必须默认 PRIVATE（历史迁移不能默认全部公开）");
        assertEquals(1L, file.getTenantId(), "必须记录上传时的技术租户");
        assertEquals(0L, file.getOwnerUserId().longValue(), "匿名/系统上下文 owner 为 0");
    }

    @Test
    public void createFileByReqVO_defaultsPrivate() {
        FileCreateReqVO reqVO = new FileCreateReqVO();
        reqVO.setName("presigned.txt");
        reqVO.setPath("presigned/" + randomString() + ".txt");
        reqVO.setUrl("https://oss.example.com/" + randomString() + ".txt");
        reqVO.setType("text/plain");
        reqVO.setSize(100L);

        Long id = fileService.createFile(reqVO);

        FileDO file = fileMapper.selectById(id);
        assertEquals("PRIVATE", file.getScope(), "presigned 直传记录同样默认 PRIVATE");
        assertEquals(1L, file.getTenantId());
    }

    // ========== ②③ 读取授权 ==========

    @Test
    public void validateReadable_public_anonymousAllowed() {
        FileDO file = seedFile(1L, "PUBLIC");

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, null),
                "公开素材按批准用途匿名可读");
    }

    @Test
    public void validateReadable_private_anonymousRejected() {
        FileDO file = seedFile(1L, "PRIVATE");

        assertThrows(AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, null),
                "私有附件必须关闭匿名旁路");
    }

    @Test
    public void validateReadable_private_otherTenantRejected() {
        FileDO file = seedFile(1L, "PRIVATE");
        LoginUser otherTenantUser = new LoginUser().setId(201L).setTenantId(2L);

        assertThrows(AccessDeniedException.class,
                () -> fileService.validateFileReadable(file, otherTenantUser),
                "另一技术租户登录不得读取他租户私有附件");
    }

    @Test
    public void validateReadable_private_sameTenantAllowed() {
        FileDO file = seedFile(1L, "PRIVATE");
        LoginUser sameTenantUser = new LoginUser().setId(101L).setTenantId(1L);

        assertDoesNotThrow(() -> fileService.validateFileReadable(file, sameTenantUser));
    }

    // ========== ④ 批量删除混入越权 ==========

    @Test
    public void deleteFileList_mixedForeignIds_rejectsAllAndDeletesNothing() throws Exception {
        FileDO mine = seedFile(1L, "PRIVATE");
        seedFile(2L, "PRIVATE"); // 他租户文件（租户过滤下查不到）

        List<Long> ids = List.of(mine.getId(), 999_999L); // 混入他租户查不到的 id
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.deleteFileList(ids));
        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode(), "批量混入越权/不存在 id 必须整批拒绝");

        assertEquals(mine.getId(), fileMapper.selectById(mine.getId()).getId(),
                "整批拒绝后合法文件也不得被删除");
    }

    // ========== ⑤ update-scope ==========

    @Test
    public void updateFileScope_toPublic_anonymousBecomesReadable() {
        FileDO file = seedFile(1L, "PRIVATE");

        fileService.updateFileScope(file.getId(), "PUBLIC");

        FileDO after = fileMapper.selectById(file.getId());
        assertEquals("PUBLIC", after.getScope());
        assertDoesNotThrow(() -> fileService.validateFileReadable(after, null),
                "显式公开后匿名可读");
    }

    @Test
    public void updateFileScope_invalidScope_rejected() {
        FileDO file = seedFile(1L, "PRIVATE");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.updateFileScope(file.getId(), "FRIENDS"));
        assertEquals(FILE_SCOPE_INVALID.getCode(), ex.getCode());
        assertEquals("PRIVATE", fileMapper.selectById(file.getId()).getScope(), "非法 scope 不得落库");
    }

    // ========== 造数辅助 ==========

    private FileDO seedFile(Long tenantId, String scope) {
        FileDO file = FileDO.builder()
                .configId(1L).name(randomString() + ".txt")
                .path("test/" + randomString() + ".txt")
                .url("https://oss.example.com/" + randomString() + ".txt")
                .type("text/plain").size(100L)
                .ownerUserId(101L).scope(scope)
                .build();
        file.setTenantId(tenantId);
        fileMapper.insert(file);
        return file;
    }
}
