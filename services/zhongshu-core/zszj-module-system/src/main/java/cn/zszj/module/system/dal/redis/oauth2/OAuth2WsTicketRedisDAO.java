package cn.zszj.module.system.dal.redis.oauth2;

import cn.zszj.framework.common.util.json.JsonUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;

import java.util.List;

/**
 * WebSocket 握手一次性短时票据的 Redis DAO（ZS-LOGIN-001.B）。
 *
 * <p><b>合同</b>（替代「?token=<刷新令牌>」迁移期兼容路径，gate {@code refresh-token-as-access-token-enabled}
 * 已随本卡删除）：
 * <ol>
 *   <li><b>短时</b>：TTL 默认 60s（{@code zszj.security.ws-ticket.ttl-seconds}），过期自动清理；</li>
 *   <li><b>一次性</b>：{@link #consumeTicket(String)} 以 GETDEL 原子取删——重放/并发握手只有一方
 *       能取到，其余返回 null（拒绝）；</li>
 *   <li><b>签发限流</b>：{@link #tryAcquireIssueQuota} 按用户固定窗口（60s ≤
 *       {@value #MAX_ISSUE_PER_WINDOW} 次）限流签发频次——消费/过期不归还计数，非「未消费积压」
 *       口径（r2 P3 正名），防取票风暴；</li>
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

    /** 按用户签发频次计数键前缀 */
    private static final String USER_TICKET_COUNT_PREFIX = "oauth2_ws_ticket_user_cnt:";

    /** 单用户签发频次上限（r1 P2-8/P3：固定窗口签发限流——消费/过期不归还计数，非「未消费积压」口径） */
    private static final int MAX_ISSUE_PER_WINDOW = 10;

    /** 配额窗口秒数（与票据 TTL 同量级） */
    private static final int QUOTA_WINDOW_SECONDS = 60;

    /**
     * INCR + EXPIRE 原子化（r1 P2-6：两步分离时，首次 INCR 后 EXPIRE 失败/进程退出会留下
     * 永久键 → 用户被永久拒绝；本脚本对「无 TTL 的存量键」补设窗口，自愈历史脏键）。
     */
    private static final DefaultRedisScript<Long> INCR_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c", Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 按用户签发频次限流 +1（固定窗口 60s）。
     *
     * @return false = 窗口内签发次数超上限，应拒绝签发
     */
    public boolean tryAcquireIssueQuota(Long userId) {
        String key = USER_TICKET_COUNT_PREFIX + userId;
        Long count = stringRedisTemplate.execute(INCR_WINDOW_SCRIPT, List.of(key), String.valueOf(QUOTA_WINDOW_SECONDS));
        return count != null && count <= MAX_ISSUE_PER_WINDOW;
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
