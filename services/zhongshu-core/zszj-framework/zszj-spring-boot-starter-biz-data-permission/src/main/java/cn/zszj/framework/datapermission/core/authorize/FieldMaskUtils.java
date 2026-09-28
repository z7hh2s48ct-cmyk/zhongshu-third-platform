package cn.zszj.framework.datapermission.core.authorize;

/**
 * 字段值脱敏工具（ZS-PERM-003.B，D-12 §5.2：F2 默认脱敏展示，保留尾 N 位可配置）。
 *
 * @author ZS-PERM-003.B
 */
public class FieldMaskUtils {

    private FieldMaskUtils() {
    }

    /**
     * 保留尾 keepTail 位、其余以 '*' 掩码（RED 骨架：原样返回，待 GREEN 实现）。
     */
    public static String maskKeepTail(String value, int keepTail) {
        return value;
    }

}
