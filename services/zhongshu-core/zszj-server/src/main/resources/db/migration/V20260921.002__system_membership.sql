-- ZS-IAM-002（D-09 最小模型 / FND-IAM-001、003）：账号-组织任职关系与历史流水
-- 一个账号可含多个任职，每任职绑定组织、岗位、角色、状态与有效期；每账号至多一条默认任职。
-- 版本号 V20260921.002，紧接组织表 V20260921.001，规避并行 ZS-DB-010（V20260920.001）冲突。

CREATE TABLE system_membership (
    id int8 NOT NULL,
    user_id int8 NOT NULL,
    organization_id int8 NOT NULL,
    post_ids varchar(255) NULL DEFAULT NULL,
    role_ids text NULL DEFAULT NULL,
    status int2 NOT NULL DEFAULT 1,
    valid_from timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to timestamp NULL DEFAULT NULL,
    is_primary int2 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_membership ADD CONSTRAINT pk_system_membership PRIMARY KEY (id);

CREATE INDEX idx_system_membership_user ON system_membership (user_id);
CREATE INDEX idx_system_membership_org ON system_membership (organization_id);
-- 每账号至多一个默认任职（FND-IAM-001 默认身份唯一性）
CREATE UNIQUE INDEX uk_system_membership_primary ON system_membership (user_id) WHERE deleted = 0 AND is_primary = 1;
-- 同账号同组织在职/停用去重（逻辑删除后可重建；过期/离职不占位，允许再入职）
CREATE UNIQUE INDEX uk_system_membership_user_org ON system_membership (user_id, organization_id) WHERE deleted = 0 AND status IN (1, 2);

COMMENT ON COLUMN system_membership.id IS '任职ID';
COMMENT ON COLUMN system_membership.user_id IS '账号（用户）ID';
COMMENT ON COLUMN system_membership.organization_id IS '组织ID';
COMMENT ON COLUMN system_membership.post_ids IS '岗位编号集合（JSON，沿用 system_users.post_ids 有界存储）';
COMMENT ON COLUMN system_membership.role_ids IS '角色编号集合（JSON，text 无界：历史回填聚合 system_user_role 无每账号上限，避免超 255 字符撑爆 INSERT 中止 Flyway）';
COMMENT ON COLUMN system_membership.status IS '任职状态（1在职 2停用 3过期 4离职）';
COMMENT ON COLUMN system_membership.valid_from IS '生效时间';
COMMENT ON COLUMN system_membership.valid_to IS '失效时间（NULL=无固定期限）';
COMMENT ON COLUMN system_membership.is_primary IS '是否默认任职（1是 0否）';
COMMENT ON COLUMN system_membership.creator IS '创建者';
COMMENT ON COLUMN system_membership.create_time IS '创建时间';
COMMENT ON COLUMN system_membership.updater IS '更新者';
COMMENT ON COLUMN system_membership.update_time IS '更新时间';
COMMENT ON COLUMN system_membership.deleted IS '是否删除';
COMMENT ON COLUMN system_membership.tenant_id IS '租户编号';
COMMENT ON TABLE system_membership IS '任职表（ZS-IAM-002）';

CREATE SEQUENCE system_membership_seq START 100000;

CREATE TABLE system_membership_history (
    id int8 NOT NULL,
    membership_id int8 NOT NULL,
    user_id int8 NOT NULL,
    action int2 NOT NULL,
    from_organization_id int8 NULL DEFAULT NULL,
    to_organization_id int8 NULL DEFAULT NULL,
    from_status int2 NULL DEFAULT NULL,
    to_status int2 NULL DEFAULT NULL,
    operator_id int8 NULL DEFAULT NULL,
    reason varchar(500) NULL DEFAULT NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_membership_history ADD CONSTRAINT pk_system_membership_history PRIMARY KEY (id);

CREATE INDEX idx_system_membership_history_mid ON system_membership_history (membership_id);
CREATE INDEX idx_system_membership_history_user ON system_membership_history (user_id);

COMMENT ON COLUMN system_membership_history.id IS '历史流水ID';
COMMENT ON COLUMN system_membership_history.membership_id IS '任职ID';
COMMENT ON COLUMN system_membership_history.user_id IS '账号（用户）ID';
COMMENT ON COLUMN system_membership_history.action IS '动作（1入职 2转岗 3停用 4复职 5离职 6过期 7变更）';
COMMENT ON COLUMN system_membership_history.from_organization_id IS '变更前组织ID';
COMMENT ON COLUMN system_membership_history.to_organization_id IS '变更后组织ID';
COMMENT ON COLUMN system_membership_history.from_status IS '变更前状态';
COMMENT ON COLUMN system_membership_history.to_status IS '变更后状态';
COMMENT ON COLUMN system_membership_history.operator_id IS '操作者用户ID';
COMMENT ON COLUMN system_membership_history.reason IS '变更原因';
COMMENT ON COLUMN system_membership_history.creator IS '创建者';
COMMENT ON COLUMN system_membership_history.create_time IS '创建时间';
COMMENT ON COLUMN system_membership_history.updater IS '更新者';
COMMENT ON COLUMN system_membership_history.update_time IS '更新时间';
COMMENT ON COLUMN system_membership_history.deleted IS '是否删除';
COMMENT ON COLUMN system_membership_history.tenant_id IS '租户编号';
COMMENT ON TABLE system_membership_history IS '任职历史流水表（ZS-IAM-002）';

CREATE SEQUENCE system_membership_history_seq START 100000;

-- 历史技术账号回填（不误合并）：以 user_id 逐一驱动，一用户一条 primary ACTIVE 任职，
-- 绝不按 username/mobile 聚合（那是 ZS-DB-010 的唯一性职责，本迁移不触碰）。
-- 组织映射：dept_id 已在 V20260921.001 回填为同 id 的 DEPARTMENT 组织，故 organization_id = dept_id。
-- post_ids 沿用 system_users.post_ids（已是 JSON）；role_ids 由 system_user_role 聚合为 JSON 数组串（无角色则 NULL）。
INSERT INTO system_membership (id, user_id, organization_id, post_ids, role_ids, status, valid_from, valid_to, is_primary, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_membership_seq'), u.id, u.dept_id, u.post_ids,
       (SELECT '[' || string_agg(r.role_id::text, ',' ORDER BY r.role_id) || ']'
          FROM system_user_role r WHERE r.user_id = u.id AND r.deleted = 0),
       1, u.create_time, NULL, 1, u.creator, u.create_time, u.updater, u.update_time, 0, u.tenant_id
FROM system_users u
WHERE u.deleted = 0 AND u.dept_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM system_organization o WHERE o.id = u.dept_id AND o.deleted = 0);

-- 为回填的任职补一条入职历史流水（承载历史归属起点）
INSERT INTO system_membership_history (id, membership_id, user_id, action, from_organization_id, to_organization_id, from_status, to_status, operator_id, reason, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_membership_history_seq'), m.id, m.user_id, 1, NULL, m.organization_id, NULL, m.status, NULL, '历史账号迁移回填', m.creator, m.create_time, m.updater, m.update_time, 0, m.tenant_id
FROM system_membership m
WHERE m.deleted = 0;
