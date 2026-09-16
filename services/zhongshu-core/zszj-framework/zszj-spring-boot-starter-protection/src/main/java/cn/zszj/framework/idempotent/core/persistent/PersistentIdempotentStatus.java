package cn.zszj.framework.idempotent.core.persistent;

/**
 * 持久化幂等记录的状态机（ZS-SEC-011.B）
 *
 * 与表 {@code infra_persistent_idempotent} 的 status CHECK 约束一一对应：
 * <ul>
 *     <li>{@link #RUNNING}：已获得执行权、业务执行中（含事务已提交但业务未完成的唯一形态——
 *         MANDATORY 参与调用方事务后，RUNNING 与业务同事务，正常路径不会悬挂）；</li>
 *     <li>{@link #SUCCESS}：业务成功，可与结果快照一起复用（快照为空时退化为状态级复用）；</li>
 *     <li>{@link #FAILED}：业务失败且 {@code deleteKeyWhenException=false} 保留记录（重放一律拒绝）。
 *         默认 {@code deleteKeyWhenException=true} 时失败即删记录、不存在 FAILED 状态。</li>
 * </ul>
 *
 * @author 众墅之家
 */
public enum PersistentIdempotentStatus {

    /** 执行中（获得执行权、业务未提交） */
    RUNNING,

    /** 业务成功（可按快照复用原结果，或状态级复用） */
    SUCCESS,

    /** 业务失败且保留记录（重放一律拒绝，不做结果复用） */
    FAILED

}
