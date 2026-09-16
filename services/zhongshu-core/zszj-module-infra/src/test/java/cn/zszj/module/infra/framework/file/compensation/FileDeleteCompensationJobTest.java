package cn.zszj.module.infra.framework.file.compensation;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileCompensationProperties;
import cn.zszj.module.infra.framework.file.config.FileConfiguration;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.module.infra.service.file.FileServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-005.B：超时 DELETING 记录自动补偿测试（H2，轮询 + DB 条件 CAS 领取）。
 *
 * <p>合同（§16.1 L1016：超时/重启/重复清理不误删且最终对账一致）：
 * ①超时 DELETING 补偿收敛（复用 .A reconcileCleanupFile 引用保护语义）；②存储仍失败保留记录并前推
 * deleting_time（失败可见可重试、不假报成功）；③引用保护不得被补偿绕过；④并发补偿被 CAS 领取串行化；
 * ⑤重复补偿不重复删对象；⑥开关关闭全程 no-op；⑦deleting_time 为 NULL 的存量记录不扫描（迁移回填职责）。</p>
 */
@Import({FileServiceImpl.class, FileConfiguration.class, FileDeleteCompensationJob.class})
public class FileDeleteCompensationJobTest extends BaseDbUnitTest {

    @Resource
    private FileDeleteCompensationJob compensationJob;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileDeliveryTicketMapper ticketMapper;
    @Resource
    private FileCompensationProperties compensationProperties;

    @MockitoBean
    private cn.zszj.module.infra.service.file.FileConfigService fileConfigService;
    @MockitoBean
    private cn.zszj.framework.common.biz.system.permission.PermissionCommonApi permissionCommonApi;

