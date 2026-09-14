package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.module.system.dal.dataobject.permission.RoleDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-PERM-001.A 小卡：超管角色判定语义测试（纯单元，无 Spring/DB）。
 *
 * <p>语义分型：{@code hasAnySuperAdmin} 的角色谓词【不区分状态】（授予上限语义——禁用超管角色同样
 * 不可由非超管授予）；{@code hasAnyEnabledSuperAdmin} 的角色谓词【仅启用状态产生豁免】（对齐
 * {@code hasAnyPermissions} 过滤禁用角色的语义，堵「禁用超管角色仍挂载 + 双角色组合」绕过自我提权上限）。</p>
 */
public class RoleSuperAdminSemanticsTest {

    private RoleDO role(String code, Integer status) {
        RoleDO role = new RoleDO();
        role.setCode(code);
        role.setStatus(status);
        return role;
    }

    // ========== 豁免语义：hasAnyEnabledSuperAdmin 的角色谓词 ==========

    @Test
    public void isEnabledSuperAdminRole_enabledSuperAdmin_true() {
        assertTrue(RoleServiceImpl.isEnabledSuperAdminRole(
                role("super_admin", CommonStatusEnum.ENABLE.getStatus())));
    }

    @Test
    public void isEnabledSuperAdminRole_disabledSuperAdmin_false() {
        assertFalse(RoleServiceImpl.isEnabledSuperAdminRole(
                role("super_admin", CommonStatusEnum.DISABLE.getStatus())),
                "禁用超管角色不得产生豁免");
    }

    @Test
    public void isEnabledSuperAdminRole_enabledNonSuper_false() {
        assertFalse(RoleServiceImpl.isEnabledSuperAdminRole(
                role("common", CommonStatusEnum.ENABLE.getStatus())));
    }

    @Test
    public void isEnabledSuperAdminRole_nullRole_false() {
        assertFalse(RoleServiceImpl.isEnabledSuperAdminRole(null));
    }

    // ========== 上限语义：hasAnySuperAdmin 的角色谓词（保持不区分状态） ==========

    @Test
    public void isSuperAdminRole_disabledSuperAdmin_true() {
        assertTrue(RoleServiceImpl.isSuperAdminRole(
                role("super_admin", CommonStatusEnum.DISABLE.getStatus())),
                "授予上限语义不区分状态：禁用超管角色同样不可由非超管授予（防先授禁用角色待解禁的搁置提权）");
    }

    @Test
    public void isSuperAdminRole_enabledSuperAdmin_true() {
        assertTrue(RoleServiceImpl.isSuperAdminRole(
                role("super_admin", CommonStatusEnum.ENABLE.getStatus())));
    }

    @Test
    public void isSuperAdminRole_nullRole_false() {
        assertFalse(RoleServiceImpl.isSuperAdminRole(null));
    }

}
