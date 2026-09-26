package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionItemRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionPreviewRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.config.FileExportProperties;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DELETE_REFERENCED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_RETENTION_BATCH_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_RETENTION_NOT_EXPIRED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_RETENTION_PURPOSE_NOT_EXPORT;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_RETENTION_STATUS_NOT_PUBLISHED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-004.B：导出件按用途保留期清理测试（H2，循 ZS-FILE-005.B 两步人工授权先例）。
 *
 * <p>合同（FILE-005.B 计划 L46/L97 明文移交「导出文件按用途保留期清理」）：①预览只读——候选 =
 * purpose='export' ∧ PUBLISHED ∧ 保留期已到（普通上传与未到期/中间态记录不在内），超上限如实截断；
 * ②清理以显式 id 授权（有界批次），执行前逐项重核验（预览→清理窗口竞态防御），逐项记录成败不伪报
 * 全成功；③既有保护全继承——引用保护（活跃交付会话）/非导出件混入/未到期均拒绝或跳过；
 * ④清理走 deleteFile（不新增绕过面）。</p>
 *
 * @author ZS-FILE-004.B
 */
@Import({FileServiceImpl.class, FileExportRetentionServiceImpl.class, FileConfiguration.class})
public class FileExportRetentionTest extends BaseDbUnitTest {

    @Resource
    private FileExportRetentionService exportRetentionService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileDeliveryTicketMapper ticketMapper;
    @Resource
    private FileExportProperties exportProperties;

    @MockitoBean
    private FileConfigService fileConfigService;
    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;
    @MockitoBean
    private OrgDataPermissionChecker orgDataPermissionChecker;

