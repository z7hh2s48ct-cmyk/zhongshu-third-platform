package cn.zszj.framework.datapermission.core.authorize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FieldMaskUtils} 的单元测试（ZS-PERM-003.B，D-12 §5.2 F2 默认脱敏：保留尾 N 位可配置）。
 *
 * @author ZS-PERM-003.B
 */
class FieldMaskUtilsTest {

    @Test // 手机号（11 位，保留尾 4 位）：其余位以 '*' 掩码
    void mask_phone_keepTail4() {
        assertEquals("*******5678", FieldMaskUtils.maskKeepTail("13812345678", 4));
    }

    @Test // 短值（长度 ≤ 保留位数）→ 全掩码（fail-closed：短值保留全部等于不脱敏）
    void mask_shortValue_fullyMasked() {
        assertEquals("***", FieldMaskUtils.maskKeepTail("abc", 4));
        assertEquals("****", FieldMaskUtils.maskKeepTail("abcd", 4));
    }

    @Test // 保留位数为 0 → 全掩码
    void mask_keepTail0_fullyMasked() {
        assertEquals("***********", FieldMaskUtils.maskKeepTail("13812345678", 0));
    }

    @Test // 自定义保留位数（D-12：规则可配置）
    void mask_customKeepTail() {
        assertEquals("**12", FieldMaskUtils.maskKeepTail("1234", 2));
        assertEquals("*********1", FieldMaskUtils.maskKeepTail("13812345678", 1));
    }

    @Test // null 返回 null；空白原样返回（无可泄露内容）
    void mask_nullAndBlank_safe() {
        assertNull(FieldMaskUtils.maskKeepTail(null, 4));
        assertEquals("", FieldMaskUtils.maskKeepTail("", 4));
        assertEquals("  ", FieldMaskUtils.maskKeepTail("  ", 4));
    }

    @Test // 负保留位数按 0 处理（配置防御，不放大可见性）
    void mask_negativeKeepTail_fullyMasked() {
        assertEquals("****", FieldMaskUtils.maskKeepTail("1234", -1));
    }

    @Test // 长度不变：掩码不改字符数（前端展示对齐）
    void mask_lengthPreserved() {
        String masked = FieldMaskUtils.maskKeepTail("13812345678", 4);
        assertEquals(11, masked.length());
        assertTrue(masked.chars().filter(c -> c == '*').count() == 7);
    }

}
