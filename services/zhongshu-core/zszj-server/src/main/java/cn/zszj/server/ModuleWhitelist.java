package cn.zszj.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块启用白名单（唯一启用清单，ZS-ENG-001）。
 *
 * 本类是"哪些业务模块在当前底座中启用"的唯一事实来源：
 * - 启用模块：以 zszj-server/pom.xml 中实际引入的 zszj-module-* 依赖为准，必须与 {@link #ENABLED_MODULES} 一致；
 * - 未启用模块：保留源码资产（services/zhongshu-core 下的 zszj-module-* 目录），不参与编译装配，
 *   其 API 前缀由 {@link #DISABLED_MODULE_API_PREFIXES} 登记并由 DefaultController 返回明确的不可用响应；
 * - 任何模块的启用/停用变更，必须同步修改：根 pom.xml 的 modules、zszj-server/pom.xml 的依赖、以及本类清单，
 *   并通过 ModuleWhitelistTest 与 scripts/eng/verify-module-whitelist.mjs 两道检查。
 *
 * 注意：未启用模块不得存在业务 Bean、后台任务或可操作菜单；菜单种子数据的治理归 ZS-DB-004。
 */
public final class ModuleWhitelist {

    /**
     * 当前启用的业务模块（zszj-server 实际依赖的 zszj-module-* 名称后缀）。
     */
    public static final List<String> ENABLED_MODULES = List.of("system", "infra");

    /**
     * 未启用模块 -> 其 admin-api 路径前缀（DefaultController 对这些前缀返回明确的不可用响应）。
     */
    public static final Map<String, List<String>> DISABLED_MODULE_API_PREFIXES;

    static {
        Map<String, List<String>> prefixes = new LinkedHashMap<>();
        prefixes.put("bpm", List.of("/admin-api/bpm/**"));
        prefixes.put("mp", List.of("/admin-api/mp/**"));
        prefixes.put("mall", List.of("/admin-api/product/**", "/admin-api/trade/**", "/admin-api/promotion/**"));
        prefixes.put("erp", List.of("/admin-api/erp/**"));
        prefixes.put("wms", List.of("/admin-api/wms/**"));
        prefixes.put("pms", List.of("/admin-api/pms/**"));
        prefixes.put("crm", List.of("/admin-api/crm/**"));
        prefixes.put("mes", List.of("/admin-api/mes/**"));
        prefixes.put("im", List.of("/admin-api/im/**"));
        prefixes.put("report", List.of("/admin-api/report/**"));
        prefixes.put("pay", List.of("/admin-api/pay/**"));
        prefixes.put("ai", List.of("/admin-api/ai/**"));
        prefixes.put("iot", List.of("/admin-api/iot/**"));
        DISABLED_MODULE_API_PREFIXES = Map.copyOf(prefixes);
    }

    private ModuleWhitelist() {
    }
}
