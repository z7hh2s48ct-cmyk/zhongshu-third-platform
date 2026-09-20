-- ZS-IAM-002（D-09 FND-IAM-002）：组织作为独立业务对象
-- 单表 type + parent_id 自引用树，承载平台/品牌/加盟商/门店/供应商/部门语义；
-- 既有 system_dept 保持不动，DEPARTMENT 类型组织经 ref_dept_id 桥接回填，向后兼容。
-- 版本号 V20260921.001，规避并行 ZS-DB-010（V20260920.001）冲突。

CREATE TABLE system_organization (
    id int8 NOT NULL,
    name varchar(64) NOT NULL,
    code varchar(64) NOT NULL DEFAULT '',
    type int2 NOT NULL,
    parent_id int8 NOT NULL DEFAULT 0,
    ref_dept_id int8 NULL DEFAULT NULL,
    sort int4 NOT NULL DEFAULT 0,
    leader_user_id int8 NULL DEFAULT NULL,
    status int2 NOT NULL DEFAULT 0,
    remark varchar(500) NULL DEFAULT NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_organization ADD CONSTRAINT pk_system_organization PRIMARY KEY (id);

CREATE INDEX idx_system_organization_parent ON system_organization (parent_id);
CREATE INDEX idx_system_organization_type ON system_organization (type);
-- 组织编码租户内唯一（逻辑删除后可重用；空编码不参与，与多行空 code 并存兼容）
CREATE UNIQUE INDEX uk_system_organization_code ON system_organization (code, tenant_id) WHERE deleted = 0 AND code <> '';

COMMENT ON COLUMN system_organization.id IS '组织ID';
COMMENT ON COLUMN system_organization.name IS '组织名称';
COMMENT ON COLUMN system_organization.code IS '组织编码（租户内唯一）';
COMMENT ON COLUMN system_organization.type IS '组织类型（1平台 2品牌 3加盟商 4门店 5供应商 6部门）';
COMMENT ON COLUMN system_organization.parent_id IS '父组织ID（根为0）';
COMMENT ON COLUMN system_organization.ref_dept_id IS '桥接的部门ID（仅DEPARTMENT类型回填system_dept.id）';
COMMENT ON COLUMN system_organization.sort IS '显示顺序';
COMMENT ON COLUMN system_organization.leader_user_id IS '负责人用户ID';
COMMENT ON COLUMN system_organization.status IS '组织状态（0正常 1停用）';
COMMENT ON COLUMN system_organization.remark IS '备注';
COMMENT ON COLUMN system_organization.creator IS '创建者';
COMMENT ON COLUMN system_organization.create_time IS '创建时间';
COMMENT ON COLUMN system_organization.updater IS '更新者';
COMMENT ON COLUMN system_organization.update_time IS '更新时间';
COMMENT ON COLUMN system_organization.deleted IS '是否删除';
COMMENT ON COLUMN system_organization.tenant_id IS '租户编号';
COMMENT ON TABLE system_organization IS '组织表（ZS-IAM-002）';

CREATE SEQUENCE system_organization_seq START 100000;

-- 历史部门回填：复用 dept.id 作为 DEPARTMENT 类型组织 id，parent_id 直接沿用 dept 树，ref_dept_id 桥接回 system_dept。
-- 序列 START 100000 远高于既有 dept 占用，保证回填 id 与后续新建 id 不撞（若撞则 PK 冲突 fail-loudly）。
INSERT INTO system_organization (id, name, code, type, parent_id, ref_dept_id, sort, leader_user_id, status, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT id, name, '', 6, parent_id, id, sort, leader_user_id, status, NULL, creator, create_time, updater, update_time, deleted, tenant_id
FROM system_dept;
