-- ZS-MSG-004：渠道发送状态、失败重试与回执对账——渠道发送生命周期台账。
-- 由 NotifyChannelSendService 在派发事务内创建（PENDING），经 Outbox 事件 NOTIFY_CHANNEL_SEND 驱动提交；
-- 渠道侧状态（待发送/已受理/送达/失败/结果未知）独立于站内消息已读状态与派发日志状态（众墅要求「分别建模」）。
-- 循 V20260915.003 system_notify_send_log / V20260915.001 outbox_event 惯用法：SEQUENCE 主键 + text 承载长文本 + timestamp；
-- tenant_id 取自 TenantContextHolder（不默认 0）；attempt_count/receipt_count 只增不减（可追溯）。

CREATE SEQUENCE IF NOT EXISTS system_notify_channel_send_seq START 1;
CREATE TABLE IF NOT EXISTS system_notify_channel_send (
    id int8 NOT NULL DEFAULT nextval('system_notify_channel_send_seq'),
    tenant_id int8 NOT NULL,
    send_log_id int8 NOT NULL,
    event_id varchar(128) NOT NULL,
    channel varchar(16) NOT NULL,
    recipient_type varchar(16) NOT NULL,
    recipient_id int8 NOT NULL,
    recipient_contact varchar(256) NULL,
    template_code varchar(64) NOT NULL,
    content text NULL,
    channel_message_id varchar(64) NOT NULL,
    status varchar(16) NOT NULL,
    status_reason varchar(512) NULL,
    channel_serial_no varchar(128) NULL,
    failed_code varchar(64) NULL,
    attempt_count int4 NOT NULL DEFAULT 0,
    receipt_count int4 NOT NULL DEFAULT 0,
    manual_retry_count int4 NOT NULL DEFAULT 0,
    accepted_at timestamp NULL,
    delivered_at timestamp NULL,
    last_receipt_at timestamp NULL,
    last_query_at timestamp NULL,
    last_query_result varchar(32) NULL,
    last_error varchar(256) NULL,
    last_retry_actor_type varchar(16) NULL,
    last_retry_actor_id varchar(64) NULL,
    last_retry_reason varchar(512) NULL,
    outbox_event_id int8 NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(64) NULL,
    trace_id varchar(64) NULL,
    creator varchar(64) DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_notify_channel_send_log UNIQUE (tenant_id, send_log_id),
    CONSTRAINT uk_notify_channel_send_receipt UNIQUE (tenant_id, channel, channel_message_id),
    CONSTRAINT ck_notify_channel_send_status CHECK (status IN ('PENDING','ACCEPTED','DELIVERED','FAILED','UNKNOWN')),
    CONSTRAINT ck_notify_channel_send_channel CHECK (channel IN ('SMS','EMAIL','PUSH'))
);
-- 查询路径索引：按状态台账（重试/回执对账扫描）/ 事件号 / 发送日志 / 租户
CREATE INDEX IF NOT EXISTS idx_notify_channel_send_status ON system_notify_channel_send (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_notify_channel_send_event ON system_notify_channel_send (event_id);
CREATE INDEX IF NOT EXISTS idx_notify_channel_send_send_log ON system_notify_channel_send (send_log_id);
CREATE INDEX IF NOT EXISTS idx_notify_channel_send_tenant ON system_notify_channel_send (tenant_id);
-- ZS-MSG-004 r1：事件身份定位（selectByOutboxEventId 为每次 Outbox 投递必经查询，需覆盖索引防台账线性退化）
CREATE INDEX IF NOT EXISTS idx_notify_channel_send_outbox ON system_notify_channel_send (tenant_id, outbox_event_id);

-- ZS-MSG-004：派发状态新增 CHANNEL_NOT_CONFIGURED（未配置渠道明确阻断，不静默丢弃——众墅要求
-- 「未配置渠道明确阻断或保留待发送」；与「未指定渠道」的 NO_CHANNEL 语义区分）。
ALTER TABLE system_notify_send_log DROP CONSTRAINT ck_notify_send_log_status;
ALTER TABLE system_notify_send_log ADD CONSTRAINT ck_notify_send_log_status
    CHECK (status IN ('SUCCESS','DISABLED_TEMPLATE','NO_CHANNEL','RECIPIENT_INVALID','RECIPIENT_TENANT_MISMATCH','TEMPLATE_PARAM_MISSING','TEMPLATE_NOT_FOUND','DUPLICATE_IGNORED','CHANNEL_NOT_CONFIGURED'));
