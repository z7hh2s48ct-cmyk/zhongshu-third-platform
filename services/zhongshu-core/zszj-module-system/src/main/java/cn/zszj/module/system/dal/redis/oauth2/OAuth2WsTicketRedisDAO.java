package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.security.core.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * WebSocket 握手一次性短时票据的 Redis DAO（ZS-LOGIN-001.B）。
 *
 * <p><b>合同</b>（替代「?token=<刷新令牌>」迁移期兼容路径，gate {@code refresh-token-as-access-token-enabled}
 * 已随本卡删除）：
 * <ol>
 *   <li><b>短时</b>：TTL 默认 60s（{@code zszj.security.ws-ticket.ttl-seconds}），过期自动清理；</li>
 *   <li><b>一次性</b>：{@link #consumeTicket(String)} 以 GETDEL 原子取删——重放/并发握手只有一方
 *       能取到，其余返回 null（拒绝）；</li>
 *   <li><b>载荷最小化</b>：仅存 userId/userType/tenantId/info/scopes（供 WS 会话归属），不落任何令牌明文。</li>
 * </ol>
 *
 * @author ZS-LOGIN-001.B
 */
@Repository
public class OAuth2WsTicketRedisDAO {

    /** 票据键前缀（一次性短时票据，非凭据：不放行任何 API，仅供 WS 握手消费） */
    private static final String TICKET_KEY_PREFIX = "oauth2_ws_ticket:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入票据（短时 TTL，过期自动清理）。
     */
    public void setTicket(String ticket, LoginUser loginUser, Duration ttl) {
        stringRedisTemplate.opsForValue().set(TICKET_KEY_PREFIX + ticket, JsonUtils.toJsonString(loginUser), ttl);
    }

    /**
     * 原子消费票据（GETDEL 取删一体，一次性语义）。
     *
     * @return 票据对应的登录用户；票据不存在/已过期/已被消费（重放）返回 null
     */
    public LoginUser consumeTicket(String ticket) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(TICKET_KEY_PREFIX + ticket);
        if (value == null) {
            return null;
        }
        // 静默解析（parseObject 失败会把原文落日志；票据非凭据但仍按 fail-closed 口径）：
        // JSON 损坏 → null；结构损坏/缺关键字段（userId/userType）→ null，不建立匿名 WS 会话
        LoginUser loginUser = JsonUtils.parseObjectQuietly(value, LoginUser.class);
        if (loginUser == null || loginUser.getId() == null || loginUser.getUserType() == null) {
            return null;
        }
        return loginUser;
    }

}
