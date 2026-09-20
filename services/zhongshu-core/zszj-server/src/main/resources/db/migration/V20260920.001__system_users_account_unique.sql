-- =====================================================================
-- V20260920.001：账号唯一约束与并发兜底（ZS-DB-010）
-- 背景：ZS-DB-009 实证——system_users 的 username/mobile/email 仅普通索引，
--       应用层「先查重后写入」在并发窗口与跨租户下无法拦截重复账号。
-- 目标：以数据库层唯一约束为最终防线（D-09 批准矩阵 2026-09-10 已拍板），
--       应用层查重保留为友好提示，DB 约束兜底并发与绕过路径。
-- 语义（对齐 docs/07 §4 D-09）：
--   M1 用户名全平台唯一（跨租户）——lower(btrim(username)) 表达式部分唯一索引；
--   M2 trim + 小写化——迁移内一次性规范化存量 username；
--   M3 手机/邮箱全局唯一，无值（NULL 或空串）允许多行；
--   M4 逻辑删除后可重建——部分索引仅约束在册行 deleted = 0。
-- 硬约束：禁止自动删重。历史冲突一律 fail-loudly（RAISE EXCEPTION）先报告，
--         等人工按批准矩阵处置后重跑，绝不静默丢弃数据。
-- 回滚：DROP INDEX（不可逆级别低，前向修复即重建索引）。
-- =====================================================================

-- 1. 历史冲突报告（fail-loudly，禁止擅自删重）
--    username 按规范化键 lower(btrim()) 分组；mobile/email 按原值分组，排除无值行。
DO $$
DECLARE
    v_username_groups bigint;
    v_mobile_groups   bigint;
    v_email_groups    bigint;
BEGIN
    SELECT count(*) INTO v_username_groups FROM (
        SELECT lower(btrim(username)) AS k
        FROM system_users
        WHERE deleted = 0
        GROUP BY lower(btrim(username))
        HAVING count(*) > 1
    ) t;

    SELECT count(*) INTO v_mobile_groups FROM (
        SELECT mobile AS k
        FROM system_users
        WHERE deleted = 0 AND mobile IS NOT NULL AND mobile <> ''
        GROUP BY mobile
        HAVING count(*) > 1
    ) t;

    SELECT count(*) INTO v_email_groups FROM (
        SELECT email AS k
        FROM system_users
        WHERE deleted = 0 AND email IS NOT NULL AND email <> ''
        GROUP BY email
        HAVING count(*) > 1
    ) t;

    IF v_username_groups > 0 OR v_mobile_groups > 0 OR v_email_groups > 0 THEN
        RAISE EXCEPTION
            'ZS-DB-010 历史账号冲突，迁移已中止（禁止自动删重）：username 冲突组=% ，mobile 冲突组=% ，email 冲突组=% 。请按 docs/07 §4 D-09 批准矩阵人工核查并处置重复账号后重跑本迁移。',
            v_username_groups, v_mobile_groups, v_email_groups;
    END IF;
END $$;

-- 2. 存量 username 规范化（M2：trim + 小写化），仅改动与规范化值不一致的行
UPDATE system_users
SET username = lower(btrim(username))
WHERE username IS DISTINCT FROM lower(btrim(username));

-- 3. 建立部分唯一索引（M1/M3/M4：仅约束在册行，支持逻辑删除后重建）
--    username 用 lower(btrim()) 表达式索引，使 DB 约束语义与应用层规范化（trim+小写）
--    完全一致，即便绕过应用层直接写入带空格/大写的值也能兜底；mobile/email 用原值列。
--    约束名与 AdminUserServiceImpl#translateDuplicateKey 的错误码映射严格对齐。
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_users_username
    ON system_users (lower(btrim(username))) WHERE deleted = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_users_mobile
    ON system_users (mobile) WHERE deleted = 0 AND mobile IS NOT NULL AND mobile <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_users_email
    ON system_users (email) WHERE deleted = 0 AND email IS NOT NULL AND email <> '';

-- 4. 保留原有普通索引 idx_system_users_01/02/03（username/mobile/email）作为等值查询路径：
--    selectByUsername/selectByMobile/selectByEmail 均为 `WHERE col = ?` 等值谓词，
--    而 uk_system_users_username 是 lower(btrim(username)) 表达式索引、无法服务普通等值查询；
--    uk_system_users_mobile/email 为部分索引，参数化查询下规划器未必能证明谓词蕴含。
--    故唯一约束与查询索引各司其职，不删原普通索引（避免登录/校验查询退化为顺序扫描）。