    /** 模拟对象存储：以 path 记录「是否仍存在」 */
    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        // 默认全关（代码保守默认），各用例显式打开，杜绝上下文缓存串味
        compensationProperties.setEnabled(true);
        compensationProperties.setGraceMinutes(60L);
        compensationProperties.setMaxPerCycle(100);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            // 默认 delete 抛存储异常（失败路径夹具）；成功路径用例显式重置
            doThrow(new IllegalStateException("storage down")).when(masterClient).delete(anyString());
            when(masterClient.getContentRange(anyString(), anyLong(), anyInt())).thenAnswer(inv -> {
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
        compensationProperties.setEnabled(false);
        TenantContextHolder.clear();
    }

    // ========== ① 超时 DELETING 补偿收敛：记录移除、对象删除 ==========

    @Test
    public void compensate_timeoutDeleting_storageRecovered_converges() throws Exception {
        FileDO file = seedDeletingFile("comp-conv", 120);
        // 存储恢复：delete 成功并从模拟存储移除
        org.mockito.Mockito.reset(masterClient);
        when(masterClient.getId()).thenReturn(1L);
        org.mockito.Mockito.doAnswer(inv -> {
            objectStore.remove(inv.getArgument(0, String.class));
            return null;
        }).when(masterClient).delete(anyString());
        when(masterClient.getContentRange(anyString(), anyLong(), anyInt()))
                .thenAnswer(inv -> objectStore.get(inv.getArgument(0, String.class)));

        compensationJob.compensate();

        assertNull(fileMapper.selectById(file.getId()), "补偿收敛后记录移除");
        assertFalse(objectStore.containsKey(file.getPath()), "对象被删除");
    }

    // ========== ② 存储仍失败：保留 DELETING 记录、deleting_time 前推（退避）、异常不上抛 ==========

    @Test
    public void compensate_storageStillFailing_keepsRecordAndAdvancesDeletingTime() throws Exception {
        FileDO file = seedDeletingFile("comp-fail", 120);
        LocalDateTime before = fileMapper.selectById(file.getId()).getDeletingTime();

        assertDoesNotThrow(() -> compensationJob.compensate(), "补偿逐项异常隔离，不向调度线程上抛");

        FileDO stuck = fileMapper.selectById(file.getId());
        assertNotNull(stuck, "存储仍失败保留可恢复记录（不假报成功）");
        assertEquals(FileDO.STATUS_DELETING, stuck.getStatus());
        assertNotNull(stuck.getDeletingTime());
        assertTrue(stuck.getDeletingTime().isAfter(before), "领取前推 deleting_time，构成退避（下轮 grace 后才可再领）");
    }

    // ========== ③ 引用保护：补偿不得绕过（复用 .A reconcileCleanupFile 语义） ==========

    @Test
    public void compensate_referencedByActiveDelivery_skipped() throws Exception {
        FileDO file = seedDeletingFile("comp-ref", 120);
        insertDeliverySession(file.getId(), LocalDateTime.now().plusMinutes(10));

        compensationJob.compensate();

        FileDO kept = fileMapper.selectById(file.getId());
        assertNotNull(kept, "被引用文件保留记录");
        assertEquals(FileDO.STATUS_DELETING, kept.getStatus());
        assertTrue(objectStore.containsKey(file.getPath()), "引用保护：对象不得被补偿删除");
        verify(masterClient, never()).delete(anyString());
    }

    // ========== ④ 并发补偿被 CAS 领取串行化 ==========

    @Test
    public void claimCas_secondClaimantRejected_andFreshClaimSkippedByJob() throws Exception {
        FileDO file = seedDeletingFile("comp-cas", 120);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime graceBefore = now.minusMinutes(compensationProperties.getGraceMinutes());

        assertEquals(1, fileMapper.claimDeletingForCompensation(file.getId(), graceBefore, now),
                "首个领取者 CAS 成功");
        assertEquals(0, fileMapper.claimDeletingForCompensation(file.getId(), graceBefore, now),
                "第二个领取者（实例/人工）败者 affected=0");

        // 已被领取（deleting_time 新鲜）→ 本轮补偿跳过，不重复删对象
        compensationJob.compensate();
        assertNotNull(fileMapper.selectById(file.getId()));
        verify(masterClient, never()).delete(anyString());
    }

    // ========== ⑤ 重复补偿不误删：成功后无候选、对象只删一次 ==========

    @Test
    public void compensate_repeatAfterSuccess_noDoubleDelete() throws Exception {
        FileDO file = seedDeletingFile("comp-repeat", 120);
        org.mockito.Mockito.reset(masterClient);
        when(masterClient.getId()).thenReturn(1L);
        org.mockito.Mockito.doAnswer(inv -> {
            objectStore.remove(inv.getArgument(0, String.class));
            return null;
        }).when(masterClient).delete(anyString());
        when(masterClient.getContentRange(anyString(), anyLong(), anyInt()))
                .thenAnswer(inv -> objectStore.get(inv.getArgument(0, String.class)));

        compensationJob.compensate();
        compensationJob.compensate(); // 重启/下一轮：记录已移除，无候选

        verify(masterClient, times(1)).delete(file.getPath());
        assertNull(fileMapper.selectById(file.getId()));
        assertFalse(objectStore.containsKey(file.getPath()));
    }

    // ========== ⑥ 开关关闭：全程 no-op ==========

    @Test
    public void compensate_disabled_noop() throws Exception {
        compensationProperties.setEnabled(false);
        FileDO file = seedDeletingFile("comp-off", 120);

        compensationJob.compensate();

        assertNotNull(fileMapper.selectById(file.getId()), "开关关闭不产生补偿");
        verify(masterClient, never()).delete(anyString());
    }

    // ========== ⑦ deleting_time 为 NULL 的存量记录不扫描（超时语义确定性，回填归迁移） ==========

    @Test
    public void compensate_nullDeletingTime_notCandidate() throws Exception {
        FileDO file = seedDeletingFile("comp-null", null);

        compensationJob.compensate();

        assertNotNull(fileMapper.selectById(file.getId()), "无进入时间的 DELETING 记录不由补偿处理");
        verify(masterClient, never()).delete(anyString());
    }

    // ========== 造数辅助 ==========

    private FileDO seedDeletingFile(String tag, Integer minutesAgo) {
        String path = "asset/" + tag + "-" + System.nanoTime() + ".bin";
        byte[] content = ("content-" + tag).getBytes();
        objectStore.put(path, content);
        FileDO file = FileDO.builder()
                .configId(1L).name(tag + ".bin").path(path)
                .url("https://oss.example.com/" + path)
                .type("application/octet-stream").size((long) content.length)
                .fileHash(DigestUtil.sha256Hex(content))
                .ownerUserId(101L).scope("PRIVATE")
                .status(FileDO.STATUS_DELETING)
                .build();
        file.setTenantId(1L);
        file.setDeletingTime(minutesAgo == null ? null : LocalDateTime.now().minusMinutes(minutesAgo));
        fileMapper.insert(file);
        return file;
    }

    private void insertDeliverySession(Long fileId, LocalDateTime expiresTime) {
        FileDeliveryTicketDO ticket = new FileDeliveryTicketDO();
        ticket.setTicketHash(DigestUtil.sha256Hex("t" + System.nanoTime()));
        ticket.setFileId(fileId);
        ticket.setOwnerUserId(101L);
        ticket.setPurpose("download");
        ticket.setStatus(FileDeliveryTicketDO.STATUS_REDEEMED);
        ticket.setDeliverySessionId("sess-" + System.nanoTime());
        ticket.setLoginSession("sess-101");
        ticket.setTenantId(1L);
        ticket.setExpiresTime(expiresTime);
        ticket.setRedeemTime(LocalDateTime.now());
        ticketMapper.insert(ticket);
    }

}
