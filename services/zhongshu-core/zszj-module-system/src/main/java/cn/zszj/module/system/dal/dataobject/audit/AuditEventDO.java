package cn.zszj.module.system.dal.dataobject.audit;

import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计事件 DO（ZS-AUDIT-001 建表 / ZS-AUDIT-002 只读查询）
 *
 * 只追加、不可改写：无 deleted/updater/update_time（不套用 BaseDO 逻辑删除语义）。
 * 查询走显式租户条件（@TenantIgnore 关闭 MP 自动注入，由 AuditEventQueryService 控制范围）。
 *
 * @author ZS-AUDIT-002
 */
@TableName("audit_event")
@TenantIgnore
@Data
public class AuditEventDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 事件类型
     */
    private String eventType;
    /**
     * 主体类型（USER/ADMIN/SYSTEM/WORKER）
     */
    private String actorType;
    /**
     * 主体标识
     */
    private String actorId;
    /**
     * 动作
     */
    private String action;
    /**
     * 业务对象类型
     */
    private String bizType;
    /**
     * 业务对象编号
     */
    private String bizId;
    /**
     * 业务对象版本
     */
    private String bizVersion;
    /**
     * 原因
     */
    private String reason;
    /**
     * 结果（SUCCESS/FAILURE/DENIED）
     */
    private String result;
    /**
     * 明细（JSON，落库前已脱敏）
     */
    private String detail;
    /**
     * 技术租户（可空=系统级）
     */
    private Long tenantId;
    /**
     * 链路标识
     */
    private String traceId;
    /**
     * 幂等键
     */
    private String idempotencyKey;
    /**
     * 创建时间（DB 填充）
     */
    private LocalDateTime createTime;

}
