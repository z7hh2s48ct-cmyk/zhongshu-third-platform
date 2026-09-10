package cn.zszj.framework.web.core.util;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.enums.TerminalEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.config.WebProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception0;

/**
 * 专属于 web 包的工具类
 *
 * @author 芋道源码
 */
public class WebFrameworkUtils {

    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_id";
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "login_user_type";

    private static final String REQUEST_ATTRIBUTE_COMMON_RESULT = "common_result";

    public static final String HEADER_TENANT_ID = "tenant-id";
    public static final String HEADER_VISIT_TENANT_ID = "visit-tenant-id";

    /**
     * 终端的 Header
     *
     * @see cn.zszj.framework.common.enums.TerminalEnum
     */
    public static final String HEADER_TERMINAL = "terminal";

    private static WebProperties properties;

    public WebFrameworkUtils(WebProperties webProperties) {
        WebFrameworkUtils.properties = webProperties;
    }

    /**
     * 获得租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getTenantId(HttpServletRequest request) {
        return parseTenantIdHeader(request.getHeader(HEADER_TENANT_ID), HEADER_TENANT_ID);
    }

    /**
     * 获得访问的租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getVisitTenantId(HttpServletRequest request) {
        return parseTenantIdHeader(request.getHeader(HEADER_VISIT_TENANT_ID), HEADER_VISIT_TENANT_ID);
    }

    /**
     * 严格解析租户类上下文头（ZS-SEC-008）。
     *
     * <p>规则：缺失 / 空白 → {@code null}（视为未传递，保持既有兜底语义）；present-but-malformed
     * （含符号、小数点、十六进制、科学计数、内嵌空白、非数字，或十进制数字串超出 {@link Long} 范围）
     * → 抛受控 {@link cn.zszj.framework.common.exception.ServiceException}（业务码 400），由统一异常出口稳定拒绝、
     * 不泄露栈。替代原 {@code NumberUtil.isNumber(...) + Long.valueOf(...)} 组合——后者对 {@code "1.5"}、{@code "0x1F"}、
     * {@code "1e5"} 等 isNumber 通过但 Long.valueOf 失败的输入会抛 {@link NumberFormatException}，
     * 在 MVC 外的过滤器中逃逸为容器 500 + 栈泄露。
     *
     * @param rawValue   上下文头原始值
     * @param headerName 头名，仅用于错误提示
     * @return 合法租户编号；缺失 / 空白返回 {@code null}
     */
    private static Long parseTenantIdHeader(String rawValue, String headerName) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        // 仅接受纯十进制数字（拒绝符号、小数点、十六进制、科学计数、内嵌空白等一切非数字字符）
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        StrUtil.format("请求头 {} 格式非法，必须为十进制非负整数", headerName));
            }
        }
        try {
            // 纯数字串但超出 Long 范围（如 99999999999999999999）→ NumberFormatException → 受控 400
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    StrUtil.format("请求头 {} 超出取值范围", headerName));
        }
    }

    public static void setLoginUserId(ServletRequest request, Long userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    /**
     * 设置用户类型
     *
     * @param request 请求
     * @param userType 用户类型
     */
    public static void setLoginUserType(ServletRequest request, Integer userType) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE, userType);
    }

    /**
     * 获得当前用户的编号，从请求中
     * 注意：该方法仅限于 framework 框架使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Long getLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (Long) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    /**
     * 获得当前用户的类型
     * 注意：该方法仅限于 web 相关的 framework 组件使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Integer getLoginUserType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1. 优先，从 Attribute 中获取
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 2. 其次，基于 URL 前缀的约定
        if (request.getServletPath().startsWith(properties.getAdminApi().getPrefix())) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (request.getServletPath().startsWith(properties.getAppApi().getPrefix())) {
            return UserTypeEnum.MEMBER.getValue();
        }
        return null;
    }

    public static Integer getLoginUserType() {
        HttpServletRequest request = getRequest();
        return getLoginUserType(request);
    }

    public static Long getLoginUserId() {
        HttpServletRequest request = getRequest();
        return getLoginUserId(request);
    }

    public static Integer getTerminal() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return TerminalEnum.UNKNOWN.getTerminal();
        }
        String terminalValue = request.getHeader(HEADER_TERMINAL);
        return NumberUtil.parseInt(terminalValue, TerminalEnum.UNKNOWN.getTerminal());
    }

    public static void setCommonResult(ServletRequest request, CommonResult<?> result) {
        request.setAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT, result);
    }

    public static CommonResult<?> getCommonResult(ServletRequest request) {
        return (CommonResult<?>) request.getAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT);
    }

    /**
     * 统一写出 {@link CommonResult} 响应，并登记到请求属性，供 API 访问日志按业务码记录结果。
     *
     * 底座 API 契约：HTTP 传输层固定 200，业务结果由 {@link CommonResult#getCode()} 表达（业务码刻意镜像 HTTP 语义，
     * 如 401/403/429/500）。因此本方法不修改 HTTP 状态码，仅统一 filter 直接写出（认证 401 / 权限 403 / 租户 / Token /
     * Demo / 加密）的出口：这些路径未经 MVC 的 GlobalResponseBodyHandler 登记 common_result，会导致访问日志误记为成功。
     *
     * @param request  请求，用于登记 common_result 属性（供 ApiAccessLogFilter 读取）
     * @param response 响应，写出 JSON 体
     * @param result   业务结果
     */
    public static void writeJSON(HttpServletRequest request, HttpServletResponse response, CommonResult<?> result) {
        setCommonResult(request, result);
        ServletUtils.writeJSON(response, result);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

}
