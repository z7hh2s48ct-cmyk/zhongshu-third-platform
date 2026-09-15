package cn.zszj.module.infra.service.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务调度器对账报告（ZS-JOB-001）
 *
 * 对账以任务表 infra_job 为权威源，调度器为被修正方，因此报告只描述"发现并修正了什么"，
 * 不描述任务表被改写——任务表不会被对账改写。
 */
@Data
public class JobSchedulerReconcileReport {

    /**
     * 调度器是否可用。定时任务被禁用（无 Scheduler Bean）时为 false，此时不做任何检查与修正
     */
    private boolean schedulerAvailable;
    /**
     * 检查过的任务数量
     */
    private int checked;
    /**
     * 发现漂移的数量（含孤儿触发器）
     */
    private int drifted;
    /**
     * 成功修正的数量
     */
    private int corrected;
    /**
     * 修正失败的数量
     */
    private int failed;
    /**
     * 调度器中存在、但任务表已无对应任务的孤儿触发器数量
     */
    private int orphanTriggerCount;
    /**
     * 漂移明细，按发现顺序排列：先任务表侧，后孤儿触发器侧
     */
    private final List<Drift> drifts = new ArrayList<>();

    void recordChecked() {
        checked++;
    }

    void recordDrift(String handlerName, String type, Integer jobStatus, String triggerState) {
        drifted++;
        drifts.add(new Drift(handlerName, type, jobStatus, triggerState));
    }

    void recordCorrected() {
        corrected++;
    }

    void recordFailed() {
        failed++;
    }

    void recordOrphanTrigger() {
        orphanTriggerCount++;
    }

    /**
     * 一行式摘要，供日志与运维接口直接输出
     *
     * @return 摘要文本
     */
    public String getSummary() {
        return String.format("调度器可用(%s) 检查(%d) 漂移(%d) 已修正(%d) 修正失败(%d) 孤儿触发器(%d)",
                schedulerAvailable, checked, drifted, corrected, failed, orphanTriggerCount);
    }

    /**
     * 单条漂移记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Drift {

        /**
         * Handler 名字，即 infra_job.handler_name 与 Quartz 的 JobKey/TriggerKey 名字
         */
        private String handlerName;
        /**
         * 漂移类型，取值见 {@link JobSchedulerReconciler} 的 DRIFT_* 常量
         */
        private String type;
        /**
         * 任务表状态（权威侧），孤儿触发器场景下为 null
         */
        private Integer jobStatus;
        /**
         * 调度器触发器状态名（被修正侧）
         */
        private String triggerState;

    }

}
