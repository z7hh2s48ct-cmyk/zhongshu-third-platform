package cn.zszj.framework.security.core.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 安全服务工具类
 *
 * @author 芋道源码
 */
public class SecurityFrameworkUtils {

    /**
     * HEADER 认证头 value 的前缀
     */
    public static final String AUTHORIZATION_BEARER = "Bearer";

    private SecurityFrameworkUtils() {}

    /**
     * 从请求中，获得认证 Token（向后兼容重载：URL 参数通道默认启用）。
     *
     * @param request 请求
     * @param headerName 认证 Token 对应的 Header 名字
     * @param parameterName 认证 Token 对应的 Parameter 名字
     * @return 认证 Token；缺失、畸形、重复冲突时返回 {@code null}
     */
    public static String obtainAuthorization(HttpServletRequest request,
                                             String headerName, String parameterName) {
        return obtainAuthorization(request, headerName, parameterName, true);
    }

    /**
     * 从请求中，获得认证 Token。
     *
     * <p>解析规则（ZS-SEC-003）：
     * <ol>
     *     <li><b>来源优先级</b>：Header &gt; Parameter。Parameter 通道仅用于 WebSocket 等无法设置 Header 的获准
     *     连接；普通 API 两端前端一律使用 Authorization Header。{@code parameterEnabled=false} 时完全忽略
     *     Parameter，从服务端禁止长效凭据进入 URL。</li>
     *     <li><b>重复/冲突拒绝</b>：同一来源（Header 或 Parameter）出现多个<b>不同</b>值时视为歧义（凭据走私），
     *     拒绝并返回 {@code null}；多个相同值归一为一个。</li>
     *     <li><b>严格 Bearer 前缀</b>（RFC 6750/7235）：仅当值以 {@code Bearer}（scheme 大小写不敏感）+ 空白开头时
     *     剥离前缀，不再子串查找，避免 {@code "xBearer y"} 被误解析为 {@code "y"}；{@code "Bearer"} 后无空白分隔
     *     或剥离后为空视为畸形拒绝；无 Bearer 前缀时按裸 Token 兼容既有客户端。</li>
     * </ol>
     *
     * @param request 请求
     * @param headerName 认证 Token 对应的 Header 名字
     * @param parameterName 认证 Token 对应的 Parameter 名字
     * @param parameterEnabled 是否允许 URL 参数通道（WebSocket 等获准连接）
     * @return 认证 Token；缺失、畸形、重复冲突时返回 {@code null}（统一视为无有效凭据）
     */
    public static String obtainAuthorization(HttpServletRequest request,
                                             String headerName, String parameterName, boolean parameterEnabled) {
        // 1. Header 优先：同一 Header 多个不同值视为冲突，拒绝
        Set<String> headerValues = distinctNonEmptyValues(Collections.list(request.getHeaders(headerName)));
        if (headerValues.size() > 1) {
            return null;
        }
        String raw = headerValues.isEmpty() ? null : headerValues.iterator().next();
        // 2. Header 缺失且参数通道启用时，回退 Parameter（WebSocket/SSE/下载等无法设置 Header 的获准连接）
        if (!StringUtils.hasText(raw) && parameterEnabled && StringUtils.hasText(parameterName)) {
            String[] parameterValues = request.getParameterValues(parameterName);
            List<String> parameterList = parameterValues == null
                    ? Collections.emptyList() : Arrays.asList(parameterValues);
            Set<String> distinctParameters = distinctNonEmptyValues(parameterList);
            if (distinctParameters.size() > 1) {
                return null;
            }
            raw = distinctParameters.isEmpty() ? null : distinctParameters.iterator().next();
        }
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        // 3. 严格解析 Bearer 前缀
        return parseBearerToken(raw);
    }

    /**
     * 归一来源值：剔除空白项并 trim，返回<b>不同值</b>的有序集合，用于重复/冲突判定。
     */
    private static Set<String> distinctNonEmptyValues(List<String> values) {
        Set<String> distinct = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                distinct.add(value.trim());
            }
        }
        return distinct;
    }

    /**
     * 严格解析 Bearer 前缀（RFC 6750）：scheme 大小写不敏感、必须以空白分隔；畸形返回 {@code null}；
     * 无 Bearer 前缀时按裸 Token 兼容返回。
     */
    private static String parseBearerToken(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        // scheme 前缀大小写不敏感匹配（RFC 7235）
        if (!value.regionMatches(true, 0, AUTHORIZATION_BEARER, 0, AUTHORIZATION_BEARER.length())) {
            // 无 Bearer 前缀：按裸 Token 兼容既有客户端（如 "abc123"）
            return value;
        }
        // 以 Bearer 开头：scheme 后必须是空白分隔，否则畸形（如 "BearerXyz"、"Bearer" 单独出现）
        if (value.length() <= AUTHORIZATION_BEARER.length()
                || !Character.isWhitespace(value.charAt(AUTHORIZATION_BEARER.length()))) {
            return null;
        }
        String token = value.substring(AUTHORIZATION_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 获得当前认证信息
     *
     * @return 认证信息
     */
    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }

    /**
     * 获取当前用户
     *
     * @return 当前用户
     */
    @Nullable
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof LoginUser ? (LoginUser) authentication.getPrincipal() : null;
    }

    /**
     * 获得当前用户的编号，从上下文中
     *
     * @return 用户编号
     */
    @Nullable
    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 获得当前用户的昵称，从上下文中
     *
     * @return 昵称
     */
    @Nullable
    public static String getLoginUserNickname() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? MapUtil.getStr(loginUser.getInfo(), LoginUser.INFO_KEY_NICKNAME) : null;
    }

    /**
     * 获得当前用户的部门编号，从上下文中
     *
     * @return 部门编号
     */
    @Nullable
    public static Long getLoginUserDeptId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? MapUtil.getLong(loginUser.getInfo(), LoginUser.INFO_KEY_DEPT_ID) : null;
    }

    /**
     * 设置当前用户
     *
     * @param loginUser 登录用户
     * @param request 请求
     */
    public static void setLoginUser(LoginUser loginUser, HttpServletRequest request) {
        // 创建 Authentication，并设置到上下文
        Authentication authentication = buildAuthentication(loginUser, request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 额外设置到 request 中，用于 ApiAccessLogFilter 可以获取到用户编号；
        // 原因是，Spring Security 的 Filter 在 ApiAccessLogFilter 后面，在它记录访问日志时，线上上下文已经没有用户编号等信息
        if (request != null) {
            WebFrameworkUtils.setLoginUserId(request, loginUser.getId());
            WebFrameworkUtils.setLoginUserType(request, loginUser.getUserType());
        }
    }

    private static Authentication buildAuthentication(LoginUser loginUser, HttpServletRequest request) {
        // 创建 UsernamePasswordAuthenticationToken 对象
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginUser, null, Collections.emptyList());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }

    /**
     * 是否条件跳过权限校验，包括数据权限、功能权限
     *
     * @return 是否跳过
     */
    public static boolean skipPermissionCheck() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getVisitTenantId() == null) {
            return false;
        }
        // 重点：跨租户访问时，无法进行权限校验
        return ObjUtil.notEqual(loginUser.getVisitTenantId(), loginUser.getTenantId());
    }

}
