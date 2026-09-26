-- ZS-FILE-004.B：导出件用途与保留期列——按用途保留期清理的数据载体
-- （FILE-005.B 计划 L46/L97 明文移交「导出文件按用途保留期清理」；主卡 docs/05 ZS-FILE-004）。
-- purpose='export' 时 retention_expire_time = 生成时刻 + infra.file.export.retention-days；
-- 普通上传/历史存量两列均 NULL——零变化（既有上传/交付/补偿路径不受影响）。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS purpose varchar(32) NULL DEFAULT NULL;
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS retention_expire_time timestamp NULL DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_infra_file_05 ON infra_file (purpose, retention_expire_time);
COMMENT ON COLUMN infra_file.purpose IS '用途（ZS-FILE-004.B：''export''=导出件，按用途保留期清理的判别依据；NULL=普通上传/历史）';
COMMENT ON COLUMN infra_file.retention_expire_time IS '保留期到期时间（ZS-FILE-004.B：导出件生成时刻+保留天数，专供保留期清理；NULL=无保留期约束）';
