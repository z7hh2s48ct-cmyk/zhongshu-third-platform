-- ZS-BPM-003：首链领域状态与幂等写回（D-07 M1/M3/M8/M10）
-- 加盟商申请对象（M3 三态 DRAFT→SUBMITTED→APPROVED/REJECTED；M8 字段）+ 审批流程实例绑定（幂等命令门）

CREATE TABLE bpm_first_chain_application (
    id            bigserial PRIMARY KEY,
    app_key       varchar(64)  NOT NULL,
    applicant_name varchar(128) NOT NULL,
    contact_name  varchar(64),
    contact_phone varchar(32),
    attachment_file_ids varchar(512),
    reject_reason varchar(1024),
    status        varchar(32)  NOT NULL,
    version       bigint       NOT NULL DEFAULT 0,
    tenant_id     bigint       NOT NULL,
    creator       varchar(64)  DEFAULT '',
    create_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)  DEFAULT '',
    update_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       boolean      NOT NULL DEFAULT FALSE
);
COMMENT ON TABLE bpm_first_chain_application IS '首链加盟商申请对象表（ZS-BPM-003，D-07 M3 三态）';
CREATE UNIQUE INDEX uk_bpm_first_chain_application_key ON bpm_first_chain_application (tenant_id, app_key) WHERE deleted = FALSE;
CREATE INDEX idx_bpm_first_chain_application_01 ON bpm_first_chain_application (tenant_id, status) WHERE deleted = FALSE;

CREATE TABLE bpm_first_chain_process_binding (
    id                  bigserial PRIMARY KEY,
    domain_type         varchar(32) NOT NULL,
    domain_id           bigint      NOT NULL,
    app_key             varchar(64) NOT NULL,
    process_instance_id varchar(64) NOT NULL,
    outcome             varchar(32),
    status              varchar(24) NOT NULL,
    tenant_id           bigint      NOT NULL,
    creator             varchar(64) DEFAULT '',
    create_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64) DEFAULT '',
    update_time         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE bpm_first_chain_process_binding IS '首链审批流程实例绑定表（ZS-BPM-003，幂等命令门）';
CREATE UNIQUE INDEX uk_bpm_first_chain_binding_pinst ON bpm_first_chain_process_binding (process_instance_id);
-- 每领域对象至多一个活跃（BOUND）绑定（部分唯一索引兜底，事务内守卫为主）
CREATE UNIQUE INDEX uk_bpm_first_chain_binding_active ON bpm_first_chain_process_binding (tenant_id, domain_type, domain_id) WHERE status = 'BOUND';
