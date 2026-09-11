package cn.zszj.framework.common.util.json.databind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IdToStringAnnotationIntrospector#isIdName(String)} 命名约定的单元测试（ZS-SEC-009）。
 *
 * <p>锁定「只把 ID 统一字符串、不误伤计数/金额」的边界：大小写敏感地匹配 {@code id} / {@code *Id} / {@code *Ids}，
 * 排除小写 "id" 结尾的普通单词（valid/android/fluid）与计数类字段（count/total）。
 */
public class IdToStringAnnotationIntrospectorTest {

    @Test
    @DisplayName("ID 语义字段名命中：id / *Id / *Ids")
    public void testIsIdName_positive() {
        assertTrue(IdToStringAnnotationIntrospector.isIdName("id"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("userId"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("deptId"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("tenantId"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("parentId"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("postIds"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("menuIds"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("roleIds"));
        assertTrue(IdToStringAnnotationIntrospector.isIdName("dataScopeDeptIds"));
    }

    @Test
    @DisplayName("非 ID 字段名不命中：计数/金额 + 小写 id 结尾单词 + null")
    public void testIsIdName_negative() {
        // 计数 / 金额等非 ID 的 Long，必须保持 number
        assertFalse(IdToStringAnnotationIntrospector.isIdName("count"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("total"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("price"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("amount"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("stock"));
        // 小写 "id" 结尾的普通单词：大小写敏感规避误伤
        assertFalse(IdToStringAnnotationIntrospector.isIdName("valid"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("android"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("fluid"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("rapid"));
        // 小写 id 前缀/中缀不构成 ID 语义后缀
        assertFalse(IdToStringAnnotationIntrospector.isIdName("uid"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("idea"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName("idCard"));
        assertFalse(IdToStringAnnotationIntrospector.isIdName(null));
    }
}
