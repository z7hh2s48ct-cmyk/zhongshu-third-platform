package cn.zszj.framework.common.catalog;

import java.util.List;
import java.util.Map;

/**
 * 模块运行目录（ZS-ENG-001 / ZS-CFG-003.A 的唯一事实来源，下沉到 common 供各模块校验）。
 *
 * - {@link #ENABLED_MODULES}：当前启用的业务模块，必须与 zszj-server/pom.xml 实际依赖一致
 *   （由 zszj-server 的 ModuleWhitelistTest 与 scripts/eng/verify-module-whitelist.mjs 双重校验）；
 * - {@link #DISABLED_MODULES}：保留源码但不参与编译装配的业务模块；
 * - {@link #DISABLED_MODULE_API_PREFIXES}：未启用模块的 admin-api 兜底前缀（DefaultController 使用）；
 * - {@link #moduleOfMenu}：按 permission/component/path 归属菜单到模块，供"套餐不能启用关闭模块"
 *   校验（TenantPackageServiceImpl）与菜单治理使用。
 */
public final class ModuleCatalog {

    /** 当前启用的业务模块 */
    public static final List<String> ENABLED_MODULES = List.of("system", "infra");

    /** 未启用的业务模块（源码保留，不装配） */
    public static final List<String> DISABLED_MODULES = List.of(
            "bpm", "mp", "mall", "erp", "wms", "pms", "crm", "mes", "im", "report", "pay", "ai", "iot");

    /** 全部业务模块（归属判定顺序：无前缀重叠，顺序不敏感） */
    private static final List<String> ALL_MODULES = List.of(
            "system", "infra", "bpm", "mp", "mall", "erp", "wms", "pms", "crm", "mes", "im", "report", "pay", "ai", "iot");

    /** 未启用模块 -> admin-api 兜底前缀 */
    public static final Map<String, List<String>> DISABLED_MODULE_API_PREFIXES = Map.of(
            "bpm", List.of("/admin-api/bpm/**"),
            "mp", List.of("/admin-api/mp/**"),
            "mall", List.of("/admin-api/product/**", "/admin-api/trade/**", "/admin-api/promotion/**"),
            "erp", List.of("/admin-api/erp/**"),
            "wms", List.of("/admin-api/wms/**"),
            "pms", List.of("/admin-api/pms/**"),
            "crm", List.of("/admin-api/crm/**"),
            "mes", List.of("/admin-api/mes/**"),
            "im", List.of("/admin-api/im/**"),
            "report", List.of("/admin-api/report/**"),
            "pay", List.of("/admin-api/pay/**"),
            "ai", List.of("/admin-api/ai/**"),
            "iot", List.of("/admin-api/iot/**"));

    /**
     * 归属菜单到业务模块。
     *
     * @return 模块名；无法判定（纯目录容器、外链、空配置等）返回 null
     */
    public static String moduleOfMenu(String permission, String component, String path) {
        if (permission != null) {
            for (String module : ALL_MODULES) {
                if (permission.startsWith(module + ":")) return module;
            }
        }
        if (component != null) {
            for (String module : ALL_MODULES) {
                if (component.startsWith(module + "/")) return module;
            }
        }
        if (path != null) {
            String p = path.startsWith("/") ? path.substring(1) : path;
            for (String module : ALL_MODULES) {
                if (p.equals(module) || p.startsWith(module + "/")) return module;
            }
        }
        return null;
    }

    /**
     * 菜单是否允许进入运行目录：无法归属模块的菜单（目录容器等）允许；
     * 归属到未启用模块的菜单不允许（套餐/角色不得通过其启用关闭模块的功能入口）。
     */
    public static boolean isMenuAllowed(String permission, String component, String path) {
        String module = moduleOfMenu(permission, component, path);
        return module == null || ENABLED_MODULES.contains(module);
    }

    private ModuleCatalog() {
    }
}
