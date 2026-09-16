package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanItemRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanPreviewRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileUploadCredentialDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.dal.mysql.file.FileUploadCredentialMapper;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.config.FileCompensationProperties;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.core.client.FileObjectEntry;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_CLEANUP_BATCH_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_LISTING_NOT_SUPPORTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-005.B：孤儿对象「预览→清理」两步流程测试（H2，mock FileClient 内存存储）。
 *
 * <p>合同：①清点范围可预览——仅列出「无任何租户 DB 记录引用 ∧ 过保留期 ∧ temp/ 无活跃凭证认领」的对象，
 * 跨租户引用必须被全局核验排除（防跨租户误删）；②清理前逐 path 重清点+重核验（预览→清理窗口的竞态防御），
 * 逐项记录成败不伪报全成功；③重复清理幂等不误删；④存储不支持清点保守报错；⑤单次清理批量有界。</p>
 */
@Import({FileOrphanServiceImpl.class, FileConfiguration.class})
public class FileOrphanServiceTest extends BaseDbUnitTest {

    @Resource
    private FileOrphanService orphanService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileUploadCredentialMapper credentialMapper;
    @Resource
    private FileCompensationProperties properties;

    @MockitoBean
    private FileConfigService fileConfigService;

    /** 模拟对象存储：path → 内容 / 最后修改时间 */
    private Map<String, byte[]> objectStore;
    private Map<String, LocalDateTime> modifiedMap;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        properties.getOrphan().setRetentionDays(7);
        properties.getOrphan().setScanMaxObjects(1000);
        properties.getOrphan().setCleanupMaxPaths(100);
        objectStore = new ConcurrentHashMap<>();
        modifiedMap = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        when(masterClient.listObjects(anyString(), anyInt())).thenAnswer(inv -> {
            String prefix = inv.getArgument(0, String.class);
            int max = inv.getArgument(1, Integer.class);
            List<String> sorted = new ArrayList<>(objectStore.keySet());
            java.util.Collections.sort(sorted);
            return sorted.stream().filter(p -> p.startsWith(prefix)).limit(max)
                    .map(p -> new FileObjectEntry(p, (long) objectStore.get(p).length, modifiedMap.get(p)))
                    .toList();
        });
        try {
            org.mockito.Mockito.doAnswer(inv -> {
                objectStore.remove(inv.getArgument(0, String.class));
                return null;
            }).when(masterClient).delete(anyString());
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== ① 预览：清点范围可预览，引用/在途/保留期过滤，跨租户引用全局排除 ==========

    @Test
    public void preview_listsOnlyUnreferencedPastRetention_andExcludesCrossTenantReferences() {
        seedOrphanObject("asset/orphan-old.bin", 30); // 无记录 + 过保留期 → 候选
        seedOrphanObject("asset/orphan-young.bin", 1); // 无记录 + 未过保留期 → 排除
        seedOrphanObject("asset/referenced.bin", 30);
        seedFileRecord("asset/referenced.bin", 1L); // 本租户引用 → 排除
        seedOrphanObject("asset/other-tenant.bin", 30);
        seedFileRecord("asset/other-tenant.bin", 2L); // 他租户引用 → 全局核验排除（不跨租户误删）
        seedOrphanObject("temp/inflight.bin", 30);
        seedActiveCredential("temp/inflight.bin"); // 在途直传 → 排除
        seedOrphanObject("temp/abandoned.bin", 30); // temp 无活跃凭证 + 过保留期 → 候选

        FileOrphanPreviewRespVO preview = orphanService.preview(null);

        assertFalse(preview.isTruncated());
        assertEquals(2, preview.getItems().size(), "仅 orphan-old 与 abandoned 进入候选");
        List<String> paths = preview.getItems().stream().map(FileOrphanItemRespVO::getPath).toList();
        assertTrue(paths.contains("asset/orphan-old.bin"));
        assertTrue(paths.contains("temp/abandoned.bin"));
        FileOrphanItemRespVO tempItem = preview.getItems().stream()
                .filter(i -> i.getPath().equals("temp/abandoned.bin")).findFirst().orElseThrow();
        assertEquals(FileOrphanItemRespVO.CATEGORY_TEMP, tempItem.getCategory());
    }

    // ========== ② 存储不支持清点：保守报错，不假报空清单 ==========

    @Test
    public void preview_listingUnsupported_failsWithErrorCode() {
        org.mockito.Mockito.reset(masterClient);
        when(masterClient.getId()).thenReturn(1L);
        when(masterClient.listObjects(anyString(), anyInt()))
                .thenThrow(new UnsupportedOperationException("s3 listing pending"));

        ServiceException ex = assertThrows(ServiceException.class, () -> orphanService.preview(null));
        assertEquals(FILE_ORPHAN_LISTING_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    // ========== ③ 清点上限如实截断标注 ==========

    @Test
    public void preview_truncationFlagHonest() {
        properties.getOrphan().setScanMaxObjects(2);
        seedOrphanObject("asset/o1.bin", 30);
        seedOrphanObject("asset/o2.bin", 30);
        seedOrphanObject("asset/o3.bin", 30);

        FileOrphanPreviewRespVO preview = orphanService.preview(null);

        assertTrue(preview.isTruncated(), "清点达到上限必须如实标注截断");
        assertEquals(2, preview.getItems().size());
    }

    // ========== ④ 清理：执行前重核验引用（预览→清理窗口竞态防御） ==========

    @Test
    public void cleanup_rechecksReferenceBeforeDelete_perItemResult() {
        seedOrphanObject("asset/still-orphan.bin", 30);
        seedOrphanObject("asset/became-referenced.bin", 30);
        seedFileRecord("asset/became-referenced.bin", 1L); // 预览后才出现的引用

        FileOrphanCleanupRespVO resp = orphanService.cleanup(req(Arrays.asList(
                "asset/still-orphan.bin", "asset/became-referenced.bin")));

        assertEquals(List.of("asset/still-orphan.bin"), resp.getSuccessPaths());
        assertEquals(1, resp.getFailures().size());
        assertEquals("asset/became-referenced.bin", resp.getFailures().get(0).getPath());
        assertFalse(objectStore.containsKey("asset/still-orphan.bin"), "确认无引用的对象被清理");
        assertTrue(objectStore.containsKey("asset/became-referenced.bin"), "重核验命中引用即跳过，不误删");
    }

    // ========== ⑤ 重复清理幂等：对象已不存在视为成功，不重复调用删除 ==========

    @Test
    public void cleanup_repeat_idempotent_noDoubleDelete() throws Exception {
        seedOrphanObject("asset/idem.bin", 30);

        FileOrphanCleanupRespVO first = orphanService.cleanup(req(List.of("asset/idem.bin")));
        FileOrphanCleanupRespVO second = orphanService.cleanup(req(List.of("asset/idem.bin")));

        assertEquals(List.of("asset/idem.bin"), first.getSuccessPaths());
        assertEquals(List.of("asset/idem.bin"), second.getSuccessPaths(), "对象已不存在幂等视为成功");
        verify(masterClient, times(1)).delete("asset/idem.bin");
    }

    // ========== ⑥ 单次清理批量有界 ==========

    @Test
    public void cleanup_batchExceed_rejected() {
        properties.getOrphan().setCleanupMaxPaths(2);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> orphanService.cleanup(req(List.of("a", "b", "c"))));
        assertEquals(FILE_ORPHAN_CLEANUP_BATCH_EXCEED.getCode(), ex.getCode());
    }

    // ========== ⑦ 存储失败逐项隔离：单 path 失败不伪报全成功 ==========

    @Test
    public void cleanup_storageFailure_recordedPerItem() throws Exception {
        seedOrphanObject("asset/fail.bin", 30);
        seedOrphanObject("asset/ok.bin", 30);
        doThrow(new IllegalStateException("storage down")).when(masterClient)
                .delete(argThat(p -> !p.equals("asset/ok.bin")));

        FileOrphanCleanupRespVO resp = orphanService.cleanup(req(Arrays.asList(
                "asset/fail.bin", "asset/ok.bin")));

        assertEquals(List.of("asset/ok.bin"), resp.getSuccessPaths());
        assertEquals(1, resp.getFailures().size());
        assertEquals("asset/fail.bin", resp.getFailures().get(0).getPath());
        assertNotNull(resp.getFailures().get(0).getErrorMessage());
        assertTrue(objectStore.containsKey("asset/fail.bin"));
    }

    // ========== 造数辅助 ==========

    private void seedOrphanObject(String path, int ageDays) {
        objectStore.put(path, ("orphan-" + path).getBytes());
        modifiedMap.put(path, LocalDateTime.now().minusDays(ageDays));
    }

    private void seedFileRecord(String path, Long tenantId) {
        TenantUtils.execute(tenantId, () -> {
            FileDO file = FileDO.builder()
                    .configId(1L).name(path).path(path)
                    .url("https://oss.example.com/" + path)
                    .type("application/octet-stream").size(10L)
                    .fileHash(DigestUtil.sha256Hex(path))
                    .ownerUserId(101L).scope("PRIVATE")
                    .build();
            file.setTenantId(tenantId);
            fileMapper.insert(file);
            return null;
        });
    }

    private void seedActiveCredential(String tempPath) {
        FileUploadCredentialDO credential = new FileUploadCredentialDO()
                .setCredentialToken("tok-" + System.nanoTime())
                .setConfigId(1L)
                .setOwnerUserId(101L)
                .setPurpose("download")
                .setTempPath(tempPath)
                .setFileName("a.bin")
                .setContentType("application/octet-stream")
                .setDeclaredSize(10L)
                .setScope("PRIVATE")
                .setStatus(FileUploadCredentialDO.STATUS_WAITING_UPLOAD)
                .setExpiresTime(LocalDateTime.now().plusMinutes(10));
        credential.setTenantId(1L);
        credentialMapper.insert(credential);
    }

    private FileOrphanCleanupReqVO req(List<String> paths) {
        FileOrphanCleanupReqVO reqVO = new FileOrphanCleanupReqVO();
        reqVO.setConfigId(1L);
        reqVO.setPaths(paths);
        return reqVO;
    }

}
