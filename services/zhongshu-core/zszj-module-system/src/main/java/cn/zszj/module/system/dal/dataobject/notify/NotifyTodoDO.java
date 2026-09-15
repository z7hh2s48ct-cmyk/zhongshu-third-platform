package cn.zszj.module.system.dal.dataobject.notify;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.module.system.service.notify.todo.NotifyTodoStatus;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 业务待办 DO（ZS-MSG-002）——独立于站内信「已读」语义的业务任务投影。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-002）：区分站内消息与业务待办生命周期——待办承载稳定任务 ID
 * ({@link #todoKey}) + 业务来源 ({@link #sourceType}) + 通用处理状态 ({@link #status}) + 版本
 * ({@link #bizVersion}) + 失效原因 ({@link #statusReason}) + 转派目标 ({@link #assigneeType}/{@link #assigneeId})；
 * 业务完成/撤回/转派经可靠事件更新，<b>已读独立</b>（本 DO 无 readStatus，站内信已读仍由 {@link NotifyMessageDO} 管理）。
 *
 * <p>D-07 红线：{@link #status} 为通用机制级枚举（{@link NotifyTodoStatus}，循 JOB-003 inbox status CHECK 先例），
 * 不含 D-07 试点业务态；Flowable 任务权威留在 BPM，本 DO 仅为其投影，不复制第二套审批引擎。
 *
 * <p>循 MSG-001 {@code NotifySendLogDO} 先例：{@code tenant_id} 取自 TenantContextHolder（不默认 0）；
 * 幂等硬兜底为唯一键 {@code uk_notify_todo_key (tenant_id, source_type, todo_key)}。
 */
@TableName(value = "system_notify_todo")
@KeySequence("system_notify_todo_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTodoDO extends BaseDO {

    /** 待办编号，自增 */
    @TableId
    private Long id;

    /** 稳定业务任务 ID（幂等键，与 source_type+tenant 唯一） */
    private String todoKey;

    /** 业务来源：BPM / FILE / GENERIC（机制级，非试点业务态） */
    private String sourceType;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务编号 */
    private String bizId;

    /** 对象版本（乱序护栏依据，Inbox 水位比较） */
    private String bizVersion;

    /** 待办标题 */
    private String title;

    /** 关联站内信 system_notify_message.id（可空，松耦合） */
    private Long messageId;

    /** 收件人类型（ADMIN / MEMBER） */
    private String recipientType;

    /** 收件人编号 */
    private Long recipientId;

    /** 转派目标类型（可空） */
    private String assigneeType;

    /** 转派目标编号（可空） */
    private Long assigneeId;

    /** 通用机制状态：PENDING/COMPLETED/WITHDRAWN/REASSIGNED/INVALID */
    private String status;

    /** 失效/状态原因（脱敏，不落敏感原文） */
    private String statusReason;

    /** 乐观锁：状态流转递增，防并发覆盖 */
    private Long todoVersion;

    /** 技术租户（取自 TenantContextHolder） */
    private Long tenantId;

}
