-- ZS-FC-001：firstchain 模块 H2 测试清理（BaseDbUnitTest AFTER_TEST_METHOD，与 bpm 模块同型）
DELETE FROM "audit_event";
-- ZS-BPM-003：首链领域状态与幂等写回
DELETE FROM "bpm_first_chain_process_binding";
DELETE FROM "bpm_first_chain_application";
-- ZS-FC-001：system 侧最小桩
DELETE FROM "system_organization";
DELETE FROM "system_role";
