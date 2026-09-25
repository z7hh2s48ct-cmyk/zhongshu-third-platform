package cn.zszj.module.system.framework.outbox;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.redis.core.CacheEvictionOperation;
import cn.zszj.framework.redis.core.CacheEvictionReplayContext;
import cn.zszj.module.infra.framework.outbox.OutboxEventRecord;
import cn.zszj.module.infra.framework.outbox.OutboxEventSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * ZS-PERM-004.C：缓存驱逐失败补偿 Sink——重放失败键清单，完成最终驱逐。
 *
 * <p>合同（循 {@link OutboxEventSink}）：
 * <ol>
 *   <li><b>幂等</b>：evict/clear 天然幂等（重复重放 = 再驱动一次，多清无害）；</li>
 *   <li><b>只做减语义</b>（驱逐/清空），禁止回写缓存——重放写回旧值 = 补偿制造不一致
 *       （由静态扫描测试锁定）；</li>
 *   <li><b>失败不静默</b>：重放再次失败必须抛出（触发 dispatcher 退避重试 / 超限 DEAD 台账）；</li>
 *   <li><b>防自我循环</b>：重放副作用在 {@link CacheEvictionReplayContext} 作用域内执行——
 *       驱逐若再次失败不再产生新补偿事件，交由 dispatcher 退避 / DEAD 兜底；</li>
 *   <li><b>物理清理容错</b>：目标 cache 不存在 → 静默返回（无补偿目标视为完成）；
 *       payload 损坏 → 抛出（进 DEAD 人工可见，不静默丢失补偿证据）。</li>
 * </ol>
 *
 * <p>事务边界：dispatcher 的 Sink 投递在领取/确认短事务之外执行（无事务上下文），
 * 故本 Sink 的 evict/clear 立即生效、不被事务装饰器延迟，重放失败异常可被 dispatcher 感知。
 *
 * @author ZS-PERM-004.C
 */
@Component
@Slf4j
public class CacheEvictionCompensationSink implements OutboxEventSink {

    private final CacheManager cacheManager;

    public CacheEvictionCompensationSink(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean supports(String eventType) {
        return SystemOutboxEventTypes.CACHE_EVICTION_COMPENSATION.equals(eventType);
    }

    @Override
    public void deliver(OutboxEventRecord event) throws Exception {
        // 步 1：反序列化 payload（静默解析——异常消息只含受控摘要，不留 payload 原文日志）；
        // 损坏视为不可恢复，上抛进 DEAD 台账由 JOB-004 人工介入（不静默跳过）
        CacheEvictionCompensationPayload payload = JsonUtils.parseObjectQuietly(
                event.getPayload(), CacheEvictionCompensationPayload.class);
        if (payload == null || payload.getCacheName() == null || payload.getOperation() == null) {
            throw new IllegalStateException(
                    "ZS-PERM-004.C payload 缺关键字段或反序列化失败（eventId=" + event.getEventId()
                            + ", payloadLength=" + (event.getPayload() == null ? 0 : event.getPayload().length())
                            + ", bizId=" + event.getBizId() + "）：人工介入请查 outbox_event.payload");
        }

        // 步 2：物理清理容错——目标 cache 不存在（缓存已移除/重建）视为无补偿目标，静默返回
        Cache cache = cacheManager.getCache(payload.getCacheName());
        if (cache == null) {
            log.debug("[deliver][eventId={} 缓存 {} 不存在，视为无补偿目标，静默 skip]",
                    event.getEventId(), payload.getCacheName());
            return;
        }

        // 步 3：重放驱逐/清空（replay scope 内执行——驱逐若再次失败不产生新补偿事件，
        // 异常原样上抛交由 dispatcher 退避重试 / 超限 DEAD + JOB-004 人工台账兜底）
        CacheEvictionReplayContext.runInReplayScope(() -> {
            if (CacheEvictionOperation.EVICT.name().equals(payload.getOperation())) {
                if (payload.getKey() == null) {
                    throw new IllegalStateException(
                            "EVICT 事件必须携带 key（eventId=" + event.getEventId() + "）");
                }
                cache.evict(payload.getKey());
            } else if (CacheEvictionOperation.CLEAR.name().equals(payload.getOperation())) {
                cache.clear();
            } else {
                throw new IllegalStateException("未知操作类型：" + payload.getOperation()
                        + "（eventId=" + event.getEventId() + "）");
            }
        });

        log.info("[deliver][eventId={} 补偿成功（cache={} key={} op={}）]",
                event.getEventId(), payload.getCacheName(), payload.getKey(), payload.getOperation());
    }

}
