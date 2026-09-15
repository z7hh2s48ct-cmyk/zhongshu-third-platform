package cn.zszj.module.infra.framework.inbox;

import lombok.Builder;
import lombok.Value;

/**
 * Inbox 消费命令（ZS-JOB-003）——消费者业务代码在业务事务内经 {@link ConsumerInboxPort#tryBegin} 抢位。
 *
 * <p>处理键 = 技术租户（取自 {@code TenantContextHolder} 当前上下文，缺失即拒绝）+ {@code consumer}
 * + {@code eventKey}（事件 ID 或业务命令键）；{@code payloadHash} 为命令载荷指纹——同键不同指纹按
 * 参数冲突拒绝，不得当作相同成功（docs/05 ZS-JOB-003 调整条款）。
 */
@Value
@Builder
public class InboxCommand {

    /** 消费者标识（必填，如 sink/服务名） */
    String consumer;

    /** 事件/业务命令键（必填，如 outbox 事件 ID 或业务唯一号） */
    String eventKey;

    /** 命令载荷指纹（必填，如 SHA-256；同键不同指纹=参数冲突） */
    String payloadHash;

    /** 关联业务类型（可空；版本乱序护栏按此分组） */
    String bizType;

    /** 关联业务编号（可空） */
    String bizId;

    /** 对象版本（可空；启用版本护栏时与同对象已完成记录比较） */
    String bizVersion;

    /** 是否启用版本乱序护栏（旧版本不覆盖新状态——仅对可解析为整数的版本生效，语义由事件类型解释，D-07 登记） */
    boolean checkVersionStale;

    /** 链路追踪 ID（可空） */
    String traceId;

}
