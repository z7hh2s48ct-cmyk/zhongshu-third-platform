package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OAuth2WsTicketRedisDAO} 一次性短时握手票据的单元测试类（ZS-LOGIN-001.B）。
 *
 * <p>合同：①set → consume 往返还原 LoginUser；②GETDEL 一次性——二次消费返回 null（重放拒绝）；
 * ③不存在的 ticket 返回 null；④损坏/缺关键字段的载荷 fail-closed 返回 null（不建立匿名会话）。
 *
 * @author ZS-LOGIN-001.B
 */
@Import(OAuth2WsTicketRedisDAO.class)
public class OAuth2WsTicketRedisDaoTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2WsTicketRedisDAO wsTicketRedisDAO;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private LoginUser buildLoginUser() {
        return new LoginUser().setId(1024L).setUserType(2).setTenantId(1L)
                .setScopes(java.util.List.of("default"));
    }

    @Test
    public void testSetAndConsume_roundTrip() {
        LoginUser loginUser = buildLoginUser();
        wsTicketRedisDAO.setTicket("ticket-ok", loginUser, Duration.ofSeconds(60));

        LoginUser consumed = wsTicketRedisDAO.consumeTicket("ticket-ok");
        assertNotNull(consumed);
        assertEquals(1024L, consumed.getId());
        assertEquals(2, consumed.getUserType());
        assertEquals(1L, consumed.getTenantId());
    }

    @Test
    public void testConsume_oneTime_secondConsumeRejected() {
        wsTicketRedisDAO.setTicket("ticket-once", buildLoginUser(), Duration.ofSeconds(60));
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
        // 模拟载荷被篡改/结构损坏：非 JSON
        stringRedisTemplate.opsForValue().set("oauth2_ws_ticket:ticket-bad", "{not-json");
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-bad"));
        // 缺关键字段（无 userId/userType）：fail-closed 不建立匿名会话
        stringRedisTemplate.opsForValue().set("oauth2_ws_ticket:ticket-empty", "{\"tenantId\":1}");
        assertNull(wsTicketRedisDAO.consumeTicket("ticket-empty"));
    }

    @Test
    public void testSet_ttlApplied() {
        wsTicketRedisDAO.setTicket("ticket-ttl", buildLoginUser(), Duration.ofSeconds(60));
        Long ttlSeconds = stringRedisTemplate.getExpire("oauth2_ws_ticket:ticket-ttl");
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0 && ttlSeconds <= 60, "票据必须带短时 TTL（<=60s），不得落永久键");
    }

}
