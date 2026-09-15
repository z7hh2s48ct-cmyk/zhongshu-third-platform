package cn.zszj.module.infra.framework.inbox;

import lombok.Value;

/**
 * Inbox 记录（ZS-JOB-003）——tryBegin 返回现场与回查端口共用视图。
 */
@Value
public class InboxRecord {

    long inboxId;

    String consumer;

    String eventKey;

    /** 命令载荷指纹（参数冲突检测依据） */
    String payloadHash;

    /** PROCESSING / COMPLETED / FAILED / RESULT_UNKNOWN */
    String status;

    /** 可重放业务结果（COMPLETED 时由首次处理写入，JSON 串） */
    String result;

    int retryCount;

    String bizType;

    String bizId;

    String bizVersion;

    /** 受控失败描述（errorClass/messageLength，不落原文） */
    String lastError;

    Long tenantId;

    String traceId;

}
