package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeleteBatchRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-005.A：删除/发布中间态、引用保护和人工对账入口测试（H2，无 Outbox 依赖）。
 *
 * <p>合同：①删除走 DELETING 可恢复中间态，对象删除失败保留记录并上抛（中段失败不假报成功）；
 * ②引用保护——存在进行中的交付会话拒绝删除（过期会话不阻塞）；③批量删除逐项记录结果；
 * ④人工对账入口——列出 DELETING 记录、重试清理（对象已不存在仅移除记录）。</p>
 */
@Import({FileServiceImpl.class, cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class FileLifecycleReconcileTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileDeliveryTicketMapper ticketMapper;

    @MockitoBean
    private FileConfigService fileConfigService;
    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    /** 模拟对象存储：以 path 记录「是否仍存在」 */
    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            doThrow(new IllegalStateException("storage down"))
                    .when(masterClient).delete(anyString());
            when(masterClient.getContentRange(anyString(), anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(inv -> {
                        byte[] content = objectStore.get(inv.getArgument(0, String.class));
                        if (content == null) {
                            return null;
                        }
                        long start = inv.getArgument(1, Long.class);
                        int length = inv.getArgument(2, Integer.class);
                        return Arrays.copyOfRange(content, (int) Math.min(start, content.length),
                                (int) Math.min(start + length, content.length));
                    });
        } catch (Exception ignored) {
        }
        when(fileConfigService.getMasterFileClient()).thenReturn(masterClient);
        when(fileConfigService.getFileClient(anyLong())).thenReturn(masterClient);
    }

    @AfterEach
    public void afterEach() {
        TenantContextHolder.clear();
    }

    // ========== ① 删除成功：对象与记录均移除 ==========

    @Test
    public void deleteFile_success_removesObjectAndRecord() throws Exception {
        FileDO file = seedFile("deletable");
        // 默认夹具 delete 抛 storage down——允许本用例删除成功（重置为不抛）
        resetDeleteToSucceed();
        fileService.deleteFile(file.getId());
        verify(masterClient).delete(file.getPath());
        assertNull(fileMapper.selectById(file.getId()));
    }

    // ========== ② 中段失败：保留 DELETING 可恢复记录，不假报成功 ==========

    @Test
    public void deleteFile_objectFailure_keepsDeletingRecordAndThrows() {
        FileDO file = seedFile("storage-down");
        // 存储异常以原始类型上抛（IllegalStateException），不得静默吞掉（中段失败不假报成功）
        assertThrows(IllegalStateException.class, () -> fileService.deleteFile(file.getId()));
        // 记录保留且处于 DELETING 中间态（可恢复、可对账）
        FileDO stuck = fileMapper.selectById(file.getId());
        assertNotNull(stuck, "对象删除失败不得移除记录（不假报成功）");
        assertEquals(FileDO.STATUS_DELETING, stuck.getStatus());
    }

    @Test
    public void deleteFile_referencedByActiveDelivery_rejected() {
        FileDO file = seedFile("referenced");
        // 进行中的交付会话引用该文件（REDEEMED 未过期）
        insertDeliverySession(file.getId(), LocalDateTime.now().plusMinutes(10));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.deleteFile(file.getId()));
        assertEquals(FILE_DELETE_REFERENCED.getCode(), ex.getCode(), "引用保护：进行中交付会话阻塞删除");
        assertEquals(FileDO.STATUS_PUBLISHED, fileMapper.selectById(file.getId()).getStatus(),
                "被引用文件不得进入删除中间态");
    }

    @Test
    public void deleteFile_expiredDeliverySession_notBlocking() throws Exception {
        FileDO file = seedFile("expired-session");
        resetDeleteToSucceed();
        // 已过期的交付会话不构成引用
        insertDeliverySession(file.getId(), LocalDateTime.now().minusMinutes(1));
        fileService.deleteFile(file.getId());
        assertNull(fileMapper.selectById(file.getId()));
    }

    // ========== ③ 批量删除：逐项记录结果，中段失败不伪报全成功 ==========

    @Test
    public void deleteFileList_partialFailure_recordedPerItem() throws Exception {
        FileDO ok = seedFile("batch-ok");
        FileDO bad = seedFile("batch-bad");
        // batch-ok 放行删除，batch-bad 维持存储异常（以 path 区分）
        resetDeleteToSucceedFor(ok.getPath());

        FileDeleteBatchRespVO respVO = fileService.deleteFileList(Arrays.asList(ok.getId(), bad.getId()));

        assertEquals(1, respVO.getSuccessIds().size());
        assertEquals(ok.getId(), respVO.getSuccessIds().get(0));
        assertEquals(1, respVO.getFailures().size());
        assertEquals(bad.getId(), respVO.getFailures().get(0).getId());
        assertNotNull(respVO.getFailures().get(0).getErrorMessage());
        // 失败项保留 DELETING 记录；成功项移除
        assertNull(fileMapper.selectById(ok.getId()));
        assertEquals(FileDO.STATUS_DELETING, fileMapper.selectById(bad.getId()).getStatus());
    }

    // ========== ④ 人工对账入口 ==========

    @Test
    public void reconcile_deletingList_andCleanupRetry() throws Exception {
        FileDO stuck = seedFile("reconcile-stuck");
        // 第一次删除失败 → DELETING
        assertThrows(Exception.class, () -> fileService.deleteFile(stuck.getId()));
        assertEquals(1, fileService.getDeletingFileList().size());
        assertEquals(stuck.getId(), fileService.getDeletingFileList().get(0).getId());

        // 人工重试（存储恢复：不再抛出）→ 完成清理
        resetDeleteToSucceed();
        fileService.reconcileCleanupFile(stuck.getId());
        assertNull(fileMapper.selectById(stuck.getId()));
        assertTrue(fileService.getDeletingFileList().isEmpty());

        // 对不存在记录重复清理 → FILE_NOT_EXISTS
        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileService.reconcileCleanupFile(stuck.getId()));
        assertEquals(FILE_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ========== 造数辅助 ==========

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
        ticket.setPurpose("download");
        ticket.setStatus(FileDeliveryTicketDO.STATUS_REDEEMED);
        ticket.setDeliverySessionId(randomString());
        ticket.setLoginSession("sess-101");
        ticket.setTenantId(1L);
        ticket.setExpiresTime(expiresTime);
        ticket.setRedeemTime(LocalDateTime.now());
        ticketMapper.insert(ticket);
    }

    /** 重置 mock：delete 不再抛出（删除成功路径/存储恢复） */
    private void resetDeleteToSucceed() throws Exception {
        resetDeleteToSucceedFor(null);
    }

    private void resetDeleteToSucceedFor(String onlyPath) throws Exception {
        if (onlyPath == null) {
            org.mockito.Mockito.reset(masterClient);
            when(masterClient.getId()).thenReturn(1L);
            when(masterClient.getContentRange(anyString(), anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(inv -> objectStore.get(inv.getArgument(0, String.class)));
            return;
        }
        // 仅对指定 path 放行删除，其余维持抛出
        doThrow(new IllegalStateException("storage down")).when(masterClient)
                .delete(org.mockito.ArgumentMatchers.argThat(p -> !p.equals(onlyPath)));
        org.mockito.Mockito.doNothing().when(masterClient).delete(onlyPath);
    }

}
