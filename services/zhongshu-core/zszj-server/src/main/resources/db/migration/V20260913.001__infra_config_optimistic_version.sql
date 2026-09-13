-- ZS-CFG-004 codex r2 P1 改型：参数配置乐观锁改用独立整数 version 列。
-- r1 曾复用 update_time 作版本，存在 JSON 毫秒截断、datetime 秒级精度同秒版本不推进、创建端可伪造三项缺陷。
-- 版本由服务端维护（创建=0，成功更新原子 +1），更新请求必须携带编辑时版本。
ALTER TABLE infra_config ADD COLUMN IF NOT EXISTS version int NOT NULL DEFAULT 0;
