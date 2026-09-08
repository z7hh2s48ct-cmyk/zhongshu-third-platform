# 众墅之家 AI 赋能平台 · 后端核心（zhongshu-core）

本目录是众墅之家 AI 赋能平台的 Java 后端单体仓库，基于上游开源项目 [ruoyi-vue-pro](https://gitee.com/zhijiantianya/ruoyi-vue-pro)（JDK 17 + Spring Boot 3.5 分支）迁入并按众墅之家要求改造。产品命名统一使用 `zszj` 前缀，映射依据见 [docs/06-品牌素材与命名映射](../../docs/06-品牌素材与命名映射.md)。

## 模块结构

| 模块 | 说明 |
|---|---|
| `zszj-dependencies` | 依赖版本 BOM |
| `zszj-framework` | 技术框架组件（`zszj-common`、`zszj-spring-boot-starter-*`：web/security/mybatis/tenant/redis/job/mq/monitor/excel/protection/test 等） |
| `zszj-server` | 后端主项目（空壳容器，按白名单装配业务模块） |
| `zszj-module-system` | 系统管理模块（用户、角色、菜单、租户、认证等） |
| `zszj-module-infra` | 基础设施模块（文件、代码生成、定时任务、API 日志等） |
| `zszj-module-*`（其余） | 上游业务模块资产（bpm/pay/mall/crm/erp 等），默认未启用，仅作源码保留；启用须按底座批次白名单与验收执行 |
| `zszj-ui` | 上游前端模板资产（vue3/vben/vue2/uniapp），未启用；当前产品前端位于仓库 `apps/` 目录 |

## 构建与运行

```bash
# JDK 17 + Maven（版本基线以 docs 文档与 ZS-ENG-002 登记为准）
mvn clean package -DskipTests
# 可执行产物
zszj-server/target/zszj-server.jar
# 默认激活 local profile；数据源/Redis/存储等运行参数按环境注入，见 application*.yaml
```

- 目标数据库为 PostgreSQL（D-08 决策禁止 MySQL 过渡基线）；初始化脚本见 [sql/postgresql](sql/postgresql/)。
- 模块启用边界、配置合同与环境注入规则由底座任务（ZS-ENG-001/003、ZS-DB-001 等）管理，本 README 不替代批次验收。

## 品牌与命名

- 产品展示名"众墅之家"，平台全称"众墅之家 AI 赋能平台"；品牌素材台账与改名映射见 [docs/06](../../docs/06-品牌素材与命名映射.md)。
- Java 包根 `cn.zszj`、Maven GroupId `cn.zszj`、配置前缀 `zszj.*`、环境变量前缀 `ZSZJ_`。
- 上游来源注释、许可证与第三方声明按原样保留，不属于品牌清理范围。

## 来源与上游说明

本目录源码迁自上游项目，上游信息保留如下（也见仓库根 [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md)）：

- 上游仓库：[ruoyi-vue-pro](https://gitee.com/zhijiantianya/ruoyi-vue-pro) / [GitHub 镜像](https://github.com/YunaiV/ruoyi-vue-pro)
- 上游文档：<https://doc.iocoder.cn/>
- 上游作者：芋艿（YunaiV）；上游采用 MIT License（[上游 LICENSE](https://gitee.com/zhijiantianya/ruoyi-vue-pro/blob/master/LICENSE)，本地副本见 [LICENSE](LICENSE)）

> 严肃声明（保留自上游）：上游项目现在、未来都不会有商业版本，所有代码全部开源。本产品对其的修改遵循同一开源协议要求。
