package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.framework.outbox.TokenRevocationCompensationPayload.TokenType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link SystemOutboxEventTypes#TOKEN_REVOCATION_COMPENSATION} 事件的 Sink（ZS-LOGIN-005.B）。
 *
 * <p><b>合同</b>（循 {@link OutboxEventSink}）：
 * <ol>
 *   <li><b>幂等</b>：dispatcher at-least-once 语义下多次 {@link #deliver} 结果一致——
 *       {@code markRevoked} 为 SET NX + EXPIRE 幂等；{@code delete} 已无 key 无副作用；</li>
 *   <li><b>只做投递动作，禁止回写业务事实</b>：Sink 全代码路径<b>无任何写缓存操作</b>
 *       （结构上不可能复活凭据），只调用 {@code revokeWithTombstone} 等价的
 *       {@code markRevoked + delete}；</li>
 *   <li><b>业务回滚保护</b>：反查 DB {@code deleted=FALSE} 且未过期 → 静默 skip 不重放
 *       （防误撤销有效凭据）；</li>
 *   <li><b>过期识别</b>：{@code expiresTime < now} → 静默返回（Redis key TTL 到期自清理，无需墓碑）；</li>
 *   <li><b>物理清理容错</b>：DB 行彻底不存在 → 静默返回（无补偿目标，视为已完成）。</li>
 * </ol>
 *
 * <p><b>当前状态：ZS-LOGIN-005.B GREEN-1 已落地</b>——{@link #deliver} 完整实现 6 步契约；
 * 7 个单元测试（{@code OAuth2TokenRevocationCompensationSinkTest}）覆盖幂等 / 过期 /
 * 穿透反查 / 业务回滚保护 / 物理清理 / REFRESH 分派 / Redis 二次失败全部转绿。
 *
 * @author ZS-LOGIN-005.B
 */
@Component
@Slf4j
public class OAuth2TokenRevocationCompensationSink implements OutboxEventSink {

    /** 访问令牌行反查（穿透逻辑删除）——GREEN 阶段用于业务回滚保护判定与 token 明文回捞。 */
    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;

    /** 刷新令牌行反查（穿透逻辑删除）——GREEN 阶段用于 REFRESH 类型分派。 */
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    /**
     * 撤销动作执行者（{@code markRevoked + delete}）——GREEN 阶段用于重放 {@code .A} 的
     * {@code revokeWithTombstone} 等价语义，Sink 结构上不写任何缓存 key（不可能复活凭据）。
     */
    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @Override
    public boolean supports(String eventType) {
        return SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION.equals(eventType);
    }

    @Override
    public void deliver(OutboxEventRecord event) throws Exception {
        // 步 1：反序列化 payload（格式错误视为不可恢复，上抛进 DEAD 台账由 JOB-004 人工介入）
        TokenRevocationCompensationPayload payload = JsonUtils.parseObject(
                event.getPayload(), TokenRevocationCompensationPayload.class);
        if (payload == null || payload.getTokenId() == null || payload.getTokenType() == null) {
            throw new IllegalStateException(
                    "ZS-LOGIN-005.B payload 缺关键字段（eventId=" + event.getEventId()
                            + ", payload=" + event.getPayload() + "）");
        }

        // 步 2：过期识别（事件载荷快照）——已过期凭据 Redis key TTL 自清理，无需墓碑，静默返回
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime payloadExpires = payload.getExpiresTime();
        if (payloadExpires != null && payloadExpires.isBefore(now)) {
            log.debug("[deliver][eventId={} 凭据已过期（expiresTime={}），静默 skip：Redis TTL 自清理]",
                    event.getEventId(), payloadExpires);
            return;
        }

        // 步 3：按 tokenType 分派反查 DB 行（穿透逻辑删除，flushCache=TRUE 绕开一级缓存）
        TokenRowSnapshot snapshot = payload.getTokenType() == TokenType.ACCESS
                ? oauth2AccessTokenMapper.selectByIdIncludeDeleted(payload.getTokenId())
                : oauth2RefreshTokenMapper.selectByIdIncludeDeleted(payload.getTokenId());

        // 步 4：物理清理容错——行已不存在（被清理任务物理删除），无补偿目标，静默返回
        if (snapshot == null) {
            log.debug("[deliver][eventId={} 行已物理清理（tokenId={} tokenType={}），静默 skip]",
                    event.getEventId(), payload.getTokenId(), payload.getTokenType());
            return;
        }

        // 步 5：业务回滚保护——deleted=FALSE 且未过期说明凭据仍活跃（业务事务回滚 / 事件误发），
        // 静默 skip 不重放撤销（防误踢下线有效用户）。WARN 级留审计痕迹。
        if (Boolean.FALSE.equals(snapshot.getDeleted())) {
            log.warn("[deliver][eventId={} 反查发现 deleted=FALSE（tokenId={} tokenType={}），"
                            + "业务事务可能已回滚，静默 skip 防误撤销有效凭据]",
                    event.getEventId(), payload.getTokenId(), payload.getTokenType());
            return;
        }

        // 步 6：重放撤销（markRevoked + delete）——各自 try/catch 隔离以最大努力尝试两者，
        // 任一失败最终上抛触发 dispatcher 退避重试（避免静默吞异常导致补偿链路断裂）。
        // 结构上无任何写缓存操作，不可能复活凭据（对齐计划 §5-3 边界）。
        String token = snapshot.getToken();
        LocalDateTime effectiveExpires = snapshot.getExpiresTime() != null ? snapshot.getExpiresTime() : payloadExpires;
        long ttlMillis = effectiveExpires != null ? Duration.between(now, effectiveExpires).toMillis() : 0L;

        RuntimeException firstFailure = null;
        try {
            oauth2AccessTokenRedisDAO.markRevoked(token, ttlMillis);
        } catch (RuntimeException ex) {
            firstFailure = ex;
            log.warn("[deliver][eventId={} markRevoked 失败（tokenId={}），继续尝试 delete]",
                    event.getEventId(), payload.getTokenId(), ex);
        }
        try {
            oauth2AccessTokenRedisDAO.delete(token);
        } catch (RuntimeException ex) {
            if (firstFailure == null) {
                firstFailure = ex;
            } else {
                firstFailure.addSuppressed(ex);
            }
            log.warn("[deliver][eventId={} delete 失败（tokenId={}）]",
                    event.getEventId(), payload.getTokenId(), ex);
        }
        if (firstFailure != null) {
            // 原样上抛（保留根因类型与 stack）——dispatcher 会将异常信息写入 outbox_event.last_error，
            // 并按退避策略重试；达最大重试后进入 DEAD 台账（ZS-JOB-004）。
            throw firstFailure;
        }

        log.info("[deliver][eventId={} 补偿成功（tokenId={} tokenType={} ttlMillis={}）]",
                event.getEventId(), payload.getTokenId(), payload.getTokenType(), ttlMillis);
    }

}
