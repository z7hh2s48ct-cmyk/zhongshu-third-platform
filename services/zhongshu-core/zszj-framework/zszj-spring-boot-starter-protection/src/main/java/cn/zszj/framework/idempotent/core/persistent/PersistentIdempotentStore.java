package cn.zszj.framework.idempotent.core.persistent;

import java.util.Optional;

/**
 * 持久化幂等存储 SPI（ZS-SEC-011.B）
 *
 * 由 protection starter 定义、业务基建模块（zszj-module-infra 的 JdbcPersistentIdempotentStore，
 * 循 ZS-JOB-002 Outbox 的 infra 承载先例）落地——starter 不反向依赖业务模块。
 *
 * 实现契约（违反即破坏持久化幂等保证，评审按 P1 对待）：
 * <ol>
 *     <li><b>事务参与强制（MANDATORY 语义）</b>：写方法（tryInsertRunning/markSuccess/markFailed/deleteRunning）
 *         必须加入调用方在本数据源上的真实事务，无事务即抛 {@code IllegalTransactionStateException}，
 *         不另开事务自提交——INSERT RUNNING → 业务 → markSuccess 与业务同事务提交，是
 *         「丢响应可复用原结果、重启不重复写、业务回滚无残留记录」三项保证的原子性根基；</li>
 *     <li><b>唯一约束并发兜底</b>：tryInsertRunning 必须对幂等键唯一约束原子——插入成功返回 true（获得执行权）；
 *         键已存在返回 false 且<b>不得使调用方事务进入 aborted 状态</b>（PG 须用
 *         {@code ON CONFLICT DO NOTHING}，不能用会 abort 事务的裸 INSERT 冲突路径），
 *         保证并发败者仍能 findByIdempotentKey 读取记录做「重复/冲突/复用」分类；</li>
 *     <li><b>fail-closed</b>：任何存储异常向上抛出，不吞异常、不静默降级放行业务；</li>
 *     <li><b>脱敏</b>：实现侧日志不得落请求原文、结果快照原文或幂等键原文（键一律指纹化）。</li>
 * </ol>
 *
 * @author 众墅之家
 */
public interface PersistentIdempotentStore {

    /**
     * 尝试为幂等键插入一条 RUNNING 记录（在调用方事务内）
     *
     * @param record 记录（status 置 RUNNING 写入）
     * @return true = 插入成功（本请求获得执行权）；false = 键已存在（他请求持有或已完成，调用方应改走读回分类）
     */
    boolean tryInsertRunning(PersistentIdempotentRecord record);

    /**
     * 按幂等键查询记录
     *
     * @param idempotentKey 幂等键
     * @return 记录；不存在返回 {@link Optional#empty()}
     */
    Optional<PersistentIdempotentRecord> findByIdempotentKey(String idempotentKey);

    /**
     * 业务成功：RUNNING → SUCCESS 并写入结果快照（在调用方事务内，与业务同生共死）
     *
     * @param idempotentKey  幂等键
     * @param resultSnapshot 结果 JSON 快照；null 表示无法快照（重放退化为状态级复用）
     */
    void markSuccess(String idempotentKey, String resultSnapshot);

    /**
     * 业务失败：RUNNING → FAILED（在调用方事务内；{@code deleteKeyWhenException=false} 时保留记录，重放一律拒绝）
     *
     * @param idempotentKey 幂等键
     */
    void markFailed(String idempotentKey);

    /**
     * 删除 RUNNING 记录（在调用方事务内；{@code deleteKeyWhenException=true} 默认路径：失败即删，客户端可重试）
     *
     * @param idempotentKey 幂等键
     * @return 是否删除了记录
     */
    boolean deleteRunning(String idempotentKey);

}
