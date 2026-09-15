-- ZS-MSG-001：统一通知发送日志——幂等硬约束 + 明确状态 + 绑定 Outbox 事件。
-- 由 NotifyDispatcher 在业务事务内经 MyBatis-Plus 写入；主键由 DB 生成（SEQUENCE + DEFAULT nextval）。
-- tenant_id 取自 TenantContextHolder（不默认 0，循 JOB-002 outbox_event 先例）；
-- 循 AUDIT-001 audit_event 惯用法：只追加、不改写（无 deleted/updater/update_time 语义，BaseDO 逻辑删除字段保留但不使用）。
-- 对齐 V20260915.001 outbox_event / V20260914.010 audit_event 迁移惯用法：SEQUENCE 主键 + text 承载长文本 + timestamp。

CREATE SEQUENCE IF NOT EXISTS system_notify_send_log_seq START 1;
CREATE TABLE IF NOT EXISTS system_notify_send_log (
    id int8 NOT NULL DEFAULT nextval('system_notify_send_log_seq'),
    event_id varchar(128) NOT NULL,
    recipient_type varchar(16) NOT NULL,
    recipient_id int8 NOT NULL,
    channel varchar(16) NOT NULL,
    template_code varchar(64) NOT NULL,
    template_id int8 NULL,
    biz_type varchar(64) NULL,
    biz_id varchar(128) NULL,
    biz_version varchar(64) NULL,
    status varchar(32) NOT NULL,
    status_reason varchar(512) NULL,
    message_id int8 NULL,
    outbox_event_id int8 NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(64) NULL,
    trace_id varchar(64) NULL,
    tenant_id int8 NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_notify_send_log_idempotent UNIQUE (tenant_id, event_id, recipient_type, recipient_id, channel),
    CONSTRAINT ck_notify_send_log_status CHECK (status IN ('SUCCESS','DISABLED_TEMPLATE','NO_CHANNEL','RECIPIENT_INVALID','RECIPIENT_TENANT_MISMATCH','TEMPLATE_PARAM_MISSING','TEMPLATE_NOT_FOUND','DUPLICATE_IGNORED'))
);
-- 查询路径索引：按事件号 / 收件人 / 业务对象 / 租户 / trace 检索
CREATE INDEX IF NOT EXISTS idx_notify_send_log_event ON system_notify_send_log (event_id);
CREATE INDEX IF NOT EXISTS idx_notify_send_log_recipient ON system_notify_send_log (recipient_type, recipient_id);
CREATE INDEX IF NOT EXISTS idx_notify_send_log_biz ON system_notify_send_log (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_notify_send_log_tenant ON system_notify_send_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_notify_send_log_trace ON system_notify_send_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_notify_send_log_status ON system_notify_send_log (status);
