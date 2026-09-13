package cn.zszj.framework.apilog.core.filter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.apilog.core.annotation.ApiAccessLog;
import cn.zszj.framework.apilog.core.enums.OperateTypeEnum;
import cn.zszj.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.filter.ApiRequestFilter;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static cn.zszj.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.zszj.framework.common.util.json.JsonUtils.toJsonString;

/**
 * API 访问日志 Filter
 *
 * 目的：记录 API 访问日志到数据库中
 *
 * @author 芋道源码
 */
@Slf4j
public class ApiAccessLogFilter extends ApiRequestFilter {

    private final String applicationName;

    private final ApiAccessLogCommonApi apiAccessLogApi;

    public ApiAccessLogFilter(WebProperties webProperties, String applicationName, ApiAccessLogCommonApi apiAccessLogApi) {
        super(webProperties);
        this.applicationName = applicationName;
        this.apiAccessLogApi = apiAccessLogApi;
    }

    /**
     * ZS-SEC-012.B codex r0/r1 P1：必须参与 ASYNC 派发——OncePerRequestFilter 默认跳过异步派发，
     * 导致异步请求的访问日志只能挂在首次派发（结果未定，伪报成功）。参与后可在 ASYNC 派发内
     * 以最终结果记录（租户上下文由派发期过滤器重建，归属正确）。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 获得开始时间
        LocalDateTime beginTime = LocalDateTime.now();
        // 提前获得参数，避免 XssFilter 过滤处理
        Map<String, String> queryString = ServletUtils.getParamMap(request);
        String requestBody = ServletUtils.getBody(request);

        try {
            // 继续过滤器
            filterChain.doFilter(request, response);
            // ZS-SEC-012.B codex r0 P1：异步请求（Callable / StreamingResponseBody / SSE）在首次 REQUEST
            // 派发返回时尚未完成响应——此时 CommonResult 未写入，立即记录必然 result=null 伪报成功
            //（异步异常也伪报成功）。处理：首次派发直接返回【不记录】，等容器 ASYNC 派发完成后，
            // 过滤器链会再次执行（TenantContextWebFilter 等同步重建上下文），在派发内以最终结果记录——
            // codex r1 P1：AsyncListener 回调方案会在租户上下文清理后执行，导致日志归属 tenant_id=0，故弃用。
            if (request.isAsyncStarted()) {
                return;
            }
            boolean asyncDispatch = request.getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC;
            Exception asyncFailure = null;
            if (asyncDispatch) {
                // codex r1 P1：异步派发若未写 CommonResult 且响应已失败（超时/错误），不得伪报成功
                CommonResult<?> dispatched = WebFrameworkUtils.getCommonResult(request);
                if (dispatched == null && response.getStatus() >= 400) {
                    asyncFailure = new IllegalStateException("async dispatch failed with status " + response.getStatus());
                }
            }
            // 记录日志（同步请求原行为；异步请求在 ASYNC 派发内以最终结果记录，幂等护栏防重复）
            createApiAccessLog(request, beginTime, queryString, requestBody, asyncFailure);
        } catch (Exception ex) {
            // 异常执行，记录日志
            createApiAccessLog(request, beginTime, queryString, requestBody, ex);
            throw ex;
        }
    }

    private void createApiAccessLog(HttpServletRequest request, LocalDateTime beginTime,
                                    Map<String, String> queryString, String requestBody, Exception ex) {
        // ZS-SEC-012.B codex r0 P1：同一请求只记录一条访问日志——ASYNC 二次派发时本 Filter 会再次执行，
        // 而 complete 回调已记录过；以请求属性做幂等护栏
        if (request.getAttribute(getClass().getName() + ".RECORDED") != null) {
            return;
        }
        request.setAttribute(getClass().getName() + ".RECORDED", Boolean.TRUE);
        ApiAccessLogCreateReqDTO accessLog = new ApiAccessLogCreateReqDTO();
        try {
            boolean enable = buildApiAccessLog(accessLog, request, beginTime, queryString, requestBody, ex);
            if (!enable) {
                return;
            }
            apiAccessLogApi.createApiAccessLogAsync(accessLog);
        } catch (Throwable th) {
            log.error("[createApiAccessLog][url({}) traceId({}) 写入访问日志失败]", request.getRequestURI(), TracerUtils.getCorrelationId(request), th);
        }
    }

    private boolean buildApiAccessLog(ApiAccessLogCreateReqDTO accessLog, HttpServletRequest request, LocalDateTime beginTime,
                                      Map<String, String> queryString, String requestBody, Exception ex) {
        // 判断：是否要记录操作日志
        HandlerMethod handlerMethod = (HandlerMethod) request.getAttribute(ATTRIBUTE_HANDLER_METHOD);
        ApiAccessLog accessLogAnnotation = null;
        if (handlerMethod != null) {
            accessLogAnnotation = handlerMethod.getMethodAnnotation(ApiAccessLog.class);
            if (accessLogAnnotation != null && BooleanUtil.isFalse(accessLogAnnotation.enable())) {
                return false;
            }
        }

        // 处理用户信息
        accessLog.setUserId(WebFrameworkUtils.getLoginUserId(request))
                .setUserType(WebFrameworkUtils.getLoginUserType(request));
        // 设置访问结果
        CommonResult<?> result = WebFrameworkUtils.getCommonResult(request);
        if (result != null) {
            accessLog.setResultCode(result.getCode()).setResultMsg(result.getMsg());
        } else if (ex != null) {
            accessLog.setResultCode(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode())
                    .setResultMsg(ExceptionUtil.getRootCauseMessage(ex));
        } else {
            accessLog.setResultCode(GlobalErrorCodeConstants.SUCCESS.getCode()).setResultMsg("");
        }
        // 设置请求字段
        accessLog.setTraceId(TracerUtils.getCorrelationId(request)).setApplicationName(applicationName)
                .setRequestUrl(request.getRequestURI()).setRequestMethod(request.getMethod())
                .setUserAgent(ServletUtils.getUserAgent(request)).setUserIp(ServletUtils.getClientIP(request));
        String[] sanitizeKeys = accessLogAnnotation != null ? accessLogAnnotation.sanitizeKeys() : null;
        Boolean requestEnable = accessLogAnnotation != null ? accessLogAnnotation.requestEnable() : Boolean.TRUE;
        if (!BooleanUtil.isFalse(requestEnable)) { // 默认记录，所以判断 !false
            Map<String, Object> requestParams = MapUtil.<String, Object>builder()
                    .put("query", LogSanitizeUtils.sanitizeMap(queryString, sanitizeKeys))
                    .put("body", LogSanitizeUtils.sanitizeJson(requestBody, sanitizeKeys)).build();
            accessLog.setRequestParams(toJsonString(requestParams));
        }
        Boolean responseEnable = accessLogAnnotation != null ? accessLogAnnotation.responseEnable() : Boolean.FALSE;
        if (BooleanUtil.isTrue(responseEnable)) { // 默认不记录，默认强制要求 true
            accessLog.setResponseBody(LogSanitizeUtils.sanitizeResponseBody(result, sanitizeKeys));
        }
        // 持续时间
        accessLog.setBeginTime(beginTime).setEndTime(LocalDateTime.now())
                .setDuration((int) LocalDateTimeUtil.between(accessLog.getBeginTime(), accessLog.getEndTime(), ChronoUnit.MILLIS));

        // 操作模块
        if (handlerMethod != null) {
            Tag tagAnnotation = handlerMethod.getBeanType().getAnnotation(Tag.class);
            Operation operationAnnotation = handlerMethod.getMethodAnnotation(Operation.class);
            String operateModule = accessLogAnnotation != null && StrUtil.isNotBlank(accessLogAnnotation.operateModule()) ?
                    accessLogAnnotation.operateModule() :
                    tagAnnotation != null ? StrUtil.nullToDefault(tagAnnotation.name(), tagAnnotation.description()) : null;
            String operateName = accessLogAnnotation != null && StrUtil.isNotBlank(accessLogAnnotation.operateName()) ?
                    accessLogAnnotation.operateName() :
                    operationAnnotation != null ? operationAnnotation.summary() : null;
            OperateTypeEnum operateType = accessLogAnnotation != null && accessLogAnnotation.operateType().length > 0 ?
                    accessLogAnnotation.operateType()[0] : parseOperateLogType(request);
            accessLog.setOperateModule(operateModule).setOperateName(operateName).setOperateType(operateType.getType());
        }
        return true;
    }

    // ========== 解析 @ApiAccessLog、@Swagger 注解  ==========

    private static OperateTypeEnum parseOperateLogType(HttpServletRequest request) {
        RequestMethod requestMethod = RequestMethod.resolve(request.getMethod());
        if (requestMethod == null) {
            return OperateTypeEnum.OTHER;
        }
        switch (requestMethod) {
            case GET:
                return OperateTypeEnum.GET;
            case POST:
                return OperateTypeEnum.CREATE;
            case PUT:
                return OperateTypeEnum.UPDATE;
            case DELETE:
                return OperateTypeEnum.DELETE;
            default:
                return OperateTypeEnum.OTHER;
        }
    }

}
