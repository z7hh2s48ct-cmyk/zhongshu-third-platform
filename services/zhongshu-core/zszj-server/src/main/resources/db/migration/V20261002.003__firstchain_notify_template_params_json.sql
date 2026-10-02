-- ZS-FC-003 缺陷修复：首链站内通知模板 params 列改为 JSON 数组（平台约定）。
--
-- V20261001.002 种子把 params 写成逗号分隔文本（如 appKey,applicantName），而 system_notify_template.params 由
-- NotifyTemplateDO（List<String> + JacksonTypeHandler）按 JSON 数组读取：任何首链通知派发一查模板即抛
-- JsonParseException（Unrecognized token 'appKey'），审批/开通/下发/分配/领取/结束六节点通知在真实环境全部失败。
-- H2 单测 mock 了模板服务，PG 运行期套件未触达模板读取，故此前未暴露（真实 server E2E 暴露）。
--
-- 已发布迁移不可改（validate-on-migrate 校验和），故以新迁移订正；WHERE 精确匹配旧值，幂等且不覆盖人工已改过的模板。
-- 恢复方式：将 params 改回逗号分隔文本即可回到旧状态（不建议——旧状态下通知不可用）。

UPDATE system_notify_template SET params = '["appKey","applicantName"]', update_time = NOW()
WHERE code IN ('firstchain_application_submitted', 'firstchain_application_approved')
  AND params = 'appKey,applicantName' AND deleted = 0;

UPDATE system_notify_template SET params = '["appKey","applicantName","reason"]', update_time = NOW()
WHERE code = 'firstchain_application_rejected'
  AND params = 'appKey,applicantName,reason' AND deleted = 0;

UPDATE system_notify_template SET params = '["leadId","leadKey","customerName"]', update_time = NOW()
WHERE code IN ('firstchain_lead_distributed', 'firstchain_lead_assigned', 'firstchain_lead_claimed')
  AND params = 'leadId,leadKey,customerName' AND deleted = 0;

UPDATE system_notify_template SET params = '["leadId","leadKey","customerName","closeType"]', update_time = NOW()
WHERE code = 'firstchain_lead_closed'
  AND params = 'leadId,leadKey,customerName,closeType' AND deleted = 0;
