-- ZS-MSG-002：业务待办——独立于站内信已读语义的业务任务投影。
-- 稳定任务 ID(todo_key) + 业务来源(source_type) + 通用处理状态(status) + 版本(biz_version) + 失效原因(status_reason)。
-- 由 NotifyTodoService 在业务事务内经 MyBatis-Plus 写入；主键由 DB 生成（SEQUENCE + DEFAULT nextval）。
-- tenant_id 取自 TenantContextHolder（不默认 0，循 MSG-001/JOB-002 先例）。
-- D-07 红线：status 为通用机制级枚举（循 JOB-003 inbox_event status CHECK 机制先例），
--            不含 D-07 试点业务态（加盟商申请/线索状态），业务态映射经 TodoStatusMapper 扩展点。
CREATE SEQUENCE IF NOT EXISTS system_notify_todo_seq START 1;
CREATE TABLE IF NOT EXISTS system_notify_todo (
    id int8 NOT NULL DEFAULT nextval('system_notify_todo_seq'),
    todo_key varchar(128) NOT NULL,
    source_type varchar(32) NOT NULL,
    biz_type varchar(64) NULL,
    biz_id varchar(128) NULL,
    biz_version varchar(64) NULL,
    title varchar(256) NULL,
    message_id int8 NULL,
    recipient_type varchar(16) NOT NULL,
    recipient_id int8 NOT NULL,
    assignee_type varchar(16) NULL,
    assignee_id int8 NULL,
    status varchar(24) NOT NULL,
    status_reason varchar(512) NULL,
    todo_version int8 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL,
    creator varchar(64) DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_notify_todo_key UNIQUE (tenant_id, source_type, todo_key),
    CONSTRAINT ck_notify_todo_status CHECK (status IN ('PENDING','COMPLETED','WITHDRAWN','REASSIGNED','INVALID'))
);
COMMENT ON TABLE system_notify_todo IS '业务待办表（ZS-MSG-002）';
COMMENT ON COLUMN system_notify_todo.todo_key IS '稳定业务任务 ID（幂等键，与 source_type+tenant 唯一）';
COMMENT ON COLUMN system_notify_todo.source_type IS '业务来源：BPM/FILE/GENERIC（机制级，非试点业务态）';
COMMENT ON COLUMN system_notify_todo.biz_version IS '对象版本（乱序护栏依据，Inbox 水位比较）';
COMMENT ON COLUMN system_notify_todo.message_id IS '关联站内信 system_notify_message.id（可空，松耦合）';
COMMENT ON COLUMN system_notify_todo.status IS '通用机制状态：PENDING/COMPLETED/WITHDRAWN/REASSIGNED/INVALID';
COMMENT ON COLUMN system_notify_todo.status_reason IS '失效/状态原因（脱敏，不落敏感原文）';
COMMENT ON COLUMN system_notify_todo.todo_version IS '乐观锁：状态流转递增，防并发覆盖';
CREATE INDEX IF NOT EXISTS idx_notify_todo_recipient ON system_notify_todo (recipient_type, recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_notify_todo_biz ON system_notify_todo (biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_notify_todo_message ON system_notify_todo (message_id);
CREATE INDEX IF NOT EXISTS idx_notify_todo_tenant ON system_notify_todo (tenant_id);
CREATE INDEX IF NOT EXISTS idx_notify_todo_assignee ON system_notify_todo (assignee_type, assignee_id);
