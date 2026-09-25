package cn.zszj.framework.redis.core;

import org.springframework.lang.Nullable;

/**
 * ZS-PERM-004.C：缓存驱逐失败记录 SPI（框架层定义、应用层实现——依赖反转）。
 *
 * <p>为什么定义在框架层：starter-redis 不依赖 DB/事务；而「失败键清单持久化 + 可靠重放」
 * 需要 outbox（应用层能力）。故框架层只定义记录点接口，由应用层（system 模块）实现为
 * outbox 事件预写，经 dispatcher 重放至补偿 Sink 完成最终驱逐。
 *
 * <p>实现合同：
 * <ul>
 *   <li>记录失败必须静默降级（仅日志），不得向驱逐调用方传播异常；</li>
 *   <li>重放上下文内（{@link CacheEvictionReplayContext#isInReplay()}）不得记录（防自我循环）。</li>
 * </ul>
 *
 * @author ZS-PERM-004.C
 */
public interface CacheEvictionFailureRecorder {

    /**
     * 记录一次驱逐失败（重试耗尽后恰记录一次）。
     *
     * @param cacheName 缓存名（如 role / user_role_ids）
     * @param key       失败键的字符串形式；{@code null} 表示 CLEAR 操作
     * @param operation 操作类型
     * @param cause     重试耗尽时的最终根因
     */
    void record(String cacheName, @Nullable String key, CacheEvictionOperation operation, RuntimeException cause);

}
