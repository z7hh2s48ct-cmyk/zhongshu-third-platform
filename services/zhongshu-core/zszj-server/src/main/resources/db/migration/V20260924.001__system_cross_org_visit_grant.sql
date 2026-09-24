-- ZS-SEC-001.B（D-09 / docs/05 line330-331）：获批跨组织访问授权记录
-- 以「服务端授权记录/策略」取代 ZS-SEC-001.A 关闭的旧越权放大链（持 system:tenant:visit 即整体跳过功能权限）：
-- 逐条限定目标租户、对象（组织）、动作、字段与有效期，判定期实时校验平台角色资格与目标状态。
-- 平台级跨租户表（无 tenant_id 隔离列，循 system_tenant 范式；visitor/target 租户为业务列），版本号 V20260924.001。

CREATE TABLE system_cross_org_visit_grant (
    id int8 NOT NULL,
    visitor_user_id int8 NOT NULL,
    visitor_tenant_id int8 NULL DEFAULT NULL,
    target_tenant_id int8 NOT NULL,
    target_org_ids varchar(1024) NULL DEFAULT NULL,
    allowed_actions text NULL DEFAULT NULL,
    allowed_fields text NULL DEFAULT NULL,
    valid_from timestamp NULL DEFAULT NULL,
    valid_to timestamp NULL DEFAULT NULL,
    status int2 NOT NULL DEFAULT 0,
    reason varchar(500) NULL DEFAULT NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE system_cross_org_visit_grant ADD CONSTRAINT pk_system_cross_org_visit_grant PRIMARY KEY (id);

-- 判定序按 (原主体, 目标租户) 取最新授权记录
CREATE INDEX idx_system_cross_org_visit_grant_visitor_target ON system_cross_org_visit_grant (visitor_user_id, target_tenant_id);

COMMENT ON COLUMN system_cross_org_visit_grant.id IS '授权记录ID';
COMMENT ON COLUMN system_cross_org_visit_grant.visitor_user_id IS '原主体：发起跨组织访问的账号（用户）ID';
COMMENT ON COLUMN system_cross_org_visit_grant.visitor_tenant_id IS '原主体所属（home）技术租户ID';
COMMENT ON COLUMN system_cross_org_visit_grant.target_tenant_id IS '目标租户ID';
COMMENT ON COLUMN system_cross_org_visit_grant.target_org_ids IS '授权可见目标组织ID集合（逗号分隔；NULL=目标租户内全部组织）';
COMMENT ON COLUMN system_cross_org_visit_grant.allowed_actions IS '授权允许动作（权限标识）集合（逗号分隔；空/NULL=fail-closed 不允许任何动作）';
COMMENT ON COLUMN system_cross_org_visit_grant.allowed_fields IS '授权允许访问字段集合（逗号分隔；NULL=不允许任何显式敏感字段。字段目录归 ZS-PERM-003）';
COMMENT ON COLUMN system_cross_org_visit_grant.valid_from IS '生效时间';
COMMENT ON COLUMN system_cross_org_visit_grant.valid_to IS '失效时间（NULL=无固定期限）';
COMMENT ON COLUMN system_cross_org_visit_grant.status IS '授权状态（0生效 1已撤销）';
COMMENT ON COLUMN system_cross_org_visit_grant.reason IS '授权/撤销理由（不含敏感明文）';
COMMENT ON COLUMN system_cross_org_visit_grant.creator IS '创建者';
COMMENT ON COLUMN system_cross_org_visit_grant.create_time IS '创建时间';
COMMENT ON COLUMN system_cross_org_visit_grant.updater IS '更新者';
COMMENT ON COLUMN system_cross_org_visit_grant.update_time IS '更新时间';
COMMENT ON COLUMN system_cross_org_visit_grant.deleted IS '是否删除';
COMMENT ON TABLE system_cross_org_visit_grant IS '跨组织访问授权记录表（ZS-SEC-001.B）';

CREATE SEQUENCE system_cross_org_visit_grant_seq START 100000;
