-- ZS-FILE-001.A：文件租户化与归属——私有附件归属技术租户与上传主体，公开素材显式标注。
-- 历史存量默认 PRIVATE（不能默认全部公开），由管理员经 update-scope 端点显式调整。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS tenant_id int8 NOT NULL DEFAULT 0;
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS owner_user_id int8 NOT NULL DEFAULT 0;
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS scope varchar(16) NOT NULL DEFAULT 'PRIVATE';
CREATE INDEX IF NOT EXISTS idx_infra_file_02 ON infra_file (tenant_id);
CREATE INDEX IF NOT EXISTS idx_infra_file_03 ON infra_file (scope);
