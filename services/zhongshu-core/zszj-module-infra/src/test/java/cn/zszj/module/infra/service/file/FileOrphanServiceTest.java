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
import cn.zszj.module.infra.dal.dataobject.file.FileConfigDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileUploadCredentialDO;
import cn.zszj.module.infra.dal.mysql.file.FileConfigMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.dal.mysql.file.FileUploadCredentialMapper;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.config.FileCompensationProperties;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.core.client.FileObjectEntry;
import cn.zszj.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.zszj.module.infra.framework.file.core.enums.FileStorageEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_CLEANUP_BATCH_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_LISTING_NOT_SUPPORTED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_ORPHAN_SHARED_STORAGE_REFUSED;
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
 * 逐项记录成败不伪报全成功；③重复清理幂等不误删；④存储不支持清点保守报错；⑤单次清理批量有界。
 * codex r0 处置：⑥存储根共享/嵌套的 local 配置保守拒绝（P1-1）；⑦引用核验 LOWER 折叠——
 * 大小写不敏感文件系统下清点拼写≠记录拼写仍能命中引用（P1-2）；⑧截断扫描可经前缀续扫推进（P2-2）。</p>
 */
@Import({FileOrphanServiceImpl.class, FileConfiguration.class})
public class FileOrphanServiceTest extends BaseDbUnitTest {

    /** master 配置（id=1）与其 local 存储根；隔离用例据此构造共享/嵌套/互斥配置 */
    private static final Long MASTER_CONFIG_ID = 1L;

    /**
     * 真实存在的物理根（codex r1 P1-A：隔离核验按 toRealPath 物理路径解析，
     * 配置 basePath 必须真实存在，故用例以临时目录构造）。
     */
    @TempDir
    Path tempRoot;

    private Path masterRoot;

    @Resource
    private FileOrphanService orphanService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileUploadCredentialMapper credentialMapper;
    @Resource
    private FileConfigMapper fileConfigMapper;
    @Resource
    private FileCompensationProperties properties;

    @MockitoBean
    private FileConfigService fileConfigService;

