package cn.zszj.framework.redis.core;

/**
 * ZS-PERM-004.C：补偿重放上下文（ThreadLocal 标记，防「记录 → 重放 → 再记录」自我循环）。
 *
 * <p>重放路径（dispatcher → 补偿 Sink → cache.evict/clear）会再次经过 {@link RetryEvictCache}
 * 记录点；若重放失败又记录新事件，驱逐持续失败将无限自增事件（放大风暴）。故重放副作用
 * 必须在作用域内执行——失败交由 dispatcher 退避重试 / 超限转 DEAD 人工台账（ZS-JOB-004）。
 *
 * @author ZS-PERM-004.C
 */
public final class CacheEvictionReplayContext {

    private static final ThreadLocal<Boolean> IN_REPLAY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private CacheEvictionReplayContext() {
    }

    /** 当前线程是否处于补偿重放作用域。 */
    public static boolean isInReplay() {
        return Boolean.TRUE.equals(IN_REPLAY.get());
    }

    /**
     * 在重放作用域内执行动作（支持嵌套：退出时恢复外层状态，最外层退出清理 ThreadLocal）。
     */
    public static void runInReplayScope(Runnable action) {
        boolean previous = isInReplay();
        IN_REPLAY.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (previous) {
                IN_REPLAY.set(Boolean.TRUE);
            } else {
                IN_REPLAY.remove();
            }
        }
    }

}
