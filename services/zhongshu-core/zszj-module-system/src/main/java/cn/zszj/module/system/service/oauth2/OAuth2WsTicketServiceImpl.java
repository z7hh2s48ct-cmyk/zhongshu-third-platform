package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2WsTicketRedisDAO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * WebSocket 握手一次性短时票据 Service 实现（ZS-LOGIN-001.B）。
 *
 * <p><b>安全语义</b>：
 * <ul>
 *   <li>签发者=当前 SecurityContext 登录主体（端点要求登录态，匿名调用不到）；</li>
 *   <li>票据载荷最小化（userId/userType/tenantId/info/scopes），不含任何令牌明文，
 *       即使票据泄露进访问日志也不构成凭据泄露；</li>
 *   <li>TTL 短时（默认 60s）+ GETDEL 一次性消费：重放/并发握手仅一方成功。</li>
 * </ul>
 *
 * @author ZS-LOGIN-001.B
 */
@Service
@Slf4j
public class OAuth2WsTicketServiceImpl implements OAuth2WsTicketService {

    /** 票据 TTL 秒数（默认 60s：覆盖取票 → 建连窗口，短到泄露即失效） */
    @Value("${zszj.security.ws-ticket.ttl-seconds:60}")
    private int ttlSeconds;

    @Resource
    private OAuth2WsTicketRedisDAO wsTicketRedisDAO;

    @Override
    public String issueTicket() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            // 防御：端点已要求登录态，此处缺主体属异常配置（permitAll 误配等）——拒绝签发而非 NPE
            throw new IllegalStateException("WS 握手票据签发要求登录态（SecurityContext 无登录主体）");
        }
        String ticket = IdUtil.fastSimpleUUID();
        wsTicketRedisDAO.setTicket(ticket, loginUser, Duration.ofSeconds(ttlSeconds));
        log.info("[issueTicket][WS 握手票据已签发 userId={} userType={} ttl={}s]",
                loginUser.getId(), loginUser.getUserType(), ttlSeconds);
        return ticket;
    }

    @Override
    public LoginUser consumeTicket(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            return null;
        }
        return wsTicketRedisDAO.consumeTicket(ticket);
    }

}
