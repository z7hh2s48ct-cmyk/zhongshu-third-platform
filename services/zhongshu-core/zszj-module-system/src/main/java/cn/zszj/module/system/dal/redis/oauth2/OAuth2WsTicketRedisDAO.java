package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.common.util.json.JsonUtils;
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
 *   <li><b>签发限流</b>：{@link #tryAcquireIssueQuota} 按用户约束未消费票据积压（60s 窗口内上限
 *       {@value #MAX_OUTSTANDING_TICKETS_PER_USER} 张），防取票风暴累积；</li>
 *   <li><b>载荷</b>：{@link OAuth2WsTicketStore}（主体快照 + 绑定访问令牌，令牌仅存服务端 Redis，
 *       不随任何响应/URL 出域）。</li>
 * </ol>
 *
 * @author ZS-LOGIN-001.B
 */
@Repository
public class OAuth2WsTicketRedisDAO {

    /** 票据键前缀（一次性短时票据） */
    private static final String TICKET_KEY_PREFIX = "oauth2_ws_ticket:";

    /** 按用户未消费票据积压计数键前缀 */
    private static final String USER_TICKET_COUNT_PREFIX = "oauth2_ws_ticket_user_cnt:";

    /** 单用户 60s 窗口内未消费票据上限（正常场景 ≤3 张：心跳外每次断线重连 1 张） */
    private static final int MAX_OUTSTANDING_TICKETS_PER_USER = 10;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 按用户签发配额 +1（60s 窗口，与票据 TTL 对齐）。
     *
     * @return false = 未消费票据积压超上限，应拒绝签发
     */
    public boolean tryAcquireIssueQuota(Long userId) {
        String key = USER_TICKET_COUNT_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(60));
        }
        return count != null && count <= MAX_OUTSTANDING_TICKETS_PER_USER;
    }

    /**
     * 写入票据（短时 TTL，过期自动清理）。
     */
    public void setTicket(String ticket, OAuth2WsTicketStore store, Duration ttl) {
        stringRedisTemplate.opsForValue().set(TICKET_KEY_PREFIX + ticket, JsonUtils.toJsonString(store), ttl);
    }

    /**
     * 原子消费票据（GETDEL 取删一体，一次性语义）。
     *
     * @return 票据存储载荷；票据不存在/已过期/已被消费（重放）返回 null
     */
    public OAuth2WsTicketStore consumeTicket(String ticket) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(TICKET_KEY_PREFIX + ticket);
        if (value == null) {
            return null;
        }
        // 静默解析（parseObject 失败会把原文落日志；循 LOGIN-005.B 教训）：损坏 → null（fail-closed）
        OAuth2WsTicketStore store = JsonUtils.parseObjectQuietly(value, OAuth2WsTicketStore.class);
        if (store == null || store.getLoginUser() == null
                || store.getLoginUser().getId() == null || store.getLoginUser().getUserType() == null) {
            return null;
        }
        return store;
    }

}
