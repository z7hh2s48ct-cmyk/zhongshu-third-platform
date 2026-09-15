package cn.zszj.framework.quartz.core.whitelist;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.zszj.framework.quartz.core.enums.JobFrameworkErrorCodes.HANDLER_NOT_WHITELISTED_CODE;
import static cn.zszj.framework.quartz.core.enums.JobFrameworkErrorCodes.HANDLER_NOT_WHITELISTED_MSG;

/**
 * JobHandler 白名单校验器（ZS-JOB-001）
 *
 * 统一承载"固化 Handler 白名单"的判定，供四个入口共用，避免各入口各写一套：
 * 1. 任务创建（JobServiceImpl#createJob）；
 * 2. 任务修改（JobServiceImpl#updateJob）；
 * 3. 任务执行（JobHandlerInvoker#executeInternal），执行前拦截，Handler 根本不被取出；
 * 4. 任务手动触发（JobServiceImpl#triggerJob）经由状态校验 + 执行前校验双重把关。
 *
 * 配置来源为 {@code zszj.job.handler-whitelist}，见 {@link JobHandlerWhitelistProperties}。
 */
@RequiredArgsConstructor
@Slf4j
public class JobHandlerWhitelistValidator {

    private final JobHandlerWhitelistProperties properties;

    /**
     * 白名单管控是否已启用。未配置清单时返回 false，表示放行全部 Handler
     */
    public boolean isEnabled() {
        return properties != null && CollUtil.isNotEmpty(properties.getHandlerWhitelist());
    }

    /**
     * 判断 Handler 是否被允许
     *
     * @param handlerName Handler 的 Spring Bean 名字
     * @return 未启用白名单时恒为 true；已启用时只有清单内的名字为 true
     */
    public boolean isWhitelisted(String handlerName) {
        if (!isEnabled()) {
            return true;
        }
        List<String> whitelist = properties.getHandlerWhitelist();
        return handlerName != null && whitelist.contains(handlerName);
    }

    /**
     * 校验 Handler 是否被允许，不允许时抛出业务异常
     *
     * @param handlerName Handler 的 Spring Bean 名字
     */
    public void validate(String handlerName) {
        if (isWhitelisted(handlerName)) {
            return;
        }
        log.warn("[validate][Handler({}) 未列入 zszj.job.handler-whitelist，已拒绝]", handlerName);
        throw exception0(HANDLER_NOT_WHITELISTED_CODE, HANDLER_NOT_WHITELISTED_MSG, handlerName);
    }

}
