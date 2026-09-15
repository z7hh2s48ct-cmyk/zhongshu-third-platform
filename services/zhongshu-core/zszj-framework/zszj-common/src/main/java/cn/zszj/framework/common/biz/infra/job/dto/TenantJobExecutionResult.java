package cn.zszj.framework.common.biz.infra.job.dto;

import cn.zszj.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多租户 Job 的结构化执行结果（ZS-JOB-001）
 *
 * 由多租户 JobHandler 切面产出，由定时任务调用者消费，用于保证：
 * 1. 某租户失败不会被汇总为整体成功——顶层直接给出成功/失败计数；
 * 2. 每技术租户的成功、失败、耗时可单独查询与补偿——{@link #perTenantResults} 保留逐租户明细。
 *
 * 放在 zszj-common 而非定时任务/多租户 starter 内，是因为多租户 starter 依赖定时任务 starter，
 * 生产方（多租户切面）与消费方（定时任务调用者、infra 持久化）需要一个双方都可见的单一类型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantJobExecutionResult {

    /**
     * 执行日志结果列（infra_job_log.result）的宽度上限
     *
     * 结构化结果会随租户数线性增长，超出列宽后执行日志写库会失败，
     * 而那里的异常被吞掉，最终表现为执行记录永久停留在“运行中”——比截断更难排查。
     * 写入执行日志前统一走 {@link #toBoundedJsonString(int)}。
     */
    public static final int JOB_LOG_RESULT_MAX_LENGTH = 4000;

    /**
     * 参与执行的租户总数
     */
    private int totalTenants;
    /**
     * 执行成功的租户数
     */
    private int successCount;
    /**
     * 执行失败的租户数。大于 0 时，整体执行结果即判失败
     */
    private int failureCount;
    /**
     * 逐租户执行明细，key 为技术租户编号
     */
    @Builder.Default
    private Map<Long, TenantItem> perTenantResults = new LinkedHashMap<>();
    /**
     * 逐租户明细是否因超出长度上限而被裁剪
     *
     * 裁剪只发生在写入执行日志的有界副本上，不影响顶层计数，也不影响单独落库的完整明细。
     */
    private boolean truncated;

    /**
     * 是否全部租户执行成功
     */
    @JsonIgnore
    public boolean isAllSuccess() {
        return totalTenants > 0 && failureCount == 0;
    }

    /**
     * 是否存在租户级失败（含全部失败与部分失败）
     */
    @JsonIgnore
    public boolean isPartialFailure() {
        return failureCount > 0;
    }

    public String toJsonString() {
        return JsonUtils.toJsonString(this);
    }

    /**
     * 产出长度不超过 maxLength 的 JSON，用于写入执行日志的结果列
     *
     * 逐租户明细已单独落 infra_job_tenant_result，因此超长时按“先丢成功明细、再逐条丢明细”的顺序裁剪，
     * 顶层计数（totalTenants/successCount/failureCount）始终完整——消费方靠 failureCount 判定整体成败，
     * 明细裁剪不能影响判定。产出必须是合法 JSON：直接截断字符串会得到半个 JSON，让执行日志不可解析。
     *
     * @param maxLength 长度上限
     * @return 有界 JSON；未超长时等价于 {@link #toJsonString()}
     */
    public String toBoundedJsonString(int maxLength) {
        String full = toJsonString();
        if (full.length() <= maxLength) {
            return full;
        }
        // 第一步：只保留失败租户明细，运维排查失败时需要它，成功租户的明细在明细表里查
        TenantJobExecutionResult bounded = copyWithFailedDetailsOnly();
        String json = bounded.toJsonString();
        // 第二步：失败租户仍装不下时逐条丢弃明细，直到顶层计数骨架能装进列宽
        while (json.length() > maxLength && !bounded.getPerTenantResults().isEmpty()) {
            removeLastDetail(bounded.getPerTenantResults());
            json = bounded.toJsonString();
        }
        return json;
    }

    /**
     * 拷一份只带失败租户明细、且标记为已裁剪的副本，顶层计数原样保留
     */
    private TenantJobExecutionResult copyWithFailedDetailsOnly() {
        Map<Long, TenantItem> failedDetails = new LinkedHashMap<>();
        if (perTenantResults != null) {
            perTenantResults.forEach((tenantId, item) -> {
                if (item != null && !item.isSuccess()) {
                    failedDetails.put(tenantId, item);
                }
            });
        }
        TenantJobExecutionResult copy = new TenantJobExecutionResult();
        copy.setTotalTenants(totalTenants);
        copy.setSuccessCount(successCount);
        copy.setFailureCount(failureCount);
        copy.setPerTenantResults(failedDetails);
        copy.setTruncated(true);
        return copy;
    }

    /**
     * 丢掉最后一条明细。用遍历取尾键而不是迭代器，避开 LinkedHashMap 迭代中 remove 的限制
     */
    private static void removeLastDetail(Map<Long, TenantItem> details) {
        Long lastKey = null;
        for (Long key : details.keySet()) {
            lastKey = key;
        }
        if (lastKey != null) {
            details.remove(lastKey);
        }
    }

    /**
     * 尝试把 JobHandler 的返回值解析为租户级结构化结果
     *
     * 只有同时具备"租户总数 > 0"和"逐租户明细"的 JSON 才被认定为本类型，
     * 普通 JobHandler 的任意返回值（纯文本、其它 JSON）一律返回 null，避免误判。
     *
     * @param json JobHandler 的返回值
     * @return 解析成功返回结果对象；不是租户级结构化结果时返回 null
     */
    public static TenantJobExecutionResult tryParse(String json) {
        if (json == null || json.isBlank() || !JsonUtils.isJsonObject(json)) {
            return null;
        }
        TenantJobExecutionResult result = JsonUtils.parseObjectQuietly(json, TenantJobExecutionResult.class);
        if (result == null || result.getTotalTenants() <= 0
                || result.getPerTenantResults() == null || result.getPerTenantResults().isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * 单个技术租户的执行明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TenantItem {

        /**
         * 该租户是否执行成功
         */
        private boolean success;
        /**
         * 该租户的执行耗时，单位：毫秒
         */
        private long durationMs;
        /**
         * 成功时的业务返回值
         */
        private String result;
        /**
         * 失败时的异常根因摘要
         */
        private String error;

    }

}
