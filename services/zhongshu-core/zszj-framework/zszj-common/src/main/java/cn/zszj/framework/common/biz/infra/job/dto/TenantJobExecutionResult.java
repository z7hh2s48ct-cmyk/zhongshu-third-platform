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
