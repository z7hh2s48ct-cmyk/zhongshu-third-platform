-- ZS-FC-003 服务端接线 wave：首链六节点站内通知模板种子（MSG-001 统一派发；PILOT-REQ-010）。
-- 模板编码/必填参数与 cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates、
-- FirstchainNotifyWiringService 载荷不得漂移（漂移即 TEMPLATE_NOT_FOUND / TEMPLATE_PARAM_MISSING 可见失败）。
-- 内容占位循 Hutool StrUtil.format（{key}）；params 列为必填参数清单（逗号分隔）。

INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (101, '首链申请提交提醒', 'firstchain_application_submitted', '首链业务',
   '【首链】加盟商申请待审批：{applicantName}（申请编号 {appKey}），请及时处理。', 1,
   'appKey,applicantName', 0, 'ZS-FC-003 六节点通知：审批节点（提交提醒，与 MSG-002 审批待办同事务落位）', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (102, '首链申请审批通过', 'firstchain_application_approved', '首链业务',
   '【首链】加盟商申请已通过：{applicantName}（{appKey}）审批通过并完成开通，可使用负责人账号开展工作。', 1,
   'appKey,applicantName', 0, 'ZS-FC-003 六节点通知：审批节点（通过支路）+ 开通节点', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (103, '首链申请审批拒绝', 'firstchain_application_rejected', '首链业务',
   '【首链】加盟商申请未通过：{applicantName}（{appKey}），审批意见：{reason}', 1,
   'appKey,applicantName,reason', 0, 'ZS-FC-003 六节点通知：审批节点（拒绝支路，意见必填）', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (104, '首链线索下发', 'firstchain_lead_distributed', '首链业务',
   '【首链】新线索已下发至贵组织：{customerName}（{leadKey}），请及时分配员工跟进。', 1,
   'leadId,leadKey,customerName', 0, 'ZS-FC-003 六节点通知：下发节点（致归属组织负责人）', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (105, '首链线索分配', 'firstchain_lead_assigned', '首链业务',
   '【首链】线索已分配给您：{customerName}（{leadKey}），请及时领取并跟进。', 1,
   'leadId,leadKey,customerName', 0, 'ZS-FC-003 六节点通知：分配节点（致被分配员工；改派复用）', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (106, '首链线索领取', 'firstchain_lead_claimed', '首链业务',
   '【首链】线索已被领取：{customerName}（{leadKey}）已由员工领取进入跟进。', 1,
   'leadId,leadKey,customerName', 0, 'ZS-FC-003 六节点通知：领取节点（致归属组织负责人）', '1', NOW(), '1', NOW(), 0);
INSERT INTO system_notify_template (id, name, code, nickname, content, type, params, status, remark, creator, create_time, updater, update_time, deleted) VALUES
  (107, '首链线索结束', 'firstchain_lead_closed', '首链业务',
   '【首链】线索已结束：{customerName}（{leadKey}），结束类型：{closeType}', 1,
   'leadId,leadKey,customerName,closeType', 0, 'ZS-FC-003 六节点通知：结束节点（转商机/无效关闭共用，致归属组织负责人）', '1', NOW(), '1', NOW(), 0);
