-- ZS-SEC-011.B：持久化幂等记录表（丢响应可复用原结果 / 并发唯一约束兜底 / 重启不重复写；非 persistent 注解仍走 Redis 窗口锁）。
-- 写入路径：切面 PersistentIdempotentAspect 经 PersistentIdempotentStore SPI（zszj-module-infra JdbcPersistentIdempotentStore 落地）
--          在业务事务内 INSERT RUNNING → 业务 → markSuccess 同事务提交（MANDATORY 参与，业务回滚则记录一并回滚）。
-- 并发裁决：idempotent_key 唯一约束单层兜底（抢占用 INSERT ... ON CONFLICT DO NOTHING，败者事务不 aborted、可读回记录分类）。
-- 对齐 V20260915.001 outbox_event 迁移惯用法：SEQUENCE 主键 + DEFAULT nextval、text 承载 JSON 快照（不用 jsonb）、
--          complete_time 由应用参数化写入（单一时钟源）、无 BaseDO 逻辑删除语义（状态机推进 + 失败删记录，不删改历史事实）。

CREATE SEQUENCE IF NOT EXISTS infra_persistent_idempotent_seq START 1;
CREATE TABLE IF NOT EXISTS infra_persistent_idempotent (
    id int8 NOT NULL DEFAULT nextval('infra_persistent_idempotent_seq'),
    idempotent_key varchar(64) NOT NULL,
    tenant_id int8 NULL,
    subject_type varchar(32) NULL,
    subject_id varchar(64) NULL,
    -- codex r1 P2-B：Method.toString() 含全限定返回/参数类型（两个长参数类型即可达 262+ 字符），256 会 22001 溢出——
    -- 列宽放宽至 1024；极端超长由切面 boundActionScope 降级为「sha256:<64hex>」定长表示（落库/重放同口径，无截断碰撞）
    action_scope varchar(1024) NOT NULL,
    request_digest varchar(64) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'RUNNING',
    result_snapshot text NULL,
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time timestamp NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_persistent_idempotent_key UNIQUE (idempotent_key),
    CONSTRAINT ck_persistent_idempotent_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);
COMMENT ON TABLE infra_persistent_idempotent IS '持久化幂等记录表（ZS-SEC-011.B）';
-- 重放读路径索引：按幂等键点查（唯一约束已覆盖）；create_time 索引供后续保留期清理（本卡不建清理任务）
CREATE INDEX IF NOT EXISTS idx_persistent_idempotent_create_time ON infra_persistent_idempotent (create_time);
