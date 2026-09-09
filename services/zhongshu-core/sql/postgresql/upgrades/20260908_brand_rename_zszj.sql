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
       'zsxq.iocoder.cn', 'zsxq.zszj.example.com')
 WHERE websites LIKE '%iocoder.cn%';

-- 1.1 修复历史升级遗留的租户同域冲突（hotfix-C P2-2 / codex r1 P2#2+P2#3）：
--     早期版本本脚本把 zsxq.iocoder.cn 与 www.iocoder.cn 一并映射到 www.zszj.example.com，
--     使租户 1（众墅之家）与租户 121（小租户）持有相同的 websites 分词：
--     getTenantByWebsite() 取首条且无 ORDER BY，域名选租户结果不确定；
--     validTenantWebsiteDuplicate() 亦拒绝租户 121 后续涉及 websites 的更新。
--     已升级库中源域已消失，重跑上面的 REPLACE 链无法自愈，故按演示租户身份定点还原。
--
--     codex r1 修正原实现（LIKE 子串判定 + REPLACE 子串替换）的两处越界：
--     P2#2 精确分词：websites 是逗号分隔列表，selectListByWebsite 以
--          POSITION(',value,' IN ',' || websites || ',') 做整词匹配（见 DbTypeEnum.POSTGRE_SQL），
--          故 www.zszj.example.com 与 www.zszj.example.com:3000 是两个不同绑定。原 LIKE '%...%'
--          会把带端口变体误判为冲突，且 REPLACE 子串替换会把运维为租户 121 手工配置的 :3000
--          地址一并改写、破坏其既有路由。改为按整词判定，且仅重写整词相等的分词、保留其余分词与顺序。
--     P2#3 目标归属：改写前须确认 zsxq.zszj.example.com 未被其他未删除租户占用；否则定点还原会把
--          该域赋给租户 121，制造新的同域冲突（歧义路由 + 更新被拒）。占用时 RAISE 使事务整体回滚
--          并给出可操作信息，交由运维人工裁决，绝不写入新重复。
--     幂等：还原后租户 121 不再持有 www.zszj.example.com 整词，v_src_conflict 为假即 RETURN。
DO $$
DECLARE
    v_src_conflict    boolean;
    v_target_occupied boolean;
BEGIN
    -- 源冲突：租户 121 与租户 1 是否都持有完全相同的 www 整词（排除 :端口 等变体）
    SELECT EXISTS (
             SELECT 1 FROM system_tenant
              WHERE id = 121 AND deleted = 0
                AND POSITION(',www.zszj.example.com,' IN ',' || websites || ',') > 0
           )
       AND EXISTS (
             SELECT 1 FROM system_tenant
              WHERE id = 1 AND deleted = 0
                AND POSITION(',www.zszj.example.com,' IN ',' || websites || ',') > 0
           )
      INTO v_src_conflict;

    IF NOT v_src_conflict THEN
        RETURN;  -- 无源冲突（含已还原后的幂等重跑）：不改动
    END IF;

    -- 目标占用：zsxq.zszj.example.com 是否已被租户 121 以外的未删除租户持有
    SELECT EXISTS (
             SELECT 1 FROM system_tenant
              WHERE id <> 121 AND deleted = 0
                AND POSITION(',zsxq.zszj.example.com,' IN ',' || websites || ',') > 0
           )
      INTO v_target_occupied;

    IF v_target_occupied THEN
        RAISE EXCEPTION
          'hotfix-C P2-2 定点还原受阻：租户 1 与 121 同持 www.zszj.example.com，但目标占位域 zsxq.zszj.example.com 已被其他未删除租户占用；请人工核对 system_tenant.websites 消除占用后再升级（本事务已回滚）';
    END IF;

    -- 仅重写租户 121 websites 中与之整词相等的分词，保留其余分词（含 :端口 变体）及原顺序
    UPDATE system_tenant
       SET websites = (
             SELECT string_agg(
                      CASE WHEN tok = 'www.zszj.example.com'
                           THEN 'zsxq.zszj.example.com'
                           ELSE tok END,
                      ',' ORDER BY ord)
               FROM unnest(string_to_array(system_tenant.websites, ','))
                    WITH ORDINALITY AS u(tok, ord)
           )
     WHERE id = 121 AND deleted = 0;
END $$;

-- 2. 部门：演示部门名
UPDATE system_dept
   SET name = '众墅之家'
 WHERE name = '芋道源码';

-- 3. 用户：演示账号用户名、昵称、邮箱与头像域名
-- 3.1 演示账号用户名：仅租户 1 的 id=100 演示账号（hotfix-A 修正：原 WHERE username='yudao' 波及所有租户）
--     先检查目标名冲突，避免唯一约束 (tenant_id, username) 冲突使事务回滚
--     hotfix-A-r1: 冲突检查限定 deleted=0，与应用 @TableLogic 语义一致，已删除行不阻塞改名
UPDATE system_users
   SET username = 'zszj'
 WHERE id = 100 AND tenant_id = 1 AND username = 'yudao'
   AND NOT EXISTS (
     SELECT 1 FROM system_users u2
      WHERE u2.tenant_id = 1 AND u2.username = 'zszj' AND u2.id <> 100 AND u2.deleted = 0
   );

UPDATE system_users
   SET nickname = REPLACE(nickname, '芋道', '众墅之家')
 WHERE nickname LIKE '%芋道%';

-- 3.2 演示账号邮箱：精确映射（hotfix-A 修正：先于通用域名替换，避免 yudao@iocoder.cn → yudao@example.com 残留旧标识）
--     hotfix-A-r1: 目标邮箱冲突检查，避免产生重复 active email 导致 selectByEmail/selectOne 异常
--     hotfix-A-r2: 冲突检查同时覆盖 3.3 通用域名替换后会变成 zszj@example.com 的行（zszj@iocoder.cn）
UPDATE system_users
   SET email = 'zszj@example.com'
 WHERE id = 100 AND tenant_id = 1 AND email = 'yudao@iocoder.cn'
   AND NOT EXISTS (
     SELECT 1 FROM system_users u2
      WHERE u2.tenant_id = 1 AND u2.id <> 100 AND u2.deleted = 0
        AND (u2.email = 'zszj@example.com' OR u2.email = 'zszj@iocoder.cn')
   );

-- 3.3 通用域名替换：处理其他 @iocoder.cn 邮箱（如 test@iocoder.cn）
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
       'zsxq.iocoder.cn', 'zsxq.zszj.example.com')
 WHERE redirect_uris LIKE '%iocoder.cn%';

-- 5.5 菜单外链：演示外链菜单中的上游演示域
UPDATE system_menu
   SET path = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(path,
       'www.iocoder.cn', 'www.zszj.example.com'),
       'test.iocoder.cn', 'test.zszj.example.com'),
       'doc.iocoder.cn', 'doc.zszj.example.com'),
       'cloud.iocoder.cn', 'doc.zszj.example.com'),
       'zsxq.iocoder.cn', 'zsxq.zszj.example.com')
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
--   SELECT count(*) FROM system_tenant  WHERE id = 121 AND POSITION(',www.zszj.example.com,' IN ',' || websites || ',') > 0;  -- hotfix-C P2-2 租户同域整词冲突（勿用 LIKE 子串，会误报 :端口 变体）
--   SELECT count(*) FROM system_users   WHERE nickname LIKE '%芋道%' OR (username = 'yudao' AND id = 100 AND tenant_id = 1) OR email = 'yudao@iocoder.cn' OR avatar LIKE '%yudao.iocoder.cn%';
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
