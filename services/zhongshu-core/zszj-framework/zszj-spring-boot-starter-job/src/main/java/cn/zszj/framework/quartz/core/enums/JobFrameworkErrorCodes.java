package cn.zszj.framework.quartz.core.enums;

/**
 * 定时任务框架层错误码常量（ZS-JOB-001）
 *
 * 号段说明：infra 的 JOB_* 错误码使用 1-001-009-xxx 段，其中 000~004 已被 ZS-JOB-002（事务 Outbox）占用，
 * 本任务在同一号段内自 005 起顺延，不跨段、不占用其它号段。
 *
 * 这里只登记"框架层需要抛出、同时业务层错误码目录也要引用"的条目，
 * 使错误码的数字与文案有单一真源：框架层直接用 {@code exception0(CODE, MSG, args)} 抛出，
 * infra 的 ErrorCodeConstants 用 {@code new ErrorCode(CODE, MSG)} 登记，两者的 code 与 message 必然一致。
 */
public interface JobFrameworkErrorCodes {

    /**
     * JobHandler 未列入白名单
     */
    int HANDLER_NOT_WHITELISTED_CODE = 1_001_009_005;
    String HANDLER_NOT_WHITELISTED_MSG = "定时任务的处理器({})未列入白名单，禁止登记或执行";

    /**
     * 存在租户级失败：整体执行结果判失败，但不触发调度器重放（避免已成功租户的副作用被重复执行）
     */
    int TENANT_PARTIAL_FAILURE_CODE = 1_001_009_008;
    String TENANT_PARTIAL_FAILURE_MSG = "定时任务存在租户级失败：成功({})个，失败({})个";

}
