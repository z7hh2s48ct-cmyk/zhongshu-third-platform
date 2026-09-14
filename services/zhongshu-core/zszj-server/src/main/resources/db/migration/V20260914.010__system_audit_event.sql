-- ZS-AUDIT-001：统一业务审计事件表——不可改写的业务审计历史（区别于通用操作日志 system_operate_log）。
-- 由 JdbcAuditPort 经 JdbcTemplate 同步写入业务数据源；主键由 DB 生成（SEQUENCE + DEFAULT nextval，与本项目迁移惯用法一致），
-- 故 JdbcTemplate 可用 GeneratedKeyHolder 取回事件 ID。tenant_id 可空且不默认 0（D-09 技术租户扩展点，不伪造归属）；
-- 无 deleted/updater/update_time（审计只追加、不改写，故不套用 BaseDO 逻辑删除语义）。
CREATE SEQUENCE IF NOT EXISTS audit_event_seq START 1;
CREATE TABLE IF NOT EXISTS audit_event (
    id int8 NOT NULL DEFAULT nextval('audit_event_seq'),
    event_type varchar(128) NOT NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(64) NULL,
    action varchar(64) NULL,
    biz_type varchar(64) NULL,
    biz_id varchar(128) NULL,
    biz_version varchar(64) NULL,
    reason varchar(512) NULL,
    result varchar(16) NOT NULL,
    detail text NULL,
    tenant_id int8 NULL,
    trace_id varchar(64) NULL,
    idempotency_key varchar(128) NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
-- 幂等键唯一（NULL 视为互异，允许多条无键事件）：携带同一键的重复事件不重复入账的 DB 硬兜底。
CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_event_idempotency ON audit_event (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_audit_event_biz ON audit_event (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_trace ON audit_event (trace_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_tenant ON audit_event (tenant_id);
