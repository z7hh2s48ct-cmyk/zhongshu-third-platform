package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.zszj.framework.test.core.util.RandomUtils.randomString;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DELIVERY_TICKET_FORBIDDEN;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DELIVERY_TICKET_REVOKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-FILE-004.A 批次执行入口（docs/03 §B04）：交付票据并发测试（H2，codex r3 请求的并发回归看守）。
 *
 * <p>合同：①同一票据多线程并发兑换——恰好一个下载会话（CAS 恰好一次成功、其余幂等返回既有会话）；
 * ②并发兑换中跨主体线程全部拒绝且不消费票据；③兑换后撤权对所有后续取流立即生效。</p>
 */
@Import({FileServiceImpl.class, FileDeliveryServiceImpl.class,
        cn.zszj.module.infra.framework.file.config.FileConfiguration.class})
public class DeliveryTicketConcurrencyTest extends BaseDbUnitTest {

    @Resource
    private FileDeliveryServiceImpl deliveryService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileDeliveryTicketMapper ticketMapper;

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

    // ========== ① 并发兑换同一票据：恰好一个下载会话 ==========

    @Test
    public void concurrentRedeem_sameTicket_exactlyOneSession() throws Exception {
        FileDO file = seedReadableFile();
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = issue(file, owner);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        // codex r0 P2：逐线程收集结果（会话 ID 或异常），不吞任何失败
        List<RedeemOutcome> outcomes = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    FileDeliverySessionRespVO session = deliveryService.redeemDeliveryTicket(
                            issued.getTicketToken(), "download", owner, "sess-101");
                    outcomes.add(RedeemOutcome.ok(session.getDeliverySessionId()));
                } catch (Exception ex) {
                    outcomes.add(RedeemOutcome.fail(ex));
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发兑换应在时限内完成");
        pool.shutdown();

        assertEquals(threads, outcomes.size(), "8 个线程必须全部返回");
        for (RedeemOutcome outcome : outcomes) {
            assertNull(outcome.error(), "同主体同会话并发兑换不得出现拒绝/异常");
            assertNotNull(outcome.sessionId());
        }
        assertEquals(1, outcomes.stream().map(RedeemOutcome::sessionId).distinct().count(),
                "并发兑换必须收敛到唯一下载会话（CAS 恰好一次）");
        List<FileDeliveryTicketDO> tickets = ticketMapper.selectList();
        assertEquals(1, tickets.size(), "并发兑换不得新增票据行");
        assertEquals(FileDeliveryTicketDO.STATUS_REDEEMED, tickets.get(0).getStatus());
        assertNotNull(tickets.get(0).getDeliverySessionId());
    }

    // ========== ② 并发兑换：跨主体线程全部拒绝、票据不被消费 ==========

