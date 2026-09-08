-- ZS-DB-002：按 docs/数据库环境方案.md 执行的建库/授权操作（本地独立测试环境实测脚本）
-- 以超管连接执行；口令仅用于一次性本地验证，不入任何环境配置
\set ON_ERROR_STOP on

-- 1. 角色（库级）
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zhongshu_owner') THEN
    CREATE ROLE zhongshu_owner LOGIN PASSWORD 'owner_local_1';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zhongshu_app') THEN
    CREATE ROLE zhongshu_app LOGIN PASSWORD 'app_local_1';
  END IF;
END $$;

-- 2. 库属主
ALTER DATABASE zhongshu OWNER TO zhongshu_owner;

-- 3. 授权（模板第 2 节）
GRANT ALL ON SCHEMA public TO zhongshu_owner;
GRANT USAGE ON SCHEMA public TO zhongshu_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO zhongshu_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO zhongshu_app;
ALTER DEFAULT PRIVILEGES FOR ROLE zhongshu_owner IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO zhongshu_app;
ALTER DEFAULT PRIVILEGES FOR ROLE zhongshu_owner IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO zhongshu_app;
