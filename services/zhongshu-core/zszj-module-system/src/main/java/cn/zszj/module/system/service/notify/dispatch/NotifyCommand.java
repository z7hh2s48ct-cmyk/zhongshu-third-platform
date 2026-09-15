package cn.zszj.module.system.service.notify.dispatch;

import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一通知命令（ZS-MSG-001）——绑定事件号 + 业务对象 + 技术租户 + 接收主体。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「建设统一通知命令/结果，绑定事件、业务对象、技术租户和接收主体」。
 * 本命令承载该要求：
 * <ul>
 *   <li>{@code eventId}：业务事件号（幂等键），同一 eventId + 收件人 + 渠道重试不重复生成消息；</li>
 *   <li>{@code bizType/bizId/bizVersion}：业务对象引用（供 MSG-002 待办生命周期消费）；</li>
 *   <li>{@code actorType/actorId/traceId}：事件主体 + 链路追踪（循 JOB-002 OutboxEventMessage 合同）；</li>
 *   <li>技术租户：取自 {@code TenantContextHolder} 当前上下文（命令不含租户字段，调用方无法跨租户串用）。</li>
 * </ul>
 *
 * <p>B05 先使用 System 用户（{@link NotifyRecipient#admin(Long)}），不先建任职关系；
 * 任职路由（按组织/岗位/角色动态解析收件人）归 MSG-001.B（D-09 后追加）。
 */
@Value
@Builder
public class NotifyCommand {

    /** 业务事件号（必填，幂等键）——如 FILE_UPLOADED_<fileId>_<version> */
    String eventId;

    /** 站内信模板编码（必填） */
    String templateCode;

    /** 收件人列表（必填，非空） */
    List<NotifyRecipient> recipients;

    /** 模板参数（可空；缺失必填参数 → TEMPLATE_PARAM_MISSING 状态） */
    Map<String, Object> templateParams;

    /** 关联业务类型（可空，如 infra_file） */
    String bizType;

    /** 关联业务编号（可空） */
    String bizId;

    /** 对象版本（可空；供 MSG-002 待办生命周期乱序/冲突策略依据） */
    String bizVersion;

    /** 事件主体类型（必填，循 JOB-002 OutboxEventMessage 合同） */
    OutboxActorType actorType;

    /** 事件主体编号（可空，如用户 ID / 实例名） */
    String actorId;

    /** 链路追踪 ID（可空） */
    String traceId;

    /** 投递渠道集合（必填，非空；B05 只支持 INBOX） */
    Set<NotifyChannel> channels;

}
