-- ZS-FILE-001.B：文件业务组织归属列——org 轴对象授权的数据载体
-- （docs/05 §16.1 line1039「按批准组织/对象授权下载，不把 tenant 简单改名」）。
-- 服务端签发的上传组织（源 LoginUser.getOrgId()）；匿名/系统/无默认任职为 NULL——历史存量默认 NULL，
-- 仍由 tenant 轴（ZS-FILE-001.A）治理；org 门仅对 organization_id IS NOT NULL 的文件施加
-- （非 NULL 时由组织范围独占裁决读取/删除，D-09 FND-AUTH-004：本人所有权不凌驾组织排除）。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS organization_id int8 NULL DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_infra_file_04 ON infra_file (organization_id);
COMMENT ON COLUMN infra_file.organization_id IS '业务组织归属ID（ZS-FILE-001.B，org 轴对象授权载体；NULL=历史/匿名文件由 tenant 轴治理）';
