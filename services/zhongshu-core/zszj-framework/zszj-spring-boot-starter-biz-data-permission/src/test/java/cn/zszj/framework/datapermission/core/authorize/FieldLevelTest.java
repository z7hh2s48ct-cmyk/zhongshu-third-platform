package cn.zszj.framework.datapermission.core.authorize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FieldLevel} 的单元测试（ZS-PERM-003.B，字段等级目录 D-12）。
 *
 * @author ZS-PERM-003.B
 */
class FieldLevelTest {

    @Test // 等级序：F0 < F1 < F2 < F3（D-12 §2 公开/内部/敏感/机密）
    void rank_ordering() {
        assertTrue(FieldLevel.F0.rank() < FieldLevel.F1.rank());
        assertTrue(FieldLevel.F1.rank() < FieldLevel.F2.rank());
        assertTrue(FieldLevel.F2.rank() < FieldLevel.F3.rank());
        assertEquals(0, FieldLevel.F0.rank());
        assertEquals(3, FieldLevel.F3.rank());
    }

    @Test // isWithin：字段等级不超过访问者可读上限即清晰可见（含等号）
    void isWithin_boundaries() {
        assertTrue(FieldLevel.F0.isWithin(FieldLevel.F1));
        assertTrue(FieldLevel.F1.isWithin(FieldLevel.F1));
        assertTrue(FieldLevel.F2.isWithin(FieldLevel.F2));
        assertTrue(FieldLevel.F2.isWithin(FieldLevel.F3));
        assertFalse(FieldLevel.F3.isWithin(FieldLevel.F2));
        assertFalse(FieldLevel.F2.isWithin(FieldLevel.F1));
        assertFalse(FieldLevel.F1.isWithin(FieldLevel.F0));
    }

}
