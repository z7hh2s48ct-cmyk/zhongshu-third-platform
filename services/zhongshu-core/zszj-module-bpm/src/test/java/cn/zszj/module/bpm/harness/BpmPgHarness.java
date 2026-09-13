package cn.zszj.module.bpm.harness;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ZS-BPM-001 中性技术夹具常量与引擎事件记录器。
 *
 * 边界（对齐 docs/02 第 6.5 节 D-07 确认前禁令）：夹具流程只含引擎技术语义
 * （部署/发起/审批完成/拒绝分支/撤回/转办/异步任务），不固化任何众墅业务字段、
 * 业务状态机或组织任职语义；流程键统一 tech_neutral_* 前缀。
 */
public final class BpmPgHarness {

    /** 中性审批流程键（发起→人工任务→按完成变量分支→两个结束态）。 */
    public static final String PROCESS_APPROVAL = "tech_neutral_approval";
    /** 中性异步回声流程键（发起→异步服务任务→结束）。 */
    public static final String PROCESS_ASYNC_ECHO = "tech_neutral_async_echo";

    /** 审批完成变量名（通过/拒绝两分支的唯一路由依据，属夹具技术变量而非业务字段）。 */
    public static final String VAR_OUTCOME = "harness_outcome";
    public static final String OUTCOME_APPROVE = "approve";
    public static final String OUTCOME_REJECT = "reject";

    /** 技术租户标签（引擎 tenantId 仅是标签，不含 ACL 语义——业务组织隔离归 B08/D-07）。 */
    public static final String TENANT_1 = "1";
    public static final String TENANT_2 = "2";

    /** 撤回原因（runtimeService.deleteProcessInstance 的技术语义）。 */
    public static final String WITHDRAW_REASON = "harness:withdraw";

    private BpmPgHarness() {
    }

    /**
     * 引擎事件计数器：证明 EngineConfigurationConfigurer#setEventListeners 装配扩展点
     * （与 BpmFlowableConfiguration 同一机制）在真实 PG 引擎上确实生效。
     * 静态存储：同一 JVM 内跨 Spring 上下文可读。
     */
    public static final class EngineEventRecorder implements
            org.flowable.common.engine.api.delegate.event.FlowableEventListener {

        private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();

        public static long count(String eventType) {
            return COUNTERS.getOrDefault(eventType, new AtomicLong()).get();
        }

        public static void reset() {
            COUNTERS.clear();
        }

        @Override
        public void onEvent(org.flowable.common.engine.api.delegate.event.FlowableEvent event) {
            COUNTERS.computeIfAbsent(event.getType().name(), k -> new AtomicLong()).incrementAndGet();
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }

        @Override
        public boolean isFireOnTransactionLifecycleEvent() {
            return false;
        }

        @Override
        public String getOnTransaction() {
            return null;
        }
    }

    /** 简单轮询等待（无 Awaitility 依赖）。 */
    public static boolean waitUntil(Duration timeout, java.util.function.BooleanSupplier condition) {
        Objects.requireNonNull(condition);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }
}
