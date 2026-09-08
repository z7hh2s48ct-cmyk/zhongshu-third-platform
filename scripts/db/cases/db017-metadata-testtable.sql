-- =====================================================================
-- ZS-DB-017：PG 元数据识别测试表（含主键/序列、时间、布尔、长文本、JSON、注释、索引）
-- 建表后由 information_schema 元数据查询核对（DatabaseTableServiceImpl 的
-- MyBatis-Plus Generator 元数据读取在 B02 以应用级验证补证）。
-- =====================================================================

CREATE TABLE zhongshu_meta_test (
    id              int8         NOT NULL,
    biz_name        varchar(100) NOT NULL DEFAULT '',
    amount          int4         NULL DEFAULT 0,
    enabled         int2         NOT NULL DEFAULT 0,
    created_at      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    long_text       text         NULL,
    ext_json        varchar(500) NULL,
    tenant_id       int8         NOT NULL DEFAULT 0,
    CONSTRAINT pk_zhongshu_meta_test PRIMARY KEY (id)
);

CREATE SEQUENCE zhongshu_meta_test_seq START 1;
ALTER TABLE zhongshu_meta_test ALTER COLUMN id SET DEFAULT nextval('zhongshu_meta_test_seq');
CREATE INDEX idx_zhongshu_meta_test_biz ON zhongshu_meta_test (biz_name);

COMMENT ON TABLE zhongshu_meta_test IS 'ZS-DB-017 元数据识别测试表';
COMMENT ON COLUMN zhongshu_meta_test.id IS '主键（序列）';
COMMENT ON COLUMN zhongshu_meta_test.biz_name IS '业务名称';
COMMENT ON COLUMN zhongshu_meta_test.enabled IS '布尔语义（0/1）';
COMMENT ON COLUMN zhongshu_meta_test.ext_json IS '扩展 JSON（varchar 承载）';

-- 元数据核对查询（每条应返回预期值；在夹具运行器中逐条断言）
-- 1) 列清单与默认值
SELECT column_name, data_type, coalesce(column_default, '-') AS dflt
  FROM information_schema.columns
 WHERE table_schema = 'public' AND table_name = 'zhongshu_meta_test'
 ORDER BY ordinal_position;
-- 2) 注释核对（表/列注释应与上方 COMMENT 一致）
SELECT obj_description('public.zhongshu_meta_test'::regclass, 'pg_class') AS table_comment;
SELECT a.attname AS column_name, col_description(a.attrelid, a.attnum) AS col_comment
  FROM pg_attribute a
 WHERE a.attrelid = 'public.zhongshu_meta_test'::regclass AND a.attnum > 0 AND NOT a.attisdropped
 ORDER BY a.attnum;
-- 3) 索引核对
SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'zhongshu_meta_test';
