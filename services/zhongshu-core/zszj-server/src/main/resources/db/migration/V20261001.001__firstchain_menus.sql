-- ZS-FC-001 默认授权 wave：首链业务菜单种子（接入合同 §1.1 第⑤步「默认角色绑菜单」落点）。
-- 编号合同唯一事实源：cn.zszj.module.firstchain.framework.FirstchainMenus（5300~5329 firstchain 专用段，
-- 基线最大菜单 ID 5010）——迁移与 FirstchainDefaultRoleRegistry 两侧不得漂移。
-- 员工账号/线索工作台页面组件（views/firstchain/**）随 ZS-FC-003 工作台交付，本期先落菜单与授权锚点。

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5300, '首链业务', '', 1, 90, 0, 'firstchain', 'ep:connection', NULL, NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5301, '加盟商申请', 'firstchain:application:query', 2, 1, 5300, 'application', 'ep:stamp', 'firstchain/application/index', 'FirstchainApplication', 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5302, '线索管理', 'firstchain:lead:query', 2, 2, 5300, 'lead', 'ep:guide', 'firstchain/lead/index', 'FirstchainLead', 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5303, '员工账号', '', 2, 3, 5300, 'employee', 'ep:user', 'firstchain/employee/index', 'FirstchainEmployee', 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5311, '创建申请', 'firstchain:application:create', 3, 1, 5301, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5312, '提交申请', 'firstchain:application:submit', 3, 2, 5301, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5313, '审批通过', 'firstchain:application:approve', 3, 3, 5301, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5314, '审批拒绝', 'firstchain:application:reject', 3, 4, 5301, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5315, '撤回审批', 'firstchain:application:withdraw', 3, 5, 5301, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5316, '下发线索', 'firstchain:lead:distribute', 3, 1, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5317, '分配线索', 'firstchain:lead:assign', 3, 2, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5318, '领取线索', 'firstchain:lead:claim', 3, 3, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5319, '改派线索', 'firstchain:lead:reassign', 3, 4, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5320, '跟进线索', 'firstchain:lead:followup', 3, 5, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5321, '转商机', 'firstchain:lead:convert', 3, 6, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5322, '无效关闭', 'firstchain:lead:invalidate', 3, 7, 5302, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
  (5323, '创建员工账号', 'firstchain:employee:create', 3, 1, 5303, '', '', '', NULL, 0, '1', '1', '1', '1', NOW(), '1', NOW(), 0);

-- 修复本迁移前已开通租户的既有默认模板角色（菜单空集登记期产物）：按 FirstchainMenus 两套菜单面补绑，
-- 幂等（已存在绑定不重复插入），不动其他角色的任何既有授权。
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.menu_id, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, r.tenant_id
FROM system_role r
CROSS JOIN (VALUES (5300), (5302), (5303), (5317), (5318), (5319), (5320), (5321), (5322), (5323)) AS m(menu_id)
WHERE r.code = 'firstchain:franchisee:leader' AND r.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm
                  WHERE rm.role_id = r.id AND rm.menu_id = m.menu_id AND rm.deleted = FALSE);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.menu_id, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, r.tenant_id
FROM system_role r
CROSS JOIN (VALUES (5300), (5302), (5318), (5320), (5321), (5322)) AS m(menu_id)
WHERE r.code = 'firstchain:franchisee:member' AND r.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm
                  WHERE rm.role_id = r.id AND rm.menu_id = m.menu_id AND rm.deleted = FALSE);
