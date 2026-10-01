-- ZS-FC-003 后续优化 wave：分配/改派员工选择器——本组织任职成员列表端点的授权锚点（firstchain:employee:list）。
-- 编号合同承接 cn.zszj.module.firstchain.framework.FirstchainMenus（5324，员工账号菜单 5303 之下）。
-- 同时为既有加盟商负责人模板角色补绑（幂等，仅 leader——员工无需选择器权限）。

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5324, '组织成员列表', 'firstchain:employee:list', 3, 2, 5303, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, 5324, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, r.tenant_id
FROM system_role r
WHERE r.code = 'firstchain:franchisee:leader' AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm
                  WHERE rm.role_id = r.id AND rm.menu_id = 5324 AND rm.deleted = 0);
