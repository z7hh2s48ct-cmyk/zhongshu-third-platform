-- ZS-JOB-002：事务 Outbox 与可恢复投递——outbox_event + dispatcher_lease（独立版本迁移，仅迁入通用机制）。
-- 写入路径：业务模块经 ReliableEventPort（JdbcReliableEventPort）在业务事务内追加，随业务提交/回滚；
-- 投递路径：OutboxDispatcherService 以独立短事务领取（FOR UPDATE SKIP LOCKED + 事件级租约 + 每次领取唯一凭证）。
-- 对齐 V20260914.010 audit_event 迁移惯用法：SEQUENCE 主键 + DEFAULT nextval、text 承载 JSON（不用 jsonb）、
-- timestamp（时间比较全部由应用参数化传入，单一时钟源）、无 BaseDO 逻辑删除语义（状态机推进，不删改历史事实）。

CREATE SEQUENCE IF NOT EXISTS outbox_event_seq START 1;
CREATE TABLE IF NOT EXISTS outbox_event (
    id int8 NOT NULL DEFAULT nextval('outbox_event_seq'),
    event_type varchar(128) NOT NULL,
    biz_type varchar(64) NULL,
    biz_id varchar(128) NULL,
    biz_version varchar(64) NULL,
    payload text NOT NULL,
    headers text NULL,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    retry_count int4 NOT NULL DEFAULT 0,
    next_retry_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_by varchar(128) NULL,
    claim_token varchar(64) NULL,
    claim_expires_at timestamp NULL,
    last_error text NULL,
    dispatched_at timestamp NULL,
    tenant_id int8 NOT NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(64) NULL,
    trace_id varchar(64) NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'DISPATCHED', 'DEAD')),
    CONSTRAINT ck_outbox_event_retry_count CHECK (retry_count >= 0)
);
-- 领取路径索引：PENDING + 到期时间 + 稳定排序（SKIP LOCKED 跳过被锁行后按 id 有界领取）
CREATE INDEX IF NOT EXISTS idx_outbox_event_claim ON outbox_event (status, next_retry_at, id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_biz ON outbox_event (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_tenant ON outbox_event (tenant_id);
CREATE INDEX IF NOT EXISTS idx_outbox_event_trace ON outbox_event (trace_id);

-- dispatcher 实例租约登记（可观测；事件级抢占由 outbox_event.claim_* 列承担，不依赖本表做互斥）
CREATE SEQUENCE IF NOT EXISTS dispatcher_lease_seq START 1;
CREATE TABLE IF NOT EXISTS dispatcher_lease (
    id int8 NOT NULL DEFAULT nextval('dispatcher_lease_seq'),
    dispatcher_name varchar(64) NOT NULL,
    instance_id varchar(64) NOT NULL,
    lease_expires_at timestamp NOT NULL,
    heartbeat_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_dispatcher_lease_instance UNIQUE (dispatcher_name, instance_id)
);
