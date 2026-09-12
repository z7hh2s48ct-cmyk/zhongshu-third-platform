package cn.zszj.framework.common.util.monitor;

import cn.zszj.framework.common.util.servlet.ServletUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 链路追踪工具类
 *
 * 考虑到每个 starter 都需要用到该工具类，所以放到 common 模块下的 util 包下
 *
 * <h2>ZS-SEC-006 关联 ID 设计说明</h2>
 * <ul>
 *   <li>{@link #getTraceId()} —— 纯 OTel 语义，无 Span 返回空串（保持向后兼容）</li>
 *   <li>{@link #getCorrelationId()} / {@link #getCorrelationId(HttpServletRequest)} —— 统一关联 ID 入口：
 *       有 OTel 用 traceId，无则用请求作用域 fallback（有界/随机/不可承载权限）</li>
 * </ul>
 * <p>关联 ID ≠ 分布式 Trace。关联 ID 仅用于前端/日志定位同一请求，绝不作为授权/身份凭据。</p>
 *
 * @author 芋道源码
 */
public class TracerUtils {

    /**
     * 请求属性键：关联 ID（由 TraceFilter 在请求入口绑定）
     */
    public static final String ATTR_CORRELATION_ID = "zszj.correlation.id";

    /**
     * 合法 trace-id 格式长度（128-bit = 32 hex chars）
     */
    private static final int TRACE_ID_LENGTH = 32;

    /**
     * 私有化构造方法
     */
    private TracerUtils() {
    }

    /**
     * 获得链路追踪编号，直接返回 OpenTelemetry 的 TraceId。
     * 如果不存在的话为空字符串！！！
     *
     * <p><b>语义不变</b>：此方法保持原始行为，供既有消费方（如 OTel 链路对接）使用。
     * 若需保证非空的请求关联标识，请使用 {@link #getCorrelationId()}。</p>
     *
     * @return 链路追踪编号
     */
    public static String getTraceId() {
        SpanContext context = Span.current().getSpanContext();
        return context.isValid() ? context.getTraceId() : "";
    }

    /**
     * 获得请求关联 ID（统一入口，保证非空）。
     *
     * <p>优先级：OTel traceId → 当前请求上下文中已绑定的 fallback → 新生成 fallback。</p>
     * <p>此方法绝不返回 null 或空串。返回值为有界（32 字符）、合法十六进制格式、不可承载权限的关联标识。</p>
     *
     * <p>ZS-SEC-006 P2-2 修复：通过 {@link ServletUtils#getRequest()} 解析当前线程绑定的请求，
     * 委托给 {@link #getCorrelationId(HttpServletRequest)} 复用 TraceFilter 已绑定的 ID，
     * 保证与响应头、访问日志、错误日志一致。仅在请求上下文之外（后台任务线程等）才生成一次性 ID。</p>
     *
     * @return 关联 ID（非空）
     */
    public static String getCorrelationId() {
        // 1. 有 OTel Span 则使用 traceId
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            return context.getTraceId();
        }
        // 2. 尝试从当前线程的请求上下文获取（TraceFilter 已绑定 ID 到 request attribute）
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            return getCorrelationId(request);
        }
        // 3. 请求上下文之外（后台任务、消息消费者等）：生成一次性 ID
        return generateCorrelationId();
    }

    /**
     * 获得请求关联 ID（统一入口，保证非空），绑定请求作用域。
     *
     * <p>优先级：OTel traceId → 请求属性中已绑定的 fallback（由 TraceFilter 设置）→ 新生成并绑定。</p>
     * <p>同一请求多次调用保证返回相同值（稳定性）。不同请求返回不同值（不串号）。</p>
     *
     * @param request 当前 HTTP 请求（可为 null）
     * @return 关联 ID（非空）
     */
    public static String getCorrelationId(HttpServletRequest request) {
        // 1. 有 OTel Span 则使用 traceId
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            return context.getTraceId();
        }
        // 2. 从请求属性获取已绑定的 fallback
        if (request != null) {
            Object attr = request.getAttribute(ATTR_CORRELATION_ID);
            if (attr instanceof String s && !s.isEmpty()) {
                return s;
            }
            // 3. 首次访问：生成并绑定到请求作用域
            String generated = generateCorrelationId();
            request.setAttribute(ATTR_CORRELATION_ID, generated);
            return generated;
        }
        // 4. 无请求上下文：生成一次性 ID
        return generateCorrelationId();
    }

    /**
     * 生成 128-bit 随机十六进制关联 ID（32 字符，小写）。
     *
     * <p>特征：有界、随机、不可预测、不可承载权限。基于 {@link UUID#randomUUID()} 的安全随机源。</p>
     *
     * @return 32 字符小写十六进制字符串
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 校验外部传入的 trace-id 格式是否合法。
     *
     * <p>合法条件：恰好 32 字符、仅含十六进制字符 [0-9a-fA-F]。
     * 畸形/超长/含特殊字符的值被视为不合法，应忽略并使用服务端生成值。</p>
     *
     * @param value 外部传入的 trace-id 值
     * @return true 如果格式合法
     */
    public static boolean isValidTraceIdFormat(String value) {
        if (value == null || value.length() != TRACE_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

}
