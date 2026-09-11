package cn.zszj.framework.common.util.json.databind;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Collection;

/**
 * ID 字段字符串序列化的注解解析器（ZS-SEC-009：接口边界 ID 合同）
 *
 * <p>背景：{@link NumberSerializer} 对 |值| &lt; 2^53-1 的 Long 输出 number、超范围输出 string，
 * 导致同一 ID 字段随数值大小在 number/string 之间漂移，违背 B03「稳定字符串 ID」合同；前端把
 * id 标注为 number 时，一旦 ID 超过 JS 安全整数（如迁移雪花算法、导入历史大 ID）即收到 string，
 * 类型说谎引发比较/传参/路由错误。
 *
 * <p>方案：按「字段名约定」在 JSON 边界把 ID 语义的 Long 恒定为 string，与数值大小无关；
 * 计数（count/total）、金额（price/amount）等非 ID 的 Long 不受影响，仍走 {@link NumberSerializer}
 * 的安全网。既满足「只把 ID 统一字符串」，又不盲目把所有 Long 变字符串。
 *
 * <ul>
 *     <li>标量 {@code Long}/{@code long} 且名匹配 → 整体 {@link ToStringSerializer}（如 {@code id}、{@code userId}）</li>
 *     <li>{@link Collection}/数组 且名匹配 → 元素 {@link ToStringSerializer}（如 {@code Set<Long> postIds} → {@code ["1","2"]}）</li>
 * </ul>
 *
 * <p>命名规则（<b>大小写敏感</b>，规避 {@code valid}/{@code android}/{@code fluid} 等小写 "id" 结尾误伤）：
 * 名为 {@code "id"}，或以 {@code "Id"} / {@code "Ids"} 结尾（{@code userId}、{@code deptId}、{@code postIds}、{@code menuIds}…）。
 * 匹配基于 Java 成员名（getter 去 get/is 前缀并首字母小写、字段取原名），因此 {@code @JsonProperty} 重命名不影响识别。
 *
 * <p>优先级：注解/解析器级 serializer 高于 {@code serializerByType(Long.class, NumberSerializer)} 的类型级注册，
 * 故 ID 字段被本解析器覆盖为 string、非 ID 的 Long 回落到 NumberSerializer。仅作用于序列化方向；
 * 反序列化（string→Long）由 Jackson 默认标量强转与 Spring 参数转换器承接，请求侧无需改动。
 *
 * @author ZS-SEC-009
 */
public class IdToStringAnnotationIntrospector extends JacksonAnnotationIntrospector {

    public static final IdToStringAnnotationIntrospector INSTANCE = new IdToStringAnnotationIntrospector();

    /**
     * 标量 Long ID 字段：整体序列化为 string
     */
    @Override
    public Object findSerializer(Annotated am) {
        Class<?> raw = am.getRawType();
        if ((raw == Long.class || raw == long.class) && isIdName(resolveName(am))) {
            return ToStringSerializer.class;
        }
        return super.findSerializer(am);
    }

    /**
     * 集合/数组 ID 字段：元素序列化为 string（如 Set&lt;Long&gt; postIds）
     */
    @Override
    public Object findContentSerializer(Annotated am) {
        Class<?> raw = am.getRawType();
        if ((Collection.class.isAssignableFrom(raw) || raw.isArray()) && isIdName(resolveName(am))) {
            return ToStringSerializer.class;
        }
        return super.findContentSerializer(am);
    }

    /**
     * ID 字段名约定：名为 "id"，或以 "Id" / "Ids" 结尾（大小写敏感）
     *
     * @param name Java 成员名（getter 已去前缀、字段取原名）
     * @return 是否 ID 语义字段
     */
    public static boolean isIdName(String name) {
        return name != null && ("id".equals(name) || name.endsWith("Id") || name.endsWith("Ids"));
    }

    /**
     * 解析成员对应的属性名：getter 去 get/is 前缀并首字母小写、字段直接取原名
     */
    private static String resolveName(Annotated am) {
        if (am instanceof AnnotatedMethod) {
            String n = am.getName();
            if (n.startsWith("get") && n.length() > 3) {
                return decapitalize(n.substring(3));
            }
            if (n.startsWith("is") && n.length() > 2) {
                return decapitalize(n.substring(2));
            }
            return n;
        }
        return am.getName();
    }

    /**
     * 首字母小写，但保留连续大写开头（如 URL 不降为 uRL），与 java.beans.Introspector 语义一致
     */
    private static String decapitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
