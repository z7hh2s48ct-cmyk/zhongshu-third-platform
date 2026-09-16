-- ZS-FILE-005.B：删除中间态时间戳——超时 DELETING 记录自动补偿的超时依据与领取租约（不加新表，复用 infra_file status 列）。
-- deleting_time 语义：进入 DELETING 的时刻；补偿领取时前推（领取即租约 + 重试退避点）；
-- 引用保护拒绝回退 PUBLISHED 时清空。update_time 不可靠（update(null, wrapper) 不触发自动填充），故用专用列。
-- 存量 DELETING 记录以 update_time 回填进入时刻（该列此前只被状态转移触碰）。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS deleting_time timestamp NULL;
UPDATE infra_file SET deleting_time = update_time WHERE status = 'DELETING' AND deleting_time IS NULL;
CREATE INDEX IF NOT EXISTS idx_infra_file_status_deleting_time ON infra_file (status, deleting_time);
