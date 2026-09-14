-- ZS-FILE-003：预签名直传凭证——绑定主体/租户/临时键/大小/类型/有效期，一次性完成确认。
CREATE SEQUENCE IF NOT EXISTS infra_file_upload_credential_seq START 1;
CREATE TABLE IF NOT EXISTS infra_file_upload_credential (
    id int8 NOT NULL,
    credential_token varchar(64) NOT NULL,
    config_id int8 NOT NULL,
    tenant_id int8 NOT NULL DEFAULT 0,
    owner_user_id int8 NOT NULL DEFAULT 0,
    purpose varchar(64) NOT NULL DEFAULT '',
    temp_path varchar(512) NOT NULL,
    file_name varchar(256) NOT NULL,
    content_type varchar(128) NOT NULL,
    declared_size int8 NOT NULL,
    scope varchar(16) NOT NULL DEFAULT 'PRIVATE',
    status varchar(16) NOT NULL DEFAULT 'WAITING_UPLOAD',
    expires_time timestamp NOT NULL,
    completed_time timestamp NULL,
    file_id int8 NULL,
    creator varchar(64) DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_infra_file_upload_credential_token ON infra_file_upload_credential (credential_token);
CREATE INDEX IF NOT EXISTS idx_infra_file_upload_credential_01 ON infra_file_upload_credential (tenant_id);
