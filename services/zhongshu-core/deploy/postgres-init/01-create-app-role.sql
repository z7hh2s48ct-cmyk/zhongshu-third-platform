-- ZS-OPS-002.A PostgreSQL 首次初始化脚本
-- 由 docker-entrypoint-initdb.d 自动执行（仅首次创建数据卷时）
--
-- 前置：ZS-DB-002（凭据分离：owner 与 app 不同账号）
-- 作用：创建应用低权限角色并授予 CRUD 权限（无 DDL），使全新数据卷部署后
--       应用即可用 ZSZJ_DATASOURCE_* 凭据认证，无需手工建角色（codex r1 P1 处置）
--
-- 说明：
--   1. 本脚本在 POSTGRES_USER（owner）上下文、POSTGRES_DB 库中执行
--   2. 应用凭据由 compose 环境变量 ZSZJ_DATASOURCE_USERNAME/PASSWORD 注入
--   3. 口令通过 psql 变量 + format(%L) 传入，不硬编码、不经 shell 字符串拼接
--   4. 用 \gexec 而非 DO 块：psql 不在美元引用（dollar-quoted）内做变量插值

\set ON_ERROR_STOP on

-- 从环境变量读取应用账号凭据（psql 客户端侧 \set + 反引号 shell 命令替换）
\set app_user `echo "$ZSZJ_DATASOURCE_USERNAME"`
\set app_pass `echo "$ZSZJ_DATASOURCE_PASSWORD"`

-- 创建应用角色（幂等：仅当不存在时执行）
-- format 的 %I 安全引用角色名、%L 安全引用口令字面量，避免注入与引号问题
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

-- 授予 schema 使用权（最小权限：应用不建表，DDL 由 owner 通过 Flyway 执行）
GRANT USAGE ON SCHEMA public TO :"app_user";

-- 默认权限：owner 今后在 public 下创建的表/序列自动授权给应用角色
-- 省略 FOR ROLE，即作用于当前角色（owner）创建的对象，适配 Flyway 以 owner 迁移
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";

-- 对已存在对象授权（首次初始化时通常无表；复用数据卷或迁移后新增表时可重跑本段）
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"app_user";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app_user";
