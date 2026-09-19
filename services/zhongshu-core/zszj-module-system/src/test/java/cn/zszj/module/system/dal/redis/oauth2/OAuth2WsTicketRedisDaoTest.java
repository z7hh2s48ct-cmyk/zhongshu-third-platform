package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OAuth2WsTicketRedisDAO} 一次性短时握手票据的单元测试类（ZS-LOGIN-001.B）。
 *
 * <p>合同：①set → consume 往返还原 {@link OAuth2WsTicketStore}（主体 + 绑定访问令牌）；
 * ②GETDEL 一次性——二次消费返回 null（重放拒绝）；③不存在的 ticket 返回 null；
 * ④损坏/缺关键字段的载荷 fail-closed 返回 null（不建立匿名会话）；⑤签发配额约束。
 *
 * @author ZS-LOGIN-001.B
 */
@Import(OAuth2WsTicketRedisDAO.class)
public class OAuth2WsTicketRedisDaoTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2WsTicketRedisDAO wsTicketRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private OAuth2WsTicketStore buildStore(String accessToken) {
        LoginUser loginUser = new LoginUser().setId(1024L).setUserType(2).setTenantId(1L)
                .setScopes(java.util.List.of("default"));
        return new OAuth2WsTicketStore(loginUser, accessToken);
    }

    @Test
    public void testSetAndConsume_roundTrip() {
        wsTicketRedisDAO.setTicket("ticket-ok", buildStore("at-ok"), Duration.ofSeconds(60));

        OAuth2WsTicketStore store = wsTicketRedisDAO.consumeTicket("ticket-ok");
        assertNotNull(store);
        assertEquals(1024L, store.getLoginUser().getId());
        assertEquals(2, store.getLoginUser().getUserType());
        assertEquals("at-ok", store.getAccessToken());
    }

    @Test
    public void testConsume_oneTime_secondConsumeRejected() {
        wsTicketRedisDAO.setTicket("ticket-once", buildStore("at-once"), Duration.ofSeconds(60));
        assertNotNull(wsTicketRedisDAO.consumeTicket("ticket-once"));
        // GETDEL 一次性：重放返回 null
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-once"));
    }

    @Test
    public void testConsume_missingTicket_null() {
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-not-exist"));
    }

    @Test
    public void testConsume_corruptedPayload_failClosed() {
        // 模拟载荷被篡改/结构损坏：非 JSON（静默解析，不落原文日志）
        stringRedisTemplate.opsForValue().set("oauth2_ws_ticket:ticket-bad", "{not-json");
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-bad"));
        // 缺关键字段（无 loginUser）：fail-closed 不建立匿名会话
        stringRedisTemplate.opsForValue().set("oauth2_ws_ticket:ticket-empty", "{\"accessToken\":\"at-x\"}");
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-empty"));
    }

    @Test
    public void testSet_ttlApplied() {
        wsTicketRedisDAO.setTicket("ticket-ttl", buildStore("at-ttl"), Duration.ofSeconds(60));
        Long ttlSeconds = stringRedisTemplate.getExpire("oauth2_ws_ticket:ticket-ttl");
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0 && ttlSeconds <= 60, "票据必须带短时 TTL（<=60s），不得落永久键");
    }

    @Test
    public void testIssueQuota_capAndWindow() {
        // 上限内可签发；达到上限（10）后拒绝；窗口计数带 TTL
        for (int i = 1; i <= 10; i++) {
            assertTrue(wsTicketRedisDAO.tryAcquireIssueQuota(2048L), "第 " + i + " 张应可签发");
        }
        assertFalse(wsTicketRedisDAO.tryAcquireIssueQuota(2048L), "超过 10 张应拒绝");
        Long ttl = stringRedisTemplate.getExpire("oauth2_ws_ticket_user_cnt:2048");
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 60, "配额计数必须带 60s 窗口 TTL");
        // 其他用户不受影响
        assertTrue(wsTicketRedisDAO.tryAcquireIssueQuota(2049L));
    }

}