    /** 模拟对象存储：path → 内容 / 最后修改时间 */
    private Map<String, byte[]> objectStore;
    private Map<String, LocalDateTime> modifiedMap;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() throws Exception {
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
            Collections.sort(sorted);
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
        // 存储根隔离核验（P1-1/r1 P1-A）依赖配置表与真实存在的物理根
        masterRoot = Files.createDirectories(tempRoot.resolve("real"));
        seedFileConfig(MASTER_CONFIG_ID, "master-local", masterRoot.toString(), true);
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

        FileOrphanPreviewRespVO preview = orphanService.preview(MASTER_CONFIG_ID, "");

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

        ServiceException ex = assertThrows(ServiceException.class,
                () -> orphanService.preview(MASTER_CONFIG_ID, ""));
        assertEquals(FILE_ORPHAN_LISTING_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    // ========== ③ 清点上限如实截断标注 ==========

    @Test
    public void preview_truncationFlagHonest() {
        properties.getOrphan().setScanMaxObjects(2);
        seedOrphanObject("asset/o1.bin", 30);
        seedOrphanObject("asset/o2.bin", 30);
        seedOrphanObject("asset/o3.bin", 30);

        FileOrphanPreviewRespVO preview = orphanService.preview(MASTER_CONFIG_ID, "");

        assertTrue(preview.isTruncated(), "清点达到上限必须如实标注截断");
        assertEquals(2, preview.getItems().size());
    }

    // ========== ④（P2-2）截断扫描可推进：前段全被引用时后部孤儿经前缀续扫可见 ==========

    @Test
    public void preview_truncatedScan_continuesByPrefix() {
        properties.getOrphan().setScanMaxObjects(2);
        // 排序最前的两个对象全被引用：首段预览为空但截断如实
        seedOrphanObject("asset/a-ref1.bin", 30);
        seedFileRecord("asset/a-ref1.bin", 1L);
        seedOrphanObject("asset/a-ref2.bin", 30);
        seedFileRecord("asset/a-ref2.bin", 1L);
        seedOrphanObject("asset/z-orphan.bin", 30);

        FileOrphanPreviewRespVO firstPage = orphanService.preview(MASTER_CONFIG_ID, "");
        assertTrue(firstPage.isTruncated(), "首段被截断必须如实标注");
        assertTrue(firstPage.getItems().isEmpty(), "首段对象全被引用，无候选");

        // 前缀续扫：后部孤儿可见（扫描可推进，不因截断而永久不可见）
        FileOrphanPreviewRespVO secondPage = orphanService.preview(MASTER_CONFIG_ID, "asset/z");
        assertFalse(secondPage.isTruncated());
        assertEquals(1, secondPage.getItems().size());
        assertEquals("asset/z-orphan.bin", secondPage.getItems().get(0).getPath());
    }

    // ========== ⑤（P1-1）存储根共享/嵌套的 local 配置保守拒绝 ==========

    @Test
    public void sharedStorageRoot_previewAndCleanup_conservativelyRefused() throws Exception {
        seedOrphanObject("asset/orphan-old.bin", 30);
        // 同 basePath 的第二个 local 配置：两 client 清点的是同一批物理文件 → 保守拒绝
        seedFileConfig(2L, "mirror-local", masterRoot.toString(), false);

        ServiceException previewEx = assertThrows(ServiceException.class,
                () -> orphanService.preview(MASTER_CONFIG_ID, ""));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), previewEx.getCode());

        FileOrphanCleanupReqVO reqVO = req(List.of("asset/orphan-old.bin"));
        ServiceException cleanupEx = assertThrows(ServiceException.class,
                () -> orphanService.cleanup(reqVO));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), cleanupEx.getCode());
        assertTrue(objectStore.containsKey("asset/orphan-old.bin"), "拒绝清理路径不产生任何删除");
    }

    @Test
    public void nestedStorageRoot_conservativelyRefused_butDisjointAllowed() throws Exception {
        seedOrphanObject("asset/orphan-old.bin", 30);
        // 嵌套根（外层清点覆盖内层全部对象）→ 拒绝
        Path nestedRoot = Files.createDirectories(tempRoot.resolve("real").resolve("sub"));
        seedFileConfig(2L, "nested-local", nestedRoot.toString(), false);
        ServiceException nestedEx = assertThrows(ServiceException.class,
                () -> orphanService.preview(MASTER_CONFIG_ID, ""));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), nestedEx.getCode());

        // 物理互斥根 → 正常清点（隔离核验不误伤正常单配置/互斥多配置）
        //（注：deleteById 为 @TableLogic 逻辑删除，行仍占主键，故互斥配置以新 id 落库）
        fileConfigMapper.deleteById(2L);
        Path elsewhereRoot = Files.createDirectories(tempRoot.resolve("elsewhere"));
        seedFileConfig(3L, "elsewhere-local", elsewhereRoot.toString(), false);
        FileOrphanPreviewRespVO preview = orphanService.preview(MASTER_CONFIG_ID, "");
        assertEquals(1, preview.getItems().size());
        assertEquals("asset/orphan-old.bin", preview.getItems().get(0).getPath());
    }

    // ========== ⑤b（codex r1 P1-A）junction/symlink 别名共享：物理路径解析后仍被拒绝 ==========

    @Test
    public void aliasStorageRoot_junctionToSamePhysicalPath_conservativelyRefused() throws Exception {
        seedOrphanObject("asset/orphan-old.bin", 30);
        // 别名根：优先符号链接，Windows 无特权环境退回目录 junction（mklink /J 无需管理员）
        Path alias = createAliasLink(tempRoot.resolve("alias"), masterRoot);
        assumeTrue(alias != null && Files.exists(alias), "环境不支持符号链接/junction 创建，跳过别名共享用例");

        // 字符串规范化下 real≠alias，物理真实路径解析后同一目录 → 保守拒绝（修复前此处放行 → 误删）
        seedFileConfig(2L, "alias-local", alias.toString(), false);
        ServiceException previewEx = assertThrows(ServiceException.class,
                () -> orphanService.preview(MASTER_CONFIG_ID, ""));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), previewEx.getCode());

        // 反向同理：以别名配置为清点入口同样拒绝
        ServiceException reverseEx = assertThrows(ServiceException.class,
                () -> orphanService.preview(2L, ""));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), reverseEx.getCode());
        assertTrue(objectStore.containsKey("asset/orphan-old.bin"), "拒绝路径不产生任何删除");
    }

    // ========== ⑤c（codex r1 P1-B）尾分隔符根：边界判定不失配 ==========

    @Test
    public void trailingSeparatorRoot_containmentStillHolds_conservativelyRefused() throws Exception {
        seedOrphanObject("asset/orphan-old.bin", 30);
        // 盘根/文件系统根规范化后带尾分隔符（C:\ → c:、/ → /）——修复前边界拼接得 c:// 使
        // 包含判定失效、内层配置绕过隔离；修复后尾分隔符剥离合同 + 「/ 包罗一切」双保险
        Path driveRoot = masterRoot.getRoot();
        assumeTrue(driveRoot != null, "临时目录无根组件（异常环境），跳过尾分隔符用例");
        seedFileConfig(2L, "drive-root-local", driveRoot.toString(), false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> orphanService.preview(MASTER_CONFIG_ID, ""));
        assertEquals(FILE_ORPHAN_SHARED_STORAGE_REFUSED.getCode(), ex.getCode());
        assertTrue(objectStore.containsKey("asset/orphan-old.bin"));
    }

    // ========== ⑥（P1-2）大小写不敏感文件系统：清点拼写≠记录拼写仍命中引用 ==========

    @Test
    public void caseInsensitiveFilesystem_referenceMatchedByFoldedPath_notOrphan() throws Exception {
        // FS 既有拼写为 "Asset/live.bin"；DB 记录拼写为 "asset/live.bin"（各自拼写）——
        // 修复前精确匹配漏检引用 → 过保留期被误删活文件；修复后 LOWER 折叠命中 → 保留
        seedOrphanObject("Asset/live.bin", 30);
        seedFileRecord("asset/live.bin", 1L);

        FileOrphanPreviewRespVO preview = orphanService.preview(MASTER_CONFIG_ID, "");
        assertTrue(preview.getItems().isEmpty(), "异拼写引用必须命中（不进入候选）");

        FileOrphanCleanupRespVO resp = orphanService.cleanup(req(List.of("Asset/live.bin")));
        assertEquals(0, resp.getSuccessPaths().size(), "清理重核验同样折叠命中，不得删除");
        assertEquals(1, resp.getFailures().size());
        assertTrue(objectStore.containsKey("Asset/live.bin"), "被引用活文件不得被误删");
    }

    // ========== ⑦ 清理：执行前重核验引用（预览→清理窗口竞态防御） ==========

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

    // ========== ⑧ 重复清理幂等：对象已不存在视为成功，不重复调用删除 ==========

    @Test
    public void cleanup_repeat_idempotent_noDoubleDelete() throws Exception {
        seedOrphanObject("asset/idem.bin", 30);

        FileOrphanCleanupRespVO first = orphanService.cleanup(req(List.of("asset/idem.bin")));
        FileOrphanCleanupRespVO second = orphanService.cleanup(req(List.of("asset/idem.bin")));

        assertEquals(List.of("asset/idem.bin"), first.getSuccessPaths());
        assertEquals(List.of("asset/idem.bin"), second.getSuccessPaths(), "对象已不存在幂等视为成功");
        verify(masterClient, times(1)).delete("asset/idem.bin");
    }

    // ========== ⑨ 单次清理批量有界 ==========

    @Test
    public void cleanup_batchExceed_rejected() {
        properties.getOrphan().setCleanupMaxPaths(2);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> orphanService.cleanup(req(List.of("a", "b", "c"))));
        assertEquals(FILE_ORPHAN_CLEANUP_BATCH_EXCEED.getCode(), ex.getCode());
    }

    // ========== ⑩ 存储失败逐项隔离：单 path 失败不伪报全成功 ==========

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

    /**
     * 创建指向 target 的目录别名：优先 Files.createSymbolicLink（Linux/特权 Windows），
     * 失败退回目录 junction（cmd mklink /J，Windows 免管理员）；均不可用返回 null（用例 skip）。
     */
    private Path createAliasLink(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException ex) {
            // fallthrough 到 junction
        }
        try {
            Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                    link.toString(), target.toString()).start();
            if (p.waitFor() == 0 && Files.exists(link)) {
                return link;
            }
        } catch (Exception ex) {
            // 非 Windows/无 cmd：返回 null 由 assumeTrue 跳过
        }
        return null;
    }

    private void seedFileConfig(Long id, String name, String basePath, boolean master) {
        LocalFileClientConfig localConfig = new LocalFileClientConfig();
        localConfig.setBasePath(basePath);
        localConfig.setDomain("http://localhost:48080");
        FileConfigDO config = FileConfigDO.builder()
                .id(id).name(name)
                .storage(FileStorageEnum.LOCAL.getStorage())
                .remark("ut").master(master)
                .config(localConfig)
                .build();
        fileConfigMapper.insert(config);
    }

    private void seedFileRecord(String path, Long tenantId) {
        TenantUtils.execute(tenantId, () -> {
            FileDO file = FileDO.builder()
                    .configId(MASTER_CONFIG_ID).name(path).path(path)
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
                .setConfigId(MASTER_CONFIG_ID)
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
        reqVO.setConfigId(MASTER_CONFIG_ID);
        reqVO.setPaths(paths);
        return reqVO;
    }

}