    /** 模拟对象存储：path → 内容（delete 即移除，观测清理事实） */
    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            org.mockito.Mockito.doAnswer(inv -> {
                objectStore.remove(inv.getArgument(0, String.class));
                return null;
            }).when(masterClient).delete(anyString());
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(anyLong())).thenReturn(masterClient);
        // 导出件 org 门的真实装配语义（ZS-FILE-001.B）：范围内组织可见——deleteFile 继承 org 门
        when(orgDataPermissionChecker.isObjectVisible(anyLong(), anyLong())).thenAnswer(inv -> true);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== R1 预览：仅过保留期的导出件进入候选（按用途） ==========

    @Test
    public void preview_listsOnlyExpiredExport() {
        FileDO expired = seedExportFile("expired", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);
        seedExportFile("young", LocalDateTime.now().plusDays(1), FileDO.STATUS_PUBLISHED);
        seedNormalFile("normal");

        FileExportRetentionPreviewRespVO preview = exportRetentionService.preview();

        assertFalse(preview.isTruncated());
        assertEquals(1, preview.getItems().size(), "仅过保留期的导出件进入候选");
        FileExportRetentionItemRespVO item = preview.getItems().get(0);
        assertEquals(expired.getId(), item.getFileId());
        assertEquals("export", item.getPurpose());
        assertNotNull(item.getRetentionExpireTime());
    }

    // ========== R2 预览：删除中间态不在候选 ==========

    @Test
    public void preview_deletingOrDeletedExcluded() {
        seedExportFile("deleting", LocalDateTime.now().minusDays(1), FileDO.STATUS_DELETING);
        FileDO published = seedExportFile("published", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);

        FileExportRetentionPreviewRespVO preview = exportRetentionService.preview();

        assertEquals(1, preview.getItems().size(), "仅 PUBLISHED 过期件进入候选（正向对照，防空洞通过）");
        assertEquals(published.getId(), preview.getItems().get(0).getFileId(),
                "DELETING 中间态不在候选（保护进行中删除）");
    }

    // ========== R3 预览：超上限如实截断 ==========

    @Test
    public void preview_truncatedWhenOverLimit() {
        exportProperties.setPreviewMaxItems(2);
        seedExportFile("o1", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);
        seedExportFile("o2", LocalDateTime.now().minusDays(2), FileDO.STATUS_PUBLISHED);
        seedExportFile("o3", LocalDateTime.now().minusDays(3), FileDO.STATUS_PUBLISHED);

        FileExportRetentionPreviewRespVO preview = exportRetentionService.preview();

        assertTrue(preview.isTruncated(), "达到上限必须如实标注截断");
        assertEquals(2, preview.getItems().size());
    }

    // ========== R4 清理：执行前逐项重核验（窗口竞态防御） ==========

    @Test
    public void cleanup_revalidatesBeforeDelete() {
        FileDO clean = seedExportFile("clean", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);
        FileDO becameDeleting = seedExportFile("became-deleting", LocalDateTime.now().minusDays(1),
                FileDO.STATUS_PUBLISHED);
        // 预览→清理窗口内被改状态（如删除侧推进 DELETING）
        becameDeleting.setStatus(FileDO.STATUS_DELETING);
        fileMapper.updateById(becameDeleting);

        FileExportRetentionCleanupRespVO resp = exportRetentionService.cleanup(req(List.of(
                clean.getId(), becameDeleting.getId())));

        assertEquals(List.of(clean.getId()), resp.getSuccessIds(), "重核验通过项正常清理");
        assertEquals(1, resp.getFailures().size());
        assertEquals(becameDeleting.getId(), resp.getFailures().get(0).getFileId());
        assertNotNull(resp.getFailures().get(0).getErrorMessage());
        assertTrue(resp.getFailures().get(0).getErrorMessage()
                        .contains(String.valueOf(FILE_EXPORT_RETENTION_STATUS_NOT_PUBLISHED.getCode())),
                "重核验失败须携状态门真实注册码（codex r1 P3-1，便于对账）");
        // 成功项：记录移除 + 对象删除；失败项：原样保留
        assertNull(fileMapper.selectById(clean.getId()));
        assertFalse(objectStore.containsKey(clean.getPath()));
        assertNotNull(fileMapper.selectById(becameDeleting.getId()));
        assertTrue(objectStore.containsKey(becameDeleting.getPath()), "重核验失败项不得删除");
    }

    // ========== R5 清理：未到期跳过（保留期边界保护） ==========

    @Test
    public void cleanup_nonExpiredSkipped() {
        FileDO young = seedExportFile("young", LocalDateTime.now().plusDays(3), FileDO.STATUS_PUBLISHED);

        FileExportRetentionCleanupRespVO resp = exportRetentionService.cleanup(req(List.of(young.getId())));

        assertTrue(resp.getSuccessIds().isEmpty());
        assertEquals(1, resp.getFailures().size());
        assertTrue(resp.getFailures().get(0).getErrorMessage()
                        .contains(String.valueOf(FILE_EXPORT_RETENTION_NOT_EXPIRED.getCode())),
                "未到期跳过须携保留期门真实注册码（codex r1 P3-1，便于对账）");
        assertNotNull(fileMapper.selectById(young.getId()), "未到期不得删除");
        assertTrue(objectStore.containsKey(young.getPath()));
    }

    // ========== R6 清理：单次批次有界 ==========

    @Test
    public void cleanup_batchExceed_rejected() {
        exportProperties.setCleanupMaxIds(2);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> exportRetentionService.cleanup(req(List.of(1L, 2L, 3L))));
        assertEquals(FILE_EXPORT_RETENTION_BATCH_EXCEED.getCode(), ex.getCode(), "超批次上限整批拒绝");
    }

    // ========== R7 清理：引用保护继承（活跃交付会话阻塞删除） ==========

    @Test
    public void cleanup_activeDeliverySession_rejected() {
        FileDO referenced = seedExportFile("referenced", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);
        insertDeliverySession(referenced.getId(), LocalDateTime.now().plusMinutes(10));

        FileExportRetentionCleanupRespVO resp = exportRetentionService.cleanup(req(List.of(referenced.getId())));

        assertTrue(resp.getSuccessIds().isEmpty(), "引用保护命中不得伪报成功");
        assertEquals(1, resp.getFailures().size());
        assertEquals(referenced.getId(), resp.getFailures().get(0).getFileId());
        assertTrue(resp.getFailures().get(0).getErrorMessage()
                        .contains(String.valueOf(FILE_DELETE_REFERENCED.getCode())),
                "逐项失败须携引用保护错误码（既有保护继承，便于对账）");
        assertNotNull(fileMapper.selectById(referenced.getId()), "被引用记录必须保留");
        assertEquals(FileDO.STATUS_PUBLISHED, fileMapper.selectById(referenced.getId()).getStatus(),
                "引用保护回退 PUBLISHED（既有语义继承）");
        assertTrue(objectStore.containsKey(referenced.getPath()), "被引用对象不得删除");
    }

    // ========== R8 清理：非导出件混入跳过（用途门，不越界清理） ==========

    @Test
    public void cleanup_nonExportIdSkipped() {
        FileDO normal = seedNormalFile("normal");

        FileExportRetentionCleanupRespVO resp = exportRetentionService.cleanup(req(List.of(normal.getId())));

        assertTrue(resp.getSuccessIds().isEmpty(), "非导出件不得经本通道清理");
        assertEquals(1, resp.getFailures().size());
        assertTrue(resp.getFailures().get(0).getErrorMessage()
                        .contains(String.valueOf(FILE_EXPORT_RETENTION_PURPOSE_NOT_EXPORT.getCode())),
                "非导出件跳过须携用途门真实注册码（codex r1 P3-1，便于对账）");
        assertNotNull(fileMapper.selectById(normal.getId()));
        assertTrue(objectStore.containsKey(normal.getPath()));
    }

    // ========== R9 清理：重复 id 去重（成功/失败账本互斥，不重复处理） ==========

    @Test
    public void cleanup_duplicateIds_deduped() {
        FileDO expired = seedExportFile("dup", LocalDateTime.now().minusDays(1), FileDO.STATUS_PUBLISHED);

        FileExportRetentionCleanupRespVO resp = exportRetentionService.cleanup(
                req(List.of(expired.getId(), expired.getId())));

        assertEquals(List.of(expired.getId()), resp.getSuccessIds(), "重复 id 只处理一次（codex r1 P3-2）");
        assertTrue(resp.getFailures().isEmpty(), "已清理成功项不得再次进入失败账本（互斥性）");
        assertNull(fileMapper.selectById(expired.getId()));
        assertFalse(objectStore.containsKey(expired.getPath()));
    }

    // ========== 造数辅助 ==========

    private FileExportRetentionCleanupReqVO req(List<Long> fileIds) {
        FileExportRetentionCleanupReqVO req = new FileExportRetentionCleanupReqVO();
        req.setFileIds(fileIds);
        return req;
    }

    private FileDO seedExportFile(String tag, LocalDateTime retentionExpireTime, String status) {
        FileDO file = seedFile(tag);
        file.setPurpose("export");
        file.setRetentionExpireTime(retentionExpireTime);
        file.setStatus(status);
        fileMapper.updateById(file);
        return file;
    }

    private FileDO seedNormalFile(String tag) {
        return seedFile(tag);
    }

    private FileDO seedFile(String tag) {
        String path = "asset/" + tag + "-" + randomString() + ".bin";
        byte[] content = ("content-" + tag).getBytes();
        objectStore.put(path, content);
        FileDO file = FileDO.builder()
                .configId(1L).name(tag + ".bin").path(path)
                .url("https://oss.example.com/" + path)
                .type("application/octet-stream").size((long) content.length)
                .fileHash(DigestUtil.sha256Hex(content))
                .ownerUserId(101L).scope("PRIVATE")
                .organizationId(100L)
                .build();
        file.setTenantId(1L);
        fileMapper.insert(file);
        return file;
    }

    private void insertDeliverySession(Long fileId, LocalDateTime expiresTime) {
        FileDeliveryTicketDO ticket = new FileDeliveryTicketDO();
        ticket.setTicketHash(DigestUtil.sha256Hex(randomString()));
        ticket.setFileId(fileId);
        ticket.setOwnerUserId(101L);
        ticket.setPurpose("export");
        ticket.setStatus(FileDeliveryTicketDO.STATUS_REDEEMED);
        ticket.setDeliverySessionId(randomString());
        ticket.setLoginSession("sess-101");
        ticket.setTenantId(1L);
        ticket.setExpiresTime(expiresTime);
        ticket.setRedeemTime(LocalDateTime.now());
        ticketMapper.insert(ticket);
    }

}
