package cn.zszj.framework.redis.core;

/**
 * ZS-PERM-004.C：缓存减语义操作类型（驱逐失败补偿的最小信息单元）。
 *
 * <p>用于「失败键清单持久化」——记录驱逐失败时区分操作语义，重放侧据此还原动作：
 * <ul>
 *   <li>{@link #EVICT}：单键驱逐（key 必填）；</li>
 *   <li>{@link #CLEAR}：整缓存清空（无键维度，key 为 null）。</li>
 * </ul>
 *
 * @author ZS-PERM-004.C
 */
public enum CacheEvictionOperation {

    /** 单键驱逐（{@code evict(key)}）。 */
    EVICT,

    /** 整缓存清空（{@code clear()}）。 */
    CLEAR

}
