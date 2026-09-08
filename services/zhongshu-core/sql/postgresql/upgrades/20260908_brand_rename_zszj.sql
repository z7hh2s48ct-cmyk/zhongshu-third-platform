-- =====================================================================
-- ZS-BRAND-004：品牌改名存量迁移（旧产品标识 → zszj / 众墅之家）
-- 文件：sql/postgresql/upgrades/20260908_brand_rename_zszj.sql
-- 适用：以 brand-rename-baseline（含）之前种子初始化、存在旧产品标识的 PostgreSQL 库
--     ：新装库直接使用改名后的 sql/postgresql/ruoyi-vue-pro.sql，无需本脚本
-- 映射依据：docs/06-品牌素材与命名映射.md（芋道→众墅之家、yudao→zszj、演示域中性化）
-- 边界：仅迁移承载旧产品标识的种子/演示数据；不改动业务内容、历史审计与已执行迁移
-- 特性：单事务原子执行（失败整体回滚）；幂等（重复执行无效果、无报错）
-- =====================================================================

BEGIN;

-- 1. 租户：租户名（登录租户标识）、演示联系人、演示域名
UPDATE system_tenant
   SET name = '众墅之家'
 WHERE name = '芋道源码';

UPDATE system_tenant
   SET contact_name = REPLACE(contact_name, '芋道', '众墅之家')
 WHERE contact_name LIKE '%芋道%';

UPDATE system_tenant
   SET websites = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(websites,
       'www.iocoder.cn', 'www.zszj.example.com'),
       'test.iocoder.cn', 'test.zszj.example.com'),
       'doc.iocoder.cn', 'doc.zszj.example.com'),
       'cloud.iocoder.cn', 'doc.zszj.example.com'),
       'zsxq.iocoder.cn', 'www.zszj.example.com')
 WHERE websites LIKE '%iocoder.cn%';

-- 2. 部门：演示部门名
UPDATE system_dept
   SET name = '众墅之家'
 WHERE name = '芋道源码';

-- 3. 用户：演示账号用户名、昵称、邮箱与头像域名
UPDATE system_users
   SET username = 'zszj'
 WHERE username = 'yudao';

UPDATE system_users
   SET nickname = REPLACE(nickname, '芋道', '众墅之家')
 WHERE nickname LIKE '%芋道%';

UPDATE system_users
   SET email = REPLACE(email, '@iocoder.cn', '@example.com')
 WHERE email LIKE '%@iocoder.cn%';

UPDATE system_users
   SET avatar = REPLACE(avatar, 'test.yudao.iocoder.cn', 'static.zszj.example.com')
 WHERE avatar LIKE '%yudao.iocoder.cn%';

-- 4. 通知公告：演示标题与内容中的品牌及图片域名
UPDATE system_notice
   SET title = '众墅之家公告示例'
 WHERE title = '芋道的公众';

UPDATE system_notice
   SET title = REPLACE(title, '芋道', '众墅之家')
 WHERE title LIKE '%芋道%';

UPDATE system_notice
   SET content = REPLACE(content, 'test.yudao.iocoder.cn', 'static.zszj.example.com')
 WHERE content LIKE '%yudao.iocoder.cn%';

-- 5. OAuth2 客户端：演示客户端标识、名称、Logo 与回调演示域
UPDATE system_oauth2_client
   SET client_id = 'zszj-' || substring(client_id from 7)
 WHERE client_id LIKE 'yudao-%';

UPDATE system_oauth2_client
   SET name = REPLACE(name, '芋道', '众墅之家')
 WHERE name LIKE '%芋道%';

UPDATE system_oauth2_client
   SET logo = REPLACE(logo, 'test.yudao.iocoder.cn', 'static.zszj.example.com')
 WHERE logo LIKE '%yudao.iocoder.cn%';

UPDATE system_oauth2_client
   SET redirect_uris = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(redirect_uris,
       'www.iocoder.cn', 'www.zszj.example.com'),
       'test.iocoder.cn', 'test.zszj.example.com'),
       'doc.iocoder.cn', 'doc.zszj.example.com'),
       'cloud.iocoder.cn', 'doc.zszj.example.com'),
       'zsxq.iocoder.cn', 'www.zszj.example.com')
 WHERE redirect_uris LIKE '%iocoder.cn%';

