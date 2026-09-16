package cn.zszj.module.system.service.notify.channel;

/**
 * 渠道发送可重试异常（ZS-MSG-004）——提交结果未知/仍未知/技术异常时由服务抛出，
 * 经 {@code OutboxDispatcherService} 捕获推进 outbox_event 失败退避（next_retry_at 后重投；
 * 连续 5 次失败转 DEAD 人工处置）。消息只含受控描述（状态/类别），不携带渠道响应原文或联系方式
 * （自由文本无可靠值级脱敏，循 JOB-002 last_error 先例）。
 */
public class NotifyChannelSendRetryableException extends RuntimeException {

    public NotifyChannelSendRetryableException(String message) {
        super(message);
    }

}
