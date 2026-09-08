# 众墅之家环境配置合同（ZS-ENG-003 / ZS-CFG-001.A）

本文是 `zszj-server` 各环境配置的唯一合同。原则：地址与凭据按环境注入；秘密只存在于环境变量或外部注入，不入版本库；缺失必需项时启动明确失败（占位符解析错误，包含变量名、不含秘密值），不静默回退演示服务。

## 配置层次

| 层次 | 位置 | 用途 |
|---|---|---|
| 打包内置 | `zszj-server/src/main/resources/application.yaml` | 全环境公共默认值（不含凭据、不含环境地址） |
| 开发覆盖 | `application-local.yaml`（默认激活）/ `application-dev.yaml` | 本地开发；演示值已中性化为环境变量占位符 |
| 部署模板 | [application-prod.yaml](application-prod.yaml) | 生产/独立测试环境模板，全部经 `ZSZJ_*` 环境变量注入，复制到部署机 jar 同级 `config/` 目录后按需填充 |

## 必填项与失败语义

- 必填项在模板中写作无默认值的占位符（如 `${ZSZJ_DATASOURCE_URL}`）；漏配时 Spring 启动报
  `Could not resolve placeholder 'ZSZJ_DATASOURCE_URL'` ——错误信息只含变量名，属于脱敏错误。
- 可选项写作带默认值的占位符（如 `${ZSZJ_REDIS_PORT:6379}`），默认值必须是中性值，不得是上游演示服务地址。
- 禁止把真实密钥写入任何受版本控制的 `.yaml`/`.env*`；提交前用 `node scripts/cfg/verify-config-secrets.mjs`（ZS-CFG-001.A）扫描。

## 环境变量清单（部署模板）

| 变量 | 必填 | 说明 |
|---|---|---|
| `ZSZJ_SERVER_PORT` | 否（默认 48080） | HTTP 端口 |
| `ZSZJ_DATASOURCE_URL` | **是** | 目标数据库 JDBC URL（D-08：PostgreSQL） |
| `ZSZJ_DATASOURCE_USERNAME` / `ZSZJ_DATASOURCE_PASSWORD` | **是** | 应用账号（低权限，迁移权限分离归 ZS-DB-002） |
| `ZSZJ_DATASOURCE_VALIDATION_QUERY` | 否（按方言） | 连接有效性检查 SQL，PG 使用 `SELECT 1` |
| `ZSZJ_REDIS_HOST` / `ZSZJ_REDIS_PORT` / `ZSZJ_REDIS_PASSWORD` | **是**（PORT 默认 6379） | Redis 连接 |
| `ZSZJ_LOG_LEVEL_MODULE` | 否（默认 INFO） | 启用模块 Mapper 日志级别 |
| `ZSZJ_PROFILE` | **是** | 激活 profile（部署机经 `--spring.profiles.active` 或此变量注入） |

说明：数据源驱动依赖与打包核验归 ZS-DB-001；模拟从库的清除归 ZS-DB-001；模拟认证（mock-enable）默认关闭归 ZS-ENG-004。

## 与其他任务的边界

- ZS-ENG-004：`zszj.security.mock-enable` 默认 false；`/test` 调试端点移除或显式限制。
- ZS-ENG-005：actuator/Spring Boot Admin 端点暴露白名单（local 的 `include: '*'` 收紧）。
- ZS-ENG-006：Quartz `auto-startup` 与任务启停边界盘点。
- ZS-CFG-001.A：秘密扫描工具与模板扫描门禁（承接本合同的机器化检查）。
