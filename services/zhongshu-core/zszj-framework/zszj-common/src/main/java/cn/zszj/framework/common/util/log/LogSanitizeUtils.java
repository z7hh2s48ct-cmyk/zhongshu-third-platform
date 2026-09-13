package cn.zszj.framework.common.util.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日志脱敏工具类（ZS-SEC-007）
 *
 * 统一访问日志、异常日志与保护切面（幂等 / 限流）四类写点的日志净化能力，核心策略：
 * 1. 键归一：小写并去除下划线 / 连字符后做大小写不敏感匹配，兼容 password / PASSWORD / user_password / access-token 等写法；
 * 2. 命中判定：内置凭据根集（password、token、secret、authorization 等）使用 contains 模糊匹配，端点级 extraKeys 使用归一后精确匹配；
 * 3. 递归脱敏：对象与数组逐层递归，命中敏感键的值统一掩码为 {@code ***}，保留字段名以便审计定位；
 * 4. 失败不回退原文：解析 / 序列化异常时只记录摘要（长度或类型 + 原因类别），绝不输出未净化的原始内容；
 * 5. 体积上限：结果超过 {@link #MAX_LENGTH} 时截断并附总长度，避免超长正文撑爆日志表。
 *
 * 该工具不依赖 servlet / web，可被 web 与 protection 两个 starter 共同复用。
 *
 * @author 众墅之家
 */
@Slf4j
public class LogSanitizeUtils {

    /**
     * 敏感值掩码，保留字段名、隐藏具体值。
     * 公开以便调用方（如签名切面）对单个凭据派生值就地掩码，复用统一掩码标记。
     */
    public static final String MASK = "***";

    /**
     * 单条日志内容的长度上限，超出部分截断
     */
    private static final int MAX_LENGTH = 2048;

    /**
     * 内置凭据敏感键根集（已归一：小写、无下划线 / 连字符），使用 contains 匹配
     */
    private static final Set<String> SENSITIVE_KEY_ROOTS = Set.of(
            "password", "passwd", "pwd", "token", "secret", "authorization",
            "credential", "privatekey", "apikey", "accesskey", "secretkey",
            "cookie", "session", "otp");

    private LogSanitizeUtils() {
    }

    /**
     * 脱敏 JSON 字符串（如请求正文）。
     *
     * @param json      原始 JSON 字符串
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}；无法解析时返回摘要（不含原文）
     */
    public static String sanitizeJson(String json, String... extraKeys) {
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = mapper().readTree(json);
            sanitizeNode(node, extra);
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unparseable(json, t);
        }
    }

    /**
     * 脱敏键值映射（如 query 参数）。
     *
     * @param map       原始映射
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}
     */
    public static String sanitizeMap(Map<String, ?> map, String... extraKeys) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }
        return sanitizeObject(map, extraKeys);
    }

    /**
     * 脱敏方法入参数组（如幂等 / 限流切面的 joinPoint.getArgs()）。
     *
     * @param args      原始入参数组
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的形如 {@code [arg1, arg2]} 的字符串
     */
    public static String sanitizeArgs(Object[] args, String... extraKeys) {
        if (ArrayUtil.isEmpty(args)) {
            return "[]";
        }
        Set<String> extra = normalizeKeys(extraKeys);
        List<String> parts = new ArrayList<>(args.length);
        for (Object arg : args) {
            parts.add(sanitizeArgValue(arg, extra));
        }
        return truncate("[" + String.join(", ", parts) + "]");
    }

    /**
     * 脱敏响应体（如 CommonResult），仅净化 data 字段，保留 code / msg 以便审计定位。
     *
     * @param result    响应对象
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为 {@code null} 返回 {@code null}
     */
    public static String sanitizeResponseBody(Object result, String... extraKeys) {
        if (result == null) {
            return null;
        }
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = toSanitizableTree(result);
            JsonNode data = node.get("data");
            if (data != null) {
                sanitizeNode(data, extra);
            }
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unserializable(result, t);
        }
    }

    private static String sanitizeObject(Object obj, String... extraKeys) {
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = toSanitizableTree(obj);
            sanitizeNode(node, extra);
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unserializable(obj, t);
        }
    }

    private static String sanitizeArgValue(Object arg, Set<String> extra) {
        if (arg == null) {
            return "null";
        }
        try {
            JsonNode node = toSanitizableTree(arg);
            sanitizeNode(node, extra);
            return mapper().writeValueAsString(node);
        } catch (Throwable t) {
            return "<" + arg.getClass().getSimpleName() + ">";
        }
    }


    /**
     * 物化 POJONode：@JsonRawValue 等原文直出字段经 valueToTree 保留为 POJONode，
     * 递归脱敏会跳过非对象/数组节点导致原文穿透（SEC-007 HANDOFF 补评 P1）。
     * 统一在脱敏前把整棵树物化为普通节点（String 原文按 JSON 解析，解析失败降级为文本节点）。
     */
    private static JsonNode toSanitizableTree(Object obj) {
        return materializeRaw(mapper().valueToTree(obj));
    }

    private static JsonNode materializeRaw(JsonNode node) {
        if (node == null || node.isNull() || (node.isValueNode() && !node.isPojo())) {
            return node;
        }
        if (node.isPojo()) {
            Object pojo = ((POJONode) node).getPojo();
            // @JsonRawValue 场景 POJONode 包装的是 RawValue（getValue() 才是原文）
            Object effective = pojo instanceof RawValue ? ((RawValue) pojo).rawValue() : pojo;
            if (effective == null) {
                return NullNode.getInstance();
            }
            if (effective instanceof String) {
                try {
                    return materializeRaw(mapper().readTree((String) effective));
                } catch (Throwable t) {
                    return TextNode.valueOf((String) effective);
                }
            }
            try {
                return materializeRaw(mapper().valueToTree(effective));
            } catch (Throwable t) {
                return TextNode.valueOf(String.valueOf(effective));
            }
        }
        if (node.isObject()) {
            ObjectNode copy = mapper().createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                copy.set(entry.getKey(), materializeRaw(entry.getValue()));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = mapper().createArrayNode();
            for (JsonNode child : node) {
                copy.add(materializeRaw(child));
            }
            return copy;
        }
        return node;
    }

    /**
     * 递归脱敏 JSON 节点：数组逐元素递归，对象命中敏感键则掩码、否则递归其值
     */
    private static void sanitizeNode(JsonNode node, Set<String> extra) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                sanitizeNode(child, extra);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        ObjectNode objectNode = (ObjectNode) node;
        List<String> sensitiveKeys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isSensitiveKey(entry.getKey(), extra)) {
                sensitiveKeys.add(entry.getKey());
            } else {
                sanitizeNode(entry.getValue(), extra);
            }
        }
        for (String key : sensitiveKeys) {
            objectNode.put(key, MASK);
        }
    }

    private static boolean isSensitiveKey(String key, Set<String> extra) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        String normalized = normalizeKey(key);
        if (extra.contains(normalized)) {
            return true;
        }
        for (String root : SENSITIVE_KEY_ROOTS) {
            if (normalized.contains(root)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeKeys(String[] keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return Set.of();
        }
        return Arrays.stream(keys)
                .filter(StrUtil::isNotEmpty)
                .map(LogSanitizeUtils::normalizeKey)
                .collect(Collectors.toSet());
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH) + "...[truncated,total=" + text.length() + "]";
    }

    private static String unparseable(String raw, Throwable t) {
        int len = raw == null ? 0 : raw.length();
        String cause = t.getClass().getSimpleName();
        log.warn("[sanitizeJson][无法解析正文，已降级为摘要 len={} cause={}]", len, cause);
        return "<unparseable:len=" + len + ",cause=" + cause + ">";
    }

    private static String unserializable(Object obj, Throwable t) {
        String type = obj == null ? "null" : obj.getClass().getSimpleName();
        String cause = t.getClass().getSimpleName();
        log.warn("[sanitize][无法序列化对象，已降级为摘要 type={} cause={}]", type, cause);
        return "<unserializable:" + type + ">";
    }

    private static ObjectMapper mapper() {
        return JsonUtils.getObjectMapper();
    }

}
