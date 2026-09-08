# 代码生成模板命名说明（ZS-BRAND-005）

本目录模板在 ZS-BRAND-002 已按 [docs/06-品牌素材与命名映射](../../../../../docs/06-品牌素材与命名映射.md) 统一命名，并在 ZS-BRAND-005 复核。生成代码的包名一律来自配置项 `zszj.codegen.base-package`（当前为 `cn.zszj`），模板内不硬编码产品包名。

## 模板目录与启用状态

| 目录 | 对应 CodegenFrontTypeEnum | 一期状态 |
|---|---|---|
| `java/`、`sql/` | 后端（所有前端类型共用） | **启用** |
| `vue3/` | VUE3_ELEMENT_PLUS(20) | **启用**（`zszj.codegen.front-type: 20`，与 apps/zhongshu-admin-web 对应） |
| `vue3_admin_uniapp/` | VUE3_ADMIN_UNIAPP_WOT(60) | **待适配**（与 apps/zhongshu-miniapp 对应；启用前按本说明执行命名检查） |
| `vue3_vben/`、`vue3_vben5_antd/`、`vue3_vben5_antdv_next/`、`vue3_vben5_ele/`、`vue/` | 30～51、10 | **禁用/待适配**（对应上游 vben/vue2 前端，产品未启用；启用前须先按 docs/06 完成目标前端命名映射） |

## 生成物命名规则（与 docs/06 冻结映射一致）

- Java 包：`${basePackage}.module.<moduleName>...` → `cn.zszj.module.<moduleName>...`；
- 类名前缀：无产品前缀（沿用业务类名），框架基类来自 `cn.zszj.framework.*`；
- 菜单/权限字符串：`<moduleName>:<businessName>:<action>`，不含产品前缀；
- 前端生成物目标目录：Web 生成到 `apps/zhongshu-admin-web/src/views`，移动端生成到 `apps/zhongshu-miniapp/src`；不得生成到 `services/zhongshu-core/zszj-ui`（该目录是未启用的上游模板资产）；
- 模板中的上游参考链接（如 MyBatisX 插件文档）为来源说明，保留不改。

## 残留与一致性检查

- `src/main/resources/codegen/**` 与 `src/test/resources/codegen/**` 不得出现 `cn.iocoder`、`yudao`、`Yudao`、`YUDAO`、`youdao`、`众墅之家`；上游参考链接域名（iocoder.cn 文档站）按 docs/06 第 4.1 节保留；
- 测试夹具与断言的示例域与上游保持一致（`www.iocoder.cn` 属来源引用，按 docs/06 第 4.1 节保留并在门禁白名单登记）；模板、夹具、断言三者必须同批修改，保持 CodegenEngineTest 口径一致；
- 残留扫描入口：`node scripts/brand/verify-brand-naming.mjs`（ZS-BRAND-006）。

## 待验证项（B02）

"从独立 PG 测试表生成样例、放入目标结构后可编译、旧包残留明确失败"需运行 infra 代码生成服务与构建链（真实 PG + Maven），在 B02 按批次补证；本仓库当前无 JDK/Maven 工具链，已以静态一致性检查代替并如实记录。
