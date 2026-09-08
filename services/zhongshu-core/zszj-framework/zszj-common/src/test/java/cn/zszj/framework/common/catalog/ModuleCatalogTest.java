package cn.zszj.framework.common.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-CFG-003.A：模块目录判定测试。
 */
class ModuleCatalogTest {

    @Test
    void enabledModulesAreSystemAndInfra() {
        assertEquals(2, ModuleCatalog.ENABLED_MODULES.size());
        assertTrue(ModuleCatalog.ENABLED_MODULES.containsAll(java.util.List.of("system", "infra")));
    }

    @Test
    void moduleOfMenuResolvesByPermissionComponentAndPath() {
        assertEquals("system", ModuleCatalog.moduleOfMenu("system:user:query", null, null));
        assertEquals("infra", ModuleCatalog.moduleOfMenu(null, "infra/file/index", null));
        assertEquals("bpm", ModuleCatalog.moduleOfMenu(null, null, "/bpm/task"));
        assertEquals("mall", ModuleCatalog.moduleOfMenu(null, null, "promotion"));
        assertNull(ModuleCatalog.moduleOfMenu(null, null, null)); // 纯容器
        assertNull(ModuleCatalog.moduleOfMenu("unknown:read", null, null)); // 未知前缀不归属
    }

    @Test
    void menuOfDisabledModuleIsNotAllowedWhileEnabledAndContainersAreAllowed() {
        assertFalse(ModuleCatalog.isMenuAllowed("bpm:task:query", null, null));
        assertFalse(ModuleCatalog.isMenuAllowed(null, "mall/product/index", null));
        assertFalse(ModuleCatalog.isMenuAllowed(null, null, "/crm/backlog"));
        assertTrue(ModuleCatalog.isMenuAllowed("infra:config:list", null, null));
        assertTrue(ModuleCatalog.isMenuAllowed(null, "system/user/index", null));
        assertTrue(ModuleCatalog.isMenuAllowed(null, null, "/system")); // 纯目录容器允许
        assertTrue(ModuleCatalog.isMenuAllowed(null, null, null)); // 无法归属的容器允许
    }
}
