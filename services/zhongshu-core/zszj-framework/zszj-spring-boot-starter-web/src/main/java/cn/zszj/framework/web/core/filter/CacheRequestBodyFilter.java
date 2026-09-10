package cn.zszj.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request Body 缓存 Filter，实现它的可重复读取
 *
 * @author 芋道源码
 */
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    /**
     * 需要排除的 URI
     *
     * 1. 排除 Spring Boot Admin 相关请求，避免客户端连接中断导致的异常。
     *    例如说：<a href="https://github.com/YunaiV/ruoyi-vue-pro/issues/795">795 ISSUE</a>
     */
    private static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};

    /**
     * JSON 请求体缓冲上限（字节），{@code <= 0} 表示不限制。
     *
     * ZS-SEC-008：在缓冲前按声明的 Content-Length 约束 JSON 大小，超限受控拒绝（业务码 400），
     * 防止认证前全量缓冲超大 body 耗尽内存。由 {@link cn.zszj.framework.web.config.WebProperties.RequestBody#getMaxCacheSize()} 注入。
     */
    private final long maxCacheSize;

    public CacheRequestBodyFilter() {
        this(-1L); // 向后兼容：无参构造不施加额外大小限制
    }

    public CacheRequestBodyFilter(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // ZS-SEC-008：缓冲前约束 JSON 大小。声明的 Content-Length 超过上限则受控拒绝，不进入全量缓冲。
        // 上传 / 流式接口非 JSON，已被 shouldNotFilter 排除，不会进入此处。
        if (maxCacheSize > 0 && request.getContentLengthLong() > maxCacheSize) {
            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
                    GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "请求体大小超过上限"));
            return;
        }
        filterChain.doFilter(new CacheRequestBodyWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1. 校验是否为排除的 URL
        String requestURI = request.getRequestURI();
        if (StrUtil.startWithAny(requestURI, IGNORE_URIS)) {
            return true;
        }

        // 2. 只处理 json 请求内容
        return !ServletUtils.isJsonRequest(request);
    }

}
