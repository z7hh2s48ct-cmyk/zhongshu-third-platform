package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryChunkRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-004.A：主体绑定交付票据与鉴权取流协议测试（H2）。
 *
 * <p>合同：①本人一次性票据原子兑换一次 + 同会话 Range/续传按同一不可变版本取流；
 * ②重复兑换（同主体同会话）幂等返回既有下载会话、不新建；③转发票据他人拒；④跨技术租户拒；
 * ⑤兑换后退出（登录会话失效）/撤权/过期均拒；⑥导出生成期间撤权拒（技术授权重检）；
 * ⑦在途传输分块重检——撤权后停止后续输出。看守 codex 要点：消费 SQL 真补 owner·tenant·purpose
 * 谓词、默认后端取流不返回存储 URL、会话 ID 不单独代替认证、票据散列存储不明文。
 *
 * @author ZS-FILE-004.A
 */
@Import({FileServiceImpl.class, FileDeliveryServiceImpl.class,
        cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class FileDeliveryTicketTest extends BaseDbUnitTest {

    @Resource
    private FileDeliveryServiceImpl deliveryService;
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

    /** 模拟对象存储：正式键命名空间（后端鉴权取流，不经客户端 URL） */
    private Map<String, byte[]> objectStore;
    private FileClient masterClient;

    @BeforeEach
    public void beforeEach() {
        TenantContextHolder.setTenantId(1L);
        objectStore = new ConcurrentHashMap<>();
        masterClient = mock(FileClient.class);
        when(masterClient.getId()).thenReturn(1L);
        try {
            when(masterClient.getContentRange(anyString(), anyLong(), anyInt())).thenAnswer(inv -> {
                byte[] content = objectStore.get(inv.getArgument(0, String.class));
                if (content == null) {
                    return null;
                }
                long start = inv.getArgument(1, Long.class);
                int length = inv.getArgument(2, Integer.class);
                int from = (int) Math.min(start, content.length);
                int to = (int) Math.min(start + length, content.length);
                return Arrays.copyOfRange(content, from, to);
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

    // ========== ① 本人原子兑换一次 + 同会话 Range/续传 ==========

    @Test
    public void ownerRedeemOnce_thenRangeAndResume_sameImmutableVersion() {
        byte[] content = sequencedBytes(1000);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        String loginSession = "sess-101";

        // 签发：返回一次性明文 token，绝不返回存储 URL（codex ②）
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, loginSession);
        assertNotNull(issued.getTicketToken(), "必须返回一次性票据 token");
        assertTrue(issued.getExpiresTime().isAfter(LocalDateTime.now()), "票据必须有未来有效期");

        // 原子兑换一次
        FileDeliverySessionRespVO session = deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "download", owner, loginSession);
        assertNotNull(session.getDeliverySessionId(), "兑换必须建立下载会话");
        assertEquals(1000L, session.getTotalSize());

        // 恰好一条 REDEEMED 票据（原子一次消费）
        List<FileDeliveryTicketDO> all = ticketMapper.selectList();
        assertEquals(1, all.size(), "兑换不得新增票据行");
        assertEquals(FileDeliveryTicketDO.STATUS_REDEEMED, all.get(0).getStatus());

        // 同会话 Range [0,99] + 续传 [100,199]，按同一不可变版本取流
        FileDeliveryChunkRespVO c1 = deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, owner, loginSession);
        assertArrayEquals(slice(content, 0, 99), c1.getContent(), "Range 首块内容必须精确");
        assertEquals(1000L, c1.getTotalSize());
        assertFalse(c1.isLast());

        FileDeliveryChunkRespVO c2 = deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 100L, 199L, owner, loginSession);
        assertArrayEquals(slice(content, 100, 199), c2.getContent(), "续传块必须按同一版本精确续接");

        // 末块 [900,999]
        FileDeliveryChunkRespVO cLast = deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 900L, 999L, owner, loginSession);
        assertArrayEquals(slice(content, 900, 999), cLast.getContent());
        assertTrue(cLast.isLast(), "覆盖到末尾必须标记 last");
    }

    // ========== ② 重复兑换不新建会话 ==========

    @Test
    public void repeatRedeem_sameSubjectAndSession_returnsSameSession_noNewRow() {
        byte[] content = sequencedBytes(64);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        String loginSession = "sess-101";

        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, loginSession);

        FileDeliverySessionRespVO first = deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "download", owner, loginSession);
        // 断线重连：同主体同会话重复兑换原票据
        FileDeliverySessionRespVO second = deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "download", owner, loginSession);

        assertEquals(first.getDeliverySessionId(), second.getDeliverySessionId(),
                "重复兑换必须幂等返回既有下载会话，不得新建");
        assertEquals(1, ticketMapper.selectList().size(), "重复兑换不得新增票据/会话行");
    }

    // ========== ③ 转发票据他人拒 ==========

    @Test
    public void forwardedTicket_redeemedByOtherUser_rejected() {
        byte[] content = sequencedBytes(64);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, "sess-101");

        // 同租户他人 102 拿到转发 token 兑换——owner 谓词必须拒绝
        LoginUser other = user(102L, 1L);
        when(permissionCommonApi.hasAnyPermissions(102L, "infra:file:query")).thenReturn(true);
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "download", other, "sess-102"));
        assertEquals(FILE_DELIVERY_TICKET_FORBIDDEN.getCode(), ex.getCode(),
                "转发票据被他人兑换必须按 owner 谓词拒绝");
        // 票据仍处 WAITING，未被他人消费
        assertEquals(FileDeliveryTicketDO.STATUS_WAITING,
                ticketMapper.selectList().get(0).getStatus(), "他人兑换失败不得消费票据");
    }

    // ========== ④ 跨技术租户拒 ==========

    @Test
    public void crossTenantRedeem_rejected() {
        byte[] content = sequencedBytes(64);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, "sess-101");

        // 另一技术租户 2 的主体兑换——tenant 谓词必须拒绝
        LoginUser otherTenant = user(101L, 2L);
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "download", otherTenant, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_FORBIDDEN.getCode(), ex.getCode(),
                "跨技术租户兑换必须按 tenant 谓词拒绝");
        assertEquals(FileDeliveryTicketDO.STATUS_WAITING,
                ticketMapper.selectList().get(0).getStatus(), "跨租户兑换失败不得消费票据");
    }

    // ========== ⑤ 兑换后退出 / 撤权 / 过期拒 ==========

    @Test
    public void afterTokenRefresh_samePrincipal_deliveryContinues_acrossPrincipalRejected() {
        byte[] content = sequencedBytes(200);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner, "sess-101", "download");

        // 令牌刷新/重登录：同主体新 token 派生新会话标识——重绑定后交付继续（codex r1 P2）
        FileDeliveryChunkRespVO c1 = deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, owner, "sess-101-refreshed");
        assertArrayEquals(slice(content, 0, 99), c1.getContent(), "同主体令牌刷新不得中断交付");
        // 跨主体持任意会话标识仍拒绝（身份重检先于会话重绑定）
        LoginUser impostor = user(999L, 1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 100L, 199L, impostor, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_FORBIDDEN.getCode(), ex.getCode(),
                "会话 ID 不能单独代替认证，跨主体必须拒绝");
    }

    @Test
    public void afterRedeem_revoked_rejected() {
        byte[] content = sequencedBytes(200);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner, "sess-101", "download");

        // 撤权（本人）：owner·tenant 匹配（codex r0 P1：撤权须授权，仅凭会话 ID 不得撤他人会话）
        deliveryService.revokeDelivery(session.getDeliverySessionId(), owner);

        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, owner, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_REVOKED.getCode(), ex.getCode(), "撤权后取流必须拒绝");
    }

    @Test
    public void afterRedeem_expired_rejected() {
        byte[] content = sequencedBytes(200);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner, "sess-101", "download");

        // 会话过期（下载会话有效期到期）
        FileDeliveryTicketDO ticket = ticketMapper.selectList().get(0);
        ticket.setExpiresTime(LocalDateTime.now().minusMinutes(1));
        ticketMapper.updateById(ticket);

        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, owner, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_EXPIRED.getCode(), ex.getCode(), "过期后取流必须拒绝");
    }

    // ========== ⑥ 导出生成期间撤权拒（技术授权重检） ==========

    @Test
    public void exportPurpose_permissionRevokedBeforeDelivery_rejected() {
        byte[] content = sequencedBytes(128);
        // 文件归属 owner=0（存量/系统资产），管理员 103 持 infra:file:query 走管理面授权
        FileDO file = seedReadableFile(content, "PRIVATE", 0L, 1L);
        LoginUser manager = user(103L, 1L);
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(true);

        // 生成时重检通过 → 签发导出票据
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("export"),
                manager, "sess-103");

        // 导出生成期间撤权：技术授权被回收
        when(permissionCommonApi.hasAnyPermissions(103L, "infra:file:query")).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "export", manager, "sess-103"));
        assertEquals(FILE_DELIVERY_TICKET_REVOKED.getCode(), ex.getCode(),
                "导出生成期间撤权，交付重检必须拒绝");
    }

    // ========== ⑦ 在途传输分块重检停止后续输出 ==========

    @Test
    public void inflightChunkRevocation_stopsSubsequentOutput() {
        byte[] content = sequencedBytes(1000);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner, "sess-101", "download");

        // 首块正常输出
        FileDeliveryChunkRespVO c1 = deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, owner, "sess-101");
        assertArrayEquals(slice(content, 0, 99), c1.getContent());

        // 在途撤权（本人）
        deliveryService.revokeDelivery(session.getDeliverySessionId(), owner);

        // 后续分块必须被重检拦截，停止输出
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 100L, 199L, owner, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_REVOKED.getCode(), ex.getCode(),
                "在途撤权后必须停止后续分块输出");
    }

    // ========== 看守：会话 ID 不单独代替认证（身份重检） ==========

    @Test
    public void sessionIdAlone_notSufficient_identityRechecked() {
        byte[] content = sequencedBytes(200);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner, "sess-101", "download");

        // 持有正确 deliverySessionId + 正确登录会话串，但主体身份不符——仍须拒绝
        LoginUser impostor = user(999L, 1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 99L, impostor, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_FORBIDDEN.getCode(), ex.getCode(),
                "会话 ID 不能单独代替认证，身份必须重检");
    }

    // ========== 看守：用途谓词（codex ①） ==========

    @Test
    public void purposeMismatch_rejected() {
        byte[] content = sequencedBytes(64);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, "sess-101");

        // 以 export 用途兑换 download 票据——purpose 谓词必须拒绝
        ServiceException ex = assertThrows(ServiceException.class, () -> deliveryService.redeemDeliveryTicket(
                issued.getTicketToken(), "export", owner, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_FORBIDDEN.getCode(), ex.getCode(),
                "用途不符必须按 purpose 谓词拒绝");
        assertEquals(FileDeliveryTicketDO.STATUS_WAITING, ticketMapper.selectList().get(0).getStatus());
    }

    // ========== 看守：票据散列存储不明文（codex ⑤） ==========

    @Test
    public void ticketStoredAsHash_notPlaintext() {
        byte[] content = sequencedBytes(64);
        FileDO file = seedReadableFile(content, "PRIVATE", 101L, 1L);
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose("download"),
                owner, "sess-101");

        FileDeliveryTicketDO stored = ticketMapper.selectList().get(0);
        assertNotEquals(issued.getTicketToken(), stored.getTicketHash(), "票据不得明文存储");
        assertEquals(DigestUtil.sha256Hex(issued.getTicketToken()), stored.getTicketHash(),
                "票据必须以 SHA-256 散列存储");
    }

    // ========== 造数辅助 ==========

    private FileDeliverySessionRespVO issueAndRedeem(FileDO file, LoginUser loginUser,
                                                     String loginSession, String purpose) {
        FileDeliveryTicketIssueRespVO issued = deliveryService.issueDeliveryTicket(
                new FileDeliveryTicketIssueReqVO().setFileId(file.getId()).setPurpose(purpose),
                loginUser, loginSession);
        return deliveryService.redeemDeliveryTicket(issued.getTicketToken(), purpose, loginUser, loginSession);
    }

    private FileDO seedReadableFile(byte[] content, String scope, Long ownerUserId, Long tenantId) {
        String path = "asset/" + randomString() + ".bin";
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
        return file;
    }

    private static LoginUser user(Long id, Long tenantId) {
        return new LoginUser().setId(id).setTenantId(tenantId);
    }

    private static byte[] sequencedBytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i % 251);
        }
        return b;
    }

    /** HTTP Range 语义：[start, endInclusive]，越界收敛到内容末尾 */
    private static byte[] slice(byte[] content, int start, int endInclusive) {
        int end = Math.min(endInclusive + 1, content.length);
        return Arrays.copyOfRange(content, start, end);
    }
}