    @Test
    public void concurrentRedeem_ownerAndImpostors_onlyOwnerPathSucceeds() throws Exception {
        FileDO file = seedReadableFile();
        LoginUser owner = user(101L, 1L);
        FileDeliveryTicketIssueRespVO issued = issue(file, owner);

        int impostors = 6;
        ExecutorService pool = Executors.newFixedThreadPool(1 + impostors);
        CountDownLatch start = new CountDownLatch(1);
        // codex r0 P2：逐线程记录结果——每个冒名线程都必须以 FORBIDDEN 失败，任何冒名成功都判失败
        List<RedeemOutcome> outcomes = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1 + impostors);
        // 1 个主体线程 + N 个冒名线程同时兑换
        pool.submit(() -> {
            try {
                start.await();
                outcomes.add(RedeemOutcome.ok(deliveryService.redeemDeliveryTicket(
                        issued.getTicketToken(), "download", owner, "sess-101").getDeliverySessionId()));
            } catch (Exception ex) {
                outcomes.add(RedeemOutcome.fail(ex));
            } finally {
                done.countDown();
            }
        });
        for (int i = 0; i < impostors; i++) {
            final int seq = i;
            pool.submit(() -> {
                try {
                    start.await();
                    outcomes.add(RedeemOutcome.ok(deliveryService.redeemDeliveryTicket(
                            issued.getTicketToken(), "download", user(200L + seq, 1L),
                            "sess-impostor-" + seq).getDeliverySessionId()));
                } catch (Exception ex) {
                    outcomes.add(RedeemOutcome.fail(ex));
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发兑换应在时限内完成");
        pool.shutdown();

        assertEquals(1 + impostors, outcomes.size(), "全部线程必须返回结果");
        for (RedeemOutcome outcome : outcomes) {
            if (outcome.ownerResult()) {
                assertNull(outcome.error(), "主体本人兑换应成功");
                assertNotNull(outcome.sessionId());
            } else {
                assertNotNull(outcome.error(), "冒名兑换必须失败");
                assertTrue(outcome.error() instanceof ServiceException
                                && ((ServiceException) outcome.error()).getCode() == FILE_DELIVERY_TICKET_FORBIDDEN.getCode(),
                        "冒名兑换必须按 FORBIDDEN 拒绝");
            }
        }
        assertEquals(1, outcomes.stream().filter(RedeemOutcome::ownerResult)
                .map(RedeemOutcome::sessionId).distinct().count(), "仅主体本人兑换成功");
        FileDeliveryTicketDO ticket = ticketMapper.selectList().get(0);
        assertEquals(FileDeliveryTicketDO.STATUS_REDEEMED, ticket.getStatus());
        assertEquals(101L, ticket.getOwnerUserId(), "会话主体必须为票据绑定主体");
    }

    // ========== ③ 兑换后撤权：对所有后续取流立即生效 ==========

    @Test
    public void revokeAfterRedeem_allSubsequentReadsRejected() throws Exception {
        FileDO file = seedReadableFile();
        LoginUser owner = user(101L, 1L);
        FileDeliverySessionRespVO session = issueAndRedeem(file, owner);
        deliveryService.revokeDelivery(session.getDeliverySessionId(), owner);

        ServiceException ex = assertThrowsServiceException(() -> deliveryService.readDeliveryChunk(
                session.getDeliverySessionId(), 0L, 9L, owner, "sess-101"));
        assertEquals(FILE_DELIVERY_TICKET_REVOKED.getCode(), ex.getCode());
    }

    /**
     * 并发兑换逐线程结果（codex r0 P2：不吞异常、按线程断言）。
     */
    private record RedeemOutcome(String sessionId, Exception error) {

        static RedeemOutcome ok(String sessionId) {
            return new RedeemOutcome(sessionId, null);
        }

        static RedeemOutcome fail(Exception error) {
            return new RedeemOutcome(null, error);
        }

        boolean ownerResult() {
            return error == null;
        }

    }

    // ========== 造数辅助 ==========

    private FileDeliveryTicketIssueRespVO issue(FileDO file, LoginUser owner) {
        FileDeliveryTicketIssueReqVO reqVO = new FileDeliveryTicketIssueReqVO();
        reqVO.setFileId(file.getId());
        reqVO.setPurpose("download");
        return deliveryService.issueDeliveryTicket(reqVO, owner, "sess-101");
    }

    private FileDeliverySessionRespVO issueAndRedeem(FileDO file, LoginUser owner) {
        FileDeliverySessionRespVO session = deliveryService.redeemDeliveryTicket(
                issue(file, owner).getTicketToken(), "download", owner, "sess-101");
        assertNotNull(session.getDeliverySessionId());
        return session;
    }

    private ServiceException assertThrowsServiceException(Runnable action) {
        try {
            action.run();
        } catch (ServiceException ex) {
            return ex;
        }
        throw new AssertionError("期望抛出 ServiceException 但未抛出");
    }

    private FileDO seedReadableFile() {
        String path = "asset/" + randomString() + ".bin";
        byte[] content = "concurrency-probe".getBytes();
        objectStore.put(path, content);
        FileDO file = FileDO.builder()
                .configId(1L).name("asset.bin").path(path)
                .url("https://oss.example.com/" + path)
                .type("application/octet-stream").size((long) content.length)
                .fileHash(DigestUtil.sha256Hex(content))
                .ownerUserId(101L).scope("PRIVATE")
                .build();
        file.setTenantId(1L);
        fileMapper.insert(file);
        return fileMapper.selectById(file.getId());
    }

    private static LoginUser user(Long id, Long tenantId) {
        return new LoginUser().setId(id).setTenantId(tenantId);
    }

}
