-- ZS-FILE-003 codex r0 P2：持久化内容 SHA-256 摘要（下载内容与入库散列一致的验收基准）。
ALTER TABLE infra_file ADD COLUMN IF NOT EXISTS file_hash varchar(64) NULL;
