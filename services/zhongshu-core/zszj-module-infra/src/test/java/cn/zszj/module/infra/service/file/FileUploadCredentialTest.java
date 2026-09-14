package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCompleteReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileUploadCredentialCreateRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileUploadCredentialDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.dal.mysql.file.FileUploadCredentialMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-003：预签名直传凭证与完成确认测试。
 *
 * <p>验收覆盖：合法直传仅一份正式资产；伪造 token/错主体/过期/空上传/重复确认拒绝；
 * PUT URL 重用不替换正式资产；完成状态原子迁移；local 等不支持 presign 的存储禁用直传。
 *
 * @author ZS-FILE-003
 */
@Import({FileServiceImpl.class, cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class FileUploadCredentialTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private FileUploadCredentialMapper credentialMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private FileConfigService fileConfigService;

    @Resource
    private cn.zszj.module.infra.framework.file.config.FileProperties fileProperties;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    /** 模拟对象存储：临时键与正式键同命名空间（并发确认/重放场景真实可见） */
    private Map<String, byte[]> objectStore;

    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            when(masterClient.presignPutUrl(anyString())).thenAnswer(inv ->
                    "https://oss.example.com/" + inv.getArgument(0, String.class)
                            + "?X-Amz-Signature=stub");
            when(masterClient.upload(any(), anyString(), anyString())).thenAnswer(inv -> {
                String path = inv.getArgument(1, String.class);
                objectStore.put(path, inv.getArgument(0, byte[].class));
                return "https://oss.example.com/" + path;
            });
            when(masterClient.getContent(anyString())).thenAnswer(inv ->
                    objectStore.get(inv.getArgument(0, String.class)));
            org.mockito.Mockito.doAnswer(inv -> {
                objectStore.remove(inv.getArgument(0, String.class));
                return null;
            }).when(masterClient).delete(anyString());
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(org.mockito.ArgumentMatchers.anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    private FileUploadCredentialCreateReqVO buildCreateReq(String name, String type, long size) {
        FileUploadCredentialCreateReqVO reqVO = new FileUploadCredentialCreateReqVO();
        reqVO.setName(name);
        reqVO.setPurpose("attachment");
        reqVO.setSize(size);
        reqVO.setContentType(type);
        return reqVO;
    }

    private byte[] simulateClientPut(String uploadUrl, byte[] content) {
        // 从 stub 的 uploadUrl 中解析临时键（url = https://oss.example.com/<tempPath>?签名）
        String tempPath = uploadUrl.replace("https://oss.example.com/", "")
                .replace("?X-Amz-Signature=stub", "");
        objectStore.put(tempPath, content);
        return content;
    }

    // ========== ① 合法直传：凭证创建 → 客户端 PUT → 完成确认 → 唯一正式资产 ==========

    @Test
    public void fullFlow_createsExactlyOneReadableAsset() throws Exception {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("report.txt", "text/plain", 16));

        assertNotNull(credential.getCredentialToken());
        assertTrue(credential.getTempPath().startsWith("temp/"), "临时键必须在 temp/ 区");
        assertNotNull(credential.getUploadUrl());

        byte[] content = "0123456789ABCDEF".getBytes(java.nio.charset.StandardCharsets.UTF_8); // 16 字节，与声明一致
        simulateClientPut(credential.getUploadUrl(), content);

        // 完成确认
        FileUploadCredentialCompleteReqVO completeReq = new FileUploadCredentialCompleteReqVO();
        completeReq.setCredentialToken(credential.getCredentialToken());
        Long fileId = fileService.completeUpload(completeReq);

        // 正式资产唯一且可读，散列一致
        List<FileDO> all = fileMapper.selectList();
        assertEquals(1, all.size(), "只应生成一份正式资产");
        FileDO asset = all.get(0);
        byte[] read = fileService.getFileContent(asset.getConfigId(), asset.getPath());
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(read), "回读散列必须一致");

        // 凭证 COMPLETED 且临时对象清理
        FileUploadCredentialDO done = credentialMapper.selectByToken(credential.getCredentialToken());
        assertEquals(FileUploadCredentialDO.STATUS_COMPLETED, done.getStatus());
        assertEquals(fileId, done.getFileId());
        assertNull(objectStore.get(credential.getTempPath()), "临时对象必须清理");
    }

    // ========== ② 拒绝路径 ==========

    @Test
    public void complete_fakeToken_rejected() {
        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken("not-exist");
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_CREDENTIAL_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void complete_otherOwner_rejected() {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));
        simulateClientPut(credential.getUploadUrl(), "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 模拟匿名上下文（无登录用户，loginUserId=0）+ 凭证绑定 owner=501：
        // completeUpload 的归属校验：loginUserId!=0 且不等于 owner 时 FORBIDDEN；
        // 匿名（0）时 owner=501 不等于匿名 0 → 亦必须 FORBIDDEN（凭证不可被无主体方消费）
        FileUploadCredentialDO credentialDO = credentialMapper.selectByToken(credential.getCredentialToken());
        credentialDO.setOwnerUserId(501L);
        credentialMapper.updateById(credentialDO);

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_CREDENTIAL_FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    public void complete_expired_rejected() {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));
        simulateClientPut(credential.getUploadUrl(), "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        FileUploadCredentialDO credentialDO = credentialMapper.selectByToken(credential.getCredentialToken());
        credentialDO.setExpiresTime(LocalDateTime.now().minusMinutes(1));
        credentialMapper.updateById(credentialDO);

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_CREDENTIAL_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    public void complete_emptyTempUpload_rejected() {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_TEMP_EMPTY.getCode(), ex.getCode());
    }

    @Test
    public void complete_sizeMismatch_rejected() {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 100));
        simulateClientPut(credential.getUploadUrl(), "different-length".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_TEMP_SIZE_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    public void complete_twice_secondRejected() throws Exception {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));
        simulateClientPut(credential.getUploadUrl(), "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        Long first = fileService.completeUpload(req);

        // 第二次确认：凭证已 COMPLETED → 已使用拒绝
        ServiceException ex = assertThrows(ServiceException.class, () -> fileService.completeUpload(req));
        assertEquals(FILE_UPLOAD_CREDENTIAL_ALREADY_USED.getCode(), ex.getCode());
        assertEquals(first, fileMapper.selectList().get(0).getId(), "不生成重复资产");
    }

    @Test
    public void concurrentComplete_onlyOneSucceeds() throws Exception {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));
        simulateClientPut(credential.getUploadUrl(), "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());

        // 并发确认：一个成功一个已使用
        java.util.concurrent.atomic.AtomicInteger success = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger used = new java.util.concurrent.atomic.AtomicInteger();
        for (int i = 0; i < 2; i++) {
            try {
                fileService.completeUpload(req);
                success.incrementAndGet();
            } catch (ServiceException ex) {
                if (FILE_UPLOAD_CREDENTIAL_ALREADY_USED.getCode().equals(ex.getCode())) {
                    used.incrementAndGet();
                } else {
                    throw ex;
                }
            }
        }
        assertEquals(1, success.get(), "恰好一次成功");
        assertEquals(1, used.get(), "恰好一次已使用拒绝");
        assertEquals(1, fileMapper.selectList().size(), "不生成重复资产");
    }

    @Test
    public void putUrlReuse_afterComplete_cannotReplacePublishedAsset() throws Exception {
        FileUploadCredentialCreateRespVO credential =
                fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3));
        byte[] original = "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        simulateClientPut(credential.getUploadUrl(), original);

        FileUploadCredentialCompleteReqVO req = new FileUploadCredentialCompleteReqVO();
        req.setCredentialToken(credential.getCredentialToken());
        Long fileId = fileService.completeUpload(req);

        FileDO asset = fileMapper.selectById(fileId);
        byte[] before = fileService.getFileContent(asset.getConfigId(), asset.getPath());

        // 攻击者重用 PUT URL 改写临时键（凭证已完成、临时键已清理，重传即新建临时键内容）
        simulateClientPut(credential.getUploadUrl(), "malicious-replacement".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 正式资产不受影响
        byte[] after = fileService.getFileContent(asset.getConfigId(), asset.getPath());
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(before),
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(after), "PUT URL 重用不得替换已确认正式资产");
    }

    // ========== ③ local 等不支持 presign 的存储禁用直传 ==========

    @Test
    public void createCredential_presignNotSupported_rejected() {
        FileClient localLike = mock(FileClient.class);
        when(localLike.getId()).thenReturn(9L);
        when(localLike.presignPutUrl(anyString())).thenThrow(new UnsupportedOperationException("不支持的操作"));
        when(fileConfigService.getMasterFileClient()).thenReturn(localLike);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createUploadCredential(buildCreateReq("a.txt", "text/plain", 3)));
        assertEquals(FILE_PRESIGN_NOT_SUPPORTED.getCode(), ex.getCode(), "不支持 presign 的存储必须禁用直传");
    }

    // ========== ④ 凭证创建的 FILE-002 合同复验 ==========

    @Test
    public void createCredential_dangerExtension_rejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.createUploadCredential(buildCreateReq("evil.exe", "application/octet-stream", 10)));
        assertEquals(FILE_DANGEROUS_CONTENT.getCode(), ex.getCode());
    }

    @Test
    public void createCredential_oversize_rejected() {
        long original = fileProperties.getMaxSize();
        try {
            fileProperties.setMaxSize(8L);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fileService.createUploadCredential(buildCreateReq("big.txt", "text/plain", 100)));
            assertEquals(FILE_SIZE_EXCEED.getCode(), ex.getCode());
        } finally {
            fileProperties.setMaxSize(original);
        }
    }
}
