package cn.zszj.framework.datapermission.core.authorize;

/**
 * 字段值脱敏工具（ZS-PERM-003.B，D-12 §5.2：F2 默认脱敏展示——保留尾 N 位、其余以 '*' 掩码，N 可配置）。
 *
 * <p>消费方：业务域按 {@code maskedFields} 输出脱敏详情、导出按本规则落脱敏值；规则参数
 * {@code keepTail} 由域接入配置传入（D-12 默认 4）。短值（长度 ≤ keepTail）与负参数一律<b>全掩码</b>
 * ——保留全部等于不脱敏，降级方向恒为收紧。
 *
 * @author ZS-PERM-003.B
 */
public class FieldMaskUtils {

    private FieldMaskUtils() {
    }

    /**
     * 保留尾 {@code keepTail} 位、其余以 '*' 掩码。
     *
     * <p>长度 ≤ keepTail（含 0/负参数）→ 全掩码；null 返回 null；空白原样返回（无可泄露内容）。
     * 掩码不改字符数（前端展示对齐）；按 char 掩码，代理对字符按两个 char 计（敏感值场景可接受）。
     *
     * @param value    原始值，可为 null
     * @param keepTail 保留的尾部字符数
     * @return 脱敏后的值
     */
    public static String maskKeepTail(String value, int keepTail) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty() || value.isBlank()) {
            return value;
        }
        int keep = Math.max(keepTail, 0);
        int length = value.length();
        int visible = length > keep ? keep : 0;
        int maskCount = length - visible;
        return "*".repeat(maskCount) + value.substring(length - visible);
    }

}
