package cn.zszj.framework.tenant.core.web;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 多租户 Context Web 过滤器
 * 将请求 Header 中的 tenant-id 解析出来，添加到 {@link TenantContextHolder} 中，这样后续的 DB 等操作，可以获得到租户编号。
 *
 * @author 芋道源码
 */
public class TenantContextWebFilter extends OncePerRequestFilter {

    /**
     * ZS-SEC-012.B codex r2 P1：必须参与 ASYNC 派发——OncePerRequestFilter 默认跳过异步派发，
     * 而首次 REQUEST 的 finally 已清理租户上下文；异步访问日志（ApiAccessLogFilter 同样参与 ASYNC
     * 派发）在派发期记录时若上下文缺失，会被 ApiAccessLogServiceImpl 以 executeIgnore 落为 tenant_id=0，
     * 原租户查不到自己的日志。参与后本过滤器在 ASYNC 派发内从 tenant-id 头重建上下文，finally 再清理。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 设置。ZS-SEC-008：上下文头严格解析——畸形/溢出 tenant-id 会抛受控 ServiceException（业务码 400）。
        // 本过滤器位于 MVC 之外，GlobalExceptionHandler（@RestControllerAdvice）无法捕获，需就地转统一出口 writeJSON，
        // 避免 NumberFormatException / ServiceException 逃逸为容器 500 + 栈泄露。
        Long tenantId;
        try {
            tenantId = WebFrameworkUtils.getTenantId(request);
        } catch (ServiceException ex) {
            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(ex.getCode(), ex.getMessage()));
            return;
        }
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理
            TenantContextHolder.clear();
        }
    }

}
