-- ZS-FILE-005.A：文件资产状态列——删除中间态可恢复记录与人工对账的基础。
-- PUBLISHED=正常可用（存量行默认回填）；DELETING=对象删除未完成的中间态（失败可重试、可对账），
-- 自动补偿与孤儿对象清理归 FILE-005.B（B05，无 Outbox 依赖不反向阻塞本批次）。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS status varchar(16) NOT NULL DEFAULT 'PUBLISHED';
