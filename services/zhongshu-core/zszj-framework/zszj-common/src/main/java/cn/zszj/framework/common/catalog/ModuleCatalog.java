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

    /** 商城子前缀归属（product/trade/promotion 等属 mall 模块，与 DefaultController 兜底分组一致） */
    private static final Map<String, String> MODULE_ALIASES = Map.of(
            "product", "mall",
            "trade", "mall",
            "promotion", "mall");

    /** 未启用模块 -> admin-api 兜底前缀（超过 10 项，必须用 ofEntries 而非 Map.of） */
    public static final Map<String, List<String>> DISABLED_MODULE_API_PREFIXES = Map.ofEntries(
            Map.entry("bpm", List.of("/admin-api/bpm/**")),
            Map.entry("mp", List.of("/admin-api/mp/**")),
            Map.entry("mall", List.of("/admin-api/product/**", "/admin-api/trade/**", "/admin-api/promotion/**")),
            Map.entry("erp", List.of("/admin-api/erp/**")),
            Map.entry("wms", List.of("/admin-api/wms/**")),
            Map.entry("pms", List.of("/admin-api/pms/**")),
            Map.entry("crm", List.of("/admin-api/crm/**")),
            Map.entry("mes", List.of("/admin-api/mes/**")),
            Map.entry("im", List.of("/admin-api/im/**")),
            Map.entry("report", List.of("/admin-api/report/**")),
            Map.entry("pay", List.of("/admin-api/pay/**")),
            Map.entry("ai", List.of("/admin-api/ai/**")),
            Map.entry("iot", List.of("/admin-api/iot/**")));

    /**
     * 归属菜单到业务模块。
     *
     * @return 模块名（含商城子前缀 product/trade/promotion → mall 的别名归并）；无法判定返回 null
     */
    public static String moduleOfMenu(String permission, String component, String path) {
        String first = firstSegment(permission, ':');
        if (first == null) first = firstSegment(component, '/');
        if (first == null) first = firstSegment(path, '/');
        if (first == null) return null;
        if (first.startsWith("/")) first = first.substring(1);
        if (MODULE_ALIASES.containsKey(first)) return MODULE_ALIASES.get(first);
        return ALL_MODULES.contains(first) ? first : null;
    }

    private static String firstSegment(String value, char sep) {
        if (value == null || value.isEmpty()) return null;
        String v = value.startsWith("/") ? value.substring(1) : value;
        int idx = v.indexOf(sep);
        String seg = idx >= 0 ? v.substring(0, idx) : v;
        return seg.isEmpty() ? null : seg;
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
