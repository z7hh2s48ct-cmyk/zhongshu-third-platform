-- ZS-JOB-004：Outbox 人工恢复台账 + DEAD 跳过终态（承接 V20260915.001 outbox_event / .020 infra job tenant result）。
-- 循 V20260914.010 audit_event / V20260915.001 outbox_event 惯用法：SEQUENCE 主键 + DEFAULT nextval、
-- timestamp（时间比较由应用参数化传入，单一时钟源）、无 BaseDO 逻辑删除（台账为不可变事实，只增不改）。
-- 铁律（开发计划 §1.2-9）：人工恢复动作永不改 outbox_event.payload/headers/event_type（历史事实不可掩盖）。

-- (1) outbox_event.status 扩展 SKIPPED（人工放弃终态；与 DISPATCHED 成功 / DEAD 自动失败上限并列，机制级状态非业务态）
ALTER TABLE outbox_event DROP CONSTRAINT ck_outbox_event_status;
ALTER TABLE outbox_event ADD CONSTRAINT ck_outbox_event_status
    CHECK (status IN ('PENDING', 'DISPATCHED', 'DEAD', 'SKIPPED'));

-- (2) 恢复台账：每次人工 retry/skip 一行，保留恢复前后关联（before/after status、before_retry_count、manual_retry_seq）
CREATE SEQUENCE IF NOT EXISTS outbox_recovery_log_seq START 1;
CREATE TABLE IF NOT EXISTS outbox_recovery_log (
    id int8 NOT NULL DEFAULT nextval('outbox_recovery_log_seq'),
    event_id int8 NOT NULL,                    -- 关联 outbox_event.id
    action varchar(16) NOT NULL,               -- RETRY / SKIP
    reason varchar(512) NOT NULL,              -- 人工理由（必填，脱敏，不落敏感原文）
    before_status varchar(16) NOT NULL,        -- 恢复前状态（DEAD）
    after_status varchar(16) NOT NULL,         -- 恢复后状态（PENDING / SKIPPED）
    before_retry_count int4 NOT NULL,          -- 恢复前自动重试计数（前后关联）
    manual_retry_seq int4 NOT NULL DEFAULT 1,  -- 本事件第几次人工重试（无限重试护栏依据）
    operator_type varchar(16) NOT NULL,        -- ADMIN / SYSTEM
    operator_id varchar(64) NULL,
    tenant_id int8 NOT NULL,
    trace_id varchar(64) NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_outbox_recovery_action CHECK (action IN ('RETRY', 'SKIP')),
    CONSTRAINT ck_outbox_recovery_reason CHECK (char_length(reason) > 0)
);
-- 回查历史（按事件 + 稳定 id 序）与租户过滤
CREATE INDEX IF NOT EXISTS idx_outbox_recovery_event ON outbox_recovery_log (event_id, id);
CREATE INDEX IF NOT EXISTS idx_outbox_recovery_tenant ON outbox_recovery_log (tenant_id);
