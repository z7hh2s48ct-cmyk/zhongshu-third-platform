package cn.zszj.module.infra.dal.dataobject.job;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 定时任务的租户级执行结果明细（ZS-JOB-001）
 *
 * 一次多租户 Job 执行汇总为一条 {@link JobLogDO}，本表按"执行日志 + 技术租户"逐行展开，
 * 使"某个租户失败"可以被单独查询、单独补偿，而不是被整体执行结果掩盖。
 *
 * 说明：{@link TenantIgnore} 是必需的。本表的 tenant_id 记录的是"被执行的那个技术租户"，
 * 而非当前线程上下文的租户；一次执行会写入多行不同 tenant_id 的数据，
 * 因此不能让租户拦截器按上下文过滤或回填。
 */
@TableName("infra_job_tenant_result")
@KeySequence("infra_job_tenant_result_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class JobTenantResultDO extends BaseDO {

    /**
     * 明细编号
     */
    private Long id;
    /**
     * 执行日志编号
     *
     * 关联 {@link JobLogDO#getId()}
     */
    private Long jobLogId;
    /**
     * 技术租户编号
     *
     * 该行的执行结果归属的租户，非当前上下文租户
     */
    private Long tenantId;
    /**
     * 是否执行成功
     */
    private Boolean success;
    /**
     * 执行时长，单位：毫秒
     */
    private Long durationMs;
    /**
     * 结果摘要（已截断）
     */
    private String resultSummary;
    /**
     * 异常摘要（已截断并脱敏）
     */
    private String errorSummary;

}
