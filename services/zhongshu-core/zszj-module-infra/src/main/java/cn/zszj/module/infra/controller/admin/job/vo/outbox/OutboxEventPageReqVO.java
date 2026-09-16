package cn.zszj.module.infra.controller.admin.job.vo.outbox;

import cn.zszj.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台 - Outbox DEAD 事件恢复台账分页 Request VO（ZS-JOB-004）。
 *
 * <p>恢复控制台工作清单固定查 DEAD 事件，服务层强制按 {@code TenantContextHolder} 租户过滤；
 * 本页不引入未被消费的过滤字段（YAGNI），后续如需按 bizType/时间窗筛选再行扩展。
 */
@Schema(description = "管理后台 - Outbox DEAD 事件恢复台账分页 Request VO（ZS-JOB-004）")
@Data
@EqualsAndHashCode(callSuper = true)
public class OutboxEventPageReqVO extends PageParam {

}
