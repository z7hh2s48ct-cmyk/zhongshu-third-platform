package cn.zszj.framework.tracer.core.filter;

import cn.zszj.framework.common.util.monitor.TracerUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Trace 过滤器：为每个请求绑定关联 ID 并写入响应头。
 *
 * <h2>ZS-SEC-006 行为说明</h2>
 * <ul>
 *   <li>有 OTel Span → 使用 traceId 作为关联 ID</li>
 *   <li>无 OTel + 外部传入合法 trace-id 头（32 字符 hex）→ 复用为关联 ID（便于端到端串联，但不承载权限）</li>
 *   <li>无 OTel + 外部畸形/超长/无 trace-id → 服务端生成 fallback 关联 ID，绝不回显攻击者可控原文</li>
 *   <li>关联 ID 绑定到请求属性（{@link TracerUtils#ATTR_CORRELATION_ID}），供下游日志/错误处理统一取值</li>
 *   <li>响应头在 chain.doFilter 之前设置 → 即使后续过滤器早退/异常，客户端仍可拿到关联 ID</li>
 * </ul>
 *
 * @author 芋道源码
 */
public class TraceFilter extends OncePerRequestFilter {

    /**
     * Header 名 - 链路追踪编号 / 请求关联 ID
     */
    private static final String HEADER_NAME_TRACE_ID = "trace-id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 解析关联 ID（OTel > 外部合法 > 服务端生成）
        String correlationId = resolveCorrelationId(request);
        // 绑定到请求作用域（下游 GlobalExceptionHandler、ApiAccessLogFilter 统一取值）
        request.setAttribute(TracerUtils.ATTR_CORRELATION_ID, correlationId);
        // 设置响应头（在 chain.doFilter 之前，确保早退/异常时客户端仍可读到）
        response.setHeader(HEADER_NAME_TRACE_ID, correlationId);
        // 继续过滤
        chain.doFilter(request, response);
    }

    /**
     * 解析关联 ID：
     * 1. 有 OTel Span → 使用 traceId
     * 2. 外部传入合法格式 trace-id → 归一化为小写后复用（信任边界：仅用于关联，不作授权凭据）
     * 3. 以上都不满足 → 服务端生成随机 fallback
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        // 优先级 1：OTel traceId
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            return context.getTraceId();
        }
        // 优先级 2：外部传入合法 trace-id（格式校验：32 字符 hex）
        String external = request.getHeader(HEADER_NAME_TRACE_ID);
        if (TracerUtils.isValidTraceIdFormat(external)) {
            return external.toLowerCase();
        }
        // 优先级 3：服务端生成（畸形/超长/缺失外部值均走此路径，不回显攻击者可控原文）
        return TracerUtils.generateCorrelationId();
    }

}
