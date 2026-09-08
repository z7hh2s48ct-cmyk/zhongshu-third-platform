-- =====================================================================
-- V20260909.003：字典编码唯一约束（ZS-CFG-002.A）
-- 目标：字典类型编码（type）与字典项（dict_type + value）在数据库层防止重复——
--       并发写入也不产生重复编码。
-- 语义：采用部分唯一索引（仅约束在册行 deleted = 0）：
--       在册行唯一；已逻辑删除的行不阻塞同编码重建。
-- 新库：V1 建表后本迁移建立约束；既有库：baseline 吸收后按版本号应用。
-- 回滚：DROP INDEX（不可逆级别低，前向修复即重建索引）。
-- =====================================================================

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_dict_type_type
    ON system_dict_type (type) WHERE deleted = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_dict_data_type_value
    ON system_dict_data (dict_type, value) WHERE deleted = 0;