-- 5.5 菜单外链：演示外链菜单中的上游演示域
UPDATE system_menu
   SET path = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(path,
       'www.iocoder.cn', 'www.zszj.example.com'),
       'test.iocoder.cn', 'test.zszj.example.com'),
       'doc.iocoder.cn', 'doc.zszj.example.com'),
       'cloud.iocoder.cn', 'doc.zszj.example.com'),
       'zsxq.iocoder.cn', 'www.zszj.example.com')
 WHERE path LIKE '%iocoder.cn%';

-- 6. 演示表：yudao_demo* 表/序列/约束/索引改名为 zszj_demo*
--    （代码侧 @TableName 已在 ZS-BRAND-002 统一为 zszj_demo*，见 docs/06 映射 #11）
DO $$
DECLARE
    r record;
BEGIN
    -- 6.1 主键/唯一约束改名（先于表改名，按旧约束名匹配）
    FOR r IN
        SELECT tc.table_name, tc.constraint_name
          FROM information_schema.table_constraints tc
         WHERE tc.table_schema = current_schema()
           AND tc.constraint_type IN ('PRIMARY KEY', 'UNIQUE')
           AND tc.constraint_name LIKE 'pk\_yudao\_%'
    LOOP
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I',
                       r.table_name, r.constraint_name,
                       'pk_zszj_' || substring(r.constraint_name from 9));
    END LOOP;

    -- 6.2 表改名
    FOR r IN
        SELECT table_name
          FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name LIKE 'yudao\_%'
    LOOP
        EXECUTE format('ALTER TABLE %I RENAME TO %I',
                       r.table_name, 'zszj_' || substring(r.table_name from 7));
    END LOOP;

    -- 6.3 序列改名（列默认值按 OID 绑定，不受改名影响）
    FOR r IN
        SELECT sequencename
          FROM pg_sequences
         WHERE schemaname = current_schema()
           AND sequencename LIKE 'yudao\_%'
    LOOP
        EXECUTE format('ALTER SEQUENCE %I RENAME TO %I',
                       r.sequencename, 'zszj_' || substring(r.sequencename from 7));
    END LOOP;

    -- 6.4 索引改名
    FOR r IN
        SELECT indexname
          FROM pg_indexes
         WHERE schemaname = current_schema()
           AND indexname LIKE 'yudao\_%'
    LOOP
        EXECUTE format('ALTER INDEX %I RENAME TO %I',
                       r.indexname, 'zszj_' || substring(r.indexname from 7));
    END LOOP;
END $$;

COMMIT;

-- =====================================================================
-- 验证（迁移后人工/脚本执行，期望全部为 0）：
--   SELECT count(*) FROM system_tenant  WHERE name LIKE '%芋道%' OR websites LIKE '%iocoder.cn%';
--   SELECT count(*) FROM system_users   WHERE nickname LIKE '%芋道%' OR username = 'yudao' OR avatar LIKE '%yudao.iocoder.cn%';
--   SELECT count(*) FROM system_notice  WHERE title LIKE '%芋道%' OR content LIKE '%yudao.iocoder.cn%';
--   SELECT count(*) FROM system_dept    WHERE name LIKE '%芋道%';
--   SELECT count(*) FROM system_oauth2_client WHERE client_id LIKE 'yudao-%' OR logo LIKE '%yudao.iocoder.cn%' OR redirect_uris LIKE '%iocoder.cn%';
--   SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name LIKE 'yudao\_%';
--   SELECT count(*) FROM pg_sequences WHERE schemaname = current_schema() AND sequencename LIKE 'yudao\_%';
-- 说明：
--   * 字典/菜单/参数/定时任务种子经基线核查不含旧产品标识，故无对应更新语句；
--   * Redis 会话与浏览器存储键不含产品前缀，随过期/退出自然更替（docs/06 映射 #11）；
--   * B03/B05 子项（ZS-BRAND-004.B/.C）对缓存、会话、任务持久化做联验，本脚本不涉及。
-- =====================================================================
