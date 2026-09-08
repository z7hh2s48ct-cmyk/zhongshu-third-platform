package cn.zszj.server;

import cn.zszj.framework.common.catalog.ModuleCatalog;

import java.util.List;
import java.util.Map;

/**
 * 模块启用白名单（ZS-ENG-001 / ZS-CFG-003.A）。
 *
 * 清单的唯一事实来源已下沉到 {@link ModuleCatalog}（zszj-common），供各模块
 * （如租户套餐的"不得启用关闭模块"校验）与默认兜底路由共同使用；本类保留为
 * zszj-server 装配边界的命名入口，字段与 ModuleCatalog 保持同一引用。
 *
 * 任何模块的启用/停用变更，必须同步修改：根 pom.xml 的 modules、
 * zszj-server/pom.xml 的依赖、以及 ModuleCatalog 清单，并通过
 * ModuleWhitelistTest 与 scripts/eng/verify-module-whitelist.mjs 两道检查。
 */
public final class ModuleWhitelist {

    /** 当前启用的业务模块 */
    public static final List<String> ENABLED_MODULES = ModuleCatalog.ENABLED_MODULES;

    /** 未启用模块 -> 其 admin-api 路径前缀 */
    public static final Map<String, List<String>> DISABLED_MODULE_API_PREFIXES =
            ModuleCatalog.DISABLED_MODULE_API_PREFIXES;

    private ModuleWhitelist() {
    }
}
