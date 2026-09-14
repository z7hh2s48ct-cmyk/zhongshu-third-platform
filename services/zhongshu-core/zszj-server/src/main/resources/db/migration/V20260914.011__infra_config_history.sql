-- ZS-CFG-004 B04（复验审计部分）：配置变更历史表——变更审计与「审查后恢复」流程的持久化基础。
-- 仅追加写：每次 create/update/delete/restore 记录变更前后值、乐观锁版本、操作者与恢复审查依据；
-- 秘密/敏感参数（ConfigSensitiveClassifier 判定）只落掩码 ******，明文不落历史（敏感旧值不写审计原文）。
CREATE SEQUENCE IF NOT EXISTS infra_config_history_seq START 1;
CREATE TABLE IF NOT EXISTS infra_config_history (
    id int8 NOT NULL DEFAULT nextval('infra_config_history_seq'),
    config_id int8 NOT NULL,
    config_key varchar(100) NOT NULL,
    change_type varchar(16) NOT NULL,
    old_value varchar(500) NULL,
    new_value varchar(500) NULL,
    old_version int4 NULL,
    new_version int4 NULL,
    operator_id int8 NULL,
    reason varchar(255) NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_config_history_config ON infra_config_history (config_id);
