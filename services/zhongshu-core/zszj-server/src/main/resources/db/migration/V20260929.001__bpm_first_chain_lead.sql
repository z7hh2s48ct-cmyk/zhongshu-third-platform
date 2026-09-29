-- ZS-FC-002：首链线索闭环链（PILOT-REQ-005~008 服务端，D-07 M6/M7/M8）
-- 线索表名合同由 ZS-BPM-003 FirstChainStateTransitionExecutor 预留（bpm_first_chain_lead）；
-- 跟进记录为独立子对象（M6，追加式不提供修改——PILOT-REQ-007「无权限用户不能新增或篡改」以不可改结构收敛）；
-- 商机为一期最小对象（M8 五字段，金额一期不建随阶段 3 报价域 D-12 B 类回填；状态机随阶段 3 细化，一期创建即 OPEN 占位）。
-- 不为未来域预建表（接入合同 §3）：报价/合同/回款/客户 360/导入导出一期排除（M9）。

CREATE TABLE bpm_first_chain_lead (
    id                bigserial PRIMARY KEY,
    lead_key          varchar(64)  NOT NULL,
    customer_name     varchar(128),
    customer_phone    varchar(32),
    customer_wechat   varchar(64),
    customer_address  varchar(256),
    source            varchar(64),
    org_id            bigint       NOT NULL,
    assignee_user_id  bigint,
    status            varchar(32)  NOT NULL,
    version           bigint       NOT NULL DEFAULT 0,
    tenant_id         bigint       NOT NULL,
    creator           varchar(64)  DEFAULT '',
    create_time       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)  DEFAULT '',
    update_time       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           boolean      NOT NULL DEFAULT FALSE
);
COMMENT ON TABLE bpm_first_chain_lead IS '首链线索对象表（ZS-FC-002，D-07 M6 五态；客户姓名/来源 F1、手机号/微信号/详细地址 F2 循 D-12 A 类挂级）';
CREATE UNIQUE INDEX uk_bpm_first_chain_lead_key ON bpm_first_chain_lead (tenant_id, lead_key) WHERE deleted = FALSE;
CREATE INDEX idx_bpm_first_chain_lead_01 ON bpm_first_chain_lead (tenant_id, org_id, status) WHERE deleted = FALSE;
CREATE INDEX idx_bpm_first_chain_lead_02 ON bpm_first_chain_lead (tenant_id, assignee_user_id) WHERE deleted = FALSE;

CREATE TABLE bpm_first_chain_followup (
    id               bigserial PRIMARY KEY,
    lead_id          bigint       NOT NULL,
    content          varchar(2048) NOT NULL,
    next_step        varchar(512),
    followup_user_id bigint       NOT NULL,
    followup_time    timestamp    NOT NULL,
    tenant_id        bigint       NOT NULL,
    creator          varchar(64)  DEFAULT '',
    create_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater          varchar(64)  DEFAULT '',
    update_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          boolean      NOT NULL DEFAULT FALSE
);
COMMENT ON TABLE bpm_first_chain_followup IS '首链线索跟进记录表（ZS-FC-002，M6 独立子对象；内容 F1 循 D-12 A 类挂级；追加式无修改通道）';
CREATE INDEX idx_bpm_first_chain_followup_01 ON bpm_first_chain_followup (tenant_id, lead_id, followup_time);

CREATE TABLE bpm_first_chain_opportunity (
    id            bigserial PRIMARY KEY,
    opp_key       varchar(64)  NOT NULL,
    lead_id       bigint       NOT NULL,
    customer_name varchar(128),
    status        varchar(32)  NOT NULL,
    version       bigint       NOT NULL DEFAULT 0,
    tenant_id     bigint       NOT NULL,
    creator       varchar(64)  DEFAULT '',
    create_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)  DEFAULT '',
    update_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       boolean      NOT NULL DEFAULT FALSE
);
COMMENT ON TABLE bpm_first_chain_opportunity IS '首链商机对象表（ZS-FC-002，M7 转商机产物，双向引用线索；金额一期不建）';
CREATE UNIQUE INDEX uk_bpm_first_chain_opp_key ON bpm_first_chain_opportunity (tenant_id, opp_key) WHERE deleted = FALSE;
-- 每线索至多一个有效商机（CONVERTED 终态锁定保证业务上不重复，唯一索引兜底并发）
CREATE UNIQUE INDEX uk_bpm_first_chain_opp_lead ON bpm_first_chain_opportunity (lead_id) WHERE deleted = FALSE;
CREATE INDEX idx_bpm_first_chain_opp_01 ON bpm_first_chain_opportunity (tenant_id, status) WHERE deleted = FALSE;
