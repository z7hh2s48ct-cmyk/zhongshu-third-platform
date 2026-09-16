package cn.zszj.module.infra.framework.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 文件删除自动补偿与孤儿对象清理配置（ZS-FILE-005.B）。
 *
 * <p>全部取值为「保守占位」，待真实对象存储接入与运行数据积累后实测回填；
 * 禁止被解读为容量/清理时效承诺（循 ZS-JOB-004 阈值占位同一红线）。</p>
 *
 * @author ZS-FILE-005.B
 */
@ConfigurationProperties(prefix = "infra.file.compensation")
@Validated
@Data
public class FileCompensationProperties {

    /**
     * 自动补偿开关（代码默认 false——未显式配置的环境不产生后台对象删除；
     * 部署配置显式开启。补偿只收敛「已获用户批准且已锁定删除意图」的 DELETING 记录，
     * 复用 .A 引用保护与对账语义，不发起新的删除）
     */
    private boolean enabled = false;

    /**
     * 补偿扫描间隔（毫秒）。保守占位 30 分钟，待实测回填。
     */
    private Long intervalMs = 1_800_000L;

    /**
     * DELETING 稳定宽限（分钟）：进入/领取超过该时长才可被补偿领取——
     * 兼作「领取租约时长」与「重试退避」，避免与在途删除/人工对账竞争。保守占位 60 分钟，待实测回填。
     */
    private Long graceMinutes = 60L;

    /**
     * 单轮补偿最多处理记录数（有界，防存储故障恢复后的补偿风暴）。保守占位 100，待实测回填。
     */
    private Integer maxPerCycle = 100;

    /**
     * 孤儿对象（存储有/DB 无记录）清理参数
     */
    private Orphan orphan = new Orphan();

    @Data
    public static class Orphan {

        /**
         * 孤儿对象保留期（天）：存储最后修改时间早于 now-retentionDays 才进入候选。
         * 保守占位 7 天，待真实对象存储接入后实测回填。
         */
        private Integer retentionDays = 7;

        /**
         * 单次清点对象上限（超过即如实标注截断，操作方分前缀多次清点）。保守占位 1000，待实测回填。
         */
        private Integer scanMaxObjects = 1000;

        /**
         * 单次授权清理 path 上限（有界爆炸半径，操作方分批授权）。保守占位 100，待实测回填。
         */
        private Integer cleanupMaxPaths = 100;

    }

}
