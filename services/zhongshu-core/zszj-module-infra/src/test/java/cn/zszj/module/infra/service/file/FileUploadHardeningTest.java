package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.test.core.util.RandomUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DANGEROUS_CONTENT;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_PUBLIC_TYPE_NOT_ALLOWED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_SIZE_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_TYPE_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-002：上传用途、内容校验和唯一对象键测试。
 *
 * <p>验收：同日同名并发上传不覆盖旧文件；扩展名/MIME 伪装、超限、危险扩展名被拒；
 * 正常附件可回读且内容散列一致；转 PUBLIC 类型白名单校验。
 *
 * @author ZS-FILE-002
 */
@Import({FileServiceImpl.class,
        cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class FileUploadHardeningTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private FileProperties fileProperties;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.zszj.module.infra.service.file.FileConfigService fileConfigService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        cn.zszj.module.infra.framework.file.core.client.FileClient masterClient =
                mock(cn.zszj.module.infra.framework.file.core.client.FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            // 模拟存储：按 path 保存内容（并发下同 path 会互相覆盖——正是唯一键要防的场景）
            java.util.Map<String, byte[]> store = new java.util.concurrent.ConcurrentHashMap<>();
            when(masterClient.upload(any(), anyString(), anyString())).thenAnswer(inv -> {
                store.put(inv.getArgument(1, String.class), inv.getArgument(0, byte[].class));
                return "https://oss.example.com/" + inv.getArgument(1, String.class);
            });
            when(masterClient.getContent(anyString())).thenAnswer(inv ->
                    store.get(inv.getArgument(0, String.class)));
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(org.mockito.ArgumentMatchers.anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== ① 同日同名并发上传不覆盖 ==========

    @Test
    public void concurrentSameNameUpload_noOverwrite_bothReadable() throws Exception {
        byte[] contentA = "content-A-unique".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] contentB = "content-B-unique".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        AtomicReference<String> urlA = new AtomicReference<>();
        AtomicReference<String> urlB = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch bothDone = new CountDownLatch(2);

        Runnable uploadA = () -> {
            try {
                TenantContextHolder.setTenantId(1L); // ThreadLocal 不跨线程，逐线程设置
                urlA.set(fileService.createFile(contentA, "report.txt", null, "text/plain"));
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                TenantContextHolder.clear();
                bothDone.countDown();
            }
        };
        Thread t1 = new Thread(uploadA);
        Thread t2 = new Thread(() -> {
            try {
                TenantContextHolder.setTenantId(1L);
                urlB.set(fileService.createFile(contentB, "report.txt", null, "text/plain"));
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                TenantContextHolder.clear();
                bothDone.countDown();
            }
        });
        t1.start();
        t2.start();
        assertTrue(bothDone.await(10, TimeUnit.SECONDS), "并发上传超时");
        if (error.get() != null) {
            throw new RuntimeException("并发上传失败", error.get());
        }

        // 同名并发：对象键必须不同（服务端唯一键），互不覆盖
        assertNotEquals(urlA.get(), urlB.get(), "同日同名并发上传必须生成不同对象键");

        // 两个文件记录均存在且回读内容与各自上传内容一致
        FileDO recordA = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, urlA.get())).get(0);
        FileDO recordB = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, urlB.get())).get(0);
        byte[] readA = fileService.getFileContent(recordA.getConfigId(), recordA.getPath());
        byte[] readB = fileService.getFileContent(recordB.getConfigId(), recordB.getPath());
        // 内容散列一致性（SHA-256 对比）
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(contentA),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(readA), "文件 A 回读散列必须一致");
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(contentB),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(readB), "文件 B 回读散列必须一致");
    }

    // ========== ② 伪装拒绝 ==========

    @Test
    public void createFile_pngContentWithTxtExtension_rejectedAsDisguise() {
        // PNG 内容伪装 .txt 扩展名
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createFile(PNG_MAGIC, "disguise.txt", null, "text/plain"));
        assertEquals(FILE_TYPE_MISMATCH.getCode(), ex.getCode(), "扩展名/MIME 伪装必须被拒");
        assertTrue(fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getName, "disguise.txt")).isEmpty(), "伪装文件不得落库");
    }

    @Test
    public void createFile_callerTypeNotTrusted_detectionWins() {
        // 调用方声明 image/png 但名字是 .txt、内容是 PNG——探测 image/png 与扩展名 txt 不一致 → 拒绝
        // （证明调用方提供 type 不能绕过服务端探测）
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createFile(PNG_MAGIC, "disguise.txt", null, "image/png"));
        assertEquals(FILE_TYPE_MISMATCH.getCode(), ex.getCode());

        // 反向：声明与实际一致（.png 名 + PNG 内容声明 image/png）→ 放行
        String url = fileService.createFile(PNG_MAGIC, "real.png", null, "image/png");
        assertTrue(url != null && !url.isEmpty());
    }

    // ========== ③ 危险扩展名隔离 ==========

    @Test
    public void createFile_dangerousExtension_rejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createFile("malicious".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "payload.exe", null, null));
        assertEquals(FILE_DANGEROUS_CONTENT.getCode(), ex.getCode());
        assertTrue(fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getName, "payload.exe")).isEmpty(), "危险文件不得落库");
    }

    @Test
    public void createFile_doubleExtension_dangerousFinalRejected() {
        // 双扩展名 a.txt.exe → 最终扩展名 exe 命中黑名单
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createFile("x".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "invoice.txt.exe", null, null));
        assertEquals(FILE_DANGEROUS_CONTENT.getCode(), ex.getCode());
    }

    // ========== ④ 大小限额 ==========

    @Test
    public void createFile_oversize_rejected() {
        long originalMax = fileProperties.getMaxSize();
        try {
            fileProperties.setMaxSize(8L); // 8 字节上限，便于测试
            byte[] big = "0123456789ABCDEF".getBytes(java.nio.charset.StandardCharsets.UTF_8); // 16 字节
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fileService.createFile(big, "big.txt", null, "text/plain"));
            assertEquals(FILE_SIZE_EXCEED.getCode(), ex.getCode());
            assertTrue(fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                    .eq(FileDO::getName, "big.txt")).isEmpty(), "超限文件不得落库");
        } finally {
            fileProperties.setMaxSize(originalMax);
        }
    }

    // ========== ⑤ 正常附件回读散列一致 ==========

    @Test
    public void createFile_normalAttachment_readbackHashMatches() throws Exception {
        byte[] content = "正常附件内容-zhongshu-2026".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String url = fileService.createFile(content, "normal.txt", null, "text/plain");

        FileDO record = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, url)).get(0);
        byte[] read = fileService.getFileContent(record.getConfigId(), record.getPath());
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(read), "正常附件回读散列必须一致");
    }

    // ========== ⑥ 转 PUBLIC 类型白名单 ==========

    @Test
    public void updateFileScope_public_typeNotInWhitelist_rejected() {
        byte[] content = "plain".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String url = fileService.createFile(content, "note.txt", null, "text/plain");
        FileDO record = fileMapper.selectList(new cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, url)).get(0);

        // text/plain 在默认白名单 → 先验证可转
        fileService.updateFileScope(record.getId(), "PUBLIC");
        assertEquals("PUBLIC", fileMapper.selectById(record.getId()).getScope());

        // 非白名单类型：伪造 octet-stream 记录（octet-stream 不在白名单）
        FileDO binary = FileDO.builder()
                .configId(1L).name("bin.dat").path("bin/" + RandomUtils.randomString() + ".dat")
                .url("https://oss.example.com/bin.dat").type("application/octet-stream").size(10L)
                .ownerUserId(1L).scope("PRIVATE").build();
        binary.setTenantId(1L);
        fileMapper.insert(binary);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.updateFileScope(binary.getId(), "PUBLIC"));
        assertEquals(FILE_PUBLIC_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        assertEquals("PRIVATE", fileMapper.selectById(binary.getId()).getScope(), "非白名单类型不得转 PUBLIC");
    }
}
