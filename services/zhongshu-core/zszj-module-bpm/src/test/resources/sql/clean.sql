DELETE FROM "bpm_form";
DELETE FROM "bpm_user_group";
DELETE FROM "bpm_category";
-- ZS-BPM-004：接入合同样例链路的跨模块设施表与样例对象表
DELETE FROM "tech_neutral_contract_record";
DELETE FROM "system_notify_todo";
DELETE FROM "inbox_object_watermark";
DELETE FROM "inbox_event";
DELETE FROM "outbox_event";
DELETE FROM "audit_event";
-- ZS-BPM-003：首链领域状态与幂等写回
DELETE FROM "bpm_first_chain_process_binding";
DELETE FROM "bpm_first_chain_application";
