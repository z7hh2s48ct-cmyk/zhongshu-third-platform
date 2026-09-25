package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.redis.core.CacheEvictionFailureRecorder;
import cn.zszj.framework.redis.core.CacheEvictionOperation;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZS-PERM-004.C：{@link CacheEvictionFailureRecorder} 的 outbox 实现（失败键清单持久化）。
 *
 * <p>为什么需要：驱逐失败发生在事务提交后的 afterCommit 路径（无事务上下文），
 * {@code ReliableEventPort.append} 的 MANDATORY 传播与租户强制无法直接满足——
 * 本实现自开独立短事务（<b>REQUIRES_NEW</b>：afterCommit 窗口下 ConnectionHolder 仍绑定且
 * {@code isTransactionActive()==true}，REQUIRED 会静默并入已提交事务导致写入随连接归还丢失，
 * CodeReview R1 P1）+ port 懒解析（{@code ObjectProvider}，infra 缺位时降级为仅日志）。
 *
 * <p>链路：重试耗尽 → record → 进程内去重（防双层重试重复记录）→ outbox 事件预写（PENDING）→
 * dispatcher 重放 → {@link CacheEvictionCompensationSink} 完成最终驱逐；超限转 DEAD 人工台账。
 *
 * <p>降级契约（逐项独立，均不向驱逐调用方传播）：
 * <ul>
 *   <li>port 不可用（infra 未装配 / system 单独运行）→ WARN 降级仅日志，不占用去重窗口；</li>
 *   <li>租户上下文缺失 → ERROR 降级（不伪造归属，循 {@code JdbcReliableEventPort} 合同）；</li>
 *   <li>写入异常 → ERROR 降级仅日志，并释放去重窗口允许后续重试。</li>
 * </ul>
 *
 * <p>载荷零秘密扩散：仅缓存名 / 键 / 操作 / 失败时间，异常对象不入载荷（只进日志）。
 *
 * @author ZS-PERM-004.C
 */
@Component
@Slf4j
public class OutboxCacheEvictionFailureRecorder implements CacheEvictionFailureRecorder {

    /** 进程内去重窗口（毫秒）——框架可能对同一失败重复调用，窗口内同键同操作仅记首条。 */
    private static final long DEDUP_WINDOW_MILLIS = 5_000L;

    /** CLEAR 操作 biz_id 的键占位（无具体键）。 */
    private static final String CLEAR_KEY_PLACEHOLDER = "__clear__";

    /**
     * 进程内去重台账：dedupKey（cacheName:key:operation）→ 最近一次记录时刻（epochMillis）。
     * 每次 {@link #record} 入口惰性清扫超窗条目（CodeReview R1 P2：防台账随时间无界增长；
     * record 为低频故障路径，O(n) 清扫可接受；上界 = 窗口内不同失败键数）。
     */
    private final ConcurrentHashMap<String, Long> recentFailures = new ConcurrentHashMap<>();

    private final ObjectProvider<ReliableEventPort> reliableEventPortProvider;
    private final TransactionTemplate transactionTemplate;

    public OutboxCacheEvictionFailureRecorder(ObjectProvider<ReliableEventPort> reliableEventPortProvider,
                                              PlatformTransactionManager transactionManager) {
        this.reliableEventPortProvider = reliableEventPortProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // CodeReview R1 P1：afterCommit 主路径下 ConnectionHolder 仍绑定且 isTransactionActive()==true——
        // REQUIRED 会静默并入已提交事务（参与者无显式 commit，落库只剩连接归还副作用兜底）；
        // REQUIRES_NEW 挂起现有资源、开独立短事务显式提交，确保补偿事件确定性落库
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void record(String cacheName, String key, CacheEvictionOperation operation, RuntimeException cause) {
        String dedupKey = cacheName + ":" + key + ":" + operation.name();
        long now = System.currentTimeMillis();
        sweepExpired(now);
        try {
            // 步 1：进程内去重——窗口内同键同操作仅记首条（事件清单去重，非业务幂等）
            Long last = recentFailures.get(dedupKey);
            if (last != null && now - last < DEDUP_WINDOW_MILLIS) {
                log.debug("[record][cache({}) key({}) op({}) 命中 {}ms 去重窗口，跳过重复记录]",
                        cacheName, key, operation, DEDUP_WINDOW_MILLIS);
                return;
            }
            recentFailures.put(dedupKey, now);

            // 步 2：port 懒解析——infra 未装配时降级为仅日志（不阻断驱逐调用方，不占用去重窗口）
            ReliableEventPort port = reliableEventPortProvider.getIfAvailable();
            if (port == null) {
                recentFailures.remove(dedupKey, now);
                log.warn("[record][cache({}) key({}) op({}) ZS-PERM-004.C 降级：ReliableEventPort 不可用，"
                        + "补偿事件未持久化（仅 ERROR 留痕）]", cacheName, key, operation);
                return;
            }

            // 步 3：租户上下文强制——缺失即降级（不伪造归属，循 JdbcReliableEventPort 合同）
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                recentFailures.remove(dedupKey, now);
                log.error("[record][cache({}) key({}) op({}) 租户上下文缺失，补偿事件无法归属（拒绝伪造），"
                        + "本次记录降级为仅日志]", cacheName, key, operation);
                return;
            }

            // 步 4：构建事件（载荷零秘密扩散；biz_id = cacheName:key 供 JOB-004 恢复台账人工排查）
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("cacheName", cacheName);
            payloadMap.put("key", key);
            payloadMap.put("operation", operation.name());
            payloadMap.put("failedAt", DateUtils.now());
            OutboxEventMessage message = OutboxEventMessage.builder()
                    .eventType(SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION)
                    .bizType(SystemOutboxEventTypes.BIZ_TYPE_CACHE_EVICTION)
                    .bizId(cacheName + ":" + (key == null ? CLEAR_KEY_PLACEHOLDER : key))
                    .payload(payloadMap)
                    .actorType(OutboxEventMessage.OutboxActorType.SYSTEM)
                    .traceId(TracerUtils.getTraceId())
                    .build();

            // 步 5：自开独立短事务（驱逐失败点无事务上下文；port 合同 PROPAGATION_MANDATORY 要求事务内调用；
            // REQUIRES_NEW 见构造——afterCommit 窗口并入已有事务会丢失落库）
            transactionTemplate.execute(status -> port.append(message));

            log.info("[record][cache({}) key({}) op({}) 驱逐失败补偿事件已持久化（tenantId={}）]",
                    cacheName, key, operation, tenantId);
        } catch (RuntimeException ex) {
            // 记录故障隔离：补偿证据写入失败仅 ERROR 留痕，绝不向驱逐调用方传播；
            // 释放去重窗口——写失败允许后续真实失败重新尝试记录
            recentFailures.remove(dedupKey, now);
            log.error("[record][cache({}) key({}) op({}) 补偿事件写入失败（降级：仅日志）]",
                    cacheName, key, operation, ex);
        }
    }

    /** 惰性清扫：移除超出去重窗口的过期条目（防台账无界增长；record 为低频故障路径，全扫可接受）。 */
    private void sweepExpired(long now) {
        recentFailures.entrySet().removeIf(entry -> now - entry.getValue() >= DEDUP_WINDOW_MILLIS);
    }

}
