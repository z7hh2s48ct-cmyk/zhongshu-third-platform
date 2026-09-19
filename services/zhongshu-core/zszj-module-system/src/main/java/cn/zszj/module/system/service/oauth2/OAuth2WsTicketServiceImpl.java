package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.zszj.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2WsTicketRedisDAO;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2WsTicketStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * WebSocket 握手一次性短时票据 Service 实现（ZS-LOGIN-001.B）。
 *
 * <p><b>安全语义</b>：
 * <ul>
 *   <li><b>凭据定位</b>：票据是短时 bearer 凭据（泄露即冒用）——以「密码学随机源 + 60s TTL +
 *       GETDEL 一次性消费 + 绑定访问令牌权威复核」四层收敛风险（r0 P1-1：Hutool fastSimpleUUID
 *       底层 ThreadLocalRandom 非密码学安全，改 SecureRandom 32 字节）；</li>
 *   <li><b>会话绑定</b>（r0 P1-2）：签发时绑定当前访问令牌，消费时经
 *       {@link OAuth2TokenCommonApi#checkAccessToken} 做 DB 权威复核——取票后用户登出/管理员
 *       撤销/令牌到期，TTL 内的票据握手一律拒绝（票据不脱离会话生命周期，对齐 .A 权威核验）；</li>
 *   <li><b>签发限流</b>（r0 P2-8）：按用户约束未消费票据积压（Redis 计数 60s 窗口，上限 10 张），
 *       超限拒绝，防取票风暴累积未消费键；</li>
 *   <li>载荷仅存服务端 Redis（主体快照 + 绑定令牌），不随任何响应/URL 出域。</li>
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

    /** 票据随机源：SecureRandom 32 字节（r0 P1-1：禁用 ThreadLocalRandom 系 fastSimpleUUID） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private OAuth2WsTicketRedisDAO wsTicketRedisDAO;

    @Resource
    private OAuth2TokenCommonApi oauth2TokenApi;

    @Override
    public String issueTicket(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            // 票据必须绑定可权威复核的访问令牌（r0 P1-2），匿名/缺令牌拒绝签发
            throw new IllegalStateException("WS 握手票据签发要求当前访问令牌（Authorization 凭据缺失）");
        }
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            // 防御：端点已要求登录态，此处缺主体属异常配置（permitAll 误配等）——拒绝签发而非 NPE
            throw new IllegalStateException("WS 握手票据签发要求登录态（SecurityContext 无登录主体）");
        }
        if (!wsTicketRedisDAO.tryAcquireIssueQuota(loginUser.getId())) {
            // r0 P2-8：未消费票据积压超限，拒绝签发（对齐限流口径，防取票风暴）
            throw new ServiceException(429, "握手票据签发过于频繁，请稍后重试");
        }
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String ticket = HexFormat.of().formatHex(bytes);
        wsTicketRedisDAO.setTicket(ticket, new OAuth2WsTicketStore(loginUser, accessToken),
                Duration.ofSeconds(ttlSeconds));
        log.info("[issueTicket][WS 握手票据已签发 userId={} userType={} ttl={}s]",
                loginUser.getId(), loginUser.getUserType(), ttlSeconds);
        return ticket;
    }

    @Override
    public LoginUser consumeTicket(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            return null;
        }
        OAuth2WsTicketStore store = wsTicketRedisDAO.consumeTicket(ticket);
        if (store == null) {
            return null;
        }
        LoginUser loginUser = store.getLoginUser();
        // r0 P1-2 会话绑定复核：签发后用户登出/管理员撤销/令牌到期 → checkAccessToken 权威拒绝
        // （对齐 TokenAuthenticationFilter 既有校验口径；票据不脱离会话生命周期）
        try {
            OAuth2AccessTokenCheckRespDTO checked = oauth2TokenApi.checkAccessToken(store.getAccessToken());
            if (checked == null || !Objects.equals(checked.getUserId(), loginUser.getId())) {
                return null;
            }
        } catch (ServiceException e) {
            log.info("[consumeTicket][绑定访问令牌权威核验未通过（已撤销/过期），拒绝握手 userId={}]",
                    loginUser.getId());
            return null;
        }
        return loginUser;
    }

}
