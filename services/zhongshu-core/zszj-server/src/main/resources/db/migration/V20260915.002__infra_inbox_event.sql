-- ZS-JOB-003：消费者业务幂等（Inbox）——处理键唯一抢占 + 参数指纹冲突检测 + 版本乱序护栏 + 可回查中间态。
-- 写入路径：消费者业务代码在业务事务内经 ConsumerInboxPort.tryBegin 抢位（幂等记录与业务副作用同事务，
-- 业务回滚则抢位一并回滚）；处理结果经 complete/fail/markResultUnknown 同事务推进。
-- 对齐 V20260915.001 outbox_event 迁移惯用法：SEQUENCE 主键、text 承载 JSON/错误描述、timestamp、
-- 无 BaseDO 逻辑删除语义（状态机推进）。

CREATE SEQUENCE IF NOT EXISTS inbox_event_seq START 1;
CREATE TABLE IF NOT EXISTS inbox_event (
    id int8 NOT NULL DEFAULT nextval('inbox_event_seq'),
    consumer varchar(64) NOT NULL,
    event_key varchar(128) NOT NULL,
    payload_hash varchar(64) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'PROCESSING',
    result text NULL,
    retry_count int4 NOT NULL DEFAULT 0,
    biz_type varchar(64) NULL,
    biz_id varchar(128) NULL,
    biz_version varchar(64) NULL,
    last_error text NULL,
    tenant_id int8 NOT NULL,
    trace_id varchar(64) NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time timestamp NULL,
    PRIMARY KEY (id),
    -- 处理键：技术租户 + 消费者 + 事件/业务命令键——同事件并发、ACK 丢失、重启重放在此 DB 硬兜底
    CONSTRAINT uk_inbox_event_key UNIQUE (tenant_id, consumer, event_key),
    CONSTRAINT ck_inbox_event_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'RESULT_UNKNOWN')),
    CONSTRAINT ck_inbox_event_retry_count CHECK (retry_count >= 0)
);
CREATE INDEX IF NOT EXISTS idx_inbox_event_tenant_consumer_status ON inbox_event (tenant_id, consumer, status, id);
CREATE INDEX IF NOT EXISTS idx_inbox_event_biz ON inbox_event (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_inbox_event_tenant ON inbox_event (tenant_id);

-- 对象版本水位表（codex r1：版本乱序护栏的稳定串行化点）——以 (tenant, consumer, biz_type, biz_id) 为唯一键，
-- version_watermark 为单调已应用版本水位；tryBegin 过护栏时 INSERT-or-LOCK 该行（行锁持至业务事务提交，
-- 新旧版本事件在此互斥），通过后同事务抬水位——副作用与水位同生共死，杜绝无锁快照反超与首次处理无锁场景。
CREATE SEQUENCE IF NOT EXISTS inbox_object_watermark_seq START 1;
CREATE TABLE IF NOT EXISTS inbox_object_watermark (
    id int8 NOT NULL DEFAULT nextval('inbox_object_watermark_seq'),
    tenant_id int8 NOT NULL,
    consumer varchar(64) NOT NULL,
    biz_type varchar(64) NOT NULL,
    biz_id varchar(128) NOT NULL,
    version_watermark int8 NOT NULL DEFAULT 0,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inbox_object_watermark UNIQUE (tenant_id, consumer, biz_type, biz_id)
);
