-- ZS-FC-003 风险排查 R4 闭合：system_role (tenant_id, code) 部分唯一索引——默认角色模板跨实例互斥的数据库级兜底。
-- 基线角色仅 super_admin/common@tenant1，无 (tenant, code) 重复（已核对）；逻辑删除行不参与唯一性（循
-- V20260920.001 system_users 部分唯一索引先例）。firstchain 默认角色模板 create-or-reuse 的跨实例并发双开
-- 由 FirstchainDefaultRoleRegistry 的 DuplicateKey 复查捕获（先到者生效）——本索引使该路径真实可达。
-- 谓词写 `deleted = 0`（基线 int2 形态，PG 无 smallint = boolean 算符）。

CREATE UNIQUE INDEX uk_system_role_tenant_code ON system_role (tenant_id, code) WHERE deleted = 0;

COMMENT ON INDEX uk_system_role_tenant_code IS '角色编码租户内唯一（逻辑删除后可重用；ZS-FC-003 R4：默认授权模板跨实例互斥兜底）';
